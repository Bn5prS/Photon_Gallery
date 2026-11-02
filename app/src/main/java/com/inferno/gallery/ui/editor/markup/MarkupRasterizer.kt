package com.inferno.gallery.ui.editor.markup

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import kotlin.math.sqrt

data class MosaicTile(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val gridX: Int,
    val gridY: Int
)

object MarkupRasterizer {

    fun rasterize(
        canvas: Canvas,
        bitmapWidth: Int,
        bitmapHeight: Int,
        markup: MarkupState,
        sourceBitmap: Bitmap? = null
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

                MarkupTool.REDACT -> {
                    val mode = stroke.redactMode ?: RedactMode.MOSAIC
                    if (mode == RedactMode.BLACKOUT) {
                        val paint = Paint().apply {
                            isAntiAlias = true
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            color = android.graphics.Color.BLACK
                            strokeWidth = strokePx
                        }
                        drawStrokePath(canvas, stroke.points, bitmapWidth, bitmapHeight, strokePx, paint)
                    } else {
                        val tileSizePx = (strokePx * 0.45f).coerceIn(10f, 60f)
                        val tiles = calculateMosaicTiles(stroke.points, bitmapWidth, bitmapHeight, strokePx, tileSizePx)
                        drawMosaicTiles(canvas, tiles, sourceBitmap)
                    }
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

    /**
     * Calculates the mosaic grid tiles intersecting the stroke path.
     */
    fun calculateMosaicTiles(
        points: List<Offset>,
        width: Int,
        height: Int,
        strokePx: Float,
        tileSizePx: Float = (strokePx * 0.45f).coerceIn(10f, 60f)
    ): List<MosaicTile> {
        if (points.isEmpty() || width <= 0 || height <= 0 || tileSizePx <= 0f) return emptyList()

        val radius = strokePx / 2f
        val maxGridX = (width / tileSizePx).toInt() + 1
        val maxGridY = (height / tileSizePx).toInt() + 1

        val tileSet = LinkedHashSet<Long>()

        fun addTilesAround(cx: Float, cy: Float) {
            val minGx = ((cx - radius) / tileSizePx).toInt().coerceIn(0, maxGridX)
            val maxGx = ((cx + radius) / tileSizePx).toInt().coerceIn(0, maxGridX)
            val minGy = ((cy - radius) / tileSizePx).toInt().coerceIn(0, maxGridY)
            val maxGy = ((cy + radius) / tileSizePx).toInt().coerceIn(0, maxGridY)

            for (gx in minGx..maxGx) {
                for (gy in minGy..maxGy) {
                    val tileCenterX = (gx + 0.5f) * tileSizePx
                    val tileCenterY = (gy + 0.5f) * tileSizePx
                    val dx = tileCenterX - cx
                    val dy = tileCenterY - cy
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist <= radius + tileSizePx * 0.6f) {
                        val key = (gx.toLong() shl 32) or (gy.toLong() and 0xFFFFFFFFL)
                        tileSet.add(key)
                    }
                }
            }
        }

        if (points.size == 1) {
            addTilesAround(points[0].x * width, points[0].y * height)
        } else {
            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                val x1 = p1.x * width
                val y1 = p1.y * height
                val x2 = p2.x * width
                val y2 = p2.y * height

                val segDx = x2 - x1
                val segDy = y2 - y1
                val segLen = sqrt(segDx * segDx + segDy * segDy)
                val step = (tileSizePx * 0.5f).coerceIn(4f, 20f)
                val steps = maxOf(1, (segLen / step).toInt())

                for (s in 0..steps) {
                    val ratio = s.toFloat() / steps
                    val cx = x1 + segDx * ratio
                    val cy = y1 + segDy * ratio
                    addTilesAround(cx, cy)
                }
            }
        }

        return tileSet.mapNotNull { key ->
            val gx = (key ushr 32).toInt()
            val gy = (key and 0xFFFFFFFFL).toInt()
            val left = (gx * tileSizePx).toInt().coerceIn(0, width)
            val top = (gy * tileSizePx).toInt().coerceIn(0, height)
            val right = ((gx + 1) * tileSizePx).toInt().coerceIn(0, width)
            val bottom = ((gy + 1) * tileSizePx).toInt().coerceIn(0, height)
            if (right > left && bottom > top) {
                MosaicTile(left, top, right, bottom, gx, gy)
            } else null
        }
    }

    private fun drawMosaicTiles(
        canvas: Canvas,
        tiles: List<MosaicTile>,
        sourceBitmap: Bitmap?
    ) {
        val fillPaint = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = 0x22000000.toInt()
        }

        for (tile in tiles) {
            val color = if (sourceBitmap != null && !sourceBitmap.isRecycled) {
                val sampleX = ((tile.left + tile.right) / 2).coerceIn(0, sourceBitmap.width - 1)
                val sampleY = ((tile.top + tile.bottom) / 2).coerceIn(0, sourceBitmap.height - 1)
                sourceBitmap.getPixel(sampleX, sampleY)
            } else {
                // Procedural pixelation shade
                val h = ((tile.gridX * 73856093) xor (tile.gridY * 19349663)) and 0x7FFFFFFF
                val gray = 100 + (h % 65)
                (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }

            fillPaint.color = color
            canvas.drawRect(
                tile.left.toFloat(),
                tile.top.toFloat(),
                tile.right.toFloat(),
                tile.bottom.toFloat(),
                fillPaint
            )
            canvas.drawRect(
                tile.left.toFloat(),
                tile.top.toFloat(),
                tile.right.toFloat(),
                tile.bottom.toFloat(),
                borderPaint
            )
        }
    }

    private fun drawStrokePath(
        canvas: Canvas,
        points: List<Offset>,
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
        tip: Offset,
        anchor: Offset,
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
        points: List<Offset>,
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

        val lines = textElem.text.lines()
        val lineBounds = lines.map { line ->
            val b = Rect()
            textPaint.getTextBounds(line, 0, line.length, b)
            b
        }
        val maxLineWidth = lineBounds.maxOfOrNull { it.width().toFloat() } ?: 0f
        val fontMetrics = textPaint.fontMetrics
        val lineHeight = (fontMetrics.descent - fontMetrics.ascent) * 1.15f
        val totalTextHeight = lines.size * lineHeight

        val centerX = textElem.position.x * width
        val centerY = textElem.position.y * height

        canvas.save()
        canvas.translate(centerX, centerY)
        if (textElem.rotation != 0f) {
            canvas.rotate(textElem.rotation)
        }

        if (textElem.hasBackgroundBox) {
            val padX = textSizePx * 0.45f
            val padY = textSizePx * 0.30f
            val rectLeft = -maxLineWidth / 2f - padX
            val rectTop = -totalTextHeight / 2f - padY
            val rectRight = maxLineWidth / 2f + padX
            val rectBottom = totalTextHeight / 2f + padY

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

        val firstBaselineY = -totalTextHeight / 2f - fontMetrics.ascent + (lineHeight - (fontMetrics.descent - fontMetrics.ascent)) / 2f
        for (i in lines.indices) {
            val line = lines[i]
            val lw = textPaint.measureText(line)
            val drawX = -lw / 2f
            val drawY = firstBaselineY + i * lineHeight
            canvas.drawText(line, drawX, drawY, textPaint)
        }
        canvas.restore()
    }

    internal fun isColorDark(color: androidx.compose.ui.graphics.Color): Boolean {
        val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
        return luminance < 0.5
    }
}
