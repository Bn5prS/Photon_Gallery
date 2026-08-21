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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.font.FontFamily
import com.inferno.gallery.ui.theme.MotionTokens
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.ShapeNone
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector




@OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun DetailScreen(
    mediaId: String,
    bucketName: String?,
    highlightText: String? = null,
    clusterId: Long? = null,
    useFullScreenGlobal: Boolean = false,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToEditor: (android.net.Uri) -> Unit = {}
) {
    androidx.compose.runtime.LaunchedEffect(bucketName) {
        viewModel.setBucket(bucketName)
    }

    val context = LocalContext.current
    val settingsRepo = viewModel.settingsRepository
    val confirmDeleteEnabled by settingsRepo.confirmDeleteEnabledFlow.collectAsState(initial = true)
    val activity = context as? android.app.Activity
    val window = activity?.window
    val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
    
    val rawGalleryItems by viewModel.detailMedia.collectAsState()
    val initialDetailItem by viewModel.initialDetailItem.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val galleryItems = remember(rawGalleryItems, initialDetailItem, mediaId) {
        if (rawGalleryItems.isNotEmpty()) {
            rawGalleryItems
        } else if (initialDetailItem != null && initialDetailItem?.id == mediaId) {
            listOf(initialDetailItem!!)
        } else {
            emptyList()
        }
    }

    val maxBrightnessEnabled by settingsRepo.maxBrightnessEnabledFlow.collectAsState(initial = false)
    val viewerBlurEffect by settingsRepo.viewerBlurEffectFlow.collectAsState(initial = false)

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

    // Ensure we don't crash if items is empty
    if (galleryItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val initialPage = remember(mediaId, galleryItems) {
        val index = galleryItems.indexOfFirst { it.id == mediaId }
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
    var isLocalFileMissing by remember(currentItem) { mutableStateOf(false) }
    LaunchedEffect(currentItem) {
        if (currentItem != null) {
            isLocalFileMissing = withContext(Dispatchers.IO) {
                !java.io.File(currentItem.path).exists()
            }
        }
    }


    val resolvedCurrentUri by produceState<Uri?>(initialValue = currentItem?.uri, key1 = currentItem) {
        val uri = currentItem?.uri
        value = uri
    }


    var activeFaceClusterId by remember { mutableStateOf(clusterId) }
    var activeHighlight by remember { mutableStateOf(highlightText) }
    var highlightRects by remember { mutableStateOf<List<android.graphics.Rect>>(emptyList()) }
    var highlightImageSize by remember { mutableStateOf<androidx.compose.ui.geometry.Size?>(null) }
    var highlightOverlayVisible by remember { mutableStateOf(highlightText != null || clusterId != null) }

    androidx.compose.runtime.LaunchedEffect(galleryItems, mediaId) {
        if (galleryItems.isNotEmpty()) {
            val targetIndex = galleryItems.indexOfFirst { it.id == mediaId }
            if (targetIndex >= 0 && pagerState.currentPage != targetIndex) {
                pagerState.scrollToPage(targetIndex)
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
    val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()

    var showUi by remember { mutableStateOf(true) }

    // Auto hide UI after 4 seconds of inactivity (e.g., no swiping/page change)
    LaunchedEffect(showUi, pagerState.isScrollInProgress, pagerState.currentPage) {
        if (showUi) {
            kotlinx.coroutines.delay(4000)
            showUi = false
        }
    }

    var currentScale by remember { mutableStateOf(1f) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var pendingDeleteItem by remember { mutableStateOf<GalleryItem?>(null) }
    var pendingDeletePage by remember { mutableStateOf<Int?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.removeMediaOptimistically(pendingDeleteItem?.uri?.toString() ?: return@rememberLauncherForActivityResult)
        }
        pendingDeletePage = null
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.removeMediaOptimistically(pendingDeleteItem?.uri?.toString() ?: return@rememberLauncherForActivityResult)
        }
        pendingDeletePage = null
    }

    // Auto-navigate after delete: if list becomes empty, go back
    LaunchedEffect(galleryItems.size) {
        if (galleryItems.isEmpty() && pendingDeleteItem != null) {
            onBack()
        }
    }

    var showRenameDialog by remember { mutableStateOf(false) }
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

    var showInfoCard by remember { mutableStateOf(false) }
    var currentExif by remember { mutableStateOf<DetailedExifData?>(null) }
    

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
            .background(MaterialTheme.colorScheme.background.copy(alpha = bgAlpha)),
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
                        .blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .alpha(0.4f * bgAlpha)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f * bgAlpha))
                )
            }
        }



        HorizontalPager(
            state = pagerState,
            userScrollEnabled = currentScale <= 1.05f && dismissOffsetY.value == 0f,
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
                                    onBack()
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
            key = { page -> galleryItems.getOrNull(page)?.uri?.toString() ?: page.toString() }
        ) { page ->
            val item = galleryItems.getOrNull(page) ?: return@HorizontalPager
            val resolvedUri by produceState(initialValue = item.uri, key1 = item) {
                value = item.uri
            }

            val request: coil3.request.ImageRequest = remember(resolvedUri) {
                ImageRequest.Builder(context)
                    .data(resolvedUri)
                    .size(coil3.size.Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .placeholderMemoryCacheKey("photo_${item.resolvedUri}_384")
                    .crossfade(true)
                    .build()
            }
            

            
            var imageAspectRatio by remember(item) { mutableStateOf(1f) }
            val scale = remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
            val offsetX = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
            val offsetY = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
            val animJob = remember { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }

            androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage != page) {
                    scale.floatValue = 1f
                    offsetX.floatValue = 0f
                    offsetY.floatValue = 0f
                } else {
                    currentScale = scale.floatValue
                    viewModel.recordMediaView(item.id)
                }
            }
            
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val absoluteOffset = kotlin.math.abs(pageOffset)
            val pagerScale = 1f - (absoluteOffset.coerceIn(0f, 1f) * 0.05f)
            val pagerAlpha = 1f - (absoluteOffset.coerceIn(0f, 1f) * 0.5f)

            androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val parentWidth = constraints.maxWidth.toFloat()
                val parentHeight = constraints.maxHeight.toFloat()
                if (item.isVideo) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        VideoPlayerItem(
                            uri = resolvedUri,
                            isCurrentPage = page == pagerState.currentPage,
                            showControls = showUi,
                            modifier = Modifier.fillMaxSize(),
                            onTap = {
                                if (showInfoCard || showUi) {
                                    showInfoCard = false
                                    showUi = false
                                } else {
                                    showUi = true
                                }
                            }
                        )
                    }
                } else {
                    androidx.compose.runtime.LaunchedEffect(activeHighlight, page, pagerState.currentPage, resolvedUri) {
                        if (page == pagerState.currentPage) {
                            if (activeHighlight != null) {
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
                                            for (block in visionText.textBlocks) {
                                                for (line in block.lines) {
                                                    for (element in line.elements) {
                                                        if (element.text.contains(activeHighlight!!, ignoreCase = true)) {
                                                            element.boundingBox?.let { rects.add(it) }
                                                        }
                                                    }
                                                }
                                            }
                                            highlightRects = rects
                                            highlightImageSize = androidx.compose.ui.geometry.Size(inputImage.width.toFloat(), inputImage.height.toFloat())
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("DetailScreen", "OCR highlight failed", e)
                                    }
                                }
                                // Auto dismiss
                                highlightOverlayVisible = true
                                kotlinx.coroutines.delay(4000)
                                highlightOverlayVisible = false
                            }
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
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                                    clipInOverlayDuringTransition = OverlayClip(ShapeNone),
                                    boundsTransform = { _, _ -> MotionTokens.sharedElementSpring() }
                                )
                                // IMPORTANT: pointerInput MUST be before graphicsLayer.
                                // When pointerInput is placed after graphicsLayer, Compose
                                // inverse-transforms all pointer coordinates through the layer's
                                // scale matrix (divides by scaleX/Y). At 5x zoom that makes
                                // calculatePan() return 1/5 of the actual finger delta, causing
                                // pan to be 5x slower than the finger. With pointerInput first
                                // (outer), events arrive in screen/layout space — 1:1 with finger.
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            activeHighlight = null
                                            activeFaceClusterId = null
                                            highlightOverlayVisible = false
                                            if (showInfoCard || showUi) {
                                                showInfoCard = false
                                                showUi = false
                                            } else {
                                                showUi = true
                                            }
                                        },
                                        onDoubleTap = { centroid ->
                                            animJob.value?.cancel()
                                            animJob.value = coroutineScope.launch {
                                                val composableW = size.width.toFloat()
                                                val composableH = size.height.toFloat()
                                                if (scale.floatValue > 1f) {
                                                    // Zoom out to fit
                                                    currentScale = 1f
                                                    launch { androidx.compose.animation.core.animate(scale.floatValue, 1f, animationSpec = MotionTokens.gentleSpring()) { v, _ -> scale.floatValue = v } }
                                                    launch { androidx.compose.animation.core.animate(offsetX.floatValue, 0f, animationSpec = MotionTokens.gentleSpring()) { v, _ -> offsetX.floatValue = v } }
                                                    launch { androidx.compose.animation.core.animate(offsetY.floatValue, 0f, animationSpec = MotionTokens.gentleSpring()) { v, _ -> offsetY.floatValue = v } }
                                                } else {
                                                    // Zoom in to tapped point
                                                    val targetScale = 3.5f
                                                    currentScale = targetScale
                                                    val targetX = -(centroid.x - composableW / 2f) * (targetScale - 1)
                                                    val targetY = -(centroid.y - composableH / 2f) * (targetScale - 1)
                                                    val maxX = (composableW * (targetScale - 1)) / 2f
                                                    val maxY = (composableH * (targetScale - 1)) / 2f
                                                    launch { androidx.compose.animation.core.animate(scale.floatValue, targetScale, animationSpec = MotionTokens.gentleSpring()) { v, _ -> scale.floatValue = v } }
                                                    launch { androidx.compose.animation.core.animate(offsetX.floatValue, targetX.coerceIn(-maxX, maxX), animationSpec = MotionTokens.gentleSpring()) { v, _ -> offsetX.floatValue = v } }
                                                    launch { androidx.compose.animation.core.animate(offsetY.floatValue, targetY.coerceIn(-maxY, maxY), animationSpec = MotionTokens.gentleSpring()) { v, _ -> offsetY.floatValue = v } }
                                                }
                                            }
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    // Unified gesture handler: pinch-zoom, pan
                                    awaitEachGesture {
                                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                                        val downTime = System.currentTimeMillis()
                                        val downPosition = firstDown.position
                                        
                                        var shouldStartZoomPan = scale.floatValue > 1f
                                        if (!shouldStartZoomPan) {
                                            // Wait to see if a second finger is placed down (pinch-to-zoom)
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.changes.any { it.isConsumed }) break
                                                if (event.changes.size > 1) {
                                                    shouldStartZoomPan = true
                                                    break
                                                }
                                                if (!event.changes.any { it.pressed }) break
                                            }
                                        }
                                        
                                        if (shouldStartZoomPan) {
                                            var accumulatedZoom = 1f
                                            var accumulatedPan = Offset.Zero
                                            var pastTouchSlop = false
                                            val touchSlop = viewConfiguration.touchSlop
                                            val composableW = size.width.toFloat()
                                            val composableH = size.height.toFloat()
                                            val velocityTracker = VelocityTracker()
                                            var maxPointerCount = 0

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.changes.any { it.isConsumed }) break

                                                val pressedCount = event.changes.count { it.pressed }
                                                maxPointerCount = maxOf(maxPointerCount, pressedCount)

                                                if (pressedCount == 1) {
                                                    event.changes.firstOrNull { it.pressed }
                                                        ?.let { velocityTracker.addPointerInputChange(it) }
                                                }

                                                val zoomChange = event.calculateZoom()
                                                val panChange = event.calculatePan()

                                                if (!pastTouchSlop) {
                                                    accumulatedZoom *= zoomChange
                                                    accumulatedPan += panChange
                                                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                                    val zoomMotion = kotlin.math.abs(1 - accumulatedZoom) * centroidSize
                                                    val panMotion = accumulatedPan.getDistance()
                                                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                                        pastTouchSlop = true
                                                    }
                                                }

                                                if (pastTouchSlop && scale.floatValue <= 1.05f && maxPointerCount == 1) {
                                                    break
                                                }

                                                if (pastTouchSlop && (zoomChange != 1f || panChange != Offset.Zero)) {
                                                    val centroid = event.calculateCentroid(useCurrent = false)
                                                    animJob.value?.cancel()
                                                    val newScale = (scale.floatValue * zoomChange).coerceIn(1f, 20f)

                                                    if (newScale > 1.02f) showUi = false
                                                    if (newScale > 1.05f && currentScale <= 1.05f) {
                                                        currentScale = newScale
                                                    } else if (newScale <= 1.05f && currentScale > 1.05f) {
                                                        currentScale = newScale
                                                    }

                                                    val maxX = (composableW * (newScale - 1)) / 2f
                                                    val maxY = (composableH * (newScale - 1)) / 2f

                                                    val focalX = centroid.x - composableW / 2f
                                                    val focalY = centroid.y - composableH / 2f
                                                    val effectiveZoom = newScale / scale.floatValue
                                                    var newOffsetX = (offsetX.floatValue - focalX) * effectiveZoom + focalX + panChange.x
                                                    var newOffsetY = (offsetY.floatValue - focalY) * effectiveZoom + focalY + panChange.y

                                                    newOffsetX = newOffsetX.coerceIn(-maxX, maxX)
                                                    newOffsetY = newOffsetY.coerceIn(-maxY, maxY)

                                                    scale.floatValue = newScale
                                                    offsetX.floatValue = newOffsetX
                                                    offsetY.floatValue = newOffsetY

                                                    if (newScale > 1f) {
                                                        val hittingLeft = newOffsetX >= maxX && panChange.x > 0
                                                        val hittingRight = newOffsetX <= -maxX && panChange.x < 0
                                                        if (!hittingLeft && !hittingRight) {
                                                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                        }
                                                    }
                                                }

                                                if (!event.changes.any { it.pressed }) break
                                            }

                                            if (pastTouchSlop && scale.floatValue > 1.05f && maxPointerCount == 1) {
                                                val velocity = velocityTracker.calculateVelocity()
                                                val currentMaxX = (composableW * (scale.floatValue - 1)) / 2f
                                                val currentMaxY = (composableH * (scale.floatValue - 1)) / 2f
                                                val flingFactor = 0.4f
                                                val targetFlingX = (offsetX.floatValue + velocity.x * flingFactor)
                                                    .coerceIn(-currentMaxX, currentMaxX)
                                                val targetFlingY = (offsetY.floatValue + velocity.y * flingFactor)
                                                    .coerceIn(-currentMaxY, currentMaxY)
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
                                            }
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = scale.floatValue * pagerScale
                                    scaleY = scale.floatValue * pagerScale
                                    alpha = pagerAlpha
                                    translationX = offsetX.floatValue
                                    translationY = offsetY.floatValue
                                }
                        )
                        
                        if (activeHighlight != null || activeFaceClusterId != null) {
                            val highlightColor = MaterialTheme.colorScheme.primary
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
                                val iSize = highlightImageSize
                                val hasRects = iSize != null && highlightRects.isNotEmpty()
                                
                                val fitScale = if (hasRects) kotlin.math.min(size.width / iSize!!.width, size.height / iSize.height) else 1f
                                val dx = if (hasRects) (size.width - iSize!!.width * fitScale) / 2f else 0f
                                val dy = if (hasRects) (size.height - iSize!!.height * fitScale) / 2f else 0f
                                val padding = 6.dp.toPx()

                                if (highlightOverlayVisible) {
                                    if (hasRects) {
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
                                    } else {
                                        drawRect(Color.Black.copy(alpha = 0.6f))
                                    }
                                }
                                
                                if (hasRects) {
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
            }
        }
        }

        // ── Photo counter pill (e.g. "23 / 487") ──────────────────────────────
        // Appears on page change, auto-hides after 2 seconds.
        var showCounter by remember { mutableStateOf(true) }
        LaunchedEffect(pagerState.currentPage) {
            showCounter = true
            kotlinx.coroutines.delay(2000)
            showCounter = false
        }
        AnimatedVisibility(
            visible = showCounter && galleryItems.size > 1,
            enter = fadeIn(MotionTokens.gentleSpring()) +
                    scaleIn(MotionTokens.bouncySpring(), initialScale = 0.8f),
            exit  = fadeOut(MotionTokens.gentleSpring()) +
                    scaleOut(MotionTokens.gentleSpring(), targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${galleryItems.size}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }



        // UI Overlay — Top bar: back button (left) + info button (right)
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(MotionTokens.gentleSpring()) +
                    slideInVertically(MotionTokens.gentleSpring()) { -it },
            exit  = fadeOut(MotionTokens.gentleSpring()) +
                    slideOutVertically(MotionTokens.gentleSpring()) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveFilledIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                        contentDescription = "Go back"
                    )
                }
                ExpressiveFilledIconButton(
                    onClick = { showInfoCard = !showInfoCard },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (showInfoCard) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_info),
                        contentDescription = "Info"
                    )
                }
                }
            }

        ExifDetailsSheet(
            isOpen = showInfoCard,
            onDismiss = { showInfoCard = false },
            galleryItem = galleryItems.getOrNull(pagerState.currentPage),
            exifData = currentExif
        )

        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(MotionTokens.gentleSpring()) +
                    slideInVertically(MotionTokens.gentleSpring()) { it },
            exit  = fadeOut(MotionTokens.gentleSpring()) +
                    slideOutVertically(MotionTokens.gentleSpring()) { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var showMoreMenu by remember { mutableStateOf(false) }
                val currentItem = galleryItems.getOrNull(pagerState.currentPage)

                HorizontalFloatingToolbar(
                    expanded = true,
                    colors = androidx.compose.material3.FloatingToolbarDefaults.standardFloatingToolbarColors(
                        toolbarContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        toolbarContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (bucketName == "Trash") {
                            IconButton(
                                onClick = {
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
                            ) { 
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_refresh), contentDescription = "Restore") 
                            }
                            IconButton(
                                onClick = {
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
                            ) { 
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_delete), contentDescription = "Permanently Delete", tint = MaterialTheme.colorScheme.error) 
                            }

                        } else {
                            IconButton(
                                onClick = {
                                    val item = currentItem
                                    if (item != null) {
                                        coroutineScope.launch {
                                            val strip = viewModel.settingsRepository.stripMetadataOnShareFlow.first()
                                            com.inferno.gallery.ui.utils.ShareUtils.shareMedia(context, listOf(item.uri), strip)
                                        }
                                    }
                                },
                                enabled = currentItem?.localExists == true
                            ) { 
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_share), contentDescription = "Share") 
                            }
                            val isFavorite = currentItem?.id?.let { favoriteIds.contains(it) } ?: false
                            // Heart pop: triggers a scale bounce when toggling to favorite
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
                            val heartView = androidx.compose.ui.platform.LocalView.current
                            IconButton(
                                onClick = {
                                    if (currentItem != null) {
                                        heartBounce = true
                                        heartView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                        viewModel.toggleFavorite(currentItem.id)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) ImageVector.vectorResource(R.drawable.ic_ms_favorite) else ImageVector.vectorResource(R.drawable.ic_ms_favorite),
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) com.inferno.gallery.ui.theme.LocalHarmonizedColors.current.error else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = heartScale
                                        scaleY = heartScale
                                    }
                                )
                            }
                            IconButton(
                                enabled = currentItem?.localExists == true,
                                onClick = {
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
                            ) { 
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_edit), contentDescription = "Edit") 
                            }
                            IconButton(
                                onClick = {
                                    if (currentItem != null) {
                                            val isMediaStoreUri = currentItem.uri.toString().startsWith("content://")
                                            if (!isMediaStoreUri) {
                                                android.widget.Toast.makeText(context, "Cannot delete this item", android.widget.Toast.LENGTH_SHORT).show()
                                                return@IconButton
                                            }
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
                            ) { 
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_delete), contentDescription = "Delete") 
                            }
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_more_vert), contentDescription = "More")
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    shape = MaterialTheme.shapes.large,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
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
                                    if (currentItem != null && !currentItem.isVideo) {
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text("Set as Wallpaper") },
                                            leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_image), contentDescription = null) },
                                            enabled = currentItem.localExists,
                                            onClick = {
                                                showMoreMenu = false
                                                val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                                                    setDataAndType(resolvedCurrentUri ?: currentItem.uri, "image/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    putExtra("mimeType", "image/*")
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Set as..."))
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
                                                            onBack()
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
