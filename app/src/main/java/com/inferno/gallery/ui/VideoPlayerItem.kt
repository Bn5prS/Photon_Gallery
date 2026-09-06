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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import com.inferno.gallery.ui.theme.MotionTokens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun VideoPlayerItem(uri: Uri, isCurrentPage: Boolean, showControls: Boolean, modifier: Modifier = Modifier, onTap: (() -> Unit)? = null) {
    VideoPlayerItemWithResolvedUri(
        uri = uri,
        isCurrentPage = isCurrentPage,
        showControls = showControls,
        modifier = modifier,
        onTap = onTap
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerItemWithResolvedUri(uri: Uri, isCurrentPage: Boolean, showControls: Boolean, modifier: Modifier = Modifier, onTap: (() -> Unit)? = null) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        val builder = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            
        builder.build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var videoDuration by remember { mutableStateOf(0L) }
    var dragPosition by remember { mutableStateOf<Long?>(null) }
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
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(
                onClick = {
                    onTap?.invoke()
                },
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
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
                        player = exoPlayer
                        useController = false // Hide default legacy controller
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
                    playerView.player = exoPlayer
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

        // Center Play/Pause button
        AnimatedVisibility(
            visible = showControls && isCurrentPage,
            enter = fadeIn(MotionTokens.snappySpring()) + scaleIn(MotionTokens.snappySpring()),
            exit = fadeOut(MotionTokens.gentleSpring()) + scaleOut(MotionTokens.gentleSpring()),
            modifier = Modifier.align(Alignment.Center)
        ) {
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
                modifier = Modifier
                    .size(56.dp)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) ImageVector.vectorResource(R.drawable.ic_ms_pause) else ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Bottom Controls Overlay
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
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = com.inferno.gallery.ui.theme.ShapeLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Consume clicks so they don't toggle controller
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(dragPosition ?: currentPosition),
                        style = MaterialTheme.typography.labelMedium,
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
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = formatTime(videoDuration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Audio mute/unmute
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) ImageVector.vectorResource(R.drawable.ic_ms_volume_off) else ImageVector.vectorResource(R.drawable.ic_ms_volume_up),
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
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
