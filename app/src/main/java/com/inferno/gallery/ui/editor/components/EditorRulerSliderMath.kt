package com.inferno.gallery.ui.editor.components

import kotlin.math.abs

/**
 * Pure mathematical utilities for ruler wheel slider calculations.
 *
 * Handles value normalization, delta to value mapping, clamping, stepping/quantization,
 * and snap-to-default thresholds.
 */
object EditorRulerSliderMath {

    /**
     * Normalizes [value] within [range] to a [0f..1f] float fraction.
     * Returns 0f if the range has zero or negative span.
     */
    fun normalize(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        val span = range.endInclusive - range.start
        if (span <= 0f) return 0f
        return ((value - range.start) / span).coerceIn(0f, 1f)
    }

    /**
     * Converts a [0f..1f] normalized fraction back to a value within [range].
     */
    fun denormalize(fraction: Float, range: ClosedFloatingPointRange<Float>): Float {
        val clampedFraction = fraction.coerceIn(0f, 1f)
        return range.start + clampedFraction * (range.endInclusive - range.start)
    }

    /**
     * Clamps [value] strictly inside [range].
     */
    fun clamp(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        return value.coerceIn(range.start, range.endInclusive)
    }

    /**
     * Quantizes [value] to the nearest multiple of [step].
     * Returns [value] unchanged if [step] <= 0f.
     */
    fun quantize(value: Float, step: Float): Float {
        if (step <= 0f) return value
        val steps = Math.round((value / step).toDouble())
        val rounded = (steps * step).toFloat()
        return if (rounded == -0.0f) 0.0f else rounded
    }

    /**
     * Snaps [value] to [defaultValue] if the absolute distance is within [snapThreshold].
     */
    fun snapToDefault(value: Float, defaultValue: Float, snapThreshold: Float): Float {
        return if (abs(value - defaultValue) <= snapThreshold) defaultValue else value
    }

    /**
     * Converts raw drag delta in pixels [dragDeltaPx] to a value change based on [pxPerUnit].
     *
     * Inverted mode (default: true) matches tape/wheel physics where dragging left (negative deltaX)
     * moves the ruler left, bringing higher values into the center.
     */
    fun deltaToValue(dragDeltaPx: Float, pxPerUnit: Float, inverted: Boolean = true): Float {
        if (pxPerUnit == 0f) return 0f
        val sign = if (inverted) -1f else 1f
        return sign * (dragDeltaPx / pxPerUnit)
    }

    /**
     * Applies a touch drag delta to [currentValue] incorporating delta conversion,
     * range clamping, stepping quantization, and snap-to-default logic.
     */
    fun applyDelta(
        currentValue: Float,
        dragDeltaPx: Float,
        pxPerUnit: Float,
        range: ClosedFloatingPointRange<Float>,
        step: Float = 1f,
        snapThreshold: Float = 0f,
        defaultValue: Float = 0f,
        inverted: Boolean = true
    ): Float {
        val delta = deltaToValue(dragDeltaPx, pxPerUnit, inverted)
        val candidate = currentValue + delta
        val clamped = clamp(candidate, range)
        val stepped = quantize(clamped, step)
        val snapped = snapToDefault(stepped, defaultValue, snapThreshold)
        return clamp(snapped, range)
    }

    /**
     * Returns true if [value] falls on a major tick mark interval (e.g. multiples of 5 or 10).
     */
    fun isMajorTick(value: Float, majorInterval: Float = 5f): Boolean {
        if (majorInterval <= 0f) return false
        val rem = abs(value % majorInterval)
        return rem < 0.001f || abs(rem - majorInterval) < 0.001f
    }
}
