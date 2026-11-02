package com.inferno.gallery.ui.editor.model

import androidx.annotation.DrawableRes
import com.inferno.gallery.R
import kotlin.math.abs

/**
 * 13 granular image adjustment controls in standard order,
 * ranges, defaults, icons, and descriptions.
 */
enum class AdjustTool(
    val label: String,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float,
    @param:DrawableRes val iconRes: Int,
    val description: String
) {
    BRIGHTNESS(
        label = "Brightness",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_light_mode,
        description = "Adjust the overall lightness or darkness"
    ),
    CONTRAST(
        label = "Contrast",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_contrast,
        description = "Adjust the difference between light and dark areas"
    ),
    TONE(
        label = "Tone",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_auto_fix_high,
        description = "Enhance dynamic range and local contrast"
    ),
    WHITE_POINT(
        label = "White point",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_circle,
        description = "Adjust the brightest parts of the image"
    ),
    HIGHLIGHTS(
        label = "Highlights",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_brightness_medium,
        description = "Recover or boost bright details"
    ),
    SHADOWS(
        label = "Shadows",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_brightness_low,
        description = "Lighten or deepen dark details"
    ),
    BLACK_POINT(
        label = "Black point",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_circle,
        description = "Adjust the darkest parts of the image"
    ),
    SATURATION(
        label = "Saturation",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_palette,
        description = "Make colors more or less vibrant"
    ),
    WARMTH(
        label = "Warmth",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_wb_twilight,
        description = "Make the image appear warmer or cooler"
    ),
    TINT(
        label = "Tint",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_palette,
        description = "Adjust color balance between magenta and green"
    ),
    SKIN_TONE(
        label = "Skin tone",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_person,
        description = "Adjust warmth and exposure specifically for skin tones"
    ),
    BLUE_TONE(
        label = "Blue tone",
        minValue = -100f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_cloud,
        description = "Make skies and blues more vivid or softened"
    ),
    VIGNETTE(
        label = "Vignette",
        minValue = 0f,
        maxValue = 100f,
        defaultValue = 0f,
        iconRes = R.drawable.ic_ms_center_focus_strong,
        description = "Darken the edges of the image"
    );

    val range: ClosedFloatingPointRange<Float>
        get() = minValue..maxValue
}

/**
 * Immutable tuning parameters holding values for all 13 image adjustments.
 */
data class TuneValues(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val tone: Float = 0f,
    val whitePoint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val blackPoint: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val skinTone: Float = 0f,
    val blueTone: Float = 0f,
    val vignette: Float = 0f
) {
    fun getValue(tool: AdjustTool): Float = when (tool) {
        AdjustTool.BRIGHTNESS -> brightness
        AdjustTool.CONTRAST -> contrast
        AdjustTool.TONE -> tone
        AdjustTool.WHITE_POINT -> whitePoint
        AdjustTool.HIGHLIGHTS -> highlights
        AdjustTool.SHADOWS -> shadows
        AdjustTool.BLACK_POINT -> blackPoint
        AdjustTool.SATURATION -> saturation
        AdjustTool.WARMTH -> warmth
        AdjustTool.TINT -> tint
        AdjustTool.SKIN_TONE -> skinTone
        AdjustTool.BLUE_TONE -> blueTone
        AdjustTool.VIGNETTE -> vignette
    }

    fun withValue(tool: AdjustTool, value: Float): TuneValues {
        val clamped = value.coerceIn(tool.minValue, tool.maxValue)
        return when (tool) {
            AdjustTool.BRIGHTNESS -> copy(brightness = clamped)
            AdjustTool.CONTRAST -> copy(contrast = clamped)
            AdjustTool.TONE -> copy(tone = clamped)
            AdjustTool.WHITE_POINT -> copy(whitePoint = clamped)
            AdjustTool.HIGHLIGHTS -> copy(highlights = clamped)
            AdjustTool.SHADOWS -> copy(shadows = clamped)
            AdjustTool.BLACK_POINT -> copy(blackPoint = clamped)
            AdjustTool.SATURATION -> copy(saturation = clamped)
            AdjustTool.WARMTH -> copy(warmth = clamped)
            AdjustTool.TINT -> copy(tint = clamped)
            AdjustTool.SKIN_TONE -> copy(skinTone = clamped)
            AdjustTool.BLUE_TONE -> copy(blueTone = clamped)
            AdjustTool.VIGNETTE -> copy(vignette = clamped)
        }
    }

    fun isToolModified(tool: AdjustTool, threshold: Float = 0.001f): Boolean {
        return abs(getValue(tool) - tool.defaultValue) > threshold
    }

    fun isDefault(threshold: Float = 0.001f): Boolean {
        return AdjustTool.entries.none { isToolModified(it, threshold) }
    }

    fun reset(tool: AdjustTool): TuneValues {
        return withValue(tool, tool.defaultValue)
    }

    fun resetAll(): TuneValues = TuneValues()
}
