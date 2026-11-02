package com.inferno.gallery.ui.editor.model

import androidx.compose.ui.graphics.ColorMatrix
import android.graphics.ColorMatrix as AndroidColorMatrix

/**
 * 14 signature photo filters with precise 4x5 ColorMatrix transformations.
 *
 * Each preset defines a 20-float affine color matrix in row-major order:
 * [ R_r, R_g, R_b, R_a, R_offset,
 *   G_r, G_g, G_b, G_a, G_offset,
 *   B_r, B_g, B_b, B_a, B_offset,
 *   A_r, A_g, A_b, A_a, A_offset ]
 */
enum class FilterPreset(
    val label: String,
    val matrixArray: FloatArray
) {
    ORIGINAL(
        label = "Original",
        matrixArray = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),

    LITE(
        label = "Lite",
        // Subtle exposure lift, slight desaturation, clean film look
        matrixArray = floatArrayOf(
            0.92f, 0.07f, 0.01f, 0f, 8f,
            0.02f, 0.97f, 0.01f, 0f, 8f,
            0.02f, 0.07f, 0.91f, 0f, 8f,
            0f,    0f,    0f,    1f, 0f
        )
    ),

    PLAYA(
        label = "Playa",
        // Warm golden hour warmth, amber highlights, gentle fade
        matrixArray = floatArrayOf(
            1.08f, 0.02f, 0.00f, 0f, 22f,
            0.00f, 1.02f, 0.00f, 0f, 14f,
            0.00f, 0.00f, 0.88f, 0f, 2f,
            0f,    0f,    0f,    1f, 0f
        )
    ),

    HONEY(
        label = "Honey",
        // Rich warm tones, deep contrast, honey-tinted midtones
        matrixArray = floatArrayOf(
            1.18f, 0.02f, 0.00f, 0f, -10f,
            0.02f, 1.12f, 0.00f, 0f, -15f,
            0.00f, 0.02f, 0.92f, 0f, -25f,
            0f,    0f,    0f,    1f, 0f
        )
    ),

    ISLA(
        label = "Isla",
        // Vibrant tropical blues, crisp contrast, saturated greens
        matrixArray = floatArrayOf(
            0.96f, 0.02f, 0.02f, 0f, -5f,
            0.00f, 1.12f, 0.02f, 0f, 0f,
            0.02f, 0.04f, 1.16f, 0f, 6f,
            0f,    0f,    0f,    1f, 0f
        )
    ),

    DESERT(
        label = "Desert",
        // Muted greens, boosted ochre/sand tones, matte shadows
        matrixArray = floatArrayOf(
            1.06f, 0.06f, 0.00f, 0f, 18f,
            0.04f, 0.92f, 0.02f, 0f, 16f,
            0.00f, 0.04f, 0.82f, 0f, 18f,
            0f,    0f,    0f,    1f, 0f
        )
    ),

    CLAY(
        label = "Clay",
        // Earthy terracotta reds, softened highlights
        matrixArray = floatArrayOf(
            1.14f, 0.04f, 0.00f, 0f, 14f,
            0.04f, 0.98f, 0.00f, 0f, 4f,
            0.00f, 0.02f, 0.86f, 0f, -4f,
            0f,    0f,    0f,    1f, 0f
        )
    ),

    PALMA(
        label = "Palma",
        // Clean Mediterranean light, balanced contrast, vivid cyan sky
        matrixArray = floatArrayOf(
            1.02f, 0.01f, 0.00f, 0f, -2f,
            0.00f, 1.06f, 0.02f, 0f, 2f,
            0.01f, 0.02f, 1.12f, 0f, 6f,
            0f,    0f,    0f,    1f, 0f
        )
    ),

    METRO(
        label = "Metro",
        // Urban cool tones, blue cast in shadows, punchy contrast
        matrixArray = floatArrayOf(
            1.16f, 0.00f, 0.00f, 0f, -18f,
            0.00f, 1.14f, 0.02f, 0f, -14f,
            0.02f, 0.02f, 1.22f, 0f, -2f,
            0f,    0f,    0f,    1f, 0f
        )
    ),

    EIFFEL(
        label = "Eiffel",
        // Classic moody French film, gentle magenta/sepia warmth
        matrixArray = floatArrayOf(
            1.04f, 0.02f, 0.02f, 0f, 16f,
            0.02f, 0.98f, 0.02f, 0f, 6f,
            0.04f, 0.02f, 0.94f, 0f, 10f,
            0f,    0f,    0f,    1f, 0f
        )
    ),

    MODENA(
        label = "Modena",
        // High contrast, deep blacks, saturated primaries
        matrixArray = floatArrayOf(
            1.28f, -0.04f, -0.04f, 0f, -16f,
            -0.04f, 1.28f, -0.04f, 0f, -16f,
            -0.04f, -0.04f, 1.28f, 0f, -16f,
            0f,     0f,     0f,    1f, 0f
        )
    ),

    BREEZE(
        label = "Breeze",
        // Airy pastel tones, lifted shadows, soft highlight glow
        matrixArray = floatArrayOf(
            0.90f, 0.06f, 0.02f, 0f, 22f,
            0.02f, 0.92f, 0.02f, 0f, 22f,
            0.02f, 0.06f, 0.94f, 0f, 24f,
            0f,    0f,    0f,    1f, 0f
        )
    ),

    VOGUE(
        label = "Vogue",
        // Monochrome B&W high fashion contrast, pure whites
        matrixArray = floatArrayOf(
            0.287f, 0.965f, 0.098f, 0f, -24f,
            0.287f, 0.965f, 0.098f, 0f, -24f,
            0.287f, 0.965f, 0.098f, 0f, -24f,
            0f,     0f,     0f,     1f, 0f
        )
    ),

    ONYX(
        label = "Onyx",
        // Deep cinematic B&W, heavy shadows, film grain feel
        matrixArray = floatArrayOf(
            0.35f, 0.65f, 0.12f, 0f, -32f,
            0.35f, 0.65f, 0.12f, 0f, -32f,
            0.35f, 0.65f, 0.12f, 0f, -32f,
            0f,    0f,    0f,    1f, 0f
        )
    );

    /**
     * ColorMatrix instance for Compose graphics pipeline.
     */
    val colorMatrix: ColorMatrix
        get() = ColorMatrix(matrixArray)

    /**
     * Interpolates this preset from identity (strength = 0f) to full preset effect (strength = 100f).
     */
    fun interpolate(strength: Float): ColorMatrix {
        return interpolate(colorMatrix, strength)
    }

    /**
     * Interpolates this preset returning an [AndroidColorMatrix] for Android Canvas / background export.
     */
    fun interpolateAndroid(strength: Float): AndroidColorMatrix {
        val composeMatrix = interpolate(strength)
        return AndroidColorMatrix(composeMatrix.values)
    }

    companion object {
        private val IDENTITY_ARRAY = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )

        /**
         * Interpolates between an identity matrix (at strength 0f) and [filterMatrix] (at strength 100f).
         * Strength values are coerced to the range 0f..100f.
         */
        fun interpolate(filterMatrix: ColorMatrix, strength: Float): ColorMatrix {
            val factor = (strength / 100f).coerceIn(0f, 1f)
            val target = filterMatrix.values
            val result = FloatArray(20)
            for (i in 0 until 20) {
                result[i] = IDENTITY_ARRAY[i] + (target[i] - IDENTITY_ARRAY[i]) * factor
            }
            return ColorMatrix(result)
        }

        /**
         * Interpolation overload for [AndroidColorMatrix].
         */
        fun interpolate(filterMatrix: AndroidColorMatrix, strength: Float): AndroidColorMatrix {
            val composeInterp = interpolate(ColorMatrix(filterMatrix.array), strength)
            return AndroidColorMatrix(composeInterp.values)
        }
    }
}

/**
 * Top-level convenience function to interpolate a [filterMatrix] according to [strength] (0f..100f).
 */
fun interpolate(filterMatrix: ColorMatrix, strength: Float): ColorMatrix {
    return FilterPreset.interpolate(filterMatrix, strength)
}
