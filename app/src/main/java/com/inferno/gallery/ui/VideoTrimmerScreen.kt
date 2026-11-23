@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.inferno.gallery.ui

import android.app.Activity
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.viewinterop.AndroidView
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
import com.inferno.gallery.data.MediaUriUtils
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.WavyProgressTokens
import com.inferno.gallery.ui.video.VideoExportResolution
import com.inferno.gallery.ui.video.VideoTrimEngine
import com.inferno.gallery.ui.video.VideoTrimMath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private enum class ActiveTrimDragTarget { START, END, PLAYHEAD }

private enum class VideoEditorTab { TRIM, SPEED, TRANSFORM, QUALITY }

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoTrimmerScreen(
    videoUri: Uri,
    onBack: () -> Unit,
    onSaved: (savedUri: Uri, isOverwrite: Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var durationMs by remember { mutableLongStateOf(0L) }
    var originalSizeBytes by remember { mutableLongStateOf(0L) }
    var originalName by remember { mutableStateOf<String?>(null) }
    var sourceWidth by remember { mutableIntStateOf(0) }
    var sourceHeight by remember { mutableIntStateOf(0) }
    var sourceFps by remember { mutableFloatStateOf(30f) }

    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(0L) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    // Quick edit states
    var muteAudio by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var rotationTurns by remember { mutableIntStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var selectedResolution by remember { mutableStateOf(VideoExportResolution.ORIGINAL) }
    var activeTab by remember { mutableStateOf(VideoEditorTab.TRIM) }

    val rotationDegreesCw = ((rotationTurns % 4) + 4) % 4 * 90

    var showSaveSheet by remember { mutableStateOf(false) }
    var isTrimming by remember { mutableStateOf(false) }
    var trimProgress by remember { mutableFloatStateOf(0f) }
    var trimJob by remember { mutableStateOf<Job?>(null) }
    var lastLiveSeekWallMs by remember { mutableLongStateOf(0L) }

    val filmstripBitmaps = remember { mutableStateListOf<Bitmap>() }

    // Hardware-accelerated ExoPlayer preview with software decoder fallback
    val exoPlayer = remember(videoUri) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            setSeekParameters(SeekParameters.EXACT)
            prepare()
            playWhenReady = false
        }
    }

    // Intercept back while exporting so users cancel gracefully instead of leaking a job
    BackHandler {
        if (isTrimming) {
            trimJob?.cancel()
            isTrimming = false
        } else {
            onBack()
        }
    }

    // Pause playback when backgrounded
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (sourceWidth <= 0 && videoSize.width > 0 && videoSize.height > 0) {
                    sourceWidth = videoSize.width
                    sourceHeight = videoSize.height
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && exoPlayer.duration > 0L) {
                    val d = exoPlayer.duration
                    if (durationMs <= 0L) {
                        durationMs = d
                        startMs = 0L
                        endMs = d
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            trimJob?.cancel()
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            filmstripBitmaps.forEach { if (!it.isRecycled) it.recycle() }
            filmstripBitmaps.clear()
        }
    }

    LaunchedEffect(muteAudio) {
        exoPlayer.volume = if (muteAudio) 0f else 1f
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Load metadata and filmstrip frames on IO
    LaunchedEffect(videoUri) {
        val meta = VideoTrimEngine.inspectSource(context, videoUri)
        if (meta.durationMs > 0L) {
            durationMs = meta.durationMs
            startMs = 0L
            endMs = meta.durationMs
        }
        originalSizeBytes = meta.sizeBytes
        originalName = meta.displayName
        if (meta.width > 0 && meta.height > 0) {
            sourceWidth = meta.width
            sourceHeight = meta.height
        }
        sourceFps = meta.frameRate

        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val totalDur = durationMs.takeIf { it > 0L }
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: 0L
                if (totalDur > 0L) {
                    withContext(Dispatchers.Main) {
                        if (durationMs <= 0L) {
                            durationMs = totalDur
                            startMs = 0L
                            endMs = totalDur
                        }
                    }
                    val count = 10
                    val step = totalDur / count.coerceAtLeast(1)
                    for (i in 0 until count) {
                        val thumb = try {
                            retriever.getScaledFrameAtTime(
                                (i * step) * 1000L,
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

    // Keep playback looping cleanly within [startMs, endMs]
    LaunchedEffect(exoPlayer, startMs, endMs, isPlaying) {
        while (isActive) {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            currentPosMs = pos
            if (isPlaying && endMs > startMs) {
                if (pos >= endMs - 40L || exoPlayer.playbackState == Player.STATE_ENDED) {
                    exoPlayer.setSeekParameters(SeekParameters.EXACT)
                    exoPlayer.seekTo(startMs)
                    if (exoPlayer.playbackState == Player.STATE_ENDED) {
                        exoPlayer.play()
                    }
                }
            }
            delay(if (isPlaying) 32L else 150L)
        }
    }

    fun seekPreview(targetMs: Long, exact: Boolean) {
        val clamped = targetMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
        currentPosMs = clamped
        val now = System.currentTimeMillis()
        if (exact) {
            exoPlayer.setSeekParameters(SeekParameters.EXACT)
            exoPlayer.seekTo(clamped)
            lastLiveSeekWallMs = now
        } else if (now - lastLiveSeekWallMs >= 33L) {
            exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            exoPlayer.seekTo(clamped)
            lastLiveSeekWallMs = now
        }
    }

    fun togglePlayPause() {
        if (isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.setSeekParameters(SeekParameters.EXACT)
            if (exoPlayer.currentPosition < startMs || exoPlayer.currentPosition >= endMs - 40L) {
                exoPlayer.seekTo(startMs)
            }
            exoPlayer.play()
        }
    }

    fun resetAllEdits() {
        startMs = 0L
        endMs = durationMs
        muteAudio = false
        playbackSpeed = 1.0f
        rotationTurns = 0
        flipHorizontal = false
        flipVertical = false
        selectedResolution = VideoExportResolution.ORIGINAL
        exoPlayer.pause()
        seekPreview(0L, exact = true)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun launchTrim(overwrite: Boolean) {
        if (isTrimming || durationMs <= 0L) return
        val (clampedStart, clampedEnd) = VideoTrimMath.clampRange(startMs, endMs, durationMs)
        exoPlayer.pause()
        showSaveSheet = false
        isTrimming = true
        trimProgress = 0f

        trimJob = coroutineScope.launch {
            val result = try {
                VideoTrimEngine.trimVideo(
                    context = context,
                    sourceUri = videoUri,
                    startMs = clampedStart,
                    endMs = clampedEnd,
                    muteAudio = muteAudio,
                    speed = playbackSpeed,
                    rotationDegreesCw = rotationDegreesCw,
                    flipHorizontal = flipHorizontal,
                    flipVertical = flipVertical,
                    resolution = selectedResolution,
                    sourceWidth = sourceWidth,
                    sourceHeight = sourceHeight,
                    overwrite = overwrite,
                    originalDisplayName = originalName,
                    onProgress = { progress -> trimProgress = progress }
                )
            } catch (ce: CancellationException) {
                isTrimming = false
                return@launch
            }
            isTrimming = false
            result.onSuccess { savedUri ->
                onSaved(savedUri, overwrite)
                val msg = context.getString(
                    if (overwrite) R.string.video_trim_replaced else R.string.video_trim_saved_copy
                )
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onBack()
            }.onFailure { err ->
                Toast.makeText(
                    context,
                    "${context.getString(R.string.video_trim_failed)}: ${err.localizedMessage ?: "error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Android 11+ write permission flow for "Replace Original"
    val overwritePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            launchTrim(overwrite = true)
        }
    }

    fun requestReplaceOriginal() {
        showSaveSheet = false
        if (videoUri.scheme == "content") {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val normalized = MediaUriUtils.normalizeMediaUri(videoUri)
                    val writeIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(normalized))
                    val req = IntentSenderRequest.Builder(writeIntent.intentSender).build()
                    withContext(Dispatchers.Main) {
                        overwritePermissionLauncher.launch(req)
                    }
                } catch (_: Throwable) {
                    withContext(Dispatchers.Main) {
                        launchTrim(overwrite = true)
                    }
                }
            }
        } else {
            launchTrim(overwrite = true)
        }
    }

    val trimmedDurationMs = (endMs - startMs).coerceAtLeast(0L)
    val outputDurationMs = VideoTrimMath.computeOutputDurationMs(trimmedDurationMs, playbackSpeed)
    val targetResolution = VideoTrimMath.computeTargetResolution(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        rotationDegreesCw = rotationDegreesCw,
        preset = selectedResolution
    )
    val estimatedSizeBytes = VideoTrimMath.estimateEditedSizeBytes(
        originalSizeBytes = originalSizeBytes,
        trimmedDurationMs = trimmedDurationMs,
        totalDurationMs = durationMs,
        speed = playbackSpeed,
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        rotationDegreesCw = rotationDegreesCw,
        resolution = selectedResolution,
        muteAudio = muteAudio
    )
    val isTrimChanged = VideoTrimMath.isTrimModified(startMs, endMs, durationMs, muteAudio)
    val isSpeedChanged = abs(playbackSpeed - 1.0f) > 0.01f
    val isTransformChanged = rotationDegreesCw != 0 || flipHorizontal || flipVertical
    val isQualityChanged = targetResolution != null
    val isModified = VideoTrimMath.isEditModified(
        startMs = startMs,
        endMs = endMs,
        totalDurationMs = durationMs,
        muteAudio = muteAudio,
        speed = playbackSpeed,
        rotationDegreesCw = rotationDegreesCw,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical,
        resolution = selectedResolution,
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight
    )

    // Summary badges for active edits (shown in viewport pill & save sheet)
    val activeEditSummary = remember(
        playbackSpeed, rotationDegreesCw, flipHorizontal, flipVertical, targetResolution, selectedResolution, muteAudio
    ) {
        buildList {
            if (abs(playbackSpeed - 1.0f) > 0.01f) add(VideoTrimMath.formatSpeedLabel(playbackSpeed))
            if (rotationDegreesCw != 0) add("${rotationDegreesCw}°")
            if (flipHorizontal) add("Flip H")
            if (flipVertical) add("Flip V")
            if (targetResolution != null) add(selectedResolution.label)
            if (muteAudio) add("Muted")
        }
    }

    val subtitleText = remember(sourceWidth, sourceHeight, sourceFps, targetResolution) {
        if (sourceWidth > 0 && sourceHeight > 0) {
            val fpsStr = "${sourceFps.roundToInt()} fps"
            if (targetResolution != null) {
                "${targetResolution.first}×${targetResolution.second} · $fpsStr"
            } else {
                "${sourceWidth}×${sourceHeight} · $fpsStr"
            }
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        if (!isTrimming) onBack()
                    },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.video_trim_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitleText != null) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                ElevatedButton(
                    onClick = { showSaveSheet = true },
                    enabled = durationMs > 0L && !isTrimming && isModified,
                    shape = ShapeFull,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_save),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // ── Live Video Viewport (with real-time Rotation, Flip & Speed) ───
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(ShapeLargeIncreased)
                    .clipToBounds()
                    .background(Color.Black)
                    .clickable { togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                val videoAspect = if (sourceWidth > 0 && sourceHeight > 0) {
                    sourceWidth.toFloat() / sourceHeight.toFloat()
                } else {
                    16f / 9f
                }

                val targetFitScale = remember(boxW, boxH, videoAspect, rotationDegreesCw) {
                    if (rotationDegreesCw == 90 || rotationDegreesCw == 270) {
                        val unrotW = min(boxW, boxH * videoAspect)
                        val unrotH = (unrotW / videoAspect).coerceAtLeast(1f)
                        min(boxW / unrotH, boxH / unrotW)
                    } else {
                        1f
                    }
                }

                val animatedRotation by animateFloatAsState(
                    targetValue = rotationTurns * 90f,
                    animationSpec = MotionTokens.snappySpring(),
                    label = "video_trim_rotation"
                )
                val animatedFitScale by animateFloatAsState(
                    targetValue = targetFitScale,
                    animationSpec = MotionTokens.snappySpring(),
                    label = "video_trim_fit_scale"
                )
                val animatedFlipX by animateFloatAsState(
                    targetValue = if (flipHorizontal) -1f else 1f,
                    animationSpec = MotionTokens.snappySpring(),
                    label = "video_trim_flip_x"
                )
                val animatedFlipY by animateFloatAsState(
                    targetValue = if (flipVertical) -1f else 1f,
                    animationSpec = MotionTokens.snappySpring(),
                    label = "video_trim_flip_y"
                )

                AndroidView(
                    factory = { ctx ->
                        val view = android.view.LayoutInflater.from(ctx)
                            .inflate(R.layout.view_video_player, null, false) as PlayerView
                        view.apply {
                            useController = false
                            controllerAutoShow = false
                            hideController()
                            player = exoPlayer
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    update = { view ->
                        if (view.player != exoPlayer) view.player = exoPlayer
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = animatedFitScale * animatedFlipX
                            scaleY = animatedFitScale * animatedFlipY
                            rotationZ = animatedRotation
                        }
                )

                // Top floating status chips: Active edits summary + Reset button
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = activeEditSummary.isNotEmpty() && !isTrimming,
                        enter = fadeIn(MotionTokens.fastEffectsSpec()),
                        exit = fadeOut(MotionTokens.fastEffectsSpec())
                    ) {
                        Surface(
                            shape = ShapeFull,
                            color = Color(0xB8121216),
                            contentColor = Color.White,
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = activeEditSummary.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isModified && !isTrimming,
                        enter = fadeIn(MotionTokens.fastEffectsSpec()),
                        exit = fadeOut(MotionTokens.fastEffectsSpec())
                    ) {
                        Surface(
                            shape = ShapeFull,
                            color = Color(0xB8121216),
                            contentColor = Color.White,
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier
                                .clip(ShapeFull)
                                .clickable { resetAllEdits() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.video_trim_reset),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                // Play indicator overlay when paused
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isPlaying && !isTrimming,
                    enter = fadeIn(MotionTokens.fastEffectsSpec()),
                    exit = fadeOut(MotionTokens.fastEffectsSpec())
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            // ── Studio Control Dock ───────────────────────────────────────────
            Surface(
                shape = ShapeExtraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Range & Output Duration / Estimated Size Telemetry Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${VideoTrimMath.formatTrimTime(startMs)} – ${VideoTrimMath.formatTrimTime(endMs)}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            val sizeLabel = if (estimatedSizeBytes > 0L) {
                                " · ~${Formatter.formatShortFileSize(context, estimatedSizeBytes)}"
                            } else ""
                            Text(
                                text = "${VideoTrimMath.formatTrimTime(outputDurationMs)}$sizeLabel",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Dual-Handle Filmstrip Timeline Scrubber (with smooth live scrub + exact snap)
                    TrimRangeFilmstripBar(
                        filmstripBitmaps = filmstripBitmaps,
                        durationMs = durationMs,
                        startMs = startMs,
                        endMs = endMs,
                        playheadMs = currentPosMs.coerceIn(startMs, endMs.coerceAtLeast(startMs)),
                        onStartChanged = { nextStart ->
                            startMs = nextStart
                            exoPlayer.pause()
                            seekPreview(nextStart, exact = false)
                        },
                        onEndChanged = { nextEnd ->
                            endMs = nextEnd
                            exoPlayer.pause()
                            seekPreview(nextEnd, exact = false)
                        },
                        onPlayheadScrub = { nextPos ->
                            exoPlayer.pause()
                            seekPreview(nextPos, exact = false)
                        },
                        onScrubFinished = { finalPos ->
                            seekPreview(finalPos, exact = true)
                        }
                    )

                    // Active Tool Control Panel
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn(MotionTokens.fastEffectsSpec()) togetherWith fadeOut(MotionTokens.fastEffectsSpec())
                        },
                        label = "video_editor_tab_content"
                    ) { tab ->
                        when (tab) {
                            VideoEditorTab.TRIM -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // [ In (Set Start at Playhead)
                                    CompactActionChip(
                                        label = stringResource(R.string.video_trim_set_start),
                                        enabled = durationMs > 0L && currentPosMs < endMs - VideoTrimMath.MIN_TRIM_DURATION_MS,
                                        onClick = {
                                            val next = VideoTrimMath.dragStartHandle(currentPosMs, endMs, durationMs)
                                            startMs = next
                                            exoPlayer.pause()
                                            seekPreview(next, exact = true)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )

                                    // -0.1s fine nudge
                                    CompactNudgeChip(
                                        label = "-0.1s",
                                        enabled = durationMs > 0L,
                                        onClick = {
                                            exoPlayer.pause()
                                            val next = (currentPosMs - 100L).coerceIn(startMs, endMs.coerceAtLeast(startMs))
                                            seekPreview(next, exact = true)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )

                                    // Play / Pause
                                    FilledIconButton(
                                        onClick = { togglePlayPause() },
                                        shape = CircleShape,
                                        modifier = Modifier.size(44.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(
                                                if (isPlaying) R.drawable.ic_ms_pause else R.drawable.ic_ms_play_arrow
                                            ),
                                            contentDescription = if (isPlaying) "Pause" else "Play"
                                        )
                                    }

                                    // +0.1s fine nudge
                                    CompactNudgeChip(
                                        label = "+0.1s",
                                        enabled = durationMs > 0L,
                                        onClick = {
                                            exoPlayer.pause()
                                            val next = (currentPosMs + 100L).coerceIn(startMs, endMs.coerceAtLeast(startMs))
                                            seekPreview(next, exact = true)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )

                                    // Out ] (Set End at Playhead)
                                    CompactActionChip(
                                        label = stringResource(R.string.video_trim_set_end),
                                        enabled = durationMs > 0L && currentPosMs > startMs + VideoTrimMath.MIN_TRIM_DURATION_MS,
                                        onClick = {
                                            val next = VideoTrimMath.dragEndHandle(startMs, currentPosMs, durationMs)
                                            endMs = next
                                            exoPlayer.pause()
                                            seekPreview(next, exact = true)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )

                                    // Mute / Sound toggle
                                    FilledTonalIconButton(
                                        onClick = {
                                            muteAudio = !muteAudio
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.size(40.dp),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = if (muteAudio) MaterialTheme.colorScheme.errorContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = if (muteAudio) MaterialTheme.colorScheme.onErrorContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(
                                                if (muteAudio) R.drawable.ic_ms_volume_off else R.drawable.ic_ms_volume_up
                                            ),
                                            contentDescription = if (muteAudio) "Audio muted" else "Audio on",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            VideoEditorTab.SPEED -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    VideoTrimMath.SPEED_PRESETS.forEach { preset ->
                                        val selected = abs(playbackSpeed - preset) < 0.01f
                                        SelectableToolPill(
                                            label = VideoTrimMath.formatSpeedLabel(preset),
                                            selected = selected,
                                            enabled = durationMs > 0L,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                playbackSpeed = preset
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        )
                                    }
                                }
                            }

                            VideoEditorTab.TRANSFORM -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TransformActionPill(
                                        iconRes = R.drawable.ic_ms_rotate_right,
                                        label = if (rotationDegreesCw == 0) {
                                            stringResource(R.string.video_trim_rotate)
                                        } else {
                                            "${rotationDegreesCw}°"
                                        },
                                        selected = rotationDegreesCw != 0,
                                        modifier = Modifier.weight(1.15f),
                                        onClick = {
                                            rotationTurns += 1
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )

                                    TransformActionPill(
                                        iconRes = R.drawable.ic_ms_flip,
                                        label = stringResource(R.string.video_trim_flip_h),
                                        selected = flipHorizontal,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            flipHorizontal = !flipHorizontal
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )

                                    TransformActionPill(
                                        iconRes = R.drawable.ic_ms_flip,
                                        iconRotation = 90f,
                                        label = stringResource(R.string.video_trim_flip_v),
                                        selected = flipVertical,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            flipVertical = !flipVertical
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )
                                }
                            }

                            VideoEditorTab.QUALITY -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    VideoExportResolution.entries.forEach { preset ->
                                        val supported = VideoTrimMath.canApplyResolution(sourceWidth, sourceHeight, preset)
                                        val selected = selectedResolution == preset
                                        SelectableToolPill(
                                            label = if (preset == VideoExportResolution.ORIGINAL) {
                                                stringResource(R.string.video_trim_original_quality)
                                            } else {
                                                preset.label
                                            },
                                            selected = selected,
                                            enabled = supported && durationMs > 0L,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                selectedResolution = preset
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4-Tab Studio Switcher (Trim | Speed | Transform | Quality)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EditorTabPill(
                                label = stringResource(R.string.video_trim_tab_trim),
                                iconRes = R.drawable.ic_ms_movie,
                                selected = activeTab == VideoEditorTab.TRIM,
                                hasActiveEdit = isTrimChanged,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    activeTab = VideoEditorTab.TRIM
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                            EditorTabPill(
                                label = stringResource(R.string.video_trim_tab_speed),
                                iconRes = R.drawable.ic_ms_speed,
                                selected = activeTab == VideoEditorTab.SPEED,
                                hasActiveEdit = isSpeedChanged,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    activeTab = VideoEditorTab.SPEED
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                            EditorTabPill(
                                label = stringResource(R.string.video_trim_tab_transform),
                                iconRes = R.drawable.ic_ms_rotate_right,
                                selected = activeTab == VideoEditorTab.TRANSFORM,
                                hasActiveEdit = isTransformChanged,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    activeTab = VideoEditorTab.TRANSFORM
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                            EditorTabPill(
                                label = stringResource(R.string.video_trim_tab_quality),
                                iconRes = R.drawable.ic_ms_tune,
                                selected = activeTab == VideoEditorTab.QUALITY,
                                hasActiveEdit = isQualityChanged,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    activeTab = VideoEditorTab.QUALITY
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Exporting Progress Overlay ────────────────────────────────────────
        androidx.compose.animation.AnimatedVisibility(
            visible = isTrimming,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = ShapeExtraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.video_trim_exporting),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LinearWavyProgressIndicator(
                            progress = { trimProgress.coerceIn(0f, 1f) },
                            wavelength = WavyProgressTokens.LinearWavelength,
                            waveSpeed = WavyProgressTokens.CalmWaveSpeed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        )
                        Text(
                            text = "${(trimProgress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = {
                                trimJob?.cancel()
                                isTrimming = false
                            },
                            shape = ShapeFull
                        ) {
                            Text(text = stringResource(R.string.action_cancel))
                        }
                    }
                }
            }
        }
    }

    // ── Save Options Bottom Sheet (Save as Copy vs Replace Original) ─────────
    if (showSaveSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSaveSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.video_trim_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val exportMeta = buildList {
                        add(VideoTrimMath.formatTrimTime(outputDurationMs))
                        if (targetResolution != null) add(selectedResolution.label)
                        if (isSpeedChanged) add(VideoTrimMath.formatSpeedLabel(playbackSpeed))
                        if (rotationDegreesCw != 0) add("${rotationDegreesCw}°")
                        if (flipHorizontal) add("Flip H")
                        if (flipVertical) add("Flip V")
                        if (muteAudio) add("Muted")
                        if (estimatedSizeBytes > 0L) add("~${Formatter.formatShortFileSize(context, estimatedSizeBytes)}")
                    }.joinToString(" · ")
                    Text(
                        text = exportMeta,
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = ShapeLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeLarge)
                        .clickable { launchTrim(overwrite = false) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_content_copy),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.video_trim_save_copy),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.video_trim_save_copy_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Surface(
                    shape = ShapeLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeLarge)
                        .clickable { requestReplaceOriginal() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_save),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.video_trim_replace_original),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.video_trim_replace_original_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun CompactActionChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.42f)
            .clip(ShapeFull)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun CompactNudgeChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.42f)
            .clip(ShapeFull)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun SelectableToolPill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "pill_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        label = "pill_fg"
    )
    Surface(
        shape = ShapeFull,
        color = bg,
        contentColor = fg,
        modifier = modifier
            .height(40.dp)
            .alpha(if (enabled) 1f else 0.36f)
            .clip(ShapeFull)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TransformActionPill(
    iconRes: Int,
    label: String,
    selected: Boolean,
    iconRotation: Float = 0f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "transform_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        label = "transform_fg"
    )
    Surface(
        shape = ShapeFull,
        color = bg,
        contentColor = fg,
        modifier = modifier
            .height(40.dp)
            .clip(ShapeFull)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = iconRotation }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditorTabPill(
    label: String,
    iconRes: Int,
    selected: Boolean,
    hasActiveEdit: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "tab_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tab_fg"
    )
    val pillShape = RoundedCornerShape(18.dp)
    Surface(
        shape = pillShape,
        color = bg,
        contentColor = fg,
        modifier = modifier
            .clip(pillShape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box {
                Icon(
                    imageVector = ImageVector.vectorResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                if (hasActiveEdit) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-2).dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrimRangeFilmstripBar(
    filmstripBitmaps: List<Bitmap>,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    onStartChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit,
    onPlayheadScrub: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val handleWidthDp = 18.dp
    val barHeightDp = 56.dp

    val safeDuration = durationMs.coerceAtLeast(1L)
    val startFraction = (startMs.toFloat() / safeDuration).coerceIn(0f, 1f)
    val endFraction = (endMs.toFloat() / safeDuration).coerceIn(startFraction, 1f)
    val playheadFraction = (playheadMs.toFloat() / safeDuration).coerceIn(startFraction, endFraction)

    var latestStartMs by remember { mutableLongStateOf(startMs) }
    var latestEndMs by remember { mutableLongStateOf(endMs) }
    var latestPlayheadMs by remember { mutableLongStateOf(playheadMs) }
    latestStartMs = startMs
    latestEndMs = endMs
    latestPlayheadMs = playheadMs

    var activeTarget by remember { mutableStateOf<ActiveTrimDragTarget?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Active scrub timecode badge (visible while dragging a handle or playhead)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeightDp)
        ) {
            val totalWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val handleWidthPx = with(density) { handleWidthDp.toPx() }
            val usableWidthPx = (totalWidthPx - handleWidthPx * 2f).coerceAtLeast(1f)

            val startLeftPx = handleWidthPx + startFraction * usableWidthPx
            val endRightPx = handleWidthPx + endFraction * usableWidthPx
            val playheadPx = handleWidthPx + playheadFraction * usableWidthPx

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(durationMs) {
                        detectTapGestures { offset ->
                            if (durationMs <= 0L) return@detectTapGestures
                            val rel = ((offset.x - handleWidthPx) / usableWidthPx).coerceIn(0f, 1f)
                            val tappedMs = (rel * durationMs).toLong().coerceIn(latestStartMs, latestEndMs)
                            onPlayheadScrub(tappedMs)
                            onScrubFinished(tappedMs)
                        }
                    }
                    .pointerInput(durationMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                if (durationMs <= 0L) return@detectHorizontalDragGestures
                                val currentStartX = handleWidthPx + (latestStartMs.toFloat() / safeDuration) * usableWidthPx
                                val currentEndX = handleWidthPx + (latestEndMs.toFloat() / safeDuration) * usableWidthPx
                                val touchSlopPx = handleWidthPx * 1.9f
                                val distStart = abs(offset.x - currentStartX)
                                val distEnd = abs(offset.x - currentEndX)
                                activeTarget = when {
                                    distStart <= touchSlopPx && distStart <= distEnd -> ActiveTrimDragTarget.START
                                    distEnd <= touchSlopPx -> ActiveTrimDragTarget.END
                                    else -> ActiveTrimDragTarget.PLAYHEAD
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDragEnd = {
                                val finalMs = when (activeTarget) {
                                    ActiveTrimDragTarget.START -> latestStartMs
                                    ActiveTrimDragTarget.END -> latestEndMs
                                    ActiveTrimDragTarget.PLAYHEAD -> latestPlayheadMs
                                    null -> latestPlayheadMs
                                }
                                activeTarget = null
                                onScrubFinished(finalMs)
                            },
                            onDragCancel = {
                                activeTarget = null
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                if (durationMs <= 0L) return@detectHorizontalDragGestures
                                val deltaMs = ((dragAmount / usableWidthPx) * durationMs).toLong()
                                when (activeTarget) {
                                    ActiveTrimDragTarget.START -> {
                                        val next = VideoTrimMath.dragStartHandle(
                                            newStartMs = latestStartMs + deltaMs,
                                            currentEndMs = latestEndMs,
                                            totalDurationMs = durationMs
                                        )
                                        if (next != latestStartMs) {
                                            latestStartMs = next
                                            onStartChanged(next)
                                        }
                                    }
                                    ActiveTrimDragTarget.END -> {
                                        val next = VideoTrimMath.dragEndHandle(
                                            currentStartMs = latestStartMs,
                                            newEndMs = latestEndMs + deltaMs,
                                            totalDurationMs = durationMs
                                        )
                                        if (next != latestEndMs) {
                                            latestEndMs = next
                                            onEndChanged(next)
                                        }
                                    }
                                    ActiveTrimDragTarget.PLAYHEAD -> {
                                        val next = (latestPlayheadMs + deltaMs).coerceIn(latestStartMs, latestEndMs)
                                        if (next != latestPlayheadMs) {
                                            latestPlayheadMs = next
                                            onPlayheadScrub(next)
                                        }
                                    }
                                    null -> Unit
                                }
                            }
                        )
                    }
            ) {
                // Filmstrip thumbnail strip inset by handleWidthDp on both sides
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = handleWidthDp, vertical = 4.dp)
                        .clip(ShapeMedium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (filmstripBitmaps.isNotEmpty()) {
                            filmstripBitmaps.forEach { bmp ->
                                if (!bmp.isRecycled) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
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

                    // Excluded left scrim
                    if (startFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(startFraction)
                                .align(Alignment.CenterStart)
                                .background(Color.Black.copy(alpha = 0.62f))
                        )
                    }

                    // Excluded right scrim
                    if (endFraction < 1f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((1f - endFraction).coerceIn(0f, 1f))
                                .align(Alignment.CenterEnd)
                                .background(Color.Black.copy(alpha = 0.62f))
                        )
                    }
                }

                // Active selection top/bottom border bracket
                val bracketWidthDp = with(density) { (endRightPx - startLeftPx).coerceAtLeast(0f).toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(startLeftPx.roundToInt(), 0) }
                        .width(bracketWidthDp)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                // Playhead vertical line
                Box(
                    modifier = Modifier
                        .offset { IntOffset((playheadPx - with(density) { 1.5.dp.toPx() }).roundToInt(), 0) }
                        .width(3.dp)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )

                // Start drag handle (left pill)
                Surface(
                    shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 3.dp, bottomEnd = 3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .offset { IntOffset((startLeftPx - handleWidthPx).roundToInt(), 0) }
                        .width(handleWidthDp)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }

                // End drag handle (right pill)
                Surface(
                    shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .offset { IntOffset(endRightPx.roundToInt(), 0) }
                        .width(handleWidthDp)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }
            }
        }
    }
}
