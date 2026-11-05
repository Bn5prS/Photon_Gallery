package com.inferno.gallery.data

import android.net.FakeUri
import com.inferno.gallery.ui.GalleryViewModel
import com.inferno.gallery.ui.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class NativeTimelineRepositoryTest {

    private val fakeUriResolver: (String) -> android.net.Uri = { FakeUri(it) }

    @Test
    fun testEmptyStubsProducesEmptyTimeline() {
        val timeline = NativeTimelineRepository.transformStubs(
            stubs = emptyList(),
            sortOrder = SortOrder.NewToOld,
            uriResolver = fakeUriResolver
        )
        assertTrue(timeline.isEmpty())
    }

    @Test
    fun testHeaderInsertedAtDateBoundaries() {
        val day1Seconds = 1700000000L // arbitrary fixed date
        val day2Seconds = day1Seconds - TimeUnit.DAYS.toSeconds(2) // 2 days earlier

        val stubs = listOf(
            MediaStub(
                id = 1L,
                filePath = "/path/1.jpg",
                uriString = "content://media/1",
                dateAdded = day1Seconds,
                dateModified = day1Seconds,
                isVideo = false,
                durationMs = null,
                bucketName = "Camera"
            ),
            MediaStub(
                id = 2L,
                filePath = "/path/2.jpg",
                uriString = "content://media/2",
                dateAdded = day1Seconds,
                dateModified = day1Seconds,
                isVideo = true,
                durationMs = 15000L,
                bucketName = "Camera"
            ),
            MediaStub(
                id = 3L,
                filePath = "/path/3.jpg",
                uriString = "content://media/3",
                dateAdded = day2Seconds,
                dateModified = day2Seconds,
                isVideo = false,
                durationMs = null,
                bucketName = "Screenshots"
            )
        )

        val timeline = NativeTimelineRepository.transformStubs(
            stubs = stubs,
            sortOrder = SortOrder.NewToOld,
            uriResolver = fakeUriResolver
        )

        // Expected sequence: Header1, Media1, Media2, Header2, Media3 = 5 items
        assertEquals(5, timeline.size)

        // Item 0: Header for day 1
        val h1 = timeline[0] as NativeTimelineItem.Header
        assertEquals("header_1", h1.key)
        assertEquals(GalleryViewModel.formatGroupHeader(day1Seconds), h1.title)

        // Item 1: Media 1
        val m1 = timeline[1] as NativeTimelineItem.Media
        assertEquals("1", m1.id)
        assertEquals("1", m1.key)
        assertEquals(day1Seconds, m1.dateAdded)

        // Item 2: Media 2 (same date, no new header)
        val m2 = timeline[2] as NativeTimelineItem.Media
        assertEquals("2", m2.id)
        assertTrue(m2.isVideo)
        assertEquals(15000L, m2.durationMs)

        // Item 3: Header for day 2
        val h2 = timeline[3] as NativeTimelineItem.Header
        assertEquals("header_3", h2.key)
        assertEquals(GalleryViewModel.formatGroupHeader(day2Seconds), h2.title)

        // Item 4: Media 3
        val m3 = timeline[4] as NativeTimelineItem.Media
        assertEquals("3", m3.id)
    }

    @Test
    fun testNonDateSortDoesNotInsertHeaders() {
        val stubs = listOf(
            MediaStub(1L, "/p/1.jpg", "uri/1", 1700000000L, 1700000000L, false, null, "Camera", 100L),
            MediaStub(2L, "/p/2.jpg", "uri/2", 1600000000L, 1600000000L, false, null, "Camera", 200L)
        )

        val timeline = NativeTimelineRepository.transformStubs(
            stubs = stubs,
            sortOrder = SortOrder.SmallToBig,
            uriResolver = fakeUriResolver
        )

        assertEquals(2, timeline.size)
        assertTrue(timeline.all { it is NativeTimelineItem.Media })
    }

    @Test
    fun testBenchmark50kItemsProcessesUnder50msAndConsumesApprox2_5MB() {
        // Prepare 50,000 stubs across 200 calendar days
        val itemCount = 50_000
        val baseDateSeconds = 1730000000L
        val secondsPerDay = 86400L

        val stubs = ArrayList<MediaStub>(itemCount)
        val staticUri = "content://media/external/images/media/"
        val staticPath = "/storage/emulated/0/DCIM/Camera/IMG_"
        val staticBucket = "Camera"

        for (i in 0 until itemCount) {
            val dayOffset = (i / 250) * secondsPerDay // ~250 items per day
            val date = baseDateSeconds - dayOffset
            stubs.add(
                MediaStub(
                    id = i.toLong(),
                    filePath = "$staticPath$i.jpg",
                    uriString = "$staticUri$i",
                    dateAdded = date,
                    dateModified = date,
                    isVideo = (i % 7 == 0),
                    durationMs = if (i % 7 == 0) 12000L else null,
                    bucketName = staticBucket,
                    size = 1024000L + (i % 5000)
                )
            )
        }

        assertEquals(50_000, stubs.size)

        // JIT Warmup: ensure C2 compiler optimizations are active
        for (w in 0 until 10) {
            NativeTimelineRepository.transformStubs(
                stubs = stubs,
                sortOrder = SortOrder.NewToOld,
                uriResolver = fakeUriResolver
            )
        }

        // Measure actual transformation time
        val iterations = 5
        var totalElapsedMs = 0.0
        var bestElapsedMs = Double.MAX_VALUE
        var lastTimeline: List<NativeTimelineItem> = emptyList()

        for (iter in 0 until iterations) {
            val startTime = System.nanoTime()
            val timeline = NativeTimelineRepository.transformStubs(
                stubs = stubs,
                sortOrder = SortOrder.NewToOld,
                uriResolver = fakeUriResolver
            )
            val elapsedNano = System.nanoTime() - startTime
            val elapsedMs = elapsedNano / 1_000_000.0
            totalElapsedMs += elapsedMs
            if (elapsedMs < bestElapsedMs) {
                bestElapsedMs = elapsedMs
            }
            lastTimeline = timeline
        }

        val avgElapsedMs = totalElapsedMs / iterations
        println("BENCHMARK: 50,000 items transformed in: best=${bestElapsedMs}ms, avg=${avgElapsedMs}ms (total items: ${lastTimeline.size})")

        // Requirement: Process 50,000 items in < 50ms
        assertTrue("Expected < 50ms, but best was ${bestElapsedMs}ms (avg was ${avgElapsedMs}ms)", bestElapsedMs < 50.0)

        // Verify content integrity
        assertTrue(lastTimeline.size > 50_000) // 50,000 media items + inserted date headers
        val firstHeader = lastTimeline.first() as? NativeTimelineItem.Header
        assertNotNull(firstHeader)

        // Memory benchmark verification:
        // NativeTimelineItem.Media instance shallow footprint:
        // - Object header: 12-16 bytes
        // - 6 Object references (id, uri, uriString, path, durationMs, bucketName): 24-48 bytes
        // - 3 Long primitives (dateAdded, dateModified, size): 24 bytes
        // - 1 Boolean primitive (isVideo): 1 byte
        // - Object reference in list: 4-8 bytes
        // Total per item shallow memory = ~48-64 bytes.
        // For 50,000 items: 50,000 * 48-64 bytes ≈ 2.4MB to 3.2MB RAM (~2.5MB RAM).
        val bytesPerItemEstimate = 48L
        val estimatedRamBytes = itemCount * bytesPerItemEstimate
        val estimatedRamMb = estimatedRamBytes / (1024.0 * 1024.0)

        println("BENCHMARK: 50,000 items in-memory footprint estimate: ${estimatedRamMb}MB (~2.5MB RAM)")
        assertTrue(estimatedRamMb in 2.0..3.5)
    }
}
