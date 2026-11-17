@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.inferno.gallery.R
import com.inferno.gallery.data.AppLockAuthManager
import com.inferno.gallery.ui.theme.ExpressiveShapes
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppLockDialogMode {
    SETUP,
    CHANGE_PIN,
    VERIFY_TO_DISABLE,
    VERIFY_TO_ENABLE_BIOMETRIC,
    VERIFY_TO_DISABLE_BIOMETRIC
}

private enum class AppLockPinStage {
    VERIFY_CURRENT_PIN,
    ENTER_NEW_PIN,
    CONFIRM_NEW_PIN
}

/**
 * Full-screen Material 3 Expressive lock screen blocking access to Photon Gallery
 * until unlocked via custom 4-digit PIN or strong biometric authentication.
 */
@Composable
fun AppLockOverlay(
    authManager: AppLockAuthManager,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(authManager.getRemainingLockoutSeconds(context)) }
    var hasPromptedThisLockSession by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val canUseBiometrics = remember(biometricEnabled, context) {
        biometricEnabled && authManager.canAuthenticateWithBiometrics(context)
    }

    // Pressing Back exits the app instead of dismissing the lock screen
    BackHandler {
        (context as? Activity)?.finish()
    }

    val isLockedOut = lockoutSeconds > 0
    LaunchedEffect(isLockedOut) {
        if (isLockedOut) {
            while (lockoutSeconds > 0) {
                delay(500L)
                lockoutSeconds = authManager.getRemainingLockoutSeconds(context)
            }
            errorMessage = null
        }
    }

    fun triggerBiometricIfReady() {
        if (canUseBiometrics && activity != null && !hasPromptedThisLockSession &&
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
            hasPromptedThisLockSession = true
            authManager.authenticateWithBiometrics(
                activity = activity,
                onSuccess = {
                    enteredPin = ""
                    errorMessage = null
                    onUnlocked()
                },
                onNegativeButton = {},
                onFailure = {}
            )
        }
    }

    // Trigger biometrics only once when RESUMED, and re-arm on ON_STOP
    LaunchedEffect(canUseBiometrics) {
        triggerBiometricIfReady()
    }

    DisposableEffect(lifecycleOwner, canUseBiometrics) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> triggerBiometricIfReady()
                Lifecycle.Event.ON_STOP -> hasPromptedThisLockSession = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val activeSubtitle = when {
        lockoutSeconds > 0 -> "Too many attempts. Try again in ${lockoutSeconds}s"
        errorMessage != null -> errorMessage!!
        canUseBiometrics -> "Enter your 4-digit PIN or tap fingerprint"
        else -> "Enter your 4-digit PIN to unlock"
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {},
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            AppLockKeypadBody(
                title = "Photon Gallery Locked",
                subtitle = activeSubtitle,
                isErrorSubtitle = errorMessage != null || lockoutSeconds > 0,
                stepLabel = null,
                enteredPin = enteredPin,
                showBiometricKey = canUseBiometrics,
                shakeOffset = shakeOffset.value,
                onDigitPress = { digit ->
                    if (lockoutSeconds > 0) return@AppLockKeypadBody
                    if (enteredPin.length < 4) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        errorMessage = null
                        val newPin = enteredPin + digit
                        enteredPin = newPin

                        if (newPin.length == 4) {
                            val currentLockout = authManager.getRemainingLockoutSeconds(context)
                            if (currentLockout > 0) {
                                lockoutSeconds = currentLockout
                                enteredPin = ""
                            } else if (authManager.verifyPin(context, newPin)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                enteredPin = ""
                                errorMessage = null
                                onUnlocked()
                            } else {
                                coroutineScope.launch {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    shakeOffset.animateTo(20f, spring(stiffness = Spring.StiffnessHigh))
                                    shakeOffset.animateTo(-20f, spring(stiffness = Spring.StiffnessHigh))
                                    shakeOffset.animateTo(10f, spring(stiffness = Spring.StiffnessHigh))
                                    shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessHigh))
                                    enteredPin = ""
                                    val newLockout = authManager.getRemainingLockoutSeconds(context)
                                    if (newLockout > 0) {
                                        lockoutSeconds = newLockout
                                    } else {
                                        errorMessage = "Incorrect PIN"
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
                        authManager.authenticateWithBiometrics(
                            activity = it,
                            negativeButtonText = "Cancel",
                            onSuccess = {
                                enteredPin = ""
                                errorMessage = null
                                onUnlocked()
                            },
                            onNegativeButton = {},
                            onFailure = {}
                        )
                    }
                }
            )
        }
    }
}

/**
 * Full-screen dialog handling App Lock PIN setup, PIN change, and verification before disabling.
 */
@Composable
fun AppLockPinFlowDialog(
    mode: AppLockDialogMode,
    authManager: AppLockAuthManager,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var stage by remember(mode) {
        mutableStateOf(
            when (mode) {
                AppLockDialogMode.SETUP -> AppLockPinStage.ENTER_NEW_PIN
                AppLockDialogMode.CHANGE_PIN,
                AppLockDialogMode.VERIFY_TO_DISABLE,
                AppLockDialogMode.VERIFY_TO_ENABLE_BIOMETRIC,
                AppLockDialogMode.VERIFY_TO_DISABLE_BIOMETRIC -> AppLockPinStage.VERIFY_CURRENT_PIN
            }
        )
    }
    var enteredPin by remember { mutableStateOf("") }
    var firstEnteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(authManager.getRemainingLockoutSeconds(context)) }
    val shakeOffset = remember { Animatable(0f) }

    val isLockedOut = lockoutSeconds > 0
    LaunchedEffect(isLockedOut) {
        if (isLockedOut) {
            while (lockoutSeconds > 0) {
                delay(500L)
                lockoutSeconds = authManager.getRemainingLockoutSeconds(context)
            }
            errorMessage = null
        }
    }

    suspend fun triggerShake(message: String?) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        shakeOffset.animateTo(20f, spring(stiffness = Spring.StiffnessHigh))
        shakeOffset.animateTo(-20f, spring(stiffness = Spring.StiffnessHigh))
        shakeOffset.animateTo(10f, spring(stiffness = Spring.StiffnessHigh))
        shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessHigh))
        enteredPin = ""
        errorMessage = message
    }

    val title = when (stage) {
        AppLockPinStage.VERIFY_CURRENT_PIN -> when (mode) {
            AppLockDialogMode.VERIFY_TO_DISABLE -> "Disable App Lock"
            AppLockDialogMode.VERIFY_TO_ENABLE_BIOMETRIC -> "Enable Biometric Unlock"
            AppLockDialogMode.VERIFY_TO_DISABLE_BIOMETRIC -> "Disable Biometric Unlock"
            else -> "Verify Current PIN"
        }
        AppLockPinStage.ENTER_NEW_PIN -> if (mode == AppLockDialogMode.CHANGE_PIN) "Enter New PIN" else "Create App Lock PIN"
        AppLockPinStage.CONFIRM_NEW_PIN -> "Confirm Your PIN"
    }

    val isLockedOutInVerifyStage = stage == AppLockPinStage.VERIFY_CURRENT_PIN && lockoutSeconds > 0
    val subtitle = when {
        isLockedOutInVerifyStage -> "Too many attempts. Try again in ${lockoutSeconds}s"
        errorMessage != null -> errorMessage!!
        stage == AppLockPinStage.VERIFY_CURRENT_PIN -> "Enter your current 4-digit App Lock PIN"
        stage == AppLockPinStage.ENTER_NEW_PIN -> "Choose a 4-digit PIN to protect Photon Gallery"
        else -> "Re-enter your 4-digit PIN to confirm"
    }

    val stepLabel = when (stage) {
        AppLockPinStage.ENTER_NEW_PIN -> "Step 1 of 2: Enter PIN"
        AppLockPinStage.CONFIRM_NEW_PIN -> "Step 2 of 2: Confirm PIN"
        AppLockPinStage.VERIFY_CURRENT_PIN -> null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Back / Cancel pill
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = onDismiss,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                                contentDescription = "Cancel"
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AppLockKeypadBody(
                        title = title,
                        subtitle = subtitle,
                        isErrorSubtitle = errorMessage != null || isLockedOutInVerifyStage,
                        stepLabel = stepLabel,
                        enteredPin = enteredPin,
                        showBiometricKey = false,
                        shakeOffset = shakeOffset.value,
                        onDigitPress = { digit ->
                            if (isLockedOutInVerifyStage) return@AppLockKeypadBody
                            if (enteredPin.length < 4) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                errorMessage = null
                                val newPin = enteredPin + digit
                                enteredPin = newPin

                                if (newPin.length == 4) {
                                    when (stage) {
                                        AppLockPinStage.VERIFY_CURRENT_PIN -> {
                                            val currentLockout = authManager.getRemainingLockoutSeconds(context)
                                            if (currentLockout > 0) {
                                                lockoutSeconds = currentLockout
                                                enteredPin = ""
                                            } else if (authManager.verifyPin(context, newPin)) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                enteredPin = ""
                                                if (mode != AppLockDialogMode.CHANGE_PIN) {
                                                    onSuccess()
                                                } else {
                                                    stage = AppLockPinStage.ENTER_NEW_PIN
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    val newLockout = authManager.getRemainingLockoutSeconds(context)
                                                    if (newLockout > 0) {
                                                        lockoutSeconds = newLockout
                                                        triggerShake(null)
                                                    } else {
                                                        triggerShake("Incorrect PIN")
                                                    }
                                                }
                                            }
                                        }

                                        AppLockPinStage.ENTER_NEW_PIN -> {
                                            firstEnteredPin = newPin
                                            enteredPin = ""
                                            stage = AppLockPinStage.CONFIRM_NEW_PIN
                                        }

                                        AppLockPinStage.CONFIRM_NEW_PIN -> {
                                            if (newPin == firstEnteredPin) {
                                                authManager.savePin(context, newPin)
                                                enteredPin = ""
                                                Toast.makeText(context, "App Lock PIN saved", Toast.LENGTH_SHORT).show()
                                                onSuccess()
                                            } else {
                                                coroutineScope.launch {
                                                    firstEnteredPin = ""
                                                    stage = AppLockPinStage.ENTER_NEW_PIN
                                                    triggerShake("PINs did not match, please try again")
                                                }
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
                        onBiometricPress = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLockKeypadBody(
    title: String,
    subtitle: String,
    isErrorSubtitle: Boolean,
    stepLabel: String?,
    enteredPin: String,
    showBiometricKey: Boolean,
    shakeOffset: Float,
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometricPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
            color = if (isErrorSubtitle) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )

        if (stepLabel != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

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
                    label = "appLockPinDotScale"
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

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AppLockKeypadDigit(digit = "1", letters = "", onClick = { onDigitPress("1") })
                AppLockKeypadDigit(digit = "2", letters = "ABC", onClick = { onDigitPress("2") })
                AppLockKeypadDigit(digit = "3", letters = "DEF", onClick = { onDigitPress("3") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AppLockKeypadDigit(digit = "4", letters = "GHI", onClick = { onDigitPress("4") })
                AppLockKeypadDigit(digit = "5", letters = "JKL", onClick = { onDigitPress("5") })
                AppLockKeypadDigit(digit = "6", letters = "MNO", onClick = { onDigitPress("6") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AppLockKeypadDigit(digit = "7", letters = "PQRS", onClick = { onDigitPress("7") })
                AppLockKeypadDigit(digit = "8", letters = "TUV", onClick = { onDigitPress("8") })
                AppLockKeypadDigit(digit = "9", letters = "WXYZ", onClick = { onDigitPress("9") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (showBiometricKey) {
                    AppLockKeypadAction(
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_fingerprint),
                        contentDescription = "Biometric Unlock",
                        onClick = onBiometricPress
                    )
                } else {
                    Box(modifier = Modifier.size(72.dp))
                }

                AppLockKeypadDigit(digit = "0", letters = "+", onClick = { onDigitPress("0") })

                AppLockKeypadAction(
                    icon = ImageVector.vectorResource(R.drawable.ic_ms_backspace),
                    contentDescription = "Backspace",
                    onClick = onBackspace
                )
            }
        }
    }
}

@Composable
private fun AppLockKeypadDigit(
    digit: String,
    letters: String,
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
private fun AppLockKeypadAction(
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
