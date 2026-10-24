package com.inferno.gallery.ui.editor.markup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
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
    eraserRadius: Float = MarkupPalette.DEFAULT_ERASER_RADIUS,
    selectedTextId: Long? = null,
    onSelectText: (Long?) -> Unit = {},
    onStateChanged: (MarkupState) -> Unit,
    onEditTextRequest: (MarkupText?) -> Unit,
    isInteractive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val selectionAccentColor = MaterialTheme.colorScheme.primary
    val eraserColor = MaterialTheme.colorScheme.error

    // Active live stroke points while finger is down (in normalized 0..1 coordinates)
    val livePoints = remember { mutableStateListOf<Offset>() }
    // Visual cursor circle while erasing
    var eraserCursorPoint by remember { mutableStateOf<Offset?>(null) }

    val interactiveModifier = if (isInteractive) {
        Modifier.pointerInput(activeTool, markupState, currentColor, currentStrokeWidth, eraserRadius, selectedTextId) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val canvasW = size.width.toFloat()
                val canvasH = size.height.toFloat()
                if (canvasW <= 0f || canvasH <= 0f) return@awaitEachGesture

                val normDown = Offset(
                    (down.position.x / canvasW).coerceIn(0f, 1f),
                    (down.position.y / canvasH).coerceIn(0f, 1f)
                )

                if (activeTool == MarkupTool.TEXT) {
                    // Text manipulation mode (drag, 2-finger scale, 2-finger twist rotation, tap)
                    val currentSelected = markupState.texts.find { it.id == selectedTextId }
                    val hitText = if (currentSelected != null && currentSelected.isHitBy(normDown, canvasW, canvasH)) {
                        currentSelected
                    } else {
                        markupState.texts.findLast { it.isHitBy(normDown, canvasW, canvasH) }
                    }

                    if (hitText != null) {
                        var targetText: MarkupText = hitText
                        var totalMovement = 0f
                        var isTransforming = false

                        if (selectedTextId != hitText.id) {
                            onSelectText(hitText.id)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }
                            if (activePointers.isEmpty()) break

                            val pointerCount = activePointers.size
                            if (pointerCount == 1) {
                                val pan = event.calculatePan()
                                totalMovement += pan.getDistance()
                                if (totalMovement > 6f || isTransforming) {
                                    isTransforming = true
                                    event.changes.forEach { it.consume() }
                                    val normPanX = pan.x / canvasW
                                    val normPanY = pan.y / canvasH
                                    val newPos = Offset(
                                        (targetText.position.x + normPanX).coerceIn(0.05f, 0.95f),
                                        (targetText.position.y + normPanY).coerceIn(0.05f, 0.95f)
                                    )
                                    targetText = targetText.copy(position = newPos)
                                    onStateChanged(markupState.withUpdatedText(targetText))
                                }
                            } else if (pointerCount >= 2) {
                                isTransforming = true
                                event.changes.forEach { it.consume() }

                                val zoom = event.calculateZoom()
                                val rotation = event.calculateRotation()
                                val pan = event.calculatePan()

                                val normPanX = pan.x / canvasW
                                val normPanY = pan.y / canvasH
                                val newPos = Offset(
                                    (targetText.position.x + normPanX).coerceIn(0.05f, 0.95f),
                                    (targetText.position.y + normPanY).coerceIn(0.05f, 0.95f)
                                )
                                val newScale = (targetText.scale * zoom).coerceIn(0.4f, 4.0f)
                                val newRotation = (targetText.rotation + rotation) % 360f

                                targetText = targetText.copy(
                                    position = newPos,
                                    scale = newScale,
                                    rotation = newRotation
                                )
                                onStateChanged(markupState.withUpdatedText(targetText))
                            }
                        }

                        if (!isTransforming && totalMovement < 10f) {
                            if (selectedTextId == hitText.id) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEditTextRequest(hitText)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectText(hitText.id)
                            }
                        }
                    } else {
                        // Touched blank canvas space in Text mode
                        var totalMovement = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }
                            if (activePointers.isEmpty()) break
                            val pan = event.calculatePan()
                            totalMovement += pan.getDistance()
                        }
                        if (totalMovement < 10f) {
                            if (selectedTextId != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectText(null)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEditTextRequest(null)
                            }
                        }
                    }
                } else if (activeTool == MarkupTool.ERASER) {
                    // Continuous interpolated erasing with visual cursor
                    var currentLocalState = markupState
                    var lastNorm = normDown
                    eraserCursorPoint = down.position
                    down.consume()

                    // Check initial touch point
                    val initialStrokeHit = currentLocalState.strokes.findLast { it.isHitBy(normDown, eraserRadius) }
                    if (initialStrokeHit != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentLocalState = currentLocalState.withRemovedStroke(initialStrokeHit.id)
                        onStateChanged(currentLocalState)
                    }
                    val initialTextHit = currentLocalState.texts.findLast { it.isHitBy(normDown, canvasW, canvasH) }
                    if (initialTextHit != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentLocalState = currentLocalState.withRemovedText(initialTextHit.id)
                        onStateChanged(currentLocalState)
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val active = event.changes.filter { it.pressed }
                        if (active.isEmpty()) {
                            eraserCursorPoint = null
                            break
                        }

                        val pt = active.first().position
                        eraserCursorPoint = pt
                        val currentNorm = Offset(
                            (pt.x / canvasW).coerceIn(0f, 1f),
                            (pt.y / canvasH).coerceIn(0f, 1f)
                        )

                        // Interpolate between lastNorm and currentNorm to catch fast finger sweeps
                        val dx = currentNorm.x - lastNorm.x
                        val dy = currentNorm.y - lastNorm.y
                        val dist = sqrt(dx * dx + dy * dy)
                        val stepSize = (eraserRadius * 0.4f).coerceIn(0.008f, 0.025f)
                        val steps = maxOf(1, (dist / stepSize).toInt())

                        var stateChanged = false
                        for (s in 1..steps) {
                            val ratio = s.toFloat() / steps
                            val interNorm = Offset(lastNorm.x + dx * ratio, lastNorm.y + dy * ratio)

                            val hitStroke = currentLocalState.strokes.findLast { it.isHitBy(interNorm, eraserRadius) }
                            if (hitStroke != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentLocalState = currentLocalState.withRemovedStroke(hitStroke.id)
                                stateChanged = true
                            }

                            val hitText = currentLocalState.texts.findLast { it.isHitBy(interNorm, canvasW, canvasH) }
                            if (hitText != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentLocalState = currentLocalState.withRemovedText(hitText.id)
                                stateChanged = true
                            }
                        }

                        if (stateChanged) {
                            onStateChanged(currentLocalState)
                        }

                        lastNorm = currentNorm
                        event.changes.forEach { it.consume() }
                    }
                } else {
                    // Drawing tools (PEN, HIGHLIGHTER, ARROW)
                    val hitText = markupState.texts.findLast { it.isHitBy(normDown, canvasW, canvasH) }
                    livePoints.clear()
                    livePoints.add(normDown)
                    down.consume()
                    var totalMovement = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val active = event.changes.filter { it.pressed }
                        if (active.isEmpty()) break
                        val pt = active.first().position
                        val norm = Offset((pt.x / canvasW).coerceIn(0f, 1f), (pt.y / canvasH).coerceIn(0f, 1f))
                        val pan = event.calculatePan()
                        totalMovement += pan.getDistance()

                        val last = livePoints.lastOrNull()
                        if (last == null || (norm.x - last.x) * (norm.x - last.x) + (norm.y - last.y) * (norm.y - last.y) >= 0.000004f) {
                            livePoints.add(norm)
                        }
                        event.changes.forEach { it.consume() }
                    }

                    if (hitText != null && totalMovement < 10f && livePoints.size <= 2) {
                        livePoints.clear()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectText(hitText.id)
                    } else if (livePoints.isNotEmpty()) {
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
            }
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
                drawTextItem(
                    textElem = textElem,
                    width = canvasW,
                    height = canvasH,
                    textMeasurer = textMeasurer,
                    isSelected = (textElem.id == selectedTextId),
                    selectionColor = selectionAccentColor
                )
            }

            // 4. Draw eraser reticle while finger is down
            if (activeTool == MarkupTool.ERASER && eraserCursorPoint != null) {
                val center = eraserCursorPoint!!
                val radiusPx = eraserRadius * canvasW

                drawCircle(
                    color = eraserColor.copy(alpha = 0.15f),
                    radius = radiusPx,
                    center = center
                )
                drawCircle(
                    color = eraserColor.copy(alpha = 0.85f),
                    radius = radiusPx,
                    center = center,
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                )
                drawCircle(
                    color = eraserColor,
                    radius = 3.5f,
                    center = center
                )
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
            drawArrowStrokeAndHead(
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

private fun DrawScope.drawArrowStrokeAndHead(
    points: List<Offset>,
    width: Float,
    height: Float,
    color: Color,
    strokePx: Float
) {
    if (points.isEmpty()) return
    val tip = points.last()
    val tipX = tip.x * width
    val tipY = tip.y * height

    if (points.size == 1) {
        drawCircle(
            color = color,
            radius = strokePx / 2f,
            center = Offset(tipX, tipY)
        )
        return
    }

    val rawHeadLen = (strokePx * 3.4f).coerceIn(24f, 68f)
    val minAnchorDist = maxOf(rawHeadLen * 0.8f, 20f)
    val minAnchorDistSq = minAnchorDist * minAnchorDist

    var anchorX = points[0].x * width
    var anchorY = points[0].y * height
    for (i in points.size - 2 downTo 0) {
        val px = points[i].x * width
        val py = points[i].y * height
        val dSq = (tipX - px) * (tipX - px) + (tipY - py) * (tipY - py)
        if (dSq >= minAnchorDistSq) {
            anchorX = px
            anchorY = py
            break
        }
    }

    val dx = tipX - anchorX
    val dy = tipY - anchorY
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1e-4f) return

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

    // 1. Draw shaft line terminating cleanly at notch (hiding round cap inside arrowhead)
    val shaftPath = Path()
    shaftPath.moveTo(points[0].x * width, points[0].y * height)
    for (i in 1 until points.size - 1) {
        shaftPath.lineTo(points[i].x * width, points[i].y * height)
    }
    shaftPath.lineTo(notchX, notchY)

    drawPath(
        path = shaftPath,
        color = color,
        style = Stroke(
            width = strokePx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 2. Draw sharp stealth arrowhead
    val headPath = Path().apply {
        moveTo(tipX, tipY)
        lineTo(leftX, leftY)
        lineTo(notchX, notchY)
        lineTo(rightX, rightY)
        close()
    }
    drawPath(path = headPath, color = color, style = Fill)
}

private fun DrawScope.drawTextItem(
    textElem: MarkupText,
    width: Float,
    height: Float,
    textMeasurer: TextMeasurer,
    isSelected: Boolean,
    selectionColor: Color
) {
    val baseScale = (width / 420f).coerceIn(0.7f, 3.5f)
    val fontSize = (textElem.fontSize * baseScale).sp

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

    withTransform({
        translate(centerX, centerY)
        if (textElem.rotation != 0f) {
            rotate(degrees = textElem.rotation, pivot = Offset.Zero)
        }
        if (textElem.scale != 1.0f) {
            scale(scaleX = textElem.scale, scaleY = textElem.scale, pivot = Offset.Zero)
        }
    }) {
        val padX = 14f * baseScale
        val padY = 8f * baseScale
        val rectLeft = -textW / 2f - padX
        val rectTop = -textH / 2f - padY
        val rectW = textW + padX * 2f
        val rectH = textH + padY * 2f

        if (textElem.hasBackgroundBox) {
            val isDark = (0.299 * textElem.color.red + 0.587 * textElem.color.green + 0.114 * textElem.color.blue) < 0.5
            val bgColor = if (isDark) Color.White.copy(alpha = 0.90f) else Color.Black.copy(alpha = 0.80f)

            drawRoundRect(
                color = bgColor,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectW, rectH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * baseScale, 10f * baseScale)
            )
        }

        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(-textW / 2f, -textH / 2f)
        )

        if (isSelected) {
            val selPad = 6f * baseScale
            val selLeft = rectLeft - selPad
            val selTop = rectTop - selPad
            val selW = rectW + selPad * 2f
            val selH = rectH + selPad * 2f
            val strokeWidthPx = 2f * baseScale

            drawRoundRect(
                color = selectionColor,
                topLeft = Offset(selLeft, selTop),
                size = Size(selW, selH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * baseScale, 12f * baseScale),
                style = Stroke(
                    width = strokeWidthPx,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            )

            val handleRadius = 4.5f * baseScale
            val corners = listOf(
                Offset(selLeft, selTop),
                Offset(selLeft + selW, selTop),
                Offset(selLeft, selTop + selH),
                Offset(selLeft + selW, selTop + selH)
            )
            for (corner in corners) {
                drawCircle(
                    color = Color.White,
                    radius = handleRadius,
                    center = corner
                )
                drawCircle(
                    color = selectionColor,
                    radius = handleRadius,
                    center = corner,
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
    }
}
