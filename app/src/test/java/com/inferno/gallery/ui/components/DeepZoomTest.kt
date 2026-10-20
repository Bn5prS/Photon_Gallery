package com.inferno.gallery.ui.components

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

class DeepZoomTest {

    @Test
    fun testSampleSizeSelectionForVariousScales() {
        fun computeSampleSize(effectiveScale: Float): Int {
            return when {
                effectiveScale >= 0.70f -> 1
                effectiveScale >= 0.35f -> 2
                effectiveScale >= 0.18f -> 4
                else -> 8
            }
        }

        // Deep zoom (1:1 sensor inspection or higher)
        assertEquals(1, computeSampleSize(1.0f))
        assertEquals(1, computeSampleSize(0.85f))
        assertEquals(1, computeSampleSize(0.70f))

        // Intermediate zoom
        assertEquals(2, computeSampleSize(0.50f))
        assertEquals(2, computeSampleSize(0.35f))

        // Lower zoom overview
        assertEquals(4, computeSampleSize(0.25f))
        assertEquals(4, computeSampleSize(0.18f))

        // Far overview
        assertEquals(8, computeSampleSize(0.10f))
    }

    @Test
    fun testTileGridBoundsCalculation() {
        val imgW = 8160 // e.g. 50MP photo
        val imgH = 6120
        val rawTileSize = 1024

        val totalCols = (imgW + rawTileSize - 1) / rawTileSize
        val totalRows = (imgH + rawTileSize - 1) / rawTileSize

        assertEquals(8, totalCols)
        assertEquals(6, totalRows)

        // Test boundary clamping on final tile in row/col
        val lastCol = 7
        val lastRow = 5

        val lastTileLeft = lastCol * rawTileSize
        val lastTileTop = lastRow * rawTileSize
        val lastTileRight = min(lastTileLeft + rawTileSize, imgW)
        val lastTileBottom = min(lastTileTop + rawTileSize, imgH)

        assertEquals(7168, lastTileLeft)
        assertEquals(8160, lastTileRight)
        assertEquals(5120, lastTileTop)
        assertEquals(6120, lastTileBottom)
    }

    @Test
    fun testTileIntersectionWithViewport() {
        val imgW = 8000
        val imgH = 6000
        val rawTileSize = 1024

        // Viewport focused on center of image
        val visibleLeft = 3500
        val visibleTop = 2500
        val visibleRight = 4500
        val visibleBottom = 3500

        val startCol = visibleLeft / rawTileSize // 3
        val endCol = visibleRight / rawTileSize   // 4
        val startRow = visibleTop / rawTileSize  // 2
        val endRow = visibleBottom / rawTileSize // 3

        assertEquals(3, startCol)
        assertEquals(4, endCol)
        assertEquals(2, startRow)
        assertEquals(3, endRow)

        // Exactly 4 tiles intersect this viewport: (3,2), (3,3), (4,2), (4,3)
        val intersectingTiles = mutableListOf<Pair<Int, Int>>()
        for (col in startCol..endCol) {
            for (row in startRow..endRow) {
                intersectingTiles.add(col to row)
            }
        }

        assertEquals(4, intersectingTiles.size)
        assertTrue(intersectingTiles.contains(3 to 2))
        assertTrue(intersectingTiles.contains(3 to 3))
        assertTrue(intersectingTiles.contains(4 to 2))
        assertTrue(intersectingTiles.contains(4 to 3))

        // Non-intersecting tiles must not be included
        assertFalse(intersectingTiles.contains(0 to 0))
        assertFalse(intersectingTiles.contains(7 to 5))
    }
}
