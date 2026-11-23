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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferno.gallery.ui.GalleryItem
import com.inferno.gallery.ui.formatDuration
import com.inferno.gallery.ui.theme.ShapeExtraSmall

import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision

private val VideoTextShadow = androidx.compose.ui.graphics.Shadow(
    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f),
    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
    blurRadius = 4f
)

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
    contentScale: ContentScale = ContentScale.Crop,
    isFavorite: Boolean = false,
    targetPxOverride: Int? = null
) {
    val context = LocalContext.current
    val targetDimensionPx = remember(gridCellsCount, targetPxOverride) {
        targetPxOverride ?: thumbnailTargetPx(gridCellsCount)
    }

    // Prefer the system MediaStore content URI for all media items so MediaStoreThumbnailFetcher
    // uses ContentResolver.loadThumbnail (system-cached, hardware-accelerated, cancellable IPC).
    // Only fall back to resolvedUri if item.uri is not a MediaStore content URI (e.g. private vault, file://).
    val modelUri = if (item.uri != android.net.Uri.EMPTY && item.uri.scheme == "content") {
        item.uri
    } else if (item.localExists) {
        item.resolvedUri
    } else {
        item.uri
    }

    val imageRequest = remember(modelUri, item.dateModified, targetDimensionPx) {
        val cacheKey = thumbnailMemoryKey(item.id, targetDimensionPx, item.dateModified)
        ImageRequest.Builder(context)
            .data(modelUri)
            .size(targetDimensionPx, targetDimensionPx)
            .precision(Precision.INEXACT)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
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

    // Outlined video icon + duration text without black overlay box (always Bottom-End; Favorite is on Bottom-Start)
    if (item.isVideo && videoIconVector != null) {
        val durationText = remember(item.durationMs) {
            item.durationMs?.let { formatDuration(it) } ?: "0:00"
        }
        val videoAlignment = Alignment.BottomEnd

        if (gridCellsCount >= 7) {
            // Ultra-dense grid (7-8 columns: ~45-50dp cell): micro video icon glyph
            Box(
                modifier = Modifier
                    .align(videoAlignment)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = videoIconVector,
                    contentDescription = null,
                    modifier = Modifier.size(8.dp),
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        } else if (gridCellsCount == 6) {
            // Dense grid (6 columns: ~58dp cell): duration only with shadow
            Box(
                modifier = Modifier
                    .align(videoAlignment)
                    .padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                Text(
                    text = durationText,
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        shadow = VideoTextShadow
                    )
                )
            }
        } else {
            // Standard / Medium grid (1-5 columns): outlined icon + duration text
            val fontSize = when {
                gridCellsCount <= 3 -> 11.sp
                gridCellsCount == 4 -> 9.5.sp
                else -> 8.5.sp
            }
            val iconSize = when {
                gridCellsCount <= 3 -> 13.dp
                gridCellsCount == 4 -> 11.dp
                else -> 9.5.dp
            }
            val outerPad = when {
                gridCellsCount <= 3 -> 5.dp
                gridCellsCount == 4 -> 4.dp
                else -> 3.dp
            }
            val iconSpacing = when {
                gridCellsCount <= 3 -> 3.dp
                else -> 2.dp
            }

            Row(
                modifier = Modifier
                    .align(videoAlignment)
                    .padding(outerPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(iconSpacing)
            ) {
                Icon(
                    imageVector = videoIconVector,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = androidx.compose.ui.graphics.Color.White
                )
                Text(
                    text = durationText,
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.SemiBold,
                        shadow = VideoTextShadow
                    )
                )
            }
        }
    } else if (!item.isVideo) {
        // RAW / GIF / PANO / Motion Photo badge on Bottom-End (strictly media type, zero collision with Favorite on Bottom-Start)
        val mediaBadge = remember(item.id, item.name, item.path) {
            detectThumbnailMediaBadge(
                id = item.id,
                nameOrPath = item.name.ifBlank { item.path },
                isVideo = false
            )
        }

        if (mediaBadge == ThumbnailMediaBadge.MOTION) {
            val outerPad = when {
                gridCellsCount <= 3 -> 5.dp
                gridCellsCount <= 5 -> 4.dp
                else -> 2.dp
            }
            val circleSize = when {
                gridCellsCount <= 3 -> 20.dp
                gridCellsCount <= 5 -> 17.dp
                else -> 13.dp
            }
            val iconSize = when {
                gridCellsCount <= 3 -> 13.dp
                gridCellsCount <= 5 -> 11.dp
                else -> 8.5.dp
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(outerPad)
                    .size(circleSize)
                    .background(
                        color = androidx.compose.ui.graphics.Color(0x7A0F0F14),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                Icon(
                    imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(
                        com.inferno.gallery.R.drawable.ic_ms_motion_photos_on
                    ),
                    contentDescription = "Motion photo",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        } else if (mediaBadge != null && gridCellsCount < 7) {
            val badgeFontSize = when {
                gridCellsCount <= 3 -> 9.sp
                gridCellsCount == 4 -> 8.sp
                gridCellsCount == 5 -> 7.5.sp
                else -> 6.5.sp
            }
            val badgeOuterPad = when {
                gridCellsCount <= 3 -> 5.dp
                gridCellsCount <= 5 -> 4.dp
                else -> 2.dp
            }
            val badgeHPad = when {
                gridCellsCount <= 3 -> 4.5.dp
                gridCellsCount <= 5 -> 3.5.dp
                else -> 2.5.dp
            }
            val badgeVPad = when {
                gridCellsCount <= 4 -> 1.5.dp
                else -> 1.dp
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(badgeOuterPad)
                    .background(
                        androidx.compose.ui.graphics.Color(0xB00F0F14),
                        ShapeExtraSmall
                    )
                    .padding(horizontal = badgeHPad, vertical = badgeVPad)
            ) {
                Text(
                    text = mediaBadge.name,
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = badgeFontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        lineHeight = badgeFontSize
                    )
                )
            }
        }
    }
}

internal enum class ThumbnailMediaBadge { RAW, GIF, PANO, MOTION }

internal fun detectThumbnailMediaBadge(
    id: String,
    nameOrPath: String,
    isVideo: Boolean
): ThumbnailMediaBadge? {
    if (isVideo) return null
    val fileName = nameOrPath.substringAfterLast('/').substringAfterLast('\\')
    return when {
        fileName.endsWith(".dng", true) || fileName.endsWith(".raw", true) ||
        fileName.endsWith(".cr2", true) || fileName.endsWith(".nef", true) ||
        fileName.endsWith(".arw", true) || fileName.endsWith(".orf", true) ||
        fileName.endsWith(".rw2", true) -> ThumbnailMediaBadge.RAW
        fileName.endsWith(".gif", true) -> ThumbnailMediaBadge.GIF
        fileName.contains("PANO", true) || fileName.contains("PANORAMA", true) -> ThumbnailMediaBadge.PANO
        com.inferno.gallery.data.MotionPhotoHelper.getCachedFastCheck(id) == true ||
        fileName.startsWith("MVIMG_", true) ||
        fileName.contains(".MP.", true) ||
        fileName.contains("_MP.", true) -> ThumbnailMediaBadge.MOTION
        else -> null
    }
}
