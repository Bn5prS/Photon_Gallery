package com.inferno.gallery.ui



import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import coil3.compose.AsyncImage
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Button
import com.inferno.gallery.ui.components.ExpressiveButton
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.work.WorkInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.ui.theme.photonContainer
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.utils.pressScale
import com.inferno.gallery.ui.components.PhotonSectionHeader
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.aspectRatio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inferno.gallery.ui.ThemeMode
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R


@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: SettingsViewModel = viewModel(),
    galleryViewModel: GalleryViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    activeSection: String? = null,
    onActiveSectionChange: (String?) -> Unit = {},
    onNavigateToVault: () -> Unit = {}
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val useMaterialYou by viewModel.useMaterialYou.collectAsState()
    val useAmoledBlack by viewModel.useAmoledBlack.collectAsState()
    val useFullScreen by viewModel.useFullScreen.collectAsState()
    val showAlbumSize by viewModel.showAlbumSize.collectAsState()
    val showHiddenAlbums by viewModel.showHiddenAlbums.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dockStyle by viewModel.dockStyle.collectAsState()
    val timelineLayoutMode by viewModel.timelineLayoutMode.collectAsState()
    val gridCellsCount by galleryViewModel.gridCellsCount.collectAsState()
    val thumbnailCornerRadius by viewModel.thumbnailCornerRadius.collectAsState()
    val ocrProgressState by viewModel.ocrProgress.collectAsState()
    val clipProgressState by viewModel.clipProgress.collectAsState()
    val ocrIndexWorkInfo by viewModel.ocrIndexWorkInfo.collectAsState(initial = null)
    val totalImagesCount by viewModel.totalImagesCount.collectAsState()
    val unindexedOcrImagesCount by viewModel.unindexedOcrImagesCount.collectAsState()
    val stripMetadataOnShare by viewModel.stripMetadataOnShare.collectAsState()
    val cacheThumbnailsEnabled by viewModel.cacheThumbnailsEnabled.collectAsState()
    val maxBrightnessEnabled by viewModel.maxBrightnessEnabled.collectAsState()
    val useSystemFont by viewModel.useSystemFont.collectAsState()
    val secureRecentsEnabled by viewModel.secureRecentsEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val hapticsStrength by viewModel.hapticsStrength.collectAsState()
    val colorPresetName by viewModel.colorPresetName.collectAsState()
    val contrastPreset by viewModel.contrastPreset.collectAsState()
    val secondaryColorOverride by viewModel.secondaryColorOverride.collectAsState()
    val tertiaryColorOverride by viewModel.tertiaryColorOverride.collectAsState()
    val viewerBlurEffect by viewModel.viewerBlurEffect.collectAsState()
    val animateThemeTransitions by viewModel.animateThemeTransitions.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allFolders by galleryViewModel.allBucketNames.collectAsState()

    val smartSearchModelDownloaded by viewModel.smartSearchModelDownloaded.collectAsState()
    val modelDownloadWorkInfo by viewModel.modelDownloadWorkInfo.collectAsState(initial = null)
    val smartSearchIndexWorkInfo by viewModel.smartSearchIndexWorkInfo.collectAsState(initial = null)
    val unindexedSmartSearchCount by viewModel.unindexedSmartSearchCount.collectAsState()
    val smartSearchAutoIndex by viewModel.smartSearchAutoIndex.collectAsState()


    var passwordVisiblePrimary by remember { mutableStateOf(false) }
    var passwordVisibleSecondary by remember { mutableStateOf(false) }



    var showClearIndexConfirm by remember { mutableStateOf(false) }
    var showDeleteModelConfirm by remember { mutableStateOf(false) }
    var showColorSchemeSheet by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }


    val isCurrentlyDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    if (showClearIndexConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearIndexConfirm = false },
            title = { Text("Clear Smart Search Index") },
            text = { Text("This will wipe out all computed image embeddings for semantic search. You will need to run the indexer again to use smart search. Are you sure you want to proceed?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showClearIndexConfirm = false
                        viewModel.clearSmartSearchEmbeddings()
                    }
                ) {
                    Text("Clear Index", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showClearIndexConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteModelConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteModelConfirm = false },
            title = { Text("Delete AI Model Files") },
            text = { Text("This will delete the local ONNX model files (approx. 30MB+). You will not be able to use semantic search or index new images until you download them again. Are you sure?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteModelConfirm = false
                        viewModel.deleteSmartSearchModel()
                    }
                ) {
                    Text("Delete Files", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteModelConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    if (showColorSchemeSheet) {
        com.inferno.gallery.ui.components.ColorSchemeBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showColorSchemeSheet = false }
        )
    }

    if (showLicensesDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Photon Gallery is built using open source software:",

                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Jetpack Compose (Apache 2.0 License)\n" +
                               "• Coil 3 (Apache 2.0 License)\n" +
                               "• Room Database (Apache 2.0 License)\n" +
                               "• ONNX Runtime (MIT License)\n" +
                               "• MobileCLIP (MIT License)\n" +
                               "• Roboto Flex & Google Sans Flex (SIL OFL 1.1)\n" +
                               "• AndroidX Lifecycle & DataStore (Apache 2.0 License)\n" +
                               "• AndroidX Navigation Compose (Apache 2.0 License)\n" +
                               "• AndroidX WorkManager (Apache 2.0 License)\n" +
                               "• AndroidX Graphics Shapes (Apache 2.0 License)\n" +
                               "• AndroidX Biometric (Apache 2.0 License)\n" +
                               "• Media3 ExoPlayer (Apache 2.0 License)\n" +
                               "• osmdroid (Apache 2.0 License)\n" +
                               "• Google ML Kit Text Recognition (Google APIs Terms of Service)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Notice", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Photon Gallery is built with a privacy-first approach.\n\n" +
                           "• All Smart Search, face detection, and OCR processing happens completely on your device.\n\n" +
                           "• No telemetry, usage statistics, tracking analytics, or personal data is collected or uploaded to any remote servers.\n\n" +
                           "• The application source code is fully open for anyone to inspect.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }


    androidx.activity.compose.BackHandler(enabled = activeSection != null) {
        onActiveSectionChange(null)
    }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
    ) {
            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(
                        animationSpec = MotionTokens.snappySpring()
                    ) togetherWith
                    androidx.compose.animation.fadeOut(
                        animationSpec = MotionTokens.snappySpring()
                    )
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "SettingsSectionContent"
            ) { section ->
                if (section == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        CategoryCard(
                            title = "Look \u0026 Feel",
                            subtitle = "Theme, colors, and display preferences",
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = { onActiveSectionChange("Look \u0026 Feel") }
                        )
                        CategoryCard(
                            title = "Layout \u0026 Navigation",
                            subtitle = "Grid size, dock style, and shapes",
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_tune),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = { onActiveSectionChange("Layout \u0026 Navigation") }
                        )
                        CategoryCard(
                            title = "General",
                            subtitle = "Full screen, brightness, and performance",
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_settings),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = { onActiveSectionChange("General") }
                        )
                        CategoryCard(
                            title = "Smart Search \u0026 OCR",
                            subtitle = "AI-powered search and text recognition",
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = { onActiveSectionChange("Smart Search \u0026 OCR") }
                        )
                        CategoryCard(
                            title = "Privacy \u0026 Security",
                            subtitle = "Metadata stripping, deletion, and data control",
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_shield),
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            iconColor = MaterialTheme.colorScheme.onErrorContainer,
                            onClick = { onActiveSectionChange("Privacy \u0026 Security") }
                        )
                        CategoryCard(
                            title = "Private Space",
                            subtitle = "Hidden photos protected with biometric lock",
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_lock),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = {
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                if (activity != null) {
                                    galleryViewModel.vaultAuthManager.authenticate(
                                        activity = activity,
                                        onSuccess = { onNavigateToVault() },
                                        onFailure = {}
                                    )
                                }
                            }
                        )
                        CategoryCard(
                            title = "Excluded Folders",
                            subtitle = "Hide folders from the main gallery",
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_folder_off),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { onActiveSectionChange("Excluded Folders") }
                        )
                        CategoryCard(
                            title = "About",
                            subtitle = "App information, updates, and licenses",
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_info),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = { onActiveSectionChange("About") }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (section) {
                            "Look & Feel" -> {
                                SettingsGroup(title = "Theme") {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        SingleChoiceSegmentedButtonRow(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            SegmentedButton(
                                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                                                selected = themeMode == ThemeMode.SYSTEM,
                                                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_wb_twilight), contentDescription = null) },
                                                label = { Text("System", style = MaterialTheme.typography.labelMedium) }
                                            )
                                            SegmentedButton(
                                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                                selected = themeMode == ThemeMode.LIGHT,
                                                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_light_mode), contentDescription = null) },
                                                label = { Text("Light", style = MaterialTheme.typography.labelMedium) }
                                            )
                                            SegmentedButton(
                                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                                                selected = themeMode == ThemeMode.DARK,
                                                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_dark_mode), contentDescription = null) },
                                                label = { Text("Dark", style = MaterialTheme.typography.labelMedium) }
                                            )
                                        }
                                    }

                                    

                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_palette), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Material You") },
                                        supportingContent = { Text("Use dynamic system colors") },
                                        trailingContent = {
                                            Switch(
                                                checked = useMaterialYou,
                                                onCheckedChange = { viewModel.setUseMaterialYou(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (useMaterialYou) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_contrast), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("AMOLED Black") },
                                        supportingContent = { Text("Use pitch black background in dark mode") },
                                        trailingContent = {
                                            Switch(
                                                checked = useAmoledBlack,
                                                onCheckedChange = { viewModel.setUseAmoledBlack(it) },
                                                enabled = isCurrentlyDark,
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (useAmoledBlack) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }

                                // ── Color Scheme ───────────────────────────────────────────────
                                SettingsGroup(title = "Theme Colors") {
                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_palette), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Color scheme") },
                                        supportingContent = { Text("Customize palette style, simple variants, and contrast") },
                                        trailingContent = {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        modifier = Modifier.clickable { showColorSchemeSheet = true },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }

                                SettingsGroup(title = "Display Preferences") {
                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_open_in_full), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Full Screen Mode") },
                                        supportingContent = { Text("Hide status bar and navigation bar to maximize content area") },
                                        trailingContent = {
                                            Switch(
                                                checked = useFullScreen,
                                                onCheckedChange = { viewModel.setUseFullScreen(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (useFullScreen) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_image), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Blur Viewer Background") },
                                        supportingContent = { Text("Show a blurred version of the photo behind it in the full screen viewer") },
                                        trailingContent = {
                                            Switch(
                                                checked = viewerBlurEffect,
                                                onCheckedChange = { viewModel.setViewerBlurEffect(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (viewerBlurEffect) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_folder), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Show Album Size") },
                                        supportingContent = { Text("Display total size of albums on the Albums screen") },
                                        trailingContent = {
                                            Switch(
                                                checked = showAlbumSize,
                                                onCheckedChange = { viewModel.setShowAlbumSize(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (showAlbumSize) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_light_mode), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Maximize Fullscreen Brightness") },
                                        supportingContent = { Text("Temporarily maximize screen brightness when viewing media in full screen") },
                                        trailingContent = {
                                            Switch(
                                                checked = maxBrightnessEnabled,
                                                onCheckedChange = { viewModel.setMaxBrightnessEnabled(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (maxBrightnessEnabled) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_text_fields), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Use System Font") },
                                        supportingContent = { Text("Use device default font instead of Photon's custom expressive typography") },
                                        trailingContent = {
                                            Switch(
                                                checked = useSystemFont,
                                                onCheckedChange = { viewModel.setUseSystemFont(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (useSystemFont) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }

                            }
                            "Layout & Navigation" -> {
                                SettingsGroup(title = "Timeline Layout") {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Main Gallery Grid Style",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Choose how photos and videos are arranged in your main timeline",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        SingleChoiceSegmentedButtonRow(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            SegmentedButton(
                                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                                onClick = { viewModel.setTimelineLayoutMode(com.inferno.gallery.data.TimelineLayoutMode.STANDARD_GRID) },
                                                selected = timelineLayoutMode == com.inferno.gallery.data.TimelineLayoutMode.STANDARD_GRID,
                                                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_grid_view), contentDescription = null) },
                                                label = { Text("Standard", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) }
                                            )
                                            SegmentedButton(
                                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                                onClick = { viewModel.setTimelineLayoutMode(com.inferno.gallery.data.TimelineLayoutMode.EDITORIAL_MOSAIC) },
                                                selected = timelineLayoutMode == com.inferno.gallery.data.TimelineLayoutMode.EDITORIAL_MOSAIC,
                                                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_dashboard), contentDescription = null) },
                                                label = { Text("Editorial", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) }
                                            )
                                            SegmentedButton(
                                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                                onClick = { viewModel.setTimelineLayoutMode(com.inferno.gallery.data.TimelineLayoutMode.STAGGERED_MASONRY) },
                                                selected = timelineLayoutMode == com.inferno.gallery.data.TimelineLayoutMode.STAGGERED_MASONRY,
                                                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_view_column), contentDescription = null) },
                                                label = { Text("Masonry", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) }
                                            )
                                        }
                                    }

                                    // Interactive Live Preview Demo Card
                                    TimelineLayoutPreviewCard(
                                        layoutMode = timelineLayoutMode,
                                        columns = gridCellsCount,
                                        cornerRadius = thumbnailCornerRadius
                                    )
                                }

                                SettingsGroup(title = "Dock & Grid") {
                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_list), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Full-Width Dock") },
                                        supportingContent = { Text("Use standard edge-to-edge dock instead of floating pill") },
                                        trailingContent = {
                                            val isDockFullWidth = dockStyle == com.inferno.gallery.data.DockStyle.FULL_WIDTH
                                            Switch(
                                                checked = isDockFullWidth,
                                                onCheckedChange = { isChecked ->
                                                    viewModel.setDockStyle(if (isChecked) com.inferno.gallery.data.DockStyle.FULL_WIDTH else com.inferno.gallery.data.DockStyle.PILL)
                                                },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (isDockFullWidth) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_visibility), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Show Hidden Albums") },
                                        supportingContent = { Text("Show albums that start with a dot (e.g., .nomedia folders)") },
                                        trailingContent = {
                                            Switch(
                                                checked = showHiddenAlbums,
                                                onCheckedChange = { isChecked ->
                                                    viewModel.setShowHiddenAlbums(isChecked)
                                                },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (showHiddenAlbums) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(text = "Grid Items per Row", style = MaterialTheme.typography.bodyLarge)
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "$gridCellsCount columns",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        androidx.compose.material3.Slider(
                                            value = gridCellsCount.toFloat(),
                                            onValueChange = { galleryViewModel.setGridCellsCount(it.toInt()) },
                                            valueRange = 2f..6f,
                                            steps = 3
                                        )
                                    }

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = "Thumbnail Corner Radius", style = MaterialTheme.typography.bodyLarge)
                                                Text(
                                                    text = "${thumbnailCornerRadius.toInt()} dp",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                modifier = Modifier.size(44.dp),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(thumbnailCornerRadius.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_image),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                        androidx.compose.material3.Slider(
                                            value = thumbnailCornerRadius,
                                            onValueChange = { viewModel.setThumbnailCornerRadius(it) },
                                            valueRange = 0f..24f
                                        )
                                    }
                                }
                            }
                            "General" -> {
                                SettingsGroup(title = "Haptics & Feedback") {
                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_touch_app), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Haptic Feedback") },
                                        supportingContent = { Text("Provide tactile feedback for taps and interactions") },
                                        trailingContent = {
                                            Switch(
                                                checked = hapticsEnabled,
                                                onCheckedChange = { viewModel.setHapticsEnabled(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (hapticsEnabled) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )

                                    if (hapticsEnabled) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )

                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(text = "Haptic Strength", style = MaterialTheme.typography.bodyLarge)
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Text(
                                                        text = "${(hapticsStrength * 100).toInt()}%",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            androidx.compose.material3.Slider(
                                                value = hapticsStrength,
                                                onValueChange = { viewModel.setHapticsStrength(it) },
                                                valueRange = 0f..1f,
                                                steps = 9
                                            )
                                        }
                                    }
                                }
                                SettingsGroup(title = "Performance") {
                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_tune), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Cache Grid Thumbnails") },
                                        supportingContent = { Text("Pre-cache grid thumbnails for instant, super-smooth scrolling (uses device storage)") },
                                        trailingContent = {
                                            Switch(
                                                checked = cacheThumbnailsEnabled,
                                                onCheckedChange = { viewModel.setCacheThumbnailsEnabled(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (cacheThumbnailsEnabled) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                                SettingsGroup(title = "Media Playback") {
                                    val autoplayWithSound by viewModel.autoplayWithSoundEnabled.collectAsState()
                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_volume_up), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Autoplay Video with Sound") },
                                        supportingContent = { Text("Play videos with sound automatically in full screen (muted by default)") },
                                        trailingContent = {
                                            Switch(
                                                checked = autoplayWithSound,
                                                onCheckedChange = { viewModel.setAutoplayWithSoundEnabled(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (autoplayWithSound) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }

                            }
                            "Smart Search & OCR" -> {

                                SettingsGroup(title = "Local Text Search (OCR)") {
                                    val dbIndexed = totalImagesCount - unindexedOcrImagesCount
                                    val isRunning = ocrProgressState.isIndexing
                                    val indexed = if (isRunning) ocrProgressState.progress else dbIndexed
                                    val total = if (isRunning) ocrProgressState.total else totalImagesCount

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Text (OCR) Indexing",
                                                style = MaterialTheme.typography.titleMedium,

                                                modifier = Modifier.weight(1f)
                                            )

                                            val badgeColor = if (isRunning) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                            val badgeTextColor = if (isRunning) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            val badgeText = if (isRunning) "Indexing" else "Idle"

                                            Surface(
                                                color = badgeColor,
                                                contentColor = badgeTextColor,
                                                shape = MaterialTheme.shapes.extraSmall,
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isRunning) {
                                                        androidx.compose.material3.LinearWavyProgressIndicator(
                                                            modifier = Modifier.size(width = 16.dp, height = 10.dp),
                                                            color = badgeTextColor,
                                                            trackColor = Color.Transparent
                                                        )
                                                    }
                                                    Text(
                                                        text = badgeText,
                                                        style = MaterialTheme.typography.labelSmall,

                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Index text in images for quick search.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (total > 0) {
                                            val progressFloat = (indexed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(10.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                                            shape = CircleShape
                                                        )
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(progressFloat)
                                                            .fillMaxHeight()
                                                            .background(
                                                                color = MaterialTheme.colorScheme.primary,
                                                                shape = CircleShape
                                                            )
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val currentImageName = ocrProgressState.currentImageName
                                                    Text(
                                                        text = if (isRunning && !currentImageName.isNullOrBlank()) "Scanning: $currentImageName"
                                                               else if (indexed == total) "Indexing complete"
                                                               else "Progress",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.weight(1f),
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (indexed == total && total > 0) "100%" else "$indexed / $total images (${(progressFloat * 100).toInt()}%)",
                                                        style = MaterialTheme.typography.labelMedium,

                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (isRunning) {
                                                ExpressiveButton(
                                                    onClick = { viewModel.stopOcrIndexing() },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Stop Indexing")
                                                }
                                            } else {
                                                FilledTonalButton(
                                                    onClick = { viewModel.startOcrIndexing() },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Start Indexing")
                                                }
                                                androidx.compose.material3.OutlinedButton(
                                                    onClick = { viewModel.rebuildOcrIndex() },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Rebuild Index")
                                                }
                                            }
                                        }
                                    }
                                }

                                SettingsGroup(title = "Local Semantic Search (AI)") {
                                    val isDownloading = modelDownloadWorkInfo?.state == WorkInfo.State.RUNNING || modelDownloadWorkInfo?.state == WorkInfo.State.ENQUEUED
                                    val downloadProgress = modelDownloadWorkInfo?.progress?.getInt("progress", 0) ?: 0
                                    val isIndexing = clipProgressState.isIndexing
                                    val unindexedCount = unindexedSmartSearchCount
                                    val indexedCount = maxOf(0, totalImagesCount - unindexedCount)
                                    val displayIndexed = if (isIndexing) clipProgressState.progress else indexedCount
                                    val displayTotal = if (isIndexing) clipProgressState.total else totalImagesCount
                                    val smartCurrentImageName = clipProgressState.currentImageName

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Smart Search (Semantic)",
                                                style = MaterialTheme.typography.titleMedium,

                                                modifier = Modifier.weight(1f)
                                            )

                                            val badgeColor = if (isIndexing) {
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            } else if (isDownloading) {
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            } else if (smartSearchModelDownloaded) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }

                                            val badgeTextColor = if (isIndexing) {
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            } else if (isDownloading) {
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            } else if (smartSearchModelDownloaded) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }

                                            val badgeText = if (isIndexing) {
                                                "Indexing"
                                            } else if (isDownloading) {
                                                "Downloading"
                                            } else if (smartSearchModelDownloaded) {
                                                "Ready"
                                            } else {
                                                "No Model"
                                            }

                                            Surface(
                                                color = badgeColor,
                                                contentColor = badgeTextColor,
                                                shape = MaterialTheme.shapes.extraSmall,
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isDownloading || isIndexing) {
                                                        androidx.compose.material3.LinearWavyProgressIndicator(
                                                            modifier = Modifier.size(width = 16.dp, height = 10.dp),
                                                            color = badgeTextColor,
                                                            trackColor = Color.Transparent
                                                        )
                                                    }
                                                    Text(
                                                        text = badgeText,
                                                        style = MaterialTheme.typography.labelSmall,

                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Find photos using AI concepts like 'sunset' or 'cat'.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (!smartSearchModelDownloaded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                                        shape = MaterialTheme.shapes.large
                                                    )
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Download AI model files (~100MB) to enable Smart Search.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )

                                                if (isDownloading) {
                                                    androidx.compose.material3.LinearWavyProgressIndicator(
                                                        progress = { downloadProgress / 100f },
                                                        modifier = Modifier.fillMaxWidth().height(10.dp)
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Downloading model: $downloadProgress%",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                        androidx.compose.material3.TextButton(
                                                            onClick = { viewModel.cancelModelDownload() },
                                                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                                                contentColor = MaterialTheme.colorScheme.error
                                                            )
                                                        ) {
                                                            Text("Cancel")
                                                        }
                                                    }
                                                } else {
                                                    ExpressiveButton(
                                                        onClick = { viewModel.startModelDownload() },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Download AI Model Files")
                                                    }
                                                }
                                            }
                                        } else {
                                            ListItem(
                                                leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                headlineContent = { Text("Auto Index New Images") },
                                                supportingContent = { Text("Index new photos automatically") },
                                                trailingContent = {
                                                    Switch(
                                                        checked = smartSearchAutoIndex,
                                                        onCheckedChange = { viewModel.setSmartSearchAutoIndex(it) },
                                                        thumbContent = {
                                                            Icon(
                                                                imageVector = if (smartSearchAutoIndex) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                                contentDescription = null,
                                                                modifier = Modifier.size(SwitchDefaults.IconSize)
                                                            )
                                                        }
                                                    )
                                                },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )

                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            )

                                            // Smart Search Indexing Progress
                                            val isIndexing = clipProgressState.isIndexing
                                            val unindexedCount = unindexedSmartSearchCount
                                            val indexedCount = maxOf(0, totalImagesCount - unindexedCount)
                                            val displayIndexed = if (isIndexing) clipProgressState.progress else indexedCount
                                            val displayTotal = if (isIndexing) clipProgressState.total else totalImagesCount
                                            val smartCurrentImageName = clipProgressState.currentImageName

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "AI Embedding Generation",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    val badgeColor = if (isIndexing) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                                    val badgeTextColor = if (isIndexing) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    val badgeText = if (isIndexing) "Indexing" else "Idle"

                                                    Surface(
                                                        color = badgeColor,
                                                        contentColor = badgeTextColor,
                                                        shape = MaterialTheme.shapes.extraSmall
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            if (isIndexing) {
                                                                androidx.compose.material3.LinearWavyProgressIndicator(
                                                                    modifier = Modifier.size(width = 16.dp, height = 10.dp),
                                                                    color = badgeTextColor,
                                                                    trackColor = Color.Transparent
                                                                )
                                                            }
                                                            Text(
                                                                text = badgeText,
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                    }
                                                }

                                                if (displayTotal > 0) {
                                                    val progressFloat = (displayIndexed.toFloat() / displayTotal.toFloat()).coerceIn(0f, 1f)
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(10.dp)
                                                                .background(
                                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                                    shape = CircleShape
                                                                )
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth(progressFloat)
                                                                    .fillMaxHeight()
                                                                    .background(
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        shape = CircleShape
                                                                    )
                                                            )
                                                        }

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = if (isIndexing && !smartCurrentImageName.isNullOrBlank()) "Scanning: $smartCurrentImageName"
                                                                       else if (displayIndexed == displayTotal) "AI indexing complete"
                                                                       else "Progress",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.weight(1f),
                                                                maxLines = 1,
                                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = if (displayIndexed == displayTotal && displayTotal > 0) "100%" else "$displayIndexed / $displayTotal images (${(progressFloat * 100).toInt()}%)",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (isIndexing) {
                                                        ExpressiveButton(
                                                            onClick = { viewModel.stopSmartSearchIndexing() },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                            ),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text("Stop Indexing")
                                                        }
                                                    } else {
                                                        FilledTonalButton(
                                                            onClick = { viewModel.startSmartSearchIndexing() },
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text("Start Indexing")
                                                        }
                                                        androidx.compose.material3.OutlinedButton(
                                                            onClick = { showClearIndexConfirm = true },
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text("Clear Index")
                                                        }
                                                    }
                                                }
                                                if (!isIndexing) {
                                                    androidx.compose.material3.OutlinedButton(
                                                        onClick = { showDeleteModelConfirm = true },
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            contentColor = MaterialTheme.colorScheme.error
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Delete AI Model Files")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "Privacy & Security" -> {
                                SettingsGroup(title = "Recycle Bin & Deletion") {
                                    val confirmDelete by viewModel.confirmDeleteEnabled.collectAsState()
                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_delete), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Confirm Deletion") },
                                        supportingContent = { Text("Show confirmation dialog when moving media to recycle bin") },
                                        trailingContent = {
                                            Switch(
                                                checked = confirmDelete,
                                                onCheckedChange = { viewModel.setConfirmDeleteEnabled(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (confirmDelete) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    val autoCleanTrash by viewModel.autoCleanTrashEnabled.collectAsState()
                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_delete), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Auto-clean Recycle Bin") },
                                        supportingContent = { Text("Automatically delete old items in the recycle bin") },
                                        trailingContent = {
                                            Switch(
                                                checked = autoCleanTrash,
                                                onCheckedChange = { viewModel.setAutoCleanTrashEnabled(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (autoCleanTrash) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )

                                    if (autoCleanTrash) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )

                                        val autoCleanDays by viewModel.autoCleanTrashDays.collectAsState()
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(text = "Retention Period", style = MaterialTheme.typography.bodyLarge)
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Text(
                                                        text = "$autoCleanDays Days",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            SingleChoiceSegmentedButtonRow(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                                    onClick = { viewModel.setAutoCleanTrashDays(7) },
                                                    selected = autoCleanDays == 7,
                                                    label = { Text("7 Days") }
                                                )
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                                    onClick = { viewModel.setAutoCleanTrashDays(14) },
                                                    selected = autoCleanDays == 14,
                                                    label = { Text("14 Days") }
                                                )
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                                    onClick = { viewModel.setAutoCleanTrashDays(30) },
                                                    selected = autoCleanDays == 30,
                                                    label = { Text("30 Days") }
                                                )
                                            }
                                        }
                                    }
                                }
                                SettingsGroup(title = "Screen & Recents Security") {
                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_shield_lock), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Hide Content in Recents") },
                                        supportingContent = { Text("Block viewing app content in the recent apps screen (also disables screenshots)") },
                                        trailingContent = {
                                            Switch(
                                                checked = secureRecentsEnabled,
                                                onCheckedChange = { viewModel.setSecureRecentsEnabled(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (secureRecentsEnabled) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }

                                SettingsGroup(title = "Metadata Privacy") {
                                    ListItem(
                                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_verified_user), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        headlineContent = { Text("Strip Metadata Before Sharing") },
                                        supportingContent = { Text("Remove GPS, camera specifications, timestamps, timezone offsets, and author/attribution details when sharing") },
                                        trailingContent = {
                                            Switch(
                                                checked = stripMetadataOnShare,
                                                onCheckedChange = { viewModel.setStripMetadataOnShare(it) },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (stripMetadataOnShare) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )

                                }

                            }
                            "Excluded Folders" -> {
                                val allFolders by galleryViewModel.allBucketNames.collectAsState()
                                val excluded by galleryViewModel.excludedFolders.collectAsState()

                                SettingsGroup(title = "Folder Visibility") {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                "Excluded folders won't appear in the Photos tab or Albums grid. You can still access them by searching.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }

                                if (allFolders.isEmpty()) {
                                    SettingsGroup(title = "") {
                                        ListItem(
                                            headlineContent = { Text("No folders found") },
                                            supportingContent = { Text("Media folders will appear here once scanned") },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                        )
                                    }
                                } else {
                                    SettingsGroup(title = "${excluded.size} folder${if (excluded.size != 1) "s" else ""} excluded") {
                                        allFolders.forEachIndexed { index, folderName ->
                                            if (index > 0) {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            }
                                            val isExcluded = excluded.contains(folderName)
                                            ListItem(
                                                headlineContent = { Text(folderName) },
                                                trailingContent = {
                                                    androidx.compose.material3.Switch(
                                                        checked = isExcluded,
                                                        onCheckedChange = { galleryViewModel.toggleExcludedFolder(folderName) },
                                                        thumbContent = {
                                                            Icon(
                                                                imageVector = if (isExcluded) ImageVector.vectorResource(R.drawable.ic_ms_visibility_off) else ImageVector.vectorResource(R.drawable.ic_ms_visibility),
                                                                contentDescription = null,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    )
                                                },
                                                leadingContent = {
                                                    Icon(
                                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                                        contentDescription = null,
                                                        tint = if (isExcluded) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                               else MaterialTheme.colorScheme.primary
                                                    )
                                                },
                                                modifier = Modifier.then(
                                                    if (isExcluded) Modifier.alpha(0.6f) else Modifier
                                                ),
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )
                                        }
                                    }
                                }
                            }
                            "About" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // 1. Hero App Identity Card
                                    Surface(
                                        shape = ShapeExtraLarge,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        tonalElevation = 2.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Real App Launcher Icon
                                            Surface(
                                                shape = ShapeLarge,
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                tonalElevation = 4.dp,
                                                modifier = Modifier.size(80.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    androidx.compose.foundation.Image(
                                                        painter = androidx.compose.ui.res.painterResource(id = com.inferno.gallery.R.drawable.launcher_icon_fg),
                                                        contentDescription = "Photon Gallery",
                                                        modifier = Modifier
                                                            .size(72.dp)
                                                            .clip(ShapeLarge)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Text(
                                                text = "Photon Gallery",
                                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Surface(
                                                shape = ShapeFull,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_code),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "Open Source",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // 2. Maintainer Card
                                    Text(
                                        text = "MAINTAINER",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.Start)
                                            .padding(horizontal = 24.dp, vertical = 6.dp)
                                    )

                                    Surface(
                                        shape = ShapeLarge,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { uriHandler.openUri("https://github.com/Bn5prS") }
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                                modifier = Modifier.size(56.dp)
                                            ) {
                                                AsyncImage(
                                                    model = "https://github.com/Bn5prS.png",
                                                    contentDescription = "Maintainer Profile",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Bn5prS",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Lead Developer",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            FilledTonalButton(
                                                onClick = { uriHandler.openUri("https://github.com/Bn5prS") },
                                                shape = ShapeFull,
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(imageVector = GithubIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Text("Profile", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // 3. Quick Action Buttons Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { uriHandler.openUri("https://github.com/Bn5prS/Photon_Gallery") },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = ShapeLarge
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(imageVector = GithubIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Text("Source", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }

                                        FilledTonalButton(
                                            onClick = { uriHandler.openUri("https://github.com/Bn5prS/Photon_Gallery/issues") },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = ShapeLarge
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_ms_bug_report), contentDescription = null, modifier = Modifier.size(18.dp))
                                                Text("Issues", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }

                                        FilledTonalButton(
                                            onClick = { showPrivacyDialog = true },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = ShapeLarge
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_ms_shield), contentDescription = null, modifier = Modifier.size(18.dp))
                                                Text("Privacy", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // 4. System Details Group (Single Tonal Card with Thin Dividers)
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    Text(
                                        text = "ABOUT",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.Start)
                                            .padding(horizontal = 24.dp, vertical = 6.dp)
                                    )

                                    Surface(
                                        shape = ShapeLarge,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            // Check for updates
                                            ListItem(
                                                leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_refresh), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                headlineContent = { Text("Check for Updates", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)) },
                                                supportingContent = { Text("Verify latest GitHub releases", style = MaterialTheme.typography.bodySmall) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        android.widget.Toast.makeText(context, "Photon Gallery is up to date (v1.0.0 (beta))", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )

                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                                thickness = 0.75.dp,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )

                                            // Version number
                                            ListItem(
                                                leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_info), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                headlineContent = { Text("Version & Build", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)) },
                                                supportingContent = { Text("v1.0.0 (beta) - Material 3 Expressive", style = MaterialTheme.typography.bodySmall) },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )

                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                                thickness = 0.75.dp,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )

                                            // Licenses
                                            ListItem(
                                                leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_description), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                headlineContent = { Text("Open Source Licenses", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)) },
                                                supportingContent = { Text("View third-party software notices", style = MaterialTheme.typography.bodySmall) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showLicensesDialog = true },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
    }
}

@Composable
fun SettingsGroup(
    title: String = "",
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }
        Card(
            shape = ShapeLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Card(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        },
        interactionSource = interactionSource,
        shape = ShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pressScale(isSelected = false, pressedScale = 0.97f, interactionSource = interactionSource)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

val GithubIcon: ImageVector = ImageVector.Builder(
    name = "Github",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).path(
    fill = SolidColor(Color.Black),
) {
    moveTo(12f, 2f)
    curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
    curveTo(2f, 16.42f, 4.865f, 20.166f, 8.839f, 21.489f)
    curveTo(9.339f, 21.581f, 9.521f, 21.272f, 9.521f, 21.007f)
    curveTo(9.521f, 20.77f, 9.513f, 20.141f, 9.508f, 19.307f)
    curveTo(6.726f, 19.91f, 6.139f, 17.967f, 6.139f, 17.967f)
    curveTo(5.685f, 16.811f, 5.029f, 16.503f, 5.029f, 16.503f)
    curveTo(4.121f, 15.883f, 5.09f, 15.895f, 5.09f, 15.895f)
    curveTo(6.093f, 15.965f, 6.621f, 16.925f, 6.621f, 16.925f)
    curveTo(7.513f, 18.454f, 8.962f, 18.012f, 9.531f, 17.756f)
    curveTo(9.623f, 17.11f, 9.881f, 16.67f, 10.167f, 16.42f)
    curveTo(7.947f, 16.167f, 5.612f, 15.31f, 5.612f, 11.477f)
    curveTo(5.612f, 10.386f, 6.002f, 9.493f, 6.641f, 8.794f)
    curveTo(6.538f, 8.541f, 6.195f, 7.524f, 6.739f, 6.147f)
    curveTo(6.739f, 6.147f, 7.579f, 5.878f, 9.489f, 7.172f)
    curveTo(10.289f, 6.95f, 11.144f, 6.839f, 12f, 6.839f)
    curveTo(12.856f, 6.839f, 13.711f, 6.95f, 14.511f, 7.172f)
    curveTo(16.421f, 5.878f, 17.261f, 6.147f, 17.261f, 6.147f)
    curveTo(17.805f, 7.524f, 17.462f, 8.541f, 17.359f, 8.794f)
    curveTo(17.998f, 9.493f, 18.388f, 10.386f, 18.388f, 11.477f)
    curveTo(18.388f, 15.32f, 16.05f, 16.164f, 13.823f, 16.412f)
    curveTo(14.182f, 16.721f, 14.501f, 17.331f, 14.501f, 18.264f)
    curveTo(14.501f, 19.6f, 14.489f, 20.679f, 14.489f, 21.007f)
    curveTo(14.489f, 21.275f, 14.669f, 21.587f, 15.177f, 21.489f)
    curveTo(19.141f, 20.163f, 22f, 16.42f, 22f, 12f)
    curveTo(22f, 6.477f, 17.522f, 2f, 12f, 2f)
    close()
}.build()

@Composable
private fun SetupGuideStep(
    stepNumber: Int,
    title: String,
    instructions: List<String>,
    highlight: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "$stepNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Column(
            modifier = Modifier.padding(start = 36.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            instructions.forEach { instruction ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        instruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (highlight != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        highlight,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}



// ── Timeline Layout Live Demo Preview Card ─────────────────────────────────────

@Composable
fun TimelineLayoutPreviewCard(
    layoutMode: com.inferno.gallery.data.TimelineLayoutMode,
    columns: Int,
    cornerRadius: Float
) {
    Surface(
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Layout Preview",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = when (layoutMode) {
                            com.inferno.gallery.data.TimelineLayoutMode.STANDARD_GRID -> "Standard Square"
                            com.inferno.gallery.data.TimelineLayoutMode.EDITORIAL_MOSAIC -> "Editorial Mosaic"
                            com.inferno.gallery.data.TimelineLayoutMode.STAGGERED_MASONRY -> "Staggered Masonry"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(ShapeMedium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(6.dp)
            ) {
                AnimatedContent(
                    targetState = layoutMode,
                    transitionSpec = {
                        fadeIn(MotionTokens.snappySpring()) togetherWith fadeOut(MotionTokens.snappySpring())
                    },
                    label = "previewLayoutTransition"
                ) { mode ->
                    when (mode) {
                        com.inferno.gallery.data.TimelineLayoutMode.STANDARD_GRID -> {
                            val cols = minOf(4, maxOf(2, columns))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(3) { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        repeat(cols) { col ->
                                            val seed = row * cols + col
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f),
                                                shape = RoundedCornerShape(cornerRadius.dp),
                                                color = getSampleColor(seed)
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }

                        com.inferno.gallery.data.TimelineLayoutMode.EDITORIAL_MOSAIC -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Row 1: Hero card (2 spans) + standard (1 span)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(2f)
                                            .height(84.dp),
                                        shape = RoundedCornerShape(cornerRadius.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Box(contentAlignment = Alignment.BottomStart, modifier = Modifier.padding(6.dp)) {
                                            Surface(
                                                shape = ShapeFull,
                                                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f),
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            ) {
                                                Text(
                                                    "Hero",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(84.dp),
                                        shape = RoundedCornerShape(cornerRadius.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {}
                                }
                                // Row 2: 3 standard cards
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp),
                                        shape = RoundedCornerShape(cornerRadius.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {}
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp),
                                        shape = RoundedCornerShape(cornerRadius.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {}
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp),
                                        shape = RoundedCornerShape(cornerRadius.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    ) {}
                                }
                            }
                        }

                        com.inferno.gallery.data.TimelineLayoutMode.STAGGERED_MASONRY -> {
                            val cols = minOf(3, maxOf(2, columns))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Column 1: Tall portrait + square
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().height(92.dp),
                                        shape = RoundedCornerShape(cornerRadius.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {}
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(cornerRadius.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {}
                                }
                                // Column 2: Landscape + tall portrait
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(cornerRadius.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {}
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().height(94.dp),
                                        shape = RoundedCornerShape(cornerRadius.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {}
                                }
                                if (cols >= 3) {
                                    // Column 3
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().height(72.dp),
                                            shape = RoundedCornerShape(cornerRadius.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                                        ) {}
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().height(72.dp),
                                            shape = RoundedCornerShape(cornerRadius.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        ) {}
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

@Composable
private fun getSampleColor(seed: Int): Color {
    return when (seed % 5) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        2 -> MaterialTheme.colorScheme.tertiaryContainer
        3 -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    }
}
