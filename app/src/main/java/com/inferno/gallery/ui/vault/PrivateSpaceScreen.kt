@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.inferno.gallery.ui.vault

import androidx.compose.ui.res.stringResource

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.inferno.gallery.data.db.VaultMediaEntity
import com.inferno.gallery.ui.GalleryViewModel
import com.inferno.gallery.ui.ViewMode
import com.inferno.gallery.ui.components.PhotonEmptyState
import com.inferno.gallery.ui.components.TimelineSectionHeader
import com.inferno.gallery.ui.components.thumbnailTargetPx
import com.inferno.gallery.ui.theme.ExpressiveShapes
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeSmall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R


private enum class PinSetupStage {
    VERIFY_CURRENT_PIN,
    ENTER_NEW_PIN,
    CONFIRM_NEW_PIN
}

private sealed interface VaultGridEntry {
    val key: String

    data class Header(val id: String, val title: String) : VaultGridEntry {
        override val key: String get() = "vault_header_$id"
    }

    data class Media(val entity: VaultMediaEntity) : VaultGridEntry {
        override val key: String get() = "vault_media_${entity.id}"
    }
}

@Composable
fun PrivateSpaceScreen(
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? FragmentActivity

    val isUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val vaultItems by viewModel.vaultItems.collectAsState()
    val gridCellsCount by viewModel.gridCellsCount.collectAsState()
    val thumbnailCornerRadius by viewModel.thumbnailCornerRadius.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val timelineGroupBy by viewModel.timelineGroupBy.collectAsState()
    val dateHeaderAlignment by viewModel.dateHeaderAlignment.collectAsState()
    val lazyGridState = rememberLazyGridState()
    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            viewModel.syncVaultOnUnlock()
        }
    }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    // PIN State
    var isPinConfigured by remember { mutableStateOf(viewModel.vaultAuthManager.isPinConfigured(context)) }
    var enteredPin by remember { mutableStateOf("") }
    var setupStage by remember { mutableStateOf(PinSetupStage.ENTER_NEW_PIN) }
    var firstEnteredPin by remember { mutableStateOf("") }
    var isChangingPinMode by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var viewingItem by remember { mutableStateOf<VaultMediaEntity?>(null) }

    fun checkAllFilesAccess(): Boolean {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R ||
            android.os.Environment.isExternalStorageManager()
    }
    var hasAllFilesAccess by remember { mutableStateOf(checkAllFilesAccess()) }

    val openAllFilesPermission: () -> Unit = {
        val packageUri = android.net.Uri.parse("package:${context.packageName}")
        val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                packageUri
            )
        } else {
            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                )
            } catch (_: Exception) {}
        }
    }

    // ── Session Invalidation on Exit / Background / Disposal ────────────────
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAllFilesAccess = checkAllFilesAccess()
            } else if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                viewingItem = null
                viewModel.lockVault()
                selectedIds = emptySet()
                enteredPin = ""
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewingItem = null
            viewModel.lockVault()
            selectedIds = emptySet()
            enteredPin = ""
        }
    }

    BackHandler(enabled = viewingItem != null || isSelectionMode || (!isUnlocked && isChangingPinMode)) {
        if (viewingItem != null) {
            viewingItem = null
        } else if (isSelectionMode) {
            selectedIds = emptySet()
        } else if (!isUnlocked && isChangingPinMode) {
            isChangingPinMode = false
            setupStage = PinSetupStage.ENTER_NEW_PIN
            firstEnteredPin = ""
            enteredPin = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                // ── Floating Action Scrim Toolbar ────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Back button
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 0.dp,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds = emptySet()
                                    } else if (!isUnlocked && isChangingPinMode) {
                                        isChangingPinMode = false
                                        setupStage = PinSetupStage.ENTER_NEW_PIN
                                        firstEnteredPin = ""
                                        enteredPin = ""
                                    } else {
                                        viewingItem = null
                                        viewModel.lockVault()
                                        selectedIds = emptySet()
                                        enteredPin = ""
                                        onNavigateBack()
                                    }
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = "Back")
                            }
                        }
                    }

                    // Right: Title & Actions Pill
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 0.dp,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, end = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedContent(
                                targetState = isSelectionMode to selectedIds.size,
                                transitionSpec = {
                                    (slideInVertically(MotionTokens.snappySpring()) { -it } + fadeIn(MotionTokens.snappySpring())) togetherWith
                                            (slideOutVertically(MotionTokens.snappySpring()) { it } + fadeOut(MotionTokens.snappySpring()))
                                },
                                label = "vaultTitle"
                            ) { (selecting, count) ->
                                Text(
                                    text = if (selecting) "$count Selected" else "Private Space",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            if (isUnlocked && isSelectionMode && vaultItems.isNotEmpty()) {
                                val allSelected = selectedIds.size == vaultItems.size
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedIds = if (allSelected) {
                                            emptySet()
                                        } else {
                                            vaultItems.map { it.id }.toSet()
                                        }
                                    },
                                    shape = ShapeFull,
                                    color = if (allSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    },
                                    contentColor = if (allSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_select_all),
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = stringResource(
                                                if (allSelected) R.string.action_deselect_all else R.string.action_select_all
                                            ),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1
                                        )
                                    }
                                }
                            } else if (isUnlocked && !isSelectionMode) {
                                Box {
                                    FilledTonalIconButton(
                                        onClick = { showOptionsMenu = true },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_more_vert), contentDescription = "Options", modifier = Modifier.size(18.dp))
                                    }

                                    DropdownMenu(
                                        expanded = showOptionsMenu,
                                        onDismissRequest = { showOptionsMenu = false },
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = ShapeLarge
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.vault_lock_now), fontWeight = FontWeight.Medium) },
                                            leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_lock), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                showOptionsMenu = false
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewingItem = null
                                                viewModel.lockVault()
                                                enteredPin = ""
                                                selectedIds = emptySet()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.vault_change_pin), fontWeight = FontWeight.Medium) },
                                            leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_key), contentDescription = null) },
                                            onClick = {
                                                showOptionsMenu = false
                                                viewModel.vaultAuthManager.lock()
                                                selectedIds = emptySet()
                                                isChangingPinMode = true
                                                isPinConfigured = true
                                                setupStage = PinSetupStage.VERIFY_CURRENT_PIN
                                                enteredPin = ""
                                                firstEnteredPin = ""
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // ── Floating Multi-Selection Action Pill ──────────────────
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = slideInVertically(MotionTokens.gentleSpring()) { it } + fadeIn(MotionTokens.gentleSpring()),
                    exit = slideOutVertically(MotionTokens.gentleSpring()) { it } + fadeOut(MotionTokens.gentleSpring())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        com.inferno.gallery.ui.components.LayeredToolPill(
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 340.dp)
                        ) {
                            com.inferno.gallery.ui.components.LayeredToolTile(
                                icon = ImageVector.vectorResource(R.drawable.ic_ms_visibility),
                                contentDescription = "Unhide",
                                label = "Unhide (${selectedIds.size})",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.unhideMedia(selectedIds.toList())
                                    selectedIds = emptySet()
                                    Toast.makeText(context, "Restored to gallery", Toast.LENGTH_SHORT).show()
                                },
                                tileShape = com.inferno.gallery.ui.components.LayeredToolPillDefaults.segmentShape(0, 2),
                                modifier = Modifier.weight(1f)
                            )

                            com.inferno.gallery.ui.components.LayeredToolTile(
                                icon = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                                contentDescription = "Delete",
                                label = "Delete",
                                tint = com.inferno.gallery.ui.theme.LocalHarmonizedColors.current.error,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.deleteFromVault(selectedIds.toList())
                                    selectedIds = emptySet()
                                    Toast.makeText(context, "Deleted permanently", Toast.LENGTH_SHORT).show()
                                },
                                tileShape = com.inferno.gallery.ui.components.LayeredToolPillDefaults.segmentShape(1, 2),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = isUnlocked,
                    transitionSpec = {
                        fadeIn(MotionTokens.gentleSpring()) togetherWith fadeOut(MotionTokens.gentleSpring())
                    },
                    label = "vaultContent"
                ) { unlocked ->
                    if (!unlocked) {
                        // ── Locked State: PIN Setup or Unlock Keypad ──────────
                        val isSetup = !isPinConfigured || (isChangingPinMode && setupStage != PinSetupStage.VERIFY_CURRENT_PIN)

                        val headerTitle = when {
                            isChangingPinMode && setupStage == PinSetupStage.VERIFY_CURRENT_PIN -> "Verify Current PIN"
                            isSetup && setupStage == PinSetupStage.ENTER_NEW_PIN -> if (isChangingPinMode) "Enter New PIN" else "Create Private Space PIN"
                            isSetup && setupStage == PinSetupStage.CONFIRM_NEW_PIN -> "Confirm Your PIN"
                            else -> "Private Space"
                        }

                        val headerSubtitle = when {
                            isChangingPinMode && setupStage == PinSetupStage.VERIFY_CURRENT_PIN -> "Enter your current 4-digit PIN to continue"
                            isSetup && setupStage == PinSetupStage.ENTER_NEW_PIN -> "Enter a 4-digit Master PIN to protect hidden photos"
                            isSetup && setupStage == PinSetupStage.CONFIRM_NEW_PIN -> "Re-enter your 4-digit PIN to confirm"
                            else -> "Enter your 4-digit PIN or tap fingerprint"
                        }

                        ExpressiveVaultKeypad(
                            title = headerTitle,
                            subtitle = headerSubtitle,
                            isSetup = isSetup,
                            setupStage = setupStage,
                            enteredPin = enteredPin,
                            showBiometricKey = !isSetup && !isChangingPinMode,
                            shakeOffset = shakeOffset.value,
                            onDigitPress = { digit ->
                                if (enteredPin.length < 4) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newPin = enteredPin + digit
                                    enteredPin = newPin

                                    if (newPin.length == 4) {
                                        if (isSetup) {
                                            // Handle PIN Setup Flow
                                            if (setupStage == PinSetupStage.ENTER_NEW_PIN) {
                                                firstEnteredPin = newPin
                                                setupStage = PinSetupStage.CONFIRM_NEW_PIN
                                                enteredPin = ""
                                            } else {
                                                // Confirming PIN
                                                if (newPin == firstEnteredPin) {
                                                    viewModel.vaultAuthManager.savePin(context, newPin)
                                                    isPinConfigured = true
                                                    isChangingPinMode = false
                                                    setupStage = PinSetupStage.ENTER_NEW_PIN
                                                    firstEnteredPin = ""
                                                    viewModel.vaultAuthManager.unlock()
                                                    enteredPin = ""
                                                    Toast.makeText(context, "Private Space PIN saved successfully!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    // Mismatch shake
                                                    coroutineScope.launch {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        shakeOffset.animateTo(20f, spring(stiffness = Spring.StiffnessHigh))
                                                        shakeOffset.animateTo(-20f, spring(stiffness = Spring.StiffnessHigh))
                                                        shakeOffset.animateTo(10f, spring(stiffness = Spring.StiffnessHigh))
                                                        shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessHigh))
                                                        setupStage = PinSetupStage.ENTER_NEW_PIN
                                                        firstEnteredPin = ""
                                                        enteredPin = ""
                                                        Toast.makeText(context, "PINs did not match, please try again", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        } else {
                                            val lockoutSec = viewModel.vaultAuthManager.getRemainingLockoutSeconds(context)
                                            if (lockoutSec > 0) {
                                                enteredPin = ""
                                                Toast.makeText(context, "Too many attempts. Try again in ${lockoutSec}s", Toast.LENGTH_SHORT).show()
                                            } else if (viewModel.vaultAuthManager.verifyPin(context, newPin)) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (isChangingPinMode && setupStage == PinSetupStage.VERIFY_CURRENT_PIN) {
                                                    setupStage = PinSetupStage.ENTER_NEW_PIN
                                                    firstEnteredPin = ""
                                                    enteredPin = ""
                                                } else {
                                                    viewModel.vaultAuthManager.unlock()
                                                    enteredPin = ""
                                                }
                                            } else {
                                                // Incorrect PIN shake
                                                coroutineScope.launch {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    shakeOffset.animateTo(20f, spring(stiffness = Spring.StiffnessHigh))
                                                    shakeOffset.animateTo(-20f, spring(stiffness = Spring.StiffnessHigh))
                                                    shakeOffset.animateTo(10f, spring(stiffness = Spring.StiffnessHigh))
                                                    shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessHigh))
                                                    enteredPin = ""
                                                    val newLockout = viewModel.vaultAuthManager.getRemainingLockoutSeconds(context)
                                                    val msg = if (newLockout > 0) "Too many attempts. Wait ${newLockout}s" else "Incorrect PIN"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            onBackspace = {
                                if (enteredPin.isNotEmpty()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    enteredPin = enteredPin.dropLast(1)
                                }
                            },
                            onBiometricPress = {
                                activity?.let {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.vaultAuthManager.authenticateWithBiometrics(
                                        activity = it,
                                        negativeButtonText = "Cancel",
                                        onSuccess = { enteredPin = "" },
                                        onNegativeButton = {},
                                        onFailure = {}
                                    )
                                }
                            }
                        )
                    } else if (vaultItems.isEmpty()) {
                        // ── Unified Expressive Empty State ────────────────────
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (!hasAllFilesAccess) {
                                VaultPersistenceBanner(
                                    onEnableClick = openAllFilesPermission,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                PhotonEmptyState(
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_shield_lock),
                                    title = stringResource(R.string.vault_empty_title),
                                    subtitle = stringResource(R.string.vault_empty_instruction),
                                    shape = ExpressiveShapes.Clover8Leaf
                                )
                            }
                        }
                    } else {
                        // ── Encrypted Media Grid (Pinch-Zoom + Date Grouping + Drag-Select) ──
                        val vaultGridEntries = remember<List<VaultGridEntry>>(vaultItems, viewMode, timelineGroupBy) {
                            if (viewMode == ViewMode.Immersive) {
                                vaultItems.map { VaultGridEntry.Media(it) }
                            } else {
                                buildList<VaultGridEntry>(vaultItems.size + (vaultItems.size / 4).coerceAtLeast(4)) {
                                    var lastHeaderTitle: String? = null
                                    for (item in vaultItems) {
                                        val dateSeconds = item.dateAdded.takeIf { it > 0L } ?: (item.dateHidden / 1000L)
                                        val title = GalleryViewModel.formatGroupHeader(dateSeconds, timelineGroupBy)
                                        if (title != lastHeaderTitle) {
                                            lastHeaderTitle = title
                                            add(VaultGridEntry.Header(id = "${item.id}_$title", title = title))
                                        }
                                        add(VaultGridEntry.Media(item))
                                    }
                                }
                            }
                        }

                        val livePinchScaleAnim = remember { Animatable(1f) }
                        val pinchScaleScope = rememberCoroutineScope()
                        val targetThumbPx = remember(gridCellsCount) { thumbnailTargetPx(gridCellsCount) }
                        val cellShape = remember(thumbnailCornerRadius) {
                            if (thumbnailCornerRadius > 0f) RoundedCornerShape(thumbnailCornerRadius.dp) else ShapeSmall
                        }
                        val bannerHeaderOffset = if (!hasAllFilesAccess) 1 else 0

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .vaultGridPinchZoom(
                                    gridCellsCount = gridCellsCount,
                                    onGridCountChange = { count -> viewModel.setGridCellsCount(count) },
                                    isSelectionMode = isSelectionMode,
                                    haptic = haptic,
                                    onPinchScaleChange = { scale, _ ->
                                        pinchScaleScope.launch {
                                            livePinchScaleAnim.snapTo(scale.coerceIn(0.84f, 1.20f))
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
                                        }
                                    }
                                )
                                .graphicsLayer {
                                    val s = livePinchScaleAnim.value
                                    scaleX = s
                                    scaleY = s
                                    transformOrigin = TransformOrigin.Center
                                }
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridCellsCount),
                                state = lazyGridState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 2.dp)
                                    .vaultGridDragSelectGesture(
                                        lazyGridState = lazyGridState,
                                        entries = vaultGridEntries,
                                        headerOffset = bannerHeaderOffset,
                                        selectedIds = selectedIds,
                                        onSelectionChange = { updated -> selectedIds = updated },
                                        haptic = haptic,
                                        coroutineScope = coroutineScope
                                    ),
                                contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (!hasAllFilesAccess) {
                                    item(
                                        key = "vault_all_files_banner",
                                        span = { GridItemSpan(maxLineSpan) },
                                        contentType = "vault_banner"
                                    ) {
                                        VaultPersistenceBanner(
                                            onEnableClick = openAllFilesPermission,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                items(
                                    items = vaultGridEntries,
                                    key = { it.key },
                                    contentType = { entry ->
                                        when (entry) {
                                            is VaultGridEntry.Header -> "vault_header"
                                            is VaultGridEntry.Media -> "vault_media"
                                        }
                                    },
                                    span = { entry ->
                                        when (entry) {
                                            is VaultGridEntry.Header -> GridItemSpan(maxLineSpan)
                                            is VaultGridEntry.Media -> GridItemSpan(1)
                                        }
                                    }
                                ) { entry ->
                                    when (entry) {
                                        is VaultGridEntry.Header -> {
                                            TimelineSectionHeader(
                                                title = entry.title,
                                                alignment = dateHeaderAlignment
                                            )
                                        }
                                        is VaultGridEntry.Media -> {
                                            val item = entry.entity
                                            val isSelected = selectedIds.contains(item.id)
                                            val viewableFile by produceState<java.io.File?>(initialValue = null, item.vaultFileName, isUnlocked) {
                                                value = viewModel.getViewableVaultFile(item)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(cellShape)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 0.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        shape = cellShape
                                                    )
                                                    .clickable {
                                                        if (isSelectionMode) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                                        } else {
                                                            viewingItem = item
                                                        }
                                                    }
                                            ) {
                                                viewableFile?.let { resolvedFile ->
                                                    val request = remember(resolvedFile, item.vaultFileName, targetThumbPx) {
                                                        ImageRequest.Builder(context)
                                                            .data(resolvedFile)
                                                            .size(targetThumbPx)
                                                            .crossfade(true)
                                                            .build()
                                                    }
                                                    AsyncImage(
                                                        model = request,
                                                        contentDescription = item.fileName,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }

                                                // Selection overlay badge with solid white contrast border
                                                if (isSelected) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                                        border = BorderStroke(2.dp, Color.White),
                                                        shadowElevation = 3.dp,
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(6.dp)
                                                            .size(24.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                                                contentDescription = "Selected",
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Video Duration Badge (no background, outlined icon + text)
                                                if (item.isVideo && item.durationMs != null) {
                                                    Row(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_videocam),
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = formatVaultDuration(item.durationMs),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.SemiBold,
                                                                shadow = androidx.compose.ui.graphics.Shadow(
                                                                    color = Color.Black.copy(alpha = 0.85f),
                                                                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                                                                    blurRadius = 4f
                                                                )
                                                            ),
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            softWrap = false
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
        }

        val activeViewerItem = viewingItem
        if (activeViewerItem != null && isUnlocked && vaultItems.isNotEmpty()) {
            VaultMediaViewerDialog(
                items = vaultItems,
                initialItem = activeViewerItem,
                viewModel = viewModel,
                onDismiss = { viewingItem = null },
                onUnhide = { target ->
                    if (vaultItems.size <= 1) viewingItem = null
                    viewModel.unhideMedia(listOf(target.id))
                },
                onDelete = { target ->
                    if (vaultItems.size <= 1) viewingItem = null
                    viewModel.deleteFromVault(listOf(target.id))
                }
            )
        }
    }
}

@Composable
private fun VaultMediaViewerDialog(
    items: List<VaultMediaEntity>,
    initialItem: VaultMediaEntity,
    viewModel: GalleryViewModel,
    onDismiss: () -> Unit,
    onUnhide: (VaultMediaEntity) -> Unit,
    onDelete: (VaultMediaEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val initialIndex = remember(initialItem.id, items) {
        items.indexOfFirst { it.id == initialItem.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { items.size }
    )
    LaunchedEffect(items.size) {
        if (items.isEmpty()) {
            onDismiss()
        } else if (pagerState.currentPage >= items.size) {
            pagerState.scrollToPage(items.size - 1)
        }
    }

    var showUi by remember { mutableStateOf(true) }
    var currentScale by remember { mutableFloatStateOf(1f) }
    var pendingActionItemId by remember { mutableStateOf<Long?>(null) }
    val dismissOffsetY = remember { Animatable(0f) }
    val dismissProgress = (kotlin.math.abs(dismissOffsetY.value) / 600f).coerceIn(0f, 1f)
    val bgAlpha = (1f - dismissProgress).coerceIn(0f, 1f)
    val viewerBlurEffect by viewModel.settingsRepository.viewerBlurEffectFlow.collectAsState(initial = false)
    val clampedPage = pagerState.currentPage.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val currentItem = items.getOrNull(clampedPage) ?: initialItem

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = bgAlpha))
        ) {
            if (viewerBlurEffect) {
                val blurFile by produceState<java.io.File?>(initialValue = null, currentItem.vaultFileName) {
                    value = viewModel.getViewableVaultFile(currentItem)
                }
                blurFile?.let { file ->
                    val blurRequest = remember(file, currentItem.id) {
                        ImageRequest.Builder(context)
                            .data(file)
                            .size(96, 96)
                            .crossfade(true)
                            .build()
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = blurRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = 1.28f
                                    scaleY = 1.28f
                                }
                                .blur(72.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                                .alpha(0.90f * bgAlpha)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Black.copy(alpha = 0.62f * bgAlpha),
                                        0.28f to Color.Black.copy(alpha = 0.44f * bgAlpha),
                                        0.72f to Color.Black.copy(alpha = 0.44f * bgAlpha),
                                        1f to Color.Black.copy(alpha = 0.64f * bgAlpha)
                                    )
                                )
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentScale <= 1.05f && kotlin.math.abs(dismissOffsetY.value) < 4f,
                key = { page -> items.getOrNull(page)?.id ?: page },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = 1f - dismissProgress * 0.15f
                        translationY = dismissOffsetY.value
                        scaleX = s
                        scaleY = s
                    }
                    .pointerInput(currentScale) {
                        if (currentScale > 1.05f) return@pointerInput
                        val velocityTracker = VelocityTracker()
                        detectVerticalDragGestures(
                            onDragStart = { velocityTracker.resetTracking() },
                            onDragEnd = {
                                val velocity = velocityTracker.calculateVelocity().y
                                coroutineScope.launch {
                                    if (kotlin.math.abs(dismissOffsetY.value) > 200f || kotlin.math.abs(velocity) > 800f) {
                                        onDismiss()
                                    } else {
                                        dismissOffsetY.animateTo(0f, MotionTokens.snappySpring())
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    dismissOffsetY.animateTo(0f, MotionTokens.snappySpring())
                                }
                            }
                        ) { change, dragAmount ->
                            velocityTracker.addPointerInputChange(change)
                            coroutineScope.launch {
                                dismissOffsetY.snapTo(dismissOffsetY.value + dragAmount)
                            }
                        }
                    }
            ) { page ->
                val pageItem = items.getOrNull(page) ?: return@HorizontalPager
                val viewableResult by produceState<Pair<Boolean, java.io.File?>>(
                    initialValue = false to null,
                    pageItem.vaultFileName
                ) {
                    value = true to viewModel.getViewableVaultFile(pageItem)
                }

                val scale = remember(pageItem.id) { mutableFloatStateOf(1f) }
                val offsetX = remember(pageItem.id) { mutableFloatStateOf(0f) }
                val offsetY = remember(pageItem.id) { mutableFloatStateOf(0f) }
                val animJob = remember(pageItem.id) { mutableStateOf<kotlinx.coroutines.Job?>(null) }

                LaunchedEffect(pagerState.currentPage, pageItem.id) {
                    if (pagerState.currentPage != page) {
                        scale.floatValue = 1f
                        offsetX.floatValue = 0f
                        offsetY.floatValue = 0f
                    } else {
                        currentScale = scale.floatValue
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    val (hasLoaded, file) = viewableResult
                    if (!hasLoaded) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (file == null) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_lock),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Unable to decrypt media",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    } else if (pageItem.isVideo) {
                        com.inferno.gallery.ui.VideoPlayerItem(
                            uri = Uri.fromFile(file),
                            isCurrentPage = page == pagerState.currentPage,
                            showControls = showUi,
                            onTap = { showUi = !showUi },
                            onScaleChanged = { newScale ->
                                if (page == pagerState.currentPage) {
                                    currentScale = newScale
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        var imageAspectRatio by remember(pageItem.id) { mutableFloatStateOf(1f) }
                        var containerSize by remember { mutableStateOf(IntSize.Zero) }
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(file)
                                .crossfade(true)
                                .build(),
                            contentDescription = pageItem.fileName,
                            contentScale = ContentScale.Fit,
                            onSuccess = { state ->
                                val s = state.painter.intrinsicSize
                                if (s.width > 0f && s.height > 0f && s.width.isFinite() && s.height.isFinite()) {
                                    imageAspectRatio = s.width / s.height
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center)
                                .onSizeChanged { containerSize = it }
                                .pointerInput(pageItem.id) {
                                    detectTapGestures(
                                        onTap = { showUi = !showUi },
                                        onDoubleTap = { centroid ->
                                            animJob.value?.cancel()
                                            animJob.value = coroutineScope.launch {
                                                val cw = size.width.toFloat()
                                                val ch = size.height.toFloat()
                                                if (scale.floatValue > 1.05f) {
                                                    launch {
                                                        animate(
                                                            initialValue = scale.floatValue,
                                                            targetValue = 1f,
                                                            animationSpec = MotionTokens.sharedElementSpring()
                                                        ) { v, _ ->
                                                            val c = v.coerceAtLeast(1f)
                                                            scale.floatValue = c
                                                            if (page == pagerState.currentPage) currentScale = c
                                                        }
                                                    }
                                                    launch { animate(offsetX.floatValue, 0f, animationSpec = MotionTokens.sharedElementSpring()) { v, _ -> offsetX.floatValue = v } }
                                                    launch { animate(offsetY.floatValue, 0f, animationSpec = MotionTokens.sharedElementSpring()) { v, _ -> offsetY.floatValue = v } }
                                                } else {
                                                    val targetScale = 3.0f
                                                    val imgW = if (imageAspectRatio > 0f) minOf(cw, ch * imageAspectRatio) else cw
                                                    val imgH = if (imageAspectRatio > 0f && imageAspectRatio.isFinite()) imgW / imageAspectRatio else ch
                                                    val maxX = maxOf(0f, (imgW * targetScale - cw) / 2f)
                                                    val maxY = maxOf(0f, (imgH * targetScale - ch) / 2f)
                                                    val targetX = -(centroid.x - cw / 2f) * (targetScale - 1f)
                                                    val targetY = -(centroid.y - ch / 2f) * (targetScale - 1f)
                                                    launch {
                                                        animate(
                                                            initialValue = scale.floatValue,
                                                            targetValue = targetScale,
                                                            animationSpec = MotionTokens.sharedElementSpring()
                                                        ) { v, _ ->
                                                            val c = v.coerceAtLeast(0.05f)
                                                            scale.floatValue = c
                                                            if (page == pagerState.currentPage) currentScale = c
                                                        }
                                                    }
                                                    launch { animate(offsetX.floatValue, targetX.coerceIn(-maxX, maxX), animationSpec = MotionTokens.sharedElementSpring()) { v, _ -> offsetX.floatValue = v } }
                                                    launch { animate(offsetY.floatValue, targetY.coerceIn(-maxY, maxY), animationSpec = MotionTokens.sharedElementSpring()) { v, _ -> offsetY.floatValue = v } }
                                                }
                                            }
                                        }
                                    )
                                }
                                .pointerInput(pageItem.id) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        var accumulatedZoom = 1f
                                        var accumulatedPan = Offset.Zero
                                        var pastTouchSlop = false
                                        val touchSlop = viewConfiguration.touchSlop
                                        val velocityTracker = VelocityTracker()
                                        val cw = size.width.toFloat()
                                        val ch = size.height.toFloat()

                                        do {
                                            val event = awaitPointerEvent()
                                            val pressedCount = event.changes.count { it.pressed }
                                            if (pressedCount == 1) {
                                                event.changes.firstOrNull { it.pressed }?.let { velocityTracker.addPointerInputChange(it) }
                                            }
                                            val isZoomed = scale.floatValue > 1.02f
                                            val isMultiTouch = pressedCount >= 2
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()

                                            if (!pastTouchSlop) {
                                                accumulatedZoom *= zoomChange
                                                accumulatedPan += panChange
                                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                                val zoomMotion = kotlin.math.abs(1f - accumulatedZoom) * centroidSize
                                                val panMotion = accumulatedPan.getDistance()
                                                if (zoomMotion > touchSlop || (isZoomed && panMotion > touchSlop) || isMultiTouch) {
                                                    pastTouchSlop = true
                                                }
                                            }

                                            if (pastTouchSlop && (zoomChange != 1f || panChange != Offset.Zero || isMultiTouch)) {
                                                val centroid = event.calculateCentroid(useCurrent = false)
                                                animJob.value?.cancel()
                                                val oldScale = scale.floatValue
                                                val newScale = (oldScale * zoomChange).coerceIn(1f, 10f)
                                                val ratio = if (oldScale > 0f) newScale / oldScale else 1f
                                                val imgW = if (imageAspectRatio > 0f) minOf(cw, ch * imageAspectRatio) else cw
                                                val imgH = if (imageAspectRatio > 0f && imageAspectRatio.isFinite()) imgW / imageAspectRatio else ch
                                                val maxX = maxOf(0f, (imgW * newScale - cw) / 2f)
                                                val maxY = maxOf(0f, (imgH * newScale - ch) / 2f)

                                                val cx = centroid.x - cw / 2f
                                                val cy = centroid.y - ch / 2f
                                                val nextX = if (newScale > 1f) ((offsetX.floatValue - cx) * ratio + cx + panChange.x).coerceIn(-maxX, maxX) else 0f
                                                val nextY = if (newScale > 1f) ((offsetY.floatValue - cy) * ratio + cy + panChange.y).coerceIn(-maxY, maxY) else 0f

                                                scale.floatValue = newScale
                                                offsetX.floatValue = nextX
                                                offsetY.floatValue = nextY
                                                if (page == pagerState.currentPage) currentScale = newScale

                                                if (isMultiTouch || isZoomed) {
                                                    event.changes.forEach { it.consume() }
                                                }
                                            }
                                        } while (event.changes.any { it.pressed })

                                        if (scale.floatValue > 1.05f && pastTouchSlop) {
                                            val velocity = velocityTracker.calculateVelocity()
                                            val imgW = if (imageAspectRatio > 0f) minOf(cw, ch * imageAspectRatio) else cw
                                            val imgH = if (imageAspectRatio > 0f && imageAspectRatio.isFinite()) imgW / imageAspectRatio else ch
                                            val maxX = maxOf(0f, (imgW * scale.floatValue - cw) / 2f)
                                            val maxY = maxOf(0f, (imgH * scale.floatValue - ch) / 2f)
                                            val targetFlingX = (offsetX.floatValue + velocity.x * 0.35f).coerceIn(-maxX, maxX)
                                            val targetFlingY = (offsetY.floatValue + velocity.y * 0.35f).coerceIn(-maxY, maxY)
                                            animJob.value?.cancel()
                                            animJob.value = coroutineScope.launch {
                                                launch {
                                                    animate(
                                                        offsetX.floatValue, targetFlingX,
                                                        initialVelocity = velocity.x,
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow)
                                                    ) { v, _ -> offsetX.floatValue = v }
                                                }
                                                launch {
                                                    animate(
                                                        offsetY.floatValue, targetFlingY,
                                                        initialVelocity = velocity.y,
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow)
                                                    ) { v, _ -> offsetY.floatValue = v }
                                                }
                                            }
                                        } else if (scale.floatValue in 1.0f..1.05f && (offsetX.floatValue != 0f || offsetY.floatValue != 0f || scale.floatValue != 1f)) {
                                            animJob.value?.cancel()
                                            animJob.value = coroutineScope.launch {
                                                launch {
                                                    animate(scale.floatValue, 1f, animationSpec = MotionTokens.sharedElementSpring()) { v, _ ->
                                                        val c = v.coerceAtLeast(1f)
                                                        scale.floatValue = c
                                                        if (page == pagerState.currentPage) currentScale = c
                                                    }
                                                }
                                                launch { animate(offsetX.floatValue, 0f, animationSpec = MotionTokens.sharedElementSpring()) { v, _ -> offsetX.floatValue = v } }
                                                launch { animate(offsetY.floatValue, 0f, animationSpec = MotionTokens.sharedElementSpring()) { v, _ -> offsetY.floatValue = v } }
                                            }
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    val safeScale = scale.floatValue.coerceAtLeast(0.05f)
                                    scaleX = safeScale
                                    scaleY = safeScale
                                    translationX = offsetX.floatValue
                                    translationY = offsetY.floatValue
                                }
                        )
                    }
                }
            }

            // Top overlay bar
            AnimatedVisibility(
                visible = showUi,
                enter = fadeIn(MotionTokens.fastEffectsSpec()),
                exit = fadeOut(MotionTokens.fastEffectsSpec()),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        FilledTonalIconButton(
                            onClick = onDismiss,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = "Close")
                        }
                        if (items.size > 1) {
                            Surface(
                                shape = ShapeFull,
                                color = Color.Black.copy(alpha = 0.55f),
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${items.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isActionBusy = pendingActionItemId == currentItem.id
                        FilledTonalIconButton(
                            enabled = !isActionBusy,
                            onClick = {
                                pendingActionItemId = currentItem.id
                                onUnhide(currentItem)
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_visibility), contentDescription = "Unhide")
                        }
                        FilledTonalIconButton(
                            enabled = !isActionBusy,
                            onClick = {
                                pendingActionItemId = currentItem.id
                                onDelete(currentItem)
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_delete), contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

// ── Expressive Vault Keypad ───────────────────────────────────────────────────

@Composable
private fun ExpressiveVaultKeypad(
    title: String,
    subtitle: String,
    isSetup: Boolean,
    setupStage: PinSetupStage,
    enteredPin: String,
    showBiometricKey: Boolean,
    shakeOffset: Float,
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometricPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // CookieShape header icon container
        Surface(
            shape = ExpressiveShapes.Cookie12Sided,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(88.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_shield_lock),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true
        )

        if (isSetup) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = if (setupStage == PinSetupStage.ENTER_NEW_PIN) "Step 1 of 2: Create Master PIN" else "Step 2 of 2: Confirm Master PIN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = ShapeLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Your hidden photos and videos stay protected in Private Space on your device and remain available across app updates, data resets, and reinstalls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // PIN Indicator Dots with spring animation & shake
        Row(
            modifier = Modifier
                .offset(x = shakeOffset.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val isFilled = index < enteredPin.length
                val dotScale by animateFloatAsState(
                    targetValue = if (isFilled) 1.25f else 1f,
                    animationSpec = MotionTokens.bouncySpring(),
                    label = "pinDotScale"
                )

                Surface(
                    shape = CircleShape,
                    color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .size(16.dp)
                        .scale(dotScale)
                ) {}
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 3x4 numeric keypad grid with ShapeLargeIncreased items
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                KeypadButton(digit = "1", letters = "", onClick = { onDigitPress("1") })
                KeypadButton(digit = "2", letters = "ABC", onClick = { onDigitPress("2") })
                KeypadButton(digit = "3", letters = "DEF", onClick = { onDigitPress("3") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                KeypadButton(digit = "4", letters = "GHI", onClick = { onDigitPress("4") })
                KeypadButton(digit = "5", letters = "JKL", onClick = { onDigitPress("5") })
                KeypadButton(digit = "6", letters = "MNO", onClick = { onDigitPress("6") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                KeypadButton(digit = "7", letters = "PQRS", onClick = { onDigitPress("7") })
                KeypadButton(digit = "8", letters = "TUV", onClick = { onDigitPress("8") })
                KeypadButton(digit = "9", letters = "WXYZ", onClick = { onDigitPress("9") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (showBiometricKey) {
                    KeypadActionButton(
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_fingerprint),
                        contentDescription = "Biometric Auth",
                        onClick = onBiometricPress
                    )
                } else {
                    Box(modifier = Modifier.size(72.dp))
                }

                KeypadButton(digit = "0", letters = "+", onClick = { onDigitPress("0") })

                KeypadActionButton(
                    icon = ImageVector.vectorResource(R.drawable.ic_ms_backspace),
                    contentDescription = "Backspace",
                    onClick = onBackspace
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    digit: String,
    letters: String = "",
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        shape = ShapeLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 0.dp,
        modifier = Modifier
            .size(72.dp)
            .scale(if (isPressed) 0.92f else 1f)
            .clip(ShapeLargeIncreased)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

    }
}
@Composable
private fun KeypadActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        shape = ShapeLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 0.dp,
        modifier = Modifier
            .size(72.dp)
            .scale(if (isPressed) 0.92f else 1f)
            .clip(ShapeLargeIncreased)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

private fun formatVaultDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun VaultPinVerifyDialog(
    vaultAuthManager: com.inferno.gallery.data.VaultAuthManager,
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val attemptVerify = {
        if (pin.length == 4) {
            val lockout = vaultAuthManager.getRemainingLockoutSeconds(context)
            if (lockout > 0) {
                errorMsg = "Too many attempts. Wait ${lockout}s"
            } else if (vaultAuthManager.verifyPin(context, pin)) {
                onVerified()
            } else {
                val rem = vaultAuthManager.getRemainingLockoutSeconds(context)
                errorMsg = if (rem > 0) "Too many attempts. Wait ${rem}s" else "Incorrect PIN"
                pin = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Private Space PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter your 4-digit Private Space PIN to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            pin = input
                            errorMsg = null
                        }
                    },
                    singleLine = true,
                    label = { Text("4-digit PIN") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { attemptVerify() }
                    ),
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { msg -> { Text(msg) } }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = pin.length == 4,
                onClick = attemptVerify
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun VaultPersistenceBanner(
    onEnableClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = ShapeExtraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Survive Clear Data & Reinstalls",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Grant All Files Access so encrypted Private Space items are backed up to .PhotonPrivateSpace.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onEnableClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                shape = ShapeFull
            ) {
                Text("Enable", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

private fun Modifier.vaultGridPinchZoom(
    gridCellsCount: Int,
    onGridCountChange: (Int) -> Unit,
    isSelectionMode: Boolean,
    haptic: HapticFeedback,
    onPinchScaleChange: (scale: Float, centroid: Offset) -> Unit,
    onPinchEnd: () -> Unit
): Modifier = composed {
    val currentGridCount by rememberUpdatedState(gridCellsCount)
    val currentOnGridCountChange by rememberUpdatedState(onGridCountChange)
    val currentIsSelectionMode by rememberUpdatedState(isSelectionMode)
    val currentOnPinchScaleChange by rememberUpdatedState(onPinchScaleChange)
    val currentOnPinchEnd by rememberUpdatedState(onPinchEnd)

    Modifier.pointerInput(Unit) {
        val touchSlop = viewConfiguration.touchSlop

        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

            if (currentIsSelectionMode) return@awaitEachGesture

            var initialDistance = -1f
            var pinchActive = false
            var pinchEverActivated = false
            var lastChangeTime = 0L

            try {
                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val changes = event.changes

                    if (changes.size < 2 && !pinchEverActivated) {
                        continue
                    }
                    var p1: Offset? = null
                    var p2: Offset? = null
                    var pressedCount = 0

                    for (i in changes.indices) {
                        val c = changes[i]
                        if (c.pressed) {
                            if (pressedCount == 0) p1 = c.position
                            else if (pressedCount == 1) p2 = c.position
                            pressedCount++
                        }
                    }

                    if (pressedCount >= 2 && p1 != null && p2 != null) {
                        val currentDistance = kotlin.math.hypot(p1.x - p2.x, p1.y - p2.y)
                        val currentCentroid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                        if (initialDistance <= 0f) {
                            initialDistance = currentDistance
                        } else {
                            val distanceDiff = kotlin.math.abs(currentDistance - initialDistance)
                            val rawScale = currentDistance / initialDistance

                            if (!pinchActive && distanceDiff > touchSlop * 1.25f && (rawScale > 1.05f || rawScale < 0.95f)) {
                                pinchActive = true
                                pinchEverActivated = true
                            }

                            if (pinchActive) {
                                event.changes.forEach { it.consume() }
                                currentOnPinchScaleChange(rawScale, currentCentroid)

                                val now = System.currentTimeMillis()
                                if (rawScale > 1.20f && now - lastChangeTime > 260L) {
                                    val currentCols = currentGridCount
                                    if (currentCols > 1) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentOnGridCountChange(currentCols - 1)
                                        lastChangeTime = now
                                        initialDistance = currentDistance
                                        currentOnPinchScaleChange(1f, currentCentroid)
                                    }
                                } else if (rawScale < 0.82f && now - lastChangeTime > 260L) {
                                    val currentCols = currentGridCount
                                    if (currentCols < 8) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentOnGridCountChange(currentCols + 1)
                                        lastChangeTime = now
                                        initialDistance = currentDistance
                                        currentOnPinchScaleChange(1f, currentCentroid)
                                    }
                                }
                            }
                        }
                    } else {
                        if (pinchEverActivated) {
                            event.changes.forEach { it.consume() }
                        }
                        pinchActive = false
                        initialDistance = -1f
                    }
                } while (event.changes.any { it.pressed })
            } finally {
                if (pinchEverActivated) {
                    currentOnPinchEnd()
                }
            }
        }
    }
}

private fun Modifier.vaultGridDragSelectGesture(
    lazyGridState: LazyGridState,
    entries: List<VaultGridEntry>,
    headerOffset: Int,
    selectedIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    haptic: HapticFeedback,
    coroutineScope: CoroutineScope
): Modifier = composed {
    val currentEntries by rememberUpdatedState(entries)
    val currentHeaderOffset by rememberUpdatedState(headerOffset)
    val currentSelectedIds by rememberUpdatedState(selectedIds)
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)

    Modifier.pointerInput(lazyGridState) {
        var dragStartIndex = -1
        var lastHitIndex = -1
        var baseSelectedIds = emptySet<Long>()
        var isSelecting = true
        var currentDragPosition: Offset? = null
        var autoScrollJob: Job? = null

        val autoScrollThresholdPx = with(density) { 72.dp.toPx() }
        val maxScrollSpeedPx = with(density) { 24.dp.toPx() }

        fun applySelectionForIndex(targetIndex: Int) {
            if (dragStartIndex == -1 || targetIndex == -1) return
            val adjustedStart = dragStartIndex - currentHeaderOffset
            val adjustedTarget = targetIndex - currentHeaderOffset
            if (adjustedStart < 0 || adjustedTarget < 0) return

            val list = currentEntries
            if (list.isEmpty()) return
            val start = minOf(adjustedStart, adjustedTarget).coerceIn(0, list.lastIndex)
            val end = maxOf(adjustedStart, adjustedTarget).coerceIn(0, list.lastIndex)

            val rangeIds = mutableSetOf<Long>()
            for (i in start..end) {
                val entry = list[i]
                if (entry is VaultGridEntry.Media) {
                    rangeIds.add(entry.entity.id)
                }
            }
            val updated = if (isSelecting) baseSelectedIds + rangeIds else baseSelectedIds - rangeIds
            currentOnSelectionChange(updated)
        }

        fun startAutoScrollIfNeeded() {
            if (autoScrollJob?.isActive == true) return
            autoScrollJob = coroutineScope.launch {
                while (isActive && dragStartIndex != -1) {
                    val pos = currentDragPosition ?: break
                    val viewportHeight = lazyGridState.layoutInfo.viewportSize.height.toFloat()
                    if (viewportHeight <= 0f) break

                    val scrollDelta = when {
                        pos.y < autoScrollThresholdPx -> {
                            val factor = ((autoScrollThresholdPx - pos.y) / autoScrollThresholdPx).coerceIn(0f, 1f)
                            -maxScrollSpeedPx * factor
                        }
                        pos.y > (viewportHeight - autoScrollThresholdPx) -> {
                            val factor = ((pos.y - (viewportHeight - autoScrollThresholdPx)) / autoScrollThresholdPx).coerceIn(0f, 1f)
                            maxScrollSpeedPx * factor
                        }
                        else -> 0f
                    }

                    if (scrollDelta != 0f) {
                        lazyGridState.scrollBy(scrollDelta)
                        val newHit = findVaultGridItemAtOffset(lazyGridState, pos)
                        if (newHit != null && newHit != lastHitIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHitIndex = newHit
                            applySelectionForIndex(newHit)
                        }
                        delay(16L)
                    } else {
                        delay(32L)
                    }
                }
            }
        }

        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                val rawHit = findVaultGridItemAtOffset(lazyGridState, offset) ?: return@detectDragGesturesAfterLongPress
                val hitIdx = rawHit - currentHeaderOffset
                val hitEntry = currentEntries.getOrNull(hitIdx) as? VaultGridEntry.Media ?: return@detectDragGesturesAfterLongPress
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                baseSelectedIds = currentSelectedIds
                isSelecting = !baseSelectedIds.contains(hitEntry.entity.id)
                dragStartIndex = rawHit
                lastHitIndex = rawHit
                currentDragPosition = offset
                applySelectionForIndex(rawHit)
                startAutoScrollIfNeeded()
            },
            onDrag = { change, _ ->
                change.consume()
                val pos = change.position
                currentDragPosition = pos
                val currentHit = findVaultGridItemAtOffset(lazyGridState, pos)
                if (currentHit != null && currentHit != lastHitIndex && dragStartIndex != -1) {
                    if (currentHit - currentHeaderOffset >= 0) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastHitIndex = currentHit
                        applySelectionForIndex(currentHit)
                    }
                }
                startAutoScrollIfNeeded()
            },
            onDragEnd = {
                autoScrollJob?.cancel()
                autoScrollJob = null
                currentDragPosition = null
                dragStartIndex = -1
                lastHitIndex = -1
            },
            onDragCancel = {
                autoScrollJob?.cancel()
                autoScrollJob = null
                currentDragPosition = null
                dragStartIndex = -1
                lastHitIndex = -1
            }
        )
    }
}

private fun findVaultGridItemAtOffset(lazyGridState: LazyGridState, offset: Offset): Int? {
    val layoutInfo = lazyGridState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val adjustedY = offset.y - layoutInfo.beforeContentPadding
    val adjustedX = offset.x

    for (item in visibleItems) {
        val itemOffset = item.offset
        val itemSize = item.size
        if (adjustedX >= itemOffset.x && adjustedX <= itemOffset.x + itemSize.width &&
            adjustedY >= itemOffset.y && adjustedY <= itemOffset.y + itemSize.height
        ) {
            return item.index
        }
    }

    val matchingRow = visibleItems.filter { item ->
        adjustedY >= item.offset.y && adjustedY <= item.offset.y + item.size.height
    }
    if (matchingRow.isNotEmpty()) {
        return matchingRow.minByOrNull { item ->
            val centerX = item.offset.x + item.size.width / 2f
            kotlin.math.abs(adjustedX - centerX)
        }?.index
    }

    return null
}
