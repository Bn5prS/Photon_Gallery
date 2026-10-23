@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)

package com.inferno.gallery.ui.people

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
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
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale
import java.io.File

@Composable
fun PeopleScreen(
    onBack: () -> Unit,
    onPersonClick: (Long) -> Unit,
    onStartScan: () -> Unit,
    viewModel: PeopleViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val promotedPeople by viewModel.promotedPersons.collectAsState()
    val allPeople by viewModel.allPersons.collectAsState()
    val faceIndexWorkInfo by viewModel.faceIndexWorkInfo.collectAsState()
    val isScanning = faceIndexWorkInfo?.state == androidx.work.WorkInfo.State.RUNNING ||
            faceIndexWorkInfo?.state == androidx.work.WorkInfo.State.ENQUEUED
    val scanProcessed = faceIndexWorkInfo?.progress?.getInt("processed", 0) ?: 0
    val scanTotal = faceIndexWorkInfo?.progress?.getInt("total", 0) ?: 0

    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0: Promoted (3+), 1: All Faces
    val displayPeople = if (selectedTab == 1 || (promotedPeople.isEmpty() && allPeople.isNotEmpty())) {
        allPeople
    } else {
        promotedPeople
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedIds by viewModel.selectedPersonIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val haptic = LocalHapticFeedback.current

    var mergeDialogPair by remember { mutableStateOf<Pair<PersonEntity, PersonEntity>?>(null) }
    var namingPerson by remember { mutableStateOf<PersonEntity?>(null) }

    if (namingPerson != null) {
        val target = namingPerson!!
        NamePersonDialog(
            initialName = target.name,
            onDismissRequest = { namingPerson = null },
            onConfirm = { newName ->
                viewModel.setPersonName(target.id, newName)
                namingPerson = null
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
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
                        if (selectedIds.size == 2) {
                            val selectedList = displayPeople.filter { selectedIds.contains(it.id) }
                            if (selectedList.size == 2) {
                                IconButton(
                                    onClick = {
                                        mergeDialogPair = Pair(selectedList[0], selectedList[1])
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
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

            // Tab / Category Selector
            if (promotedPeople.isNotEmpty() && allPeople.size > promotedPeople.size) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.XS)
                ) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = 0
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("People (${promotedPeople.size})")
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = 1
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("All Faces (${allPeople.size})")
                    }
                }
            } else if (promotedPeople.isEmpty() && allPeople.isNotEmpty() && isScanning) {
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
                            text = "Showing discovered faces (${allPeople.size} found) while scanning",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (displayPeople.isEmpty() && searchQuery.isBlank() && !isScanning) {
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
            } else {
                // People Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    contentPadding = PaddingValues(SpacingTokens.L),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.L),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = displayPeople,
                        key = { "person_grid_${it.id}" }
                    ) { person ->
                        val isSelected = selectedIds.contains(person.id)

                        PersonGridCard(
                            person = person,
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

@Composable
private fun PersonGridCard(
    person: PersonEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAddName: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.94f)
            .clip(ShapeLarge)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
    ) {
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = if (isSelected) 6.dp else 2.dp,
                modifier = Modifier.size(80.dp)
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
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(IconSizeTokens.XL)
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
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
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
