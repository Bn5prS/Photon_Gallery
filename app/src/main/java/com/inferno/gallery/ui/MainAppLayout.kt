package com.inferno.gallery.ui
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ButtonDefaults
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.components.DisplayAndSortBottomSheet
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.utils.popBackStackSafely

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.core.animate
import kotlinx.coroutines.Job
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.inferno.gallery.data.AlbumLayoutMode
import com.inferno.gallery.data.DockStyle
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import com.inferno.gallery.ui.theme.EditorialPunchyHeadline
import com.inferno.gallery.ui.theme.mainScreenTitleStyle

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
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.components.ExpressiveFilledIconButton
import com.inferno.gallery.ui.components.PhotonHeaderIconButton
import com.inferno.gallery.ui.components.PhotonBackButton
import com.inferno.gallery.ui.components.PhotonCloseButton
import com.inferno.gallery.ui.components.PhotonOverflowMenuButton
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
import androidx.compose.ui.res.stringResource
import com.inferno.gallery.R


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainAppLayout(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?, String?) -> Unit,
    onCreateCollage: (List<String>) -> Unit = {},
    onCreateStitch: (List<String>) -> Unit = {},
    onOpenEditor: (String) -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel()
) {
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = LocalContext.current as androidx.lifecycle.ViewModelStoreOwner
    )
    val peopleViewModel: com.inferno.gallery.ui.people.PeopleViewModel = run {
        var cur = LocalContext.current
        while (cur is android.content.ContextWrapper) {
            if (cur is androidx.lifecycle.ViewModelStoreOwner) break
            cur = cur.baseContext
        }
        androidx.lifecycle.viewmodel.compose.viewModel(
            viewModelStoreOwner = cur as androidx.lifecycle.ViewModelStoreOwner
        )
    }
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val isDragSelecting by viewModel.isDragSelecting.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val albumSortOrder by viewModel.albumSortOrder.collectAsState()
    val albumLayoutMode by viewModel.albumLayoutMode.collectAsState()
    val dockStyle by viewModel.dockStyle.collectAsState()
    val showVideosInDock by viewModel.showVideosInDock.collectAsState()
    val nestedNavController = rememberNavController()

    var showMenu by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var showDisplaySortSheet by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var settingsActiveSection by remember { mutableStateOf<String?>(null) }
    val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
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
    val onboardingV210Completed by viewModel.onboardingV210Completed.collectAsState()
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

    val triggerSync = {
        val syncWorkRequest = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.MediaSyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "MediaSyncWorker",
            androidx.work.ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }

    fun updatePermissionStates() {
        val wasMediaGranted = photosGranted
        photosGranted = checkPhotosPermission()
        videosGranted = checkVideosPermission()
        allFilesGranted = checkAllFilesPermission()
        if (!wasMediaGranted && photosGranted) {
            triggerSync()
        }
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

    var pendingTrashUris by remember { mutableStateOf<List<String>>(emptyList()) }

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selected = pendingTrashUris.ifEmpty { viewModel.selectedUris.value.toList() }
            viewModel.trashSelectedMedia(selected)
            viewModel.clearSelection()
        }
        pendingTrashUris = emptyList()
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selected = pendingTrashUris.ifEmpty { viewModel.selectedUris.value.toList() }
            viewModel.restoreSelectedMedia(selected)
            viewModel.clearSelection()
        }
        pendingTrashUris = emptyList()
    }

    val permanentDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selected = pendingTrashUris.ifEmpty { viewModel.selectedUris.value.toList() }
            viewModel.deleteSelectedMediaFromDb(selected)
            viewModel.clearSelection()
        }
        pendingTrashUris = emptyList()
    }

    val hasRequiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        photosGranted && videosGranted
    } else {
        photosGranted
    }
    
    if (onboardingV210Completed == null) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    var onboardingFinishedLocally by rememberSaveable { mutableStateOf(false) }
    val showOnboardingFlow = (onboardingV210Completed == false && !onboardingFinishedLocally)

    if (showOnboardingFlow || !hasRequiredPermissions) {
        val initialStep = if (hasRequiredPermissions && showOnboardingFlow) 1 else 0
        PermissionOnboardingScreen(
            photosGranted = photosGranted,
            videosGranted = videosGranted,
            allFilesGranted = allFilesGranted,
            initialStep = initialStep,
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
                onboardingFinishedLocally = true
                viewModel.completeOnboarding()
                triggerSync()
            },
            settingsViewModel = settingsViewModel,
            modifier = modifier
        )
        return
    }

    val isHomeScreen = currentRoute == "photos" || currentRoute == null
    val isCollapsibleTopBarActive = isHomeScreen && !isSelectionMode

    var topBarOffsetY by remember { mutableFloatStateOf(0f) }
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    var topBarAnimationJob by remember { mutableStateOf<Job?>(null) }
    var lastScrollDelta by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember(isCollapsibleTopBarActive, topBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!isCollapsibleTopBarActive || topBarHeightPx <= 0f) return Offset.Zero

                val delta = available.y

                // Only cancel animation and track offset when the user is actively dragging
                if (source == NestedScrollSource.UserInput) {
                    topBarAnimationJob?.cancel()
                    topBarAnimationJob = null
                    lastScrollDelta = delta

                    val prevOffset = topBarOffsetY
                    val newOffset = (prevOffset + delta).coerceIn(-topBarHeightPx, 0f)
                    topBarOffsetY = newOffset

                    // Consume the delta used to collapse or expand the top bar
                    val consumedY = newOffset - prevOffset
                    return Offset(0f, consumedY)
                }

                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!isCollapsibleTopBarActive || topBarHeightPx <= 0f) return Offset.Zero
                // If user drags down when at top of list or overscrolling, expand top bar
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    lastScrollDelta = available.y
                    val prevOffset = topBarOffsetY
                    val newOffset = (prevOffset + available.y).coerceIn(-topBarHeightPx, 0f)
                    topBarOffsetY = newOffset
                    return Offset(0f, newOffset - prevOffset)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!isCollapsibleTopBarActive || topBarHeightPx <= 0f) return Velocity.Zero

                val velocityY = available.y
                val targetOffset = when {
                    // Swiping down (positive velocity) -> bring top bar back!
                    velocityY > 150f -> 0f
                    // Swiping up (negative velocity) -> hide top bar!
                    velocityY < -150f -> -topBarHeightPx
                    // Moderate drag direction
                    lastScrollDelta > 8f -> 0f // User dragged down -> expand
                    lastScrollDelta < -8f -> -topBarHeightPx // User dragged up -> collapse
                    // Low velocity or micro-drag release: snap to nearest state
                    topBarOffsetY > -topBarHeightPx * 0.5f -> 0f
                    else -> -topBarHeightPx
                }

                if (topBarOffsetY != targetOffset) {
                    topBarAnimationJob?.cancel()
                    topBarAnimationJob = coroutineScope.launch {
                        animate(
                            initialValue = topBarOffsetY,
                            targetValue = targetOffset,
                            animationSpec = MotionTokens.snappySpring()
                        ) { value, _ ->
                            topBarOffsetY = value
                        }
                    }
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(isCollapsibleTopBarActive) {
        if (!isCollapsibleTopBarActive && topBarOffsetY != 0f) {
            topBarAnimationJob?.cancel()
            topBarOffsetY = 0f
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.isTopBarCollapsed.collect { collapsed ->
            if (!collapsed && topBarOffsetY != 0f) {
                topBarAnimationJob?.cancel()
                topBarAnimationJob = coroutineScope.launch {
                    animate(
                        initialValue = topBarOffsetY,
                        targetValue = 0f,
                        animationSpec = MotionTokens.snappySpring()
                    ) { value, _ ->
                        topBarOffsetY = value
                    }
                }
            }
        }
    }

    val isMainScreen = currentRoute == "photos" || currentRoute == "videos" || currentRoute == "albums" || currentRoute == "collections" || currentRoute == "search" || currentRoute == null
    val isAlbumScreen = currentRoute == "album/{bucketName}"
    val isSettingsScreen = currentRoute == "settings"

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            if (isMainScreen || isAlbumScreen || isSettingsScreen) {
                val isTopBarCollapsed by viewModel.isTopBarCollapsed.collectAsState()
                val topBarColor by animateColorAsState(
                    targetValue = when {
                        isSelectionMode -> MaterialTheme.colorScheme.surfaceContainerHigh
                        isTopBarCollapsed -> MaterialTheme.colorScheme.surfaceContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "topBarColor"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = if (isCollapsibleTopBarActive) topBarOffsetY else 0f
                            alpha = if (isCollapsibleTopBarActive && topBarHeightPx > 0f) {
                                (1f + topBarOffsetY / (topBarHeightPx * 0.7f)).coerceIn(0f, 1f)
                            } else 1f
                        }
                        .background(topBarColor)
                        .statusBarsPadding()
                        .onGloballyPositioned { coords ->
                            if (coords.size.height > 0) {
                                topBarHeightPx = coords.size.height.toFloat()
                            }
                        }
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
                                PhotonCloseButton(
                                    onClick = { viewModel.clearSelection() },
                                    contentDescription = "Clear selection"
                                )
                                val selectionCount = selectedUris.size
                                var lastCountChangeTime by remember { mutableLongStateOf(0L) }
                                var isRapidUpdate by remember { mutableStateOf(false) }

                                LaunchedEffect(selectionCount) {
                                    val now = android.os.SystemClock.uptimeMillis()
                                    isRapidUpdate = (now - lastCountChangeTime) < 180L
                                    lastCountChangeTime = now
                                }

                                Row(
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = selectionCount,
                                        transitionSpec = {
                                            if (isDragSelecting || isRapidUpdate) {
                                                EnterTransition.None togetherWith ExitTransition.None
                                            } else {
                                                val direction = if (targetState > initialState) 1 else -1
                                                (slideInVertically(animationSpec = tween(120, easing = FastOutSlowInEasing)) { (it * 0.35f).toInt() * -direction } +
                                                        fadeIn(animationSpec = tween(100))) togetherWith
                                                        (slideOutVertically(animationSpec = tween(90, easing = FastOutSlowInEasing)) { (it * 0.35f).toInt() * direction } +
                                                                fadeOut(animationSpec = tween(70)))
                                            }
                                        },
                                        label = "selectionCountNumber"
                                    ) { count ->
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = "Selected",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                PhotonHeaderIconButton(
                                    onClick = { viewModel.toggleSelectAll() }
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_select_all),
                                        contentDescription = "Select or Deselect All",
                                        modifier = Modifier.size(20.dp)
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
                                PhotonBackButton(
                                    onClick = { nestedNavController.popBackStackSafely() }
                                )
                                val friendlyTitle = when {
                                    albumNameArg == "search_text" -> "Search Results"
                                    albumNameArg == "search_smart" -> "Semantic Matches"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_RAW -> "RAW Photos"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_PANORAMAS -> "Panoramas"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_SLOW_MO -> "Slow Motion"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_ANIMATIONS -> "GIFs & Animations"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_NIGHT -> "Night Shots"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_PHOTO_SPHERE -> "Photo Spheres"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_PORTRAIT -> "Portraits"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_MACRO -> "Macro Shots"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.MEDIA_TYPE_TIMELAPSE -> "Time-Lapses"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.DOC_BOOKS_MAGAZINES -> "Books & Magazines"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.DOC_EVENTS -> "Events"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.DOC_IDS -> "IDs"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.DOC_NOTES -> "Notes"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.DOC_PAYMENTS -> "Payments / Methods"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.DOC_RECEIPTS -> "Receipts"
                                    albumNameArg == com.inferno.gallery.data.BucketNames.DOC_SOCIAL -> "Social"
                                    albumNameArg?.startsWith("place:") == true -> albumNameArg.removePrefix("place:")
                                    else -> albumNameArg ?: "Album"
                                }
                                Text(
                                    friendlyTitle,
                                    style = MaterialTheme.typography.headlineMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .padding(start = 16.dp, end = 16.dp)
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
                                PhotonBackButton(
                                    onClick = {
                                        if (settingsActiveSection != null) {
                                            settingsActiveSection = null
                                        } else {
                                            nestedNavController.popBackStackSafely()
                                        }
                                    }
                                )
                                val settingsHeaderTitle = when (settingsActiveSection) {
                                    "Look & Feel" -> stringResource(R.string.category_theme_display)
                                    "Layout & Navigation" -> stringResource(R.string.category_layout_navigation)
                                    "General" -> stringResource(R.string.category_general)
                                    "Smart Search & OCR" -> stringResource(R.string.smart_search)
                                    "Privacy & Security" -> stringResource(R.string.category_security_privacy)
                                    "About" -> stringResource(R.string.category_about)
                                    null -> stringResource(R.string.settings_title)
                                    else -> settingsActiveSection ?: stringResource(R.string.settings_title)
                                }
                                Text(
                                    settingsHeaderTitle,
                                    style = MaterialTheme.typography.headlineMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .weight(1f)
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = topBarColor,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val titleText = when (currentRoute) {
                                    "photos" -> stringResource(R.string.nav_photos)
                                    "videos" -> stringResource(R.string.album_videos)
                                    "albums" -> stringResource(R.string.nav_albums)
                                    "collections" -> stringResource(R.string.nav_collections)
                                    "search" -> stringResource(R.string.nav_search)
                                    else -> stringResource(R.string.app_name)
                                }
                                Text(
                                    titleText,
                                    style = mainScreenTitleStyle(),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    if (currentRoute == "albums") {
                                        PhotonHeaderIconButton(
                                            onClick = { showCreateAlbumDialog = true }
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_create_new_folder),
                                                contentDescription = "Create Album",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    if (currentRoute == "photos" || currentRoute == "videos" || currentRoute == "albums" || currentRoute == null) {
                                        PhotonHeaderIconButton(
                                            onClick = { showDisplaySortSheet = true }
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_sort),
                                                contentDescription = stringResource(R.string.menu_view_mode) + " & " + stringResource(R.string.menu_sort_order),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    if (currentRoute == "photos" || currentRoute == "videos" || currentRoute == "albums" || currentRoute == "collections" || currentRoute == null) {
                                        PhotonHeaderIconButton(
                                            onClick = {
                                                nestedNavController.navigate("settings") {
                                                    popUpTo("photos") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_settings),
                                                contentDescription = stringResource(R.string.settings_title),
                                                modifier = Modifier.size(20.dp)
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
            val isDockVisible = isMainScreen && !isSelectionMode
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
                            .padding(bottom = 14.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        FloatingNavigationPill(
                            currentRoute = currentRoute,
                            showVideosTab = showVideosInDock,
                            onNavigateToPhotos = {
                                if (nestedNavController.currentDestination?.route == "photos") {
                                    viewModel.triggerPhotosScrollToTop()
                                } else {
                                    nestedNavController.navigate("photos") {
                                        popUpTo(nestedNavController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            onNavigateToVideos = {
                                if (nestedNavController.currentDestination?.route == "videos") {
                                    viewModel.triggerVideosScrollToTop()
                                } else {
                                    nestedNavController.navigate("videos") {
                                        popUpTo(nestedNavController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            onNavigateToAlbums = {
                                if (nestedNavController.currentDestination?.route == "albums") {
                                    viewModel.triggerAlbumsScrollToTop()
                                } else {
                                    nestedNavController.navigate("albums") {
                                        popUpTo(nestedNavController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            onNavigateToCollections = {
                                if (nestedNavController.currentDestination?.route == "collections") {
                                    viewModel.triggerCollectionsScrollToTop()
                                } else {
                                    nestedNavController.navigate("collections") {
                                        popUpTo(nestedNavController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            onNavigateToSearch = {
                                nestedNavController.navigate("search") {
                                    popUpTo(nestedNavController.graph.findStartDestination().id) { saveState = true }
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
                                .height(56.dp)
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DockItem(
                                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_image), contentDescription = stringResource(R.string.nav_photos), modifier = Modifier.size(24.dp)) },
                                label = stringResource(R.string.nav_photos),
                                isSelected = currentRoute == "photos",
                                onClick = { 
                                    if (nestedNavController.currentDestination?.route == "photos") {
                                        viewModel.triggerPhotosScrollToTop()
                                    } else {
                                        nestedNavController.navigate("photos") {
                                            popUpTo(nestedNavController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            if (showVideosInDock) {
                                DockItem(
                                    icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_play_arrow), contentDescription = stringResource(R.string.album_videos), modifier = Modifier.size(24.dp)) },
                                    label = stringResource(R.string.album_videos),
                                    isSelected = currentRoute == "videos",
                                    onClick = {
                                        if (nestedNavController.currentDestination?.route == "videos") {
                                            viewModel.triggerVideosScrollToTop()
                                        } else {
                                            nestedNavController.navigate("videos") {
                                                popUpTo(nestedNavController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                            DockItem(
                                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_photo_album), contentDescription = stringResource(R.string.nav_albums), modifier = Modifier.size(24.dp)) },
                                label = stringResource(R.string.nav_albums),
                                isSelected = currentRoute?.startsWith("album") == true || currentRoute == "albums",
                                onClick = { 
                                    if (nestedNavController.currentDestination?.route == "albums") {
                                        viewModel.triggerAlbumsScrollToTop()
                                    } else {
                                        nestedNavController.navigate("albums") {
                                            popUpTo(nestedNavController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            DockItem(
                                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_dashboard), contentDescription = stringResource(R.string.nav_collections), modifier = Modifier.size(24.dp)) },
                                label = stringResource(R.string.nav_collections),
                                isSelected = currentRoute == "collections",
                                onClick = { 
                                    if (nestedNavController.currentDestination?.route == "collections") {
                                        viewModel.triggerCollectionsScrollToTop()
                                    } else {
                                        nestedNavController.navigate("collections") {
                                            popUpTo(nestedNavController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            DockItem(
                                icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search), contentDescription = stringResource(R.string.nav_search), modifier = Modifier.size(24.dp)) },
                                label = stringResource(R.string.nav_search),
                                isSelected = currentRoute == "search",
                                onClick = { nestedNavController.navigate("search") {
                                    popUpTo(nestedNavController.graph.findStartDestination().id) { saveState = true }
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
        val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val targetBottomPadding = if (isSelectionMode) {
            76.dp + navBarBottomInset
        } else {
            innerPadding.calculateBottomPadding()
        }
        val animatedBottomPadding by animateDpAsState(
            targetValue = targetBottomPadding.coerceAtLeast(0.dp),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "scaffoldBottomPadding"
        )
        val layoutDirection = LocalLayoutDirection.current
        val effectivePadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection).coerceAtLeast(0.dp),
            top = innerPadding.calculateTopPadding().coerceAtLeast(0.dp),
            end = innerPadding.calculateEndPadding(layoutDirection).coerceAtLeast(0.dp),
            bottom = animatedBottomPadding.coerceAtLeast(0.dp)
        )
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
                    contentPadding = effectivePadding,
                    isMainTab = true,
                    topBarCollapseFractionProvider = {
                        if (isCollapsibleTopBarActive && topBarHeightPx > 0f) {
                            (-topBarOffsetY / topBarHeightPx).coerceIn(0f, 1f)
                        } else 0f
                    },
                    onNavigateToSettings = {
                        nestedNavController.navigate("settings") {
                            popUpTo("photos") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("videos") {
                GalleryScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onPhotoClick = onPhotoClick,
                    viewModel = viewModel,
                    contentPadding = effectivePadding,
                    bucketName = com.inferno.gallery.data.BucketNames.VIDEOS,
                    isMainTab = true,
                    topBarCollapseFractionProvider = {
                        if (isCollapsibleTopBarActive && topBarHeightPx > 0f) {
                            (-topBarOffsetY / topBarHeightPx).coerceIn(0f, 1f)
                        } else 0f
                    },
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
                    if (initialState.destination.route?.startsWith("album/") == true || initialState.destination.route == "all_albums" || initialState.destination.route == "pinned_albums") {
                        fadeIn(MotionTokens.fastEffectsSpec())
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                exitTransition = {
                    if (targetState.destination.route?.startsWith("album/") == true || targetState.destination.route == "all_albums" || targetState.destination.route == "pinned_albums") {
                        fadeOut(MotionTokens.fastEffectsSpec())
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route?.startsWith("album/") == true || initialState.destination.route == "all_albums" || initialState.destination.route == "pinned_albums") {
                        fadeIn(MotionTokens.fastEffectsSpec())
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popExitTransition = {
                    if (targetState.destination.route?.startsWith("album/") == true || targetState.destination.route == "all_albums" || targetState.destination.route == "pinned_albums") {
                        fadeOut(MotionTokens.fastEffectsSpec())
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
                    contentPadding = effectivePadding,
                    onAlbumClick = { bucketName -> 
                        viewModel.resetGridScrollPosition(bucketName)
                        viewModel.setBucket(bucketName)
                        val encoded = android.net.Uri.encode(bucketName)
                        nestedNavController.navigate("album/$encoded")
                    },
                    onNavigateToVault = onNavigateToVault,
                    onNavigateToDuplicateCleaner = { nestedNavController.navigate("duplicate_cleaner") },
                    onNavigateToPhotoMap = { nestedNavController.navigate("photo_map") },
                    onNavigateToPlacesList = { nestedNavController.navigate("places_list") },
                    onNavigateToPeople = { nestedNavController.navigate("people_list") },
                    onPersonClick = { personId -> nestedNavController.navigate("person_detail/$personId") },
                    onNavigateToAllAlbums = { nestedNavController.navigate("all_albums") },
                    onNavigateToPinnedAlbums = { nestedNavController.navigate("pinned_albums") },
                    onCreateCollage = onCreateCollage,
                    onCreateStitch = onCreateStitch,
                    onOpenEditor = onOpenEditor,
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
                route = "collections",
                enterTransition = { getEnterTransition(initialState.destination.route, targetState.destination.route) },
                exitTransition = { getExitTransition(initialState.destination.route, targetState.destination.route) },
                popEnterTransition = { getEnterTransition(initialState.destination.route, targetState.destination.route) },
                popExitTransition = { getExitTransition(initialState.destination.route, targetState.destination.route) }
            ) {
                CollectionsScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                    viewModel = viewModel,
                    contentPadding = effectivePadding,
                    onAlbumClick = { bucketName ->
                        viewModel.resetGridScrollPosition(bucketName)
                        viewModel.setBucket(bucketName)
                        val encoded = android.net.Uri.encode(bucketName)
                        nestedNavController.navigate("album/$encoded")
                    },
                    onPersonClick = { personId -> nestedNavController.navigate("person_detail/$personId") },
                    onNavigateToVault = onNavigateToVault,
                    onNavigateToDuplicateCleaner = { nestedNavController.navigate("duplicate_cleaner") },
                    onNavigateToPhotoMap = { nestedNavController.navigate("photo_map") },
                    onNavigateToPlacesList = { nestedNavController.navigate("places_list") },
                    onNavigateToPeople = { nestedNavController.navigate("people_list") },
                    onCreateCollage = onCreateCollage,
                    onCreateStitch = onCreateStitch,
                    onOpenEditor = onOpenEditor
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
                    onNavigateUp = { nestedNavController.popBackStackSafely() },
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
                    onNavigateBack = { nestedNavController.popBackStackSafely() }
                )
            }

            composable(
                route = "people_list",
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
                com.inferno.gallery.ui.people.PeopleScreen(
                    onBack = { nestedNavController.popBackStackSafely() },
                    onPersonClick = { personId -> nestedNavController.navigate("person_detail/$personId") },
                    onStartScan = { settingsViewModel.startFaceIndexing() },
                    onNavigateToSettings = {
                        settingsActiveSection = "Smart Search & OCR"
                        nestedNavController.navigate("settings")
                    },
                    viewModel = peopleViewModel
                )
            }

            composable(
                route = "people",
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
                com.inferno.gallery.ui.people.PeopleScreen(
                    onBack = { nestedNavController.popBackStackSafely() },
                    onPersonClick = { personId -> nestedNavController.navigate("person_detail/$personId") },
                    onStartScan = { settingsViewModel.startFaceIndexing() },
                    onNavigateToSettings = {
                        settingsActiveSection = "Smart Search & OCR"
                        nestedNavController.navigate("settings")
                    },
                    viewModel = peopleViewModel
                )
            }

            composable(
                route = "person_detail/{personId}",
                arguments = listOf(
                    androidx.navigation.navArgument("personId") { type = androidx.navigation.NavType.LongType }
                ),
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
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getLong("personId") ?: return@composable
                com.inferno.gallery.ui.people.PersonDetailScreen(
                    personId = personId,
                    onBack = { nestedNavController.popBackStackSafely(backStackEntry) },
                    onMediaClick = { media -> onPhotoClick(media.id.toString(), "person:$personId", null) },
                    onCreateCollage = onCreateCollage,
                    onCreateStitch = onCreateStitch
                )
            }

            composable(
                route = "person/{personId}",
                arguments = listOf(
                    androidx.navigation.navArgument("personId") { type = androidx.navigation.NavType.LongType }
                ),
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
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getLong("personId") ?: return@composable
                com.inferno.gallery.ui.people.PersonDetailScreen(
                    personId = personId,
                    onBack = { nestedNavController.popBackStackSafely(backStackEntry) },
                    onMediaClick = { media -> onPhotoClick(media.id.toString(), "person:$personId", null) },
                    onCreateCollage = onCreateCollage,
                    onCreateStitch = onCreateStitch
                )
            }

            composable(
                route = "album/{bucketName}",
                enterTransition = {
                    if (initialState.destination.route == "albums" || initialState.destination.route == "all_albums" || initialState.destination.route == "pinned_albums" || initialState.destination.route == "search") {
                        fadeIn(MotionTokens.fastEffectsSpec())
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                exitTransition = {
                    if (targetState.destination.route == "albums" || targetState.destination.route == "all_albums" || targetState.destination.route == "pinned_albums" || targetState.destination.route == "search") {
                        fadeOut(MotionTokens.fastEffectsSpec())
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route == "albums" || initialState.destination.route == "all_albums" || initialState.destination.route == "pinned_albums" || initialState.destination.route == "search") {
                        fadeIn(MotionTokens.fastEffectsSpec())
                    } else {
                        getEnterTransition(initialState.destination.route, targetState.destination.route)
                    }
                },
                popExitTransition = {
                    if (targetState.destination.route == "albums" || targetState.destination.route == "all_albums" || targetState.destination.route == "pinned_albums" || targetState.destination.route == "search") {
                        fadeOut(MotionTokens.fastEffectsSpec())
                    } else {
                        getExitTransition(initialState.destination.route, targetState.destination.route)
                    }
                }
            ) { backStackEntry ->
                val rawBucketName = backStackEntry.arguments?.getString("bucketName") ?: ""
                val bucketName = try { android.net.Uri.decode(rawBucketName) } catch (e: Exception) { rawBucketName }
                androidx.compose.runtime.key(bucketName) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        GalleryScreen(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onPhotoClick = onPhotoClick,
                            viewModel = viewModel,
                            contentPadding = effectivePadding,
                            bucketName = bucketName,
                            topBarCollapseFractionProvider = {
                                if (isCollapsibleTopBarActive && topBarHeightPx > 0f) {
                                    (-topBarOffsetY / topBarHeightPx).coerceIn(0f, 1f)
                                } else 0f
                            }
                        )
                    }
                }
            }
            composable("search") {
                SearchScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onPhotoClick = onPhotoClick,
                    viewModel = viewModel,
                    contentPadding = effectivePadding,
                    onAlbumClick = { bucketName ->
                        viewModel.resetGridScrollPosition(bucketName)
                        viewModel.setBucket(bucketName)
                        val encoded = android.net.Uri.encode(bucketName)
                        nestedNavController.navigate("album/$encoded")
                    },
                    onPersonClick = { personId ->
                        nestedNavController.navigate("person/$personId")
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    contentPadding = effectivePadding,
                    galleryViewModel = viewModel,
                    onBackClick = { nestedNavController.popBackStackSafely() },
                    onNavigateToVault = onNavigateToVault,
                    activeSection = settingsActiveSection,
                    onActiveSectionChange = { settingsActiveSection = it }
                )
            }


            composable("photo_map") {
                PhotoMapScreen(
                    galleryViewModel = viewModel,
                    onPhotoClick = onPhotoClick,
                    onBackClick = { nestedNavController.popBackStackSafely() }
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
                    onBackClick = { nestedNavController.popBackStackSafely() },
                    onAlbumClick = { bucketName -> 
                        viewModel.resetGridScrollPosition(bucketName)
                        viewModel.setBucket(bucketName)
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
            composable(
                route = "pinned_albums",
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
                PinnedAlbumsScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                    galleryViewModel = viewModel,
                    onBackClick = { nestedNavController.popBackStackSafely() },
                    onAlbumClick = { bucketName ->
                        viewModel.resetGridScrollPosition(bucketName)
                        viewModel.setBucket(bucketName)
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
                    shape = ShapeEdgeTop,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
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
                                shape = ShapeMedium
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
                                                        negativeButtonText = "Cancel",
                                                        unlockSession = false,
                                                        onSuccess = {
                                                            val uris = selectedUris.mapNotNull { android.net.Uri.parse(it) }
                                                            viewModel.hideMedia(uris)
                                                            viewModel.vaultAuthManager.lock()
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
                                                    shape = ShapeMedium
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
                                                .clip(ShapeMedium)
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
                                com.inferno.gallery.ui.components.PhotonDialog(
                                    onDismissRequest = {
                                        showCreateFolderDialog = false
                                        newFolderName = ""
                                    },
                                    title = stringResource(R.string.dialog_create_folder_title),
                                    subtitle = currentBrowsingDirectory.name,
                                    icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(R.drawable.ic_ms_create_new_folder),
                                    confirmText = stringResource(R.string.action_create),
                                    isConfirmEnabled = newFolderName.isNotBlank(),
                                    onConfirm = {
                                        if (newFolderName.isNotBlank()) {
                                            val newDir = java.io.File(currentBrowsingDirectory, newFolderName.trim())
                                            if (!newDir.exists()) {
                                                newDir.mkdirs()
                                                refreshTrigger++
                                            }
                                            showCreateFolderDialog = false
                                            newFolderName = ""
                                        }
                                    },
                                    dismissText = stringResource(R.string.action_cancel),
                                    content = {
                                        androidx.compose.material3.OutlinedTextField(
                                            value = newFolderName,
                                            onValueChange = { newFolderName = it },
                                            label = { Text(stringResource(R.string.dialog_folder_name_label)) },
                                            singleLine = true,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
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
                                    shape = ShapeMedium
                                ) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                                ExpressiveButton(
                                    onClick = {
                                        showMoveSheet = false
                                        viewModel.moveSelectedMediaToPath(currentBrowsingDirectory.absolutePath)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = ShapeMedium
                                ) {
                                    Text(stringResource(R.string.sheet_move_here))
                                }
                            }
                        }
                    }
                }
            }
            
            if (showCopySheet) {
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { showCopySheet = false },
                    shape = ShapeEdgeTop,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
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
                                shape = ShapeMedium
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
                                                .clip(ShapeMedium)
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
                                com.inferno.gallery.ui.components.PhotonDialog(
                                    onDismissRequest = {
                                        showCreateFolderDialog = false
                                        newFolderName = ""
                                    },
                                    title = stringResource(R.string.dialog_create_folder_title),
                                    subtitle = currentBrowsingDirectory.name,
                                    icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(R.drawable.ic_ms_create_new_folder),
                                    confirmText = stringResource(R.string.action_create),
                                    isConfirmEnabled = newFolderName.isNotBlank(),
                                    onConfirm = {
                                        if (newFolderName.isNotBlank()) {
                                            val newDir = java.io.File(currentBrowsingDirectory, newFolderName.trim())
                                            if (!newDir.exists()) {
                                                newDir.mkdirs()
                                                refreshTrigger++
                                            }
                                            showCreateFolderDialog = false
                                            newFolderName = ""
                                        }
                                    },
                                    dismissText = stringResource(R.string.action_cancel),
                                    content = {
                                        androidx.compose.material3.OutlinedTextField(
                                            value = newFolderName,
                                            onValueChange = { newFolderName = it },
                                            label = { Text(stringResource(R.string.dialog_folder_name_label)) },
                                            singleLine = true,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
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
                                    shape = ShapeMedium
                                ) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                                ExpressiveButton(
                                    onClick = {
                                        showCopySheet = false
                                        viewModel.copySelectedMediaToPath(currentBrowsingDirectory.absolutePath)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = ShapeMedium
                                ) {
                                    Text(stringResource(R.string.sheet_copy_here))
                                }
                            }
                        }
                    }
                }
            }

            if (showMultiDeleteConfirmDialog) {
                var dialogSkipConfirm by remember { mutableStateOf(viewModel.sessionSkipDeleteConfirmation) }
                var dialogSkipRecycleBin by remember { mutableStateOf(viewModel.sessionSkipRecycleBin) }
                val isPermanent = dialogSkipRecycleBin

                com.inferno.gallery.ui.components.PhotonDialog(
                    onDismissRequest = { showMultiDeleteConfirmDialog = false },
                    title = if (isPermanent) stringResource(R.string.dialog_delete_perm_title) else stringResource(R.string.dialog_move_to_trash_title),
                    subtitle = if (isPermanent) {
                        stringResource(R.string.dialog_delete_perm_msg, selectedUris.size)
                    } else {
                        stringResource(R.string.dialog_multi_trash_msg, selectedUris.size)
                    },
                    icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(R.drawable.ic_ms_delete),
                    confirmText = if (isPermanent) stringResource(R.string.dialog_delete_perm_title) else stringResource(R.string.dialog_move_to_trash_confirm),
                    isDanger = isPermanent,
                    onConfirm = {
                        showMultiDeleteConfirmDialog = false
                        viewModel.setTrashSessionOptions(dialogSkipConfirm, dialogSkipRecycleBin)
                        if (dialogSkipConfirm) {
                            android.widget.Toast.makeText(
                                context,
                                if (dialogSkipRecycleBin) "Direct permanent deletion active for this session" else "Quick trash active for this session",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        val rawSelected = selectedUris.toList()
                        pendingTrashUris = rawSelected
                        val uris = rawSelected.map { Uri.parse(it) }
                        if (uris.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val intentSender = if (isPermanent) {
                                        android.provider.MediaStore.createDeleteRequest(
                                            context.contentResolver,
                                            uris
                                        ).intentSender
                                    } else {
                                        android.provider.MediaStore.createTrashRequest(
                                            context.contentResolver,
                                            uris,
                                            true
                                        ).intentSender
                                    }
                                    val request = androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                                    withContext(Dispatchers.Main) {
                                        trashLauncher.launch(request)
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    dismissText = stringResource(R.string.action_cancel),
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isPermanent) {
                                    "Selected items will be permanently removed from your device. This cannot be undone."
                                } else {
                                    stringResource(R.string.dialog_move_to_trash_msg)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            com.inferno.gallery.ui.components.TrashSessionOptions(
                                skipConfirm = dialogSkipConfirm,
                                onSkipConfirmChange = { dialogSkipConfirm = it },
                                skipRecycleBin = dialogSkipRecycleBin,
                                onSkipRecycleBinChange = { dialogSkipRecycleBin = it }
                            )
                        }
                    }
                )
            }
            
            val isTrashPage = currentRoute == "album/{bucketName}" && albumNameArg == "Trash"
            if (isTrashPage) {
                if (showTrashRestoreConfirmDialog) {
                    com.inferno.gallery.ui.components.PhotonDialog(
                        onDismissRequest = { showTrashRestoreConfirmDialog = false },
                        title = stringResource(R.string.dialog_restore_items_title),
                        subtitle = stringResource(R.string.dialog_restore_items_msg, selectedUris.size),
                        icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                        confirmText = stringResource(R.string.action_restore),
                        onConfirm = {
                            showTrashRestoreConfirmDialog = false
                            val rawSelected = selectedUris.toList()
                            pendingTrashUris = rawSelected
                            val uris = rawSelected.map { Uri.parse(it) }
                            if (uris.isNotEmpty()) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val restoreIntent = android.provider.MediaStore.createTrashRequest(
                                            context.contentResolver,
                                            uris,
                                            false
                                        )
                                        val request = androidx.activity.result.IntentSenderRequest.Builder(restoreIntent.intentSender).build()
                                        withContext(Dispatchers.Main) {
                                            restoreLauncher.launch(request)
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(context, "Error restoring: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        dismissText = stringResource(R.string.action_cancel)
                    )
                }

                if (showTrashDeleteConfirmDialog) {
                    com.inferno.gallery.ui.components.PhotonDialog(
                        onDismissRequest = { showTrashDeleteConfirmDialog = false },
                        title = stringResource(R.string.dialog_delete_perm_title),
                        subtitle = stringResource(R.string.dialog_delete_perm_msg, selectedUris.size),
                        icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(R.drawable.ic_ms_delete),
                        confirmText = stringResource(R.string.action_delete),
                        onConfirm = {
                            showTrashDeleteConfirmDialog = false
                            val rawSelected = selectedUris.toList()
                            pendingTrashUris = rawSelected
                            val uris = rawSelected.map { Uri.parse(it) }
                            if (uris.isNotEmpty()) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val deleteIntent = android.provider.MediaStore.createDeleteRequest(
                                            context.contentResolver,
                                            uris
                                        )
                                        val request = androidx.activity.result.IntentSenderRequest.Builder(deleteIntent.intentSender).build()
                                        withContext(Dispatchers.Main) {
                                            permanentDeleteLauncher.launch(request)
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(context, "Error deleting: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        dismissText = stringResource(R.string.action_cancel)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveButton(
                        onClick = { showTrashRestoreConfirmDialog = true },
                        shape = ShapeFull,
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
                        Text(stringResource(R.string.action_restore))
                    }

                    ExpressiveButton(
                        onClick = { showTrashDeleteConfirmDialog = true },
                        shape = ShapeFull,
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
                        Text(stringResource(R.string.action_delete))
                    }
                }
            } else {
                val isCreateEnabled = selectedUris.size in 1..8
                val isStitchEnabled = selectedUris.size in 2..10
                var createMenuExpanded by remember { mutableStateOf(false) }
                var moreMenuExpanded by remember { mutableStateOf(false) }

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
                        IconButton(onClick = {
                            if (selectedUris.isNotEmpty()) {
                                coroutineScope.launch {
                                    val strip = viewModel.settingsRepository.stripMetadataOnShareFlow.first()
                                    com.inferno.gallery.ui.utils.ShareUtils.shareMedia(context, selectedUris.map { android.net.Uri.parse(it) }, strip)
                                }
                            }
                        }) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_share), contentDescription = "Share", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(IconSizeTokens.L))
                        }

                        // Copy
                        IconButton(onClick = { showCopySheet = true }) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_content_copy), contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(IconSizeTokens.L))
                        }

                        // Move
                        IconButton(onClick = { showMoveSheet = true }) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_drive_file_move), contentDescription = "Move", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(IconSizeTokens.L))
                        }

                        // Create (Collage/Stitch)
                        Box {
                            IconButton(onClick = { createMenuExpanded = true }) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_add), contentDescription = "Create", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(IconSizeTokens.L))
                            }
                            DropdownMenu(
                                expanded = createMenuExpanded,
                                onDismissRequest = { createMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_collage)) },
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
                                    text = { Text(stringResource(R.string.action_stitch)) },
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
                            val isSessionSkipConfirm = viewModel.sessionSkipDeleteConfirmation
                            val isSessionPermanent = viewModel.sessionSkipRecycleBin

                            if (isSessionSkipConfirm || !confirmDeleteEnabled) {
                                val rawSelected = selectedUris.toList()
                                pendingTrashUris = rawSelected
                                val uris = rawSelected.map { Uri.parse(it) }
                                if (uris.isNotEmpty()) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val intentSender = if (isSessionPermanent) {
                                                android.provider.MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
                                            } else {
                                                android.provider.MediaStore.createTrashRequest(context.contentResolver, uris, true).intentSender
                                            }
                                            val request = androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                                            withContext(Dispatchers.Main) {
                                                trashLauncher.launch(request)
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            } else {
                                showMultiDeleteConfirmDialog = true
                            }
                        }) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_delete), contentDescription = "Delete", tint = com.inferno.gallery.ui.theme.LocalHarmonizedColors.current.error, modifier = Modifier.size(IconSizeTokens.L))
                        }

                        // More options
                        Box {
                            IconButton(onClick = { moreMenuExpanded = true }) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_more_vert), contentDescription = "More", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(IconSizeTokens.L))
                            }
                            DropdownMenu(
                                expanded = moreMenuExpanded,
                                onDismissRequest = { moreMenuExpanded = false }
                            ) {
                                // Hide (Private Space)
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_hide)) },
                                    leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_visibility_off), contentDescription = null) },
                                    onClick = {
                                        moreMenuExpanded = false
                                        val activity = context as? androidx.fragment.app.FragmentActivity
                                        if (activity != null) {
                                            viewModel.vaultAuthManager.authenticate(
                                                activity = activity,
                                                negativeButtonText = "Cancel",
                                                unlockSession = false,
                                                onSuccess = {
                                                    val uris = selectedUris.mapNotNull { android.net.Uri.parse(it) }
                                                    viewModel.hideMedia(uris)
                                                    viewModel.vaultAuthManager.lock()
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
        com.inferno.gallery.ui.components.PhotonDialog(
            onDismissRequest = {
                showCreateAlbumDialog = false
                newAlbumName = ""
            },
            title = stringResource(R.string.new_album),
            icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(R.drawable.ic_ms_create_new_folder),
            confirmText = stringResource(R.string.action_create),
            isConfirmEnabled = newAlbumName.isNotBlank(),
            onConfirm = {
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
            dismissText = stringResource(R.string.action_cancel),
            content = {
                androidx.compose.material3.OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    label = { Text(stringResource(R.string.album_name_label)) },
                    placeholder = { Text(stringResource(R.string.album_name_hint)) },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    if (showDisplaySortSheet) {
        DisplayAndSortBottomSheet(
            isAlbumsScreen = (currentRoute == "albums" || currentRoute == "all_albums" || currentRoute == "pinned_albums"),
            viewMode = viewMode,
            onViewModeChange = { viewModel.setViewMode(it) },
            albumLayoutMode = albumLayoutMode,
            onAlbumLayoutModeChange = { viewModel.setAlbumLayoutMode(it) },
            sortOrder = sortOrder,
            onSortOrderChange = { viewModel.setSortOrder(it) },
            albumSortOrder = albumSortOrder,
            onAlbumSortOrderChange = { viewModel.setAlbumSortOrder(it) },
            onDismissRequest = { showDisplaySortSheet = false }
        )
    }

}
}
