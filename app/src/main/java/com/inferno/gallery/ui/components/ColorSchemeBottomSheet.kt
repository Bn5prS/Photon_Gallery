package com.inferno.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.inferno.gallery.ui.SettingsViewModel
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.materialkolor.PaletteStyle
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSchemeBottomSheet(
    viewModel: SettingsViewModel,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val colorPresetName by viewModel.colorPresetName.collectAsState()
    val themePaletteStyleStr by viewModel.themePaletteStyle.collectAsState()
    val themeContrastLevel by viewModel.themeContrastLevel.collectAsState()
    
    var showPaletteStyleSelector by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = ShapeEdgeTop,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        if (showPaletteStyleSelector) {
            PaletteStyleSelector(
                currentStyle = themePaletteStyleStr,
                onStyleSelected = { style -> 
                    viewModel.setThemePaletteStyle(style.name)
                },
                onBack = { showPaletteStyleSelector = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_palette), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(IconSizeTokens.M))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Color scheme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }

                // Palette Style
                Card(
                    onClick = { showPaletteStyleSelector = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = ShapeExtraLarge
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_palette), contentDescription = null) },
                        headlineContent = { Text("Palette style") },
                        supportingContent = { Text(getPaletteStyleDisplayName(themePaletteStyleStr)) },
                        trailingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_edit), contentDescription = null) }
                    )
                }

                // Contrast Slider
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = ShapeExtraLarge
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_contrast), contentDescription = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Contrast", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = String.format("%.2f", themeContrastLevel),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        
                        Slider(
                            value = themeContrastLevel,
                            onValueChange = { viewModel.setThemeContrastLevel(it) },
                            valueRange = -1f..1f,
                            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        )
                    }
                }

                // Simple Variants (Seed Colors)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = ShapeExtraLarge
                ) {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text(
                            text = "Simple Variants",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)
                        )
                        
                        val appSeedColor by viewModel.appSeedColor.collectAsState()
                        val useMaterialYou by viewModel.useMaterialYou.collectAsState()
                        
                        // Predefined colors (beautiful, highly distinct vibrant hues for variants)
                        val predefinedColors = remember {
                            listOf(
                                Color(0xFFF44336), // Vibrant Red
                                Color(0xFFFF9800), // Bright Orange
                                Color(0xFFFFEB3B), // Golden Yellow
                                Color(0xFF8BC34A), // Lime Green
                                Color(0xFF4CAF50), // Standard Green
                                Color(0xFF009688), // Teal
                                Color(0xFF00BCD4), // Cyan
                                Color(0xFF03A9F4), // Light Blue
                                Color(0xFF2196F3), // Deep Blue
                                Color(0xFF673AB7), // Deep Purple
                                Color(0xFFE91E63), // Pink
                                Color(0xFF9C27B0), // Magenta
                            )
                        }
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Dynamic / Wallpaper variant
                            item {
                                ColorSwatch(
                                    color = MaterialTheme.colorScheme.primary, // Proxy for dynamic
                                    isSelected = useMaterialYou,
                                    isDynamicIcon = true,
                                    onClick = { 
                                        viewModel.setUseMaterialYou(true) 
                                        viewModel.setSecondaryColorOverride(-1)
                                        viewModel.setTertiaryColorOverride(-1)
                                    }
                                )
                            }
                            
                            items(predefinedColors) { color ->
                                val isSelected = !useMaterialYou && appSeedColor == color.toArgb()
                                ColorSwatch(
                                    color = color,
                                    isSelected = isSelected,
                                    onClick = {
                                        viewModel.setUseMaterialYou(false)
                                        viewModel.setAppSeedColor(color.toArgb())
                                        viewModel.setSecondaryColorOverride(-1)
                                        viewModel.setTertiaryColorOverride(-1)
                                    }
                                )
                            }
                        }
                        
                        // Custom color row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isCustomSelected = !useMaterialYou && !predefinedColors.map { it.toArgb() }.contains(appSeedColor)
                            val customColor = if (isCustomSelected) Color(appSeedColor) else Color.Transparent
                            
                            if (isCustomSelected) {
                                ColorSwatch(
                                    color = customColor,
                                    isSelected = true,
                                    onClick = {}
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { showCustomColorDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.ic_ms_add),
                                    contentDescription = "Add custom color",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
    
    if (showCustomColorDialog) {
        val appSeedColor by viewModel.appSeedColor.collectAsState()
        CustomColorPickerDialog(
            initialColor = Color(appSeedColor),
            onColorSelected = { color ->
                viewModel.setUseMaterialYou(false)
                viewModel.setAppSeedColor(color.toArgb())
                showCustomColorDialog = false
            },
            onDismissRequest = { showCustomColorDialog = false }
        )
    }
}

@Composable
fun PaletteStyleSelector(
    currentStyle: String,
    onStyleSelected: (PaletteStyle) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val styles = listOf(
            PaletteStyle.TonalSpot to "Default palette style, it allows to customize all four colors",
            PaletteStyle.Neutral to "A style that's slightly more chromatic than monochrome",
            PaletteStyle.Vibrant to "A loud theme, colorfulness is maximum for Primary palette",
            PaletteStyle.Expressive to "A playful theme - the source color's hue does not appear in the theme",
            PaletteStyle.Rainbow to "A playful theme - the source color's hue does not appear in the theme",
            PaletteStyle.FruitSalad to "A playful theme - the source color's hue does not appear in the theme",
            PaletteStyle.Monochrome to "A monochrome theme, colors are purely black / white / gray",
            PaletteStyle.Fidelity to "A theme that matches the source color exactly"
        )
        
        styles.forEach { (style, description) ->
            Card(
                onClick = { onStyleSelected(style) },
                colors = CardDefaults.cardColors(
                    containerColor = if (currentStyle == style.name) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getPaletteStyleDisplayName(style.name),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (currentStyle == style.name) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentStyle == style.name) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (currentStyle == style.name) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_ms_check_circle), 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_ms_circle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(ImageVector.vectorResource(R.drawable.ic_ms_palette), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Palette style", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDynamicIcon: Boolean = false
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val scheme = com.materialkolor.dynamicColorScheme(
        seedColor = color,
        isDark = isDark,
        style = PaletteStyle.TonalSpot
    )
    
    val colors = listOf(
        scheme.primary,
        scheme.tertiary,
        scheme.tertiaryContainer,
        scheme.primaryContainer
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Scalloped / Badge shape background
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.width / 2
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            
            // Draw scalloped outer border (approximate with 12 circles)
            val outerRadius = radius
            val innerRadius = radius * 0.85f
            
            if (isSelected) {
                // If selected, draw the scalloped border using a slightly darker/lighter shade
                val borderColor = scheme.outline
                for (i in 0 until 12) {
                    val angle = (i * 30) * (Math.PI / 180f)
                    val cx = center.x + (radius * 0.9f) * kotlin.math.cos(angle).toFloat()
                    val cy = center.y + (radius * 0.9f) * kotlin.math.sin(angle).toFloat()
                    drawCircle(color = borderColor, radius = radius * 0.15f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                }
                drawCircle(color = borderColor, radius = radius * 0.95f, center = center)
            } else {
                // Not selected, just draw a subtle background shadow/outline
                val shadowColor = scheme.outlineVariant.copy(alpha = 0.5f)
                for (i in 0 until 12) {
                    val angle = (i * 30) * (Math.PI / 180f)
                    val cx = center.x + (radius * 0.9f) * kotlin.math.cos(angle).toFloat()
                    val cy = center.y + (radius * 0.9f) * kotlin.math.sin(angle).toFloat()
                    drawCircle(color = shadowColor, radius = radius * 0.15f, center = androidx.compose.ui.geometry.Offset(cx, cy))
                }
                drawCircle(color = shadowColor, radius = radius * 0.95f, center = center)
            }

            // Draw the 4 quadrants
            val quadRadius = innerRadius
            
            if (isDynamicIcon) {
                drawCircle(color = scheme.primary, radius = quadRadius, center = center)
            } else {
                // Top-Right
                drawArc(
                    color = colors[1],
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - quadRadius, center.y - quadRadius),
                    size = androidx.compose.ui.geometry.Size(quadRadius * 2, quadRadius * 2)
                )
                // Bottom-Right
                drawArc(
                    color = colors[2],
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - quadRadius, center.y - quadRadius),
                    size = androidx.compose.ui.geometry.Size(quadRadius * 2, quadRadius * 2)
                )
                // Bottom-Left
                drawArc(
                    color = colors[3],
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - quadRadius, center.y - quadRadius),
                    size = androidx.compose.ui.geometry.Size(quadRadius * 2, quadRadius * 2)
                )
                // Top-Left
                drawArc(
                    color = colors[0],
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - quadRadius, center.y - quadRadius),
                    size = androidx.compose.ui.geometry.Size(quadRadius * 2, quadRadius * 2)
                )
            }
        }
        
        if (isDynamicIcon) {
            Icon(
                ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                contentDescription = "Dynamic",
                tint = scheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        } else if (isSelected) {
            Icon(
                ImageVector.vectorResource(R.drawable.ic_ms_check),
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.surface, // Often high contrast
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .padding(2.dp)
            )
        }
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    var red by remember { mutableStateOf(initialColor.red) }
    var green by remember { mutableStateOf(initialColor.green) }
    var blue by remember { mutableStateOf(initialColor.blue) }

    val currentColor = Color(red, green, blue)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Custom Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                )
                
                Column {
                    Text("Red", style = MaterialTheme.typography.labelMedium)
                    Slider(value = red, onValueChange = { red = it }, colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red.copy(alpha = 0.5f)))
                }
                Column {
                    Text("Green", style = MaterialTheme.typography.labelMedium)
                    Slider(value = green, onValueChange = { green = it }, colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green.copy(alpha = 0.5f)))
                }
                Column {
                    Text("Blue", style = MaterialTheme.typography.labelMedium)
                    Slider(value = blue, onValueChange = { blue = it }, colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue.copy(alpha = 0.5f)))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(currentColor) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

private fun getPaletteStyleDisplayName(name: String): String {
    return name.replace(Regex("([a-z])([A-Z]+)"), "$1 $2")
}
