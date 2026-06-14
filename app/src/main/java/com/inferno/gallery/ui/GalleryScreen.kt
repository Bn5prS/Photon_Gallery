package com.inferno.gallery.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.graphics.drawable.Animatable
import androidx.compose.foundation.Image
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.rotate
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import coil3.gif.repeatCount
import coil3.video.videoFrameMillis
import com.inferno.gallery.ui.components.WavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.AsyncImagePainter
import coil3.asDrawable

import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.SettingsRepository
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import com.inferno.gallery.ui.GalleryListItem



// Removed resolvedUriCache

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (mediaId: String, bucketName: String?, query: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    bucketName: String? = null,
    isMainTab: Boolean = false
) {
    LaunchedEffect(bucketName) {
        viewModel.setBucket(bucketName)
    }

    val pagedMedia = viewModel.pagedMedia.collectAsLazyPagingItems()
    val viewMode by viewModel.viewMode.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val gridAutoPlay by viewModel.gridAutoPlay.collectAsState()
    val gridCellsCount by viewModel.gridCellsCount.collectAsState()
    val thumbnailCornerRadius by viewModel.thumbnailCornerRadius.collectAsState()
    val backupStatuses by viewModel.backupStatuses.collectAsState()
    val lazyGridState = rememberLazyGridState()

    LaunchedEffect(lazyGridState) {
        var previousIndex = 0
        var previousScrollOffset = 0
        snapshotFlow {
            Triple(
                lazyGridState.firstVisibleItemIndex,
                lazyGridState.firstVisibleItemScrollOffset,
                lazyGridState.isScrollInProgress
            )
        }
        .collectLatest { (index, offset, isScrollInProgress) ->
            if (isScrollInProgress) {
                if (index > previousIndex) {
                    viewModel.setScrollDockVisible(false)
                } else if (index < previousIndex) {
                    viewModel.setScrollDockVisible(true)
                } else {
                    if (offset > previousScrollOffset + 15) {
                        viewModel.setScrollDockVisible(false)
                    } else if (offset < previousScrollOffset - 15) {
                        viewModel.setScrollDockVisible(true)
                    }
                }
            }
            previousIndex = index
            previousScrollOffset = offset
        }
    }
    val totalItems = pagedMedia.itemCount
    val isScrollInProgress = lazyGridState.isScrollInProgress
    val context = LocalContext.current

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    val coroutineScope = rememberCoroutineScope()
    var boxHeight by remember { mutableStateOf(0f) }

    val onMediaClick = remember(viewModel, bucketName, onPhotoClick) {
        { item: GalleryItem ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleSelection(item.uri.toString())
            } else {
                onPhotoClick(item.id, bucketName, null)
            }
        }
    }

    val onMediaLongClick = remember(viewModel) {
        { item: GalleryItem ->
            viewModel.toggleSelection(item.uri.toString())
        }
    }

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { boxHeight = it.size.height.toFloat() }) {

            LazyVerticalGrid(
            columns = GridCells.Fixed(gridCellsCount),
            state = lazyGridState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp)
            .gridZoomGestureModifier(gridCellsCount, viewModel::setGridCellsCount, isSelectionMode)
    ) {
        if (viewMode == ViewMode.Immersive) {
            items(
                count = pagedMedia.itemCount,
                key = pagedMedia.itemKey { item ->
                    when (item) {
                        is GalleryListItem.Header -> "header_${item.title}"
                        is GalleryListItem.Item -> item.galleryItem.uri.toString()
                    }
                },
                contentType = pagedMedia.itemContentType { item: GalleryListItem ->
                    if (item is GalleryListItem.Item) "media" else "header"
                }
            ) { index ->
                val listItem = pagedMedia[index]
                if (listItem is GalleryListItem.Item) {
                    val item = listItem.galleryItem
                    GalleryGridItem(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = onMediaClick,
                        onLongClick = onMediaLongClick,
                        modifier = Modifier,
                        isSelected = selectedUris.contains(item.uri.toString()),
                        gridAutoPlay = gridAutoPlay,
                        gridCellsCount = gridCellsCount,
                        thumbnailCornerRadius = thumbnailCornerRadius,
                        backupStatus = backupStatuses[item.id.toLongOrNull() ?: -1L],
                        isScrollInProgress = isScrollInProgress
                    )
                }
            }
        } else {
            items(
                count = pagedMedia.itemCount,
                key = pagedMedia.itemKey { item ->
                    when (item) {
                        is GalleryListItem.Header -> "header_${item.title}"
                        is GalleryListItem.Item -> item.galleryItem.uri.toString()
                    }
                },
                contentType = pagedMedia.itemContentType { item: GalleryListItem ->
                    if (item is GalleryListItem.Item) "media" else "header"
                },
                span = { index ->
                    val listItem = pagedMedia[index]
                    if (listItem is GalleryListItem.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                }
            ) { index ->
                val listItem = pagedMedia[index]
                if (listItem is GalleryListItem.Header) {
                    Text(
                        text = listItem.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                    )
                } else if (listItem is GalleryListItem.Item) {
                    val item = listItem.galleryItem
                    GalleryGridItem(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = onMediaClick,
                        onLongClick = onMediaLongClick,
                        modifier = Modifier,
                        isSelected = selectedUris.contains(item.uri.toString()),
                        gridAutoPlay = gridAutoPlay,
                        gridCellsCount = gridCellsCount,
                        thumbnailCornerRadius = thumbnailCornerRadius,
                        backupStatus = backupStatuses[item.id.toLongOrNull() ?: -1L],
                        isScrollInProgress = isScrollInProgress
                    )
                }
            }
        }
    }

        FastScroller(
            lazyGridState = lazyGridState,
            totalItems = totalItems,
            images = remember(pagedMedia.itemSnapshotList) {
                pagedMedia.itemSnapshotList.mapNotNull { (it as? GalleryListItem.Item)?.galleryItem }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 56.dp
                )
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryGridItem(
    item: GalleryItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (GalleryItem) -> Unit,
    onLongClick: (GalleryItem) -> Unit = {},
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    gridAutoPlay: Boolean = true,
    gridCellsCount: Int = 3,
    thumbnailCornerRadius: Float = 0f,
    backupStatus: String? = null,
    isScrollInProgress: Boolean = false
) {
    val context = LocalContext.current
    val resolvedUri = remember(item) {
        if (item.telegramThumbFileId != null && !java.io.File(item.path).exists()) {
            Uri.parse("telegram://${item.telegramThumbFileId}")
        } else if (item.telegramFileId != null && !java.io.File(item.path).exists()) {
            Uri.parse("telegram://${item.telegramFileId}")
        } else {
            item.uri
        }
    }

    val settings = remember { com.inferno.gallery.data.SettingsRepository(context) }
    val cacheThumbnailsEnabled by settings.cacheThumbnailsEnabledFlow.collectAsState(initial = true)

    val request = remember<ImageRequest>(resolvedUri, gridAutoPlay, cacheThumbnailsEnabled) {
        val cachePolicy = if (cacheThumbnailsEnabled) CachePolicy.ENABLED else CachePolicy.DISABLED
        ImageRequest.Builder(context)
            .data(resolvedUri)
            .size(384, 384)
            .memoryCacheKey("photo_${resolvedUri}_384")
            .precision(Precision.EXACT)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(cachePolicy)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .apply {
                if (item.isVideo || !gridAutoPlay) {
                    videoFrameMillis(0)
                }
                if (!gridAutoPlay) {
                    repeatCount(0)
                }
            }
            .build()
    }

    val sharedKey = remember(item.uri) {
        "photo_${item.uri}"
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetScale = when {
        isSelected -> 0.90f
        isPressed -> 0.97f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    
    val combinedScale = scale

    val painter = rememberAsyncImagePainter(
        model = request,
        filterQuality = androidx.compose.ui.graphics.FilterQuality.Low
    )

    val painterState by painter.state.collectAsState()

    LaunchedEffect(gridAutoPlay, painterState) {
        val state = painterState
        if (state is AsyncImagePainter.State.Success) {
            val image = state.result.image
            val drawable = (image as? coil3.DrawableImage)?.drawable
            @Suppress("USELESS_IS_CHECK")
            if (drawable is Animatable) {
                if (gridAutoPlay) {
                    drawable.start()
                } else {
                    drawable.stop()
                }
            }
        }
    }

    Box(modifier = modifier.scale(combinedScale)) {
        val isSkeletonVisible = painterState !is AsyncImagePainter.State.Success
        val skeletonColor = if (isSkeletonVisible) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent

        with(sharedTransitionScope) {
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            val imageModifier = Modifier
                .aspectRatio(1f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(thumbnailCornerRadius.dp))
                .background(skeletonColor)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onClick(item) },
                    onLongClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onLongClick(item)
                    }
                )

            val finalModifier = imageModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = sharedKey),
                animatedVisibilityScope = animatedVisibilityScope,
                enter = fadeIn(),
                exit = fadeOut(),
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                boundsTransform = { _, _ ->
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                }
            )

            Image(
                painter = painter,
                contentDescription = null, 
                contentScale = ContentScale.Crop,
                modifier = finalModifier
            )
        }

        if (item.isVideo) {
            val iconSize = when (gridCellsCount) {
                1, 2, 3 -> 14.dp
                4 -> 11.dp
                else -> 9.dp
            }
            val containerSize = when (gridCellsCount) {
                1, 2, 3 -> 24.dp
                4 -> 20.dp
                else -> 16.dp
            }
            val fontSize = when (gridCellsCount) {
                1, 2, 3 -> 11.sp
                4 -> 9.sp
                else -> 8.sp
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Duration Text: Outlined/shadowed, NO background shape
                Text(
                    text = formatDuration(item.durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )

                // 2. Play Icon: Rounded/circular background shape
                Box(
                    modifier = Modifier
                        .size(containerSize)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        if (isSelected) {
            // Apply a thick primary border inside the bounds
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = 4.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(thumbnailCornerRadius.dp)
                    )
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier.padding(4.dp).size(20.dp)
                )
            }
        }

        val isPending = backupStatus == "PENDING"
        val isBackedUp = backupStatus == "SUCCESS" || item.telegramFileId != null || item.telegramThumbFileId != null
        val localExists = remember(item.path) { java.io.File(item.path).exists() }

        if (!isSelected) {
            if (isPending) {
                var rotationTarget by remember { mutableFloatStateOf(0f) }
                val rotation by animateFloatAsState(
                    targetValue = rotationTarget,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessVeryLow
                    ),
                    label = "syncRotation"
                )

                LaunchedEffect(Unit) {
                    while (true) {
                        rotationTarget += 360f
                        delay(1200)
                    }
                }

                Icon(
                    imageVector = Icons.Outlined.Sync,
                    contentDescription = "Backup in progress",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .graphicsLayer(rotationZ = rotation)
                        .padding(8.dp)
                        .size(20.dp)
                )
            } else if (isBackedUp) {
                val isCloudOnly = !localExists
                val icon = if (isCloudOnly) Icons.Outlined.CloudDownload else Icons.Outlined.CloudDone
                val tint = if (isCloudOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                val contentDesc = if (isCloudOnly) "Cloud only" else "Backed up"
                
                Icon(
                    imageVector = icon,
                    contentDescription = contentDesc,
                    tint = tint,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
        }

        val ext = remember(item.name) { item.name.substringAfterLast('.', "").lowercase() }
        val badgeText = when (ext) {
            "gif", "webp" -> "GIF"
            "svg" -> "SVG"
            "dng", "tiff", "tif", "raw", "cr2", "nef", "arw" -> "RAW"
            else -> null
        }

        if (badgeText != null && !item.isVideo) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        if (item.searchScore != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                val formattedScore = String.format("%.3f", item.searchScore)
                Text(
                    text = "Score: $formattedScore",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

fun formatDuration(millis: Long?): String {
    if (millis == null || millis <= 0) return ""
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%02d:%02d", minutes, seconds)
}


private fun Modifier.gridZoomGestureModifier(
    gridCellsCount: Int,
    onGridCountChange: (Int) -> Unit,
    isSelectionMode: Boolean
) = composed {
    val currentGridCellsCount by androidx.compose.runtime.rememberUpdatedState(gridCellsCount)
    val currentOnGridCountChange by androidx.compose.runtime.rememberUpdatedState(onGridCountChange)
    
    this.then(
        Modifier.pointerInput(isSelectionMode) {
            if (isSelectionMode) return@pointerInput
            
            var accumulatedScale = 1f
            var activeZoom = 1f
            
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val initialEvent = awaitPointerEvent()
                
                val pointers = initialEvent.changes.filter { it.pressed }
                if (pointers.size >= 2) {
                    accumulatedScale = 1f
                    activeZoom = 1f
                    
                    val initialGridCellsCount = currentGridCellsCount
                    
                    do {
                        val zoomEvent = awaitPointerEvent()
                        val currentPointers = zoomEvent.changes.filter { it.pressed }
                        
                        if (currentPointers.size >= 2) {
                            val zoomChange = zoomEvent.calculateZoom()
                            
                            // Consume ALL events immediately to prevent scroll conflicts
                            zoomEvent.changes.forEach { 
                                if (it.positionChanged()) it.consume() 
                            }
                            
                            if (kotlin.math.abs(zoomChange - 1f) > 0.01f) {
                                accumulatedScale *= zoomChange
                                activeZoom = accumulatedScale
                                
                                val zoomRatio = 1f / activeZoom
                                val newCount = initialGridCellsCount * zoomRatio
                                val roundedCount = newCount.roundToInt().coerceIn(2, 6)
                                
                                if (roundedCount != currentGridCellsCount) {
                                    currentOnGridCountChange(roundedCount)
                                }
                            }
                        }
                    } while (zoomEvent.changes.any { it.pressed })
                    
                    accumulatedScale = 1f
                    activeZoom = 1f
                }
            }
        }
    )
}

@Composable
fun FastScroller(
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    totalItems: Int,
    images: List<GalleryItem>,
    modifier: Modifier = Modifier
) {
    if (totalItems < 50) return

    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    var boxHeight by remember { mutableStateOf(0f) }
    
    val haptic = LocalHapticFeedback.current
    var currentDateString by remember { mutableStateOf("") }
    
    val thumbHeight = if (isDragging) 48.dp else 36.dp
    val thumbWidth = if (isDragging) 8.dp else 4.dp
    
    val animatedThumbHeight by animateDpAsState(
        targetValue = thumbHeight, 
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "thumbH"
    )
    val animatedThumbWidth by animateDpAsState(
        targetValue = thumbWidth, 
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "thumbW"
    )

    val density = LocalDensity.current
    val thumbHeightPx = with(density) { 48.dp.toPx() } 

    LaunchedEffect(lazyGridState, boxHeight, isDragging, totalItems) {
        snapshotFlow { lazyGridState.firstVisibleItemIndex }.collectLatest { firstVisible ->
            if (!isDragging && totalItems > 0 && boxHeight > thumbHeightPx) {
                val currentPct = firstVisible.toFloat() / totalItems
                dragOffset = currentPct * (boxHeight - thumbHeightPx)
            }
        }
    }

    var targetIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(targetIndex) {
        if (targetIndex >= 0) {
            delay(16)
            lazyGridState.scrollToItem(targetIndex)
        }
    }

    val isVisible = lazyGridState.isScrollInProgress || isDragging

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { boxHeight = it.size.height.toFloat() }
                .fillMaxHeight()
                .width(48.dp) 
        ) {
            AnimatedVisibility(
                visible = isDragging && currentDateString.isNotEmpty(),
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 }),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(-120, dragOffset.roundToInt()) }
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = currentDateString,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp)
                    .offset { IntOffset(0, dragOffset.roundToInt()) }
                    .size(width = animatedThumbWidth, height = animatedThumbHeight)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            )

            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalItems) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                if (boxHeight > thumbHeightPx) {
                                    dragOffset = offset.y.coerceIn(0f, boxHeight - thumbHeightPx)
                                }
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false }
                        ) { change, dragAmount ->
                            change.consume()
                            if (boxHeight > thumbHeightPx) {
                                dragOffset = (dragOffset + dragAmount).coerceIn(0f, boxHeight - thumbHeightPx)
                                val percentage = dragOffset / (boxHeight - thumbHeightPx)
                                targetIndex = (percentage * totalItems).toInt().coerceIn(0, totalItems - 1)
                                
                                val imagesIndex = (percentage * images.size).toInt().coerceIn(0, maxOf(0, images.size - 1))
                                val itemDate = images.getOrNull(imagesIndex)?.dateAdded
                                if (itemDate != null) {
                                    val date = Date(itemDate * 1000L) // dateAdded is likely in seconds, but we should check. Wait, dateAdded in MediaStore is seconds.

                                    val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                                    val newDateString = formatter.format(date)
                                    if (newDateString != currentDateString) {
                                        currentDateString = newDateString
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                        }
                    }
            )
        }
    }
}
