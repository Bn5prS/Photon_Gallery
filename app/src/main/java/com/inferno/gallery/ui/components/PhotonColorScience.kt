package com.inferno.gallery.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.inferno.gallery.R
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.delay

@Composable
fun PhotonPaletteCard(
    name: String,
    colors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(16.dp)
    val cardColor = if (isSelected) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Column(
        modifier = modifier
            .clip(containerShape)
            .background(cardColor)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, containerShape)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), containerShape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            if (isSelected) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check_circle),
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.take(4).forEachIndexed { index, color ->
                val circleSize = if (index == 0) 22.dp else 16.dp
                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                )
            }
        }
    }
}

@Composable
fun PhotonPaletteGrid(
    currentStyle: String,
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    onStyleSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val styles = remember { PaletteStyle.entries }
    val chunkedStyles = remember(styles) { styles.chunked(2) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chunkedStyles.forEach { rowStyles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowStyles.forEach { style ->
                    val effectiveSeed = if (style == PaletteStyle.Monochrome) Color.Gray else seedColor
                    val previewScheme = rememberDynamicColorScheme(
                        seedColor = effectiveSeed,
                        isDark = isDark,
                        isAmoled = isAmoled,
                        style = style
                    )

                    val previewColors = listOf(
                        previewScheme.primary,
                        previewScheme.secondary,
                        previewScheme.tertiary,
                        previewScheme.surfaceContainerHighest
                    )

                    val formattedName = remember(style) {
                        style.name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
                    }

                    val isSelected = currentStyle.equals(style.name, ignoreCase = true)

                    PhotonPaletteCard(
                        name = formattedName,
                        colors = previewColors,
                        isSelected = isSelected,
                        onClick = { onStyleSelected(style.name) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowStyles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PhotonColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueDisplay: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    trackBrush: Brush? = null,
    thumbColor: Color? = null,
    enabled: Boolean = true
) {
    val scallopShape = MaterialShapes.Cookie4Sided.toShape()
    val activeThumb = thumbColor ?: MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            if (valueDisplay != null) {
                Text(
                    text = valueDisplay,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }

        if (trackBrush != null) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                contentAlignment = Alignment.Center
            ) {
                val totalWidthPx = constraints.maxWidth.toFloat()
                val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

                val trackHeight = 12.dp
                val thumbSize = 28.dp
                val gapDp = 3.dp
                val gapPx = with(LocalDensity.current) { gapDp.toPx() }
                val thumbHalf = with(LocalDensity.current) { (thumbSize / 2).toPx() }
                val availableTravel = (totalWidthPx - 2 * thumbHalf).coerceAtLeast(0f)
                val thumbCenterPx = thumbHalf + fraction * availableTravel
                val leftEndPx = (thumbCenterPx - thumbHalf - gapPx).coerceAtLeast(0f)
                val rightStartPx = (thumbCenterPx + thumbHalf + gapPx).coerceAtMost(totalWidthPx)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                ) {
                    val h = size.height
                    val outerCap = h / 2f
                    val innerCap = 3.dp.toPx()

                    if (leftEndPx > 0f) {
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = 0f,
                                    top = 0f,
                                    right = leftEndPx.coerceAtMost(size.width),
                                    bottom = h,
                                    topLeftCornerRadius = CornerRadius(outerCap, outerCap),
                                    bottomLeftCornerRadius = CornerRadius(outerCap, outerCap),
                                    topRightCornerRadius = CornerRadius(innerCap, innerCap),
                                    bottomRightCornerRadius = CornerRadius(innerCap, innerCap)
                                )
                            )
                        }
                        drawPath(path = path, brush = trackBrush)
                    }

                    if (rightStartPx < size.width) {
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = rightStartPx,
                                    top = 0f,
                                    right = size.width,
                                    bottom = h,
                                    topLeftCornerRadius = CornerRadius(innerCap, innerCap),
                                    bottomLeftCornerRadius = CornerRadius(innerCap, innerCap),
                                    topRightCornerRadius = CornerRadius(outerCap, outerCap),
                                    bottomRightCornerRadius = CornerRadius(outerCap, outerCap)
                                )
                            )
                        }
                        drawPath(path = path, brush = trackBrush)
                    }
                }

                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(thumbSize)
                                .shadow(elevation = 3.dp, shape = scallopShape)
                                .clip(scallopShape)
                                .background(if (enabled) activeThumb else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                                .border(1.5.dp, Color.White.copy(alpha = 0.9f), scallopShape)
                        )
                    },
                    track = {
                        Spacer(modifier = Modifier.fillMaxWidth())
                    }
                )
            }
        } else {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .shadow(elevation = 3.dp, shape = scallopShape)
                            .clip(scallopShape)
                            .background(if (enabled) activeThumb else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.9f), scallopShape)
                    )
                }
            )
        }
    }
}

@Composable
fun PhotonCustomColorSection(
    currentColor: Int,
    enabled: Boolean = true,
    onColorChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialHsl = remember(currentColor) {
        val out = FloatArray(3)
        ColorUtils.colorToHSL(currentColor, out)
        if (out[1] < 0.05f) out[1] = 0.8f
        if (out[2] !in 0.05f..0.95f) out[2] = 0.5f
        out
    }

    var localHue by rememberSaveable { mutableFloatStateOf(initialHsl[0]) }
    var localSaturation by rememberSaveable { mutableFloatStateOf(initialHsl[1]) }
    var localLightness by rememberSaveable { mutableFloatStateOf(initialHsl[2]) }

    var lastDispatchedColor by rememberSaveable { mutableIntStateOf(currentColor) }
    var pendingColor by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(currentColor) {
        if (currentColor != lastDispatchedColor && currentColor != -1) {
            val out = FloatArray(3)
            ColorUtils.colorToHSL(currentColor, out)
            localHue = out[0]
            localSaturation = if (out[1] < 0.05f) 0.8f else out[1]
            localLightness = if (out[2] !in 0.05f..0.95f) 0.5f else out[2]
        }
    }

    LaunchedEffect(pendingColor) {
        val color = pendingColor ?: return@LaunchedEffect
        delay(40)
        lastDispatchedColor = color
        onColorChanged(color)
    }

    fun updateColor(h: Float, s: Float, l: Float) {
        val calculated = ColorUtils.HSLToColor(floatArrayOf(h, s, l))
        pendingColor = calculated
    }

    val displayColor = remember(localHue, localSaturation, localLightness) {
        ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, localLightness))
    }

    val hexString = remember(displayColor) {
        String.format("#%06X", (0xFFFFFF and displayColor))
    }

    val hueGradient = remember {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFFF0000),
                Color(0xFFFFFF00),
                Color(0xFF00FF00),
                Color(0xFF00FFFF),
                Color(0xFF0000FF),
                Color(0xFFFF00FF),
                Color(0xFFFF0000)
            )
        )
    }

    val saturationGradient = remember(localHue, localLightness) {
        val start = ColorUtils.HSLToColor(floatArrayOf(localHue, 0f, localLightness))
        val end = ColorUtils.HSLToColor(floatArrayOf(localHue, 1f, localLightness))
        Brush.horizontalGradient(listOf(Color(start), Color(end)))
    }

    val lightnessGradient = remember(localHue, localSaturation) {
        val black = ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, 0f))
        val mid = ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, 0.5f))
        val white = ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, 1f))
        Brush.horizontalGradient(listOf(Color(black), Color(mid), Color(white)))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Color preview pill (with circular swatch + hex code)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(displayColor))
                    )
                    Text(
                        text = hexString,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        PhotonColorSlider(
            label = "Hue",
            value = localHue,
            onValueChange = {
                localHue = it
                updateColor(it, localSaturation, localLightness)
            },
            valueRange = 0f..360f,
            valueDisplay = "${localHue.toInt()}°",
            trackBrush = hueGradient,
            thumbColor = Color(ColorUtils.HSLToColor(floatArrayOf(localHue, 1f, 0.5f))),
            enabled = enabled
        )

        PhotonColorSlider(
            label = "Saturation",
            value = localSaturation,
            onValueChange = {
                localSaturation = it
                updateColor(localHue, it, localLightness)
            },
            valueRange = 0f..1f,
            valueDisplay = "${(localSaturation * 100).toInt()}%",
            trackBrush = saturationGradient,
            thumbColor = Color(ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, localLightness))),
            enabled = enabled
        )

        PhotonColorSlider(
            label = "Lightness",
            value = localLightness,
            onValueChange = {
                localLightness = it
                updateColor(localHue, localSaturation, it)
            },
            valueRange = 0f..1f,
            valueDisplay = "${(localLightness * 100).toInt()}%",
            trackBrush = lightnessGradient,
            thumbColor = Color(ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, localLightness))),
            enabled = enabled
        )
    }
}
