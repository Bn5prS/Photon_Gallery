package com.inferno.gallery.ui

import com.inferno.gallery.ui.components.VideoControlsBottomSheet
import androidx.compose.material3.FilledTonalIconButton
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector


@Composable
fun VideoPlayerItem(
    uri: Uri,
    isCurrentPage: Boolean,
    showControls: Boolean,
    modifier: Modifier = Modifier,
    isFilmstripOverview: Boolean = false,
    onTap: (() -> Unit)? = null,
    onPinchOverview: (() -> Unit)? = null,
    onPinchExpand: (() -> Unit)? = null,
    onScrubbingChanged: ((Boolean) -> Unit)? = null,
    onScaleChanged: ((Float) -> Unit)? = null
) {
    VideoPlayerItemWithResolvedUri(
        uri = uri,
        isCurrentPage = isCurrentPage,
        showControls = showControls,
        modifier = modifier,
        isFilmstripOverview = isFilmstripOverview,
        onTap = onTap,
        onPinchOverview = onPinchOverview,
        onPinchExpand = onPinchExpand,
        onScrubbingChanged = onScrubbingChanged,
        onScaleChanged = onScaleChanged
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerItemWithResolvedUri(
    uri: Uri,
    isCurrentPage: Boolean,
    showControls: Boolean,
    modifier: Modifier = Modifier,
    isFilmstripOverview: Boolean = false,
    onTap: (() -> Unit)? = null,
    onPinchOverview: (() -> Unit)? = null,
    onPinchExpand: (() -> Unit)? = null,
    onScrubbingChanged: ((Boolean) -> Unit)? = null,
    onScaleChanged: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        val builder = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            
        builder.build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var videoDuration by remember { mutableStateOf(0L) }
    var dragPosition by remember { mutableStateOf<Long?>(null) }

    // Zoom & Pan state
    val scale = remember { mutableFloatStateOf(1f) }
    val offsetX = remember { mutableFloatStateOf(0f) }
    val offsetY = remember { mutableFloatStateOf(0f) }
    val animJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val currentIsOverview by rememberUpdatedState(isFilmstripOverview)
    val currentOnPinchOverview by rememberUpdatedState(onPinchOverview)
    val currentOnPinchExpand by rememberUpdatedState(onPinchExpand)
    val currentOnScaleChanged by rememberUpdatedState(onScaleChanged)

    LaunchedEffect(scale.floatValue, isCurrentPage) {
        if (isCurrentPage) {
            currentOnScaleChanged?.invoke(scale.floatValue)
        }
    }

    // Reset zoom when switching pages or entering filmstrip overview
    LaunchedEffect(isCurrentPage, isFilmstripOverview) {
        if (!isCurrentPage || isFilmstripOverview) {
            animJob.value?.cancel()
            scale.floatValue = 1f
            offsetX.floatValue = 0f
            offsetY.floatValue = 0f
        }
    }
    var videoAspectRatio by remember(uri) {
        val vs = exoPlayer.videoSize
        val initialRatio = if (vs.width > 0 && vs.height > 0) {
            val par = if (vs.pixelWidthHeightRatio > 0f) vs.pixelWidthHeightRatio else 1f
            val isRotated = vs.unappliedRotationDegrees == 90 || vs.unappliedRotationDegrees == 270
            val rawW = if (isRotated) vs.height else vs.width
            val rawH = if (isRotated) vs.width else vs.height
            (rawW.toFloat() * par) / rawH.toFloat()
        } else null
        mutableStateOf(initialRatio)
    }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val widthStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val rotationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                retriever.release()
                val w = widthStr?.toFloatOrNull() ?: 0f
                val h = heightStr?.toFloatOrNull() ?: 0f
                val rot = rotationStr?.toIntOrNull() ?: 0
                if (w > 0f && h > 0f) {
                    val ratio = if (rot == 90 || rot == 270) h / w else w / h
                    if (ratio > 0f && videoAspectRatio == null) {
                        withContext(Dispatchers.Main) {
                            videoAspectRatio = ratio
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    val settings = remember { com.inferno.gallery.data.SettingsRepository.getInstance(context) }
    val autoplayWithSound by settings.autoplayWithSoundEnabledFlow.collectAsState(initial = false)
    val viewerFilmstripEnabled = false // Feature backed up: re-bind to settings.viewerFilmstripEnabledFlow when requested
    var isMuted by remember(autoplayWithSound) { mutableStateOf(!autoplayWithSound) }
    var playbackSpeed by remember(uri) { mutableFloatStateOf(1.0f) }
    var rotationDegrees by remember(uri) { mutableStateOf(0) }
    var isLooping by remember(uri) { mutableStateOf(false) }
    var resizeMode by remember(uri) { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var showControlsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(isLooping) {
        exoPlayer.repeatMode = if (isLooping) {
            androidx.media3.common.Player.REPEAT_MODE_ONE
        } else {
            androidx.media3.common.Player.REPEAT_MODE_OFF
        }
    }

    val animatedRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = rotationDegrees.toFloat(),
        animationSpec = MotionTokens.snappySpring(),
        label = "videoRotation"
    )

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_READY) {
                    videoDuration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val par = if (videoSize.pixelWidthHeightRatio > 0f) videoSize.pixelWidthHeightRatio else 1f
                    val isRotated = videoSize.unappliedRotationDegrees == 90 || videoSize.unappliedRotationDegrees == 270
                    val rawW = if (isRotated) videoSize.height else videoSize.width
                    val rawH = if (isRotated) videoSize.width else videoSize.height
                    val computedRatio = (rawW.toFloat() * par) / rawH.toFloat()
                    if (computedRatio > 0f) {
                        videoAspectRatio = computedRatio
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                exoPlayer.seekTo(0)
            }
            exoPlayer.play()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(100)
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onTap?.invoke()
                    },
                    onDoubleTap = { centroid ->
                        animJob.value?.cancel()
                        animJob.value = coroutineScope.launch {
                            val composableW = size.width.toFloat()
                            val composableH = size.height.toFloat()
                            if (scale.floatValue > 1.05f) {
                                // Zoom out back to 1x fit (critically damped — no bounce or negative-scale flip)
                                launch {
                                    androidx.compose.animation.core.animate(
                                        initialValue = scale.floatValue,
                                        targetValue = 1f,
                                        animationSpec = MotionTokens.sharedElementSpring()
                                    ) { v, _ -> scale.floatValue = v.coerceAtLeast(1f) }
                                }
                                launch {
                                    androidx.compose.animation.core.animate(
                                        initialValue = offsetX.floatValue,
                                        targetValue = 0f,
                                        animationSpec = MotionTokens.sharedElementSpring()
                                    ) { v, _ -> offsetX.floatValue = v }
                                }
                                launch {
                                    androidx.compose.animation.core.animate(
                                        initialValue = offsetY.floatValue,
                                        targetValue = 0f,
                                        animationSpec = MotionTokens.sharedElementSpring()
                                    ) { v, _ -> offsetY.floatValue = v }
                                }
                            } else {
                                // Zoom in to tapped point (3.0x)
                                val targetScale = 3.0f
                                val targetX = -(centroid.x - composableW / 2f) * (targetScale - 1)
                                val targetY = -(centroid.y - composableH / 2f) * (targetScale - 1)
                                val maxX = (composableW * (targetScale - 1)) / 2f
                                val maxY = (composableH * (targetScale - 1)) / 2f
                                launch {
                                    androidx.compose.animation.core.animate(
                                        initialValue = scale.floatValue,
                                        targetValue = targetScale,
                                        animationSpec = MotionTokens.sharedElementSpring()
                                    ) { v, _ -> scale.floatValue = v.coerceAtLeast(0.05f) }
                                }
                                launch {
                                    androidx.compose.animation.core.animate(
                                        initialValue = offsetX.floatValue,
                                        targetValue = targetX.coerceIn(-maxX, maxX),
                                        animationSpec = MotionTokens.sharedElementSpring()
                                    ) { v, _ -> offsetX.floatValue = v }
                                }
                                launch {
                                    androidx.compose.animation.core.animate(
                                        initialValue = offsetY.floatValue,
                                        targetValue = targetY.coerceIn(-maxY, maxY),
                                        animationSpec = MotionTokens.sharedElementSpring()
                                    ) { v, _ -> offsetY.floatValue = v }
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)

                    var shouldStartZoomPan = scale.floatValue > 1f
                    if (!shouldStartZoomPan) {
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
                        val gestureStartScale = scale.floatValue
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
                            val isMultiTouch = pressedCount >= 2

                            if (!pastTouchSlop) {
                                accumulatedZoom *= zoomChange
                                accumulatedPan += panChange
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = kotlin.math.abs(1 - accumulatedZoom) * centroidSize
                                val panMotion = accumulatedPan.getDistance()
                                if (zoomMotion > touchSlop || panMotion > touchSlop || isMultiTouch) {
                                    pastTouchSlop = true
                                }
                            } else if (isMultiTouch) {
                                accumulatedZoom *= zoomChange
                            }

                            if (pastTouchSlop && !isMultiTouch && scale.floatValue <= 1.05f && maxPointerCount == 1) {
                                break
                            }

                            if (pastTouchSlop && currentIsOverview) {
                                // In filmstrip overview: pinch out exits overview without zooming video
                                if (isMultiTouch && accumulatedZoom > 1.15f) {
                                    currentOnPinchExpand?.invoke()
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (pastTouchSlop && (zoomChange != 1f || panChange != Offset.Zero || isMultiTouch)) {
                                val centroid = event.calculateCentroid(useCurrent = false)
                                animJob.value?.cancel()
                                val oldScale = scale.floatValue
                                val newScale = (oldScale * zoomChange).coerceIn(0.85f, 6f)

                                val maxX = (composableW * (newScale - 1)).coerceAtLeast(0f) / 2f
                                val maxY = (composableH * (newScale - 1)).coerceAtLeast(0f) / 2f

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

                                // Pinch-in from unzoomed normal view enters filmstrip overview
                                if (isMultiTouch && gestureStartScale <= 1.05f && oldScale <= 1.05f && accumulatedZoom < 0.85f) {
                                    currentOnPinchOverview?.invoke()
                                }

                                if (isMultiTouch) {
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                } else if (newScale > 1f) {
                                    val hittingLeft = newOffsetX >= maxX && panChange.x > 0
                                    val hittingRight = newOffsetX <= -maxX && panChange.x < 0
                                    if (!hittingLeft && !hittingRight) {
                                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    }
                                }
                            }

                            if (!event.changes.any { it.pressed }) break
                        }

                        if (!currentIsOverview) {
                            // Only enter overview if pinch started unzoomed and pinched inward past threshold
                            if (pastTouchSlop && maxPointerCount >= 2 && gestureStartScale <= 1.05f &&
                                (scale.floatValue < 0.90f || accumulatedZoom < 0.85f)
                            ) {
                                currentOnPinchOverview?.invoke()
                                scale.floatValue = 1f
                                offsetX.floatValue = 0f
                                offsetY.floatValue = 0f
                            } else if (scale.floatValue < 1.05f && (scale.floatValue != 1f || offsetX.floatValue != 0f || offsetY.floatValue != 0f)) {
                                // Snap back cleanly if released below 1.05x
                                animJob.value?.cancel()
                                animJob.value = coroutineScope.launch {
                                    launch {
                                        androidx.compose.animation.core.animate(
                                            initialValue = scale.floatValue,
                                            targetValue = 1f,
                                            animationSpec = MotionTokens.gentleSpring()
                                        ) { v, _ -> scale.floatValue = v }
                                    }
                                    launch {
                                        androidx.compose.animation.core.animate(
                                            initialValue = offsetX.floatValue,
                                            targetValue = 0f,
                                            animationSpec = MotionTokens.gentleSpring()
                                        ) { v, _ -> offsetX.floatValue = v }
                                    }
                                    launch {
                                        androidx.compose.animation.core.animate(
                                            initialValue = offsetY.floatValue,
                                            targetValue = 0f,
                                            animationSpec = MotionTokens.gentleSpring()
                                        ) { v, _ -> offsetY.floatValue = v }
                                    }
                                }
                            } else if (pastTouchSlop && scale.floatValue >= 1.05f && maxPointerCount == 1) {
                                // Fling momentum when panning while zoomed
                                val velocity = velocityTracker.calculateVelocity()
                                val currentMaxX = (composableW * (scale.floatValue - 1)) / 2f
                                val currentMaxY = (composableH * (scale.floatValue - 1)) / 2f
                                val flingFactor = 0.35f
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
                                            animationSpec = androidx.compose.animation.core.spring(
                                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                                stiffness = androidx.compose.animation.core.Spring.StiffnessVeryLow
                                            )
                                        ) { v, _ -> offsetX.floatValue = v }
                                    }
                                    launch {
                                        androidx.compose.animation.core.animate(
                                            initialValue = offsetY.floatValue,
                                            targetValue = targetFlingY,
                                            initialVelocity = velocity.y,
                                            animationSpec = androidx.compose.animation.core.spring(
                                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                                stiffness = androidx.compose.animation.core.Spring.StiffnessVeryLow
                                            )
                                        ) { v, _ -> offsetY.floatValue = v }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    val safeScale = scale.floatValue.coerceAtLeast(0.05f)
                    val isQuarterRotated = (rotationDegrees % 180) != 0
                    val w = size.width
                    val h = size.height
                    val rotationScale = if (isQuarterRotated && w > 0f && h > 0f) {
                        minOf(w / h, h / w).coerceAtMost(1f)
                    } else 1f

                    scaleX = safeScale * rotationScale
                    scaleY = safeScale * rotationScale
                    translationX = offsetX.floatValue
                    translationY = offsetY.floatValue
                    rotationZ = animatedRotation
                },
            contentAlignment = Alignment.Center
        ) {
            val playerModifier = if (videoAspectRatio != null && videoAspectRatio!! > 0f) {
                Modifier
                    .fillMaxSize()
                    .aspectRatio(videoAspectRatio!!, matchHeightConstraintsFirst = false)
            } else {
                Modifier.fillMaxSize()
            }
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        controllerAutoShow = false
                        hideController()
                        // Completely eradicate the Media3 default controller view from the hierarchy
                        // so legacy ghost controls (prev/rewind/ffwd/next/progress) never appear
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_controller)?.let { ctrl ->
                            ctrl.visibility = android.view.View.GONE
                            (ctrl.parent as? android.view.ViewGroup)?.removeView(ctrl)
                        }
                        player = exoPlayer
                        this.resizeMode = resizeMode
                        try {
                            val method = javaClass.getMethod("setEnableComposeSurfaceSyncWorkaround", Boolean::class.javaPrimitiveType)
                            method.invoke(this, true)
                        } catch (_: Throwable) {}
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.useController = false
                    playerView.controllerAutoShow = false
                    playerView.hideController()
                    playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_controller)?.let { ctrl ->
                        ctrl.visibility = android.view.View.GONE
                        (ctrl.parent as? android.view.ViewGroup)?.removeView(ctrl)
                    }
                    if (playerView.player != exoPlayer) {
                        playerView.player = exoPlayer
                    }
                    if (playerView.resizeMode != resizeMode) {
                        playerView.resizeMode = resizeMode
                    }
                },
                modifier = playerModifier
            )
        }

        // Floating Video Scrubber Capsule: Single unified floating pill styling
        AnimatedVisibility(
            visible = showControls && isCurrentPage,
            enter = slideInVertically(
                animationSpec = MotionTokens.snappySpring(),
                initialOffsetY = { it / 2 }
            ) + fadeIn(MotionTokens.snappySpring()),
            exit = slideOutVertically(
                animationSpec = MotionTokens.gentleSpring(),
                targetOffsetY = { it / 2 }
            ) + fadeOut(MotionTokens.gentleSpring()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = null,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
                    .height(52.dp)
                    .clickable(enabled = false) {} // Consume clicks so they don't toggle controller
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play / Pause button on left: Circular tonal button
                    FilledTonalIconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                    exoPlayer.seekTo(0)
                                }
                                exoPlayer.play()
                            }
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(if (isPlaying) R.drawable.ic_ms_pause else R.drawable.ic_ms_play_arrow),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatTime(dragPosition ?: currentPosition),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Slider(
                        value = (dragPosition ?: currentPosition).toFloat(),
                        onValueChange = {
                            if (dragPosition == null) onScrubbingChanged?.invoke(true)
                            dragPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            dragPosition?.let {
                                exoPlayer.seekTo(it)
                                currentPosition = it
                            }
                            dragPosition = null
                            onScrubbingChanged?.invoke(false)
                        },
                        valueRange = 0f..videoDuration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = formatTime(videoDuration),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Audio mute / unmute button: Circular tonal button
                    FilledTonalIconButton(
                        onClick = { isMuted = !isMuted },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) ImageVector.vectorResource(R.drawable.ic_ms_volume_off) else ImageVector.vectorResource(R.drawable.ic_ms_volume_up),
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Video controls / tune button: opens M3 Expressive Quick Sheet
                    val hasCustomSettings = playbackSpeed != 1.0f || rotationDegrees != 0 || isLooping || resizeMode != androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    FilledTonalIconButton(
                        onClick = { showControlsSheet = true },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (hasCustomSettings) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (hasCustomSettings) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_tune),
                            contentDescription = "Video Controls",
                            tint = if (hasCustomSettings) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (showControlsSheet) {
            VideoControlsBottomSheet(
                playbackSpeed = playbackSpeed,
                onPlaybackSpeedChange = { playbackSpeed = it },
                rotationDegrees = rotationDegrees,
                onRotationDegreesChange = { rotationDegrees = it },
                isLooping = isLooping,
                onLoopingChange = { isLooping = it },
                resizeMode = resizeMode,
                onResizeModeChange = { resizeMode = it },
                onDismissRequest = { showControlsSheet = false }
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
