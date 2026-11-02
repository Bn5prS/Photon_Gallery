package com.inferno.gallery.ui.editor.studios

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.editor.engine.ImageProcessingEngine
import com.inferno.gallery.ui.editor.model.TuneValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 4 signature auto suggestions.
 */
enum class AutoSuggestionType(
    val title: String,
    val subtitle: String,
    @param:DrawableRes val iconRes: Int
) {
    ENHANCE("Enhance", "Auto levels & balance", R.drawable.ic_ms_auto_fix_high),
    DYNAMIC("Dynamic", "Shadow lift & vibrance", R.drawable.ic_ms_motion_photos_on),
    WARM("Warm", "Warmth & saturation", R.drawable.ic_ms_wb_twilight),
    COOL("Cool", "Cool tint & contrast", R.drawable.ic_ms_cloud)
}

/**
 * Encapsulates an auto suggestion and its tuned parameter payload.
 */
data class AutoSuggestion(
    val type: AutoSuggestionType,
    val tuneValues: TuneValues
)

/**
 * Extracted histogram and luminance metrics used for adaptive auto enhancement.
 */
data class ImageStats(
    val meanLuminance: Float = 128f,
    val minLuminance: Float = 0f,
    val maxLuminance: Float = 255f,
    val p5Luminance: Float = 10f,
    val p95Luminance: Float = 245f,
    val meanRed: Float = 128f,
    val meanGreen: Float = 128f,
    val meanBlue: Float = 128f
) {
    val dynamicRange: Float get() = (p95Luminance - p5Luminance).coerceAtLeast(0f)
    val colorTempBias: Float get() = meanRed - meanBlue
}

/**
 * Fast image-adaptive histogram analysis and auto suggestion generator engine.
 */
object AutoSuggestionsEngine {

    /**
     * Downsamples [bitmap] to analyze luminance mean, dynamic range percentiles,
     * and color temperature bias in microsecond/sub-millisecond time.
     */
    fun analyzeBitmap(bitmap: Bitmap?): ImageStats {
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
            return ImageStats()
        }

        val stepX = (bitmap.width / 64).coerceAtLeast(1)
        val stepY = (bitmap.height / 64).coerceAtLeast(1)
        var totalLum = 0.0
        var totalR = 0.0
        var totalG = 0.0
        var totalB = 0.0
        var sampleCount = 0

        val histogram = IntArray(256)

        for (y in 0 until bitmap.height step stepY) {
            for (x in 0 until bitmap.width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b).toInt().coerceIn(0, 255)

                histogram[lum]++
                totalLum += lum
                totalR += r
                totalG += g
                totalB += b
                sampleCount++
            }
        }

        if (sampleCount == 0) return ImageStats()

        val meanLum = (totalLum / sampleCount).toFloat()
        val meanR = (totalR / sampleCount).toFloat()
        val meanG = (totalG / sampleCount).toFloat()
        val meanB = (totalB / sampleCount).toFloat()

        // Percentiles
        val p5Target = (sampleCount * 0.05f).toInt()
        val p95Target = (sampleCount * 0.95f).toInt()
        var cum = 0
        var p5 = 0f
        var p95 = 255f
        for (i in 0..255) {
            cum += histogram[i]
            if (p5 == 0f && cum >= p5Target) {
                p5 = i.toFloat()
            }
            if (cum >= p95Target) {
                p95 = i.toFloat()
                break
            }
        }

        return ImageStats(
            meanLuminance = meanLum,
            p5Luminance = p5,
            p95Luminance = p95,
            meanRed = meanR,
            meanGreen = meanG,
            meanBlue = meanB
        )
    }

    /**
     * 1. Enhance: Auto-levels + contrast stretch + white balance correction.
     */
    fun computeEnhanceTune(stats: ImageStats): TuneValues {
        // Exposure correction
        val brightness = when {
            stats.meanLuminance < 110f -> ((120f - stats.meanLuminance) * 0.45f).coerceIn(0f, 35f)
            stats.meanLuminance > 145f -> ((135f - stats.meanLuminance) * 0.35f).coerceIn(-30f, 0f)
            else -> 8f
        }

        // Contrast stretch
        val dynamicRange = stats.dynamicRange
        val contrast = if (dynamicRange < 180f) {
            ((180f - dynamicRange) / 180f * 30f + 10f).coerceIn(10f, 35f)
        } else {
            12f
        }

        // White balance correction (color temperature bias)
        val tempBias = stats.colorTempBias
        val warmth = when {
            tempBias > 15f -> (-(tempBias - 15f) * 0.4f).coerceIn(-30f, 0f)
            tempBias < -15f -> (-(tempBias + 15f) * 0.4f).coerceIn(0f, 30f)
            else -> 0f
        }

        // Highlights recovery and shadow lift
        val highlights = if (stats.p95Luminance > 235f) -18f else -8f
        val shadows = if (stats.p5Luminance < 25f) 22f else 10f

        return TuneValues(
            brightness = brightness,
            contrast = contrast,
            tone = 20f,
            highlights = highlights,
            shadows = shadows,
            saturation = 10f,
            warmth = warmth
        )
    }

    /**
     * 2. Dynamic: Shadow lift (+35) + highlight recovery (-20) + saturation boost (+25) + clarity/tone (+30).
     */
    fun computeDynamicTune(): TuneValues = TuneValues(
        shadows = 35f,
        highlights = -20f,
        saturation = 25f,
        tone = 30f,
        contrast = 10f
    )

    /**
     * 3. Warm: Warmth (+40) + saturation (+15).
     */
    fun computeWarmTune(): TuneValues = TuneValues(
        warmth = 40f,
        saturation = 15f
    )

    /**
     * 4. Cool: Cool tint (-30) + contrast (+10).
     */
    fun computeCoolTune(): TuneValues = TuneValues(
        warmth = -30f,
        contrast = 10f,
        blueTone = 15f
    )

    /**
     * Produces all 4 auto suggestions with live image-adaptive parameters.
     */
    fun generateSuggestions(bitmap: Bitmap?): List<AutoSuggestion> {
        val stats = analyzeBitmap(bitmap)
        return listOf(
            AutoSuggestion(AutoSuggestionType.ENHANCE, computeEnhanceTune(stats)),
            AutoSuggestion(AutoSuggestionType.DYNAMIC, computeDynamicTune()),
            AutoSuggestion(AutoSuggestionType.WARM, computeWarmTune()),
            AutoSuggestion(AutoSuggestionType.COOL, computeCoolTune())
        )
    }
}

/**
 * Auto Suggestions Studio displaying image-adaptive Enhance, Dynamic, Warm, and Cool suggestions
 * with live thumbnail previews, checkmark badges, and one-tap application.
 */
@Composable
fun AutoStudio(
    previewBitmap: Bitmap?,
    onApplyTuneValues: (TuneValues) -> Unit,
    modifier: Modifier = Modifier,
    initialSelected: AutoSuggestionType? = null,
    onDone: (() -> Unit)? = null
) {
    var selectedType by rememberSaveable { mutableStateOf(initialSelected) }
    val view = LocalView.current

    // Background calculation of image-adaptive suggestions
    val suggestions by produceState(
        initialValue = AutoSuggestionsEngine.generateSuggestions(null),
        key1 = previewBitmap
    ) {
        value = withContext(Dispatchers.Default) {
            AutoSuggestionsEngine.generateSuggestions(previewBitmap)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Carousel of Suggestion Cards
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(suggestions, key = { it.type.name }) { suggestion ->
                val isSelected = selectedType == suggestion.type
                AutoSuggestionCard(
                    suggestion = suggestion,
                    previewBitmap = previewBitmap,
                    isSelected = isSelected,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        if (isSelected) {
                            // Toggle off, revert to default
                            selectedType = null
                            onApplyTuneValues(TuneValues())
                        } else {
                            // Apply suggestion
                            selectedType = suggestion.type
                            onApplyTuneValues(suggestion.tuneValues)
                        }
                    }
                )
            }
        }

        // Optional Done Button
        if (onDone != null) {
            FilledTonalButton(
                onClick = onDone,
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Overload matching plan interface:
 * `AutoStudio(previewBitmap, onSelectSuggestion)`
 */
@Composable
fun AutoStudio(
    previewBitmap: Bitmap?,
    onSelectSuggestion: (TuneValues) -> Unit,
    modifier: Modifier = Modifier
) {
    AutoStudio(
        previewBitmap = previewBitmap,
        onApplyTuneValues = onSelectSuggestion,
        modifier = modifier
    )
}

/**
 * Individual Auto Suggestion Card with live thumbnail preview, title, icon, and checkmark badge.
 */
@Composable
private fun AutoSuggestionCard(
    suggestion: AutoSuggestion,
    previewBitmap: Bitmap?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matrix = remember(suggestion.tuneValues) {
        ImageProcessingEngine.buildColorMatrix(suggestion.tuneValues)
    }

    Column(
        modifier = modifier
            .width(82.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Squircle Thumbnail Container
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(RoundedCornerShape(18.dp))
                .then(
                    if (isSelected) {
                        Modifier.border(
                            BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                            RoundedCornerShape(18.dp)
                        )
                    } else {
                        Modifier.border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            RoundedCornerShape(18.dp)
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = suggestion.type.title,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(matrix),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
            }

            // Selection Checkmark Badge
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ms_check),
                        contentDescription = "Applied",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(3.dp)
                            .size(14.dp)
                    )
                }
            }
        }

        // Title with Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Icon(
                painter = painterResource(id = suggestion.type.iconRes),
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(13.dp)
            )

            Text(
                text = suggestion.type.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
