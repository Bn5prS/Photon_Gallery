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
import kotlinx.coroutines.flow.first
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.ui.theme.ShapeExtraSmall
import com.inferno.gallery.ui.theme.ShapeMedium
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import com.inferno.gallery.ui.GalleryListItem
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
    onNavigateToSettings: () -> Unit = {}
) {
    val selectedFilterIndex by viewModel.selectedFilterIndex.collectAsState()
    val savedPosition = remember(bucketName) { viewModel.getSavedGridScrollPosition(bucketName) }
    val lazyGridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = savedPosition.first,
        initialFirstVisibleItemScrollOffset = savedPosition.second
    )

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

    // Persist scroll position when scrolling settles so returning from detail or navigation preserves exact place
    LaunchedEffect(lazyGridState, bucketName) {
        snapshotFlow { lazyGridState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { inProgress ->
                if (!inProgress) {
                    val index = lazyGridState.firstVisibleItemIndex
                    val offset = lazyGridState.firstVisibleItemScrollOffset
                    if (index > 0 || offset > 0) {
                        viewModel.saveGridScrollPosition(bucketName, index, offset)
                    }
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
    val selectedUris by viewModel.selectedUris.collectAsState()
    val gridCellsCount by viewModel.gridCellsCount.collectAsState()
    val thumbnailCornerRadius by viewModel.thumbnailCornerRadius.collectAsState()
    val cacheThumbnailsEnabled by viewModel.cacheThumbnailsEnabled.collectAsState()
    val timelineLayoutMode by viewModel.timelineLayoutMode.collectAsState()

    val totalItems = pagedMedia.itemCount
    val context = LocalContext.current

    BackHandler(enabled = isSelectionMode) {
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
                viewModel.setInitialDetailItem(item)
                val query = if (bucketName == "search_text") viewModel.searchQuery.value else null
                onPhotoClick(item.id, bucketName, query)
            }
        }
    }

    val onMediaLongClick = remember(viewModel) {
        { item: GalleryItem ->
            viewModel.toggleSelection(item.uri.toString())
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
                    val cacheKey = "t_${galleryItem.id}@$thumbSizePx"
                    if (imageLoader.memoryCache?.get(coil3.memory.MemoryCache.Key(cacheKey)) != null) continue
                    val req = coil3.request.ImageRequest.Builder(context)
                        .data(galleryItem.uri)
                        .size(thumbSizePx, thumbSizePx)
                        .precision(coil3.size.Precision.INEXACT)
                        .memoryCacheKey(cacheKey)
                        .build()
                    imageLoader.enqueue(req)
                }
            }
    }
    // ──────────────────────────────────────────────────────────────────────────────────────

    val isFilterChanging = pagedMedia.loadState.refresh is androidx.paging.LoadState.Loading
    val gridAlpha by animateFloatAsState(
        targetValue = if (isFilterChanging && totalItems > 0) 0.88f else 1f,
        animationSpec = MotionTokens.snappySpring(),
        label = "GridFilterAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = gridAlpha
                }
        ) {
            com.inferno.gallery.ui.components.PhotonGrid(
                pagedMedia = pagedMedia,
                lazyGridState = lazyGridState,
                gridCellsCount = gridCellsCount,
                onGridCountChange = viewModel::setGridCellsCount,
                isSelectionMode = isSelectionMode,
                selectedUris = selectedUris,
                onMediaClick = onMediaClick,
                onMediaLongClick = onMediaLongClick,
                viewMode = viewMode,
                thumbnailCornerRadius = thumbnailCornerRadius,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                viewModel = viewModel,
                modifier = modifier,
                contentPadding = contentPadding,
                timelineLayoutMode = timelineLayoutMode
            )
        }

        // ── Initial sync loading ────────────────────────────────────────────
        val isSyncRunning by viewModel.isInitialSyncRunning.collectAsState()
        val isNotLoading = pagedMedia.loadState.refresh is androidx.paging.LoadState.NotLoading
        AnimatedVisibility(
            visible = totalItems == 0 && isSyncRunning,
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
            visible = totalItems == 0 && isNotLoading && !isSyncRunning,
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

        FastScroller(
            lazyGridState = lazyGridState,
            totalItems = totalItems,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 56.dp
                )
        )
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
    totalItems: Int,
    modifier: Modifier = Modifier
) {
    if (totalItems < 50) return

    com.inferno.gallery.ui.components.ExpressiveScrollBar(
        gridState = lazyGridState,
        modifier = modifier
    )
}


