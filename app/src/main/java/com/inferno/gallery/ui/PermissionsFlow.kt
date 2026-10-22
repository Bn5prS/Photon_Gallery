@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.inferno.gallery.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import com.inferno.gallery.BuildConfig
import com.inferno.gallery.R
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
        readImages && readVideos
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
 * Photon Gallery Material 3 Expressive Setup Wizard.
 *
 * 3 Guided Expressive Steps with Back & Next navigation dock:
 * - Step 0: Welcome, Privacy Guarantee & Granular Media Permissions
 * - Step 1: Look & Feel Customization with Interactive Live Theme Viewport
 * - Step 2: Semantic AI Smart Search Setup with Live Speed & Model Size Metrics
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isForward = currentStep >= previousStep

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (isForward) {
                        (slideInHorizontally(
                            animationSpec = MotionTokens.gentleSpring(),
                            initialOffsetX = { fullWidth -> fullWidth }
                        ) + fadeIn(animationSpec = MotionTokens.fastEffectsSpec())).togetherWith(
                            slideOutHorizontally(
                                animationSpec = MotionTokens.gentleSpring(),
                                targetOffsetX = { fullWidth -> -fullWidth }
                            ) + fadeOut(animationSpec = MotionTokens.fastEffectsSpec())
                        )
                    } else {
                        (slideInHorizontally(
                            animationSpec = MotionTokens.gentleSpring(),
                            initialOffsetX = { fullWidth -> -fullWidth }
                        ) + fadeIn(animationSpec = MotionTokens.fastEffectsSpec())).togetherWith(
                            slideOutHorizontally(
                                animationSpec = MotionTokens.gentleSpring(),
                                targetOffsetX = { fullWidth -> fullWidth }
                            ) + fadeOut(animationSpec = MotionTokens.fastEffectsSpec())
                        )
                    }
                },
                label = "OnboardingStepTransition",
                modifier = Modifier.fillMaxSize()
            ) { step ->
                when (step) {
                    0 -> SetupWelcomePermissionsPage(
                        photosGranted = photosGranted,
                        videosGranted = videosGranted,
                        allFilesGranted = allFilesGranted,
                        allGranted = allGranted,
                        onGrantMediaClick = onGrantMediaClick,
                        onGrantAllFilesClick = onGrantAllFilesClick,
                        onContinue = {
                            previousStep = 0
                            currentStep = 1
                        }
                    )
                    1 -> SetupLooksFeelPage(
                        settingsViewModel = resolvedSettingsViewModel,
                        onBack = if (initialStep == 0) {
                            {
                                previousStep = 1
                                currentStep = 0
                            }
                        } else null,
                        onContinue = {
                            previousStep = 1
                            currentStep = 2
                        }
                    )
                    2 -> SetupAiPage(
                        settingsViewModel = resolvedSettingsViewModel,
                        onBack = {
                            previousStep = 2
                            currentStep = 1
                        },
                        onFinish = onContinueClick
                    )
                }
            }
        }
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
    nextIconRes: Int? = R.drawable.ic_ms_chevron_right,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
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
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .pressScale()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(IconSizeTokens.M)
                        )
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
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
                    .weight(if (onBack != null) 1.5f else 1f)
                    .height(56.dp)
                    .pressScale()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                ) {
                    Text(
                        text = nextText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (nextIconRes != null) {
                        Icon(
                            imageVector = ImageVector.vectorResource(nextIconRes),
                            contentDescription = null,
                            modifier = Modifier.size(IconSizeTokens.M)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 0: Welcome Screen with Expressive Left-Aligned Header, Floating Shapes Collage, and Unified Permission Cards.
 */
@Composable
private fun SetupWelcomePermissionsPage(
    photosGranted: Boolean,
    videosGranted: Boolean,
    allFilesGranted: Boolean,
    allGranted: Boolean,
    onGrantMediaClick: () -> Unit,
    onGrantAllFilesClick: () -> Unit,
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
        horizontalAlignment = Alignment.Start
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
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = contentMinHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    // Top: Welcome Header
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Spacer(modifier = Modifier.height(SpacingTokens.M))

                        Text(
                            text = "Welcome to",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        )

                        Text(
                            text = "Photon Gallery",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(SpacingTokens.XS))

                        Text(
                            text = "A private, beautiful space for all your memories.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    }

                    // Middle: Asymmetric Expressive Photography Shapes Collage (Varied sizes, angles & non-overlapping locations)
                    ExpressiveGallerySymbolsCollage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = SpacingTokens.M)
                    )

                    // Bottom: Elevated Permission Cards sitting right above the dock
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = SpacingTokens.M),
                        verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                    ) {
                        Text(
                            text = "Required Permissions",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        PermissionBlockCard(
                            title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "Photos & Videos Access" else "Storage Access",
                            subtitle = "Required to view, organize, and manage your photo and video library",
                            isGranted = mediaGranted,
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_photo_album),
                            iconShape = OfficialM3Shapes.Cookie12Sided,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = onGrantMediaClick
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            PermissionBlockCard(
                                title = "All Files Access",
                                subtitle = "Required for lossless folder moves, deletions & Vault",
                                isGranted = allFilesGranted,
                                icon = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                iconShape = OfficialM3Shapes.Sunny,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = onGrantAllFilesClick
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Dock (Next or Grant Permissions)
        OnboardingNavigationDock(
            onNext = {
                if (mediaGranted) {
                    onContinue()
                } else {
                    onGrantMediaClick()
                }
            },
            nextText = if (mediaGranted) "Next" else "Grant Permissions",
            onBack = null,
            nextIconRes = if (mediaGranted) R.drawable.ic_ms_chevron_right else null
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
    onBack: (() -> Unit)? = null,
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(SpacingTokens.S))

            // Header Icon
            Surface(
                shape = OfficialM3Shapes.Clover4Leaf,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                        contentDescription = "Look & Feel",
                        modifier = Modifier.size(IconSizeTokens.XL)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            Text(
                text = "Look & Feel",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(SpacingTokens.XS))

            Text(
                text = "Personalize theme modes, deep AMOLED blacks, and vibrant accent colors in real time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = SpacingTokens.S)
            )

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // Interactive Live Theme Preview Card
            OnboardingLiveThemePreview(
                isCurrentlyDark = isCurrentlyDark,
                useAmoledBlack = useAmoledBlack
            )

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // Theme Mode Segmented Button (System, Light, Dark)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
            ) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            settingsViewModel?.setThemeMode(ThemeMode.SYSTEM)
                        },
                        selected = currentThemeMode == ThemeMode.SYSTEM,
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_wb_twilight),
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeTokens.S)
                            )
                        },
                        label = { Text("System", style = MaterialTheme.typography.labelMedium) }
                    )

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            settingsViewModel?.setThemeMode(ThemeMode.LIGHT)
                        },
                        selected = currentThemeMode == ThemeMode.LIGHT,
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_light_mode),
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeTokens.S)
                            )
                        },
                        label = { Text("Light", style = MaterialTheme.typography.labelMedium) }
                    )

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            settingsViewModel?.setThemeMode(ThemeMode.DARK)
                        },
                        selected = currentThemeMode == ThemeMode.DARK,
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_dark_mode),
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeTokens.S)
                            )
                        },
                        label = { Text("Dark", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // Pure AMOLED Black Switch
            Surface(
                shape = ShapeLargeIncreased,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isCurrentlyDark) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            settingsViewModel?.setUseAmoledBlack(!useAmoledBlack)
                        }
                        .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.M),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = ShapeMedium,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_contrast),
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeTokens.L)
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
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // Color Palette Selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
            ) {
                Text(
                    text = "Accent Color Palette",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                ) {
                    // Dynamic Material You Swatch (Android 12+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        item {
                            ColorSwatchItem(
                                title = "Dynamic",
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
 * Step 2: Semantic AI Smart Search Setup with Live Speed & Model Size Metrics.
 */
@Composable
private fun SetupAiPage(
    settingsViewModel: SettingsViewModel?,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val isModelDownloaded = settingsViewModel?.smartSearchModelDownloaded?.collectAsState()?.value ?: false
    val modelDownloadWorkInfo = settingsViewModel?.modelDownloadWorkInfo?.collectAsState(initial = null)?.value
    val isDownloading = modelDownloadWorkInfo?.state == WorkInfo.State.RUNNING
    val downloadProgress = modelDownloadWorkInfo?.progress?.getInt("progress", 0) ?: 0
    val bytesDownloaded = modelDownloadWorkInfo?.progress?.getLong("bytesDownloaded", 0L) ?: 0L
    val totalBytes = modelDownloadWorkInfo?.progress?.getLong("totalBytes", 154_190_190L) ?: 154_190_190L
    val speedBytesPerSec = modelDownloadWorkInfo?.progress?.getLong("speedBytesPerSec", 0L) ?: 0L
    val autoIndexEnabled = settingsViewModel?.smartSearchAutoIndex?.collectAsState()?.value ?: true
    val ocrIndexingEnabled = settingsViewModel?.ocrIndexingEnabled?.collectAsState()?.value ?: true

    // Interactive query simulation playground state
    val sampleQueries = listOf("sunset beach", "latte coffee", "happy puppy", "receipts")
    var selectedQueryIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentSampleQuery = sampleQueries[selectedQueryIndex]

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(SpacingTokens.S))

            // AI Search Icon
            Surface(
                shape = OfficialM3Shapes.Cookie9Sided,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                        contentDescription = "Smart Search",
                        modifier = Modifier.size(IconSizeTokens.XL)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            Text(
                text = "Smart Search",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(SpacingTokens.XS))

            Text(
                text = "Search photos with natural concepts and keywords like 'beach sunset' or 'white car' without uploading to the cloud.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = SpacingTokens.S)
            )

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // Interactive Playground Card: Tap sample concept chips to see results update live
            Surface(
                shape = ShapeLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SpacingTokens.M),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                ) {
                    // Simulated Search Input Pill
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(IconSizeTokens.S)
                            )
                            Text(
                                text = currentSampleQuery,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Sample Concept Suggestion Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                    ) {
                        sampleQueries.forEachIndexed { index, query ->
                            val isChipSelected = index == selectedQueryIndex
                            Surface(
                                shape = ShapeFull,
                                color = if (isChipSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = if (isChipSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedQueryIndex = index
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = SpacingTokens.XS)
                                ) {
                                    Text(
                                        text = query,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Simulated 3 Match Tiles responding to selected concept
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                    ) {
                        val tileGradients = when (selectedQueryIndex) {
                            0 -> listOf(
                                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)),
                                listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f))
                            )
                            1 -> listOf(
                                listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceContainerHighest),
                                listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f))
                            )
                            2 -> listOf(
                                listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)),
                                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                                listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                            )
                            else -> listOf(
                                listOf(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.surfaceVariant),
                                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceContainerHigh),
                                listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainer)
                            )
                        }

                        for (gradientColors in tileGradients) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(ShapeMedium)
                                    .background(Brush.linearGradient(gradientColors))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // AI Model Configuration Container with Model Size & Speed Metrics
            Surface(
                shape = ShapeLargeIncreased,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SpacingTokens.L),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = ShapeMedium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeTokens.L)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(SpacingTokens.M))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "On-Device MobileCLIP",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Apple MobileCLIP • ~147 MB • 100% Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = if (isModelDownloaded) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else if (isDownloading) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            shape = ShapeFull
                        ) {
                            Text(
                                text = if (isModelDownloaded) "Ready • 147 MB" else if (isDownloading) "$downloadProgress%" else "~147 MB",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isModelDownloaded) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else if (isDownloading) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS)
                            )
                        }
                    }

                    // OCR Text Processing Toggle (Placed on top)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                settingsViewModel?.setOcrIndexingEnabled(!ocrIndexingEnabled)
                            }
                            .padding(vertical = SpacingTokens.XS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = ShapeMedium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_text_fields),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(SpacingTokens.M))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OCR Text Processing",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Extract and index text inside screenshots, receipts, and photos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(SpacingTokens.S))

                        Switch(
                            checked = ocrIndexingEnabled,
                            onCheckedChange = { checked ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                settingsViewModel?.setOcrIndexingEnabled(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        thickness = 0.75.dp
                    )

                    // Smart Search Auto-Index Toggle (Placed below OCR)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                settingsViewModel?.setSmartSearchAutoIndex(!autoIndexEnabled)
                            }
                            .padding(vertical = SpacingTokens.XS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = ShapeMedium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(SpacingTokens.M))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart Search Auto-Index",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Automatically generate semantic embeddings for new photos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(SpacingTokens.S))

                        Switch(
                            checked = autoIndexEnabled,
                            onCheckedChange = { checked ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                settingsViewModel?.setSmartSearchAutoIndex(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        thickness = 0.75.dp
                    )

                    // Download Model Status / Action Area with Speed and Size Metrics
                    if (isDownloading) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                        ) {
                            // Top Size and Percentage Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading: $downloadProgress%",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${formatBytes(bytesDownloaded)} / ${formatBytes(totalBytes)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Dynamic Progress Indicator
                            LinearWavyProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                            )

                            // Bottom Network Speed and Cancel Row
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
                                        tint = MaterialTheme.colorScheme.tertiary,
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
                                        settingsViewModel?.cancelModelDownload()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    } else if (isModelDownloaded) {
                        Surface(
                            shape = ShapeMedium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check_circle),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                                Text(
                                    text = "AI Model downloaded and verified (147 MB). Ready for offline search.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                settingsViewModel?.startModelDownload()
                            },
                            shape = ShapeFull,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                                    text = "Download Model (147 MB)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.XL))
        }

        // Bottom Navigation Dock: Back & Start Exploring / Finish Setup Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.S),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OnboardingNavigationDock(
                onBack = onBack,
                onNext = onFinish,
                nextText = if (isModelDownloaded) "Start Exploring" else "Finish Setup",
                nextIconRes = if (isModelDownloaded) R.drawable.ic_ms_check else R.drawable.ic_ms_chevron_right
            )

            if (!isModelDownloaded && !isDownloading) {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFinish()
                    },
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = "Set Up Later in Settings",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
        MaterialTheme.colorScheme.surfaceContainerLowest
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
        shape = ShapeLargeIncreased,
        color = animatedCardBg,
        border = if (useAmoledBlack && isCurrentlyDark) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
        },
        shadowElevation = if (isCurrentlyDark) 0.dp else 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SpacingTokens.M),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mini Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "Moments",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = SpacingTokens.XS),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeTokens.S)
                            )
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_settings),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSizeTokens.S)
                    )
                }
            }

            // Mini Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
            ) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS)
                    )
                }

                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS)
                    )
                }

                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = "Videos",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS)
                    )
                }
            }

            // Mini Media Grid & Mini FAB
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                ) {
                    // Tile 1 (Primary Gradient with Favorite badge)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(ShapeMedium)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_favorite),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(IconSizeTokens.S)
                        )
                    }

                    // Tile 2 (Secondary Gradient with Play badge)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(ShapeMedium)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(IconSizeTokens.S)
                        )
                    }

                    // Tile 3 (Tertiary Gradient)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(ShapeMedium)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )

                    // Tile 4 (Surface Container High)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(ShapeMedium)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            )
                    )
                }

                // Mini Floating Action Button
                Surface(
                    shape = OfficialM3Shapes.Sunny,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = SpacingTokens.XS, bottom = SpacingTokens.XS)
                        .size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_camera),
                            contentDescription = null,
                            modifier = Modifier.size(IconSizeTokens.S)
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
        modifier = Modifier.padding(vertical = SpacingTokens.XS)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .clip(ShapeMedium)
                .background(color)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.5.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = ShapeMedium
                        )
                    } else {
                        Modifier
                    }
                )
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
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape),
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

        Spacer(modifier = Modifier.height(SpacingTokens.XS))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = ShapeLarge,
        color = if (isGranted) containerColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer,
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
                .padding(horizontal = SpacingTokens.L, vertical = SpacingTokens.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = iconShape,
                color = if (isGranted) containerColor else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isGranted) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(46.dp)
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

            Spacer(modifier = Modifier.width(SpacingTokens.M))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(SpacingTokens.M))

            if (isGranted) {
                Surface(
                    shape = ShapeFull,
                    color = containerColor,
                    contentColor = contentColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                            contentDescription = null,
                            modifier = Modifier.size(IconSizeTokens.S)
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
