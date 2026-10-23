@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)

package com.inferno.gallery.ui.people

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.inferno.gallery.R
import com.inferno.gallery.data.db.PersonEntity
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun PeopleScreen(
    onBack: () -> Unit,
    onPersonClick: (Long) -> Unit,
    onStartScan: () -> Unit,
    viewModel: PeopleViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val promotedPeople by viewModel.promotedPersons.collectAsState()
    val allPeople by viewModel.allPersons.collectAsState()
    val pendingSuggestions by viewModel.pendingMergeSuggestions.collectAsState()
    val faceIndexWorkInfo by viewModel.faceIndexWorkInfo.collectAsState()
    val isScanning = faceIndexWorkInfo?.state == androidx.work.WorkInfo.State.RUNNING ||
            faceIndexWorkInfo?.state == androidx.work.WorkInfo.State.ENQUEUED
    val scanProcessed = faceIndexWorkInfo?.progress?.getInt("processed", 0) ?: 0
    val scanTotal = faceIndexWorkInfo?.progress?.getInt("total", 0) ?: 0

    var minPhotosThreshold by rememberSaveable { mutableIntStateOf(1) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val displayPeople = remember(allPeople, minPhotosThreshold) {
        if (minPhotosThreshold <= 1) {
            allPeople
        } else {
            allPeople.filter { person ->
                person.faceCount >= minPhotosThreshold || !person.name.isNullOrBlank() || person.isFavorite
            }
        }
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedIds by viewModel.selectedPersonIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val allPersonsIncludingHidden by viewModel.allPersonsIncludingHidden.collectAsState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var mergeDialogPair by remember { mutableStateOf<Pair<PersonEntity, PersonEntity>?>(null) }
    var batchMergeList by remember { mutableStateOf<List<PersonEntity>?>(null) }
    var namingPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var collisionPair by remember { mutableStateOf<Pair<PersonEntity, PersonEntity>?>(null) }
    var showHideShowSheet by remember { mutableStateOf(false) }
    var gridCols by rememberSaveable { mutableIntStateOf(3) }
    var isPinching by remember { mutableStateOf(false) }
    val livePinchScaleAnim = remember { Animatable(1f) }
    val pinchScaleScope = rememberCoroutineScope()

    if (namingPerson != null) {
        val target = namingPerson!!
        NamePersonDialog(
            initialName = target.name,
            onDismissRequest = { namingPerson = null },
            onConfirm = { newName ->
                coroutineScope.launch {
                    val collision = viewModel.checkNameCollision(target.id, newName)
                    if (collision != null) {
                        collisionPair = Pair(target, collision)
                    } else {
                        viewModel.setPersonName(target.id, newName)
                    }
                    namingPerson = null
                }
            }
        )
    }

    if (collisionPair != null) {
        val (source, target) = collisionPair!!
        SamePersonCollisionDialog(
            existingPersonName = target.name ?: "Person #${target.id}",
            onDismissRequest = { collisionPair = null },
            onConfirmMerge = {
                viewModel.mergePersons(sourcePersonId = source.id, targetPersonId = target.id)
                collisionPair = null
            }
        )
    }

    if (mergeDialogPair != null) {
        val (p1, p2) = mergeDialogPair!!
        val name1 = p1.name ?: "Person #${p1.id}"
        val name2 = p2.name ?: "Person #${p2.id}"
        MergePersonDialog(
            sourceName = name2,
            targetName = name1,
            onDismissRequest = { mergeDialogPair = null },
            onConfirm = {
                viewModel.mergePersons(sourcePersonId = p2.id, targetPersonId = p1.id)
                mergeDialogPair = null
            }
        )
    }

    if (batchMergeList != null) {
        val list = batchMergeList!!
        val target = list.firstOrNull { !it.name.isNullOrBlank() }
            ?: list.maxByOrNull { it.faceCount }
            ?: list[0]
        val sources = list.filter { it.id != target.id }
        val targetName = target.name ?: "Person #${target.id}"

        BatchMergePersonDialog(
            selectedCount = list.size,
            targetName = targetName,
            onDismissRequest = { batchMergeList = null },
            onConfirm = {
                viewModel.mergeMultiplePersons(sourcePersonIds = sources.map { it.id }, targetPersonId = target.id)
                batchMergeList = null
            }
        )
    }

    if (showFilterSheet) {
        FilterCountSheet(
            currentThreshold = minPhotosThreshold,
            allPeople = allPeople,
            onDismissRequest = { showFilterSheet = false },
            onApplyThreshold = { newThreshold ->
                minPhotosThreshold = newThreshold
            }
        )
    }

    if (showHideShowSheet) {
        HideShowFacesSheet(
            people = allPersonsIncludingHidden,
            onDismissRequest = { showHideShowSheet = false },
            onToggleVisibility = { personId, isHidden ->
                if (isHidden) viewModel.unhidePerson(personId)
                else viewModel.hidePerson(personId)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isSelectionMode) "${selectedIds.size} selected" else "People",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.clearSelection()
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (isSelectionMode) R.drawable.ic_ms_close else R.drawable.ic_ms_arrow_back
                            ),
                            contentDescription = if (isSelectionMode) "Close selection" else "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(IconSizeTokens.L)
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        if (selectedIds.size >= 2) {
                            val selectedList = displayPeople.filter { selectedIds.contains(it.id) }
                            if (selectedList.size >= 2) {
                                IconButton(
                                    onClick = {
                                        if (selectedList.size == 2) {
                                            mergeDialogPair = Pair(selectedList[0], selectedList[1])
                                        } else {
                                            batchMergeList = selectedList
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                                        contentDescription = "Merge people",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(IconSizeTokens.L)
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                selectedIds.forEach { viewModel.hidePerson(it) }
                                viewModel.clearSelection()
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_visibility_off),
                                contentDescription = "Hide selected people",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(IconSizeTokens.L)
                            )
                        }
                    } else {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_more_vert),
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(IconSizeTokens.L)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Merge Similar Faces") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.consolidateClusters()
                                        android.widget.Toast.makeText(context, "Consolidating similar faces…", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Filter by photo count") },
                                    onClick = {
                                        showMenu = false
                                        showFilterSheet = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_tune),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hide & show faces") },
                                    onClick = {
                                        showMenu = false
                                        showHideShowSheet = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_visibility),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            if (displayPeople.isNotEmpty() || searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Search by name…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSizeTokens.M)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = ShapeFull,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.S),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )
            }

            // Proactive Merge Suggestions Hero Card
            AnimatedVisibility(
                visible = pendingSuggestions.isNotEmpty() && !isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MergeSuggestionHeroCard(
                    suggestions = pendingSuggestions,
                    onMerge = { suggestion ->
                        viewModel.acceptMergeSuggestion(suggestion)
                    },
                    onDismiss = { suggestion ->
                        viewModel.dismissMergeSuggestion(suggestion)
                    }
                )
            }

            // Photo Count Filter Chips Row
            if (allPeople.isNotEmpty()) {
                val count3 = remember(allPeople) {
                    allPeople.count { it.faceCount >= 3 || !it.name.isNullOrBlank() || it.isFavorite }
                }
                val count5 = remember(allPeople) {
                    allPeople.count { it.faceCount >= 5 || !it.name.isNullOrBlank() || it.isFavorite }
                }
                val count10 = remember(allPeople) {
                    allPeople.count { it.faceCount >= 10 || !it.name.isNullOrBlank() || it.isFavorite }
                }
                val isCustomActive = minPhotosThreshold !in setOf(1, 3, 5, 10)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.XS),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // All (1+)
                    FilterChip(
                        selected = minPhotosThreshold == 1,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            minPhotosThreshold = 1
                        },
                        label = { Text("All (${allPeople.size})") },
                        shape = ShapeFull
                    )

                    // 3+ photos
                    FilterChip(
                        selected = minPhotosThreshold == 3,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            minPhotosThreshold = 3
                        },
                        label = { Text("3+ photos ($count3)") },
                        shape = ShapeFull
                    )

                    // 5+ photos
                    FilterChip(
                        selected = minPhotosThreshold == 5,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            minPhotosThreshold = 5
                        },
                        label = { Text("5+ photos ($count5)") },
                        shape = ShapeFull
                    )

                    // 10+ photos
                    FilterChip(
                        selected = minPhotosThreshold == 10,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            minPhotosThreshold = 10
                        },
                        label = { Text("10+ photos ($count10)") },
                        shape = ShapeFull
                    )

                    // Custom threshold chip
                    FilterChip(
                        selected = isCustomActive,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showFilterSheet = true
                        },
                        label = {
                            Text(if (isCustomActive) "$minPhotosThreshold+ photos (${displayPeople.size})" else "Custom…")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_tune),
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeTokens.S)
                            )
                        },
                        shape = ShapeFull
                    )
                }
            } else if (isScanning) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = ShapeFull,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.XS)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(IconSizeTokens.S)
                        )
                        Text(
                            text = "Scanning photos for faces…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (displayPeople.isEmpty() && searchQuery.isBlank() && !isScanning) {
                if (allPeople.isNotEmpty() && minPhotosThreshold > 1) {
                    // Filter empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(SpacingTokens.XXL),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(SpacingTokens.L)
                        ) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_tune),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(IconSizeTokens.XL)
                                    )
                                }
                            }

                            Text(
                                text = "No faces with $minPhotosThreshold+ photos",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "All ${allPeople.size} recognized faces have fewer than $minPhotosThreshold photos. Tap below to see all recognized faces.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = SpacingTokens.L)
                            )

                            FilledTonalButton(
                                onClick = { minPhotosThreshold = 1 },
                                shape = ShapeFull
                            ) {
                                Text(
                                    text = "Show All Faces (${allPeople.size})",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                } else {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(SpacingTokens.XXL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(SpacingTokens.L)
                    ) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(IconSizeTokens.XL)
                                )
                            }
                        }

                        Text(
                            text = "No people recognized yet",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Photon Gallery clusters faces automatically in the background when charging. Only people appearing in at least 3 photos are grouped here to eliminate clutter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = SpacingTokens.L)
                        )

                        FilledTonalButton(
                            onClick = onStartScan,
                            shape = ShapeFull,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeTokens.M)
                            )
                            Spacer(modifier = Modifier.width(SpacingTokens.S))
                            Text(
                                text = "Scan Faces Now",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        } else {
            // People Grid with Pinch-to-Zoom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .peopleGridPinchZoom(
                            gridCellsCount = gridCols,
                            onGridCountChange = { newCols ->
                                gridCols = newCols.coerceIn(2, 5)
                            },
                            isSelectionMode = isSelectionMode,
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
                        columns = GridCells.Fixed(gridCols),
                        contentPadding = PaddingValues(SpacingTokens.L),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
                        verticalArrangement = Arrangement.spacedBy(SpacingTokens.L),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = livePinchScaleAnim.value
                                scaleY = livePinchScaleAnim.value
                                transformOrigin = TransformOrigin.Center
                            }
                    ) {
                        items(
                            items = displayPeople,
                            key = { "person_grid_${it.id}" }
                        ) { person ->
                            val isSelected = selectedIds.contains(person.id)

                            PersonGridCard(
                                person = person,
                                gridCols = gridCols,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.toggleSelectPerson(person.id)
                                    } else {
                                        onPersonClick(person.id)
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleSelectPerson(person.id)
                                },
                                onAddName = {
                                    namingPerson = person
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonGridCard(
    person: PersonEntity,
    gridCols: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAddName: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val (boxSize, avatarSize) = when (gridCols) {
        2 -> Pair(116.dp, 104.dp)
        3 -> Pair(88.dp, 80.dp)
        4 -> Pair(72.dp, 64.dp)
        else -> Pair(60.dp, 52.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.95f, interactionSource = interactionSource)
            .clip(ShapeLarge)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
    ) {
        Box(
            modifier = Modifier.size(boxSize),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = if (isSelected) 6.dp else 2.dp,
                modifier = Modifier.size(avatarSize)
            ) {
                val coverFile = person.coverCropPath?.let { File(it) }
                if (coverFile != null && coverFile.exists()) {
                    AsyncImage(
                        model = coverFile,
                        contentDescription = person.name ?: "Person",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!person.name.isNullOrBlank()) {
                            Text(
                                text = person.name.take(1).uppercase(),
                                style = if (gridCols <= 2) MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                                       else if (gridCols == 3) MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                       else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (gridCols <= 2) IconSizeTokens.XXL else if (gridCols == 3) IconSizeTokens.XL else IconSizeTokens.L)
                            )
                        }
                    }
                }
            }

            // Selection checkmark badge
            if (isSelected) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                            contentDescription = "Selected",
                            modifier = Modifier.size(IconSizeTokens.S)
                        )
                    }
                }
            }

            // Favorite star badge
            if (person.isFavorite && !isSelected) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_star),
                            contentDescription = "Favorite",
                            modifier = Modifier.size(IconSizeTokens.S)
                        )
                    }
                }
            }
        }

        if (person.name != null) {
            Text(
                text = person.name,
                style = if (gridCols <= 3) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        else MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                onClick = onAddName,
                modifier = Modifier.padding(horizontal = SpacingTokens.XS)
            ) {
                Text(
                    text = "Add name",
                    style = if (gridCols <= 3) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                           else MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = 2.dp)
                )
            }
        }

        Text(
            text = "${person.faceCount} ${if (person.faceCount == 1) "photo" else "photos"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Smooth two-finger pinch-to-zoom gesture detector for People grid.
 * Spreading fingers decreases columns (zooms in: 5 -> 4 -> 3 -> 2),
 * pinching fingers increases columns (zooms out: 2 -> 3 -> 4 -> 5).
 */
private fun Modifier.peopleGridPinchZoom(
    gridCellsCount: Int,
    onGridCountChange: (Int) -> Unit,
    isSelectionMode: Boolean,
    haptic: HapticFeedback,
    onPinchScaleChange: (scale: Float) -> Unit,
    onPinchEnd: () -> Unit
): Modifier = composed {
    val currentGridCount by rememberUpdatedState(gridCellsCount)
    val currentOnGridCountChange by rememberUpdatedState(onGridCountChange)
    val currentIsSelectionMode by rememberUpdatedState(isSelectionMode)
    val currentOnPinchScaleChange by rememberUpdatedState(onPinchScaleChange)
    val currentOnPinchEnd by rememberUpdatedState(onPinchEnd)

    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

            if (currentIsSelectionMode) return@awaitEachGesture

            var pinchActive = false
            var accumulatedScale = 1.0f
            var lastChangeTime = 0L

            try {
                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val pressedChanges = event.changes.filter { it.pressed }

                    if (pressedChanges.size >= 2) {
                        pinchActive = true
                        // Intercept immediately so LazyVerticalGrid scroll and card clicks don't fire
                        event.changes.forEach { it.consume() }

                        val p1 = pressedChanges[0]
                        val p2 = pressedChanges[1]

                        if (p1.previousPressed && p2.previousPressed) {
                            val currDist = kotlin.math.hypot(p1.position.x - p2.position.x, p1.position.y - p2.position.y)
                            val prevDist = kotlin.math.hypot(p1.previousPosition.x - p2.previousPosition.x, p1.previousPosition.y - p2.previousPosition.y)

                            if (prevDist > 1f && currDist > 1f) {
                                val zoomDelta = currDist / prevDist
                                if (zoomDelta in 0.6f..1.6f) {
                                    accumulatedScale *= zoomDelta
                                    currentOnPinchScaleChange(accumulatedScale)

                                    val now = System.currentTimeMillis()
                                    // Spreading fingers -> fewer columns (zooms in: e.g. 5 -> 4 -> 3 -> 2)
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
                                    // Pinching fingers together -> more columns (zooms out: e.g. 2 -> 3 -> 4 -> 5)
                                    else if (accumulatedScale <= 0.85f && now - lastChangeTime > 240L) {
                                        val currentCols = currentGridCount
                                        if (currentCols < 5) {
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
                    } else if (pinchActive) {
                        // While lifting fingers after pinch, consume until all fingers are up to prevent flinging
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

@Composable
private fun FilterCountSheet(
    currentThreshold: Int,
    allPeople: List<PersonEntity>,
    onDismissRequest: () -> Unit,
    onApplyThreshold: (Int) -> Unit
) {
    var tempThreshold by remember { mutableIntStateOf(currentThreshold) }
    val matchedCount = remember(allPeople, tempThreshold) {
        if (tempThreshold <= 1) allPeople.size
        else allPeople.count { it.faceCount >= tempThreshold || !it.name.isNullOrBlank() || it.isFavorite }
    }
    val hiddenCount = (allPeople.size - matchedCount).coerceAtLeast(0)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = ShapeEdgeTop,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.L)
                .padding(bottom = SpacingTokens.XXL),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.L)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Filter by Photo Count",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (tempThreshold <= 1) "Showing all ${allPeople.size} faces"
                               else "Showing $matchedCount people ($hiddenCount hidden)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Information note: Named & Favorites always preserved
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ShapeMedium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(SpacingTokens.M),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSizeTokens.S)
                    )
                    Text(
                        text = "Named people and favorites always stay visible regardless of photo count.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stepper / Slider Title
            Text(
                text = "Minimum photos per person: $tempThreshold+",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Slider
            Slider(
                value = tempThreshold.toFloat(),
                onValueChange = { tempThreshold = it.toInt().coerceIn(1, 25) },
                valueRange = 1f..25f,
                steps = 23,
                modifier = Modifier.fillMaxWidth()
            )

            // Quick preset chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
            ) {
                listOf(1, 2, 3, 5, 8, 9, 10, 15, 20).forEach { preset ->
                    FilterChip(
                        selected = tempThreshold == preset,
                        onClick = { tempThreshold = preset },
                        label = { Text(if (preset == 1) "All (1+)" else "$preset+ pics") },
                        shape = ShapeFull
                    )
                }
            }

            // Apply button
            Button(
                onClick = {
                    onApplyThreshold(tempThreshold)
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeFull,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (tempThreshold <= 1) "Show All Faces (${allPeople.size})"
                           else "Show $tempThreshold+ Photos ($matchedCount)",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

