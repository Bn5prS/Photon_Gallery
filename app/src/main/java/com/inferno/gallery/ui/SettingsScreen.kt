package com.inferno.gallery.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.inferno.gallery.ui.components.FourQuadrantColorSwatch
import com.inferno.gallery.ui.components.PhotonConnectedCardDefaults
import com.inferno.gallery.ui.components.PhotonConnectedCardGroup
import com.inferno.gallery.ui.components.PhotonConnectedCardWrapper
import com.inferno.gallery.ui.components.PhotonBackButton
import com.inferno.gallery.ui.components.PhotonCloseButton
import com.inferno.gallery.ui.components.PhotonHeaderIconButton
import com.inferno.gallery.ui.components.PhotonPaletteGrid
import com.inferno.gallery.ui.components.PhotonCustomColorSection
import com.inferno.gallery.ui.theme.ThemePalettes
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.core.graphics.ColorUtils
import com.inferno.gallery.ui.theme.ShapeExtraSmall
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.IconSizeTokens
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import com.inferno.gallery.ui.components.LanguageSelectionDialog
import com.inferno.gallery.ui.components.getCurrentAppLanguageTag
import com.inferno.gallery.ui.components.getActiveLanguage
import com.inferno.gallery.ui.components.setAppLanguage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.inferno.gallery.BuildConfig
import com.inferno.gallery.R
import com.inferno.gallery.ui.components.AboutScreenContent
import com.inferno.gallery.ui.components.UpdateLogContent
import com.inferno.gallery.ui.components.ExpressiveButton
import com.inferno.gallery.ui.theme.ExpressiveShapes
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.photonColors
import com.inferno.gallery.ui.utils.pressScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val colorPresetName by viewModel.colorPresetName.collectAsState()
    val themePaletteStyle by viewModel.themePaletteStyle.collectAsState()
    val appSeedColor by viewModel.appSeedColor.collectAsState()
    val useAmoledBlack by viewModel.useAmoledBlack.collectAsState()
    val showAlbumSize by viewModel.showAlbumSize.collectAsState()
    val showHiddenAlbums by viewModel.showHiddenAlbums.collectAsState()
    val showEmptyAlbums by viewModel.showEmptyAlbums.collectAsState()
    val showMemories by viewModel.showMemories.collectAsState()
    val memoriesDismissed by viewModel.memoriesDismissed.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val dockStyle by viewModel.dockStyle.collectAsState()
    val showVideosInDock by viewModel.showVideosInDock.collectAsState()
    val timelineLayoutMode by viewModel.timelineLayoutMode.collectAsState()
    val gridCellsCount by galleryViewModel.gridCellsCount.collectAsState()
    val thumbnailCornerRadius by viewModel.thumbnailCornerRadius.collectAsState()
    val ocrProgressState by viewModel.ocrProgress.collectAsState()
    val clipProgressState by viewModel.clipProgress.collectAsState()
    val modelDownloadWorkInfo by viewModel.modelDownloadWorkInfo.collectAsState(initial = null)
    val totalImagesCount by viewModel.totalImagesCount.collectAsState()
    val unindexedOcrImagesCount by viewModel.unindexedOcrImagesCount.collectAsState()
    val stripMetadataOnShare by viewModel.stripMetadataOnShare.collectAsState()
    val maxBrightnessEnabled by viewModel.maxBrightnessEnabled.collectAsState()
    val useSystemFont by viewModel.useSystemFont.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val secureRecentsEnabled by viewModel.secureRecentsEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val hapticsStrength by viewModel.hapticsStrength.collectAsState()
    val viewerBlurEffect by viewModel.viewerBlurEffect.collectAsState()
    val viewerDynamicColor by viewModel.viewerDynamicColor.collectAsState()
    val viewerZoomMinimapEnabled by viewModel.viewerZoomMinimapEnabled.collectAsState()
    val confirmDelete by viewModel.confirmDeleteEnabled.collectAsState()
    val autoCleanTrash by viewModel.autoCleanTrashEnabled.collectAsState()
    val autoCleanDays by viewModel.autoCleanTrashDays.collectAsState()
    val autoplayWithSound by viewModel.autoplayWithSoundEnabled.collectAsState()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val dateHeaderAlignment by viewModel.dateHeaderAlignment.collectAsState()

    val smartSearchModelDownloaded by viewModel.smartSearchModelDownloaded.collectAsState()
    val unindexedSmartSearchCount by viewModel.unindexedSmartSearchCount.collectAsState()
    val smartSearchAutoIndex by viewModel.smartSearchAutoIndex.collectAsState()

    val faceRecognitionEnabled by viewModel.faceRecognitionEnabled.collectAsState()
    val faceModelDownloaded by viewModel.faceModelDownloaded.collectAsState()
    val unindexedFacesCount by viewModel.unindexedFacesCount.collectAsState()
    val faceProgressState by viewModel.faceProgress.collectAsState()
    val faceIndexWorkInfo by viewModel.faceIndexWorkInfo.collectAsState(initial = null)
    val faceModelDownloadWorkInfo by viewModel.faceModelDownloadWorkInfo.collectAsState(initial = null)
    val faceAutoIndex by viewModel.faceAutoIndex.collectAsState()

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current

    var showClearIndexConfirm by remember { mutableStateOf(false) }
    var showDeleteModelConfirm by remember { mutableStateOf(false) }
    var showClearFacesConfirm by remember { mutableStateOf(false) }
    var showRescanFacesConfirm by remember { mutableStateOf(false) }
    var showDeleteFaceModelConfirm by remember { mutableStateOf(false) }
    var showRebuildOcrConfirm by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguageTag by remember { mutableStateOf(getCurrentAppLanguageTag(context)) }
    var showCustomColorSheet by remember { mutableStateOf(false) }
    var showPaletteStyleSheet by remember { mutableStateOf(false) }

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

    val isCurrentlyDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    if (showCustomColorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCustomColorSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = ShapeEdgeTop,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.setting_custom_color_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.setting_custom_color_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showCustomColorSheet = false },
                        shape = CircleShape
                    ) {
                        Text(
                            text = stringResource(R.string.action_done),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                PhotonCustomColorSection(
                    currentColor = if (colorPresetName == "custom" && appSeedColor != -1) appSeedColor else 0xFF4285F4.toInt(),
                    enabled = true,
                    onColorChanged = { newColor ->
                        viewModel.setUseMaterialYou(false)
                        viewModel.setAppSeedColor(newColor)
                        viewModel.setColorPresetName("custom")
                    }
                )
            }
        }
    }

    if (showPaletteStyleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaletteStyleSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = ShapeEdgeTop,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.setting_palette_style_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.setting_palette_style_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showPaletteStyleSheet = false },
                        shape = CircleShape
                    ) {
                        Text(
                            text = stringResource(R.string.action_done),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                val effectiveSeedColor = if (colorPresetName == "custom" && appSeedColor != -1) {
                    Color(appSeedColor)
                } else if (useMaterialYou || colorPresetName == "dynamic") {
                    MaterialTheme.colorScheme.primary
                } else {
                    ThemePalettes.getById(colorPresetName).seedColor
                }

                PhotonPaletteGrid(
                    currentStyle = themePaletteStyle,
                    seedColor = effectiveSeedColor,
                    isDark = isCurrentlyDark,
                    isAmoled = useAmoledBlack && isCurrentlyDark,
                    onStyleSelected = { selectedStyle ->
                        viewModel.setThemePaletteStyle(selectedStyle)
                    }
                )
            }
        }
    }



    if (showClearFacesConfirm) {
        AlertDialog(
            onDismissRequest = { showClearFacesConfirm = false },
            title = { Text(stringResource(R.string.dialog_clear_face_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.dialog_clear_face_msg), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearFacesConfirm = false
                        viewModel.clearFaceIndex()
                    }
                ) {
                    Text(stringResource(R.string.dialog_clear_all_faces_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearFacesConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showClearIndexConfirm) {
        AlertDialog(
            onDismissRequest = { showClearIndexConfirm = false },
            title = { Text(stringResource(R.string.dialog_clear_smart_search_title)) },
            text = { Text(stringResource(R.string.dialog_clear_smart_search_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearIndexConfirm = false
                        viewModel.clearSmartSearchEmbeddings()
                    }
                ) {
                    Text(stringResource(R.string.btn_clear_index), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearIndexConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteModelConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteModelConfirm = false },
            title = { Text(stringResource(R.string.dialog_delete_ai_models_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.dialog_delete_ai_models_msg), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteModelConfirm = false
                        viewModel.deleteSmartSearchModel()
                    }
                ) {
                    Text(stringResource(R.string.dialog_delete_files_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteModelConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showRescanFacesConfirm) {
        AlertDialog(
            onDismissRequest = { showRescanFacesConfirm = false },
            title = { Text(stringResource(R.string.dialog_rescan_faces_title), style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = stringResource(R.string.dialog_rescan_faces_msg),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRescanFacesConfirm = false
                        viewModel.startFaceIndexing(forceRescan = true)
                    }
                ) {
                    Text(stringResource(R.string.dialog_rescan_all_confirm), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescanFacesConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteFaceModelConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteFaceModelConfirm = false },
            title = { Text(stringResource(R.string.dialog_delete_face_model_title), style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = stringResource(R.string.dialog_delete_face_model_msg),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteFaceModelConfirm = false
                        viewModel.deleteFaceModel()
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFaceModelConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showRebuildOcrConfirm) {
        AlertDialog(
            onDismissRequest = { showRebuildOcrConfirm = false },
            title = { Text(stringResource(R.string.dialog_rebuild_text_index_title), style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = stringResource(R.string.dialog_rebuild_text_index_msg),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRebuildOcrConfirm = false
                        viewModel.rebuildOcrIndex()
                    }
                ) {
                    Text(stringResource(R.string.btn_rebuild_index), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebuildOcrConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showLicensesDialog) {
        OpenSourceLicensesDialog(onDismiss = { showLicensesDialog = false })
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.dialog_privacy_notice_title), fontWeight = FontWeight.Bold) },
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
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(R.string.dialog_understood))
                }
            }
        )
    }


    androidx.activity.compose.BackHandler(enabled = activeSection != null) {
        onActiveSectionChange(if (activeSection == "Update Log") "About" else null)
    }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
    ) {
        AnimatedContent(
            targetState = activeSection,
            transitionSpec = {
                fadeIn(animationSpec = MotionTokens.snappySpring()) togetherWith
                        fadeOut(animationSpec = MotionTokens.snappySpring())
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "SettingsSectionContent"
        ) { section ->
            if (section == null) {
                // ── Main Settings Categories View (Grouped Cards) ──────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingsGroupCard(
                        title = stringResource(R.string.category_theme_display),
                        items = listOf(
                            {
                                SettingsCategoryRow(
                                    title = stringResource(R.string.category_theme_display),
                                    subtitle = stringResource(R.string.settings_category_theme_sub),
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    onClick = { onActiveSectionChange("Look & Feel") }
                                )
                            },
                            {
                                SettingsCategoryRow(
                                    title = stringResource(R.string.category_layout_navigation),
                                    subtitle = stringResource(R.string.settings_category_layout_sub),
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_tune),
                                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    onClick = { onActiveSectionChange("Layout & Navigation") }
                                )
                            }
                        )
                    )

                    SettingsGroupCard(
                        title = stringResource(R.string.category_general),
                        items = listOf(
                            {
                                SettingsCategoryRow(
                                    title = stringResource(R.string.category_general),
                                    subtitle = stringResource(R.string.settings_category_general_sub),
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_settings),
                                    iconContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = { onActiveSectionChange("General") }
                                )
                            },
                            {
                                SettingsCategoryRow(
                                    title = stringResource(R.string.smart_search),
                                    subtitle = stringResource(R.string.settings_category_smart_search_sub),
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    onClick = { onActiveSectionChange("Smart Search & OCR") }
                                )
                            }
                        )
                    )

                    SettingsGroupCard(
                        title = stringResource(R.string.category_security_privacy),
                        items = listOf(
                            {
                                SettingsCategoryRow(
                                    title = stringResource(R.string.category_security_privacy),
                                    subtitle = stringResource(R.string.settings_category_privacy_sub),
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_shield),
                                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    onClick = { onActiveSectionChange("Privacy & Security") }
                                )
                            },
                            {
                                SettingsCategoryRow(
                                    title = stringResource(R.string.setting_excluded_folders_category),
                                    subtitle = stringResource(R.string.setting_excluded_folders_category_sub),
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_folder_off),
                                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    onClick = { onActiveSectionChange("Excluded Folders") }
                                )
                            }
                        )
                    )

                    SettingsGroupCard(
                        title = stringResource(R.string.category_about),
                        items = listOf(
                            {
                                SettingsCategoryRow(
                                    title = stringResource(R.string.category_about),
                                    subtitle = stringResource(R.string.settings_category_about_sub),
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_info),
                                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    onClick = { onActiveSectionChange("About") }
                                )
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }
            } else {
                // ── Sub-Screens (Connected Segmented Cards) ────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (section) {
                        "Look & Feel" -> {
                            // ── Card 1: Theme & Colors ──
                            SettingsGroupCard(
                                title = stringResource(R.string.setting_theme_mode),
                                items = listOf(
                                    {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            SingleChoiceSegmentedButtonRow(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                                                    selected = themeMode == ThemeMode.SYSTEM,
                                                    icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_wb_twilight), contentDescription = null) },
                                                    label = { Text(stringResource(R.string.setting_theme_system), style = MaterialTheme.typography.labelMedium) }
                                                )
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                                    selected = themeMode == ThemeMode.LIGHT,
                                                    icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_light_mode), contentDescription = null) },
                                                    label = { Text(stringResource(R.string.setting_theme_light), style = MaterialTheme.typography.labelMedium) }
                                                )
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                                                    selected = themeMode == ThemeMode.DARK,
                                                    icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_dark_mode), contentDescription = null) },
                                                    label = { Text(stringResource(R.string.setting_theme_dark), style = MaterialTheme.typography.labelMedium) }
                                                )
                                            }
                                        }
                                    },
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_amoled_black),
                                            subtitle = stringResource(R.string.setting_amoled_black_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_contrast),
                                            checked = useAmoledBlack,
                                            onCheckedChange = { viewModel.setUseAmoledBlack(it) },
                                            enabled = isCurrentlyDark
                                        )
                                    },
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_material_you),
                                            subtitle = stringResource(R.string.setting_material_you_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                                            checked = useMaterialYou || colorPresetName == "dynamic",
                                            onCheckedChange = { viewModel.setUseMaterialYou(it) }
                                        )
                                    },
                                    {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = stringResource(R.string.setting_color_palette),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = if (useMaterialYou || colorPresetName == "dynamic") "Select any preset to switch from system wallpaper" else "Curated 4-color Material 3 combinations",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            LazyRow(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                            ) {
                                                items(ThemePalettes.allPalettes) { palette ->
                                                    FourQuadrantColorSwatch(
                                                        palette = palette,
                                                        isSelected = !useMaterialYou && colorPresetName != "dynamic" && (colorPresetName == palette.id),
                                                        onClick = {
                                                            viewModel.setUseMaterialYou(false)
                                                            viewModel.setColorPresetName(palette.id)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    {
                                        val isCustomActive = !useMaterialYou && colorPresetName == "custom"
                                        val effectiveColorInt = if (colorPresetName == "custom" && appSeedColor != -1) appSeedColor else 0xFF4285F4.toInt()
                                        val effectiveColor = Color(effectiveColorInt)
                                        val hexCode = String.format("#%06X", (0xFFFFFF and effectiveColorInt))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setUseMaterialYou(false)
                                                    viewModel.setColorPresetName("custom")
                                                    showCustomColorSheet = true
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(effectiveColor)
                                                        .border(
                                                            width = if (isCustomActive) 2.5.dp else 1.dp,
                                                            color = if (isCustomActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isCustomActive) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(if (ColorUtils.calculateLuminance(effectiveColorInt) > 0.5) Color.Black else Color.White)
                                                        )
                                                    }
                                                }
                                                Column {
                                                    Text(
                                                        text = stringResource(R.string.setting_custom_color_title),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = if (isCustomActive) hexCode else stringResource(R.string.setting_custom_color_sub),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.setUseMaterialYou(false)
                                                    viewModel.setColorPresetName("custom")
                                                    showCustomColorSheet = true
                                                },
                                                shape = CircleShape,
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.action_customize),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    },
                                    {
                                        val styleDisplayName = when (themePaletteStyle) {
                                            "TonalSpot" -> "Tonal Spot"
                                            "Neutral" -> "Neutral"
                                            "Vibrant" -> "Vibrant"
                                            "Expressive" -> "Expressive"
                                            "Rainbow" -> "Rainbow"
                                            "FruitSalad" -> "Fruit Salad"
                                            "Monochrome" -> "Monochrome"
                                            "Fidelity" -> "Fidelity"
                                            "Content" -> "Content"
                                            else -> themePaletteStyle
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showPaletteStyleSheet = true }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = stringResource(R.string.setting_palette_style_title),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = styleDisplayName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            FilledTonalButton(
                                                onClick = { showPaletteStyleSheet = true },
                                                shape = CircleShape,
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.action_change),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    }
                                )
                            )

                            // ── Card 2: Media Viewer ──
                            SettingsGroupCard(
                                title = stringResource(R.string.setting_media_viewer_title),
                                items = listOf(
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_dynamic_ambient_title),
                                            subtitle = stringResource(R.string.setting_dynamic_ambient_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                                            checked = viewerDynamicColor,
                                            onCheckedChange = { viewModel.setViewerDynamicColor(it) }
                                        )
                                    },
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_blur_viewer_bg_title),
                                            subtitle = stringResource(R.string.setting_blur_viewer_bg_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_image),
                                            checked = viewerBlurEffect,
                                            onCheckedChange = { viewModel.setViewerBlurEffect(it) }
                                        )
                                    },
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_zoom_navigator_title),
                                            subtitle = stringResource(R.string.setting_zoom_navigator_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_center_focus_strong),
                                            checked = viewerZoomMinimapEnabled,
                                            onCheckedChange = { viewModel.setViewerZoomMinimapEnabled(it) }
                                        )
                                    },
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_max_brightness_title),
                                            subtitle = stringResource(R.string.setting_max_brightness_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_light_mode),
                                            checked = maxBrightnessEnabled,
                                            onCheckedChange = { viewModel.setMaxBrightnessEnabled(it) }
                                        )
                                    }
                                )
                            )

                            // ── Card 3: Typography & Layout ──
                            SettingsGroupCard(
                                title = stringResource(R.string.setting_typography_layout_title),
                                items = listOf(
                                    {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = stringResource(R.string.setting_date_header_align),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (dateHeaderAlignment == "LEFT") {
                                                    "Left-aligned timeline date headers and floating scroll badge"
                                                } else {
                                                    "Center-aligned timeline date headers and floating scroll badge (Center-aligned style)"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            SingleChoiceSegmentedButtonRow(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                                    onClick = { viewModel.setDateHeaderAlignment("LEFT") },
                                                    selected = dateHeaderAlignment == "LEFT",
                                                    icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_format_align_left), contentDescription = null) },
                                                    label = { Text(stringResource(R.string.align_left), style = MaterialTheme.typography.labelMedium) }
                                                )
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                                    onClick = { viewModel.setDateHeaderAlignment("CENTER") },
                                                    selected = dateHeaderAlignment == "CENTER",
                                                    icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_format_align_center), contentDescription = null) },
                                                    label = { Text(stringResource(R.string.align_center), style = MaterialTheme.typography.labelMedium) }
                                                )
                                            }
                                        }
                                    },
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_show_album_size),
                                            subtitle = stringResource(R.string.setting_show_album_size_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                            checked = showAlbumSize,
                                            onCheckedChange = { viewModel.setShowAlbumSize(it) }
                                        )
                                    },
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_use_system_font_title),
                                            subtitle = stringResource(R.string.setting_use_system_font_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_text_fields),
                                            checked = useSystemFont,
                                            onCheckedChange = { viewModel.setUseSystemFont(it) }
                                        )
                                    },
                                    {
                                        FontScaleSettingItem(
                                            fontScale = fontScale,
                                            onFontScaleChange = { viewModel.setFontScale(it) },
                                            onReset = { viewModel.setFontScale(1.0f) }
                                        )
                                    }
                                )
                            )
                        }

                        "Layout & Navigation" -> {
                            SettingsGroupCard(
                                title = stringResource(R.string.setting_timeline_grid_title),
                                items = listOf(
                                    {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // 1. Grid Style Selector
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.setting_grid_style_title),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = stringResource(R.string.setting_grid_style_sub),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))

                                                SingleChoiceSegmentedButtonRow(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    SegmentedButton(
                                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                                        onClick = { viewModel.setTimelineLayoutMode(com.inferno.gallery.data.TimelineLayoutMode.STANDARD_GRID) },
                                                        selected = timelineLayoutMode == com.inferno.gallery.data.TimelineLayoutMode.STANDARD_GRID,
                                                        icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_grid_view), contentDescription = null) },
                                                        label = { Text(stringResource(R.string.layout_standard), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) }
                                                    )
                                                    SegmentedButton(
                                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                                        onClick = { viewModel.setTimelineLayoutMode(com.inferno.gallery.data.TimelineLayoutMode.EDITORIAL_MOSAIC) },
                                                        selected = timelineLayoutMode == com.inferno.gallery.data.TimelineLayoutMode.EDITORIAL_MOSAIC,
                                                        icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_dashboard), contentDescription = null) },
                                                        label = { Text(stringResource(R.string.layout_editorial_mosaic), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) }
                                                    )
                                                }
                                            }

                                            // 2. Live Layout Preview
                                            TimelineLayoutPreviewCard(
                                                layoutMode = timelineLayoutMode,
                                                columns = gridCellsCount,
                                                cornerRadius = thumbnailCornerRadius
                                            )

                                            // 3. Grid Items per Row Slider
                                            Column {
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column {
                                                        Text(text = stringResource(R.string.setting_grid_items_per_row), style = MaterialTheme.typography.bodyLarge)
                                                        Text(
                                                            text = stringResource(R.string.setting_grid_items_sub),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Surface(
                                                        shape = ShapeFull,
                                                        color = MaterialTheme.colorScheme.secondaryContainer
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.columns_count, gridCellsCount),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Slider(
                                                    value = gridCellsCount.toFloat().coerceIn(2f, 8f),
                                                    onValueChange = { galleryViewModel.setGridCellsCount(it.toInt()) },
                                                    valueRange = 2f..8f,
                                                    steps = 5
                                                )
                                            }

                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                            )

                                            // 4. Thumbnail Corner Radius Slider (Max 16dp limit with proportional preview)
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(text = stringResource(R.string.setting_thumbnail_corner_radius), style = MaterialTheme.typography.bodyLarge)
                                                        Text(
                                                            text = "${thumbnailCornerRadius.coerceIn(0f, 16f).toInt()} dp",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Surface(
                                                        modifier = Modifier.size(56.dp),
                                                        shape = RoundedCornerShape(thumbnailCornerRadius.coerceIn(0f, 16f).dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_image),
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Slider(
                                                    value = thumbnailCornerRadius.coerceIn(0f, 16f),
                                                    onValueChange = { viewModel.setThumbnailCornerRadius(it.coerceIn(0f, 16f)) },
                                                    valueRange = 0f..16f
                                                )
                                            }
                                        }
                                    }
                                )
                            )

                            SettingsGroupCard(
                                title = stringResource(R.string.setting_dock_navigation_title),
                                items = buildList<@Composable () -> Unit> {
                                    add {
                                        val isDockFullWidth = dockStyle == com.inferno.gallery.data.DockStyle.FULL_WIDTH
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_dock_classic),
                                            subtitle = stringResource(R.string.setting_dock_videos_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_list),
                                            checked = isDockFullWidth,
                                            onCheckedChange = { isChecked ->
                                                viewModel.setDockStyle(if (isChecked) com.inferno.gallery.data.DockStyle.FULL_WIDTH else com.inferno.gallery.data.DockStyle.PILL)
                                            }
                                        )
                                    }
                                    add {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_dock_videos),
                                            subtitle = stringResource(R.string.setting_dock_videos_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                                            checked = showVideosInDock,
                                            onCheckedChange = { viewModel.setShowVideosInDock(it) }
                                        )
                                    }
                                    add {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_memories),
                                            subtitle = stringResource(R.string.setting_memories_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_history),
                                            checked = showMemories,
                                            onCheckedChange = { viewModel.setShowMemories(it) }
                                        )
                                    }
                                    if (showMemories && memoriesDismissed) {
                                        add {
                                            ClickableSettingItem(
                                                title = stringResource(R.string.setting_restore_spotlight_title),
                                                subtitle = stringResource(R.string.setting_restore_spotlight_sub),
                                                icon = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                                onClick = { viewModel.restoreMemoriesBanner() }
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        "General" -> {
                            SettingsGroupCard(
                                title = stringResource(R.string.setting_albums_library_title),
                                items = listOf(
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_show_hidden),
                                            subtitle = stringResource(R.string.setting_show_hidden_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_visibility),
                                            checked = showHiddenAlbums,
                                            onCheckedChange = { viewModel.setShowHiddenAlbums(it) }
                                        )
                                    },
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_show_empty),
                                            subtitle = stringResource(R.string.setting_show_empty_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                            checked = showEmptyAlbums,
                                            onCheckedChange = { viewModel.setShowEmptyAlbums(it) }
                                        )
                                    }
                                )
                            )

                            val hapticsItems = mutableListOf<@Composable () -> Unit>(
                                {
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setting_haptics),
                                        subtitle = stringResource(R.string.setting_haptics_sub),
                                        icon = ImageVector.vectorResource(R.drawable.ic_ms_touch_app),
                                        checked = hapticsEnabled,
                                        onCheckedChange = { viewModel.setHapticsEnabled(it) }
                                    )
                                }
                            )

                            if (hapticsEnabled) {
                                hapticsItems.add {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(text = stringResource(R.string.setting_haptic_strength), style = MaterialTheme.typography.bodyLarge)
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
                                        Slider(
                                            value = hapticsStrength,
                                            onValueChange = { viewModel.setHapticsStrength(it) },
                                            valueRange = 0f..1f,
                                            steps = 9
                                        )
                                    }
                                }
                            }

                            SettingsGroupCard(
                                title = stringResource(R.string.setting_haptics_title),
                                items = hapticsItems
                            )


                            SettingsGroupCard(
                                title = stringResource(R.string.setting_media_playback_title),
                                items = listOf(
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_video_autoplay),
                                            subtitle = stringResource(R.string.setting_video_autoplay_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_volume_up),
                                            checked = autoplayWithSound,
                                            onCheckedChange = { viewModel.setAutoplayWithSoundEnabled(it) }
                                        )
                                    }
                                )
                            )

                            val activeLang = getActiveLanguage(context, currentLanguageTag)
                            val langSubtitle = if (currentLanguageTag.isEmpty()) {
                                "${stringResource(R.string.language_system_default)} (${activeLang.displayName})"
                            } else {
                                "${activeLang.flagEmoji} ${activeLang.displayName}"
                            }

                            SettingsGroupCard(
                                title = stringResource(R.string.select_language),
                                items = listOf(
                                    {
                                        ClickableSettingItem(
                                            title = stringResource(R.string.select_language),
                                            subtitle = langSubtitle,
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_menu_book),
                                            onClick = {
                                                showLanguageDialog = true
                                            }
                                        )
                                    }
                                )
                            )
                        }

                        "Smart Search & OCR" -> {
                            val dbIndexed = totalImagesCount - unindexedOcrImagesCount
                            val isOcrRunning = ocrProgressState.isIndexing
                            val ocrIndexed = if (isOcrRunning) ocrProgressState.progress else dbIndexed
                            val ocrTotal = if (isOcrRunning) ocrProgressState.total else totalImagesCount

                            // ── 100% On-Device & Open Source Privacy Explainer Card ──
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = ShapeLarge,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        shape = ShapeFull,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_shield_lock),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(IconSizeTokens.M)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "100% On-Device & Open Source",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "These AI models are open source and completely optional. You only need to download them once. All photo scanning, text recognition, and face clustering run entirely on your device—no images or personal data will ever be sent to any cloud or external server.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            SettingsGroupCard(
                                title = stringResource(R.string.setting_ocr_search),
                                items = listOf(
                                    {
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

                                                val badgeColor = if (isOcrRunning) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                                val badgeTextColor = if (isOcrRunning) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                val badgeText = if (isOcrRunning) "Indexing" else "Idle"

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
                                                        if (isOcrRunning) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(12.dp),
                                                                strokeWidth = 2.dp,
                                                                color = badgeTextColor
                                                            )
                                                        }
                                                        Text(
                                                            text = badgeText,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "Index text in images entirely on-device for instant, offline search.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (ocrTotal > 0) {
                                                val progressFloat = (ocrIndexed.toFloat() / ocrTotal.toFloat()).coerceIn(0f, 1f)
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
                                                            text = if (isOcrRunning && !currentImageName.isNullOrBlank()) "Scanning: $currentImageName"
                                                            else if (ocrIndexed == ocrTotal) "Indexing complete"
                                                            else "Progress",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.weight(1f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = if (ocrIndexed == ocrTotal && ocrTotal > 0) "100%" else "$ocrIndexed / $ocrTotal images (${(progressFloat * 100).toInt()}%)",
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
                                                if (isOcrRunning) {
                                                    ExpressiveButton(
                                                        onClick = { viewModel.stopOcrIndexing() },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(stringResource(R.string.btn_stop_indexing))
                                                    }
                                                } else {
                                                    FilledTonalButton(
                                                        onClick = { viewModel.startOcrIndexing() },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(stringResource(R.string.btn_start_indexing))
                                                    }
                                                    OutlinedButton(
                                                        onClick = { showRebuildOcrConfirm = true },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(stringResource(R.string.btn_rebuild_index))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            )

                            val isDownloading = modelDownloadWorkInfo?.state == WorkInfo.State.RUNNING || modelDownloadWorkInfo?.state == WorkInfo.State.ENQUEUED
                            val downloadProgress = modelDownloadWorkInfo?.progress?.getInt("progress", 0) ?: 0
                            val isClipIndexing = clipProgressState.isIndexing
                            val unindexedClip = unindexedSmartSearchCount
                            val indexedClip = maxOf(0, totalImagesCount - unindexedClip)
                            val displayClipIndexed = if (isClipIndexing) clipProgressState.progress else indexedClip
                            val displayClipTotal = if (isClipIndexing) clipProgressState.total else totalImagesCount
                            val smartCurrentImageName = clipProgressState.currentImageName

                            val smartSearchItems = mutableListOf<@Composable () -> Unit>()

                            if (!smartSearchModelDownloaded) {
                                smartSearchItems.add {
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
                                            Surface(
                                                shape = ExpressiveShapes.Clover4Leaf,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Icon(
                                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text(
                                                text = "Smart Search (Semantic AI)",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                color = if (isDownloading) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                                shape = ShapeFull
                                            ) {
                                                Text(
                                                    text = if (isDownloading) "Downloading" else "No Model",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isDownloading) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Download on-device OpenAI CLIP ViT-B/16 model (~150MB) to search photos using natural descriptions (e.g. 'dog sleeping on green rug', 'sunset at beach with friends'). Runs 100% offline on CPU.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row(
                                            modifier = Modifier
                                                .clip(ShapeSmall)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    uriHandler.openUri("https://huggingface.co/Xenova/clip-vit-base-patch16")
                                                }
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_open_in_new),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(IconSizeTokens.S)
                                            )
                                            Text(
                                                text = "Model source: Hugging Face (OpenAI CLIP ViT-B/16)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        if (isDownloading) {
                                            LinearWavyProgressIndicator(
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
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                TextButton(
                                                    onClick = { viewModel.cancelModelDownload() },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Text(stringResource(R.string.action_cancel))
                                                }
                                            }
                                        } else {
                                            ExpressiveButton(
                                                onClick = { viewModel.startModelDownload() },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(stringResource(R.string.btn_download_ai_models))
                                            }
                                        }
                                    }
                                }
                            } else {
                                smartSearchItems.add {
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setting_auto_index_images),
                                        subtitle = stringResource(R.string.setting_auto_index_images_sub),
                                        icon = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                        checked = smartSearchAutoIndex,
                                        onCheckedChange = { viewModel.setSmartSearchAutoIndex(it) }
                                    )
                                }

                                smartSearchItems.add {
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

                                            val badgeColor = if (isClipIndexing) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                            val badgeTextColor = if (isClipIndexing) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                            val badgeText = if (isClipIndexing) "Indexing" else "Ready"

                                            Surface(
                                                color = badgeColor,
                                                contentColor = badgeTextColor,
                                                shape = ShapeFull
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isClipIndexing) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(12.dp),
                                                            strokeWidth = 2.dp,
                                                            color = badgeTextColor
                                                        )
                                                    }
                                                    Text(
                                                        text = badgeText,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        }

                                        if (displayClipTotal > 0) {
                                            val progressFloat = (displayClipIndexed.toFloat() / displayClipTotal.toFloat()).coerceIn(0f, 1f)
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
                                                        text = if (isClipIndexing && !smartCurrentImageName.isNullOrBlank()) "Scanning: $smartCurrentImageName"
                                                        else if (displayClipIndexed == displayClipTotal) "AI indexing complete"
                                                        else "Progress",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.weight(1f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (displayClipIndexed == displayClipTotal && displayClipTotal > 0) "100%" else "$displayClipIndexed / $displayClipTotal images (${(progressFloat * 100).toInt()}%)",
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
                                            if (isClipIndexing) {
                                                ExpressiveButton(
                                                    onClick = { viewModel.stopSmartSearchIndexing() },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(stringResource(R.string.btn_stop_indexing))
                                                }
                                            } else {
                                                FilledTonalButton(
                                                    onClick = { viewModel.startSmartSearchIndexing() },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(stringResource(R.string.btn_start_indexing))
                                                }
                                                OutlinedButton(
                                                    onClick = { showClearIndexConfirm = true },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(stringResource(R.string.btn_clear_index))
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .clip(ShapeSmall)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    uriHandler.openUri("https://huggingface.co/Xenova/clip-vit-base-patch16")
                                                }
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_open_in_new),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(IconSizeTokens.S)
                                            )
                                            Text(
                                                text = "Model source: Hugging Face (OpenAI CLIP ViT-B/16)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        if (!isClipIndexing) {
                                            OutlinedButton(
                                                onClick = { showDeleteModelConfirm = true },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(stringResource(R.string.setting_delete_models))
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsGroupCard(
                                title = stringResource(R.string.setting_smart_search_title),
                                items = smartSearchItems
                            )

                            // ── Face Recognition & Clustering Group Card ──
                            val isFaceDownloading = faceModelDownloadWorkInfo?.state == androidx.work.WorkInfo.State.RUNNING ||
                                    faceModelDownloadWorkInfo?.state == androidx.work.WorkInfo.State.ENQUEUED
                            val isFaceIndexing = faceProgressState.isIndexing ||
                                    faceIndexWorkInfo?.state == androidx.work.WorkInfo.State.RUNNING ||
                                    faceIndexWorkInfo?.state == androidx.work.WorkInfo.State.ENQUEUED
                            val faceDownloadProgress = faceModelDownloadWorkInfo?.progress?.getInt("progress", 0) ?: 0
                            val faceProcessed = if (faceProgressState.total > 0) faceProgressState.progress else (faceIndexWorkInfo?.progress?.getInt("processed", 0) ?: 0)
                            val faceTotal = if (faceProgressState.total > 0) faceProgressState.total else (faceIndexWorkInfo?.progress?.getInt("total", 0) ?: 0)
                            val faceProgressFloat = if (faceTotal > 0) (faceProcessed.toFloat() / faceTotal.toFloat()).coerceIn(0f, 1f) else 0f
                            val faceItems = mutableListOf<@Composable () -> Unit>()

                            faceItems.add {
                                SwitchSettingItem(
                                    title = stringResource(R.string.setting_face_clustering),
                                    subtitle = stringResource(R.string.setting_face_clustering_desc),
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                    checked = faceRecognitionEnabled,
                                    onCheckedChange = { viewModel.setFaceRecognitionEnabled(it) }
                                )
                            }

                            if (faceRecognitionEnabled) {
                                if (!faceModelDownloaded) {
                                    faceItems.add {
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
                                                Surface(
                                                    shape = ShapeFull,
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                        Icon(
                                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Text(
                                                    text = "Face Recognition Model",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Surface(
                                                    color = if (isFaceDownloading) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                                    shape = ShapeFull
                                                ) {
                                                    Text(
                                                        text = if (isFaceDownloading) "Downloading" else "Needs Download",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isFaceDownloading) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "Download lightweight MobileFaceNet (~13MB) to cluster and group people directly on your device with 100% offline privacy.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .clip(ShapeSmall)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        uriHandler.openUri("https://huggingface.co/deepghs/insightface")
                                                    }
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_open_in_new),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(IconSizeTokens.S)
                                                )
                                                Text(
                                                    text = "Model source: Hugging Face (MobileFaceNet)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            if (isFaceDownloading) {
                                                LinearWavyProgressIndicator(
                                                    progress = { faceDownloadProgress / 100f },
                                                    modifier = Modifier.fillMaxWidth().height(10.dp)
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Downloading model: $faceDownloadProgress%",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    TextButton(
                                                        onClick = { viewModel.cancelFaceModelDownload() },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                    ) {
                                                        Text(stringResource(R.string.action_cancel))
                                                    }
                                                }
                                            } else {
                                                ExpressiveButton(
                                                    onClick = { viewModel.startFaceModelDownload() },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(stringResource(R.string.btn_download_face_model))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    faceItems.add {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_auto_index_faces),
                                            subtitle = stringResource(R.string.setting_auto_index_faces_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                            checked = faceAutoIndex,
                                            onCheckedChange = { viewModel.setFaceAutoIndex(it) }
                                        )
                                    }

                                    faceItems.add {
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
                                                    text = "Face Scanning & Clustering",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                val badgeColor = if (isFaceIndexing) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                                val badgeTextColor = if (isFaceIndexing) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                                val badgeText = if (isFaceIndexing) "Indexing" else if (unindexedFacesCount == 0) "Completed" else "Ready"

                                                Surface(
                                                    color = badgeColor,
                                                    contentColor = badgeTextColor,
                                                    shape = ShapeFull
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        if (isFaceIndexing) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(12.dp),
                                                                strokeWidth = 2.dp,
                                                                color = badgeTextColor
                                                            )
                                                        }
                                                        Text(
                                                            text = badgeText,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                            }

                                            if (isFaceIndexing || (faceTotal > 0 && faceProcessed < faceTotal)) {
                                                val animatedFaceProgress by animateFloatAsState(
                                                    targetValue = faceProgressFloat,
                                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                                    label = "faceProgress"
                                                )
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (faceTotal > 0) {
                                                        LinearWavyProgressIndicator(
                                                            progress = { animatedFaceProgress.coerceIn(0f, 1f) },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(10.dp)
                                                        )
                                                    } else {
                                                        LinearWavyProgressIndicator(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(10.dp)
                                                        )
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        val currentFaceImage = faceProgressState.currentImageName
                                                        Text(
                                                            text = if (isFaceIndexing) {
                                                                if (!currentFaceImage.isNullOrBlank()) "Scanning: $currentFaceImage"
                                                                else if (faceTotal > 0) "Scanning: $faceProcessed / $faceTotal photos"
                                                                else "Scanning faces in your gallery…"
                                                            } else if (faceProcessed == faceTotal && faceTotal > 0) {
                                                                "Face indexing complete"
                                                            } else {
                                                                "Progress"
                                                            },
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.weight(1f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        val pctInt = (animatedFaceProgress * 100).toInt()
                                                        val displayPct = if (faceProcessed == faceTotal && faceTotal > 0) "100%"
                                                            else if (faceProcessed > 0 && pctInt == 0) "<1%"
                                                            else "$pctInt%"
                                                        Text(
                                                            text = if (faceTotal > 0) "$faceProcessed / $faceTotal ($displayPct)" else displayPct,
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = if (unindexedFacesCount == 0) {
                                                        "All photos have been scanned and organized into People."
                                                    } else {
                                                        "$unindexedFacesCount photos waiting to be scanned for faces."
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (isFaceIndexing) {
                                                    ExpressiveButton(
                                                        onClick = { viewModel.stopFaceIndexing() },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(stringResource(R.string.btn_stop_scanning))
                                                    }
                                                } else {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            if (unindexedFacesCount == 0) {
                                                                showRescanFacesConfirm = true
                                                            } else {
                                                                viewModel.startFaceIndexing(forceRescan = false)
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(if (unindexedFacesCount == 0) "Rescan All Faces" else "Scan Faces Now")
                                                    }
                                                    OutlinedButton(
                                                        onClick = { showClearFacesConfirm = true },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(stringResource(R.string.btn_clear_faces))
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .clip(ShapeSmall)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        uriHandler.openUri("https://huggingface.co/deepghs/insightface")
                                                    }
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_open_in_new),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(IconSizeTokens.S)
                                                )
                                                Text(
                                                    text = "Model source: Hugging Face (MobileFaceNet)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            if (!isFaceIndexing) {
                                                OutlinedButton(
                                                    onClick = { showDeleteFaceModelConfirm = true },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(stringResource(R.string.dialog_delete_face_model_title))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsGroupCard(
                                title = stringResource(R.string.setting_face_clustering),
                                items = faceItems
                            )
                        }

                        "Privacy & Security" -> {
                            val recycleBinItems = mutableListOf<@Composable () -> Unit>(
                                {
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setting_confirm_delete),
                                        subtitle = stringResource(R.string.setting_confirm_delete_sub),
                                        icon = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                                        checked = confirmDelete,
                                        onCheckedChange = { viewModel.setConfirmDeleteEnabled(it) }
                                    )
                                },
                                {
                                    SwitchSettingItem(
                                        title = stringResource(R.string.auto_clean_trash),
                                        subtitle = stringResource(R.string.auto_clean_trash_desc),
                                        icon = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                                        checked = autoCleanTrash,
                                        onCheckedChange = { viewModel.setAutoCleanTrashEnabled(it) }
                                    )
                                }
                            )

                            if (autoCleanTrash) {
                                recycleBinItems.add {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(text = stringResource(R.string.setting_retention_period), style = MaterialTheme.typography.bodyLarge)
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.days_count, autoCleanDays),
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
                                                label = { Text(stringResource(R.string.days_count, 7)) }
                                            )
                                            SegmentedButton(
                                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                                onClick = { viewModel.setAutoCleanTrashDays(14) },
                                                selected = autoCleanDays == 14,
                                                label = { Text(stringResource(R.string.days_count, 14)) }
                                            )
                                            SegmentedButton(
                                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                                onClick = { viewModel.setAutoCleanTrashDays(30) },
                                                selected = autoCleanDays == 30,
                                                label = { Text(stringResource(R.string.days_count, 30)) }
                                            )
                                        }
                                    }
                                }
                            }

                            SettingsGroupCard(
                                title = stringResource(R.string.setting_recycle_bin_title),
                                items = recycleBinItems
                            )

                            SettingsGroupCard(
                                title = stringResource(R.string.setting_recents_security_title),
                                items = listOf(
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_hide_recents),
                                            subtitle = stringResource(R.string.setting_hide_recents_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_shield_lock),
                                            checked = secureRecentsEnabled,
                                            onCheckedChange = { viewModel.setSecureRecentsEnabled(it) }
                                        )
                                    }
                                )
                            )

                            SettingsGroupCard(
                                title = stringResource(R.string.setting_metadata_privacy_title),
                                items = listOf(
                                    {
                                        SwitchSettingItem(
                                            title = stringResource(R.string.setting_strip_metadata_title),
                                            subtitle = stringResource(R.string.setting_strip_metadata_sub),
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_verified_user),
                                            checked = stripMetadataOnShare,
                                            onCheckedChange = { viewModel.setStripMetadataOnShare(it) }
                                        )
                                    }
                                )
                            )
                        }

                        "Excluded Folders" -> {
                            val allFolders by galleryViewModel.allBucketNames.collectAsState()
                            val excludedFolders by galleryViewModel.excludedFolders.collectAsState()

                            SettingsGroupCard(
                                title = stringResource(R.string.setting_folder_visibility_title),
                                items = listOf(
                                    {
                                        Text(
                                            text = stringResource(R.string.setting_excluded_folders_desc),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                )
                            )

                            if (allFolders.isEmpty()) {
                                SettingsGroupCard(
                                    items = listOf(
                                        {
                                            SettingsRow(
                                                title = stringResource(R.string.setting_no_folders_found),
                                                subtitle = stringResource(R.string.setting_no_folders_found_sub)
                                            )
                                        }
                                    )
                                )
                            } else {
                                val folderItems = allFolders.map { folderName ->
                                    val isExcluded = excludedFolders.contains(folderName)
                                    val item: @Composable () -> Unit = {
                                        SettingsRow(
                                            title = folderName,
                                            icon = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                            iconTint = if (isExcluded) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.then(
                                                if (isExcluded) Modifier.alpha(0.6f) else Modifier
                                            ),
                                            trailing = {
                                                Switch(
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
                                            }
                                        )
                                    }
                                    item
                                }

                                SettingsGroupCard(
                                    title = "${excludedFolders.size} folder${if (excludedFolders.size != 1) "s" else ""} excluded",
                                    items = folderItems
                                )
                            }
                        }

                        "About" -> {
                            AboutScreenContent(
                                autoCheckUpdates = autoCheckUpdates,
                                updateState = updateState,
                                onAutoCheckUpdatesChange = { viewModel.setAutoCheckUpdates(it) },
                                onCheckForUpdates = { viewModel.checkForUpdates() },
                                onOpenUpdateLog = { onActiveSectionChange("Update Log") },
                                onOpenLicenses = { showLicensesDialog = true },
                                onOpenPrivacy = { showPrivacyDialog = true }
                            )
                        }

                        "Update Log" -> {
                            val pendingInfo = when (val state = updateState) {
                                is com.inferno.gallery.update.UpdateState.UpdateAvailable -> state.info
                                is com.inferno.gallery.update.UpdateState.ReadyToInstall -> state.info
                                else -> null
                            }
                            UpdateLogContent(pendingUpdate = pendingInfo)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ── Connected Segmented Card Components ────────────────────────────────────────

private data class CategoryItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val containerColor: Color,
    val iconColor: Color,
    val section: String
)

/**
 * Container for a group of settings items rendered in the connected segmented card style:
 * 2dp gap between items, with outer corners rounded to 16dp and intermediate corners rounded to 4dp.
 */
@Composable
fun SettingsGroupCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    items: List<@Composable () -> Unit>
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 2.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )
        }

        PhotonConnectedCardGroup(
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEachIndexed { index, itemContent ->
                PhotonConnectedCardWrapper(
                    index = index,
                    count = items.size,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    itemContent()
                }
            }
        }
    }
}

/**
 * Connected segmented settings category row for root screen.
 */
@Composable
private fun SettingsCategoryRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconContainerColor,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
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

/**
 * Standard segmented settings row: leading icon, title + optional supporting text, and an optional trailing control.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconTint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        titleColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) ImageVector.vectorResource(R.drawable.ic_ms_check) else ImageVector.vectorResource(R.drawable.ic_ms_close),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }
    )
}

@Composable
fun FontScaleSettingItem(
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember(fontScale) { mutableFloatStateOf(fontScale) }
    val percentage = (sliderPosition * 100).roundToInt()
    val scaleLabel = when {
        percentage <= 75 -> "Compact"
        percentage <= 90 -> "Small"
        percentage <= 105 -> "Default"
        percentage <= 120 -> "Medium"
        percentage <= 135 -> "Large"
        else -> "Extra Large"
    }

    val haptic = LocalHapticFeedback.current
    val currentDensity = LocalDensity.current
    val baseFontScale = if (fontScale > 0.01f) currentDensity.fontScale / fontScale else currentDensity.fontScale
    val previewDensity = remember(currentDensity.density, baseFontScale, sliderPosition) {
        Density(
            density = currentDensity.density,
            fontScale = baseFontScale * sliderPosition
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 1. Header with Title & Badge / Reset Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.setting_font_scale_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.setting_font_scale_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "$percentage% • $scaleLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                AnimatedVisibility(
                    visible = percentage != 100,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            sliderPosition = 1.0f
                            onReset()
                        },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                            contentDescription = "Reset font scale",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Live Preview Card (dynamically reacts to sliderPosition in real time)
        CompositionLocalProvider(LocalDensity provides previewDensity) {
            Surface(
                shape = ShapeMedium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.setting_live_typography_preview),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.setting_live_typography_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Oct 22 • 1,420 photos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Slider controls with - / + nudge buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val prev = ((sliderPosition - 0.05f) * 20f).roundToInt() / 20f
                    val clamped = prev.coerceIn(0.70f, 1.50f)
                    sliderPosition = clamped
                    onFontScaleChange(clamped)
                },
                enabled = sliderPosition > 0.705f,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_remove),
                    contentDescription = "Decrease font size",
                    tint = if (sliderPosition > 0.705f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Slider(
                value = sliderPosition.coerceIn(0.70f, 1.50f),
                onValueChange = { newValue ->
                    val snapped = ((newValue * 20f).roundToInt() / 20f).coerceIn(0.70f, 1.50f)
                    sliderPosition = snapped
                },
                onValueChangeFinished = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFontScaleChange(sliderPosition)
                },
                valueRange = 0.70f..1.50f,
                steps = 15,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val next = ((sliderPosition + 0.05f) * 20f).roundToInt() / 20f
                    val clamped = next.coerceIn(0.70f, 1.50f)
                    sliderPosition = clamped
                    onFontScaleChange(clamped)
                },
                enabled = sliderPosition < 1.495f,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_add),
                    contentDescription = "Increase font size",
                    tint = if (sliderPosition < 1.495f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 4. Tick Labels Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "70% Compact",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "100% Default",
                style = MaterialTheme.typography.labelSmall,
                color = if (percentage == 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (percentage == 100) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = "150% Large",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ClickableSettingItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    },
    onClick: () -> Unit
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconTint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        enabled = enabled,
        onClick = onClick,
        trailing = trailing
    )
}

@Composable
fun OpenSourceLicensesDialog(onDismiss: () -> Unit) {
    val licenses = listOf(
        "AndroidX & Jetpack Compose" to "Apache License 2.0 • Google LLC",
        "Coil 3 Image Loading" to "Apache License 2.0 • Coil Contributors",
        "Room Database" to "Apache License 2.0 • Google LLC",
        "ONNX Runtime" to "MIT License • Microsoft Corp.",
        "CLIP Vision & Language" to "MIT License • OpenAI",
        "Roboto Flex & Google Sans Flex" to "SIL Open Font License 1.1 • Google LLC",
        "AndroidX WorkManager" to "Apache License 2.0 • Google LLC",
        "AndroidX Graphics Shapes" to "Apache License 2.0 • Google LLC",
        "AndroidX Biometric" to "Apache License 2.0 • Google LLC",
        "Media3 ExoPlayer" to "Apache License 2.0 • Google LLC",
        "osmdroid" to "Apache License 2.0 • osmdroid contributors",
        "Google ML Kit Text Recognition" to "Google APIs Terms of Service • Google LLC",
        "Kotlin Coroutines & Flow" to "Apache License 2.0 • JetBrains"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Open Source Licenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 380.dp)
            ) {
                items(licenses) { (name, license) ->
                    Column {
                        Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

// ── Timeline Layout Live Demo Preview Card ─────────────────────────────────────

@Composable
fun TimelineLayoutPreviewCard(
    layoutMode: com.inferno.gallery.data.TimelineLayoutMode,
    columns: Int,
    cornerRadius: Float,
    modifier: Modifier = Modifier
) {
    val cols = columns.coerceIn(2, 6)
    val effectiveRadius = cornerRadius.coerceIn(0f, 16f)
    val previewRadius = (effectiveRadius * 0.45f).coerceIn(0f, 8f)

    Surface(
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setting_live_layout_preview),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "$cols col · ${effectiveRadius.toInt()}dp",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = when (layoutMode) {
                            com.inferno.gallery.data.TimelineLayoutMode.STANDARD_GRID -> "Standard Square"
                            com.inferno.gallery.data.TimelineLayoutMode.EDITORIAL_MOSAIC -> "Editorial Mosaic"
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
                    .padding(8.dp)
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
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(3) { row ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        repeat(cols) { col ->
                                            val seed = row * cols + col
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight(),
                                                shape = RoundedCornerShape(previewRadius.dp),
                                                color = getSampleColor(seed),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                            ) {
                                                if (seed == 1) {
                                                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.padding(4.dp)) {
                                                        Icon(
                                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                } else if (seed == 3) {
                                                    Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.padding(4.dp)) {
                                                        Icon(
                                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_favorite),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        com.inferno.gallery.data.TimelineLayoutMode.EDITORIAL_MOSAIC -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (cols == 2) {
                                    // Row 1: 2 items
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PreviewMosaicTile(modifier = Modifier.weight(1f).fillMaxHeight(), cornerRadius = effectiveRadius, seed = 0)
                                        PreviewMosaicTile(modifier = Modifier.weight(1f).fillMaxHeight(), cornerRadius = effectiveRadius, seed = 1, isVideo = true)
                                    }
                                    // Row 2: Full-width hero story tile
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1.35f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PreviewHeroTile(modifier = Modifier.fillMaxSize(), cornerRadius = effectiveRadius, seed = 2, cols = cols)
                                    }
                                    // Row 3: 2 items
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PreviewMosaicTile(modifier = Modifier.weight(1f).fillMaxHeight(), cornerRadius = effectiveRadius, seed = 3, isFavorite = true)
                                        PreviewMosaicTile(modifier = Modifier.weight(1f).fillMaxHeight(), cornerRadius = effectiveRadius, seed = 4)
                                    }
                                } else {
                                    // Row 1: Hero (weight 2f) + (cols - 2) standard items (weight 1f each)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1.35f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PreviewHeroTile(modifier = Modifier.weight(2f).fillMaxHeight(), cornerRadius = effectiveRadius, seed = 0, cols = cols)
                                        repeat(cols - 2) { idx ->
                                            PreviewMosaicTile(
                                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                                cornerRadius = effectiveRadius,
                                                seed = idx + 1,
                                                isVideo = (idx == 0)
                                            )
                                        }
                                    }

                                    // Row 2: cols standard items (weight 1f each)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        repeat(cols) { idx ->
                                            val seed = cols + idx
                                            PreviewMosaicTile(
                                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                                cornerRadius = effectiveRadius,
                                                seed = seed,
                                                isFavorite = (idx == cols - 1)
                                            )
                                        }
                                    }

                                    // Row 3: (cols - 2) standard items (weight 1f each) + Hero (weight 2f)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1.35f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        repeat(cols - 2) { idx ->
                                            val seed = cols * 2 + idx
                                            PreviewMosaicTile(
                                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                                cornerRadius = effectiveRadius,
                                                seed = seed
                                            )
                                        }
                                        PreviewHeroTile(
                                            modifier = Modifier.weight(2f).fillMaxHeight(),
                                            cornerRadius = effectiveRadius,
                                            seed = cols * 2 + (cols - 2),
                                            cols = cols,
                                            label = "Story"
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

@Composable
private fun PreviewHeroTile(
    modifier: Modifier = Modifier,
    cornerRadius: Float,
    seed: Int,
    cols: Int = 3,
    label: String = "Story"
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(4.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_camera),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(if (cols >= 5) 20.dp else 28.dp)
                    .align(Alignment.Center)
            )

            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                        contentDescription = null,
                        modifier = Modifier.size(9.dp)
                    )
                    Text(
                        text = if (cols >= 4) "Hero" else label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewMosaicTile(
    modifier: Modifier = Modifier,
    cornerRadius: Float,
    seed: Int,
    isVideo: Boolean = false,
    isFavorite: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        color = getSampleColor(seed),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            if (isVideo) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                        contentDescription = null,
                        modifier = Modifier
                            .size(13.dp)
                            .padding(2.dp)
                    )
                }
            } else if (isFavorite) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_favorite_filled),
                    contentDescription = null,
                    tint = com.inferno.gallery.ui.theme.LocalHarmonizedColors.current.favorite,
                    modifier = Modifier
                        .size(11.dp)
                        .align(Alignment.BottomEnd)
                )
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
