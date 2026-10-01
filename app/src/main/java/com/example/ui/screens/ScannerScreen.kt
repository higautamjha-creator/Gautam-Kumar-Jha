package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.DocumentCategory
import com.example.data.model.ScanFilterMode
import com.example.data.scanner.DocumentFileProcessor
import com.example.ui.components.iconVector
import com.example.ui.theme.VaultEmeraldLight
import com.example.ui.theme.VaultNavyDark
import com.example.ui.theme.VaultTealLight
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScannerScreen(
    pendingBitmap: Bitmap?,
    pendingUri: Uri?,
    defaultOfflinePinned: Boolean,
    isBusy: Boolean,
    onStageBitmap: (Bitmap) -> Unit,
    onStageUri: (Uri) -> Unit,
    onClearStaged: () -> Unit,
    onSaveScannedBitmap: (
        bitmap: Bitmap,
        title: String,
        category: DocumentCategory,
        keywords: String,
        notes: String,
        filterMode: ScanFilterMode,
        isOfflinePinned: Boolean
    ) -> Unit,
    onSaveImportedUri: (
        uri: Uri,
        title: String,
        category: DocumentCategory,
        keywords: String,
        notes: String,
        isOfflinePinned: Boolean
    ) -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler {
        if (pendingBitmap != null || pendingUri != null) {
            onClearStaged()
        } else {
            onBackToDashboard()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val decoded = DocumentFileProcessor.decodeBitmapFromUri(context, uri)
                if (decoded != null) {
                    onStageBitmap(decoded)
                } else {
                    onStageUri(uri)
                }
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
            if (mime.contains("image")) {
                scope.launch {
                    val decoded = DocumentFileProcessor.decodeBitmapFromUri(context, uri)
                    if (decoded != null) {
                        onStageBitmap(decoded)
                    } else {
                        onStageUri(uri)
                    }
                }
            } else {
                onStageUri(uri)
            }
        }
    }

    if (pendingBitmap != null || pendingUri != null) {
        StagedDocumentEditor(
            pendingBitmap = pendingBitmap,
            pendingUri = pendingUri,
            defaultOfflinePinned = defaultOfflinePinned,
            isBusy = isBusy,
            onDiscard = onClearStaged,
            onSaveScannedBitmap = onSaveScannedBitmap,
            onSaveImportedUri = onSaveImportedUri,
            modifier = modifier
        )
    } else {
        LiveCameraScannerView(
            hasCameraPermission = hasCameraPermission,
            onRequestCameraPermission = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onBitmapCaptured = onStageBitmap,
            onPickImageClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPickPdfOrFileClick = {
                documentPickerLauncher.launch(arrayOf("application/pdf", "image/*"))
            },
            onBackClick = onBackToDashboard,
            modifier = modifier
        )
    }
}

@Composable
private fun LiveCameraScannerView(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onBitmapCaptured: (Bitmap) -> Unit,
    onPickImageClick: () -> Unit,
    onPickPdfOrFileClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var flashEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraErrorText by remember { mutableStateOf<String?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("scanner_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Vault"
                    )
                }
                Column {
                    Text(
                        text = "Document Scanner & Upload",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Align paper inside frame or import PDF / Image",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (hasCameraPermission) {
                IconButton(
                    onClick = {
                        flashEnabled = !flashEnabled
                        imageCapture.flashMode = if (flashEnabled) {
                            ImageCapture.FLASH_MODE_ON
                        } else {
                            ImageCapture.FLASH_MODE_OFF
                        }
                    },
                    modifier = Modifier.testTag("scanner_flash_toggle")
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle camera flash",
                        tint = if (flashEnabled) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Camera Viewfinder Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("scanner_viewfinder_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = VaultNavyDark)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission && cameraErrorText == null) {
                    val previewView = remember {
                        PreviewView(context).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    }

                    DisposableEffect(lifecycleOwner) {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        val executor = ContextCompat.getMainExecutor(context)
                        val listener = Runnable {
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                cameraErrorText = "Camera hardware unavailable (${e.localizedMessage ?: "No back camera"})"
                            }
                        }
                        cameraProviderFuture.addListener(listener, executor)

                        onDispose {
                            try {
                                cameraProviderFuture.get().unbindAll()
                            } catch (_: Exception) {
                            }
                        }
                    }

                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Animated Document Scanner Overlay
                    ScannerReticleOverlay(modifier = Modifier.fillMaxSize())

                    // Bottom Capture Bar inside Viewfinder
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                if (!isCapturing) {
                                    isCapturing = true
                                    val tempFile = File(
                                        DocumentFileProcessor.getTempScanDirectory(context),
                                        "capture_${System.currentTimeMillis()}.jpg"
                                    )
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                                    imageCapture.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                scope.launch {
                                                    val bmp = DocumentFileProcessor.decodeBitmapFromUri(
                                                        context,
                                                        Uri.fromFile(tempFile)
                                                    )
                                                    isCapturing = false
                                                    if (bmp != null) {
                                                        onBitmapCaptured(bmp)
                                                    } else {
                                                        cameraErrorText = "Unable to decode captured scan."
                                                    }
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                isCapturing = false
                                                cameraErrorText = "Capture failed: ${exception.localizedMessage}"
                                            }
                                        }
                                    )
                                }
                            },
                            enabled = !isCapturing,
                            shape = CircleShape,
                            modifier = Modifier
                                .height(58.dp)
                                .testTag("capture_scan_button")
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Capture Document Page",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Camera Permission Request or Hardware Fallback UI
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VaultTealLight.copy(alpha = 0.16f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = VaultTealLight,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (!hasCameraPermission) {
                                "Camera Access Required for Live Scanning"
                            } else {
                                cameraErrorText ?: "Camera Preview Ready"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Grant camera permission to scan physical ID cards, tax forms, receipts, and contracts with automatic B&W document enhancement, or upload existing PDFs and photos below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (!hasCameraPermission) {
                            Button(
                                onClick = onRequestCameraPermission,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("grant_camera_permission_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enable Camera Scanner")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Direct File & Photo Upload Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onPickPdfOrFileClick)
                    .testTag("upload_pdf_card"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Import PDF or Document",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Upload PDF",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Multi-page PDFs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onPickImageClick)
                    .testTag("upload_image_card"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Import Photo from Gallery",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Upload Image",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Enhance & crop",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerReticleOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_y"
    )

    Canvas(modifier = modifier) {
        val marginX = size.width * 0.10f
        val marginY = size.height * 0.12f
        val left = marginX
        val right = size.width - marginX
        val top = marginY
        val bottom = size.height - marginY
        val cornerLen = 42.dp.toPx()
        val stroke = 4.dp.toPx()

        val bracketColor = VaultEmeraldLight

        // Top-Left Bracket
        drawLine(bracketColor, Offset(left, top), Offset(left + cornerLen, top), stroke, StrokeCap.Round)
        drawLine(bracketColor, Offset(left, top), Offset(left, top + cornerLen), stroke, StrokeCap.Round)

        // Top-Right Bracket
        drawLine(bracketColor, Offset(right, top), Offset(right - cornerLen, top), stroke, StrokeCap.Round)
        drawLine(bracketColor, Offset(right, top), Offset(right, top + cornerLen), stroke, StrokeCap.Round)

        // Bottom-Left Bracket
        drawLine(bracketColor, Offset(left, bottom), Offset(left + cornerLen, bottom), stroke, StrokeCap.Round)
        drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom - cornerLen), stroke, StrokeCap.Round)

        // Bottom-Right Bracket
        drawLine(bracketColor, Offset(right, bottom), Offset(right - cornerLen, bottom), stroke, StrokeCap.Round)
        drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - cornerLen), stroke, StrokeCap.Round)

        // Laser Scan Beam
        val laserY = top + (bottom - top) * scanProgress
        drawLine(
            color = VaultTealLight.copy(alpha = 0.85f),
            start = Offset(left + 12f, laserY),
            end = Offset(right - 12f, laserY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StagedDocumentEditor(
    pendingBitmap: Bitmap?,
    pendingUri: Uri?,
    defaultOfflinePinned: Boolean,
    isBusy: Boolean,
    onDiscard: () -> Unit,
    onSaveScannedBitmap: (
        bitmap: Bitmap,
        title: String,
        category: DocumentCategory,
        keywords: String,
        notes: String,
        filterMode: ScanFilterMode,
        isOfflinePinned: Boolean
    ) -> Unit,
    onSaveImportedUri: (
        uri: Uri,
        title: String,
        category: DocumentCategory,
        keywords: String,
        notes: String,
        isOfflinePinned: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DocumentCategory.IDENTITY) }
    var keywords by remember { mutableStateOf("important, verified") }
    var notes by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ScanFilterMode.BW_DOCUMENT) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var isOfflinePinned by remember { mutableStateOf(defaultOfflinePinned) }
    var previewBitmap by remember(pendingBitmap) { mutableStateOf(pendingBitmap) }

    LaunchedEffect(pendingBitmap, selectedFilter, rotationDegrees) {
        if (pendingBitmap != null) {
            val rotated = DocumentFileProcessor.rotateBitmap(pendingBitmap, rotationDegrees)
            previewBitmap = DocumentFileProcessor.applyScanFilter(rotated, selectedFilter)
        }
    }

    val suggestedTags = remember(selectedCategory) {
        when (selectedCategory) {
            DocumentCategory.IDENTITY -> listOf("passport", "id-card", "license", "government")
            DocumentCategory.FINANCIAL -> listOf("tax-2026", "bank", "invoice", "insurance")
            DocumentCategory.MEDICAL -> listOf("prescription", "lab-report", "health", "vaccine")
            DocumentCategory.LEGAL -> listOf("contract", "deed", "property", "agreement")
            DocumentCategory.ACADEMIC -> listOf("degree", "transcript", "certificate", "resume")
            DocumentCategory.RECEIPTS -> listOf("warranty", "receipt", "electronics", "purchase")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDiscard,
                    modifier = Modifier.testTag("discard_staged_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Discard scan"
                    )
                }
                Text(
                    text = if (pendingBitmap != null) "Enhance & Save Scan" else "Save Imported Document",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (pendingBitmap != null) {
                OutlinedButton(
                    onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                    modifier = Modifier.testTag("rotate_scan_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Rotate 90 degrees",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rotate")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = VaultNavyDark)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Filtered scan preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF File Staged",
                            tint = VaultTealLight,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PDF Document Ready for Vault Encryption",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = pendingUri?.lastPathSegment ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Scan Filter Selector (For Image Scans)
        if (pendingBitmap != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Document Scan Enhancement",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScanFilterMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedFilter == mode,
                        onClick = { selectedFilter = mode },
                        label = { Text(mode.label) },
                        leadingIcon = if (selectedFilter == mode) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("scan_filter_${mode.name.lowercase()}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Document Title
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Document Title") },
            placeholder = { Text("e.g., Passport Renewal, 2026 Tax Return, Blood Test") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scan_title_input"),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Category Selection
        Text(
            text = "Vault Category",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DocumentCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category.displayName) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.iconVector(),
                            contentDescription = null,
                            tint = category.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("scan_category_${category.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Keywords for Smart Search
        OutlinedTextField(
            value = keywords,
            onValueChange = { keywords = it },
            label = { Text("Smart Search Keywords (comma-separated)") },
            placeholder = { Text("passport, travel, identity, urgent") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scan_keywords_input"),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick tags:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            suggestedTags.forEach { suggestion ->
                AssistChip(
                    onClick = {
                        val existing = keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (!existing.contains(suggestion)) {
                            keywords = (existing + suggestion).joinToString(", ")
                        }
                    },
                    label = { Text("+$suggestion") }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Document Notes / ID Number
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Document Number / Expiry / Notes") },
            placeholder = { Text("Optional ID number, expiration date, or searchable notes…") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scan_notes_input"),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Offline Access Pin Switch
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Available Offline",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Keep encrypted copy cached on device for zero-internet viewing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isOfflinePinned,
                    onCheckedChange = { isOfflinePinned = it },
                    modifier = Modifier.testTag("scan_offline_pin_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (pendingBitmap != null) {
                    val rotated = DocumentFileProcessor.rotateBitmap(pendingBitmap, rotationDegrees)
                    onSaveScannedBitmap(
                        rotated,
                        title.ifBlank { "${selectedCategory.shortName} Scan" },
                        selectedCategory,
                        keywords,
                        notes,
                        selectedFilter,
                        isOfflinePinned
                    )
                } else if (pendingUri != null) {
                    onSaveImportedUri(
                        pendingUri,
                        title,
                        selectedCategory,
                        keywords,
                        notes,
                        isOfflinePinned
                    )
                }
            },
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_document_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save to Vault & Cloud Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
