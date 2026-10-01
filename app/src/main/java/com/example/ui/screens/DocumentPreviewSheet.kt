package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentFileType
import com.example.data.model.FirebaseRuntimeConfig
import com.example.data.model.SyncStatus
import com.example.data.model.VaultDocumentEntity
import com.example.data.scanner.DocumentFileProcessor
import com.example.ui.components.iconVector
import com.example.ui.theme.VaultEmeraldAccent
import com.example.ui.theme.VaultNavyDark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentPreviewDialog(
    document: VaultDocumentEntity,
    onDismiss: () -> Unit,
    onEditClick: (VaultDocumentEntity) -> Unit,
    onToggleOfflinePin: (VaultDocumentEntity) -> Unit,
    onDeleteDocument: (VaultDocumentEntity) -> Unit
) {
    val context = LocalContext.current
    val category = document.categoryEnum
    val fileType = document.fileTypeEnum
    val syncStatus = document.syncStatusEnum
    val localFile = remember(document.localFilePath) { File(document.localFilePath) }
    val existsLocally = remember(document.localFilePath) { localFile.exists() }

    var currentPageIndex by remember(document.id) { mutableIntStateOf(0) }
    var renderedPdfBitmap by remember(document.id, currentPageIndex) { mutableStateOf<Bitmap?>(null) }
    var isRenderingPdf by remember(document.id, currentPageIndex) { mutableStateOf(false) }

    var zoomScale by remember(document.id, currentPageIndex) { mutableFloatStateOf(1f) }
    var offsetX by remember(document.id, currentPageIndex) { mutableFloatStateOf(0f) }
    var offsetY by remember(document.id, currentPageIndex) { mutableFloatStateOf(0f) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    LaunchedEffect(document.id, currentPageIndex, fileType) {
        if (fileType == DocumentFileType.PDF_DOCUMENT && localFile.exists()) {
            isRenderingPdf = true
            renderedPdfBitmap = DocumentFileProcessor.renderPdfPageToBitmap(
                pdfFile = localFile,
                pageIndex = currentPageIndex,
                maxDimensionPx = 1600
            )
            isRenderingPdf = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("document_preview_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_preview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close preview"
                            )
                        }
                        Column {
                            Text(
                                text = document.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${category.displayName} · ${fileType.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { onEditClick(document) },
                            modifier = Modifier.testTag("edit_document_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit metadata"
                            )
                        }
                        IconButton(
                            onClick = {
                                if (localFile.exists()) {
                                    try {
                                        val shareUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            localFile
                                        )
                                        val mime = if (fileType == DocumentFileType.PDF_DOCUMENT) {
                                            "application/pdf"
                                        } else {
                                            "image/jpeg"
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = mime
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            putExtra(Intent.EXTRA_SUBJECT, document.title)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, "Share ${document.title}")
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                            },
                            modifier = Modifier.testTag("share_document_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share or export document"
                            )
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_document_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete document",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Zoomable Document Viewer Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(370.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultNavyDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                                    zoomScale = newScale
                                    if (newScale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            fileType == DocumentFileType.PDF_DOCUMENT && isRenderingPdf -> {
                                CircularProgressIndicator(color = Color.White)
                            }
                            fileType == DocumentFileType.PDF_DOCUMENT && renderedPdfBitmap != null -> {
                                Image(
                                    bitmap = renderedPdfBitmap!!.asImageBitmap(),
                                    contentDescription = "PDF Page ${currentPageIndex + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .graphicsLayer(
                                            scaleX = zoomScale,
                                            scaleY = zoomScale,
                                            translationX = offsetX,
                                            translationY = offsetY
                                        )
                                )
                            }
                            existsLocally -> {
                                AsyncImage(
                                    model = localFile,
                                    contentDescription = document.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .graphicsLayer(
                                            scaleX = zoomScale,
                                            scaleY = zoomScale,
                                            translationX = offsetX,
                                            translationY = offsetY
                                        )
                                )
                            }
                            else -> {
                                Text(
                                    text = "Document binary not found in local cache.",
                                    color = Color.White
                                )
                            }
                        }

                        // Zoom Reset / Hint Pill
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (zoomScale > 1.05f) {
                                        "${String.format(Locale.US, "%.1fx", zoomScale)}"
                                    } else {
                                        "Pinch to zoom"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }

                        // Multi-Page PDF Paginator Controls
                        if (fileType == DocumentFileType.PDF_DOCUMENT && document.pageCount > 1) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp),
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.72f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (currentPageIndex > 0) currentPageIndex--
                                        },
                                        enabled = currentPageIndex > 0,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous PDF Page",
                                            tint = Color.White
                                        )
                                    }
                                    Text(
                                        text = "Page ${currentPageIndex + 1} of ${document.pageCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = {
                                            if (currentPageIndex < document.pageCount - 1) currentPageIndex++
                                        },
                                        enabled = currentPageIndex < document.pageCount - 1,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next PDF Page",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Offline & Cloud Status Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (document.isOfflinePinned && existsLocally) {
                            VaultEmeraldAccent.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                tint = if (document.isOfflinePinned && existsLocally) {
                                    VaultEmeraldAccent
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Column {
                                Text(
                                    text = if (document.isOfflinePinned && existsLocally) {
                                        "Offline Ready"
                                    } else {
                                        "Standard Cache"
                                    },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = DocumentFileProcessor.formatBytes(document.fileSizeBytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (syncStatus == SyncStatus.SYNCED) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (syncStatus == SyncStatus.SYNCED) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.CloudQueue
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = if (syncStatus == SyncStatus.SYNCED) {
                                        "Cloud Backed Up"
                                    } else {
                                        "Pending Sync"
                                    },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = document.ownerEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Document Details & Cryptographic Integrity Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Document Metadata & Security",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (document.keywordList.isNotEmpty()) {
                            Text(
                                text = "Smart Search Keywords",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                document.keywordList.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (document.notes.isNotBlank()) {
                            Text(
                                text = "Notes / Document Details",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = document.notes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = VaultEmeraldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "SHA-256 Checksum:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = document.sha256Checksum.ifBlank { "Verified Local Binary" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val formattedDate = remember(document.createdAt) {
                            SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US).format(Date(document.createdAt))
                        }
                        Text(
                            text = "Added on $formattedDate · ${document.pageCount} page(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { onToggleOfflinePin(document) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preview_toggle_offline_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (document.isOfflinePinned) {
                            "Pinned for Offline Access (Tap to Unpin)"
                        } else {
                            "Pin Document for Guaranteed Offline Access"
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Document?") },
            text = {
                Text(
                    "This will permanently remove \"${document.title}\" from your offline device cache and your linked Google Cloud backup."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteDocument(document)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_document_button")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditDocumentMetadataDialog(
    document: VaultDocumentEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, category: DocumentCategory, keywords: String, notes: String) -> Unit
) {
    var title by remember(document.id) { mutableStateOf(document.title) }
    var category by remember(document.id) { mutableStateOf(document.categoryEnum) }
    var keywords by remember(document.id) { mutableStateOf(document.keywords) }
    var notes by remember(document.id) { mutableStateOf(document.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Document Details") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_doc_title_input")
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DocumentCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.shortName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keywords (comma-separated)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_doc_keywords_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / ID Number") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_doc_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, category, keywords, notes) },
                modifier = Modifier.testTag("save_edit_doc_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FirebaseCloudConfigDialog(
    initialConfig: FirebaseRuntimeConfig,
    onDismiss: () -> Unit,
    onSaveConfig: (FirebaseRuntimeConfig) -> Unit
) {
    var projectId by remember(initialConfig) { mutableStateOf(initialConfig.projectId) }
    var appId by remember(initialConfig) { mutableStateOf(initialConfig.applicationId) }
    var apiKey by remember(initialConfig) { mutableStateOf(initialConfig.apiKey) }
    var storageBucket by remember(initialConfig) { mutableStateOf(initialConfig.storageBucket) }
    var webClientId by remember(initialConfig) { mutableStateOf(initialConfig.webClientId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Firebase Cloud & OAuth Setup") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "You can either place your google-services.json file inside the app/ directory or enter your Firebase project credentials below to initialize Firebase Auth, Firestore, and Cloud Storage dynamically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = projectId,
                    onValueChange = { projectId = it },
                    label = { Text("Firebase Project ID") },
                    placeholder = { Text("my-docuvault-project") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_project_id_input")
                )

                OutlinedTextField(
                    value = appId,
                    onValueChange = { appId = it },
                    label = { Text("Firebase App ID (mobilesdk_app_id)") },
                    placeholder = { Text("1:123456789:android:abcdef") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_app_id_input")
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Firebase Web API Key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_api_key_input")
                )

                OutlinedTextField(
                    value = storageBucket,
                    onValueChange = { storageBucket = it },
                    label = { Text("Storage Bucket (Optional)") },
                    placeholder = { Text("my-docuvault-project.appspot.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_bucket_input")
                )

                OutlinedTextField(
                    value = webClientId,
                    onValueChange = { webClientId = it },
                    label = { Text("Google OAuth Web Client ID") },
                    placeholder = { Text("123456-abc.apps.googleusercontent.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_web_client_id_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveConfig(
                        FirebaseRuntimeConfig(
                            projectId = projectId,
                            applicationId = appId,
                            apiKey = apiKey,
                            storageBucket = storageBucket,
                            webClientId = webClientId
                        )
                    )
                },
                modifier = Modifier.testTag("save_firebase_config_button")
            ) {
                Text("Initialize & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
