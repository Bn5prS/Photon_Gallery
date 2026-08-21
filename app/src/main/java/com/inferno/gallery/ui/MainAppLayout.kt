package com.inferno.gallery.ui
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ButtonDefaults

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inferno.gallery.data.DockStyle
import com.inferno.gallery.ui.components.overscrollStretch

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.os.Environment
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material3.TextButton
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import com.inferno.gallery.ui.components.ExpressiveButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.MotionTokens
import androidx.compose.ui.input.pointer.pointerInput
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import androidx.compose.runtime.rememberCoroutineScope
import com.inferno.gallery.data.SettingsRepository
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainAppLayout(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?, String?) -> Unit,
    onCreateCollage: (List<String>) -> Unit = {},
    onCreateStitch: (List<String>) -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel()
) {
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = LocalContext.current as androidx.lifecycle.ViewModelStoreOwner
    )
    val selectedFilter by viewModel.selectedFilterIndex.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val albumSortOrder by viewModel.albumSortOrder.collectAsState()
    val dockStyle by viewModel.dockStyle.collectAsState()
    val nestedNavController = rememberNavController()

    var showMenu by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var settingsActiveSection by remember { mutableStateOf<String?>(null) }
    val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isScrollDockVisible by viewModel.isScrollDockVisible.collectAsState()

    LaunchedEffect(currentRoute) {
        viewModel.setScrollDockVisible(true)
        viewModel.setTopBarCollapsed(false)
    }

    val albumNameArg = navBackStackEntry?.arguments?.getString("bucketName")
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    var selectedUrisToDelete by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showTrashRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showTrashDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Collect toast events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    fun checkPhotosPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
             ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED)
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkVideosPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
             ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED)
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkAllFilesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    var photosGranted by remember { mutableStateOf(checkPhotosPermission()) }
    var videosGranted by remember { mutableStateOf(checkVideosPermission()) }
    var allFilesGranted by remember { mutableStateOf(checkAllFilesPermission()) }
    var hasRequestedMediaOnce by remember { mutableStateOf(false) }

    fun updatePermissionStates() {
        photosGranted = checkPhotosPermission()
        videosGranted = checkVideosPermission()
        allFilesGranted = checkAllFilesPermission()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updatePermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val intentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        updatePermissionStates()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        updatePermissionStates()
    }

    val triggerSync = {
        val syncWorkRequest = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.MediaSyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "MediaSyncWorker",
            androidx.work.ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selected = viewModel.selectedUris.value.toList()
            viewModel.deleteSelectedMediaFromDb(selected)
            viewModel.clearSelection()
            triggerSync()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.clearSelection()
            triggerSync()
        }
    }

    val permanentDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selected = viewModel.selectedUris.value.toList()
            viewModel.deleteSelectedMediaFromDb(selected)
            viewModel.clearSelection()
            triggerSync()
        }
    }

    val hasRequiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        photosGranted && videosGranted && allFilesGranted
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        photosGranted && allFilesGranted
    } else {
        photosGranted
    }
    
    if (onboardingCompleted == null) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    if (onboardingCompleted == false || !hasRequiredPermissions) {
        PermissionOnboardingScreen(
            photosGranted = photosGranted,
            videosGranted = videosGranted,
            allFilesGranted = allFilesGranted,
            onGrantMediaClick = {
                val activity = context as? android.app.Activity
                val showRationale = activity?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_MEDIA_IMAGES) ||
                        androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_MEDIA_VIDEO)
                    } else {
                        androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                } ?: true

                if (hasRequestedMediaOnce && !photosGranted && !showRationale) {
                    Toast.makeText(context, "Permissions permanently denied. Opening settings...", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    intentLauncher.launch(intent)
                } else {
                    hasRequestedMediaOnce = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                            )
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO
                            )
                        )
                    } else {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        )
                    }
                }
            },
            onGrantAllFilesClick = {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                intentLauncher.launch(intent)
            },
            onContinueClick = {
                viewModel.completeOnboarding()
                triggerSync()
            },
            modifier = modifier
        )
        return
    }
    val isTopBarCollapsed by viewModel.isTopBarCollapsed.collectAsState()

    val topBarColor = if (isSelectionMode) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Scaffold(
            modifier = modifier.fillMaxSize().overscrollStretch(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            if (currentRoute != "photo_map" && currentRoute != "duplicate_cleaner" && currentRoute != "all_albums" && currentRoute != "story_viewer" && currentRoute != "places_list") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(topBarColor)
                        .statusBarsPadding()
                ) {
                    if (isSelectionMode) {
                        Surface(
                            color = topBarColor,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = { viewModel.clearSelection() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_close), contentDescription = "Clear selection", modifier = Modifier.size(22.dp))
                                }
                                AnimatedContent(
                                    targetState = selectedUris.size,
                                    transitionSpec = {
                                        if (targetState > initialState) {
                                            (slideInVertically { -it } + fadeIn()) togetherWith
                                                    (slideOutVertically { it } + fadeOut())
                                        } else {
                                            (slideInVertically { it } + fadeIn()) togetherWith
                                                    (slideOutVertically { -it } + fadeOut())
                                        }
                                    },
                                    label = "selectionCount",
                                    modifier = Modifier.padding(start = 16.dp).weight(1f)
                                ) { count ->
                                    Text(
                                        "$count Selected",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = { viewModel.toggleSelectAll() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_select_all),
                                        contentDescription = "Select or Deselect All",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    } else if (currentRoute == "album/{bucketName}") {
                        Surface(
                            color = topBarColor,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = { nestedNavController.popBackStack() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = "Back", modifier = Modifier.size(22.dp))
                                }
                                val friendlyTitle = when {
                                    albumNameArg == "search_text" -> "Text Matches"
                                    albumNameArg == "search_smart" -> "Semantic Matches"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_RAW -> "RAW Photos"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_PANORAMAS -> "Panoramas"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_SLOW_MO -> "Slow Motion"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_ANIMATIONS -> "GIFs & Animations"
                                    albumNameArg?.startsWith("place:") == true -> albumNameArg.removePrefix("place:")
                                    else -> albumNameArg ?: "Album"
                                }
                                Text(
                                    friendlyTitle,
                                    style = if (friendlyTitle.length > 15) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .weight(1f)
                                )
                            }
                        }
                    } else if (currentRoute == "settings") {
                        Surface(
                            color = topBarColor,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        if (settingsActiveSection != null) {
                                            settingsActiveSection = null
                                        } else {
                                            nestedNavController.popBackStack()
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = "Back", modifier = Modifier.size(22.dp))
                                }
                                Text(
                                    settingsActiveSection ?: "Settings",
                                    style = if (settingsActiveSection != null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .weight(1f)
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = topBarColor,
                            shadowElevation = 0.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val titleText = when (currentRoute) {
                                        "albums" -> "Albums"
                                        "search" -> "Search"
                                        "photos" -> "Photos"
                                        else -> "Photon Gallery"
                                    }
                                    Text(
                                        titleText,
                                        style = MaterialTheme.typography.headlineLarge,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        if (currentRoute != "search") {
                                            FilledTonalIconButton(
                                                onClick = { nestedNavController.navigate("search") },
                                                modifier = Modifier.size(40.dp),
                                                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                                                    contentDescription = "Search",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        if (currentRoute == "albums") {
                                            FilledTonalIconButton(
                                                onClick = { showCreateAlbumDialog = true },
                                                modifier = Modifier.size(40.dp),
                                                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_create_new_folder),
                                                    contentDescription = "Create Album",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        
                                        var showOverflowMenu by remember { mutableStateOf(false) }
                                        var overflowState by remember { mutableStateOf("main") }
                                        Box {
                                            FilledTonalIconButton(
                                                onClick = { showOverflowMenu = true },
                                                modifier = Modifier.size(40.dp),
                                                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_more_vert),
                                                    contentDescription = "Menu",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showOverflowMenu,
                                                onDismissRequest = { 
                                                    showOverflowMenu = false
                                                    overflowState = "main"
                                                },
                                                shape = ShapeExtraLarge,
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                            ) {
                                                if (overflowState == "main") {
                                                    DropdownMenuItem(
                                                        text = { Text("Settings") },
                                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_settings), contentDescription = null) },
                                                        onClick = {
                                                            showOverflowMenu = false
                                                            overflowState = "main"
                                                            nestedNavController.navigate("settings") {
                                                                popUpTo("photos") { saveState = true }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        }
                                                    )
                                                    
                                                    if (currentRoute == "photos") {
                                                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                                        DropdownMenuItem(
                                                            text = { Text("View Mode") },
                                                            leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_grid_view), contentDescription = null) },
                                                            trailingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_chevron_right), contentDescription = null) },
                                                            onClick = { overflowState = "view" }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Sort By") },
                                                            leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_sort), contentDescription = null) },
                                                            trailingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_chevron_right), contentDescription = null) },
                                                            onClick = { overflowState = "sort" }
                                                        )
                                                    }
                                                } else if (overflowState == "view") {
                                                    DropdownMenuItem(
                                                        text = { Text("Back") },
                                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = null) },
                                                        onClick = { overflowState = "main" }
                                                    )
                                                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                                    DropdownMenuItem(text = { Text("Immersive View") }, trailingIcon = { androidx.compose.material3.RadioButton(selected = viewMode == ViewMode.Immersive, onClick = null) }, onClick = { viewModel.setViewMode(ViewMode.Immersive); showOverflowMenu = false; overflowState = "main" })
                                                    DropdownMenuItem(text = { Text("Grouped View") }, trailingIcon = { androidx.compose.material3.RadioButton(selected = viewMode == ViewMode.Grouped, onClick = null) }, onClick = { viewModel.setViewMode(ViewMode.Grouped); showOverflowMenu = false; overflowState = "main" })
                                                } else if (overflowState == "sort") {
                                                    DropdownMenuItem(
                                                        text = { Text("Back") },
                                                        leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = null) },
                                                        onClick = { overflowState = "main" }
                                                    )
                                                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                                    DropdownMenuItem(text = { Text("New to Old") }, trailingIcon = { androidx.compose.material3.RadioButton(selected = sortOrder == SortOrder.NewToOld, onClick = null) }, onClick = { viewModel.setSortOrder(SortOrder.NewToOld); showOverflowMenu = false; overflowState = "main" })
                                                    DropdownMenuItem(text = { Text("Old to New") }, trailingIcon = { androidx.compose.material3.RadioButton(selected = sortOrder == SortOrder.OldToNew, onClick = null) }, onClick = { viewModel.setSortOrder(SortOrder.OldToNew); showOverflowMenu = false; overflowState = "main" })
                                                    DropdownMenuItem(text = { Text("Large to Small") }, trailingIcon = { androidx.compose.material3.RadioButton(selected = sortOrder == SortOrder.BigToSmall, onClick = null) }, onClick = { viewModel.setSortOrder(SortOrder.BigToSmall); showOverflowMenu = false; overflowState = "main" })
                                                    DropdownMenuItem(text = { Text("Small to Large") }, trailingIcon = { androidx.compose.material3.RadioButton(selected = sortOrder == SortOrder.SmallToBig, onClick = null) }, onClick = { viewModel.setSortOrder(SortOrder.SmallToBig); showOverflowMenu = false; overflowState = "main" })
                                                    DropdownMenuItem(text = { Text("A - Z") }, trailingIcon = { androidx.compose.material3.RadioButton(selected = sortOrder == SortOrder.NameAsc, onClick = null) }, onClick = { viewModel.setSortOrder(SortOrder.NameAsc); showOverflowMenu = false; overflowState = "main" })
                                                }
                                            }
                                        }
                                    }
                                }

                                // ── Secondary Chips Row (Below Photos Title) ──
                                if (currentRoute == "photos") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 6.dp, top = 0.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isAll = selectedFilter == 0
                                        val isCamera = selectedFilter == 1

                                        val cameraBg by animateColorAsState(
                                            targetValue = if (isCamera) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                            animationSpec = MotionTokens.snappySpring(),
                                            label = "CameraChipBg"
                                        )
                                        val cameraContentColor by animateColorAsState(
                                            targetValue = if (isCamera) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            animationSpec = MotionTokens.snappySpring(),
                                            label = "CameraChipColor"
                                        )
                                        val cameraScale by animateFloatAsState(
                                            targetValue = if (isCamera) 1.02f else 1.0f,
                                            animationSpec = MotionTokens.bouncySpring(),
                                            label = "CameraChipScale"
                                        )

                                        val allBg by animateColorAsState(
                                            targetValue = if (isAll) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                            animationSpec = MotionTokens.snappySpring(),
                                            label = "AllChipBg"
                                        )
                                        val allContentColor by animateColorAsState(
                                            targetValue = if (isAll) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            animationSpec = MotionTokens.snappySpring(),
                                            label = "AllChipColor"
                                        )
                                        val allScale by animateFloatAsState(
                                            targetValue = if (isAll) 1.02f else 1.0f,
                                            animationSpec = MotionTokens.bouncySpring(),
                                            label = "AllChipScale"
                                        )

                                        Surface(
                                            onClick = {
                                                if (!isCamera) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.setFilterIndex(1)
                                                }
                                            },
                                            shape = CircleShape,
                                            color = cameraBg,
                                            contentColor = cameraContentColor,
                                            modifier = Modifier.graphicsLayer {
                                                scaleX = cameraScale
                                                scaleY = cameraScale
                                            }
                                        ) {
                                            Text(
                                                text = "Camera",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isCamera) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                            )
                                        }

                                        Surface(
                                            onClick = {
                                                if (!isAll) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.setFilterIndex(0)
                                                }
                                            },
                                            shape = CircleShape,
                                            color = allBg,
                                            contentColor = allContentColor,
                                            modifier = Modifier.graphicsLayer {
                                                scaleX = allScale
                                                scaleY = allScale
                                            }
                                        ) {
                                            Text(
                                                text = "All",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            val isHiddenAlbum = currentRoute?.startsWith("album/") == true
            // Pill dock auto-hide on scroll temporarily disabled per user request (preserved for later revival)
            // val isDockVisible = currentRoute != "settings" && currentRoute != "duplicate_cleaner" && currentRoute != "photo_map" && currentRoute != "all_albums" && currentRoute != "story_viewer" && currentRoute != "places_list" && !isHiddenAlbum && !isSelectionMode && (dockStyle != DockStyle.PILL || isScrollDockVisible)
            val isDockVisible = currentRoute != "settings" && currentRoute != "duplicate_cleaner" && currentRoute != "photo_map" && currentRoute != "all_albums" && currentRoute != "story_viewer" && currentRoute != "places_list" && !isHiddenAlbum && !isSelectionMode
            AnimatedVisibility(
                visible = isDockVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                if (dockStyle == DockStyle.PILL) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 6.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        FloatingNavigationPill(
                            currentRoute = currentRoute,
                            onNavigateToPhotos = {
                                nestedNavController.navigate("photos") {
                                    popUpTo("photos") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToAlbums = {
                                nestedNavController.navigate("albums") {
                                    popUpTo("photos") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToSearch = {
                                nestedNavController.navigate("search") {
                                    popUpTo("photos") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                } else {
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {},
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .height(50.dp)
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DockItem(
                                icon = { Icon(if (currentRoute == "photos") ImageVector.vectorResource(R.drawable.ic_ms_image) else ImageVector.vectorResource(R.drawable.ic_ms_image), contentDescription = "Photos", modifier = Modifier.size(24.dp)) },
                                label = "Photos",
                                isSelected = currentRoute == "photos",
                                onClick = { nestedNavController.navigate("photos") {
                                    popUpTo("photos") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                } }
                            )
                            DockItem(
                                icon = { Icon(if (currentRoute?.startsWith("album") == true || currentRoute == "albums") ImageVector.vectorResource(R.drawable.ic_ms_photo_album) else ImageVector.vectorResource(R.drawable.ic_ms_photo_album), contentDescription = "Albums", modifier = Modifier.size(24.dp)) },
                                label = "Albums",
                                isSelected = currentRoute?.startsWith("album") == true || currentRoute == "albums",
                                onClick = { nestedNavController.navigate("albums") {
                                    popUpTo("photos") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                } }
                            )
                            DockItem(
                                icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search), contentDescription = "Search", modifier = Modifier.size(24.dp)) },
                                label = "Search",
                                isSelected = currentRoute == "search",
                                onClick = { nestedNavController.navigate("search") {
                                    popUpTo("photos") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                } }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = nestedNavController,
            startDestination = "photos",
            enterTransition = { getEnterTransition(initialState.destination.route, targetState.destination.route) },
            exitTransition = { getExitTransition(initialState.destination.route, targetState.destination.route) },
            popEnterTransition = { getEnterTransition(initialState.destination.route, targetState.destination.route) },
            popExitTransition = { getExitTransition(initialState.destination.route, targetState.destination.route) }
        ) {
            composable("photos") {
                GalleryScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onPhotoClick = onPhotoClick,
                    viewModel = viewModel,
                    contentPadding = innerPadding,
                    isMainTab = true,
                    onNavigateToSettings = {
                        nestedNavController.navigate("settings") {
                            popUpTo("photos") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = "albums",
                enterTransition = {
                    if (initialState.destination.route?.startsWith("album/") == true || initialState.destination.route == "all_albums") {
                        fadeIn(MotionTokens.snappySpring())
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                exitTransition = {
                    if (targetState.destination.route?.startsWith("album/") == true || targetState.destination.route == "all_albums") {
                        fadeOut(MotionTokens.snappySpring())
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route?.startsWith("album/") == true || initialState.destination.route == "all_albums") {
                        fadeIn(MotionTokens.snappySpring())
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popExitTransition = {
                    if (targetState.destination.route?.startsWith("album/") == true || targetState.destination.route == "all_albums") {
                        fadeOut(MotionTokens.snappySpring())
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                }
            ) {
                var pendingCoverAlbumName by remember { mutableStateOf<String?>(null) }
                val pickCoverLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        val albumName = pendingCoverAlbumName
                        if (albumName != null) {
                            viewModel.setAlbumCustomCover(albumName, uri.toString())
                        }
                    }
                    pendingCoverAlbumName = null
                }

                AlbumsScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                    viewModel = viewModel,
                    contentPadding = innerPadding,
                    onAlbumClick = { bucketName -> 
                        val encoded = android.net.Uri.encode(bucketName)
                        nestedNavController.navigate("album/$encoded")
                    },
                    onPersonClick = { personId ->
                        nestedNavController.navigate("person/$personId")
                    },
                    onNavigateToVault = onNavigateToVault,
                    onNavigateToDuplicateCleaner = { nestedNavController.navigate("duplicate_cleaner") },
                    onNavigateToPhotoMap = { nestedNavController.navigate("photo_map") },
                    onNavigateToPlacesList = { nestedNavController.navigate("places_list") },
                    onNavigateToAllAlbums = { nestedNavController.navigate("all_albums") },
                    onChangeCover = { albumName ->
                        pendingCoverAlbumName = albumName
                        pickCoverLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onDeleteCover = { albumName ->
                        viewModel.removeAlbumCustomCover(albumName)
                    }
                )
            }
            
            composable(
                route = "places_list",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = MotionTokens.snappySpring()
                    ) + fadeIn(MotionTokens.snappySpring())
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = MotionTokens.snappySpring()
                    ) + fadeOut(MotionTokens.snappySpring())
                }
            ) {
                PlacesListScreen(
                    viewModel = viewModel,
                    onNavigateUp = { nestedNavController.popBackStack() },
                    onNavigateToMap = { nestedNavController.navigate("photo_map") },
                    onPlaceClick = { placeName ->
                        val target = if (placeName.startsWith("place:")) placeName else "place:$placeName"
                        val encoded = android.net.Uri.encode(target)
                        nestedNavController.navigate("album/$encoded")
                    }
                )
            }

            composable(
                route = "duplicate_cleaner",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = MotionTokens.snappySpring()
                    ) + fadeIn(MotionTokens.snappySpring())
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = MotionTokens.snappySpring()
                    ) + fadeOut(MotionTokens.snappySpring())
                }
            ) {
                DuplicateCleanerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { nestedNavController.popBackStack() }
                )
            }


            composable(
                route = "album/{bucketName}",
                enterTransition = {
                    if (initialState.destination.route == "albums" || initialState.destination.route == "all_albums" || initialState.destination.route == "search") {
                        fadeIn(androidx.compose.animation.core.tween(150))
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                exitTransition = {
                    if (targetState.destination.route == "albums" || targetState.destination.route == "all_albums" || targetState.destination.route == "search") {
                        fadeOut(androidx.compose.animation.core.tween(150))
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route == "albums" || initialState.destination.route == "all_albums" || initialState.destination.route == "search") {
                        fadeIn(androidx.compose.animation.core.tween(150))
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popExitTransition = {
                    if (targetState.destination.route == "albums" || targetState.destination.route == "all_albums" || targetState.destination.route == "search") {
                        fadeOut(androidx.compose.animation.core.tween(150))
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                }
            ) { backStackEntry ->
                val bucketName = backStackEntry.arguments?.getString("bucketName") ?: ""
                val boundsModifier = with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "album_$bucketName"),
                        animatedVisibilityScope = this@composable,
                        enter = fadeIn(androidx.compose.animation.core.tween(150)),
                        exit = fadeOut(androidx.compose.animation.core.tween(150)),
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                        clipInOverlayDuringTransition = OverlayClip(androidx.compose.foundation.shape.RoundedCornerShape(0.dp)),
                        boundsTransform = { _, _ -> MotionTokens.sharedElementSpring() }
                    )
                }
                Box(modifier = boundsModifier.fillMaxSize()) {
                    GalleryScreen(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onPhotoClick = onPhotoClick,
                        viewModel = viewModel,
                        contentPadding = innerPadding,
                        bucketName = bucketName
                    )
                }
            }
            composable("search") {
                SearchScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onPhotoClick = onPhotoClick,
                    viewModel = viewModel,
                    contentPadding = innerPadding,
                    onAlbumClick = { bucketName ->
                        val encoded = android.net.Uri.encode(bucketName)
                        nestedNavController.navigate("album/$encoded")
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    contentPadding = innerPadding,
                    galleryViewModel = viewModel,
                    onBackClick = { nestedNavController.popBackStack() },
                    onNavigateToVault = onNavigateToVault,
                    activeSection = settingsActiveSection,
                    onActiveSectionChange = { settingsActiveSection = it }
                )
            }


            composable("photo_map") {
                PhotoMapScreen(
                    galleryViewModel = viewModel,
                    onPhotoClick = onPhotoClick,
                    onBackClick = { nestedNavController.popBackStack() }
                )
            }
            composable(
                route = "all_albums",
                enterTransition = {
                    if (initialState.destination.route?.startsWith("album/") == true || initialState.destination.route == "albums") {
                        fadeIn(MotionTokens.snappySpring())
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                exitTransition = {
                    if (targetState.destination.route?.startsWith("album/") == true || targetState.destination.route == "albums") {
                        fadeOut(MotionTokens.snappySpring())
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route?.startsWith("album/") == true || initialState.destination.route == "albums") {
                        fadeIn(MotionTokens.snappySpring())
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popExitTransition = {
                    if (targetState.destination.route?.startsWith("album/") == true || targetState.destination.route == "albums") {
                        fadeOut(MotionTokens.snappySpring())
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                }
            ) {
                var pendingCoverAlbumName by remember { mutableStateOf<String?>(null) }
                val pickCoverLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        val albumName = pendingCoverAlbumName
                        if (albumName != null) {
                            viewModel.setAlbumCustomCover(albumName, uri.toString())
                        }
                    }
                    pendingCoverAlbumName = null
                }
                AllAlbumsScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                    galleryViewModel = viewModel,
                    onBackClick = { nestedNavController.popBackStack() },
                    onAlbumClick = { bucketName -> 
                        val encoded = android.net.Uri.encode(bucketName)
                        nestedNavController.navigate("album/$encoded")
                    },
                    onChangeCover = { bucketName -> 
                        pendingCoverAlbumName = bucketName
                        pickCoverLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onDeleteCover = { bucketName ->
                        viewModel.removeAlbumCustomCover(bucketName)
                    }
                )
            }
        }
        
        // Selection Mode SplitButton Overlay
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(MotionTokens.gentleSpring()) { it } + fadeIn(MotionTokens.gentleSpring()),
                exit = slideOutVertically(MotionTokens.gentleSpring()) { it } + fadeOut(MotionTokens.gentleSpring()),
                modifier = Modifier
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
            ) {
            var expanded by remember { mutableStateOf(false) }
            var showMoveSheet by remember { mutableStateOf(false) }
            var showCopySheet by remember { mutableStateOf(false) }

            var showMultiDeleteConfirmDialog by remember { mutableStateOf(false) }
            val confirmDeleteEnabled by viewModel.settingsRepository.confirmDeleteEnabledFlow.collectAsState(initial = true)
            
            if (showMoveSheet) {
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { showMoveSheet = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    var isBrowsingStorage by remember { mutableStateOf(false) }
                    val albums by viewModel.allAlbums.collectAsState()

                    if (!isBrowsingStorage) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 24.dp)
                        ) {
                            Text(
                                "Move to Album",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                "${selectedUris.size} items · ${albums.size} albums",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Tonal button at the top to browse system storage tree
                            ExpressiveButton(
                                onClick = { isBrowsingStorage = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Browse Other Folders",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.heightIn(max = 400.dp)
                            ) {
                                // Private Space as first item
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showMoveSheet = false
                                                val activity = context as? androidx.fragment.app.FragmentActivity
                                                if (activity != null) {
                                                    viewModel.vaultAuthManager.authenticate(
                                                        activity = activity,
                                                        onSuccess = {
                                                            val uris = selectedUris.mapNotNull { android.net.Uri.parse(it) }
                                                            viewModel.hideMedia(uris)
                                                            viewModel.clearSelection()
                                                        },
                                                        onFailure = {}
                                                    )
                                                }
                                            }
                                            .padding(vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                ImageVector.vectorResource(R.drawable.ic_ms_shield),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Private Space",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                            )
                                            Text(
                                                "Hidden \u00b7 Biometric protected",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            ImageVector.vectorResource(R.drawable.ic_ms_visibility_off),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                }
                                items(albums.size) { index ->
                                    val album = albums[index]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showMoveSheet = false
                                                viewModel.moveSelectedMedia(album.bucketName)
                                            }
                                            .padding(vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        coil3.compose.AsyncImage(
                                            model = album.coverUri,
                                            contentDescription = album.bucketName,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                album.bucketName,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                            )
                                            Text(
                                                "${album.itemCount} items",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (index < albums.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.padding(start = 56.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Directory Tree Selector UI
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 24.dp)
                        ) {
                            val storageRoot = remember { android.os.Environment.getExternalStorageDirectory() }
                            var currentBrowsingDirectory by remember { mutableStateOf(storageRoot) }
                            var subdirectories by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
                            var showCreateFolderDialog by remember { mutableStateOf(false) }
                            var newFolderName by remember { mutableStateOf("") }
                            var refreshTrigger by remember { mutableStateOf(0) }

                            LaunchedEffect(currentBrowsingDirectory, refreshTrigger) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    subdirectories = currentBrowsingDirectory.listFiles()
                                        ?.filter { it.isDirectory && !it.name.startsWith(".") }
                                        ?.sortedBy { it.name.lowercase() }
                                        ?: emptyList()
                                }
                            }

                            if (showCreateFolderDialog) {
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { showCreateFolderDialog = false },
                                    title = { Text("Create New Folder") },
                                    text = {
                                        androidx.compose.material3.OutlinedTextField(
                                            value = newFolderName,
                                            onValueChange = { newFolderName = it },
                                            label = { Text("Folder Name") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    confirmButton = {
                                        androidx.compose.material3.TextButton(
                                            onClick = {
                                                if (newFolderName.isNotBlank()) {
                                                    val newDir = java.io.File(currentBrowsingDirectory, newFolderName.trim())
                                                    if (!newDir.exists()) {
                                                        newDir.mkdirs()
                                                        refreshTrigger++
                                                    }
                                                    showCreateFolderDialog = false
                                                    newFolderName = ""
                                                }
                                            }
                                        ) {
                                            Text("Create")
                                        }
                                    },
                                    dismissButton = {
                                        androidx.compose.material3.TextButton(
                                            onClick = {
                                                showCreateFolderDialog = false
                                                newFolderName = ""
                                            }
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.FilledTonalIconButton(
                                    onClick = {
                                        val parent = currentBrowsingDirectory.parentFile
                                        if (parent != null && parent.absolutePath.startsWith(storageRoot.absolutePath)) {
                                            currentBrowsingDirectory = parent
                                        } else {
                                            isBrowsingStorage = false
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                                        contentDescription = "Back"
                                    )
                                }
                                Text(
                                    text = "Browse Storage",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                androidx.compose.material3.FilledTonalIconButton(
                                    onClick = { showCreateFolderDialog = true }
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_create_new_folder),
                                        contentDescription = "Create Folder"
                                    )
                                }
                            }

                            val displayPath = remember(currentBrowsingDirectory) {
                                val relative = currentBrowsingDirectory.absolutePath
                                    .removePrefix(storageRoot.absolutePath)
                                    .removePrefix("/")
                                if (relative.isEmpty()) "Internal Storage" else "Internal Storage > " + relative.replace("/", " > ")
                            }
                            Text(
                                text = displayPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()
                            ) {
                                if (subdirectories.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No subfolders found",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(subdirectories.size) { index ->
                                        val folder = subdirectories[index]
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    currentBrowsingDirectory = folder
                                                }
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = folder.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (index < subdirectories.size - 1) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                                modifier = Modifier.padding(start = 40.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { isBrowsingStorage = false },
                                    modifier = Modifier.weight(1f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel")
                                }
                                ExpressiveButton(
                                    onClick = {
                                        showMoveSheet = false
                                        viewModel.moveSelectedMediaToPath(currentBrowsingDirectory.absolutePath)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                ) {
                                    Text("Move Here")
                                }
                            }
                        }
                    }
                }
            }
            
            if (showCopySheet) {
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { showCopySheet = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    var isBrowsingStorage by remember { mutableStateOf(false) }
                    val albums by viewModel.allAlbums.collectAsState()

                    if (!isBrowsingStorage) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 24.dp)
                        ) {
                            Text(
                                "Copy to Album",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                "${selectedUris.size} items · ${albums.size} albums",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Tonal button at the top to browse system storage tree
                            ExpressiveButton(
                                onClick = { isBrowsingStorage = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Browse Other Folders",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.heightIn(max = 400.dp)
                            ) {
                                items(albums.size) { index ->
                                    val album = albums[index]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showCopySheet = false
                                                viewModel.copySelectedMedia(album.bucketName)
                                            }
                                            .padding(vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        coil3.compose.AsyncImage(
                                            model = album.coverUri,
                                            contentDescription = album.bucketName,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                album.bucketName,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                            )
                                            Text(
                                                "${album.itemCount} items",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (index < albums.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.padding(start = 56.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Directory Tree Selector UI (Copy)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 24.dp)
                        ) {
                            val storageRoot = remember { android.os.Environment.getExternalStorageDirectory() }
                            var currentBrowsingDirectory by remember { mutableStateOf(storageRoot) }
                            var subdirectories by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
                            var showCreateFolderDialog by remember { mutableStateOf(false) }
                            var newFolderName by remember { mutableStateOf("") }
                            var refreshTrigger by remember { mutableStateOf(0) }

                            LaunchedEffect(currentBrowsingDirectory, refreshTrigger) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    subdirectories = currentBrowsingDirectory.listFiles()
                                        ?.filter { it.isDirectory && !it.name.startsWith(".") }
                                        ?.sortedBy { it.name.lowercase() }
                                        ?: emptyList()
                                }
                            }

                            if (showCreateFolderDialog) {
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { showCreateFolderDialog = false },
                                    title = { Text("Create New Folder") },
                                    text = {
                                        androidx.compose.material3.OutlinedTextField(
                                            value = newFolderName,
                                            onValueChange = { newFolderName = it },
                                            label = { Text("Folder Name") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    confirmButton = {
                                        androidx.compose.material3.TextButton(
                                            onClick = {
                                                if (newFolderName.isNotBlank()) {
                                                    val newDir = java.io.File(currentBrowsingDirectory, newFolderName.trim())
                                                    if (!newDir.exists()) {
                                                        newDir.mkdirs()
                                                        refreshTrigger++
                                                    }
                                                    showCreateFolderDialog = false
                                                    newFolderName = ""
                                                }
                                            }
                                        ) {
                                            Text("Create")
                                        }
                                    },
                                    dismissButton = {
                                        androidx.compose.material3.TextButton(
                                            onClick = {
                                                showCreateFolderDialog = false
                                                newFolderName = ""
                                            }
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.FilledTonalIconButton(
                                    onClick = {
                                        val parent = currentBrowsingDirectory.parentFile
                                        if (parent != null && parent.absolutePath.startsWith(storageRoot.absolutePath)) {
                                            currentBrowsingDirectory = parent
                                        } else {
                                            isBrowsingStorage = false
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                                        contentDescription = "Back"
                                    )
                                }
                                Text(
                                    text = "Browse Storage",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                androidx.compose.material3.FilledTonalIconButton(
                                    onClick = { showCreateFolderDialog = true }
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_create_new_folder),
                                        contentDescription = "Create Folder"
                                    )
                                }
                            }

                            val displayPath = remember(currentBrowsingDirectory) {
                                val relative = currentBrowsingDirectory.absolutePath
                                    .removePrefix(storageRoot.absolutePath)
                                    .removePrefix("/")
                                if (relative.isEmpty()) "Internal Storage" else "Internal Storage > " + relative.replace("/", " > ")
                            }
                            Text(
                                text = displayPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()
                            ) {
                                if (subdirectories.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No subfolders found",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(subdirectories.size) { index ->
                                        val folder = subdirectories[index]
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    currentBrowsingDirectory = folder
                                                }
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = folder.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (index < subdirectories.size - 1) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                                modifier = Modifier.padding(start = 40.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { isBrowsingStorage = false },
                                    modifier = Modifier.weight(1f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel")
                                }
                                ExpressiveButton(
                                    onClick = {
                                        showCopySheet = false
                                        viewModel.copySelectedMediaToPath(currentBrowsingDirectory.absolutePath)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                ) {
                                    Text("Copy Here")
                                }
                            }
                        }
                    }
                }
            }

            if (showMultiDeleteConfirmDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showMultiDeleteConfirmDialog = false },
                    title = { Text("Move to Recycle Bin") },
                    text = { Text("Move ${selectedUris.size} items to the Recycle Bin?") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showMultiDeleteConfirmDialog = false
                                val uris = selectedUris.map { Uri.parse(it) }
                                if (uris.isNotEmpty()) {
                                    try {
                                        val trashIntent = android.provider.MediaStore.createTrashRequest(
                                            context.contentResolver,
                                            uris,
                                            true
                                        )
                                        trashLauncher.launch(
                                            androidx.activity.result.IntentSenderRequest.Builder(trashIntent.intentSender).build()
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text("Move to Bin", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { showMultiDeleteConfirmDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            val isTrashPage = currentRoute == "album/{bucketName}" && albumNameArg == "Trash"
            if (isTrashPage) {
                if (showTrashRestoreConfirmDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showTrashRestoreConfirmDialog = false },
                        title = { Text("Restore Items") },
                        text = { Text("Restore ${selectedUris.size} items from the Recycle Bin?") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showTrashRestoreConfirmDialog = false
                                    val uris = selectedUris.map { Uri.parse(it) }
                                    if (uris.isNotEmpty()) {
                                        try {
                                            val restoreIntent = android.provider.MediaStore.createTrashRequest(
                                                context.contentResolver,
                                                uris,
                                                false
                                            )
                                            restoreLauncher.launch(
                                                androidx.activity.result.IntentSenderRequest.Builder(restoreIntent.intentSender).build()
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            android.widget.Toast.makeText(context, "Error restoring: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Text("Restore")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { showTrashRestoreConfirmDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (showTrashDeleteConfirmDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showTrashDeleteConfirmDialog = false },
                        title = { Text("Delete Permanently") },
                        text = { Text("Permanently delete ${selectedUris.size} items? This action cannot be undone.") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showTrashDeleteConfirmDialog = false
                                    val uris = selectedUris.map { Uri.parse(it) }
                                    if (uris.isNotEmpty()) {
                                        try {
                                            val deleteIntent = android.provider.MediaStore.createDeleteRequest(
                                                context.contentResolver,
                                                uris
                                            )
                                            permanentDeleteLauncher.launch(
                                                androidx.activity.result.IntentSenderRequest.Builder(deleteIntent.intentSender).build()
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            android.widget.Toast.makeText(context, "Error deleting: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { showTrashDeleteConfirmDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveButton(
                        onClick = { showTrashRestoreConfirmDialog = true },
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                            contentDescription = "Restore",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Restore")
                    }

                    ExpressiveButton(
                        onClick = { showTrashDeleteConfirmDialog = true },
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                            contentDescription = "Delete Permanently",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
            } else {
                val isCreateEnabled = selectedUris.size in 1..8
                val isStitchEnabled = selectedUris.size in 2..10
                var createMenuExpanded by remember { mutableStateOf(false) }
                var moreMenuExpanded by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(28.dp),
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
                        IconButton(onClick = {
                            if (selectedUris.isNotEmpty()) {
                                coroutineScope.launch {
                                    val strip = viewModel.settingsRepository.stripMetadataOnShareFlow.first()
                                    com.inferno.gallery.ui.utils.ShareUtils.shareMedia(context, selectedUris.map { android.net.Uri.parse(it) }, strip)
                                }
                            }
                        }) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_share), contentDescription = "Share", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                        }

                        // Copy
                        IconButton(onClick = { showCopySheet = true }) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_content_copy), contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                        }

                        // Move
                        IconButton(onClick = { showMoveSheet = true }) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_folder), contentDescription = "Move", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                        }

                        // Create (Collage/Stitch)
                        Box {
                            IconButton(onClick = { createMenuExpanded = true }) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_add), contentDescription = "Create", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                            }
                            DropdownMenu(
                                expanded = createMenuExpanded,
                                onDismissRequest = { createMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Collage") },
                                    leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_grid_view), contentDescription = null) },
                                    onClick = {
                                        createMenuExpanded = false
                                        if (isCreateEnabled) {
                                            onCreateCollage(selectedUris.toList())
                                        } else {
                                            android.widget.Toast.makeText(context, "Collage supports up to 8 images", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Stitch") },
                                    leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_sort), contentDescription = null) },
                                    onClick = {
                                        createMenuExpanded = false
                                        if (isStitchEnabled) {
                                            onCreateStitch(selectedUris.toList())
                                        } else {
                                            android.widget.Toast.makeText(context, "Select between 2 and 10 images to stitch", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }

                        // Delete
                        IconButton(onClick = {
                            if (confirmDeleteEnabled) {
                                showMultiDeleteConfirmDialog = true
                            } else {
                                val uris = selectedUris.map { Uri.parse(it) }
                                if (uris.isNotEmpty()) {
                                    try {
                                        val trashIntent = android.provider.MediaStore.createTrashRequest(context.contentResolver, uris, true)
                                        trashLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(trashIntent.intentSender).build())
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_delete), contentDescription = "Delete", tint = com.inferno.gallery.ui.theme.LocalHarmonizedColors.current.error, modifier = Modifier.size(22.dp))
                        }

                        // More options
                        Box {
                            IconButton(onClick = { moreMenuExpanded = true }) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_more_vert), contentDescription = "More", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                            }
                            DropdownMenu(
                                expanded = moreMenuExpanded,
                                onDismissRequest = { moreMenuExpanded = false }
                            ) {
                                // Hide (Private Space)
                                DropdownMenuItem(
                                    text = { Text("Hide") },
                                    leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_visibility_off), contentDescription = null) },
                                    onClick = {
                                        moreMenuExpanded = false
                                        val activity = context as? androidx.fragment.app.FragmentActivity
                                        if (activity != null) {
                                            viewModel.vaultAuthManager.authenticate(
                                                activity = activity,
                                                onSuccess = {
                                                    val uris = selectedUris.mapNotNull { android.net.Uri.parse(it) }
                                                    viewModel.hideMedia(uris)
                                                    viewModel.clearSelection()
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

    if (showCreateAlbumDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showCreateAlbumDialog = false
                newAlbumName = ""
            }
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Create new album",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                    
                    androidx.compose.material3.OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        label = { Text("Album name") },
                        placeholder = { Text("Enter album name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showCreateAlbumDialog = false
                                newAlbumName = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ExpressiveButton(
                            onClick = {
                                if (newAlbumName.isNotBlank()) {
                                    val albumToCreate = newAlbumName.trim()
                                    viewModel.createAlbum(
                                        albumName = albumToCreate,
                                        onSuccess = {
                                            showCreateAlbumDialog = false
                                            newAlbumName = ""
                                            android.widget.Toast.makeText(context, "Album '$albumToCreate' created successfully", android.widget.Toast.LENGTH_SHORT).show()
                                            triggerSync()
                                        },
                                        onError = { errorMsg ->
                                            android.widget.Toast.makeText(context, "Error: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            enabled = newAlbumName.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

}
}
