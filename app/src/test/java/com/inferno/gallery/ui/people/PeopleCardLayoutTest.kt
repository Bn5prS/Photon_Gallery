package com.inferno.gallery.ui.people

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleCardLayoutTest {

    @Test
    fun computePersonGridSpacingDp_reducesGapsAtDenseColumnCounts() {
        val (pad3, gap3) = computePersonGridSpacingDp(gridCols = 3)
        val (pad4, gap4) = computePersonGridSpacingDp(gridCols = 4)
        val (pad5, gap5) = computePersonGridSpacingDp(gridCols = 5)

        assertEquals(16f to 12f, pad3 to gap3)
        assertTrue("4-col padding/gap must be tighter than 3-col to fit multi-digit photo counts", pad4 < pad3 && gap4 < gap3)
        assertTrue("5-col padding/gap must leave >= 64dp per column on a 360dp phone", (360f - 2f * pad5 - 4f * gap5) / 5f >= 64f)
    }

    @Test
    fun nextFittedPhotoCountFontSp_shrinksUntilFitsAboveMinimumFloor() {
        val base4Col = initialPhotoCountFontSp(gridCols = 4)
        assertEquals(10.5f, base4Col, 0.01f)

        // When multi-digit text ("148 photos") overflows width, step down by 0.5sp
        val shrunkOnce = nextFittedPhotoCountFontSp(currentSp = base4Col, didOverflowWidth = true)
        assertEquals(10.0f, shrunkOnce, 0.01f)

        // Never shrink below 8.0sp floor
        val atFloor = nextFittedPhotoCountFontSp(currentSp = 8.0f, didOverflowWidth = true)
        assertEquals(8.0f, atFloor, 0.01f)

        // No change when text fits cleanly
        assertEquals(10.5f, nextFittedPhotoCountFontSp(currentSp = 10.5f, didOverflowWidth = false), 0.01f)
    }
}
