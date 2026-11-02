package com.inferno.gallery.ui.editor.engine

import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.inferno.gallery.ui.editor.model.TuneValues
import kotlin.math.max
import android.graphics.ColorMatrix as AndroidColorMatrix
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix

/**
 * Universal GPU ColorMatrix and shader generation engine for real-time photo editing.
 *
 * Computes a combined 4x5 affine color transformation matrix in microsecond time
 * covering all 13 granular adjustments, plus radial vignette shaders.
 */
object ImageProcessingEngine {

    /**
     * Builds the unified 20-float array representing the full affine transformation matrix.
     */
    fun buildMatrixArray(
        tune: TuneValues,
        filterMatrixArray: FloatArray? = null
    ): FloatArray {
        var current = createIdentity()

        // 1. Brightness: translation offset in RGB channels
        if (tune.brightness != 0f) {
            val b = tune.brightness * 1.5f
            val bMatrix = floatArrayOf(
                1f, 0f, 0f, 0f, b,
                0f, 1f, 0f, 0f, b,
                0f, 0f, 1f, 0f, b,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiply4x5(bMatrix, current)
        }

        // 2. Contrast: scale around 0.5 mid-gray with offset
        if (tune.contrast != 0f) {
            val c = tune.contrast / 100f
            val scale = 1f + c
            val offset = 127.5f * (1f - scale)
            val cMatrix = floatArrayOf(
                scale, 0f, 0f, 0f, offset,
                0f, scale, 0f, 0f, offset,
                0f, 0f, scale, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiply4x5(cMatrix, current)
        }

        // 3. Tone (HDR): dynamic range expansion / sigmoid contrast curve
        if (tune.tone != 0f) {
            val t = tune.tone / 100f
            val tScale = 1f + t * 0.2f
            val tOffset = 127.5f * (1f - tScale)
            val tMatrix = floatArrayOf(
                tScale, 0f, 0f, 0f, tOffset,
                0f, tScale, 0f, 0f, tOffset,
                0f, 0f, tScale, 0f, tOffset,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiply4x5(tMatrix, current)
        }

        // 4. White Point: adjust peak brightness ceiling
        if (tune.whitePoint != 0f) {
            val wp = 1f + (tune.whitePoint / 200f)
            val wpMatrix = floatArrayOf(
                wp, 0f, 0f, 0f, 0f,
                0f, wp, 0f, 0f, 0f,
                0f, 0f, wp, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiply4x5(wpMatrix, current)
        }

        // 5. Highlights: scale upper-range luminance
        if (tune.highlights != 0f) {
            val hl = tune.highlights / 100f
            val hlScale = 1f + hl * 0.25f
            val hlOffset = -hl * 8f
            val hlMatrix = floatArrayOf(
                hlScale, 0f, 0f, 0f, hlOffset,
                0f, hlScale, 0f, 0f, hlOffset,
                0f, 0f, hlScale, 0f, hlOffset,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiply4x5(hlMatrix, current)
        }

        // 6. Shadows: lift/deepen lower-range luminance
        if (tune.shadows != 0f) {
            val sh = tune.shadows * 0.5f
            val shMatrix = floatArrayOf(
                1f, 0f, 0f, 0f, sh,
                0f, 1f, 0f, 0f, sh,
                0f, 0f, 1f, 0f, sh,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiply4x5(shMatrix, current)
        }

        // 7. Black Point: adjust black floor pedestal
        if (tune.blackPoint != 0f) {
            val bp = tune.blackPoint * 0.6f
            val bpMatrix = floatArrayOf(
                1f, 0f, 0f, 0f, bp,
                0f, 1f, 0f, 0f, bp,
                0f, 0f, 1f, 0f, bp,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiply4x5(bpMatrix, current)
        }

        // 8. Saturation: standard luminance matrix weights (0.2126R + 0.7152G + 0.0722B)
        if (tune.saturation != 0f) {
            val s = 1f + (tune.saturation / 100f)
            val invS = 1f - s
            val rWeight = invS * 0.2126f
            val gWeight = invS * 0.7152f
            val bWeight = invS * 0.0722f
            val sMatrix = floatArrayOf(
                rWeight + s, gWeight,     bWeight,     0f, 0f,
                rWeight,     gWeight + s, bWeight,     0f, 0f,
                rWeight,     gWeight,     bWeight + s, 0f, 0f,
                0f,          0f,          0f,          1f, 0f
            )
            current = multiply4x5(sMatrix, current)
        }

        // 9. Warmth: boost Red, decrease Blue
        if (tune.warmth != 0f) {
            val w = tune.warmth * 0.8f
            val wMatrix = floatArrayOf(
                1f, 0f, 0f, 0f, w,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, -w,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiply4x5(wMatrix, current)
        }

        // 10. Tint: boost Magenta (Red+Blue), decrease Green or vice versa
        if (tune.tint != 0f) {
            val t = tune.tint * 0.6f
            val tMatrix = floatArrayOf(
                1f, 0f, 0f, 0f, t,
                0f, 1f, 0f, 0f, -t,
                0f, 0f, 1f, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiply4x5(tMatrix, current)
        }

        // 11. Skin Tone: selective warm hue matrix shift
        if (tune.skinTone != 0f) {
            val st = tune.skinTone / 100f
            val rShift = st * 14f
            val gShift = st * 6f
            val stMatrix = floatArrayOf(
                1f, 0f, 0f, 0f, rShift,
                0f, 1f, 0f, 0f, gShift,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiply4x5(stMatrix, current)
        }

        // 12. Blue Tone: selective hue shift enhancing blues/skies
        if (tune.blueTone != 0f) {
            val bt = tune.blueTone / 100f
            val bScale = 1f + bt * 0.15f
            val bShift = bt * 15f
            val btMatrix = floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, bScale, 0f, bShift,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiply4x5(btMatrix, current)
        }

        // 13. Post-concatenate filterMatrix if provided
        if (filterMatrixArray != null && filterMatrixArray.size == 20) {
            current = multiply4x5(filterMatrixArray, current)
        }

        return current
    }

    /**
     * Builds Compose [ComposeColorMatrix] combining all [tune] adjustments and optional [filterMatrix].
     */
    fun buildColorMatrix(
        tune: TuneValues,
        filterMatrix: ComposeColorMatrix? = null
    ): ComposeColorMatrix {
        val array = buildMatrixArray(tune, filterMatrix?.values)
        return ComposeColorMatrix(array)
    }

    /**
     * Builds Android [AndroidColorMatrix] combining all [tune] adjustments and optional [filterMatrix].
     */
    fun buildColorMatrix(
        tune: TuneValues,
        filterMatrix: AndroidColorMatrix
    ): AndroidColorMatrix {
        val array = buildMatrixArray(tune, filterMatrix.array)
        return AndroidColorMatrix(array)
    }

    /**
     * Alias for [buildColorMatrix] returning Compose [ComposeColorMatrix].
     */
    fun buildCombinedMatrix(
        tune: TuneValues,
        filterMatrix: ComposeColorMatrix? = null
    ): ComposeColorMatrix {
        return buildColorMatrix(tune, filterMatrix)
    }

    /**
     * Alias for [buildColorMatrix] returning Android [AndroidColorMatrix].
     */
    fun buildCombinedMatrix(
        tune: TuneValues,
        filterMatrix: AndroidColorMatrix
    ): AndroidColorMatrix {
        return buildColorMatrix(tune, filterMatrix)
    }

    /**
     * Builds Android [AndroidColorMatrix] combining all [tune] adjustments and optional [filterMatrix].
     */
    fun buildCombinedAndroidMatrix(
        tune: TuneValues,
        filterMatrix: AndroidColorMatrix? = null
    ): AndroidColorMatrix {
        val array = buildMatrixArray(tune, filterMatrix?.array)
        return AndroidColorMatrix(array)
    }

    /**
     * Creates an Android [Shader] for vignette rendering onto high-resolution Bitmaps.
     */
    fun createVignetteShader(size: Size, amount: Float): Shader? {
        if (amount <= 0f || size.width <= 0f || size.height <= 0f) return null
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = max(cx, cy) * 1.2f
        val alpha = ((amount / 100f) * 200).toInt().coerceIn(0, 255)
        return RadialGradient(
            cx, cy, radius,
            intArrayOf(0x00000000, 0x00000000, android.graphics.Color.argb(alpha, 0, 0, 0)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    /**
     * Creates a Compose [Brush] for real-time vignette rendering on GPU Canvas.
     */
    fun createVignetteBrush(size: Size, amount: Float): Brush? {
        if (amount <= 0f || size.width <= 0f || size.height <= 0f) return null
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = max(cx, cy) * 1.2f
        val alpha = (amount / 100f * 0.78f).coerceIn(0f, 1f)
        return Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.45f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = alpha)
            ),
            center = Offset(cx, cy),
            radius = radius
        )
    }

    /**
     * Multiplies two 4x5 affine matrices: B x A.
     */
    fun multiply4x5(b: FloatArray, a: FloatArray): FloatArray {
        val result = FloatArray(20)
        for (r in 0 until 4) {
            val r5 = r * 5
            for (c in 0 until 4) {
                result[r5 + c] = b[r5 + 0] * a[0 * 5 + c] +
                                 b[r5 + 1] * a[1 * 5 + c] +
                                 b[r5 + 2] * a[2 * 5 + c] +
                                 b[r5 + 3] * a[3 * 5 + c]
            }
            // Translation column: (B * [A_04, A_14, A_24, A_34]^T) + B_r4
            result[r5 + 4] = b[r5 + 0] * a[0 * 5 + 4] +
                             b[r5 + 1] * a[1 * 5 + 4] +
                             b[r5 + 2] * a[2 * 5 + 4] +
                             b[r5 + 3] * a[3 * 5 + 4] +
                             b[r5 + 4]
        }
        return result
    }

    /**
     * Returns a 20-element 4x5 identity matrix.
     */
    fun createIdentity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}
