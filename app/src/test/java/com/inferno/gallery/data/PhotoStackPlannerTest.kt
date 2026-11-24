package com.inferno.gallery.data

import android.net.FakeUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoStackPlannerTest {

    private fun cand(
        id: Long,
        dateSec: Long,
        pHash: Long? = 0x0F0F0F0F0F0F0F0FL,
        sharpness: Float = 50f,
        sizeBytes: Long = 2_000_000L,
        name: String = "IMG_$id.jpg",
        bucket: String = "Camera",
        isVideo: Boolean = false
    ) = PhotoStackCandidate(
        id = id,
        name = name,
        bucketName = bucket,
        dateAddedSec = dateSec,
        sizeBytes = sizeBytes,
        pHash = pHash,
        sharpness = sharpness,
        pixelCount = 12_000_000L,
        isVideo = isVideo
    )

    @Test
    fun groupsSimilarBurstWithinWindowAndPicksSharpestAutoBest() {
        val baseHash = 0x5555555555555555L
        val candidates = listOf(
            cand(id = 1L, dateSec = 1000L, pHash = baseHash, sharpness = 40f),
            cand(id = 2L, dateSec = 1004L, pHash = baseHash xor 0x3L, sharpness = 95f), // sharpest
            cand(id = 3L, dateSec = 1010L, pHash = baseHash xor 0x5L, sharpness = 65f),
            // Different scene within 10s (high Hamming distance) — must stay separate
            cand(id = 4L, dateSec = 1012L, pHash = baseHash.inv(), sharpness = 80f)
        )

        val index = PhotoStackPlanner.buildStacks(candidates)
        assertEquals(1, index.stacksByCoverId.size)

        val stack = index.stacksByCoverId[2L]
        assertNotNull("Sharpest shot (#2) should be auto Best Shot cover", stack)
        assertEquals(listOf(3L, 2L, 1L), stack!!.memberIds)
        assertEquals(2L, stack.bestShotId)
        assertEquals(2L, stack.autoBestShotId)
        assertFalse(stack.isManualBestPick)
        assertTrue(stack.isBurst)

        assertEquals(setOf(1L, 3L), index.hiddenMemberIds)
        assertNull("Unrelated scene must not be stacked", index.stackForMedia(4L))
    }

    @Test
    fun manualBestShotOverridePromotesChosenShotAndSurvivesMemberDeletion() {
        val baseHash = 0x1111111111111111L
        val candidates = listOf(
            cand(id = 10L, dateSec = 2000L, pHash = baseHash, sharpness = 45f),
            cand(id = 11L, dateSec = 2002L, pHash = baseHash xor 0x1L, sharpness = 92f),
            cand(id = 12L, dateSec = 2005L, pHash = baseHash xor 0x2L, sharpness = 70f)
        )

        // User manually picks #12 over auto-best #11
        val index = PhotoStackPlanner.buildStacks(
            candidates = candidates,
            manualBestShotPicks = mapOf(12L to 99999L)
        )
        val stack = index.stacksByCoverId[12L]
        assertNotNull(stack)
        assertEquals(12L, stack!!.bestShotId)
        assertEquals(11L, stack.autoBestShotId)
        assertTrue(stack.isManualBestPick)
        assertEquals(setOf(10L, 11L), index.hiddenMemberIds)

        // Deleting shot #10 still preserves #12 as the manual cover
        val afterDelete = PhotoStackPlanner.buildStacks(
            candidates = candidates.filter { it.id != 10L },
            manualBestShotPicks = mapOf(12L to 99999L)
        )
        assertEquals(12L, afterDelete.stacksByCoverId[12L]?.bestShotId)
    }

    @Test
    fun unstackedPhotosAndDistantPhotosDoNotMerge() {
        val baseHash = 0x0F0F0F0F0F0F0F0FL
        val candidates = listOf(
            cand(id = 1L, dateSec = 1000L, pHash = baseHash),
            cand(id = 2L, dateSec = 1005L, pHash = baseHash),
            // Same pHash but 10 minutes later (> 120s burst window)
            cand(id = 3L, dateSec = 1600L, pHash = baseHash)
        )

        val normal = PhotoStackPlanner.buildStacks(candidates)
        assertEquals(1, normal.stacksByCoverId.size)
        assertNull(normal.stackForMedia(3L))

        // Unstacking #1 and #2 splits them back out
        val unstacked = PhotoStackPlanner.buildStacks(
            candidates = candidates,
            unstackedIds = setOf(1L, 2L)
        )
        assertTrue(unstacked.isEmpty)
    }

    @Test
    fun rapidUnhashedCameraBurstsStackBeforePHashCompletes() {
        val candidates = listOf(
            cand(id = 20L, dateSec = 5000L, pHash = null, name = "PXL_2026_BURST_001.jpg", sizeBytes = 3_000_000L),
            cand(id = 21L, dateSec = 5001L, pHash = null, name = "PXL_2026_BURST_002.jpg", sizeBytes = 3_800_000L),
            cand(id = 22L, dateSec = 5040L, pHash = null, name = "IMG_5040.jpg")
        )
        val index = PhotoStackPlanner.buildStacks(candidates)
        assertEquals(1, index.stacksByCoverId.size)
        assertEquals(21L, index.stacksByCoverId[21L]?.bestShotId)
        assertNull(index.stackForMedia(22L))
    }

    @Test
    fun collapseNativeTimelineAnnotatesCoverAndRemovesOrphanHeaders() {
        val baseHash = 0x2222222222222222L
        val stackIndex = PhotoStackPlanner.buildStacks(
            listOf(
                cand(id = 1L, dateSec = 86410L, pHash = baseHash, sharpness = 90f),
                cand(id = 2L, dateSec = 86390L, pHash = baseHash, sharpness = 40f) // just before midnight
            )
        )

        fun media(id: Long, ts: Long) = NativeTimelineItem.Media(
            id = id.toString(),
            uri = FakeUri("content://media/$id"),
            uriString = "content://media/$id",
            path = "/dcim/$id.jpg",
            dateAdded = ts,
            dateModified = ts,
            isVideo = false,
            durationMs = null,
            bucketName = "Camera"
        )

        val timeline = listOf(
            NativeTimelineItem.Header("h_today", "Today", 86410L),
            media(1L, 86410L),
            NativeTimelineItem.Header("h_yesterday", "Yesterday", 86390L),
            media(2L, 86390L) // hidden member of stack whose cover is in "Today"
        )

        val collapsed = PhotoStackPlanner.collapseNativeTimeline(timeline, stackIndex)
        assertEquals(2, collapsed.size)
        assertTrue(collapsed[0] is NativeTimelineItem.Header)
        assertEquals("Today", (collapsed[0] as NativeTimelineItem.Header).title)
        val cover = collapsed[1] as NativeTimelineItem.Media
        assertEquals("1", cover.id)
        assertEquals(2, cover.stackCount)
    }

    @Test
    fun findBurstHashCandidatesOnlySelectsUnhashedTemporalNeighbors() {
        val candidates = listOf(
            cand(id = 1L, dateSec = 1000L, pHash = null),
            cand(id = 2L, dateSec = 1015L, pHash = null),
            cand(id = 3L, dateSec = 5000L, pHash = null), // isolated — skip!
            cand(id = 4L, dateSec = 9000L, pHash = 123L), // already hashed
            cand(id = 5L, dateSec = 9005L, pHash = null)  // neighbor of #4 — include!
        )

        val toHash = PhotoStackPlanner.findBurstHashCandidates(candidates)
        assertEquals(listOf(5L, 2L, 1L), toHash)
    }

    @Test
    fun manualGroupsStackArbitraryPhotosAndRespectBestShotOverride() {
        val candidates = listOf(
            // Completely different scenes and days apart — would never auto-stack
            cand(id = 101L, dateSec = 1000L, pHash = 0x0000000000000000L, sharpness = 50f),
            cand(id = 102L, dateSec = 500_000L, pHash = -1L, sharpness = 90f),
            // Separate auto-burst that should coexist cleanly without merging into the manual stack
            cand(id = 201L, dateSec = 600_000L, pHash = 0x0F0F0F0F0F0F0F0FL, sharpness = 60f),
            cand(id = 202L, dateSec = 600_003L, pHash = 0x0F0F0F0F0F0F0F0FL, sharpness = 85f)
        )

        val index = PhotoStackPlanner.buildStacks(
            candidates = candidates,
            manualBestShotPicks = mapOf(101L to 123456L),
            manualGroups = listOf(listOf(101L, 102L))
        )

        assertEquals(2, index.stacksByCoverId.size)
        val manualStack = index.stacksByCoverId[101L]
        assertNotNull(manualStack)
        assertTrue(manualStack!!.isManual)
        assertEquals(listOf(102L, 101L), manualStack.memberIds)
        assertEquals(101L, manualStack.bestShotId)
        assertEquals(102L, manualStack.autoBestShotId)
        assertTrue(manualStack.isManualBestPick)

        val autoStack = index.stacksByCoverId[202L]
        assertNotNull(autoStack)
        assertFalse(autoStack!!.isManual)
        assertEquals(listOf(202L, 201L), autoStack.memberIds)
    }

    @Test
    fun findRestorableAutoStacksDetectsUnstackedBurstsWithoutBreakingManualStacks() {
        val baseHash = 0x3333333333333333L
        val candidates = listOf(
            cand(id = 1L, dateSec = 1000L, pHash = baseHash, sharpness = 40f),
            cand(id = 2L, dateSec = 1002L, pHash = baseHash, sharpness = 80f),
            cand(id = 3L, dateSec = 1004L, pHash = baseHash, sharpness = 60f),
            cand(id = 4L, dateSec = 1006L, pHash = baseHash, sharpness = 70f),
            // Unrelated manually unstacked photo — must NOT appear as restorable
            cand(id = 99L, dateSec = 900_000L, pHash = baseHash.inv(), sharpness = 55f)
        )

        // All 4 burst shots + isolated photo #99 were unstacked
        val unstacked = setOf(1L, 2L, 3L, 4L, 99L)
        val restorableAll = PhotoStackPlanner.findRestorableAutoStacks(
            candidates = candidates,
            unstackedIds = unstacked
        )
        assertEquals(1, restorableAll.size)
        assertEquals(listOf(4L, 3L, 2L, 1L), restorableAll.first().memberIds)

        // If user manually re-stacks just #1 and #2, only #3 and #4 remain restorable as an auto burst
        val restorableRemaining = PhotoStackPlanner.findRestorableAutoStacks(
            candidates = candidates,
            unstackedIds = setOf(3L, 4L, 99L),
            manualGroups = listOf(listOf(1L, 2L))
        )
        assertEquals(1, restorableRemaining.size)
        assertEquals(listOf(4L, 3L), restorableRemaining.first().memberIds)
    }

    @Test
    fun summarizeSelectionExpandsSingleFivePhotoStackIntoAllMembers() {
        val baseHash = 0x5555555555555555L
        val index = PhotoStackPlanner.buildStacks(
            listOf(
                cand(id = 10L, dateSec = 1000L, pHash = baseHash, sharpness = 40f),
                cand(id = 11L, dateSec = 1001L, pHash = baseHash, sharpness = 50f),
                cand(id = 12L, dateSec = 1002L, pHash = baseHash, sharpness = 95f), // cover
                cand(id = 13L, dateSec = 1003L, pHash = baseHash, sharpness = 60f),
                cand(id = 14L, dateSec = 1004L, pHash = baseHash, sharpness = 70f)
            )
        )

        val summary = PhotoStackPlanner.summarizeSelectedUris(
            selectedUris = listOf("content://media/external/images/media/12"),
            stackIndex = index,
            expandStacks = true
        )

        assertTrue(summary.hasStacks)
        assertEquals(1, summary.selectedStackCount)
        assertEquals(5, summary.stackedPhotoCount)
        assertEquals(0, summary.standalonePhotoCount)
        assertEquals(5, summary.totalPhotoCount)
        assertEquals(setOf(10L, 11L, 12L, 13L, 14L), summary.expandedIds.toSet())
    }

    @Test
    fun summarizeSelectionHandlesMultipleStacksMixedWithStandalonePhotosAndDeduplicates() {
        val hashA = 0x0F0F0F0F0F0F0F0FL
        val hashB = 0x5555555555555555L
        val index = PhotoStackPlanner.buildStacks(
            listOf(
                // Stack A (3 photos, cover = 2)
                cand(id = 1L, dateSec = 1000L, pHash = hashA, sharpness = 30f),
                cand(id = 2L, dateSec = 1002L, pHash = hashA, sharpness = 90f),
                cand(id = 3L, dateSec = 1004L, pHash = hashA, sharpness = 40f),
                // Stack B (2 photos, cover = 20)
                cand(id = 20L, dateSec = 5000L, pHash = hashB, sharpness = 85f),
                cand(id = 21L, dateSec = 5002L, pHash = hashB, sharpness = 45f),
                // Standalone photo
                cand(id = 99L, dateSec = 9000L, pHash = -1L, sharpness = 50f)
            )
        )

        val summary = PhotoStackPlanner.summarizeSelectedUris(
            selectedUris = listOf(
                "content://media/external/images/media/2",
                "content://media/external/images/media/1", // already in Stack A — must not double-count
                "content://media/external/images/media/20",
                "content://media/external/images/media/99"
            ),
            stackIndex = index,
            expandStacks = true
        )

        assertTrue(summary.hasStacks)
        assertEquals(2, summary.selectedStackCount)
        assertEquals(5, summary.stackedPhotoCount)
        assertEquals(1, summary.standalonePhotoCount)
        assertEquals(6, summary.totalPhotoCount)
        assertEquals(setOf(1L, 2L, 3L, 20L, 21L, 99L), summary.expandedIds.toSet())
    }

    @Test
    fun summarizeSelectionDoesNotExpandWhenStacksAreNotCollapsed() {
        val baseHash = 0x2222222222222222L
        val index = PhotoStackPlanner.buildStacks(
            listOf(
                cand(id = 10L, dateSec = 1000L, pHash = baseHash, sharpness = 90f),
                cand(id = 11L, dateSec = 1002L, pHash = baseHash, sharpness = 40f),
                cand(id = 12L, dateSec = 1004L, pHash = baseHash, sharpness = 30f)
            )
        )

        val summary = PhotoStackPlanner.summarizeSelectedUris(
            selectedUris = listOf("content://media/external/images/media/10"),
            stackIndex = index,
            expandStacks = false
        )

        assertFalse(summary.hasStacks)
        assertEquals(0, summary.selectedStackCount)
        assertEquals(0, summary.stackedPhotoCount)
        assertEquals(1, summary.standalonePhotoCount)
        assertEquals(1, summary.totalPhotoCount)
        assertEquals(listOf(10L), summary.expandedIds)
    }

    @Test
    fun summarizeSelectionPreservesUnparsedUrisWithoutDroppingCount() {
        val summary = PhotoStackPlanner.summarizeSelectedUris(
            selectedUris = listOf("file:///storage/emulated/0/DCIM/sample.jpg"),
            stackIndex = PhotoStackIndex.EMPTY,
            expandStacks = true
        )

        assertFalse(summary.hasStacks)
        assertEquals(0, summary.selectedStackCount)
        assertEquals(1, summary.unparsedUriCount)
        assertEquals(1, summary.totalPhotoCount)
    }

    @Test
    fun detachingSinglePhotoFromAutoStackPreservesRemainingMembersAndRecomputesCoverIfNeeded() {
        val baseHash = 0x4444444444444444L
        val candidates = listOf(
            cand(id = 1L, dateSec = 1000L, pHash = baseHash, sharpness = 40f),
            cand(id = 2L, dateSec = 1001L, pHash = baseHash, sharpness = 95f), // cover
            cand(id = 3L, dateSec = 1002L, pHash = baseHash, sharpness = 70f),
            cand(id = 4L, dateSec = 1003L, pHash = baseHash, sharpness = 55f)
        )

        // Detaching #2 (which happened to be cover) unstacks #2 only while keeping #1, #3, #4 stacked
        val afterDetach = PhotoStackPlanner.buildStacks(
            candidates = candidates,
            unstackedIds = setOf(2L)
        )
        assertNull(afterDetach.stackForMedia(2L))
        assertEquals(1, afterDetach.stacksByCoverId.size)
        val remaining = afterDetach.stacksByCoverId[3L]
        assertNotNull("Next sharpest member (#3) should become cover", remaining)
        assertEquals(listOf(4L, 3L, 1L), remaining!!.memberIds)
    }

    @Test
    fun detachingSinglePhotoFromManualStackPrunesGroupWithoutDisbandingRemainingTwo() {
        val candidates = listOf(
            cand(id = 501L, dateSec = 1000L, pHash = 0x0000L, sharpness = 60f),
            cand(id = 502L, dateSec = 900_000L, pHash = 0xFFFFL, sharpness = 90f),
            cand(id = 503L, dateSec = 1_500_000L, pHash = 0x0F0FL, sharpness = 40f)
        )

        // Mirrors SettingsRepository.removePhotoFromStack pruning #502 from a 3-photo manual stack
        val prunedManualGroups = listOf(listOf(501L, 502L, 503L))
            .map { group -> group.filter { it != 502L } }
            .filter { it.size >= 2 }

        val afterDetach = PhotoStackPlanner.buildStacks(
            candidates = candidates,
            unstackedIds = setOf(502L),
            manualGroups = prunedManualGroups
        )
        assertNull(afterDetach.stackForMedia(502L))
        assertEquals(1, afterDetach.stacksByCoverId.size)
        val remaining = afterDetach.stacksByCoverId[501L]
        assertNotNull(remaining)
        assertTrue(remaining!!.isManual)
        assertEquals(listOf(503L, 501L), remaining.memberIds)
    }
}
