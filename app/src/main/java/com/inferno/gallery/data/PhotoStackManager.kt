package com.inferno.gallery.data

import androidx.compose.runtime.Immutable
import kotlin.math.abs

/**
 * A group of burst or very similar photos collapsed into a single cover thumbnail on the Timeline.
 * [bestShotId] is displayed as the cover thumbnail (user's manual pick if chosen, otherwise auto-best).
 */
@Immutable
data class PhotoStack(
    val stackId: String,
    val bestShotId: Long,
    val autoBestShotId: Long,
    val memberIds: List<Long>,
    val isBurst: Boolean,
    val isManual: Boolean = false
) {
    val count: Int get() = memberIds.size
    val isManualBestPick: Boolean get() = bestShotId != autoBestShotId
}

/**
 * O(1) lookup index describing all active Timeline photo stacks.
 */
@Immutable
data class PhotoStackIndex(
    val stacksByCoverId: Map<Long, PhotoStack> = emptyMap(),
    val stackByMemberId: Map<Long, PhotoStack> = emptyMap(),
    val hiddenMemberIds: Set<Long> = emptySet()
) {
    val isEmpty: Boolean get() = stacksByCoverId.isEmpty()

    fun stackForMedia(mediaId: Long): PhotoStack? = stackByMemberId[mediaId]

    fun stackCountForCover(mediaId: Long): Int = stacksByCoverId[mediaId]?.count ?: 1

    companion object {
        val EMPTY = PhotoStackIndex()
    }
}

/**
 * Lightweight projection of a photo used to build Timeline photo stacks completely offline.
 */
data class PhotoStackCandidate(
    val id: Long,
    val name: String,
    val bucketName: String,
    val dateAddedSec: Long,
    val sizeBytes: Long,
    val pHash: Long?,
    val sharpness: Float = 0f,
    val pixelCount: Long = 0L,
    val isVideo: Boolean = false
)

/**
 * Pure Kotlin offline planner that groups burst shots and rapid similar retakes (within ~2 minutes)
 * into clean Timeline Photo Stacks, picks the sharpest auto Best Shot, respects manual Best Shot
 * overrides and unstacked photo IDs, and collapses timeline item lists without leaving orphan headers.
 */
object PhotoStackPlanner {

    /** Photos taken within 2 minutes of each other are eligible for burst / quick-retake stacking. */
    const val BURST_WINDOW_SEC = 120L

    /** Maximum 64-bit dHash Hamming distance (~84%+ visual similarity) for photos within [BURST_WINDOW_SEC]. */
    const val BURST_MAX_HAMMING = 10

    /** Sub-2-second fallback window for rapid camera bursts before pHash has been computed. */
    const val RAPID_UNHASHED_WINDOW_SEC = 2L

    /** Wider fallback window when both filenames explicitly carry camera burst markers. */
    const val NAMED_BURST_UNHASHED_WINDOW_SEC = 15L

    private val BURST_NAME_REGEX = Regex(
        """(^|[_\-.])BURST(\d|[_\-.]|$)|_COVER\b""",
        RegexOption.IGNORE_CASE
    )

    fun isBurstFilename(name: String): Boolean {
        if (name.isBlank()) return false
        return BURST_NAME_REGEX.containsMatchIn(name)
    }

    /**
     * Groups eligible burst & rapid similar photos (plus any explicit user-created [manualGroups])
     * into [PhotoStackIndex].
     *
     * Rules:
     * - Videos, Trash, and manually unstacked photo IDs are never stacked.
     * - Explicit [manualGroups] (size >= 2) are honored first and stay intact without merging into
     *   or being split by automatic temporal clustering.
     * - When both photos have a perceptual hash ([PhotoStackCandidate.pHash]), they stack iff taken
     *   within [BURST_WINDOW_SEC] and Hamming distance <= [BURST_MAX_HAMMING].
     * - Before perceptual hashes finish computing, photos in the same bucket taken within
     *   [RAPID_UNHASHED_WINDOW_SEC] (or explicitly marked burst filenames within
     *   [NAMED_BURST_UNHASHED_WINDOW_SEC]) stack immediately.
     * - If any member of a stack has a manual Best Shot override in [manualBestShotPicks]
     *   (map of mediaId -> pickedAtEpochMs), the most recently picked member becomes [PhotoStack.bestShotId].
     */
    fun buildStacks(
        candidates: List<PhotoStackCandidate>,
        manualBestShotPicks: Map<Long, Long> = emptyMap(),
        unstackedIds: Set<Long> = emptySet(),
        manualGroups: List<List<Long>> = emptyList(),
        burstWindowSec: Long = BURST_WINDOW_SEC,
        burstMaxHamming: Int = BURST_MAX_HAMMING
    ): PhotoStackIndex {
        val allPhotos = candidates.filter {
            !it.isVideo && !it.bucketName.equals("Trash", ignoreCase = true)
        }
        if (allPhotos.size < 2) return PhotoStackIndex.EMPTY

        val stacksByCoverId = HashMap<Long, PhotoStack>()
        val stackByMemberId = HashMap<Long, PhotoStack>()
        val hiddenMemberIds = HashSet<Long>()
        val manuallyAssignedIds = HashSet<Long>()

        fun registerStack(members: List<PhotoStackCandidate>, isManual: Boolean) {
            if (members.size < 2) return

            // Keep chronological (newest first) order matching timeline
            val ordered = members.sortedWith(
                compareByDescending<PhotoStackCandidate> { it.dateAddedSec }
                    .thenByDescending { it.id }
            )

            val autoBest = ordered.maxWithOrNull(
                compareBy<PhotoStackCandidate> {
                    PerceptualHash.scoreBestShot(it.sharpness, it.pixelCount, it.sizeBytes)
                }.thenBy { it.sizeBytes }.thenBy { it.id }
            ) ?: ordered.first()

            val manualBest = ordered
                .mapNotNull { cand -> manualBestShotPicks[cand.id]?.let { ts -> cand to ts } }
                .maxByOrNull { it.second }
                ?.first

            val chosenBest = manualBest ?: autoBest
            val minDate = ordered.minOf { it.dateAddedSec }
            val maxDate = ordered.maxOf { it.dateAddedSec }
            val isBurst = !isManual && ((maxDate - minDate) <= 30L || ordered.any { isBurstFilename(it.name) })
            val memberIds = ordered.map { it.id }
            val stack = PhotoStack(
                stackId = if (isManual) "manual_stack_${memberIds.min()}" else "stack_${memberIds.min()}",
                bestShotId = chosenBest.id,
                autoBestShotId = autoBest.id,
                memberIds = memberIds,
                isBurst = isBurst,
                isManual = isManual
            )

            stacksByCoverId[chosenBest.id] = stack
            for (memberId in memberIds) {
                stackByMemberId[memberId] = stack
                if (memberId != chosenBest.id) {
                    hiddenMemberIds.add(memberId)
                }
            }
        }

        // 1. Honor explicit user-created manual stacks first
        if (manualGroups.isNotEmpty()) {
            val byId = allPhotos.associateBy { it.id }
            for (group in manualGroups) {
                val members = group.distinct().mapNotNull { id ->
                    if (id !in unstackedIds && id !in manuallyAssignedIds) byId[id] else null
                }
                if (members.size >= 2) {
                    for (m in members) manuallyAssignedIds.add(m.id)
                    registerStack(members, isManual = true)
                }
            }
        }

        // 2. Run automatic burst / similar-retake clustering on remaining eligible photos
        val eligible = allPhotos.filter {
            it.id !in unstackedIds && it.id !in manuallyAssignedIds
        }
        if (eligible.size >= 2) {
            val sorted = eligible.sortedWith(
                compareByDescending<PhotoStackCandidate> { it.dateAddedSec }
                    .thenByDescending { it.id }
            )
            val n = sorted.size
            val parent = IntArray(n) { it }

            fun find(x: Int): Int {
                var r = x
                while (parent[r] != r) {
                    parent[r] = parent[parent[r]]
                    r = parent[r]
                }
                return r
            }

            fun union(a: Int, b: Int) {
                val ra = find(a)
                val rb = find(b)
                if (ra != rb) parent[ra] = rb
            }

            // Single-pass sliding temporal window (descending dateAddedSec)
            for (i in 0 until n) {
                val a = sorted[i]
                val aIsBurstName = isBurstFilename(a.name)
                var j = i + 1
                while (j < n) {
                    val b = sorted[j]
                    val dt = abs(a.dateAddedSec - b.dateAddedSec)
                    if (dt > burstWindowSec) break

                    val shouldLink = when {
                        a.pHash != null && b.pHash != null -> {
                            PerceptualHash.hammingDistance(a.pHash, b.pHash) <= burstMaxHamming
                        }
                        a.bucketName == b.bucketName && dt <= RAPID_UNHASHED_WINDOW_SEC -> {
                            true
                        }
                        a.bucketName == b.bucketName &&
                            dt <= NAMED_BURST_UNHASHED_WINDOW_SEC &&
                            aIsBurstName &&
                            isBurstFilename(b.name) -> {
                            true
                        }
                        else -> false
                    }
                    if (shouldLink) {
                        union(i, j)
                    }
                    j++
                }
            }

            val grouped = HashMap<Int, MutableList<PhotoStackCandidate>>()
            for (i in 0 until n) {
                grouped.getOrPut(find(i)) { ArrayList(4) }.add(sorted[i])
            }

            for (members in grouped.values) {
                registerStack(members, isManual = false)
            }
        }

        if (stacksByCoverId.isEmpty()) return PhotoStackIndex.EMPTY
        return PhotoStackIndex(
            stacksByCoverId = stacksByCoverId,
            stackByMemberId = stackByMemberId,
            hiddenMemberIds = hiddenMemberIds
        )
    }

    /**
     * Identifies automatic burst/similar stacks that would re-form if [unstackedIds] were cleared,
     * while respecting any photos currently held inside explicit [manualGroups].
     */
    fun findRestorableAutoStacks(
        candidates: List<PhotoStackCandidate>,
        unstackedIds: Set<Long>,
        manualGroups: List<List<Long>> = emptyList(),
        burstWindowSec: Long = BURST_WINDOW_SEC,
        burstMaxHamming: Int = BURST_MAX_HAMMING
    ): List<PhotoStack> {
        if (unstackedIds.isEmpty()) return emptyList()
        val manuallyAssigned = HashSet<Long>()
        for (group in manualGroups) {
            if (group.size >= 2) manuallyAssigned.addAll(group)
        }
        val pool = if (manuallyAssigned.isEmpty()) candidates else candidates.filter { it.id !in manuallyAssigned }
        val restoredIndex = buildStacks(
            candidates = pool,
            manualBestShotPicks = emptyMap(),
            unstackedIds = emptySet(),
            manualGroups = emptyList(),
            burstWindowSec = burstWindowSec,
            burstMaxHamming = burstMaxHamming
        )
        return restoredIndex.stacksByCoverId.values
            .filter { stack -> stack.memberIds.any { it in unstackedIds } }
            .sortedByDescending { it.bestShotId }
    }

    /**
     * Collapses stacked members out of a [NativeTimelineItem] list, annotating each stack's cover
     * item with [NativeTimelineItem.Media.stackCount] and stripping any orphan date headers.
     */
    fun collapseNativeTimeline(
        items: List<NativeTimelineItem>,
        stackIndex: PhotoStackIndex
    ): List<NativeTimelineItem> {
        if (items.isEmpty() || stackIndex.isEmpty) return items

        val collapsed = ArrayList<NativeTimelineItem>(items.size)
        var pendingHeader: NativeTimelineItem.Header? = null

        for (item in items) {
            when (item) {
                is NativeTimelineItem.Header -> {
                    pendingHeader = item
                }
                is NativeTimelineItem.Media -> {
                    val idLong = item.id.toLongOrNull()
                    if (idLong != null && idLong in stackIndex.hiddenMemberIds) {
                        continue
                    }
                    pendingHeader?.let {
                        collapsed.add(it)
                        pendingHeader = null
                    }
                    val count = if (idLong != null) stackIndex.stackCountForCover(idLong) else 1
                    collapsed.add(if (count > 1) item.copy(stackCount = count) else item)
                }
            }
        }
        return collapsed
    }

    /**
     * Identifies unhashed photos that sit within [burstWindowSec] of another photo, so background
     * pHash indexing for Timeline Stacks only touches genuine burst/retake candidates instead of
     * decoding the entire photo library.
     */
    fun findBurstHashCandidates(
        candidates: List<PhotoStackCandidate>,
        burstWindowSec: Long = BURST_WINDOW_SEC,
        maxCount: Int = 250
    ): List<Long> {
        val photos = candidates
            .filter { !it.isVideo && !it.bucketName.equals("Trash", ignoreCase = true) }
            .sortedWith(
                compareByDescending<PhotoStackCandidate> { it.dateAddedSec }
                    .thenByDescending { it.id }
            )
        if (photos.size < 2) return emptyList()

        val result = ArrayList<Long>(minOf(maxCount, photos.size))
        for (i in photos.indices) {
            if (result.size >= maxCount) break
            val cur = photos[i]
            if (cur.pHash != null) continue
            val closeToPrev = i > 0 && abs(photos[i - 1].dateAddedSec - cur.dateAddedSec) <= burstWindowSec
            val closeToNext = i + 1 < photos.size && abs(cur.dateAddedSec - photos[i + 1].dateAddedSec) <= burstWindowSec
            if (closeToPrev || closeToNext) {
                result.add(cur.id)
            }
        }
        return result
    }
}
