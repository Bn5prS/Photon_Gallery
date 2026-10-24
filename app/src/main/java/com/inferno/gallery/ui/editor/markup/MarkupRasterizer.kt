package com.inferno.gallery.ui.editor.markup

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import kotlin.math.sqrt

object MarkupRasterizer {

    fun rasterize(
        canvas: Canvas,
        bitmapWidth: Int,
        bitmapHeight: Int,
        markup: MarkupState
    ) {
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return

        // 1. Draw strokes
        for (stroke in markup.strokes) {
            if (stroke.points.isEmpty()) continue
            val strokePx = (stroke.strokeWidth * bitmapWidth).coerceAtLeast(2f)

            when (stroke.tool) {
                MarkupTool.PEN -> {
                    val paint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        color = stroke.color.toArgb()
                        strokeWidth = strokePx
                    }
                    drawStrokePath(canvas, stroke.points, bitmapWidth, bitmapHeight, strokePx, paint)
                }

                MarkupTool.HIGHLIGHTER -> {
                    val baseColor = stroke.color.toArgb()
                    val alpha = 110 // Semi-transparent
                    val highlighterColor = (alpha shl 24) or (baseColor and 0x00FFFFFF)
                    val paint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        color = highlighterColor
                        strokeWidth = strokePx * 1.8f
                    }
                    drawStrokePath(canvas, stroke.points, bitmapWidth, bitmapHeight, strokePx * 1.8f, paint)
                }

                MarkupTool.ARROW -> {
                    val paint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        color = stroke.color.toArgb()
                        strokeWidth = strokePx
                    }
                    drawStrokePath(canvas, stroke.points, bitmapWidth, bitmapHeight, strokePx, paint)
                    drawArrowhead(canvas, stroke.points, bitmapWidth, bitmapHeight, strokePx, stroke.color.toArgb())
                }

                else -> {}
            }
        }

        // 2. Draw text overlays
        for (textElem in markup.texts) {
            if (textElem.text.isBlank()) continue
            drawTextElement(canvas, textElem, bitmapWidth, bitmapHeight)
        }
    }

    private fun drawStrokePath(
        canvas: Canvas,
        points: List<androidx.compose.ui.geometry.Offset>,
        width: Int,
        height: Int,
        strokePx: Float,
        paint: Paint
    ) {
        if (points.size == 1) {
            val p = points[0]
            val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
            canvas.drawCircle(p.x * width, p.y * height, strokePx / 2f, fillPaint)
            return
        }

        val path = Path()
        val first = points[0]
        path.moveTo(first.x * width, first.y * height)

        for (i in 1 until points.size) {
            val pt = points[i]
            path.lineTo(pt.x * width, pt.y * height)
        }
        canvas.drawPath(path, paint)
    }

    data class Arrowhead(
        val tipX: Float,
        val tipY: Float,
        val leftX: Float,
        val leftY: Float,
        val rightX: Float,
        val rightY: Float
    )

    internal fun calculateArrowhead(
        tip: androidx.compose.ui.geometry.Offset,
        prev: androidx.compose.ui.geometry.Offset,
        width: Int,
        height: Int,
        strokePx: Float
    ): Arrowhead? {
        val tipX = tip.x * width
        val tipY = tip.y * height
        val prevX = prev.x * width
        val prevY = prev.y * height

        val dx = tipX - prevX
        val dy = tipY - prevY
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1e-4f) return null

        val ndx = dx / len
        val ndy = dy / len
        val perpX = -ndy
        val perpY = ndx

        val arrowHeadLen = (strokePx * 3.5f).coerceAtLeast(20f)
        val arrowHeadHalfWidth = arrowHeadLen * 0.55f

        val baseX = tipX - ndx * arrowHeadLen
        val baseY = tipY - ndy * arrowHeadLen

        val leftX = baseX + perpX * arrowHeadHalfWidth
        val leftY = baseY + perpY * arrowHeadHalfWidth

        val rightX = baseX - perpX * arrowHeadHalfWidth
        val rightY = baseY - perpY * arrowHeadHalfWidth

        return Arrowhead(tipX, tipY, leftX, leftY, rightX, rightY)
    }

    private fun drawArrowhead(
        canvas: Canvas,
        points: List<androidx.compose.ui.geometry.Offset>,
        width: Int,
        height: Int,
        strokePx: Float,
        colorArgb: Int
    ) {
        if (points.size < 2) return
        val head = calculateArrowhead(points.last(), points[points.size - 2], width, height, strokePx) ?: return

        val headPath = Path().apply {
            moveTo(head.tipX, head.tipY)
            lineTo(head.leftX, head.leftY)
            lineTo(head.rightX, head.rightY)
            close()
        }

        val fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = colorArgb
        }
        canvas.drawPath(headPath, fillPaint)
    }

    private fun drawTextElement(
        canvas: Canvas,
        textElem: MarkupText,
        width: Int,
        height: Int
    ) {
        val scale = (width / 420f).coerceIn(0.5f, 6.0f)
        val textSizePx = textElem.fontSize * scale

        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = textSizePx
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = textElem.color.toArgb()
        }

        val bounds = Rect()
        textPaint.getTextBounds(textElem.text, 0, textElem.text.length, bounds)

        val centerX = textElem.position.x * width
        val centerY = textElem.position.y * height

        val textWidth = bounds.width().toFloat()
        val textHeight = bounds.height().toFloat()

        if (textElem.hasBackgroundBox) {
            val padX = textSizePx * 0.45f
            val padY = textSizePx * 0.30f
            val rectLeft = centerX - textWidth / 2f - padX
            val rectTop = centerY - textHeight / 2f - padY
            val rectRight = centerX + textWidth / 2f + padX
            val rectBottom = centerY + textHeight / 2f + padY

            val isDarkText = isColorDark(textElem.color)
            val bgArgb = if (isDarkText) 0xDDFFFFFF.toInt() else 0xCC000000.toInt()
            val bgPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = bgArgb
            }
            val radius = 12f * scale
            canvas.drawRoundRect(
                RectF(rectLeft, rectTop, rectRight, rectBottom),
                radius,
                radius,
                bgPaint
            )
        }

        val drawX = centerX - textWidth / 2f - bounds.left
        val drawY = centerY + textHeight / 2f - bounds.bottom
        canvas.drawText(textElem.text, drawX, drawY, textPaint)
    }

    internal fun isColorDark(color: androidx.compose.ui.graphics.Color): Boolean {
        val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
        return luminance < 0.5
    }
}
