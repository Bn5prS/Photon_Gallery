@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.components

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.SettingsViewModel
import com.inferno.gallery.ui.theme.CuratedColorPalettes
import com.inferno.gallery.ui.theme.CuratedPalette
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.theme.WallpaperSeedExtractor
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSchemeBottomSheet(
    viewModel: SettingsViewModel,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val themePaletteStyleStr by viewModel.themePaletteStyle.collectAsState()
    val appSeedColor by viewModel.appSeedColor.collectAsState()
    val useMaterialYou by viewModel.useMaterialYou.collectAsState()
    val themeContrastLevel by viewModel.themeContrastLevel.collectAsState()

    var showCustomColorDialog by remember { mutableStateOf(false) }

    val activePaletteStyle = remember(themePaletteStyleStr) {
        try {
            PaletteStyle.valueOf(themePaletteStyleStr)
        } catch (_: Exception) {
            PaletteStyle.TonalSpot
        }
    }

    val isDynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Resolve wallpaper primary seed
    val wallpaperSeedColor = remember(isDynamicAvailable) {
        if (isDynamicAvailable) {
            WallpaperSeedExtractor.getInstantWallpaperSeedColor(context) ?: 0xFF0A6EFF.toInt()
        } else {
            0xFF0A6EFF.toInt()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = ShapeEdgeTop,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SpacingTokens.L)
                .padding(bottom = SpacingTokens.XXL),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.L)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSizeTokens.L)
                    )
                    Spacer(modifier = Modifier.width(SpacingTokens.S))
                    Text(
                        text = "Theme Colors",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sleek Dual-Pill Switcher: Wallpaper vs Curated Colors
            if (isDynamicAvailable) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Tab 1: Wallpaper
                        Surface(
                            onClick = { viewModel.setUseMaterialYou(true) },
                            shape = CircleShape,
                            color = if (useMaterialYou) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                    contentDescription = null,
                                    tint = if (useMaterialYou) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(SpacingTokens.XS))
                                Text(
                                    text = "Wallpaper",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (useMaterialYou) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (useMaterialYou) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        // Tab 2: Curated Tones
                        Surface(
                            onClick = {
                                if (useMaterialYou) {
                                    viewModel.setCuratedPalette(appSeedColor)
                                }
                            },
                            shape = CircleShape,
                            color = if (!useMaterialYou) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                                    contentDescription = null,
                                    tint = if (!useMaterialYou) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(SpacingTokens.XS))
                                Text(
                                    text = "Curated Tones",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (!useMaterialYou) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (!useMaterialYou) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // SECTION A: Wallpaper Dynamic Palette Card
            if (useMaterialYou && isDynamicAvailable) {
                val wallpaperScheme = remember(wallpaperSeedColor, isDark, activePaletteStyle) {
                    dynamicColorScheme(
                        seedColor = Color(wallpaperSeedColor),
                        isDark = isDark,
                        style = activePaletteStyle
                    )
                }

                val cookie12Shape = MaterialShapes.Cookie12Sided.toShape()

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SpacingTokens.L),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                    ) {
                        // 4-Dot Harmonic Preview of Wallpaper in Cookie shapes
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                wallpaperScheme.primary,
                                wallpaperScheme.secondary,
                                wallpaperScheme.tertiary,
                                wallpaperScheme.primaryContainer
                            ).forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(cookie12Shape)
                                        .background(col)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamic Wallpaper Harmony",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Extracted from your phone's active wallpaper",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // SECTION B: Curated Distinct Colors Grid (4 columns, Cookie shape)
                val curatedList = CuratedColorPalettes.items
                val rows = remember(curatedList) { curatedList.chunked(4) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowItems.forEach { palette ->
                                val isSelected = !useMaterialYou && appSeedColor == palette.seedColor.toArgb()
                                CuratedColorSwatch(
                                    palette = palette,
                                    isSelected = isSelected,
                                    activeStyle = activePaletteStyle,
                                    onClick = {
                                        viewModel.setCuratedPalette(palette.seedColor.toArgb())
                                    }
                                )
                            }

                            // Pad last row if needed
                            if (rowItems.size < 4) {
                                val isCustomActive = !useMaterialYou && CuratedColorPalettes.findByArgb(appSeedColor) == null
                                CustomColorSwatch(
                                    color = if (isCustomActive) Color(appSeedColor) else null,
                                    isSelected = isCustomActive,
                                    onClick = { showCustomColorDialog = true }
                                )
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.width(68.dp))
                                }
                            }
                        }
                    }

                    // If exactly multiple of 4, custom swatch is placed on its own row
                    if (curatedList.size % 4 == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            val isCustomActive = !useMaterialYou && CuratedColorPalettes.findByArgb(appSeedColor) == null
                            CustomColorSwatch(
                                color = if (isCustomActive) Color(appSeedColor) else null,
                                isSelected = isCustomActive,
                                onClick = { showCustomColorDialog = true }
                            )
                        }
                    }
                }
            }

            // SECTION 2: HARMONIC MOOD (PALETTE STYLE)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
            ) {
                Text(
                    text = "Harmonic Mood",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                val moods = remember {
                    listOf(
                        "Balanced" to PaletteStyle.TonalSpot,
                        "Vibrant" to PaletteStyle.Vibrant,
                        "Expressive" to PaletteStyle.Expressive,
                        "Soft" to PaletteStyle.Neutral,
                        "Monochrome" to PaletteStyle.Monochrome
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(moods) { (label, style) ->
                        val isSelected = activePaletteStyle == style
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setThemePaletteStyle(style.name) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // SECTION 3: CONTRAST LEVEL
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contrast Level",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = when {
                                themeContrastLevel <= -0.5f -> "Reduced"
                                themeContrastLevel >= 0.75f -> "High"
                                themeContrastLevel in 0.25f..0.75f -> "Medium"
                                else -> "Default"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Slider(
                    value = themeContrastLevel,
                    onValueChange = { viewModel.setThemeContrastLevel(it) },
                    valueRange = -1f..1f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Done Button
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SpacingTokens.XS)
            ) {
                Text("Done", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showCustomColorDialog) {
        CustomColorPickerDialog(
            initialColor = Color(appSeedColor),
            paletteStyle = activePaletteStyle,
            onColorSelected = { color ->
                viewModel.setUseMaterialYou(false)
                viewModel.setAppSeedColor(color.toArgb())
                showCustomColorDialog = false
            },
            onDismissRequest = { showCustomColorDialog = false }
        )
    }
}

/**
 * Clean 54dp Curated Color Swatch with Cookie-12 morphic shape and 4-quadrant dynamic harmony.
 */
@Composable
private fun CuratedColorSwatch(
    palette: CuratedPalette,
    isSelected: Boolean,
    activeStyle: PaletteStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scheme = remember(palette.seedColor, isDark, activeStyle) {
        dynamicColorScheme(seedColor = palette.seedColor, isDark = isDark, style = activeStyle)
    }

    val cookie12Shape = MaterialShapes.Cookie12Sided.toShape()

    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 1.dp,
        label = "swatch_border"
    )

    Column(
        modifier = modifier
            .width(68.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(cookie12Shape)
                .border(
                    width = animatedBorderWidth,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = cookie12Shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Top-Left (Primary)
                drawArc(
                    color = scheme.primary,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )
                // Top-Right (Tertiary)
                drawArc(
                    color = scheme.tertiary,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )
                // Bottom-Right (PrimaryContainer)
                drawArc(
                    color = scheme.primaryContainer,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )
                // Bottom-Left (TertiaryContainer)
                drawArc(
                    color = scheme.tertiaryContainer,
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Text(
            text = palette.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Custom Color Swatch with Cookie-12 morphic shape and edit icon.
 */
@Composable
private fun CustomColorSwatch(
    color: Color?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cookie12Shape = MaterialShapes.Cookie12Sided.toShape()

    Column(
        modifier = modifier
            .width(68.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(cookie12Shape)
                .background(color ?: MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = cookie12Shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (color != null && isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_edit),
                    contentDescription = "Custom Color",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = if (color != null && isSelected) String.format("#%06X", 0xFFFFFF and color.toArgb()) else "Custom",
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Accurate Material 3 Custom Color Picker Dialog with HSV sliders, Hex input, and live palette preview.
 */
@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val currentColor = remember(hue, saturation, value) {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        Color(argb)
    }

    var hexText by remember(currentColor) {
        mutableStateOf(String.format("%06X", 0xFFFFFF and currentColor.toArgb()))
    }
    var hexError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val previewScheme = remember(currentColor, isDark, paletteStyle) {
        dynamicColorScheme(seedColor = currentColor, isDark = isDark, style = paletteStyle)
    }

    val cookie12Shape = MaterialShapes.Cookie12Sided.toShape()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(SpacingTokens.XS))
                Text("Custom Color", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
            ) {
                // Color Display Header with Cookie-12 shape
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(SpacingTokens.S),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(cookie12Shape)
                            .background(currentColor)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, cookie12Shape)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hue: ${hue.roundToInt()}° • Sat: ${(saturation * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "RGB: ${(currentColor.red * 255).roundToInt()}, ${(currentColor.green * 255).roundToInt()}, ${(currentColor.blue * 255).roundToInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Live M3 Palette Role Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val roles = listOf(
                        "Primary" to previewScheme.primary,
                        "Secondary" to previewScheme.secondary,
                        "Tertiary" to previewScheme.tertiary,
                        "Container" to previewScheme.primaryContainer
                    )
                    roles.forEach { (label, col) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp),
                            color = col,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (col == previewScheme.primaryContainer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.surface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Hue Spectrum Slider
                Column {
                    Text(
                        text = "Hue",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Red,
                                        Color.Yellow,
                                        Color.Green,
                                        Color.Cyan,
                                        Color.Blue,
                                        Color.Magenta,
                                        Color.Red
                                    )
                                )
                            )
                    )
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Saturation Slider
                Column {
                    Text(
                        text = "Saturation: ${(saturation * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Brightness / Value Slider
                Column {
                    Text(
                        text = "Brightness: ${(value * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Hex Code Input Field
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        val cleaned = input.removePrefix("#").take(6).uppercase()
                        hexText = cleaned
                        if (cleaned.length == 6) {
                            try {
                                val parsedArgb = (0xFF000000.toInt()) or cleaned.toInt(16)
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsedArgb, hsv)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                                hexError = false
                            } catch (_: Exception) {
                                hexError = true
                            }
                        } else {
                            hexError = cleaned.length in 1..5
                        }
                    },
                    label = { Text("Hex Code (#RRGGBB)") },
                    prefix = { Text("#") },
                    isError = hexError,
                    supportingText = if (hexError) { { Text("Enter a valid 6-digit hex code") } } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(currentColor) },
                shape = MaterialTheme.shapes.large
            ) {
                Text("Select & Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                shape = MaterialTheme.shapes.large
            ) {
                Text("Cancel")
            }
        }
    )
}
