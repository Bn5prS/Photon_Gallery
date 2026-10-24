package com.inferno.gallery.ui.editor.markup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkupRasterizerTest {

    @Test
    fun testCalculateArrowheadDirection() {
        val prev = Offset(0.1f, 0.5f)
        val tip = Offset(0.9f, 0.5f)
        val head = MarkupRasterizer.calculateArrowhead(tip, prev, width = 1000, height = 1000, strokePx = 10f)

        assertNotNull(head)
        assertEquals(900f, head!!.tipX, 1e-2f)
        assertEquals(500f, head.tipY, 1e-2f)
        // Left and right arrowhead points should be strictly to the left of the tip (smaller X)
        assertTrue(head.leftX < head.tipX)
        assertTrue(head.rightX < head.tipX)
        // Notch is indented between base and tip
        assertTrue(head.notchX > head.leftX)
        assertTrue(head.notchX < head.tipX)
        // Arrowhead should be symmetric above and below the horizontal axis (500f)
        assertEquals(500f, (head.leftY + head.rightY) / 2f, 1e-2f)
        assertEquals(500f, head.notchY, 1e-2f)
    }

    @Test
    fun testCalculateArrowheadZeroLengthReturnsNull() {
        val samePoint = Offset(0.5f, 0.5f)
        val head = MarkupRasterizer.calculateArrowhead(samePoint, samePoint, width = 1000, height = 1000, strokePx = 10f)
        assertNull(head)
    }

    @Test
    fun testIsColorDark() {
        assertTrue("Black must be dark", MarkupRasterizer.isColorDark(Color.Black))
        assertFalse("White must be light", MarkupRasterizer.isColorDark(Color.White))
        assertFalse("Yellow must be light", MarkupRasterizer.isColorDark(Color(0xFFFFD600)))
        assertTrue("Dark Blue must be dark", MarkupRasterizer.isColorDark(Color(0xFF0D47A1)))
    }
}
