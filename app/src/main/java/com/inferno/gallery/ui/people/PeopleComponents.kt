@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.inferno.gallery.ui.people

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import com.inferno.gallery.R
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.MergeSuggestionWithPersons
import com.inferno.gallery.data.db.PersonEntity
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale
import java.io.File

/**
 * Bottom sheet to pick another person to merge into (Google Photos style).
 */
@Composable
fun MergePersonPickerSheet(
    people: List<PersonEntity>,
    currentPersonId: Long,
    title: String = "Merge into...",
    subtitle: String = "Select who this person is to combine all photos together.",
    onDismissRequest: () -> Unit,
    onSelectTargetPerson: (PersonEntity) -> Unit,
    onCreateNewPerson: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var showCreateNewDialog by remember { mutableStateOf(false) }

    val candidatePeople = remember(people, currentPersonId, searchQuery) {
        people.filter { it.id != currentPersonId }
            .filter { person ->
                if (searchQuery.isBlank()) true
                else person.name?.contains(searchQuery, ignoreCase = true) == true ||
                        "Person #${person.id}".contains(searchQuery, ignoreCase = true)
            }
    }

    if (showCreateNewDialog) {
        NamePersonDialog(
            initialName = if (searchQuery.isNotBlank() && candidatePeople.none { it.name.equals(searchQuery, ignoreCase = true) }) searchQuery else null,
            onDismissRequest = { showCreateNewDialog = false },
            onConfirm = { newName ->
                showCreateNewDialog = false
                onCreateNewPerson?.invoke(newName)
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = ShapeEdgeTop,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpacingTokens.XXL)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.M),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search people...",
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
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(IconSizeTokens.S)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = ShapeMedium,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                contentPadding = PaddingValues(horizontal = SpacingTokens.L, vertical = SpacingTokens.XS),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
            ) {
                if (onCreateNewPerson != null) {
                    item(key = "create_new_person_tile") {
                        Surface(
                            shape = ShapeMedium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressScale(pressedScale = 0.98f)
                                .clickable { showCreateNewDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                            ) {
                                Surface(
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_add),
                                            contentDescription = "New person",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(IconSizeTokens.M)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "New person...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Create a separate group with a new name",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                items(
                    items = candidatePeople,
                    key = { "target_picker_${it.id}" }
                ) { person ->
                    Surface(
                        shape = ShapeMedium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(pressedScale = 0.98f)
                            .clickable { onSelectTargetPerson(person) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                        ) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(48.dp)
                            ) {
                                val coverFile = person.coverCropPath?.let { File(it) }
                                if (coverFile != null && coverFile.exists()) {
                                    AsyncImage(
                                        model = coverFile,
                                        contentDescription = person.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(IconSizeTokens.M)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = person.name ?: "Person #${person.id}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${person.faceCount} ${if (person.faceCount == 1) "photo" else "photos"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(IconSizeTokens.M)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet to show & hide faces across the gallery (Google Photos style).
 */
@Composable
fun HideShowFacesSheet(
    people: List<PersonEntity>,
    onDismissRequest: () -> Unit,
    onToggleVisibility: (personId: Long, isCurrentlyHidden: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredPeople = remember(people, searchQuery) {
        if (searchQuery.isBlank()) people
        else people.filter {
            it.name?.contains(searchQuery, ignoreCase = true) == true ||
                    "Person #${it.id}".contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = ShapeEdgeTop,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpacingTokens.XXL)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.M),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
            ) {
                Text(
                    text = "Hide & show faces",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Faces you hide won't appear in the People album or carousels.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search people...",
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
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(IconSizeTokens.S)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = ShapeMedium,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                contentPadding = PaddingValues(horizontal = SpacingTokens.L, vertical = SpacingTokens.XS),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
            ) {
                items(
                    items = filteredPeople,
                    key = { "hide_show_${it.id}" }
                ) { person ->
                    Surface(
                        shape = ShapeMedium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                        ) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(48.dp)
                            ) {
                                val coverFile = person.coverCropPath?.let { File(it) }
                                if (coverFile != null && coverFile.exists()) {
                                    AsyncImage(
                                        model = coverFile,
                                        contentDescription = person.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(IconSizeTokens.M)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = person.name ?: "Person #${person.id}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (person.isHidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (person.isHidden) "Hidden · ${person.faceCount} photos" else "${person.faceCount} photos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { onToggleVisibility(person.id, person.isHidden) }
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(
                                        if (person.isHidden) R.drawable.ic_ms_visibility_off else R.drawable.ic_ms_visibility
                                    ),
                                    contentDescription = if (person.isHidden) "Unhide face" else "Hide face",
                                    tint = if (person.isHidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(IconSizeTokens.L)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hero card displayed at the top of PeopleScreen proposing proactive cluster merges.
 */
@Composable
fun MergeSuggestionHeroCard(
    suggestions: List<MergeSuggestionWithPersons>,
    onMerge: (MergeSuggestionWithPersons) -> Unit,
    onDismiss: (MergeSuggestionWithPersons) -> Unit,
    onCloseCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    var currentIndex by rememberSaveable(suggestions.size) { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, suggestions.lastIndex)
    val currentSuggestion = suggestions[safeIndex]
    val personA = currentSuggestion.personA
    val personB = currentSuggestion.personB

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.S),
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.L)
        ) {
            // Header: Sparkle + Title + Carousel Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(IconSizeTokens.M)
                )
                Spacer(modifier = Modifier.width(SpacingTokens.S))
                Text(
                    text = "Same person?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))

                if (suggestions.size > 1) {
                    Text(
                        text = "${safeIndex + 1} of ${suggestions.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(SpacingTokens.XS))
                    IconButton(
                        onClick = { if (safeIndex > 0) currentIndex = safeIndex - 1 },
                        enabled = safeIndex > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                            contentDescription = "Previous suggestion",
                            tint = if (safeIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier
                                .size(IconSizeTokens.M)
                                .graphicsLayer { rotationZ = 180f }
                        )
                    }
                    IconButton(
                        onClick = { if (safeIndex < suggestions.lastIndex) currentIndex = safeIndex + 1 },
                        enabled = safeIndex < suggestions.lastIndex,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                            contentDescription = "Next suggestion",
                            tint = if (safeIndex < suggestions.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(IconSizeTokens.M)
                        )
                    }
                    Spacer(modifier = Modifier.width(SpacingTokens.XS))
                }

                IconButton(
                    onClick = onCloseCard,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                        contentDescription = "Close suggestions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSizeTokens.M)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            // Comparison row: Person A <-> Match Pill <-> Person B
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PersonAvatarColumn(
                    person = personA,
                    modifier = Modifier.weight(1f)
                )

                // Match Percentage Pill
                val similarityPct = ((currentSuggestion.suggestion.similarity) * 100).toInt()
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.padding(horizontal = SpacingTokens.S)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = SpacingTokens.XS),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(IconSizeTokens.S)
                        )
                        Text(
                            text = "$similarityPct% match",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                PersonAvatarColumn(
                    person = personB,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // Action buttons row: Different vs Yes, Merge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M)
            ) {
                OutlinedButton(
                    onClick = { onDismiss(currentSuggestion) },
                    modifier = Modifier.weight(1f),
                    shape = ShapeFull
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                        contentDescription = null,
                        modifier = Modifier.size(IconSizeTokens.S)
                    )
                    Spacer(modifier = Modifier.width(SpacingTokens.XS))
                    Text(
                        text = "Different",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = { onMerge(currentSuggestion) },
                    modifier = Modifier.weight(1f),
                    shape = ShapeFull,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                        contentDescription = null,
                        modifier = Modifier.size(IconSizeTokens.S)
                    )
                    Spacer(modifier = Modifier.width(SpacingTokens.XS))
                    Text(
                        text = "Yes, Merge",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonAvatarColumn(
    person: PersonEntity,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = ShapeFull,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(64.dp)
        ) {
            val coverFile = person.coverCropPath?.let { File(it) }
            if (coverFile != null && coverFile.exists()) {
                AsyncImage(
                    model = coverFile,
                    contentDescription = person.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSizeTokens.XL)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingTokens.XS))

        Text(
            text = person.name ?: "Person #${person.id}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${person.faceCount} ${if (person.faceCount == 1) "photo" else "photos"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * Bottom sheet allowing the user to select one of this person's photos to be used as their feature/cover photo.
 */
@Composable
fun FeaturePhotoPickerSheet(
    person: PersonEntity,
    photos: List<CoreMediaEntity>,
    onDismissRequest: () -> Unit,
    onPhotoSelected: (CoreMediaEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val haptic = LocalHapticFeedback.current

    val currentCoverMediaId = remember(person.coverCropPath) {
        person.coverCropPath?.let { path ->
            val filename = path.substringAfterLast('/')
            Regex("""face_(\d+)_""").find(filename)?.groupValues?.getOrNull(1)?.toLongOrNull()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = ShapeEdgeTop,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpacingTokens.XXL)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.S),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Change feature photo",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (!person.name.isNullOrBlank()) "Choose a photo to represent ${person.name}" else "Choose a photo to represent this person",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(
                    onClick = onDismissRequest,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                        contentDescription = "Close",
                        modifier = Modifier.size(IconSizeTokens.M)
                    )
                }
            }

            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No photos found for this person",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    contentPadding = PaddingValues(
                        start = SpacingTokens.M,
                        end = SpacingTokens.M,
                        top = SpacingTokens.S,
                        bottom = SpacingTokens.L
                    ),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    items(
                        count = photos.size,
                        key = { photos[it].id }
                    ) { index ->
                        val media = photos[index]
                        val isCurrent = currentCoverMediaId == media.id
                        Surface(
                            shape = ShapeMedium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = if (isCurrent) {
                                BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .pressScale(pressedScale = 0.95f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPhotoSelected(media)
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = android.net.Uri.parse(media.uriString),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isCurrent) {
                                    Surface(
                                        shape = ShapeFull,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .align(Alignment.TopStart)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                                contentDescription = "Current feature photo",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if (media.isVideo) {
                                    Surface(
                                        shape = ShapeFull,
                                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.BottomEnd)
                                            .padding(3.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                                                contentDescription = "Video",
                                                tint = MaterialTheme.colorScheme.surfaceContainerLowest,
                                                modifier = Modifier.size(IconSizeTokens.S)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

