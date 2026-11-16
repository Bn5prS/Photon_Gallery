@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.inferno.gallery.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlin.random.Random
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import com.inferno.gallery.ui.theme.WavyProgressTokens
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import com.inferno.gallery.BuildConfig
import com.inferno.gallery.R
import com.inferno.gallery.ui.components.FloatingGalleryBackground
import com.inferno.gallery.ui.components.LanguageSelectionDialog
import com.inferno.gallery.ui.components.OnboardingLanguagePill
import com.inferno.gallery.ui.components.BigOnboardingLanguagePill
import com.inferno.gallery.ui.components.getCurrentAppLanguageTag
import com.inferno.gallery.ui.components.setAppLanguage
import com.inferno.gallery.ui.components.FourQuadrantColorSwatch
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.OfficialM3Shapes
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.theme.ThemePalettes
import com.inferno.gallery.ui.utils.pressScale
import java.util.Locale

/**
 * Checks whether storage/media permissions required by the current Android SDK level are granted.
 */
fun hasStoragePermissions(context: Context): Boolean {
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
        val partialMedia = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        } else false
        (readImages && readVideos) || readImages || partialMedia
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Utility to format byte counts into human-readable MB strings.
 */
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}

/**
 * Utility to format network transfer speeds into human-readable strings.
 */
private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0L) return "Calculating…"
    val mbPerSec = bytesPerSec.toDouble() / (1024.0 * 1024.0)
    return if (mbPerSec >= 1.0) {
        String.format(Locale.US, "%.1f MB/s", mbPerSec)
    } else {
        val kbPerSec = bytesPerSec.toDouble() / 1024.0
        String.format(Locale.US, "%.0f KB/s", kbPerSec)
    }
}

/**
 * Expressive Material 3 four-step stadium indicator with spring morphing animation.
 */
@Composable
private fun OnboardingStepIndicator(
    currentStep: Int,
    totalSteps: Int = 4,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isSelected = i == currentStep
            val isPast = i < currentStep

            val width by animateDpAsState(
                targetValue = if (isSelected) 28.dp else 8.dp,
                animationSpec = MotionTokens.gentleSpring(),
                label = "StepIndicatorWidth_$i"
            )

            val color by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isPast -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                },
                animationSpec = MotionTokens.fastEffectsSpec(),
                label = "StepIndicatorColor_$i"
            )

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(ShapeFull)
                    .background(color)
            )
        }
    }
}

/**
 * Photon Gallery Material 3 Expressive Setup Wizard.
 *
 * 4 Guided Expressive Steps with Back & Next navigation dock:
 * - Step 0: Welcome, Cinematic App Intro & Big Pill Language Selector
 * - Step 1: Privacy Guarantee & Granular Media & Storage Permissions
 * - Step 2: Look & Feel Customization with Interactive Live Theme Viewport
 * - Step 3: On-Device AI Setup (Smart Search & Face Recognition) with Live Speed & Model Size Metrics
 */
@Composable
fun PermissionOnboardingScreen(
    photosGranted: Boolean,
    videosGranted: Boolean,
    allFilesGranted: Boolean,
    onGrantMediaClick: () -> Unit,
    onGrantAllFilesClick: () -> Unit,
    onContinueClick: () -> Unit,
    settingsViewModel: SettingsViewModel? = null,
    initialStep: Int = 0,
    modifier: Modifier = Modifier
) {
    var currentStep by rememberSaveable { mutableIntStateOf(initialStep) }
    var previousStep by rememberSaveable { mutableIntStateOf(initialStep) }

    BackHandler(enabled = currentStep > 0) {
        previousStep = currentStep
        currentStep -= 1
    }

    val resolvedSettingsViewModel: SettingsViewModel? = settingsViewModel ?: runCatching {
        val context = LocalContext.current
        if (context is androidx.lifecycle.ViewModelStoreOwner) {
            androidx.lifecycle.viewmodel.compose.viewModel<SettingsViewModel>(viewModelStoreOwner = context)
        } else {
            null
        }
    }.getOrNull()

    val allGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        photosGranted && videosGranted && allFilesGranted
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        photosGranted && allFilesGranted
    } else {
        photosGranted
    }

    val waveTimeState = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    waveTimeState.floatValue += dt
                }
                lastNanos = frameNanos
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        FloatingGalleryBackground(
            modifier = Modifier.fillMaxSize()
        ) {
            CompositionLocalProvider(LocalOnboardingWaveTime provides waveTimeState) {
            Box(modifier = Modifier.fillMaxSize()) {
                OnboardingCornerWatermark(
                    currentStep = currentStep,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                // Pinned 4-Step Stadium Progress Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SpacingTokens.M, bottom = SpacingTokens.XS),
                    contentAlignment = Alignment.Center
                ) {
                    OnboardingStepIndicator(currentStep = currentStep, totalSteps = 4)
                }

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(
                            durationMillis = MotionTokens.Durations.Long,
                            easing = MotionTokens.EmphasizedEasing
                        )
                        val fadeInSpec = tween<Float>(
                            durationMillis = MotionTokens.Durations.Medium,
                            delayMillis = MotionTokens.Durations.Short,
                            easing = MotionTokens.EmphasizedDecelerateEasing
                        )
                        val fadeOutSpec = tween<Float>(
                            durationMillis = MotionTokens.Durations.Short,
                            easing = MotionTokens.EmphasizedAccelerateEasing
                        )
                        if (forward) {
                            (slideInHorizontally(
                                animationSpec = slideSpec,
                                initialOffsetX = { fullWidth -> (fullWidth * 0.28f).toInt() }
                            ) + fadeIn(animationSpec = fadeInSpec)).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = slideSpec,
                                    targetOffsetX = { fullWidth -> -(fullWidth * 0.28f).toInt() }
                                ) + fadeOut(animationSpec = fadeOutSpec)
                            ).apply {
                                targetContentZIndex = 1f
                            }
                        } else {
                            (slideInHorizontally(
                                animationSpec = slideSpec,
                                initialOffsetX = { fullWidth -> -(fullWidth * 0.28f).toInt() }
                            ) + fadeIn(animationSpec = fadeInSpec)).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = slideSpec,
                                    targetOffsetX = { fullWidth -> (fullWidth * 0.28f).toInt() }
                                ) + fadeOut(animationSpec = fadeOutSpec)
                            ).apply {
                                targetContentZIndex = 0f
                            }
                        }
                    },
                    label = "OnboardingStepTransition",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { step ->
                when (step) {
                    0 -> SetupIntroPage(
                        onContinue = {
                            previousStep = 0
                            currentStep = 1
                        }
                    )
                    1 -> SetupPermissionsPage(
                        photosGranted = photosGranted,
                        videosGranted = videosGranted,
                        allFilesGranted = allFilesGranted,
                        allGranted = allGranted,
                        onGrantMediaClick = onGrantMediaClick,
                        onGrantAllFilesClick = onGrantAllFilesClick,
                        onBack = {
                            previousStep = 1
                            currentStep = 0
                        },
                        onContinue = {
                            previousStep = 1
                            currentStep = 2
                        }
                    )
                    2 -> SetupLooksFeelPage(
                        settingsViewModel = resolvedSettingsViewModel,
                        onBack = {
                            previousStep = 2
                            currentStep = 1
                        },
                        onContinue = {
                            previousStep = 2
                            currentStep = 3
                        }
                    )
                    3 -> SetupAiPage(
                        settingsViewModel = resolvedSettingsViewModel,
                        onBack = {
                            previousStep = 3
                            currentStep = 2
                        },
                        onFinish = onContinueClick
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
 * Half-cropped, softly fading Material Symbol watermark in the top-right corner for Steps 1–3.
 * Step 0 stays clean so the centered app emblem owns the welcome screen.
 */
@Composable
private fun OnboardingCornerWatermark(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CornerWatermarkFloat")
    val driftY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WatermarkDriftY"
    )
    val swayDeg by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WatermarkSway"
    )
    val watermarkTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.085f)

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            val forward = targetState > initialState
            (slideInHorizontally(
                animationSpec = tween(MotionTokens.Durations.Long, easing = MotionTokens.EmphasizedEasing),
                initialOffsetX = { fullWidth -> (fullWidth * if (forward) 0.18f else -0.18f).toInt() }
            ) + fadeIn(
                animationSpec = tween(MotionTokens.Durations.Medium, delayMillis = MotionTokens.Durations.Short, easing = MotionTokens.EmphasizedDecelerateEasing)
            )).togetherWith(
                slideOutHorizontally(
                    animationSpec = tween(MotionTokens.Durations.Medium, easing = MotionTokens.EmphasizedEasing),
                    targetOffsetX = { fullWidth -> (fullWidth * if (forward) -0.18f else 0.18f).toInt() }
                ) + fadeOut(
                    animationSpec = tween(MotionTokens.Durations.Short, easing = MotionTokens.EmphasizedAccelerateEasing)
                )
            )
        },
        label = "CornerWatermarkTransition",
        modifier = modifier
    ) { step ->
        val iconRes: Int? = when (step) {
            1 -> R.drawable.ic_ms_shield_lock
            2 -> R.drawable.ic_ms_palette
            3 -> R.drawable.ic_ms_search_sparkle
            else -> null
        }
        if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = watermarkTint,
                modifier = Modifier
                    .size(240.dp)
                    .offset(x = 64.dp, y = (-32 + driftY).dp)
                    .graphicsLayer {
                        rotationZ = swayDeg
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(
                                    size.width * 0.72f,
                                    size.height * 0.28f
                                ),
                                radius = size.minDimension * 0.78f
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
        } else {
            Spacer(modifier = Modifier.size(1.dp))
        }
    }
}

/**
 * Segmented corner shape matching the full-screen viewer toolbar pill:
 * large outer corners (20.dp) on first/last items, tight 8.dp inner corners between adjacent tiles.
 */
private fun onboardingSegmentedTileShape(
    index: Int,
    total: Int,
    outerRadius: Dp = 20.dp,
    innerRadius: Dp = 8.dp
): RoundedCornerShape = when {
    total <= 1 -> RoundedCornerShape(outerRadius)
    index == 0 -> RoundedCornerShape(
        topStart = outerRadius,
        topEnd = outerRadius,
        bottomStart = innerRadius,
        bottomEnd = innerRadius
    )
    index == total - 1 -> RoundedCornerShape(
        topStart = innerRadius,
        topEnd = innerRadius,
        bottomStart = outerRadius,
        bottomEnd = outerRadius
    )
    else -> RoundedCornerShape(innerRadius)
}

private val LocalOnboardingWaveTime = compositionLocalOf<State<Float>> { mutableFloatStateOf(0f) }

/**
 * Translucent outer tray surface for two-layer onboarding cards so the animated
 * liquid background waves subtly shine through without muddying the inner tiles.
 */
@Composable
private fun onboardingOuterTrayColor(): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (isDark) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.52f)
    } else {
        lerp(Color.White, MaterialTheme.colorScheme.surfaceContainerHigh, 0.42f).copy(alpha = 0.50f)
    }
}

/**
 * Two-layer onboarding card tray with an organic "Caustic Pool" color drift
 * synced to the background wave clock, visible through the translucent tray & tile gaps.
 */
@Composable
private fun OnboardingCausticTray(
    modifier: Modifier = Modifier,
    trayColor: Color = onboardingOuterTrayColor(),
    phaseOffset: Float = 0f,
    content: @Composable ColumnScope.() -> Unit
) {
    val waveTime = LocalOnboardingWaveTime.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val primaryTint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.28f else 0.42f)
    val tertiaryTint = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isDark) 0.14f else 0.24f)

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = trayColor,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val t = waveTime.value * 0.55f + phaseOffset
                    val cx = size.width * (0.5f + 0.42f * sin(t * 1.31f + cos(t * 0.63f)))
                    val cy = size.height * (0.5f + 0.38f * cos(t * 0.97f + 0.9f))
                    val radius = max(size.width, size.height) * 0.68f
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryTint, tertiaryTint, Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                            radius = radius
                        )
                    )
                }
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

/**
 * Soft, eye-pleasing translucent inner tile surface for the two-layer onboarding card tray.
 * Uses soft porcelain white (white softened with a subtle surface tint) in light mode,
 * and lifted surfaceContainerHighest in dark mode.
 */
@Composable
private fun onboardingInnerTileColor(): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.74f)
    } else {
        lerp(Color.White, MaterialTheme.colorScheme.surfaceContainer, 0.18f).copy(alpha = 0.90f)
    }
}

/**
 * Unified Back and Next navigation dock replacing legacy single full-width button.
 */
@Composable
private fun OnboardingNavigationDock(
    onNext: () -> Unit,
    nextText: String,
    onBack: (() -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") nextIconRes: Int? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            FilledTonalButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                },
                shape = ShapeFull,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .pressScale()
            ) {
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNext()
            },
            shape = ShapeFull,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .height(76.dp)
                .pressScale()
        ) {
            Text(
                text = nextText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Step 0: App Intro Screen featuring the Floating Gallery Background,
 * Dynamic Themed Circular App Icon, App Title & Tagline, and a Big Pill Language Selector.
 */
@Composable
private fun SetupIntroPage(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguageTag by remember { mutableStateOf(getCurrentAppLanguageTag(context)) }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguageTag = currentLanguageTag,
            onSelectLanguage = { tag ->
                setAppLanguage(context, tag)
                currentLanguageTag = tag
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.XL, vertical = SpacingTokens.L)
    ) {
        val contentMinHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = contentMinHeight),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top: Hero + Minimal Feature List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SpacingTokens.M),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ExpressiveOnboardingHero(
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(SpacingTokens.XL))

                    val introHighlights = listOf(
                        R.drawable.ic_ms_check_circle to "Open-source & ad-free",
                        R.drawable.ic_ms_shield_lock to "Zero cloud uploads",
                        R.drawable.ic_ms_search to "Smart on-device search & sorting",
                        R.drawable.ic_ms_auto_fix_high to "Built-in editor & collage tools"
                    )

                    val innerTileColor = onboardingInnerTileColor()
                    OnboardingCausticTray(
                        phaseOffset = 0.0f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        introHighlights.forEachIndexed { index, (iconRes, label) ->
                            Surface(
                                shape = onboardingSegmentedTileShape(index, introHighlights.size),
                                color = innerTileColor,
                                shadowElevation = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(iconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(SpacingTokens.L))

                // Bottom: Big Pill Language Badge + "Get Started" Primary Action Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SpacingTokens.S),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                ) {
                    BigOnboardingLanguagePill(
                        currentLanguageTag = currentLanguageTag,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showLanguageDialog = true
                        }
                    )

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onContinue()
                        },
                        shape = ShapeFull,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .pressScale()
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_action_get_started),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 1: Media & Storage Permissions Screen with Privacy Guarantee and Granular Permission Cards.
 */
@Composable
private fun SetupPermissionsPage(
    photosGranted: Boolean,
    videosGranted: Boolean,
    allFilesGranted: Boolean,
    allGranted: Boolean,
    onGrantMediaClick: () -> Unit,
    onGrantAllFilesClick: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val mediaGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        photosGranted && videosGranted
    } else {
        photosGranted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.XL, vertical = SpacingTokens.M),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val contentMinHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = contentMinHeight),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(SpacingTokens.M))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_shield_lock),
                                contentDescription = "Permissions",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(SpacingTokens.M))

                    Text(
                        text = stringResource(R.string.onboarding_permissions_header),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(SpacingTokens.XS))

                    Text(
                        text = stringResource(R.string.onboarding_permissions_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(SpacingTokens.XL))

                    // Two-layer segmented tonal Permission Tray (matches viewer toolbar pill)
                    val showAllFilesCard = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    val permTotal = if (showAllFilesCard) 2 else 1
                    val permTileColor = onboardingInnerTileColor()
                    OnboardingCausticTray(
                        phaseOffset = 1.4f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PermissionBlockCard(
                                title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    stringResource(R.string.onboarding_perm_photos_videos_title)
                                } else {
                                    stringResource(R.string.onboarding_perm_storage_title)
                                },
                                subtitle = stringResource(R.string.onboarding_perm_photos_videos_desc),
                                isGranted = mediaGranted,
                                icon = ImageVector.vectorResource(R.drawable.ic_ms_photo_album),
                                iconShape = RoundedCornerShape(14.dp),
                                containerColor = permTileColor,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                cardShape = onboardingSegmentedTileShape(0, permTotal),
                                onClick = onGrantMediaClick
                            )

                            if (showAllFilesCard) {
                                PermissionBlockCard(
                                    title = stringResource(R.string.onboarding_perm_all_files_title),
                                    subtitle = stringResource(R.string.onboarding_perm_all_files_desc),
                                    isGranted = allFilesGranted,
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                    iconShape = RoundedCornerShape(14.dp),
                                    containerColor = permTileColor,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    cardShape = onboardingSegmentedTileShape(1, permTotal),
                                    onClick = onGrantAllFilesClick
                                )
                            }
                    }

                    Spacer(modifier = Modifier.height(SpacingTokens.M))
                }
            }
        }

        // Bottom Action Dock: Back & (Grant or Next) Buttons
        OnboardingNavigationDock(
            onBack = onBack,
            onNext = {
                if (mediaGranted) {
                    onContinue()
                } else {
                    onGrantMediaClick()
                }
            },
            nextText = if (mediaGranted) {
                stringResource(R.string.onboarding_action_next)
            } else {
                stringResource(R.string.onboarding_action_grant)
            },
            nextIconRes = if (mediaGranted) R.drawable.ic_ms_chevron_right else null
        )
    }
}

/**
 * Material 3 Expressive Onboarding Hero.
 * Features a circular dynamic themed app icon and refined typography.
 */
@Composable
private fun ExpressiveOnboardingHero(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Single clean M3 Expressive app icon (no layered/rotated card stack)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 0.dp,
            modifier = Modifier.size(104.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = "Photon Gallery",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(128.dp)
                        .graphicsLayer(scaleX = 1.55f, scaleY = 1.55f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_to).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(SpacingTokens.XS))

        Text(
            text = "Photon Gallery",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(SpacingTokens.M))

        Text(
            text = stringResource(R.string.onboarding_welcome_tagline),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = SpacingTokens.M)
        )
    }
}


private data class ShapeGridItem(
    val shape: Shape,
    @param:DrawableRes val iconRes: Int,
    val isSpotlight: Boolean = false,
    val iconOffset: androidx.compose.ui.unit.Dp = 0.dp
)

/**
 * Material 3 Expressive Gallery 3x3 Shape Grid.
 * Displays all 9 official shapes at uniform size, each hosting a gallery feature icon,
 * with the center Cookie12 shape spotlighting the Photos/Image icon.
 */
@Composable
private fun ExpressiveGallerySymbolsCollage(
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            // Row 0: Camera, Videos, Albums
            listOf(
                ShapeGridItem(OfficialM3Shapes.Arch, R.drawable.ic_ms_photo_camera),
                ShapeGridItem(OfficialM3Shapes.Bun, R.drawable.ic_ms_movie),
                ShapeGridItem(OfficialM3Shapes.Clover4Leaf, R.drawable.ic_ms_photo_album)
            ),
            // Row 1: Favorites, Spotlight Image, Magic AI Enhance
            listOf(
                ShapeGridItem(OfficialM3Shapes.Cookie7Sided, R.drawable.ic_ms_favorite),
                ShapeGridItem(OfficialM3Shapes.Cookie12Sided, R.drawable.ic_ms_image, isSpotlight = true),
                ShapeGridItem(OfficialM3Shapes.Fan, R.drawable.ic_ms_auto_fix_high)
            ),
            // Row 2: Highlights, Private Vault, Smart Search
            listOf(
                ShapeGridItem(OfficialM3Shapes.Cookie4Sided, R.drawable.ic_ms_star),
                ShapeGridItem(OfficialM3Shapes.Ghostish, R.drawable.ic_ms_shield_lock),
                ShapeGridItem(OfficialM3Shapes.Triangle, R.drawable.ic_ms_search, iconOffset = 3.dp)
            )
        )
    }

    val tileSize = 62.dp
    val spacing = SpacingTokens.M

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SpacingTokens.S),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            items.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowItems.forEach { item ->
                        val containerColor = if (item.isSpotlight) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                        val contentColor = if (item.isSpotlight) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val iconSize = if (item.isSpotlight) 32.dp else if (item.iconOffset > 0.dp) 22.dp else 26.dp

                        Surface(
                            shape = item.shape,
                            color = containerColor,
                            shadowElevation = 0.dp,
                            modifier = Modifier.size(tileSize)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(item.iconRes),
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier
                                        .size(iconSize)
                                        .then(
                                            if (item.iconOffset > 0.dp) Modifier.offset(y = item.iconOffset) else Modifier
                                        )
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
 * Step 1: Look & Feel Customization with Live Theme Preview Viewport,
 * Theme Mode Segmented Selector, Pure AMOLED Black switch, and Accent Palettes.
 */
@Composable
private fun SetupLooksFeelPage(
    settingsViewModel: SettingsViewModel?,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val currentThemeMode = settingsViewModel?.themeMode?.collectAsState()?.value ?: ThemeMode.SYSTEM
    val useMaterialYou = settingsViewModel?.useMaterialYou?.collectAsState()?.value 
        ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val colorPresetName = settingsViewModel?.colorPresetName?.collectAsState()?.value 
        ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "dynamic" else "default_blue"
    val useAmoledBlack = settingsViewModel?.useAmoledBlack?.collectAsState()?.value ?: false
    val isSystemDark = isSystemInDarkTheme()
    val isCurrentlyDark = when (currentThemeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.XL, vertical = SpacingTokens.M),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(SpacingTokens.M))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                        contentDescription = "Look & Feel",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            Text(
                text = "Look & Feel",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(SpacingTokens.XS))

            Text(
                text = "Personalize theme modes, deep AMOLED blacks, and vibrant accent colors in real time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // Interactive Live Theme Preview Card
            OnboardingLiveThemePreview(
                isCurrentlyDark = isCurrentlyDark,
                useAmoledBlack = useAmoledBlack
            )

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // Two-layer segmented tonal Look & Feel Tray (Theme Mode + AMOLED + Accent Palette)
            val lookTileColor = onboardingInnerTileColor()
            OnboardingCausticTray(phaseOffset = 2.7f) {
                    // Segment 0: Theme Mode Selector (System, Light, Dark)
                    Surface(
                        shape = onboardingSegmentedTileShape(0, 3),
                        color = lookTileColor,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                        ) {
                            Text(
                                text = "Theme Mode",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shadowElevation = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val themeOptions = listOf(
                                        Triple(ThemeMode.SYSTEM, R.drawable.ic_ms_wb_twilight, stringResource(R.string.theme_mode_system)),
                                        Triple(ThemeMode.LIGHT, R.drawable.ic_ms_light_mode, stringResource(R.string.theme_mode_light)),
                                        Triple(ThemeMode.DARK, R.drawable.ic_ms_dark_mode, stringResource(R.string.theme_mode_dark))
                                    )
                                    themeOptions.forEach { (mode, iconRes, label) ->
                                        val selected = currentThemeMode == mode
                                        Surface(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                settingsViewModel?.setThemeMode(mode)
                                            },
                                            shape = ShapeFull,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            shadowElevation = 0.dp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(iconRes),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Segment 1: Pure AMOLED Black Switch
                    Surface(
                        shape = onboardingSegmentedTileShape(1, 3),
                        color = lookTileColor,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isCurrentlyDark) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    settingsViewModel?.setUseAmoledBlack(!useAmoledBlack)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (useAmoledBlack && isCurrentlyDark) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                contentColor = if (useAmoledBlack && isCurrentlyDark) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_contrast),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(SpacingTokens.M))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pure AMOLED Black",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCurrentlyDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isCurrentlyDark) "Pitch-black canvas for OLED efficiency" else "Active when Dark theme mode is selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(SpacingTokens.S))

                            Switch(
                                checked = useAmoledBlack,
                                onCheckedChange = { checked ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    settingsViewModel?.setUseAmoledBlack(checked)
                                },
                                enabled = isCurrentlyDark,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    uncheckedBorderColor = Color.Transparent,
                                    disabledUncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }

                    // Segment 2: Color Palette Selector
                    Surface(
                        shape = onboardingSegmentedTileShape(2, 3),
                        color = lookTileColor,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                        ) {
                            Text(
                                text = "Accent Color Palette",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Dynamic Material You Swatch (Android 12+)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    item {
                                        ColorSwatchItem(
                                            title = stringResource(R.string.material_you),
                                            color = MaterialTheme.colorScheme.primary,
                                            isSelected = useMaterialYou || colorPresetName == "dynamic",
                                            isDynamic = true,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                settingsViewModel?.setUseMaterialYou(true)
                                                settingsViewModel?.setColorPresetName("dynamic")
                                            }
                                        )
                                    }
                                }

                                // Curated 4-Color Preset Palettes
                                items(ThemePalettes.allPalettes) { palette ->
                                    FourQuadrantColorSwatch(
                                        palette = palette,
                                        isSelected = !useMaterialYou && colorPresetName != "dynamic" && (colorPresetName == palette.id),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            settingsViewModel?.setUseMaterialYou(false)
                                            settingsViewModel?.setColorPresetName(palette.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.XL))
        }

        // Bottom Action Dock: Back & Next Buttons
        OnboardingNavigationDock(
            onBack = onBack,
            onNext = onContinue,
            nextText = "Next",
            nextIconRes = R.drawable.ic_ms_chevron_right
        )
    }
}

/**
 * Step 3: Semantic AI Smart Search Setup with Live Speed & Model Size Metrics.
 */
@Composable
private fun SetupAiPage(
    settingsViewModel: SettingsViewModel?,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val isModelDownloaded = settingsViewModel?.smartSearchModelDownloaded?.collectAsState()?.value ?: false
    val modelDownloadWorkInfo = settingsViewModel?.modelDownloadWorkInfo?.collectAsState(initial = null)?.value
    val isDownloading = modelDownloadWorkInfo?.state == WorkInfo.State.RUNNING
    val downloadProgress = modelDownloadWorkInfo?.progress?.getInt("progress", 0) ?: 0
    val bytesDownloaded = modelDownloadWorkInfo?.progress?.getLong("bytesDownloaded", 0L) ?: 0L
    val totalBytes = modelDownloadWorkInfo?.progress?.getLong("totalBytes", 404_814_260L) ?: 404_814_260L
    val speedBytesPerSec = modelDownloadWorkInfo?.progress?.getLong("speedBytesPerSec", 0L) ?: 0L
    val autoIndexEnabled = settingsViewModel?.smartSearchAutoIndex?.collectAsState()?.value ?: true
    val ocrIndexingEnabled = settingsViewModel?.ocrIndexingEnabled?.collectAsState()?.value ?: true

    // Face Recognition & Model Download state
    val faceRecognitionEnabled = settingsViewModel?.faceRecognitionEnabled?.collectAsState()?.value ?: true
    val faceModelDownloaded = settingsViewModel?.faceModelDownloaded?.collectAsState()?.value ?: false
    val faceModelDownloadWorkInfo = settingsViewModel?.faceModelDownloadWorkInfo?.collectAsState(initial = null)?.value
    val isFaceDownloading = faceModelDownloadWorkInfo?.state == WorkInfo.State.RUNNING
    val faceDownloadProgress = faceModelDownloadWorkInfo?.progress?.getInt("progress", 0) ?: 0
    val faceBytesDownloaded = faceModelDownloadWorkInfo?.progress?.getLong("bytesDownloaded", 0L) ?: 0L
    val faceTotalBytes = faceModelDownloadWorkInfo?.progress?.getLong("totalBytes", 13_600_000L) ?: 13_600_000L
    val faceSpeedBytesPerSec = faceModelDownloadWorkInfo?.progress?.getLong("speedBytesPerSec", 0L) ?: 0L
    val faceAutoIndex = settingsViewModel?.faceAutoIndex?.collectAsState()?.value ?: true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.XL, vertical = SpacingTokens.M),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(bottom = SpacingTokens.S),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(SpacingTokens.M))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search_sparkle),
                        contentDescription = "On-Device AI",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            Text(
                text = "Smart Search",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(SpacingTokens.XS))

            Text(
                text = "Search by scene, people, or text — processed privately on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            SmartSearchLoopPreviewCard()

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            // 1. Text (OCR) Indexing Card (Built into Android, 0 MB)
            OnboardingAiModelCard(
                title = "Text (OCR) Indexing",
                subtitle = "Google ML Kit • Built into Android",
                statusText = "Built-in",
                isStatusActive = true,
                isDownloading = false,
                iconRes = R.drawable.ic_ms_text_fields,
                iconShape = RoundedCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                accentColor = MaterialTheme.colorScheme.primaryContainer,
                onAccentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                toggles = listOf(
                    AiModelToggle(
                        title = "Auto-Index OCR Text",
                        subtitle = "Scan new screenshots and photos locally",
                        checked = ocrIndexingEnabled,
                        onCheckedChange = { settingsViewModel?.setOcrIndexingEnabled(it) }
                    )
                )
            )

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            // 2. Face Recognition & People Grouping Card
            val faceToggles = buildList {
                add(
                    AiModelToggle(
                        title = "Enable Face Grouping",
                        subtitle = "Organize photos into private People albums",
                        checked = faceRecognitionEnabled,
                        onCheckedChange = { settingsViewModel?.setFaceRecognitionEnabled(it) }
                    )
                )
                if (faceRecognitionEnabled) {
                    add(
                        AiModelToggle(
                            title = "Auto-Index New Faces",
                            subtitle = "Detect faces when new photos arrive",
                            checked = faceAutoIndex,
                            onCheckedChange = { settingsViewModel?.setFaceAutoIndex(it) }
                        )
                    )
                }
            }
            OnboardingAiModelCard(
                title = "People & Face Grouping",
                subtitle = "MobileFaceNet • InsightFace (w600k)",
                statusText = if (faceModelDownloaded) "Ready • 13 MB" else if (isFaceDownloading) "$faceDownloadProgress%" else "~13 MB",
                isStatusActive = faceModelDownloaded,
                isDownloading = isFaceDownloading,
                iconRes = R.drawable.ic_ms_person,
                iconShape = RoundedCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                accentColor = MaterialTheme.colorScheme.primaryContainer,
                onAccentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                toggles = faceToggles,
                downloadArea = if (faceRecognitionEnabled && !faceModelDownloaded) {
                    {
                        AiModelDownloadArea(
                            isDownloading = isFaceDownloading,
                            progress = faceDownloadProgress,
                            bytesDownloaded = faceBytesDownloaded,
                            totalBytes = faceTotalBytes,
                            speedBytesPerSec = faceSpeedBytesPerSec,
                            downloadButtonText = "Download Face Model (13 MB)",
                            accentColor = MaterialTheme.colorScheme.primary,
                            onAccentColor = MaterialTheme.colorScheme.onPrimary,
                            onStartDownload = { settingsViewModel?.startFaceModelDownload() },
                            onCancelDownload = { settingsViewModel?.cancelFaceModelDownload() }
                        )
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            // 3. Smart Search (Semantic AI) Card
            OnboardingAiModelCard(
                title = "Smart Search (Semantic AI)",
                subtitle = "Apple MobileCLIP2-S2 • ONNX FP32",
                statusText = if (isModelDownloaded) "Ready • ~386 MB" else if (isDownloading) "$downloadProgress%" else "~386 MB",
                isStatusActive = isModelDownloaded,
                isDownloading = isDownloading,
                iconRes = R.drawable.ic_ms_auto_fix_high,
                iconShape = RoundedCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                accentColor = MaterialTheme.colorScheme.primaryContainer,
                onAccentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                toggles = listOf(
                    AiModelToggle(
                        title = "Auto-Index New Photos",
                        subtitle = "Generate search vectors in background",
                        checked = autoIndexEnabled,
                        onCheckedChange = { settingsViewModel?.setSmartSearchAutoIndex(it) }
                    )
                ),
                downloadArea = if (!isModelDownloaded) {
                    {
                        AiModelDownloadArea(
                            isDownloading = isDownloading,
                            progress = downloadProgress,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes,
                            speedBytesPerSec = speedBytesPerSec,
                            downloadButtonText = "Download Model (~386 MB)",
                            accentColor = MaterialTheme.colorScheme.primary,
                            onAccentColor = MaterialTheme.colorScheme.onPrimary,
                            onStartDownload = { settingsViewModel?.startModelDownload() },
                            onCancelDownload = { settingsViewModel?.cancelModelDownload() }
                        )
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(SpacingTokens.S))
        }

        // Bottom Navigation Dock: Back & Finish Setup / Start Exploring
        val canExplore = isModelDownloaded || faceModelDownloaded
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.S),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OnboardingNavigationDock(
                onBack = onBack,
                onNext = onFinish,
                nextText = "Finish",
                nextIconRes = R.drawable.ic_ms_check
            )

            if (!canExplore && !isDownloading && !isFaceDownloading) {
                Text(
                    text = "AI models can also be downloaded anytime in Settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = SpacingTokens.XS)
                )
            }
        }
    }
}

/**
 * Auto-looping animated demo card illustrating Photon Gallery's 3 offline AI search modes:
 * People (face grouping), Text inside photos (OCR), and Scenes (semantic search).
 */
@Composable
private fun SmartSearchLoopPreviewCard(
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val modes = remember {
        listOf(
            Triple("People", "Sarah", R.drawable.ic_ms_person),
            Triple("Text (OCR)", "wifi password", R.drawable.ic_ms_text_fields),
            Triple("Scenes", "sunset beach", R.drawable.ic_ms_search_sparkle)
        )
    }

    var activeMode by remember { mutableIntStateOf(0) }
    var typedLength by remember { mutableIntStateOf(0) }
    var showResults by remember { mutableStateOf(false) }

    LaunchedEffect(activeMode) {
        val fullQuery = modes[activeMode].second
        typedLength = 0
        showResults = false
        delay(180)
        for (i in 1..fullQuery.length) {
            typedLength = i
            delay(50)
        }
        delay(220)
        showResults = true
        delay(2400)
        showResults = false
        delay(220)
        activeMode = (activeMode + 1) % modes.size
    }

    val currentQuery = modes[activeMode].second.take(typedLength)
    val currentIcon = modes[activeMode].third
    val innerTileColor = onboardingInnerTileColor()

    OnboardingCausticTray(
        modifier = modifier.fillMaxWidth(),
        phaseOffset = 3.9f
    ) {
        // Segment 0: Mode pills + Animated typing search bar
            Surface(
                shape = onboardingSegmentedTileShape(0, 2),
                color = innerTileColor,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                    ) {
                        modes.forEachIndexed { idx, (label, _, iconRes) ->
                            val selected = idx == activeMode
                            Surface(
                                shape = ShapeFull,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    activeMode = idx
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Animated typing search bar
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(currentIcon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(IconSizeTokens.S)
                            )
                            Text(
                                text = if (currentQuery.isEmpty()) "Search..." else currentQuery,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (currentQuery.isEmpty()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            // Segment 1: Animated visual result cards tailored to the active mode
            Surface(
                shape = onboardingSegmentedTileShape(1, 2),
                color = innerTileColor,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    AnimatedContent(
                        targetState = if (showResults) activeMode else -1,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(MotionTokens.Durations.Medium, easing = MotionTokens.EmphasizedDecelerateEasing)) +
                                slideInHorizontally(
                                    animationSpec = tween(MotionTokens.Durations.Medium, easing = MotionTokens.EmphasizedEasing),
                                    initialOffsetX = { (it * 0.08f).toInt() }
                                )).togetherWith(
                                fadeOut(animationSpec = tween(MotionTokens.Durations.Short, easing = MotionTokens.EmphasizedAccelerateEasing))
                            )
                        },
                        label = "SmartSearchLoopResults"
                    ) { state ->
                        if (state < 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                            ) {
                                repeat(3) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(64.dp)
                                    ) {}
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                            ) {
                                repeat(3) { idx ->
                                    val isHighlight = idx == 0
                                    val bgColor = if (isHighlight) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    }
                                    val fgColor = if (isHighlight) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = bgColor,
                                        contentColor = fgColor,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(64.dp)
                                    ) {
                                        when (state) {
                                            0 -> {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = fgColor.copy(alpha = 0.14f),
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                                                contentDescription = null,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            1 -> {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(0.75f)
                                                            .height(5.dp)
                                                            .clip(ShapeFull)
                                                            .background(fgColor.copy(alpha = 0.22f))
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(if (isHighlight) 0.9f else 0.7f)
                                                            .height(7.dp)
                                                            .clip(ShapeFull)
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isHighlight) 0.85f else 0.55f))
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(0.55f)
                                                            .height(5.dp)
                                                            .clip(ShapeFull)
                                                            .background(fgColor.copy(alpha = 0.22f))
                                                    )
                                                }
                                            }
                                            else -> {
                                                val sceneIcon = when (idx) {
                                                    0 -> R.drawable.ic_ms_wb_twilight
                                                    1 -> R.drawable.ic_ms_photo_camera
                                                    else -> R.drawable.ic_ms_image
                                                }
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(
                                                        imageVector = ImageVector.vectorResource(sceneIcon),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(22.dp)
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

/**
 * Interactive Live Theme Preview Card.
 * Displays a realistic miniature gallery viewport that reactively updates
 * when theme mode, AMOLED black, and accent color palettes change.
 */
@Composable
private fun OnboardingLiveThemePreview(
    isCurrentlyDark: Boolean,
    useAmoledBlack: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBackground = if (useAmoledBlack && isCurrentlyDark) {
        Color.Black
    } else if (isCurrentlyDark) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    val animatedCardBg by animateColorAsState(
        targetValue = cardBackground,
        animationSpec = MotionTokens.defaultEffectsSpec(),
        label = "LivePreviewCardBg"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = animatedCardBg,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(172.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Real Photon Top Header ("Photos" + tonal action pills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Photos",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_image),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            // 2. Clean Solid-Tone Gallery Grid Preview
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tile 1: Primary Tonal Photo Card
                    Surface(
                        shape = ShapeMedium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_favorite),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tile 2: Secondary Tonal Video Card
                    Surface(
                        shape = ShapeMedium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Text(
                                text = "0:42",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 8.sp,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                    }

                    // Tile 3: Tertiary Tonal Highlight Card
                    Surface(
                        shape = ShapeMedium,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Surface(
                                shape = OfficialM3Shapes.Clover4Leaf,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_star),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Real Photon Expressive Floating Navigation Dock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_image),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .size(11.dp)
                            )
                        }
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_album),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Color Swatch Item on Step 1 (Look & Feel).
 */
@Composable
private fun ColorSwatchItem(
    title: String,
    color: Color,
    isSelected: Boolean,
    isDynamic: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(78.dp)
            .padding(vertical = SpacingTokens.XS)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
        ) {
            if (isSelected) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = ShapeMedium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 0.dp
                ) {}
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (isSelected) 46.dp else 48.dp)
                    .clip(ShapeMedium)
                    .background(color)
            ) {
                if (isDynamic) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(IconSizeTokens.M)
                    )
                } else if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(IconSizeTokens.S)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingTokens.XS))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                letterSpacing = (-0.2).sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Individual Elevated Granular Permission Card.
 */
@Composable
private fun PermissionBlockCard(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    icon: ImageVector,
    iconShape: Shape,
    containerColor: Color,
    contentColor: Color,
    cardShape: Shape = ShapeLarge,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val badgeColor = if (isGranted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val badgeContentColor = if (isGranted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val cardColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = MotionTokens.fastEffectsSpec(),
        label = "PermissionCardColor"
    )

    Surface(
        shape = cardShape,
        color = cardColor,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = iconShape,
                color = badgeColor,
                contentColor = badgeContentColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(IconSizeTokens.L)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Allowed",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                } else {
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                    shape = ShapeFull,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Allow",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

private data class AiModelToggle(
    val title: String,
    val subtitle: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

/**
 * Clean, minimal M3 Expressive AI model card for Step 3 onboarding.
 * Replaces multi-layered boxes with one header row and one unified tonal control surface.
 */
@Composable
private fun OnboardingAiModelCard(
    title: String,
    subtitle: String,
    statusText: String,
    isStatusActive: Boolean,
    isDownloading: Boolean,
    @DrawableRes iconRes: Int,
    iconShape: Shape,
    containerColor: Color,
    accentColor: Color,
    onAccentColor: Color,
    toggles: List<AiModelToggle>,
    downloadArea: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val innerTileColor = onboardingInnerTileColor()
    val totalSegments = 1 + toggles.size + (if (downloadArea != null) 1 else 0)

    OnboardingCausticTray(
        trayColor = containerColor,
        phaseOffset = title.length * 0.43f
    ) {
            // Segment 0: Header (Icon + Title/Subtitle + Status Pill)
            Surface(
                shape = onboardingSegmentedTileShape(0, totalSegments),
                color = innerTileColor,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = iconShape,
                        color = accentColor,
                        contentColor = onAccentColor,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = ImageVector.vectorResource(iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = when {
                            isStatusActive -> accentColor
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = when {
                            isStatusActive -> onAccentColor
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        shape = ShapeFull
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Segments 1..N: Individual toggle tiles
            toggles.forEachIndexed { idx, toggle ->
                Surface(
                    shape = onboardingSegmentedTileShape(1 + idx, totalSegments),
                    color = innerTileColor,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toggle.onCheckedChange(!toggle.checked)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = toggle.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = toggle.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = toggle.checked,
                            onCheckedChange = { checked ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toggle.onCheckedChange(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // Optional bottom download segment
            if (downloadArea != null) {
                Surface(
                    shape = onboardingSegmentedTileShape(totalSegments - 1, totalSegments),
                    color = innerTileColor,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        downloadArea()
                    }
                }
            }
    }
}

@Composable
private fun AiModelDownloadArea(
    isDownloading: Boolean,
    progress: Int,
    bytesDownloaded: Long,
    totalBytes: Long,
    speedBytesPerSec: Long,
    downloadButtonText: String,
    accentColor: Color,
    onAccentColor: Color,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    if (isDownloading) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Downloading: $progress%",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
                Text(
                    text = if (bytesDownloaded > 0L) "${formatBytes(bytesDownloaded)} / ${formatBytes(totalBytes)}" else formatBytes(totalBytes),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearWavyProgressIndicator(
                progress = { progress / 100f },
                wavelength = WavyProgressTokens.LinearWavelength,
                waveSpeed = WavyProgressTokens.CalmWaveSpeed,
                color = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_bolt),
                        contentDescription = "Speed",
                        tint = accentColor,
                        modifier = Modifier.size(IconSizeTokens.S)
                    )
                    Text(
                        text = "Speed: ${formatSpeed(speedBytesPerSec)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCancelDownload()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    } else {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onStartDownload()
            },
            shape = ShapeFull,
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = onAccentColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pressScale()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_download),
                    contentDescription = null,
                    modifier = Modifier.size(IconSizeTokens.S)
                )
                Text(
                    text = downloadButtonText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
