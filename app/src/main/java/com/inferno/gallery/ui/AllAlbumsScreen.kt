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
    val userPinnedNames by galleryViewModel.userPinnedFolderNames.collectAsState()
    val albumSortOrder by galleryViewModel.albumSortOrder.collectAsState()
    val albumLayoutMode by galleryViewModel.albumLayoutMode.collectAsState()
    val showAlbumSize by galleryViewModel.showAlbumSize.collectAsState()
    val customCovers by galleryViewModel.albumCustomCovers.collectAsState()
    val customShapes by galleryViewModel.albumCustomShapes.collectAsState()
    var shapePickerBucket by remember { mutableStateOf<AlbumBucket?>(null) }
    val haptic = LocalHapticFeedback.current

    var showSortMenu by remember { mutableStateOf(false) }

    // Display albums (excluding Favorites, Trash, and All, and excluding empty albums)
    val allDisplayAlbums = albums.filter { it.bucketName != "Favorites" && it.bucketName != "Trash" && it.bucketName != "All" && it.itemCount > 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Albums",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
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
                actions = {
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    galleryViewModel.setAlbumLayoutMode(AlbumLayoutMode.GRID_2_COL)
                                },
                                shape = ShapeFull,
                                color = if (albumLayoutMode == AlbumLayoutMode.GRID_2_COL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = if (albumLayoutMode == AlbumLayoutMode.GRID_2_COL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_grid_view),
                                        contentDescription = "2 Columns",
                                        modifier = Modifier.size(IconSizeTokens.S)
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    galleryViewModel.setAlbumLayoutMode(AlbumLayoutMode.GRID_3_COL)
                                },
                                shape = ShapeFull,
                                color = if (albumLayoutMode == AlbumLayoutMode.GRID_3_COL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = if (albumLayoutMode == AlbumLayoutMode.GRID_3_COL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_dashboard),
                                        contentDescription = "3 Columns Compact",
                                        modifier = Modifier.size(IconSizeTokens.S)
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    galleryViewModel.setAlbumLayoutMode(AlbumLayoutMode.LIST)
                                },
                                shape = ShapeFull,
                                color = if (albumLayoutMode == AlbumLayoutMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = if (albumLayoutMode == AlbumLayoutMode.LIST) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_list),
                                        contentDescription = "List View",
                                        modifier = Modifier.size(IconSizeTokens.S)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(SpacingTokens.XS))

                    Box {
                        ExpressiveFilledIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showSortMenu = true
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_sort),
                                contentDescription = "Sort",
                                modifier = Modifier.size(IconSizeTokens.L)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            shape = ShapeExtraLarge,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            DropdownMenuItem(
                                text = { Text("Name (A to Z)", style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = {
                                    RadioButton(
                                        selected = albumSortOrder == SortOrder.NameAsc,
                                        onClick = null
                                    )
                                },
                                onClick = {
                                    galleryViewModel.setAlbumSortOrder(SortOrder.NameAsc)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z to A)", style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = {
                                    RadioButton(
                                        selected = albumSortOrder == SortOrder.NameDesc,
                                        onClick = null
                                    )
                                },
                                onClick = {
                                    galleryViewModel.setAlbumSortOrder(SortOrder.NameDesc)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date Modified (Newest)", style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = {
                                    RadioButton(
                                        selected = albumSortOrder == SortOrder.NewToOld,
                                        onClick = null
                                    )
                                },
                                onClick = {
                                    galleryViewModel.setAlbumSortOrder(SortOrder.NewToOld)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date Modified (Oldest)", style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = {
                                    RadioButton(
                                        selected = albumSortOrder == SortOrder.OldToNew,
                                        onClick = null
                                    )
                                },
                                onClick = {
                                    galleryViewModel.setAlbumSortOrder(SortOrder.OldToNew)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Item Count (Most items)", style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = {
                                    RadioButton(
                                        selected = albumSortOrder == SortOrder.CountDesc,
                                        onClick = null
                                    )
                                },
                                onClick = {
                                    galleryViewModel.setAlbumSortOrder(SortOrder.CountDesc)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Item Count (Least items)", style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = {
                                    RadioButton(
                                        selected = albumSortOrder == SortOrder.CountAsc,
                                        onClick = null
                                    )
                                },
                                onClick = {
                                    galleryViewModel.setAlbumSortOrder(SortOrder.CountAsc)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(12),
                contentPadding = PaddingValues(
                    start = SpacingTokens.L,
                    end = SpacingTokens.L,
                    top = paddingValues.calculateTopPadding() + SpacingTokens.M,
                    bottom = paddingValues.calculateBottomPadding() + SpacingTokens.XXL
                ),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.L),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
                modifier = Modifier.fillMaxSize()
            ) {
            items(
                items = allDisplayAlbums,
                key = { "all_folder_${it.bucketName}" },
                span = {
                    when (albumLayoutMode) {
                        AlbumLayoutMode.GRID_2_COL -> GridItemSpan(6)
                        AlbumLayoutMode.GRID_3_COL -> GridItemSpan(4)
                        AlbumLayoutMode.LIST -> GridItemSpan(12)
                    }
                }
            ) { bucket ->
                val isPinned = bucket.bucketName in userPinnedNames || bucket.bucketName == "Camera"
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
                            isCompact = (albumLayoutMode == AlbumLayoutMode.GRID_3_COL),
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
                        if (bucket.bucketName != "All" && bucket.bucketName != "Camera") {
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
