package com.inferno.gallery.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaDateUtilsTest {

    @Test
    fun `prefers capture date over file modification date`() {
        val captureDate = 1_600_000_000L
        val copyDate = 1_700_000_000L

        assertEquals(captureDate, resolveDisplayDateSeconds(captureDate, copyDate))
    }

    @Test
    fun `falls back to file modification date when there is no capture date`() {
        val copyDate = 1_700_000_000L

        assertEquals(copyDate, resolveDisplayDateSeconds(0L, copyDate))
    }

    @Test
    fun `returns zero when the item has no dates at all`() {
        assertEquals(0L, resolveDisplayDateSeconds(0L, 0L))
    }
}
