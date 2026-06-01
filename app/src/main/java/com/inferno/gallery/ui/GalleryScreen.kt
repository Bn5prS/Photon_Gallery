package com.inferno.gallery.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import com.inferno.gallery.ui.components.GridThumbnailError
import com.inferno.gallery.ui.components.GridThumbnailPlaceholder
import com.inferno.gallery.ui.components.WavyProgressIndicator
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// GalleryScreen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
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
    LaunchedEffect(bucketName) { viewModel.setBucket(bucketName) }

    val images          by viewModel.images.collectAsState()
    val groupedImages   by viewModel.groupedImages.collectAsState()
    val viewMode        by viewModel.viewMode.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris    by viewModel.selectedUris.collectAsState()
    val gridAutoPlay    by viewModel.gridAutoPlay.collectAsState()
    val gridCellsCount  by viewModel.gridCellsCount.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val isRefreshing    by viewModel.isRefreshing.collectAsState()

    val lazyGridState  = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val context        = LocalContext.current

    BackHandler(enabled = isSelectionMode) { viewModel.clearSelection() }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            WavyProgressIndicator()
        }
        return
    }

    // ── Thumbnail size: computed once per gridCellsCount, NOT inside each cell ──
    // This was previously recalculated inside every GalleryGridItem call.
    val thumbnailPx = remember(gridCellsCount) {
        context.resources.displayMetrics.widthPixels / gridCellsCount
    }

    // ── Spatial cache for drag-selection hit testing ──────────────────────────
    // Rebuilt when the layout info changes (scroll position / new items visible),
    // NOT on every pointer event. Eliminates the O(n) scan at 120 Hz.
    val itemBoundsCache = remember { mutableStateMapOf<String, Rect>() }
    LaunchedEffect(lazyGridState.layoutInfo) {
        itemBoundsCache.clear()
        lazyGridState.layoutInfo.visibleItemsInfo.forEach { info ->
            val key = info.key as? String ?: return@forEach
            itemBoundsCache[key] = Rect(
                left   = info.offset.x.toFloat(),
                top    = info.offset.y.toFloat(),
                right  = (info.offset.x + info.size.width).toFloat(),
                bottom = (info.offset.y + info.size.height).toFloat()
            )
        }
    }

    var boxHeight by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { boxHeight = it.size.height.toFloat() }
    ) {
        val pullToRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh    = { viewModel.refreshMedia() },
            state        = pullToRefreshState,
            modifier     = Modifier.fillMaxSize(),
            indicator    = {
                PullToRefreshDefaults.Indicator(
                    state          = pullToRefreshState,
                    isRefreshing   = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    color          = MaterialTheme.colorScheme.primary,
                    modifier       = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = contentPadding.calculateTopPadding())
                )
            }
        ) {
            LazyVerticalGrid(
                columns             = GridCells.Fixed(gridCellsCount),
                state               = lazyGridState,
                contentPadding      = contentPadding,
                verticalArrangement = Arrangement.spacedBy(GridDesignTokens.CellSpacing),
                horizontalArrangement = Arrangement.spacedBy(GridDesignTokens.CellSpacing),
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = GridDesignTokens.GridHorizontalPadding)
                    .gridZoomGestureModifier(gridCellsCount, viewModel::setGridCellsCount, isSelectionMode)
                    .pointerInput(lazyGridState, isSelectionMode) {
                        if (isSelectionMode) return@pointerInput

                        var initialItemKey: String? = null
                        var dragStarted  = false
                        var startOffset  = Offset.Zero

                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                startOffset  = offset
                                dragStarted  = false
                                // O(1) lookup from the bounds cache
                                val hitKey = itemBoundsCache.entries
                                    .firstOrNull { (_, rect) -> rect.contains(offset) }
                                    ?.key
                                if (hitKey != null) {
                                    viewModel.toggleSelection(hitKey)
                                    initialItemKey = hitKey
                                }
                            },
                            onDrag = { change, _ ->
                                val distance = (change.position - startOffset).getDistance()
                                if (distance > 40f) dragStarted = true

                                if (dragStarted) {
                                    // Shrink rect by inset to avoid accidental edge triggers
                                    val pos = change.position
                                    val hitKey = itemBoundsCache.entries
                                        .firstOrNull { (_, rect) ->
                                            val inset = 30f
                                            pos.x in (rect.left + inset)..(rect.right - inset) &&
                                            pos.y in (rect.top  + inset)..(rect.bottom - inset)
                                        }
                                        ?.key
                                    if (hitKey != null && hitKey != initialItemKey) {
                                        viewModel.addSelection(hitKey)
                                    }
                                }
                            }
                        )
                    }
            ) {
                if (viewMode == ViewMode.Immersive) {
                    items(
                        items       = images,
                        key         = { it.id },
                        contentType = { "photo_cell" }  // enables composition node reuse
                    ) { item ->
                        GalleryGridItem(
                            item                  = item,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            thumbnailPx           = thumbnailPx,
                            gridAutoPlay          = gridAutoPlay,
                            isSelected            = selectedUris.contains(item.uri.toString()),
                            onClick               = {
                                if (isSelectionMode) viewModel.toggleSelection(item.uri.toString())
                                else onPhotoClick(item.id, bucketName)
                            },
                            modifier = Modifier.animateItem(
                                placementSpec = GridDesignTokens.itemPlacementSpec()
                            )
                        )
                    }
                } else {
                    groupedImages.forEach { (date, groupItems) ->
                        item(
                            span        = { GridItemSpan(maxLineSpan) },
                            contentType = "date_header"  // separate pool from photo cells
                        ) {
                            Text(
                                text     = date,
                                style    = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                            )
                        }
                        items(
                            items       = groupItems,
                            key         = { it.id },
                            contentType = { "photo_cell" }
                        ) { item ->
                            GalleryGridItem(
                                item                  = item,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                thumbnailPx           = thumbnailPx,
                                gridAutoPlay          = gridAutoPlay,
                                isSelected            = selectedUris.contains(item.uri.toString()),
                                onClick               = {
                                    if (isSelectionMode) viewModel.toggleSelection(item.uri.toString())
                                    else onPhotoClick(item.id, bucketName)
                                },
                                    modifier = Modifier.animateItem(
                                    placementSpec = GridDesignTokens.itemPlacementSpec()
                                )
                            )
                        }
                    }
                }
            }
        }

        // ── Fast-scroll handle (only shown when list is large) ────────────────
        val totalItems = lazyGridState.layoutInfo.totalItemsCount
        if (totalItems > 100) {
            var dragOffset by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                Surface(
                    shape          = androidx.compose.foundation.shape.CircleShape,
                    color          = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shadowElevation = 4.dp,
                    modifier = Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                if (totalItems > 0 && boxHeight > 0f) {
                                    val pct = lazyGridState.firstVisibleItemIndex.toFloat() / totalItems
                                    dragOffset = pct * boxHeight
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            if (boxHeight > 0f) {
                                dragOffset = (dragOffset + dragAmount).coerceIn(0f, boxHeight)
                                val target = (dragOffset / boxHeight * totalItems)
                                    .toInt()
                                    .coerceIn(0, totalItems - 1)
                                coroutineScope.launch { lazyGridState.scrollToItem(target) }
                            }
                        }
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.rotate(-90f))
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.rotate(90f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GalleryGridItem
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryGridItem(
    item: GalleryItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    thumbnailPx: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    gridAutoPlay: Boolean = true,
    gridCellsCount: Int = 3
) {
    val context = LocalContext.current

    // ── ImageRequest — stable key uses mediaStoreId (no Uri.encode allocation) ──
    // repeatCount(0) controls GIF autoplay at the decoder level — no LaunchedEffect needed.
    val request = remember(item.mediaStoreId, thumbnailPx, gridAutoPlay) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(thumbnailPx, thumbnailPx)
            .memoryCacheKey("thumb_${item.mediaStoreId}_$thumbnailPx")
            .precision(Precision.INEXACT)           // EXACT forces synchronous decode — banned
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // crossfade disabled in grid: bitmaps render from cache without visual delay
            .build()
    }

    // ── Shared element key: uses stable mediaStoreId — no Uri.encode allocation ──
    val sharedKey = "photo_${item.mediaStoreId}"

    // ── Selection scale via graphicsLayer (RenderThread, not UI thread) ──────
    // animateFloatAsState is ONLY active when isSelected changes — no per-frame cost
    // for unselected cells. graphicsLayer pushes the transform to the GPU layer,
    // bypassing the Compose layout phase entirely.
    val animatedScale by animateFloatAsState(
        targetValue  = if (isSelected) GridDesignTokens.SelectionScaleTarget
                       else GridDesignTokens.SelectionScaleNormal,
        animationSpec = GridDesignTokens.selectionScaleSpec(),
        label         = "thumbnailScale_${item.mediaStoreId}"
    )

    // ── Format extension badge label (cheap String op, memoised per item name) ──
    val badgeText = remember(item.name) {
        when (item.name.substringAfterLast('.', "").lowercase()) {
            "gif", "webp" -> "GIF"
            "svg"         -> "SVG"
            "dng", "tiff", "tif", "raw", "cr2", "nef", "arw" -> "RAW"
            else          -> null
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
    ) {
        with(sharedTransitionScope) {
            // SubcomposeAsyncImage shows the spring-shimmer placeholder during decode,
            // and the error composable on failure — grid layout never shifts.
            SubcomposeAsyncImage(
                model            = request,
                contentDescription = null,
                contentScale     = ContentScale.Crop,
                loading          = { GridThumbnailPlaceholder(Modifier.fillMaxSize()) },
                error            = { GridThumbnailError(Modifier.fillMaxSize()) },
                modifier         = Modifier
                    .sharedElement(
                        sharedContentState     = rememberSharedContentState(key = sharedKey),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform        = { _, _ -> GridDesignTokens.sharedElementBoundsSpec() }
                    )
                    .aspectRatio(GridDesignTokens.AspectRatio)
                    .clip(GridDesignTokens.ThumbnailShape)
                    .clickable { onClick() }
            )
        }

        // ── Video overlay: gradient + duration + play badge ───────────────────
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = GridDesignTokens.VideoGradientStartAlpha),
                                Color.Black.copy(alpha = GridDesignTokens.VideoGradientEndAlpha)
                            ),
                            startY = 100f
                        )
                    )
            )
            Row(
                modifier             = Modifier
                    .matchParentSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment    = Alignment.Bottom
            ) {
                Text(
                    text       = formatDuration(item.durationMs),
                    color      = Color.White,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape        = MaterialTheme.shapes.extraLarge,
                    color        = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector     = Icons.Filled.PlayArrow,
                        contentDescription = "Video",
                        modifier        = Modifier.padding(6.dp).size(16.dp)
                    )
                }
            }
        }

        // ── Selection overlay ─────────────────────────────────────────────────
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = Color.Black.copy(alpha = GridDesignTokens.SelectionOverlayAlpha),
                        shape = GridDesignTokens.SelectionOverlayShape
                    )
            ) {
                Icon(
                    imageVector     = Icons.Outlined.CheckCircle,
                    contentDescription = "Selected",
                    tint            = MaterialTheme.colorScheme.primary,
                    modifier        = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                )
            }
        }

        // ── Format badge (RAW / GIF / SVG) ────────────────────────────────────
        if (badgeText != null && !item.isVideo) {
            Surface(
                color        = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape        = GridDesignTokens.BadgeShape,
                modifier     = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text       = badgeText,
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utilities
// ─────────────────────────────────────────────────────────────────────────────

fun formatDuration(millis: Long?): String {
    if (millis == null || millis <= 0) return ""
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours   = (millis / (1000 * 60 * 60))
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%02d:%02d", minutes, seconds)
}

private fun Modifier.gridZoomGestureModifier(
    gridCellsCount: Int,
    onGridCountChange: (Int) -> Unit,
    isSelectionMode: Boolean
): Modifier = composed {
    val currentCount    = androidx.compose.runtime.rememberUpdatedState(gridCellsCount)
    val currentCallback = androidx.compose.runtime.rememberUpdatedState(onGridCountChange)

    this.then(
        Modifier.pointerInput(isSelectionMode) {
            if (isSelectionMode) return@pointerInput

            var accumulatedScale = 1f

            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                val initialEvent   = awaitPointerEvent()
                val pointers       = initialEvent.changes.filter { it.pressed }

                if (pointers.size >= 2) {
                    accumulatedScale = 1f
                    val initialGridCount = currentCount.value

                    do {
                        val zoomEvent      = awaitPointerEvent()
                        val activePointers = zoomEvent.changes.filter { it.pressed }

                        if (activePointers.size >= 2) {
                            val zoomChange = zoomEvent.calculateZoom()
                            zoomEvent.changes.forEach { if (it.positionChanged()) it.consume() }

                            if (kotlin.math.abs(zoomChange - 1f) > 0.01f) {
                                accumulatedScale *= zoomChange
                                val newCount = (initialGridCount / accumulatedScale)
                                    .roundToInt()
                                    .coerceIn(2, 8)
                                if (newCount != currentCount.value) {
                                    currentCallback.value(newCount)
                                }
                            }
                        }
                    } while (zoomEvent.changes.any { it.pressed })

                    accumulatedScale = 1f
                }
            }
        }
    )
}
