@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)

package com.inferno.gallery.ui.people

import android.net.Uri
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.inferno.gallery.R
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale
import java.io.File

@Composable
fun PersonDetailScreen(
    personId: Long,
    onBack: () -> Unit,
    onMediaClick: (CoreMediaEntity) -> Unit,
    viewModel: PeopleViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val person by viewModel.observePerson(personId).collectAsState(initial = null)
    val mediaList by viewModel.observeMediaForPerson(personId).collectAsState(initial = emptyList())
    val haptic = LocalHapticFeedback.current

    var showMenu by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    if (showNameDialog && person != null) {
        NamePersonDialog(
            initialName = person?.name,
            onDismissRequest = { showNameDialog = false },
            onConfirm = { newName ->
                viewModel.setPersonName(personId, newName)
                showNameDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(IconSizeTokens.L)
                        )
                    }
                },
                actions = {
                    person?.let { p ->
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleFavorite(p)
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    if (p.isFavorite) R.drawable.ic_ms_favorite_filled else R.drawable.ic_ms_favorite
                                ),
                                contentDescription = if (p.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (p.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(IconSizeTokens.L)
                            )
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_more_vert),
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(IconSizeTokens.L)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = ShapeLarge,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (p.name.isNullOrBlank()) "Add name" else "Rename",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_edit),
                                            contentDescription = null,
                                            modifier = Modifier.size(IconSizeTokens.M)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showNameDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Hide from People",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_visibility_off),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(IconSizeTokens.M)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.hidePerson(personId)
                                        onBack()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(
                start = SpacingTokens.XS,
                end = SpacingTokens.XS,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + SpacingTokens.XXL
            ),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Header
            item(span = { GridItemSpan(3) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.L),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                ) {
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 4.dp,
                        modifier = Modifier.size(104.dp)
                    ) {
                        val coverFile = person?.coverCropPath?.let { File(it) }
                        if (coverFile != null && coverFile.exists()) {
                            AsyncImage(
                                model = coverFile,
                                contentDescription = person?.name ?: "Person",
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
                                if (!person?.name.isNullOrBlank()) {
                                    Text(
                                        text = person?.name!!.take(1).uppercase(),
                                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(IconSizeTokens.XXL)
                                    )
                                }
                            }
                        }
                    }

                    if (!person?.name.isNullOrBlank()) {
                        Text(
                            text = person!!.name!!,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        FilledTonalButton(
                            onClick = { showNameDialog = true },
                            shape = ShapeFull,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_edit),
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeTokens.S)
                            )
                            Spacer(modifier = Modifier.width(SpacingTokens.S))
                            Text(
                                text = "Add name",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Text(
                        text = "${mediaList.size} ${if (mediaList.size == 1) "photo" else "photos"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(SpacingTokens.S))
                }
            }

            // Media Photos
            items(
                items = mediaList,
                key = { "person_media_${it.id}" }
            ) { media ->
                PersonMediaTile(
                    media = media,
                    onClick = { onMediaClick(media) }
                )
            }
        }
    }
}

@Composable
private fun PersonMediaTile(
    media: CoreMediaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = ShapeSmall,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .aspectRatio(1f)
            .pressScale(pressedScale = 0.95f)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = Uri.parse(media.uriString),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (media.isVideo) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
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
