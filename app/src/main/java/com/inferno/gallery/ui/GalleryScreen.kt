package com.inferno.gallery.ui

import androidx.compose.ui.res.stringResource

import android.Manifest
import com.inferno.gallery.ui.utils.pressScale
import com.inferno.gallery.ui.components.PhotonEmptyState
import com.inferno.gallery.ui.components.thumbnailMemoryKey
import com.inferno.gallery.ui.components.thumbnailTargetPx
import com.inferno.gallery.ui.components.toGalleryItem
import com.inferno.gallery.data.TimelineLayoutMode
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

@OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    val timelineLayoutMode by viewModel.timelineLayoutMode.collectAsState()

    LaunchedEffect(bucketName) {
        viewModel.setBucket(bucketName)
    }

    val timelineSnapshot by viewModel.nativeTimelineSnapshot.collectAsState()
    val scopedTimelineItems = remember(bucketName) {
        mutableStateOf(
            if (timelineSnapshot.isLoaded && timelineSnapshot.bucket == bucketName) {
                timelineSnapshot.items
            } else {
                viewModel.getCachedTimelineItems(bucketName)
            }
        )
    }
    if (timelineSnapshot.isLoaded && timelineSnapshot.bucket == bucketName) {
        scopedTimelineItems.value = timelineSnapshot.items
    }
    val nativeTimelineItemsRaw = scopedTimelineItems.value
    val photoStacksEnabled by viewModel.photoStacksEnabled.collectAsState()
    val photoStackIndex by viewModel.photoStackIndex.collectAsState()
    val nativeTimelineItems = remember(nativeTimelineItemsRaw, photoStacksEnabled, photoStackIndex, bucketName, selectedFilterIndex) {
        if (photoStacksEnabled && bucketName == null && selectedFilterIndex == 0 && !photoStackIndex.isEmpty) {
            com.inferno.gallery.data.PhotoStackPlanner.collapseNativeTimeline(nativeTimelineItemsRaw, photoStackIndex)
        } else {
            nativeTimelineItemsRaw
        }
    }
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val albumMediaFilter by viewModel.albumMediaFilter.collectAsState()

    var showNativeDateBadge by remember { mutableStateOf(false) }
    var activeNativeDateBadge by remember { mutableStateOf("") }

    var showMosaicDateBadge by remember { mutableStateOf(false) }
    var activeMosaicDateBadge by remember { mutableStateOf<String?>(null) }
    var isMosaicEmpty by remember { mutableStateOf(false) }
    var isMosaicNotLoading by remember { mutableStateOf(false) }

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUrisRaw by viewModel.selectedUris.collectAsState()
    // Re-wrap so the grid receives a stable, content-comparable type. See StableUriSet.
    val selectedUris = remember(selectedUrisRaw) { StableUriSet(selectedUrisRaw) }
    val gridCellsCount by viewModel.gridCellsCount.collectAsState()
    val thumbnailCornerRadius by viewModel.thumbnailCornerRadius.collectAsState()
    val dateHeaderAlignment by viewModel.dateHeaderAlignment.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val totalMediaCount by viewModel.currentMediaCount.collectAsState()

    // ── On This Day Memories ──
    val onThisDayMemories by viewModel.onThisDayMemories.collectAsState()
    val isMemoriesDismissed by viewModel.isMemoriesDismissed.collectAsState()
    val showMemoriesSetting by viewModel.showMemoriesSetting.collectAsState()
    val memoriesCarouselStyle by viewModel.memoriesCarouselStyle.collectAsState()
    var activeMemoryReelGroup by remember { mutableStateOf<MemoryDayGroup?>(null) }

    val shouldShowMemoriesHeader = bucketName == null &&
        isMainTab &&
        !isSelectionMode &&
        showMemoriesSetting &&
        onThisDayMemories.isNotEmpty()

    BackHandler(enabled = activeMemoryReelGroup != null) {
        activeMemoryReelGroup = null
    }

    BackHandler(enabled = isSelectionMode && activeMemoryReelGroup == null) {
        viewModel.clearSelection()
    }

    val onNativeMediaClick = remember(viewModel, bucketName, onPhotoClick) {
        { item: GalleryItem ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleSelection(item.uri.toString())
            } else {
                viewModel.setInitialDetailItem(item, bucketName)
                val query = if (bucketName == "search_text" || bucketName == "search_smart") viewModel.searchQuery.value else null
                onPhotoClick(item.id, bucketName, query)
            }
        }
    }

    val onGridCountChange = remember(viewModel) {
        { count: Int -> viewModel.setGridCellsCount(count) }
    }
    val onToggleSelectionStable = remember(viewModel) {
        { media: com.inferno.gallery.data.NativeTimelineItem.Media -> viewModel.toggleSelection(media.uriString) }
    }
    val onStartDragSelectionStable = remember(viewModel) {
        { uri: String, isSelecting: Boolean -> viewModel.startDragSelection(uri, isSelecting) }
    }
    val onUpdateDragSelectionStable = remember(viewModel) {
        { uris: Set<String> -> viewModel.updateDragSelection(uris) }
    }
    val onEndDragSelectionStable = remember(viewModel) {
        { viewModel.endDragSelection() }
    }
    val onScrolledAwayFromTopStable = remember(viewModel) {
        { isScrolled: Boolean -> viewModel.setTopBarCollapsed(isScrolled) }
    }
    val onSaveScrollPositionStable = remember(viewModel, bucketName) {
        { index: Int, offset: Int -> viewModel.saveGridScrollPosition(bucketName, index, offset) }
    }
    val onScrollDateStateChangedStable = remember {
        { visible: Boolean, dateText: String ->
            showNativeDateBadge = visible
            if (dateText.isNotEmpty()) {
                activeNativeDateBadge = dateText
            }
        }
    }

    val isGridEmpty = remember(timelineLayoutMode, isMosaicEmpty, nativeTimelineItems) {
        derivedStateOf {
            if (timelineLayoutMode == TimelineLayoutMode.EDITORIAL_MOSAIC) {
                isMosaicEmpty
            } else {
                nativeTimelineItems.isEmpty()
            }
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    val haptic = LocalHapticFeedback.current

    val memoriesHeaderContent = remember(shouldShowMemoriesHeader, onThisDayMemories, isMemoriesDismissed, memoriesCarouselStyle) {
        if (shouldShowMemoriesHeader) {
            @Composable {
                MemoriesSpotlightCard(
                    memoryGroups = onThisDayMemories,
                    isDismissed = isMemoriesDismissed,
                    style = memoriesCarouselStyle,
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
    }

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.refreshMedia()
        },
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = {
            androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding())
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
        if (timelineLayoutMode == TimelineLayoutMode.EDITORIAL_MOSAIC) {
            EditorialMosaicGridHost(
                viewModel = viewModel,
                bucketName = bucketName,
                selectedFilterIndex = selectedFilterIndex,
                sortOrder = sortOrder,
                viewMode = viewMode,
                albumMediaFilter = albumMediaFilter,
                gridCellsCount = gridCellsCount,
                onGridCountChange = onGridCountChange,
                isSelectionMode = isSelectionMode,
                selectedUris = selectedUris,
                onPhotoClick = onPhotoClick,
                thumbnailCornerRadius = thumbnailCornerRadius,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                contentPadding = contentPadding,
                dateHeaderAlignment = dateHeaderAlignment,
                shouldShowMemoriesHeader = shouldShowMemoriesHeader,
                totalMediaCount = totalMediaCount,
                isRefreshing = isRefreshing,
                headerContent = memoriesHeaderContent,
                onDateBadgeChanged = { visible, dateText ->
                    showMosaicDateBadge = visible
                    if (!dateText.isNullOrBlank()) {
                        activeMosaicDateBadge = dateText
                    }
                },
                onLoadStateChanged = { empty, notLoading ->
                    isMosaicEmpty = empty
                    isMosaicNotLoading = notLoading
                },
                modifier = modifier
            )
        } else {
            androidx.compose.runtime.key(bucketName) {
                val (savedIndex, savedOffset) = remember(bucketName) { viewModel.getSavedGridScrollPosition(bucketName) }
                val nativeScrollToTopKey = remember(selectedFilterIndex, bucketName, sortOrder, viewMode) {
                    Triple(selectedFilterIndex, bucketName, sortOrder to viewMode)
                }
                com.inferno.gallery.ui.components.NativePhotonGrid(
                    items = nativeTimelineItems,
                    initialFirstVisibleItemIndex = savedIndex,
                    initialFirstVisibleItemScrollOffset = savedOffset,
                    gridCellsCount = gridCellsCount,
                    onGridCountChange = onGridCountChange,
                    isSelectionMode = isSelectionMode,
                    selectedUris = selectedUris,
                    favoriteIds = favoriteIds,
                    onMediaClick = onNativeMediaClick,
                    onToggleSelection = onToggleSelectionStable,
                    onMediaLongClick = onToggleSelectionStable,
                    onStartDragSelection = onStartDragSelectionStable,
                    onUpdateDragSelection = onUpdateDragSelectionStable,
                    onEndDragSelection = onEndDragSelectionStable,
                    thumbnailCornerRadius = thumbnailCornerRadius,
                    contentPadding = contentPadding,
                    dateHeaderAlignment = dateHeaderAlignment,
                    headerContent = memoriesHeaderContent,
                    scrollToTopTrigger = if (bucketName == com.inferno.gallery.data.BucketNames.VIDEOS) {
                        viewModel.videosScrollToTopTrigger
                    } else {
                        viewModel.photosScrollToTopTrigger
                    },
                    scrollToTopKey = nativeScrollToTopKey,
                    onScrolledAwayFromTop = onScrolledAwayFromTopStable,
                    onSaveScrollPosition = onSaveScrollPositionStable,
                    onScrollDateStateChanged = onScrollDateStateChangedStable
                )
            }
        }

        // ── Initial sync loading ────────────────────────────────────────────
        val isSyncRunning by viewModel.isInitialSyncRunning.collectAsState()
        val isNativeTimelineLoading by viewModel.isNativeTimelineLoading.collectAsState()
        val isNativeTimelineLoaded by viewModel.isNativeTimelineLoaded.collectAsState()
        val isNotLoading = if (timelineLayoutMode == TimelineLayoutMode.EDITORIAL_MOSAIC) {
            isMosaicNotLoading
        } else {
            isNativeTimelineLoaded && !isNativeTimelineLoading
        }
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
                "search_text", "search_smart" -> ImageVector.vectorResource(R.drawable.ic_ms_search)
                null -> ImageVector.vectorResource(R.drawable.ic_ms_image)
                else -> ImageVector.vectorResource(R.drawable.ic_ms_image)
            }
            val (title, subtitle) = when (bucketName) {
                "Trash" -> "Trash is empty" to "Deleted photos will appear here."
                "Favorites" -> "No favorites yet" to "Tap ❤️ on any photo to save it here."
                "search_text", "search_smart" -> "No results found" to "No matching photos or videos found."
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

        // ── Top Gradient Scrim (Refined Status Bar & Notch Vignette) ──────────
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        if (contentPadding.calculateTopPadding() > 0.dp) {
            val scrimHeight = statusBarTop + 16.dp
            val scrimColor = MaterialTheme.colorScheme.surfaceContainerLow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scrimHeight)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            0.0f to scrimColor.copy(alpha = 0.82f),
                            0.35f to scrimColor.copy(alpha = 0.60f),
                            0.65f to scrimColor.copy(alpha = 0.32f),
                            0.85f to scrimColor.copy(alpha = 0.10f),
                            1.0f to Color.Transparent
                        )
                    )
            )
        }

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

        val isCenterAlignment = dateHeaderAlignment == "CENTER"
        DynamicDateBadge(
            // Lambda, not a value: reading showDateBadge/activeDateBadge in the screen's
            // composition subscribed the whole GalleryScreen to state that is written every
            // ~60ms while scrolling, forcing a full-screen recompose mid-fling. The reads now
            // happen inside DynamicDateBadge's own (small) restart scope.
            visible = {
                if (timelineLayoutMode == TimelineLayoutMode.EDITORIAL_MOSAIC) showMosaicDateBadge else showNativeDateBadge
            },
            dateText = {
                if (timelineLayoutMode == TimelineLayoutMode.EDITORIAL_MOSAIC) activeMosaicDateBadge else activeNativeDateBadge
            },
            isCenter = isCenterAlignment,
            modifier = Modifier
                .align(if (isCenterAlignment) Alignment.TopCenter else Alignment.TopStart)
                    .padding(
                        top = topBarContentPadding + 12.dp,
                        start = if (isCenterAlignment) 0.dp else 4.dp
                    )
                    .graphicsLayer {
                        translationY = -topBarCollapseFractionProvider() * maxUpwardShiftPx
                    }
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
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun androidx.compose.foundation.layout.BoxScope.EditorialMosaicGridHost(
    viewModel: GalleryViewModel,
    bucketName: String?,
    selectedFilterIndex: Int,
    sortOrder: SortOrder,
    viewMode: ViewMode,
    albumMediaFilter: com.inferno.gallery.data.AlbumMediaFilter,
    gridCellsCount: Int,
    onGridCountChange: (Int) -> Unit,
    isSelectionMode: Boolean,
    selectedUris: StableUriSet,
    onPhotoClick: (mediaId: String, bucketName: String?, query: String?) -> Unit,
    thumbnailCornerRadius: Float,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    contentPadding: PaddingValues,
    dateHeaderAlignment: String,
    shouldShowMemoriesHeader: Boolean,
    totalMediaCount: Int,
    isRefreshing: Boolean,
    headerContent: (@Composable () -> Unit)?,
    onDateBadgeChanged: (Boolean, String?) -> Unit,
    onLoadStateChanged: (isEmpty: Boolean, isNotLoading: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyGridState = androidx.compose.runtime.key(bucketName) {
        val savedPosition = remember(bucketName) { viewModel.getSavedGridScrollPosition(bucketName) }
        rememberLazyGridState(
            initialFirstVisibleItemIndex = savedPosition.first,
            initialFirstVisibleItemScrollOffset = savedPosition.second
        )
    }

    val pagedMedia = viewModel.pagedMedia.collectAsLazyPagingItems()

    var previousFilter by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(selectedFilterIndex) }
    var previousBucket by rememberSaveable { mutableStateOf(bucketName) }
    var previousSortOrder by rememberSaveable { mutableStateOf(sortOrder) }
    var previousViewMode by rememberSaveable { mutableStateOf(viewMode) }
    var previousAlbumFilter by rememberSaveable { mutableStateOf(albumMediaFilter) }

    LaunchedEffect(selectedFilterIndex, bucketName, sortOrder, viewMode, albumMediaFilter) {
        if (previousFilter != selectedFilterIndex || previousBucket != bucketName || previousSortOrder != sortOrder || previousViewMode != viewMode || previousAlbumFilter != albumMediaFilter) {
            previousFilter = selectedFilterIndex
            previousBucket = bucketName
            previousSortOrder = sortOrder
            previousViewMode = viewMode
            previousAlbumFilter = albumMediaFilter
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

    LaunchedEffect(lazyGridState, bucketName) {
        snapshotFlow { lazyGridState.isScrollInProgress }
            .distinctUntilChanged()
            .drop(1)
            .collect { inProgress ->
                if (!inProgress) {
                    viewModel.saveGridScrollPosition(
                        bucketName,
                        lazyGridState.firstVisibleItemIndex,
                        lazyGridState.firstVisibleItemScrollOffset
                    )
                }
            }
    }

    LaunchedEffect(bucketName) {
        val count = snapshotFlow { pagedMedia.itemCount }.first { it > 0 }
        val (savedIndex, savedOffset) = viewModel.getSavedGridScrollPosition(bucketName)
        if (savedIndex > 0 && count > savedIndex && lazyGridState.firstVisibleItemIndex == 0) {
            lazyGridState.scrollToItem(savedIndex, savedOffset)
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            val (savedIndex, savedOffset) = viewModel.getSavedGridScrollPosition(bucketName)
            if (savedIndex > 0 && pagedMedia.itemCount > savedIndex) {
                lazyGridState.scrollToItem(savedIndex, savedOffset)
            }
        }
    }

    LaunchedEffect(pagedMedia) {
        snapshotFlow {
            (pagedMedia.itemCount == 0) to (pagedMedia.loadState.refresh is androidx.paging.LoadState.NotLoading)
        }
            .distinctUntilChanged()
            .collect { (empty, notLoading) ->
                onLoadStateChanged(empty, notLoading)
            }
    }

    LaunchedEffect(lazyGridState) {
        var cachedDateSeconds = -1L
        var cachedDateString = ""
        var badgeVisible = false

        @OptIn(kotlinx.coroutines.FlowPreview::class)
        snapshotFlow { lazyGridState.firstVisibleItemIndex to lazyGridState.isScrollInProgress }
            .debounce(60L)
            .collectLatest { (index, inProgress) ->
                var emittedDate: String? = null
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
                                    cachedDateString = GalleryViewModel.formatGroupHeader(seconds)
                                }
                                cachedDateString
                            } else null
                        }
                        null -> null
                    }
                    if (!dateStr.isNullOrBlank()) {
                        emittedDate = dateStr
                    }
                }
                if (inProgress) {
                    badgeVisible = true
                    onDateBadgeChanged(true, emittedDate)
                } else if (badgeVisible) {
                    if (emittedDate != null) onDateBadgeChanged(true, emittedDate)
                    delay(1200)
                    badgeVisible = false
                    onDateBadgeChanged(false, null)
                }
            }
    }

    val onMosaicMediaClick = remember(viewModel, bucketName, onPhotoClick, lazyGridState) {
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
                val query = if (bucketName == "search_text" || bucketName == "search_smart") viewModel.searchQuery.value else null
                onPhotoClick(item.id, bucketName, query)
            }
        }
    }

    com.inferno.gallery.ui.components.PhotonGrid(
        pagedMedia = pagedMedia,
        lazyGridState = lazyGridState,
        gridCellsCount = gridCellsCount,
        onGridCountChange = onGridCountChange,
        isSelectionMode = isSelectionMode,
        selectedUris = selectedUris,
        onMediaClick = onMosaicMediaClick,
        viewMode = viewMode,
        thumbnailCornerRadius = thumbnailCornerRadius,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        viewModel = viewModel,
        modifier = modifier,
        contentPadding = contentPadding,
        timelineLayoutMode = TimelineLayoutMode.EDITORIAL_MOSAIC,
        dateHeaderAlignment = dateHeaderAlignment,
        showMatchBadge = (bucketName == "search_text" || bucketName == "search_smart"),
        headerContent = headerContent
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
                bottom = contentPadding.calculateBottomPadding()
            )
    )
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
        modifier = modifier
    )
}

@Composable
private fun DynamicDateBadge(
    visible: () -> Boolean,
    dateText: () -> String?,
    isCenter: Boolean,
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
            val rawDate = dateText() ?: ""
            val displayDate = when (rawDate) {
                "Today" -> stringResource(R.string.today)
                "Yesterday" -> stringResource(R.string.yesterday)
                else -> rawDate
            }
            Text(
                text = displayDate,
                style = com.inferno.gallery.ui.theme.TimelineDateHeaderStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = if (isCenter) 16.dp else 12.dp,
                    end = 16.dp,
                    top = 7.dp,
                    bottom = 7.dp
                )
            )
        }
    }
}


