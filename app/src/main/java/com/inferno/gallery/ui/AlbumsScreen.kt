@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.inferno.gallery.ui

import android.net.Uri
import android.text.format.Formatter
import com.inferno.gallery.data.BucketNames
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumsScreen(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    viewModel: GalleryViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onAlbumClick: (String) -> Unit = {},
    onPersonClick: (String) -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    onNavigateToDuplicateCleaner: () -> Unit = {},
    onNavigateToPhotoMap: () -> Unit = {},
    onNavigateToPlacesList: () -> Unit = {},
    onNavigateToAllAlbums: () -> Unit = {},
    onNavigateToPeople: () -> Unit = {},
    onChangeCover: (String) -> Unit = {},
    onDeleteCover: (String) -> Unit = {}
) {
    val albums by viewModel.allAlbums.collectAsState()
    val pinnedAlbums by viewModel.pinnedAlbums.collectAsState()
    val userPinnedAlbums by viewModel.userPinnedAlbums.collectAsState()
    val userPinnedNames by viewModel.userPinnedFolderNames.collectAsState()
    val albumSortOrder by viewModel.albumSortOrder.collectAsState()
    val favoriteItems by viewModel.favoriteMedia.collectAsState()
    val trashCount by viewModel.trashCount.collectAsState()
    val vaultItemCount by viewModel.vaultItemCount.collectAsState()
    val showAlbumSize by viewModel.showAlbumSize.collectAsState()
    val placesClusters by viewModel.placesClusters.collectAsState()
    val mediaTypeBuckets by viewModel.mediaTypeBuckets.collectAsState()
    val customCovers by viewModel.albumCustomCovers.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Merge system pinned + user pinned, cap at 6 visible
    val systemPinned = remember(pinnedAlbums) {
        pinnedAlbums.filter {
            it.bucketName != "Trash" && it.bucketName != "Favorites"
        }
    }

    val allPinnedCards = remember(systemPinned, userPinnedAlbums) {
        (systemPinned + userPinnedAlbums).distinctBy { it.bucketName }
    }

    val maxVisible = 6
    val visiblePinned = allPinnedCards.take(maxVisible)
    val overflowPinned = allPinnedCards.drop(maxVisible)
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Persistent expand states for sections
    val pinnedExpanded by viewModel.albumsExpandedPinned.collectAsState()
    val moreExpanded by viewModel.albumsExpandedMore.collectAsState()
    val peopleExpanded by viewModel.albumsExpandedPeople.collectAsState()
    val placesExpanded by viewModel.albumsExpandedPlaces.collectAsState()
    val mediaTypesExpanded by viewModel.albumsExpandedMediaTypes.collectAsState()

    val unpinnedAlbums = remember(albums, userPinnedNames) {
        albums.filter { it.bucketName != "Favorites" && it.bucketName !in userPinnedNames }
    }

    val lazyGridState = rememberLazyGridState()

    // Ensure top pinned section is always visible and not scrolled down on initial open
    var hasPinnedLoaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(visiblePinned.isNotEmpty()) {
        if (visiblePinned.isNotEmpty() && !hasPinnedLoaded) {
            hasPinnedLoaded = true
            lazyGridState.scrollToItem(0, 0)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setTopBarCollapsed(false)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(12),
        state = lazyGridState,
        contentPadding = PaddingValues(
            start = SpacingTokens.L,
            end = SpacingTokens.L,
            top = contentPadding.calculateTopPadding() + SpacingTokens.M,
            bottom = contentPadding.calculateBottomPadding() + SpacingTokens.XXL
        ),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.L),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
        modifier = modifier.fillMaxSize()
    ) {

        // ── 1. Pinned albums ──
        if (visiblePinned.isNotEmpty()) {
            item(key = "header_pinned", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Pinned albums",
                    count = allPinnedCards.size,
                    isExpanded = pinnedExpanded,
                    onToggle = { viewModel.toggleAlbumsExpandedPinned() }
                )
            }

            if (pinnedExpanded) {
                items(
                    items = visiblePinned,
                    key = { "pinned_${it.bucketName}" },
                    span = { GridItemSpan(4) }
                ) { bucket ->
                    val displayBucketName = when (bucket.bucketName) {
                        "Screenrecordings", "Screenrecords", "ScreenRecord" -> "Screen recordings"
                        else -> bucket.bucketName
                    }
                    val isUserPinned = bucket.bucketName in userPinnedNames
                    var showCardMenu by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        AlbumCard(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            bucket = bucket.copy(bucketName = displayBucketName),
                            showAlbumSize = showAlbumSize,
                            onClick = { onAlbumClick(bucket.bucketName) },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (overflowPinned.isNotEmpty() && bucket == visiblePinned.last()) {
                                    showOverflowMenu = true
                                } else {
                                    showCardMenu = true
                                }
                            }
                        )

                        // Context menu for pinned albums
                        DropdownMenu(
                            expanded = showCardMenu,
                            onDismissRequest = { showCardMenu = false },
                            shape = ShapeExtraLarge,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            if (isUserPinned) {
                                DropdownMenuItem(
                                    text = { Text("Unpin Album", style = MaterialTheme.typography.bodyMedium) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_folder_off),
                                            contentDescription = null,
                                            modifier = Modifier.size(IconSizeTokens.M)
                                        )
                                    },
                                    onClick = {
                                        showCardMenu = false
                                        viewModel.togglePinAlbum(bucket.bucketName)
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
                                    showCardMenu = false
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
                                        showCardMenu = false
                                        onDeleteCover(bucket.bucketName)
                                    }
                                )
                            }
                        }

                        // Overflow menu for extra pinned albums
                        if (overflowPinned.isNotEmpty() && bucket == visiblePinned.last()) {
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                shape = ShapeExtraLarge,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                overflowPinned.forEach { overflowBucket ->
                                    DropdownMenuItem(
                                        text = { Text(overflowBucket.bucketName, style = MaterialTheme.typography.bodyMedium) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                                contentDescription = null,
                                                modifier = Modifier.size(IconSizeTokens.M)
                                            )
                                        },
                                        trailingIcon = {
                                            Surface(
                                                shape = ShapeFull,
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                modifier = Modifier.padding(start = SpacingTokens.XS)
                                            ) {
                                                Text(
                                                    text = "${overflowBucket.itemCount}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = 2.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            onAlbumClick(overflowBucket.bucketName)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 2. More albums (Horizontal Carousel) ──
        if (unpinnedAlbums.isNotEmpty()) {
            item(key = "header_more", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "More albums",
                    count = unpinnedAlbums.size,
                    isExpanded = moreExpanded,
                    onToggle = { viewModel.toggleAlbumsExpandedMore() },
                    onSeeAll = onNavigateToAllAlbums
                )
            }

            item(key = "content_more", span = { GridItemSpan(maxLineSpan) }) {
                AnimatedVisibility(
                    visible = moreExpanded,
                    enter = expandVertically(animationSpec = MotionTokens.snappySpring()) + fadeIn(animationSpec = MotionTokens.snappySpring()),
                    exit = shrinkVertically(animationSpec = MotionTokens.snappySpring()) + fadeOut(animationSpec = MotionTokens.snappySpring())
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = SpacingTokens.XS),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                    ) {
                        items(
                            items = unpinnedAlbums,
                            key = { "folder_${it.bucketName}" }
                        ) { bucket ->
                            Box(modifier = Modifier.width(116.dp)) {
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
                                            viewModel.togglePinAlbum(bucket.bucketName)
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
        }



        // ── 3. Places Carousel ──
        item(key = "header_places", span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = "Places",
                count = if (placesClusters.isNotEmpty()) placesClusters.size else null,
                isExpanded = placesExpanded,
                onToggle = { viewModel.toggleAlbumsExpandedPlaces() },
                onSeeAll = onNavigateToPlacesList
            )
        }

        item(key = "content_places", span = { GridItemSpan(maxLineSpan) }) {
            AnimatedVisibility(
                visible = placesExpanded,
                enter = expandVertically(animationSpec = MotionTokens.snappySpring()) + fadeIn(animationSpec = MotionTokens.snappySpring()),
                exit = shrinkVertically(animationSpec = MotionTokens.snappySpring()) + fadeOut(animationSpec = MotionTokens.snappySpring())
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = SpacingTokens.XS),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                ) {
                    item {
                        // "Explore Map" Hero Card
                        Surface(
                            onClick = onNavigateToPhotoMap,
                            shape = ShapeLargeIncreased,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .width(130.dp)
                                .aspectRatio(1f)
                                .pressScale(pressedScale = 0.96f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(R.drawable.map_bg_placeholder),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f)
                                                )
                                            )
                                        )
                                )
                                Surface(
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_map),
                                            contentDescription = null,
                                            modifier = Modifier.size(IconSizeTokens.L),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "Explore Map",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = SpacingTokens.S)
                                )
                            }
                        }
                    }

                    items(placesClusters, key = { "place_${it.bucketName}" }) { cluster ->
                        Surface(
                            onClick = { onAlbumClick("place:${cluster.bucketName}") },
                            shape = ShapeLargeIncreased,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .width(130.dp)
                                .aspectRatio(1f)
                                .pressScale(pressedScale = 0.96f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(Uri.parse(cluster.representativeUri))
                                        .size(300)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f)
                                                )
                                            )
                                        )
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .padding(SpacingTokens.S),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_location_on),
                                        contentDescription = null,
                                        modifier = Modifier.size(IconSizeTokens.S),
                                        tint = MaterialTheme.colorScheme.surfaceContainerLowest
                                    )
                                    Text(
                                        text = cluster.bucketName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3.5 Media Types (RAW, Panoramas, Slow-Mo, GIFs) ──
        if (mediaTypeBuckets.isNotEmpty()) {
            item(key = "header_media_types", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Media Types",
                    count = mediaTypeBuckets.size,
                    isExpanded = mediaTypesExpanded,
                    onToggle = { viewModel.toggleAlbumsExpandedMediaTypes() }
                )
            }

            item(key = "content_media_types", span = { GridItemSpan(maxLineSpan) }) {
                AnimatedVisibility(
                    visible = mediaTypesExpanded,
                    enter = expandVertically(animationSpec = MotionTokens.snappySpring()) + fadeIn(animationSpec = MotionTokens.snappySpring()),
                    exit = shrinkVertically(animationSpec = MotionTokens.snappySpring()) + fadeOut(animationSpec = MotionTokens.snappySpring())
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
                        contentPadding = PaddingValues(horizontal = SpacingTokens.XS)
                    ) {
                        items(mediaTypeBuckets, key = { "media_type_${it.bucketName}" }) { mediaType ->
                            val (title, iconRes) = when (mediaType.bucketName) {
                                BucketNames.MEDIA_TYPE_RAW -> "RAW" to R.drawable.ic_ms_photo_camera
                                BucketNames.MEDIA_TYPE_PANORAMAS -> "Panoramas" to R.drawable.ic_ms_aspect_ratio
                                BucketNames.MEDIA_TYPE_SLOW_MO -> "Slow Motion" to R.drawable.ic_ms_schedule
                                BucketNames.MEDIA_TYPE_ANIMATIONS -> "GIFs" to R.drawable.ic_ms_auto_fix_high
                                else -> "Media" to R.drawable.ic_ms_image
                            }

                            Surface(
                                onClick = { onAlbumClick(mediaType.bucketName) },
                                shape = ShapeLargeIncreased,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(96.dp)
                                    .pressScale(pressedScale = 0.96f)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (mediaType.coverUri != Uri.EMPTY) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(mediaType.coverUri)
                                                .size(280)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f)
                                                        )
                                                    )
                                                )
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(SpacingTokens.S)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(iconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(IconSizeTokens.S),
                                                tint = if (mediaType.coverUri != Uri.EMPTY) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (mediaType.coverUri != Uri.EMPTY) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.onSecondaryContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${mediaType.itemCount} items",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (mediaType.coverUri != Uri.EMPTY) MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 4. Collections Section (Segmented Block Grid) ──
        item(key = "header_collections", span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SpacingTokens.L, bottom = SpacingTokens.XS),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Collections",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "content_collections", span = { GridItemSpan(maxLineSpan) }) {
            val collectionItems = remember(favoriteItems, trashCount, vaultItemCount) {
                listOf(
                    CollectionBlockItem(
                        title = "Videos",
                        iconRes = R.drawable.ic_ms_play_arrow,
                        onClick = { onAlbumClick("Videos") }
                    ),
                    CollectionBlockItem(
                        title = "Screenshots",
                        iconRes = R.drawable.ic_ms_aspect_ratio,
                        onClick = { onAlbumClick("Screenshots") }
                    ),
                    CollectionBlockItem(
                        title = "Favorites",
                        iconRes = R.drawable.ic_ms_star,
                        onClick = { onAlbumClick("Favorites") }
                    ),
                    CollectionBlockItem(
                        title = "Locked",
                        iconRes = R.drawable.ic_ms_lock,
                        onClick = {
                            val activity = context as? androidx.fragment.app.FragmentActivity
                            if (activity != null) {
                                viewModel.vaultAuthManager.authenticate(
                                    activity = activity,
                                    onSuccess = onNavigateToVault,
                                    onFailure = {}
                                )
                            }
                        }
                    ),
                    CollectionBlockItem(
                        title = "Clean Up",
                        iconRes = R.drawable.ic_ms_cleaning_services,
                        onClick = onNavigateToDuplicateCleaner
                    ),
                    CollectionBlockItem(
                        title = "Trash",
                        iconRes = R.drawable.ic_ms_delete,
                        onClick = { onAlbumClick("Trash") }
                    )
                )
            }

            SegmentedCollectionsGrid(
                items = collectionItems,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Shared M3 Expressive Album Card ──

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumCard(
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    bucket: AlbumBucket,
    modifier: Modifier = Modifier,
    showAlbumSize: Boolean = false,
    onClick: () -> Unit = {},
    onLongPress: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val request = remember<ImageRequest>(bucket.coverUri) {
        ImageRequest.Builder(context)
            .data(bucket.coverUri)
            .size(Size(360, 360))
            .precision(Precision.EXACT)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    val sharedBoundsModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "album_${bucket.bucketName}"),
                animatedVisibilityScope = animatedVisibilityScope,
                enter = fadeIn(animationSpec = MotionTokens.snappySpring()),
                exit = fadeOut(animationSpec = MotionTokens.snappySpring()),
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                clipInOverlayDuringTransition = OverlayClip(ShapeLarge),
                boundsTransform = { _, _ -> MotionTokens.sharedElementSpring() }
            )
        }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.96f)
            .clip(ShapeLarge)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        horizontalAlignment = Alignment.Start
    ) {
        // Outer Card Container (M3 Expressive Carded Surface - Filled Tonal)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .then(sharedBoundsModifier),
            shape = ShapeLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                if (bucket.coverUris.size == 4) {
                    CollageCover(
                        uris = bucket.coverUris,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (bucket.coverUri == Uri.EMPTY) {
                    val icon = if (bucket.bucketName == "Favorites") {
                        ImageVector.vectorResource(R.drawable.ic_ms_favorite)
                    } else {
                        ImageVector.vectorResource(R.drawable.ic_ms_photo_album)
                    }
                    Surface(
                        shape = ShapeLarge,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = bucket.bucketName,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(IconSizeTokens.L)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    AsyncImage(
                        model = request,
                        contentDescription = bucket.bucketName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(ShapeLarge)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingTokens.S))

        // Title
        Text(
            text = bucket.bucketName,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Metadata: Count and Size
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            val countText = remember(bucket.itemCount) {
                val formatted = NumberFormat.getInstance(Locale.US).format(bucket.itemCount)
                if (bucket.itemCount == 1) "1 item" else "$formatted items"
            }

            Text(
                text = countText,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (showAlbumSize && bucket.totalSizeBytes > 0) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = Formatter.formatShortFileSize(context, bucket.totalSizeBytes),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── 2x2 Collage Cover (Carded 4-thumbnail grid) ──

@Composable
fun CollageCover(
    uris: List<Uri>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val requests = remember(uris) {
        uris.map { uri ->
            ImageRequest.Builder(context)
                .data(uri)
                .size(180, 180)
                .precision(Precision.EXACT)
                .crossfade(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AsyncImage(
                model = requests.getOrNull(0),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(ShapeLarge)
            )
            AsyncImage(
                model = requests.getOrNull(1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(ShapeLarge)
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AsyncImage(
                model = requests.getOrNull(2),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(ShapeLarge)
            )
            AsyncImage(
                model = requests.getOrNull(3),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(ShapeLarge)
            )
        }
    }
}

// ── Segmented Collections Grid (Single Card Divided into Blocks) ──

data class CollectionBlockItem(
    val title: String,
    @DrawableRes val iconRes: Int,
    val onClick: () -> Unit
)

@Composable
fun SegmentedCollectionsGrid(
    items: List<CollectionBlockItem>,
    modifier: Modifier = Modifier,
    outerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    innerRadius: androidx.compose.ui.unit.Dp = 6.dp,
    gap: androidx.compose.ui.unit.Dp = 4.dp
) {
    val rows = remember(items) { items.chunked(2) }
    val totalRows = rows.size

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        rows.forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                rowItems.forEachIndexed { colIndex, item ->
                    val isTop = (rowIndex == 0)
                    val isBottom = (rowIndex == totalRows - 1)
                    val isLeft = (colIndex == 0)
                    val isRight = (colIndex == 1)

                    val cellShape = RoundedCornerShape(
                        topStart = if (isTop && isLeft) outerRadius else innerRadius,
                        topEnd = if (isTop && isRight) outerRadius else innerRadius,
                        bottomStart = if (isBottom && isLeft) outerRadius else innerRadius,
                        bottomEnd = if (isBottom && isRight) outerRadius else innerRadius
                    )

                    Surface(
                        onClick = item.onClick,
                        shape = cellShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .pressScale(pressedScale = 0.97f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(item.iconRes),
                                contentDescription = item.title,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(IconSizeTokens.L)
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Expressive Section Header ──

@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    isExpanded: Boolean = true,
    isCollapsible: Boolean = true,
    onToggle: () -> Unit = {},
    onSeeAll: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SpacingTokens.M, bottom = SpacingTokens.XS)
            .then(
                if (isCollapsible) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggle()
                    }
                } else Modifier
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
        ) {
            // M3 Expressive Pill Accent
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(ShapeFull)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (count != null && count > 0) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = 2.dp)
                    )
                }
            }

            if (isCollapsible) {
                val rotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = MotionTokens.snappySpring(),
                    label = "sectionChevronRotation"
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_keyboard_arrow_down),
                    contentDescription = "Expand/Collapse",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(IconSizeTokens.M)
                        .rotate(rotation)
                )
            }
        }

        if (onSeeAll != null) {
            FilledTonalButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSeeAll()
                },
                shape = ShapeFull,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = SpacingTokens.L, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}
