package com.inferno.gallery.ui

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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.text.font.FontFamily
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Wallpaper

import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import android.os.Build
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.SettingsRepository



@OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailScreen(
    mediaId: String,
    bucketName: String?,
    highlightText: String? = null,
    useFullScreenGlobal: Boolean = false,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryViewModel = viewModel(),
    onBack: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(bucketName) {
        viewModel.setBucket(bucketName)
    }

    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val window = activity?.window
    val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
    
    val galleryItems by viewModel.images.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()


    // Ensure we don't crash if items is empty
    if (galleryItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pagerState = rememberPagerState(
        pageCount = { galleryItems.size }
    )

    val currentItem = galleryItems.getOrNull(pagerState.currentPage)
    val resolvedCurrentUri by produceState<Uri?>(initialValue = currentItem?.uri, key1 = currentItem) {
        val uri = currentItem?.uri
        if (uri != null) {
            value = uri
            val isReadable = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { true } ?: false
                } catch (e: Exception) {
                    false
                }
            }
            if (!isReadable) {
                val mediaId = currentItem.id.toLongOrNull()
                if (mediaId != null) {
                    val db = DatabaseProvider.getDatabase(context)
                    val backup = withContext(Dispatchers.IO) {
                        db.telegramBackupDao().getBackupForMedia(mediaId)
                    }
                    if (backup != null && backup.backupStatus == "SUCCESS") {
                        val fileId = backup.telegramFileId
                        if (fileId != null) {
                            val settingsRepo = SettingsRepository(context)
                            val token = withContext(Dispatchers.IO) {
                                try { settingsRepo.telegramBotTokenFlow.first() } catch (e: Exception) { "" }
                            }
                            val chatId = withContext(Dispatchers.IO) {
                                try { settingsRepo.telegramChatIdFlow.first() } catch (e: Exception) { "" }
                            }
                            if (token.isNotEmpty() && chatId.isNotEmpty()) {
                                val resolved = withContext(Dispatchers.IO) {
                                    try {
                                        com.inferno.gallery.data.network.TelegramClient(token, chatId).getFileUrl(fileId)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (resolved != null) {
                                    value = Uri.parse(resolved)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            value = null
        }
    }


    var activeHighlight by remember { mutableStateOf(highlightText) }
    var highlightRects by remember { mutableStateOf<List<android.graphics.Rect>>(emptyList()) }
    var highlightImageSize by remember { mutableStateOf<androidx.compose.ui.geometry.Size?>(null) }

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

    var showUi by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    var isUserScrollEnabled by remember { mutableStateOf(true) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCloudDeleteConfirmDialog by remember { mutableStateOf(false) }
    var pendingDeleteItem by remember { mutableStateOf<GalleryItem?>(null) }
    var pendingDeletePage by remember { mutableStateOf<Int?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val deletedPage = pendingDeletePage
            val currentList = galleryItems
            
            viewModel.removeMediaOptimistically(pendingDeleteItem?.uri?.toString() ?: return@rememberLauncherForActivityResult)
            
            // Navigate to next item if available, otherwise previous
            if (deletedPage != null && currentList.isNotEmpty()) {
                val newTargetIndex = when {
                    deletedPage < currentList.size - 1 -> deletedPage // Next item takes this position
                    deletedPage > 0 -> deletedPage - 1 // Previous item if at end
                    else -> 0
                }
                if (newTargetIndex < galleryItems.size) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(newTargetIndex)
                    }
                }
            }
            // Don't call onBack() - let user stay in viewer
        }
        pendingDeletePage = null
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
    
    if (showCloudDeleteConfirmDialog && currentItem != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCloudDeleteConfirmDialog = false },
            title = { androidx.compose.material3.Text("Delete Cloud Backup") },
            text = { androidx.compose.material3.Text("This will delete the backed-up file from Telegram and remove it from your Cloud tab. The original photo will remain on your device.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showCloudDeleteConfirmDialog = false
                        viewModel.deleteCloudBackup(currentItem.id.toLong())
                        android.widget.Toast.makeText(context, "Cloud backup deleted", android.widget.Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                ) {
                    androidx.compose.material3.Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showCloudDeleteConfirmDialog = false }
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
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

    var showInfoCard by remember { mutableStateOf(false) }
    var currentExif by remember { mutableStateOf<ExifData?>(null) }

    androidx.compose.runtime.LaunchedEffect(resolvedCurrentUri, showInfoCard) {
        if (showInfoCard) {
            val uri = resolvedCurrentUri
            if (uri != null) {
                currentExif = extractExif(context, uri)
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = isUserScrollEnabled,
            modifier = Modifier.fillMaxSize(),
            key = { page -> galleryItems.getOrNull(page)?.uri?.toString() ?: page.toString() }
        ) { page ->
            val item = galleryItems.getOrNull(page) ?: return@HorizontalPager
            val resolvedUri by produceState<Uri>(initialValue = item.uri, key1 = item.uri) {
                val isReadable = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(item.uri)?.use { true } ?: false
                    } catch (e: Exception) {
                        false
                    }
                }
                if (!isReadable) {
                    val mediaId = item.id.toLongOrNull()
                    if (mediaId != null) {
                        val db = DatabaseProvider.getDatabase(context)
                        val backup = withContext(Dispatchers.IO) {
                            db.telegramBackupDao().getBackupForMedia(mediaId)
                        }
                        if (backup != null && backup.backupStatus == "SUCCESS") {
                            val fileId = backup.telegramFileId
                            if (fileId != null) {
                                val settingsRepo = SettingsRepository(context)
                                val token = withContext(Dispatchers.IO) {
                                    try { settingsRepo.telegramBotTokenFlow.first() } catch (e: Exception) { "" }
                                }
                                val chatId = withContext(Dispatchers.IO) {
                                    try { settingsRepo.telegramChatIdFlow.first() } catch (e: Exception) { "" }
                                }
                                if (token.isNotEmpty() && chatId.isNotEmpty()) {
                                    val resolved = withContext(Dispatchers.IO) {
                                        try {
                                            com.inferno.gallery.data.network.TelegramClient(token, chatId).getFileUrl(fileId)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (resolved != null) {
                                        value = Uri.parse(resolved)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val request: coil3.request.ImageRequest = remember(resolvedUri) {
                ImageRequest.Builder(context)
                    .data(resolvedUri)
                    .size(coil3.size.Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            

            
            val scale = remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
            val offsetX = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
            val offsetY = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
            val animJob = remember { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }
            
            // Removed LaunchedEffect for isUserScrollEnabled to prevent race condition
            
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val absoluteOffset = kotlin.math.abs(pageOffset)
            val pagerScale = 1f - (absoluteOffset.coerceIn(0f, 1f) * 0.05f)
            val pagerAlpha = 1f - (absoluteOffset.coerceIn(0f, 1f) * 0.5f)

            Box(modifier = Modifier.fillMaxSize()) {
                if (item.isVideo) {
                    VideoPlayerItem(
                        uri = resolvedUri,
                        isCurrentPage = page == pagerState.currentPage,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    androidx.compose.runtime.LaunchedEffect(activeHighlight, page, pagerState.currentPage, resolvedUri) {
                        if (activeHighlight != null && page == pagerState.currentPage) {
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
                            kotlinx.coroutines.delay(4000)
                            activeHighlight = null
                        }
                    }

                    with(sharedTransitionScope) {
                        AsyncImage(
                            model = request,
                            contentDescription = "Full-screen photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "photo_${item.uri}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ ->
                                        spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    }
                                )
                                // IMPORTANT: pointerInput MUST be before graphicsLayer.
                                // When pointerInput is placed after graphicsLayer, Compose
                                // inverse-transforms all pointer coordinates through the layer's
                                // scale matrix (divides by scaleX/Y). At 5x zoom that makes
                                // calculatePan() return 1/5 of the actual finger delta, causing
                                // pan to be 5x slower than the finger. With pointerInput first
                                // (outer), events arrive in screen/layout space — 1:1 with finger.
                                .pointerInput(Unit) {
                                    // Unified gesture handler: tap, double-tap, pinch-zoom, pan
                                    // Uses a single awaitEachGesture loop to eliminate gesture conflicts
                                    // caused by two stacked pointerInput blocks competing for events.
                                    var lastTapTime = 0L
                                    var lastTapPosition = Offset.Zero
                                    awaitEachGesture {
                                        // requireUnconsumed=false: prevent missing gestures if a parent
                                        // or sibling already consumed the DOWN event.
                                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                                        val downTime = System.currentTimeMillis()
                                        val downPosition = firstDown.position

                                        val isDoubleTap = (downTime - lastTapTime) < 300L &&
                                            (downPosition - lastTapPosition).getDistance() < 100f

                                        var accumulatedZoom = 1f
                                        var accumulatedPan = Offset.Zero
                                        var pastTouchSlop = false
                                        val touchSlop = viewConfiguration.touchSlop
                                        val composableW = size.width.toFloat()
                                        val composableH = size.height.toFloat()
                                        // VelocityTracker records pointer positions each frame so we can
                                        // compute a fling velocity when the finger lifts.
                                        val velocityTracker = VelocityTracker()

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.changes.any { it.isConsumed }) break

                                            // Feed every pointer change into the tracker for accurate velocity.
                                            event.changes.forEach { velocityTracker.addPointerInputChange(it) }

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
                                                    lastTapTime = 0L
                                                }
                                            }

                                            if (pastTouchSlop && (zoomChange != 1f || panChange != Offset.Zero)) {
                                                val centroid = event.calculateCentroid(useCurrent = false)
                                                animJob.value?.cancel()
                                                val newScale = (scale.floatValue * zoomChange).coerceIn(1f, 20f)

                                                if (newScale > 1.02f) showUi = false
                                                if (newScale > 1.05f && isUserScrollEnabled) {
                                                    isUserScrollEnabled = false
                                                } else if (newScale <= 1.05f && !isUserScrollEnabled) {
                                                    isUserScrollEnabled = true
                                                }

                                                val maxX = (composableW * (newScale - 1)) / 2f
                                                val maxY = (composableH * (newScale - 1)) / 2f

                                                // BUG FIX: Unified zoom+pan formula.
                                                // Old code: applied pan first, then wrapped the entire
                                                // (offset+pan) inside the focal zoom — this multiplied
                                                // panChange by zoomDelta every frame, amplifying it and
                                                // causing the image to fly to corners at deep zoom.
                                                //
                                                // Correct formula: (offset − focal) × zoomDelta + focal + pan
                                                // → zoom around focal point, THEN translate by pan.
                                                // For pure pan (zoomDelta=1) this reduces to offset + pan. ✓
                                                val focalX = centroid.x - composableW / 2f
                                                val focalY = centroid.y - composableH / 2f
                                                var newOffsetX = (offsetX.floatValue - focalX) * zoomChange + focalX + panChange.x
                                                var newOffsetY = (offsetY.floatValue - focalY) * zoomChange + focalY + panChange.y

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

                                        // Fling: if the user was panning a zoomed image, use the
                                        // measured velocity to animate a momentum scroll on release.
                                        if (pastTouchSlop && scale.floatValue > 1f) {
                                            val velocity = velocityTracker.calculateVelocity()
                                            val currentMaxX = (composableW * (scale.floatValue - 1)) / 2f
                                            val currentMaxY = (composableH * (scale.floatValue - 1)) / 2f
                                            // Project where the image would coast to with natural deceleration.
                                            val flingFactor = 0.18f
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

                                        // --- Tap / double-tap detection (fires after gesture ends) ---
                                        if (!pastTouchSlop) {
                                            if (isDoubleTap) {
                                                lastTapTime = 0L
                                                animJob.value?.cancel()
                                                animJob.value = coroutineScope.launch {
                                                    if (scale.floatValue > 1f) {
                                                        // Zoom out to fit
                                                        isUserScrollEnabled = true
                                                        launch { androidx.compose.animation.core.animate(scale.floatValue, 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) { v, _ -> scale.floatValue = v } }
                                                        launch { androidx.compose.animation.core.animate(offsetX.floatValue, 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) { v, _ -> offsetX.floatValue = v } }
                                                        launch { androidx.compose.animation.core.animate(offsetY.floatValue, 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) { v, _ -> offsetY.floatValue = v } }
                                                    } else {
                                                        // Zoom in to tapped point
                                                        isUserScrollEnabled = false
                                                        val targetScale = 3.5f
                                                        val targetX = -(downPosition.x - composableW / 2f) * (targetScale - 1)
                                                        val targetY = -(downPosition.y - composableH / 2f) * (targetScale - 1)
                                                        val maxX = (composableW * (targetScale - 1)) / 2f
                                                        val maxY = (composableH * (targetScale - 1)) / 2f
                                                        launch { androidx.compose.animation.core.animate(scale.floatValue, targetScale, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) { v, _ -> scale.floatValue = v } }
                                                        launch { androidx.compose.animation.core.animate(offsetX.floatValue, targetX.coerceIn(-maxX, maxX), animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) { v, _ -> offsetX.floatValue = v } }
                                                        launch { androidx.compose.animation.core.animate(offsetY.floatValue, targetY.coerceIn(-maxY, maxY), animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) { v, _ -> offsetY.floatValue = v } }
                                                    }
                                                }
                                            } else {
                                                // Single tap — use a short delay so a rapid second tap
                                                // is detected as a double-tap before the UI toggle fires.
                                                lastTapTime = downTime
                                                lastTapPosition = downPosition
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(300L)
                                                    if (lastTapTime == downTime) {
                                                        activeHighlight = null
                                                        if (showInfoCard || showUi) {
                                                            showInfoCard = false
                                                            showUi = false
                                                        } else {
                                                            showUi = true
                                                        }
                                                        lastTapTime = 0L
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
                        
                        if (activeHighlight != null) {
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
                                drawRect(Color.Black.copy(alpha = 0.5f))
                                val iSize = highlightImageSize
                                if (iSize != null && highlightRects.isNotEmpty()) {
                                    val fitScale = kotlin.math.min(size.width / iSize.width, size.height / iSize.height)
                                    val dWidth = iSize.width * fitScale
                                    val dHeight = iSize.height * fitScale
                                    val dx = (size.width - dWidth) / 2f
                                    val dy = (size.height - dHeight) / 2f
                                    
                                    for (rect in highlightRects) {
                                        val rLeft = dx + rect.left * fitScale
                                        val rTop = dy + rect.top * fitScale
                                        val rWidth = (rect.right - rect.left) * fitScale
                                        val rHeight = (rect.bottom - rect.top) * fitScale
                                        
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.3f),
                                            topLeft = Offset(rLeft, rTop),
                                            size = androidx.compose.ui.geometry.Size(rWidth, rHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                                        )
                                        drawRoundRect(
                                            color = Color.White,
                                            topLeft = Offset(rLeft, rTop),
                                            size = androidx.compose.ui.geometry.Size(rWidth, rHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
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
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp, 150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolvedUri)
                            .size(300, 450)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Minimap map",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Draw the Viewport Indicator Map
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val viewportWidth = size.width / scale.floatValue
                        val viewportHeight = size.height / scale.floatValue
                        val maxOffsetX = (screenWidth * (scale.floatValue - 1)) / 2
                        val maxOffsetY = (screenHeight * (scale.floatValue - 1)) / 2
                        val pctX = if (maxOffsetX > 0) -offsetX.floatValue / maxOffsetX else 0f
                        val pctY = if (maxOffsetY > 0) -offsetY.floatValue / maxOffsetY else 0f
                        val rectX = (size.width - viewportWidth) / 2 + (pctX * (size.width - viewportWidth) / 2)
                        val rectY = (size.height - viewportHeight) / 2 + (pctY * (size.height - viewportHeight) / 2)
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.White,
                            topLeft = Offset(rectX, rectY),
                            size = androidx.compose.ui.geometry.Size(viewportWidth, viewportHeight),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }
        }
        }

        // UI Overlay
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                FilledIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Go back"
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showInfoCard,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 80.dp, end = 16.dp)
        ) {
            ExifInfoCard(
                galleryItem = galleryItems.getOrNull(pagerState.currentPage),
                exifData = currentExif
            )
        }

        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var showMoreMenu by remember { mutableStateOf(false) }
                val currentItem = galleryItems.getOrNull(pagerState.currentPage)
                
                HorizontalFloatingToolbar(
                    expanded = true,
                    colors = androidx.compose.material3.FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    modifier = Modifier.padding(bottom = 16.dp).height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showShareSheet = true }) { 
                            Icon(Icons.Outlined.Share, contentDescription = "Share") 
                        }
                        IconButton(
                            onClick = {
                                if (currentItem != null) {
                                    val editIntent = Intent(Intent.ACTION_EDIT).apply {
                                        setDataAndType(resolvedCurrentUri ?: currentItem.uri, if (currentItem.isVideo) "video/*" else "image/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(editIntent, "Edit Media"))
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "No editor available on device", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) { 
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit") 
                        }
                        IconButton(
                            onClick = {
                                if (currentItem != null) {
                                    if (bucketName == "telegram_cloud") {
                                        showCloudDeleteConfirmDialog = true
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        val isMediaStoreUri = currentItem.uri.toString().startsWith("content://")
                                        if (!isMediaStoreUri) {
                                            android.widget.Toast.makeText(context, "Cannot delete this item", android.widget.Toast.LENGTH_SHORT).show()
                                            return@IconButton
                                        }
                                        pendingDeleteItem = currentItem
                                        pendingDeletePage = pagerState.currentPage
                                        showDeleteConfirmDialog = true
                                    }
                                }
                            }
                        ) { 
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete") 
                        }
                        
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                            }
                            val isFavorite = currentItem?.id?.let { favoriteIds.contains(it) } ?: false
                            androidx.compose.material3.DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                if (currentItem != null) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites") },
                                        leadingIcon = {
                                            if (isFavorite) {
                                                Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                            } else {
                                                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null)
                                            }
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            currentItem.let { viewModel.toggleFavorite(it.id) }
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Info") },
                                        leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            showInfoCard = !showInfoCard
                                        }
                                    )
                                }
                                if (currentItem != null && !currentItem.isVideo) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Set as Wallpaper") },
                                        leadingIcon = { Icon(Icons.Filled.Wallpaper, contentDescription = null) },
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
                            }
                        }
                    }
                }
        }
        }

        if (showShareSheet) {
            CustomShareSheet(
                items = galleryItems,
                initialIndex = pagerState.currentPage,
                onDismiss = { showShareSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomShareSheet(items: List<GalleryItem>, initialIndex: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val selectedUris = remember { mutableStateListOf(items.getOrNull(initialIndex)?.uri).apply { removeAll { it == null } } }
    
    // Query apps that handle image sharing
    val shareTargets = remember {
        val intent = Intent(Intent.ACTION_SEND).setType("image/*")
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            // Header
            Text("${selectedUris.size} selected", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
            
            // Image Multi-Select Carousel
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { item ->
                    val isSelected = selectedUris.contains(item.uri)
                    Box(modifier = Modifier.size(140.dp, 180.dp).clickable { if (isSelected) selectedUris.remove(item.uri) else item.uri?.let { selectedUris.add(it) } }) {
                        AsyncImage(model = item.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(4.dp, MaterialTheme.colorScheme.primary)
                            )
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.padding(4.dp).size(20.dp))
                            }
                        } else {
                            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(28.dp).border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val sortedShareTargets = remember(shareTargets) {
                val priorityApps = listOf("com.instagram.android", "com.whatsapp", "com.facebook.katana", "com.google.android.gm")
                shareTargets.sortedByDescending { target ->
                    priorityApps.contains(target.activityInfo.packageName)
                }
            }
            
            // App Targets Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 240.dp) // Limits height to 2 rows to allow for a 'peek' effect
            ) {
                items(sortedShareTargets) { target ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                        if (selectedUris.isEmpty()) return@clickable
                        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "image/*"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(selectedUris))
                            setClassName(target.activityInfo.packageName, target.activityInfo.name)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                        onDismiss()
                    }) {
                        // Coil handles native Android Drawables automatically
                        AsyncImage(model = target.loadIcon(pm), contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = target.loadLabel(pm).toString(), style = MaterialTheme.typography.labelMedium, maxLines = 1)
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
        modifier = modifier.width(280.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            InfoRow(label = "Name", value = galleryItem.name)
            InfoRow(label = "Time", value = formatExifDate(galleryItem.dateModified * 1000L))
            InfoRow(label = "Size", value = "${formatSizeToMB(galleryItem.size)}   ${exifData?.resolution ?: ""}".trim())
            
            val device = exifData?.model ?: "Unknown Device"
            val paramsParts = listOfNotNull(exifData?.focalLength, exifData?.shutterSpeed, exifData?.iso, exifData?.aperture).filter { it.isNotBlank() }
            val builtParamsString = if (paramsParts.isNotEmpty()) paramsParts.joinToString(" • ") else "Unknown Parameters"
            InfoColumn(label = "Parameters", deviceName = device, params = builtParamsString)
            
            InfoRow(label = "Path", value = galleryItem.path)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row { 
        Text(text = label, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.width(80.dp))
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) 
    }
}

@Composable
private fun InfoColumn(label: String, deviceName: String, params: String) {
    Row { 
        Text(text = label, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.width(80.dp))
        Column(modifier = Modifier.weight(1f)) { 
            Text(text = deviceName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(text = params, color = Color.LightGray, fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) 
        } 
    }
}
