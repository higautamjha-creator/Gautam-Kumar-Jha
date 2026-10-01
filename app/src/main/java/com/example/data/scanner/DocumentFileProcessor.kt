package com.example.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.data.model.DocumentFileType
import com.example.data.model.ScanFilterMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

data class ProcessedFileResult(
    val localFile: File,
    val fileType: DocumentFileType,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val sha256Checksum: String,
    val suggestedTitle: String
)

object DocumentFileProcessor {

    fun getVaultDirectory(context: Context): File {
        val dir = File(context.filesDir, "vault_docs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getTempScanDirectory(context: Context): File {
        val dir = File(context.cacheDir, "camera_scans")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun applyScanFilter(source: Bitmap, filterMode: ScanFilterMode): Bitmap =
        withContext(Dispatchers.Default) {
            if (filterMode == ScanFilterMode.ORIGINAL) return@withContext source

            val width = source.width.coerceAtLeast(1)
            val height = source.height.coerceAtLeast(1)
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            val matrix = when (filterMode) {
                ScanFilterMode.ORIGINAL -> ColorMatrix()
                ScanFilterMode.GRAYSCALE -> ColorMatrix().apply {
                    setSaturation(0f)
                }
                ScanFilterMode.HIGH_CONTRAST -> {
                    val contrast = 1.45f
                    val translate = (-0.5f * contrast + 0.5f) * 255f
                    ColorMatrix(
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, translate,
                            0f, contrast, 0f, 0f, translate,
                            0f, 0f, contrast, 0f, translate,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
                ScanFilterMode.BW_DOCUMENT -> {
                    val grayscale = ColorMatrix().apply { setSaturation(0f) }
                    val contrast = 2.35f
                    val translate = (-0.5f * contrast + 0.52f) * 255f
                    val highContrast = ColorMatrix(
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, translate,
                            0f, contrast, 0f, 0f, translate,
                            0f, 0f, contrast, 0f, translate,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    grayscale.postConcat(highContrast)
                    grayscale
                }
            }

            paint.colorFilter = ColorMatrixColorFilter(matrix)
            canvas.drawBitmap(source, 0f, 0f, paint)
            output
        }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    suspend fun saveScannedBitmapToVault(
        context: Context,
        bitmap: Bitmap,
        filterMode: ScanFilterMode,
        documentId: String = UUID.randomUUID().toString()
    ): ProcessedFileResult = withContext(Dispatchers.IO) {
        val filteredBitmap = applyScanFilter(bitmap, filterMode)
        val vaultDir = getVaultDirectory(context)
        val targetFile = File(vaultDir, "scan_${documentId}.jpg")

        FileOutputStream(targetFile).use { out ->
            filteredBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            out.flush()
        }

        val sha256 = computeSha256(targetFile)
        ProcessedFileResult(
            localFile = targetFile,
            fileType = DocumentFileType.IMAGE_SCAN,
            fileSizeBytes = targetFile.length(),
            pageCount = 1,
            sha256Checksum = sha256,
            suggestedTitle = "Scanned Document"
        )
    }

    suspend fun importUriToVault(
        context: Context,
        uri: Uri,
        documentId: String = UUID.randomUUID().toString()
    ): ProcessedFileResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)?.lowercase(Locale.ROOT) ?: ""
        val originalName = resolveDisplayName(context, uri)
        val isPdf = mimeType.contains("pdf") || originalName.endsWith(".pdf", ignoreCase = true)

        val extension = when {
            isPdf -> "pdf"
            mimeType.contains("png") || originalName.endsWith(".png", ignoreCase = true) -> "png"
            mimeType.contains("webp") || originalName.endsWith(".webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }

        val vaultDir = getVaultDirectory(context)
        val targetFile = File(vaultDir, "doc_${documentId}.$extension")

        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
                output.flush()
            }
        } ?: throw IllegalArgumentException("Unable to read selected document URI")

        val pageCount = if (isPdf) {
            inspectPdfPageCount(targetFile)
        } else {
            1
        }

        val cleanTitle = originalName
            .substringBeforeLast(".")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
            .ifEmpty { if (isPdf) "Imported PDF Document" else "Imported Image Document" }

        ProcessedFileResult(
            localFile = targetFile,
            fileType = if (isPdf) DocumentFileType.PDF_DOCUMENT else DocumentFileType.IMAGE_UPLOAD,
            fileSizeBytes = targetFile.length(),
            pageCount = pageCount,
            sha256Checksum = computeSha256(targetFile),
            suggestedTitle = cleanTitle
        )
    }

    fun inspectPdfPageCount(pdfFile: File): Int {
        if (!pdfFile.exists()) return 1
        return try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount.coerceAtLeast(1)
                }
            }
        } catch (_: Exception) {
            1
        }
    }

    suspend fun renderPdfPageToBitmap(
        pdfFile: File,
        pageIndex: Int,
        maxDimensionPx: Int = 1400
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext null
        try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) return@withContext null
                    val safeIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(safeIndex).use { page ->
                        val aspect = page.width.toFloat() / page.height.toFloat().coerceAtLeast(1f)
                        val targetWidth: Int
                        val targetHeight: Int
                        if (aspect >= 1f) {
                            targetWidth = maxDimensionPx
                            targetHeight = (maxDimensionPx / aspect).toInt().coerceAtLeast(1)
                        } else {
                            targetHeight = maxDimensionPx
                            targetWidth = (maxDimensionPx * aspect).toInt().coerceAtLeast(1)
                        }
                        val bitmap = Bitmap.createBitmap(
                            targetWidth,
                            targetHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                        bitmap
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun decodeBitmapFromUri(context: Context, uri: Uri, maxDim: Int = 1800): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                var sampleSize = 1
                while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                }
            } catch (_: Exception) {
                null
            }
        }

    fun computeSha256(file: File): String {
        if (!file.exists()) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var read = fis.read(buffer)
                while (read != -1) {
                    digest.update(buffer, 0, read)
                    read = fis.read(buffer)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        var displayName = ""
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex) ?: ""
                }
            }
        } catch (_: Exception) {
        }
        if (displayName.isBlank()) {
            displayName = uri.lastPathSegment?.substringAfterLast("/") ?: "Document"
        }
        return displayName
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }
}
