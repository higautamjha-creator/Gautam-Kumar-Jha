package com.example.data.cloud

import android.content.Context
import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentFileType
import com.example.data.model.FirebaseRuntimeConfig
import com.example.data.model.ScanFilterMode
import com.example.data.model.SyncStatus
import com.example.data.model.UserAuthProfile
import com.example.data.model.VaultDocumentEntity
import com.example.data.scanner.DocumentFileProcessor
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            cont.resume(task.result)
        } else {
            cont.resumeWithException(
                task.exception ?: IllegalStateException("Firebase task failed")
            )
        }
    }
}

class FirebaseVaultService(private val appContext: Context) {

    fun isFirebaseInitialized(): Boolean {
        return try {
            FirebaseApp.getApps(appContext).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun tryInitializeFirebase(config: FirebaseRuntimeConfig): Result<Boolean> {
        return try {
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                return Result.success(true)
            }
            // First try default google-services.json initialization
            val defaultApp = FirebaseApp.initializeApp(appContext)
            if (defaultApp != null) {
                configureFirestoreOfflineCache()
                return Result.success(true)
            }
            // Otherwise initialize from runtime config if provided
            if (config.isComplete) {
                val optionsBuilder = FirebaseOptions.Builder()
                    .setProjectId(config.projectId.trim())
                    .setApplicationId(config.applicationId.trim())
                    .setApiKey(config.apiKey.trim())
                if (config.storageBucket.isNotBlank()) {
                    optionsBuilder.setStorageBucket(config.storageBucket.trim())
                }
                FirebaseApp.initializeApp(appContext, optionsBuilder.build())
                configureFirestoreOfflineCache()
                return Result.success(true)
            }
            Result.success(false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun configureFirestoreOfflineCache() {
        try {
            if (!isFirebaseInitialized()) return
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder(firestore.firestoreSettings)
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(100L * 1024L * 1024L) // 100 MB Firestore offline cache
                        .build()
                )
                .build()
            firestore.firestoreSettings = settings
        } catch (_: Exception) {
            // Firestore settings already locked after first use; safe to ignore
        }
    }

    fun getCurrentUser(): UserAuthProfile? {
        if (!isFirebaseInitialized()) return null
        return try {
            val user = FirebaseAuth.getInstance().currentUser ?: return null
            user.toAuthProfile()
        } catch (_: Exception) {
            null
        }
    }

    fun resolveDefaultWebClientId(customWebClientId: String): String {
        if (customWebClientId.isNotBlank()) return customWebClientId.trim()
        return try {
            val resId = appContext.resources.getIdentifier(
                "default_web_client_id",
                "string",
                appContext.packageName
            )
            if (resId != 0) appContext.getString(resId) else ""
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun signInWithGoogleCredentialManager(
        activityContext: Context,
        customWebClientId: String
    ): Result<UserAuthProfile> {
        return try {
            if (!isFirebaseInitialized()) {
                return Result.failure(
                    IllegalStateException(
                        "Firebase is not initialized yet. Add google-services.json to app/ or enter your Firebase Project settings under Cloud Configuration."
                    )
                )
            }
            val serverClientId = resolveDefaultWebClientId(customWebClientId)
            if (serverClientId.isBlank()) {
                return Result.failure(
                    IllegalStateException(
                        "Missing Google Web Client ID (OAuth 2.0 Client ID). Provide it in Cloud Configuration or via google-services.json."
                    )
                )
            }

            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential =
                    GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = FirebaseAuth.getInstance()
                    .signInWithCredential(firebaseCredential)
                    .awaitTask()
                val user = authResult.user
                    ?: return Result.failure(IllegalStateException("Google Sign-In returned no user"))
                Result.success(user.toAuthProfile())
            } else {
                Result.failure(IllegalStateException("Unsupported credential type returned"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInOrRegisterWithGmailPassword(
        email: String,
        password: String,
        isRegistration: Boolean
    ): Result<UserAuthProfile> {
        return try {
            if (!isFirebaseInitialized()) {
                return Result.failure(
                    IllegalStateException(
                        "Firebase is not initialized yet. Add google-services.json to app/ or configure Firebase project credentials."
                    )
                )
            }
            val auth = FirebaseAuth.getInstance()
            val authResult = if (isRegistration) {
                auth.createUserWithEmailAndPassword(email.trim(), password).awaitTask()
            } else {
                auth.signInWithEmailAndPassword(email.trim(), password).awaitTask()
            }
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Authentication returned empty user"))
            Result.success(user.toAuthProfile())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            if (isFirebaseInitialized()) {
                FirebaseAuth.getInstance().signOut()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun uploadDocumentToCloud(
        document: VaultDocumentEntity,
        user: UserAuthProfile
    ): Result<VaultDocumentEntity> = withContext(Dispatchers.IO) {
        try {
            if (!isFirebaseInitialized()) {
                return@withContext Result.failure(
                    IllegalStateException("Firebase Cloud is not initialized")
                )
            }

            val localFile = File(document.localFilePath)
            var downloadUrl = document.cloudStorageUrl
            var storagePath = document.cloudStoragePath

            // Upload binary file to Firebase Cloud Storage if local file exists
            if (localFile.exists()) {
                try {
                    val storageRef = FirebaseStorage.getInstance().reference
                    val ext = localFile.extension.ifBlank { "jpg" }
                    val remotePath = "users/${user.uid}/documents/${document.id}.$ext"
                    val docRef = storageRef.child(remotePath)
                    docRef.putFile(Uri.fromFile(localFile)).awaitTask()
                    downloadUrl = docRef.downloadUrl.awaitTask().toString()
                    storagePath = remotePath
                } catch (_: Exception) {
                    // If Firebase Storage bucket isn't provisioned yet, still sync metadata to Firestore
                }
            }

            val updatedDoc = document.copy(
                ownerUid = user.uid,
                ownerEmail = user.email,
                cloudStorageUrl = downloadUrl,
                cloudStoragePath = storagePath,
                syncStatus = SyncStatus.SYNCED.name,
                updatedAt = System.currentTimeMillis()
            )

            val firestore = FirebaseFirestore.getInstance()
            val payload = mapOf(
                "id" to updatedDoc.id,
                "ownerUid" to updatedDoc.ownerUid,
                "ownerEmail" to updatedDoc.ownerEmail,
                "title" to updatedDoc.title,
                "category" to updatedDoc.category,
                "fileType" to updatedDoc.fileType,
                "keywords" to updatedDoc.keywords,
                "notes" to updatedDoc.notes,
                "cloudStorageUrl" to (updatedDoc.cloudStorageUrl ?: ""),
                "cloudStoragePath" to (updatedDoc.cloudStoragePath ?: ""),
                "fileSizeBytes" to updatedDoc.fileSizeBytes,
                "pageCount" to updatedDoc.pageCount,
                "sha256Checksum" to updatedDoc.sha256Checksum,
                "isOfflinePinned" to updatedDoc.isOfflinePinned,
                "isFavorite" to updatedDoc.isFavorite,
                "scanFilter" to updatedDoc.scanFilter,
                "createdAt" to updatedDoc.createdAt,
                "updatedAt" to updatedDoc.updatedAt
            )

            firestore.collection("users")
                .document(user.uid)
                .collection("documents")
                .document(updatedDoc.id)
                .set(payload, SetOptions.merge())
                .awaitTask()

            Result.success(updatedDoc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchDocumentsFromCloud(user: UserAuthProfile): Result<List<VaultDocumentEntity>> =
        withContext(Dispatchers.IO) {
            try {
                if (!isFirebaseInitialized()) {
                    return@withContext Result.failure(
                        IllegalStateException("Firebase Cloud is not initialized")
                    )
                }

                val firestore = FirebaseFirestore.getInstance()
                val snapshot = firestore.collection("users")
                    .document(user.uid)
                    .collection("documents")
                    .get()
                    .awaitTask()

                val vaultDir = DocumentFileProcessor.getVaultDirectory(appContext)
                val restoredDocs = mutableListOf<VaultDocumentEntity>()

                for (docSnap in snapshot.documents) {
                    val id = docSnap.getString("id") ?: docSnap.id
                    val title = docSnap.getString("title") ?: "Restored Document"
                    val category = docSnap.getString("category") ?: DocumentCategory.IDENTITY.name
                    val fileType = docSnap.getString("fileType") ?: DocumentFileType.IMAGE_SCAN.name
                    val keywords = docSnap.getString("keywords") ?: ""
                    val notes = docSnap.getString("notes") ?: ""
                    val cloudUrl = docSnap.getString("cloudStorageUrl")?.takeIf { it.isNotBlank() }
                    val cloudPath = docSnap.getString("cloudStoragePath")?.takeIf { it.isNotBlank() }
                    val fileSizeBytes = docSnap.getLong("fileSizeBytes") ?: 0L
                    val pageCount = (docSnap.getLong("pageCount") ?: 1L).toInt()
                    val sha256Checksum = docSnap.getString("sha256Checksum") ?: ""
                    val isOfflinePinned = docSnap.getBoolean("isOfflinePinned") ?: true
                    val isFavorite = docSnap.getBoolean("isFavorite") ?: false
                    val scanFilter = docSnap.getString("scanFilter") ?: ScanFilterMode.ORIGINAL.name
                    val createdAt = docSnap.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = docSnap.getLong("updatedAt") ?: System.currentTimeMillis()

                    val ext = if (fileType == DocumentFileType.PDF_DOCUMENT.name) "pdf" else "jpg"
                    val localTarget = File(vaultDir, "doc_${id}.$ext")

                    if (!localTarget.exists() && !cloudPath.isNullOrBlank()) {
                        try {
                            val storageRef = FirebaseStorage.getInstance().reference.child(cloudPath)
                            storageRef.getFile(localTarget).awaitTask()
                        } catch (_: Exception) {
                        }
                    }

                    restoredDocs.add(
                        VaultDocumentEntity(
                            id = id,
                            ownerUid = user.uid,
                            ownerEmail = user.email,
                            title = title,
                            category = category,
                            fileType = fileType,
                            keywords = keywords,
                            notes = notes,
                            localFilePath = localTarget.absolutePath,
                            cloudStorageUrl = cloudUrl,
                            cloudStoragePath = cloudPath,
                            fileSizeBytes = if (localTarget.exists()) localTarget.length() else fileSizeBytes,
                            pageCount = pageCount,
                            sha256Checksum = sha256Checksum,
                            isOfflinePinned = isOfflinePinned,
                            isFavorite = isFavorite,
                            scanFilter = scanFilter,
                            syncStatus = SyncStatus.SYNCED.name,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                    )
                }

                Result.success(restoredDocs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteDocumentFromCloud(document: VaultDocumentEntity, user: UserAuthProfile) =
        withContext(Dispatchers.IO) {
            try {
                if (!isFirebaseInitialized()) return@withContext
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .collection("documents")
                    .document(document.id)
                    .delete()
                    .awaitTask()

                val storagePath = document.cloudStoragePath
                if (!storagePath.isNullOrBlank()) {
                    FirebaseStorage.getInstance().reference.child(storagePath).delete().awaitTask()
                }
            } catch (_: Exception) {
            }
        }

    private fun FirebaseUser.toAuthProfile(): UserAuthProfile {
        val emailStr = email ?: "authenticated.user@gmail.com"
        val nameStr = displayName?.takeIf { it.isNotBlank() }
            ?: emailStr.substringBefore("@").replaceFirstChar { it.uppercase() }
        return UserAuthProfile(
            uid = uid,
            email = emailStr,
            displayName = nameStr,
            photoUrl = photoUrl?.toString(),
            isFirebaseConnected = true
        )
    }
}
