package com.inferno.gallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.carousel.CarouselItemScope
import com.inferno.gallery.ui.components.WavyProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import com.inferno.gallery.ui.theme.AppShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import coil3.gif.repeatCount
import coil3.video.videoFrameMillis
@Composable
fun AlbumsScreen(
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onAlbumClick: (String) -> Unit = {},

) {
    val albums by viewModel.allAlbums.collectAsState()
    val pinnedAlbums by viewModel.pinnedAlbums.collectAsState()
    val albumSortOrder by viewModel.albumSortOrder.collectAsState()
    val allMedia by viewModel.images.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val gridAutoPlay by viewModel.gridAutoPlay.collectAsState()
    
    var showSortMenu by remember { mutableStateOf(false) }
    
    val favoriteItems = remember(allMedia, favoriteIds) {
        allMedia.filter { favoriteIds.contains(it.id) }
    }




    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        if (favoriteItems.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text("Favorites", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                    HorizontalUncontainedCarousel(
                        state = rememberCarouselState { favoriteItems.size },
                        itemWidth = 160.dp,
                        itemSpacing = 8.dp,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    ) { i ->
                        val item = favoriteItems[i]
                        Box(modifier = Modifier.fillMaxSize().clickable { onAlbumClick("All") }) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.uri)
                                    .size(300, 300)
                                    .crossfade(true)
                                    .apply {
                                        if (!gridAutoPlay) {
                                            repeatCount(0)
                                            videoFrameMillis(0)
                                        }
                                    }
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Text Overlay
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                Text(
                                    text = item.name ?: "Unknown",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        if (pinnedAlbums.isNotEmpty()) {
            items(
                items = pinnedAlbums,
                key = { "pinned_${it.bucketName}" }
            ) { bucket ->
                AlbumCard(
                    bucket = bucket, 
                    gridAutoPlay = gridAutoPlay,
                    onClick = { onAlbumClick(bucket.bucketName) }
                )
            }
        }
        
        if (albums.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Device Folders",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Box {
                        androidx.compose.material3.IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, androidx.compose.foundation.shape.CircleShape)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Newest to Oldest") },
                                trailingIcon = {
                                    androidx.compose.material3.RadioButton(
                                        selected = albumSortOrder == SortOrder.NewToOld,
                                        onClick = null
                                    )
                                },
                                onClick = { viewModel.setAlbumSortOrder(SortOrder.NewToOld); showSortMenu = false }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Oldest to Newest") },
                                trailingIcon = {
                                    androidx.compose.material3.RadioButton(
                                        selected = albumSortOrder == SortOrder.OldToNew,
                                        onClick = null
                                    )
                                },
                                onClick = { viewModel.setAlbumSortOrder(SortOrder.OldToNew); showSortMenu = false }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Largest to Smallest") },
                                trailingIcon = {
                                    androidx.compose.material3.RadioButton(
                                        selected = albumSortOrder == SortOrder.BigToSmall,
                                        onClick = null
                                    )
                                },
                                onClick = { viewModel.setAlbumSortOrder(SortOrder.BigToSmall); showSortMenu = false }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Smallest to Largest") },
                                trailingIcon = {
                                    androidx.compose.material3.RadioButton(
                                        selected = albumSortOrder == SortOrder.SmallToBig,
                                        onClick = null
                                    )
                                },
                                onClick = { viewModel.setAlbumSortOrder(SortOrder.SmallToBig); showSortMenu = false }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("A to Z") },
                                trailingIcon = {
                                    androidx.compose.material3.RadioButton(
                                        selected = albumSortOrder == SortOrder.NameAsc,
                                        onClick = null
                                    )
                                },
                                onClick = { viewModel.setAlbumSortOrder(SortOrder.NameAsc); showSortMenu = false }
                            )
                        }
                    }
                }
            }
            
            items(
                items = albums,
                key = { "folder_${it.bucketName}" }
            ) { bucket ->
                AlbumCard(
                    bucket = bucket, 
                    gridAutoPlay = gridAutoPlay,
                    onClick = { onAlbumClick(bucket.bucketName) }
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .expressiveClick { onAlbumClick("Trash") },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Recycle Bin",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Recycle Bin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun Modifier.expressiveClick(onClick: () -> Unit): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(),
        label = "expressiveClickScale"
    )
    return this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { onClick() }
            )
        }
}

@Composable
fun AlbumCard(
    bucket: AlbumBucket,
    modifier: Modifier = Modifier,
    gridAutoPlay: Boolean = true,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val request = remember<ImageRequest>(bucket.coverUri, gridAutoPlay) {
        ImageRequest.Builder(context)
            .data(bucket.coverUri)
            .size(Size(300, 300))
            .precision(Precision.INEXACT)
            .crossfade(150)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .apply {
                if (!gridAutoPlay) {
                    repeatCount(0)
                    videoFrameMillis(0)
                }
            }
            .build()
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.extraLarge)
                .expressiveClick(onClick),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = request,
                contentDescription = bucket.bucketName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = bucket.bucketName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        val formattedSize = android.text.format.Formatter.formatShortFileSize(context, bucket.totalSizeBytes)
        Text(
            text = "${bucket.itemCount} items • $formattedSize",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
    }
}


