@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.inferno.gallery.ui

import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.inferno.gallery.data.AlbumLayoutMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
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
import com.inferno.gallery.ui.theme.AlbumShapes
import com.inferno.gallery.ui.components.AlbumShapePickerSheet
import com.inferno.gallery.ui.utils.pressScale
import com.inferno.gallery.ui.theme.photonColors
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
    onChangeCover: (String) -> Unit = {},
    onDeleteCover: (String) -> Unit = {}
) {
    val albums by viewModel.allAlbums.collectAsState()
    val userPinnedAlbums by viewModel.userPinnedAlbums.collectAsState()
    val userPinnedNames by viewModel.userPinnedFolderNames.collectAsState()
    val albumSortOrder by viewModel.albumSortOrder.collectAsState()
    val albumLayoutMode by viewModel.albumLayoutMode.collectAsState()
    val favoriteItems by viewModel.favoriteMedia.collectAsState()
    val trashCount by viewModel.trashCount.collectAsState()
    val vaultItemCount by viewModel.vaultItemCount.collectAsState()
    val videoCount by viewModel.videoCount.collectAsState()
    val screenshotsCount = remember(albums) {
        albums.firstOrNull { it.bucketName.equals("Screenshots", ignoreCase = true) }?.itemCount
            ?: albums.filter { it.bucketName.contains("Screenshot", ignoreCase = true) }.sumOf { it.itemCount }
    }
    val showAlbumSize by viewModel.showAlbumSize.collectAsState()
    val placesClusters by viewModel.placesClusters.collectAsState()
    val mediaTypeBuckets by viewModel.mediaTypeBuckets.collectAsState()
    val customCovers by viewModel.albumCustomCovers.collectAsState()
    val customShapes by viewModel.albumCustomShapes.collectAsState()
    var shapePickerBucket by remember { mutableStateOf<AlbumBucket?>(null) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val showEmptyAlbums by viewModel.showEmptyAlbums.collectAsState()

    val unpinnedAlbums = remember(albums, userPinnedNames, showEmptyAlbums) {
        albums.filter { it.bucketName != "Favorites" && it.bucketName != "Trash" && it.bucketName != "All" && it.bucketName !in userPinnedNames && (it.itemCount > 0 || showEmptyAlbums) }
    }

    val visibleAlbums = remember(unpinnedAlbums) {
        unpinnedAlbums.take(9)
    }
    val hasMoreAlbums = unpinnedAlbums.size > 9

    // Persistent expand states for sections
    val pinnedExpanded by viewModel.albumsExpandedPinned.collectAsState()
    val albumsExpanded by viewModel.albumsExpandedMore.collectAsState()
    val placesExpanded by viewModel.albumsExpandedPlaces.collectAsState()
    val mediaTypesExpanded by viewModel.albumsExpandedMediaTypes.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setTopBarCollapsed(false)
    }

    key(albumLayoutMode) {
        val lazyGridState = rememberLazyGridState()

        // Ensure clean top alignment when albums populate or if previously pushed up
        var initialTopResetDone by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(visibleAlbums.isNotEmpty() || userPinnedAlbums.isNotEmpty()) {
            if ((visibleAlbums.isNotEmpty() || userPinnedAlbums.isNotEmpty()) && !initialTopResetDone) {
                initialTopResetDone = true
                if (!lazyGridState.isScrollInProgress && lazyGridState.firstVisibleItemIndex > 0) {
                    lazyGridState.scrollToItem(0, 0)
                }
            }
        }

        LaunchedEffect(viewModel) {
            viewModel.albumsScrollToTopTrigger.collect {
                lazyGridState.animateScrollToItem(0, 0)
            }
        }

        LazyVerticalGrid(
            columns = if (albumLayoutMode == AlbumLayoutMode.LIST) GridCells.Fixed(1) else GridCells.Fixed(3),
            state = lazyGridState,
            contentPadding = PaddingValues(
                start = SpacingTokens.L,
                end = SpacingTokens.L,
                top = contentPadding.calculateTopPadding() + SpacingTokens.M,
                bottom = contentPadding.calculateBottomPadding() + SpacingTokens.XXL
            ),
            verticalArrangement = if (albumLayoutMode == AlbumLayoutMode.LIST) {
                Arrangement.spacedBy(SpacingTokens.S)
            } else {
                Arrangement.spacedBy(SpacingTokens.L)
            },
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
            modifier = modifier.fillMaxSize()
        ) {

        // ── 1. Pinned Albums Section (Dedicated Grid) ──
        if (userPinnedAlbums.isNotEmpty()) {
            item(key = "header_pinned_albums", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Pinned",
                    count = userPinnedAlbums.size,
                    isExpanded = pinnedExpanded,
                    onToggle = { viewModel.toggleAlbumsExpandedPinned() }
                )
            }

            if (pinnedExpanded) {
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
                                isCompact = true,
                                isPinned = true,
                                onClick = { onAlbumClick(bucket.bucketName) },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showCardMenu = true
                                }
                            )
                        }

                        AlbumDropdownMenu(
                            expanded = showCardMenu,
                            onDismissRequest = { showCardMenu = false },
                            bucketName = bucket.bucketName,
                            isUserPinned = true,
                            hasCustomCover = customCovers.containsKey(bucket.bucketName),
                            hasCustomShape = customShapes.containsKey(bucket.bucketName),
                            onTogglePin = {
                                viewModel.togglePinAlbum(bucket.bucketName) { success ->
                                    if (!success) {
                                        Toast.makeText(context, "Maximum 15 albums can be pinned", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onChangeCover = { onChangeCover(bucket.bucketName) },
                            onDeleteCover = { onDeleteCover(bucket.bucketName) },
                            onChangeShape = { shapePickerBucket = bucket },
                            onResetShape = { viewModel.removeAlbumCustomShape(bucket.bucketName) }
                        )
                    }
                }
            }
        }

        // ── 2. All Other Albums Section ──
        item(key = "header_albums", span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = "Albums",
                count = if (unpinnedAlbums.isNotEmpty()) unpinnedAlbums.size else null,
                isExpanded = albumsExpanded,
                onToggle = { viewModel.toggleAlbumsExpandedMore() },
                seeAllText = "Show all",
                onSeeAll = if (hasMoreAlbums) onNavigateToAllAlbums else null
            )
        }

        if (albumsExpanded) {
            // Layout switcher & sort control bar
            item(key = "albums_controls_bar", span = { GridItemSpan(maxLineSpan) }) {
                AlbumsControlBar(
                    currentLayout = albumLayoutMode,
                    onLayoutChange = { newLayout ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setAlbumLayoutMode(newLayout)
                    },
                    currentSort = albumSortOrder,
                    onSortChange = { newSort ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setAlbumSortOrder(newSort)
                    }
                )
            }

            if (visibleAlbums.isNotEmpty()) {
                items(
                    items = visibleAlbums,
                    key = { "album_${it.bucketName}" }
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
                                isPinned = false,
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
                                isCompact = true,
                                isPinned = false,
                                onClick = { onAlbumClick(bucket.bucketName) },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showCardMenu = true
                                }
                            )
                        }

                        // Context menu for albums
                        AlbumDropdownMenu(
                            expanded = showCardMenu,
                            onDismissRequest = { showCardMenu = false },
                            bucketName = bucket.bucketName,
                            isUserPinned = false,
                            hasCustomCover = customCovers.containsKey(bucket.bucketName),
                            hasCustomShape = customShapes.containsKey(bucket.bucketName),
                            onTogglePin = {
                                viewModel.togglePinAlbum(bucket.bucketName) { success ->
                                    if (!success) {
                                        Toast.makeText(context, "Maximum 15 albums can be pinned", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onChangeCover = { onChangeCover(bucket.bucketName) },
                            onDeleteCover = { onDeleteCover(bucket.bucketName) },
                            onChangeShape = { shapePickerBucket = bucket },
                            onResetShape = { viewModel.removeAlbumCustomShape(bucket.bucketName) }
                        )
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
                seeAllText = "Show all",
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
                                    color = MaterialTheme.photonColors.onMediaScrim,
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
                                        tint = MaterialTheme.photonColors.onMediaScrim
                                    )
                                    Text(
                                        text = cluster.bucketName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.photonColors.onMediaScrim,
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

        // ── 3.5 Capture Modes (RAW, Panoramas, Night, Portraits, Macro, etc.) ──
        if (mediaTypeBuckets.isNotEmpty()) {
            item(key = "header_media_types", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Capture Modes",
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
                                BucketNames.MEDIA_TYPE_ANIMATIONS -> "Animations" to R.drawable.ic_ms_auto_fix_high
                                BucketNames.MEDIA_TYPE_NIGHT -> "Night Shots" to R.drawable.ic_ms_wb_twilight
                                BucketNames.MEDIA_TYPE_PHOTO_SPHERE -> "Photo Spheres" to R.drawable.ic_ms_vrpano
                                BucketNames.MEDIA_TYPE_PORTRAIT -> "Portraits" to R.drawable.ic_ms_person
                                BucketNames.MEDIA_TYPE_MACRO -> "Macro" to R.drawable.ic_ms_filter_vintage
                                BucketNames.MEDIA_TYPE_TIMELAPSE -> "Time-Lapses" to R.drawable.ic_ms_timelapse
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
                                                tint = if (mediaType.coverUri != Uri.EMPTY) MaterialTheme.photonColors.onMediaScrim else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (mediaType.coverUri != Uri.EMPTY) MaterialTheme.photonColors.onMediaScrim else MaterialTheme.colorScheme.onSecondaryContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${mediaType.itemCount} items",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (mediaType.coverUri != Uri.EMPTY) MaterialTheme.photonColors.onMediaScrim.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
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

        // ── 4. Utilities & Tools ──
        item(key = "header_more_tools", span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SpacingTokens.L, bottom = SpacingTokens.XS),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Utilities & Tools",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "row_utilities", span = { GridItemSpan(maxLineSpan) }) {
            val utilities = remember(favoriteItems.size, screenshotsCount, videoCount, vaultItemCount) {
                listOf(
                    UtilityItem(
                        title = "Favorites",
                        iconRes = R.drawable.ic_ms_star,
                        count = favoriteItems.size,
                        onClick = { onAlbumClick("Favorites") }
                    ),
                    UtilityItem(
                        title = "Screenshots",
                        iconRes = R.drawable.ic_ms_aspect_ratio,
                        count = screenshotsCount,
                        onClick = { onAlbumClick("Screenshots") }
                    ),
                    UtilityItem(
                        title = "Videos",
                        iconRes = R.drawable.ic_ms_play_arrow,
                        count = videoCount,
                        onClick = { onAlbumClick("Videos") }
                    ),
                    UtilityItem(
                        title = "Locked",
                        iconRes = R.drawable.ic_ms_lock,
                        count = vaultItemCount,
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
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SpacingTokens.XS),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
            ) {
                utilities.forEachIndexed { index, item ->
                    val cardShape = when (index) {
                        0 -> RoundedCornerShape(
                            topStart = 28.dp,
                            bottomStart = 28.dp,
                            topEnd = 12.dp,
                            bottomEnd = 12.dp
                        )
                        utilities.lastIndex -> RoundedCornerShape(
                            topStart = 12.dp,
                            bottomStart = 12.dp,
                            topEnd = 28.dp,
                            bottomEnd = 28.dp
                        )
                        else -> ShapeMedium
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.88f)
                            .pressScale(pressedScale = 0.96f)
                            .clip(cardShape)
                            .clickable { item.onClick() },
                        shape = cardShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shadowElevation = 0.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        start = SpacingTokens.XS,
                                        end = SpacingTokens.XS,
                                        top = 22.dp,
                                        bottom = SpacingTokens.S
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shadowElevation = 0.dp
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(item.iconRes),
                                            contentDescription = item.title,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(IconSizeTokens.L)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(SpacingTokens.XS))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            item.count?.let { count ->
                                Surface(
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(
                                            top = if (index == utilities.lastIndex) 8.dp else 6.dp,
                                            end = if (index == utilities.lastIndex) 10.dp else 6.dp
                                        )
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "content_more_tools", span = { GridItemSpan(maxLineSpan) }) {
            val toolItems = remember(trashCount) {
                listOf(
                    CollectionBlockItem(
                        title = "Trash",
                        iconRes = R.drawable.ic_ms_delete,
                        count = trashCount,
                        onClick = { onAlbumClick("Trash") }
                    ),
                    CollectionBlockItem(
                        title = "Clean Up",
                        iconRes = R.drawable.ic_ms_cleaning_services,
                        onClick = onNavigateToDuplicateCleaner
                    )
                )
            }

            SegmentedCollectionsGrid(
                items = toolItems,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

    val pickerBucket = shapePickerBucket
    if (pickerBucket != null) {
        AlbumShapePickerSheet(
            bucket = pickerBucket,
            currentShapeId = customShapes[pickerBucket.bucketName],
            onShapeSelected = { shapeId ->
                viewModel.setAlbumCustomShape(pickerBucket.bucketName, shapeId)
            },
            onResetShape = {
                viewModel.removeAlbumCustomShape(pickerBucket.bucketName)
            },
            onDismissRequest = {
                shapePickerBucket = null
            }
        )
    }
}

// ── Utility Hub Data Item ──

data class UtilityItem(
    val title: String,
    @DrawableRes val iconRes: Int,
    val count: Int? = null,
    val onClick: () -> Unit
)

// ── Shared M3 Expressive Album Card (28dp Squircle, Single Thumbnail) ──

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumCard(
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    bucket: AlbumBucket,
    modifier: Modifier = Modifier,
    showAlbumSize: Boolean = false,
    customShape: Shape? = null,
    isCompact: Boolean = false,
    isPinned: Boolean = false,
    onClick: () -> Unit = {},
    onLongPress: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val tileShape = customShape ?: if (isCompact) ShapeMedium else ShapeLarge

    var loadFailed by remember(bucket.coverUri) { mutableStateOf(false) }

    val effectiveUri = if (loadFailed && bucket.defaultCoverUri != Uri.EMPTY) {
        bucket.defaultCoverUri
    } else {
        bucket.coverUri
    }

    val request = remember<ImageRequest>(effectiveUri) {
        ImageRequest.Builder(context)
            .data(effectiveUri)
            .size(Size(480, 480))
            .precision(Precision.EXACT)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.96f),
        horizontalAlignment = Alignment.Start
    ) {
        // Outer Card Container (M3 Expressive Album Tile Shape)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(tileShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongPress
                ),
            shape = tileShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (effectiveUri == Uri.EMPTY) {
                    val icon = if (bucket.bucketName == "Favorites") {
                        ImageVector.vectorResource(R.drawable.ic_ms_favorite)
                    } else {
                        ImageVector.vectorResource(R.drawable.ic_ms_photo_album)
                    }
                    Surface(
                        shape = tileShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(if (isCompact) 36.dp else 44.dp),
                                shadowElevation = 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = bucket.bucketName,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(if (isCompact) IconSizeTokens.S else IconSizeTokens.M)
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
                        onError = {
                            if (!loadFailed && bucket.defaultCoverUri != Uri.EMPTY && bucket.defaultCoverUri != bucket.coverUri) {
                                loadFailed = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(tileShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingTokens.XS))

        // Title (Outfit Bold / SemiBold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS),
            modifier = Modifier.padding(horizontal = SpacingTokens.XS)
        ) {
            Text(
                text = bucket.bucketName,
                style = if (isCompact) {
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                },
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isPinned) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_pin),
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(IconSizeTokens.S)
                )
            }
        }

        // Metadata: Count and Size (Plus Jakarta Sans)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.XS, vertical = 2.dp)
        ) {
            val countText = remember(bucket.itemCount) {
                val formatted = NumberFormat.getInstance(Locale.US).format(bucket.itemCount)
                if (bucket.itemCount == 1) "1 item" else "$formatted items"
            }

            Text(
                text = countText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (showAlbumSize && bucket.totalSizeBytes > 0) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = Formatter.formatShortFileSize(context, bucket.totalSizeBytes),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }
    }
}

// ── Shared M3 Expressive Album List Row (Full-width list item) ──

@Composable
fun AlbumListRow(
    bucket: AlbumBucket,
    modifier: Modifier = Modifier,
    showAlbumSize: Boolean = false,
    customShape: Shape? = null,
    isPinned: Boolean = false,
    onClick: () -> Unit = {},
    onLongPress: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val tileShape = customShape ?: ShapeMedium

    var loadFailed by remember(bucket.coverUri) { mutableStateOf(false) }

    val effectiveUri = if (loadFailed && bucket.defaultCoverUri != Uri.EMPTY) {
        bucket.defaultCoverUri
    } else {
        bucket.coverUri
    }

    val request = remember<ImageRequest>(effectiveUri) {
        ImageRequest.Builder(context)
            .data(effectiveUri)
            .size(Size(200, 200))
            .precision(Precision.EXACT)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Surface(
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.98f)
            .clip(ShapeLarge)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.S),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading square thumbnail (56x56dp)
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(tileShape),
                shape = tileShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                if (effectiveUri == Uri.EMPTY) {
                    val icon = if (bucket.bucketName == "Favorites") {
                        ImageVector.vectorResource(R.drawable.ic_ms_favorite)
                    } else {
                        ImageVector.vectorResource(R.drawable.ic_ms_photo_album)
                    }
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = bucket.bucketName,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(IconSizeTokens.M)
                        )
                    }
                } else {
                    AsyncImage(
                        model = request,
                        contentDescription = bucket.bucketName,
                        contentScale = ContentScale.Crop,
                        onError = {
                            if (!loadFailed && bucket.defaultCoverUri != Uri.EMPTY && bucket.defaultCoverUri != bucket.coverUri) {
                                loadFailed = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(tileShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(SpacingTokens.M))

            // Metadata text column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    Text(
                        text = bucket.bucketName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isPinned) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_pin),
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(IconSizeTokens.S)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    val countText = remember(bucket.itemCount) {
                        val formatted = NumberFormat.getInstance(Locale.US).format(bucket.itemCount)
                        if (bucket.itemCount == 1) "1 item" else "$formatted items"
                    }
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showAlbumSize && bucket.totalSizeBytes > 0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = Formatter.formatShortFileSize(context, bucket.totalSizeBytes),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(SpacingTokens.S))

            // Trailing Count Pill Badge
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = "${bucket.itemCount}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = 2.dp)
                )
            }
        }
    }
}

// ── Reusable Album Context Menu ──

@Composable
fun AlbumDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    bucketName: String,
    isUserPinned: Boolean,
    hasCustomCover: Boolean,
    hasCustomShape: Boolean,
    onTogglePin: () -> Unit,
    onChangeCover: () -> Unit,
    onDeleteCover: () -> Unit,
    onChangeShape: () -> Unit,
    onResetShape: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = ShapeExtraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        if (bucketName != "All") {
            DropdownMenuItem(
                text = { Text(if (isUserPinned) "Unpin Album" else "Pin Album", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(if (isUserPinned) R.drawable.ic_ms_pin_off else R.drawable.ic_ms_pin),
                        contentDescription = null,
                        modifier = Modifier.size(IconSizeTokens.M)
                    )
                },
                onClick = {
                    onDismissRequest()
                    onTogglePin()
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
                onDismissRequest()
                onChangeCover()
            }
        )
        if (hasCustomCover) {
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
                    onDismissRequest()
                    onDeleteCover()
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
                onDismissRequest()
                onChangeShape()
            }
        )
        if (hasCustomShape) {
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
                    onDismissRequest()
                    onResetShape()
                }
            )
        }
    }
}

// ── Albums Control Bar (Layout Switcher & Custom Sorting) ──

@Composable
fun AlbumsControlBar(
    currentLayout: AlbumLayoutMode,
    onLayoutChange: (AlbumLayoutMode) -> Unit,
    currentSort: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    val sortLabel = when (currentSort) {
        SortOrder.NameAsc -> "Name (A-Z)"
        SortOrder.NameDesc -> "Name (Z-A)"
        SortOrder.NewToOld -> "Date (Newest)"
        SortOrder.OldToNew -> "Date (Oldest)"
        SortOrder.CountDesc -> "Count (Most)"
        SortOrder.CountAsc -> "Count (Least)"
        SortOrder.BigToSmall -> "Size (Largest)"
        SortOrder.SmallToBig -> "Size (Smallest)"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SpacingTokens.XS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort Dropdown Pill Button
        Box {
            Surface(
                onClick = { showSortMenu = true },
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .height(34.dp)
                    .pressScale(0.96f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_sort),
                        contentDescription = "Sort Albums",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSizeTokens.S)
                    )
                    Text(
                        text = sortLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_keyboard_arrow_down),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                shape = ShapeExtraLarge,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                // Name Sorts
                DropdownMenuItem(
                    text = { Text("Name (A to Z)", style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.NameAsc, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.NameAsc)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Name (Z to A)", style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.NameDesc, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.NameDesc)
                        showSortMenu = false
                    }
                )
                // Date Modified Sorts
                DropdownMenuItem(
                    text = { Text("Date Modified (Newest)", style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.NewToOld, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.NewToOld)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Date Modified (Oldest)", style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.OldToNew, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.OldToNew)
                        showSortMenu = false
                    }
                )
                // Item Count Sorts
                DropdownMenuItem(
                    text = { Text("Item Count (Most items)", style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.CountDesc, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.CountDesc)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Item Count (Least items)", style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.CountAsc, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.CountAsc)
                        showSortMenu = false
                    }
                )
            }
        }

        // Layout Toggle Pill Group (2-col, 3-col, List)
        Surface(
            shape = ShapeFull,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.height(34.dp)
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Grid Button (3 Columns)
                LayoutPillOption(
                    isSelected = currentLayout == AlbumLayoutMode.GRID,
                    iconRes = R.drawable.ic_ms_grid_view,
                    description = "Grid View",
                    onClick = { onLayoutChange(AlbumLayoutMode.GRID) }
                )

                // List Button
                LayoutPillOption(
                    isSelected = currentLayout == AlbumLayoutMode.LIST,
                    iconRes = R.drawable.ic_ms_list,
                    description = "List View",
                    onClick = { onLayoutChange(AlbumLayoutMode.LIST) }
                )
            }
        }
    }
}

@Composable
private fun LayoutPillOption(
    isSelected: Boolean,
    @DrawableRes iconRes: Int,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ShapeFull,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = description,
                modifier = Modifier.size(IconSizeTokens.S)
            )
        }
    }
}

// ── Segmented Collections Grid (Single Card Divided into Blocks) ──

data class CollectionBlockItem(
    val title: String,
    @DrawableRes val iconRes: Int,
    val count: Int? = null,
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(item.iconRes),
                                contentDescription = item.title,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(IconSizeTokens.L)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            item.count?.let { count ->
                                Surface(
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
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
    seeAllText: String = "Show all",
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
                    Modifier
                        .clip(ShapeMedium)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    text = seeAllText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}
