package com.inferno.gallery.ui

import com.inferno.gallery.ui.components.VideoControlsBottomSheet
import androidx.compose.material3.FilledTonalIconButton
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Pooled single-instance ExoPlayer holder for full-screen pager swiping in [DetailScreen].
 *
 * Creating and releasing an [ExoPlayer] + hardware MediaCodec pipeline synchronously on every
 * pager swipe blocks the main thread for 100–300 ms per swipe and risks exhausting SoC codec
 * instances. Reusing one player across all swipes makes transitions instant and eliminates
 * mid-swipe frame drops.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SharedVideoPlayerHolder(private val appContext: android.content.Context) {
    private var player: ExoPlayer? = null
    private var currentUri: Uri? = null
    private var wasPlayingBeforeScroll: Boolean = false

    fun getOrCreate(): ExoPlayer {
        val existing = player
        if (existing != null) return existing
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(appContext)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)
        return ExoPlayer.Builder(appContext)
            .setRenderersFactory(renderersFactory)
            .build()
            .also {
                ActiveVideoPlayerCoordinator.registerExternal(it)
                player = it
            }
    }

    fun prepareUri(uri: Uri, autoPlay: Boolean = true): ExoPlayer {
        val exo = getOrCreate()
        val uriKey = uri.toString()
        if (currentUri != uri || exo.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            currentUri = uri
            wasPlayingBeforeScroll = false
            exo.playWhenReady = false
            exo.stop()
            exo.clearMediaItems()
            exo.setMediaItem(
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(uriKey)
                    .build()
            )
            exo.prepare()
            exo.playWhenReady = autoPlay
        } else if (autoPlay) {
            if (exo.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                exo.seekTo(0)
            }
            if (!exo.isPlaying && wasPlayingBeforeScroll) {
                exo.play()
            }
            wasPlayingBeforeScroll = false
        }
        return exo
    }

    fun onScrollStarted() {
        val exo = player ?: return
        if (exo.isPlaying || exo.playWhenReady) {
            wasPlayingBeforeScroll = true
            exo.pause()
        }
    }

    fun onScrollSettledOnSameVideo() {
        val exo = player ?: return
        if (wasPlayingBeforeScroll) {
            wasPlayingBeforeScroll = false
            exo.play()
        }
    }

    fun pause() {
        wasPlayingBeforeScroll = false
        player?.playWhenReady = false
        player?.pause()
    }

    fun pauseIfUri(uri: Uri) {
        if (currentUri == uri) {
            wasPlayingBeforeScroll = false
            player?.playWhenReady = false
            player?.pause()
        }
    }

    fun release() {
        val exo = player ?: return
        player = null
        currentUri = null
        wasPlayingBeforeScroll = false
        ActiveVideoPlayerCoordinator.releaseIfActive(exo)
    }
}

/**
 * Ensures at most one ExoPlayer / hardware MediaCodec video decoder is active across callers.
 */
private object ActiveVideoPlayerCoordinator {
    private var activePlayer: ExoPlayer? = null

    fun isActive(player: ExoPlayer): Boolean = activePlayer === player

    fun registerExternal(newPlayer: ExoPlayer) {
        val previous = activePlayer
        if (previous != null && previous !== newPlayer) {
            try {
                previous.playWhenReady = false
                previous.stop()
                previous.clearVideoSurface()
                previous.release()
            } catch (_: Throwable) {
            }
        }
        activePlayer = newPlayer
    }

    fun registerAndClaim(newPlayer: ExoPlayer) = registerExternal(newPlayer)

    fun releaseIfActive(player: ExoPlayer) {
        if (activePlayer === player) {
            activePlayer = null
        }
        try {
            player.playWhenReady = false
            player.stop()
            player.clearVideoSurface()
            player.release()
        } catch (_: Throwable) {
        }
    }
}


@Composable
fun VideoPlayerItem(
    uri: Uri,
    isCurrentPage: Boolean,
    showControls: Boolean,
    modifier: Modifier = Modifier,
    isFilmstripOverview: Boolean = false,
    placeholderCacheKey: String? = null,
    sharedPlayerHolder: SharedVideoPlayerHolder? = null,
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
        placeholderCacheKey = placeholderCacheKey,
        sharedPlayerHolder = sharedPlayerHolder,
        onTap = onTap,
        onPinchOverview = onPinchOverview,
        onPinchExpand = onPinchExpand,
        onScrubbingChanged = onScrubbingChanged,
        onScaleChanged = onScaleChanged
    )
}

@Composable
private fun VideoPosterFallback(
    uri: Uri,
    placeholderCacheKey: String?,
    showPlayBadge: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val posterRequest = remember<ImageRequest>(uri, placeholderCacheKey) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(720, 720)
            .precision(coil3.size.Precision.INEXACT)
            .memoryCacheKey("video_poster_${uri}@720")
            .diskCacheKey("video_poster_${uri}@720")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .apply {
                if (!placeholderCacheKey.isNullOrEmpty()) {
                    placeholderMemoryCacheKey(placeholderCacheKey)
                }
            }
            .crossfade(false)
            .build()
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = posterRequest,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        if (showPlayBadge) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.48f),
                contentColor = Color.White,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerItemWithResolvedUri(
    uri: Uri,
    isCurrentPage: Boolean,
    showControls: Boolean,
    modifier: Modifier = Modifier,
    isFilmstripOverview: Boolean = false,
    placeholderCacheKey: String? = null,
    sharedPlayerHolder: SharedVideoPlayerHolder? = null,
    onTap: (() -> Unit)? = null,
    onPinchOverview: (() -> Unit)? = null,
    onPinchExpand: (() -> Unit)? = null,
    onScrubbingChanged: ((Boolean) -> Unit)? = null,
    onScaleChanged: ((Float) -> Unit)? = null
) {
    // Offscreen / in-fling pager video pages must NOT instantiate ExoPlayer or allocate hardware
    // MediaCodec decoders. Render only the cached video poster frame until scrolling settles.
    if (!isCurrentPage) {
        VideoPosterFallback(
            uri = uri,
            placeholderCacheKey = placeholderCacheKey,
            showPlayBadge = true,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val isPooledPlayer = sharedPlayerHolder != null
    val uriKey = remember(uri) { uri.toString() }
    val exoPlayer = remember(uri, sharedPlayerHolder) {
        if (sharedPlayerHolder != null) {
            sharedPlayerHolder.prepareUri(uri, autoPlay = true)
        } else {
            val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
                .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true)

            ExoPlayer.Builder(context)
                .setRenderersFactory(renderersFactory)
                .build()
                .apply {
                    ActiveVideoPlayerCoordinator.registerAndClaim(this)
                    setMediaItem(
                        MediaItem.Builder()
                            .setUri(uri)
                            .setMediaId(uriKey)
                            .build()
                    )
                    prepare()
                    playWhenReady = true
                }
        }
    }

    var isPlaying by remember(uri) { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember(uri) { mutableStateOf(false) }
    var currentPosition by remember(uri) { mutableStateOf(0L) }
    var videoDuration by remember(uri) { mutableStateOf(0L) }
    var dragPosition by remember(uri) { mutableStateOf<Long?>(null) }

    // Zoom & Pan state
    val scale = remember(uri) { mutableFloatStateOf(1f) }
    val offsetX = remember(uri) { mutableFloatStateOf(0f) }
    val offsetY = remember(uri) { mutableFloatStateOf(0f) }
    val animJob = remember(uri) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
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
    // IMPORTANT: Never initialize videoAspectRatio from a pooled ExoPlayer's previous exoPlayer.videoSize!
    // When swiping between videos of different orientations/ratios (e.g. 16:9 <-> 9:16), reading the
    // previous video's VideoSize shrinks or stretches the new video until onVideoSizeChanged arrives.
    var videoAspectRatio by remember(uri) { mutableStateOf<Float?>(null) }

    LaunchedEffect(uri, videoAspectRatio) {
        if (videoAspectRatio != null) return@LaunchedEffect
        delay(280L)
        if (videoAspectRatio != null) return@LaunchedEffect
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
        animationSpec = MotionTokens.sharedElementSpring(),
        label = "videoRotation"
    )

    DisposableEffect(exoPlayer, uriKey, isPooledPlayer) {
        var retryAttempt = 0
        var retryJob: kotlinx.coroutines.Job? = null
        fun matchesCurrentUri(): Boolean = exoPlayer.currentMediaItem?.mediaId == uriKey
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                if (matchesCurrentUri()) {
                    isPlaying = isPlayingState
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_READY && matchesCurrentUri()) {
                    retryAttempt = 0
                    videoDuration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
            override fun onRenderedFirstFrame() {
                if (matchesCurrentUri() && exoPlayer.playbackState != androidx.media3.common.Player.STATE_IDLE) {
                    retryAttempt = 0
                    hasRenderedFirstFrame = true
                }
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (!matchesCurrentUri()) return
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
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Recover from transient MediaCodec hardware-decoder release latency during rapid swipes.
                if (retryAttempt < 3 && matchesCurrentUri() && ActiveVideoPlayerCoordinator.isActive(exoPlayer)) {
                    retryAttempt++
                    val backoffMs = when (retryAttempt) {
                        1 -> 160L
                        2 -> 380L
                        else -> 750L
                    }
                    retryJob?.cancel()
                    retryJob = coroutineScope.launch {
                        delay(backoffMs)
                        if (!matchesCurrentUri() || !ActiveVideoPlayerCoordinator.isActive(exoPlayer)) return@launch
                        try {
                            val resumePos = exoPlayer.currentPosition.coerceAtLeast(0L)
                            exoPlayer.stop()
                            exoPlayer.setMediaItem(
                                MediaItem.Builder()
                                    .setUri(uri)
                                    .setMediaId(uriKey)
                                    .build()
                            )
                            if (resumePos > 0L) exoPlayer.seekTo(resumePos)
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        // Re-sync in case ExoPlayer already reached READY/first-frame synchronously
        if (matchesCurrentUri()) {
            val curVs = exoPlayer.videoSize
            if (curVs.width > 0 && curVs.height > 0) {
                val par = if (curVs.pixelWidthHeightRatio > 0f) curVs.pixelWidthHeightRatio else 1f
                val isRotated = curVs.unappliedRotationDegrees == 90 || curVs.unappliedRotationDegrees == 270
                val rawW = if (isRotated) curVs.height else curVs.width
                val rawH = if (isRotated) curVs.width else curVs.height
                val ratio = (rawW.toFloat() * par) / rawH.toFloat()
                if (ratio > 0f && videoAspectRatio == null) videoAspectRatio = ratio
            }
            if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_READY) {
                videoDuration = exoPlayer.duration.coerceAtLeast(0L)
            }
        }
        onDispose {
            retryJob?.cancel()
            exoPlayer.removeListener(listener)
            if (isPooledPlayer) {
                sharedPlayerHolder?.pauseIfUri(uri)
            } else {
                ActiveVideoPlayerCoordinator.releaseIfActive(exoPlayer)
            }
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

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val currentOnTap by rememberUpdatedState(onTap)
    val currentVideoDuration by rememberUpdatedState(videoDuration)
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    // Double-tap seek feedback state (left = -1, right = +1, 0 = hidden)
    var seekOverlayDirection by remember(uri) { androidx.compose.runtime.mutableIntStateOf(0) }
    var seekOverlayTotalSec by remember(uri) { androidx.compose.runtime.mutableIntStateOf(0) }
    var seekOverlayHideJob by remember(uri) { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Center double-tap play/pause feedback state (null = hidden, true = playing, false = paused)
    var centerToggleFeedback by remember(uri) { mutableStateOf<Boolean?>(null) }
    var centerFeedbackHideJob by remember(uri) { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(uri, exoPlayer) {
                detectTapGestures(
                    onTap = {
                        currentOnTap?.invoke()
                    },
                    onDoubleTap = { centroid ->
                        val composableW = size.width.toFloat().coerceAtLeast(1f)
                        val relX = centroid.x / composableW
                        val totalDur = currentVideoDuration.takeIf { it > 0L }
                            ?: exoPlayer.duration.coerceAtLeast(0L)
                        // Adaptive seek step: 5s for short clips (< 30s), 10s for longer videos
                        val stepSec = if (totalDur in 1L..29_999L) 5 else 10
                        val stepMs = stepSec * 1000L

                        when {
                            relX < 0.35f -> {
                                // Left zone: rewind (accumulates on consecutive rapid double-taps)
                                val basePos = exoPlayer.currentPosition.coerceAtLeast(0L)
                                val nextPos = (basePos - stepMs).coerceAtLeast(0L)
                                exoPlayer.seekTo(nextPos)
                                currentPosition = nextPos
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                seekOverlayTotalSec = if (seekOverlayDirection == -1) seekOverlayTotalSec + stepSec else stepSec
                                seekOverlayDirection = -1
                                seekOverlayHideJob?.cancel()
                                seekOverlayHideJob = coroutineScope.launch {
                                    delay(750L)
                                    seekOverlayDirection = 0
                                    seekOverlayTotalSec = 0
                                }
                            }
                            relX > 0.65f -> {
                                // Right zone: fast-forward (accumulates on consecutive rapid double-taps)
                                val basePos = exoPlayer.currentPosition.coerceAtLeast(0L)
                                val maxPos = if (totalDur > 0L) totalDur else Long.MAX_VALUE
                                val nextPos = (basePos + stepMs).coerceAtMost(maxPos)
                                exoPlayer.seekTo(nextPos)
                                currentPosition = nextPos
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                seekOverlayTotalSec = if (seekOverlayDirection == 1) seekOverlayTotalSec + stepSec else stepSec
                                seekOverlayDirection = 1
                                seekOverlayHideJob?.cancel()
                                seekOverlayHideJob = coroutineScope.launch {
                                    delay(750L)
                                    seekOverlayDirection = 0
                                    seekOverlayTotalSec = 0
                                }
                            }
                            else -> {
                                // Center zone: if currently pinch-zoomed, snap back to 1x; otherwise toggle Play/Pause
                                if (scale.floatValue > 1.05f) {
                                    animJob.value?.cancel()
                                    animJob.value = coroutineScope.launch {
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
                                    }
                                } else {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    val willPlay = !currentIsPlaying
                                    if (willPlay) {
                                        if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                            exoPlayer.seekTo(0L)
                                        }
                                        exoPlayer.play()
                                    } else {
                                        exoPlayer.pause()
                                    }
                                    centerToggleFeedback = willPlay
                                    centerFeedbackHideJob?.cancel()
                                    centerFeedbackHideJob = coroutineScope.launch {
                                        delay(550L)
                                        centerToggleFeedback = null
                                    }
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val containerWidth = maxWidth
            val containerHeight = maxHeight
            val containerRatio = if (containerHeight.value > 0f) containerWidth.value / containerHeight.value else 1f
            val rawRatio = videoAspectRatio ?: containerRatio

            val isFillMode = resizeMode == androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM

            // Calculate target unrotated video dimensions based on Fit vs Fill
            val (targetWidth, targetHeight) = if (isFillMode) {
                // Crop to fill screen: expand beyond bounds so no black bars
                if (rawRatio > containerRatio) {
                    Pair(containerHeight * rawRatio, containerHeight)
                } else {
                    Pair(containerWidth, containerWidth / rawRatio)
                }
            } else {
                // Fit within screen: entire video visible with letterboxing
                if (rawRatio > containerRatio) {
                    Pair(containerWidth, containerWidth / rawRatio)
                } else {
                    Pair(containerHeight * rawRatio, containerHeight)
                }
            }

            // Only animate box dimensions when the user explicitly toggles Fit <-> Fill.
            // Initial load, async videoAspectRatio resolution, and container layout changes
            // must snap immediately so the video never rubber-bands or stretches when opened.
            val widthAnim = remember(uri) { Animatable(targetWidth.value) }
            val heightAnim = remember(uri) { Animatable(targetHeight.value) }
            var lastResizeMode by remember(uri) { mutableStateOf(resizeMode) }
            var isAnimatingResize by remember(uri) { mutableStateOf(false) }

            LaunchedEffect(targetWidth, targetHeight, resizeMode) {
                val resizeModeToggled = resizeMode != lastResizeMode
                lastResizeMode = resizeMode
                if (resizeModeToggled) {
                    isAnimatingResize = true
                    try {
                        kotlinx.coroutines.coroutineScope {
                            launch { widthAnim.animateTo(targetWidth.value, MotionTokens.sharedElementSpring()) }
                            launch { heightAnim.animateTo(targetHeight.value, MotionTokens.sharedElementSpring()) }
                        }
                    } finally {
                        isAnimatingResize = false
                    }
                } else {
                    isAnimatingResize = false
                    widthAnim.snapTo(targetWidth.value)
                    heightAnim.snapTo(targetHeight.value)
                }
            }

            val displayWidth = if (resizeMode == lastResizeMode && !isAnimatingResize) {
                targetWidth
            } else {
                widthAnim.value.dp
            }
            val displayHeight = if (resizeMode == lastResizeMode && !isAnimatingResize) {
                targetHeight
            } else {
                heightAnim.value.dp
            }

            // Calculate rotation scaling when rotated 90° or 270°
            val isQuarterRotated = (rotationDegrees % 180) != 0
            val targetRotationScale = if (isQuarterRotated && targetWidth.value > 0f && targetHeight.value > 0f) {
                if (isFillMode) {
                    maxOf(
                        containerWidth.value / targetHeight.value,
                        containerHeight.value / targetWidth.value
                    )
                } else {
                    minOf(
                        containerWidth.value / targetHeight.value,
                        containerHeight.value / targetWidth.value
                    )
                }
            } else {
                1f
            }

            val animatedRotationScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = targetRotationScale,
                animationSpec = MotionTokens.sharedElementSpring(),
                label = "videoRotationScale"
            )

            // Keep PlayerView aspect-locked (FIT/ZOOM, never FILL) so even frames rendered before
            // videoAspectRatio resolves can never stretch or distort.
            val playerResizeMode = if (isFillMode) {
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            } else {
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            val isVideoSurfaceReady = hasRenderedFirstFrame && videoAspectRatio != null

            // Always show the aspect-fitted poster full-container until BOTH videoAspectRatio
            // AND the first decoded frame for this exact URI are ready. Keeping the PlayerView
            // at alpha = 0f until then prevents any full-screen flash or intermediate aspect jump.
            if (!isVideoSurfaceReady) {
                VideoPosterFallback(
                    uri = uri,
                    placeholderCacheKey = placeholderCacheKey,
                    showPlayBadge = false,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .size(displayWidth, displayHeight)
                    .graphicsLayer {
                        val safeScale = scale.floatValue.coerceAtLeast(0.05f)
                        scaleX = safeScale * animatedRotationScale
                        scaleY = safeScale * animatedRotationScale
                        translationX = offsetX.floatValue
                        translationY = offsetY.floatValue
                        rotationZ = animatedRotation
                        alpha = if (isVideoSurfaceReady) 1f else 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        val view = android.view.LayoutInflater.from(ctx)
                            .inflate(R.layout.view_video_player, null, false) as PlayerView
                        view.apply {
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            useController = false
                            controllerAutoShow = false
                            hideController()
                            isClickable = false
                            isFocusable = false
                            findViewById<android.view.View>(androidx.media3.ui.R.id.exo_controller)?.let { ctrl ->
                                ctrl.visibility = android.view.View.GONE
                                (ctrl.parent as? android.view.ViewGroup)?.removeView(ctrl)
                            }
                            player = exoPlayer
                            this.resizeMode = playerResizeMode
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
                        if (playerView.resizeMode != playerResizeMode) {
                            playerView.resizeMode = playerResizeMode
                        }
                    },
                    onRelease = { playerView ->
                        playerView.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Double-tap seek feedback pills (Left = Rewind, Right = Fast-Forward)
        AnimatedVisibility(
            visible = seekOverlayDirection == -1 && seekOverlayTotalSec > 0,
            enter = fadeIn(MotionTokens.fastEffectsSpec()) + scaleIn(MotionTokens.snappySpring(), initialScale = 0.85f),
            exit = fadeOut(MotionTokens.fastEffectsSpec()) + scaleOut(MotionTokens.fastEffectsSpec(), targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp)
        ) {
            DoubleTapSeekFeedbackPill(
                isForward = false,
                seconds = seekOverlayTotalSec
            )
        }

        AnimatedVisibility(
            visible = seekOverlayDirection == 1 && seekOverlayTotalSec > 0,
            enter = fadeIn(MotionTokens.fastEffectsSpec()) + scaleIn(MotionTokens.snappySpring(), initialScale = 0.85f),
            exit = fadeOut(MotionTokens.fastEffectsSpec()) + scaleOut(MotionTokens.fastEffectsSpec(), targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp)
        ) {
            DoubleTapSeekFeedbackPill(
                isForward = true,
                seconds = seekOverlayTotalSec
            )
        }

        // Subtle center Play/Pause badge on center double-tap
        AnimatedVisibility(
            visible = centerToggleFeedback != null,
            enter = fadeIn(MotionTokens.fastEffectsSpec()) + scaleIn(MotionTokens.snappySpring(), initialScale = 0.8f),
            exit = fadeOut(MotionTokens.fastEffectsSpec()) + scaleOut(MotionTokens.fastEffectsSpec(), targetScale = 0.9f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val isNowPlaying = centerToggleFeedback == true
            Surface(
                shape = CircleShape,
                color = Color(0xB8121216),
                contentColor = Color.White,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.22f)),
                modifier = Modifier.size(58.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            if (isNowPlaying) R.drawable.ic_ms_play_arrow else R.drawable.ic_ms_pause
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
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

@Composable
private fun DoubleTapSeekFeedbackPill(
    isForward: Boolean,
    seconds: Int
) {
    Surface(
        shape = ShapeFull,
        color = Color(0xBD121216),
        contentColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (!isForward) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = 180f }
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = 180f }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "-${seconds}s",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    )
                )
            } else {
                Text(
                    text = "+${seconds}s",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
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
