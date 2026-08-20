package com.inferno.gallery.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


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

    // Hoisted static image vectors resolved once per grid, avoiding per-cell recomposition lookups
    val videoIconVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow)
    val checkIconVector = ImageVector.vectorResource(R.drawable.ic_ms_check)

    // ── Live Pinch-to-Zoom Spring Animation State ─────────────────────────────
    var isPinching by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        val basePinchModifier = Modifier
            .fillMaxSize()
            .robustTwoFingerPinchZoom(
                gridCellsCount = gridCellsCount,
                onGridCountChange = onGridCountChange,
                isSelectionMode = isSelectionMode,
                haptic = haptic,
                onPinchUpdate = { pinching ->
                    isPinching = pinching
                }
            )

        when (timelineLayoutMode) {
            TimelineLayoutMode.STAGGERED_MASONRY -> {
                // ── Staggered Masonry Layout (True Aspect Ratio Columns) ──────
                val staggeredState = rememberLazyStaggeredGridState()
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(gridCellsCount),
                    state = staggeredState,
                    contentPadding = contentPadding,
                    verticalItemSpacing = 2.dp,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = basePinchModifier
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
                            Row(
                                modifier = Modifier
                                    .animateItem(placementSpec = MotionTokens.snappySpring())
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
                            // derivedStateOf: only recompose this cell when its own selection changes
                            val isSelected by remember(uriString, selectedUris) {
                                derivedStateOf { selectedUris.contains(uriString) }
                            }

                            OptimizedThumbnailCell(
                                modifier = Modifier.animateItem(placementSpec = MotionTokens.snappySpring()),
                                item = item,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onClick = onMediaClick,
                                isSelected = isSelected,
                                gridCellsCount = gridCellsCount,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                aspectRatio = masonryRatio,
                                isScrolling = staggeredState.isScrollInProgress,
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
                                    .animateItem(placementSpec = MotionTokens.snappySpring())
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
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = basePinchModifier
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
                                    .animateItem(placementSpec = MotionTokens.snappySpring())
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
                            // derivedStateOf: only recompose this cell when its own selection changes
                            val isSelected by remember(uriString, selectedUris) {
                                derivedStateOf { selectedUris.contains(uriString) }
                            }

                            OptimizedThumbnailCell(
                                modifier = Modifier.animateItem(placementSpec = MotionTokens.snappySpring()),
                                item = item,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onClick = onMediaClick,
                                isSelected = isSelected,
                                gridCellsCount = gridCellsCount,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                aspectRatio = mosaicRatio,
                                isScrolling = lazyGridState.isScrollInProgress,
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
                                    .animateItem(placementSpec = MotionTokens.snappySpring())
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
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = basePinchModifier
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
                            Row(
                                modifier = Modifier
                                    .animateItem(placementSpec = MotionTokens.snappySpring())
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
                            // derivedStateOf: this cell only recomposes when ITS OWN selection state changes,
                            // not when any other URI is toggled. Eliminates N-cell cascade recompositions.
                            val isSelected by remember(uriString, selectedUris) {
                                derivedStateOf { selectedUris.contains(uriString) }
                            }

                            OptimizedThumbnailCell(
                                modifier = Modifier.animateItem(placementSpec = MotionTokens.snappySpring()),
                                item = item,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onClick = onMediaClick,
                                isSelected = isSelected,
                                gridCellsCount = gridCellsCount,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                aspectRatio = 1.0f,
                                isScrolling = lazyGridState.isScrollInProgress,
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
                                    .animateItem(placementSpec = MotionTokens.snappySpring())
                                    .aspectRatio(1.0f)
                                    .clip(placeholderShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }
            }
        }

        // ── Floating Material 3 Expressive Column Count HUD Badge ─────────────
        AnimatedVisibility(
            visible = isPinching,
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

private val SelectionBorderStroke = BorderStroke(2.dp, Color.White)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OptimizedThumbnailCell(
    item: GalleryItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (GalleryItem) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    gridCellsCount: Int = 3,
    thumbnailCornerRadius: Float = 0f,
    aspectRatio: Float = 1.0f,
    isScrolling: Boolean = false,
    videoIconVector: ImageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
    checkIconVector: ImageVector = ImageVector.vectorResource(R.drawable.ic_ms_check)
) {
    val context = LocalContext.current

    // Hoisted click handler
    val clickHandler = remember(item, onClick) { { onClick(item) } }

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
            .bitmapConfig(android.graphics.Bitmap.Config.HARDWARE) // GPU-resident bitmaps; eliminates software-copy blit per frame
            .build()
    }

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

    val contentModifier = remember(thumbnailCornerRadius, aspectRatio) {
        if (thumbnailCornerRadius > 0f) {
            Modifier
                .aspectRatio(aspectRatio)
                .clip(cellShape)
        } else {
            Modifier.aspectRatio(aspectRatio)
        }
    }

    val sharedTransitionModifier = with(sharedTransitionScope) {
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

    Box(modifier = cellModifier) {
        Box(
            modifier = contentModifier
                .then(sharedTransitionModifier)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = clickHandler
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
            val durationText = remember(item.durationMs) {
                item.durationMs?.let { formatDuration(it) } ?: "0:00"
            }
            if (!isScrolling) {
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
                            imageVector = videoIconVector,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = durationText,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = fontSize,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
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

            if (badgeText != null && !isScrolling) {
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
            if (isScrolling) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    border = SelectionBorderStroke,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = checkIconVector,
                            contentDescription = "Selected",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Shared thumbnail cache key ────────────────────────────────────────────────

internal fun thumbnailMemoryKey(id: String): String = "t_$id"

// Shared-element fade specs — hoisted so cells don't allocate them per composition
private val CellEnterFade = fadeIn(animationSpec = androidx.compose.animation.core.tween(150))
private val CellExitFade = fadeOut(animationSpec = androidx.compose.animation.core.tween(150))

// ── Gestures ──────────────────────────────────────────────────────────────────

/**
 * Clean, high-performance two-finger pinch-to-zoom detector with live interactive scaling.
 * Only activates when 2 fingers touch simultaneously so single-finger scrolling
 * and drag selection are never blocked or jittered.
 */
private fun Modifier.robustTwoFingerPinchZoom(
    gridCellsCount: Int,
    onGridCountChange: (Int) -> Unit,
    isSelectionMode: Boolean,
    haptic: HapticFeedback,
    onPinchUpdate: (isPinching: Boolean) -> Unit
): Modifier = this.pointerInput(gridCellsCount, isSelectionMode) {
    if (isSelectionMode) return@pointerInput

    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

        var initialDistance = -1f
        var lastChangeTime = 0L
        var hasActivePinch = false

        do {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
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
                hasActivePinch = true
                changes.forEach { it.consume() }

                val currentDistance = kotlin.math.hypot(p1x - p2x, p1y - p2y)

                if (initialDistance <= 0f) {
                    initialDistance = currentDistance
                    onPinchUpdate(true)
                } else {
                    val rawScale = currentDistance / initialDistance
                    val now = System.currentTimeMillis()

                    // Zoom IN (Fingers spreading -> fewer columns, e.g. 3 -> 2)
                    if (rawScale > 1.20f && now - lastChangeTime > 240L) {
                        if (gridCellsCount > 1) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGridCountChange(gridCellsCount - 1)
                            lastChangeTime = now
                            initialDistance = currentDistance
                            onPinchUpdate(true)
                        }
                    }
                    // Zoom OUT (Fingers pinching closer -> more columns, e.g. 3 -> 4)
                    else if (rawScale < 0.82f && now - lastChangeTime > 240L) {
                        if (gridCellsCount < 6) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGridCountChange(gridCellsCount + 1)
                            lastChangeTime = now
                            initialDistance = currentDistance
                            onPinchUpdate(true)
                        }
                    }
                }
            } else {
                if (hasActivePinch) {
                    // Consume lingering release events so lifting one finger doesn't trigger a single-finger fling/scroll
                    changes.forEach { it.consume() }
                }
                if (initialDistance > 0f) {
                    onPinchUpdate(false)
                }
                initialDistance = -1f
            }
        } while (event.changes.any { it.pressed })

        onPinchUpdate(false)
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

    // Exact bounding box test
    for (item in visibleItems) {
        val itemOffset = item.offset
        val itemSize = item.size
        if (offset.x >= itemOffset.x && offset.x <= itemOffset.x + itemSize.width &&
            offset.y >= itemOffset.y && offset.y <= itemOffset.y + itemSize.height
        ) {
            return item.index
        }
    }

    // Row-aligned fallback test
    val matchingRow = visibleItems.filter { item ->
        offset.y >= item.offset.y && offset.y <= item.offset.y + item.size.height
    }
    if (matchingRow.isNotEmpty()) {
        return matchingRow.minByOrNull { item ->
            val centerX = item.offset.x + item.size.width / 2f
            kotlin.math.abs(offset.x - centerX)
        }?.index
    }

    return null
}

private fun findStaggeredItemAtOffset(staggeredState: LazyStaggeredGridState, offset: Offset): Int? {
    val layoutInfo = staggeredState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    // Exact bounding box test
    for (item in visibleItems) {
        val itemOffset = item.offset
        val itemSize = item.size
        if (offset.x >= itemOffset.x && offset.x <= itemOffset.x + itemSize.width &&
            offset.y >= itemOffset.y && offset.y <= itemOffset.y + itemSize.height
        ) {
            return item.index
        }
    }

    // Band-aligned fallback test
    val matchingBand = visibleItems.filter { item ->
        offset.y >= item.offset.y && offset.y <= item.offset.y + item.size.height
    }
    if (matchingBand.isNotEmpty()) {
        return matchingBand.minByOrNull { item ->
            val centerX = item.offset.x + item.size.width / 2f
            kotlin.math.abs(offset.x - centerX)
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
