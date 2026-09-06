package com.inferno.gallery.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.animation.fadeIn
import com.inferno.gallery.ui.utils.tick
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import coil3.size.Precision
import com.inferno.gallery.R
import com.inferno.gallery.data.TimelineLayoutMode
import com.inferno.gallery.ui.GalleryItem
import com.inferno.gallery.ui.GalleryListItem
import com.inferno.gallery.ui.GalleryViewModel
import com.inferno.gallery.ui.ViewMode
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraSmall
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.TimelineDateHeaderStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/**
 * Expressive, buttery-smooth fling behavior for Photon Gallery grids.
 *
 * Employs standard Android spline-based decay for natural tactile momentum,
 * but cleanly cuts off when velocity drops below the subpixel crawl threshold (~36 px/s).
 * This eliminates the 0px/1px alternating quantisation judder that occurs during the tail
 * end of Compose fling deceleration, ensuring a crisp, smooth landing.
 */
@Composable
fun rememberExpressiveGridFlingBehavior(): FlingBehavior {
    val density = LocalDensity.current
    val splineDecay = remember(density) { splineBasedDecay<Float>(density) }

    return remember(splineDecay) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                if (kotlin.math.abs(initialVelocity) <= 16f) return 0f

                var lastValue = 0f
                var velocityLeft = initialVelocity

                AnimationState(
                    initialValue = 0f,
                    initialVelocity = initialVelocity
                ).animateDecay(splineDecay) {
                    val delta = value - lastValue
                    val consumed = scrollBy(delta)
                    lastValue = value
                    velocityLeft = this.velocity

                    if (kotlin.math.abs(delta - consumed) > 0.5f) {
                        this.cancelAnimation()
                    }

                    // Clean deceleration cutoff: avoids subpixel integer quantisation
                    // crawl (< 36 px/s = 0.3 px/frame at 120Hz) which causes stutter.
                    if (kotlin.math.abs(this.velocity) < 36f) {
                        this.cancelAnimation()
                    }
                }
                return velocityLeft
            }
        }
    }
}

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
    onMediaLongClick: (GalleryItem) -> Unit = {},
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
    val coroutineScope = rememberCoroutineScope()
    val gridFlingBehavior = rememberExpressiveGridFlingBehavior()

    // Hoisted static image vectors resolved once per grid, avoiding per-cell recomposition lookups
    val videoIconVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow)
    val checkIconVector = ImageVector.vectorResource(R.drawable.ic_ms_check)

    // ── Live Pinch-to-Zoom Spring Animation & Scale State ───────────────────────
    var isPinching by remember { mutableStateOf(false) }
    var livePinchScale by remember { mutableFloatStateOf(1f) }
    var pinchCentroid by remember { mutableStateOf<Offset?>(null) }
    val columnChangeSettleAnim = remember { Animatable(1f) }

    // When gridCellsCount changes, trigger a subtle, buttery Material 3 Expressive spring settle
    LaunchedEffect(gridCellsCount) {
        if (!isPinching) {
            columnChangeSettleAnim.snapTo(1.05f)
            columnChangeSettleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val basePinchModifier = Modifier
        .fillMaxSize()
        .smoothGridPinchZoom(
            gridCellsCount = gridCellsCount,
            onGridCountChange = onGridCountChange,
            isSelectionMode = isSelectionMode,
            haptic = haptic,
            onPinchScaleChange = { scale, centroid ->
                isPinching = true
                livePinchScale = scale.coerceIn(0.70f, 1.40f)
                pinchCentroid = centroid
            },
            onPinchEnd = {
                coroutineScope.launch {
                    animate(
                        initialValue = livePinchScale,
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { value, _ ->
                        livePinchScale = value
                    }
                    isPinching = false
                }
            }
        )
        .graphicsLayer {
            val scale = if (isPinching || livePinchScale != 1f) livePinchScale else columnChangeSettleAnim.value
            scaleX = scale
            scaleY = scale
            pinchCentroid?.let { c ->
                if (size.width > 0f && size.height > 0f) {
                    transformOrigin = TransformOrigin(
                        pivotFractionX = (c.x / size.width).coerceIn(0f, 1f),
                        pivotFractionY = (c.y / size.height).coerceIn(0f, 1f)
                    )
                }
            }
        }

    val staggeredState = rememberLazyStaggeredGridState()
    val isScrolled by remember(timelineLayoutMode) {
        derivedStateOf {
            if (timelineLayoutMode == TimelineLayoutMode.STAGGERED_MASONRY) {
                staggeredState.firstVisibleItemIndex > 0 || staggeredState.firstVisibleItemScrollOffset > 8
            } else {
                lazyGridState.firstVisibleItemIndex > 0 || lazyGridState.firstVisibleItemScrollOffset > 8
            }
        }
    }

    LaunchedEffect(isScrolled) {
        viewModel.setTopBarCollapsed(isScrolled)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = basePinchModifier) {
            when (timelineLayoutMode) {
                TimelineLayoutMode.STAGGERED_MASONRY -> {
                    // ── Staggered Masonry Layout (True Aspect Ratio Columns) ──────
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(gridCellsCount),
                        state = staggeredState,
                        flingBehavior = gridFlingBehavior,
                        contentPadding = contentPadding,
                        verticalItemSpacing = 2.dp,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .staggeredGridDragSelectGesture(
                                staggeredState = staggeredState,
                                pagedMedia = pagedMedia,
                                viewModel = viewModel,
                                hapticFeedback = haptic,
                                coroutineScope = coroutineScope
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
                            TimelineSectionHeader(title = listItem.title)
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
                            // derivedStateOf: only recompose this cell when its own selection changes
                            val isSelected by remember(uriString, selectedUris) {
                                derivedStateOf { selectedUris.contains(uriString) }
                            }

                            OptimizedThumbnailCell(
                                modifier = Modifier,
                                item = item,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onClick = onMediaClick,
                                isSelected = isSelected,
                                gridCellsCount = gridCellsCount,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                aspectRatio = masonryRatio,
                                videoIconVector = videoIconVector,
                                checkIconVector = checkIconVector
                            )
                        } else {
                            val placeholderShape = remember(thumbnailCornerRadius) {
                                if (thumbnailCornerRadius > 0f) RoundedCornerShape(thumbnailCornerRadius.dp)
                                else RoundedCornerShape(0.dp)
                            }
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.0f)
                                    .clip(placeholderShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    flingBehavior = gridFlingBehavior,
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .gridDragSelectGesture(
                            lazyGridState = lazyGridState,
                            pagedMedia = pagedMedia,
                            viewModel = viewModel,
                            hapticFeedback = haptic,
                            coroutineScope = coroutineScope
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
                            TimelineSectionHeader(title = listItem.title)
                        } else if (listItem is GalleryListItem.Item) {
                            val item = listItem.galleryItem
                            val uriString = remember(item.id) { item.uri.toString() }
                            val isHero = (index % 7 == 1) || (index % 13 == 4)
                            val mosaicRatio = if (isHero && gridCellsCount >= 3) 1.4f else 1.0f
                            // derivedStateOf: only recompose this cell when its own selection changes
                            val isSelected by remember(uriString, selectedUris) {
                                derivedStateOf { selectedUris.contains(uriString) }
                            }

                            OptimizedThumbnailCell(
                                modifier = Modifier,
                                item = item,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onClick = onMediaClick,
                                isSelected = isSelected,
                                gridCellsCount = gridCellsCount,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                aspectRatio = mosaicRatio,
                                videoIconVector = videoIconVector,
                                checkIconVector = checkIconVector
                            )
                        } else {
                            val isHero = (index % 7 == 1) || (index % 13 == 4)
                            val mosaicRatio = if (isHero && gridCellsCount >= 3) 1.4f else 1.0f
                            val placeholderShape = remember(thumbnailCornerRadius) {
                                if (thumbnailCornerRadius > 0f) RoundedCornerShape(thumbnailCornerRadius.dp)
                                else RoundedCornerShape(0.dp)
                            }
                            Box(
                                modifier = Modifier
                                    .aspectRatio(mosaicRatio)
                                    .clip(placeholderShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    flingBehavior = gridFlingBehavior,
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .gridDragSelectGesture(
                            lazyGridState = lazyGridState,
                            pagedMedia = pagedMedia,
                            viewModel = viewModel,
                            hapticFeedback = haptic,
                            coroutineScope = coroutineScope
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
                            TimelineSectionHeader(title = listItem.title)
                        } else if (listItem is GalleryListItem.Item) {
                            val item = listItem.galleryItem
                            val uriString = remember(item.id) { item.uri.toString() }
                            // derivedStateOf: this cell only recomposes when ITS OWN selection state changes,
                            // not when any other URI is toggled. Eliminates N-cell cascade recompositions.
                            val isSelected by remember(uriString, selectedUris) {
                                derivedStateOf { selectedUris.contains(uriString) }
                            }

                            OptimizedThumbnailCell(
                                modifier = Modifier,
                                item = item,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onClick = onMediaClick,
                                isSelected = isSelected,
                                gridCellsCount = gridCellsCount,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                aspectRatio = 1.0f,
                                videoIconVector = videoIconVector,
                                checkIconVector = checkIconVector
                            )
                        } else {
                            val placeholderShape = remember(thumbnailCornerRadius) {
                                if (thumbnailCornerRadius > 0f) RoundedCornerShape(thumbnailCornerRadius.dp)
                                else RoundedCornerShape(0.dp)
                            }
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.0f)
                                    .clip(placeholderShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }
            }
        }
    }

        // ── Floating Material 3 Expressive Column Count HUD Badge ─────────────
        var showHudPill by remember { mutableStateOf(false) }
        LaunchedEffect(isPinching, gridCellsCount) {
            if (isPinching) {
                showHudPill = true
            } else {
                kotlinx.coroutines.delay(650)
                showHudPill = false
            }
        }

        AnimatedVisibility(
            visible = showHudPill,
            enter = fadeIn(MotionTokens.snappySpring()) + scaleIn(MotionTokens.bouncySpring()),
            exit = fadeOut(MotionTokens.snappySpring()) + scaleOut(MotionTokens.snappySpring()),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "$gridCellsCount columns",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Editorial Leica-style date section header featuring a condensed Google Sans Flex date
 * and an ultra-subtle horizontal gradient hairline accent rule.
 */
@Composable
fun TimelineSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, top = 26.dp, bottom = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title.uppercase(),
            style = TimelineDateHeaderStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val SelectionBorderStroke = BorderStroke(2.dp, Color.White)

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun OptimizedThumbnailCell(
    item: GalleryItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (GalleryItem) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: ((GalleryItem) -> Unit)? = null,
    isSelected: Boolean = false,
    gridCellsCount: Int = 3,
    thumbnailCornerRadius: Float = 0f,
    aspectRatio: Float = 1.0f,
    videoIconVector: ImageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
    checkIconVector: ImageVector = ImageVector.vectorResource(R.drawable.ic_ms_check)
) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Hoisted click handlers
    val clickHandler = remember(item, onClick) { { onClick(item) } }
    val longClickHandler = remember(item, onLongClick) {
        if (onLongClick != null) {
            {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onLongClick(item)
            }
        } else null
    }

    // Only allocate and tick selection animation when item is selected
    val cellModifier = if (isSelected) {
        val selectionScale by animateFloatAsState(
            targetValue = 0.88f,
            animationSpec = MotionTokens.bouncySpring(),
            label = "cellSelectionScale"
        )
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

    val contentModifier = remember(thumbnailCornerRadius, aspectRatio) {
        if (thumbnailCornerRadius > 0f) {
            Modifier
                .aspectRatio(aspectRatio)
                .clip(cellShape)
        } else {
            Modifier.aspectRatio(aspectRatio)
        }
    }

    // Shared bounds transition is active for standard grid densities (1-4 columns).
    // For dense grids (5-8 columns: 70-120+ visible tiles), bypassing sharedBounds
    // eliminates ApproachLayoutModifierNode evaluation overhead on every scroll tick.
    val sharedTransitionModifier = if (gridCellsCount <= 4) {
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
        val clickModifier = if (onLongClick != null) {
            Modifier.combinedClickable(
                indication = null,
                interactionSource = null,
                onClick = clickHandler,
                onLongClick = longClickHandler
            )
        } else {
            Modifier.clickable(
                indication = null,
                interactionSource = null,
                onClick = clickHandler
            )
        }

        Box(
            modifier = contentModifier
                .then(sharedTransitionModifier)
                .then(clickModifier)
        ) {
            PhotonGridThumbnail(
                item = item,
                gridCellsCount = gridCellsCount,
                videoIconVector = videoIconVector
            )
        }

        // Selection overlay checkmark badge with solid white contrast border
        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(SelectionBorderStroke, CircleShape)
            ) {
                Icon(
                    imageVector = checkIconVector,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

// ── Shared thumbnail cache key ────────────────────────────────────────────────

internal fun thumbnailMemoryKey(id: String): String = "t_$id"

// Shared-element fade specs — hoisted so cells don't allocate them per composition
private val CellEnterFade = fadeIn(animationSpec = MotionTokens.fastEffectsSpec())
private val CellExitFade = fadeOut(animationSpec = MotionTokens.fastEffectsSpec())

// ── Gestures ──────────────────────────────────────────────────────────────────

/**
 * Non-conflicting, smooth two-finger pinch-to-zoom gesture detector for Photon Gallery grids.
 *
 * Never blocks or delays single-finger scrolling, fast flings, or drag-selection.
 * Only engages when two fingers deliberately move apart (zoom in) or together (zoom out)
 * beyond the touch slop threshold.
 */
private fun Modifier.smoothGridPinchZoom(
    gridCellsCount: Int,
    onGridCountChange: (Int) -> Unit,
    isSelectionMode: Boolean,
    haptic: HapticFeedback,
    onPinchScaleChange: (scale: Float, centroid: Offset) -> Unit,
    onPinchEnd: () -> Unit
): Modifier = this.pointerInput(gridCellsCount, isSelectionMode) {
    if (isSelectionMode) return@pointerInput

    val touchSlop = viewConfiguration.touchSlop

    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)

        var initialDistance = -1f
        var pinchActive = false
        var pinchEverActivated = false
        var lastChangeTime = 0L

        do {
            val event = awaitPointerEvent(pass = PointerEventPass.Main)
            val pressed = event.changes.filter { it.pressed }

            if (pressed.size >= 2) {
                val p1 = pressed[0].position
                val p2 = pressed[1].position
                val currentDistance = kotlin.math.hypot(p1.x - p2.x, p1.y - p2.y)
                val currentCentroid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                if (initialDistance <= 0f) {
                    initialDistance = currentDistance
                } else {
                    val distanceDiff = kotlin.math.abs(currentDistance - initialDistance)
                    val rawScale = currentDistance / initialDistance

                    // Slop threshold: require noticeable finger movement before activating pinch
                    if (!pinchActive && distanceDiff > touchSlop * 1.5f && (rawScale > 1.06f || rawScale < 0.94f)) {
                        pinchActive = true
                        pinchEverActivated = true
                    }

                    if (pinchActive) {
                        pressed.forEach { it.consume() }
                        onPinchScaleChange(rawScale, currentCentroid)

                        val now = System.currentTimeMillis()
                        // Zoom IN: spreading fingers -> fewer columns (e.g. 4 -> 3)
                        if (rawScale > 1.22f && now - lastChangeTime > 240L) {
                            if (gridCellsCount > 1) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                val oldColumns = gridCellsCount
                                val newColumns = gridCellsCount - 1
                                onGridCountChange(newColumns)
                                lastChangeTime = now
                                initialDistance = currentDistance
                                val ratio = oldColumns.toFloat() / newColumns.toFloat()
                                val compensatingScale = (rawScale / ratio).coerceIn(0.85f, 1.20f)
                                onPinchScaleChange(compensatingScale, currentCentroid)
                            }
                        }
                        // Zoom OUT: pinching fingers -> more columns (e.g. 3 -> 4)
                        else if (rawScale < 0.80f && now - lastChangeTime > 240L) {
                            if (gridCellsCount < 8) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                val oldColumns = gridCellsCount
                                val newColumns = gridCellsCount + 1
                                onGridCountChange(newColumns)
                                lastChangeTime = now
                                initialDistance = currentDistance
                                val ratio = oldColumns.toFloat() / newColumns.toFloat()
                                val compensatingScale = (rawScale / ratio).coerceIn(0.85f, 1.20f)
                                onPinchScaleChange(compensatingScale, currentCentroid)
                            }
                        }
                    }
                }
            } else {
                if (pinchActive) {
                    event.changes.forEach { it.consume() }
                }
                pinchActive = false
                initialDistance = -1f
            }
        } while (event.changes.any { it.pressed })

        if (pinchEverActivated) {
            onPinchEnd()
        }
    }
}

/**
 * Unified Press-and-Hold & Drag-to-Select gesture detector for LazyVerticalGrid.
 * Supports instant hold-to-select, multi-item continuous drag selection, and smooth edge auto-scrolling.
 */
private fun Modifier.gridDragSelectGesture(
    lazyGridState: LazyGridState,
    pagedMedia: LazyPagingItems<GalleryListItem>,
    viewModel: GalleryViewModel,
    hapticFeedback: HapticFeedback,
    coroutineScope: CoroutineScope
): Modifier = this.pointerInput(lazyGridState) {
    var dragStartIndex = -1
    var lastHitIndex = -1
    var currentDragPosition: Offset? = null // Plain var — never observed by Composable, no snapshot overhead
    var autoScrollJob: Job? = null

    val autoScrollThresholdPx = with(density) { 72.dp.toPx() }
    val maxScrollSpeedPx = with(density) { 24.dp.toPx() }

    fun updateSelectionForIndex(targetIndex: Int) {
        if (dragStartIndex == -1 || targetIndex == -1) return
        val start = minOf(dragStartIndex, targetIndex)
        val end = maxOf(dragStartIndex, targetIndex)
        val count = pagedMedia.itemCount
        if (count == 0) return

        val uris = mutableSetOf<String>()
        val clampedStart = start.coerceIn(0, count - 1)
        val clampedEnd = end.coerceIn(0, count - 1)
        for (i in clampedStart..clampedEnd) {
            val item = pagedMedia.peek(i)
            if (item is GalleryListItem.Item) {
                uris.add(item.galleryItem.uri.toString())
            }
        }
        viewModel.updateDragSelection(uris)
    }

    fun startAutoScrollIfNeeded() {
        if (autoScrollJob?.isActive == true) return
        autoScrollJob = coroutineScope.launch {
            while (isActive && dragStartIndex != -1) {
                val pos = currentDragPosition ?: break
                val viewportHeight = lazyGridState.layoutInfo.viewportSize.height.toFloat()
                if (viewportHeight <= 0f) break

                val scrollDelta = when {
                    pos.y < autoScrollThresholdPx -> {
                        val factor = ((autoScrollThresholdPx - pos.y) / autoScrollThresholdPx).coerceIn(0f, 1f)
                        -maxScrollSpeedPx * factor
                    }
                    pos.y > (viewportHeight - autoScrollThresholdPx) -> {
                        val factor = ((pos.y - (viewportHeight - autoScrollThresholdPx)) / autoScrollThresholdPx).coerceIn(0f, 1f)
                        maxScrollSpeedPx * factor
                    }
                    else -> 0f
                }

                if (scrollDelta != 0f) {
                    lazyGridState.scrollBy(scrollDelta)
                    val newHit = findItemAtOffset(lazyGridState, pos)
                    if (newHit != null && newHit != lastHitIndex) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastHitIndex = newHit
                        updateSelectionForIndex(newHit)
                    }
                    delay(16L) // Only poll at 60fps when actively scrolling the edge
                } else {
                    delay(32L) // Idle: check at 30fps to avoid wasting coroutine wake-ups
                }
            }
        }
    }

    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            val hitItem = findItemAtOffset(lazyGridState, offset)
            if (hitItem != null && hitItem < pagedMedia.itemCount) {
                val listItem = pagedMedia.peek(hitItem)
                if (listItem is GalleryListItem.Item) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    val uri = listItem.galleryItem.uri.toString()
                    val isSelecting = !viewModel.selectedUris.value.contains(uri)
                    viewModel.startDragSelection(uri, isSelecting)
                    dragStartIndex = hitItem
                    lastHitIndex = hitItem
                    currentDragPosition = offset
                    startAutoScrollIfNeeded()
                }
            }
        },
        onDrag = { change, _ ->
            change.consume()
            val pos = change.position
            currentDragPosition = pos
            val currentHit = findItemAtOffset(lazyGridState, pos)
            if (currentHit != null && currentHit != lastHitIndex && dragStartIndex != -1) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastHitIndex = currentHit
                updateSelectionForIndex(currentHit)
            }
            startAutoScrollIfNeeded()
        },
        onDragEnd = {
            autoScrollJob?.cancel()
            autoScrollJob = null
            currentDragPosition = null
            dragStartIndex = -1
            lastHitIndex = -1
            viewModel.endDragSelection()
        },
        onDragCancel = {
            autoScrollJob?.cancel()
            autoScrollJob = null
            currentDragPosition = null
            dragStartIndex = -1
            lastHitIndex = -1
            viewModel.endDragSelection()
        }
    )
}

/**
 * Unified Press-and-Hold & Drag-to-Select gesture detector for LazyVerticalStaggeredGrid.
 */
private fun Modifier.staggeredGridDragSelectGesture(
    staggeredState: LazyStaggeredGridState,
    pagedMedia: LazyPagingItems<GalleryListItem>,
    viewModel: GalleryViewModel,
    hapticFeedback: HapticFeedback,
    coroutineScope: CoroutineScope
): Modifier = this.pointerInput(staggeredState) {
    var dragStartIndex = -1
    var lastHitIndex = -1
    var currentDragPosition: Offset? = null // Plain var — never observed by Composable, no snapshot overhead
    var autoScrollJob: Job? = null

    val autoScrollThresholdPx = with(density) { 72.dp.toPx() }
    val maxScrollSpeedPx = with(density) { 24.dp.toPx() }

    fun updateSelectionForIndex(targetIndex: Int) {
        if (dragStartIndex == -1 || targetIndex == -1) return
        val start = minOf(dragStartIndex, targetIndex)
        val end = maxOf(dragStartIndex, targetIndex)
        val count = pagedMedia.itemCount
        if (count == 0) return

        val uris = mutableSetOf<String>()
        val clampedStart = start.coerceIn(0, count - 1)
        val clampedEnd = end.coerceIn(0, count - 1)
        for (i in clampedStart..clampedEnd) {
            val item = pagedMedia.peek(i)
            if (item is GalleryListItem.Item) {
                uris.add(item.galleryItem.uri.toString())
            }
        }
        viewModel.updateDragSelection(uris)
    }

    fun startAutoScrollIfNeeded() {
        if (autoScrollJob?.isActive == true) return
        autoScrollJob = coroutineScope.launch {
            while (isActive && dragStartIndex != -1) {
                val pos = currentDragPosition ?: break
                val viewportHeight = staggeredState.layoutInfo.viewportSize.height.toFloat()
                if (viewportHeight <= 0f) break

                val scrollDelta = when {
                    pos.y < autoScrollThresholdPx -> {
                        val factor = ((autoScrollThresholdPx - pos.y) / autoScrollThresholdPx).coerceIn(0f, 1f)
                        -maxScrollSpeedPx * factor
                    }
                    pos.y > (viewportHeight - autoScrollThresholdPx) -> {
                        val factor = ((pos.y - (viewportHeight - autoScrollThresholdPx)) / autoScrollThresholdPx).coerceIn(0f, 1f)
                        maxScrollSpeedPx * factor
                    }
                    else -> 0f
                }

                if (scrollDelta != 0f) {
                    staggeredState.scrollBy(scrollDelta)
                    val newHit = findStaggeredItemAtOffset(staggeredState, pos)
                    if (newHit != null && newHit != lastHitIndex) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastHitIndex = newHit
                        updateSelectionForIndex(newHit)
                    }
                    delay(16L) // Only poll at 60fps when actively scrolling the edge
                } else {
                    delay(32L) // Idle: check at 30fps to avoid wasting coroutine wake-ups
                }
            }
        }
    }

    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            val hitItem = findStaggeredItemAtOffset(staggeredState, offset)
            if (hitItem != null && hitItem < pagedMedia.itemCount) {
                val listItem = pagedMedia.peek(hitItem)
                if (listItem is GalleryListItem.Item) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    val uri = listItem.galleryItem.uri.toString()
                    val isSelecting = !viewModel.selectedUris.value.contains(uri)
                    viewModel.startDragSelection(uri, isSelecting)
                    dragStartIndex = hitItem
                    lastHitIndex = hitItem
                    currentDragPosition = offset
                    startAutoScrollIfNeeded()
                }
            }
        },
        onDrag = { change, _ ->
            change.consume()
            val pos = change.position
            currentDragPosition = pos
            val currentHit = findStaggeredItemAtOffset(staggeredState, pos)
            if (currentHit != null && currentHit != lastHitIndex && dragStartIndex != -1) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastHitIndex = currentHit
                updateSelectionForIndex(currentHit)
            }
            startAutoScrollIfNeeded()
        },
        onDragEnd = {
            autoScrollJob?.cancel()
            autoScrollJob = null
            currentDragPosition = null
            dragStartIndex = -1
            lastHitIndex = -1
            viewModel.endDragSelection()
        },
        onDragCancel = {
            autoScrollJob?.cancel()
            autoScrollJob = null
            currentDragPosition = null
            dragStartIndex = -1
            lastHitIndex = -1
            viewModel.endDragSelection()
        }
    )
}

private fun findItemAtOffset(lazyGridState: LazyGridState, offset: Offset): Int? {
    val layoutInfo = lazyGridState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val adjustedY = offset.y - layoutInfo.beforeContentPadding
    val adjustedX = offset.x

    // Exact bounding box test
    for (item in visibleItems) {
        val itemOffset = item.offset
        val itemSize = item.size
        if (adjustedX >= itemOffset.x && adjustedX <= itemOffset.x + itemSize.width &&
            adjustedY >= itemOffset.y && adjustedY <= itemOffset.y + itemSize.height
        ) {
            return item.index
        }
    }

    // Row-aligned fallback test
    val matchingRow = visibleItems.filter { item ->
        adjustedY >= item.offset.y && adjustedY <= item.offset.y + item.size.height
    }
    if (matchingRow.isNotEmpty()) {
        return matchingRow.minByOrNull { item ->
            val centerX = item.offset.x + item.size.width / 2f
            kotlin.math.abs(adjustedX - centerX)
        }?.index
    }

    return null
}

private fun findStaggeredItemAtOffset(staggeredState: LazyStaggeredGridState, offset: Offset): Int? {
    val layoutInfo = staggeredState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val adjustedY = offset.y - layoutInfo.beforeContentPadding
    val adjustedX = offset.x

    // Exact bounding box test
    for (item in visibleItems) {
        val itemOffset = item.offset
        val itemSize = item.size
        if (adjustedX >= itemOffset.x && adjustedX <= itemOffset.x + itemSize.width &&
            adjustedY >= itemOffset.y && adjustedY <= itemOffset.y + itemSize.height
        ) {
            return item.index
        }
    }

    // Band-aligned fallback test
    val matchingBand = visibleItems.filter { item ->
        adjustedY >= item.offset.y && adjustedY <= item.offset.y + item.size.height
    }
    if (matchingBand.isNotEmpty()) {
        return matchingBand.minByOrNull { item ->
            val centerX = item.offset.x + item.size.width / 2f
            kotlin.math.abs(adjustedX - centerX)
        }?.index
    }

    return null
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
