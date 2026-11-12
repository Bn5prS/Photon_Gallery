@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.inferno.gallery.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.inferno.gallery.R
import com.inferno.gallery.data.BucketNames
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionsScreen(
    modifier: Modifier = Modifier,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
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
    onCreateCollage: (List<String>) -> Unit = {},
    onCreateStitch: (List<String>) -> Unit = {},
    onOpenEditor: (String) -> Unit = {}
) {
    val context = LocalContext.current

    // ── 1. People & Pets ──
    val peopleExpanded by viewModel.albumsExpandedPeople.collectAsState()
    val peopleViewModel: com.inferno.gallery.ui.people.PeopleViewModel = run {
        var cur: Any? = LocalContext.current
        while (cur is android.content.ContextWrapper) {
            if (cur is androidx.lifecycle.ViewModelStoreOwner) break
            cur = cur.baseContext
        }
        androidx.lifecycle.viewmodel.compose.viewModel(
            viewModelStoreOwner = cur as androidx.lifecycle.ViewModelStoreOwner
        )
    }
    val allPeople by peopleViewModel.allPersons.collectAsState()
    val people = allPeople

    // ── 2. Places ──
    val placesClusters by viewModel.placesClusters.collectAsState()
    val placesExpanded by viewModel.albumsExpandedPlaces.collectAsState()

    // ── 3. Documents ──
    val documentBuckets by viewModel.documentBuckets.collectAsState()
    val documentsExpanded by viewModel.albumsExpandedDocuments.collectAsState()

    // ── 4. Capture Modes ──
    val mediaTypeBuckets by viewModel.mediaTypeBuckets.collectAsState()
    val mediaTypesExpanded by viewModel.albumsExpandedMediaTypes.collectAsState()

    // ── 5. Utilities & Tools ──
    val favoriteItems by viewModel.favoriteMedia.collectAsState()
    val trashCount by viewModel.trashCount.collectAsState()
    val vaultItemCount by viewModel.vaultItemCount.collectAsState()
    val videoCount by viewModel.videoCount.collectAsState()
    val albums by viewModel.allAlbums.collectAsState()
    val screenshotsCount = remember(albums) {
        albums.firstOrNull { it.bucketName.equals("Screenshots", ignoreCase = true) }?.itemCount
            ?: albums.filter { it.bucketName.contains("Screenshot", ignoreCase = true) }.sumOf { it.itemCount }
    }
    val utilitiesExpanded by viewModel.albumsExpandedUtilities.collectAsState()

    // ── Action Launchers ──
    val selectAtLeastTwoToast = stringResource(R.string.toast_select_at_least_two_photos)
    val pickCollageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        if (uris.size >= 2) {
            onCreateCollage(uris.map { it.toString() })
        } else if (uris.size == 1) {
            Toast.makeText(context, selectAtLeastTwoToast, Toast.LENGTH_SHORT).show()
        }
    }

    val pickStitchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.size >= 2) {
            onCreateStitch(uris.map { it.toString() })
        } else if (uris.size == 1) {
            Toast.makeText(context, selectAtLeastTwoToast, Toast.LENGTH_SHORT).show()
        }
    }

    val pickEditorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onOpenEditor(uri.toString())
        }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(viewModel) {
        viewModel.collectionsScrollToTopTrigger.collect {
            lazyListState.animateScrollToItem(0, 0)
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 32
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
        modifier = modifier.fillMaxSize(),
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
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                start = SpacingTokens.L,
                end = SpacingTokens.L,
                top = contentPadding.calculateTopPadding() + SpacingTokens.M,
                bottom = contentPadding.calculateBottomPadding() + SpacingTokens.XXL
            ),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.L),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── 1. People Carousel on TOP ──
            item(key = "section_people") {
                com.inferno.gallery.ui.people.PeopleCarousel(
                    people = people,
                    isExpanded = peopleExpanded,
                    onToggle = { viewModel.toggleAlbumsExpandedPeople() },
                    onSeeAll = onNavigateToPeople,
                    onPersonClick = { personId -> onPersonClick(personId.toString()) }
                )
            }

            // ── 2. Places Carousel ──
            item(key = "header_places") {
                SectionHeader(
                    title = stringResource(R.string.section_places),
                    count = if (placesClusters.isNotEmpty()) placesClusters.size else null,
                    isExpanded = placesExpanded,
                    onToggle = { viewModel.toggleAlbumsExpandedPlaces() },
                    seeAllText = stringResource(R.string.action_show_all),
                    onSeeAll = onNavigateToPlacesList
                )
            }

            item(key = "content_places") {
                AnimatedVisibility(
                    visible = placesExpanded,
                    enter = expandVertically(animationSpec = MotionTokens.snappySpring()) + fadeIn(animationSpec = MotionTokens.snappySpring()),
                    exit = shrinkVertically(animationSpec = MotionTokens.snappySpring()) + fadeOut(animationSpec = MotionTokens.snappySpring())
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = SpacingTokens.XS),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                    ) {
                        item {
                            // "Explore Map" Hero Card
                            Surface(
                                onClick = onNavigateToPhotoMap,
                                shape = ShapeLargeIncreased,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .width(120.dp)
                                    .pressScale(pressedScale = 0.96f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(ShapeMedium),
                                        shape = ShapeMedium,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shadowElevation = 0.dp
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            androidx.compose.foundation.Image(
                                                painter = painterResource(R.drawable.map_bg_placeholder),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Surface(
                                                shape = ShapeFull,
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(36.dp),
                                                shadowElevation = 0.dp
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_location_on),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(IconSizeTokens.M),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(SpacingTokens.XS))
                                    Text(
                                        text = stringResource(R.string.explore_map),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }

                        items(placesClusters, key = { "place_${it.bucketName}" }) { cluster ->
                            Surface(
                                onClick = { onAlbumClick("place:${cluster.bucketName}") },
                                shape = ShapeLargeIncreased,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .width(120.dp)
                                    .pressScale(pressedScale = 0.96f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(ShapeMedium),
                                        shape = ShapeMedium,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shadowElevation = 0.dp
                                    ) {
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
                                    }
                                    Spacer(modifier = Modifier.height(SpacingTokens.XS))
                                    Text(
                                        text = cluster.bucketName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 3. Documents ──
            if (documentBuckets.isNotEmpty()) {
                item(key = "header_documents") {
                    SectionHeader(
                        title = stringResource(R.string.section_documents),
                        count = documentBuckets.size,
                        isExpanded = documentsExpanded,
                        onToggle = { viewModel.toggleAlbumsExpandedDocuments() }
                    )
                }

                item(key = "content_documents") {
                    AnimatedVisibility(
                        visible = documentsExpanded,
                        enter = expandVertically(animationSpec = MotionTokens.snappySpring()) + fadeIn(animationSpec = MotionTokens.snappySpring()),
                        exit = shrinkVertically(animationSpec = MotionTokens.snappySpring()) + fadeOut(animationSpec = MotionTokens.snappySpring())
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                            contentPadding = PaddingValues(horizontal = SpacingTokens.XS)
                        ) {
                            items(documentBuckets, key = { "document_${it.bucketName}" }) { docBucket ->
                                val title = when (docBucket.bucketName) {
                                    BucketNames.DOC_BOOKS_MAGAZINES -> stringResource(R.string.doc_books_magazines)
                                    BucketNames.DOC_EVENTS -> stringResource(R.string.doc_events)
                                    BucketNames.DOC_IDS -> stringResource(R.string.doc_ids)
                                    BucketNames.DOC_NOTES -> stringResource(R.string.doc_notes)
                                    BucketNames.DOC_PAYMENTS -> stringResource(R.string.doc_payments)
                                    BucketNames.DOC_RECEIPTS -> stringResource(R.string.doc_receipts)
                                    BucketNames.DOC_SOCIAL -> stringResource(R.string.doc_social)
                                    else -> stringResource(R.string.doc_documents)
                                }
                                val iconRes = when (docBucket.bucketName) {
                                    BucketNames.DOC_BOOKS_MAGAZINES -> R.drawable.ic_ms_menu_book
                                    BucketNames.DOC_EVENTS -> R.drawable.ic_ms_confirmation_number
                                    BucketNames.DOC_IDS -> R.drawable.ic_ms_badge
                                    BucketNames.DOC_NOTES -> R.drawable.ic_ms_edit_note
                                    BucketNames.DOC_PAYMENTS -> R.drawable.ic_ms_payments
                                    BucketNames.DOC_RECEIPTS -> R.drawable.ic_ms_receipt_long
                                    BucketNames.DOC_SOCIAL -> R.drawable.ic_ms_share
                                    else -> R.drawable.ic_ms_description
                                }

                                Surface(
                                    onClick = { onAlbumClick(docBucket.bucketName) },
                                    shape = ShapeLargeIncreased,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shadowElevation = 0.dp,
                                    modifier = Modifier
                                        .width(120.dp)
                                        .pressScale(pressedScale = 0.96f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(ShapeMedium),
                                            shape = ShapeMedium,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shadowElevation = 0.dp
                                        ) {
                                            if (docBucket.coverUri != Uri.EMPTY) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(docBucket.coverUri)
                                                        .size(280)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Surface(
                                                        shape = ShapeFull,
                                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                        modifier = Modifier.size(36.dp),
                                                        shadowElevation = 0.dp
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = ImageVector.vectorResource(iconRes),
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(IconSizeTokens.M)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(SpacingTokens.XS))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.media_count_multiple, docBucket.itemCount),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 4. Capture Modes ──
            if (mediaTypeBuckets.isNotEmpty()) {
                item(key = "header_media_types") {
                    SectionHeader(
                        title = stringResource(R.string.section_capture_modes),
                        count = mediaTypeBuckets.size,
                        isExpanded = mediaTypesExpanded,
                        onToggle = { viewModel.toggleAlbumsExpandedMediaTypes() }
                    )
                }

                item(key = "content_media_types") {
                    AnimatedVisibility(
                        visible = mediaTypesExpanded,
                        enter = expandVertically(animationSpec = MotionTokens.snappySpring()) + fadeIn(animationSpec = MotionTokens.snappySpring()),
                        exit = shrinkVertically(animationSpec = MotionTokens.snappySpring()) + fadeOut(animationSpec = MotionTokens.snappySpring())
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                            contentPadding = PaddingValues(horizontal = SpacingTokens.XS)
                        ) {
                            items(mediaTypeBuckets, key = { "media_type_${it.bucketName}" }) { mediaType ->
                                val title = when (mediaType.bucketName) {
                                    BucketNames.MEDIA_TYPE_RAW -> stringResource(R.string.mode_raw)
                                    BucketNames.MEDIA_TYPE_PANORAMAS -> stringResource(R.string.mode_panoramas)
                                    BucketNames.MEDIA_TYPE_SLOW_MO -> stringResource(R.string.mode_slow_mo)
                                    BucketNames.MEDIA_TYPE_ANIMATIONS -> stringResource(R.string.mode_animations)
                                    BucketNames.MEDIA_TYPE_NIGHT -> stringResource(R.string.mode_night)
                                    BucketNames.MEDIA_TYPE_PHOTO_SPHERE -> stringResource(R.string.mode_photo_sphere)
                                    BucketNames.MEDIA_TYPE_PORTRAIT -> stringResource(R.string.mode_portrait)
                                    BucketNames.MEDIA_TYPE_MACRO -> stringResource(R.string.mode_macro)
                                    BucketNames.MEDIA_TYPE_TIMELAPSE -> stringResource(R.string.mode_timelapse)
                                    else -> stringResource(R.string.nav_photos)
                                }
                                val iconRes = when (mediaType.bucketName) {
                                    BucketNames.MEDIA_TYPE_RAW -> R.drawable.ic_ms_photo_camera
                                    BucketNames.MEDIA_TYPE_PANORAMAS -> R.drawable.ic_ms_aspect_ratio
                                    BucketNames.MEDIA_TYPE_SLOW_MO -> R.drawable.ic_ms_schedule
                                    BucketNames.MEDIA_TYPE_ANIMATIONS -> R.drawable.ic_ms_auto_fix_high
                                    BucketNames.MEDIA_TYPE_NIGHT -> R.drawable.ic_ms_wb_twilight
                                    BucketNames.MEDIA_TYPE_PHOTO_SPHERE -> R.drawable.ic_ms_vrpano
                                    BucketNames.MEDIA_TYPE_PORTRAIT -> R.drawable.ic_ms_person
                                    BucketNames.MEDIA_TYPE_MACRO -> R.drawable.ic_ms_filter_vintage
                                    BucketNames.MEDIA_TYPE_TIMELAPSE -> R.drawable.ic_ms_timelapse
                                    else -> R.drawable.ic_ms_image
                                }

                                Surface(
                                    onClick = { onAlbumClick(mediaType.bucketName) },
                                    shape = ShapeLargeIncreased,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shadowElevation = 0.dp,
                                    modifier = Modifier
                                        .width(160.dp)
                                        .pressScale(pressedScale = 0.96f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .clip(ShapeMedium),
                                            shape = ShapeMedium,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shadowElevation = 0.dp
                                        ) {
                                            if (mediaType.coverUri != Uri.EMPTY) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(mediaType.coverUri)
                                                        .size(360)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Surface(
                                                        shape = ShapeFull,
                                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                        modifier = Modifier.size(36.dp),
                                                        shadowElevation = 0.dp
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = ImageVector.vectorResource(iconRes),
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(IconSizeTokens.M)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(SpacingTokens.XS))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.media_count_multiple, mediaType.itemCount),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 5. Utilities & Tools ──
            item(key = "header_more_tools") {
                SectionHeader(
                    title = stringResource(R.string.section_utilities),
                    isExpanded = utilitiesExpanded,
                    onToggle = { viewModel.toggleAlbumsExpandedUtilities() },
                    isCollapsible = true
                )
            }

            item(key = "content_utilities_and_tools") {
                val favTitle = stringResource(R.string.album_favorites)
                val screenTitle = stringResource(R.string.album_screenshots)
                val vidTitle = stringResource(R.string.album_videos)
                val vaultTitle = stringResource(R.string.album_vault)
                val vaultProtected = stringResource(R.string.vault_status_protected)

                val utilities = remember(favoriteItems.size, screenshotsCount, videoCount, vaultItemCount, favTitle, screenTitle, vidTitle, vaultTitle, vaultProtected) {
                    listOf(
                        UtilityItem(
                            title = favTitle,
                            iconRes = R.drawable.ic_ms_star,
                            count = favoriteItems.size,
                            subtitle = if (favoriteItems.size == 1) "1 photo" else "${favoriteItems.size} photos",
                            onClick = { onAlbumClick("Favorites") }
                        ),
                        UtilityItem(
                            title = screenTitle,
                            iconRes = R.drawable.ic_ms_aspect_ratio,
                            count = screenshotsCount,
                            subtitle = if (screenshotsCount == 1) "1 item" else "$screenshotsCount items",
                            onClick = { onAlbumClick("Screenshots") }
                        ),
                        UtilityItem(
                            title = vidTitle,
                            iconRes = R.drawable.ic_ms_play_arrow,
                            count = videoCount,
                            subtitle = if (videoCount == 1) "1 clip" else "$videoCount clips",
                            onClick = { onAlbumClick("Videos") }
                        ),
                        UtilityItem(
                            title = vaultTitle,
                            iconRes = R.drawable.ic_ms_lock,
                            count = vaultItemCount,
                            subtitle = vaultProtected,
                            onClick = {
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                if (activity != null) {
                                    if (viewModel.vaultAuthManager.isPinConfigured(activity)) {
                                        if (viewModel.vaultAuthManager.canAuthenticateWithBiometrics(activity)) {
                                            viewModel.vaultAuthManager.authenticate(
                                                activity = activity,
                                                negativeButtonText = "Use PIN",
                                                onSuccess = onNavigateToVault,
                                                onNegativeButton = onNavigateToVault,
                                                onFailure = { onNavigateToVault() }
                                            )
                                        } else {
                                            onNavigateToVault()
                                        }
                                    } else {
                                        onNavigateToVault()
                                    }
                                } else {
                                    onNavigateToVault()
                                }
                            }
                        )
                    )
                }

                val collageTitle = stringResource(R.string.action_collage)
                val stitchTitle = stringResource(R.string.action_stitch)
                val editorTitle = stringResource(R.string.action_edit)
                val cleanUpTitle = stringResource(R.string.utility_clean_up)
                val layoutsSub = stringResource(R.string.tool_sub_layouts)
                val mergeSub = stringResource(R.string.tool_sub_merge)
                val adjustSub = stringResource(R.string.tool_sub_adjust)
                val duplicatesSub = stringResource(R.string.tool_sub_duplicates)

                val tools = remember(collageTitle, stitchTitle, editorTitle, cleanUpTitle, layoutsSub, mergeSub, adjustSub, duplicatesSub) {
                    listOf(
                        UtilityItem(
                            title = collageTitle,
                            iconRes = R.drawable.ic_ms_dashboard,
                            subtitle = layoutsSub,
                            onClick = {
                                pickCollageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ),
                        UtilityItem(
                            title = stitchTitle,
                            iconRes = R.drawable.ic_ms_merge,
                            subtitle = mergeSub,
                            onClick = {
                                pickStitchLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ),
                        UtilityItem(
                            title = editorTitle,
                            iconRes = R.drawable.ic_ms_tune,
                            subtitle = adjustSub,
                            onClick = {
                                pickEditorLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ),
                        UtilityItem(
                            title = cleanUpTitle,
                            iconRes = R.drawable.ic_ms_cleaning_services,
                            subtitle = duplicatesSub,
                            onClick = onNavigateToDuplicateCleaner
                        )
                    )
                }

                val allUtilitiesAndTools = remember(utilities, tools) {
                    utilities + tools
                }

                AnimatedVisibility(
                    visible = utilitiesExpanded,
                    enter = expandVertically(animationSpec = MotionTokens.snappySpring()) + fadeIn(animationSpec = MotionTokens.snappySpring()),
                    exit = shrinkVertically(animationSpec = MotionTokens.snappySpring()) + fadeOut(animationSpec = MotionTokens.snappySpring())
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UtilityGrid(items = allUtilitiesAndTools)
                        TrashRow(trashCount = trashCount, onAlbumClick = onAlbumClick)
                    }
                }
            }
        }
    }
}
