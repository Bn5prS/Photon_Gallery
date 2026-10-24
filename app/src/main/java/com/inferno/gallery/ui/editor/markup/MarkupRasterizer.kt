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
                    drawArrowElement(canvas, stroke.points, bitmapWidth, bitmapHeight, strokePx, stroke.color.toArgb())
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
        val rightY: Float,
        val notchX: Float,
        val notchY: Float
    )

    internal fun calculateArrowhead(
        tip: androidx.compose.ui.geometry.Offset,
        anchor: androidx.compose.ui.geometry.Offset,
        width: Int,
        height: Int,
        strokePx: Float
    ): Arrowhead? {
        val tipX = tip.x * width
        val tipY = tip.y * height
        val anchorX = anchor.x * width
        val anchorY = anchor.y * height

        val dx = tipX - anchorX
        val dy = tipY - anchorY
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1e-4f) return null

        val rawHeadLen = (strokePx * 3.4f).coerceIn(24f, 120f)
        val headLen = minOf(rawHeadLen, len * 0.85f)
        val halfWidth = headLen * 0.48f
        val notchDepth = headLen * 0.25f

        val ndx = dx / len
        val ndy = dy / len
        val perpX = -ndy
        val perpY = ndx

        val baseCenterX = tipX - ndx * headLen
        val baseCenterY = tipY - ndy * headLen
        val notchX = tipX - ndx * (headLen - notchDepth)
        val notchY = tipY - ndy * (headLen - notchDepth)

        val leftX = baseCenterX + perpX * halfWidth
        val leftY = baseCenterY + perpY * halfWidth

        val rightX = baseCenterX - perpX * halfWidth
        val rightY = baseCenterY - perpY * halfWidth

        return Arrowhead(tipX, tipY, leftX, leftY, rightX, rightY, notchX, notchY)
    }

    private fun drawArrowElement(
        canvas: Canvas,
        points: List<androidx.compose.ui.geometry.Offset>,
        width: Int,
        height: Int,
        strokePx: Float,
        colorArgb: Int
    ) {
        if (points.isEmpty()) return
        if (points.size < 2) {
            val p = points[0]
            val fillPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = colorArgb
            }
            canvas.drawCircle(p.x * width, p.y * height, strokePx / 2f, fillPaint)
            return
        }

        val tip = points.last()
        val tipX = tip.x * width
        val tipY = tip.y * height

        val rawHeadLen = (strokePx * 3.4f).coerceIn(24f, 120f)
        val minAnchorDist = maxOf(rawHeadLen * 0.8f, 20f)
        val minAnchorDistSq = minAnchorDist * minAnchorDist

        var anchor = points[0]
        for (i in points.size - 2 downTo 0) {
            val px = points[i].x * width
            val py = points[i].y * height
            val dSq = (tipX - px) * (tipX - px) + (tipY - py) * (tipY - py)
            if (dSq >= minAnchorDistSq) {
                anchor = points[i]
                break
            }
        }

        val head = calculateArrowhead(tip, anchor, width, height, strokePx) ?: return

        // 1. Draw shaft line terminating at notch
        val shaftPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = colorArgb
            strokeWidth = strokePx
        }
        val shaftPath = Path()
        shaftPath.moveTo(points[0].x * width, points[0].y * height)
        for (i in 1 until points.size - 1) {
            shaftPath.lineTo(points[i].x * width, points[i].y * height)
        }
        shaftPath.lineTo(head.notchX, head.notchY)
        canvas.drawPath(shaftPath, shaftPaint)

        // 2. Draw sleek arrowhead with sharp tip
        val headPath = Path().apply {
            moveTo(head.tipX, head.tipY)
            lineTo(head.leftX, head.leftY)
            lineTo(head.notchX, head.notchY)
            lineTo(head.rightX, head.rightY)
            close()
        }
        val headPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = colorArgb
        }
        canvas.drawPath(headPath, headPaint)
    }

    private fun drawTextElement(
        canvas: Canvas,
        textElem: MarkupText,
        width: Int,
        height: Int
    ) {
        val baseScale = (width / 420f).coerceIn(0.5f, 6.0f)
        val scale = baseScale * textElem.scale
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

        canvas.save()
        canvas.translate(centerX, centerY)
        if (textElem.rotation != 0f) {
            canvas.rotate(textElem.rotation)
        }

        if (textElem.hasBackgroundBox) {
            val padX = textSizePx * 0.45f
            val padY = textSizePx * 0.30f
            val rectLeft = -textWidth / 2f - padX
            val rectTop = -textHeight / 2f - padY
            val rectRight = textWidth / 2f + padX
            val rectBottom = textHeight / 2f + padY

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

        val drawX = -textWidth / 2f - bounds.left
        val drawY = textHeight / 2f - bounds.bottom
        canvas.drawText(textElem.text, drawX, drawY, textPaint)
        canvas.restore()
    }

    internal fun isColorDark(color: androidx.compose.ui.graphics.Color): Boolean {
        val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
        return luminance < 0.5
    }
}
