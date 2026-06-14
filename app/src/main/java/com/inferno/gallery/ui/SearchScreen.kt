package com.inferno.gallery.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?, String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onAlbumClick: (String) -> Unit = {}
) {
    val query by viewModel.searchQuery.collectAsState()
    val ftsResults by viewModel.ftsSearchResults.collectAsState()
    val smartResults by viewModel.smartSearchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val ocrIndexWorkInfo by viewModel.ocrIndexWorkInfo.collectAsState(initial = null)
    val unindexedOcrCount by viewModel.unindexedOcrImagesCount.collectAsState()
    val totalImagesCount by viewModel.totalImagesCount.collectAsState()

    val hasAnyResults = ftsResults.isNotEmpty() || smartResults.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // ── Search Bar ──────────────────────────────────────────────────────────
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { /* Auto-search handles this via debounce */ },
                    expanded = false,
                    onExpandedChange = {},
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    placeholder = {
                        Text("Search text in photos…")
                    }
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {}

        // ── Results Area ────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = Triple(query.isBlank(), isSearching, hasAnyResults),
            transitionSpec = {
                (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.95f))
                    .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleOut(spring(stiffness = Spring.StiffnessMediumLow), targetScale = 0.95f))
            },
            modifier = Modifier.fillMaxSize()
        ) { (isBlank, searching, hasResults) ->
            when {
                isBlank -> EmptySearchState(
                    ocrIndexWorkInfo = ocrIndexWorkInfo,
                    unindexedOcrCount = unindexedOcrCount,
                    totalCount = totalImagesCount,
                    onStartOcr = { viewModel.startOcrIndexing() },
                    onStopOcr = { viewModel.stopOcrIndexing() },
                    onClearOcr = { viewModel.clearOcrIndexAndReindex() }
                )
                searching -> SearchingState()
                !hasResults -> NoResultsState(query)
                else -> SearchResultsList(
                    ftsResults = ftsResults,
                    smartResults = smartResults,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onPhotoClick = onPhotoClick,
                    onAlbumClick = onAlbumClick,
                    query = query,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    ocrIndexWorkInfo: WorkInfo?,
    unindexedOcrCount: Int,
    totalCount: Int,
    onStartOcr: () -> Unit,
    onStopOcr: () -> Unit,
    onClearOcr: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "Text Search",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Search for any text contained within your images.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                val isOcrWorkerRunning = ocrIndexWorkInfo?.state == WorkInfo.State.RUNNING || ocrIndexWorkInfo?.state == WorkInfo.State.ENQUEUED
                if (unindexedOcrCount > 0 || isOcrWorkerRunning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    IndexingCard(
                        title = "Text Indexing",
                        workInfo = ocrIndexWorkInfo,
                        unindexedCount = unindexedOcrCount,
                        totalCount = totalCount,
                        onStart = onStartOcr,
                        onStop = onStopOcr,
                        onClearAndReindex = onClearOcr,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .width(160.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                text = "Thinking…",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoResultsState(query: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "No results for",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "\u201c$query\u201d",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Try different words or descriptions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SearchResultsList(
    ftsResults: List<GalleryItem>,
    smartResults: List<GalleryItem>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?, String?) -> Unit,
    onAlbumClick: (String) -> Unit,
    query: String,
    viewModel: GalleryViewModel
) {
    val onFtsClick = remember(onPhotoClick, query) {
        { item: GalleryItem -> onPhotoClick(item.id, "search_text", query) }
    }
    val onSmartClick = remember(onPhotoClick, query) {
        { item: GalleryItem -> onPhotoClick(item.id, "search_smart", query) }
    }

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

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = lazyGridState,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Total Matches Header
        item(span = { GridItemSpan(3) }) {
            Text(
                text = "Found ${ftsResults.size + smartResults.size} total matches for \"$query\"",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        // 1. Text Matches Section (OCR)
        if (ftsResults.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Text(
                        text = "Text Matches",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${ftsResults.size} images containing text",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(
                items = ftsResults.take(3),
                key = { "fts_grid_${it.id}" }
            ) { item ->
                GalleryGridItem(
                    item = item,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = onFtsClick
                )
            }
            if (ftsResults.size > 3) {
                item(span = { GridItemSpan(3) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { onAlbumClick("search_text") },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "Show all text matches (${ftsResults.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Section separator spacer
        if (ftsResults.isNotEmpty() && smartResults.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 2. Semantic Matches Section (Smart Search)
        if (smartResults.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Text(
                        text = "Semantic Matches",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${smartResults.size} images matching description",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(
                items = smartResults.take(3),
                key = { "smart_grid_${it.id}" }
            ) { item ->
                GalleryGridItem(
                    item = item,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = onSmartClick
                )
            }
            if (smartResults.size > 3) {
                item(span = { GridItemSpan(3) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { onAlbumClick("search_smart") },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "Show all semantic matches (${smartResults.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndexingCard(
    title: String,
    workInfo: WorkInfo?,
    unindexedCount: Int,
    totalCount: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClearAndReindex: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = workInfo?.state == WorkInfo.State.RUNNING || workInfo?.state == WorkInfo.State.ENQUEUED
    val progress = workInfo?.progress
    val dbIndexed = totalCount - unindexedCount
    val workerIndexed = progress?.getInt("progress", 0) ?: 0
    val indexed = maxOf(dbIndexed, workerIndexed)
    val total = if (totalCount > 0) totalCount else (progress?.getInt("total", 0) ?: 0)

    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Reindex Text") },
            text = { Text("This will clear all currently indexed text search data and scan all your images again. This process uses battery and CPU. Are you sure you want to continue?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onClearAndReindex()
                    }
                ) {
                    Text("Reindex", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isRunning) {
                FilledTonalButton(onClick = onStop, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Text("Stop", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                FilledTonalButton(onClick = onStart, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Text("Start", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            val statusDescription = when {
                workInfo?.state == WorkInfo.State.RUNNING -> {
                    val currentImage = progress?.getString("current_image")
                    if (currentImage != null) "Scanning: $currentImage" else "Scanning images for text…"
                }
                workInfo?.state == WorkInfo.State.ENQUEUED -> "Preparing text indexing…"
                unindexedCount == 0 -> "All images indexed"
                else -> "Indexing paused"
            }

            Text(
                text = statusDescription,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            val progressFloat = if (total > 0) indexed.toFloat() / total.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progressFloat },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "$indexed / $total", 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reindex", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
