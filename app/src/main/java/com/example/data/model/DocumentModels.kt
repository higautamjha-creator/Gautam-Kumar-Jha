package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.theme.CategoryAcademicColor
import com.example.ui.theme.CategoryFinancialColor
import com.example.ui.theme.CategoryIdentityColor
import com.example.ui.theme.CategoryLegalColor
import com.example.ui.theme.CategoryMedicalColor
import com.example.ui.theme.CategoryReceiptsColor

enum class DocumentCategory(
    val displayName: String,
    val shortName: String,
    val subtitle: String,
    val accentColor: Color
) {
    IDENTITY(
        displayName = "Identity & Passports",
        shortName = "Identity",
        subtitle = "Passports, National ID, Driver's Licenses",
        accentColor = CategoryIdentityColor
    ),
    FINANCIAL(
        displayName = "Tax & Finance",
        shortName = "Finance",
        subtitle = "Tax Returns, Bank Statements, Insurance",
        accentColor = CategoryFinancialColor
    ),
    MEDICAL(
        displayName = "Medical & Health",
        shortName = "Medical",
        subtitle = "Prescriptions, Lab Reports, Vaccination",
        accentColor = CategoryMedicalColor
    ),
    LEGAL(
        displayName = "Property & Legal",
        shortName = "Legal",
        subtitle = "Contracts, Deeds, Leases, Agreements",
        accentColor = CategoryLegalColor
    ),
    ACADEMIC(
        displayName = "Certificates & Career",
        shortName = "Academic",
        subtitle = "Degrees, Transcripts, Certifications",
        accentColor = CategoryAcademicColor
    ),
    RECEIPTS(
        displayName = "Receipts & Warranties",
        shortName = "Receipts",
        subtitle = "Invoices, Manuals, Product Warranties",
        accentColor = CategoryReceiptsColor
    );

    companion object {
        fun fromName(name: String): DocumentCategory {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: IDENTITY
        }
    }
}

enum class DocumentFileType(val label: String) {
    IMAGE_SCAN("Camera Scan"),
    IMAGE_UPLOAD("Image File"),
    PDF_DOCUMENT("PDF Document");

    companion object {
        fun fromName(name: String): DocumentFileType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: IMAGE_SCAN
        }
    }
}

enum class ScanFilterMode(val label: String, val description: String) {
    ORIGINAL("Original Color", "Natural lighting & full color"),
    BW_DOCUMENT("Crisp B&W Scan", "High-legibility monochrome text"),
    HIGH_CONTRAST("Enhanced Ink", "Boosted contrast for faded pages"),
    GRAYSCALE("Archival Gray", "Balanced grayscale document tone");

    companion object {
        fun fromName(name: String): ScanFilterMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ORIGINAL
        }
    }
}

enum class SyncStatus(val label: String) {
    SYNCED("Cloud Synced"),
    PENDING_UPLOAD("Cached Offline · Pending Sync"),
    UPLOADING("Uploading to Cloud…"),
    SYNC_ERROR("Sync Retry Needed");

    companion object {
        fun fromName(name: String): SyncStatus {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: PENDING_UPLOAD
        }
    }
}

enum class SortOption(val label: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    TITLE_ASC("Name (A–Z)"),
    SIZE_DESC("Largest Size")
}

@Entity(tableName = "vault_documents")
data class VaultDocumentEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val ownerEmail: String,
    val title: String,
    val category: String,
    val fileType: String,
    val keywords: String,
    val notes: String,
    val localFilePath: String,
    val cloudStorageUrl: String? = null,
    val cloudStoragePath: String? = null,
    val fileSizeBytes: Long = 0L,
    val pageCount: Int = 1,
    val sha256Checksum: String = "",
    val isOfflinePinned: Boolean = true,
    val isFavorite: Boolean = false,
    val scanFilter: String = ScanFilterMode.ORIGINAL.name,
    val syncStatus: String = SyncStatus.PENDING_UPLOAD.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val categoryEnum: DocumentCategory
        get() = DocumentCategory.fromName(category)

    val fileTypeEnum: DocumentFileType
        get() = DocumentFileType.fromName(fileType)

    val syncStatusEnum: SyncStatus
        get() = SyncStatus.fromName(syncStatus)

    val scanFilterEnum: ScanFilterMode
        get() = ScanFilterMode.fromName(scanFilter)

    val keywordList: List<String>
        get() = keywords.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}

data class UserAuthProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val isFirebaseConnected: Boolean = true
)

data class FirebaseRuntimeConfig(
    val projectId: String = "",
    val applicationId: String = "",
    val apiKey: String = "",
    val storageBucket: String = "",
    val webClientId: String = ""
) {
    val isComplete: Boolean
        get() = projectId.isNotBlank() && applicationId.isNotBlank() && apiKey.isNotBlank()
}
