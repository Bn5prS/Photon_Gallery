package com.inferno.gallery.ui

import android.net.Uri
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inferno.gallery.ui.components.WavyProgressIndicator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CloudScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (mediaId: String, bucketName: String?, query: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val cloudMedia by viewModel.cloudMedia.collectAsState()
    val pendingCount by viewModel.pendingBackupsCount.collectAsState()
    val thumbnailCornerRadius by viewModel.thumbnailCornerRadius.collectAsState()
    val gridAutoPlay by viewModel.gridAutoPlay.collectAsState()
    val isRefreshing by viewModel.isCloudRefreshing.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val gridState = rememberLazyGridState()

    val galleryItems = remember(cloudMedia) {
        cloudMedia.map { item ->
            GalleryItem(
                id = item.id.toString(),
                uri = Uri.parse(item.uriString),
                bucketName = item.bucketName,
                dateAdded = item.dateAdded,
                size = item.size,
                name = item.name,
                dateModified = item.dateModified,
                path = item.filePath,
                isVideo = item.isVideo,
                durationMs = item.durationMs,
                telegramFileId = item.telegramFileId,
                telegramThumbFileId = item.telegramThumbFileId
            )
        }
    }

    val totalSizeBytes = remember(cloudMedia) {
        cloudMedia.sumOf { it.size }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp,
            start = 12.dp,
            end = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxSize()
            .pointerInput(gridState) {
                var initialItemUri: String? = null
                var dragStarted = false
                var startOffset = Offset.Zero

                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        startOffset = offset
                        dragStarted = false
                        val x = offset.x.toInt()
                        val y = offset.y.toInt()
                        
                        val item = gridState.layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
                            x in itemInfo.offset.x..(itemInfo.offset.x + itemInfo.size.width) &&
                            y in itemInfo.offset.y..(itemInfo.offset.y + itemInfo.size.height)
                        }
                        item?.let {
                            val id = it.key as? String
                            val matchedItem = galleryItems.find { item -> item.id == id }
                            val uri = matchedItem?.uri?.toString()
                            if (uri != null) {
                                viewModel.toggleSelection(uri)
                                initialItemUri = uri
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        val distance = (change.position - startOffset).getDistance()
                        if (distance > 40f) {
                            dragStarted = true
                        }

                        if (dragStarted) {
                            val x = change.position.x.toInt()
                            val y = change.position.y.toInt()
                            
                            val inset = 30
                            val item = gridState.layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
                                x in (itemInfo.offset.x + inset)..(itemInfo.offset.x + itemInfo.size.width - inset) &&
                                y in (itemInfo.offset.y + inset)..(itemInfo.offset.y + itemInfo.size.height - inset)
                            }
                            item?.let {
                                val id = it.key as? String
                                val matchedItem = galleryItems.find { item -> item.id == id }
                                val uri = matchedItem?.uri?.toString()
                                if (uri != null && uri != initialItemUri) {
                                    viewModel.addSelection(uri) 
                                }
                            }
                        }
                    }
                )
            }
    ) {
        // ── Stats Dashboard Card (Spans all 3 columns) ───────────────────────────
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Telegram Cloud Storage",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            if (isRefreshing) {
                                WavyProgressIndicator(
                                    modifier = Modifier.size(width = 36.dp, height = 24.dp),
                                    strokeWidth = 2.dp,
                                    amplitude = 3.dp,
                                    frequency = 1.5f,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                IconButton(
                                    onClick = { viewModel.refreshCloudBackups() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Refresh",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Synced Items",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${galleryItems.size}",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Storage Used",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = formatSize(totalSizeBytes),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        if (pendingCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    WavyProgressIndicator(
                                        modifier = Modifier.size(width = 28.dp, height = 16.dp),
                                        strokeWidth = 2.dp,
                                        amplitude = 2.dp,
                                        frequency = 1.5f,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$pendingCount pending",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = "Up to date",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gap/Spacer item
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Photo Grid or Empty State ──────────────────────────────────────────
        if (galleryItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No media backed up to Telegram yet.\nSelect photos in the Gallery and tap 'Backup to Cloud'.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = galleryItems,
                key = { it.id }
            ) { item ->
                GalleryGridItem(
                    item = item,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = { _ ->
                        if (isSelectionMode) {
                            viewModel.toggleSelection(item.uri.toString())
                        } else {
                            onPhotoClick(item.id, "telegram_cloud", null)
                        }
                    },
                    isSelected = selectedUris.contains(item.uri.toString()),
                    gridAutoPlay = gridAutoPlay,
                    gridCellsCount = 3,
                    thumbnailCornerRadius = thumbnailCornerRadius
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(java.util.Locale.US, "%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
