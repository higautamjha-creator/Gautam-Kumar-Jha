package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.FirebaseVaultService
import com.example.data.local.VaultDatabase
import com.example.data.local.VaultPreferences
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentFileType
import com.example.data.model.FirebaseRuntimeConfig
import com.example.data.model.ScanFilterMode
import com.example.data.model.SortOption
import com.example.data.model.SyncStatus
import com.example.data.model.UserAuthProfile
import com.example.data.model.VaultDocumentEntity
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class VaultTab(val route: String, val label: String) {
    DASHBOARD("dashboard", "Vault"),
    SCANNER("scanner", "Scanner"),
    OFFLINE("offline", "Offline"),
    ACCOUNT("account", "Cloud Auth")
}

data class CategorySummary(
    val category: DocumentCategory,
    val count: Int,
    val totalBytes: Long
)

data class VaultUiState(
    val currentTab: VaultTab = VaultTab.DASHBOARD,
    val allDocuments: List<VaultDocumentEntity> = emptyList(),
    val filteredDocuments: List<VaultDocumentEntity> = emptyList(),
    val categorySummaries: List<CategorySummary> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: DocumentCategory? = null,
    val onlyFavorites: Boolean = false,
    val onlyPdfs: Boolean = false,
    val sortOption: SortOption = SortOption.NEWEST_FIRST,
    val currentUser: UserAuthProfile? = null,
    val isFirebaseInitialized: Boolean = false,
    val firebaseConfig: FirebaseRuntimeConfig = FirebaseRuntimeConfig(),
    val autoSyncEnabled: Boolean = true,
    val offlineCacheDefault: Boolean = true,
    val isBusy: Boolean = false,
    val isSyncing: Boolean = false,
    val statusBannerMessage: String? = null,
    val errorBannerMessage: String? = null,
    val selectedDocumentForPreview: VaultDocumentEntity? = null,
    val selectedDocumentForEdit: VaultDocumentEntity? = null,
    val showFirebaseConfigDialog: Boolean = false,
    val pendingScanBitmap: Bitmap? = null,
    val pendingImportUri: Uri? = null,
    val isVaultLocked: Boolean = true,
    val biometricLockEnabled: Boolean = true,
    val autoLockOnBackground: Boolean = true,
    val hasConfiguredPin: Boolean = false,
    val lockScreenMessage: String? = null
) {
    val totalStorageBytes: Long
        get() = allDocuments.sumOf { it.fileSizeBytes }

    val offlineCachedDocuments: List<VaultDocumentEntity>
        get() = allDocuments.filter { it.isOfflinePinned && File(it.localFilePath).exists() }

    val offlineStorageBytes: Long
        get() = offlineCachedDocuments.sumOf { it.fileSizeBytes }

    val syncedDocumentsCount: Int
        get() = allDocuments.count { it.syncStatusEnum == SyncStatus.SYNCED }

    val pendingSyncCount: Int
        get() = allDocuments.count { it.syncStatusEnum != SyncStatus.SYNCED }
}

private data class FilterParams(
    val searchQuery: String = "",
    val selectedCategory: DocumentCategory? = null,
    val onlyFavorites: Boolean = false,
    val onlyPdfs: Boolean = false,
    val sortOption: SortOption = SortOption.NEWEST_FIRST
)

private data class TransientUiFlags(
    val currentTab: VaultTab = VaultTab.DASHBOARD,
    val currentUser: UserAuthProfile? = null,
    val isFirebaseInitialized: Boolean = false,
    val isBusy: Boolean = false,
    val isSyncing: Boolean = false,
    val statusBannerMessage: String? = null,
    val errorBannerMessage: String? = null,
    val selectedDocumentIdForPreview: String? = null,
    val selectedDocumentIdForEdit: String? = null,
    val showFirebaseConfigDialog: Boolean = false,
    val pendingScanBitmap: Bitmap? = null,
    val pendingImportUri: Uri? = null,
    val isSessionUnlocked: Boolean = false,
    val lockScreenMessage: String? = null
)

class VaultViewModel(
    private val repository: VaultRepository
) : ViewModel() {

    private val filterParamsFlow = MutableStateFlow(FilterParams())
    private val transientFlagsFlow = MutableStateFlow(
        TransientUiFlags(
            isFirebaseInitialized = repository.isFirebaseReady(),
            currentUser = repository.getCurrentUser()
        )
    )

    val uiState: StateFlow<VaultUiState> = combine(
        repository.allDocumentsFlow,
        filterParamsFlow,
        transientFlagsFlow,
        repository.firebaseConfigFlow,
        combine(
            repository.autoSyncEnabledFlow,
            repository.offlineCacheDefaultFlow,
            repository.securityLockSettingsFlow
        ) { a, b, sec -> Triple(a, b, sec) }
    ) { allDocs, filters, flags, fbConfig, prefsTriple ->
        val (autoSync, offlineDefault, secSettings) = prefsTriple

        // Ensure Firebase initializes automatically if config was saved in DataStore
        if (!flags.isFirebaseInitialized && (fbConfig.isComplete || repository.isFirebaseReady())) {
            val initRes = repository.initializeFirebaseIfPossible(fbConfig)
            if (initRes.getOrDefault(false)) {
                transientFlagsFlow.update {
                    it.copy(
                        isFirebaseInitialized = true,
                        currentUser = repository.getCurrentUser()
                    )
                }
            }
        }

        val query = filters.searchQuery.trim().lowercase()
        val filtered = allDocs
            .filter { doc ->
                val matchesCategory = filters.selectedCategory == null ||
                    doc.categoryEnum == filters.selectedCategory
                val matchesFav = !filters.onlyFavorites || doc.isFavorite
                val matchesPdf = !filters.onlyPdfs || doc.fileTypeEnum == DocumentFileType.PDF_DOCUMENT
                val matchesSearch = if (query.isEmpty()) {
                    true
                } else {
                    val searchableText = buildString {
                        append(doc.title.lowercase()).append(' ')
                        append(doc.keywords.lowercase()).append(' ')
                        append(doc.notes.lowercase()).append(' ')
                        append(doc.categoryEnum.displayName.lowercase()).append(' ')
                        append(doc.categoryEnum.shortName.lowercase()).append(' ')
                        append(doc.fileTypeEnum.label.lowercase()).append(' ')
                        append(doc.sha256Checksum.lowercase())
                    }
                    val tokens = query.split(" ").filter { it.isNotBlank() }
                    tokens.all { token -> searchableText.contains(token) }
                }
                matchesCategory && matchesFav && matchesPdf && matchesSearch
            }
            .let { list ->
                when (filters.sortOption) {
                    SortOption.NEWEST_FIRST -> list.sortedWith(
                        compareByDescending<VaultDocumentEntity> { it.isFavorite }
                            .thenByDescending { it.updatedAt }
                    )
                    SortOption.OLDEST_FIRST -> list.sortedBy { it.createdAt }
                    SortOption.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
                    SortOption.SIZE_DESC -> list.sortedByDescending { it.fileSizeBytes }
                }
            }

        val summaries = DocumentCategory.entries.map { category ->
            val docsInCategory = allDocs.filter { it.categoryEnum == category }
            CategorySummary(
                category = category,
                count = docsInCategory.size,
                totalBytes = docsInCategory.sumOf { it.fileSizeBytes }
            )
        }

        val previewDoc = flags.selectedDocumentIdForPreview?.let { id ->
            allDocs.firstOrNull { it.id == id }
        }
        val editDoc = flags.selectedDocumentIdForEdit?.let { id ->
            allDocs.firstOrNull { it.id == id }
        }

        VaultUiState(
            currentTab = flags.currentTab,
            allDocuments = allDocs,
            filteredDocuments = filtered,
            categorySummaries = summaries,
            searchQuery = filters.searchQuery,
            selectedCategory = filters.selectedCategory,
            onlyFavorites = filters.onlyFavorites,
            onlyPdfs = filters.onlyPdfs,
            sortOption = filters.sortOption,
            currentUser = flags.currentUser,
            isFirebaseInitialized = flags.isFirebaseInitialized || repository.isFirebaseReady(),
            firebaseConfig = fbConfig,
            autoSyncEnabled = autoSync,
            offlineCacheDefault = offlineDefault,
            isBusy = flags.isBusy,
            isSyncing = flags.isSyncing,
            statusBannerMessage = flags.statusBannerMessage,
            errorBannerMessage = flags.errorBannerMessage,
            selectedDocumentForPreview = previewDoc,
            selectedDocumentForEdit = editDoc,
            showFirebaseConfigDialog = flags.showFirebaseConfigDialog,
            pendingScanBitmap = flags.pendingScanBitmap,
            pendingImportUri = flags.pendingImportUri,
            isVaultLocked = secSettings.biometricLockEnabled && !flags.isSessionUnlocked,
            biometricLockEnabled = secSettings.biometricLockEnabled,
            autoLockOnBackground = secSettings.autoLockOnBackground,
            hasConfiguredPin = secSettings.vaultPinHash.isNotBlank(),
            lockScreenMessage = flags.lockScreenMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultUiState()
    )

    private var cachedPinHash: String = ""

    init {
        viewModelScope.launch {
            val ready = repository.initializeFirebaseIfPossible(FirebaseRuntimeConfig()).getOrDefault(false)
            val user = repository.getCurrentUser()
            transientFlagsFlow.update {
                it.copy(
                    isFirebaseInitialized = ready,
                    currentUser = user
                )
            }
        }
        viewModelScope.launch {
            repository.securityLockSettingsFlow.collect { sec ->
                cachedPinHash = sec.vaultPinHash
            }
        }
    }

    fun onBiometricUnlockSuccess() {
        transientFlagsFlow.update {
            it.copy(
                isSessionUnlocked = true,
                lockScreenMessage = null,
                statusBannerMessage = "Biometric identity verified. DocuVault unlocked."
            )
        }
    }

    fun onBiometricError(message: String) {
        transientFlagsFlow.update {
            it.copy(lockScreenMessage = message)
        }
    }

    fun clearLockScreenMessage() {
        transientFlagsFlow.update {
            it.copy(lockScreenMessage = null)
        }
    }

    fun submitPinToUnlockOrSetup(rawPin: String) {
        val cleaned = rawPin.trim()
        if (cleaned.length < 4) {
            transientFlagsFlow.update {
                it.copy(lockScreenMessage = "Enter a 4-digit security PIN.")
            }
            return
        }
        viewModelScope.launch {
            if (cachedPinHash.isBlank()) {
                repository.saveNewVaultPin(cleaned)
                transientFlagsFlow.update {
                    it.copy(
                        isSessionUnlocked = true,
                        lockScreenMessage = null,
                        statusBannerMessage = "4-digit backup security PIN configured & DocuVault unlocked."
                    )
                }
            } else {
                val valid = repository.verifyVaultPin(cleaned, cachedPinHash)
                if (valid) {
                    transientFlagsFlow.update {
                        it.copy(
                            isSessionUnlocked = true,
                            lockScreenMessage = null
                        )
                    }
                } else {
                    transientFlagsFlow.update {
                        it.copy(lockScreenMessage = "Incorrect Vault PIN. Please try again or use Fingerprint/Face unlock.")
                    }
                }
            }
        }
    }

    fun updateBackupVaultPin(newPin: String) {
        val cleaned = newPin.trim()
        if (cleaned.length < 4) {
            transientFlagsFlow.update {
                it.copy(errorBannerMessage = "Backup Vault PIN must be 4 digits.")
            }
            return
        }
        viewModelScope.launch {
            repository.saveNewVaultPin(cleaned)
            transientFlagsFlow.update {
                it.copy(statusBannerMessage = "Backup 4-digit Vault PIN updated.")
            }
        }
    }

    fun lockVaultNow() {
        viewModelScope.launch {
            if (!uiState.value.biometricLockEnabled) {
                repository.setBiometricLockEnabled(true)
            }
            transientFlagsFlow.update {
                it.copy(
                    isSessionUnlocked = false,
                    lockScreenMessage = null
                )
            }
        }
    }

    fun onAppBackgrounded() {
        val current = uiState.value
        if (current.biometricLockEnabled && current.autoLockOnBackground) {
            transientFlagsFlow.update {
                it.copy(
                    isSessionUnlocked = false,
                    lockScreenMessage = null
                )
            }
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricLockEnabled(enabled)
            transientFlagsFlow.update {
                it.copy(
                    isSessionUnlocked = true,
                    statusBannerMessage = if (enabled) {
                        "Biometric & PIN lock enabled for DocuVault."
                    } else {
                        "Biometric startup lock disabled."
                    }
                )
            }
        }
    }

    fun setAutoLockOnBackground(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoLockOnBackground(enabled)
        }
    }

    fun selectTab(tab: VaultTab) {
        transientFlagsFlow.update { it.copy(currentTab = tab) }
    }

    fun updateSearchQuery(query: String) {
        filterParamsFlow.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: DocumentCategory?) {
        filterParamsFlow.update { current ->
            val next = if (current.selectedCategory == category) null else category
            current.copy(selectedCategory = next)
        }
    }

    fun toggleFavoritesFilter() {
        filterParamsFlow.update { it.copy(onlyFavorites = !it.onlyFavorites) }
    }

    fun togglePdfOnlyFilter() {
        filterParamsFlow.update { it.copy(onlyPdfs = !it.onlyPdfs) }
    }

    fun updateSortOption(option: SortOption) {
        filterParamsFlow.update { it.copy(sortOption = option) }
    }

    fun clearAllFilters() {
        filterParamsFlow.value = FilterParams()
    }

    fun openDocumentPreview(document: VaultDocumentEntity) {
        transientFlagsFlow.update { it.copy(selectedDocumentIdForPreview = document.id) }
    }

    fun closeDocumentPreview() {
        transientFlagsFlow.update { it.copy(selectedDocumentIdForPreview = null) }
    }

    fun openDocumentEdit(document: VaultDocumentEntity) {
        transientFlagsFlow.update { it.copy(selectedDocumentIdForEdit = document.id) }
    }

    fun closeDocumentEdit() {
        transientFlagsFlow.update { it.copy(selectedDocumentIdForEdit = null) }
    }

    fun setShowFirebaseConfigDialog(show: Boolean) {
        transientFlagsFlow.update { it.copy(showFirebaseConfigDialog = show) }
    }

    fun stageCapturedBitmap(bitmap: Bitmap) {
        transientFlagsFlow.update {
            it.copy(
                pendingScanBitmap = bitmap,
                pendingImportUri = null,
                currentTab = VaultTab.SCANNER
            )
        }
    }

    fun stageImportedUri(uri: Uri) {
        transientFlagsFlow.update {
            it.copy(
                pendingImportUri = uri,
                pendingScanBitmap = null,
                currentTab = VaultTab.SCANNER
            )
        }
    }

    fun clearStagedDocument() {
        transientFlagsFlow.update {
            it.copy(pendingScanBitmap = null, pendingImportUri = null)
        }
    }

    fun dismissBanners() {
        transientFlagsFlow.update {
            it.copy(statusBannerMessage = null, errorBannerMessage = null)
        }
    }

    fun saveScannedDocument(
        bitmap: Bitmap,
        title: String,
        category: DocumentCategory,
        keywords: String,
        notes: String,
        filterMode: ScanFilterMode,
        isOfflinePinned: Boolean
    ) {
        viewModelScope.launch {
            transientFlagsFlow.update { it.copy(isBusy = true, errorBannerMessage = null) }
            try {
                val user = transientFlagsFlow.value.currentUser
                val saved = repository.saveScannedDocument(
                    bitmap = bitmap,
                    title = title,
                    category = category,
                    keywords = keywords,
                    notes = notes,
                    filterMode = filterMode,
                    isOfflinePinned = isOfflinePinned,
                    currentUser = user
                )
                val syncNote = if (saved.syncStatusEnum == SyncStatus.SYNCED) {
                    "and backed up to Google Cloud"
                } else {
                    "and cached locally for offline access"
                }
                transientFlagsFlow.update {
                    it.copy(
                        isBusy = false,
                        pendingScanBitmap = null,
                        pendingImportUri = null,
                        currentTab = VaultTab.DASHBOARD,
                        statusBannerMessage = "\"${saved.title}\" saved $syncNote."
                    )
                }
            } catch (e: Exception) {
                transientFlagsFlow.update {
                    it.copy(
                        isBusy = false,
                        errorBannerMessage = "Failed to save scan: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun saveImportedUriDocument(
        uri: Uri,
        title: String,
        category: DocumentCategory,
        keywords: String,
        notes: String,
        isOfflinePinned: Boolean
    ) {
        viewModelScope.launch {
            transientFlagsFlow.update { it.copy(isBusy = true, errorBannerMessage = null) }
            try {
                val user = transientFlagsFlow.value.currentUser
                val saved = repository.importDocumentFromUri(
                    uri = uri,
                    title = title,
                    category = category,
                    keywords = keywords,
                    notes = notes,
                    isOfflinePinned = isOfflinePinned,
                    currentUser = user
                )
                val syncNote = if (saved.syncStatusEnum == SyncStatus.SYNCED) {
                    "and backed up to Google Cloud"
                } else {
                    "and cached locally for offline access"
                }
                transientFlagsFlow.update {
                    it.copy(
                        isBusy = false,
                        pendingScanBitmap = null,
                        pendingImportUri = null,
                        currentTab = VaultTab.DASHBOARD,
                        statusBannerMessage = "\"${saved.title}\" (${saved.pageCount} pg) imported $syncNote."
                    )
                }
            } catch (e: Exception) {
                transientFlagsFlow.update {
                    it.copy(
                        isBusy = false,
                        errorBannerMessage = "Failed to import file: ${e.localizedMessage ?: "Unsupported file"}"
                    )
                }
            }
        }
    }

    fun updateDocumentMetadata(
        document: VaultDocumentEntity,
        newTitle: String,
        newCategory: DocumentCategory,
        newKeywords: String,
        newNotes: String
    ) {
        viewModelScope.launch {
            repository.updateDocumentMetadata(
                document = document,
                newTitle = newTitle,
                newCategory = newCategory,
                newKeywords = newKeywords,
                newNotes = newNotes,
                currentUser = transientFlagsFlow.value.currentUser
            )
            transientFlagsFlow.update {
                it.copy(
                    selectedDocumentIdForEdit = null,
                    statusBannerMessage = "Updated \"${newTitle.trim().ifEmpty { document.title }}\""
                )
            }
        }
    }

    fun toggleFavorite(document: VaultDocumentEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(document)
        }
    }

    fun toggleOfflinePin(document: VaultDocumentEntity) {
        viewModelScope.launch {
            repository.toggleOfflinePin(document)
            val stateLabel = if (!document.isOfflinePinned) "pinned for offline access" else "unpinned from priority offline cache"
            transientFlagsFlow.update {
                it.copy(statusBannerMessage = "\"${document.title}\" $stateLabel.")
            }
        }
    }

    fun deleteDocument(document: VaultDocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(document, transientFlagsFlow.value.currentUser)
            transientFlagsFlow.update {
                it.copy(
                    selectedDocumentIdForPreview = if (it.selectedDocumentIdForPreview == document.id) null else it.selectedDocumentIdForPreview,
                    selectedDocumentIdForEdit = if (it.selectedDocumentIdForEdit == document.id) null else it.selectedDocumentIdForEdit,
                    statusBannerMessage = "Deleted \"${document.title}\" from vault."
                )
            }
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            transientFlagsFlow.update { it.copy(isBusy = true, errorBannerMessage = null) }
            val webClientId = uiState.value.firebaseConfig.webClientId
            val result = repository.signInWithGoogle(activityContext, webClientId)
            result.onSuccess { user ->
                transientFlagsFlow.update {
                    it.copy(
                        isBusy = false,
                        currentUser = user,
                        isFirebaseInitialized = true,
                        statusBannerMessage = "Signed in as ${user.email}. Syncing cloud vault…"
                    )
                }
                syncAllPendingDocuments()
                restoreFromCloud()
            }.onFailure { err ->
                transientFlagsFlow.update {
                    it.copy(
                        isBusy = false,
                        errorBannerMessage = err.localizedMessage ?: "Google Sign-In failed"
                    )
                }
            }
        }
    }

    fun signInWithGmailPassword(email: String, password: String, isRegistration: Boolean) {
        viewModelScope.launch {
            if (email.isBlank() || password.length < 6) {
                transientFlagsFlow.update {
                    it.copy(errorBannerMessage = "Enter a valid Gmail address and a password of at least 6 characters.")
                }
                return@launch
            }
            transientFlagsFlow.update { it.copy(isBusy = true, errorBannerMessage = null) }
            val result = repository.signInWithGmailPassword(email, password, isRegistration)
            result.onSuccess { user ->
                transientFlagsFlow.update {
                    it.copy(
                        isBusy = false,
                        currentUser = user,
                        isFirebaseInitialized = true,
                        statusBannerMessage = "Authenticated as ${user.email}. Cloud backup active."
                    )
                }
                syncAllPendingDocuments()
                restoreFromCloud()
            }.onFailure { err ->
                transientFlagsFlow.update {
                    it.copy(
                        isBusy = false,
                        errorBannerMessage = err.localizedMessage ?: "Authentication failed"
                    )
                }
            }
        }
    }

    fun signOut() {
        repository.signOut()
        transientFlagsFlow.update {
            it.copy(
                currentUser = null,
                statusBannerMessage = "Signed out of Google Cloud. Offline cached documents remain accessible locally."
            )
        }
    }

    fun saveFirebaseConfiguration(config: FirebaseRuntimeConfig) {
        viewModelScope.launch {
            val result = repository.saveFirebaseConfig(config)
            result.onSuccess { initialized ->
                transientFlagsFlow.update {
                    it.copy(
                        isFirebaseInitialized = initialized,
                        currentUser = repository.getCurrentUser(),
                        showFirebaseConfigDialog = false,
                        statusBannerMessage = if (initialized) {
                            "Firebase Cloud connected (${config.projectId}). You can now sign in with Google."
                        } else {
                            "Firebase settings saved."
                        }
                    )
                }
            }.onFailure { err ->
                transientFlagsFlow.update {
                    it.copy(
                        errorBannerMessage = "Invalid Firebase configuration: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun syncAllPendingDocuments() {
        val user = transientFlagsFlow.value.currentUser
        if (user == null) {
            transientFlagsFlow.update {
                it.copy(
                    currentTab = VaultTab.ACCOUNT,
                    errorBannerMessage = "Sign in with your Gmail account first to back up documents to Firebase Cloud."
                )
            }
            return
        }
        viewModelScope.launch {
            transientFlagsFlow.update { it.copy(isSyncing = true, errorBannerMessage = null) }
            val count = repository.syncPendingDocuments(user)
            transientFlagsFlow.update {
                it.copy(
                    isSyncing = false,
                    statusBannerMessage = if (count > 0) {
                        "Synced $count document(s) to Firebase Firestore & Cloud Storage."
                    } else {
                        "All documents are up to date with your Google Cloud account."
                    }
                )
            }
        }
    }

    fun restoreFromCloud() {
        val user = transientFlagsFlow.value.currentUser
        if (user == null) {
            transientFlagsFlow.update {
                it.copy(
                    currentTab = VaultTab.ACCOUNT,
                    errorBannerMessage = "Sign in with your Google account to restore cloud backups."
                )
            }
            return
        }
        viewModelScope.launch {
            transientFlagsFlow.update { it.copy(isSyncing = true, errorBannerMessage = null) }
            val res = repository.pullCloudBackup(user)
            res.onSuccess { count ->
                transientFlagsFlow.update {
                    it.copy(
                        isSyncing = false,
                        statusBannerMessage = "Restored & verified $count document(s) from your Google Cloud backup."
                    )
                }
            }.onFailure { err ->
                transientFlagsFlow.update {
                    it.copy(
                        isSyncing = false,
                        errorBannerMessage = "Cloud restore failed: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun verifyOfflineFiles() {
        viewModelScope.launch {
            transientFlagsFlow.update { it.copy(isBusy = true) }
            val (verified, total) = repository.verifyOfflineCacheIntegrity()
            transientFlagsFlow.update {
                it.copy(
                    isBusy = false,
                    statusBannerMessage = if (total == 0) {
                        "Offline cache is ready. Scan or import documents to cache them locally."
                    } else {
                        "SHA-256 Integrity Verified: $verified of $total cached file(s) intact for offline viewing."
                    }
                )
            }
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSyncEnabled(enabled)
        }
    }

    fun setOfflineCacheDefault(enabled: Boolean) {
        viewModelScope.launch {
            repository.setOfflineCacheDefault(enabled)
        }
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = VaultDatabase.getInstance(appContext)
            val firebaseService = FirebaseVaultService(appContext)
            val preferences = VaultPreferences(appContext)
            val repository = VaultRepository(
                appContext = appContext,
                documentDao = database.documentDao(),
                firebaseService = firebaseService,
                preferences = preferences
            )
            return VaultViewModel(repository) as T
        }
    }
}
