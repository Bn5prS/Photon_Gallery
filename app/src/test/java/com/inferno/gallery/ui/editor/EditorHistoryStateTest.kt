package com.inferno.gallery.ui.editor

import com.inferno.gallery.ui.editor.model.EditorHistoryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorHistoryStateTest {

    @Test
    fun testInitialState() {
        val history = EditorHistoryState(current = "initial")
        assertEquals("initial", history.current)
        assertTrue(history.past.isEmpty())
        assertTrue(history.future.isEmpty())
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun testPushNewState() {
        val history0 = EditorHistoryState(current = "state0")
        val history1 = history0.push("state1")

        assertEquals("state1", history1.current)
        assertEquals(listOf("state0"), history1.past)
        assertTrue(history1.future.isEmpty())
        assertTrue(history1.canUndo)
        assertFalse(history1.canRedo)

        val history2 = history1.push("state2")
        assertEquals("state2", history2.current)
        assertEquals(listOf("state0", "state1"), history2.past)
        assertTrue(history2.future.isEmpty())
        assertTrue(history2.canUndo)
        assertFalse(history2.canRedo)
    }

    @Test
    fun testUndo() {
        val history = EditorHistoryState(current = "v0")
            .push("v1")
            .push("v2")

        assertTrue(history.canUndo)
        assertFalse(history.canRedo)

        val undo1 = history.undo()
        assertEquals("v1", undo1.current)
        assertEquals(listOf("v0"), undo1.past)
        assertEquals(listOf("v2"), undo1.future)
        assertTrue(undo1.canUndo)
        assertTrue(undo1.canRedo)

        val undo2 = undo1.undo()
        assertEquals("v0", undo2.current)
        assertTrue(undo2.past.isEmpty())
        assertEquals(listOf("v1", "v2"), undo2.future)
        assertFalse(undo2.canUndo)
        assertTrue(undo2.canRedo)

        // Boundary: undo when past is empty
        val undo3 = undo2.undo()
        assertEquals("v0", undo3.current)
        assertTrue(undo3.past.isEmpty())
        assertFalse(undo3.canUndo)
    }

    @Test
    fun testRedo() {
        val history = EditorHistoryState(current = "v0")
            .push("v1")
            .push("v2")
            .undo()
            .undo()

        assertEquals("v0", history.current)
        assertTrue(history.canRedo)

        val redo1 = history.redo()
        assertEquals("v1", redo1.current)
        assertEquals(listOf("v0"), redo1.past)
        assertEquals(listOf("v2"), redo1.future)
        assertTrue(redo1.canUndo)
        assertTrue(redo1.canRedo)

        val redo2 = redo1.redo()
        assertEquals("v2", redo2.current)
        assertEquals(listOf("v0", "v1"), redo2.past)
        assertTrue(redo2.future.isEmpty())
        assertTrue(redo2.canUndo)
        assertFalse(redo2.canRedo)

        // Boundary: redo when future is empty
        val redo3 = redo2.redo()
        assertEquals("v2", redo3.current)
        assertTrue(redo3.future.isEmpty())
        assertFalse(redo3.canRedo)
    }

    @Test
    fun testPushClearsFuture() {
        val history = EditorHistoryState(current = "v0")
            .push("v1")
            .push("v2")
            .undo() // current is v1, future has v2

        assertTrue(history.canRedo)
        assertEquals(listOf("v2"), history.future)

        // Now push a new branch
        val branched = history.push("v3")
        assertEquals("v3", branched.current)
        assertEquals(listOf("v0", "v1"), branched.past)
        assertTrue(branched.future.isEmpty())
        assertFalse(branched.canRedo)
    }

    @Test
    fun testImmutability() {
        val original = EditorHistoryState(current = "v0")
        val next = original.push("v1")

        assertEquals("v0", original.current)
        assertTrue(original.past.isEmpty())
        assertEquals("v1", next.current)
        assertEquals(1, next.past.size)
    }
}
