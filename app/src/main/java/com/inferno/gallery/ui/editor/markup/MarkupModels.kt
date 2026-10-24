package com.inferno.gallery.ui.editor.markup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

enum class MarkupTool(val label: String) {
    PEN("Pen"),
    HIGHLIGHTER("Highlighter"),
    ARROW("Arrow"),
    ERASER("Eraser"),
    TEXT("Text")
}

data class MarkupStroke(
    val id: Long = System.nanoTime(),
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float, // Normalized relative to container width (e.g. 0.005f to 0.04f)
    val tool: MarkupTool
) {
    /**
     * Hit-test whether a normalized touch point is within [threshold] distance of any segment in this stroke.
     */
    fun isHitBy(touch: Offset, threshold: Float = 0.035f): Boolean {
        if (points.isEmpty()) return false
        if (points.size == 1) {
            val d = (points[0] - touch)
            return sqrt(d.x * d.x + d.y * d.y) <= threshold
        }
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            if (distanceToSegment(touch, a, b) <= threshold) {
                return true
            }
        }
        return false
    }

    private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
        val ab = b - a
        val ap = p - a
        val abLenSq = ab.x * ab.x + ab.y * ab.y
        if (abLenSq <= 1e-6f) {
            return sqrt(ap.x * ap.x + ap.y * ap.y)
        }
        val t = ((ap.x * ab.x + ap.y * ab.y) / abLenSq).coerceIn(0f, 1f)
        val proj = a + Offset(ab.x * t, ab.y * t)
        val diff = p - proj
        return sqrt(diff.x * diff.x + diff.y * diff.y)
    }
}

data class MarkupText(
    val id: Long = System.nanoTime(),
    val text: String,
    val position: Offset, // Normalized center coordinates (0..1)
    val color: Color,
    val fontSize: Float = 22f, // Sp-equivalent
    val hasBackgroundBox: Boolean = true
)

data class MarkupState(
    val strokes: List<MarkupStroke> = emptyList(),
    val texts: List<MarkupText> = emptyList()
) {
    fun withAddedStroke(stroke: MarkupStroke): MarkupState =
        copy(strokes = strokes + stroke)

    fun withRemovedStroke(strokeId: Long): MarkupState =
        copy(strokes = strokes.filterNot { it.id == strokeId })

    fun withAddedText(text: MarkupText): MarkupState =
        copy(texts = texts + text)

    fun withUpdatedText(updated: MarkupText): MarkupState =
        copy(texts = texts.map { if (it.id == updated.id) updated else it })

    fun withRemovedText(textId: Long): MarkupState =
        copy(texts = texts.filterNot { it.id == textId })
}

object MarkupPalette {
    val COLORS = listOf(
        Color(0xFFFFFFFF), // White
        Color(0xFF000000), // Black
        Color(0xFFE53935), // Red
        Color(0xFFFB8C00), // Orange
        Color(0xFFFFD600), // Yellow
        Color(0xFF43A047), // Green
        Color(0xFF00ACC1), // Cyan
        Color(0xFF1E88E5), // Blue
        Color(0xFF8E24AA), // Purple
        Color(0xFFD81B60)  // Pink
    )

    const val DEFAULT_PEN_WIDTH = 0.010f
    const val DEFAULT_HIGHLIGHTER_WIDTH = 0.040f
    const val DEFAULT_ARROW_WIDTH = 0.012f
}
