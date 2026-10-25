package com.inferno.gallery.ui

import androidx.compose.material3.FilledTonalIconButton
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    onTap: (() -> Unit)? = null,
    onPinchOverview: (() -> Unit)? = null,
    onPinchExpand: (() -> Unit)? = null
) {
    VideoPlayerItemWithResolvedUri(
        uri = uri,
        isCurrentPage = isCurrentPage,
        showControls = showControls,
        modifier = modifier,
        onTap = onTap,
        onPinchOverview = onPinchOverview,
        onPinchExpand = onPinchExpand
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerItemWithResolvedUri(
    uri: Uri,
    isCurrentPage: Boolean,
    showControls: Boolean,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    onPinchOverview: (() -> Unit)? = null,
    onPinchExpand: (() -> Unit)? = null
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

    // Reset zoom when switching pages
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
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
            .background(androidx.compose.ui.graphics.Color.Black)
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
                                // Zoom out back to 1x fit
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
                            } else {
                                // Zoom in to tapped point (2.8x)
                                val targetScale = 2.8f
                                val targetX = -(centroid.x - composableW / 2f) * (targetScale - 1)
                                val targetY = -(centroid.y - composableH / 2f) * (targetScale - 1)
                                val maxX = (composableW * (targetScale - 1)) / 2f
                                val maxY = (composableH * (targetScale - 1)) / 2f
                                launch {
                                    androidx.compose.animation.core.animate(
                                        initialValue = scale.floatValue,
                                        targetValue = targetScale,
                                        animationSpec = MotionTokens.gentleSpring()
                                    ) { v, _ -> scale.floatValue = v }
                                }
                                launch {
                                    androidx.compose.animation.core.animate(
                                        initialValue = offsetX.floatValue,
                                        targetValue = targetX.coerceIn(-maxX, maxX),
                                        animationSpec = MotionTokens.gentleSpring()
                                    ) { v, _ -> offsetX.floatValue = v }
                                }
                                launch {
                                    androidx.compose.animation.core.animate(
                                        initialValue = offsetY.floatValue,
                                        targetValue = targetY.coerceIn(-maxY, maxY),
                                        animationSpec = MotionTokens.gentleSpring()
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
                                val newScale = (scale.floatValue * zoomChange).coerceIn(0.85f, 6f)

                                val maxX = (composableW * (newScale - 1)).coerceAtLeast(0f) / 2f
                                val maxY = (composableH * (newScale - 1)).coerceAtLeast(0f) / 2f

                                val focalX = centroid.x - composableW / 2f
                                val focalY = centroid.y - composableH / 2f
                                val effectiveZoom = if (scale.floatValue > 0f) newScale / scale.floatValue else 1f
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
                            } // end if (pastTouchSlop && (zoomChange != 1f || panChange != Offset.Zero))

                            // Pinch-in threshold check on every multitouch frame
                            if (pastTouchSlop && event.changes.count { it.pressed } >= 2) {
                                accumulatedZoom *= zoomChange
                                if (accumulatedZoom < 0.85f && scale.floatValue <= 1.05f) {
                                    onPinchOverview?.invoke()
                                } else if (accumulatedZoom > 1.15f) {
                                    onPinchExpand?.invoke()
                                }
                            }

                            if (!event.changes.any { it.pressed }) break
                        }

                        // If pinched in past threshold or released below 0.90x, trigger overview
                        if (pastTouchSlop && maxPointerCount >= 2) {
                            if (scale.floatValue < 0.90f || accumulatedZoom < 0.88f) {
                                onPinchOverview?.invoke()
                                scale.floatValue = 1f
                                offsetX.floatValue = 0f
                                offsetY.floatValue = 0f
                            } else if (accumulatedZoom > 1.15f) {
                                onPinchExpand?.invoke()
                                scale.floatValue = 1f
                                offsetX.floatValue = 0f
                                offsetY.floatValue = 0f
                            }
                        }

                        // Snap back if released below 1.05x
                        if (scale.floatValue < 1.05f && scale.floatValue != 1f) {
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
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    scaleX = scale.floatValue
                    scaleY = scale.floatValue
                    translationX = offsetX.floatValue
                    translationY = offsetY.floatValue
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
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
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
                },
                modifier = playerModifier
            )
        }

        // Dim Scrim overlay
        AnimatedVisibility(
            visible = showControls && isCurrentPage,
            enter = fadeIn(MotionTokens.snappySpring()),
            exit = fadeOut(MotionTokens.gentleSpring()),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
            )
        }

        // Video playback controls overlay: Reduced-width Slider Pill + Play/Pause button on right side
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
                .padding(
                    bottom = if (viewerFilmstripEnabled) 138.dp else 82.dp,
                    start = 16.dp,
                    end = 16.dp
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 350.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reduced-width Slider Pill
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(enabled = false) {} // Consume clicks so they don't toggle controller
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(dragPosition ?: currentPosition),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Slider(
                            value = (dragPosition ?: currentPosition).toFloat(),
                            onValueChange = { dragPosition = it.toLong() },
                            onValueChangeFinished = {
                                dragPosition?.let {
                                    exoPlayer.seekTo(it)
                                    currentPosition = it
                                }
                                dragPosition = null
                            },
                            valueRange = 0f..videoDuration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        )

                        Text(
                            text = formatTime(videoDuration),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Audio mute/unmute
                        FilledTonalIconButton(
                            onClick = { isMuted = !isMuted },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) ImageVector.vectorResource(R.drawable.ic_ms_volume_off) else ImageVector.vectorResource(R.drawable.ic_ms_volume_up),
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Video play/pause button on right side of the video slider pill
                Surface(
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
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.primary,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = ImageVector.vectorResource(if (isPlaying) R.drawable.ic_ms_pause else R.drawable.ic_ms_play_arrow),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
