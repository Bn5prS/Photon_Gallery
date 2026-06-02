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
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (mediaId: String, bucketName: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    bucketName: String? = null,
    isMainTab: Boolean = false
) {
    LaunchedEffect(bucketName) {
        viewModel.setBucket(bucketName)
    }

    val images by viewModel.images.collectAsState()
    val groupedImages by viewModel.groupedImages.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val gridAutoPlay by viewModel.gridAutoPlay.collectAsState()
    val gridCellsCount by viewModel.gridCellsCount.collectAsState()


    val lazyGridState = rememberLazyGridState()
    val context = LocalContext.current

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }



    val coroutineScope = rememberCoroutineScope()
    var boxHeight by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { boxHeight = it.size.height.toFloat() }) {

            LazyVerticalGrid(
            columns = GridCells.Fixed(gridCellsCount),
            state = lazyGridState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .gridZoomGestureModifier(gridCellsCount, viewModel::setGridCellsCount, isSelectionMode)
            .pointerInput(lazyGridState) {
                var initialItemUri: String? = null
                var dragStarted = false
                var startOffset = Offset.Zero

                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        startOffset = offset
                        dragStarted = false
                        val x = offset.x.toInt()
                        val y = offset.y.toInt()
                        
                        val item = lazyGridState.layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
                            x in itemInfo.offset.x..(itemInfo.offset.x + itemInfo.size.width) &&
                            y in itemInfo.offset.y..(itemInfo.offset.y + itemInfo.size.height)
                        }
                        item?.let {
                            val uri = it.key as? String
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
                            val item = lazyGridState.layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
                                x in (itemInfo.offset.x + inset)..(itemInfo.offset.x + itemInfo.size.width - inset) &&
                                y in (itemInfo.offset.y + inset)..(itemInfo.offset.y + itemInfo.size.height - inset)
                            }
                            item?.let {
                                val uri = it.key as? String
                                if (uri != null && uri != initialItemUri) {
                                    viewModel.addSelection(uri) 
                                }
                            }
                        }
                    }
                )
            }
    ) {
        if (viewMode == ViewMode.Immersive) {
            items(
                items = images,
                key = { it.id }
            ) { item ->
                GalleryGridItem(
                    item = item,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = {
                        if (isSelectionMode) {
                            viewModel.toggleSelection(item.uri.toString())
                        } else {
                            onPhotoClick(item.id, bucketName)
                        }
                    },
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    isSelected = selectedUris.contains(item.uri.toString()),
                    gridAutoPlay = gridAutoPlay,
                    gridCellsCount = gridCellsCount
                )
            }
        } else {
            groupedImages.forEach { (date, groupItems) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                    )
                }
                items(
                    items = groupItems,
                    key = { it.id }
                ) { item ->
                    GalleryGridItem(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.toggleSelection(item.uri.toString())
                        } else {
                            onPhotoClick(item.id, bucketName)
                        }
                    },
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    isSelected = selectedUris.contains(item.uri.toString()),
                    gridAutoPlay = gridAutoPlay,
                    gridCellsCount = gridCellsCount
                    )
                }
            }
        }
    }


        val actualTotalItems = lazyGridState.layoutInfo.totalItemsCount
        if (actualTotalItems > 100) {
            var dragOffset by remember { mutableStateOf(0f) }
            
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shadowElevation = 4.dp,
                    modifier = Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { 
                                if (actualTotalItems > 0 && boxHeight > 0f) {
                                    val currentPct = lazyGridState.firstVisibleItemIndex.toFloat() / actualTotalItems
                                    dragOffset = currentPct * boxHeight
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            if (boxHeight > 0f) {
                                dragOffset = (dragOffset + dragAmount).coerceIn(0f, boxHeight)
                                val percentage = dragOffset / boxHeight
                                val targetIndex = (percentage * actualTotalItems).toInt().coerceIn(0, actualTotalItems - 1)
                                coroutineScope.launch {
                                    lazyGridState.scrollToItem(targetIndex)
                                }
                            }
                        }
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.rotate(-90f))
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.rotate(90f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryGridItem(
    item: GalleryItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    gridAutoPlay: Boolean = true,
    gridCellsCount: Int = 3
) {
    val context = LocalContext.current
    val screenWidth = context.resources.displayMetrics.widthPixels
    val maxThumbnailSize = screenWidth / 2

    val request = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(maxThumbnailSize, maxThumbnailSize)
            .memoryCacheKey("photo_${item.uri}_$maxThumbnailSize")
            .precision(Precision.INEXACT) 
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }

    val sharedKey = remember(item.uri) {
        "photo_${Uri.encode(item.uri.toString())}"
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )
    
    val combinedScale = scale

    val videoThumbnail by produceState<Bitmap?>(initialValue = null, item.uri) {
        if (item.isVideo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.loadThumbnail(
                        item.uri,
                        android.util.Size(maxThumbnailSize, maxThumbnailSize),
                        null
                    )
                }.getOrNull()
            }
        }
    }

    val painter = rememberAsyncImagePainter(
        model = videoThumbnail ?: request
    )

    LaunchedEffect(gridAutoPlay, painter.state) {
        val state = painter.state
        if (state is AsyncImagePainter.State.Success) {
            val drawable = state.result.image.asDrawable(context.resources)
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
        with(sharedTransitionScope) {
            Image(
                painter = painter,
                contentDescription = null, 
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = sharedKey),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        }
                    )
                    .aspectRatio(1f)
                    .clickable { onClick() }
            )
        }
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 100f
                        )
                    )
            )
            
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatDuration(item.durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Video",
                        modifier = Modifier.padding(6.dp).size(16.dp)
                    )
                }
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
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
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
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
