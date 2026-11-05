@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import coil3.imageLoader
import coil3.asDrawable
import coil3.request.CachePolicy
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.inferno.gallery.R
import com.inferno.gallery.data.NativeTimelineItem
import com.inferno.gallery.data.ThumbnailScrollThrottle
import com.inferno.gallery.ui.GalleryItem
import com.inferno.gallery.ui.GalleryListItem
import com.inferno.gallery.ui.StableUriSet
import com.inferno.gallery.ui.formatDuration
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

private const val VIEW_TYPE_COMPOSE_HEADER = -1
private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_MEDIA = 1

private const val PAYLOAD_LOAD_THUMBNAIL = 1
private const val PAYLOAD_SELECTION_ONLY = 2
private const val PAYLOAD_FAVORITE_ONLY = 3
private const val PAYLOAD_SPAN_COUNT = 4
private const val PAYLOAD_CORNER_RADIUS = 5

/**
 * Extension mapper to convert [NativeTimelineItem.Media] to standard [GalleryItem].
 */
fun NativeTimelineItem.Media.toGalleryItem(): GalleryItem = GalleryItem(
    id = id,
    uri = uri,
    bucketName = bucketName,
    dateAdded = dateAdded,
    size = size,
    name = path.substringAfterLast('/').ifEmpty { id },
    dateModified = dateModified,
    path = path,
    isVideo = isVideo,
    durationMs = durationMs,
    localExists = true,
    resolvedUri = uri,
    uriString = uriString.ifEmpty { uri.toString() }
)

/**
 * Extension mapper to convert [GalleryItem] to [NativeTimelineItem.Media].
 */
fun GalleryItem.toNativeTimelineItem(): NativeTimelineItem.Media = NativeTimelineItem.Media(
    id = id,
    uri = uri,
    uriString = uriString,
    path = path,
    dateAdded = dateAdded,
    dateModified = dateModified,
    isVideo = isVideo,
    durationMs = durationMs,
    bucketName = bucketName,
    size = size
)

/**
 * Extension mapper to convert [GalleryListItem] to [NativeTimelineItem].
 */
fun GalleryListItem.toNativeTimelineItem(): NativeTimelineItem = when (this) {
    is GalleryListItem.Header -> NativeTimelineItem.Header(
        id = id,
        title = title,
        timestamp = 0L
    )
    is GalleryListItem.Item -> galleryItem.toNativeTimelineItem()
}

/**
 * Dynamic Material 3 color container passed to programmatic ViewHolders.
 */
data class GridColorScheme(
    @ColorInt val surface: Int,
    @ColorInt val surfaceContainer: Int,
    @ColorInt val onSurface: Int,
    @ColorInt val onSurfaceVariant: Int,
    @ColorInt val primary: Int,
    @ColorInt val onPrimary: Int
)

/**
 * Ultra-high performance native Android [RecyclerView] embedded via Compose [AndroidView].
 *
 * Implements 120 FPS fluid scrolling:
 * - Hosted within [NativeFastScroller] with RenderThread translation and instant date bubble.
 * - Dedicated [RecyclerView.RecycledViewPool] for media cells and timeline headers.
 * - 100ms fling throttle: skips image decodes during high-velocity flings,
 *   displaying clean [surfaceContainer] tiles until scroll motion settles.
 * - Dynamic span counts (1 to 8 columns) with pinch-to-zoom [ScaleGestureDetector].
 * - Zero outlines or borders, respecting Material 3 Expressive guidelines.
 * - Hardware-accelerated corner clipping without memory leaks.
 */
@Composable
fun NativePhotonGrid(
    items: List<NativeTimelineItem>,
    modifier: Modifier = Modifier,
    gridCellsCount: Int = 3,
    onGridCountChange: (Int) -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedUris: Set<String> = emptySet(),
    favoriteIds: Set<String> = emptySet(),
    onMediaClick: (GalleryItem) -> Unit = {},
    onToggleSelection: ((NativeTimelineItem.Media) -> Unit)? = null,
    onMediaLongClick: ((NativeTimelineItem.Media) -> Unit)? = null,
    onStartDragSelection: ((initialUri: String, isSelecting: Boolean) -> Unit)? = null,
    onUpdateDragSelection: ((uris: Set<String>) -> Unit)? = null,
    onEndDragSelection: (() -> Unit)? = null,
    thumbnailCornerRadius: Float = 0f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    dateHeaderAlignment: String = "LEFT",
    headerContent: (@Composable () -> Unit)? = null,
    scrollToTopTrigger: Flow<Unit>? = null,
    scrollToTopKey: Any? = null,
    onScrolledAwayFromTop: ((Boolean) -> Unit)? = null,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    onSaveScrollPosition: ((Int, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { context.imageLoader }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val colorScheme = MaterialTheme.colorScheme
    val currentColorScheme = remember(colorScheme) {
        GridColorScheme(
            surface = colorScheme.surface.toArgb(),
            surfaceContainer = colorScheme.surfaceContainer.toArgb(),
            onSurface = colorScheme.onSurface.toArgb(),
            onSurfaceVariant = colorScheme.onSurfaceVariant.toArgb(),
            primary = colorScheme.primary.toArgb(),
            onPrimary = colorScheme.onPrimary.toArgb()
        )
    }

    val leftPaddingPx = remember(contentPadding, density, layoutDirection) {
        with(density) { contentPadding.calculateLeftPadding(layoutDirection).roundToPx() }
    }
    val topPaddingPx = remember(contentPadding, density) {
        with(density) { contentPadding.calculateTopPadding().roundToPx() }
    }
    val rightPaddingPx = remember(contentPadding, density, layoutDirection) {
        with(density) { contentPadding.calculateRightPadding(layoutDirection).roundToPx() }
    }
    val bottomPaddingPx = remember(contentPadding, density) {
        with(density) { contentPadding.calculateBottomPadding().roundToPx() }
    }

    val currentOnGridCountChange by rememberUpdatedState(onGridCountChange)
    val currentSelectedUris by rememberUpdatedState(selectedUris)
    val currentOnMediaClick by rememberUpdatedState(onMediaClick)
    val currentOnToggleSelection by rememberUpdatedState(onToggleSelection)
    val currentOnMediaLongClick by rememberUpdatedState(onMediaLongClick)
    val currentOnStartDragSelection by rememberUpdatedState(onStartDragSelection)
    val currentOnUpdateDragSelection by rememberUpdatedState(onUpdateDragSelection)
    val currentOnEndDragSelection by rememberUpdatedState(onEndDragSelection)
    val currentOnScrolledAwayFromTop by rememberUpdatedState(onScrolledAwayFromTop)
    val currentOnSaveScrollPosition by rememberUpdatedState(onSaveScrollPosition)

    val dragSelectHelper = remember {
        NativeGridDragSelectHelper(
            selectedUrisProvider = { currentSelectedUris },
            onStartDragSelection = { uri, isSelecting ->
                currentOnStartDragSelection?.invoke(uri, isSelecting)
            },
            onUpdateDragSelection = { uris ->
                currentOnUpdateDragSelection?.invoke(uris)
            },
            onEndDragSelection = {
                currentOnEndDragSelection?.invoke()
            },
            density = density.density
        )
    }

    val safeSpanCount = gridCellsCount.coerceIn(1, 8)

    val recycledViewPool = remember {
        RecyclerView.RecycledViewPool().apply {
            setMaxRecycledViews(VIEW_TYPE_MEDIA, 40)
            setMaxRecycledViews(VIEW_TYPE_HEADER, 10)
        }
    }

    var targetRecyclerView by remember { mutableStateOf<RecyclerView?>(null) }
    var hasRestoredScroll by remember(scrollToTopKey) { mutableStateOf(initialFirstVisibleItemIndex <= 0) }
    var lastScrollToTopKey by remember { mutableStateOf(scrollToTopKey) }

    android.util.Log.d("NativePhotonGrid", "NativePhotonGrid recomposed: initialIndex=$initialFirstVisibleItemIndex offset=$initialFirstVisibleItemScrollOffset hasRestoredScroll=$hasRestoredScroll")

    if (scrollToTopTrigger != null) {
        LaunchedEffect(scrollToTopTrigger) {
            scrollToTopTrigger.collect {
                hasRestoredScroll = true
                targetRecyclerView?.scrollToPosition(0)
                currentOnSaveScrollPosition?.invoke(0, 0)
            }
        }
    }

    LaunchedEffect(scrollToTopKey) {
        if (scrollToTopKey != null && lastScrollToTopKey != scrollToTopKey) {
            android.util.Log.d("NativePhotonGrid", "scrollToTopKey changed: $lastScrollToTopKey -> $scrollToTopKey, resetting scroll to 0")
            lastScrollToTopKey = scrollToTopKey
            hasRestoredScroll = true
            targetRecyclerView?.scrollToPosition(0)
            currentOnSaveScrollPosition?.invoke(0, 0)
        }
    }

    var showHudPill by remember { mutableStateOf(false) }
    var hudSpanCount by remember { mutableIntStateOf(safeSpanCount) }
    LaunchedEffect(safeSpanCount) {
        hudSpanCount = safeSpanCount
        showHudPill = true
        delay(650)
        showHudPill = false
    }

    DisposableEffect(Unit) {
        onDispose {
            ThumbnailScrollThrottle.isFastScrolling = false
        }
    }

    val nestedScrollInterop = rememberNestedScrollInteropConnection()

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollInterop),
            factory = { ctx ->
                val scroller = NativeFastScroller(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val rv = RecyclerView(ctx).apply {
                    id = View.generateViewId()
                    setHasFixedSize(true)
                    isNestedScrollingEnabled = true
                    setItemViewCacheSize(20)
                    @Suppress("DEPRECATION")
                    isDrawingCacheEnabled = false
                    clipToPadding = false
                    itemAnimator = null // Maximum scroll throughput without layout animator hitches
                    setRecycledViewPool(recycledViewPool)

                    val gridLayoutManager = GridLayoutManager(ctx, safeSpanCount).apply {
                        isItemPrefetchEnabled = true
                        initialPrefetchItemCount = safeSpanCount * 2
                    }
                    layoutManager = gridLayoutManager

                    if (!hasRestoredScroll && initialFirstVisibleItemIndex > 0) {
                        gridLayoutManager.scrollToPositionWithOffset(
                            initialFirstVisibleItemIndex,
                            initialFirstVisibleItemScrollOffset
                        )
                    }

                    val adapter = NativePhotonAdapter(
                        context = ctx,
                        imageLoader = imageLoader,
                        colorSchemeProvider = { currentColorScheme },
                        currentSpanCount = safeSpanCount,
                        thumbnailCornerRadius = thumbnailCornerRadius,
                        isSelectionMode = isSelectionMode,
                        initialSelectedUris = selectedUris,
                        initialFavoriteIds = favoriteIds,
                        dateHeaderAlignment = dateHeaderAlignment,
                        onMediaClick = { media ->
                            val lm = gridLayoutManager
                            val firstPos = lm.findFirstVisibleItemPosition()
                            if (firstPos != RecyclerView.NO_POSITION && firstPos >= 0) {
                                val firstView = lm.findViewByPosition(firstPos)
                                val topOffset = (firstView?.top ?: topPaddingPx) - topPaddingPx
                                currentOnSaveScrollPosition?.invoke(firstPos, topOffset)
                            }
                            currentOnMediaClick(media.toGalleryItem())
                        },
                        onToggleSelection = { media -> currentOnToggleSelection?.invoke(media) },
                        onMediaLongClick = { position, media ->
                            if (currentOnStartDragSelection != null) {
                                dragSelectHelper.startDragSelect(position)
                            } else if (currentOnMediaLongClick != null) {
                                currentOnMediaLongClick?.invoke(media)
                            } else {
                                currentOnToggleSelection?.invoke(media)
                            }
                        }
                    )
                    this.adapter = adapter
                    dragSelectHelper.attach(this, adapter)
                    addOnItemTouchListener(dragSelectHelper)

                    // SpanSizeLookup: Header & Compose Header = spanCount, Media = 1
                    gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            if (position < 0 || position >= adapter.itemCount) return 1
                            return when (adapter.getItemViewType(position)) {
                                VIEW_TYPE_HEADER, VIEW_TYPE_COMPOSE_HEADER -> gridLayoutManager.spanCount
                                VIEW_TYPE_MEDIA -> 1
                                else -> 1
                            }
                        }
                    }
                    gridLayoutManager.spanSizeLookup.isSpanIndexCacheEnabled = true

                    // Hairline 1dp tile spacing item decoration
                    val spacingPx = (1f * ctx.resources.displayMetrics.density).toInt().coerceAtLeast(1)
                    addItemDecoration(GridSpacingItemDecoration(spacingPx) { gridLayoutManager.spanCount })

                    // Scroll state and position tracking
                    addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                            super.onScrollStateChanged(recyclerView, newState)
                            if (hasRestoredScroll && (newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_DRAGGING)) {
                                val lm = recyclerView.layoutManager as? GridLayoutManager
                                if (lm != null) {
                                    val firstPos = lm.findFirstVisibleItemPosition()
                                    if (firstPos != RecyclerView.NO_POSITION && firstPos >= 0) {
                                        val firstView = lm.findViewByPosition(firstPos)
                                        val topOffset = (firstView?.top ?: recyclerView.paddingTop) - recyclerView.paddingTop
                                        currentOnSaveScrollPosition?.invoke(firstPos, topOffset)
                                    }
                                }
                            }
                        }

                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)
                            val lm = recyclerView.layoutManager as? GridLayoutManager ?: return
                            val isScrolled = lm.findFirstVisibleItemPosition() > 0 || recyclerView.computeVerticalScrollOffset() > 32
                            currentOnScrolledAwayFromTop?.invoke(isScrolled)
                            if (hasRestoredScroll && recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                                val firstPos = lm.findFirstVisibleItemPosition()
                                if (firstPos != RecyclerView.NO_POSITION && firstPos >= 0) {
                                    val firstView = lm.findViewByPosition(firstPos)
                                    val topOffset = (firstView?.top ?: recyclerView.paddingTop) - recyclerView.paddingTop
                                    currentOnSaveScrollPosition?.invoke(firstPos, topOffset)
                                }
                            }
                        }
                    })

                    // Pinch-to-zoom ScaleGestureDetector (2..8 columns)
                    var cumulativeScale = 1.0f
                    val scaleGestureDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                            cumulativeScale = 1.0f
                            return true
                        }

                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            cumulativeScale *= detector.scaleFactor
                            val currentSpan = gridLayoutManager.spanCount
                            if (cumulativeScale > 1.25f) {
                                val nextSpan = (currentSpan - 1).coerceIn(2, 8)
                                if (nextSpan != currentSpan) {
                                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    currentOnGridCountChange(nextSpan)
                                    cumulativeScale = 1.0f
                                }
                            } else if (cumulativeScale < 0.80f) {
                                val nextSpan = (currentSpan + 1).coerceIn(2, 8)
                                if (nextSpan != currentSpan) {
                                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    currentOnGridCountChange(nextSpan)
                                    cumulativeScale = 1.0f
                                }
                            }
                            return true
                        }

                        override fun onScaleEnd(detector: ScaleGestureDetector) {
                            cumulativeScale = 1.0f
                        }
                    })

                    addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                            scaleGestureDetector.onTouchEvent(e)
                            return scaleGestureDetector.isInProgress
                        }

                        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                            scaleGestureDetector.onTouchEvent(e)
                        }

                        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
                    })

                    setPadding(leftPaddingPx, topPaddingPx, rightPaddingPx, bottomPaddingPx)
                }

                scroller.addView(rv, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                scroller.attachRecyclerView(rv)
                scroller.setTrackPadding(topPaddingPx, bottomPaddingPx)

                targetRecyclerView = rv
                scroller
            },
            update = { scroller ->
                val recyclerView = scroller.attachedRecyclerView ?: return@AndroidView
                val adapter = recyclerView.adapter as? NativePhotonAdapter ?: return@AndroidView
                val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return@AndroidView

                targetRecyclerView = recyclerView

                scroller.timelineItems = items
                scroller.setThemeColors(
                    primary = currentColorScheme.primary,
                    onPrimary = currentColorScheme.onPrimary,
                    surfaceContainerHighest = currentColorScheme.surfaceContainer,
                    onSurface = currentColorScheme.onSurface
                )

                // Header content
                adapter.setHeaderContent(headerContent)

                // Date header alignment
                if (adapter.dateHeaderAlignment != dateHeaderAlignment) {
                    adapter.dateHeaderAlignment = dateHeaderAlignment
                    adapter.notifyItemRangeChanged(0, adapter.itemCount)
                }

                // Update span count
                if (layoutManager.spanCount != safeSpanCount) {
                    layoutManager.spanCount = safeSpanCount
                    adapter.currentSpanCount = safeSpanCount
                    recyclerView.invalidateItemDecorations()
                    adapter.notifyItemRangeChanged(0, adapter.itemCount, PAYLOAD_SPAN_COUNT)
                }

                // Update corner radius
                if (adapter.thumbnailCornerRadius != thumbnailCornerRadius) {
                    adapter.thumbnailCornerRadius = thumbnailCornerRadius
                    adapter.notifyItemRangeChanged(0, adapter.itemCount, PAYLOAD_CORNER_RADIUS)
                }

                // Update selection & favorites
                adapter.isSelectionMode = isSelectionMode
                adapter.updateSelection(selectedUris)
                adapter.updateFavorites(favoriteIds)

                // Update items
                if (!hasRestoredScroll && initialFirstVisibleItemIndex > 0 && items.isNotEmpty()) {
                    adapter.submitList(items) {
                        val targetPos = initialFirstVisibleItemIndex.coerceAtMost((adapter.itemCount - 1).coerceAtLeast(0))
                        android.util.Log.d("NativePhotonGrid", "submitList commitCallback: restoring to targetPos=$targetPos offset=$initialFirstVisibleItemScrollOffset (itemCount=${adapter.itemCount})")
                        layoutManager.scrollToPositionWithOffset(targetPos, initialFirstVisibleItemScrollOffset)
                        hasRestoredScroll = true
                    }
                    recyclerView.post {
                        if (!hasRestoredScroll && initialFirstVisibleItemIndex > 0 && adapter.itemCount > 0) {
                            val targetPos = initialFirstVisibleItemIndex.coerceAtMost((adapter.itemCount - 1).coerceAtLeast(0))
                            android.util.Log.d("NativePhotonGrid", "recyclerView.post: restoring to targetPos=$targetPos offset=$initialFirstVisibleItemScrollOffset")
                            layoutManager.scrollToPositionWithOffset(targetPos, initialFirstVisibleItemScrollOffset)
                            hasRestoredScroll = true
                        }
                    }
                } else {
                    adapter.submitList(items)
                }

                // Update padding
                recyclerView.setPadding(leftPaddingPx, topPaddingPx, rightPaddingPx, bottomPaddingPx)
                scroller.setTrackPadding(topPaddingPx, bottomPaddingPx)
            },
            onRelease = { scroller ->
                dragSelectHelper.detach()
                val rv = scroller.attachedRecyclerView
                if (hasRestoredScroll) {
                    val lm = rv?.layoutManager as? GridLayoutManager
                    if (lm != null) {
                        val firstPos = lm.findFirstVisibleItemPosition()
                        if (firstPos != RecyclerView.NO_POSITION && firstPos >= 0) {
                            val firstView = lm.findViewByPosition(firstPos)
                            val topOffset = (firstView?.top ?: rv.paddingTop) - rv.paddingTop
                            currentOnSaveScrollPosition?.invoke(firstPos, topOffset)
                        }
                    }
                }
                rv?.adapter = null
                scroller.detachRecyclerView()
                targetRecyclerView = null
                recycledViewPool.clear()
                ThumbnailScrollThrottle.isFastScrolling = false
            }
        )

        // Floating Material 3 Expressive Column Count HUD Badge
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
                    text = "$hudSpanCount columns",
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
 * Convenient overload accepting [StableUriSet] for selection.
 */
@Composable
fun NativePhotonGrid(
    items: List<NativeTimelineItem>,
    modifier: Modifier = Modifier,
    gridCellsCount: Int = 3,
    onGridCountChange: (Int) -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedUris: StableUriSet,
    favoriteIds: Set<String> = emptySet(),
    onMediaClick: (GalleryItem) -> Unit = {},
    onToggleSelection: ((NativeTimelineItem.Media) -> Unit)? = null,
    onMediaLongClick: ((NativeTimelineItem.Media) -> Unit)? = null,
    onStartDragSelection: ((initialUri: String, isSelecting: Boolean) -> Unit)? = null,
    onUpdateDragSelection: ((uris: Set<String>) -> Unit)? = null,
    onEndDragSelection: (() -> Unit)? = null,
    thumbnailCornerRadius: Float = 0f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    dateHeaderAlignment: String = "LEFT",
    headerContent: (@Composable () -> Unit)? = null,
    scrollToTopTrigger: Flow<Unit>? = null,
    scrollToTopKey: Any? = null,
    onScrolledAwayFromTop: ((Boolean) -> Unit)? = null,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    onSaveScrollPosition: ((Int, Int) -> Unit)? = null
) = NativePhotonGrid(
    items = items,
    modifier = modifier,
    gridCellsCount = gridCellsCount,
    onGridCountChange = onGridCountChange,
    isSelectionMode = isSelectionMode,
    selectedUris = selectedUris.uris,
    favoriteIds = favoriteIds,
    onMediaClick = onMediaClick,
    onToggleSelection = onToggleSelection,
    onMediaLongClick = onMediaLongClick,
    onStartDragSelection = onStartDragSelection,
    onUpdateDragSelection = onUpdateDragSelection,
    onEndDragSelection = onEndDragSelection,
    thumbnailCornerRadius = thumbnailCornerRadius,
    contentPadding = contentPadding,
    dateHeaderAlignment = dateHeaderAlignment,
    headerContent = headerContent,
    scrollToTopTrigger = scrollToTopTrigger,
    scrollToTopKey = scrollToTopKey,
    onScrolledAwayFromTop = onScrolledAwayFromTop,
    initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
    initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset,
    onSaveScrollPosition = onSaveScrollPosition
)

/**
 * High-performance RecyclerView Adapter for Photon Gallery timeline.
 */
private class NativePhotonAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader,
    val colorSchemeProvider: () -> GridColorScheme,
    var currentSpanCount: Int,
    var thumbnailCornerRadius: Float,
    var isSelectionMode: Boolean,
    initialSelectedUris: Set<String>,
    initialFavoriteIds: Set<String>,
    var dateHeaderAlignment: String,
    val onMediaClick: (NativeTimelineItem.Media) -> Unit,
    val onToggleSelection: (NativeTimelineItem.Media) -> Unit,
    val onMediaLongClick: (position: Int, media: NativeTimelineItem.Media) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var selectedUris: Set<String> = initialSelectedUris
        private set
    var favoriteIds: Set<String> = initialFavoriteIds
        private set

    var headerContent: (@Composable () -> Unit)? = null
        private set
    val hasHeader: Boolean get() = headerContent != null

    fun setHeaderContent(content: (@Composable () -> Unit)?) {
        val hadHeader = headerContent != null
        val nowHasHeader = content != null
        headerContent = content
        if (hadHeader != nowHasHeader) {
            if (nowHasHeader) {
                notifyItemInserted(0)
            } else {
                notifyItemRemoved(0)
            }
        } else if (nowHasHeader) {
            notifyItemChanged(0)
        }
    }

    private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<NativeTimelineItem>() {
        override fun areItemsTheSame(oldItem: NativeTimelineItem, newItem: NativeTimelineItem): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: NativeTimelineItem, newItem: NativeTimelineItem): Boolean {
            return oldItem == newItem
        }
    })

    fun submitList(list: List<NativeTimelineItem>, commitCallback: (() -> Unit)? = null) {
        differ.submitList(list, commitCallback)
    }

    fun getItem(position: Int): NativeTimelineItem {
        val actualPos = if (hasHeader) position - 1 else position
        return differ.currentList[actualPos]
    }

    override fun getItemCount(): Int = differ.currentList.size + (if (hasHeader) 1 else 0)

    override fun getItemViewType(position: Int): Int {
        if (hasHeader && position == 0) return VIEW_TYPE_COMPOSE_HEADER
        return when (getItem(position)) {
            is NativeTimelineItem.Header -> VIEW_TYPE_HEADER
            is NativeTimelineItem.Media -> VIEW_TYPE_MEDIA
        }
    }

    fun updateSelection(newSelection: Set<String>) {
        if (selectedUris == newSelection) return
        val oldSelection = selectedUris
        selectedUris = newSelection
        val diff = (oldSelection - newSelection) + (newSelection - oldSelection)
        if (diff.size <= 24) {
            for (i in 0 until differ.currentList.size) {
                val item = differ.currentList[i]
                if (item is NativeTimelineItem.Media && item.uriString in diff) {
                    val pos = if (hasHeader) i + 1 else i
                    notifyItemChanged(pos, PAYLOAD_SELECTION_ONLY)
                }
            }
        } else {
            val start = if (hasHeader) 1 else 0
            notifyItemRangeChanged(start, differ.currentList.size, PAYLOAD_SELECTION_ONLY)
        }
    }

    fun updateFavorites(newFavorites: Set<String>) {
        if (favoriteIds == newFavorites) return
        val oldFavorites = favoriteIds
        favoriteIds = newFavorites
        val diff = (oldFavorites - newFavorites) + (newFavorites - oldFavorites)
        if (diff.size <= 24) {
            for (i in 0 until differ.currentList.size) {
                val item = differ.currentList[i]
                if (item is NativeTimelineItem.Media && item.id in diff) {
                    val pos = if (hasHeader) i + 1 else i
                    notifyItemChanged(pos, PAYLOAD_FAVORITE_ONLY)
                }
            }
        } else {
            val start = if (hasHeader) 1 else 0
            notifyItemRangeChanged(start, differ.currentList.size, PAYLOAD_FAVORITE_ONLY)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_COMPOSE_HEADER -> {
                val composeView = ComposeView(parent.context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                ComposeHeaderViewHolder(composeView)
            }
            VIEW_TYPE_HEADER -> {
                val textView = TextView(parent.context).apply {
                    val density = resources.displayMetrics.density
                    val padH = (16 * density).toInt()
                    val padTop = (16 * density).toInt()
                    val padBottom = (12 * density).toInt()
                    setPadding(padH, padTop, padH, padBottom)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    letterSpacing = 0.01f
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                HeaderViewHolder(textView, colorSchemeProvider, dateHeaderAlignment)
            }
            VIEW_TYPE_MEDIA -> {
                val components = createMediaViewComponents(parent.context)
                MediaViewHolder(
                    components = components,
                    imageLoader = imageLoader,
                    colorSchemeProvider = colorSchemeProvider,
                    onItemClick = { position ->
                        val actualPos = if (hasHeader) position - 1 else position
                        if (actualPos in 0 until differ.currentList.size) {
                            val item = differ.currentList[actualPos] as? NativeTimelineItem.Media
                            if (item != null) {
                                if (isSelectionMode) {
                                    onToggleSelection(item)
                                } else {
                                    onMediaClick(item)
                                }
                            }
                        }
                    },
                    onItemLongClick = { position ->
                        val actualPos = if (hasHeader) position - 1 else position
                        if (actualPos in 0 until differ.currentList.size) {
                            val item = differ.currentList[actualPos] as? NativeTimelineItem.Media
                            if (item != null) {
                                onMediaLongClick(position, item)
                            }
                        }
                    }
                )
            }
            else -> error("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ComposeHeaderViewHolder) {
            holder.bind(headerContent)
            return
        }

        when (val item = getItem(position)) {
            is NativeTimelineItem.Header -> {
                (holder as? HeaderViewHolder)?.bind(item)
            }
            is NativeTimelineItem.Media -> {
                (holder as? MediaViewHolder)?.bind(
                    item = item,
                    isSelected = selectedUris.contains(item.uriString),
                    isFavorite = favoriteIds.contains(item.id),
                    spanCount = currentSpanCount,
                    thumbnailCornerRadius = thumbnailCornerRadius
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        if (holder is ComposeHeaderViewHolder) {
            holder.bind(headerContent)
            return
        }

        if (holder is MediaViewHolder) {
            val item = getItem(position) as? NativeTimelineItem.Media ?: return
            var fullRebindNeeded = false
            for (payload in payloads) {
                when (payload) {
                    PAYLOAD_LOAD_THUMBNAIL -> {
                        holder.bindThumbnail(item, spanCount = currentSpanCount)
                    }
                    PAYLOAD_SELECTION_ONLY -> {
                        holder.updateSelection(selectedUris.contains(item.uriString))
                    }
                    PAYLOAD_FAVORITE_ONLY -> {
                        holder.updateFavorite(favoriteIds.contains(item.id))
                    }
                    PAYLOAD_SPAN_COUNT -> {
                        holder.updateSpan(currentSpanCount)
                        holder.bindThumbnail(item, spanCount = currentSpanCount)
                    }
                    PAYLOAD_CORNER_RADIUS -> {
                        holder.applyCornerRadius(thumbnailCornerRadius)
                    }
                    else -> fullRebindNeeded = true
                }
            }
            if (fullRebindNeeded) {
                onBindViewHolder(holder, position)
            }
        } else if (holder is HeaderViewHolder) {
            val item = getItem(position) as? NativeTimelineItem.Header ?: return
            holder.bind(item)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is MediaViewHolder) {
            holder.unbind()
        }
    }
}

private class ComposeHeaderViewHolder(
    private val composeView: ComposeView
) : RecyclerView.ViewHolder(composeView) {
    fun bind(content: (@Composable () -> Unit)?) {
        composeView.setContent {
            content?.invoke()
        }
    }
}

/**
 * Header ViewHolder rendering date string with Material 3 typography.
 */
private class HeaderViewHolder(
    private val textView: TextView,
    private val colorSchemeProvider: () -> GridColorScheme,
    var dateHeaderAlignment: String = "LEFT"
) : RecyclerView.ViewHolder(textView) {

    fun bind(header: NativeTimelineItem.Header) {
        val context = itemView.context
        val title = header.title
        val displayTitle = when (title) {
            "Today" -> context.getString(R.string.today)
            "Yesterday" -> context.getString(R.string.yesterday)
            else -> title
        }
        textView.text = displayTitle
        textView.setTextColor(colorSchemeProvider().onSurface)
        textView.gravity = if (dateHeaderAlignment == "CENTER") Gravity.CENTER else Gravity.START
    }
}

/**
 * Media ViewHolder rendering thumbnail image, badges, and smooth selection state.
 */
private class MediaViewHolder(
    private val components: MediaViewComponents,
    private val imageLoader: ImageLoader,
    private val colorSchemeProvider: () -> GridColorScheme,
    private val onItemClick: (Int) -> Unit,
    private val onItemLongClick: (Int) -> Unit
) : RecyclerView.ViewHolder(components.container) {

    private var currentDisposable: Disposable? = null
    private var loadedUri: Uri? = null

    init {
        itemView.setOnClickListener {
            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onItemClick(pos)
            }
        }
        itemView.setOnLongClickListener {
            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onItemLongClick(pos)
                true
            } else {
                false
            }
        }
    }

    fun bind(
        item: NativeTimelineItem.Media,
        isSelected: Boolean,
        isFavorite: Boolean,
        spanCount: Int,
        thumbnailCornerRadius: Float
    ) {
        updateColors()
        applyCornerRadius(thumbnailCornerRadius)
        updateSpan(spanCount)

        updateSelection(isSelected)
        updateFavorite(isFavorite)

        if (item.isVideo) {
            components.videoBadgeContainer.visibility = View.VISIBLE
            components.durationText.text = formatDuration(item.durationMs)
        } else {
            components.videoBadgeContainer.visibility = View.GONE
        }

        bindThumbnail(item, spanCount)
    }

    fun updateColors() {
        val colors = colorSchemeProvider()
        components.container.setBackgroundColor(colors.surfaceContainer)
        val circleDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colors.primary)
        }
        components.selectionBadge.background = circleDrawable
        components.checkIcon.setColorFilter(colors.onPrimary)
    }

    fun updateSelection(isSelected: Boolean) {
        components.selectionBadge.visibility = if (isSelected) View.VISIBLE else View.GONE
        components.container.scaleX = if (isSelected) 0.88f else 1.0f
        components.container.scaleY = if (isSelected) 0.88f else 1.0f
    }

    fun updateFavorite(isFavorite: Boolean) {
        components.favoriteBadgeView.visibility = if (isFavorite) View.VISIBLE else View.GONE
    }

    fun applyCornerRadius(radiusDp: Float) {
        val density = components.container.resources.displayMetrics.density
        val radiusPx = radiusDp * density
        if (radiusPx > 0f) {
            components.container.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                }
            }
            components.container.clipToOutline = true
        } else {
            components.container.outlineProvider = null
            components.container.clipToOutline = false
        }
    }

    fun updateSpan(spanCount: Int) {
        val density = components.container.resources.displayMetrics.density
        val badgeSizePx = when {
            spanCount <= 3 -> (24 * density).toInt()
            spanCount <= 5 -> (20 * density).toInt()
            spanCount == 6 -> (17 * density).toInt()
            else -> (14 * density).toInt()
        }
        val iconSizePx = when {
            spanCount <= 3 -> (14 * density).toInt()
            spanCount <= 5 -> (12 * density).toInt()
            spanCount == 6 -> (10 * density).toInt()
            else -> (8 * density).toInt()
        }
        val badgeMarginPx = when {
            spanCount <= 4 -> (6 * density).toInt()
            spanCount <= 6 -> (4 * density).toInt()
            else -> (2 * density).toInt()
        }

        val lp = components.selectionBadge.layoutParams as? FrameLayout.LayoutParams
        if (lp != null) {
            lp.width = badgeSizePx
            lp.height = badgeSizePx
            lp.setMargins(0, badgeMarginPx, badgeMarginPx, 0)
            components.selectionBadge.layoutParams = lp
        }

        val checkLp = components.checkIcon.layoutParams as? FrameLayout.LayoutParams
        if (checkLp != null) {
            checkLp.width = iconSizePx
            checkLp.height = iconSizePx
            components.checkIcon.layoutParams = checkLp
        }

        if (spanCount >= 7) {
            components.durationText.visibility = View.GONE
            components.videoIcon.visibility = View.VISIBLE
        } else {
            components.durationText.visibility = View.VISIBLE
            val fontSize = when {
                spanCount <= 3 -> 10.5f
                spanCount == 4 -> 9.5f
                spanCount == 5 -> 8.5f
                else -> 8f
            }
            components.durationText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
        }
    }

    fun bindThumbnail(item: NativeTimelineItem.Media, spanCount: Int) {
        val targetPx = thumbnailTargetPx(spanCount)
        val cacheKey = thumbnailMemoryKey(item.id, targetPx)

        if (loadedUri == item.uri && components.thumbnailView.drawable != null) {
            return
        }

        // Fast-path 1: Instant synchronous RAM cache hit (0ms latency, zero IPC, zero blanking)
        val memKey = coil3.memory.MemoryCache.Key(cacheKey)
        val cached = imageLoader.memoryCache?.get(memKey)
        if (cached != null) {
            currentDisposable?.dispose()
            currentDisposable = null
            loadedUri = item.uri
            val bitmap = (cached.image as? coil3.BitmapImage)?.bitmap
            if (bitmap != null) {
                components.thumbnailView.setImageBitmap(bitmap)
            } else {
                components.thumbnailView.setImageDrawable(cached.image.asDrawable(itemView.context.resources))
            }
            return
        }

        // Fast-path 2: Rapid continuous streaming background load
        currentDisposable?.dispose()
        loadedUri = item.uri
        components.thumbnailView.setImageDrawable(null)

        val loadUri = if (item.uri != Uri.EMPTY) item.uri else Uri.parse("file://${item.path}")
        val req = ImageRequest.Builder(itemView.context)
            .data(loadUri)
            .size(targetPx, targetPx)
            .precision(Precision.INEXACT)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .target(
                onSuccess = { image ->
                    val bitmap = (image as? coil3.BitmapImage)?.bitmap
                    if (bitmap != null) {
                        components.thumbnailView.setImageBitmap(bitmap)
                    } else {
                        components.thumbnailView.setImageDrawable(image.asDrawable(itemView.context.resources))
                    }
                },
                onError = {
                    components.thumbnailView.setImageDrawable(null)
                }
            )
            .build()

        currentDisposable = imageLoader.enqueue(req)
    }

    fun unbind() {
        currentDisposable?.dispose()
        currentDisposable = null
        loadedUri = null
        components.thumbnailView.setImageDrawable(null)
    }
}

/**
 * Custom [FrameLayout] enforcing exact 1:1 square aspect ratio for media cells.
 */
internal class SquareFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}

private data class MediaViewComponents(
    val container: SquareFrameLayout,
    val thumbnailView: ImageView,
    val selectionBadge: FrameLayout,
    val checkIcon: ImageView,
    val videoBadgeContainer: LinearLayout,
    val videoIcon: ImageView,
    val durationText: TextView,
    val favoriteBadgeView: ImageView
)

private fun createMediaViewComponents(context: Context): MediaViewComponents {
    val density = context.resources.displayMetrics.density

    val container = SquareFrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val thumbnailView = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    container.addView(thumbnailView)

    // Selection Badge (Top-End)
    val selectionBadge = FrameLayout(context).apply {
        val size = (24 * density).toInt()
        val margin = (6 * density).toInt()
        layoutParams = FrameLayout.LayoutParams(size, size).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, margin, margin, 0)
        }
        visibility = View.GONE
    }
    val checkIcon = ImageView(context).apply {
        val iconSize = (14 * density).toInt()
        layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.CENTER
        }
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_ms_check))
    }
    selectionBadge.addView(checkIcon)
    container.addView(selectionBadge)

    // Video Badge (Bottom-End)
    val videoBadgeContainer = LinearLayout(context).apply {
        val margin = (4 * density).toInt()
        val padH = (4 * density).toInt()
        val padV = (2 * density).toInt()
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            setMargins(0, 0, margin, margin)
        }
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(padH, padV, padH, padV)
        visibility = View.GONE
    }
    val videoIcon = ImageView(context).apply {
        val iconSize = (12 * density).toInt()
        layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_ms_videocam))
        setColorFilter(Color.WHITE)
    }
    videoBadgeContainer.addView(videoIcon)

    val durationText = TextView(context).apply {
        val marginStart = (3 * density).toInt()
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(marginStart, 0, 0, 0)
        }
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setTextColor(Color.WHITE)
        setShadowLayer(4f, 0f, 1f, 0xCC000000.toInt())
    }
    videoBadgeContainer.addView(durationText)
    container.addView(videoBadgeContainer)

    // Favorite Badge (Bottom-Start)
    val favoriteBadgeView = ImageView(context).apply {
        val size = (16 * density).toInt()
        val margin = (4 * density).toInt()
        layoutParams = FrameLayout.LayoutParams(size, size).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            setMargins(margin, 0, 0, margin)
        }
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_ms_favorite_filled))
        setColorFilter(0xFFE53935.toInt())
        visibility = View.GONE
    }
    container.addView(favoriteBadgeView)

    return MediaViewComponents(
        container = container,
        thumbnailView = thumbnailView,
        selectionBadge = selectionBadge,
        checkIcon = checkIcon,
        videoBadgeContainer = videoBadgeContainer,
        videoIcon = videoIcon,
        durationText = durationText,
        favoriteBadgeView = favoriteBadgeView
    )
}

/**
 * Hairline item decoration for mathematically uniform grid spacing.
 */
private class GridSpacingItemDecoration(
    private val spacingPx: Int,
    private val spanCountProvider: () -> Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val adapter = parent.adapter ?: return
        val viewType = adapter.getItemViewType(position)
        if (viewType == VIEW_TYPE_HEADER || viewType == VIEW_TYPE_COMPOSE_HEADER) {
            outRect.set(0, 0, 0, 0)
            return
        }

        val spanCount = spanCountProvider().coerceAtLeast(1)
        val lp = view.layoutParams as? GridLayoutManager.LayoutParams
        if (lp != null) {
            val spanIndex = lp.spanIndex
            outRect.left = spanIndex * spacingPx / spanCount
            outRect.right = spacingPx - (spanIndex + 1) * spacingPx / spanCount
            outRect.top = spacingPx
            outRect.bottom = 0
        } else {
            outRect.set(0, spacingPx, 0, 0)
        }
    }
}

/**
 * Fluid, touch-driven drag-to-select controller for [NativePhotonGrid].
 *
 * Implements 120 FPS continuous multi-item selection with edge auto-scrolling
 * and tactile haptic ticks across item boundaries.
 */
private class NativeGridDragSelectHelper(
    private val selectedUrisProvider: () -> Set<String>,
    private val onStartDragSelection: (String, Boolean) -> Unit,
    private val onUpdateDragSelection: (Set<String>) -> Unit,
    private val onEndDragSelection: () -> Unit,
    private val density: Float
) : RecyclerView.OnItemTouchListener {

    private var recyclerView: RecyclerView? = null
    private var adapter: NativePhotonAdapter? = null

    var isDragSelecting = false
        private set

    private var dragStartIndex = RecyclerView.NO_POSITION
    private var lastActiveIndex = RecyclerView.NO_POSITION
    private var isSelectingMode = true

    // Auto-scroll configuration
    private val autoScrollThresholdPx = 70f * density
    private val maxScrollSpeedPx = 28f * density
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var autoScrollActive = false

    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            val rv = recyclerView ?: return
            if (!isDragSelecting) {
                autoScrollActive = false
                return
            }

            val rvHeight = rv.height.toFloat()
            if (rvHeight <= 0f) {
                autoScrollActive = false
                return
            }

            val topPad = rv.paddingTop.toFloat()
            val bottomPad = rv.paddingBottom.toFloat()

            val scrollDelta = when {
                lastTouchY < topPad + autoScrollThresholdPx -> {
                    val factor = ((topPad + autoScrollThresholdPx - lastTouchY) / autoScrollThresholdPx).coerceIn(0f, 1f)
                    -(maxScrollSpeedPx * factor).toInt().coerceAtLeast(1)
                }
                lastTouchY > (rvHeight - bottomPad - autoScrollThresholdPx) -> {
                    val factor = ((lastTouchY - (rvHeight - bottomPad - autoScrollThresholdPx)) / autoScrollThresholdPx).coerceIn(0f, 1f)
                    (maxScrollSpeedPx * factor).toInt().coerceAtLeast(1)
                }
                else -> 0
            }

            if (scrollDelta != 0) {
                rv.scrollBy(0, scrollDelta)
                updateSelectionAtPoint(lastTouchX, lastTouchY)
                rv.postOnAnimation(this)
            } else {
                autoScrollActive = false
            }
        }
    }

    fun attach(rv: RecyclerView, ad: NativePhotonAdapter) {
        this.recyclerView = rv
        this.adapter = ad
    }

    fun detach() {
        finishDragSelect()
        this.recyclerView = null
        this.adapter = null
    }

    fun startDragSelect(position: Int) {
        val rv = recyclerView ?: return
        val ad = adapter ?: return
        if (position < 0 || position >= ad.itemCount) return
        val item = ad.getItem(position) as? NativeTimelineItem.Media ?: return

        isDragSelecting = true
        dragStartIndex = position
        lastActiveIndex = position

        val currentlySelected = selectedUrisProvider().contains(item.uriString)
        isSelectingMode = !currentlySelected

        rv.parent?.requestDisallowInterceptTouchEvent(true)
        rv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        onStartDragSelection(item.uriString, isSelectingMode)
    }

    private fun updateSelectionAtPoint(rawX: Float, rawY: Float) {
        val rv = recyclerView ?: return
        val ad = adapter ?: return
        if (!isDragSelecting || dragStartIndex == RecyclerView.NO_POSITION) return

        val clampedX = rawX.coerceIn(
            rv.paddingLeft.toFloat() + 2f,
            (rv.width - rv.paddingRight).toFloat().coerceAtLeast(rv.paddingLeft + 4f) - 2f
        )
        val clampedY = rawY.coerceIn(
            rv.paddingTop.toFloat() + 2f,
            (rv.height - rv.paddingBottom).toFloat().coerceAtLeast(rv.paddingTop + 4f) - 2f
        )

        val child = rv.findChildViewUnder(clampedX, clampedY) ?: return
        val pos = rv.getChildAdapterPosition(child)
        if (pos == RecyclerView.NO_POSITION || pos < 0 || pos >= ad.itemCount) return
        if (pos == lastActiveIndex) return

        lastActiveIndex = pos
        val start = minOf(dragStartIndex, pos)
        val end = maxOf(dragStartIndex, pos)

        val uris = mutableSetOf<String>()
        for (i in start..end) {
            val it = ad.getItem(i)
            if (it is NativeTimelineItem.Media) {
                uris.add(it.uriString)
            }
        }

        if (uris.isNotEmpty()) {
            rv.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onUpdateDragSelection(uris)
        }
    }

    private fun checkStartAutoScroll() {
        val rv = recyclerView ?: return
        val rvHeight = rv.height.toFloat()
        if (rvHeight <= 0f) return

        val topPad = rv.paddingTop.toFloat()
        val bottomPad = rv.paddingBottom.toFloat()

        val needsScroll = lastTouchY < topPad + autoScrollThresholdPx ||
                lastTouchY > (rvHeight - bottomPad - autoScrollThresholdPx)

        if (needsScroll && !autoScrollActive) {
            autoScrollActive = true
            rv.postOnAnimation(autoScrollRunnable)
        }
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (!isDragSelecting) return false

        if (e.pointerCount > 1) {
            finishDragSelect()
            return false
        }

        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                lastTouchX = e.x
                lastTouchY = e.y
                updateSelectionAtPoint(e.x, e.y)
                checkStartAutoScroll()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                finishDragSelect()
                return true
            }
        }
        return true
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (!isDragSelecting) return

        if (e.pointerCount > 1) {
            finishDragSelect()
            return
        }

        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                lastTouchX = e.x
                lastTouchY = e.y
                updateSelectionAtPoint(e.x, e.y)
                checkStartAutoScroll()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                finishDragSelect()
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept && isDragSelecting) {
            recyclerView?.parent?.requestDisallowInterceptTouchEvent(true)
        }
    }

    fun finishDragSelect() {
        if (!isDragSelecting) return
        isDragSelecting = false
        autoScrollActive = false
        recyclerView?.removeCallbacks(autoScrollRunnable)
        dragStartIndex = RecyclerView.NO_POSITION
        lastActiveIndex = RecyclerView.NO_POSITION
        recyclerView?.parent?.requestDisallowInterceptTouchEvent(false)
        onEndDragSelection()
    }
}
