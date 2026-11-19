package com.inferno.gallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.data.ai.AiFolderScopeMode
import com.inferno.gallery.data.ai.AiIndexingFeature
import com.inferno.gallery.data.ai.AiIndexingFolderConfig
import com.inferno.gallery.data.ai.ExcludedAiDataSummary
import com.inferno.gallery.ui.theme.ShapeFull

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiIndexingFoldersSection(
    allFolders: List<String>,
    imageBucketCounts: Map<String, Int>,
    galleryExcluded: Set<String>,
    aiConfig: AiIndexingFolderConfig,
    excludedDataSummary: ExcludedAiDataSummary,
    onSetRespectGalleryExcluded: (Boolean) -> Unit,
    onSetAutoScanNewFolders: (Boolean, List<String>) -> Unit,
    onSetAllFoldersSelection: (Boolean) -> Unit,
    onSetFolderIncluded: (AiIndexingFeature?, String, Boolean) -> Unit,
    onPurgeExcludedData: () -> Unit
) {
    var expandedFolder by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortByCount by remember { mutableStateOf(true) }
    var showPurgeConfirmDialog by remember { mutableStateOf(false) }

    val mergedFolders = remember(allFolders, imageBucketCounts) {
        (allFolders + imageBucketCounts.keys)
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "Trash" }
            .distinct()
    }

    val displayedFolders = remember(mergedFolders, imageBucketCounts, searchQuery, sortByCount) {
        val filtered = if (searchQuery.isBlank()) {
            mergedFolders
        } else {
            val q = searchQuery.trim()
            mergedFolders.filter { it.contains(q, ignoreCase = true) }
        }
        if (sortByCount) {
            filtered.sortedWith(
                compareByDescending<String> { imageBucketCounts[it] ?: 0 }
                    .thenBy { it.lowercase() }
            )
        } else {
            filtered.sortedBy { it.lowercase() }
        }
    }

    val totalPhotosInLibrary = remember(imageBucketCounts) {
        imageBucketCounts.values.sum()
    }
    val smartEligiblePhotos = remember(aiConfig, imageBucketCounts, galleryExcluded) {
        aiConfig.countEligiblePhotos(AiIndexingFeature.SMART_SEARCH, imageBucketCounts, galleryExcluded)
    }
    val faceEligiblePhotos = remember(aiConfig, imageBucketCounts, galleryExcluded) {
        aiConfig.countEligiblePhotos(AiIndexingFeature.FACE_RECOGNITION, imageBucketCounts, galleryExcluded)
    }
    val ocrEligiblePhotos = remember(aiConfig, imageBucketCounts, galleryExcluded) {
        aiConfig.countEligiblePhotos(AiIndexingFeature.OCR_TEXT, imageBucketCounts, galleryExcluded)
    }

    val autoScanNewFolders = remember(aiConfig) {
        aiConfig.ruleFor(AiIndexingFeature.SMART_SEARCH).mode == AiFolderScopeMode.EXCLUDE_SELECTED
    }

    if (showPurgeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            title = {
                Text("Remove skipped folder data?", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(
                    "Clear ${excludedDataSummary.totalCount} old AI search, face, and text index entries from skipped folders. Your photos and videos stay untouched on device storage.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPurgeConfirmDialog = false
                        onPurgeExcludedData()
                    }
                ) {
                    Text(
                        "Remove",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // ── Card 1: Simple Overview + Scan All / Skip All ──
    val summaryHeadline = if (
        smartEligiblePhotos == faceEligiblePhotos &&
        faceEligiblePhotos == ocrEligiblePhotos
    ) {
        "Scanning $smartEligiblePhotos of $totalPhotosInLibrary photos"
    } else {
        "Search: $smartEligiblePhotos · Faces: $faceEligiblePhotos · Text: $ocrEligiblePhotos photos"
    }

    SettingsGroupCard(
        items = listOf(
            {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = summaryHeadline,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Turn folders off to reduce battery & storage load. Tap any folder to choose Search, Faces, or Text separately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onSetAllFoldersSelection(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Scan All")
                        }
                        OutlinedButton(
                            onClick = { onSetAllFoldersSelection(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Skip All")
                        }
                    }
                }
            }
        )
    )

    // ── Card 2: Folders Checklist ──
    if (mergedFolders.isEmpty()) {
        SettingsGroupCard(
            title = "Folders",
            items = listOf(
                {
                    SettingsRow(
                        title = stringResource(R.string.setting_no_folders_found),
                        subtitle = stringResource(R.string.setting_no_folders_found_sub)
                    )
                }
            )
        )
    } else {
        val folderItems = mutableListOf<@Composable () -> Unit>()

        // Search & Sort Controls row
        folderItems.add {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (mergedFolders.size > 5) {
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Find a folder (e.g. WhatsApp, Camera)…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                val activeFolderCount = mergedFolders.count { folder ->
                    AiIndexingFeature.entries.any { aiConfig.isBucketEligible(it, folder, galleryExcluded) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$activeFolderCount of ${mergedFolders.size} active",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = sortByCount,
                            onClick = { sortByCount = true },
                            label = { Text("Most photos", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = !sortByCount,
                            onClick = { sortByCount = false },
                            label = { Text("A–Z", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        if (displayedFolders.isEmpty()) {
            folderItems.add {
                SettingsRow(
                    title = "No matching folders",
                    subtitle = "Try another keyword"
                )
            }
        } else {
            for (folderName in displayedFolders) {
                val photoCount = imageBucketCounts[folderName] ?: 0
                val hiddenByGallery = aiConfig.respectGalleryExcluded && folderName in galleryExcluded
                val smartOn = aiConfig.isBucketEligible(AiIndexingFeature.SMART_SEARCH, folderName, galleryExcluded)
                val faceOn = aiConfig.isBucketEligible(AiIndexingFeature.FACE_RECOGNITION, folderName, galleryExcluded)
                val ocrOn = aiConfig.isBucketEligible(AiIndexingFeature.OCR_TEXT, folderName, galleryExcluded)
                val anyOn = smartOn || faceOn || ocrOn
                val allOn = smartOn && faceOn && ocrOn
                val isExpanded = expandedFolder == folderName && !hiddenByGallery
                val isEnabled = !hiddenByGallery

                val statusLabel = when {
                    hiddenByGallery -> "Hidden in Gallery"
                    allOn -> "Will scan"
                    !anyOn -> "Skipped"
                    else -> buildList {
                        if (smartOn) add("Search")
                        if (faceOn) add("Faces")
                        if (ocrOn) add("Text")
                    }.joinToString(" · ")
                }

                folderItems.add {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (anyOn) Modifier else Modifier.alpha(0.68f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isEnabled) {
                                        Modifier.clickable {
                                            expandedFolder = if (isExpanded) null else folderName
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    if (anyOn) R.drawable.ic_ms_folder else R.drawable.ic_ms_folder_off
                                ),
                                contentDescription = null,
                                tint = if (anyOn && isEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                },
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = folderName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isEnabled) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(
                                                if (isExpanded) R.drawable.ic_ms_keyboard_arrow_up else R.drawable.ic_ms_keyboard_arrow_down
                                            ),
                                            contentDescription = if (isExpanded) "Collapse options" else "Choose AI features",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$photoCount photo${if (photoCount != 1) "s" else ""} · $statusLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (!allOn && anyOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Switch(
                                checked = anyOn,
                                onCheckedChange = if (isEnabled) {
                                    { newValue -> onSetFolderIncluded(null, folderName, newValue) }
                                } else null,
                                enabled = isEnabled,
                                thumbContent = {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(
                                            if (anyOn) R.drawable.ic_ms_check else R.drawable.ic_ms_close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 54.dp, end = 16.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Process with:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FolderFeatureChip(
                                        label = "Search",
                                        selected = smartOn,
                                        onClick = {
                                            onSetFolderIncluded(AiIndexingFeature.SMART_SEARCH, folderName, !smartOn)
                                        }
                                    )
                                    FolderFeatureChip(
                                        label = "Faces",
                                        selected = faceOn,
                                        onClick = {
                                            onSetFolderIncluded(AiIndexingFeature.FACE_RECOGNITION, folderName, !faceOn)
                                        }
                                    )
                                    FolderFeatureChip(
                                        label = "Text",
                                        selected = ocrOn,
                                        onClick = {
                                            onSetFolderIncluded(AiIndexingFeature.OCR_TEXT, folderName, !ocrOn)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SettingsGroupCard(
            title = "Folders",
            items = folderItems
        )
    }

    // ── Card 3: Quiet Bottom Options & Cleanup (Never jumps folder list) ──
    val footerItems = mutableListOf<@Composable () -> Unit>()

    footerItems.add {
        SwitchSettingItem(
            title = "Auto-scan new folders",
            subtitle = "Automatically process newly created folders",
            icon = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
            checked = autoScanNewFolders,
            onCheckedChange = { enabled -> onSetAutoScanNewFolders(enabled, mergedFolders) }
        )
    }

    footerItems.add {
        val galleryHiddenSub = if (galleryExcluded.isEmpty()) {
            "Also ignore folders hidden in Excluded Folders"
        } else {
            "Ignore ${galleryExcluded.size} folder${if (galleryExcluded.size != 1) "s" else ""} hidden from gallery"
        }
        SwitchSettingItem(
            title = "Skip gallery hidden folders",
            subtitle = galleryHiddenSub,
            icon = ImageVector.vectorResource(R.drawable.ic_ms_folder_off),
            checked = aiConfig.respectGalleryExcluded,
            onCheckedChange = onSetRespectGalleryExcluded
        )
    }

    if (excludedDataSummary.totalCount > 0) {
        footerItems.add {
            ClickableSettingItem(
                title = "Remove skipped data (${excludedDataSummary.totalCount})",
                subtitle = "Delete old AI entries from skipped folders",
                icon = ImageVector.vectorResource(R.drawable.ic_ms_cleaning_services),
                onClick = { showPurgeConfirmDialog = true }
            )
        }
    }

    SettingsGroupCard(
        title = "Options",
        items = footerItems
    )
}

@Composable
private fun FolderFeatureChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    )
}
