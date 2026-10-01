package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import com.example.data.security.BiometricAuthHelper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.UserAuthProfile
import com.example.data.model.VaultDocumentEntity
import com.example.data.scanner.DocumentFileProcessor
import com.example.ui.components.DocumentVaultCard
import com.example.ui.theme.VaultEmeraldAccent
import com.example.ui.theme.VaultEmeraldLight
import com.example.ui.theme.VaultNavyDark
import com.example.ui.theme.VaultTealLight

@Composable
fun OfflineVaultScreen(
    offlineDocuments: List<VaultDocumentEntity>,
    totalDocumentsCount: Int,
    offlineStorageBytes: Long,
    offlineCacheDefault: Boolean,
    isBusy: Boolean,
    onToggleOfflineDefault: (Boolean) -> Unit,
    onVerifyIntegrityClick: () -> Unit,
    onOpenDocumentPreview: (VaultDocumentEntity) -> Unit,
    onToggleFavorite: (VaultDocumentEntity) -> Unit,
    onToggleOfflinePin: (VaultDocumentEntity) -> Unit,
    onOpenScanner: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackToDashboard() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("offline_vault_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = VaultNavyDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VaultEmeraldAccent.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflinePin,
                                contentDescription = null,
                                tint = VaultEmeraldLight,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Zero-Internet Offline Vault",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Essential documents cached locally with SHA-256 verification",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${offlineDocuments.size} / $totalDocumentsCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pinned Offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = DocumentFileProcessor.formatBytes(offlineStorageBytes),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Local Cache Used",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onVerifyIntegrityClick,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultEmeraldAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("verify_offline_integrity_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verify Offline Cache Checksums (SHA-256)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Cache New Scans Offline",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Automatically keep local copies of newly scanned or imported files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = offlineCacheDefault,
                        onCheckedChange = onToggleOfflineDefault,
                        modifier = Modifier.testTag("offline_default_switch")
                    )
                }
            }
        }

        item {
            Text(
                text = "Cached Documents Ready Offline (${offlineDocuments.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (offlineDocuments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Offline Documents Cached Yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan a document or import a PDF—pinned documents are stored in internal vault storage so you can view them even in airplane mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onOpenScanner,
                            modifier = Modifier.testTag("offline_empty_scan_button")
                        ) {
                            Text("Scan or Upload Document")
                        }
                    }
                }
            }
        } else {
            items(offlineDocuments, key = { it.id }) { doc ->
                DocumentVaultCard(
                    document = doc,
                    onPreviewClick = { onOpenDocumentPreview(doc) },
                    onToggleFavorite = { onToggleFavorite(doc) },
                    onToggleOfflinePin = { onToggleOfflinePin(doc) }
                )
            }
        }
    }
}

@Composable
fun AccountCloudScreen(
    currentUser: UserAuthProfile?,
    isFirebaseInitialized: Boolean,
    syncedCount: Int,
    pendingSyncCount: Int,
    autoSyncEnabled: Boolean,
    biometricLockEnabled: Boolean = true,
    autoLockOnBackground: Boolean = true,
    hasConfiguredPin: Boolean = false,
    isBusy: Boolean,
    isSyncing: Boolean,
    onSignInWithGoogle: (android.content.Context) -> Unit,
    onSignInWithGmailPassword: (email: String, password: String, isRegistration: Boolean) -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onOpenFirebaseConfigDialog: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    onToggleAutoLockBackground: (Boolean) -> Unit = {},
    onLockVaultNow: () -> Unit = {},
    onUpdateBackupPin: (String) -> Unit = {},
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var gmailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var biometricTestFeedback by remember { mutableStateOf<String?>(null) }

    BackHandler { onBackToDashboard() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("account_cloud_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = VaultNavyDark)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VaultTealLight.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (currentUser != null) {
                                    Icons.Default.CloudDone
                                } else {
                                    Icons.Default.AccountCircle
                                },
                                contentDescription = null,
                                tint = VaultTealLight,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (currentUser != null) {
                                    currentUser.displayName
                                } else {
                                    "Google Cloud Authentication"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = currentUser?.email
                                    ?: "Tie your document backups to your personal Gmail ID",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    IconButtonIfNeeded(onOpenFirebaseConfigDialog = onOpenFirebaseConfigDialog)
                }

                if (currentUser != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Firebase UID: ${currentUser.uid.take(14)}…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultEmeraldLight
                                )
                                Text(
                                    text = "$syncedCount Synced · $pendingSyncCount Pending Upload",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = VaultEmeraldAccent.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "CONNECTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultEmeraldLight,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSyncNow,
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VaultTealLight,
                                contentColor = VaultNavyDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("cloud_sync_now_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = VaultNavyDark
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sync Now",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onRestoreFromCloud,
                            enabled = !isSyncing,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("cloud_restore_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Vault")
                        }
                    }
                }
            }
        }

        // Firebase Connection Status Notice if google-services.json isn't present yet
        if (!isFirebaseInitialized) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Firebase Cloud Configuration Needed",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Text(
                        text = "To connect live Firebase Auth, Firestore, and Cloud Storage, place your google-services.json in app/ or tap below to enter your Firebase Project ID, API Key, and Google OAuth Web Client ID.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Button(
                        onClick = onOpenFirebaseConfigDialog,
                        modifier = Modifier.testTag("configure_firebase_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Configure Firebase Credentials")
                    }
                }
            }
        }

        // Authentication Methods Card (When signed out)
        if (currentUser == null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Sign in with Google Account",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Authenticate with your Gmail ID so your scanned documents and metadata are backed up to Firebase Firestore & Cloud Storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { onSignInWithGoogle(context) },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google (Credential Manager)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Or use Gmail & Password (Firebase Auth)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        FilterChip(
                            selected = isRegisterMode,
                            onClick = { isRegisterMode = !isRegisterMode },
                            label = { Text(if (isRegisterMode) "New Account" else "Existing User") },
                            modifier = Modifier.testTag("toggle_register_mode_chip")
                        )
                    }

                    OutlinedTextField(
                        value = gmailInput,
                        onValueChange = { gmailInput = it },
                        label = { Text("Gmail Address") },
                        placeholder = { Text("your.name@gmail.com") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gmail_email_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Vault Cloud Password") },
                        placeholder = { Text("Minimum 6 characters") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gmail_password_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            onSignInWithGmailPassword(gmailInput, passwordInput, isRegisterMode)
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("gmail_password_submit_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (isRegisterMode) {
                                "Create Gmail Cloud Vault Account"
                            } else {
                                "Sign In with Gmail & Sync"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            // Signed-In Cloud Settings & Sign Out Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Real-Time Cloud Backup",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Automatically upload new scans to Firestore & Firebase Storage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = onToggleAutoSync,
                            modifier = Modifier.testTag("auto_sync_switch")
                        )
                    }

                    HorizontalDivider()

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("sign_out_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out of Google Cloud")
                    }
                }
            }
        }

        // Biometric Authentication & Vault Lock Settings Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("biometric_settings_card"),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Biometric & App Lock Protection",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Protect dashboard with Fingerprint, Face Unlock & 4-digit PIN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Require Biometric / PIN on Open",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Keep documents locked even if the phone is unlocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = biometricLockEnabled,
                        onCheckedChange = onToggleBiometricLock,
                        modifier = Modifier.testTag("biometric_lock_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Lock When App Minimized",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Immediately re-lock vault when switching apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoLockOnBackground,
                        onCheckedChange = onToggleAutoLockBackground,
                        enabled = biometricLockEnabled,
                        modifier = Modifier.testTag("auto_lock_background_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            BiometricAuthHelper.showBiometricPrompt(
                                context = context,
                                title = "Verify Biometric Sensor",
                                subtitle = "Touch fingerprint sensor or use face unlock",
                                onSuccess = {
                                    biometricTestFeedback = "Biometric sensor verified successfully!"
                                },
                                onUsePinFallback = {
                                    biometricTestFeedback = "Switched to backup PIN verification."
                                },
                                onErrorMessage = { msg ->
                                    biometricTestFeedback = msg
                                }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("test_biometric_sensor_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Sensor")
                    }

                    OutlinedButton(
                        onClick = onLockVaultNow,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("account_lock_vault_now_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock Vault Now")
                    }
                }

                if (!biometricTestFeedback.isNullOrBlank()) {
                    Text(
                        text = biometricTestFeedback!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                newPinInput = input
                            }
                        },
                        label = {
                            Text(
                                if (hasConfiguredPin) "Change 4-Digit PIN" else "Set 4-Digit Backup PIN"
                            )
                        },
                        placeholder = { Text("4 digits") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backup_pin_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            onUpdateBackupPin(newPinInput)
                            newPinInput = ""
                        },
                        enabled = newPinInput.length == 4,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("save_backup_pin_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save PIN")
                    }
                }
            }
        }

        // Security Architecture Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = VaultEmeraldAccent
                    )
                    Text(
                        text = "How Your Documents Are Protected",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                SecurityBulletRow(
                    title = "Account-Isolated Cloud Storage",
                    subtitle = "Files are stored under users/{uid}/documents/ in Firebase Firestore & Storage tied exclusively to your Gmail ID."
                )
                SecurityBulletRow(
                    title = "SHA-256 Document Checksums",
                    subtitle = "Every scanned image and imported PDF is hashed with SHA-256 on save to guarantee tamper-free offline integrity."
                )
                SecurityBulletRow(
                    title = "Offline-First Local Persistence",
                    subtitle = "Room SQLite & internal file caching ensure your essential IDs, medical records, and PDFs open instantly without internet."
                )
            }
        }
    }
}

@Composable
private fun IconButtonIfNeeded(onOpenFirebaseConfigDialog: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f),
        onClick = onOpenFirebaseConfigDialog,
        modifier = Modifier.testTag("open_firebase_settings_icon")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configure Firebase Cloud",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Cloud Config",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SecurityBulletRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = VaultEmeraldAccent,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
