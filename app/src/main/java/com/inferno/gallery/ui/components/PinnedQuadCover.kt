package com.inferno.gallery.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import com.inferno.gallery.R
import com.inferno.gallery.ui.AlbumBucket
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeSmall

/**
 * Renders a responsive collage cover of mini album thumbnails for the Pinned Albums collection.
 * - 4+ albums: 2x2 grid
 * - 3 albums: 1 left half (full height) + 2 right stacked quadrants
 * - 2 albums: 2 vertical halves
 * - 1 album: 1 full cover
 * - 0 albums: fallback icon
 * Strictly adheres to the "No Outlines Policy" (zero borders).
 */
@Composable
fun PinnedQuadCover(
    pinnedAlbums: List<AlbumBucket>,
    modifier: Modifier = Modifier,
    tileShape: Shape = ShapeSmall,
    showPinBadge: Boolean = true
) {
    val covers: List<Uri> = remember(pinnedAlbums) {
        pinnedAlbums.mapNotNull { bucket ->
            val uri = if (bucket.coverUri != Uri.EMPTY) bucket.coverUri else bucket.defaultCoverUri
            if (uri != Uri.EMPTY) uri else null
        }.take(4)
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(tileShape),
        shape = tileShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (covers.size) {
                0 -> {
                    // Empty fallback
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(36.dp),
                            shadowElevation = 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_pin),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                1 -> {
                    MiniThumbnail(uri = covers[0], modifier = Modifier.fillMaxSize())
                }
                2 -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        MiniThumbnail(uri = covers[0], modifier = Modifier.weight(1f).fillMaxHeight())
                        MiniThumbnail(uri = covers[1], modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
                3 -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        MiniThumbnail(uri = covers[0], modifier = Modifier.weight(1f).fillMaxHeight())
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            MiniThumbnail(uri = covers[1], modifier = Modifier.weight(1f).fillMaxWidth())
                            MiniThumbnail(uri = covers[2], modifier = Modifier.weight(1f).fillMaxWidth())
                        }
                    }
                }
                else -> {
                    // 4 items: 2x2 quad
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            MiniThumbnail(uri = covers[0], modifier = Modifier.weight(1f).fillMaxHeight())
                            MiniThumbnail(uri = covers[1], modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            MiniThumbnail(uri = covers[2], modifier = Modifier.weight(1f).fillMaxHeight())
                            MiniThumbnail(uri = covers[3], modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }

            if (showPinBadge) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(22.dp),
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_pin),
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(uri) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(Size(240, 240))
            .precision(Precision.EXACT)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}
