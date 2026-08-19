package com.inferno.gallery.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.inferno.gallery.data.TimelineLayoutMode
import com.inferno.gallery.ui.GalleryItem
import com.inferno.gallery.ui.GalleryListItem
import com.inferno.gallery.ui.GalleryViewModel
import com.inferno.gallery.ui.ViewMode
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraSmall
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeSmall
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotonGrid(
    pagedMedia: LazyPagingItems<GalleryListItem>,
    lazyGridState: LazyGridState,
    gridCellsCount: Int,
    onGridCountChange: (Int) -> Unit,
    isSelectionMode: Boolean,
    selectedUris: Set<String>,
    onMediaClick: (GalleryItem) -> Unit,
    onMediaLongClick: (GalleryItem) -> Unit,
    viewMode: ViewMode,
    thumbnailCornerRadius: Float,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    timelineLayoutMode: TimelineLayoutMode = TimelineLayoutMode.STANDARD_GRID
) {
    val haptic = LocalHapticFeedback.current

    // Shared-element matching is suspended while the grid is scrolling — see
    // OptimizedThumbnailCell for why. One derived read per grid, not per cell.
    val gridSharedElementsActive by remember(lazyGridState) {
        derivedStateOf { !lazyGridState.isScrollInProgress }
    }

    when (timelineLayoutMode) {
        TimelineLayoutMode.STAGGERED_MASONRY -> {
            // ── Staggered Masonry Layout (True Aspect Ratio Columns) ──────
            val staggeredState = rememberLazyStaggeredGridState()
            val staggeredSharedElementsActive by remember(staggeredState) {
                derivedStateOf { !staggeredState.isScrollInProgress }
            }
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(gridCellsCount),
                state = staggeredState,
                contentPadding = contentPadding,
                verticalItemSpacing = 2.dp,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = modifier
                    .fillMaxSize()
                    .robustTwoFingerPinchZoom(
                        gridCellsCount = gridCellsCount,
                        onGridCountChange = onGridCountChange,
                        isSelectionMode = isSelectionMode,
                        haptic = haptic
                    )
            ) {
                items(
                    count = pagedMedia.itemCount,
                    key = { index ->
                        val item = if (index < pagedMedia.itemCount) pagedMedia.peek(index) else null
                        when (item) {
                            is GalleryListItem.Header -> "header_${item.title}"
                            is GalleryListItem.Item -> item.galleryItem.id
                            null -> "placeholder_$index"
                        }
                    },
                    contentType = { index ->
                        val item = if (index < pagedMedia.itemCount) pagedMedia.peek(index) else null
                        if (item is GalleryListItem.Header) "header" else "media"
                    },
                    span = { index ->
                        val listItem = if (index < pagedMedia.itemCount) pagedMedia.peek(index) else null
                        if (listItem is GalleryListItem.Header) StaggeredGridItemSpan.FullLine
                        else StaggeredGridItemSpan.SingleLane
                    }
                ) { index ->
                    val listItem = pagedMedia[index]
                    if (listItem is GalleryListItem.Header && viewMode != ViewMode.Immersive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = listItem.title.uppercase(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    letterSpacing = 0.8.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (listItem is GalleryListItem.Item) {
                        val item = listItem.galleryItem
                        val uriString = remember(item.id) { item.uri.toString() }
                        val masonryRatio = remember(item.id) {
                            val hash = (item.id.hashCode() and 0x7FFFFFFF) % 5
                            when (hash) {
                                0 -> 0.75f  // 3:4 portrait
                                1 -> 0.67f  // 2:3 tall portrait
                                2 -> 1.33f  // 4:3 landscape
                                3 -> 1.0f   // 1:1 square
                                else -> 0.85f
                            }
                        }

                        OptimizedThumbnailCell(
                            item = item,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = onMediaClick,
                            onLongClick = onMediaLongClick,
                            isSelected = selectedUris.contains(uriString),
                            gridCellsCount = gridCellsCount,
                            thumbnailCornerRadius = thumbnailCornerRadius,
                            aspectRatio = masonryRatio,
                            sharedElementsEnabled = staggeredSharedElementsActive
                        )
                    }
                }
            }
        }

        TimelineLayoutMode.EDITORIAL_MOSAIC -> {
            // ── Editorial Mosaic Layout (Smart Hero & Grid) ───────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(maxOf(3, gridCellsCount)),
                state = lazyGridState,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = modifier
                    .fillMaxSize()
                    .robustTwoFingerPinchZoom(
                        gridCellsCount = gridCellsCount,
                        onGridCountChange = onGridCountChange,
                        isSelectionMode = isSelectionMode,
                        haptic = haptic
                    )
                    .optimizedDragSelectGesture(
                        lazyGridState = lazyGridState,
                        pagedMedia = pagedMedia,
                        viewModel = viewModel,
                        hapticFeedback = haptic
                    )
            ) {
                items(
                    count = pagedMedia.itemCount,
                    key = { index ->
                        val item = if (index < pagedMedia.itemCount) pagedMedia.peek(index) else null
                        when (item) {
                            is GalleryListItem.Header -> "header_${item.title}"
                            is GalleryListItem.Item -> item.galleryItem.id
                            null -> "placeholder_$index"
                        }
                    },
                    contentType = { index ->
                        val item = if (index < pagedMedia.itemCount) pagedMedia.peek(index) else null
                        if (item is GalleryListItem.Header) "header" else "media"
                    },
                    span = { index ->
                        val listItem = if (index < pagedMedia.itemCount) (pagedMedia.peek(index) ?: pagedMedia[index]) else null
                        if (listItem is GalleryListItem.Header) {
                            GridItemSpan(maxLineSpan)
                        } else {
                            val isHero = (index % 7 == 1) || (index % 13 == 4)
                            if (isHero && gridCellsCount >= 3) GridItemSpan(2) else GridItemSpan(1)
                        }
                    }
                ) { index ->
                    val listItem = pagedMedia[index]
                    if (listItem is GalleryListItem.Header && viewMode != ViewMode.Immersive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = listItem.title.uppercase(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    letterSpacing = 0.8.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (listItem is GalleryListItem.Item) {
                        val item = listItem.galleryItem
                        val uriString = remember(item.id) { item.uri.toString() }
                        val isHero = (index % 7 == 1) || (index % 13 == 4)
                        val mosaicRatio = if (isHero && gridCellsCount >= 3) 1.4f else 1.0f

                        OptimizedThumbnailCell(
                            item = item,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = onMediaClick,
                            onLongClick = onMediaLongClick,
                            isSelected = selectedUris.contains(uriString),
                            gridCellsCount = gridCellsCount,
                            thumbnailCornerRadius = thumbnailCornerRadius,
                            aspectRatio = mosaicRatio,
                            sharedElementsEnabled = gridSharedElementsActive
                        )
                    }
                }
            }
        }

        TimelineLayoutMode.STANDARD_GRID -> {
            // ── Standard Uniform Square Grid ──────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridCellsCount),
                state = lazyGridState,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                modifier = modifier
                    .fillMaxSize()
                    .robustTwoFingerPinchZoom(
                        gridCellsCount = gridCellsCount,
                        onGridCountChange = onGridCountChange,
                        isSelectionMode = isSelectionMode,
                        haptic = haptic
                    )
                    .optimizedDragSelectGesture(
                        lazyGridState = lazyGridState,
                        pagedMedia = pagedMedia,
                        viewModel = viewModel,
                        hapticFeedback = haptic
                    )
            ) {
                items(
                    count = pagedMedia.itemCount,
                    key = { index ->
                        val item = if (index < pagedMedia.itemCount) pagedMedia.peek(index) else null
                        when (item) {
                            is GalleryListItem.Header -> "header_${item.title}"
                            is GalleryListItem.Item -> item.galleryItem.id
                            null -> "placeholder_$index"
                        }
                    },
                    contentType = { index ->
                        val item = if (index < pagedMedia.itemCount) pagedMedia.peek(index) else null
                        if (item is GalleryListItem.Header) "header" else "media"
                    },
                    span = { index ->
                        val listItem = if (index < pagedMedia.itemCount) pagedMedia.peek(index) else null
                        if (listItem is GalleryListItem.Header) GridItemSpan(maxLineSpan)
                        else GridItemSpan(1)
                    }
                ) { index ->
                    val listItem = pagedMedia[index]
                    if (listItem is GalleryListItem.Header && viewMode != ViewMode.Immersive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = listItem.title.uppercase(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    letterSpacing = 0.8.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (listItem is GalleryListItem.Item) {
                        val item = listItem.galleryItem
                        val uriString = remember(item.id) { item.uri.toString() }

                        OptimizedThumbnailCell(
                            item = item,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = onMediaClick,
                            onLongClick = onMediaLongClick,
                            isSelected = selectedUris.contains(uriString),
                            gridCellsCount = gridCellsCount,
                            thumbnailCornerRadius = thumbnailCornerRadius,
                            aspectRatio = 1.0f,
                            sharedElementsEnabled = gridSharedElementsActive
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun OptimizedThumbnailCell(
    item: GalleryItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (GalleryItem) -> Unit,
    onLongClick: ((GalleryItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    gridCellsCount: Int = 3,
    thumbnailCornerRadius: Float = 0f,
    aspectRatio: Float = 1.0f,
    sharedElementsEnabled: Boolean = true
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Size optimization based on grid cells — never request more than the rendered size
    val thumbSizePx = remember(gridCellsCount) {
        when (gridCellsCount) {
            1, 2 -> 512
            3 -> 320
            4 -> 240
            else -> 160
        }
    }

    // Unique per-media key — must match the prefetcher in GalleryScreen
    val cacheKey: String = remember(item.id) { thumbnailMemoryKey(item.id) }

    val request = remember(item.id, thumbSizePx) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(thumbSizePx, thumbSizePx)
            .precision(Precision.INEXACT)
            .memoryCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }

    // Always call animateFloatAsState unconditionally — Compose rules require this.
    // When not selected, target 1f so the animation is a no-op but state is valid.
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 0.88f else 1.0f,
        animationSpec = MotionTokens.bouncySpring(),
        label = "cellSelectionScale"
    )

    val cellModifier = if (selectionScale != 1.0f) {
        modifier.graphicsLayer {
            scaleX = selectionScale
            scaleY = selectionScale
        }
    } else {
        modifier
    }

    val cellShape = remember(thumbnailCornerRadius) {
        if (thumbnailCornerRadius > 0f) RoundedCornerShape(thumbnailCornerRadius.dp)
        else RoundedCornerShape(0.dp)
    }

    // Cache the aspectRatio + clip modifier — avoids allocation every recomposition
    val contentModifier = remember(thumbnailCornerRadius, aspectRatio) {
        if (thumbnailCornerRadius > 0f) {
            Modifier
                .aspectRatio(aspectRatio)
                .clip(cellShape)
        } else {
            Modifier.aspectRatio(aspectRatio)
        }
    }

    // Every sharedBounds participant forces the SharedTransitionLayout's
    // look-ahead pass to measure it — with one per cell the whole grid is
    // double-measured every scroll frame. A shared-element transition can
    // only ever start from a cell while the grid is idle (scrolling consumes
    // taps), so the modifier is attached only when scrolling has stopped.
    // It re-attaches before any tap can fire, keeping grid→detail morphs intact.
    val sharedTransitionModifier = if (sharedElementsEnabled) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "photo_${item.uri}"),
                animatedVisibilityScope = animatedVisibilityScope,
                enter = CellEnterFade,
                exit = CellExitFade,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                clipInOverlayDuringTransition = OverlayClip(cellShape),
                boundsTransform = { _, _ -> MotionTokens.sharedElementSpring() }
            )
        }
    } else {
        Modifier
    }

    Box(modifier = cellModifier) {
        Box(
            modifier = contentModifier
                .then(sharedTransitionModifier)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(
                    indication = null,
                    interactionSource = null,
                    onClick = { onClick(item) },
                    onLongClick = onLongClick?.let {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            it(item)
                        }
                    }
                )
        ) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Expressive Video duration badge
        if (item.isVideo) {
            val fontSize = when (gridCellsCount) {
                1, 2, 3 -> 12.sp
                4 -> 11.sp
                else -> 10.sp
            }
            Surface(
                shape = ShapeExtraSmall,
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.80f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.durationMs?.let { formatDuration(it) } ?: "0:00",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        } else {
            // RAW / GIF / PANO badge
            val badgeText = remember(item.name) {
                when {
                    item.name.endsWith(".dng", true) || item.name.endsWith(".raw", true) || item.name.endsWith(".cr2", true) || item.name.endsWith(".nef", true) || item.name.endsWith(".arw", true) -> "RAW"
                    item.name.endsWith(".gif", true) -> "GIF"
                    item.name.contains("PANO", true) || item.name.contains("PANORAMA", true) -> "PANO"
                    else -> null
                }
            }

            if (badgeText != null) {
                Surface(
                    shape = ShapeExtraSmall,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.82f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Selection overlay checkmark badge with solid white contrast border
        if (isSelected) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                border = BorderStroke(2.dp, Color.White),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                        contentDescription = "Selected",
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

// ── Shared thumbnail cache key ────────────────────────────────────────────────

/**
 * Memory-cache key for grid thumbnails. Unique per media id (unlike a hashCode,
 * which can collide across a large library and serve the wrong thumbnail).
 * Must stay in sync with the prefetcher in GalleryScreen.
 */
internal fun thumbnailMemoryKey(id: String): String = "t_$id"

// Shared-element fade specs — hoisted so cells don't allocate them per composition
private val CellEnterFade = fadeIn(animationSpec = androidx.compose.animation.core.tween(150))
private val CellExitFade = fadeOut(animationSpec = androidx.compose.animation.core.tween(150))

// ── Gestures ──────────────────────────────────────────────────────────────────

/**
 * Clean, high-performance two-finger pinch-to-zoom detector.
 * Only activates when 2 fingers touch simultaneously so single-finger scrolling
 * and drag selection are never blocked or jittered.
 */
private fun Modifier.robustTwoFingerPinchZoom(
    gridCellsCount: Int,
    onGridCountChange: (Int) -> Unit,
    isSelectionMode: Boolean,
    haptic: HapticFeedback
): Modifier = this.pointerInput(gridCellsCount, isSelectionMode) {
    if (isSelectionMode) return@pointerInput

    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

        var initialDistance = -1f
        var lastChangeTime = 0L

        do {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            // Track the first two pressed pointers without allocating a list —
            // this loop runs for every motion event of every gesture.
            val changes = event.changes
            var p1x = 0f; var p1y = 0f; var p2x = 0f; var p2y = 0f
            var pressedCount = 0
            for (c in changes) {
                if (c.pressed) {
                    if (pressedCount == 0) {
                        p1x = c.position.x; p1y = c.position.y; pressedCount = 1
                    } else {
                        p2x = c.position.x; p2y = c.position.y; pressedCount = 2
                        break
                    }
                }
            }

            if (pressedCount == 2) {
                val currentDistance = kotlin.math.hypot(p1x - p2x, p1y - p2y)

                if (initialDistance <= 0f) {
                    initialDistance = currentDistance
                } else {
                    val scale = currentDistance / initialDistance
                    val now = System.currentTimeMillis()

                    // Zoom IN (Fingers spreading -> fewer columns, e.g. 3 -> 2)
                    if (scale > 1.22f && now - lastChangeTime > 280L) {
                        if (gridCellsCount > 1) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGridCountChange(gridCellsCount - 1)
                            lastChangeTime = now
                            initialDistance = currentDistance
                        }
                    }
                    // Zoom OUT (Fingers pinching closer -> more columns, e.g. 3 -> 4)
                    else if (scale < 0.80f && now - lastChangeTime > 280L) {
                        if (gridCellsCount < 6) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGridCountChange(gridCellsCount + 1)
                            lastChangeTime = now
                            initialDistance = currentDistance
                        }
                    }
                }
            } else {
                initialDistance = -1f
            }
        } while (event.changes.any { it.pressed })
    }
}

private fun Modifier.optimizedDragSelectGesture(
    lazyGridState: LazyGridState,
    pagedMedia: LazyPagingItems<GalleryListItem>,
    viewModel: GalleryViewModel,
    hapticFeedback: HapticFeedback
): Modifier = this.pointerInput(Unit) {
    var dragStartIndex = -1
    var lastSelectedIndex = -1

    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            val hitItem = findItemAtOffset(lazyGridState, offset)
            if (hitItem != null) {
                val listItem = if (hitItem < pagedMedia.itemCount) (pagedMedia.peek(hitItem) ?: pagedMedia[hitItem]) else null
                if (listItem is GalleryListItem.Item) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    val uri = listItem.galleryItem.uri.toString()
                    val isSelecting = !viewModel.selectedUris.value.contains(uri)
                    viewModel.startDragSelection(uri, isSelecting)
                    dragStartIndex = hitItem
                    lastSelectedIndex = hitItem
                }
            }
        },
        onDrag = { change, _ ->
            change.consume()
            val currentHit = findItemAtOffset(lazyGridState, change.position)
            if (currentHit != null && currentHit != lastSelectedIndex && dragStartIndex != -1) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val start = minOf(dragStartIndex, currentHit)
                val end = maxOf(dragStartIndex, currentHit)

                val uris = mutableSetOf<String>()
                for (i in start..end) {
                    if (i < pagedMedia.itemCount) {
                        val item = pagedMedia.peek(i) ?: pagedMedia[i]
                        if (item is GalleryListItem.Item) {
                            uris.add(item.galleryItem.uri.toString())
                        }
                    }
                }
                viewModel.updateDragSelection(uris)
                lastSelectedIndex = currentHit
            }
        },
        onDragEnd = {
            dragStartIndex = -1
            lastSelectedIndex = -1
            viewModel.endDragSelection()
        },
        onDragCancel = {
            dragStartIndex = -1
            lastSelectedIndex = -1
            viewModel.endDragSelection()
        }
    )
}

private fun findItemAtOffset(lazyGridState: LazyGridState, offset: Offset): Int? {
    val layoutInfo = lazyGridState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    for (item in visibleItems) {
        val itemOffset = item.offset
        val itemSize = item.size
        if (offset.x >= itemOffset.x && offset.x <= itemOffset.x + itemSize.width &&
            offset.y >= itemOffset.y && offset.y <= itemOffset.y + itemSize.height
        ) {
            return item.index
        }
    }
    return null
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
