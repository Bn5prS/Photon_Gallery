package com.inferno.gallery.ui

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
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

    val galleryItems = remember(rawGalleryItems, initialDetailItem) {
        if (rawGalleryItems.isNotEmpty()) {
            rawGalleryItems
        } else if (initialDetailItem != null) {
            listOf(initialDetailItem!!)
        } else {
            emptyList()
        }
    }

    val maxBrightnessEnabled by settingsRepo.maxBrightnessEnabledFlow.collectAsState(initial = false)
    val viewerBlurEffect by settingsRepo.viewerBlurEffectFlow.collectAsState(initial = false)
    val viewerDynamicColor by settingsRepo.viewerDynamicColorFlow.collectAsState(initial = false)
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

    // Ensure we don't crash if items is empty
    if (galleryItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow))
        return
    }

    val initialPage = remember(mediaId, galleryItems) {
        val index = galleryItems.indexOfFirst { 
            it.id == mediaId || it.uri.toString() == mediaId || it.path == mediaId 
        }
        if (index >= 0) index else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { galleryItems.size }
    )

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

    val ambientColorCache = remember { android.util.LruCache<String, Color>(64) }
    var extractedAmbientColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(currentItem?.uri, viewerDynamicColor) {
        if (!viewerDynamicColor || currentItem == null) {
            extractedAmbientColor = null
            return@LaunchedEffect
        }
        val cacheKey = currentItem.uri.toString()
        val cached = ambientColorCache.get(cacheKey)
        if (cached != null) {
            extractedAmbientColor = cached
            return@LaunchedEffect
        }

        val color = withContext(Dispatchers.IO) {
            try {
                val req = ImageRequest.Builder(context)
                    .data(currentItem.uri)
                    .size(48, 48)
                    .precision(coil3.size.Precision.INEXACT)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(req)
                val image = (result as? coil3.request.SuccessResult)?.image
                val bitmap = when (image) {
                    is BitmapImage -> image.bitmap
                    is DrawableImage -> drawableToBitmap(image.drawable)
                    else -> null
                }
                if (bitmap != null) {
                    val extracted = extractAmbientMoodColor(bitmap)
                    if (extracted != Color.Unspecified) extracted else null
                } else null
            } catch (e: Exception) {
                null
            }
        }
        if (color != null) {
            ambientColorCache.put(cacheKey, color)
        }
        extractedAmbientColor = color
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
    val overviewProgress by animateFloatAsState(
        targetValue = if (isFilmstripOverview) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "overviewProgress"
    )

    // Exit overview mode on hardware/gesture back
    BackHandler(enabled = isFilmstripOverview) {
        isFilmstripOverview = false
        showUi = true
    }

    // Auto hide UI after 4 seconds of inactivity — skip if in filmstrip overview
    LaunchedEffect(showUi, pagerState.isScrollInProgress, pagerState.currentPage, isFilmstripOverview) {
        if (showUi && !isFilmstripOverview) {
            kotlinx.coroutines.delay(4000)
            showUi = false
        }
    }

    var currentScale by remember { mutableStateOf(1f) }
    var isZoomBadgeVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentScale) {
        if (currentScale > 1.05f) {
            isZoomBadgeVisible = true
            kotlinx.coroutines.delay(700L)
            isZoomBadgeVisible = false
        } else {
            isZoomBadgeVisible = false
        }
    }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var pendingDeleteItem by remember { mutableStateOf<GalleryItem?>(null) }
    var pendingDeletePage by remember { mutableStateOf<Int?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val itemToDelete = pendingDeleteItem
            val deletedUri = itemToDelete?.uri?.toString()
            if (deletedUri != null) {
                viewModel.removeMediaOptimistically(deletedUri)
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

    var showRenameDialog by remember { mutableStateOf(false) }
    var pendingRenameItem by remember { mutableStateOf<GalleryItem?>(null) }
    var pendingRenameNewName by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showCopySheet by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }

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

    androidx.compose.runtime.DisposableEffect(useFullScreenGlobal) {
        if (window != null && insetsController != null) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            
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
                if (!useFullScreenGlobal) {
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
    }

    // Single-tap immersive mode: the status bar follows the viewer chrome toggle.
    // Skipped when the global full-screen setting already keeps system bars hidden.
    LaunchedEffect(showUi, useFullScreenGlobal) {
        if (insetsController != null && !useFullScreenGlobal) {
            if (showUi) {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    if (showDeleteConfirmDialog && pendingDeleteItem != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { androidx.compose.material3.Text("Move to Recycle Bin") },
            text = { androidx.compose.material3.Text("This item will be moved to the Recycle Bin.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        try {
                            val trashIntent = MediaStore.createTrashRequest(
                                context.contentResolver, 
                                listOf(pendingDeleteItem!!.uri), 
                                true
                            )
                            val request = IntentSenderRequest.Builder(trashIntent.intentSender).build()
                            launcher.launch(request)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(
                                context, 
                                "Unable to trash this item: ${e.message}", 
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    androidx.compose.material3.Text("Move to Bin")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    if (showRenameDialog && currentItem != null) {
        var renameText by remember(currentItem) { mutableStateOf(currentItem.name) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { androidx.compose.material3.Text("Rename File") },
            text = {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { androidx.compose.material3.Text("File Name") },
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
                    androidx.compose.material3.Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showRenameDialog = false }
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    if (showCopySheet && currentItem != null) {
        val allAlbumsList by viewModel.allAlbums.collectAsState()
        AlbumPickerSheet(
            title = "Copy to Album",
            itemCount = 1,
            albums = allAlbumsList,
            showPrivateSpaceOption = false,
            confirmPathButtonText = "Copy Here",
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
            title = "Move to Album",
            itemCount = 1,
            albums = allAlbumsList,
            showPrivateSpaceOption = true,
            confirmPathButtonText = "Move Here",
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
                        onSuccess = {
                            viewModel.hideMedia(listOf(currentItem.uri))
                            safeOnBack()
                        },
                        onFailure = {}
                    )
                }
            }
        )
    }

    var showInfoCard by remember { mutableStateOf(false) }
    var currentExif by remember { mutableStateOf<DetailedExifData?>(null) }
    
    var motionPhotoInfo by remember { mutableStateOf<MotionPhotoInfo?>(null) }
    var extractedMotionVideo by remember { mutableStateOf<java.io.File?>(null) }
    var isMotionPlaying by remember { mutableStateOf(false) }
    var isMotionLooping by remember { mutableStateOf(false) }

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

    val baseCanvasColor = MaterialTheme.colorScheme.surfaceContainerLow
    val targetBgColor = if (viewerDynamicColor) {
        extractedAmbientColor ?: baseCanvasColor
    } else {
        baseCanvasColor
    }
    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "viewerBgColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor.copy(alpha = bgAlpha)),
        contentAlignment = Alignment.Center
    ) {
        if (viewerDynamicColor && !viewerBlurEffect && extractedAmbientColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedBgColor.copy(alpha = 0.85f * bgAlpha),
                                animatedBgColor.copy(alpha = 0.40f * bgAlpha),
                                Color.Black.copy(alpha = 0.95f * bgAlpha)
                            ),
                            radius = 1400f
                        )
                    )
            )
        }

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
                        .blur(24.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .alpha(if (viewerDynamicColor) 0.50f * bgAlpha else 0.40f * bgAlpha)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (viewerDynamicColor) {
                                animatedBgColor.copy(alpha = 0.55f * bgAlpha)
                            } else {
                                Color.Black.copy(alpha = 0.45f * bgAlpha)
                            }
                        )
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
                                        animationSpec = MotionTokens.bouncySpring()
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                dismissOffsetY.animateTo(
                                    0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
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
                                                val newScale = (oldScale * zoomChange).coerceIn(1f, 25f)
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
                                    scaleX = scale.floatValue
                                    scaleY = scale.floatValue
                                    translationX = offsetX.floatValue
                                    translationY = offsetY.floatValue
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
                visible = scale.floatValue >= 5f,
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
                        val viewportWidth = size.width / scale.floatValue
                        val viewportHeight = size.height / scale.floatValue
                        
                        val maxOffsetX = (parentWidth * (scale.floatValue - 1)) / 2
                        val maxOffsetY = (parentHeight * (scale.floatValue - 1)) / 2
                        
                        val pctX = if (maxOffsetX > 0) -offsetX.floatValue / maxOffsetX else 0f
                        val pctY = if (maxOffsetY > 0) -offsetY.floatValue / maxOffsetY else 0f
                        
                        val maxViewportW = maxOf(0f, size.width - viewportWidth)
                        val maxViewportH = maxOf(0f, size.height - viewportHeight)
                        val rectX = ((size.width - viewportWidth) / 2 * (1 + pctX)).coerceIn(0f, maxViewportW)
                        val rectY = ((size.height - viewportHeight) / 2 * (1 + pctY)).coerceIn(0f, maxViewportH)
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.White,
                            topLeft = Offset(rectX, rectY),
                            size = androidx.compose.ui.geometry.Size(viewportWidth, viewportHeight),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            } // end BoxWithConstraints
            } // end wrapper Box
        }
        }

        // ── Unified Solid Top App Bar ──
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                shadowElevation = 3.dp,
                shape = ShapeNone
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = safeOnBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                            contentDescription = "Go back",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (currentItem != null) {
                            val timestamp = remember(currentItem) {
                                val raw = if (currentItem.dateModified > 0) currentItem.dateModified else currentItem.dateAdded
                                if (raw > 1_000_000_000_000L) raw else raw * 1000L
                            }
                            val formattedDate = remember(timestamp) {
                                java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(timestamp))
                            }
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentItem.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (motionPhotoInfo?.isMotionPhoto == true) {
                        Surface(
                            onClick = {
                                isMotionLooping = !isMotionLooping
                                isMotionPlaying = isMotionLooping
                            },
                            shape = ShapeFull,
                            color = if (isMotionPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (isMotionPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_motion_photos_on),
                                    contentDescription = "Motion Photo",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }

                    FilledTonalIconButton(
                        onClick = { showInfoCard = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_info),
                            contentDescription = "Info",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        FilledTonalIconButton(
                            onClick = { showMoreMenu = true },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_more_vert),
                                contentDescription = "More options",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                    androidx.compose.material3.DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        shape = MaterialTheme.shapes.large,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        if (currentItem != null && motionPhotoInfo?.isMotionPhoto == true) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Export Motion Video (MP4)") },
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
                                text = { Text("Open with") },
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
                                    text = { Text("Set as wallpaper") },
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
                                    text = { Text("Copy to") },
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
                                    text = { Text("Move to") },
                                    leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_drive_file_move), contentDescription = null) },
                                    enabled = currentItem.localExists,
                                    onClick = {
                                        showMoreMenu = false
                                        showMoveSheet = true
                                    }
                                )
                            }
                            if (currentItem != null) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Rename") },
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
                                    text = { Text("Extract frame") },
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
                                    text = { Text("Hide") },
                                    leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_visibility_off), contentDescription = null) },
                                    enabled = currentItem.localExists,
                                    onClick = {
                                        showMoreMenu = false
                                        val activity = context as? androidx.fragment.app.FragmentActivity
                                        if (activity != null) {
                                            viewModel.vaultAuthManager.authenticate(
                                                activity = activity,
                                                onSuccess = {
                                                    viewModel.hideMedia(listOf(currentItem.uri))
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

        ExifDetailsSheet(
            isOpen = showInfoCard,
            onDismiss = { showInfoCard = false },
            galleryItem = galleryItems.getOrNull(pagerState.currentPage),
            exifData = currentExif,
            onPersonClick = onNavigateToPerson
        )

        // ── Floating Deep Zoom Pill ──
        AnimatedVisibility(
            visible = isZoomBadgeVisible && currentScale > 1.05f,
            enter = fadeIn(MotionTokens.fastEffectsSpec()) + scaleIn(MotionTokens.bouncySpring()),
            exit = fadeOut(MotionTokens.fastEffectsSpec()) + scaleOut(MotionTokens.bouncySpring()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (showUi) (if (viewerFilmstripEnabled && galleryItems.size > 1) 130.dp else 78.dp) else 24.dp)
        ) {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 6.dp,
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f×", currentScale),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (currentScale >= 3.0f) {
                        Text(
                            text = "• DEEP ZOOM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        val tipMotionDismissed by settingsRepo.tipMotionPhotoDismissedFlow.collectAsState(initial = true)
        if (!tipMotionDismissed && motionPhotoInfo?.isMotionPhoto == true && showUi) {
            FloatingTipCard(
                title = "Quick Tip",
                message = "Press & hold anywhere on the photo to play live motion video.",
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
                    val restoreIntent = MediaStore.createTrashRequest(context.contentResolver, listOf(currentItem.uri), false)
                    restoreLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(restoreIntent.intentSender).build())
                }
            }
        }

        val onPermanentDeleteAction = {
            if (currentItem != null) {
                val isMediaStoreUri = currentItem.uri.toString().startsWith("content://")
                if (isMediaStoreUri) {
                    pendingDeleteItem = currentItem
                    pendingDeletePage = pagerState.currentPage
                    val deleteIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(currentItem.uri))
                    launcher.launch(androidx.activity.result.IntentSenderRequest.Builder(deleteIntent.intentSender).build())
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
                        try {
                            val trashIntent = MediaStore.createTrashRequest(
                                context.contentResolver,
                                listOf(currentItem.uri),
                                true
                            )
                            val request = IntentSenderRequest.Builder(trashIntent.intentSender).build()
                            launcher.launch(request)
                        } catch (e: Exception) {
                            e.printStackTrace()
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

        val onLensAction = {
            if (currentItem != null) {
                val targetUri = resolvedCurrentUri ?: currentItem.uri
                com.inferno.gallery.ui.utils.GoogleLensUtils.launch(context, targetUri)
            }
        }

        if (!viewerFilmstripEnabled) {
            // ── Material 3 Expressive Floating Bottom Action Dock (Preserved) ──
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
                    .padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 2.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(min = 280.dp, max = 360.dp)
                        .height(54.dp)
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
                        onLens = onLensAction,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        } else {
            // ── Material 3 Expressive Docked Bottom Bar with Filmstrip ──
            AnimatedVisibility(
                visible = showUi,
                enter = fadeIn(MotionTokens.gentleSpring()) +
                        slideInVertically(MotionTokens.gentleSpring()) { it },
                exit  = fadeOut(MotionTokens.gentleSpring()) +
                        slideOutVertically(MotionTokens.gentleSpring()) { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Surface(
                    shape = ShapeNone,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (galleryItems.size > 1) {
                            ViewerFilmstripRow(
                                galleryItems = galleryItems,
                                currentPage = pagerState.currentPage,
                                onThumbnailClick = { index ->
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                },
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            contentAlignment = Alignment.Center
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
                                onLens = onLensAction,
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun ViewerFilmstripRow(
    galleryItems: List<GalleryItem>,
    currentPage: Int,
    onThumbnailClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
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
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
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
    onLens: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTrash) {
            FilledTonalIconButton(
                onClick = onRestore,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                    contentDescription = "Restore",
                    modifier = Modifier.size(24.dp)
                )
            }

            FilledTonalIconButton(
                onClick = onPermanentDelete,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                    contentDescription = "Permanently Delete",
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // Share
            FilledTonalIconButton(
                onClick = onShare,
                enabled = currentItem?.localExists == true,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_share),
                    contentDescription = "Share",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Edit / Frame extractor
            FilledTonalIconButton(
                enabled = currentItem?.localExists == true,
                onClick = onEdit,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(if (currentItem?.isVideo == true) R.drawable.ic_ms_photo_camera else R.drawable.ic_ms_edit),
                    contentDescription = if (currentItem?.isVideo == true) "Extract frame" else "Edit",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Favorite
            val favoriteHarmonized = com.inferno.gallery.ui.theme.LocalHarmonizedColors.current.favorite
            var heartBounce by remember { mutableStateOf(false) }
            val heartScale by animateFloatAsState(
                targetValue = if (heartBounce) 1.45f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "heartScale",
                finishedListener = { if (heartBounce) heartBounce = false }
            )
            FilledTonalIconButton(
                onClick = {
                    heartBounce = true
                    onFavorite()
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isFavorite) favoriteHarmonized.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (isFavorite) favoriteHarmonized else MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) ImageVector.vectorResource(R.drawable.ic_ms_favorite_filled) else ImageVector.vectorResource(R.drawable.ic_ms_favorite),
                    contentDescription = "Favorite",
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = heartScale
                            scaleY = heartScale
                        }
                )
            }

            // Delete
            FilledTonalIconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                    contentDescription = "Delete",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Google Lens (Images only)
            if (currentItem != null && !currentItem.isVideo) {
                FilledTonalIconButton(
                    onClick = onLens,
                    enabled = currentItem.localExists,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_google_lens),
                        contentDescription = "Google Lens",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}


data class ExifData(
    val model: String?,
    val aperture: String?,
    val iso: String?,
    val shutterSpeed: String?,
    val focalLength: String?,
    val resolution: String?
)

suspend fun extractExif(context: android.content.Context, uri: Uri): ExifData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    var model: String? = null
    var aperture: String? = null
    var iso: String? = null
    var shutterSpeed: String? = null
    var focalLength: String? = null
    var resolution: String? = null

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val exif = androidx.exifinterface.media.ExifInterface(inputStream)
            model = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL)
            aperture = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }
            iso = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" }
            shutterSpeed = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME)?.let { "${it}s" }
            focalLength = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it}mm" }
            
            val width = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_WIDTH)
            val length = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_LENGTH)
            if (width != null && length != null) {
                resolution = "${width}x${length}"
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    ExifData(model, aperture, iso, shutterSpeed, focalLength, resolution)
}

fun formatExifDate(timestamp: Long): String {
    return java.text.SimpleDateFormat("dd-MM-yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}

fun formatSizeToMB(sizeBytes: Long): String {
    return String.format(java.util.Locale.US, "%.2f MB", sizeBytes / (1024.0 * 1024.0))
}

@Composable
fun ExifInfoCard(galleryItem: GalleryItem?, exifData: ExifData?, modifier: Modifier = Modifier) {
    if (galleryItem == null) return
    Surface(
        modifier = modifier.widthIn(min = 280.dp, max = 340.dp),
        shape = ShapeLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // File Name — full wrap
            InfoBlock(label = "Name", value = galleryItem.name)

            // Time + Size row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoBlock(
                    label = "Modified",
                    value = formatExifDate(galleryItem.dateModified * 1000L),
                    modifier = Modifier.weight(1f)
                )
                InfoBlock(
                    label = "Size",
                    value = formatSizeToMB(galleryItem.size),
                    modifier = Modifier.weight(1f)
                )
            }

            // Resolution + Device row
            val resolution = exifData?.resolution
            val device = exifData?.model
            if (resolution != null || device != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (resolution != null) {
                        InfoBlock(label = "Resolution", value = resolution, modifier = Modifier.weight(1f))
                    }
                    if (device != null) {
                        InfoBlock(label = "Device", value = device, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Camera parameters
            val paramsParts = listOfNotNull(exifData?.focalLength, exifData?.shutterSpeed, exifData?.iso, exifData?.aperture).filter { it.isNotBlank() }
            if (paramsParts.isNotEmpty()) {
                InfoBlock(label = "Parameters", value = paramsParts.joinToString("  •  "))
            }

            // Path — full wrap
            InfoBlock(label = "Path", value = galleryItem.path)
        }
    }
}

@Composable
private fun InfoBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(

                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
    if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth <= 0) 1 else drawable.intrinsicWidth
    val height = if (drawable.intrinsicHeight <= 0) 1 else drawable.intrinsicHeight
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private fun extractAmbientMoodColor(bitmap: Bitmap): Color {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return Color.Unspecified

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    var totalWeight = 0f
    var sumR = 0f
    var sumG = 0f
    var sumB = 0f
    val hsv = FloatArray(3)

    for (pixel in pixels) {
        val a = (pixel shr 24) and 0xff
        if (a < 128) continue

        android.graphics.Color.colorToHSV(pixel, hsv)
        val s = hsv[1]
        val v = hsv[2]

        // Skip near-black voids or blown highlights
        if (v < 0.08f || (v > 0.94f && s < 0.12f)) continue

        val weight = 1f + (s * 3.5f)
        val r = (pixel shr 16) and 0xff
        val g = (pixel shr 8) and 0xff
        val b = pixel and 0xff

        sumR += r * weight
        sumG += g * weight
        sumB += b * weight
        totalWeight += weight
    }

    if (totalWeight <= 0f) return Color.Unspecified

    val avgR = (sumR / totalWeight).toInt().coerceIn(0, 255)
    val avgG = (sumG / totalWeight).toInt().coerceIn(0, 255)
    val avgB = (sumB / totalWeight).toInt().coerceIn(0, 255)

    android.graphics.Color.colorToHSV(android.graphics.Color.rgb(avgR, avgG, avgB), hsv)

    if (hsv[1] < 0.08f) return Color.Unspecified

    hsv[1] = hsv[1].coerceIn(0.40f, 0.75f)
    hsv[2] = 0.13f

    return Color(android.graphics.Color.HSVToColor(hsv))
}
