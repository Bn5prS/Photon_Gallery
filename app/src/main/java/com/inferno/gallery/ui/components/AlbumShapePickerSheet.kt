@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.inferno.gallery.ui.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import com.inferno.gallery.R
import com.inferno.gallery.ui.AlbumBucket
import com.inferno.gallery.ui.theme.AlbumShapes
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale

/**
 * Material 3 Expressive BottomSheet for picking custom album card shapes.
 */
@Composable
fun AlbumShapePickerSheet(
    bucket: AlbumBucket,
    currentShapeId: String?,
    onShapeSelected: (String) -> Unit,
    onResetShape: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var selectedShapeId by remember(currentShapeId) {
        mutableStateOf(currentShapeId ?: AlbumShapes.DEFAULT_ID)
    }

    val selectedShape = remember(selectedShapeId) {
        AlbumShapes.getShape(selectedShapeId)
    }

    val activeItem = remember(selectedShapeId) {
        AlbumShapes.items.firstOrNull { it.id == selectedShapeId } ?: AlbumShapes.items.first()
    }

    var loadFailed by remember(bucket.coverUri) { mutableStateOf(false) }
    val effectiveUri = if (loadFailed && bucket.defaultCoverUri != Uri.EMPTY) {
        bucket.defaultCoverUri
    } else {
        bucket.coverUri
    }

    val imageRequest = remember<ImageRequest>(effectiveUri) {
        ImageRequest.Builder(context)
            .data(effectiveUri)
            .size(Size(320, 320))
            .precision(Precision.EXACT)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = ShapeEdgeTop,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.L)
                .padding(bottom = SpacingTokens.XL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Album Shape",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = bucket.bucketName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSizeTokens.L)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            // ── Live Interactive Preview ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(pressedScale = 0.98f),
                shape = ShapeLargeIncreased,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SpacingTokens.L),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Preview container using selected shape
                    Surface(
                        modifier = Modifier.size(108.dp),
                        shape = selectedShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shadowElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (effectiveUri == Uri.EMPTY) {
                                Surface(
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_album),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(IconSizeTokens.M)
                                        )
                                    }
                                }
                            } else {
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = bucket.bucketName,
                                    contentScale = ContentScale.Crop,
                                    onError = {
                                        if (!loadFailed && bucket.defaultCoverUri != Uri.EMPTY && bucket.defaultCoverUri != bucket.coverUri) {
                                            loadFailed = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(selectedShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(SpacingTokens.S))

                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = 2.dp)
                    ) {
                        Text(
                            text = activeItem.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // ── Material 3 Expressive Shapes Catalog Grid ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                contentPadding = PaddingValues(2.dp)
            ) {
                items(AlbumShapes.items, key = { it.id }) { item ->
                    val isSelected = item.id == selectedShapeId

                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        animationSpec = spring(),
                        label = "shape_item_bg"
                    )

                    val outlineBorder = if (isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.92f)
                            .pressScale(pressedScale = 0.94f)
                            .clip(ShapeMedium)
                            .clickable {
                                if (selectedShapeId != item.id) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedShapeId = item.id
                                    onShapeSelected(item.id)
                                }
                            },
                        shape = ShapeMedium,
                        color = containerColor,
                        border = outlineBorder,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(SpacingTokens.XS),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(item.shape)
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                        }
                                    )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = item.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            // ── Bottom Action Controls ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedShapeId != AlbumShapes.DEFAULT_ID) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedShapeId = AlbumShapes.DEFAULT_ID
                            onResetShape()
                        },
                        modifier = Modifier.weight(1f),
                        shape = ShapeFull
                    ) {
                        Text(
                            text = "Reset Shape",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    shape = ShapeFull
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
