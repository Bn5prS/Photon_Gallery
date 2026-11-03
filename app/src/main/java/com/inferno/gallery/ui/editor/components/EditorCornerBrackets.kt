package com.inferno.gallery.ui.editor.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Interactive 4-corner L-bracket crop handles.
 *
 * Overlays directly on the photo bounding box with:
 * - 4 white L-shaped thick bracket handles with drop shadows on the corners.
 * - 4 edge line handles for top, bottom, left, right edge resizing.
 * - Multi-touch pinch-to-zoom on the image with locked center crop box.
 * - 1-finger swipe / pan (left, right, up, down) of the image within the crop box.
 * - Constant, non-scaling DP line and bracket thickness (never thick or distorted).
 * - Aspect ratio constraint clamping (preserving exact aspect ratio when provided).
 * - Animated rule-of-thirds 3x3 grid lines visible when dragging.
 * - Scrim darkening outside of the active crop rectangle.
 */
@Composable
fun EditorCornerBrackets(
    cropRect: Rect, // Normalized 0..1
    onCropRectChange: (Rect) -> Unit,
    aspectRatio: Float? = null, // null for freeform
    isDragging: Boolean = false,
    onDragHandleStateChange: (Boolean) -> Unit = {},
    cropBoxOnScreen: Rect? = null, // Screen-space crop box to draw and hit-test
    photoWidth: Float = 0f,
    photoHeight: Float = 0f,
    photoLeft: Float = 0f,
    photoTop: Float = 0f,
    onPhotoPanZoom: (Rect) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentCropRect by rememberUpdatedState(cropRect)
    val currentAspectRatio by rememberUpdatedState(aspectRatio)
    val currentOnCropRectChange by rememberUpdatedState(onCropRectChange)
    val currentOnDragHandleStateChange by rememberUpdatedState(onDragHandleStateChange)
    val currentCropBoxOnScreen by rememberUpdatedState(cropBoxOnScreen)
    val currentPhotoWidth by rememberUpdatedState(photoWidth)
    val currentPhotoHeight by rememberUpdatedState(photoHeight)
    val currentPhotoLeft by rememberUpdatedState(photoLeft)
    val currentPhotoTop by rememberUpdatedState(photoTop)
    val currentOnPhotoPanZoom by rememberUpdatedState(onPhotoPanZoom)

    val density = LocalDensity.current
    val touchRadiusPx = with(density) { 32.dp.toPx() } // 64dp hit target diameter
    val armLengthPx = with(density) { 24.dp.toPx() }
    val bracketThicknessPx = with(density) { 3.5.dp.toPx() }
    val shadowThicknessPx = bracketThicknessPx + with(density) { 2.dp.toPx() }
    val thinBorderPx = with(density) { 1.dp.toPx() }

    var activeHandle by remember { mutableStateOf(CornerHandle.NONE) }
    var isInteracting by remember { mutableStateOf(false) }
    val showGrid = isDragging || isInteracting || activeHandle != CornerHandle.NONE
    val gridAlpha by animateFloatAsState(
        targetValue = if (showGrid) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "cropGridAlpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    if (w <= 0f || h <= 0f) return@awaitEachGesture

                    val activeFrame = currentCropBoxOnScreen ?: Rect(
                        currentCropRect.left * w,
                        currentCropRect.top * h,
                        currentCropRect.right * w,
                        currentCropRect.bottom * h
                    )

                    val handle = EditorCornerBracketsMath.hitTestScreen(
                        touchX = down.position.x,
                        touchY = down.position.y,
                        boxRect = activeFrame,
                        touchRadiusPx = touchRadiusPx
                    )

                    if (handle == CornerHandle.NONE) {
                        return@awaitEachGesture
                    }

                    val pw = if (currentPhotoWidth > 0f) currentPhotoWidth else w
                    val ph = if (currentPhotoHeight > 0f) currentPhotoHeight else h
                    val pl = if (currentPhotoWidth > 0f) currentPhotoLeft else (activeFrame.left - currentCropRect.left * pw)
                    val pt = if (currentPhotoHeight > 0f) currentPhotoTop else (activeFrame.top - currentCropRect.top * ph)

                    if (handle != CornerHandle.INSIDE) {
                        // ── CORNER OR EDGE HANDLE DRAGGING ────────────────────────
                        activeHandle = handle
                        isInteracting = true
                        currentOnDragHandleStateChange(true)
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedPointers = event.changes.filter { it.pressed }
                            if (pressedPointers.isEmpty()) break

                            val pointer = pressedPointers.first()
                            val dragDelta = pointer.position - pointer.previousPosition

                            if (dragDelta != Offset.Zero) {
                                pointer.consume()
                                val deltaXNorm = dragDelta.x / pw
                                val deltaYNorm = dragDelta.y / ph

                                val updated = EditorCornerBracketsMath.updateCropRect(
                                    currentRect = currentCropRect,
                                    handle = handle,
                                    deltaXNormalized = deltaXNorm,
                                    deltaYNormalized = deltaYNorm,
                                    aspectRatio = currentAspectRatio,
                                    canvasWidth = pw,
                                    canvasHeight = ph
                                )
                                currentOnCropRectChange(updated)
                            }
                        }

                        activeHandle = CornerHandle.NONE
                        isInteracting = false
                        currentOnDragHandleStateChange(false)

                    } else {
                        // ── INSIDE CROP BOX: 1-FINGER SWIPE PAN & 2-FINGER PINCH-TO-ZOOM ──
                        activeHandle = CornerHandle.INSIDE
                        isInteracting = true
                        down.consume()

                        var prevPointerCount = 1
                        var prevCentroid = down.position

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedPointers = event.changes.filter { it.pressed }
                            if (pressedPointers.isEmpty()) break

                            val pointerCount = pressedPointers.size

                            if (pointerCount >= 2) {
                                val p1 = pressedPointers[0]
                                val p2 = pressedPointers[1]
                                val currentCentroid = (p1.position + p2.position) / 2f
                                val currentDist = (p1.position - p2.position).getDistance()
                                val prevDist = (p1.previousPosition - p2.previousPosition).getDistance()

                                val zoomFactor = if (prevDist > 0f) currentDist / prevDist else 1f
                                val panDelta = if (prevPointerCount >= 2) currentCentroid - prevCentroid else Offset.Zero

                                if (zoomFactor != 1f || panDelta != Offset.Zero) {
                                    pressedPointers.forEach { it.consume() }

                                    var updated = if (zoomFactor != 1f) {
                                        EditorCornerBracketsMath.zoomPhotoInCrop(
                                            currentRect = currentCropRect,
                                            zoomFactor = zoomFactor,
                                            centroidXPx = currentCentroid.x,
                                            centroidYPx = currentCentroid.y,
                                            photoLeftPx = pl,
                                            photoTopPx = pt,
                                            photoWidthPx = pw,
                                            photoHeightPx = ph,
                                            aspectRatio = currentAspectRatio
                                        )
                                    } else currentCropRect

                                    if (panDelta != Offset.Zero) {
                                        updated = EditorCornerBracketsMath.panPhotoInCrop(
                                            currentRect = updated,
                                            dragDeltaXPx = panDelta.x,
                                            dragDeltaYPx = panDelta.y,
                                            photoWidthPx = pw,
                                            photoHeightPx = ph
                                        )
                                    }
                                    currentOnPhotoPanZoom(updated)
                                }

                                prevCentroid = currentCentroid
                                prevPointerCount = pointerCount
                            } else {
                                val pointer = pressedPointers.first()
                                val dragDelta = pointer.position - pointer.previousPosition

                                if (dragDelta != Offset.Zero) {
                                    pointer.consume()
                                    val updated = EditorCornerBracketsMath.panPhotoInCrop(
                                        currentRect = currentCropRect,
                                        dragDeltaXPx = dragDelta.x,
                                        dragDeltaYPx = dragDelta.y,
                                        photoWidthPx = pw,
                                        photoHeightPx = ph
                                    )
                                    currentOnPhotoPanZoom(updated)
                                }

                                prevCentroid = pointer.position
                                prevPointerCount = 1
                            }
                        }

                        activeHandle = CornerHandle.NONE
                        isInteracting = false
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val activeFrame = currentCropBoxOnScreen ?: Rect(
            currentCropRect.left * w,
            currentCropRect.top * h,
            currentCropRect.right * w,
            currentCropRect.bottom * h
        )

        val l = activeFrame.left.coerceIn(0f, w)
        val t = activeFrame.top.coerceIn(0f, h)
        val r = activeFrame.right.coerceIn(l, w)
        val b = activeFrame.bottom.coerceIn(t, h)
        val cropW = r - l
        val cropH = b - t

        // 1. Dimmed Scrim Outside Crop Rectangle (full bleed to edges via EvenOdd path)
        val scrimColor = Color.Black.copy(alpha = 0.55f)
        val scrimPath = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, w, h))
            addRect(Rect(l, t, r, b))
        }
        drawPath(scrimPath, color = scrimColor)

        // 2. Subtle White Thin Bounding Box (always crisp 1dp)
        drawRect(
            color = Color.White.copy(alpha = 0.65f),
            topLeft = Offset(l, t),
            size = Size(cropW, cropH),
            style = Stroke(width = thinBorderPx)
        )

        // 3. Rule-of-Thirds Grid Lines (3x3)
        if (gridAlpha > 0f && cropW > 0f && cropH > 0f) {
            val gridColor = Color.White.copy(alpha = 0.65f * gridAlpha)
            val shadowGridColor = Color.Black.copy(alpha = 0.4f * gridAlpha)

            val x1 = l + cropW / 3f
            val x2 = l + 2f * cropW / 3f
            val y1 = t + cropH / 3f
            val y2 = t + 2f * cropH / 3f

            // Shadow vertical
            drawLine(shadowGridColor, Offset(x1 + 1f, t), Offset(x1 + 1f, b), strokeWidth = thinBorderPx)
            drawLine(shadowGridColor, Offset(x2 + 1f, t), Offset(x2 + 1f, b), strokeWidth = thinBorderPx)
            // Vertical lines
            drawLine(gridColor, Offset(x1, t), Offset(x1, b), strokeWidth = thinBorderPx)
            drawLine(gridColor, Offset(x2, t), Offset(x2, b), strokeWidth = thinBorderPx)

            // Shadow horizontal
            drawLine(shadowGridColor, Offset(l, y1 + 1f), Offset(r, y1 + 1f), strokeWidth = thinBorderPx)
            drawLine(shadowGridColor, Offset(l, y2 + 1f), Offset(r, y2 + 1f), strokeWidth = thinBorderPx)
            // Horizontal lines
            drawLine(gridColor, Offset(l, y1), Offset(r, y1), strokeWidth = thinBorderPx)
            drawLine(gridColor, Offset(l, y2), Offset(r, y2), strokeWidth = thinBorderPx)
        }

        // 4. 4 White L-Bracket Handles with Shadows (always crisp 3.5dp)
        val clampedArmW = minOf(armLengthPx, cropW / 2f)
        val clampedArmH = minOf(armLengthPx, cropH / 2f)

        drawCornerLBracket(
            x = l, y = t,
            dirX = 1f, dirY = 1f,
            armW = clampedArmW, armH = clampedArmH,
            strokeWidth = bracketThicknessPx,
            shadowWidth = shadowThicknessPx
        )
        drawCornerLBracket(
            x = r, y = t,
            dirX = -1f, dirY = 1f,
            armW = clampedArmW, armH = clampedArmH,
            strokeWidth = bracketThicknessPx,
            shadowWidth = shadowThicknessPx
        )
        drawCornerLBracket(
            x = l, y = b,
            dirX = 1f, dirY = -1f,
            armW = clampedArmW, armH = clampedArmH,
            strokeWidth = bracketThicknessPx,
            shadowWidth = shadowThicknessPx
        )
        drawCornerLBracket(
            x = r, y = b,
            dirX = -1f, dirY = -1f,
            armW = clampedArmW, armH = clampedArmH,
            strokeWidth = bracketThicknessPx,
            shadowWidth = shadowThicknessPx
        )
    }
}

/**
 * Draws an L-shaped corner bracket with a subtle drop shadow for maximum legibility.
 */
private fun DrawScope.drawCornerLBracket(
    x: Float,
    y: Float,
    dirX: Float,
    dirY: Float,
    armW: Float,
    armH: Float,
    strokeWidth: Float,
    shadowWidth: Float
) {
    val shadowColor = Color.Black.copy(alpha = 0.55f)
    val bracketColor = Color.White

    // Shadow L-Bracket
    drawLine(
        color = shadowColor,
        start = Offset(x, y),
        end = Offset(x + dirX * armW, y),
        strokeWidth = shadowWidth,
        cap = StrokeCap.Square
    )
    drawLine(
        color = shadowColor,
        start = Offset(x, y),
        end = Offset(x, y + dirY * armH),
        strokeWidth = shadowWidth,
        cap = StrokeCap.Square
    )

    // Foreground White L-Bracket
    drawLine(
        color = bracketColor,
        start = Offset(x, y),
        end = Offset(x + dirX * armW, y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Square
    )
    drawLine(
        color = bracketColor,
        start = Offset(x, y),
        end = Offset(x, y + dirY * armH),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Square
    )
}

