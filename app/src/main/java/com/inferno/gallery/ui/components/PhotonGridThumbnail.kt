package com.inferno.gallery.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferno.gallery.ui.GalleryItem
import com.inferno.gallery.ui.formatDuration
import com.inferno.gallery.ui.theme.ShapeExtraSmall
import com.inferno.gallery.ui.theme.googleSansFlex

import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision

/**
 * Lightweight, high-performance thumbnail composable for Photon Gallery grids.
 * Routes through Coil 3's unified hardware bitmap pool and memory cache,
 * eliminating redundant unpooled decodes and GC thrashing.
 *
 * Emits directly into the caller's [BoxScope] (no wrapper layout node — at 7-8 columns
 * every saved node × ~100 visible cells is measurable composition/layout time).
 */
@Composable
fun BoxScope.PhotonGridThumbnail(
    item: GalleryItem,
    gridCellsCount: Int,
    videoIconVector: ImageVector?,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val targetDimensionPx = remember(gridCellsCount) { thumbnailTargetPx(gridCellsCount) }

    val imageRequest = remember(item.uri, targetDimensionPx) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(targetDimensionPx, targetDimensionPx)
            .precision(Precision.INEXACT)
            .memoryCacheKey("t_${item.id}@$targetDimensionPx")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = contentScale,
        modifier = Modifier.matchParentSize()
    )

    // Expressive video duration badge — dynamically scaled to grid scale/density
    if (item.isVideo && videoIconVector != null) {
        val durationText = remember(item.durationMs) {
            item.durationMs?.let { formatDuration(it) } ?: "0:00"
        }
        val condensedBadgeFont = remember {
            googleSansFlex(weight = 650, width = 80f, opticalSize = 10f, roundness = 0f)
        }

        if (gridCellsCount >= 7) {
            // Ultra-dense grid (7-8 columns: ~45-50dp cell): micro video icon glyph to preserve thumbnail subject
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.80f),
                        ShapeExtraSmall
                    )
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = videoIconVector,
                    contentDescription = null,
                    modifier = Modifier.size(7.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        } else if (gridCellsCount == 6) {
            // Dense grid (6 columns: ~58dp cell): duration only with ultra-compact padding (no icon)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.80f),
                        ShapeExtraSmall
                    )
                    .padding(horizontal = 2.5.dp, vertical = 0.5.dp)
            ) {
                Text(
                    text = durationText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = condensedBadgeFont,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Standard / Medium grid (1-5 columns): dynamic icon + duration text
            val fontSize = when {
                gridCellsCount <= 3 -> 11.sp
                gridCellsCount == 4 -> 9.sp
                else -> 8.sp
            }
            val iconSize = when {
                gridCellsCount <= 3 -> 9.dp
                gridCellsCount == 4 -> 8.dp
                else -> 7.dp
            }
            val outerPad = when {
                gridCellsCount <= 3 -> 4.dp
                gridCellsCount == 4 -> 3.dp
                else -> 2.5.dp
            }
            val hPad = when {
                gridCellsCount <= 3 -> 4.dp
                gridCellsCount == 4 -> 3.dp
                else -> 2.5.dp
            }
            val vPad = when {
                gridCellsCount <= 3 -> 1.5.dp
                else -> 1.dp
            }
            val iconSpacing = when {
                gridCellsCount <= 3 -> 3.dp
                else -> 2.dp
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(outerPad)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.80f),
                        ShapeExtraSmall
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = hPad, vertical = vPad),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(iconSpacing)
                ) {
                    Icon(
                        imageVector = videoIconVector,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = durationText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = condensedBadgeFont,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    } else if (!item.isVideo) {
        // RAW / GIF / PANO badge — scaled with grid scale
        val badgeText = remember(item.name) {
            when {
                item.name.endsWith(".dng", true) || item.name.endsWith(".raw", true) ||
                item.name.endsWith(".cr2", true) || item.name.endsWith(".nef", true) ||
                item.name.endsWith(".arw", true) -> "RAW"
                item.name.endsWith(".gif", true) -> "GIF"
                item.name.contains("PANO", true) || item.name.contains("PANORAMA", true) -> "PANO"
                else -> null
            }
        }

        if (badgeText != null) {
            val condensedBadgeFont = remember {
                googleSansFlex(weight = 700, width = 80f, opticalSize = 10f, roundness = 0f)
            }
            val badgeFontSize = when {
                gridCellsCount <= 3 -> 9.5.sp
                gridCellsCount == 4 -> 8.5.sp
                gridCellsCount == 5 -> 7.5.sp
                else -> 6.5.sp
            }
            val badgeOuterPad = when {
                gridCellsCount <= 3 -> 4.dp
                gridCellsCount == 4 -> 3.dp
                gridCellsCount == 5 -> 2.5.dp
                else -> 2.dp
            }
            val badgeHPad = when {
                gridCellsCount <= 3 -> 3.5.dp
                gridCellsCount == 4 -> 3.dp
                gridCellsCount == 5 -> 2.dp
                else -> 1.5.dp
            }
            val badgeVPad = when {
                gridCellsCount <= 4 -> 1.dp
                else -> 0.5.dp
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(badgeOuterPad)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.82f),
                        ShapeExtraSmall
                    )
                    .padding(horizontal = badgeHPad, vertical = badgeVPad)
            ) {
                Text(
                    text = badgeText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = condensedBadgeFont,
                    fontSize = badgeFontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
