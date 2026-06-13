package com.inferno.gallery.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayerItem(uri: Uri, isCurrentPage: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var resolvedPlayUri by remember(uri) { mutableStateOf<Uri?>(null) }
    var isLoadingUrl by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri) {
        if (uri.scheme == "telegram") {
            isLoadingUrl = true
            try {
                val fileId = uri.host
                if (fileId != null) {
                    val settings = com.inferno.gallery.data.SettingsRepository(context)
                    val botTokens = settings.telegramBotTokensFlow.first()
                    if (botTokens.isNotEmpty()) {
                        val client = com.inferno.gallery.data.network.TelegramClient(botTokens.first(), "")
                        val httpUrl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            client.getFileUrl(fileId)
                        }
                        resolvedPlayUri = Uri.parse(httpUrl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingUrl = false
            }
        } else {
            resolvedPlayUri = uri
        }
    }

    if (resolvedPlayUri != null) {
        VideoPlayerItemWithResolvedUri(
            uri = resolvedPlayUri!!,
            isCurrentPage = isCurrentPage,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingUrl) {
                com.inferno.gallery.ui.components.WavyProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun VideoPlayerItemWithResolvedUri(uri: Uri, isCurrentPage: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            
        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
            }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var videoDuration by remember { mutableStateOf(0L) }
    var dragPosition by remember { mutableStateOf<Long?>(null) }
    val settings = remember { com.inferno.gallery.data.SettingsRepository(context) }
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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Control Overlay
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 4.dp,
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time Text
                    Text(
                        text = "${formatDuration(dragPosition ?: currentPosition)} / ${formatDuration(videoDuration)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // M3 Slider
                    Slider(
                        value = (dragPosition ?: currentPosition).toFloat(),
                        onValueChange = { dragPosition = it.toLong() },
                        onValueChangeFinished = { 
                            dragPosition?.let { exoPlayer.seekTo(it) }
                            dragPosition = null 
                        },
                        valueRange = 0f..videoDuration.coerceAtLeast(1L).toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    
                    // Mute Toggle
                    IconButton(onClick = { isMuted = !isMuted }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
