package com.inferno.gallery.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

enum class DragDropTargetZone { NONE, TOOLBAR, SHELF }

enum class DragActionType { REORDER, ADD, REMOVE, REPLACE, REJECTED }

data class DragDropResult(
    val updatedActive: List<String>,
    val updatedAvailable: List<String>,
    val actionTaken: DragActionType
)

object ToolbarDragDropMath {
    fun calculateTargetSlot(pointerX: Float, slotCenters: List<Float>): Int {
        if (slotCenters.isEmpty()) return 0
        var closestIndex = 0
        var minDistance = Float.MAX_VALUE
        for (i in slotCenters.indices) {
            val dist = kotlin.math.abs(pointerX - slotCenters[i])
            if (dist < minDistance) {
                minDistance = dist
                closestIndex = i
            }
        }
        return closestIndex
    }

    fun resolveDrop(
        draggedId: String,
        sourceZone: DragDropTargetZone,
        sourceIndex: Int,
        targetZone: DragDropTargetZone,
        targetSlotIndex: Int,
        currentActive: List<String>,
        currentAvailable: List<String>,
        minSlots: Int = 2,
        maxSlots: Int = 6
    ): DragDropResult {
        if (targetZone == DragDropTargetZone.NONE) {
            return DragDropResult(currentActive, currentAvailable, DragActionType.REJECTED)
        }

        if (sourceZone == DragDropTargetZone.TOOLBAR && targetZone == DragDropTargetZone.TOOLBAR) {
            if (sourceIndex !in currentActive.indices) {
                return DragDropResult(currentActive, currentAvailable, DragActionType.REJECTED)
            }
            val clampedTarget = targetSlotIndex.coerceIn(0, currentActive.size - 1)
            if (sourceIndex == clampedTarget) {
                return DragDropResult(currentActive, currentAvailable, DragActionType.REORDER)
            }
            val mutableActive = currentActive.toMutableList()
            val item = mutableActive.removeAt(sourceIndex)
            mutableActive.add(clampedTarget, item)
            return DragDropResult(mutableActive, currentAvailable, DragActionType.REORDER)
        }

        if (sourceZone == DragDropTargetZone.SHELF && targetZone == DragDropTargetZone.TOOLBAR) {
            if (currentActive.size >= maxSlots) {
                val clampedTarget = targetSlotIndex.coerceIn(0, currentActive.size - 1)
                val mutableActive = currentActive.toMutableList()
                val replacedItem = mutableActive[clampedTarget]
                mutableActive[clampedTarget] = draggedId
                val mutableAvailable = currentAvailable.toMutableList()
                mutableAvailable.remove(draggedId)
                mutableAvailable.add(replacedItem)
                return DragDropResult(mutableActive, mutableAvailable, DragActionType.REPLACE)
            } else {
                val clampedTarget = targetSlotIndex.coerceIn(0, currentActive.size)
                val mutableActive = currentActive.toMutableList()
                mutableActive.add(clampedTarget, draggedId)
                val mutableAvailable = currentAvailable.toMutableList()
                mutableAvailable.remove(draggedId)
                return DragDropResult(mutableActive, mutableAvailable, DragActionType.ADD)
            }
        }

        if (sourceZone == DragDropTargetZone.TOOLBAR && targetZone == DragDropTargetZone.SHELF) {
            if (currentActive.size <= minSlots) {
                return DragDropResult(currentActive, currentAvailable, DragActionType.REJECTED)
            }
            val mutableActive = currentActive.toMutableList()
            mutableActive.remove(draggedId)
            val mutableAvailable = currentAvailable.toMutableList()
            if (!mutableAvailable.contains(draggedId)) {
                mutableAvailable.add(draggedId)
            }
            return DragDropResult(mutableActive, mutableAvailable, DragActionType.REMOVE)
        }

        return DragDropResult(currentActive, currentAvailable, DragActionType.REJECTED)
    }
}

class ToolbarDragDropState(
    val minSlots: Int = 2,
    val maxSlots: Int = 6
) {
    var draggedId by mutableStateOf<String?>(null)
    var dragSourceZone by mutableStateOf(DragDropTargetZone.NONE)
    var dragSourceIndex by mutableIntStateOf(-1)
    var dragOffset by mutableStateOf(Offset.Zero)
    var dragInitialGlobalOffset by mutableStateOf(Offset.Zero)
    var hoverTargetZone by mutableStateOf(DragDropTargetZone.NONE)
    var hoverSlotIndex by mutableIntStateOf(-1)

    val isDragging: Boolean get() = draggedId != null
    val currentPointer: Offset get() = dragInitialGlobalOffset + dragOffset

    fun startDrag(
        id: String,
        sourceZone: DragDropTargetZone,
        sourceIndex: Int,
        initialGlobalOffset: Offset
    ) {
        draggedId = id
        dragSourceZone = sourceZone
        dragSourceIndex = sourceIndex
        dragOffset = Offset.Zero
        dragInitialGlobalOffset = initialGlobalOffset
        hoverTargetZone = sourceZone
        hoverSlotIndex = sourceIndex
    }

    fun updateDrag(
        dragDelta: Offset,
        toolbarBounds: Rect?,
        shelfBounds: Rect?,
        slotCenters: List<Float>
    ) {
        if (!isDragging) return
        dragOffset += dragDelta
        val pointer = dragInitialGlobalOffset + dragOffset

        hoverTargetZone = when {
            toolbarBounds != null && shelfBounds != null -> {
                val dividingY = (toolbarBounds.bottom + shelfBounds.top) / 2f
                if (pointer.y < dividingY) DragDropTargetZone.TOOLBAR else DragDropTargetZone.SHELF
            }
            toolbarBounds != null && toolbarBounds.contains(pointer) -> DragDropTargetZone.TOOLBAR
            shelfBounds != null && shelfBounds.contains(pointer) -> DragDropTargetZone.SHELF
            else -> DragDropTargetZone.NONE
        }

        if (hoverTargetZone == DragDropTargetZone.TOOLBAR && slotCenters.isNotEmpty()) {
            hoverSlotIndex = ToolbarDragDropMath.calculateTargetSlot(pointer.x, slotCenters)
        } else {
            hoverSlotIndex = -1
        }
    }

    fun endDrag(
        currentActive: List<String>,
        currentAvailable: List<String>
    ): DragDropResult {
        val id = draggedId ?: return DragDropResult(currentActive, currentAvailable, DragActionType.REJECTED)
        val result = ToolbarDragDropMath.resolveDrop(
            draggedId = id,
            sourceZone = dragSourceZone,
            sourceIndex = dragSourceIndex,
            targetZone = hoverTargetZone,
            targetSlotIndex = hoverSlotIndex,
            currentActive = currentActive,
            currentAvailable = currentAvailable,
            minSlots = minSlots,
            maxSlots = maxSlots
        )
        reset()
        return result
    }

    fun reset() {
        draggedId = null
        dragSourceZone = DragDropTargetZone.NONE
        dragSourceIndex = -1
        dragOffset = Offset.Zero
        dragInitialGlobalOffset = Offset.Zero
        hoverTargetZone = DragDropTargetZone.NONE
        hoverSlotIndex = -1
    }
}

@Composable
fun rememberToolbarDragDropState(
    minSlots: Int = 2,
    maxSlots: Int = 6
): ToolbarDragDropState {
    return remember { ToolbarDragDropState(minSlots = minSlots, maxSlots = maxSlots) }
}

