package com.inferno.gallery.ui.editor.markup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkupModelsTest {

    @Test
    fun testStrokeHitTestDetectsPointNearSegment() {
        val stroke = MarkupStroke(
            id = 1L,
            points = listOf(Offset(0.1f, 0.1f), Offset(0.5f, 0.5f)),
            color = Color.Red,
            strokeWidth = 0.01f,
            tool = MarkupTool.PEN
        )
        // Hit near segment (0.3, 0.3)
        assertTrue(stroke.isHitBy(Offset(0.31f, 0.30f), threshold = 0.03f))
        // Far away point (0.9, 0.1) should not hit
        assertFalse(stroke.isHitBy(Offset(0.9f, 0.1f), threshold = 0.03f))
    }

    @Test
    fun testStateImmutabilityAndUndo() {
        val state1 = MarkupState()
        val stroke = MarkupStroke(1L, listOf(Offset(0.1f, 0.1f)), Color.Black, 0.01f, MarkupTool.PEN)
        val state2 = state1.withAddedStroke(stroke)
        assertEquals(0, state1.strokes.size)
        assertEquals(1, state2.strokes.size)

        val text = MarkupText(2L, "Caption", Offset(0.5f, 0.5f), Color.White)
        val state3 = state2.withAddedText(text)
        assertEquals(1, state3.texts.size)

        val state4 = state3.withRemovedStroke(1L)
        assertEquals(0, state4.strokes.size)
        assertEquals(1, state4.texts.size)

        val state5 = state4.withRemovedText(2L)
        assertEquals(0, state5.texts.size)
    }

    @Test
    fun testTextHitTestingWithScaleAndRotation() {
        val text = MarkupText(
            id = 10L,
            text = "Hello World",
            position = Offset(0.5f, 0.5f),
            color = Color.White,
            scale = 1.0f,
            rotation = 0f
        )
        // Center point should hit
        assertTrue(text.isHitBy(Offset(0.5f, 0.5f), width = 1000f, height = 1000f))
        // Way off point should not hit
        assertFalse(text.isHitBy(Offset(0.1f, 0.1f), width = 1000f, height = 1000f))

        // Rotated 90 degrees: vertical offset near center should hit, horizontal far offset should not
        val rotated = text.copy(rotation = 90f)
        assertTrue(rotated.isHitBy(Offset(0.5f, 0.52f), width = 1000f, height = 1000f))

        // Scaled up text covers larger area
        val scaledUp = text.copy(scale = 2.5f)
        assertTrue(scaledUp.isHitBy(Offset(0.58f, 0.5f), width = 1000f, height = 1000f))
    }
}

