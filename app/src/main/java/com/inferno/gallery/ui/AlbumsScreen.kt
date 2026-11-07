@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.inferno.gallery.ui

import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.inferno.gallery.data.BucketNames
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.inferno.gallery.ui.components.FloatingTipCard
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.inferno.gallery.ui.utils.pressScale
import com.inferno.gallery.ui.components.PinnedQuadCover
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
    onNavigateToPeople: () -> Unit = {},
    onNavigateToAllAlbums: () -> Unit = {},
    onNavigateToPinnedAlbums: () -> Unit = {},
    onCreateCollage: (List<String>) -> Unit = {},
    onCreateStitch: (List<String>) -> Unit = {},
    onOpenEditor: (String) -> Unit = {},
    onChangeCover: (String) -> Unit = {},
    onDeleteCover: (String) -> Unit = {}
) {
    val albums by viewModel.allAlbums.collectAsState()
    val userPinnedAlbums by viewModel.userPinnedAlbums.collectAsState()
    val userPinnedNames by viewModel.userPinnedFolderNames.collectAsState()
    val albumSortOrder by viewModel.albumSortOrder.collectAsState()
    val albumLayoutMode by viewModel.albumLayoutMode.collectAsState()
    val showAlbumSize by viewModel.showAlbumSize.collectAsState()
    val customCovers by viewModel.albumCustomCovers.collectAsState()
    val customShapes by viewModel.albumCustomShapes.collectAsState()
    var shapePickerBucket by remember { mutableStateOf<AlbumBucket?>(null) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val showEmptyAlbums by viewModel.showEmptyAlbums.collectAsState()
    val albumsGridCellsCount by viewModel.albumsGridCellsCount.collectAsState()
    var isPinching by remember { mutableStateOf(false) }
    val livePinchScaleAnim = remember { Animatable(1f) }
    val pinchScaleScope = rememberCoroutineScope()

    val unpinnedAlbums = remember(albums, userPinnedNames, showEmptyAlbums) {
        albums.filter {
            it.bucketName != "Favorites" && it.bucketName != "Trash" && it.bucketName != "All" && it.bucketName !in userPinnedNames && (it.itemCount > 0 || showEmptyAlbums)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setTopBarCollapsed(false)
    }

    Box(modifier = modifier.fillMaxSize()) {
        key(albumLayoutMode, albumSortOrder) {
            val lazyGridState = rememberLazyGridState()

            // Ensure clean top alignment when albums populate or if previously pushed up
            var initialTopResetDone by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(unpinnedAlbums.isNotEmpty()) {
                if (unpinnedAlbums.isNotEmpty() && !initialTopResetDone) {
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

            LaunchedEffect(lazyGridState) {
                snapshotFlow {
                    lazyGridState.firstVisibleItemIndex > 0 || lazyGridState.firstVisibleItemScrollOffset > 32
                }
                    .distinctUntilChanged()
                    .collect { isScrolled ->
                        viewModel.setTopBarCollapsed(isScrolled)
                    }
            }

            val isRefreshing by viewModel.isRefreshing.collectAsState()
            val pullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()

            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshMedia() },
                modifier = Modifier.fillMaxSize(),
                state = pullToRefreshState,
                indicator = {
                    androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = contentPadding.calculateTopPadding())
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .albumsGridPinchZoom(
                            gridCellsCount = albumsGridCellsCount,
                            onGridCountChange = { newCols ->
                                viewModel.updateAlbumsGridCellsCount(newCols.coerceIn(2, 4))
                            },
                            enabled = albumLayoutMode == AlbumLayoutMode.GRID,
                            haptic = haptic,
                            onPinchScaleChange = { scale ->
                                isPinching = true
                                pinchScaleScope.launch {
                                    livePinchScaleAnim.snapTo(scale.coerceIn(0.75f, 1.30f))
                                }
                            },
                            onPinchEnd = {
                                pinchScaleScope.launch {
                                    livePinchScaleAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = 0.80f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                    isPinching = false
                                }
                            }
                        )
                ) {
                    LazyVerticalGrid(
                        columns = if (albumLayoutMode == AlbumLayoutMode.LIST) GridCells.Fixed(1) else GridCells.Fixed(albumsGridCellsCount),
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
                        horizontalArrangement = Arrangement.spacedBy(if (albumsGridCellsCount >= 4) SpacingTokens.S else SpacingTokens.M),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (albumLayoutMode == AlbumLayoutMode.GRID) {
                                    scaleX = livePinchScaleAnim.value
                                    scaleY = livePinchScaleAnim.value
                                }
                            }
                    ) {

                if (albumLayoutMode == AlbumLayoutMode.LIST) {
                    if (userPinnedAlbums.isNotEmpty()) {
                        item(key = "row_pinned_hub") {
                            PinnedHubListRow(
                                pinnedAlbums = userPinnedAlbums,
                                onClick = onNavigateToPinnedAlbums
                            )
                        }
                    }

                    items(
                        items = unpinnedAlbums,
                        key = { "album_${it.bucketName}" }
                    ) { bucket ->
                        val displayBucketName = when (bucket.bucketName) {
                            "Screenrecordings", "Screenrecords", "ScreenRecord" -> "Screen recordings"
                            else -> bucket.bucketName
                        }
                        val isPinned = bucket.bucketName in userPinnedNames
                        var showCardMenu by remember { mutableStateOf(false) }

                        val albumShape = remember(customShapes, bucket.bucketName) {
                            AlbumShapes.getShape(customShapes[bucket.bucketName])
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            AlbumListRow(
                                bucket = bucket.copy(bucketName = displayBucketName),
                                showAlbumSize = showAlbumSize,
                                customShape = albumShape,
                                isPinned = isPinned,
                                onClick = { onAlbumClick(bucket.bucketName) },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showCardMenu = true
                                }
                            )

                            // Context menu for albums
                            AlbumDropdownMenu(
                                expanded = showCardMenu,
                                onDismissRequest = { showCardMenu = false },
                                bucketName = bucket.bucketName,
                                isUserPinned = isPinned,
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
                } else {
                    if (userPinnedAlbums.isNotEmpty()) {
                        item(key = "card_pinned_hub") {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                PinnedHubCard(
                                    pinnedAlbums = userPinnedAlbums,
                                    isCompact = albumsGridCellsCount >= 3,
                                    onClick = onNavigateToPinnedAlbums
                                )
                            }
                        }
                    }

                    items(
                        items = unpinnedAlbums,
                        key = { "album_${it.bucketName}" }
                    ) { bucket ->
                        val displayBucketName = when (bucket.bucketName) {
                            "Screenrecordings", "Screenrecords", "ScreenRecord" -> "Screen recordings"
                            else -> bucket.bucketName
                        }
                        val isPinned = bucket.bucketName in userPinnedNames
                        var showCardMenu by remember { mutableStateOf(false) }

                        val albumShape = remember(customShapes, bucket.bucketName) {
                            AlbumShapes.getShape(customShapes[bucket.bucketName])
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            AlbumCard(
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                bucket = bucket.copy(bucketName = displayBucketName),
                                showAlbumSize = showAlbumSize && albumsGridCellsCount < 4,
                                customShape = albumShape,
                                isCompact = albumsGridCellsCount >= 3,
                                isPinned = isPinned,
                                onClick = { onAlbumClick(bucket.bucketName) },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showCardMenu = true
                                }
                            )

                            AlbumDropdownMenu(
                                expanded = showCardMenu,
                                onDismissRequest = { showCardMenu = false },
                                bucketName = bucket.bucketName,
                                isUserPinned = isPinned,
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

    val tipAlbumsDismissed by viewModel.settingsRepository.tipAlbumsCustomizeDismissedFlow.collectAsState(initial = true)
    val coroutineScope = rememberCoroutineScope()

    if (!tipAlbumsDismissed) {
        FloatingTipCard(
            title = stringResource(R.string.quick_tip),
            message = stringResource(R.string.tip_albums_customize),
            iconResId = R.drawable.ic_ms_auto_fix_high,
            onDismiss = {
                coroutineScope.launch {
                    viewModel.settingsRepository.dismissTip(com.inferno.gallery.data.SettingsRepository.TIP_ALBUMS_CUSTOMIZE)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + SpacingTokens.M)
        )
    }
}
}

// ── Utility Hub Data Item ──

data class UtilityItem(
    val title: String,
    @DrawableRes val iconRes: Int,
    val count: Int? = null,
    val onClick: () -> Unit
)

@Composable
fun UtilityCardRow(
    items: List<UtilityItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = SpacingTokens.XS),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
    ) {
        items.forEachIndexed { index, item ->
            val cardShape = when (index) {
                0 -> RoundedCornerShape(
                    topStart = 16.dp,
                    bottomStart = 16.dp,
                    topEnd = 12.dp,
                    bottomEnd = 12.dp
                )
                items.lastIndex -> RoundedCornerShape(
                    topStart = 12.dp,
                    bottomStart = 12.dp,
                    topEnd = 16.dp,
                    bottomEnd = 16.dp
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
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.surface,
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
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.15).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
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
                                    top = if (index == items.lastIndex) 8.dp else 6.dp,
                                    end = if (index == items.lastIndex) 10.dp else 6.dp
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

@Composable
fun TrashRow(
    trashCount: Int,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onAlbumClick("Trash") },
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .pressScale(pressedScale = 0.98f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shadowElevation = 0.dp
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                        contentDescription = stringResource(R.string.album_trash),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.album_trash),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            if (trashCount > 0) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = "$trashCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(IconSizeTokens.M)
            )
        }
    }
}

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
    val cardShape = if (isCompact) ShapeLarge else ShapeLargeIncreased
    val defaultCoverShape = if (isCompact) ShapeSmall else ShapeMedium
    val tileShape = customShape ?: defaultCoverShape

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

    // Outer Card Container (Enclosed card — clean tonal, no outlines)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.96f)
            .clip(cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isCompact) 6.dp else 8.dp,
                    top = if (isCompact) 6.dp else 8.dp,
                    end = if (isCompact) 6.dp else 8.dp,
                    bottom = if (isCompact) 8.dp else 10.dp
                ),
            horizontalAlignment = Alignment.Start
        ) {
            // Inner Media Thumbnail Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(tileShape),
                shape = tileShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 0.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (effectiveUri == Uri.EMPTY) {
                        val icon = if (bucket.bucketName == "Favorites") {
                            ImageVector.vectorResource(R.drawable.ic_ms_favorite)
                        } else {
                            ImageVector.vectorResource(R.drawable.ic_ms_photo_album)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(if (isCompact) 32.dp else 40.dp),
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
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            ) {
                Text(
                    text = bucket.bucketName,
                    style = if (isCompact) {
                        MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
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
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            ) {
                val countText = remember(bucket.itemCount) {
                    val formatted = NumberFormat.getInstance(Locale.US).format(bucket.itemCount)
                    if (bucket.itemCount == 1) "1 item" else "$formatted items"
                }

                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (showAlbumSize && bucket.totalSizeBytes > 0) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                    Text(
                        text = Formatter.formatShortFileSize(context, bucket.totalSizeBytes),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }
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
    val tileShape = if (customShape == null || customShape == ShapeLarge) ShapeMedium else customShape

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
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )

                    if (showAlbumSize && bucket.totalSizeBytes > 0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                        Text(
                            text = Formatter.formatShortFileSize(context, bucket.totalSizeBytes),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }

        }
    }
}

// ── Pinned Albums Collection Hub Card & Row ──

@Composable
fun PinnedHubCard(
    pinnedAlbums: List<AlbumBucket>,
    modifier: Modifier = Modifier,
    isCompact: Boolean = true,
    onClick: () -> Unit = {}
) {
    val cardShape = if (isCompact) ShapeLarge else ShapeLargeIncreased
    val defaultCoverShape = if (isCompact) ShapeSmall else ShapeMedium

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.96f)
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isCompact) 6.dp else 8.dp,
                    top = if (isCompact) 6.dp else 8.dp,
                    end = if (isCompact) 6.dp else 8.dp,
                    bottom = if (isCompact) 8.dp else 10.dp
                ),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(defaultCoverShape)
            ) {
                PinnedQuadCover(
                    pinnedAlbums = pinnedAlbums,
                    tileShape = defaultCoverShape,
                    showPinBadge = true
                )
            }

            Spacer(modifier = Modifier.height(SpacingTokens.XS))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.section_pinned),
                    style = if (isCompact) {
                        MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_pin),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(IconSizeTokens.S)
                )
            }

            val countText = if (pinnedAlbums.size == 1) "1 album" else "${pinnedAlbums.size} albums"
            Text(
                text = countText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
fun PinnedHubListRow(
    pinnedAlbums: List<AlbumBucket>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.98f)
            .clip(ShapeLarge)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.S),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(ShapeMedium)
            ) {
                PinnedQuadCover(
                    pinnedAlbums = pinnedAlbums,
                    tileShape = ShapeMedium,
                    showPinBadge = false
                )
            }

            Spacer(modifier = Modifier.width(SpacingTokens.M))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    Text(
                        text = stringResource(R.string.section_pinned),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_pin),
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSizeTokens.S)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                val countText = if (pinnedAlbums.size == 1) "1 album" else "${pinnedAlbums.size} albums"
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(IconSizeTokens.M)
            )
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
            text = { Text(stringResource(R.string.action_change_cover), style = MaterialTheme.typography.bodyMedium) },
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
                text = { Text(stringResource(R.string.action_reset_cover), style = MaterialTheme.typography.bodyMedium) },
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
            text = { Text(stringResource(R.string.action_change_shape), style = MaterialTheme.typography.bodyMedium) },
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
                text = { Text(stringResource(R.string.action_reset_shape), style = MaterialTheme.typography.bodyMedium) },
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
        SortOrder.NewToOld -> "Date Taken (Newest)"
        SortOrder.OldToNew -> "Date Taken (Oldest)"
        SortOrder.DateModifiedNewToOld -> "Date Added (Newest)"
        SortOrder.DateModifiedOldToNew -> "Date Added (Oldest)"
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
                    text = { Text(stringResource(R.string.sort_name_a_to_z_short), style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.NameAsc, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.NameAsc)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_name_z_to_a_short), style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.NameDesc, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.NameDesc)
                        showSortMenu = false
                    }
                )
                // Date Added / Modified Sorts
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_date_added_newest), style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.DateModifiedNewToOld, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.DateModifiedNewToOld)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_date_added_oldest), style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.DateModifiedOldToNew, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.DateModifiedOldToNew)
                        showSortMenu = false
                    }
                )
                // Date Taken Sorts
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_date_taken_newest), style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.NewToOld, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.NewToOld)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_date_taken_oldest), style = MaterialTheme.typography.bodyMedium) },
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
                    text = { Text(stringResource(R.string.sort_count_most), style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        RadioButton(selected = currentSort == SortOrder.CountDesc, onClick = null)
                    },
                    onClick = {
                        onSortChange(SortOrder.CountDesc)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_count_least), style = MaterialTheme.typography.bodyMedium) },
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
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(item.iconRes),
                                contentDescription = item.title,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.1).sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            item.count?.let { count ->
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
    seeAllText: String = stringResource(R.string.action_show_all),
    onToggle: () -> Unit = {},
    onSeeAll: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(top = SpacingTokens.S, bottom = SpacingTokens.XS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .then(
                    if (isCollapsible) {
                        Modifier
                            .clip(ShapeMedium)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggle()
                            }
                            .padding(horizontal = SpacingTokens.XS, vertical = SpacingTokens.XS)
                    } else {
                        Modifier.padding(horizontal = SpacingTokens.XS, vertical = SpacingTokens.XS)
                    }
                ),
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

/**
 * Smooth two-finger pinch-to-zoom gesture detector for Albums grid (min 2, max 4 columns).
 * Spreading fingers decreases columns (zooms in: 4 -> 3 -> 2),
 * pinching fingers increases columns (zooms out: 2 -> 3 -> 4).
 */
internal fun Modifier.albumsGridPinchZoom(
    gridCellsCount: Int,
    onGridCountChange: (Int) -> Unit,
    enabled: Boolean,
    haptic: HapticFeedback,
    onPinchScaleChange: (scale: Float) -> Unit,
    onPinchEnd: () -> Unit
): Modifier = composed {
    val currentGridCount by rememberUpdatedState(gridCellsCount)
    val currentOnGridCountChange by rememberUpdatedState(onGridCountChange)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnPinchScaleChange by rememberUpdatedState(onPinchScaleChange)
    val currentOnPinchEnd by rememberUpdatedState(onPinchEnd)

    Modifier.pointerInput(Unit) {
        val touchSlop = viewConfiguration.touchSlop

        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

            if (!currentEnabled) return@awaitEachGesture

            var initialDistance = -1f
            var pinchActive = false
            var pinchEverActivated = false
            var accumulatedScale = 1.0f
            var lastChangeTime = 0L

            try {
                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val changes = event.changes

                    if (changes.size < 2 && !pinchEverActivated) {
                        continue
                    }

                    val pressedChanges = changes.filter { it.pressed }

                    if (pressedChanges.size >= 2) {
                        val p1 = pressedChanges[0]
                        val p2 = pressedChanges[1]

                        val currDist = kotlin.math.hypot(p1.position.x - p2.position.x, p1.position.y - p2.position.y)

                        if (initialDistance <= 0f) {
                            initialDistance = currDist
                        } else {
                            val distDiff = kotlin.math.abs(currDist - initialDistance)
                            val rawScale = currDist / initialDistance

                            if (!pinchActive && distDiff > touchSlop * 1.25f && (rawScale > 1.05f || rawScale < 0.95f)) {
                                pinchActive = true
                                pinchEverActivated = true
                            }

                            if (pinchActive) {
                                event.changes.forEach { it.consume() }

                                if (p1.previousPressed && p2.previousPressed) {
                                    val prevDist = kotlin.math.hypot(p1.previousPosition.x - p2.previousPosition.x, p1.previousPosition.y - p2.previousPosition.y)
                                    if (prevDist > 1f && currDist > 1f) {
                                        val zoomDelta = currDist / prevDist
                                        if (zoomDelta in 0.6f..1.6f) {
                                            accumulatedScale *= zoomDelta
                                            currentOnPinchScaleChange(accumulatedScale)

                                            val now = System.currentTimeMillis()
                                            // Spreading fingers apart -> fewer columns (zooms in: 4 -> 3 -> 2, min 2)
                                            if (accumulatedScale >= 1.15f && now - lastChangeTime > 240L) {
                                                val currentCols = currentGridCount
                                                if (currentCols > 2) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    currentOnGridCountChange(currentCols - 1)
                                                    lastChangeTime = now
                                                    accumulatedScale = 1.0f
                                                    currentOnPinchScaleChange(1.0f)
                                                }
                                            }
                                            // Pinching fingers together -> more columns (zooms out: 2 -> 3 -> 4, max 4)
                                            else if (accumulatedScale <= 0.85f && now - lastChangeTime > 240L) {
                                                val currentCols = currentGridCount
                                                if (currentCols < 4) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    currentOnGridCountChange(currentCols + 1)
                                                    lastChangeTime = now
                                                    accumulatedScale = 1.0f
                                                    currentOnPinchScaleChange(1.0f)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (pinchActive) {
                        event.changes.forEach { it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            } finally {
                if (pinchActive) {
                    currentOnPinchEnd()
                }
            }
        }
    }
}
