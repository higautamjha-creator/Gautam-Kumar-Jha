package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core. FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.scanner.DocumentFileProcessor
import com.example.ui.components.CategoryStripSection
import com.example.ui.components.DocumentVaultCard
import com.example.ui.components.EmptyVaultStateCard
import com.example.ui.components.SmartSearchHeader
import com.example.ui.components.VaultHeroSecurityCard
import com.example.ui.theme.VaultEmeraldAccent
import com.example.ui.theme.VaultEmeraldLight
import com.example.ui.theme.VaultNavyDark
import com.example.ui.theme.VaultTealLight
import com.example.ui.viewmodel.VaultTab
import com.example.ui.viewmodel.VaultUiState
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun VaultTab.icon(selected: Boolean): ImageVector = when (this) {
    VaultTab.DASHBOARD -> if (selected) Icons.Filled.FolderSpecial else Icons.Outlined.FolderSpecial
    VaultTab.SCANNER -> if (selected) Icons.Filled.CameraAlt else Icons.Outlined.CameraAlt
    VaultTab.OFFLINE -> if (selected) Icons.Filled.OfflinePin else Icons.Outlined.OfflinePin
    VaultTab.ACCOUNT -> if (selected) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle
}

@Composable
fun DocuVaultApp(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showWelcomeSplash by rememberSaveable { mutableStateOf(true) }

    if (showWelcomeSplash) {
        WelcomeSplashScreen(
            onContinueToLogin = {
                showWelcomeSplash = false
                viewModel.selectTab(VaultTab.ACCOUNT)
            },
            onContinueToDashboard = {
                showWelcomeSplash = false
                viewModel.selectTab(VaultTab.DASHBOARD)
            },
            modifier = modifier
        )
        return
    }

    if (uiState.isVaultLocked) {
        BiometricLockScreen(
            hasConfiguredPin = uiState.hasConfiguredPin,
            lockScreenMessage = uiState.lockScreenMessage,
            onBiometricSuccess = viewModel::onBiometricUnlockSuccess,
            onBiometricError = viewModel::onBiometricError,
            onClearMessage = viewModel::clearLockScreenMessage,
            onSubmitPin = viewModel::submitPinToUnlockOrSetup,
            modifier = modifier
        )
        return
    }

    val quickFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
            if (mime.contains("image")) {
                scope.launch {
                    val bmp = DocumentFileProcessor.decodeBitmapFromUri(context, uri)
                    if (bmp != null) {
                        viewModel.stageCapturedBitmap(bmp)
                    } else {
                        viewModel.stageImportedUri(uri)
                    }
                }
            } else {
                viewModel.stageImportedUri(uri)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            floatingActionButton = {
                if (uiState.currentTab == VaultTab.DASHBOARD) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.selectTab(VaultTab.SCANNER) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null
                            )
                        },
                        text = {
                            Text(
                                text = "Scan / Upload",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_scan_document")
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    VaultBottomNavigationBar(
                        currentTab = uiState.currentTab,
                        offlineCount = uiState.offlineCachedDocuments.size,
                        isCloudSignedIn = uiState.currentUser != null,
                        onSelectTab = viewModel::selectTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    VaultNavigationRail(
                        currentTab = uiState.currentTab,
                        offlineCount = uiState.offlineCachedDocuments.size,
                        isCloudSignedIn = uiState.currentUser != null,
                        onSelectTab = viewModel::selectTab
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 860.dp)
                    ) {
                        // Status / Error Banner
                        FeedbackBanners(
                            statusMessage = uiState.statusBannerMessage,
                            errorMessage = uiState.errorBannerMessage,
                            onDismiss = viewModel::dismissBanners
                        )

                        when (uiState.currentTab) {
                            VaultTab.DASHBOARD -> {
                                DashboardContent(
                                    uiState = uiState,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onSelectCategory = viewModel::selectCategory,
                                    onToggleFavorites = viewModel::toggleFavoritesFilter,
                                    onTogglePdfs = viewModel::togglePdfOnlyFilter,
                                    onSelectSortOption = viewModel::updateSortOption,
                                    onResetFilters = viewModel::clearAllFilters,
                                    onOpenScanner = { viewModel.selectTab(VaultTab.SCANNER) },
                                    onUploadFileClick = {
                                        quickFilePickerLauncher.launch(
                                            arrayOf("application/pdf", "image/*")
                                        )
                                    },
                                    onAccountOrSyncClick = {
                                        if (uiState.currentUser != null && uiState.pendingSyncCount > 0) {
                                            viewModel.syncAllPendingDocuments()
                                        } else {
                                            viewModel.selectTab(VaultTab.ACCOUNT)
                                        }
                                    },
                                    onOpenDocumentPreview = viewModel::openDocumentPreview,
                                    onToggleDocumentFavorite = viewModel::toggleFavorite,
                                    onToggleDocumentOfflinePin = viewModel::toggleOfflinePin,
                                    onLockVaultClick = viewModel::lockVaultNow
                                )
                            }

                            VaultTab.SCANNER -> {
                                ScannerScreen(
                                    pendingBitmap = uiState.pendingScanBitmap,
                                    pendingUri = uiState.pendingImportUri,
                                    defaultOfflinePinned = uiState.offlineCacheDefault,
                                    isBusy = uiState.isBusy,
                                    onStageBitmap = viewModel::stageCapturedBitmap,
                                    onStageUri = viewModel::stageImportedUri,
                                    onClearStaged = viewModel::clearStagedDocument,
                                    onSaveScannedBitmap = viewModel::saveScannedDocument,
                                    onSaveImportedUri = viewModel::saveImportedUriDocument,
                                    onBackToDashboard = { viewModel.selectTab(VaultTab.DASHBOARD) }
                                )
                            }

                            VaultTab.OFFLINE -> {
                                OfflineVaultScreen(
                                    offlineDocuments = uiState.offlineCachedDocuments,
                                    totalDocumentsCount = uiState.allDocuments.size,
                                    offlineStorageBytes = uiState.offlineStorageBytes,
                                    offlineCacheDefault = uiState.offlineCacheDefault,
                                    isBusy = uiState.isBusy,
                                    onToggleOfflineDefault = viewModel::setOfflineCacheDefault,
                                    onVerifyIntegrityClick = viewModel::verifyOfflineFiles,
                                    onOpenDocumentPreview = viewModel::openDocumentPreview,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onToggleOfflinePin = viewModel::toggleOfflinePin,
                                    onOpenScanner = { viewModel.selectTab(VaultTab.SCANNER) },
                                    onBackToDashboard = { viewModel.selectTab(VaultTab.DASHBOARD) }
                                )
                            }

                            VaultTab.ACCOUNT -> {
                                AccountCloudScreen(
                                    currentUser = uiState.currentUser,
                                    isFirebaseInitialized = uiState.isFirebaseInitialized,
                                    syncedCount = uiState.syncedDocumentsCount,
                                    pendingSyncCount = uiState.pendingSyncCount,
                                    autoSyncEnabled = uiState.autoSyncEnabled,
                                    biometricLockEnabled = uiState.biometricLockEnabled,
                                    autoLockOnBackground = uiState.autoLockOnBackground,
                                    hasConfiguredPin = uiState.hasConfiguredPin,
                                    isBusy = uiState.isBusy,
                                    isSyncing = uiState.isSyncing,
                                    onSignInWithGoogle = viewModel::signInWithGoogle,
                                    onSignInWithGmailPassword = viewModel::signInWithGmailPassword,
                                    onSignOut = viewModel::signOut,
                                    onSyncNow = viewModel::syncAllPendingDocuments,
                                    onRestoreFromCloud = viewModel::restoreFromCloud,
                                    onOpenFirebaseConfigDialog = {
                                        viewModel.setShowFirebaseConfigDialog(true)
                                    },
                                    onToggleAutoSync = viewModel::setAutoSyncEnabled,
                                    onToggleBiometricLock = viewModel::setBiometricLockEnabled,
                                    onToggleAutoLockBackground = viewModel::setAutoLockOnBackground,
                                    onLockVaultNow = viewModel::lockVaultNow,
                                    onUpdateBackupPin = viewModel::updateBackupVaultPin,
                                    onBackToDashboard = { viewModel.selectTab(VaultTab.DASHBOARD) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Document Full-Screen Preview Modal
    uiState.selectedDocumentForPreview?.let { previewDoc ->
        DocumentPreviewDialog(
            document = previewDoc,
            onDismiss = viewModel::closeDocumentPreview,
            onEditClick = { doc ->
                viewModel.closeDocumentPreview()
                viewModel.openDocumentEdit(doc)
            },
            onToggleOfflinePin = viewModel::toggleOfflinePin,
            onDeleteDocument = viewModel::deleteDocument
        )
    }

    // Edit Document Metadata Dialog
    uiState.selectedDocumentForEdit?.let { editDoc ->
        EditDocumentMetadataDialog(
            document = editDoc,
            onDismiss = viewModel::closeDocumentEdit,
            onSave = { title, category, keywords, notes ->
                viewModel.updateDocumentMetadata(editDoc, title, category, keywords, notes)
            }
        )
    }

    // Runtime Firebase Cloud Configuration Dialog
    if (uiState.showFirebaseConfigDialog) {
        FirebaseCloudConfigDialog(
            initialConfig = uiState.firebaseConfig,
            onDismiss = { viewModel.setShowFirebaseConfigDialog(false) },
            onSaveConfig = viewModel::saveFirebaseConfiguration
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: VaultUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (com.example.data.model.DocumentCategory?) -> Unit,
    onToggleFavorites: () -> Unit,
    onTogglePdfs: () -> Unit,
    onSelectSortOption: (com.example.data.model.SortOption) -> Unit,
    onResetFilters: () -> Unit,
    onOpenScanner: () -> Unit,
    onUploadFileClick: () -> Unit,
    onAccountOrSyncClick: () -> Unit,
    onOpenDocumentPreview: (com.example.data.model.VaultDocumentEntity) -> Unit,
    onToggleDocumentFavorite: (com.example.data.model.VaultDocumentEntity) -> Unit,
    onToggleDocumentOfflinePin: (com.example.data.model.VaultDocumentEntity) -> Unit,
    onLockVaultClick: () -> Unit = {}
) {
    val isFilterActive = uiState.searchQuery.isNotBlank() ||
        uiState.selectedCategory != null ||
        uiState.onlyFavorites ||
        uiState.onlyPdfs

    Column(modifier = Modifier.fillMaxSize()) {
        // Persistent Smart Search Bar at the very top
        SmartSearchHeader(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            selectedCategory = uiState.selectedCategory,
            onSelectCategory = onSelectCategory,
            onlyFavorites = uiState.onlyFavorites,
            onToggleFavorites = onToggleFavorites,
            onlyPdfs = uiState.onlyPdfs,
            onTogglePdfs = onTogglePdfs,
            sortOption = uiState.sortOption,
            onSelectSortOption = onSelectSortOption,
            totalResultCount = uiState.filteredDocuments.size
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_lazy_column"),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.searchQuery.isBlank()) {
                item {
                    VaultHeroSecurityCard(
                        currentUser = uiState.currentUser,
                        totalDocuments = uiState.allDocuments.size,
                        offlineCachedCount = uiState.offlineCachedDocuments.size,
                        totalStorageBytes = uiState.totalStorageBytes,
                        pendingSyncCount = uiState.pendingSyncCount,
                        isSyncing = uiState.isSyncing,
                        onOpenScanner = onOpenScanner,
                        onUploadFileClick = onUploadFileClick,
                        onAccountOrSyncClick = onAccountOrSyncClick,
                        onLockVaultClick = onLockVaultClick,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    CategoryStripSection(
                        summaries = uiState.categorySummaries,
                        selectedCategory = uiState.selectedCategory,
                        onSelectCategory = { cat -> onSelectCategory(cat) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            uiState.searchQuery.isNotBlank() ->
                                "Search Results (${uiState.filteredDocuments.size})"
                            uiState.selectedCategory != null ->
                                "${uiState.selectedCategory.displayName} (${uiState.filteredDocuments.size})"
                            else ->
                                "Stored Documents (${uiState.filteredDocuments.size})"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = uiState.sortOption.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.filteredDocuments.isEmpty()) {
                item {
                    EmptyVaultStateCard(
                        isFiltered = isFilterActive,
                        onScanClick = onOpenScanner,
                        onUploadClick = onUploadFileClick,
                        onResetFilters = onResetFilters
                    )
                }
            } else {
                items(
                    items = uiState.filteredDocuments,
                    key = { it.id }
                ) { document ->
                    DocumentVaultCard(
                        document = document,
                        onPreviewClick = { onOpenDocumentPreview(document) },
                        onToggleFavorite = { onToggleDocumentFavorite(document) },
                        onToggleOfflinePin = { onToggleDocumentOfflinePin(document) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackBanners(
    statusMessage: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = statusMessage != null || errorMessage != null) {
        val isError = errorMessage != null
        val text = errorMessage ?: statusMessage ?: ""
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("feedback_banner"),
            shape = RoundedCornerShape(14.dp),
            color = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                VaultEmeraldAccent.copy(alpha = 0.16f)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            VaultEmeraldAccent
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss notification",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultBottomNavigationBar(
    currentTab: VaultTab,
    offlineCount: Int,
    isCloudSignedIn: Boolean,
    onSelectTab: (VaultTab) -> Unit
) {
    NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
        VaultTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (tab == VaultTab.OFFLINE && offlineCount > 0) {
                                Badge { Text(offlineCount.toString()) }
                            } else if (tab == VaultTab.ACCOUNT && isCloudSignedIn) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Connected",
                                    tint = VaultEmeraldAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = tab.icon(selected),
                            contentDescription = tab.label
                        )
                    }
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("nav_tab_${tab.route}")
            )
        }
    }
}

@Composable
private fun VaultNavigationRail(
    currentTab: VaultTab,
    offlineCount: Int,
    isCloudSignedIn: Boolean,
    onSelectTab: (VaultTab) -> Unit
) {
    NavigationRail(modifier = Modifier.testTag("side_navigation_rail")) {
        VaultTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (tab == VaultTab.OFFLINE && offlineCount > 0) {
                                Badge { Text(offlineCount.toString()) }
                            } else if (tab == VaultTab.ACCOUNT && isCloudSignedIn) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Connected",
                                    tint = VaultEmeraldAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = tab.icon(selected),
                            contentDescription = tab.label
                        )
                    }
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("rail_tab_${tab.route}")
            )
        }
    }
}

@Composable
private fun WelcomeSplashScreen(
    onContinueToLogin: () -> Unit,
    onContinueToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var targetProgress by remember { mutableFloatStateOf(0.15f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
        label = "splash_progress"
    )

    LaunchedEffect(Unit) {
        targetProgress = 1f
        delay(4200L)
        onContinueToDashboard()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VaultNavyDark)
            .testTag("welcome_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_vault_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VaultNavyDark.copy(alpha = 0.85f),
                            VaultNavyDark.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, VaultTealLight.copy(alpha = 0.45f)),
                modifier = Modifier.size(96.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon),
                    contentDescription = "DocuVault Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = CircleShape,
                color = VaultEmeraldAccent.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, VaultEmeraldLight.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = VaultEmeraldLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.vault_tagline),
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultEmeraldLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Prominent Welcome & Developer Attribution Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.09f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.welcome_splash_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("welcome_splash_text")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Store, scan, search, and back up your important personal documents securely with Google Cloud & offline SHA-256 caching.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(6.dp)
                    .clip(CircleShape),
                color = VaultTealLight,
                trackColor = Color.White.copy(alpha = 0.16f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinueToDashboard,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultTealLight,
                    contentColor = VaultNavyDark
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("welcome_enter_dashboard_button")
            ) {
                Text(
                    text = "Open Document Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onContinueToLogin,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("welcome_google_login_button")
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign In with Google Account",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

