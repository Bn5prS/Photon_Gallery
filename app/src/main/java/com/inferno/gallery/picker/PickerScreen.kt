@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.inferno.gallery.picker

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.inferno.gallery.R
import com.inferno.gallery.data.LocalMediaRepository
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeExtraSmall
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeSmall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class PickerMediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val bucketName: String,
    val isVideo: Boolean,
    val durationMs: Long?
)

@Composable
fun PickerScreen(
    allowMultiple: Boolean,
    mimeType: String,
    onMediaSelected: (List<Uri>) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)

    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val database = remember { DatabaseProvider.getDatabase(context) }

    val allMedia by database.mediaDao().observeAllMedia().collectAsState(initial = emptyList())
    val selectedUris = remember { mutableStateListOf<Uri>() }

    var directMediaStoreItems by remember { mutableStateOf<List<PickerMediaItem>>(emptyList()) }

    // Instant direct MediaStore fallback to ensure picker always renders even before initial Room sync
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val repo = LocalMediaRepository(context.contentResolver)
            val items = repo.getImagesListForSync()
            directMediaStoreItems = items.map {
                PickerMediaItem(
                    id = it.id,
                    uri = it.uri,
                    name = it.name,
                    bucketName = it.bucketName,
                    isVideo = it.isVideo,
                    durationMs = it.durationMs
                )
            }
        }
    }

    // Filter index: 0 = All, 1 = Photos, 2 = Videos
    var filterIndex by remember { mutableIntStateOf(0) }

    val isVideoOnly = mimeType.startsWith("video/", ignoreCase = true)
    val isImageOnly = mimeType.startsWith("image/", ignoreCase = true)

    val sourceItems = remember(allMedia, directMediaStoreItems) {
        if (allMedia.isNotEmpty()) {
            allMedia.map {
                PickerMediaItem(
                    id = it.id,
                    uri = Uri.parse(it.uriString),
                    name = it.name,
                    bucketName = it.bucketName,
                    isVideo = it.isVideo,
                    durationMs = it.durationMs
                )
            }
        } else {
            directMediaStoreItems
        }
    }

    val filteredMedia = remember(sourceItems, mimeType, filterIndex) {
        sourceItems.filter { item ->
            if (item.bucketName == "Trash") return@filter false
            when {
                isVideoOnly -> item.isVideo
                isImageOnly -> !item.isVideo
                filterIndex == 1 -> !item.isVideo
                filterIndex == 2 -> item.isVideo
                else -> true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onCancel,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                            contentDescription = "Cancel"
                        )
                    }

                    Column {
                        Text(
                            text = if (allowMultiple) "Select Media" else "Choose an Item",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (allowMultiple) {
                            Text(
                                text = "${selectedUris.size} selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Filter Chips (only when general mimeType allows both photos and videos)
            if (!isVideoOnly && !isImageOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = filterIndex == 0,
                        onClick = { filterIndex = 0 },
                        label = { Text("All", style = MaterialTheme.typography.labelMedium) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = filterIndex == 1,
                        onClick = { filterIndex = 1 },
                        label = { Text("Photos", style = MaterialTheme.typography.labelMedium) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = filterIndex == 2,
                        onClick = { filterIndex = 2 },
                        label = { Text("Videos", style = MaterialTheme.typography.labelMedium) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Media Grid
            if (filteredMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No media found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 108.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredMedia, key = { it.id }) { item ->
                        val isSelected = selectedUris.contains(item.uri)

                        PickerGridTile(
                            item = item,
                            isSelected = isSelected,
                            allowMultiple = allowMultiple,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (allowMultiple) {
                                    if (isSelected) {
                                        selectedUris.remove(item.uri)
                                    } else {
                                        selectedUris.add(item.uri)
                                    }
                                } else {
                                    onMediaSelected(listOf(item.uri))
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Confirmation Dock for Multi-Select
        AnimatedVisibility(
            visible = allowMultiple && selectedUris.isNotEmpty(),
            enter = slideInVertically(
                animationSpec = MotionTokens.gentleSpring(),
                initialOffsetY = { it }
            ) + fadeIn(MotionTokens.fastEffectsSpec()),
            exit = slideOutVertically(
                animationSpec = MotionTokens.gentleSpring(),
                targetOffsetY = { it }
            ) + fadeOut(MotionTokens.fastEffectsSpec()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = ShapeExtraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedUris.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ElevatedButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMediaSelected(selectedUris.toList())
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerGridTile(
    item: PickerMediaItem,
    isSelected: Boolean,
    allowMultiple: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val borderModifier = if (isSelected) {
        Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, ShapeLarge)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(ShapeLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(borderModifier)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .size(384, 384)
                .crossfade(false)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Selected tint overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            )
        }

        // Video Indicator Badge
        if (item.isVideo) {
            Surface(
                shape = ShapeExtraSmall,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_movie),
                        contentDescription = "Video",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(12.dp)
                    )
                    if (item.durationMs != null && item.durationMs > 0L) {
                        Text(
                            text = formatDuration(item.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Multi-Select Checkmark Pill
        if (allowMultiple) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
