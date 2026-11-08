package com.inferno.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class FilenameDateParserTest {

    @Test
    fun parseStandardCameraTimestamps() {
        val parsed = FilenameDateParser.parseDateFromFilename("IMG_20231108_143025.jpg")
        assertNotNull(parsed)

        val cal = Calendar.getInstance().apply { timeInMillis = parsed!! * 1000L }
        assertEquals(2023, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(8, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertEquals(25, cal.get(Calendar.SECOND))
    }

    @Test
    fun parsePixelAndSgCamTimestampsWithMillis() {
        val parsed = FilenameDateParser.parseDateFromFilename("SGCAM_20261107_171536731.jpg")
        assertNotNull(parsed)

        val cal = Calendar.getInstance().apply { timeInMillis = parsed!! * 1000L }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(17, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, cal.get(Calendar.MINUTE))
        assertEquals(36, cal.get(Calendar.SECOND))
    }

    @Test
    fun parseDashedScreenshotTimestamps() {
        val parsed = FilenameDateParser.parseDateFromFilename("Screenshot_2023-11-08-14-30-25.png")
        assertNotNull(parsed)

        val cal = Calendar.getInstance().apply { timeInMillis = parsed!! * 1000L }
        assertEquals(2023, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(8, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertEquals(25, cal.get(Calendar.SECOND))
    }

    @Test
    fun parseUnderscoreScreenshotTimestamps() {
        val parsed = FilenameDateParser.parseDateFromFilename("Screenshot_2026_1109_001835.png")
        assertNotNull(parsed)

        val cal = Calendar.getInstance().apply { timeInMillis = parsed!! * 1000L }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(9, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(18, cal.get(Calendar.MINUTE))
        assertEquals(35, cal.get(Calendar.SECOND))
    }

    @Test
    fun parseWhatsAppDateOnly() {
        val parsed = FilenameDateParser.parseDateFromFilename("IMG-20231108-WA0001.jpg")
        assertNotNull(parsed)

        val cal = Calendar.getInstance().apply { timeInMillis = parsed!! * 1000L }
        assertEquals(2023, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(8, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun returnsNullForNonDateFilenames() {
        assertNull(FilenameDateParser.parseDateFromFilename("random_image.jpg"))
        assertNull(FilenameDateParser.parseDateFromFilename("wallpaper.png"))
        assertNull(FilenameDateParser.parseDateFromFilename("12345.jpg"))
        assertNull(FilenameDateParser.parseDateFromFilename(""))
    }
}

