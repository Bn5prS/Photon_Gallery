@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.inferno.gallery.ui.vault

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeSmall
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R


private enum class PinSetupStage {
    ENTER_NEW_PIN,
    CONFIRM_NEW_PIN
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
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
                                    } else {
                                        viewModel.vaultAuthManager.lock()
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

                            if (isUnlocked && !isSelectionMode) {
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
                                            text = { Text("Lock Vault Now", fontWeight = FontWeight.Medium) },
                                            leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_lock), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                showOptionsMenu = false
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.vaultAuthManager.lock()
                                                enteredPin = ""
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Change Master PIN", fontWeight = FontWeight.Medium) },
                                            leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_key), contentDescription = null) },
                                            onClick = {
                                                showOptionsMenu = false
                                                viewModel.vaultAuthManager.lock()
                                                isChangingPinMode = true
                                                isPinConfigured = false
                                                setupStage = PinSetupStage.ENTER_NEW_PIN
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
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Unhide Action
                                FilledTonalButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.unhideMedia(selectedIds.toList())
                                        selectedIds = emptySet()
                                        Toast.makeText(context, "Restored to gallery", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = ShapeFull,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_visibility), contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Unhide (${selectedIds.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                // Delete Permanently Action
                                FilledTonalButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.deleteFromVault(selectedIds.toList())
                                        selectedIds = emptySet()
                                        Toast.makeText(context, "Deleted permanently", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = ShapeFull,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_delete), contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Delete",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
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
                        val isSetup = !isPinConfigured

                        val headerTitle = when {
                            !isPinConfigured && setupStage == PinSetupStage.ENTER_NEW_PIN -> "Create Vault PIN"
                            !isPinConfigured && setupStage == PinSetupStage.CONFIRM_NEW_PIN -> "Confirm Your PIN"
                            else -> "Private Space"
                        }

                        val headerSubtitle = when {
                            !isPinConfigured && setupStage == PinSetupStage.ENTER_NEW_PIN -> "Enter a 4-digit Master PIN to protect hidden photos"
                            !isPinConfigured && setupStage == PinSetupStage.CONFIRM_NEW_PIN -> "Re-enter your 4-digit PIN to confirm"
                            else -> "Enter your 4-digit PIN or tap fingerprint"
                        }

                        ExpressiveVaultKeypad(
                            title = headerTitle,
                            subtitle = headerSubtitle,
                            enteredPin = enteredPin,
                            showBiometricKey = !isSetup,
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
                                                    viewModel.vaultAuthManager.unlock()
                                                    enteredPin = ""
                                                    Toast.makeText(context, "Vault PIN created successfully!", Toast.LENGTH_SHORT).show()
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
                                            // Handle Existing PIN Verification Flow
                                            if (viewModel.vaultAuthManager.verifyPin(context, newPin)) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.vaultAuthManager.unlock()
                                                enteredPin = ""
                                            } else {
                                                // Incorrect PIN shake
                                                coroutineScope.launch {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    shakeOffset.animateTo(20f, spring(stiffness = Spring.StiffnessHigh))
                                                    shakeOffset.animateTo(-20f, spring(stiffness = Spring.StiffnessHigh))
                                                    shakeOffset.animateTo(10f, spring(stiffness = Spring.StiffnessHigh))
                                                    shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessHigh))
                                                    enteredPin = ""
                                                    Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
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
                                        onSuccess = { enteredPin = "" },
                                        onFailure = {}
                                    )
                                }
                            }
                        )
                    } else if (vaultItems.isEmpty()) {
                        // ── Empty State ──────────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(96.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        ImageVector.vectorResource(R.drawable.ic_ms_shield_lock),
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Private Space is Empty",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Select photos in your gallery and tap \"Hide\" to move them to this encrypted space.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        // ── Encrypted Media Grid ──────────────────────────────
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                items = vaultItems,
                                key = { it.id }
                            ) { item ->
                                val isSelected = selectedIds.contains(item.id)
                                val vaultUri = remember(item.vaultFileName) {
                                    viewModel.getVaultFileUri(item)
                                }

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(ShapeLarge)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = ShapeLarge
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                if (isSelectionMode) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                            }
                                        )
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(vaultUri)
                                            .size(384)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = item.fileName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

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

                                    // Video Duration Pill
                                    if (item.isVideo && item.durationMs != null) {
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp),
                                            shape = ShapeFull,
                                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = formatVaultDuration(item.durationMs),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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

// ── Expressive Vault Keypad ───────────────────────────────────────────────────

@Composable
private fun ExpressiveVaultKeypad(
    title: String,
    subtitle: String,
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
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Shield Icon Header
        Surface(
            shape = ShapeFull,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_lock),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true
        )

        Spacer(modifier = Modifier.height(28.dp))

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

        Spacer(modifier = Modifier.height(32.dp))

        // 3x4 Keypad Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                KeypadButton(digit = "1", onClick = { onDigitPress("1") })
                KeypadButton(digit = "2", onClick = { onDigitPress("2") })
                KeypadButton(digit = "3", onClick = { onDigitPress("3") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                KeypadButton(digit = "4", onClick = { onDigitPress("4") })
                KeypadButton(digit = "5", onClick = { onDigitPress("5") })
                KeypadButton(digit = "6", onClick = { onDigitPress("6") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                KeypadButton(digit = "7", onClick = { onDigitPress("7") })
                KeypadButton(digit = "8", onClick = { onDigitPress("8") })
                KeypadButton(digit = "9", onClick = { onDigitPress("9") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (showBiometricKey) {
                    // Biometrics Action
                    KeypadActionButton(
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_fingerprint),
                        contentDescription = "Biometric Auth",
                        onClick = onBiometricPress
                    )
                } else {
                    Box(modifier = Modifier.size(72.dp))
                }

                KeypadButton(digit = "0", onClick = { onDigitPress("0") })

                // Backspace Action
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
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 0.dp,
        modifier = Modifier
            .size(72.dp)
            .scale(if (isPressed) 0.92f else 1f)
            .clip(ShapeLarge)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                softWrap = false
            )
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
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 0.dp,
        modifier = Modifier
            .size(72.dp)
            .scale(if (isPressed) 0.92f else 1f)
            .clip(ShapeLarge)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
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
