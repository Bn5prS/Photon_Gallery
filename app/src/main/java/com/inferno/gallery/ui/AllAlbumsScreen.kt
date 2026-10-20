@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.inferno.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import com.inferno.gallery.ui.components.ExpressiveFilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import com.inferno.gallery.data.AlbumLayoutMode
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.theme.AlbumShapes
import com.inferno.gallery.ui.components.AlbumShapePickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAlbumsScreen(
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
    galleryViewModel: GalleryViewModel,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onChangeCover: (String) -> Unit,
    onDeleteCover: (String) -> Unit
) {
    val albums by galleryViewModel.allAlbums.collectAsState()
    val userPinnedAlbums by galleryViewModel.userPinnedAlbums.collectAsState()
    val userPinnedNames by galleryViewModel.userPinnedFolderNames.collectAsState()
    val albumSortOrder by galleryViewModel.albumSortOrder.collectAsState()
    val albumLayoutMode by galleryViewModel.albumLayoutMode.collectAsState()
    val showAlbumSize by galleryViewModel.showAlbumSize.collectAsState()
    val customCovers by galleryViewModel.albumCustomCovers.collectAsState()
    val customShapes by galleryViewModel.albumCustomShapes.collectAsState()
    var shapePickerBucket by remember { mutableStateOf<AlbumBucket?>(null) }
    val haptic = LocalHapticFeedback.current

    val unpinnedAlbums = remember(albums, userPinnedNames) {
        albums.filter { it.bucketName != "Favorites" && it.bucketName != "Trash" && it.bucketName != "All" && it.bucketName !in userPinnedNames && it.itemCount > 0 }
    }

    // Unified albums: user pinned folders first (if any), then all other albums sorted according to selected sort order
    val allDisplayAlbums = remember(userPinnedAlbums, unpinnedAlbums) {
        (userPinnedAlbums + unpinnedAlbums)
            .filter { it.itemCount > 0 }
            .distinctBy { it.bucketName }
    }

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
                            text = "All Albums",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (allDisplayAlbums.isNotEmpty()) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = allDisplayAlbums.size.toString(),
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
                            contentDescription = "Back",
                            modifier = Modifier.size(IconSizeTokens.L)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            key(albumLayoutMode) {
                val gridState = rememberLazyGridState()
                LazyVerticalGrid(
                    columns = if (albumLayoutMode == AlbumLayoutMode.LIST) GridCells.Fixed(1) else GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = SpacingTokens.L,
                        end = SpacingTokens.L,
                        top = paddingValues.calculateTopPadding() + SpacingTokens.M,
                        bottom = paddingValues.calculateBottomPadding() + SpacingTokens.XXL
                    ),
                    verticalArrangement = if (albumLayoutMode == AlbumLayoutMode.LIST) {
                        Arrangement.spacedBy(SpacingTokens.S)
                    } else {
                        Arrangement.spacedBy(SpacingTokens.L)
                    },
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "all_albums_controls_bar", span = { GridItemSpan(maxLineSpan) }) {
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
                        items = allDisplayAlbums,
                        key = { "all_folder_${it.bucketName}" }
                    ) { bucket ->
                val isPinned = bucket.bucketName in userPinnedNames
                val displayBucketName = when (bucket.bucketName) {
                    "Screenrecordings", "Screenrecords", "ScreenRecord" -> "Screen recordings"
                    else -> bucket.bucketName
                }
                val albumShape = remember(customShapes, bucket.bucketName) {
                    AlbumShapes.getShape(customShapes[bucket.bucketName])
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    var showPinMenu by remember { mutableStateOf(false) }
                    if (albumLayoutMode == AlbumLayoutMode.LIST) {
                        AlbumListRow(
                            bucket = bucket.copy(bucketName = displayBucketName),
                            showAlbumSize = showAlbumSize,
                            customShape = albumShape,
                            isPinned = isPinned,
                            onClick = { onAlbumClick(bucket.bucketName) },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showPinMenu = true
                            }
                        )
                    } else {
                        AlbumCard(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            bucket = bucket.copy(bucketName = displayBucketName),
                            showAlbumSize = showAlbumSize,
                            customShape = albumShape,
                            isCompact = true,
                            onClick = { onAlbumClick(bucket.bucketName) },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showPinMenu = true
                            }
                        )
                    }

                    if (isPinned && albumLayoutMode != AlbumLayoutMode.LIST) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(SpacingTokens.XS)
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = showPinMenu,
                        onDismissRequest = { showPinMenu = false },
                        shape = ShapeExtraLarge,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        if (bucket.bucketName != "All") {
                            val isUserPinned = bucket.bucketName in userPinnedNames
                            DropdownMenuItem(
                                text = { Text(if (isUserPinned) "Unpin Album" else "Pin Album", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(if (isUserPinned) R.drawable.ic_ms_folder_off else R.drawable.ic_ms_folder),
                                        contentDescription = null,
                                        modifier = Modifier.size(IconSizeTokens.M)
                                    )
                                },
                                onClick = {
                                    showPinMenu = false
                                    galleryViewModel.togglePinAlbum(bucket.bucketName)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Change Cover", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_image),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                            },
                            onClick = {
                                showPinMenu = false
                                onChangeCover(bucket.bucketName)
                            }
                        )
                        if (customCovers.containsKey(bucket.bucketName)) {
                            DropdownMenuItem(
                                text = { Text("Reset to Default Cover", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                                        contentDescription = null,
                                        modifier = Modifier.size(IconSizeTokens.M)
                                    )
                                },
                                onClick = {
                                    showPinMenu = false
                                    onDeleteCover(bucket.bucketName)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Change Shape", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                            },
                            onClick = {
                                showPinMenu = false
                                shapePickerBucket = bucket
                            }
                        )
                        if (customShapes.containsKey(bucket.bucketName)) {
                            DropdownMenuItem(
                                text = { Text("Reset to Default Shape", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                                        contentDescription = null,
                                        modifier = Modifier.size(IconSizeTokens.M)
                                    )
                                },
                                onClick = {
                                    showPinMenu = false
                                    galleryViewModel.removeAlbumCustomShape(bucket.bucketName)
                                }
                            )
                        }
                    }
                }
            }
        }
        }
        }
    }

    val pickerBucket = shapePickerBucket
    if (pickerBucket != null) {
        AlbumShapePickerSheet(
            bucket = pickerBucket,
            currentShapeId = customShapes[pickerBucket.bucketName],
            onShapeSelected = { shapeId ->
                galleryViewModel.setAlbumCustomShape(pickerBucket.bucketName, shapeId)
            },
            onResetShape = {
                galleryViewModel.removeAlbumCustomShape(pickerBucket.bucketName)
            },
            onDismissRequest = {
                shapePickerBucket = null
            }
        )
    }
}
