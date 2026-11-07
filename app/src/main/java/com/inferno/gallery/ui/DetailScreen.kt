package com.inferno.gallery.ui

import androidx.compose.ui.res.stringResource

import androidx.compose.material3.FilledTonalIconButton
import android.content.Context

import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import android.graphics.Bitmap
import coil3.BitmapImage
import coil3.DrawableImage
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Brush
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.components.thumbnailMemoryKey
import com.inferno.gallery.ui.components.thumbnailTargetPx
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledIconButton
import com.inferno.gallery.ui.components.ExpressiveFilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.asImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.ui.theme.ShapeExtraSmall
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.allowHardware
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.ui.components.DetailedExifData
import com.inferno.gallery.ui.components.ExifDetailsSheet
import com.inferno.gallery.ui.components.extractDetailedExif
import com.inferno.gallery.ui.components.AlbumPickerSheet
import com.inferno.gallery.ui.theme.ShapeLarge
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.wrapContentWidth
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeNone
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.data.MotionPhotoHelper
import com.inferno.gallery.data.MotionPhotoInfo
import com.inferno.gallery.ui.components.MotionPhotoPlayerOverlay
import com.inferno.gallery.ui.components.FloatingTipCard
import androidx.activity.compose.BackHandler
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.layout.BoxWithConstraints




@OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun DetailScreen(
    mediaId: String,
    bucketName: String?,
    highlightText: String? = null,
    useFullScreenGlobal: Boolean = false,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToEditor: (android.net.Uri) -> Unit = {},
    onNavigateToFrameExtractor: (android.net.Uri) -> Unit = {},
    onNavigateToPerson: ((Long) -> Unit)? = null
) {
    androidx.compose.runtime.LaunchedEffect(bucketName) {
        viewModel.setBucket(bucketName)
    }

    val context = LocalContext.current
    val settingsRepo = viewModel.settingsRepository
    // Matches the size bucket the grid used for this item, so the placeholder key resolves.
    val placeholderSizePx = remember(viewModel) {
        androidx.compose.runtime.derivedStateOf {
            thumbnailTargetPx(viewModel.gridCellsCount.value)
        }
    }.value
    val confirmDeleteEnabled by settingsRepo.confirmDeleteEnabledFlow.collectAsState(initial = true)
    val activity = context as? android.app.Activity
    val window = activity?.window
    val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
    val coroutineScope = rememberCoroutineScope()

    var hasNavigatedBack by remember { mutableStateOf(false) }
    val safeOnBack: () -> Unit = remember(onBack) {
        {
            if (!hasNavigatedBack) {
                hasNavigatedBack = true
                onBack()
            }
        }
    }
    
    val rawGalleryItems by viewModel.detailMedia.collectAsState()
    val initialDetailItem by viewModel.initialDetailItem.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val externalItem = remember(mediaId) {
        if (mediaId.startsWith("content://") || mediaId.startsWith("file://") || mediaId.startsWith("http://") || mediaId.startsWith("https://")) {
            val parsedUri = android.net.Uri.parse(mediaId)
            val isVid = try {
                val mime = context.contentResolver.getType(parsedUri)
                mime?.startsWith("video/") == true ||
                    parsedUri.lastPathSegment?.lowercase()?.let { 
                        it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".mov") || it.endsWith(".webm") || it.endsWith(".avi") || it.endsWith(".3gp")
                    } == true
            } catch (_: Exception) {
                false
            }
            val fileName = try {
                var name: String? = null
                if (parsedUri.scheme == "content") {
                    context.contentResolver.query(parsedUri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIdx >= 0) name = cursor.getString(nameIdx)
                        }
                    }
                }
                name ?: parsedUri.lastPathSegment ?: "Media"
            } catch (_: Exception) {
                parsedUri.lastPathSegment ?: "Media"
            }
            GalleryItem(
                id = mediaId,
                uri = parsedUri,
                bucketName = "External",
                dateAdded = System.currentTimeMillis() / 1000L,
                size = 0L,
                name = fileName,
                dateModified = System.currentTimeMillis() / 1000L,
                path = if (parsedUri.scheme == "file") (parsedUri.path ?: "") else "",
                isVideo = isVid,
                localExists = true
            )
        } else {
            null
        }
    }

    val galleryItems = remember(rawGalleryItems, initialDetailItem, externalItem) {
        if (externalItem != null) {
            val existingIndex = rawGalleryItems.indexOfFirst {
                it.id == mediaId || it.uri.toString() == mediaId || it.path == mediaId || it.uri == externalItem.uri
            }
            if (existingIndex >= 0) {
                rawGalleryItems
            } else {
                listOf(externalItem)
            }
        } else if (rawGalleryItems.isNotEmpty()) {
            rawGalleryItems
        } else if (initialDetailItem != null) {
            listOf(initialDetailItem!!)
        } else {
            emptyList()
        }
    }

    val maxBrightnessEnabled by settingsRepo.maxBrightnessEnabledFlow.collectAsState(initial = false)
    val viewerBlurEffect by settingsRepo.viewerBlurEffectFlow.collectAsState(initial = false)
    val viewerZoomMinimapEnabled by settingsRepo.viewerZoomMinimapEnabledFlow.collectAsState(initial = true)
    val viewerFilmstripEnabled = false // Feature backed up: re-bind to settingsRepo.viewerFilmstripEnabledFlow when requested

    // Overrides screen brightness to maximum in fullscreen and restores it when leaving
    androidx.compose.runtime.DisposableEffect(maxBrightnessEnabled) {
        if (maxBrightnessEnabled) {
            val layoutParams = window?.attributes
            if (layoutParams != null) {
                val originalBrightness = layoutParams.screenBrightness
                layoutParams.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                window.attributes = layoutParams

                onDispose {
                    val currentParams = window.attributes
                    if (currentParams != null) {
                        currentParams.screenBrightness = originalBrightness
                        window.attributes = currentParams
                    }
                }
            } else {
                onDispose {}
            }
        } else {
            onDispose {}
        }
    }

    // Auto-navigate back safely when list is empty
    LaunchedEffect(galleryItems.isEmpty()) {
        if (galleryItems.isEmpty()) {
            safeOnBack()
        }
    }

    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val baseCanvasColor = MaterialTheme.colorScheme.surface

    // Ensure we don't crash if items is empty
    if (galleryItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(baseCanvasColor))
        return
    }

    val initialPage = remember(mediaId, galleryItems) {
        val index = galleryItems.indexOfFirst { 
            it.id == mediaId || it.uri.toString() == mediaId || it.path == mediaId || (externalItem != null && it.uri == externalItem.uri)
        }
        if (index >= 0) index else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { galleryItems.size }
    )

    LaunchedEffect(galleryItems) {
        if (externalItem != null && galleryItems.size > 1) {
            val targetIndex = galleryItems.indexOfFirst {
                it.id == mediaId || it.uri.toString() == mediaId || it.path == mediaId || it.uri == externalItem.uri
            }
            if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                pagerState.scrollToPage(targetIndex)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val imageLoader = context.imageLoader
        for (offset in -2..2) {
            if (offset == 0) continue
            val prefetchItem = galleryItems.getOrNull(pagerState.currentPage + offset) ?: continue
            val prefetchUri = prefetchItem.uri
            val req = coil3.request.ImageRequest.Builder(context)
                .data(prefetchUri)
                .size(coil3.size.Size.ORIGINAL)
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .build()
            imageLoader.enqueue(req)
        }
    }

    val currentItem = galleryItems.getOrNull(pagerState.currentPage)
    LaunchedEffect(currentItem?.id) {
        val item = currentItem
        if (item != null) {
            viewModel.syncGridScrollToMedia(bucketName, item.id)
        }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, currentItem) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val item = currentItem
                if (item != null && item.path.isNotBlank()) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val file = java.io.File(item.path)
                        val showHidden = settingsRepo.showHiddenAlbumsFlow.first()
                        val isHidden = !showHidden && com.inferno.gallery.data.NoMediaUtils.isPathHiddenByNoMedia(item.path)
                        if (!file.exists() || isHidden) {
                            try {
                                android.media.MediaScannerConnection.scanFile(
                                    context.applicationContext,
                                    arrayOf(item.path),
                                    null,
                                    null
                                )
                            } catch (_: Exception) {}
                            withContext(Dispatchers.Main) {
                                viewModel.notifyExternalMediaModifiedOrDeleted(item.uri.toString())
                                if (galleryItems.size <= 1) {
                                    safeOnBack()
                                }
                            }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentItem) {
        if (currentItem != null && currentItem.path.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val file = java.io.File(currentItem.path)
                val showHidden = settingsRepo.showHiddenAlbumsFlow.first()
                val isHidden = !showHidden && com.inferno.gallery.data.NoMediaUtils.isPathHiddenByNoMedia(currentItem.path)
                if (!file.exists() || isHidden) {
                    try {
                        android.media.MediaScannerConnection.scanFile(
                            context.applicationContext,
                            arrayOf(currentItem.path),
                            null,
                            null
                        )
                    } catch (_: Exception) {}
                    withContext(Dispatchers.Main) {
                        viewModel.notifyExternalMediaModifiedOrDeleted(currentItem.uri.toString())
                        if (galleryItems.size <= 1) {
                            safeOnBack()
                        }
                    }
                }
            }
        }
    }


    val resolvedCurrentUri by produceState<Uri?>(initialValue = currentItem?.uri, key1 = currentItem) {
        val uri = currentItem?.uri
        value = uri
    }



    var activeHighlight by remember { mutableStateOf(highlightText) }
    var highlightRects by remember { mutableStateOf<List<android.graphics.Rect>>(emptyList()) }
    var highlightImageSize by remember { mutableStateOf<androidx.compose.ui.geometry.Size?>(null) }
    var highlightOverlayVisible by remember { mutableStateOf(false) }


    var hasInitialScrolled by remember(mediaId) { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(galleryItems, mediaId) {
        if (!hasInitialScrolled && galleryItems.isNotEmpty()) {
            val targetIndex = galleryItems.indexOfFirst { 
                it.id == mediaId || it.uri.toString() == mediaId || it.path == mediaId 
            }
            if (targetIndex >= 0) {
                if (rawGalleryItems.isNotEmpty() || galleryItems.size > 1) {
                    if (pagerState.currentPage != targetIndex) {
                        pagerState.scrollToPage(targetIndex)
                    }
                    hasInitialScrolled = true
                }
            }
        }
    }

    val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
    val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()

    val isFromSearch = bucketName == com.inferno.gallery.data.BucketNames.SEARCH_TEXT ||
                       bucketName == com.inferno.gallery.data.BucketNames.SEARCH_SMART ||
                       bucketName == "search_text" ||
                       bucketName == "search_smart" ||
                       !highlightText.isNullOrBlank()

    var showUi by remember { mutableStateOf(!isFromSearch) }
    var isFilmstripOverview by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showInfoCard by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCopySheet by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var isMotionPlaying by remember { mutableStateOf(false) }
    var isMotionLooping by remember { mutableStateOf(false) }
    var isVideoScrubbing by remember { mutableStateOf(false) }
    var isFilmstripScrolling by remember { mutableStateOf(false) }
    var userInteractionTrigger by remember { mutableLongStateOf(0L) }

    val isAnyToolOrMenuOpen = showMoreMenu ||
                              showInfoCard ||
                              showRenameDialog ||
                              showDeleteConfirmDialog ||
                              showCopySheet ||
                              showMoveSheet ||
                              isFilmstripOverview ||
                              isMotionPlaying ||
                              isMotionLooping ||
                              isVideoScrubbing ||
                              isFilmstripScrolling

    val overviewProgress by animateFloatAsState(
        targetValue = if (isFilmstripOverview) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "overviewProgress"
    )

    // Exit overview mode on hardware/gesture back with predictive back
    PredictiveBackHandler(enabled = isFilmstripOverview) { progressFlow ->
        try {
            progressFlow.collect { }
            isFilmstripOverview = false
            showUi = true
        } catch (e: CancellationException) {
            // Keep overview open if cancelled
        }
    }

    // Auto hide UI after 4 seconds of inactivity — never hide if user is using any tools or opened 3-dot menu
    LaunchedEffect(
        showUi,
        pagerState.isScrollInProgress,
        pagerState.currentPage,
        isAnyToolOrMenuOpen,
        userInteractionTrigger
    ) {
        if (showUi && !isAnyToolOrMenuOpen) {
            kotlinx.coroutines.delay(4000)
            showUi = false
        }
    }

    LaunchedEffect(isAnyToolOrMenuOpen) {
        if (isAnyToolOrMenuOpen) {
            showUi = true
        }
    }

    var currentScale by remember { mutableStateOf(1f) }
    var pendingDeleteItem by remember { mutableStateOf<GalleryItem?>(null) }
    var pendingDeletePage by remember { mutableStateOf<Int?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val itemToDelete = pendingDeleteItem
            val deletedUri = itemToDelete?.uri?.toString()
            if (deletedUri != null) {
                if (bucketName == "Trash" || itemToDelete?.bucketName == "Trash") {
                    viewModel.deleteMediaPermanentlyOptimistically(deletedUri)
                } else {
                    viewModel.removeMediaOptimistically(deletedUri)
                }
            }
        }
        pendingDeletePage = null
        pendingDeleteItem = null
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val itemToRestore = pendingDeleteItem
            val restoredUri = itemToRestore?.uri?.toString()
            if (restoredUri != null) {
                viewModel.restoreMediaOptimistically(restoredUri)
            }
        }
        pendingDeletePage = null
        pendingDeleteItem = null
    }

    // Auto-navigate after delete: if list becomes empty, go back; clamp if at the end
    LaunchedEffect(galleryItems.size) {
        if (galleryItems.isEmpty()) {
            safeOnBack()
        } else if (pagerState.currentPage >= galleryItems.size) {
            pagerState.scrollToPage(galleryItems.size - 1)
        }
    }

    var pendingRenameItem by remember { mutableStateOf<GalleryItem?>(null) }
    var pendingRenameNewName by remember { mutableStateOf("") }

    val renameLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val item = pendingRenameItem
            val name = pendingRenameNewName
            if (item != null && name.isNotBlank()) {
                viewModel.renameMedia(context, item, name) {
                    // Fallback
                }
            }
        }
    }

    var pendingRotateItem by remember { mutableStateOf<GalleryItem?>(null) }
    val rotateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val item = pendingRotateItem
            if (item != null) {
                viewModel.rotateMedia(context, item, 90, onSecurityException = {}) {
                    // Success callback
                }
            }
        }
        pendingRotateItem = null
    }

    val animRotationMap = remember { androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>>() }
    val targetRotationMap = remember { androidx.compose.runtime.mutableStateMapOf<String, Float>() }

    androidx.compose.runtime.DisposableEffect(useFullScreenGlobal, isDarkTheme) {
        val prevLightStatusBars = insetsController?.isAppearanceLightStatusBars ?: false
        val prevLightNavBars = insetsController?.isAppearanceLightNavigationBars ?: false
        if (window != null && insetsController != null) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            
            if (useFullScreenGlobal) {
                // Global full screen mode is enabled -> hide system bars
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                // Global full screen mode is disabled -> show system bars
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        
        onDispose {
            // Restore to the global state when leaving DetailScreen
            if (window != null && insetsController != null) {
                insetsController.isAppearanceLightStatusBars = prevLightStatusBars
                insetsController.isAppearanceLightNavigationBars = prevLightNavBars
                if (!useFullScreenGlobal) {
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
    }

    // Single-tap immersive mode: system bars follow the viewer chrome toggle.
    // Skipped when the global full-screen setting already keeps system bars hidden.
    LaunchedEffect(showUi, useFullScreenGlobal, isDarkTheme) {
        if (insetsController != null && !useFullScreenGlobal) {
            if (showUi) {
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    if (showDeleteConfirmDialog && pendingDeleteItem != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { androidx.compose.material3.Text(stringResource(R.string.dialog_move_to_trash_title)) },
            text = { androidx.compose.material3.Text(stringResource(R.string.dialog_move_to_trash_msg)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        val item = pendingDeleteItem ?: return@TextButton
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val trashIntent = MediaStore.createTrashRequest(
                                    context.contentResolver, 
                                    listOf(item.uri), 
                                    true
                                )
                                val request = IntentSenderRequest.Builder(trashIntent.intentSender).build()
                                withContext(Dispatchers.Main) {
                                    launcher.launch(request)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(
                                        context, 
                                        "Unable to trash this item: ${e.message}", 
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                ) {
                    androidx.compose.material3.Text(stringResource(R.string.dialog_move_to_trash_confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    androidx.compose.material3.Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showRenameDialog && currentItem != null) {
        var renameText by remember(currentItem) { mutableStateOf(currentItem.name) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { androidx.compose.material3.Text(stringResource(R.string.dialog_rename_title)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { androidx.compose.material3.Text(stringResource(R.string.dialog_rename_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val trimmed = renameText.trim()
                        if (trimmed.isNotEmpty() && trimmed != currentItem.name) {
                            val oldExt = currentItem.name.substringAfterLast('.', "")
                            val finalNewName = if (oldExt.isNotEmpty() && !trimmed.endsWith(".$oldExt", ignoreCase = true)) {
                                if (trimmed.contains('.')) trimmed else "$trimmed.$oldExt"
                            } else {
                                trimmed
                            }
                            viewModel.renameMedia(context, currentItem, finalNewName) { pendingIntent ->
                                pendingRenameItem = currentItem
                                pendingRenameNewName = finalNewName
                                renameLauncher.launch(
                                    androidx.activity.result.IntentSenderRequest.Builder(pendingIntent).build()
                                )
                            }
                        }
                        showRenameDialog = false
                    }
                ) {
                    androidx.compose.material3.Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showRenameDialog = false }
                ) {
                    androidx.compose.material3.Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showCopySheet && currentItem != null) {
        val allAlbumsList by viewModel.allAlbums.collectAsState()
        AlbumPickerSheet(
            title = stringResource(R.string.sheet_copy_to_album),
            itemCount = 1,
            albums = allAlbumsList,
            showPrivateSpaceOption = false,
            confirmPathButtonText = stringResource(R.string.sheet_copy_here),
            onDismissRequest = { showCopySheet = false },
            onSelectAlbum = { bucket ->
                showCopySheet = false
                viewModel.copyMedia(listOf(currentItem.uri), bucket)
            },
            onSelectPath = { path ->
                showCopySheet = false
                viewModel.copyMediaToPath(listOf(currentItem.uri), path)
            }
        )
    }

    if (showMoveSheet && currentItem != null) {
        val allAlbumsList by viewModel.allAlbums.collectAsState()
        AlbumPickerSheet(
            title = stringResource(R.string.sheet_move_to_album),
            itemCount = 1,
            albums = allAlbumsList,
            showPrivateSpaceOption = true,
            confirmPathButtonText = stringResource(R.string.sheet_move_here),
            onDismissRequest = { showMoveSheet = false },
            onSelectAlbum = { bucket ->
                showMoveSheet = false
                viewModel.moveMedia(listOf(currentItem.uri), bucket)
            },
            onSelectPath = { path ->
                showMoveSheet = false
                viewModel.moveMediaToPath(listOf(currentItem.uri), path)
            },
            onSelectPrivateSpace = {
                showMoveSheet = false
                val activity = context as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    viewModel.vaultAuthManager.authenticate(
                        activity = activity,
                        negativeButtonText = "Cancel",
                        unlockSession = false,
                        onSuccess = {
                            viewModel.hideMedia(listOf(currentItem.uri))
                            viewModel.vaultAuthManager.lock()
                            safeOnBack()
                        },
                        onFailure = {}
                    )
                }
            }
        )
    }

    var currentExif by remember { mutableStateOf<DetailedExifData?>(null) }
    
    var motionPhotoInfo by remember { mutableStateOf<MotionPhotoInfo?>(null) }
    var extractedMotionVideo by remember { mutableStateOf<java.io.File?>(null) }

    androidx.compose.runtime.LaunchedEffect(currentItem) {
        isMotionPlaying = false
        isMotionLooping = false
        extractedMotionVideo = null
        motionPhotoInfo = null
        if (currentItem != null && !currentItem.isVideo) {
            val info = MotionPhotoHelper.getMotionPhotoInfo(context, currentItem)
            motionPhotoInfo = info
            if (info.isMotionPhoto) {
                extractedMotionVideo = MotionPhotoHelper.getOrExtractMotionVideo(context, currentItem, info)
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(currentItem, showInfoCard) {
        if (showInfoCard && currentItem != null) {
            currentExif = extractDetailedExif(context, currentItem)
        }
    }

    // ── Swipe-to-dismiss state ─────────────────────────────────────────────
    // Vertical drag offset (px). When the user drags the image downward at
    // 1× zoom, this translates the pager and fades the black scrim.
    val dismissOffsetY = remember { Animatable(0f) }
    val dismissProgress = (kotlin.math.abs(dismissOffsetY.value) / 600f).coerceIn(0f, 1f)
    val bgAlpha = 1f - dismissProgress          // scrim fades out
    val dismissScale = 1f - dismissProgress * 0.15f // subtle shrink

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseCanvasColor.copy(alpha = bgAlpha)),
        contentAlignment = Alignment.Center
    ) {

        if (viewerBlurEffect && currentItem != null) {
            val blurRequest = remember(currentItem.uri) {
                ImageRequest.Builder(context)
                    .data(currentItem.uri)
                    .size(300, 300)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = blurRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(36.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .alpha(0.40f * bgAlpha)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseCanvasColor.copy(alpha = 0.82f * bgAlpha))
                )
            }
        }



        HorizontalPager(
            state = pagerState,
            userScrollEnabled = (currentScale <= 1.05f && dismissOffsetY.value == 0f) || isFilmstripOverview,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dismissOffsetY.value
                    scaleX = dismissScale
                    scaleY = dismissScale
                }
                .pointerInput(currentScale) {
                    // Swipe-to-dismiss: only when not zoomed in.
                    if (currentScale > 1.05f) return@pointerInput
                    val dismissThreshold = 200f
                    val velocityThreshold = 800f
                    val velocityTracker = VelocityTracker()
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            velocityTracker.resetTracking()
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().y
                            coroutineScope.launch {
                                val current = dismissOffsetY.value
                                if (kotlin.math.abs(current) > dismissThreshold ||
                                    kotlin.math.abs(velocity) > velocityThreshold
                                ) {
                                    // Commit dismiss — trigger existing back transition
                                    safeOnBack()
                                } else {
                                    // Snap back with spring
                                    dismissOffsetY.animateTo(
                                        0f,
                                        animationSpec = MotionTokens.snappySpring()
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                dismissOffsetY.animateTo(
                                    0f,
                                    animationSpec = MotionTokens.snappySpring()
                                )
                            }
                        }
                    ) { change, dragAmount ->
                        velocityTracker.addPointerInputChange(change)
                        coroutineScope.launch {
                            dismissOffsetY.snapTo(dismissOffsetY.value + dragAmount)
                        }
                    }
                },
            key = { page -> galleryItems.getOrNull(page)?.id ?: page.toString() },
            contentPadding = PaddingValues(horizontal = (72.dp * overviewProgress.coerceIn(0f, 1f))),
            pageSpacing = (12.dp * overviewProgress.coerceIn(0f, 1f)),
        ) { page ->
            val item = galleryItems.getOrNull(page) ?: return@HorizontalPager
            val resolvedUri by produceState(initialValue = item.uri, key1 = item) {
                value = item.uri
            }

            val request: coil3.request.ImageRequest = remember(resolvedUri, item.id, placeholderSizePx) {
                ImageRequest.Builder(context)
                    .data(resolvedUri)
                    .size(coil3.size.Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    // Reuse the exact thumbnail the grid already has cached for this column
                    // count, so the tap-to-detail hand-off shows the image immediately.
                    .placeholderMemoryCacheKey(thumbnailMemoryKey(item.id, placeholderSizePx))
                    .crossfade(false)
                    .build()
            }
            

            
            var imageAspectRatio by remember(item) { mutableStateOf(1f) }
            val scale = remember(item.id) { androidx.compose.runtime.mutableFloatStateOf(1f) }
            val offsetX = remember(item.id) { androidx.compose.runtime.mutableFloatStateOf(0f) }
            val offsetY = remember(item.id) { androidx.compose.runtime.mutableFloatStateOf(0f) }
            val animJob = remember(item.id) { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }
            val currentTargetRot = targetRotationMap[item.id] ?: 0f
            val animRotation = remember(item.id) { androidx.compose.animation.core.Animatable(currentTargetRot) }
            DisposableEffect(item.id) {
                animRotationMap[item.id] = animRotation
                onDispose { animRotationMap.remove(item.id) }
            }

            androidx.compose.runtime.LaunchedEffect(pagerState.currentPage, item.id) {
                if (pagerState.currentPage != page) {
                    scale.floatValue = 1f
                    offsetX.floatValue = 0f
                    offsetY.floatValue = 0f
                    isMotionPlaying = false
                } else {
                    currentScale = scale.floatValue
                    viewModel.recordMediaView(item.id)
                }
            }
            
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val absoluteOffset = kotlin.math.abs(pageOffset)
            // Normal (non-overview) subtle transform
            val normalPagerScale = 1f - (absoluteOffset.coerceIn(0f, 1f) * 0.05f)
            val normalPagerAlpha = 1f - (absoluteOffset.coerceIn(0f, 1f) * 0.5f)
            // Overview-mode transform: center card 1.0 scale; neighbors 0.88; lerp by overviewProgress
            val overviewCardScale = if (absoluteOffset < 0.5f)
                androidx.compose.ui.util.lerp(normalPagerScale, 0.95f, overviewProgress)
            else
                androidx.compose.ui.util.lerp(normalPagerScale, 0.88f, overviewProgress)
            val overviewCardAlpha = if (absoluteOffset < 0.5f)
                1f
            else
                androidx.compose.ui.util.lerp(normalPagerAlpha, 0.70f, overviewProgress)
            val pagerScale = overviewCardScale
            val pagerAlpha = overviewCardAlpha
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pagerScale
                        scaleY = pagerScale
                        alpha = pagerAlpha
                    }
            ) {
            androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

                val parentWidth = constraints.maxWidth.toFloat()
                val parentHeight = constraints.maxHeight.toFloat()
                if (item.isVideo) {
                    with(sharedTransitionScope) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "photo_${item.uri}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    enter = fadeIn(MotionTokens.fastEffectsSpec()),
                                    exit = fadeOut(MotionTokens.fastEffectsSpec()),
                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                                    clipInOverlayDuringTransition = OverlayClip(ShapeLarge),
                                    boundsTransform = { _, _ -> MotionTokens.sharedElementSpring() }
                                )
                        ) {
                            VideoPlayerItem(
                                uri = resolvedUri,
                                isCurrentPage = page == pagerState.currentPage,
                                showControls = showUi && !isFilmstripOverview,
                                modifier = Modifier.fillMaxSize(),
                                onScrubbingChanged = { isVideoScrubbing = it },
                                onTap = {
                                    if (isFilmstripOverview) {
                                        coroutineScope.launch {
                                            if (page != pagerState.currentPage) {
                                                pagerState.animateScrollToPage(page)
                                            }
                                            isFilmstripOverview = false
                                            showUi = true
                                        }
                                    } else {
                                        if (showInfoCard || showUi) {
                                            showInfoCard = false
                                            showUi = false
                                        } else {
                                            showUi = true
                                        }
                                    }
                                },
                                onPinchOverview = {
                                    isFilmstripOverview = true
                                    showUi = false
                                },
                                onPinchExpand = {
                                    isFilmstripOverview = false
                                    showUi = true
                                }
                            )
                        }
                    }
                } else {
                    androidx.compose.runtime.LaunchedEffect(activeHighlight, page, pagerState.currentPage, resolvedUri) {
                        if (page == pagerState.currentPage && activeHighlight != null) {
                            var foundRects = emptyList<android.graphics.Rect>()
                            var imgSize: androidx.compose.ui.geometry.Size? = null
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
                                    val inputImage = com.google.mlkit.vision.common.InputImage.fromFilePath(context, resolvedUri)
                                    val visionText: com.google.mlkit.vision.text.Text? = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                                        recognizer.process(inputImage)
                                            .addOnSuccessListener { cont.resumeWith(kotlin.Result.success(it)) }
                                            .addOnFailureListener { cont.resumeWith(kotlin.Result.success(null)) }
                                    }
                                    if (visionText != null) {
                                        val rects = mutableListOf<android.graphics.Rect>()
                                        val cleanHighlight = activeHighlight!!.trim()
                                        val queryTokens = cleanHighlight.split(Regex("\\s+")).filter { it.isNotBlank() }
                                        for (block in visionText.textBlocks) {
                                            for (line in block.lines) {
                                                if (cleanHighlight.contains(" ") && line.text.contains(cleanHighlight, ignoreCase = true)) {
                                                    line.boundingBox?.let { rects.add(it) }
                                                } else {
                                                    for (element in line.elements) {
                                                        if (queryTokens.any { token -> element.text.contains(token, ignoreCase = true) }) {
                                                            element.boundingBox?.let { rects.add(it) }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        foundRects = rects
                                        imgSize = androidx.compose.ui.geometry.Size(inputImage.width.toFloat(), inputImage.height.toFloat())
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("DetailScreen", "OCR highlight failed", e)
                                }
                            }
                            highlightRects = foundRects
                            highlightImageSize = imgSize
                            if (foundRects.isNotEmpty()) {
                                highlightOverlayVisible = true
                                kotlinx.coroutines.delay(4000)
                                highlightOverlayVisible = false
                            } else {
                                highlightOverlayVisible = false
                            }
                        } else {
                            highlightRects = emptyList()
                            highlightImageSize = null
                            highlightOverlayVisible = false
                        }
                    }


                    with(sharedTransitionScope) {
                        AsyncImage(
                            model = request,
                            contentDescription = "Full-screen photo",
                            contentScale = ContentScale.Fit,
                            filterQuality = if (scale.floatValue >= 30f) FilterQuality.None else FilterQuality.Low,
                            onSuccess = { state ->
                                val size = state.painter.intrinsicSize
                                if (size.width > 0f && size.height > 0f && size.width.isFinite() && size.height.isFinite()) {
                                    imageAspectRatio = size.width / size.height
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "photo_${item.uri}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    enter = fadeIn(MotionTokens.fastEffectsSpec()),
                                    exit = fadeOut(MotionTokens.fastEffectsSpec()),
                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                                    clipInOverlayDuringTransition = OverlayClip(ShapeLarge),
                                    boundsTransform = { _, _ -> MotionTokens.sharedElementSpring() }
                                )
                                // IMPORTANT: pointerInput MUST be before graphicsLayer.
                                // When pointerInput is placed after graphicsLayer, Compose
                                // inverse-transforms all pointer coordinates through the layer's
                                // scale matrix (divides by scaleX/Y). At 5x zoom that makes
                                // calculatePan() return 1/5 of the actual finger delta, causing
                                // pan to be 5x slower than the finger. With pointerInput first
                                // (outer), events arrive in screen/layout space — 1:1 with finger.
                                .pointerInput(page, motionPhotoInfo, extractedMotionVideo, isMotionLooping) {
                                    detectTapGestures(
                                        onPress = {
                                            val isMotion = page == pagerState.currentPage && motionPhotoInfo?.isMotionPhoto == true && (extractedMotionVideo != null || motionPhotoInfo?.pairedVideoUri != null)
                                            if (isMotion) {
                                                var isHeld = false
                                                try {
                                                    val released = kotlinx.coroutines.withTimeoutOrNull(260) {
                                                        tryAwaitRelease()
                                                    }
                                                    if (released == null) {
                                                        isHeld = true
                                                        isMotionPlaying = true
                                                        tryAwaitRelease()
                                                    }
                                                } finally {
                                                    if (isHeld && !isMotionLooping) {
                                                        isMotionPlaying = false
                                                    }
                                                }
                                            }
                                        },
                                        onTap = {
                                            if (isFilmstripOverview) {
                                                // Tap in overview: navigate to this page if not current, then exit overview
                                                coroutineScope.launch {
                                                    if (page != pagerState.currentPage) {
                                                        pagerState.animateScrollToPage(page)
                                                    }
                                                    isFilmstripOverview = false
                                                    showUi = true
                                                }
                                            } else {
                                                activeHighlight = null
                                                highlightOverlayVisible = false
                                                if (showInfoCard || showUi) {
                                                    showInfoCard = false
                                                    showUi = false
                                                } else {
                                                    showUi = true
                                                }
                                            }
                                        },
                                        onDoubleTap = { centroid ->
                                            animJob.value?.cancel()
                                            animJob.value = coroutineScope.launch {
                                                val composableW = size.width.toFloat()
                                                val composableH = size.height.toFloat()
                                                if (scale.floatValue > 1f) {
                                                    // Zoom out to fit
                                                    launch {
                                                        androidx.compose.animation.core.animate(
                                                            initialValue = scale.floatValue,
                                                            targetValue = 1f,
                                                            animationSpec = MotionTokens.gentleSpring()
                                                        ) { v, _ ->
                                                            scale.floatValue = v
                                                            currentScale = v
                                                        }
                                                    }
                                                    launch { androidx.compose.animation.core.animate(offsetX.floatValue, 0f, animationSpec = MotionTokens.gentleSpring()) { v, _ -> offsetX.floatValue = v } }
                                                    launch { androidx.compose.animation.core.animate(offsetY.floatValue, 0f, animationSpec = MotionTokens.gentleSpring()) { v, _ -> offsetY.floatValue = v } }
                                                } else {
                                                    // Zoom in to tapped point
                                                    val targetScale = 3.0f
                                                    val imageW = if (imageAspectRatio > 0f) minOf(composableW, composableH * imageAspectRatio) else composableW
                                                    val imageH = if (imageAspectRatio > 0f && imageAspectRatio.isFinite()) imageW / imageAspectRatio else composableH
                                                    val maxX = maxOf(0f, (imageW * targetScale - composableW) / 2f)
                                                    val maxY = maxOf(0f, (imageH * targetScale - composableH) / 2f)

                                                    val targetX = -(centroid.x - composableW / 2f) * (targetScale - 1)
                                                    val targetY = -(centroid.y - composableH / 2f) * (targetScale - 1)
                                                    launch {
                                                        androidx.compose.animation.core.animate(
                                                            initialValue = scale.floatValue,
                                                            targetValue = targetScale,
                                                            animationSpec = MotionTokens.gentleSpring()
                                                        ) { v, _ ->
                                                            scale.floatValue = v
                                                            currentScale = v
                                                        }
                                                    }
                                                    launch { androidx.compose.animation.core.animate(offsetX.floatValue, targetX.coerceIn(-maxX, maxX), animationSpec = MotionTokens.gentleSpring()) { v, _ -> offsetX.floatValue = v } }
                                                    launch { androidx.compose.animation.core.animate(offsetY.floatValue, targetY.coerceIn(-maxY, maxY), animationSpec = MotionTokens.gentleSpring()) { v, _ -> offsetY.floatValue = v } }
                                                }
                                            }
                                        }
                                    )
                                }
                                .pointerInput(item.id) {
                                    // Unified gesture handler: pinch-zoom, pan
                                    awaitEachGesture {
                                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                                        var accumulatedZoom = 1f
                                        var accumulatedPan = Offset.Zero
                                        var pastTouchSlop = false
                                        val touchSlop = viewConfiguration.touchSlop
                                        val composableW = size.width.toFloat()
                                        val composableH = size.height.toFloat()
                                        val velocityTracker = VelocityTracker()
                                        var maxPointerCount = 0

                                        do {
                                            val event = awaitPointerEvent()
                                            val pressedCount = event.changes.count { it.pressed }
                                            maxPointerCount = maxOf(maxPointerCount, pressedCount)

                                            if (pressedCount == 1) {
                                                event.changes.firstOrNull { it.pressed }?.let { velocityTracker.addPointerInputChange(it) }
                                            }

                                            val isZoomed = scale.floatValue > 1.02f
                                            val isMultiTouch = pressedCount >= 2

                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()

                                            if (!pastTouchSlop) {
                                                accumulatedZoom *= zoomChange
                                                accumulatedPan += panChange
                                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                                val zoomMotion = kotlin.math.abs(1 - accumulatedZoom) * centroidSize
                                                val panMotion = accumulatedPan.getDistance()
                                                if (zoomMotion > touchSlop || (isZoomed && panMotion > touchSlop) || isMultiTouch) {
                                                    pastTouchSlop = true
                                                }
                                            }

                                            if (pastTouchSlop && (zoomChange != 1f || panChange != Offset.Zero || isMultiTouch)) {
                                                val centroid = event.calculateCentroid(useCurrent = false)
                                                animJob.value?.cancel()
                                                val oldScale = scale.floatValue
                                                // Keep tracking accumulated zoom after slop
                                                if (isMultiTouch) accumulatedZoom *= zoomChange
                                                val zoomFactor = if (oldScale > 30f) {
                                                    1f + (zoomChange - 1f) * (30f / oldScale).coerceIn(0.25f, 1f)
                                                } else {
                                                    zoomChange
                                                }
                                                val newScale = (oldScale * zoomFactor).coerceIn(1f, 200f)
                                                // Pinch-in past threshold while at normal scale → enter filmstrip overview
                                                if (isMultiTouch && !isFilmstripOverview &&
                                                    oldScale <= 1.05f && accumulatedZoom < 0.82f) {
                                                    isFilmstripOverview = true
                                                    showUi = false
                                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                }
                                                // Pinch-out while in overview → exit overview
                                                if (isMultiTouch && isFilmstripOverview &&
                                                    accumulatedZoom > 1.25f) {
                                                    isFilmstripOverview = false
                                                    showUi = true
                                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                }

                                                if (isFilmstripOverview) {
                                                    // eat gesture events in overview — pager handles horizontal scroll
                                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                } else if (newScale > 1.02f) {
                                                    showUi = false
                                                }

                                                val imageW = if (imageAspectRatio > 0f) minOf(composableW, composableH * imageAspectRatio) else composableW
                                                val imageH = if (imageAspectRatio > 0f && imageAspectRatio.isFinite()) imageW / imageAspectRatio else composableH
                                                val maxX = maxOf(0f, (imageW * newScale - composableW) / 2f)
                                                val maxY = maxOf(0f, (imageH * newScale - composableH) / 2f)

                                                val focalX = centroid.x - composableW / 2f
                                                val focalY = centroid.y - composableH / 2f
                                                val effectiveZoom = if (oldScale > 0f) newScale / oldScale else 1f
                                                var newOffsetX = (offsetX.floatValue - focalX) * effectiveZoom + focalX + panChange.x
                                                var newOffsetY = (offsetY.floatValue - focalY) * effectiveZoom + focalY + panChange.y

                                                newOffsetX = newOffsetX.coerceIn(-maxX, maxX)
                                                newOffsetY = newOffsetY.coerceIn(-maxY, maxY)

                                                scale.floatValue = newScale
                                                offsetX.floatValue = newOffsetX
                                                offsetY.floatValue = newOffsetY
                                                currentScale = newScale

                                                val isAtLeftBoundary = newOffsetX >= maxX && panChange.x > 0
                                                val isAtRightBoundary = newOffsetX <= -maxX && panChange.x < 0
                                                val allowPagerSwipe = !isMultiTouch && (isAtLeftBoundary || isAtRightBoundary)

                                                if (!allowPagerSwipe) {
                                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                }
                                            }
                                        } while (event.changes.any { it.pressed })

                                        if (pastTouchSlop && scale.floatValue > 1.05f && maxPointerCount == 1) {
                                            val velocity = velocityTracker.calculateVelocity()
                                            val imageW = if (imageAspectRatio > 0f) minOf(composableW, composableH * imageAspectRatio) else composableW
                                            val imageH = if (imageAspectRatio > 0f && imageAspectRatio.isFinite()) imageW / imageAspectRatio else composableH
                                            val currentMaxX = maxOf(0f, (imageW * scale.floatValue - composableW) / 2f)
                                            val currentMaxY = maxOf(0f, (imageH * scale.floatValue - composableH) / 2f)
                                            val flingFactor = 0.35f
                                            val targetFlingX = (offsetX.floatValue + velocity.x * flingFactor).coerceIn(-currentMaxX, currentMaxX)
                                            val targetFlingY = (offsetY.floatValue + velocity.y * flingFactor).coerceIn(-currentMaxY, currentMaxY)
                                            animJob.value?.cancel()
                                            animJob.value = coroutineScope.launch {
                                                launch {
                                                    androidx.compose.animation.core.animate(
                                                        initialValue = offsetX.floatValue,
                                                        targetValue = targetFlingX,
                                                        initialVelocity = velocity.x,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessVeryLow
                                                        )
                                                    ) { v, _ -> offsetX.floatValue = v }
                                                }
                                                launch {
                                                    androidx.compose.animation.core.animate(
                                                        initialValue = offsetY.floatValue,
                                                        targetValue = targetFlingY,
                                                        initialVelocity = velocity.y,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessVeryLow
                                                        )
                                                    ) { v, _ -> offsetY.floatValue = v }
                                                }
                                            }
                                        } else if (scale.floatValue in 1.0f..1.05f && (offsetX.floatValue != 0f || offsetY.floatValue != 0f || scale.floatValue != 1f)) {
                                            animJob.value?.cancel()
                                            animJob.value = coroutineScope.launch {
                                                launch { androidx.compose.animation.core.animate(scale.floatValue, 1f, animationSpec = MotionTokens.snappySpring()) { v, _ -> scale.floatValue = v; currentScale = v } }
                                                launch { androidx.compose.animation.core.animate(offsetX.floatValue, 0f, animationSpec = MotionTokens.snappySpring()) { v, _ -> offsetX.floatValue = v } }
                                                launch { androidx.compose.animation.core.animate(offsetY.floatValue, 0f, animationSpec = MotionTokens.snappySpring()) { v, _ -> offsetY.floatValue = v } }
                                            }
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    val rotZ = animRotation.value
                                    val theta = Math.toRadians(rotZ.toDouble())
                                    val sinSq = (kotlin.math.sin(theta) * kotlin.math.sin(theta)).toFloat()
                                    val composableW = parentWidth
                                    val composableH = parentHeight
                                    val imgW = if (imageAspectRatio > 0f) minOf(composableW, composableH * imageAspectRatio) else composableW
                                    val imgH = if (imageAspectRatio > 0f && imageAspectRatio.isFinite()) imgW / imageAspectRatio else composableH
                                    val fitScale = if (imgH > composableW && imgH > 0f && composableH > 0f && composableW > 0f) {
                                        val maxRotScale = minOf(composableW / imgH, composableH / imgW, 1f)
                                        1f - (1f - maxRotScale) * sinSq
                                    } else {
                                        1f
                                    }
                                    scaleX = scale.floatValue * fitScale
                                    scaleY = scale.floatValue * fitScale
                                    translationX = offsetX.floatValue
                                    translationY = offsetY.floatValue
                                    rotationZ = rotZ
                                }
                        )

                        if (page == pagerState.currentPage && motionPhotoInfo?.isMotionPhoto == true) {
                            MotionPhotoPlayerOverlay(
                                videoFile = extractedMotionVideo,
                                videoUri = motionPhotoInfo?.pairedVideoUri,
                                isPlaying = isMotionPlaying,
                                isLooping = isMotionLooping,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale.floatValue
                                        scaleY = scale.floatValue
                                        translationX = offsetX.floatValue
                                        translationY = offsetY.floatValue
                                    },
                                onPlaybackEnded = {
                                    if (!isMotionLooping) isMotionPlaying = false
                                }
                            )
                        }

                        if (page == pagerState.currentPage && !item.isVideo && scale.floatValue >= 3.0f && !isMotionPlaying) {
                            com.inferno.gallery.ui.components.TileBasedDeepZoomOverlay(
                                item = item,
                                scale = scale.floatValue,
                                offsetX = offsetX.floatValue,
                                offsetY = offsetY.floatValue,
                                pagerScale = 1f,
                                pagerAlpha = 1f,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        if (activeHighlight != null && highlightOverlayVisible && highlightRects.isNotEmpty() && highlightImageSize != null) {
                            val highlightColor = MaterialTheme.colorScheme.primary
                            val iSize = highlightImageSize!!

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale.floatValue * pagerScale
                                        scaleY = scale.floatValue * pagerScale
                                        alpha = pagerAlpha
                                        translationX = offsetX.floatValue
                                        translationY = offsetY.floatValue
                                    }
                            ) {
                                val fitScale = kotlin.math.min(size.width / iSize.width, size.height / iSize.height)
                                val dx = (size.width - iSize.width * fitScale) / 2f
                                val dy = (size.height - iSize.height * fitScale) / 2f
                                val padding = 6.dp.toPx()

                                val clipPath = androidx.compose.ui.graphics.Path().apply {
                                    for (rect in highlightRects) {
                                        val rLeft = dx + rect.left * fitScale - padding
                                        val rTop = dy + rect.top * fitScale - padding
                                        val rWidth = (rect.right - rect.left) * fitScale + (padding * 2)
                                        val rHeight = (rect.bottom - rect.top) * fitScale + (padding * 2)
                                        addRoundRect(androidx.compose.ui.geometry.RoundRect(
                                            rLeft, rTop, rLeft + rWidth, rTop + rHeight,
                                            androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                                        ))
                                    }
                                }
                                clipPath(path = clipPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                                    drawRect(Color.Black.copy(alpha = 0.6f))
                                }

                                for (rect in highlightRects) {
                                    val rLeft = dx + rect.left * fitScale - padding
                                    val rTop = dy + rect.top * fitScale - padding
                                    val rWidth = (rect.right - rect.left) * fitScale + (padding * 2)
                                    val rHeight = (rect.bottom - rect.top) * fitScale + (padding * 2)

                                    drawRoundRect(
                                        color = highlightColor.copy(alpha = 0.4f),
                                        topLeft = Offset(rLeft, rTop),
                                        size = androidx.compose.ui.geometry.Size(rWidth, rHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                                    )
                                    drawRoundRect(
                                        color = highlightColor,
                                        topLeft = Offset(rLeft, rTop),
                                        size = androidx.compose.ui.geometry.Size(rWidth, rHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                                    )
                                }
                            }
                        }

                    }
                }

            // Minimap Overlay — top-right corner
            AnimatedVisibility(
                visible = viewerZoomMinimapEnabled && scale.floatValue >= 10f,
                enter = fadeIn(MotionTokens.gentleSpring()),
                exit = fadeOut(MotionTokens.gentleSpring()),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 16.dp)
            ) {
                val maxThumbnailWidth = 80.dp
                val maxThumbnailHeight = 120.dp

                val thumbnailWidth = remember(imageAspectRatio) {
                    if (imageAspectRatio > (80f / 120f)) {
                        maxThumbnailWidth
                    } else {
                        maxThumbnailHeight * imageAspectRatio
                    }
                }
                val thumbnailHeight = remember(imageAspectRatio) {
                    if (imageAspectRatio > (80f / 120f)) {
                        maxThumbnailWidth / imageAspectRatio
                    } else {
                        maxThumbnailHeight
                    }
                }

                Box(
                    modifier = Modifier
                        .size(thumbnailWidth, thumbnailHeight)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(0.5.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolvedUri)
                            .size(240, 360)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Minimap map",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Draw the Viewport Indicator Map
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val imgW = if (imageAspectRatio > 0f) minOf(parentWidth, parentHeight * imageAspectRatio) else parentWidth
                        val imgH = if (imageAspectRatio > 0f && imageAspectRatio.isFinite()) imgW / imageAspectRatio else parentHeight
                        
                        val s = scale.floatValue
                        val visibleFractionX = minOf(1f, parentWidth / maxOf(1f, imgW * s))
                        val visibleFractionY = minOf(1f, parentHeight / maxOf(1f, imgH * s))

                        val viewportWidth = (size.width * visibleFractionX).coerceIn(10.dp.toPx(), size.width)
                        val viewportHeight = (size.height * visibleFractionY).coerceIn(10.dp.toPx(), size.height)

                        val maxPanX = maxOf(0.001f, (imgW * s - parentWidth) / 2f)
                        val maxPanY = maxOf(0.001f, (imgH * s - parentHeight) / 2f)

                        val panFractionX = (-offsetX.floatValue / maxPanX).coerceIn(-1f, 1f)
                        val panFractionY = (-offsetY.floatValue / maxPanY).coerceIn(-1f, 1f)

                        val maxTravelX = maxOf(0f, size.width - viewportWidth)
                        val maxTravelY = maxOf(0f, size.height - viewportHeight)

                        val rectX = (maxTravelX / 2f * (1f + panFractionX)).coerceIn(0f, maxTravelX)
                        val rectY = (maxTravelY / 2f * (1f + panFractionY)).coerceIn(0f, maxTravelY)

                        drawRect(
                            color = androidx.compose.ui.graphics.Color.White,
                            topLeft = Offset(rectX, rectY),
                            size = androidx.compose.ui.geometry.Size(viewportWidth, viewportHeight),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Zoom value badge at bottom center
                    val currentZoom = scale.floatValue
                    val zoomText = if (currentZoom >= 100f) {
                        "${currentZoom.toInt()}x"
                    } else {
                        val rounded = kotlin.math.round(currentZoom * 10f) / 10f
                        if (rounded % 1f == 0f) {
                            "${rounded.toInt()}x"
                        } else {
                            String.format(java.util.Locale.US, "%.1fx", rounded)
                        }
                    }

                    Surface(
                        shape = ShapeFull,
                        color = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 3.dp)
                    ) {
                        Text(
                            text = zoomText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            } // end BoxWithConstraints
            } // end wrapper Box
        }
        }

        val onLensAction = {
            if (currentItem != null) {
                val targetUri = resolvedCurrentUri ?: currentItem.uri
                com.inferno.gallery.ui.utils.GoogleLensUtils.launch(context, targetUri)
            }
        }

        // ── Floating Top Islands ──
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(MotionTokens.gentleSpring()) +
                    slideInVertically(MotionTokens.gentleSpring()) { -it },
            exit  = fadeOut(MotionTokens.gentleSpring()) +
                    slideOutVertically(MotionTokens.gentleSpring()) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            userInteractionTrigger++
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Island: Circular back button
                    FilledTonalIconButton(
                        onClick = safeOnBack,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                            contentDescription = "Go back",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center Island: Floating metadata pill
                    if (currentItem != null) {
                        val timestamp = remember(currentItem) {
                            val raw = if (currentItem.dateModified > 0) currentItem.dateModified else currentItem.dateAdded
                            if (raw > 1_000_000_000_000L) raw else raw * 1000L
                        }
                        val formattedDate = remember(timestamp) {
                            java.text.SimpleDateFormat("MMMM d, yyyy  h:mm a", java.util.Locale.getDefault())
                                .format(java.util.Date(timestamp))
                        }

                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = null,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(horizontal = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentItem.name,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (motionPhotoInfo?.isMotionPhoto == true) {
                            Surface(
                                onClick = {
                                    isMotionLooping = !isMotionLooping
                                    isMotionPlaying = isMotionLooping
                                },
                                shape = ShapeFull,
                                color = if (isMotionPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = if (isMotionPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_motion_photos_on),
                                        contentDescription = "Motion Photo",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Right Island: Circular more button
                        Box {
                            FilledTonalIconButton(
                                onClick = { showMoreMenu = true },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_more_horiz),
                                    contentDescription = stringResource(R.string.cd_more_options),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            androidx.compose.material3.DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                shape = MaterialTheme.shapes.large,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.detail_details)) },
                                    leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_info), contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        showInfoCard = true
                                    }
                                )
                                if (currentItem != null && !currentItem.isVideo) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Google Lens") },
                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_google_lens), contentDescription = null) },
                                        enabled = currentItem.localExists,
                                        onClick = {
                                            showMoreMenu = false
                                            onLensAction()
                                        }
                                    )
                                }
                                if (currentItem != null && motionPhotoInfo?.isMotionPhoto == true) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_export_motion_mp4)) },
                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_movie), contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            coroutineScope.launch {
                                                val exported = MotionPhotoHelper.exportMotionVideo(context, currentItem, motionPhotoInfo!!)
                                                if (exported != null) {
                                                    Toast.makeText(context, "Motion video saved to Movies/Photon_Motion", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to export motion video", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }
                                if (currentItem != null) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_open_with)) },
                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_open_in_new), contentDescription = null) },
                                        enabled = currentItem.localExists,
                                        onClick = {
                                            showMoreMenu = false
                                            val uri = resolvedCurrentUri ?: currentItem.uri
                                            val mimeType = context.contentResolver.getType(uri) ?: if (currentItem.isVideo) "video/*" else "image/*"
                                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, mimeType)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            try {
                                                context.startActivity(Intent.createChooser(viewIntent, "Open with"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                                if (currentItem != null && !currentItem.isVideo) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.detail_set_wallpaper)) },
                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_image), contentDescription = null) },
                                        enabled = currentItem.localExists,
                                        onClick = {
                                            showMoreMenu = false
                                            val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                                                setDataAndType(resolvedCurrentUri ?: currentItem.uri, "image/*")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                putExtra("mimeType", "image/*")
                                            }
                                            try {
                                                context.startActivity(Intent.createChooser(intent, "Set as..."))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "No app available to set wallpaper", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                                if (currentItem != null) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_copy)) },
                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_content_copy), contentDescription = null) },
                                        enabled = currentItem.localExists,
                                        onClick = {
                                            showMoreMenu = false
                                            showCopySheet = true
                                        }
                                    )
                                }
                                if (currentItem != null) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_move)) },
                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_drive_file_move), contentDescription = null) },
                                        enabled = currentItem.localExists,
                                        onClick = {
                                            showMoreMenu = false
                                            showMoveSheet = true
                                        }
                                    )
                                }
                                if (currentItem != null && !currentItem.isVideo) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_rotate)) },
                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_rotate_right), contentDescription = null) },
                                        enabled = currentItem.localExists,
                                        onClick = {
                                            // Keep 3-dot menu open so the user can tap Rotate repeatedly
                                            val target = currentItem
                                            pendingRotateItem = target
                                            val currentTarget = targetRotationMap[target.id] ?: 0f
                                            val nextTarget = currentTarget + 90f
                                            targetRotationMap[target.id] = nextTarget
                                            val anim = animRotationMap[target.id]
                                            coroutineScope.launch {
                                                anim?.animateTo(
                                                    targetValue = nextTarget,
                                                    animationSpec = androidx.compose.animation.core.spring(
                                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                            viewModel.rotateMedia(
                                                context = context,
                                                item = target,
                                                degrees = 90,
                                                onSecurityException = { pendingIntent ->
                                                    pendingRotateItem = target
                                                    rotateLauncher.launch(
                                                        androidx.activity.result.IntentSenderRequest.Builder(pendingIntent).build()
                                                    )
                                                }
                                            )
                                        }
                                    )
                                }
                                if (currentItem != null) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_rename)) },
                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_edit), contentDescription = null) },
                                        enabled = currentItem.localExists,
                                        onClick = {
                                            showMoreMenu = false
                                            showRenameDialog = true
                                        }
                                    )
                                }
                                if (currentItem != null && currentItem.isVideo) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_extract_frame)) },
                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_photo_camera), contentDescription = null) },
                                        enabled = currentItem.localExists,
                                        onClick = {
                                            showMoreMenu = false
                                            val uri = resolvedCurrentUri ?: currentItem.uri
                                            onNavigateToFrameExtractor(uri)
                                        }
                                    )
                                }
                                // Hide (Private Space)
                                if (currentItem != null) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.detail_move_vault)) },
                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_visibility_off), contentDescription = null) },
                                        enabled = currentItem.localExists,
                                        onClick = {
                                            showMoreMenu = false
                                            val activity = context as? androidx.fragment.app.FragmentActivity
                                            if (activity != null) {
                                                viewModel.vaultAuthManager.authenticate(
                                                    activity = activity,
                                                    negativeButtonText = "Cancel",
                                                    unlockSession = false,
                                                    onSuccess = {
                                                        viewModel.hideMedia(listOf(currentItem.uri))
                                                        viewModel.vaultAuthManager.lock()
                                                        safeOnBack()
                                                    },
                                                    onFailure = {}
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                }
            }
        }
    }

        ExifDetailsSheet(
            isOpen = showInfoCard,
            onDismiss = { showInfoCard = false },
            galleryItem = galleryItems.getOrNull(pagerState.currentPage),
            exifData = currentExif,
            onPersonClick = onNavigateToPerson
        )

        val tipMotionDismissed by settingsRepo.tipMotionPhotoDismissedFlow.collectAsState(initial = true)
        if (!tipMotionDismissed && motionPhotoInfo?.isMotionPhoto == true && showUi) {
            FloatingTipCard(
                title = stringResource(R.string.quick_tip),
                message = stringResource(R.string.tip_motion_photo),
                iconResId = R.drawable.ic_ms_motion_photos_on,
                onDismiss = {
                    coroutineScope.launch {
                        settingsRepo.dismissTip(com.inferno.gallery.data.SettingsRepository.TIP_MOTION_PHOTO)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (viewerFilmstripEnabled && galleryItems.size > 1) 136.dp else 84.dp)
            )
        }

        val currentItem = galleryItems.getOrNull(pagerState.currentPage)
        val isFavorite = currentItem?.id?.let { favoriteIds.contains(it) } ?: false
        val heartView = androidx.compose.ui.platform.LocalView.current

        val onRestoreAction = {
            if (currentItem != null) {
                val isMediaStoreUri = currentItem.uri.toString().startsWith("content://")
                if (isMediaStoreUri) {
                    pendingDeleteItem = currentItem
                    pendingDeletePage = pagerState.currentPage
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val restoreIntent = MediaStore.createTrashRequest(context.contentResolver, listOf(currentItem.uri), false)
                            val request = androidx.activity.result.IntentSenderRequest.Builder(restoreIntent.intentSender).build()
                            withContext(Dispatchers.Main) {
                                restoreLauncher.launch(request)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(context, "Unable to restore: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        val onPermanentDeleteAction = {
            if (currentItem != null) {
                val isMediaStoreUri = currentItem.uri.toString().startsWith("content://")
                if (isMediaStoreUri) {
                    pendingDeleteItem = currentItem
                    pendingDeletePage = pagerState.currentPage
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val deleteIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(currentItem.uri))
                            val request = androidx.activity.result.IntentSenderRequest.Builder(deleteIntent.intentSender).build()
                            withContext(Dispatchers.Main) {
                                launcher.launch(request)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(context, "Unable to delete: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        val onShareAction = {
            val item = currentItem
            if (item != null) {
                coroutineScope.launch {
                    val strip = viewModel.settingsRepository.stripMetadataOnShareFlow.first()
                    com.inferno.gallery.ui.utils.ShareUtils.shareMedia(context, listOf(item.uri), strip)
                }
            }
        }

        val onEditAction = {
            if (currentItem != null) {
                val uri = resolvedCurrentUri ?: currentItem.uri
                if (!currentItem.isVideo) {
                    onNavigateToEditor(uri)
                } else {
                    val editIntent = Intent(Intent.ACTION_EDIT).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(Intent.createChooser(editIntent, "Edit Media"))
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "No editor available on device", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val onFavoriteAction = {
            if (currentItem != null) {
                heartView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                viewModel.toggleFavorite(currentItem.id)
            }
        }

        val onDeleteAction = {
            if (currentItem != null) {
                val isMediaStoreUri = currentItem.uri.toString().startsWith("content://")
                if (!isMediaStoreUri) {
                    android.widget.Toast.makeText(context, "Cannot delete this item", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    pendingDeleteItem = currentItem
                    pendingDeletePage = pagerState.currentPage
                    if (confirmDeleteEnabled) {
                        showDeleteConfirmDialog = true
                    } else {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val trashIntent = MediaStore.createTrashRequest(
                                    context.contentResolver,
                                    listOf(currentItem.uri),
                                    true
                                )
                                val request = IntentSenderRequest.Builder(trashIntent.intentSender).build()
                                withContext(Dispatchers.Main) {
                                    launcher.launch(request)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Unable to trash this item: ${e.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }
        }



        // ── Floating Action Dock ──
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(MotionTokens.gentleSpring()) +
                    slideInVertically(MotionTokens.gentleSpring()) { it } +
                    scaleIn(MotionTokens.snappySpring()),
            exit  = fadeOut(MotionTokens.gentleSpring()) +
                    slideOutVertically(MotionTokens.gentleSpring()) { it } +
                    scaleOut(MotionTokens.gentleSpring()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            ViewerActionButtonsRow(
                currentItem = currentItem,
                isTrash = bucketName == "Trash",
                isFavorite = isFavorite,
                onRestore = onRestoreAction,
                onPermanentDelete = onPermanentDeleteAction,
                onShare = onShareAction,
                onEdit = onEditAction,
                onFavorite = onFavoriteAction,
                onDelete = onDeleteAction,
                onInfo = { showInfoCard = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            userInteractionTrigger++
                        }
                    }
            )
        }
    }
}

@Composable
private fun ViewerFilmstripRow(
    galleryItems: List<GalleryItem>,
    currentPage: Int,
    onThumbnailClick: (Int) -> Unit,
    onScrollInProgress: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) {
        onScrollInProgress(listState.isScrollInProgress)
    }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp

    // Center the active thumbnail when currentPage changes
    LaunchedEffect(currentPage, galleryItems.size) {
        if (galleryItems.isNotEmpty() && currentPage in galleryItems.indices) {
            val offset = with(density) { -(screenWidth / 2 - 24.dp).roundToPx() }
            listState.animateScrollToItem(index = currentPage, scrollOffset = offset)
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        itemsIndexed(
            items = galleryItems,
            key = { _, item -> item.id }
        ) { index, item ->
            val isSelected = index == currentPage
            val alpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.80f,
                animationSpec = MotionTokens.fastEffectsSpec(),
                label = "filmstrip_alpha"
            )
            val borderStroke = if (isSelected) {
                BorderStroke(2.dp, Color.White)
            } else {
                null
            }

            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 50.dp)
                    .clip(ShapeExtraSmall)
                    .then(
                        if (borderStroke != null) Modifier.border(borderStroke, ShapeExtraSmall)
                        else Modifier
                    )
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable {
                        onThumbnailClick(index)
                    }
            ) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(alpha)
                )

                if (item.isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .size(14.dp)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f), ShapeFull),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerActionButtonsRow(
    currentItem: GalleryItem?,
    isTrash: Boolean,
    isFavorite: Boolean,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Outer Pill container for bottom controls pill:
    // Stadium capsule (CircleShape), height 64.dp, 6.dp padding on all sides,
    // pure solid tonal layering with zero outlines, shadows, or elevations.
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTrash) {
                // 1. Restore
                ViewerToolTile(
                    icon = ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                    contentDescription = "Restore",
                    onClick = onRestore,
                    tileShape = getViewerSegmentShape(index = 0, total = 2),
                    modifier = Modifier.weight(1f)
                )

                // 2. Permanently Delete
                ViewerToolTile(
                    icon = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                    contentDescription = "Permanently Delete",
                    onClick = onPermanentDelete,
                    tileShape = getViewerSegmentShape(index = 1, total = 2),
                    modifier = Modifier.weight(1f)
                )
            } else {
                // 1. Share
                ViewerToolTile(
                    icon = ImageVector.vectorResource(R.drawable.ic_ms_share),
                    contentDescription = stringResource(R.string.detail_share),
                    enabled = currentItem?.localExists == true,
                    onClick = onShare,
                    tileShape = getViewerSegmentShape(index = 0, total = 5),
                    modifier = Modifier.weight(1f)
                )

                // 2. Favorite with spring bounce animation
                val favoriteHarmonized = com.inferno.gallery.ui.theme.LocalHarmonizedColors.current.favorite
                var heartBounce by remember { mutableStateOf(false) }
                val heartScale by animateFloatAsState(
                    targetValue = if (heartBounce) 1.45f else 1f,
                    animationSpec = MotionTokens.bouncySpring(),
                    label = "heartScale",
                    finishedListener = { if (heartBounce) heartBounce = false }
                )
                ViewerToolTile(
                    icon = if (isFavorite) ImageVector.vectorResource(R.drawable.ic_ms_favorite_filled) else ImageVector.vectorResource(R.drawable.ic_ms_favorite),
                    contentDescription = stringResource(R.string.detail_favorite),
                    tint = if (isFavorite) favoriteHarmonized else MaterialTheme.colorScheme.onSurface,
                    iconModifier = Modifier.graphicsLayer {
                        scaleX = heartScale
                        scaleY = heartScale
                    },
                    onClick = {
                        heartBounce = true
                        onFavorite()
                    },
                    tileShape = getViewerSegmentShape(index = 1, total = 5),
                    modifier = Modifier.weight(1f)
                )

                // 3. Edit / Tune
                ViewerToolTile(
                    icon = ImageVector.vectorResource(if (currentItem?.isVideo == true) R.drawable.ic_ms_photo_camera else R.drawable.ic_ms_tune),
                    contentDescription = stringResource(if (currentItem?.isVideo == true) R.string.action_edit else R.string.detail_edit),
                    enabled = currentItem?.localExists == true,
                    onClick = onEdit,
                    tileShape = getViewerSegmentShape(index = 2, total = 5),
                    modifier = Modifier.weight(1f)
                )

                // 4. Info / Details
                ViewerToolTile(
                    icon = ImageVector.vectorResource(R.drawable.ic_ms_info),
                    contentDescription = stringResource(R.string.detail_details),
                    onClick = onInfo,
                    tileShape = getViewerSegmentShape(index = 3, total = 5),
                    modifier = Modifier.weight(1f)
                )

                // 5. Delete
                ViewerToolTile(
                    icon = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                    contentDescription = stringResource(R.string.detail_delete),
                    onClick = onDelete,
                    tileShape = getViewerSegmentShape(index = 4, total = 5),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Calculates connected segmented button shapes for concentric capsule design:
 * - Start tile: Outer corner follows concentric capsule curve (26.dp), inner corner has subtle radius (6.dp).
 * - Middle tiles: All corners subtle radius (6.dp).
 * - End tile: Inner corner subtle radius (6.dp), outer corner follows concentric capsule curve (26.dp).
 */
private fun getViewerSegmentShape(index: Int, total: Int, outerCorner: Dp = 26.dp, innerCorner: Dp = 6.dp): Shape =
    com.inferno.gallery.ui.theme.getSegmentShape(index, total, outerCorner, innerCorner)

/**
 * Individual tool tile inside the Pixel-style controls pill.
 * Features independent spring touch scale, tonal surface, and subtle border.
 */
@Composable
private fun ViewerToolTile(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    iconModifier: Modifier = Modifier,
    tileShape: Shape = RoundedCornerShape(6.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = MotionTokens.bouncySpring(),
        label = "toolTileScale"
    )

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(tileShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                onClick = onClick
            ),
        shape = tileShape,
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.38f),
        border = null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = iconModifier.size(22.dp)
            )
        }
    }
}
