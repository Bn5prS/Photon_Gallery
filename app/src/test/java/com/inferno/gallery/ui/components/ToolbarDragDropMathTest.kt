package com.inferno.gallery.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolbarDragDropMathTest {

    @Test
    fun testCalculateTargetSlot() {
        val slotCenters = listOf(50f, 150f, 250f, 350f, 450f)
        assertEquals(0, ToolbarDragDropMath.calculateTargetSlot(20f, slotCenters))
        assertEquals(0, ToolbarDragDropMath.calculateTargetSlot(80f, slotCenters))
        assertEquals(1, ToolbarDragDropMath.calculateTargetSlot(130f, slotCenters))
        assertEquals(2, ToolbarDragDropMath.calculateTargetSlot(220f, slotCenters))
        assertEquals(4, ToolbarDragDropMath.calculateTargetSlot(490f, slotCenters))
        assertEquals(4, ToolbarDragDropMath.calculateTargetSlot(600f, slotCenters))
    }

    @Test
    fun testReorderWithinToolbar() {
        val active = listOf("share", "favorite", "edit", "info", "delete")
        val available = listOf("move", "vault")

        val result = ToolbarDragDropMath.resolveDrop(
            draggedId = "share",
            sourceZone = DragDropTargetZone.TOOLBAR,
            sourceIndex = 0,
            targetZone = DragDropTargetZone.TOOLBAR,
            targetSlotIndex = 2,
            currentActive = active,
            currentAvailable = available
        )

        assertEquals(DragActionType.REORDER, result.actionTaken)
        assertEquals(listOf("favorite", "edit", "share", "info", "delete"), result.updatedActive)
        assertEquals(available, result.updatedAvailable)
    }

    @Test
    fun testAddFromShelfToToolbarWithinCapacity() {
        val active = listOf("share", "favorite", "edit", "info", "delete") // size 5 < max 6
        val available = listOf("move", "vault")

        val result = ToolbarDragDropMath.resolveDrop(
            draggedId = "move",
            sourceZone = DragDropTargetZone.SHELF,
            sourceIndex = 0,
            targetZone = DragDropTargetZone.TOOLBAR,
            targetSlotIndex = 1,
            currentActive = active,
            currentAvailable = available
        )

        assertEquals(DragActionType.ADD, result.actionTaken)
        assertEquals(listOf("share", "move", "favorite", "edit", "info", "delete"), result.updatedActive)
        assertEquals(listOf("vault"), result.updatedAvailable)
    }

    @Test
    fun testReplaceWhenToolbarIsFull() {
        val active = listOf("share", "favorite", "edit", "info", "delete", "copy") // size 6 == max
        val available = listOf("move", "vault")

        val result = ToolbarDragDropMath.resolveDrop(
            draggedId = "vault",
            sourceZone = DragDropTargetZone.SHELF,
            sourceIndex = 1,
            targetZone = DragDropTargetZone.TOOLBAR,
            targetSlotIndex = 2, // replace "edit"
            currentActive = active,
            currentAvailable = available,
            maxSlots = 6
        )

        assertEquals(DragActionType.REPLACE, result.actionTaken)
        assertEquals(listOf("share", "favorite", "vault", "info", "delete", "copy"), result.updatedActive)
        assertEquals(listOf("move", "edit"), result.updatedAvailable)
    }

    @Test
    fun testRemoveFromToolbarToShelf() {
        val active = listOf("share", "favorite", "edit", "info", "delete") // size 5 > min 2
        val available = listOf("move")

        val result = ToolbarDragDropMath.resolveDrop(
            draggedId = "edit",
            sourceZone = DragDropTargetZone.TOOLBAR,
            sourceIndex = 2,
            targetZone = DragDropTargetZone.SHELF,
            targetSlotIndex = -1,
            currentActive = active,
            currentAvailable = available
        )

        assertEquals(DragActionType.REMOVE, result.actionTaken)
        assertEquals(listOf("share", "favorite", "info", "delete"), result.updatedActive)
        assertEquals(listOf("move", "edit"), result.updatedAvailable)
    }

    @Test
    fun testRejectRemoveAtMinimumCapacity() {
        val active = listOf("share", "delete") // size 2 == min
        val available = listOf("favorite", "edit")

        val result = ToolbarDragDropMath.resolveDrop(
            draggedId = "delete",
            sourceZone = DragDropTargetZone.TOOLBAR,
            sourceIndex = 1,
            targetZone = DragDropTargetZone.SHELF,
            targetSlotIndex = -1,
            currentActive = active,
            currentAvailable = available,
            minSlots = 2
        )

        assertEquals(DragActionType.REJECTED, result.actionTaken)
        assertEquals(active, result.updatedActive)
        assertEquals(available, result.updatedAvailable)
    }
}

