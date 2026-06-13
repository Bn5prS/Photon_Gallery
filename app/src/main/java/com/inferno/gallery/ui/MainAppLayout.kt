package com.inferno.gallery.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.CreateNewFolder

import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LibraryAddCheck
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.LaunchedEffect
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

private fun hasStoragePermissions(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (Environment.isExternalStorageManager()) {
            return true
        }
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val readImages = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
        val readVideos = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VIDEO
        ) == PackageManager.PERMISSION_GRANTED
        readImages && readVideos
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PermissionOnboardingScreen(
    photosGranted: Boolean,
    videosGranted: Boolean,
    allFilesGranted: Boolean,
    onGrantMediaClick: () -> Unit,
    onGrantAllFilesClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) {
        listOf(
            MaterialTheme.colorScheme.surfaceContainerLowest,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.surfaceContainerLowest,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.surfaceContainer
        )
    }
    val backgroundBrush = Brush.verticalGradient(colors)

    val allGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        photosGranted && videosGranted && allFilesGranted
    } else {
        photosGranted && allFilesGranted
    }

    var animateIcon by remember { mutableStateOf(false) }
    LaunchedEffect(allGranted) {
        if (allGranted) {
            while (true) {
                animateIcon = !animateIcon
                kotlinx.coroutines.delay(2000)
            }
        }
    }
    val iconScale by animateFloatAsState(
        targetValue = if (allGranted && animateIcon) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            ),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                        .background(
                            color = if (allGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (allGranted) Icons.Outlined.Check else Icons.Outlined.Image,
                        contentDescription = null,
                        tint = if (allGranted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Storage Access Required",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Please grant the following permissions to enable local media organization and AI indexing features.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PermissionRowItem(
                        title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "Photos Access" else "Storage Access",
                        subtitle = "Required to display images",
                        isGranted = photosGranted,
                        icon = Icons.Outlined.Image,
                        onClick = onGrantMediaClick
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRowItem(
                            title = "Videos Access",
                            subtitle = "Required to index and play video files",
                            isGranted = videosGranted,
                            icon = Icons.Outlined.PhotoAlbum,
                            onClick = onGrantMediaClick
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        PermissionRowItem(
                            title = "All Files Access",
                            subtitle = "Required to organize directories privately",
                            isGranted = allFilesGranted,
                            icon = Icons.Outlined.Folder,
                            onClick = onGrantAllFilesClick
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onContinueClick,
                    enabled = allGranted,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (allGranted) "Continue to Gallery" else "Grant All Permissions to Continue",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRowItem(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isGranted) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (isGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Granted", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.height(28.dp).clickable { onClick() }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Grant", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainAppLayout(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?, String?) -> Unit,
    onCreateCollage: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel()
) {
    val selectedFilter by viewModel.selectedFilterIndex.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val albumSortOrder by viewModel.albumSortOrder.collectAsState()
    val dockStyle by viewModel.dockStyle.collectAsState()
    val gridAutoPlay by viewModel.gridAutoPlay.collectAsState()
    val nestedNavController = rememberNavController()
    var showMenu by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isScrollDockVisible by viewModel.isScrollDockVisible.collectAsState()

    LaunchedEffect(currentRoute) {
        viewModel.setScrollDockVisible(true)
    }

    val albumNameArg = navBackStackEntry?.arguments?.getString("bucketName")
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    fun checkPhotosPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkVideosPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
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

    val hasRequiredPermissions = photosGranted && videosGranted
    if (!onboardingCompleted && !hasRequiredPermissions) {
        PermissionOnboardingScreen(
            photosGranted = photosGranted,
            videosGranted = videosGranted,
            allFilesGranted = allFilesGranted,
            onGrantMediaClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    Scaffold(
        modifier = modifier.fillMaxSize().overscrollStretch(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            if (currentRoute != "settings") {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .statusBarsPadding()
                ) {
                    if (isSelectionMode) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.clearSelection() }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear selection")
                                }
                                Text(
                                    "${selectedUris.size} Selected",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(start = 16.dp).weight(1f)
                                )
                                IconButton(onClick = { viewModel.toggleSelectAll() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.LibraryAddCheck,
                                        contentDescription = "Select or Deselect All"
                                    )
                                }
                            }
                        }
                    } else if (currentRoute == "album/{bucketName}") {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { nestedNavController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                                }
                                Text(
                                    albumNameArg ?: "Album",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(start = 16.dp).weight(1f)
                                )
                            }
                        }
                    } else {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val titleText = when (currentRoute) {
                                    "photos" -> "Photos"
                                    "albums" -> "Albums"
                                    "search" -> "Search"
                                    "cloud" -> "Cloud"
                                    else -> "Photon Gallery"
                                }
                                Text(
                                    titleText,
                                    style = MaterialTheme.typography.displayMedium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentRoute == "albums") {
                                        IconButton(onClick = { showCreateAlbumDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Outlined.CreateNewFolder,
                                                contentDescription = "Create Album",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Box {
                                        IconButton(onClick = { showMenu = true }) {
                                            Icon(Icons.Outlined.MoreVert, contentDescription = "Menu")
                                        }
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false },
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            if (currentRoute == "photos") {
                                                DropdownMenuItem(
                                                    text = { Text("Immersive View") },
                                                    trailingIcon = {
                                                        androidx.compose.material3.RadioButton(
                                                            selected = viewMode == ViewMode.Immersive,
                                                            onClick = null
                                                        )
                                                    },
                                                    onClick = {
                                                        viewModel.setViewMode(ViewMode.Immersive)
                                                        showMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Grouped View") },
                                                    trailingIcon = {
                                                        androidx.compose.material3.RadioButton(
                                                            selected = viewMode == ViewMode.Grouped,
                                                            onClick = null
                                                        )
                                                    },
                                                    onClick = {
                                                        viewModel.setViewMode(ViewMode.Grouped)
                                                        showMenu = false
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Settings") },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Settings,
                                                        contentDescription = "Settings"
                                                    )
                                                },
                                                onClick = {
                                                    nestedNavController.navigate("settings") {
                                                        popUpTo("photos") { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                    showMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (currentRoute == "photos" && !isSelectionMode) {
                        QuickFilterRow(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { viewModel.setFilter(it) },
                            sortOrder = sortOrder,
                            onSortOrderSelected = { viewModel.setSortOrder(it) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            val isDockVisible = currentRoute != "settings" && !isSelectionMode && (dockStyle != DockStyle.PILL || isScrollDockVisible)
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
                            .padding(bottom = 4.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        HorizontalFloatingToolbar(
                            expanded = true,
                            modifier = Modifier.fillMaxWidth(0.765f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 13.dp, vertical = 0.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DockItem(
                                    icon = { Icon(Icons.Outlined.Image, contentDescription = "Photos", modifier = Modifier.size(19.dp)) },
                                    label = "Photos",
                                    isSelected = currentRoute == "photos",
                                    onClick = { nestedNavController.navigate("photos") {
                                        popUpTo("photos") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    } }
                                )
                                DockItem(
                                    icon = { Icon(Icons.Outlined.PhotoAlbum, contentDescription = "Albums", modifier = Modifier.size(19.dp)) },
                                    label = "Albums",
                                    isSelected = currentRoute?.startsWith("album") == true,
                                    onClick = { nestedNavController.navigate("albums") {
                                        popUpTo("photos") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    } }
                                )
                                DockItem(
                                    icon = { MagicSearchIcon(modifier = Modifier.size(19.dp)) },
                                    label = "Search",
                                    isSelected = currentRoute == "search",
                                    onClick = { nestedNavController.navigate("search") {
                                        popUpTo("photos") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    } }
                                )
                                DockItem(
                                    icon = { Icon(Icons.Outlined.CloudUpload, contentDescription = "Cloud", modifier = Modifier.size(19.dp)) },
                                    label = "Cloud",
                                    isSelected = currentRoute == "cloud",
                                    onClick = { nestedNavController.navigate("cloud") {
                                        popUpTo("photos") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    } }
                                )
                            }
                        }
                    }
                } else {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .navigationBarsPadding()
                    ) {
                        NavigationBar(
                            modifier = Modifier.height(60.dp),
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Outlined.Image, contentDescription = "Photos") },
                                label = { Text("Photos") },
                                selected = currentRoute == "photos",
                                onClick = { nestedNavController.navigate("photos") {
                                    popUpTo("photos") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                } }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Outlined.PhotoAlbum, contentDescription = "Albums") },
                                label = { Text("Albums") },
                                selected = currentRoute?.startsWith("album") == true,
                                onClick = { nestedNavController.navigate("albums") {
                                    popUpTo("photos") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                } }
                            )
                            NavigationBarItem(
                                icon = { MagicSearchIcon(modifier = Modifier.size(24.dp)) },
                                label = { Text("Search") },
                                selected = currentRoute == "search",
                                onClick = { nestedNavController.navigate("search") {
                                    popUpTo("photos") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                } }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Outlined.CloudUpload, contentDescription = "Cloud") },
                                label = { Text("Cloud") },
                                selected = currentRoute == "cloud",
                                onClick = { nestedNavController.navigate("cloud") {
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
                    isMainTab = true
                )
            }
            composable(
                route = "albums",
                enterTransition = {
                    if (initialState.destination.route == "album/{bucketName}") {
                        slideInHorizontally(
                            initialOffsetX = { -it / 3 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
                        ) + fadeIn(spring(stiffness = Spring.StiffnessHigh))
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                exitTransition = {
                    if (targetState.destination.route == "album/{bucketName}") {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
                        ) + fadeOut(spring(stiffness = Spring.StiffnessHigh))
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route == "album/{bucketName}") {
                        slideInHorizontally(
                            initialOffsetX = { -it / 3 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
                        ) + fadeIn(spring(stiffness = Spring.StiffnessHigh))
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popExitTransition = {
                    if (targetState.destination.route == "album/{bucketName}") {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
                        ) + fadeOut(spring(stiffness = Spring.StiffnessHigh))
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                }
            ) {
                AlbumsScreen(
                    viewModel = viewModel,
                    contentPadding = innerPadding,
                    onAlbumClick = { bucketName -> 
                        nestedNavController.navigate("album/$bucketName")
                    }
                )
            }
            composable(
                route = "album/{bucketName}",
                enterTransition = {
                    if (initialState.destination.route == "albums") {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
                        ) + fadeIn(spring(stiffness = Spring.StiffnessHigh))
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                exitTransition = {
                    if (targetState.destination.route == "albums") {
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
                        ) + fadeOut(spring(stiffness = Spring.StiffnessHigh))
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route == "albums") {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
                        ) + fadeIn(spring(stiffness = Spring.StiffnessHigh))
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popExitTransition = {
                    if (targetState.destination.route == "albums") {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
                        ) + fadeOut(spring(stiffness = Spring.StiffnessHigh))
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                }
            ) { backStackEntry ->
                val bucketName = backStackEntry.arguments?.getString("bucketName")
                GalleryScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onPhotoClick = onPhotoClick,
                    viewModel = viewModel,
                    contentPadding = innerPadding,
                    bucketName = bucketName
                )
            }
            composable("search") {
                SearchScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onPhotoClick = onPhotoClick,
                    viewModel = viewModel,
                    contentPadding = innerPadding
                )
            }
            composable("settings") {
                SettingsScreen(
                    contentPadding = innerPadding,
                    galleryViewModel = viewModel,
                    onBackClick = { nestedNavController.popBackStack() }
                )
            }
            composable("cloud") {
                CloudScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onPhotoClick = onPhotoClick,
                    viewModel = viewModel,
                    contentPadding = innerPadding,
                    onNavigateToSettings = { nestedNavController.navigate("settings") }
                )
            }
        }
        
        // Selection Mode SplitButton Overlay
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
            ) {
            var expanded by remember { mutableStateOf(false) }
            var showMoveSheet by remember { mutableStateOf(false) }
            var showCopySheet by remember { mutableStateOf(false) }
            var showSmartDeleteDialog by remember { mutableStateOf(false) }
            var showCloudDeleteDialog by remember { mutableStateOf(false) }
            var showMultiDeleteConfirmDialog by remember { mutableStateOf(false) }
            val settingsRepo = remember { SettingsRepository(context) }
            val confirmDeleteEnabled by settingsRepo.confirmDeleteEnabledFlow.collectAsState(initial = true)
            
            val cloudMediaIds by viewModel.cloudMediaIds.collectAsState()
            val selectedIds = remember(selectedUris) {
                selectedUris.mapNotNull { Uri.parse(it).lastPathSegment }
            }
            val hasUnbackedUp = remember(selectedIds, cloudMediaIds) {
                selectedIds.isEmpty() || selectedIds.any { id -> !cloudMediaIds.contains(id) }
            }
            val hasBackedUp = remember(selectedIds, cloudMediaIds) {
                selectedIds.isNotEmpty() && selectedIds.any { id -> cloudMediaIds.contains(id) }
            }
            
            if (showMoveSheet) {
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { showMoveSheet = false },
                ) {
                    val albums by viewModel.allAlbums.collectAsState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Move to Album", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(albums.size) { index ->
                                val album = albums[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showMoveSheet = false
                                            viewModel.moveSelectedMedia(album.bucketName)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.PhotoAlbum, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(album.bucketName, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
            
            if (showCopySheet) {
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { showCopySheet = false },
                ) {
                    val albums by viewModel.allAlbums.collectAsState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Copy to Album", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(albums.size) { index ->
                                val album = albums[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showCopySheet = false
                                            viewModel.copySelectedMedia(album.bucketName)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.PhotoAlbum, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(album.bucketName, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }

            if (showSmartDeleteDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showSmartDeleteDialog = false },
                    title = { Text("Delete Items") },
                    text = { Text("Some of the selected items are backed up to the cloud. Do you want to delete them everywhere (device and cloud) or from this device only?") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showSmartDeleteDialog = false
                                // Delete everywhere (device & cloud)
                                viewModel.deleteCloudBackupsByUris(selectedUris)
                                
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
                                    }
                                }
                            }
                        ) {
                            Text("Delete Everywhere", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        Row {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showSmartDeleteDialog = false
                                    // Delete from device only
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
                                        }
                                    }
                                }
                            ) {
                                Text("Device Only")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.TextButton(
                                onClick = { showSmartDeleteDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                )
            }

            if (showCloudDeleteDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showCloudDeleteDialog = false },
                    title = { Text("Delete Backup from Cloud") },
                    text = { Text("Are you sure you want to delete the cloud backup for the selected item(s)? The files on your device will NOT be deleted, but the backups on Telegram will be permanently removed.") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showCloudDeleteDialog = false
                                viewModel.deleteCloudBackupsByUris(selectedUris)
                                viewModel.clearSelection()
                            }
                        ) {
                            Text("Delete Backup", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showCloudDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
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
                        },
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Restore",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Restore", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
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
                        },
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Permanently",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Delete", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isCreateEnabled = selectedUris.size in 1..8
                    Button(
                        onClick = { 
                            if (isCreateEnabled) {
                                onCreateCollage(selectedUris.toList())
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Collage creation supports up to 8 images",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCreateEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isCreateEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = "Create Collage",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Create", fontWeight = FontWeight.SemiBold)
                    }
                    Box {
                    CustomSplitButton(
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        leadingText = "Share",
                        onLeadingClick = {
                            if (selectedUris.isNotEmpty()) {
                                coroutineScope.launch {
                                    val stripMetadata = withContext(Dispatchers.IO) {
                                        SettingsRepository(context).stripMetadataOnShareFlow.first()
                                    }
                                    val urisToShare: List<Uri> = if (stripMetadata) {
                                        withContext(Dispatchers.IO) {
                                            val shareDir = File(context.cacheDir, "shared_images").also { it.mkdirs() }
                                            selectedUris.toList().mapIndexed { idx, uriStr ->
                                                val uri = Uri.parse(uriStr)
                                                try {
                                                    val extension = context.contentResolver.getType(uri)?.substringAfter("/") ?: "jpg"
                                                    val tempFile = File(shareDir, "share_${System.currentTimeMillis()}_$idx.$extension")
                                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                                        tempFile.outputStream().use { output -> input.copyTo(output) }
                                                    }
                                                    val exif = androidx.exifinterface.media.ExifInterface(tempFile.absolutePath)
                                                    val piiTags = listOf(
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_TIMESTAMP,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_DATESTAMP,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_PROCESSING_METHOD,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_BEARING,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_DISTANCE,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_SPEED,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_TRACK,
                                                        androidx.exifinterface.media.ExifInterface.TAG_GPS_IMG_DIRECTION,
                                                        androidx.exifinterface.media.ExifInterface.TAG_MAKE,
                                                        androidx.exifinterface.media.ExifInterface.TAG_MODEL,
                                                        androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE,
                                                        androidx.exifinterface.media.ExifInterface.TAG_ARTIST,
                                                        androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT,
                                                        androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT,
                                                        androidx.exifinterface.media.ExifInterface.TAG_DATETIME,
                                                        androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL,
                                                        androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED,
                                                        androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME,
                                                        androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_ORIGINAL
                                                    )
                                                    piiTags.forEach { tag -> exif.setAttribute(tag, null) }
                                                    exif.saveAttributes()
                                                    FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        tempFile
                                                    )
                                                } catch (e: Exception) {
                                                    android.util.Log.e("MainAppLayout", "Failed to strip metadata for $uriStr", e)
                                                    uri  // Fallback to original
                                                }
                                            }
                                        }
                                    } else {
                                        selectedUris.map { Uri.parse(it) }
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "*/*"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(urisToShare))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Selected Media"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        onTrailingClick = { expanded = true }
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {

                        DropdownMenuItem(
                            text = { Text("Move") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.DriveFileMove, contentDescription = null) },
                            onClick = { 
                                expanded = false
                                showMoveSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy to Album") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = { 
                                expanded = false
                                showCopySheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                expanded = false
                                if (hasBackedUp) {
                                    showSmartDeleteDialog = true
                                } else {
                                    if (confirmDeleteEnabled) {
                                        showMultiDeleteConfirmDialog = true
                                    } else {
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
                                }
                            }
                        )
                        if (hasUnbackedUp) {
                            DropdownMenuItem(
                                text = { Text("Backup to Cloud") },
                                leadingIcon = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) },
                                onClick = { 
                                    expanded = false
                                    viewModel.backupSelectedMedia()
                                }
                            )
                        }
                        if (hasBackedUp) {
                            DropdownMenuItem(
                                text = { Text("Delete from Cloud") },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    expanded = false
                                    showCloudDeleteDialog = true
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
                        Button(
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


@Composable
fun CustomSplitButton(
    leadingIcon: @Composable () -> Unit,
    leadingText: String,
    onLeadingClick: () -> Unit,
    onTrailingClick: () -> Unit,
    trailingIcon: @Composable () -> Unit = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(56.dp)
        ) {
            // Leading
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onLeadingClick() }
                    .padding(start = 20.dp, end = 16.dp)
                    .fillMaxHeight()
            ) {
                leadingIcon()
                Spacer(Modifier.width(12.dp))
                Text(leadingText, fontWeight = FontWeight.SemiBold)
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            
            // Trailing
            Box(
                modifier = Modifier
                    .clickable { onTrailingClick() }
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                trailingIcon()
            }
        }
    }
}

@Composable
private fun QuickFilterRow(
    selectedFilter: Int,
    onFilterSelected: (Int) -> Unit,
    sortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CustomFilterChip(
                    text = "All",
                    icon = Icons.Outlined.Image,
                    selected = selectedFilter == 0,
                    onClick = { onFilterSelected(0) }
                )
            }
            item {
                CustomFilterChip(
                    text = "Camera",
                    icon = Icons.Outlined.CameraAlt,
                    selected = selectedFilter == 1,
                    onClick = { onFilterSelected(1) }
                )
            }
        }
        
        Box {
            androidx.compose.material3.IconButton(
                onClick = { showSortMenu = true }
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                DropdownMenuItem(
                    text = { Text("Newest to Oldest") },
                    trailingIcon = {
                        androidx.compose.material3.RadioButton(
                            selected = sortOrder == SortOrder.NewToOld,
                            onClick = null
                        )
                    },
                    onClick = { onSortOrderSelected(SortOrder.NewToOld); showSortMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Oldest to Newest") },
                    trailingIcon = {
                        androidx.compose.material3.RadioButton(
                            selected = sortOrder == SortOrder.OldToNew,
                            onClick = null
                        )
                    },
                    onClick = { onSortOrderSelected(SortOrder.OldToNew); showSortMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Largest to Smallest") },
                    trailingIcon = {
                        androidx.compose.material3.RadioButton(
                            selected = sortOrder == SortOrder.BigToSmall,
                            onClick = null
                        )
                    },
                    onClick = { onSortOrderSelected(SortOrder.BigToSmall); showSortMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Smallest to Largest") },
                    trailingIcon = {
                        androidx.compose.material3.RadioButton(
                            selected = sortOrder == SortOrder.SmallToBig,
                            onClick = null
                        )
                    },
                    onClick = { onSortOrderSelected(SortOrder.SmallToBig); showSortMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("A to Z") },
                    trailingIcon = {
                        androidx.compose.material3.RadioButton(
                            selected = sortOrder == SortOrder.NameAsc,
                            onClick = null
                        )
                    },
                    onClick = { onSortOrderSelected(SortOrder.NameAsc); showSortMenu = false }
                )
            }
        }
    }
}


@Composable
private fun CustomFilterChip(
    text: String,
    icon: ImageVector? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .height(24.dp)
            .clip(CircleShape)
            .expressiveClick(onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun DockItem(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val touchScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "dockItemScale"
    )

    val pillColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pillColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "contentColor"
    )

    Box(
        modifier = Modifier
            .scale(touchScale)
            .clip(CircleShape)
            .background(pillColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                icon()
                
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Row {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}


private fun getTabRouteIndex(route: String?): Int {
    return when (route) {
        "photos" -> 0
        "albums", "album/{bucketName}" -> 1
        "search" -> 2
        "settings" -> 3
        else -> 0
    }
}

private fun getEnterTransition(initialRoute: String?, targetRoute: String?): androidx.compose.animation.EnterTransition {
    return androidx.compose.animation.fadeIn(
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
    )
}

private fun getExitTransition(initialRoute: String?, targetRoute: String?): androidx.compose.animation.ExitTransition {
    return androidx.compose.animation.fadeOut(
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
    )
}



