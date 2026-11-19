package com.inferno.gallery.ui

import androidx.compose.ui.res.stringResource

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.inferno.gallery.data.db.PersonEntity
import com.inferno.gallery.ui.theme.ShapeMedium
import java.io.File
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inferno.gallery.ui.components.PhotonEmptyState
import com.inferno.gallery.ui.components.PhotonSectionHeader
import com.inferno.gallery.ui.theme.ExpressiveShapes
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector


@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?, String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onAlbumClick: (String) -> Unit = {},
    onPersonClick: (Long) -> Unit = {},
    onOpenSmartSearchSettings: () -> Unit = {}
) {
    val query by viewModel.searchQuery.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val ftsResults by viewModel.ftsSearchResults.collectAsState()
    val smartResults by viewModel.smartSearchResults.collectAsState()
    val matchingAlbums by viewModel.matchingAlbums.collectAsState()
    val matchingPeople by viewModel.matchingPeople.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val smartSearchStatus by viewModel.smartSearchStatus.collectAsState()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val hasAnyResults = ftsResults.isNotEmpty() || smartResults.isNotEmpty() || matchingAlbums.isNotEmpty() || (query.isNotBlank() && matchingPeople.isNotEmpty())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // ── Active Search Bar (Filled Tonal Container) ──────────────────────────
        val focusRequester = remember { FocusRequester() }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(56.dp),
            shape = ShapeFull,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            if (query.isNotBlank()) {
                                viewModel.submitSearch(query)
                            }
                        }
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search scenes, names, albums, dates, text…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (query.isNotEmpty()) {
                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.updateSearchQuery("")
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                            contentDescription = "Clear search",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Subtle inline refresh indicator while refining an active search without collapsing results.
        AnimatedVisibility(visible = isSearching && query.isNotBlank() && hasAnyResults) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )
        }

        // ── Results Area ────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = Triple(query.isBlank(), isSearching && !hasAnyResults, hasAnyResults),
            transitionSpec = {
                (fadeIn(MotionTokens.gentleSpring()) +
                    scaleIn(MotionTokens.gentleSpring(), initialScale = 0.95f))
                    .togetherWith(fadeOut(MotionTokens.gentleSpring()) +
                        scaleOut(MotionTokens.gentleSpring(), targetScale = 0.95f))
            },
            label = "searchContentTransition",
            modifier = Modifier.fillMaxSize()
        ) { (isBlank, searchingEmpty, hasResults) ->
            when {
                isBlank -> EmptySearchState(
                    recentSearches = recentSearches,
                    topPeople = matchingPeople,
                    smartSearchStatus = smartSearchStatus,
                    onPersonClick = onPersonClick,
                    onRecentSearchClick = { recent ->
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        viewModel.submitSearch(recent)
                    },
                    onOpenSmartSearchSettings = onOpenSmartSearchSettings,
                    onStartIndexing = { viewModel.startSmartSearchIndexing() },
                    viewModel = viewModel
                )
                searchingEmpty -> SearchingState()
                !hasResults -> NoResultsState(
                    query = query,
                    smartSearchStatus = smartSearchStatus,
                    onClear = { viewModel.updateSearchQuery("") },
                    onOpenSmartSearchSettings = onOpenSmartSearchSettings,
                    onStartIndexing = { viewModel.startSmartSearchIndexing() }
                )
                else -> SearchResultsList(
                    ftsResults = ftsResults,
                    smartResults = smartResults,
                    matchingAlbums = matchingAlbums,
                    matchingPeople = matchingPeople,
                    smartSearchStatus = smartSearchStatus,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onPhotoClick = onPhotoClick,
                    onAlbumClick = onAlbumClick,
                    onPersonClick = onPersonClick,
                    onOpenSmartSearchSettings = onOpenSmartSearchSettings,
                    onStartIndexing = { viewModel.startSmartSearchIndexing() },
                    query = query,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    recentSearches: List<String>,
    topPeople: List<PersonEntity>,
    smartSearchStatus: SmartSearchStatus,
    onPersonClick: (Long) -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onOpenSmartSearchSettings: () -> Unit,
    onStartIndexing: () -> Unit,
    viewModel: GalleryViewModel
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!smartSearchStatus.modelDownloaded || smartSearchStatus.isModelDownloading || smartSearchStatus.isIndexing ||
            (smartSearchStatus.totalCount > 0 && smartSearchStatus.indexedCount < smartSearchStatus.totalCount)
        ) {
            item(key = "semantic_status_banner") {
                SemanticStatusBanner(
                    status = smartSearchStatus,
                    onOpenSettings = onOpenSmartSearchSettings,
                    onStartIndexing = onStartIndexing
                )
            }
        }

        if (topPeople.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "People",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(topPeople, key = { it.id }) { person ->
                        PersonSearchCard(
                            person = person,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                person.name?.takeIf { it.isNotBlank() }?.let { viewModel.addRecentSearch(it) }
                                onPersonClick(person.id)
                            }
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent searches",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.clearRecentSearches()
                        },
                        shape = ShapeFull,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Clear all",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            items(recentSearches.size) { index ->
                val recentQuery = recentSearches[index]
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRecentSearchClick(recentQuery)
                    },
                    shape = ShapeLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_history),
                            contentDescription = "Recent",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = recentQuery,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateSearchQuery(recentQuery)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_north_west),
                                contentDescription = "Insert query",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (recentSearches.isEmpty() && topPeople.isEmpty()) {
            item {
                PhotonEmptyState(
                    icon = ImageVector.vectorResource(R.drawable.ic_ms_search),
                    title = stringResource(R.string.search_empty_title),
                    subtitle = stringResource(R.string.search_empty_subtitle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun SemanticStatusBanner(
    status: SmartSearchStatus,
    onOpenSettings: () -> Unit,
    onStartIndexing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val progress = if (status.totalCount > 0) {
        (status.indexedCount.toFloat() / status.totalCount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (!status.modelDownloaded) R.drawable.ic_ms_download else R.drawable.ic_ms_search_sparkle
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val title = when {
                        status.isModelDownloading -> "Downloading AI search model…"
                        !status.modelDownloaded -> "Visual AI search not set up"
                        status.isIndexing -> "Indexing photos for AI search"
                        status.indexedCount == 0 -> "Index photos for visual search"
                        else -> "${status.indexedCount} of ${status.totalCount} photos AI-indexed"
                    }
                    val subtitle = when {
                        status.isModelDownloading -> "${status.modelDownloadProgress}% complete"
                        !status.modelDownloaded -> "Download on-device model (~386 MB) to search scenes & objects"
                        status.isIndexing -> "${status.indexedCount} / ${status.totalCount} photos ready (${(progress * 100).toInt()}%)"
                        status.indexedCount == 0 -> "Run quick on-device indexing to search any scene naturally"
                        else -> "${status.totalCount - status.indexedCount} remaining photos won't appear in visual results yet"
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!status.isModelDownloading && !status.isIndexing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!status.modelDownloaded) onOpenSettings() else onStartIndexing()
                        },
                        shape = ShapeFull,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (!status.modelDownloaded) "Set up" else "Index",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            if (status.isModelDownloading || status.isIndexing) {
                LinearProgressIndicator(
                    progress = {
                        if (status.isModelDownloading) (status.modelDownloadProgress / 100f).coerceIn(0f, 1f)
                        else progress
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContainedLoadingIndicator(
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.search_in_progress),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoResultsState(
    query: String,
    smartSearchStatus: SmartSearchStatus,
    onClear: () -> Unit,
    onOpenSmartSearchSettings: () -> Unit,
    onStartIndexing: () -> Unit
) {
    val customSubtitle = when {
        !smartSearchStatus.modelDownloaded ->
            "Exact filenames and text matched nothing. Set up on-device AI search in Settings to search scenes like “$query”."
        smartSearchStatus.totalCount > 0 && smartSearchStatus.indexedCount == 0 && !smartSearchStatus.isIndexing ->
            "Visual AI indexing hasn't run yet. Index your photos or try file/date keywords."
        smartSearchStatus.isIndexing ->
            "Visual AI indexing is still running (${smartSearchStatus.indexedCount} / ${smartSearchStatus.totalCount}). Some scenes aren't indexed yet."
        else -> stringResource(R.string.search_no_results_subtitle)
    }

    PhotonEmptyState(
        icon = ImageVector.vectorResource(R.drawable.ic_ms_search),
        title = stringResource(R.string.search_no_results_title, query),
        subtitle = customSubtitle,
        shape = ExpressiveShapes.Clover8Leaf,
        modifier = Modifier.fillMaxSize(),
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onClear,
                    shape = ShapeFull
                ) {
                    Text(
                        text = stringResource(R.string.search_clear_button),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                if (!smartSearchStatus.modelDownloaded && !smartSearchStatus.isModelDownloading) {
                    FilledTonalButton(
                        onClick = onOpenSmartSearchSettings,
                        shape = ShapeFull,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(
                            text = "Set up AI search",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                } else if (smartSearchStatus.modelDownloaded && !smartSearchStatus.isIndexing &&
                    smartSearchStatus.totalCount > 0 && smartSearchStatus.indexedCount < smartSearchStatus.totalCount
                ) {
                    FilledTonalButton(
                        onClick = onStartIndexing,
                        shape = ShapeFull,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(
                            text = "Index photos",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    )
}

private data class SearchFilterCategory(
    val key: String,
    val title: String,
    val count: Int,
    val iconRes: Int
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SearchResultsList(
    ftsResults: List<GalleryItem>,
    smartResults: List<GalleryItem>,
    matchingAlbums: List<AlbumBucket>,
    matchingPeople: List<PersonEntity>,
    smartSearchStatus: SmartSearchStatus,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?, String?) -> Unit,
    onAlbumClick: (String) -> Unit,
    onPersonClick: (Long) -> Unit,
    onOpenSmartSearchSettings: () -> Unit,
    onStartIndexing: () -> Unit,
    query: String,
    viewModel: GalleryViewModel
) {
    val haptic = LocalHapticFeedback.current
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val favoriteIconVector = ImageVector.vectorResource(R.drawable.ic_ms_favorite_filled)

    var selectedFilter by remember(query) { mutableStateOf("ALL") }

    val visibleCellBounds = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val onCellBoundsChanged = remember {
        { key: String, rect: androidx.compose.ui.geometry.Rect ->
            visibleCellBounds[key] = rect
        }
    }
    DisposableEffect(Unit) {
        val resolver: (String) -> androidx.compose.ui.geometry.Rect? = { key -> visibleCellBounds[key] }
        com.inferno.gallery.ui.components.MediaTransitionRegistry.registerResolver(resolver)
        onDispose {
            com.inferno.gallery.ui.components.MediaTransitionRegistry.unregisterResolver(resolver)
        }
    }

    val onFtsClick = remember(onPhotoClick, query, viewModel, isSelectionMode) {
        { item: GalleryItem ->
            if (isSelectionMode) {
                viewModel.toggleSelection(item.uri.toString())
            } else {
                query.trim().takeIf { it.isNotBlank() }?.let { viewModel.addRecentSearch(it) }
                viewModel.setInitialDetailItem(item, com.inferno.gallery.data.BucketNames.SEARCH_TEXT)
                onPhotoClick(item.id, "search_text", query)
            }
        }
    }
    val onSmartClick = remember(onPhotoClick, query, viewModel, isSelectionMode) {
        { item: GalleryItem ->
            if (isSelectionMode) {
                viewModel.toggleSelection(item.uri.toString())
            } else {
                query.trim().takeIf { it.isNotBlank() }?.let { viewModel.addRecentSearch(it) }
                viewModel.setInitialDetailItem(item, com.inferno.gallery.data.BucketNames.SEARCH_SMART)
                onPhotoClick(item.id, "search_smart", query)
            }
        }
    }

    val onLongClickItem = remember(viewModel) {
        { item: GalleryItem ->
            viewModel.toggleSelection(item.uri.toString())
        }
    }

    val lazyGridState = rememberLazyGridState()

    val totalMatches = ftsResults.size + smartResults.size

    val ocrCount = remember(ftsResults) { ftsResults.count { it.matchType == "OCR" } }
    val nameCount = remember(ftsResults) { ftsResults.count { it.matchType == "NAME" } }
    val formatCount = remember(ftsResults) { ftsResults.count { it.matchType == "FORMAT" } }
    val semanticCount = remember(smartResults, ftsResults) {
        smartResults.size + ftsResults.count { it.matchType == "SEMANTIC" }
    }
    val albumCount = remember(matchingAlbums, ftsResults) {
        matchingAlbums.size + ftsResults.count { it.matchType == "ALBUM" }
    }
    val locationCount = remember(ftsResults) { ftsResults.count { it.matchType == "LOCATION" } }
    val dateCount = remember(ftsResults) { ftsResults.count { it.matchType == "DATE" } }

    val ocrItems = remember(ftsResults) { ftsResults.filter { it.matchType == "OCR" } }
    val nameItems = remember(ftsResults) { ftsResults.filter { it.matchType == "NAME" } }
    val formatItems = remember(ftsResults) { ftsResults.filter { it.matchType == "FORMAT" } }
    val allSemanticItems = remember(smartResults, ftsResults) {
        smartResults + ftsResults.filter { it.matchType == "SEMANTIC" }
    }
    val albumMedia = remember(ftsResults) { ftsResults.filter { it.matchType == "ALBUM" } }
    val locItems = remember(ftsResults) { ftsResults.filter { it.matchType == "LOCATION" } }
    val dateItems = remember(ftsResults) { ftsResults.filter { it.matchType == "DATE" } }

    val categories = remember(ocrCount, nameCount, formatCount, semanticCount, albumCount, locationCount, dateCount, totalMatches, matchingAlbums.size, matchingPeople.size) {
        val list = mutableListOf<SearchFilterCategory>()
        val total = totalMatches + matchingAlbums.size + matchingPeople.size
        list.add(SearchFilterCategory("ALL", "All", total, R.drawable.ic_ms_search))
        if (matchingPeople.isNotEmpty()) list.add(SearchFilterCategory("PEOPLE", "People", matchingPeople.size, R.drawable.ic_ms_person))
        if (ocrCount > 0) list.add(SearchFilterCategory("OCR", "OCR Text", ocrCount, R.drawable.ic_ms_text_fields))
        if (nameCount > 0) list.add(SearchFilterCategory("NAME", "File Name", nameCount, R.drawable.ic_ms_description))
        if (formatCount > 0) list.add(SearchFilterCategory("FORMAT", "Format", formatCount, R.drawable.ic_ms_image))
        if (semanticCount > 0) list.add(SearchFilterCategory("SEMANTIC", "Semantic", semanticCount, R.drawable.ic_ms_auto_fix_high))
        if (albumCount > 0) list.add(SearchFilterCategory("ALBUM", "Albums", albumCount, R.drawable.ic_ms_photo_album))
        if (locationCount > 0) list.add(SearchFilterCategory("LOCATION", "Location", locationCount, R.drawable.ic_ms_location_on))
        if (dateCount > 0) list.add(SearchFilterCategory("DATE", "Date", dateCount, R.drawable.ic_ms_calendar_month))
        list
    }

    val bannerCount = when (selectedFilter) {
        "ALL" -> if (totalMatches > 0) "$totalMatches" else if (matchingPeople.isNotEmpty()) "${matchingPeople.size}" else "${matchingAlbums.size}"
        "PEOPLE" -> "${matchingPeople.size}"
        "OCR" -> "$ocrCount"
        "NAME" -> "$nameCount"
        "FORMAT" -> "$formatCount"
        "SEMANTIC" -> "$semanticCount"
        "ALBUM" -> "$albumCount"
        "LOCATION" -> "$locationCount"
        "DATE" -> "$dateCount"
        else -> "$totalMatches"
    }

    val bannerText = when (selectedFilter) {
        "ALL" -> when {
            totalMatches > 0 && matchingAlbums.isNotEmpty() ->
                "${if (totalMatches == 1) "result" else "results"} & ${matchingAlbums.size} ${if (matchingAlbums.size == 1) "album" else "albums"} for “$query”"
            totalMatches > 0 ->
                if (totalMatches == 1) "Result found for “$query”" else "Results found for “$query”"
            matchingPeople.isNotEmpty() ->
                "${matchingPeople.size} ${if (matchingPeople.size == 1) "Person" else "People"} found for “$query”"
            else ->
                if (matchingAlbums.size == 1) "1 Album found for “$query”" else "${matchingAlbums.size} Albums found for “$query”"
        }
        "PEOPLE" -> "${matchingPeople.size} ${if (matchingPeople.size == 1) "Person" else "People"} found"
        "OCR" -> "$ocrCount OCR text ${if (ocrCount == 1) "match" else "matches"} found"
        "NAME" -> "$nameCount File name ${if (nameCount == 1) "match" else "matches"} found"
        "FORMAT" -> "$formatCount Format ${if (formatCount == 1) "match" else "matches"} found"
        "SEMANTIC" -> "$semanticCount Semantic AI ${if (semanticCount == 1) "match" else "matches"} found"
        "ALBUM" -> "$albumCount Album ${if (albumCount == 1) "match" else "matches"} found"
        "LOCATION" -> "$locationCount Location ${if (locationCount == 1) "match" else "matches"} found"
        "DATE" -> "$dateCount Date ${if (dateCount == 1) "match" else "matches"} found"
        else -> "Results for “$query”"
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = lazyGridState,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Total Matches Header Banner
        item(span = { GridItemSpan(4) }) {
            Surface(
                shape = ShapeLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = bannerCount,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Match Category Filter Chips (if more than 1 category exists)
        if (categories.size > 2) {
            item(span = { GridItemSpan(4) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    items(categories, key = { it.key }) { cat ->
                        val isSelected = selectedFilter == cat.key
                        val containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                        val contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedFilter = cat.key
                            },
                            shape = ShapeFull,
                            color = containerColor,
                            contentColor = contentColor,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(cat.iconRes),
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(IconSizeTokens.S)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = contentColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = ShapeFull,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        text = "${cat.count}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Render Content based on selectedFilter ──
        when (selectedFilter) {
            "ALL" -> {
                // Matching People Section
                if (matchingPeople.isNotEmpty()) {
                    item(span = { GridItemSpan(4) }) {
                        PhotonSectionHeader(title = stringResource(R.string.search_section_people))
                    }
                    item(span = { GridItemSpan(4) }) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(matchingPeople, key = { it.id }) { person ->
                                PersonSearchCard(
                                    person = person,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        person.name?.takeIf { it.isNotBlank() }?.let { viewModel.addRecentSearch(it) }; onPersonClick(person.id)
                                    }
                                )
                            }
                        }
                    }
                    item(span = { GridItemSpan(4) }) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Matching Albums Section
                if (matchingAlbums.isNotEmpty()) {
                    item(span = { GridItemSpan(4) }) {
                        PhotonSectionHeader(title = stringResource(R.string.section_albums))
                    }
                    item(span = { GridItemSpan(4) }) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(matchingAlbums, key = { it.bucketName }) { album ->
                                AlbumSearchCard(
                                    album = album,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        query.trim().takeIf { it.isNotBlank() }?.let { viewModel.addRecentSearch(it) }
                                        onAlbumClick(album.bucketName)
                                    }
                                )
                            }
                        }
                    }
                    item(span = { GridItemSpan(4) }) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Photos & Videos (Direct Matches)
                if (ftsResults.isNotEmpty()) {
                    item(span = { GridItemSpan(4) }) {
                        PhotonSectionHeader(title = stringResource(R.string.nav_photos))
                    }
                    items(
                        items = ftsResults.take(12),
                        key = { "fts_grid_${it.id}" }
                    ) { item ->
                        val isSelected = selectedUris.contains(item.uri.toString())
                        val isFav = favoriteIds.contains(item.id)
                        com.inferno.gallery.ui.components.OptimizedThumbnailCell(
                            item = item,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = onFtsClick,
                            onLongClick = onLongClickItem,
                            isSelected = isSelected,
                            favoriteIconVector = favoriteIconVector,
                            isFavorite = isFav,
                            showMatchBadge = true,
                            useContainerTransform = true,
                            onBoundsChanged = onCellBoundsChanged
                        )
                    }
                    if (ftsResults.size > 12) {
                        item(span = { GridItemSpan(4) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        query.trim().takeIf { it.isNotBlank() }?.let { viewModel.addRecentSearch(it) }
                                        viewModel.setBucket("search_text")
                                        onAlbumClick("search_text")
                                    },
                                    shape = ShapeFull,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.search_show_all_fts, ftsResults.size),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
                }

                // Separator
                if (ftsResults.isNotEmpty() && smartResults.isNotEmpty()) {
                    item(span = { GridItemSpan(4) }) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }
                }

                // Semantic Matches
                if (smartResults.isNotEmpty()) {
                    val inlineSemanticLimit = if (ftsResults.isEmpty() && matchingAlbums.isEmpty() && matchingPeople.isEmpty()) 24 else 12
                    item(span = { GridItemSpan(4) }) {
                        PhotonSectionHeader(title = stringResource(R.string.search_section_semantic))
                    }
                    items(
                        items = smartResults.take(inlineSemanticLimit),
                        key = { "smart_grid_${it.id}" }
                    ) { item ->
                        val isSelected = selectedUris.contains(item.uri.toString())
                        val isFav = favoriteIds.contains(item.id)
                        com.inferno.gallery.ui.components.OptimizedThumbnailCell(
                            item = item,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = onSmartClick,
                            onLongClick = onLongClickItem,
                            isSelected = isSelected,
                            favoriteIconVector = favoriteIconVector,
                            isFavorite = isFav,
                            showMatchBadge = true,
                            useContainerTransform = true,
                            onBoundsChanged = onCellBoundsChanged
                        )
                    }
                    if (smartResults.size > inlineSemanticLimit) {
                        item(span = { GridItemSpan(4) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        query.trim().takeIf { it.isNotBlank() }?.let { viewModel.addRecentSearch(it) }
                                        viewModel.setBucket("search_smart")
                                        onAlbumClick("search_smart")
                                    },
                                    shape = ShapeFull,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.search_show_all_semantic, smartResults.size),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
                }

                // Helpful coverage note when only partial / unindexed semantic coverage exists
                if (!smartSearchStatus.modelDownloaded || smartSearchStatus.isIndexing ||
                    (smartSearchStatus.totalCount > 0 && smartSearchStatus.indexedCount < smartSearchStatus.totalCount && smartResults.isEmpty())
                ) {
                    item(span = { GridItemSpan(4) }) {
                        SemanticStatusBanner(
                            status = smartSearchStatus,
                            onOpenSettings = onOpenSmartSearchSettings,
                            onStartIndexing = onStartIndexing,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            "PEOPLE" -> {
                item(span = { GridItemSpan(4) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(matchingPeople, key = { it.id }) { person ->
                            PersonSearchCard(
                                person = person,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    person.name?.takeIf { it.isNotBlank() }?.let { viewModel.addRecentSearch(it) }; onPersonClick(person.id)
                                }
                            )
                        }
                    }
                }
            }
            "OCR" -> {
                items(
                    items = ocrItems,
                    key = { "ocr_filter_${it.id}" }
                ) { item ->
                    val isSelected = selectedUris.contains(item.uri.toString())
                    val isFav = favoriteIds.contains(item.id)
                    com.inferno.gallery.ui.components.OptimizedThumbnailCell(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = onFtsClick,
                        onLongClick = onLongClickItem,
                        isSelected = isSelected,
                        favoriteIconVector = favoriteIconVector,
                        isFavorite = isFav,
                        showMatchBadge = true,
                        useContainerTransform = true,
                        onBoundsChanged = onCellBoundsChanged
                    )
                }
            }
            "NAME" -> {
                items(
                    items = nameItems,
                    key = { "name_filter_${it.id}" }
                ) { item ->
                    val isSelected = selectedUris.contains(item.uri.toString())
                    val isFav = favoriteIds.contains(item.id)
                    com.inferno.gallery.ui.components.OptimizedThumbnailCell(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = onFtsClick,
                        onLongClick = onLongClickItem,
                        isSelected = isSelected,
                        favoriteIconVector = favoriteIconVector,
                        isFavorite = isFav,
                        showMatchBadge = true,
                        useContainerTransform = true,
                        onBoundsChanged = onCellBoundsChanged
                    )
                }
            }
            "FORMAT" -> {
                items(
                    items = formatItems,
                    key = { "format_filter_${it.id}" }
                ) { item ->
                    val isSelected = selectedUris.contains(item.uri.toString())
                    val isFav = favoriteIds.contains(item.id)
                    com.inferno.gallery.ui.components.OptimizedThumbnailCell(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = onFtsClick,
                        onLongClick = onLongClickItem,
                        isSelected = isSelected,
                        favoriteIconVector = favoriteIconVector,
                        isFavorite = isFav,
                        showMatchBadge = true,
                        useContainerTransform = true,
                        onBoundsChanged = onCellBoundsChanged
                    )
                }
            }
            "SEMANTIC" -> {
                items(
                    items = allSemanticItems,
                    key = { "semantic_filter_${it.id}" }
                ) { item ->
                    val isSelected = selectedUris.contains(item.uri.toString())
                    val isFav = favoriteIds.contains(item.id)
                    com.inferno.gallery.ui.components.OptimizedThumbnailCell(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = onSmartClick,
                        onLongClick = onLongClickItem,
                        isSelected = isSelected,
                        favoriteIconVector = favoriteIconVector,
                        isFavorite = isFav,
                        showMatchBadge = true,
                        useContainerTransform = true,
                        onBoundsChanged = onCellBoundsChanged
                    )
                }
            }
            "ALBUM" -> {
                if (matchingAlbums.isNotEmpty()) {
                    item(span = { GridItemSpan(4) }) {
                        PhotonSectionHeader(title = stringResource(R.string.search_section_matching_albums))
                    }
                    item(span = { GridItemSpan(4) }) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(matchingAlbums, key = { it.bucketName }) { album ->
                                AlbumSearchCard(
                                    album = album,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        query.trim().takeIf { it.isNotBlank() }?.let { viewModel.addRecentSearch(it) }
                                        onAlbumClick(album.bucketName)
                                    }
                                )
                            }
                        }
                    }
                }
                if (albumMedia.isNotEmpty()) {
                    item(span = { GridItemSpan(4) }) {
                        PhotonSectionHeader(title = stringResource(R.string.search_section_photos_in_albums))
                    }
                    items(
                        items = albumMedia,
                        key = { "album_media_${it.id}" }
                    ) { item ->
                        val isSelected = selectedUris.contains(item.uri.toString())
                        val isFav = favoriteIds.contains(item.id)
                        com.inferno.gallery.ui.components.OptimizedThumbnailCell(
                            item = item,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = onFtsClick,
                            onLongClick = onLongClickItem,
                            isSelected = isSelected,
                            favoriteIconVector = favoriteIconVector,
                            isFavorite = isFav,
                            showMatchBadge = true,
                            useContainerTransform = true,
                            onBoundsChanged = onCellBoundsChanged
                        )
                    }
                }
            }
            "LOCATION" -> {
                items(
                    items = locItems,
                    key = { "location_filter_${it.id}" }
                ) { item ->
                    val isSelected = selectedUris.contains(item.uri.toString())
                    val isFav = favoriteIds.contains(item.id)
                    com.inferno.gallery.ui.components.OptimizedThumbnailCell(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = onFtsClick,
                        onLongClick = onLongClickItem,
                        isSelected = isSelected,
                        favoriteIconVector = favoriteIconVector,
                        isFavorite = isFav,
                        showMatchBadge = true,
                        useContainerTransform = true,
                        onBoundsChanged = onCellBoundsChanged
                    )
                }
            }
            "DATE" -> {
                items(
                    items = dateItems,
                    key = { "date_filter_${it.id}" }
                ) { item ->
                    val isSelected = selectedUris.contains(item.uri.toString())
                    val isFav = favoriteIds.contains(item.id)
                    com.inferno.gallery.ui.components.OptimizedThumbnailCell(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = onFtsClick,
                        onLongClick = onLongClickItem,
                        isSelected = isSelected,
                        favoriteIconVector = favoriteIconVector,
                        isFavorite = isFav,
                        showMatchBadge = true,
                        useContainerTransform = true,
                        onBoundsChanged = onCellBoundsChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumSearchCard(
    album: AlbumBucket,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 0.dp,
        modifier = modifier
            .width(140.dp)
            .height(110.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (album.coverUri != android.net.Uri.EMPTY) {
                AsyncImage(
                    model = album.coverUri,
                    contentDescription = album.bucketName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_album),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Scrim overlay at the bottom for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = album.bucketName,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.itemCount} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonSearchCard(
    person: PersonEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(84.dp)
            .clip(ShapeMedium)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = ShapeFull,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primaryContainer)
        ) {
            val cropPath = person.coverCropPath
            if (!cropPath.isNullOrBlank() && File(cropPath).exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(cropPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = person.name ?: "Person",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = person.name ?: "Unnamed",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (person.name != null) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (person.name != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        com.inferno.gallery.ui.people.PersonPhotoCountText(
            count = person.faceCount,
            baseFontSizeSp = 11f
        )
    }
}


