package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.cloud.FirebaseVaultService
import com.example.data.local.DocumentDao
import com.example.data.local.SecurityLockSettings
import com.example.data.local.VaultPreferences
import com.example.data.model.DocumentCategory
import com.example.data.model.FirebaseRuntimeConfig
import com.example.data.model.ScanFilterMode
import com.example.data.model.SyncStatus
import com.example.data.model.UserAuthProfile
import com.example.data.model.VaultDocumentEntity
import com.example.data.scanner.DocumentFileProcessor
import com.example.data.scanner.ProcessedFileResult
import com.example.data.security.BiometricAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class VaultRepository(
    private val appContext: Context,
    private val documentDao: DocumentDao,
    private val firebaseService: FirebaseVaultService,
    private val preferences: VaultPreferences
) {
    val allDocumentsFlow: Flow<List<VaultDocumentEntity>> = documentDao.observeAllDocuments()
    val firebaseConfigFlow: Flow<FirebaseRuntimeConfig> = preferences.firebaseConfigFlow
    val autoSyncEnabledFlow: Flow<Boolean> = preferences.autoSyncEnabledFlow
    val offlineCacheDefaultFlow: Flow<Boolean> = preferences.offlineCacheDefaultFlow
    val securityLockSettingsFlow: Flow<SecurityLockSettings> = preferences.securityLockSettingsFlow

    fun isFirebaseReady(): Boolean = firebaseService.isFirebaseInitialized()

    fun initializeFirebaseIfPossible(config: FirebaseRuntimeConfig): Result<Boolean> {
        return firebaseService.tryInitializeFirebase(config)
    }

    fun getCurrentUser(): UserAuthProfile? = firebaseService.getCurrentUser()

    suspend fun saveFirebaseConfig(config: FirebaseRuntimeConfig): Result<Boolean> {
        preferences.saveFirebaseConfig(config)
        return firebaseService.tryInitializeFirebase(config)
    }

    suspend fun signInWithGoogle(
        activityContext: Context,
        customWebClientId: String
    ): Result<UserAuthProfile> {
        val result = firebaseService.signInWithGoogleCredentialManager(activityContext, customWebClientId)
        result.onSuccess { user ->
            documentDao.claimLocalDocumentsForUser(user.uid, user.email)
        }
        return result
    }

    suspend fun signInWithGmailPassword(
        email: String,
        password: String,
        isRegistration: Boolean
    ): Result<UserAuthProfile> {
        val result = firebaseService.signInOrRegisterWithGmailPassword(email, password, isRegistration)
        result.onSuccess { user ->
            documentDao.claimLocalDocumentsForUser(user.uid, user.email)
        }
        return result
    }

    fun signOut() {
        firebaseService.signOut()
    }

    suspend fun saveScannedDocument(
        bitmap: Bitmap,
        title: String,
        category: DocumentCategory,
        keywords: String,
        notes: String,
        filterMode: ScanFilterMode,
        isOfflinePinned: Boolean,
        currentUser: UserAuthProfile?
    ): VaultDocumentEntity {
        val docId = UUID.randomUUID().toString()
        val processed = DocumentFileProcessor.saveScannedBitmapToVault(
            context = appContext,
            bitmap = bitmap,
            filterMode = filterMode,
            documentId = docId
        )
        return persistNewDocument(
            docId = docId,
            processed = processed,
            title = title,
            category = category,
            keywords = keywords,
            notes = notes,
            filterMode = filterMode,
            isOfflinePinned = isOfflinePinned,
            currentUser = currentUser
        )
    }

    suspend fun importDocumentFromUri(
        uri: Uri,
        title: String,
        category: DocumentCategory,
        keywords: String,
        notes: String,
        isOfflinePinned: Boolean,
        currentUser: UserAuthProfile?
    ): VaultDocumentEntity {
        val docId = UUID.randomUUID().toString()
        val processed = DocumentFileProcessor.importUriToVault(
            context = appContext,
            uri = uri,
            documentId = docId
        )
        val finalTitle = title.trim().ifEmpty { processed.suggestedTitle }
        return persistNewDocument(
            docId = docId,
            processed = processed,
            title = finalTitle,
            category = category,
            keywords = keywords,
            notes = notes,
            filterMode = ScanFilterMode.ORIGINAL,
            isOfflinePinned = isOfflinePinned,
            currentUser = currentUser
        )
    }

    private suspend fun persistNewDocument(
        docId: String,
        processed: ProcessedFileResult,
        title: String,
        category: DocumentCategory,
        keywords: String,
        notes: String,
        filterMode: ScanFilterMode,
        isOfflinePinned: Boolean,
        currentUser: UserAuthProfile?
    ): VaultDocumentEntity {
        val now = System.currentTimeMillis()
        val entity = VaultDocumentEntity(
            id = docId,
            ownerUid = currentUser?.uid ?: "local_vault",
            ownerEmail = currentUser?.email ?: "Offline Local Vault",
            title = title.trim().ifEmpty { processed.suggestedTitle },
            category = category.name,
            fileType = processed.fileType.name,
            keywords = keywords.trim(),
            notes = notes.trim(),
            localFilePath = processed.localFile.absolutePath,
            fileSizeBytes = processed.fileSizeBytes,
            pageCount = processed.pageCount,
            sha256Checksum = processed.sha256Checksum,
            isOfflinePinned = isOfflinePinned,
            isFavorite = false,
            scanFilter = filterMode.name,
            syncStatus = SyncStatus.PENDING_UPLOAD.name,
            createdAt = now,
            updatedAt = now
        )

        documentDao.upsertDocument(entity)

        if (currentUser != null && firebaseService.isFirebaseInitialized()) {
            val uploadRes = firebaseService.uploadDocumentToCloud(entity, currentUser)
            uploadRes.onSuccess { syncedDoc ->
                documentDao.upsertDocument(syncedDoc)
                return syncedDoc
            }
        }
        return entity
    }

    suspend fun updateDocumentMetadata(
        document: VaultDocumentEntity,
        newTitle: String,
        newCategory: DocumentCategory,
        newKeywords: String,
        newNotes: String,
        currentUser: UserAuthProfile?
    ) {
        val updated = document.copy(
            title = newTitle.trim().ifEmpty { document.title },
            category = newCategory.name,
            keywords = newKeywords.trim(),
            notes = newNotes.trim(),
            syncStatus = SyncStatus.PENDING_UPLOAD.name,
            updatedAt = System.currentTimeMillis()
        )
        documentDao.updateDocument(updated)
        if (currentUser != null && firebaseService.isFirebaseInitialized()) {
            firebaseService.uploadDocumentToCloud(updated, currentUser).onSuccess {
                documentDao.upsertDocument(it)
            }
        }
    }

    suspend fun toggleFavorite(document: VaultDocumentEntity) {
        documentDao.updateFavorite(document.id, !document.isFavorite)
    }

    suspend fun toggleOfflinePin(document: VaultDocumentEntity) {
        documentDao.updateOfflinePinned(document.id, !document.isOfflinePinned)
    }

    suspend fun syncPendingDocuments(user: UserAuthProfile): Int {
        documentDao.claimLocalDocumentsForUser(user.uid, user.email)
        val pending = documentDao.getPendingSyncDocuments()
        var syncedCount = 0
        for (doc in pending) {
            documentDao.updateSyncState(
                id = doc.id,
                syncStatus = SyncStatus.UPLOADING.name,
                cloudUrl = doc.cloudStorageUrl,
                cloudPath = doc.cloudStoragePath,
                ownerUid = user.uid,
                ownerEmail = user.email
            )
            val res = firebaseService.uploadDocumentToCloud(doc, user)
            res.onSuccess { updated ->
                documentDao.upsertDocument(updated)
                syncedCount++
            }.onFailure {
                documentDao.updateSyncState(
                    id = doc.id,
                    syncStatus = SyncStatus.SYNC_ERROR.name,
                    cloudUrl = doc.cloudStorageUrl,
                    cloudPath = doc.cloudStoragePath,
                    ownerUid = user.uid,
                    ownerEmail = user.email
                )
            }
        }
        return syncedCount
    }

    suspend fun pullCloudBackup(user: UserAuthProfile): Result<Int> {
        val result = firebaseService.fetchDocumentsFromCloud(user)
        return result.map { remoteDocs ->
            val localDocsById = documentDao.getAllDocumentsOnce().associateBy { it.id }
            val merged = remoteDocs.map { remote ->
                val existingLocal = localDocsById[remote.id]
                if (existingLocal != null && File(existingLocal.localFilePath).exists()) {
                    remote.copy(localFilePath = existingLocal.localFilePath)
                } else {
                    remote
                }
            }
            if (merged.isNotEmpty()) {
                documentDao.upsertDocuments(merged)
            }
            merged.size
        }
    }

    suspend fun verifyOfflineCacheIntegrity(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val allDocs = documentDao.getAllDocumentsOnce()
        var verified = 0
        for (doc in allDocs) {
            val file = File(doc.localFilePath)
            if (file.exists()) {
                val actualHash = DocumentFileProcessor.computeSha256(file)
                if (doc.sha256Checksum.isBlank() || actualHash.equals(doc.sha256Checksum, ignoreCase = true)) {
                    verified++
                }
            }
        }
        verified to allDocs.size
    }

    suspend fun deleteDocument(document: VaultDocumentEntity, currentUser: UserAuthProfile?) =
        withContext(Dispatchers.IO) {
            try {
                val localFile = File(document.localFilePath)
                if (localFile.exists()) {
                    localFile.delete()
                }
            } catch (_: Exception) {
            }
            documentDao.deleteDocumentById(document.id)
            if (currentUser != null) {
                firebaseService.deleteDocumentFromCloud(document, currentUser)
            }
        }

    suspend fun setAutoSyncEnabled(enabled: Boolean) = preferences.setAutoSyncEnabled(enabled)
    suspend fun setOfflineCacheDefault(enabled: Boolean) = preferences.setOfflineCacheDefault(enabled)
    suspend fun setBiometricLockEnabled(enabled: Boolean) = preferences.setBiometricLockEnabled(enabled)
    suspend fun setAutoLockOnBackground(enabled: Boolean) = preferences.setAutoLockOnBackground(enabled)

    suspend fun saveNewVaultPin(rawPin: String) {
        val hash = BiometricAuthHelper.hashPinSha256(rawPin)
        preferences.saveVaultPinHash(hash)
    }

    fun verifyVaultPin(rawPin: String, expectedHash: String): Boolean {
        if (expectedHash.isBlank()) return false
        return BiometricAuthHelper.hashPinSha256(rawPin) == expectedHash
    }
}
