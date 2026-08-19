package com.inferno.gallery.ui

import androidx.compose.material3.FilledTonalIconButton
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .clickable(
                onClick = {
                    onTap?.invoke()
                },
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Hide default legacy controller
                }
            },
            modifier = Modifier.fillMaxSize()
        )

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
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp)
        ) {
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Consume clicks so they don't toggle controller
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Slider / Timeline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(dragPosition ?: currentPosition),
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
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
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                activeTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        Text(
                            text = formatTime(videoDuration),
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Audio mute/unmute
                        androidx.compose.material3.IconButton(
                            onClick = { isMuted = !isMuted }
                        ) {
                            Icon(
                                imageVector = if (isMuted) ImageVector.vectorResource(R.drawable.ic_ms_volume_off) else ImageVector.vectorResource(R.drawable.ic_ms_volume_up),
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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
