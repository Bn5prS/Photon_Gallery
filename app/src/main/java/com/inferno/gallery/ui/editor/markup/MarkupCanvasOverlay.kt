package com.inferno.gallery.ui.editor.markup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

@Composable
fun MarkupCanvasOverlay(
    markupState: MarkupState,
    activeTool: MarkupTool,
    currentColor: Color,
    currentStrokeWidth: Float,
    onStateChanged: (MarkupState) -> Unit,
    onEditTextRequest: (MarkupText?) -> Unit,
    isInteractive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()

    // Active live stroke points while finger is down (in normalized 0..1 coordinates)
    val livePoints = remember { mutableStateListOf<Offset>() }
    var draggingTextId by remember { mutableStateOf<Long?>(null) }

    val interactiveModifier = if (isInteractive) {
        Modifier
            .pointerInput(activeTool, markupState, currentColor, currentStrokeWidth) {
                if (activeTool == MarkupTool.TEXT) {
                    detectTapGestures { tapOffset ->
                        val normX = (tapOffset.x / size.width).coerceIn(0f, 1f)
                        val normY = (tapOffset.y / size.height).coerceIn(0f, 1f)
                        val normTap = Offset(normX, normY)

                        // Check if tapped on existing text
                        val hitText = markupState.texts.findLast { text ->
                            val dx = normTap.x - text.position.x
                            val dy = normTap.y - text.position.y
                            sqrt(dx * dx + dy * dy) <= 0.08f
                        }

                        if (hitText != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEditTextRequest(hitText)
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEditTextRequest(null)
                        }
                    }
                }
            }
            .pointerInput(activeTool, markupState, currentColor, currentStrokeWidth) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val normX = (startOffset.x / size.width).coerceIn(0f, 1f)
                        val normY = (startOffset.y / size.height).coerceIn(0f, 1f)
                        val normStart = Offset(normX, normY)

                        when (activeTool) {
                            MarkupTool.PEN, MarkupTool.HIGHLIGHTER, MarkupTool.ARROW -> {
                                livePoints.clear()
                                livePoints.add(normStart)
                            }

                            MarkupTool.ERASER -> {
                                val hit = markupState.strokes.findLast { it.isHitBy(normStart, 0.04f) }
                                if (hit != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onStateChanged(markupState.withRemovedStroke(hit.id))
                                }
                            }

                            MarkupTool.TEXT -> {
                                val hitText = markupState.texts.findLast { text ->
                                    val dx = normStart.x - text.position.x
                                    val dy = normStart.y - text.position.y
                                    sqrt(dx * dx + dy * dy) <= 0.08f
                                }
                                draggingTextId = hitText?.id
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                        val normY = (change.position.y / size.height).coerceIn(0f, 1f)
                        val currentPoint = Offset(normX, normY)

                        when (activeTool) {
                            MarkupTool.PEN, MarkupTool.HIGHLIGHTER, MarkupTool.ARROW -> {
                                val last = livePoints.lastOrNull()
                                if (last == null) {
                                    livePoints.add(currentPoint)
                                } else {
                                    val dx = currentPoint.x - last.x
                                    val dy = currentPoint.y - last.y
                                    // Filter out micro-movements to keep path clean
                                    if (dx * dx + dy * dy >= 0.000004f) {
                                        livePoints.add(currentPoint)
                                    }
                                }
                            }

                            MarkupTool.ERASER -> {
                                val hit = markupState.strokes.findLast { it.isHitBy(currentPoint, 0.04f) }
                                if (hit != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onStateChanged(markupState.withRemovedStroke(hit.id))
                                }
                            }

                            MarkupTool.TEXT -> {
                                val targetId = draggingTextId
                                if (targetId != null) {
                                    val target = markupState.texts.find { it.id == targetId }
                                    if (target != null) {
                                        val updated = target.copy(position = currentPoint)
                                        onStateChanged(markupState.withUpdatedText(updated))
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        when (activeTool) {
                            MarkupTool.PEN, MarkupTool.HIGHLIGHTER, MarkupTool.ARROW -> {
                                if (livePoints.isNotEmpty()) {
                                    val stroke = MarkupStroke(
                                        points = livePoints.toList(),
                                        color = currentColor,
                                        strokeWidth = currentStrokeWidth,
                                        tool = activeTool
                                    )
                                    onStateChanged(markupState.withAddedStroke(stroke))
                                    livePoints.clear()
                                }
                            }

                            MarkupTool.TEXT -> {
                                draggingTextId = null
                            }

                            else -> {}
                        }
                    },
                    onDragCancel = {
                        livePoints.clear()
                        draggingTextId = null
                    }
                )
            }
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(interactiveModifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw completed strokes
            for (stroke in markupState.strokes) {
                drawStrokeItem(stroke, canvasW, canvasH)
            }

            // 2. Draw live stroke in progress
            if (livePoints.isNotEmpty()) {
                val liveStroke = MarkupStroke(
                    points = livePoints.toList(),
                    color = currentColor,
                    strokeWidth = currentStrokeWidth,
                    tool = activeTool
                )
                drawStrokeItem(liveStroke, canvasW, canvasH)
            }

            // 3. Draw text elements
            for (textElem in markupState.texts) {
                drawTextItem(textElem, canvasW, canvasH, textMeasurer)
            }
        }
    }
}

private fun DrawScope.drawStrokeItem(stroke: MarkupStroke, width: Float, height: Float) {
    if (stroke.points.isEmpty()) return
    val strokePx = (stroke.strokeWidth * width).coerceAtLeast(3f)

    when (stroke.tool) {
        MarkupTool.PEN -> {
            drawPathLine(
                points = stroke.points,
                width = width,
                height = height,
                color = stroke.color,
                strokePx = strokePx
            )
        }

        MarkupTool.HIGHLIGHTER -> {
            drawPathLine(
                points = stroke.points,
                width = width,
                height = height,
                color = stroke.color.copy(alpha = 0.40f),
                strokePx = strokePx * 1.8f
            )
        }

        MarkupTool.ARROW -> {
            drawPathLine(
                points = stroke.points,
                width = width,
                height = height,
                color = stroke.color,
                strokePx = strokePx
            )
            drawArrowheadScope(
                points = stroke.points,
                width = width,
                height = height,
                color = stroke.color,
                strokePx = strokePx
            )
        }

        else -> {}
    }
}

private fun DrawScope.drawPathLine(
    points: List<Offset>,
    width: Float,
    height: Float,
    color: Color,
    strokePx: Float
) {
    if (points.size == 1) {
        val p = points[0]
        drawCircle(
            color = color,
            radius = strokePx / 2f,
            center = Offset(p.x * width, p.y * height)
        )
        return
    }

    val path = Path()
    path.moveTo(points[0].x * width, points[0].y * height)
    for (i in 1 until points.size) {
        path.lineTo(points[i].x * width, points[i].y * height)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokePx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawArrowheadScope(
    points: List<Offset>,
    width: Float,
    height: Float,
    color: Color,
    strokePx: Float
) {
    if (points.size < 2) return
    val tip = points.last()
    val prev = points[points.size - 2]

    val tipX = tip.x * width
    val tipY = tip.y * height
    val prevX = prev.x * width
    val prevY = prev.y * height

    val dx = tipX - prevX
    val dy = tipY - prevY
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1e-4f) return

    val ndx = dx / len
    val ndy = dy / len
    val perpX = -ndy
    val perpY = ndx

    val arrowHeadLen = (strokePx * 3.5f).coerceAtLeast(18f)
    val arrowHeadHalfWidth = arrowHeadLen * 0.55f

    val baseX = tipX - ndx * arrowHeadLen
    val baseY = tipY - ndy * arrowHeadLen

    val leftX = baseX + perpX * arrowHeadHalfWidth
    val leftY = baseY + perpY * arrowHeadHalfWidth

    val rightX = baseX - perpX * arrowHeadHalfWidth
    val rightY = baseY - perpY * arrowHeadHalfWidth

    val path = Path().apply {
        moveTo(tipX, tipY)
        lineTo(leftX, leftY)
        lineTo(rightX, rightY)
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}

private fun DrawScope.drawTextItem(
    textElem: MarkupText,
    width: Float,
    height: Float,
    textMeasurer: TextMeasurer
) {
    val scale = (width / 420f).coerceIn(0.7f, 3.5f)
    val fontSize = (textElem.fontSize * scale).sp

    val textLayout = textMeasurer.measure(
        text = textElem.text,
        style = TextStyle(
            color = textElem.color,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    )

    val centerX = textElem.position.x * width
    val centerY = textElem.position.y * height
    val textW = textLayout.size.width.toFloat()
    val textH = textLayout.size.height.toFloat()

    if (textElem.hasBackgroundBox) {
        val padX = 14f * scale
        val padY = 8f * scale
        val rectLeft = centerX - textW / 2f - padX
        val rectTop = centerY - textH / 2f - padY
        val isDark = (0.299 * textElem.color.red + 0.587 * textElem.color.green + 0.114 * textElem.color.blue) < 0.5
        val bgColor = if (isDark) Color.White.copy(alpha = 0.90f) else Color.Black.copy(alpha = 0.80f)

        drawRoundRect(
            color = bgColor,
            topLeft = Offset(rectLeft, rectTop),
            size = Size(textW + padX * 2, textH + padY * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale, 10f * scale)
        )
    }

    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(centerX - textW / 2f, centerY - textH / 2f)
    )
}
