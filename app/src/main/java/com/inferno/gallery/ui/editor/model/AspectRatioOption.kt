package com.inferno.gallery.ui.editor.model

import androidx.compose.ui.geometry.Rect

/**
 * Aspect ratio options for photo crop tool:
 * 11 options in canonical order: Freeform, Original, Square, 5:4, 4:3, 3:2, 16:9, 4:5, 3:4, 2:3, 9:16.
 */
enum class AspectRatioOption(
    val label: String,
    val ratio: Float?
) {
    FREEFORM("Freeform", null),
    ORIGINAL("Original", -1f),
    SQUARE("Square", 1f),
    RATIO_5_4("5:4", 5f / 4f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_3_2("3:2", 3f / 2f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_4_5("4:5", 4f / 5f),
    RATIO_3_4("3:4", 3f / 4f),
    RATIO_2_3("2:3", 2f / 3f),
    RATIO_9_16("9:16", 9f / 16f);

    /**
     * Resolves the effective target aspect ratio (width / height) given the effective image aspect ratio.
     * Returns null for [FREEFORM] where aspect ratio is unconstrained.
     */
    fun resolveTargetRatio(effectiveAspect: Float): Float? {
        return when (this) {
            FREEFORM -> null
            ORIGINAL -> if (effectiveAspect > 0f) effectiveAspect else 1f
            else -> ratio
        }
    }
}

/**
 * State holding geometric crop, framing, rotation, and 2-axis perspective keystone parameters.
 */
data class CropState(
    val rotation: Int = 0, // 0, 90, 180, 270 degrees
    val straightenAngle: Float = 0f, // -45f..+45f
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val cropRect: Rect = Rect(0f, 0f, 1f, 1f),
    val aspectOption: AspectRatioOption = AspectRatioOption.FREEFORM,
    val verticalTilt: Float = 0f, // -30f..+30f
    val horizontalTilt: Float = 0f // -30f..+30f
) {
    val isModified: Boolean
        get() = rotation != 0 ||
                straightenAngle != 0f ||
                flipH ||
                flipV ||
                cropRect != Rect(0f, 0f, 1f, 1f) ||
                aspectOption != AspectRatioOption.FREEFORM ||
                verticalTilt != 0f ||
                horizontalTilt != 0f

    fun reset(): CropState = CropState()

    fun rotate90Ccw(): CropState = copy(
        rotation = ((rotation - 90) % 360 + 360) % 360
    )

    fun rotate90Cw(): CropState = copy(
        rotation = (rotation + 90) % 360
    )

    fun toggleFlipH(): CropState = copy(flipH = !flipH)

    fun toggleFlipV(): CropState = copy(flipV = !flipV)

    fun withStraighten(angle: Float): CropState = copy(
        straightenAngle = angle.coerceIn(-45f, 45f)
    )

    fun withVerticalTilt(tilt: Float): CropState = copy(
        verticalTilt = tilt.coerceIn(-30f, 30f)
    )

    fun withHorizontalTilt(tilt: Float): CropState = copy(
        horizontalTilt = tilt.coerceIn(-30f, 30f)
    )
}
