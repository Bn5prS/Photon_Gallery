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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
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

suspend fun PointerInputScope.detectZoomPanGesture(
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, consume: () -> Unit) -> Unit
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = true)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = Math.abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    if (zoomChange != 1f || panChange != Offset.Zero) {
                        var shouldConsume = false
                        onGesture(centroid, panChange, zoomChange) {
                            shouldConsume = true
                        }
                        if (shouldConsume) {
                            event.changes.forEach {
                                if (it.positionChanged()) {
                                    it.consume()
                                }
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    mediaId: String,
    bucketName: String?,
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
    
    // Check if display supports HDR
    val displayManager = context.getSystemService(android.content.Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
    val isHdrSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val display = displayManager?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
        display?.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true || context.resources.configuration.isScreenHdr == true
    } else {
        false
    }
    
    val galleryItems by viewModel.images.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val hdrDisplayEnabled by viewModel.hdrDisplayEnabled.collectAsState()

    androidx.compose.runtime.DisposableEffect(hdrDisplayEnabled, window, isHdrSupported) {
        if (isHdrSupported && hdrDisplayEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            window?.colorMode = android.content.pm.ActivityInfo.COLOR_MODE_HDR
        } else {
            window?.colorMode = android.content.pm.ActivityInfo.COLOR_MODE_DEFAULT
        }
        onDispose {
            window?.colorMode = android.content.pm.ActivityInfo.COLOR_MODE_DEFAULT
        }
    }


    // Ensure we don't crash if items is empty
    if (galleryItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pagerState = rememberPagerState(
        pageCount = { galleryItems.size }
    )

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
    var showShareSheet by remember { mutableStateOf(false) }

    var isUserScrollEnabled by remember { mutableStateOf(true) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
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

    androidx.compose.runtime.LaunchedEffect(showUi) {
        if (window != null) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        if (insetsController != null) {
            if (showUi) {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    if (showDeleteConfirmDialog && pendingDeleteItem != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { androidx.compose.material3.Text("Move to Trash") },
            text = { androidx.compose.material3.Text("This item will be moved to trash") },
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
                    androidx.compose.material3.Text("Move to Trash")
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

    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage, showInfoCard) {
        if (showInfoCard) {
            val currentUri = galleryItems.getOrNull(pagerState.currentPage)?.uri
            if (currentUri != null) {
                currentExif = extractExif(context, currentUri)
            }
        }
    }

    val listState = rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        listState.animateScrollToItem(pagerState.currentPage)
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
            val request: coil3.request.ImageRequest = remember(item.uri) {
                ImageRequest.Builder(context)
                    .data(item.uri)
                    .size(coil3.size.Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            
            // HDR metadata hints for proper color rendering
            val isHdrImage: Boolean = remember(item.uri) {
                item.uri.toString().contains("hdr", ignoreCase = true) || 
                item.name.contains("hdr", ignoreCase = true)
            }
            
            val scale = remember { Animatable(1f) }
            val offsetX = remember { Animatable(0f) }
            val offsetY = remember { Animatable(0f) }
            
            // Removed LaunchedEffect for isUserScrollEnabled to prevent race condition
            
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val absoluteOffset = kotlin.math.abs(pageOffset)
            val pagerScale = 1f - (absoluteOffset.coerceIn(0f, 1f) * 0.05f)
            val pagerAlpha = 1f - (absoluteOffset.coerceIn(0f, 1f) * 0.5f)

            Box(modifier = Modifier.fillMaxSize()) {
                if (item.isVideo) {
                    VideoPlayerItem(
                        uri = item.uri,
                        isCurrentPage = page == pagerState.currentPage,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
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
                                .graphicsLayer {
                                    scaleX = scale.value * pagerScale
                                    scaleY = scale.value * pagerScale
                                    alpha = pagerAlpha
                                    translationX = offsetX.value
                                    translationY = offsetY.value
                                    
                                    // HDR color space aware rendering
                                    if (hdrDisplayEnabled && isHdrSupported && isHdrImage) {
                                        // Enable wide color gamut rendering
                                        this.renderEffect = null // Let system handle HDR color mapping
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectZoomPanGesture { centroid, pan, zoom, consume ->
                                        coroutineScope.launch {
                                            val newScale = (scale.value * zoom).coerceIn(1f, 20f)
                                            val scaleChange = newScale / scale.value
                                            
                                            if (newScale > 1.02f) { showUi = false }
                                            
                                            if (newScale > 1.05f && isUserScrollEnabled) {
                                                isUserScrollEnabled = false
                                            } else if (newScale <= 1.05f && !isUserScrollEnabled) {
                                                isUserScrollEnabled = true
                                            }
                                            
                                            val maxX = (screenWidth * (newScale - 1)) / 2
                                            val maxY = (screenHeight * (newScale - 1)) / 2
                                            
                                            val panSensitivity = when {
                                                scale.value < 2f -> 1.5f
                                                scale.value < 5f -> 2f
                                                else -> 2.5f
                                            }
                                            
                                            val effectivePanX = pan.x * panSensitivity
                                            val effectivePanY = pan.y * panSensitivity

                                            var newOffsetX = offsetX.value + effectivePanX
                                            var newOffsetY = offsetY.value + effectivePanY
                                            
                                            if (zoom != 1f) {
                                                val focalX = (centroid.x - screenWidth / 2)
                                                val focalY = (centroid.y - screenHeight / 2)
                                                newOffsetX += focalX * (1 - scaleChange)
                                                newOffsetY += focalY * (1 - scaleChange)
                                            }
                                            
                                            newOffsetX = newOffsetX.coerceIn(-maxX, maxX)
                                            newOffsetY = newOffsetY.coerceIn(-maxY, maxY)
                                            
                                            scale.snapTo(newScale)
                                            offsetX.snapTo(newOffsetX)
                                            offsetY.snapTo(newOffsetY)
                                        }
                                        // Crucial Gesture Priority: Only consume if zoomed in!
                                        if (scale.value > 1f) {
                                            consume()
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { 
                                            if (showInfoCard || showUi) {
                                                showInfoCard = false
                                                showUi = false
                                            } else {
                                                showUi = true
                                            }
                                        },
                                        onDoubleTap = { tapOffset ->
                                            coroutineScope.launch {
                                                if (scale.value > 1f) {
                                                    isUserScrollEnabled = true
                                                    launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) }
                                                    launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) }
                                                    launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) }
                                                } else {
                                                    isUserScrollEnabled = false
                                                    val targetScale = 3.5f
                                                    val targetX = -(tapOffset.x - screenWidth / 2) * (targetScale - 1)
                                                    val targetY = -(tapOffset.y - screenHeight / 2) * (targetScale - 1)
                                                    val maxX = (screenWidth * (targetScale - 1)) / 2
                                                    val maxY = (screenHeight * (targetScale - 1)) / 2
                                                    launch { scale.animateTo(targetScale, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) }
                                                    launch { offsetX.animateTo(targetX.coerceIn(-maxX, maxX), spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) }
                                                    launch { offsetY.animateTo(targetY.coerceIn(-maxY, maxY), spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)) }
                                                }
                                            }
                                        }
                                    )
                                }
                        )
                    }
                }

            // Minimap Overlay
            AnimatedVisibility(
                visible = scale.value >= 5f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                ) {
                    // Draw the base image (Coil will pull this from memory instantly)
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                    // Draw the Viewport Indicator Map
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val viewportWidth = size.width / scale.value
                        val viewportHeight = size.height / scale.value
                        val maxOffsetX = (screenWidth * (scale.value - 1)) / 2
                        val maxOffsetY = (screenHeight * (scale.value - 1)) / 2
                        val pctX = if (maxOffsetX > 0) -offsetX.value / maxOffsetX else 0f
                        val pctY = if (maxOffsetY > 0) -offsetY.value / maxOffsetY else 0f
                        val rectX = (size.width - viewportWidth) / 2 + (pctX * (size.width - viewportWidth) / 2)
                        val rectY = (size.height - viewportHeight) / 2 + (pctY * (size.height - viewportHeight) / 2)
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(rectX, rectY),
                            size = androidx.compose.ui.geometry.Size(viewportWidth, viewportHeight),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.2f),
                            topLeft = Offset(rectX, rectY),
                            size = androidx.compose.ui.geometry.Size(viewportWidth, viewportHeight)
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Go back"
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentItem = galleryItems.getOrNull(pagerState.currentPage)
                    val isFavorite = currentItem?.id?.let { favoriteIds.contains(it) } ?: false
                    
                    FilledIconButton(
                        onClick = { currentItem?.let { viewModel.toggleFavorite(it.id) } },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        if (isFavorite) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.error)
                        } else {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favorite", tint = Color.White)
                        }
                    }

                    FilledIconButton(
                        onClick = { showInfoCard = !showInfoCard },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Info"
                        )
                    }
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
                val snapFling = rememberSnapFlingBehavior(lazyListState = listState)
                val density = androidx.compose.ui.platform.LocalDensity.current
                val screenWidthDp = with(density) { screenWidth.toDp() }

                LazyRow(
                    state = listState,
                    flingBehavior = snapFling,
                    contentPadding = PaddingValues(horizontal = (screenWidthDp / 2) - 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    items(galleryItems.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(galleryItems[index].uri).size(150, 150).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(2.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                    .alpha(if (isSelected) 1f else 0.7f)
                            )
                        }
                    }
                }
                
                var expandedActionMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.padding(bottom = 16.dp)) {
                    CustomSplitButton(
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        leadingText = "Edit",
                        onLeadingClick = { /* TODO: Edit */ },
                        onTrailingClick = { expandedActionMenu = true },
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = expandedActionMenu,
                        onDismissRequest = { expandedActionMenu = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Info") },
                            leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                            onClick = { 
                                expandedActionMenu = false
                                showInfoCard = !showInfoCard
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = { 
                                expandedActionMenu = false
                                showShareSheet = true 
                            }
                        )

                        val currentItem = galleryItems.getOrNull(pagerState.currentPage)
                        
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Move to Trash") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = {
                                expandedActionMenu = false
                                if (currentItem != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val isMediaStoreUri = currentItem.uri.toString().startsWith("content://")
                                    if (!isMediaStoreUri) {
                                        android.widget.Toast.makeText(context, "Cannot delete this item", android.widget.Toast.LENGTH_SHORT).show()
                                        return@DropdownMenuItem
                                    }
                                    pendingDeleteItem = currentItem
                                    pendingDeletePage = pagerState.currentPage
                                    showDeleteConfirmDialog = true
                                }
                            }
                        )

                        if (currentItem != null && !currentItem.isVideo) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Set as Wallpaper") },
                                leadingIcon = { Icon(Icons.Filled.Wallpaper, contentDescription = null) },
                                onClick = {
                                    expandedActionMenu = false
                                    val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                                        setDataAndType(currentItem.uri, "image/*")
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
                    Box(modifier = Modifier.size(140.dp, 180.dp).clip(RoundedCornerShape(16.dp)).clickable { if (isSelected) selectedUris.remove(item.uri) else item.uri?.let { selectedUris.add(it) } }) {
                        AsyncImage(model = item.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(28.dp))
                        } else {
                            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(28.dp).border(2.dp, Color.White, CircleShape))
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
        color = Color.Black.copy(alpha = 0.7f),
        contentColor = Color.White
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
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)) 
    }
}

@Composable
private fun InfoColumn(label: String, deviceName: String, params: String) {
    Row { 
        Text(text = label, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.width(80.dp))
        Column(modifier = Modifier.weight(1f)) { 
            Text(text = deviceName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(text = params, color = Color.LightGray, fontSize = 11.sp) 
        } 
    }
}
