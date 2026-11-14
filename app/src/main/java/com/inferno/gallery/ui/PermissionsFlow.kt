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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import com.inferno.gallery.ui.theme.WavyProgressTokens
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
                    isPast -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        FloatingGalleryBackground(
            modifier = Modifier.fillMaxSize()
        ) {
            val isForward = currentStep >= previousStep

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

    Row(
        modifier = modifier.fillMaxWidth(),
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
                // Top balancing spacer
                Spacer(modifier = Modifier.height(SpacingTokens.XS))

                // Center: Dynamic Themed Circular App Icon + App Name + Tagline (elevated slightly)
                ExpressiveOnboardingHero(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp)
                )

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
                            .height(60.dp)
                            .pressScale()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_action_get_started),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeTokens.M)
                            )
                        }
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
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(SpacingTokens.S))

            // Header Icon Badge
            Surface(
                shape = OfficialM3Shapes.Ghostish,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_shield_lock),
                        contentDescription = "Permissions",
                        modifier = Modifier.size(IconSizeTokens.XL)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            Text(
                text = stringResource(R.string.onboarding_permissions_header),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(SpacingTokens.XS))

            Text(
                text = stringResource(R.string.onboarding_permissions_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = SpacingTokens.S)
            )

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // Elevated Permission Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.M)
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
                    iconShape = OfficialM3Shapes.Cookie12Sided,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onGrantMediaClick
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    PermissionBlockCard(
                        title = stringResource(R.string.onboarding_perm_all_files_title),
                        subtitle = stringResource(R.string.onboarding_perm_all_files_desc),
                        isGranted = allFilesGranted,
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                        iconShape = OfficialM3Shapes.Sunny,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = onGrantAllFilesClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.XL))
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
        // Dynamic Themed Circular App Icon (large, bold Google-scale shape)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(128.dp),
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = "Photon Gallery",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(168.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

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
                letterSpacing = 0.5.sp
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
                        label = { Text(stringResource(R.string.theme_mode_system), style = MaterialTheme.typography.labelMedium) }
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
                        label = { Text(stringResource(R.string.theme_mode_light), style = MaterialTheme.typography.labelMedium) }
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
                        label = { Text(stringResource(R.string.theme_mode_dark), style = MaterialTheme.typography.labelMedium) }
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

private data class SimulatedAiResult(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
    val gradientColors: List<Color>,
    val confidence: String
)

/**
 * Step 3: Semantic AI Smart Search Setup with Live Speed & Model Size Metrics.
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
    val totalBytes = modelDownloadWorkInfo?.progress?.getLong("totalBytes", 103_077_230L) ?: 103_077_230L
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

    // Interactive query simulation playground state
    val sampleQueries = listOf("sunset beach", "latte coffee", "happy puppy", "receipts")
    var selectedQueryIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentSampleQuery = sampleQueries[selectedQueryIndex]

    val simulatedAiResults = remember {
        mapOf(
            0 to listOf(
                SimulatedAiResult("Golden Hour", "Malibu Coast", R.drawable.ic_ms_wb_twilight, listOf(Color(0xFFE65100), Color(0xFFAD1457)), "99% Match"),
                SimulatedAiResult("Pacific Surf", "Beach & Waves", R.drawable.ic_ms_image, listOf(Color(0xFF00695C), Color(0xFF0277BD)), "96% Match"),
                SimulatedAiResult("Dusk Palms", "Shoreline Walk", R.drawable.ic_ms_filter_vintage, listOf(Color(0xFF4A148C), Color(0xFFD84315)), "92% Match")
            ),
            1 to listOf(
                SimulatedAiResult("Artisan Pour", "Cafe Latte Art", R.drawable.ic_ms_star, listOf(Color(0xFF5D4037), Color(0xFF8D6E63)), "98% Match"),
                SimulatedAiResult("Morning Brew", "Espresso Cup", R.drawable.ic_ms_image, listOf(Color(0xFF3E2723), Color(0xFF6D4C41)), "95% Match"),
                SimulatedAiResult("Bakery Table", "Breakfast Roasts", R.drawable.ic_ms_favorite, listOf(Color(0xFF795548), Color(0xFFA1887F)), "91% Match")
            ),
            2 to listOf(
                SimulatedAiResult("Golden Pup", "Park Playtime", R.drawable.ic_ms_favorite, listOf(Color(0xFFE65100), Color(0xFFFFB300)), "99% Match"),
                SimulatedAiResult("Grass Fetch", "Backyard Lawn", R.drawable.ic_ms_image, listOf(Color(0xFF2E7D32), Color(0xFF558B2F)), "97% Match"),
                SimulatedAiResult("Puppy Snooze", "Cozy Blanket", R.drawable.ic_ms_star, listOf(Color(0xFF00695C), Color(0xFFAD1457)), "94% Match")
            ),
            3 to listOf(
                SimulatedAiResult("Supermarket", "Receipt • \$42.50", R.drawable.ic_ms_receipt_long, listOf(Color(0xFF37474F), Color(0xFF455A64)), "OCR 100%"),
                SimulatedAiResult("Flight Ticket", "Boarding Pass", R.drawable.ic_ms_confirmation_number, listOf(Color(0xFF1565C0), Color(0xFF0277BD)), "OCR 98%"),
                SimulatedAiResult("Fuel Station", "Payment Slip", R.drawable.ic_ms_description, listOf(Color(0xFF263238), Color(0xFF00695C)), "OCR 97%")
            )
        )
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

            // AI Hub Icon
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
                        contentDescription = "On-Device AI",
                        modifier = Modifier.size(IconSizeTokens.XL)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.M))

            Text(
                text = "On-Device AI",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(SpacingTokens.XS))

            Text(
                text = "Supercharge your gallery with private, on-device semantic search and facial recognition — your photos never leave your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = SpacingTokens.S)
            )

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // Interactive Playground Card: Tap sample concept chips to see results update live
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SpacingTokens.L),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                ) {
                    // Simulated Search Input Pill
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
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

                    // Sample Concept Suggestion Chips (Horizontally scrollable so queries never truncate)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                    ) {
                        sampleQueries.forEachIndexed { index, query ->
                            val isChipSelected = index == selectedQueryIndex
                            Surface(
                                shape = ShapeFull,
                                color = if (isChipSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = if (isChipSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedQueryIndex = index
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS)
                                ) {
                                    Text(
                                        text = query,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Simulated 3 Live Photo Match Cards cross-fading smoothly with query selection
                    AnimatedContent(
                        targetState = selectedQueryIndex,
                        transitionSpec = {
                            fadeIn(animationSpec = MotionTokens.gentleSpring()) togetherWith fadeOut(animationSpec = MotionTokens.fastEffectsSpec())
                        },
                        label = "SimulatedResultsCrossFade"
                    ) { targetIndex ->
                        val results = simulatedAiResults[targetIndex] ?: emptyList()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                        ) {
                            results.forEach { result ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(86.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.verticalGradient(result.gradientColors))
                                        .padding(SpacingTokens.S)
                                ) {
                                    // Top row: mini icon badge + confidence pill
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.32f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(result.iconRes),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(ShapeFull)
                                                .background(Color.Black.copy(alpha = 0.38f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = result.confidence,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // Bottom: Title & Subtitle with crisp contrast
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = result.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = result.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Simulation indicator caption
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_bolt),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Simulated results • Tap chips to test natural query matching",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // 1. Text (OCR) Indexing Card (Built into Android, 0 MB)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = OfficialM3Shapes.ClamShell,
                            color = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_text_fields),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(IconSizeTokens.L)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Text (OCR) Indexing",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Google ML Kit • Built into Android",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            shape = ShapeFull
                        ) {
                            Text(
                                text = "Built-in",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS)
                            )
                        }
                    }

                    Text(
                        text = "Extract and search text inside screenshots, receipts, and documents 100% on-device without cloud sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 0.5.dp
                    )

                    // OCR Toggle
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
                            shape = OfficialM3Shapes.Bun,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                            contentColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Index OCR Text",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Automatically scan new screenshots and photos for text",
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
                                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 0.5.dp
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
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
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(IconSizeTokens.M)
                            )
                            Text(
                                text = "Built into Android • Zero download required",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // 2. Face Recognition & People Grouping Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = OfficialM3Shapes.Cookie12Sided,
                            color = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(IconSizeTokens.L)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "People & Face Grouping",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "MobileFaceNet • InsightFace (w600k)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = if (faceModelDownloaded) {
                                MaterialTheme.colorScheme.tertiary
                            } else if (isFaceDownloading) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (faceModelDownloaded) {
                                MaterialTheme.colorScheme.onTertiary
                            } else if (isFaceDownloading) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            shape = ShapeFull
                        ) {
                            Text(
                                text = if (faceModelDownloaded) "Ready • 13 MB" else if (isFaceDownloading) "$faceDownloadProgress%" else "~13 MB",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS)
                            )
                        }
                    }

                    Text(
                        text = "Detect and cluster faces locally to organize photos into private People albums.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 0.5.dp
                    )

                    // Face Recognition Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                settingsViewModel?.setFaceRecognitionEnabled(!faceRecognitionEnabled)
                            }
                            .padding(vertical = SpacingTokens.XS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = OfficialM3Shapes.Flower,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Face Grouping",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cluster and organize individuals across your gallery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(SpacingTokens.S))

                        Switch(
                            checked = faceRecognitionEnabled,
                            onCheckedChange = { checked ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                settingsViewModel?.setFaceRecognitionEnabled(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                                checkedTrackColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }

                    if (faceRecognitionEnabled) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            thickness = 0.5.dp
                        )

                        // Auto-Index Faces Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    settingsViewModel?.setFaceAutoIndex(!faceAutoIndex)
                                }
                                .padding(vertical = SpacingTokens.XS),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = OfficialM3Shapes.Sunny,
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                                contentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(IconSizeTokens.M)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Index New Faces",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Automatically detect and cluster faces when new photos arrive",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(SpacingTokens.S))

                            Switch(
                                checked = faceAutoIndex,
                                onCheckedChange = { checked ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    settingsViewModel?.setFaceAutoIndex(checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                                    checkedTrackColor = MaterialTheme.colorScheme.tertiary
                                )
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            thickness = 0.5.dp
                        )

                        // Download Face Model Action / Progress Area
                        if (isFaceDownloading) {
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
                                        text = "Downloading: $faceDownloadProgress%",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = if (faceBytesDownloaded > 0L) "${formatBytes(faceBytesDownloaded)} / ${formatBytes(faceTotalBytes)}" else "${formatBytes(faceTotalBytes)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                LinearWavyProgressIndicator(
                                    progress = { faceDownloadProgress / 100f },
                                    wavelength = WavyProgressTokens.LinearWavelength,
                                    waveSpeed = WavyProgressTokens.CalmWaveSpeed,
                                    color = MaterialTheme.colorScheme.tertiary,
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
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(IconSizeTokens.S)
                                        )
                                        Text(
                                            text = "Speed: ${formatSpeed(faceSpeedBytesPerSec)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            settingsViewModel?.cancelFaceModelDownload()
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text(stringResource(R.string.action_cancel))
                                    }
                                }
                            }
                        } else if (faceModelDownloaded) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
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
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(IconSizeTokens.M)
                                    )
                                    Text(
                                        text = "Model ready (13 MB) • Face grouping enabled",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    settingsViewModel?.startFaceModelDownload()
                                },
                                shape = ShapeFull,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
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
                                        text = "Download Face Model (13 MB)",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.L))

            // 3. Smart Search (Semantic AI) Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = OfficialM3Shapes.Clover4Leaf,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(IconSizeTokens.L)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart Search (Semantic AI)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Apple MobileCLIP2-S2 • ONNX FP32",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = if (isModelDownloaded) {
                                MaterialTheme.colorScheme.primary
                            } else if (isDownloading) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (isModelDownloaded) {
                                MaterialTheme.colorScheme.onPrimary
                            } else if (isDownloading) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            shape = ShapeFull
                        ) {
                            Text(
                                text = if (isModelDownloaded) "Ready • ~386 MB" else if (isDownloading) "$downloadProgress%" else "~386 MB",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS)
                            )
                        }
                    }

                    Text(
                        text = "Search photos with natural descriptions (e.g. 'sunset at beach', 'latte coffee') entirely on-device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 0.5.dp
                    )

                    // Smart Search Auto-Index Toggle
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
                            shape = OfficialM3Shapes.PuffyDiamond,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Index New Photos",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Generate semantic search vectors automatically in background",
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 0.5.dp
                    )

                    // Download Model Status / Action Area
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

                            LinearWavyProgressIndicator(
                                progress = { downloadProgress / 100f },
                                wavelength = WavyProgressTokens.LinearWavelength,
                                waveSpeed = WavyProgressTokens.CalmWaveSpeed,
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
                                    Text(stringResource(R.string.action_cancel))
                                }
                            }
                        }
                    } else if (isModelDownloaded) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
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
                                    text = "Model ready (103 MB) • Natural search enabled",
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
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
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
                                    text = "Download Model (103 MB)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpacingTokens.XL))
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
                nextText = if (canExplore) "Launch Photon" else "Enter Gallery",
                nextIconRes = if (canExplore) R.drawable.ic_ms_auto_fix_high else R.drawable.ic_ms_chevron_right
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
        shape = RoundedCornerShape(20.dp),
        color = animatedCardBg,
        border = if (useAmoledBlack && isCurrentlyDark) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        },
        shadowElevation = if (isCurrentlyDark) 0.dp else 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Mini Status Bar + Centered Punch-Hole Camera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "9:41",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontSize = 10.sp
                )

                // Miniature Camera Punch-Hole Pill
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 7.dp)
                        .clip(ShapeFull)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_bolt),
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            // 2. Mini Top App Bar: "Photon" + Search Capsule
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_launcher_monochrome),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        text = "Photon",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                            contentDescription = null,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "Search…",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // 3. Mini Filter Pills (All Photos, Favorites, Videos)
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
                        text = "All Photos",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // 4. Realistic 3-Tile Gallery Grid Simulation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
            ) {
                // Tile 1: Hero Photo with Sunset Gradient & Heart icon
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxSize()
                        .clip(ShapeMedium)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_favorite),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 8.sp
                        )
                    }
                }

                // Tile 2: Video Tile with Play icon & Duration badge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(ShapeMedium)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
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
                            .size(14.dp)
                    )
                    Text(
                        text = "0:42",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 8.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                    )
                }

                // Tile 3: Star Highlight Tile
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(ShapeMedium)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_star),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(12.dp)
                    )
                }
            }

            // 5. Mini Bottom Floating Navigation Dock
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 1 (Active: Photos)
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_image),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .size(10.dp)
                            )
                        }

                        // Tab 2 (Albums)
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_album),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(10.dp)
                        )

                        // Tab 3 (Search)
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(10.dp)
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
        color = if (isGranted) containerColor.copy(alpha = 0.35f) else containerColor.copy(alpha = 0.18f),
        border = BorderStroke(
            1.dp,
            if (isGranted) containerColor.copy(alpha = 0.65f) else containerColor.copy(alpha = 0.35f)
        ),
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
                color = containerColor,
                contentColor = contentColor,
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
