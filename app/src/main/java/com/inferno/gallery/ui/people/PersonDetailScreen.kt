@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)

package com.inferno.gallery.ui.people

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext


import androidx.compose.ui.res.stringResource

import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.TopAppBar
import com.inferno.gallery.ui.GalleryViewModel
import com.inferno.gallery.ui.components.AlbumPickerSheet
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.inferno.gallery.data.db.PersonEntity
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    onBack: () -> Unit,
    onMediaClick: (CoreMediaEntity) -> Unit,
    viewModel: PeopleViewModel = viewModel(),
    galleryViewModel: GalleryViewModel = viewModel(),
    onCreateCollage: ((List<String>) -> Unit)? = null,
    onCreateStitch: ((List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val person by viewModel.observePerson(personId).collectAsState(initial = null)
    val mediaList by viewModel.observeMediaForPerson(personId).collectAsState(initial = emptyList())
    val allPeople by viewModel.allPersons.collectAsState()
    val haptic = LocalHapticFeedback.current

    var selectedMediaIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedMediaIds.isNotEmpty()
    val selectedMedia = remember(selectedMediaIds, mediaList) {
        mediaList.filter { it.id in selectedMediaIds }
    }

    val savedGridCols by viewModel.personDetailGridCellsCount.collectAsState()
    var gridCols by rememberSaveable(savedGridCols) { mutableIntStateOf(savedGridCols) }
    LaunchedEffect(savedGridCols) {
        gridCols = savedGridCols
    }
    var isPinching by remember { mutableStateOf(false) }
    val livePinchScaleAnim = remember { Animatable(1f) }
    val pinchScaleScope = rememberCoroutineScope()

    var showMultiDeleteConfirmDialog by remember { mutableStateOf(false) }
    val confirmDeleteEnabled by viewModel.settingsRepository.confirmDeleteEnabledFlow.collectAsState(initial = true)
    var showCopySheet by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var createMenuExpanded by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            selectedMediaIds = emptySet()
        }
    }

    val performTrash: () -> Unit = {
        val uris = selectedMedia.map { Uri.parse(it.uriString) }
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val trashIntent = android.provider.MediaStore.createTrashRequest(
                        context.contentResolver,
                        uris,
                        true
                    )
                    val request = androidx.activity.result.IntentSenderRequest.Builder(trashIntent.intentSender).build()
                    withContext(Dispatchers.Main) {
                        trashLauncher.launch(request)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    BackHandler(enabled = isSelectionMode) {
        selectedMediaIds = emptySet()
    }

    var showMenu by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showMergePicker by remember { mutableStateOf(false) }
    var showFeaturePhotoPicker by remember { mutableStateOf(false) }
    var showMovePhotosPicker by remember { mutableStateOf(false) }
    var mergeCandidate by remember { mutableStateOf<PersonEntity?>(null) }
    var collisionTarget by remember { mutableStateOf<PersonEntity?>(null) }
    var isMerging by remember { mutableStateOf(false) }

    if (showFeaturePhotoPicker && person != null) {
        FeaturePhotoPickerSheet(
            person = person!!,
            photos = mediaList,
            onDismissRequest = { showFeaturePhotoPicker = false },
            onPhotoSelected = { media ->
                viewModel.setMediaAsCover(personId, media.id)
                Toast.makeText(context, "Feature photo updated", Toast.LENGTH_SHORT).show()
                showFeaturePhotoPicker = false
            }
        )
    }

    if (showNameDialog && person != null) {
        NamePersonDialog(
            initialName = person?.name,
            onDismissRequest = { showNameDialog = false },
            onConfirm = { newName ->
                coroutineScope.launch {
                    val collision = viewModel.checkNameCollision(personId, newName)
                    if (collision != null) {
                        collisionTarget = collision
                    } else {
                        viewModel.setPersonName(personId, newName)
                    }
                    showNameDialog = false
                }
            }
        )
    }

    if (isMerging) {
        Dialog(
            onDismissRequest = { /* prevent cancel while merging */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = ShapeLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.padding(SpacingTokens.L)
            ) {
                Row(
                    modifier = Modifier.padding(SpacingTokens.XL),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.L)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "Merging groups...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    val currentCollision = collisionTarget
    if (currentCollision != null) {
        val targetName = currentCollision.name ?: "Person #${currentCollision.id}"
        SamePersonCollisionDialog(
            existingPersonName = targetName,
            onDismissRequest = { if (!isMerging) collisionTarget = null },
            onConfirmMerge = {
                isMerging = true
                viewModel.mergePersons(sourcePersonId = personId, targetPersonId = currentCollision.id) { success ->
                    isMerging = false
                    collisionTarget = null
                    if (success) {
                        Toast.makeText(context, "Merged into $targetName", Toast.LENGTH_SHORT).show()
                        onBack()
                    } else {
                        Toast.makeText(context, "Failed to merge person", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showMergePicker) {
        MergePersonPickerSheet(
            people = allPeople,
            currentPersonId = personId,
            onDismissRequest = { showMergePicker = false },
            onSelectTargetPerson = { target ->
                showMergePicker = false
                mergeCandidate = target
            }
        )
    }

    val currentCandidate = mergeCandidate
    if (currentCandidate != null) {
        val sourceName = person?.name ?: "Person #$personId"
        val targetName = currentCandidate.name ?: "Person #${currentCandidate.id}"
        MergePersonDialog(
            sourceName = sourceName,
            targetName = targetName,
            onDismissRequest = { if (!isMerging) mergeCandidate = null },
            onConfirm = {
                isMerging = true
                viewModel.mergePersons(sourcePersonId = personId, targetPersonId = currentCandidate.id) { success ->
                    isMerging = false
                    mergeCandidate = null
                    if (success) {
                        Toast.makeText(context, "Merged into $targetName", Toast.LENGTH_SHORT).show()
                        onBack()
                    } else {
                        Toast.makeText(context, "Failed to merge person", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showMovePhotosPicker) {
        MergePersonPickerSheet(
            people = allPeople,
            currentPersonId = personId,
            title = stringResource(R.string.people_move_photos_title),
            subtitle = stringResource(R.string.people_move_photos_desc, selectedMediaIds.size),
            onDismissRequest = { showMovePhotosPicker = false },
            onSelectTargetPerson = { target ->
                showMovePhotosPicker = false
                val count = selectedMediaIds.size
                val targetName = target.name ?: "Person #${target.id}"
                viewModel.moveMediaToPerson(
                    sourcePersonId = personId,
                    targetPersonId = target.id,
                    mediaIds = selectedMediaIds.toList()
                ) { success ->
                    if (success) {
                        Toast.makeText(context, "Moved $count photo(s) to $targetName", Toast.LENGTH_SHORT).show()
                        selectedMediaIds = emptySet()
                    } else {
                        Toast.makeText(context, "Failed to move photos", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCreateNewPerson = { newName ->
                showMovePhotosPicker = false
                val count = selectedMediaIds.size
                viewModel.createPersonAndMoveMedia(
                    sourcePersonId = personId,
                    name = newName,
                    mediaIds = selectedMediaIds.toList()
                ) { success ->
                    if (success) {
                        Toast.makeText(context, "Moved $count photo(s) to $newName", Toast.LENGTH_SHORT).show()
                        selectedMediaIds = emptySet()
                    } else {
                        Toast.makeText(context, "Failed to move photos", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showMultiDeleteConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showMultiDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_move_to_trash_title)) },
            text = { Text(stringResource(R.string.dialog_multi_trash_msg, selectedMediaIds.size)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showMultiDeleteConfirmDialog = false
                        performTrash()
                    }
                ) {
                    Text(stringResource(R.string.dialog_move_to_trash_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showMultiDeleteConfirmDialog = false }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showCopySheet && selectedMedia.isNotEmpty()) {
        val allAlbumsList by galleryViewModel.allAlbums.collectAsState()
        val uris = remember(selectedMedia) { selectedMedia.map { Uri.parse(it.uriString) } }
        AlbumPickerSheet(
            title = stringResource(R.string.sheet_copy_to_album),
            itemCount = uris.size,
            albums = allAlbumsList,
            showPrivateSpaceOption = false,
            confirmPathButtonText = stringResource(R.string.sheet_copy_here),
            onDismissRequest = { showCopySheet = false },
            onSelectAlbum = { bucket ->
                showCopySheet = false
                galleryViewModel.copyMedia(uris, bucket) {
                    selectedMediaIds = emptySet()
                }
            },
            onSelectPath = { path ->
                showCopySheet = false
                galleryViewModel.copyMediaToPath(uris, path) {
                    selectedMediaIds = emptySet()
                }
            }
        )
    }

    if (showMoveSheet && selectedMedia.isNotEmpty()) {
        val allAlbumsList by galleryViewModel.allAlbums.collectAsState()
        val uris = remember(selectedMedia) { selectedMedia.map { Uri.parse(it.uriString) } }
        AlbumPickerSheet(
            title = stringResource(R.string.sheet_move_to_album),
            itemCount = uris.size,
            albums = allAlbumsList,
            showPrivateSpaceOption = true,
            confirmPathButtonText = stringResource(R.string.sheet_move_here),
            onDismissRequest = { showMoveSheet = false },
            onSelectAlbum = { bucket ->
                showMoveSheet = false
                galleryViewModel.moveMedia(uris, bucket) {
                    selectedMediaIds = emptySet()
                }
            },
            onSelectPath = { path ->
                showMoveSheet = false
                galleryViewModel.moveMediaToPath(uris, path) {
                    selectedMediaIds = emptySet()
                }
            },
            onSelectPrivateSpace = {
                showMoveSheet = false
                val activity = context as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    galleryViewModel.vaultAuthManager.authenticate(
                        activity = activity,
                        onSuccess = {
                            galleryViewModel.hideMedia(uris)
                            selectedMediaIds = emptySet()
                        },
                        onFailure = {}
                    )
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            text = "${selectedMediaIds.size} selected",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode) {
                                selectedMediaIds = emptySet()
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (isSelectionMode) R.drawable.ic_ms_close else R.drawable.ic_ms_arrow_back
                            ),
                            contentDescription = if (isSelectionMode) "Clear selection" else "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(IconSizeTokens.L)
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Move to different cluster
                        IconButton(
                            onClick = { showMovePhotosPicker = true }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                                contentDescription = "Move to different cluster",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(IconSizeTokens.L)
                            )
                        }

                        // Remove from cluster
                        IconButton(
                            onClick = {
                                val count = selectedMediaIds.size
                                viewModel.removeMediaFromPerson(personId, selectedMediaIds.toList())
                                Toast.makeText(context, "Removed $count photo(s) from this cluster", Toast.LENGTH_SHORT).show()
                                selectedMediaIds = emptySet()
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_remove),
                                contentDescription = "Remove from cluster",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(IconSizeTokens.L)
                            )
                        }

                        // Select or Deselect All
                        IconButton(
                            onClick = {
                                if (selectedMediaIds.size == mediaList.size) {
                                    selectedMediaIds = emptySet()
                                } else {
                                    selectedMediaIds = mediaList.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_select_all),
                                contentDescription = "Select or Deselect All",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(IconSizeTokens.L)
                            )
                        }
                    } else {
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
                                                text = "Change feature photo",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_camera),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(IconSizeTokens.M)
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            showFeaturePhotoPicker = true
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Merge with another person",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(IconSizeTokens.M)
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            showMergePicker = true
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .peopleGridPinchZoom(
                    gridCellsCount = gridCols,
                    onGridCountChange = { newCols ->
                        val clamped = newCols.coerceIn(2, 5)
                        gridCols = clamped
                        viewModel.updatePersonDetailGridCellsCount(clamped)
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
                contentPadding = PaddingValues(
                    start = SpacingTokens.XS,
                    end = SpacingTokens.XS,
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + SpacingTokens.XXL + (if (isSelectionMode) 72.dp else 0.dp)
                ),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = livePinchScaleAnim.value
                        scaleY = livePinchScaleAnim.value
                    }
            ) {
                // Hero Header
                item(span = { GridItemSpan(gridCols) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.L),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .clickable { showFeaturePhotoPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxSize()
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
                val isSelected = selectedMediaIds.contains(media.id)
                PersonMediaTile(
                    media = media,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) {
                            selectedMediaIds = if (isSelected) selectedMediaIds - media.id else selectedMediaIds + media.id
                        } else {
                            onMediaClick(media)
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedMediaIds = if (isSelected) selectedMediaIds - media.id else selectedMediaIds + media.id
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = isPinching,
            enter = fadeIn(MotionTokens.fastEffectsSpec()) + scaleIn(MotionTokens.bouncySpring()),
            exit = fadeOut(MotionTokens.fastEffectsSpec()) + scaleOut(MotionTokens.fastEffectsSpec()),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 64.dp)
        ) {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = "$gridCols columns",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
    }

    // Floating Selection Tools Dock
    AnimatedVisibility(
        visible = isSelectionMode,
        enter = slideInVertically(MotionTokens.gentleSpring()) { it } + fadeIn(MotionTokens.gentleSpring()),
        exit = slideOutVertically(MotionTokens.gentleSpring()) { it } + fadeOut(MotionTokens.gentleSpring()),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
    ) {
        val isCreateEnabled = selectedMediaIds.size in 1..8
        val isStitchEnabled = selectedMediaIds.size in 2..10

        Surface(
            shape = ShapeExtraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share
                IconButton(
                    onClick = {
                        if (selectedMedia.isNotEmpty()) {
                            coroutineScope.launch {
                                val strip = viewModel.settingsRepository.stripMetadataOnShareFlow.first()
                                val uris = selectedMedia.map { Uri.parse(it.uriString) }
                                ShareUtils.shareMedia(context, uris, strip)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_share),
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(IconSizeTokens.L)
                    )
                }

                // Copy
                IconButton(onClick = { showCopySheet = true }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_content_copy),
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(IconSizeTokens.L)
                    )
                }

                // Move
                IconButton(onClick = { showMoveSheet = true }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_drive_file_move),
                        contentDescription = "Move",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(IconSizeTokens.L)
                    )
                }

                // Create (Collage/Stitch)
                Box {
                    IconButton(onClick = { createMenuExpanded = true }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_add),
                            contentDescription = "Create",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(IconSizeTokens.L)
                        )
                    }
                    DropdownMenu(
                        expanded = createMenuExpanded,
                        onDismissRequest = { createMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_collage)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_grid_view),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                createMenuExpanded = false
                                if (isCreateEnabled) {
                                    val uris = selectedMedia.map { it.uriString }
                                    onCreateCollage?.invoke(uris)
                                } else {
                                    Toast.makeText(context, "Collage supports up to 8 images", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_stitch)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_sort),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                createMenuExpanded = false
                                if (isStitchEnabled) {
                                    val uris = selectedMedia.map { it.uriString }
                                    onCreateStitch?.invoke(uris)
                                } else {
                                    Toast.makeText(context, "Select between 2 and 10 images to stitch", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                // Delete
                IconButton(
                    onClick = {
                        if (confirmDeleteEnabled) {
                            showMultiDeleteConfirmDialog = true
                        } else {
                            performTrash()
                        }
                    }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(IconSizeTokens.L)
                    )
                }

                // More options
                Box {
                    IconButton(onClick = { moreMenuExpanded = true }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_more_vert),
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(IconSizeTokens.L)
                        )
                    }
                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false }
                    ) {
                        // Hide (Private Space)
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_hide)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_visibility_off),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                moreMenuExpanded = false
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                if (activity != null) {
                                    galleryViewModel.vaultAuthManager.authenticate(
                                        activity = activity,
                                        onSuccess = {
                                            val uris = selectedMedia.map { Uri.parse(it.uriString) }
                                            galleryViewModel.hideMedia(uris)
                                            selectedMediaIds = emptySet()
                                        },
                                        onFailure = {}
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonMediaTile(
    media: CoreMediaEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = ShapeSmall,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .aspectRatio(1f)
            .pressScale(pressedScale = 0.95f)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = ShapeSmall
                    )
                } else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = Uri.parse(media.uriString),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                            contentDescription = "Selected",
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
