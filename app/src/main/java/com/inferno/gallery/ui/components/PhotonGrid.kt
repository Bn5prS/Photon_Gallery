@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.composed
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
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
import com.inferno.gallery.ui.StableUriSet
import com.inferno.gallery.ui.ViewMode
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraSmall
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeNone
import com.inferno.gallery.ui.theme.TimelineDateHeaderStyle
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
    selectedUris: StableUriSet,
    onMediaClick: (GalleryItem) -> Unit,
    viewMode: ViewMode,
    thumbnailCornerRadius: Float,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    timelineLayoutMode: TimelineLayoutMode = TimelineLayoutMode.STANDARD_GRID,
    dateHeaderAlignment: String = "CENTER",
    showMatchBadge: Boolean = false,
    headerContent: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Hoisted static image vectors resolved once per grid, avoiding per-cell recomposition lookups
    val videoIconVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow)
    val checkIconVector = ImageVector.vectorResource(R.drawable.ic_ms_check)

    // ── Live Pinch-to-Zoom Spring Animation & Scale State ───────────────────────
    var isPinching by remember { mutableStateOf(false) }
    val livePinchScaleAnim = remember { Animatable(1f) }
    val pinchScaleScope = rememberCoroutineScope()

    val basePinchModifier = Modifier
        .fillMaxSize()
        .smoothGridPinchZoom(
            gridCellsCount = gridCellsCount,
            onGridCountChange = onGridCountChange,
            isSelectionMode = isSelectionMode,
            haptic = haptic,
            onPinchScaleChange = { scale, _ ->
                isPinching = true
                pinchScaleScope.launch {
                    livePinchScaleAnim.snapTo(scale.coerceIn(0.84f, 1.20f))
                }
            },
            onPinchEnd = {
                pinchScaleScope.launch {
                    livePinchScaleAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.80f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    isPinching = false
                }
            }
        )
        .graphicsLayer {
            val scale = livePinchScaleAnim.value
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
        }

    LaunchedEffect(lazyGridState) {
        snapshotFlow {
            lazyGridState.firstVisibleItemIndex > 0 || lazyGridState.firstVisibleItemScrollOffset > 32
        }
            .distinctUntilChanged()
            .collect { isScrolled ->
                viewModel.setTopBarCollapsed(isScrolled)
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = basePinchModifier) {
            when (timelineLayoutMode) {
                TimelineLayoutMode.EDITORIAL_MOSAIC -> {
                // ── Editorial Mosaic Layout (Smart Hero & Grid) ───────────────
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridCellsCount.coerceIn(2, 6)),
                    state = lazyGridState,
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
                            coroutineScope = coroutineScope,
                            headerOffset = if (headerContent != null) 1 else 0
                        )
                ) {
                    if (headerContent != null) {
                        item(
                            key = "grid_top_header_content",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "header_content"
                        ) {
                            headerContent()
                        }
                    }
                    items(
                        count = pagedMedia.itemCount,
                        key = pagedMedia.itemKey { item ->
                            when (item) {
                                is GalleryListItem.Header -> "header_${item.id}"
                                is GalleryListItem.Item -> item.galleryItem.id
                            }
                        },
                        contentType = pagedMedia.itemContentType { item ->
                            if (item is GalleryListItem.Header) "header" else "media"
                        },
                        span = { index ->
                            val listItem = if (index < pagedMedia.itemCount) pagedMedia.peek(index) else null
                            if (listItem is GalleryListItem.Header) {
                                GridItemSpan(maxLineSpan)
                            } else {
                                val isHero = (index % 7 == 1) || (index % 13 == 4)
                                if (isHero) GridItemSpan(minOf(2, gridCellsCount)) else GridItemSpan(1)
                            }
                        }
                    ) { index ->
                        val listItem = pagedMedia[index]
                        if (listItem is GalleryListItem.Header && viewMode != ViewMode.Immersive) {
                            TimelineSectionHeader(title = listItem.title, alignment = dateHeaderAlignment)
                        } else if (listItem is GalleryListItem.Item) {
                            val item = listItem.galleryItem
                            val isHero = (index % 7 == 1) || (index % 13 == 4)
                            val mosaicRatio = if (isHero) 1.35f else 1.0f
                            val isSelected = selectedUris.contains(item.uri.toString())

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
                                checkIconVector = checkIconVector,
                                showMatchBadge = showMatchBadge
                            )
                        } else {
                            val isHero = (index % 7 == 1) || (index % 13 == 4)
                            val mosaicRatio = if (isHero && gridCellsCount >= 3) 1.4f else 1.0f
                            val placeholderShape = remember(thumbnailCornerRadius) {
                                if (thumbnailCornerRadius > 0f) RoundedCornerShape(thumbnailCornerRadius.dp) else null
                            }
                            Box(
                                modifier = Modifier
                                    .aspectRatio(mosaicRatio)
                                    .run { if (placeholderShape != null) clip(placeholderShape) else this }
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .gridDragSelectGesture(
                            lazyGridState = lazyGridState,
                            pagedMedia = pagedMedia,
                            viewModel = viewModel,
                            hapticFeedback = haptic,
                            coroutineScope = coroutineScope,
                            headerOffset = if (headerContent != null) 1 else 0
                        )
                ) {
                    if (headerContent != null) {
                        item(
                            key = "grid_top_header_content",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "header_content"
                        ) {
                            headerContent()
                        }
                    }
                    items(
                        count = pagedMedia.itemCount,
                        key = pagedMedia.itemKey { item ->
                            when (item) {
                                is GalleryListItem.Header -> "header_${item.id}"
                                is GalleryListItem.Item -> item.galleryItem.id
                            }
                        },
                        contentType = pagedMedia.itemContentType { item ->
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
                            TimelineSectionHeader(title = listItem.title, alignment = dateHeaderAlignment)
                        } else if (listItem is GalleryListItem.Item) {
                            val item = listItem.galleryItem
                            val isSelected = selectedUris.contains(item.uri.toString())

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
                                checkIconVector = checkIconVector,
                                showMatchBadge = showMatchBadge
                            )
                        } else {
                            val placeholderShape = remember(thumbnailCornerRadius) {
                                if (thumbnailCornerRadius > 0f) RoundedCornerShape(thumbnailCornerRadius.dp) else null
                            }
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.0f)
                                    .run { if (placeholderShape != null) clip(placeholderShape) else this }
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
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
 * Timeline section date group header composable.
 */
@Composable
fun TimelineSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    alignment: String = "CENTER"
) {
    val isCenter = alignment == "CENTER"
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        contentAlignment = if (isCenter) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = title,
            style = TimelineDateHeaderStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (isCenter) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
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
    videoIconVector: ImageVector = ImageVector.vectorResource(R.drawable.ic_ms_videocam),
    checkIconVector: ImageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
    showMatchBadge: Boolean = false
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Hoisted click handlers. PhotonGrid passes no onLongClick (long-press there is owned by
    // gridDragSelectGesture on the grid itself, so cells stay on plain clickable and don't add
    // a second long-press detector competing for the pointer stream). SearchScreen, which has
    // no grid-level drag-select, does supply one.
    val clickHandler = remember(item, onClick) { { onClick(item) } }
    val longClickHandler = remember(item, onLongClick) {
        if (onLongClick != null) {
            {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onLongClick(item)
            }
        } else null
    }

    val cellShape = remember(thumbnailCornerRadius) {
        if (thumbnailCornerRadius > 0f) RoundedCornerShape(thumbnailCornerRadius.dp) else null
    }
    val sharedContentKey = remember(item.uri) { "photo_${item.uri}" }

    // Shared bounds transition is active for standard grid densities (1-6 columns)
    val sharedTransitionModifier = if (gridCellsCount <= 6) {
        with(sharedTransitionScope) {
            val overlayClip = remember(cellShape) { OverlayClip(cellShape ?: ShapeNone) }
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = sharedContentKey),
                animatedVisibilityScope = animatedVisibilityScope,
                enter = CellEnterFade,
                exit = CellExitFade,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                clipInOverlayDuringTransition = overlayClip,
                boundsTransform = SharedBoundsTransform
            )
        }
    } else {
        Modifier
    }

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = clickHandler,
            onLongClick = longClickHandler
        )
    } else {
        Modifier.clickable(
            onClick = clickHandler
        )
    }

    val selectionModifier = if (isSelected) {
        // Read inside the graphicsLayer block (draw phase) rather than via `by`. Delegating to
        // a State read in composition made every selected cell RECOMPOSE on each frame of the
        // spring; reading it here drives only the layer. Identical visuals, no per-frame
        // recomposition — which is what made drag-select across many cells look "gittery".
        val selectionScale = animateFloatAsState(
            targetValue = 0.88f,
            animationSpec = MotionTokens.bouncySpring(),
            label = "cellSelectionScale"
        )
        Modifier.graphicsLayer {
            val scale = selectionScale.value
            scaleX = scale
            scaleY = scale
        }
    } else {
        Modifier
    }

    // Single unified Box — provides the M3 surfaceContainer skeleton canvas tile
    // ensuring the grid cell skeleton is always visible before and during thumbnail loading.
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .then(selectionModifier)
            .then(sharedTransitionModifier)
            .run { if (cellShape != null) clip(cellShape) else this }
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(clickModifier)
    ) {
        PhotonGridThumbnail(
            item = item,
            gridCellsCount = gridCellsCount,
            videoIconVector = videoIconVector
        )

        // ── Search Match Reason Badge & Snippet Overlay ───────────────────────
        if (showMatchBadge && item.matchType != null) {
            SearchMatchBadge(
                matchType = item.matchType,
                matchSnippet = item.matchSnippet,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            )

            if (!item.matchSnippet.isNullOrBlank() && item.matchType in setOf("OCR", "NAME", "SEMANTIC", "LOCATION")) {
                val snippetModifier = if (item.isVideo) {
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 2.dp, start = 3.dp)
                        .fillMaxWidth(0.6f)
                } else {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                }
                SearchSnippetOverlay(
                    snippet = item.matchSnippet,
                    modifier = snippetModifier
                )
            }
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

private data class MatchBadgeConfig(
    val iconRes: Int,
    val label: String,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
fun SearchMatchBadge(
    matchType: String,
    matchSnippet: String? = null,
    modifier: Modifier = Modifier
) {
    val config = when (matchType) {
        "OCR" -> MatchBadgeConfig(
            R.drawable.ic_ms_text_fields,
            "OCR",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        "NAME" -> MatchBadgeConfig(
            R.drawable.ic_ms_description,
            "Name",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        "FORMAT" -> MatchBadgeConfig(
            R.drawable.ic_ms_image,
            matchSnippet?.take(8) ?: "Format",
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurface
        )
        "SEMANTIC" -> MatchBadgeConfig(
            R.drawable.ic_ms_auto_fix_high,
            "Semantic",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        "ALBUM" -> MatchBadgeConfig(
            R.drawable.ic_ms_photo_album,
            "Album",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        "LOCATION" -> MatchBadgeConfig(
            R.drawable.ic_ms_location_on,
            "Location",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        "DATE" -> MatchBadgeConfig(
            R.drawable.ic_ms_calendar_month,
            "Date",
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurface
        )
        else -> MatchBadgeConfig(
            R.drawable.ic_ms_search,
            matchType,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurface
        )
    }

    Surface(
        shape = ShapeFull,
        color = config.containerColor.copy(alpha = 0.92f),
        contentColor = config.contentColor,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(config.iconRes),
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = config.contentColor
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = config.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                ),
                color = config.contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SearchSnippetOverlay(
    snippet: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0f),
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f)
                    )
                )
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = snippet,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.surface
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Shared thumbnail cache key ────────────────────────────────────────────────

/**
 * Size-qualified on purpose. An unqualified key lets a bitmap decoded for one column count
 * satisfy a request for another, and because the key matched Coil would not re-request — the
 * cell stays permanently soft at the larger size. Qualifying also stops screens that do not
 * pass a column count (e.g. SearchScreen) from poisoning the main grid's entries.
 */
fun thumbnailMemoryKey(id: String, sizePx: Int): String = "t_${id}@$sizePx"

/**
 * Shared thumbnail resolution buckets, derived from column count. Used by the grid cells, the
 * scroll-ahead prefetcher and the detail-screen placeholder so their cache keys always agree.
 */
fun thumbnailTargetPx(gridCellsCount: Int): Int = when {
    gridCellsCount <= 2 -> 512
    gridCellsCount == 3 -> 320
    gridCellsCount == 4 -> 240
    gridCellsCount <= 6 -> 160
    else -> 128
}

// Shared-element fade specs & springs — hoisted so cells don't allocate them per composition
private val CellEnterFade = fadeIn(animationSpec = MotionTokens.fastEffectsSpec())
private val CellExitFade = fadeOut(animationSpec = MotionTokens.fastEffectsSpec())
private val SharedElementSpringSpec = MotionTokens.sharedElementSpring<androidx.compose.ui.geometry.Rect>()
private val SharedBoundsTransform = androidx.compose.animation.BoundsTransform { _, _ -> SharedElementSpringSpec }

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
): Modifier = composed {
    val currentGridCount by rememberUpdatedState(gridCellsCount)
    val currentOnGridCountChange by rememberUpdatedState(onGridCountChange)
    val currentIsSelectionMode by rememberUpdatedState(isSelectionMode)
    val currentOnPinchScaleChange by rememberUpdatedState(onPinchScaleChange)
    val currentOnPinchEnd by rememberUpdatedState(onPinchEnd)

    Modifier.pointerInput(Unit) {
        val touchSlop = viewConfiguration.touchSlop

        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

            if (currentIsSelectionMode) return@awaitEachGesture

            var initialDistance = -1f
            var pinchActive = false
            var pinchEverActivated = false
            var lastChangeTime = 0L

            try {
                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val changes = event.changes

                    // Ultra-fast path for 1-finger scrolls/flings when pinch was never active
                    if (changes.size < 2 && !pinchEverActivated) {
                        continue
                    }
                    var p1: Offset? = null
                    var p2: Offset? = null
                    var pressedCount = 0

                    for (i in changes.indices) {
                        val c = changes[i]
                        if (c.pressed) {
                            if (pressedCount == 0) p1 = c.position
                            else if (pressedCount == 1) p2 = c.position
                            pressedCount++
                        }
                    }

                    if (pressedCount >= 2 && p1 != null && p2 != null) {
                        val currentDistance = kotlin.math.hypot(p1.x - p2.x, p1.y - p2.y)
                        val currentCentroid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                        if (initialDistance <= 0f) {
                            initialDistance = currentDistance
                        } else {
                            val distanceDiff = kotlin.math.abs(currentDistance - initialDistance)
                            val rawScale = currentDistance / initialDistance

                            // Slop threshold: require noticeable finger movement before activating pinch
                            if (!pinchActive && distanceDiff > touchSlop * 1.25f && (rawScale > 1.05f || rawScale < 0.95f)) {
                                pinchActive = true
                                pinchEverActivated = true
                            }

                            if (pinchActive) {
                                // Consume in Initial pass so child scrollable never sees move events
                                event.changes.forEach { it.consume() }
                                currentOnPinchScaleChange(rawScale, currentCentroid)

                                val now = System.currentTimeMillis()
                                // Zoom IN: spreading fingers -> fewer columns (e.g. 4 -> 3)
                                if (rawScale > 1.20f && now - lastChangeTime > 260L) {
                                    val currentCols = currentGridCount
                                    if (currentCols > 1) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentOnGridCountChange(currentCols - 1)
                                        lastChangeTime = now
                                        initialDistance = currentDistance
                                        currentOnPinchScaleChange(1f, currentCentroid)
                                    }
                                }
                                // Zoom OUT: pinching fingers -> more columns (e.g. 3 -> 4)
                                else if (rawScale < 0.82f && now - lastChangeTime > 260L) {
                                    val currentCols = currentGridCount
                                    if (currentCols < 6) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentOnGridCountChange(currentCols + 1)
                                        lastChangeTime = now
                                        initialDistance = currentDistance
                                        currentOnPinchScaleChange(1f, currentCentroid)
                                    }
                                }
                            }
                        }
                    } else {
                        // Less than 2 fingers down:
                        // Crucial: if pinch was ever active in this gesture, consume ALL remaining
                        // pointers (including during lift-off) so LazyVerticalGrid never flings!
                        if (pinchEverActivated) {
                            event.changes.forEach { it.consume() }
                        }
                        pinchActive = false
                        initialDistance = -1f
                    }
                } while (event.changes.any { it.pressed })
            } finally {
                if (pinchEverActivated) {
                    currentOnPinchEnd()
                }
            }
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
    coroutineScope: CoroutineScope,
    headerOffset: Int = 0
): Modifier = this.pointerInput(lazyGridState) {
    var dragStartIndex = -1
    var lastHitIndex = -1
    var currentDragPosition: Offset? = null // Plain var — never observed by Composable, no snapshot overhead
    var autoScrollJob: Job? = null

    val autoScrollThresholdPx = with(density) { 72.dp.toPx() }
    val maxScrollSpeedPx = with(density) { 24.dp.toPx() }

    fun updateSelectionForIndex(targetIndex: Int) {
        if (dragStartIndex == -1 || targetIndex == -1) return
        val adjustedStart = dragStartIndex - headerOffset
        val adjustedTarget = targetIndex - headerOffset
        if (adjustedStart < 0 || adjustedTarget < 0) return
        val start = minOf(adjustedStart, adjustedTarget)
        val end = maxOf(adjustedStart, adjustedTarget)
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
            val rawHit = findItemAtOffset(lazyGridState, offset) ?: return@detectDragGesturesAfterLongPress
            val hitItem = rawHit - headerOffset
            if (hitItem >= 0 && hitItem < pagedMedia.itemCount) {
                val listItem = pagedMedia.peek(hitItem)
                if (listItem is GalleryListItem.Item) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    val uri = listItem.galleryItem.uri.toString()
                    val isSelecting = !viewModel.selectedUris.value.contains(uri)
                    viewModel.startDragSelection(uri, isSelecting)
                    dragStartIndex = rawHit
                    lastHitIndex = rawHit
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
                val adjustedHit = currentHit - headerOffset
                if (adjustedHit >= 0) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHitIndex = currentHit
                    updateSelectionForIndex(currentHit)
                }
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

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
