package com.inferno.gallery.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.data.AlbumLayoutMode
import com.inferno.gallery.ui.components.AlbumShapePickerSheet
import com.inferno.gallery.ui.components.ExpressiveFilledIconButton
import com.inferno.gallery.ui.theme.AlbumShapes
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.SpacingTokens

/**
 * Dedicated destination screen displaying all pinned albums.
 * Follows the "No Outlines Policy" and clean Material 3 Expressive styling.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PinnedAlbumsScreen(
    galleryViewModel: GalleryViewModel,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onChangeCover: (String) -> Unit,
    onDeleteCover: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val userPinnedAlbums by galleryViewModel.userPinnedAlbums.collectAsState()
    val albumSortOrder by galleryViewModel.albumSortOrder.collectAsState()
    val albumLayoutMode by galleryViewModel.albumLayoutMode.collectAsState()
    val showAlbumSize by galleryViewModel.showAlbumSize.collectAsState()
    val customCovers by galleryViewModel.albumCustomCovers.collectAsState()
    val customShapes by galleryViewModel.albumCustomShapes.collectAsState()
    var shapePickerBucket by remember { mutableStateOf<AlbumBucket?>(null) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                    ) {
                        Text(
                            text = stringResource(R.string.pinned_albums),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (userPinnedAlbums.isNotEmpty()) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text = userPinnedAlbums.size.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    ExpressiveFilledIconButton(
                        onClick = onBackClick,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                            contentDescription = stringResource(R.string.action_close),
                            modifier = Modifier.size(IconSizeTokens.L)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        if (userPinnedAlbums.isEmpty()) {
            // Empty State
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(SpacingTokens.XL)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(72.dp),
                        shadowElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_pin),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(SpacingTokens.M))
                    Text(
                        text = stringResource(R.string.pinned_albums_empty_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(SpacingTokens.XS))
                    Text(
                        text = stringResource(R.string.pinned_albums_empty_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(SpacingTokens.L))
                    ElevatedButton(
                        onClick = onBackClick,
                        shape = ShapeFull,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(text = stringResource(R.string.action_done))
                    }
                }
            }
        } else {
            val lazyGridState = rememberLazyGridState()

            LazyVerticalGrid(
                columns = if (albumLayoutMode == AlbumLayoutMode.LIST) GridCells.Fixed(1) else GridCells.Fixed(3),
                state = lazyGridState,
                contentPadding = PaddingValues(
                    start = SpacingTokens.L,
                    end = SpacingTokens.L,
                    top = innerPadding.calculateTopPadding() + SpacingTokens.M,
                    bottom = innerPadding.calculateBottomPadding() + SpacingTokens.XXL
                ),
                verticalArrangement = if (albumLayoutMode == AlbumLayoutMode.LIST) {
                    Arrangement.spacedBy(SpacingTokens.S)
                } else {
                    Arrangement.spacedBy(SpacingTokens.L)
                },
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
                modifier = Modifier.fillMaxSize()
            ) {
                // Layout switcher & sort control bar
                item(key = "pinned_albums_controls_bar", span = { GridItemSpan(maxLineSpan) }) {
                    AlbumsControlBar(
                        currentLayout = albumLayoutMode,
                        onLayoutChange = { newLayout ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            galleryViewModel.setAlbumLayoutMode(newLayout)
                        },
                        currentSort = albumSortOrder,
                        onSortChange = { newSort ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            galleryViewModel.setAlbumSortOrder(newSort)
                        }
                    )
                }

                items(
                    items = userPinnedAlbums,
                    key = { "pinned_${it.bucketName}" }
                ) { bucket ->
                    val displayBucketName = when (bucket.bucketName) {
                        "Screenrecordings", "Screenrecords", "ScreenRecord" -> "Screen recordings"
                        else -> bucket.bucketName
                    }
                    var showCardMenu by remember { mutableStateOf(false) }

                    val albumShape = remember(customShapes, bucket.bucketName) {
                        AlbumShapes.getShape(customShapes[bucket.bucketName])
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (albumLayoutMode == AlbumLayoutMode.LIST) {
                            AlbumListRow(
                                bucket = bucket.copy(bucketName = displayBucketName),
                                showAlbumSize = showAlbumSize,
                                customShape = albumShape,
                                isPinned = true,
                                onClick = { onAlbumClick(bucket.bucketName) },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showCardMenu = true
                                }
                            )
                        } else {
                            AlbumCard(
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                bucket = bucket.copy(bucketName = displayBucketName),
                                showAlbumSize = showAlbumSize,
                                customShape = albumShape,
                                isCompact = false,
                                isPinned = true,
                                onClick = { onAlbumClick(bucket.bucketName) },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showCardMenu = true
                                }
                            )
                        }

                        // Context menu for pinned album
                        AlbumDropdownMenu(
                            expanded = showCardMenu,
                            onDismissRequest = { showCardMenu = false },
                            bucketName = bucket.bucketName,
                            isUserPinned = true,
                            hasCustomCover = customCovers.containsKey(bucket.bucketName),
                            hasCustomShape = customShapes.containsKey(bucket.bucketName),
                            onTogglePin = {
                                galleryViewModel.togglePinAlbum(bucket.bucketName) { success ->
                                    if (!success) {
                                        Toast.makeText(context, "Unable to unpin album", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onChangeCover = { onChangeCover(bucket.bucketName) },
                            onDeleteCover = { onDeleteCover(bucket.bucketName) },
                            onChangeShape = { shapePickerBucket = bucket },
                            onResetShape = { galleryViewModel.removeAlbumCustomShape(bucket.bucketName) }
                        )
                    }
                }
            }
        }

        // Custom shape picker sheet
        val pickerBucket = shapePickerBucket
        if (pickerBucket != null) {
            AlbumShapePickerSheet(
                bucket = pickerBucket,
                currentShapeId = customShapes[pickerBucket.bucketName],
                onShapeSelected = { shapeId ->
                    galleryViewModel.setAlbumCustomShape(pickerBucket.bucketName, shapeId)
                    shapePickerBucket = null
                },
                onResetShape = {
                    galleryViewModel.removeAlbumCustomShape(pickerBucket.bucketName)
                    shapePickerBucket = null
                },
                onDismissRequest = {
                    shapePickerBucket = null
                }
            )
        }
    }
}

