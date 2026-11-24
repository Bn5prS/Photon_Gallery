@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.inferno.gallery.ui

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.video.FrameExportFormat
import com.inferno.gallery.ui.video.VideoFrameExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoFrameExtractorScreen(
    videoUri: Uri,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var durationMs by remember(videoUri) { mutableLongStateOf(0L) }
    var currentPositionMs by remember(videoUri) { mutableLongStateOf(0L) }
    var videoWidth by remember(videoUri) { mutableIntStateOf(0) }
    var videoHeight by remember(videoUri) { mutableIntStateOf(0) }
    var detectedFps by remember(videoUri) { mutableFloatStateOf(30f) }

    var isPlaying by remember(videoUri) { mutableStateOf(false) }
    var isScrubbing by remember(videoUri) { mutableStateOf(false) }
    var hasFirstFrame by remember(videoUri) { mutableStateOf(false) }

    var exportFormat by remember { mutableStateOf(FrameExportFormat.JPEG) }
    var isSaving by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var savedCount by remember(videoUri) { mutableIntStateOf(0) }
    var showSavedBanner by remember { mutableStateOf(false) }

    // Preview zoom & pan state so users can inspect focus/sharpness before saving
    var previewScale by remember(videoUri) { mutableFloatStateOf(1f) }
    var previewOffsetX by remember(videoUri) { mutableFloatStateOf(0f) }
    var previewOffsetY by remember(videoUri) { mutableFloatStateOf(0f) }

    // Tactile camera-shutter flash animation on capture
    val shutterFlashAlpha = remember { Animatable(0f) }

    val filmstripBitmaps = remember(videoUri) { mutableStateListOf<Bitmap>() }

    val frameStepMs = remember(detectedFps) {
        (1000.0 / detectedFps.coerceIn(10f, 120f)).roundToLong().coerceIn(8L, 100L)
    }

    // Live hardware-accelerated ExoPlayer for smooth real-time scrubbing & exact frame stepping
    val exoPlayer = remember(videoUri) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)
        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                setSeekParameters(SeekParameters.EXACT)
                volume = 0f // Keep frame extraction silent and focused
                repeatMode = Player.REPEAT_MODE_OFF
                setMediaItem(MediaItem.fromUri(videoUri))
                prepare()
                playWhenReady = false
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (exoPlayer.duration > 0L) {
                        durationMs = exoPlayer.duration
                    }
                    exoPlayer.videoFormat?.frameRate?.let { fr ->
                        if (fr in 10f..240f) detectedFps = fr
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                }
            }

            override fun onRenderedFirstFrame() {
                hasFirstFrame = true
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val rotated = videoSize.unappliedRotationDegrees == 90 || videoSize.unappliedRotationDegrees == 270
                    videoWidth = if (rotated) videoSize.height else videoSize.width
                    videoHeight = if (rotated) videoSize.width else videoSize.height
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            filmstripBitmaps.forEach { if (!it.isRecycled) it.recycle() }
            filmstripBitmaps.clear()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Dedicated background MediaMetadataRetriever (thread-safe, never blocks main thread)
    // to inspect exact metadata (duration/FPS/dimensions) and build a 10-frame filmstrip track.
    LaunchedEffect(videoUri) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                val captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
                val frameCount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toLongOrNull()
                val computedFps = when {
                    captureFps != null && captureFps in 10f..240f -> captureFps
                    frameCount != null && frameCount > 1 && dur > 0L -> {
                        ((frameCount * 1000.0) / dur.toDouble()).toFloat().takeIf { it in 10f..240f }
                    }
                    else -> null
                }

                withContext(Dispatchers.Main) {
                    if (dur > 0L && durationMs <= 0L) durationMs = dur
                    if (w > 0 && h > 0 && videoWidth == 0) {
                        val isRotated = rot == 90 || rot == 270
                        videoWidth = if (isRotated) h else w
                        videoHeight = if (isRotated) w else h
                    }
                    if (computedFps != null) detectedFps = computedFps
                }

                val effectiveDur = dur.takeIf { it > 0L } ?: durationMs
                if (effectiveDur > 0L) {
                    val count = 10
                    val step = effectiveDur / count.coerceAtLeast(1)
                    for (i in 0 until count) {
                        if (!isActive) break
                        val timeUs = (i * step + step / 2).coerceIn(0L, effectiveDur) * 1000L
                        val thumb = try {
                            retriever.getScaledFrameAtTime(
                                timeUs,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                180,
                                180
                            )
                        } catch (_: Throwable) {
                            null
                        }
                        if (thumb != null) {
                            withContext(Dispatchers.Main) {
                                filmstripBitmaps.add(thumb)
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {}
            }
        }
    }

    // Smooth playhead polling while playing
    LaunchedEffect(isPlaying, isScrubbing) {
        while (isPlaying && !isScrubbing) {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            currentPositionMs = if (durationMs > 0L) pos.coerceAtMost(durationMs) else pos
            delay(33L)
        }
    }

    // Auto-dismiss saved confirmation pill
    LaunchedEffect(showSavedBanner, savedCount) {
        if (showSavedBanner) {
            delay(2400L)
            showSavedBanner = false
        }
    }

    var lastScrubSeekRealtime by remember { mutableLongStateOf(0L) }

    fun seekExact(targetMs: Long) {
        val clamped = if (durationMs > 0L) targetMs.coerceIn(0L, durationMs) else targetMs.coerceAtLeast(0L)
        exoPlayer.pause()
        exoPlayer.setSeekParameters(SeekParameters.EXACT)
        exoPlayer.seekTo(clamped)
        currentPositionMs = clamped
    }

    fun scrubLive(targetMs: Long) {
        val clamped = if (durationMs > 0L) targetMs.coerceIn(0L, durationMs) else targetMs.coerceAtLeast(0L)
        currentPositionMs = clamped
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastScrubSeekRealtime >= 36L) {
            lastScrubSeekRealtime = now
            exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            exoPlayer.seekTo(clamped)
        }
    }

    fun togglePlayback() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (isPlaying) {
            exoPlayer.pause()
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
        } else {
            exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            if (exoPlayer.playbackState == Player.STATE_ENDED ||
                (durationMs > 0L && currentPositionMs >= durationMs - 60L)
            ) {
                exoPlayer.seekTo(0L)
                currentPositionMs = 0L
            }
            exoPlayer.play()
        }
    }

    fun saveFrame() {
        if (isSaving || isSharing) return
        val capturePosMs = if (isPlaying) {
            exoPlayer.pause()
            exoPlayer.currentPosition.coerceAtLeast(0L).also { currentPositionMs = it }
        } else {
            currentPositionMs
        }
        isSaving = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        coroutineScope.launch {
            launch {
                shutterFlashAlpha.snapTo(0.32f)
                shutterFlashAlpha.animateTo(0f, animationSpec = tween(220))
            }
            val result = VideoFrameExportHelper.exportFrameAtPosition(
                context = context,
                videoUri = videoUri,
                positionMs = capturePosMs,
                format = exportFormat
            )
            isSaving = false
            if (result.isSuccess) {
                savedCount += 1
                showSavedBanner = true
            } else {
                Toast.makeText(
                    context,
                    resources.getString(R.string.video_frame_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun shareFrame() {
        if (isSaving || isSharing) return
        val capturePosMs = if (isPlaying) {
            exoPlayer.pause()
            exoPlayer.currentPosition.coerceAtLeast(0L).also { currentPositionMs = it }
        } else {
            currentPositionMs
        }
        isSharing = true
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        coroutineScope.launch {
            val result = VideoFrameExportHelper.exportFrameToCacheFile(
                context = context,
                videoUri = videoUri,
                positionMs = capturePosMs,
                format = exportFormat
            )
            isSharing = false
            result.onSuccess { file ->
                try {
                    val shareUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = exportFormat.mimeType
                        putExtra(Intent.EXTRA_STREAM, shareUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, resources.getString(R.string.action_extract_frame))
                    )
                } catch (_: Throwable) {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.video_frame_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Toast.makeText(
                    context,
                    resources.getString(R.string.video_frame_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val metadataSubtitle = remember(videoWidth, videoHeight, detectedFps) {
        val resLabel = when {
            videoWidth <= 0 || videoHeight <= 0 -> null
            maxOf(videoWidth, videoHeight) >= 3840 -> "4K (${videoWidth}×${videoHeight})"
            else -> "${videoWidth}×${videoHeight}"
        }
        val fpsLabel = "${detectedFps.roundToInt()} fps"
        if (resLabel != null) "$resLabel • $fpsLabel" else fpsLabel
    }

    val currentFrameNumber = remember(currentPositionMs, detectedFps) {
        ((currentPositionMs / 1000.0) * detectedFps).roundToLong().coerceAtLeast(0L)
    }

    // ── Studio Screen Layout ──────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                    contentDescription = stringResource(R.string.cd_back),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_extract_frame),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metadataSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Segmented JPG / PNG format pill
            FormatSegmentedPill(
                selectedFormat = exportFormat,
                onSelect = { next ->
                    if (exportFormat != next) {
                        exportFormat = next
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            )
        }

        // ── Live Hardware Video Viewport (Tap to Play/Pause, Pinch/Double-Tap to Inspect Sharpness) ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(ShapeLargeIncreased)
                .background(Color(0xFF0E0E11))
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { togglePlayback() },
                        onDoubleTap = {
                            if (previewScale > 1.05f) {
                                previewScale = 1f
                                previewOffsetX = 0f
                                previewOffsetY = 0f
                            } else {
                                previewScale = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (previewScale * zoom).coerceIn(1f, 4f)
                        previewScale = nextScale
                        if (nextScale <= 1.01f) {
                            previewOffsetX = 0f
                            previewOffsetY = 0f
                        } else {
                            val maxX = (size.width * (nextScale - 1f)) / 2f
                            val maxY = (size.height * (nextScale - 1f)) / 2f
                            previewOffsetX = (previewOffsetX + pan.x).coerceIn(-maxX, maxX)
                            previewOffsetY = (previewOffsetY + pan.y).coerceIn(-maxY, maxY)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val view = android.view.LayoutInflater.from(ctx)
                        .inflate(R.layout.view_video_player, null, false) as PlayerView
                    view.apply {
                        useController = false
                        controllerAutoShow = false
                        hideController()
                        isClickable = false
                        isFocusable = false
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        player = exoPlayer
                    }
                },
                update = { view ->
                    if (view.player != exoPlayer) view.player = exoPlayer
                },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = previewScale
                        scaleY = previewScale
                        translationX = previewOffsetX
                        translationY = previewOffsetY
                    }
            )

            // Loading indicator before first video frame renders
            if (!hasFirstFrame) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Camera shutter flash feedback on Save
            if (shutterFlashAlpha.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = shutterFlashAlpha.value))
                )
            }

            // Floating "Reset Zoom" chip when magnified to inspect sharpness
            androidx.compose.animation.AnimatedVisibility(
                visible = previewScale > 1.05f,
                enter = fadeIn(MotionTokens.fastEffectsSpec()) + scaleIn(MotionTokens.snappySpring()),
                exit = fadeOut(MotionTokens.fastEffectsSpec()) + scaleOut(MotionTokens.fastEffectsSpec()),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Surface(
                    onClick = {
                        previewScale = 1f
                        previewOffsetX = 0f
                        previewOffsetY = 0f
                    },
                    shape = ShapeFull,
                    color = Color(0xCC121216),
                    contentColor = Color.White,
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f×  Reset", previewScale),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Floating Saved Confirmation Pill (allows multi-frame capture without leaving screen)
            androidx.compose.animation.AnimatedVisibility(
                visible = showSavedBanner && savedCount > 0,
                enter = fadeIn(MotionTokens.fastEffectsSpec()) + scaleIn(MotionTokens.snappySpring()),
                exit = fadeOut(MotionTokens.fastEffectsSpec()) + scaleOut(MotionTokens.fastEffectsSpec()),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check_circle),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (savedCount == 1) {
                                "Saved ${exportFormat.label} to Gallery"
                            } else {
                                "Saved $savedCount frames to Gallery"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // ── Bottom Studio Dock (Timecode + Interactive Filmstrip + Precision Transport + Save/Share) ──
        Surface(
            shape = ShapeLargeIncreased,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Timecode & Frame Number Readout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formatTimeCode(currentPositionMs),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "/ ${formatTimeCode(durationMs)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = "Frame $currentFrameNumber",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Interactive Full-Width Filmstrip Scrubber with Playhead Needle
                InteractiveFilmstripScrubber(
                    bitmaps = filmstripBitmaps,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    onScrubStart = {
                        isScrubbing = true
                        exoPlayer.pause()
                    },
                    onScrubPosition = { posMs ->
                        scrubLive(posMs)
                    },
                    onScrubEnd = { posMs ->
                        isScrubbing = false
                        seekExact(posMs)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )

                // Precision Transport & Exact Frame-Step Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NudgePillButton(label = "-1s") {
                        seekExact(currentPositionMs - 1000L)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }

                    NudgePillButton(label = "-1f") {
                        seekExact(currentPositionMs - frameStepMs)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }

                    FilledIconButton(
                        onClick = { togglePlayback() },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (isPlaying) R.drawable.ic_ms_pause else R.drawable.ic_ms_play_arrow
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    NudgePillButton(label = "+1f") {
                        seekExact(currentPositionMs + frameStepMs)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }

                    NudgePillButton(label = "+1s") {
                        seekExact(currentPositionMs + 1000L)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }

                // Primary Save & Share Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { shareFrame() },
                        enabled = !isSaving && !isSharing && hasFirstFrame,
                        shape = ShapeFull,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_share),
                                contentDescription = stringResource(R.string.detail_share),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.detail_share),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    Button(
                        onClick = { saveFrame() },
                        enabled = !isSaving && !isSharing && hasFirstFrame,
                        shape = ShapeFull,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_camera),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (savedCount == 0) {
                                    "Save Frame (${exportFormat.label})"
                                } else {
                                    "Save Another ($savedCount)"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatSegmentedPill(
    selectedFormat: FrameExportFormat,
    onSelect: (FrameExportFormat) -> Unit
) {
    Surface(
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FrameExportFormat.entries.forEach { format ->
                val isSelected = format == selectedFormat
                val bg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = MotionTokens.fastEffectsSpec(),
                    label = "formatPillBg"
                )
                val fg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = MotionTokens.fastEffectsSpec(),
                    label = "formatPillFg"
                )
                Surface(
                    onClick = { onSelect(format) },
                    shape = ShapeFull,
                    color = bg,
                    contentColor = fg
                ) {
                    Text(
                        text = if (format == FrameExportFormat.JPEG) "JPG" else "PNG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveFilmstripScrubber(
    bitmaps: List<Bitmap>,
    currentPositionMs: Long,
    durationMs: Long,
    onScrubStart: () -> Unit,
    onScrubPosition: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit
) {
    val density = LocalDensity.current
    val safeDuration = durationMs.coerceAtLeast(1L)
    val fraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        val trackWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val playheadWidthDp = 4.dp
        val playheadWidthPx = with(density) { playheadWidthDp.toPx() }
        val playheadOffsetPx = ((trackWidthPx - playheadWidthPx) * fraction).roundToInt()

        var dragFraction by remember { mutableFloatStateOf(fraction) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 3.dp)
                .clip(ShapeMedium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    shape = ShapeMedium
                )
                .pointerInput(safeDuration, trackWidthPx) {
                    detectTapGestures { offset ->
                        val f = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        val target = (f * safeDuration).roundToLong()
                        onScrubEnd(target)
                    }
                }
                .pointerInput(safeDuration, trackWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragFraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            onScrubStart()
                            onScrubPosition((dragFraction * safeDuration).roundToLong())
                        },
                        onDragEnd = {
                            onScrubEnd((dragFraction * safeDuration).roundToLong())
                        },
                        onDragCancel = {
                            onScrubEnd((dragFraction * safeDuration).roundToLong())
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragFraction = (dragFraction + dragAmount / trackWidthPx).coerceIn(0f, 1f)
                            onScrubPosition((dragFraction * safeDuration).roundToLong())
                        }
                    )
                }
        ) {
            if (bitmaps.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    bitmaps.forEach { thumb ->
                        if (!thumb.isRecycled) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }

            // Subtle progress overlay before playhead for quick spatial orientation
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            )
        }

        // Crisp vertical playhead needle extending slightly above/below filmstrip
        Box(
            modifier = Modifier
                .offset { IntOffset(x = playheadOffsetPx, y = 0) }
                .width(playheadWidthDp)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(3.dp)
                )
                .border(
                    width = 0.75.dp,
                    color = Color.White.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(3.dp)
                )
        )
    }
}

@Composable
private fun NudgePillButton(
    label: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = ShapeLarge,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            )
        )
    }
}

private fun formatTimeCode(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val millis = safeMs % 1000L
    return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
}
