@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.inferno.gallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.SpacingTokens

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
    val showAlbumSize by galleryViewModel.showAlbumSize.collectAsState()
    val customCovers by galleryViewModel.albumCustomCovers.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showSortMenu by remember { mutableStateOf(false) }

    // Display all unpinned albums
    val unpinnedAlbums = albums.filter { it.bucketName != "Favorites" && it.bucketName !in userPinnedNames }

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
                    FilledTonalIconButton(
                        onClick = onBackClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                    Box {
                        FilledTonalIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showSortMenu = true
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_sort),
                                contentDescription = "Sort",
                                modifier = Modifier.size(IconSizeTokens.M)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            shape = ShapeExtraLarge,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            DropdownMenuItem(
                                text = { Text("New to Old", style = MaterialTheme.typography.bodyMedium) },
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
                                text = { Text("Old to New", style = MaterialTheme.typography.bodyMedium) },
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
                                text = { Text("Large to Small", style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = {
                                    RadioButton(
                                        selected = albumSortOrder == SortOrder.BigToSmall,
                                        onClick = null
                                    )
                                },
                                onClick = {
                                    galleryViewModel.setAlbumSortOrder(SortOrder.BigToSmall)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Small to Large", style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = {
                                    RadioButton(
                                        selected = albumSortOrder == SortOrder.SmallToBig,
                                        onClick = null
                                    )
                                },
                                onClick = {
                                    galleryViewModel.setAlbumSortOrder(SortOrder.SmallToBig)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("A - Z", style = MaterialTheme.typography.bodyMedium) },
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
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { paddingValues ->
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
                items = unpinnedAlbums,
                key = { "all_folder_${it.bucketName}" },
                span = { GridItemSpan(4) }
            ) { bucket ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    var showPinMenu by remember { mutableStateOf(false) }
                    AlbumCard(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        bucket = bucket,
                        showAlbumSize = showAlbumSize,
                        onClick = { onAlbumClick(bucket.bucketName) },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showPinMenu = true
                        }
                    )
                    DropdownMenu(
                        expanded = showPinMenu,
                        onDismissRequest = { showPinMenu = false },
                        shape = ShapeExtraLarge,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        val isPinned = bucket.bucketName in userPinnedNames
                        DropdownMenuItem(
                            text = { Text(if (isPinned) "Unpin Album" else "Pin Album", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(if (isPinned) R.drawable.ic_ms_folder_off else R.drawable.ic_ms_folder),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                            },
                            onClick = {
                                showPinMenu = false
                                galleryViewModel.togglePinAlbum(bucket.bucketName)
                            }
                        )
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
                    }
                }
            }
        }
    }
}
