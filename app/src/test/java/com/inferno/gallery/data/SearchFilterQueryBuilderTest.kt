package com.inferno.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class SearchFilterQueryBuilderTest {

    @Test
    fun filterState_tracksActiveCountAndClearsCleanly() {
        val empty = SearchFilterState()
        assertFalse(empty.isActive)
        assertEquals(0, empty.activeCount)

        val full = SearchFilterState(
            dateStartSec = 1000L,
            dateEndSec = 2000L,
            dateLabel = "Custom",
            personIds = setOf(1L, 2L),
            documentCategories = setOf(BucketNames.DOC_RECEIPTS),
            mediaType = SearchMediaTypeFilter.VIDEOS,
            albumNames = setOf("Camera"),
            locationName = "Tokyo"
        )
        assertTrue(full.isActive)
        assertEquals(6, full.activeCount)
        assertFalse(full.clearAll().isActive)
    }

    @Test
    fun datePresets_computeExpectedRanges() {
        // 2026-06-15 12:00:00 UTC
        val nowSec = 1_781_524_800L
        val utc = TimeZone.getTimeZone("UTC")

        val (start7, end7) = SearchDatePreset.LAST_7_DAYS.computeEpochSecRange(nowSec, utc)
        assertEquals(nowSec - 7L * 86_400L, start7)
        assertEquals(nowSec, end7)

        val (start30, end30) = SearchDatePreset.LAST_30_DAYS.computeEpochSecRange(nowSec, utc)
        assertEquals(nowSec - 30L * 86_400L, start30)
        assertEquals(nowSec, end30)

        val (startToday, endToday) = SearchDatePreset.TODAY.computeEpochSecRange(nowSec, utc)
        assertTrue(startToday <= nowSec)
        assertEquals(nowSec, endToday)
        assertTrue((endToday - startToday) < 86_400L)
    }

    @Test
    fun buildFilteredMediaQuery_combinesAllSixMetadataDimensionsOffline() {
        val filters = SearchFilterState(
            dateStartSec = 1_700_000_000L,
            dateEndSec = 1_710_000_000L,
            personIds = setOf(10L, 20L),
            documentCategories = setOf(BucketNames.DOC_RECEIPTS, BucketNames.DOC_PAYMENTS),
            mediaType = SearchMediaTypeFilter.PHOTOS,
            albumNames = setOf("Camera", "Screenshots"),
            locationName = "Paris"
        )

        val built = SearchFilterQueryBuilder.buildFilteredMediaQuery(
            filters = filters,
            excludedBuckets = setOf("PrivateTemp", "Screenshots"), // Screenshots explicitly selected -> must NOT be excluded!
            candidateIds = listOf(501L, 502L),
            limit = 250
        )

        assertTrue(built.sql.contains("cm.bucketName NOT IN (?)"))
        assertTrue(built.sql.contains("EXISTS (SELECT 1 FROM faces f WHERE f.mediaId = cm.id AND f.personId = ?)"))
        assertTrue(built.sql.contains("document_classifications WHERE category IN (?,?)"))
        assertTrue(built.sql.contains("cm.isVideo = 0"))
        assertTrue(built.sql.contains("cm.bucketName IN (?,?)"))
        assertTrue(built.sql.contains("geocoded_locations WHERE placeName = ?"))
        assertTrue(built.sql.contains("cm.id IN (?,?)"))
        assertTrue(built.sql.endsWith("ORDER BY cm.dateAdded DESC LIMIT ?"))

        // Verify bind argument ordering matches placeholders 1-for-1
        val expectedArgs = listOf<Any>(
            "PrivateTemp",
            1_700_000_000L,
            1_710_000_000L,
            10L,
            20L,
            BucketNames.DOC_PAYMENTS,
            BucketNames.DOC_RECEIPTS,
            "Camera",
            "Screenshots",
            "Paris",
            501L,
            502L,
            250
        )
        assertEquals(expectedArgs, built.args)
        assertEquals(built.args.size, built.sql.count { it == '?' })
    }

    @Test
    fun buildFilteredMediaQuery_handlesEmptyFavoritesSafely() {
        val built = SearchFilterQueryBuilder.buildFilteredMediaQuery(
            filters = SearchFilterState(mediaType = SearchMediaTypeFilter.FAVORITES),
            favoriteIds = emptySet()
        )
        assertTrue(built.sql.contains("1 = 0"))
        assertEquals(built.args.size, built.sql.count { it == '?' })
    }

    @Test
    fun utcPickerRangeToLocalEpochSec_preservesFullLocalDayAcrossTimezones() {
        // 2026-10-14 00:00:00 UTC
        val utcDayMs = 1_791_936_000_000L

        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val (istStart, istEnd) = SearchFilterQueryBuilder.utcPickerRangeToLocalEpochSec(utcDayMs, utcDayMs, ist)
        // Local 00:00:00 IST is 5h30m earlier than UTC midnight; end is 23:59:59 IST
        assertEquals(utcDayMs / 1000L - 19_800L, istStart)
        assertEquals(istStart + 86_399L, istEnd)

        val est = TimeZone.getTimeZone("GMT-05:00")
        val (estStart, estEnd) = SearchFilterQueryBuilder.utcPickerRangeToLocalEpochSec(utcDayMs, utcDayMs, est)
        assertEquals(utcDayMs / 1000L + 18_000L, estStart)
        assertEquals(estStart + 86_399L, estEnd)
    }

    @Test
    fun buildFilteredMediaQuery_narrowsAndPrioritizesMotionPhotos() {
        val built = SearchFilterQueryBuilder.buildFilteredMediaQuery(
            filters = SearchFilterState(mediaType = SearchMediaTypeFilter.MOTION_PHOTOS),
            limit = 1500
        )
        assertTrue(built.sql.contains("cm.isVideo = 0"))
        assertTrue(built.sql.contains("image/jpeg"))
        assertTrue(built.sql.contains("mvimg_%"))
        assertEquals(listOf<Any>(1500), built.args)
    }
}
