package com.inferno.gallery.ui.editor.markup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.sqrt

enum class MarkupTool(val label: String) {
    PEN("Pen"),
    HIGHLIGHTER("Highlighter"),
    ARROW("Arrow"),
    ERASER("Eraser"),
    TEXT("Text"),
    REDACT("Redact")
}

enum class RedactMode {
    MOSAIC,
    BLUR,
    BLACKOUT
}

data class MarkupStroke(
    val id: Long = System.nanoTime(),
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float, // Normalized relative to container width (e.g. 0.005f to 0.04f)
    val tool: MarkupTool,
    val redactMode: RedactMode? = null
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

    /**
     * Computes the bounding rectangle in normalized 0..1 coordinates.
     * Optionally pads by half the stroke width.
     */
    fun computeBounds(strokePadding: Boolean = true): Rect? {
        if (points.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (pt in points) {
            if (pt.x < minX) minX = pt.x
            if (pt.x > maxX) maxX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.y > maxY) maxY = pt.y
        }
        val pad = if (strokePadding) strokeWidth / 2f else 0f
        return Rect(
            left = minX - pad,
            top = minY - pad,
            right = maxX + pad,
            bottom = maxY + pad
        )
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

object MarkupModelsHelper {
    fun serializeStroke(stroke: MarkupStroke): String {
        val pointsStr = stroke.points.joinToString(";") { "${it.x},${it.y}" }
        val redactStr = stroke.redactMode?.name ?: ""
        val argb = stroke.color.value.toLong()
        return "${stroke.id}|${stroke.tool.name}|$argb|${stroke.strokeWidth}|$redactStr|$pointsStr"
    }

    fun deserializeStroke(data: String): MarkupStroke? {
        return try {
            val parts = data.split("|")
            if (parts.size < 6) return null
            val id = parts[0].toLong()
            val tool = MarkupTool.valueOf(parts[1])
            val argb = parts[2].toLong()
            val strokeWidth = parts[3].toFloat()
            val redactMode = if (parts[4].isNotEmpty()) RedactMode.valueOf(parts[4]) else null
            val points = if (parts[5].isEmpty()) emptyList() else parts[5].split(";").mapNotNull {
                val coords = it.split(",")
                if (coords.size == 2) Offset(coords[0].toFloat(), coords[1].toFloat()) else null
            }
            MarkupStroke(
                id = id,
                points = points,
                color = Color(argb.toULong()),
                strokeWidth = strokeWidth,
                tool = tool,
                redactMode = redactMode
            )
        } catch (e: Exception) {
            null
        }
    }
}

enum class TextHitPart {
    NONE,
    HANDLE_TOP_LEFT,
    HANDLE_TOP_RIGHT,
    HANDLE_BOTTOM_LEFT,
    HANDLE_BOTTOM_RIGHT,
    HANDLE_ROTATE,
    EDGE_TOP,
    EDGE_BOTTOM,
    EDGE_LEFT,
    EDGE_RIGHT,
    BODY;

    val isResizeHandle: Boolean
        get() = this in listOf(
            HANDLE_TOP_LEFT,
            HANDLE_TOP_RIGHT,
            HANDLE_BOTTOM_LEFT,
            HANDLE_BOTTOM_RIGHT,
            EDGE_TOP,
            EDGE_BOTTOM,
            EDGE_LEFT,
            EDGE_RIGHT
        )
}

data class MarkupText(
    val id: Long = System.nanoTime(),
    val text: String,
    val position: Offset, // Normalized center coordinates (0..1)
    val color: Color,
    val fontSize: Float = 22f, // Sp-equivalent
    val scale: Float = 1.0f, // Multiplier (0.5f..4.0f)
    val rotation: Float = 0f, // Degrees (-180f..180f)
    val hasBackgroundBox: Boolean = true,
    val textAlign: TextAlign = TextAlign.Center
) {
    fun isHitBy(normPoint: Offset, width: Float, height: Float, touchMarginDp: Float = 16f): Boolean {
        if (width <= 0f || height <= 0f) return false
        val baseScale = (width / 420f).coerceIn(0.7f, 3.5f)
        val effScale = baseScale * scale
        val lines = text.lines()
        val maxChars = lines.maxOfOrNull { it.length } ?: text.length
        val lineCount = maxOf(1, lines.size)

        val estimatedCharWidthPx = (fontSize * 0.58f) * effScale
        val slopPx = touchMarginDp * baseScale
        val halfWPx = maxOf((maxChars * estimatedCharWidthPx + 40f * effScale) / 2f, 32f * effScale) + slopPx
        val halfHPx = maxOf((lineCount * fontSize * 1.35f * effScale + 30f * effScale) / 2f, 28f * effScale) + slopPx

        val px = (normPoint.x - position.x) * width
        val py = (normPoint.y - position.y) * height

        val rad = Math.toRadians(-rotation.toDouble())
        val cos = kotlin.math.cos(rad).toFloat()
        val sin = kotlin.math.sin(rad).toFloat()
        val localPx = kotlin.math.abs(cos * px - sin * py)
        val localPy = kotlin.math.abs(sin * px + cos * py)

        return localPx <= halfWPx && localPy <= halfHPx
    }
}

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
        Color(0xFFE53935), // Red / Coral
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
    const val DEFAULT_ARROW_WIDTH = 0.014f
    const val DEFAULT_REDACT_WIDTH = 0.045f
    const val DEFAULT_ERASER_RADIUS = 0.045f
}
