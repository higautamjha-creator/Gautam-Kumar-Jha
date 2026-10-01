package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentFileType
import com.example.data.model.SortOption
import com.example.data.model.SyncStatus
import com.example.data.model.UserAuthProfile
import com.example.data.model.VaultDocumentEntity
import com.example.data.scanner.DocumentFileProcessor
import com.example.ui.theme.VaultEmeraldAccent
import com.example.ui.theme.VaultEmeraldLight
import com.example.ui.theme.VaultNavyDark
import com.example.ui.theme.VaultTealLight
import com.example.ui.viewmodel.CategorySummary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun DocumentCategory.iconVector(): ImageVector = when (this) {
    DocumentCategory.IDENTITY -> Icons.Default.Badge
    DocumentCategory.FINANCIAL -> Icons.Default.AccountBalance
    DocumentCategory.MEDICAL -> Icons.Default.LocalHospital
    DocumentCategory.LEGAL -> Icons.Default.Gavel
    DocumentCategory.ACADEMIC -> Icons.Default.School
    DocumentCategory.RECEIPTS -> Icons.AutoMirrored.Filled.ReceiptLong
}

@Composable
fun SmartSearchHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: DocumentCategory?,
    onSelectCategory: (DocumentCategory?) -> Unit,
    onlyFavorites: Boolean,
    onToggleFavorites: () -> Unit,
    onlyPdfs: Boolean,
    onTogglePdfs: () -> Unit,
    sortOption: SortOption,
    onSelectSortOption: (SortOption) -> Unit,
    totalResultCount: Int,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("smart_search_input"),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search documents",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search query"
                            )
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_documents_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Sort documents"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSelectSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedCategory == null && !onlyFavorites && !onlyPdfs,
                onClick = {
                    onSelectCategory(null)
                    if (onlyFavorites) onToggleFavorites()
                    if (onlyPdfs) onTogglePdfs()
                },
                label = { Text("All ($totalResultCount)") },
                modifier = Modifier.testTag("filter_chip_all")
            )

            FilterChip(
                selected = onlyFavorites,
                onClick = onToggleFavorites,
                label = { Text("Starred") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("filter_chip_starred")
            )

            FilterChip(
                selected = onlyPdfs,
                onClick = onTogglePdfs,
                label = { Text("PDFs") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("filter_chip_pdfs")
            )

            DocumentCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCategory(category) },
                    label = { Text(category.shortName) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.iconVector(),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else category.accentColor
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = category.accentColor.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
fun VaultHeroSecurityCard(
    currentUser: UserAuthProfile?,
    totalDocuments: Int,
    offlineCachedCount: Int,
    totalStorageBytes: Long,
    pendingSyncCount: Int,
    isSyncing: Boolean,
    onOpenScanner: () -> Unit,
    onUploadFileClick: () -> Unit,
    onAccountOrSyncClick: () -> Unit,
    onLockVaultClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vault_hero_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_vault_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                VaultNavyDark.copy(alpha = 0.86f),
                                VaultNavyDark.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VaultEmeraldAccent.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, VaultEmeraldLight.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable(onClick = onLockVaultClick)
                                .testTag("lock_vault_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Lock Vault with Biometrics",
                                    tint = VaultEmeraldLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "LOCK VAULT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultEmeraldLight
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clickable(onClick = onAccountOrSyncClick)
                            .testTag("hero_cloud_status_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isSyncing -> Icons.Default.CloudSync
                                    currentUser != null && pendingSyncCount == 0 -> Icons.Default.CloudDone
                                    else -> Icons.Default.CloudQueue
                                },
                                contentDescription = "Cloud backup status",
                                tint = if (currentUser != null) VaultTealLight else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = when {
                                    isSyncing -> "Syncing…"
                                    currentUser != null -> currentUser.email
                                    else -> "Connect Gmail Cloud"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (currentUser != null) {
                        "Welcome back, ${currentUser.displayName}"
                    } else {
                        "Personal Document Vault"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )

                Text(
                    text = if (currentUser != null) {
                        "Backed up to Google Cloud (${currentUser.email}) & cached for offline access."
                    } else {
                        "Documents are stored in encrypted offline cache. Sign in with Gmail to enable cloud backup."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroMetricBadge(
                        value = totalDocuments.toString(),
                        label = "Documents",
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricBadge(
                        value = offlineCachedCount.toString(),
                        label = "Offline Ready",
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricBadge(
                        value = DocumentFileProcessor.formatBytes(totalStorageBytes),
                        label = "Vault Size",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenScanner,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("hero_scan_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultTealLight,
                            contentColor = VaultNavyDark
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_scan_document),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onUploadFileClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("hero_upload_button"),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PDF / Image",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricBadge(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun CategoryStripSection(
    summaries: List<CategorySummary>,
    selectedCategory: DocumentCategory?,
    onSelectCategory: (DocumentCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Document Categories",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (selectedCategory != null) {
                Text(
                    text = "Clear filter",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onSelectCategory(selectedCategory) }
                        .padding(4.dp)
                        .testTag("clear_category_filter_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(summaries, key = { it.category.name }) { summary ->
                val isSelected = selectedCategory == summary.category
                val accent = summary.category.accentColor
                ElevatedCard(
                    modifier = Modifier
                        .width(156.dp)
                        .clickable { onSelectCategory(summary.category) }
                        .testTag("category_card_${summary.category.name.lowercase()}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isSelected) {
                            accent.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = summary.category.iconVector(),
                                    contentDescription = summary.category.displayName,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) accent else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${summary.count}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = summary.category.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (summary.totalBytes > 0) {
                                DocumentFileProcessor.formatBytes(summary.totalBytes)
                            } else {
                                summary.category.shortName
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentVaultCard(
    document: VaultDocumentEntity,
    onPreviewClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleOfflinePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = document.categoryEnum
    val fileType = document.fileTypeEnum
    val syncStatus = document.syncStatusEnum
    val localFile = remember(document.localFilePath) { File(document.localFilePath) }
    val existsLocally = remember(document.localFilePath, document.updatedAt) { localFile.exists() }
    val dateFormatted = remember(document.updatedAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(document.updatedAt))
    }

    var pdfThumbnailBitmap by remember(document.id, document.localFilePath) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(document.id, document.localFilePath, fileType) {
        if (fileType == DocumentFileType.PDF_DOCUMENT && localFile.exists()) {
            pdfThumbnailBitmap = DocumentFileProcessor.renderPdfPageToBitmap(
                pdfFile = localFile,
                pageIndex = 0,
                maxDimensionPx = 360
            )
        }
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPreviewClick)
            .testTag("document_card_${document.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Document Thumbnail Preview Box
                Box(
                    modifier = Modifier
                        .size(width = 74.dp, height = 94.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        fileType == DocumentFileType.PDF_DOCUMENT && pdfThumbnailBitmap != null -> {
                            Image(
                                bitmap = pdfThumbnailBitmap!!.asImageBitmap(),
                                contentDescription = "PDF preview thumbnail for ${document.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        fileType != DocumentFileType.PDF_DOCUMENT && existsLocally -> {
                            AsyncImage(
                                model = localFile,
                                contentDescription = "Document preview thumbnail for ${document.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = if (fileType == DocumentFileType.PDF_DOCUMENT) {
                                    Icons.Default.PictureAsPdf
                                } else {
                                    Icons.Default.Description
                                },
                                contentDescription = document.title,
                                tint = category.accentColor,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    // File type badge at bottom of thumbnail
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = VaultNavyDark.copy(alpha = 0.82f)
                    ) {
                        Text(
                            text = if (fileType == DocumentFileType.PDF_DOCUMENT) {
                                "PDF · ${document.pageCount}p"
                            } else {
                                "SCAN"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Metadata Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = category.accentColor.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = category.iconVector(),
                                    contentDescription = null,
                                    tint = category.accentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = category.shortName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = category.accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleOfflinePin,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("pin_offline_button_${document.id}")
                            ) {
                                Icon(
                                    imageVector = if (document.isOfflinePinned) {
                                        Icons.Filled.PushPin
                                    } else {
                                        Icons.Outlined.PushPin
                                    },
                                    contentDescription = if (document.isOfflinePinned) {
                                        "Pinned for offline access"
                                    } else {
                                        "Pin for offline access"
                                    },
                                    tint = if (document.isOfflinePinned) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("favorite_button_${document.id}")
                            ) {
                                Icon(
                                    imageVector = if (document.isFavorite) {
                                        Icons.Default.Star
                                    } else {
                                        Icons.Default.StarBorder
                                    },
                                    contentDescription = if (document.isFavorite) {
                                        "Remove from starred"
                                    } else {
                                        "Add to starred"
                                    },
                                    tint = if (document.isFavorite) {
                                        Color(0xFFF59E0B)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (document.notes.isNotBlank()) {
                        Text(
                            text = document.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Keywords tags
                    AnimatedVisibility(visible = document.keywordList.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            document.keywordList.take(4).forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Status Row: Offline Cache + Cloud Sync + Size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Offline cached indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = "Offline cached",
                                    tint = if (existsLocally && document.isOfflinePinned) {
                                        VaultEmeraldAccent
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (existsLocally && document.isOfflinePinned) "Offline" else "Local",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (existsLocally && document.isOfflinePinned) {
                                        VaultEmeraldAccent
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            // Cloud sync indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (syncStatus == SyncStatus.SYNCED) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.CloudQueue
                                    },
                                    contentDescription = syncStatus.label,
                                    tint = if (syncStatus == SyncStatus.SYNCED) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (syncStatus == SyncStatus.SYNCED) "Cloud Synced" else "Local Vault",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (syncStatus == SyncStatus.SYNCED) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        Text(
                            text = "${DocumentFileProcessor.formatBytes(document.fileSizeBytes)} · $dateFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyVaultStateCard(
    isFiltered: Boolean,
    onScanClick: () -> Unit,
    onUploadClick: () -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("empty_vault_state_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_vault),
                contentDescription = "Empty vault illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 200.dp, height = 145.dp)
                    .clip(RoundedCornerShape(18.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isFiltered) {
                    "No Matching Documents Found"
                } else {
                    stringResource(R.string.empty_vault_title)
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isFiltered) {
                    "Try searching for a different keyword, tag, or document category."
                } else {
                    stringResource(R.string.empty_vault_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (isFiltered) {
                AssistChip(
                    onClick = onResetFilters,
                    label = { Text("Reset Search & Filters") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        leadingIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("reset_filters_button")
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onScanClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("empty_state_scan_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Page")
                    }

                    OutlinedButton(
                        onClick = onUploadClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("empty_state_upload_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload File")
                    }
                }
            }
        }
    }
}
