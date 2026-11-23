package com.inferno.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveDateAddedTest {

    private var exifCalls = 0

    private fun resolve(
        taken: Long = 0, parsed: Long? = null, hasPath: Boolean = true,
        modified: Long = 300, added: Long = 400, cached: Long? = null, exif: Long? = null
    ): Long = LocalMediaRepository.resolveDateAdded(
        dateTakenSec = taken, parsedFilenameDate = parsed, hasPath = hasPath,
        rawDateModified = modified, rawDateAdded = added,
        cached = { cached }, exif = { exifCalls++; exif }
    )

    @Test
    fun sameOrderAsBefore_withoutCache() {
        assertEquals(100L, resolve(taken = 100, exif = 999))
        assertEquals(200L, resolve(parsed = 200, exif = 999))
        assertEquals(999L, resolve(exif = 999))
        assertEquals(300L, resolve(exif = null))
        assertEquals(400L, resolve(modified = 0, exif = null))
        // a non-positive filename date skips EXIF, like the original code
        assertEquals(300L, resolve(parsed = 0, exif = 999))
        assertEquals(300L, resolve(hasPath = false, exif = 999))
    }

    @Test
    fun knownDate_isUsed_andTheFileIsNotOpened() {
        assertEquals(777L, resolve(cached = 777, exif = 999))
        assertEquals(0, exifCalls)
    }

    @Test
    fun exifIsReadOnlyWhenNeeded() {
        resolve(taken = 100); resolve(parsed = 200); resolve(hasPath = false)
        assertEquals(0, exifCalls)
        resolve(cached = null, exif = 5)
        assertEquals(1, exifCalls)
    }

    @Test
    fun knownDate_neverOverridesDateTakenOrFilename() {
        assertEquals(100L, resolve(taken = 100, cached = 777))
        assertEquals(200L, resolve(parsed = 200, cached = 777))
    }
}
