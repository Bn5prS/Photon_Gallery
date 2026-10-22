package com.inferno.gallery.ui

import android.Manifest
import com.inferno.gallery.ui.utils.pressScale
import com.inferno.gallery.ui.components.PhotonEmptyState
import com.inferno.gallery.ui.components.thumbnailMemoryKey
import com.inferno.gallery.ui.components.thumbnailTargetPx
import com.inferno.gallery.ui.theme.MotionTokens
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.graphics.drawable.Animatable
import androidx.compose.foundation.Image
import androidx.paging.compose.LazyPagingItems
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.gestures.scrollBy

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text


import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.bitmapConfig
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import coil3.asImage
import androidx.compose.ui.platform.LocalHapticFeedback
import com.inferno.gallery.ui.utils.tick
import com.inferno.gallery.ui.utils.thud
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.ui.theme.ShapeExtraSmall
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeFull
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import com.inferno.gallery.ui.GalleryListItem
import com.inferno.gallery.data.MemoryDayGroup
import com.inferno.gallery.ui.components.MemoriesSpotlightCard
import com.inferno.gallery.ui.components.MemoryReelViewer
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector




// Removed resolvedUriCache

// Profiling switch for the prefetch pipeline (kept false to prevent thread competition during fling deceleration)
private const val PREFETCH_EXPERIMENT_ENABLED = false

@OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GalleryScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (mediaId: String, bucketName: String?, query: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    bucketName: String? = null,
    isMainTab: Boolean = false,
    topBarCollapseFractionProvider: () -> Float = { 0f },
    onNavigateToSettings: () -> Unit = {}
) {
    val selectedFilterIndex by viewModel.selectedFilterIndex.collectAsState()
    val lazyGridState = androidx.compose.runtime.key(bucketName) {
        val savedPosition = remember(bucketName) { viewModel.getSavedGridScrollPosition(bucketName) }
        rememberLazyGridState(
            initialFirstVisibleItemIndex = savedPosition.first,
            initialFirstVisibleItemScrollOffset = savedPosition.second
        )
    }

    LaunchedEffect(bucketName) {
        viewModel.setBucket(bucketName)
    }

    val pagedMedia = viewModel.pagedMedia.collectAsLazyPagingItems()

    var previousFilter by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(selectedFilterIndex) }
    var previousBucket by rememberSaveable { mutableStateOf(bucketName) }

    // Only scroll to top when filter or bucket ACTUALLY changes, NOT when returning from detail screen
    LaunchedEffect(selectedFilterIndex, bucketName) {
        if (previousFilter != selectedFilterIndex || previousBucket != bucketName) {
            previousFilter = selectedFilterIndex
            previousBucket = bucketName
            viewModel.resetGridScrollPosition(bucketName)
            lazyGridState.scrollToItem(0, 0)
        }
    }

    LaunchedEffect(viewModel, bucketName) {
        if (bucketName == com.inferno.gallery.data.BucketNames.VIDEOS) {
            viewModel.videosScrollToTopTrigger.collect {
                lazyGridState.animateScrollToItem(0, 0)
            }
        } else {
            viewModel.photosScrollToTopTrigger.collect {
                lazyGridState.animateScrollToItem(0, 0)
            }
        }
    }

    // Persist scroll position when scrolling settles so returning from detail or navigation preserves exact place
    LaunchedEffect(lazyGridState, bucketName) {
        snapshotFlow { lazyGridState.isScrollInProgress }
            .distinctUntilChanged()
            .drop(1)
            .collect { inProgress ->
                if (!inProgress) {
                    val index = lazyGridState.firstVisibleItemIndex
                    val offset = lazyGridState.firstVisibleItemScrollOffset
                    viewModel.saveGridScrollPosition(bucketName, index, offset)
                }
            }
    }

    // If initial composition clamped to 0 while PagingData was loading, restore to saved position once items load
    LaunchedEffect(pagedMedia.itemCount) {
        val (savedIndex, savedOffset) = viewModel.getSavedGridScrollPosition(bucketName)
        if (savedIndex > 0 && pagedMedia.itemCount > savedIndex && lazyGridState.firstVisibleItemIndex == 0) {
            lazyGridState.scrollToItem(savedIndex, savedOffset)
        }
    }

    val viewMode by viewModel.viewMode.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUrisRaw by viewModel.selectedUris.collectAsState()
    // Re-wrap so the grid receives a stable, content-comparable type. See StableUriSet.
    val selectedUris = remember(selectedUrisRaw) { StableUriSet(selectedUrisRaw) }
    val gridCellsCount by viewModel.gridCellsCount.collectAsState()
    val thumbnailCornerRadius by viewModel.thumbnailCornerRadius.collectAsState()
    val timelineLayoutMode by viewModel.timelineLayoutMode.collectAsState()
    val dateHeaderAlignment by viewModel.dateHeaderAlignment.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val totalMediaCount by viewModel.currentMediaCount.collectAsState()

    // ── On This Day Memories ──
    val onThisDayMemories by viewModel.onThisDayMemories.collectAsState()
    val isMemoriesDismissed by viewModel.isMemoriesDismissed.collectAsState()
    val showMemoriesSetting by viewModel.showMemoriesSetting.collectAsState()
    var activeMemoryReelGroup by remember { mutableStateOf<MemoryDayGroup?>(null) }

    val shouldShowMemoriesHeader = bucketName == null &&
        isMainTab &&
        !isSelectionMode &&
        showMemoriesSetting &&
        onThisDayMemories.isNotEmpty()

    // NOTE: pagedMedia.itemCount is deliberately NOT read in this composition scope. Reading
    // it here subscribed the entire GalleryScreen restart scope to the paging snapshot, so
    // every 120-item page append recomposed the screen (and with it the whole grid). Each
    // consumer below reads it inside its own scope instead.
    val context = LocalContext.current

    BackHandler(enabled = activeMemoryReelGroup != null) {
        activeMemoryReelGroup = null
    }

    BackHandler(enabled = isSelectionMode && activeMemoryReelGroup == null) {
        viewModel.clearSelection()
    }

    val coroutineScope = rememberCoroutineScope()

    val onMediaClick = remember(viewModel, bucketName, onPhotoClick, lazyGridState) {
        { item: GalleryItem ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleSelection(item.uri.toString())
            } else {
                viewModel.saveGridScrollPosition(
                    bucketName,
                    lazyGridState.firstVisibleItemIndex,
                    lazyGridState.firstVisibleItemScrollOffset
                )
                viewModel.setInitialDetailItem(item, bucketName)
                val query = if (bucketName == "search_text") viewModel.searchQuery.value else null
                onPhotoClick(item.id, bucketName, query)
            }
        }
    }

    // Stable callback reference. A bound `viewModel::setGridCellsCount` KFunction is allocated
    // fresh on every GalleryScreen recomposition and KFunction does not override equals(), so
    // under strong skipping PhotonGrid could never skip and the whole visible grid was
    // re-subcomposed whenever any screen-level state changed (including during every fling).
    val onGridCountChange = remember(viewModel) {
        { count: Int -> viewModel.setGridCellsCount(count) }
    }

    var activeDateBadge by remember { mutableStateOf<String?>(null) }
    var showDateBadge by remember { mutableStateOf(false) }

    // ── Dynamic Date Badge Scroll Observer ───────────────────────────────────
    // Keyed only on the grid state: keying on pagedMedia.itemCount tore down and rebuilt this
    // collector (and its SimpleDateFormat) on every 120-item page append. The body reads
    // itemCount directly, which inside a coroutine is not a composition subscription.
    LaunchedEffect(lazyGridState) {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        var cachedDateSeconds = -1L
        var cachedDateString = ""

        @OptIn(kotlinx.coroutines.FlowPreview::class)
        snapshotFlow { lazyGridState.firstVisibleItemIndex to lazyGridState.isScrollInProgress }
            .debounce(60L)
            .collectLatest { (index, inProgress) ->
                if (index in 0 until pagedMedia.itemCount) {
                    val listItem = pagedMedia.peek(index)
                    val dateStr = when (listItem) {
                        is GalleryListItem.Header -> listItem.title
                        is GalleryListItem.Item -> {
                            val seconds = listItem.galleryItem.dateAdded
                            if (seconds > 0) {
                                val dayStart = (seconds / 86400) * 86400
                                if (dayStart != cachedDateSeconds) {
                                    cachedDateSeconds = dayStart
                                    cachedDateString = dateFormat.format(Date(seconds * 1000L))
                                }
                                cachedDateString
                            } else null
                        }
                        null -> null
                    }
                    if (!dateStr.isNullOrBlank()) {
                        activeDateBadge = dateStr
                    }
                }
                if (inProgress) {
                    showDateBadge = true
                } else if (showDateBadge) {
                    delay(1200)
                    showDateBadge = false
                }
            }
    }


    // ── Scroll-ahead thumbnail prefetcher ──────────────────────────────────────────────────
    // Pre-warms Coil's unified hardware bitmap pool for items about to scroll into view.
    // Strictly bounded (2 rows ahead, direction-aware, duplicates skipped).
    if (PREFETCH_EXPERIMENT_ENABLED) LaunchedEffect(lazyGridState, gridCellsCount) {
        val thumbSizePx = thumbnailTargetPx(gridCellsCount)
        val imageLoader = coil3.SingletonImageLoader.get(context)
        var previousFirstIndex = lazyGridState.firstVisibleItemIndex

        snapshotFlow { lazyGridState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { firstIndex ->
                val scrollingDown = firstIndex >= previousFirstIndex
                previousFirstIndex = firstIndex

                // Debounce burst ticks during high-velocity flings: only the newest position survives
                delay(60L)

                val layoutInfo = lazyGridState.layoutInfo
                val viewportWidth = layoutInfo.viewportSize.width.toFloat()
                val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                if (viewportWidth <= 0f || viewportHeight <= 0f) return@collectLatest

                val rowHeightPx = maxOf(viewportWidth / gridCellsCount, 1f)
                val rowsVisible = kotlin.math.ceil(viewportHeight / rowHeightPx).toInt().coerceAtLeast(4)
                val visibleCount = gridCellsCount * rowsVisible
                val preloadCount = gridCellsCount * 2

                val start: Int
                val end: Int
                if (scrollingDown) {
                    start = firstIndex + visibleCount
                    end = minOf(start + preloadCount, pagedMedia.itemCount - 1)
                } else {
                    end = firstIndex - 1
                    start = maxOf(end - preloadCount, 0)
                }
                if (start > end || start >= pagedMedia.itemCount) return@collectLatest

                for (i in start..end) {
                    val listItem = pagedMedia.peek(i) as? GalleryListItem.Item ?: continue
                    val galleryItem = listItem.galleryItem
                    val cacheKey = thumbnailMemoryKey(galleryItem.id, thumbSizePx)
                    if (imageLoader.memoryCache?.get(coil3.memory.MemoryCache.Key(cacheKey)) != null) continue
                    val req = coil3.request.ImageRequest.Builder(context)
                        .data(if (galleryItem.isVideo) galleryItem.uri else (if (galleryItem.localExists) galleryItem.resolvedUri else galleryItem.uri))
                        .size(thumbSizePx, thumbSizePx)
                        .precision(coil3.size.Precision.INEXACT)
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .build()
                    imageLoader.enqueue(req)
                }
            }
    }
    // ──────────────────────────────────────────────────────────────────────────────────────

    val isGridEmpty = remember(pagedMedia) {
        derivedStateOf { pagedMedia.itemCount == 0 }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        com.inferno.gallery.ui.components.PhotonGrid(
            pagedMedia = pagedMedia,
            lazyGridState = lazyGridState,
            gridCellsCount = gridCellsCount,
            onGridCountChange = onGridCountChange,
            isSelectionMode = isSelectionMode,
            selectedUris = selectedUris,
            onMediaClick = onMediaClick,
            viewMode = viewMode,
            thumbnailCornerRadius = thumbnailCornerRadius,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            viewModel = viewModel,
            modifier = modifier,
            contentPadding = contentPadding,
            timelineLayoutMode = timelineLayoutMode,
            dateHeaderAlignment = dateHeaderAlignment,
            showMatchBadge = (bucketName == "search_text" || bucketName == "search_smart"),
            headerContent = if (shouldShowMemoriesHeader) {
                {
                    MemoriesSpotlightCard(
                        memoryGroups = onThisDayMemories,
                        isDismissed = isMemoriesDismissed,
                        onOpenMemory = { group ->
                            activeMemoryReelGroup = group
                        },
                        onDismiss = {
                            viewModel.dismissMemories()
                        },
                        onRestore = {
                            viewModel.uncollapseMemories()
                        }
                    )
                }
            } else null
        )

        // ── Initial sync loading ────────────────────────────────────────────
        val isSyncRunning by viewModel.isInitialSyncRunning.collectAsState()
        val isNotLoading = pagedMedia.loadState.refresh is androidx.paging.LoadState.NotLoading
        AnimatedVisibility(
            visible = isGridEmpty.value && isSyncRunning,
            enter = fadeIn(MotionTokens.snappySpring()),
            exit  = fadeOut(MotionTokens.snappySpring()),
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.material3.ContainedLoadingIndicator(
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Scanning media…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Empty state ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isGridEmpty.value && isNotLoading && !isSyncRunning,
            enter = fadeIn(MotionTokens.snappySpring()) +
                    scaleIn(MotionTokens.snappySpring(), initialScale = 0.7f),
            exit  = fadeOut(MotionTokens.snappySpring()),
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            val emptyIcon = when (bucketName) {
                "Trash" -> ImageVector.vectorResource(R.drawable.ic_ms_delete)
                "Favorites" -> ImageVector.vectorResource(R.drawable.ic_ms_check_circle)
                null -> ImageVector.vectorResource(R.drawable.ic_ms_image)
                else -> ImageVector.vectorResource(R.drawable.ic_ms_image)
            }
            val (title, subtitle) = when (bucketName) {
                "Trash" -> "Trash is empty" to "Deleted photos will appear here."
                "Favorites" -> "No favorites yet" to "Tap ❤️ on any photo to save it here."
                null -> "No photos yet" to "Photos and videos on your device will appear here."
                else -> "Album is empty" to "\"$bucketName\" has no photos or videos."
            }
            PhotonEmptyState(
                icon = emptyIcon,
                title = title,
                subtitle = subtitle,
                modifier = Modifier.fillMaxSize(),
                action = null
            )
        }

        // ── Top Gradient Scrim (Google Photos Edge-to-Edge Status Bar Vignette) ──
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val scrimHeight = statusBarTop + 36.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(scrimHeight)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to MaterialTheme.colorScheme.surfaceContainerLow,
                        0.35f to MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.90f),
                        0.65f to MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f),
                        0.85f to MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.18f),
                        1.0f to Color.Transparent
                    )
                )
        )

        // ── Dynamic Date Badge ──────────────────────────────────────────────
        val topBarContentPadding = contentPadding.calculateTopPadding()
        val density = LocalDensity.current
        val maxUpwardShiftPx = remember(topBarContentPadding, statusBarTop, density) {
            with(density) {
                // Expanded top offset: topBarContentPadding + 12dp.
                // Collapsed top offset: statusBarTop + 8dp.
                val expandedTop = topBarContentPadding + 12.dp
                val collapsedTop = statusBarTop + 8.dp
                (expandedTop - collapsedTop).coerceAtLeast(0.dp).toPx()
            }
        }

        DynamicDateBadge(
            // Lambda, not a value: reading showDateBadge/activeDateBadge in the screen's
            // composition subscribed the whole GalleryScreen to state that is written every
            // ~60ms while scrolling, forcing a full-screen recompose mid-fling. The reads now
            // happen inside DynamicDateBadge's own (small) restart scope.
            visible = { showDateBadge },
            dateText = { activeDateBadge },
            modifier = Modifier
                .align(if (dateHeaderAlignment == "CENTER") Alignment.TopCenter else Alignment.TopStart)
                .padding(
                    top = topBarContentPadding + 12.dp,
                    start = if (dateHeaderAlignment == "CENTER") 0.dp else 16.dp
                )
                .graphicsLayer {
                    translationY = -topBarCollapseFractionProvider() * maxUpwardShiftPx
                }
        )

        FastScroller(
            lazyGridState = lazyGridState,
            pagedMedia = pagedMedia,
            headerOffset = if (shouldShowMemoriesHeader) 1 else 0,
            gridCellsCount = gridCellsCount,
            totalMediaCount = totalMediaCount,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 56.dp
                )
        )

        // Full-screen Memory Reel Viewer
        val activeGroup = activeMemoryReelGroup
        if (activeGroup != null) {
            MemoryReelViewer(
                group = activeGroup,
                favoriteIds = favoriteIds,
                onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                onOpenPhotoDetail = { mediaId ->
                    activeMemoryReelGroup = null
                    onPhotoClick(mediaId, null, null)
                },
                onDismiss = { activeMemoryReelGroup = null },
                stripMetadataFlow = viewModel.settingsRepository.stripMetadataOnShareFlow
            )
        }
    }
}


fun formatDuration(millis: Long?): String {
    if (millis == null || millis <= 0) return ""
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        val s = if (seconds < 10) "0$seconds" else "$seconds"
        val m = if (minutes < 10) "0$minutes" else "$minutes"
        "$hours:$m:$s"
    } else {
        val s = if (seconds < 10) "0$seconds" else "$seconds"
        "$minutes:$s"
    }
}



@Composable
fun FastScroller(
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    pagedMedia: LazyPagingItems<GalleryListItem>,
    modifier: Modifier = Modifier,
    headerOffset: Int = 0,
    gridCellsCount: Int = 3,
    totalMediaCount: Int = 0
) {
    // Read here, not in the caller: keeps the paging snapshot subscription out of the
    // screen's restart scope.
    if (pagedMedia.itemCount < 50 && totalMediaCount < 50) return

    val fullDateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    // Sample header density once when the first page lands, then lock it so the scrollbar track
    // denominator NEVER fluctuates or jumps while scrolling or loading subsequent pages.
    var sampledHeaderRatio by remember { mutableFloatStateOf(0.06f) }
    var hasSampledHeaders by remember { mutableStateOf(false) }

    val loadedCount = pagedMedia.itemCount
    if (!hasSampledHeaders && loadedCount >= 40) {
        val sampleSize = minOf(loadedCount, 150)
        var headers = 0
        for (i in 0 until sampleSize) {
            val item = if (i in 0 until loadedCount) pagedMedia.peek(i) else null
            if (item is GalleryListItem.Header) headers++
        }
        sampledHeaderRatio = (headers.toFloat() / sampleSize.toFloat()).coerceIn(0.02f, 0.25f)
        hasSampledHeaders = true
    }

    // Stable upfront total items count derived from Room SQLite count + sampled date headers:
    // Completely eliminates denominator expansion and jumping during fresh scrolling,
    // while accurately reflecting the entire gallery without premature bottom-sticking!
    val totalEstimatedItems = remember(totalMediaCount, sampledHeaderRatio, loadedCount > 0) {
        val baseCount = if (totalMediaCount > 0) totalMediaCount else maxOf(loadedCount, 1)
        val estimatedHeaders = (baseCount.toFloat() * sampledHeaderRatio).toInt().coerceAtLeast(0)
        maxOf(baseCount + estimatedHeaders + headerOffset, loadedCount)
    }

    com.inferno.gallery.ui.components.ExpressiveScrollBar(
        gridState = lazyGridState,
        totalItemsCount = totalEstimatedItems,
        dragLabelProvider = { rawIndex ->
            val index = rawIndex - headerOffset
            if (index in 0 until pagedMedia.itemCount) {
                when (val item = pagedMedia.peek(index)) {
                    is GalleryListItem.Header -> item.title
                    is GalleryListItem.Item -> {
                        val sec = item.galleryItem.dateAdded
                        if (sec > 0) fullDateFormat.format(Date(sec * 1000L)) else null
                    }
                    null -> null
                }
            } else null
        },
        modifier = modifier
    )
}

@Composable
private fun DynamicDateBadge(
    visible: () -> Boolean,
    dateText: () -> String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible() && !dateText().isNullOrBlank(),
        enter = fadeIn(animationSpec = MotionTokens.snappySpring()) + slideInVertically(
            initialOffsetY = { -it },
            animationSpec = MotionTokens.snappySpring()
        ),
        exit = fadeOut(animationSpec = MotionTokens.snappySpring()) + slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = MotionTokens.snappySpring()
        ),
        modifier = modifier
    ) {
        Surface(
            shape = ShapeFull,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
            shadowElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        ) {
            Text(
                text = dateText() ?: "",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    }
}


