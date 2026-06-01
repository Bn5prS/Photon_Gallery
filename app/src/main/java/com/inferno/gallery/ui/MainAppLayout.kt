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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close

import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.GridView
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.os.Environment
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.ui.text.style.TextAlign

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainAppLayout(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel()
) {
    val selectedFilter by viewModel.selectedFilterIndex.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val dockStyle by viewModel.dockStyle.collectAsState()
    val gridAutoPlay by viewModel.gridAutoPlay.collectAsState()
    val nestedNavController = rememberNavController()
    var showMenu by remember { mutableStateOf(false) }
    val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val albumNameArg = navBackStackEntry?.arguments?.getString("bucketName")
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var hasPermission by remember { mutableStateOf(Environment.isExternalStorageManager()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = Environment.isExternalStorageManager()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val context = LocalContext.current
    val intentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        hasPermission = Environment.isExternalStorageManager()
    }

    if (!hasPermission) {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("All Files Access Required", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Photon Gallery requires access to organize media locally and privately.",
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    intentLauncher.launch(intent)
                }) {
                    Text("Grant Permission")
                }
            }
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedUris.size} Selected", style = MaterialTheme.typography.titleLarge) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear selection")
                            }
                        },
                        actions = {
                            // SplitButton handles actions now
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                } else if (currentRoute == "album/{bucketName}") {
                    TopAppBar(
                        title = { Text(albumNameArg ?: "Album", style = MaterialTheme.typography.titleLarge) },
                        navigationIcon = {
                            IconButton(onClick = { nestedNavController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                } else {
                    LargeTopAppBar(
                        title = {
                            val titleText = when (currentRoute) {
                                "photos" -> "Photos"
                                "albums" -> "Albums"
                                "search" -> "Search"
                                else -> "Photon Gallery"
                            }
                            Text(
                                titleText,
                                style = MaterialTheme.typography.displayMedium
                            )
                        },
                        actions = {
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
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.background
                        ),
                        scrollBehavior = scrollBehavior
                    )
                    if (currentRoute == "photos") {
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
            val isDockVisible = !isSelectionMode && scrollBehavior.state.collapsedFraction < 0.7f
            AnimatedVisibility(
                visible = isDockVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                if (dockStyle == DockStyle.PILL) {
                    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = systemNavBarInset + 4.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        HorizontalFloatingToolbar(
                            expanded = scrollBehavior.state.collapsedFraction < 0.3f,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DockItem(
                                    icon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Photos") },
                                    label = "Photos",
                                    isSelected = currentRoute == "photos",
                                    onClick = { nestedNavController.navigate("photos") {
                                        popUpTo("photos") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    } }
                                )
                                DockItem(
                                    icon = { Icon(Icons.Outlined.PhotoAlbum, contentDescription = "Albums") },
                                    label = "Albums",
                                    isSelected = currentRoute?.startsWith("album") == true,
                                    onClick = { nestedNavController.navigate("albums") {
                                        popUpTo("photos") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    } }
                                )
                                DockItem(
                                    icon = { MagicSearchIcon(modifier = Modifier.size(24.dp)) },
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
        }
    ) { innerPadding ->
        NavHost(
            navController = nestedNavController,
            startDestination = "photos"
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
            composable("albums") {
                AlbumsScreen(
                    viewModel = viewModel,
                    contentPadding = innerPadding,
                    onAlbumClick = { bucketName -> 
                        nestedNavController.navigate("album/$bucketName")
                    }
                )
            }
            composable("album/{bucketName}") { backStackEntry ->
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
            Box {
                CustomSplitButton(
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    leadingText = "Share",
                    onLeadingClick = { /* TODO: Share */ },
                    onTrailingClick = { expanded = true }
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {

                    DropdownMenuItem(
                        text = { Text("Move") },
                        leadingIcon = { Icon(Icons.Outlined.DriveFileMove, contentDescription = null) },
                        onClick = { 
                            expanded = false
                            // TODO: Move
                        }
                    )
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
                    .background(contentColor.copy(alpha = 0.2f))
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
                    icon = Icons.Outlined.PhotoLibrary,
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
            item {
                CustomFilterChip(
                    text = "Screenshots",
                    icon = Icons.Outlined.PhotoAlbum,
                    selected = selectedFilter == 2,
                    onClick = { onFilterSelected(2) }
                )
            }
        }
        
        Box {
            androidx.compose.material3.IconButton(
                onClick = { showSortMenu = true },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, androidx.compose.foundation.shape.CircleShape)
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
    val pillWidth by animateDpAsState(
        targetValue = if (isSelected) 64.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pillWidth"
    )
    val pillColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pillColor"
    )
    val iconTint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .width(pillWidth)
                .clip(CircleShape)
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides iconTint) {
                icon()
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall,
            color = iconTint,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

