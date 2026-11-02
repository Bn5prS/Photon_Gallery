package com.inferno.gallery.ui.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.inferno.gallery.ui.editor.markup.MarkupModelsHelper
import com.inferno.gallery.ui.editor.markup.MarkupRasterizer
import com.inferno.gallery.ui.editor.markup.MarkupState
import com.inferno.gallery.ui.editor.markup.MarkupStroke
import com.inferno.gallery.ui.editor.markup.MarkupTool
import com.inferno.gallery.ui.editor.markup.RedactMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkupRedactTest {

    @Test
    fun testMarkupToolEnumHasRedact() {
        val tool = MarkupTool.REDACT
        assertEquals("Redact", tool.label)
        assertTrue(MarkupTool.entries.contains(MarkupTool.REDACT))
    }

    @Test
    fun testRedactModeEnum() {
        assertEquals(3, RedactMode.entries.size)
        assertTrue(RedactMode.entries.contains(RedactMode.MOSAIC))
        assertTrue(RedactMode.entries.contains(RedactMode.BLUR))
        assertTrue(RedactMode.entries.contains(RedactMode.BLACKOUT))
    }

    @Test
    fun testMarkupStrokeWithRedactMode() {
        val strokeMosaic = MarkupStroke(
            id = 1L,
            points = listOf(Offset(0.2f, 0.3f), Offset(0.7f, 0.3f)),
            color = Color.Black,
            strokeWidth = 0.04f,
            tool = MarkupTool.REDACT,
            redactMode = RedactMode.MOSAIC
        )

        assertEquals(MarkupTool.REDACT, strokeMosaic.tool)
        assertEquals(RedactMode.MOSAIC, strokeMosaic.redactMode)

        val strokeBlackout = strokeMosaic.copy(id = 2L, redactMode = RedactMode.BLACKOUT)
        assertEquals(RedactMode.BLACKOUT, strokeBlackout.redactMode)
    }

    @Test
    fun testRedactHitTestingAlongSegment() {
        val stroke = MarkupStroke(
            id = 3L,
            points = listOf(Offset(0.2f, 0.5f), Offset(0.8f, 0.5f)),
            color = Color.Black,
            strokeWidth = 0.05f,
            tool = MarkupTool.REDACT,
            redactMode = RedactMode.MOSAIC
        )

        // Point directly on segment
        assertTrue(stroke.isHitBy(Offset(0.5f, 0.5f), threshold = 0.03f))

        // Point close to segment within threshold
        assertTrue(stroke.isHitBy(Offset(0.5f, 0.52f), threshold = 0.03f))

        // Point beyond endpoint along horizontal line
        assertFalse(stroke.isHitBy(Offset(0.95f, 0.5f), threshold = 0.03f))

        // Point vertically far from segment
        assertFalse(stroke.isHitBy(Offset(0.5f, 0.7f), threshold = 0.03f))
    }

    @Test
    fun testStrokeComputeBounds() {
        val emptyStroke = MarkupStroke(
            id = 4L,
            points = emptyList(),
            color = Color.Black,
            strokeWidth = 0.04f,
            tool = MarkupTool.REDACT
        )
        assertNull(emptyStroke.computeBounds())

        val stroke = MarkupStroke(
            id = 5L,
            points = listOf(Offset(0.2f, 0.3f), Offset(0.6f, 0.7f)),
            color = Color.Black,
            strokeWidth = 0.04f,
            tool = MarkupTool.REDACT
        )
        val bounds = stroke.computeBounds()
        assertNotNull(bounds)
        // Bounds should enclose all points with half-stroke padding (0.02f)
        assertEquals(0.18f, bounds!!.left, 1e-4f)
        assertEquals(0.28f, bounds.top, 1e-4f)
        assertEquals(0.62f, bounds.right, 1e-4f)
        assertEquals(0.72f, bounds.bottom, 1e-4f)
    }

    @Test
    fun testStrokeSerializationRoundTrip() {
        val original = MarkupStroke(
            id = 42L,
            points = listOf(Offset(0.123f, 0.456f), Offset(0.789f, 0.321f)),
            color = Color.Black,
            strokeWidth = 0.045f,
            tool = MarkupTool.REDACT,
            redactMode = RedactMode.MOSAIC
        )

        val serialized = MarkupModelsHelper.serializeStroke(original)
        val restored = MarkupModelsHelper.deserializeStroke(serialized)

        assertNotNull(restored)
        assertEquals(original.id, restored!!.id)
        assertEquals(original.points.size, restored.points.size)
        assertEquals(original.points[0].x, restored.points[0].x, 1e-4f)
        assertEquals(original.points[0].y, restored.points[0].y, 1e-4f)
        assertEquals(original.points[1].x, restored.points[1].x, 1e-4f)
        assertEquals(original.points[1].y, restored.points[1].y, 1e-4f)
        assertEquals(original.strokeWidth, restored.strokeWidth, 1e-4f)
        assertEquals(original.tool, restored.tool)
        assertEquals(original.redactMode, restored.redactMode)
    }

    @Test
    fun testCalculateMosaicTilesGeneration() {
        val points = listOf(Offset(0.2f, 0.5f), Offset(0.4f, 0.5f))
        val tiles = MarkupRasterizer.calculateMosaicTiles(
            points = points,
            width = 1000,
            height = 1000,
            strokePx = 40f,
            tileSizePx = 20f
        )

        assertTrue("Tiles list should not be empty for non-empty stroke", tiles.isNotEmpty())

        for (tile in tiles) {
            assertTrue("Tile left must be within bounds", tile.left >= 0)
            assertTrue("Tile top must be within bounds", tile.top >= 0)
            assertTrue("Tile right must be <= width", tile.right <= 1000)
            assertTrue("Tile bottom must be <= height", tile.bottom <= 1000)
            assertTrue("Tile width must be positive", tile.right > tile.left)
            assertTrue("Tile height must be positive", tile.bottom > tile.top)
            // Center of tile should be near path (y = 500)
            val centerY = (tile.top + tile.bottom) / 2f
            assertTrue("Tile should be close to path y=500", kotlin.math.abs(centerY - 500f) <= 40f)
        }
    }

    @Test
    fun testRasterizeRedactDoesNotCrash() {
        val androidCanvas = android.graphics.Canvas()
        val state = MarkupState(
            strokes = listOf(
                MarkupStroke(
                    id = 1L,
                    points = listOf(Offset(0.1f, 0.1f), Offset(0.3f, 0.3f)),
                    color = Color.Black,
                    strokeWidth = 0.04f,
                    tool = MarkupTool.REDACT,
                    redactMode = RedactMode.BLACKOUT
                ),
                MarkupStroke(
                    id = 2L,
                    points = listOf(Offset(0.4f, 0.4f), Offset(0.6f, 0.6f)),
                    color = Color.Black,
                    strokeWidth = 0.04f,
                    tool = MarkupTool.REDACT,
                    redactMode = RedactMode.MOSAIC
                )
            )
        )

        MarkupRasterizer.rasterize(
            canvas = androidCanvas,
            bitmapWidth = 800,
            bitmapHeight = 600,
            markup = state
        )
    }

    @Test
    fun testMarkupStateWithRedactStrokes() {
        var state = MarkupState()
        assertEquals(0, state.strokes.size)

        val stroke = MarkupStroke(
            id = 100L,
            points = listOf(Offset(0.1f, 0.1f), Offset(0.2f, 0.2f)),
            color = Color.Black,
            strokeWidth = 0.045f,
            tool = MarkupTool.REDACT,
            redactMode = RedactMode.MOSAIC
        )
        state = state.withAddedStroke(stroke)
        assertEquals(1, state.strokes.size)
        assertTrue(state.strokes.any { it.tool == MarkupTool.REDACT })

        state = state.withRemovedStroke(100L)
        assertEquals(0, state.strokes.size)
    }

    @Test
    fun testRedactModeToggle() {
        val stroke = MarkupStroke(
            id = 200L,
            points = listOf(Offset(0.3f, 0.3f)),
            color = Color.Black,
            strokeWidth = 0.045f,
            tool = MarkupTool.REDACT,
            redactMode = RedactMode.MOSAIC
        )
        assertEquals(RedactMode.MOSAIC, stroke.redactMode)

        val toggled = stroke.copy(redactMode = RedactMode.BLACKOUT)
        assertEquals(RedactMode.BLACKOUT, toggled.redactMode)
    }
}
