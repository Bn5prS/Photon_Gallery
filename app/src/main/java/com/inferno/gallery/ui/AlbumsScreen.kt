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
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import android.net.Uri
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
    val favoriteItems by viewModel.favoriteMedia.collectAsState()
    val gridAutoPlay by viewModel.gridAutoPlay.collectAsState()
    val trashCount by viewModel.trashCount.collectAsState()

    var isInitialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        isInitialLoading = false
    }

    val pinnedAlbumsWithTrash = remember(pinnedAlbums, trashCount) {
        val list = pinnedAlbums.toMutableList()
        val screenshotIndex = list.indexOfFirst { it.bucketName.contains("Screenshots", ignoreCase = true) || it.bucketName.contains("Screenshot", ignoreCase = true) }
        val trashBucket = AlbumBucket(
            bucketName = "Trash",
            coverUri = Uri.EMPTY,
            itemCount = trashCount
        )
        if (screenshotIndex != -1) {
            list.add(screenshotIndex + 1, trashBucket)
        } else {
            list.add(trashBucket)
        }
        list
    }
    
    var showSortMenu by remember { mutableStateOf(false) }

    val lazyGridState = rememberLazyGridState()

    LaunchedEffect(lazyGridState) {
        var previousIndex = 0
        var previousScrollOffset = 0
        snapshotFlow {
            Triple(
                lazyGridState.firstVisibleItemIndex,
                lazyGridState.firstVisibleItemScrollOffset,
                lazyGridState.isScrollInProgress
            )
        }
        .collectLatest { (index, offset, isScrollInProgress) ->
            if (isScrollInProgress) {
                if (index > previousIndex) {
                    viewModel.setScrollDockVisible(false)
                } else if (index < previousIndex) {
                    viewModel.setScrollDockVisible(true)
                } else {
                    if (offset > previousScrollOffset + 15) {
                        viewModel.setScrollDockVisible(false)
                    } else if (offset < previousScrollOffset - 15) {
                        viewModel.setScrollDockVisible(true)
                    }
                }
            }
            previousIndex = index
            previousScrollOffset = offset
        }
    }
    
    if (isInitialLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            WavyProgressIndicator(modifier = Modifier.size(48.dp))
        }
        return
    }
    
    




    LazyVerticalGrid(
        columns = GridCells.Fixed(12),
        state = lazyGridState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (favoriteItems.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                        HorizontalUncontainedCarousel(
                            state = rememberCarouselState { favoriteItems.size },
                            itemWidth = 140.dp,
                            itemSpacing = 8.dp,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        ) { i ->
                            val item = favoriteItems[i]
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.large)
                                    .expressiveClick { onAlbumClick("Favorites") }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(item.uri)
                                        .size(300, 300)
                                        .precision(Precision.EXACT)
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
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
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
        }
        if (pinnedAlbumsWithTrash.isNotEmpty()) {
            items(
                items = pinnedAlbumsWithTrash,
                key = { "pinned_${it.bucketName}" },
                span = { GridItemSpan(4) }
            ) { bucket ->
                if (bucket.bucketName == "Trash") {
                    TrashCard(
                        itemCount = bucket.itemCount,
                        onClick = { onAlbumClick("Trash") }
                    )
                } else {
                    // Map screen recordings bucket name for UI display
                    val displayBucketName = when (bucket.bucketName) {
                        "Screenrecordings", "Screenrecords", "ScreenRecord" -> "Screen recordings"
                        else -> bucket.bucketName
                    }
                    AlbumCard(
                        bucket = bucket.copy(bucketName = displayBucketName), 
                        gridAutoPlay = gridAutoPlay,
                        onClick = { onAlbumClick(bucket.bucketName) } // Pass original bucketName for click querying
                    )
                }
            }
        }
        
        if (albums.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "More albums",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Box {
                        androidx.compose.material3.IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
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
                key = { "folder_${it.bucketName}" },
                span = { GridItemSpan(3) }
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
        }
    }
}

@Composable
fun Modifier.expressiveClick(onClick: () -> Unit): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
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
fun CollageCover(
    uris: List<Uri>,
    modifier: Modifier = Modifier,
    gridAutoPlay: Boolean = true
) {
    val context = LocalContext.current
    val requests = remember(uris, gridAutoPlay) {
        uris.map { uri ->
            ImageRequest.Builder(context)
                .data(uri)
                .size(150, 150)
                .precision(Precision.EXACT)
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
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AsyncImage(
                    model = requests.getOrNull(0),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                )
                AsyncImage(
                    model = requests.getOrNull(1),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                )
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AsyncImage(
                    model = requests.getOrNull(2),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                )
                AsyncImage(
                    model = requests.getOrNull(3),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                )
            }
        }
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
            .precision(Precision.EXACT)
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .expressiveClick(onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (bucket.coverUris.size == 4) {
                    CollageCover(
                        uris = bucket.coverUris,
                        modifier = Modifier.fillMaxSize(),
                        gridAutoPlay = gridAutoPlay
                    )
                } else if (bucket.coverUri == Uri.EMPTY) {
                    val icon = if (bucket.bucketName == "Favorites") {
                        Icons.Outlined.FavoriteBorder
                    } else {
                        Icons.Outlined.Folder
                    }
                    androidx.compose.material3.Icon(
                        imageVector = icon,
                        contentDescription = bucket.bucketName,
                        tint = if (bucket.bucketName == "Favorites") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    AsyncImage(
                        model = request,
                        contentDescription = bucket.bucketName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = bucket.bucketName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = java.text.NumberFormat.getInstance(java.util.Locale.US).format(bucket.itemCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun TrashCard(
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .expressiveClick(onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Recycle Bin",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Recycle Bin",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = java.text.NumberFormat.getInstance(java.util.Locale.US).format(itemCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}


