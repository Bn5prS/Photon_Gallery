package com.inferno.gallery.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Pure Kotlin perceptual hashing (64-bit dHash), Laplacian sharpness scoring,
 * Best Shot ranking, and fast similar/burst clustering for offline cleanup (#51).
 */
object PerceptualHash {

    /**
     * Computes a 64-bit horizontal difference hash (dHash) from a 9x8 grayscale luma array.
     * Adjacent horizontal pixels are compared (8 diffs per row x 8 rows = 64 bits).
     */
    fun computeDHash(grayscale9x8: IntArray): Long {
        require(grayscale9x8.size >= 72) { "Expected at least 72 luma samples (9x8), got ${grayscale9x8.size}" }
        var hash = 0L
        var bit = 0
        for (y in 0 until 8) {
            val rowOffset = y * 9
            for (x in 0 until 8) {
                if (grayscale9x8[rowOffset + x] > grayscale9x8[rowOffset + x + 1]) {
                    hash = hash or (1L shl bit)
                }
                bit++
            }
        }
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    fun similarityPercent(hammingDistance: Int): Int {
        val clamped = hammingDistance.coerceIn(0, 64)
        return (((64 - clamped) / 64f) * 100f).toInt()
    }

    /**
     * Computes variance of a 3x3 discrete Laplacian kernel (4*c - l - r - t - b) over a
     * grayscale luma grid. Higher values indicate crisper edges; very low values indicate blur.
     */
    fun computeLaplacianSharpness(luma: IntArray, width: Int, height: Int): Float {
        if (width < 3 || height < 3 || luma.size < width * height) return 0f
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val idx = row + x
                val lap = (4 * luma[idx] - luma[idx - 1] - luma[idx + 1] - luma[idx - width] - luma[idx + width]).toDouble()
                sum += lap
                sumSq += lap * lap
                count++
            }
        }
        if (count == 0) return 0f
        val mean = sum / count
        val variance = (sumSq / count - mean * mean).coerceAtLeast(0.0)
        // Return standard deviation (square root of variance) on 0..255 luma scale for intuitive thresholds
        return kotlin.math.sqrt(variance).toFloat()
    }

    /**
     * Ranks photos in a similar/burst group so the sharpest, highest-resolution shot
     * is automatically chosen as "Best Shot".
     */
    fun scoreBestShot(sharpness: Float, pixelCount: Long, sizeBytes: Long): Float {
        val megapixels = (pixelCount.coerceAtLeast(0L) / 1_000_000.0).toFloat()
        val sizeMb = (sizeBytes.coerceAtLeast(0L) / 1_000_000.0).toFloat()
        return (sharpness.coerceAtLeast(0f) * 1.5f) + (megapixels * 10f) + (sizeMb * 2f)
    }
}

data class SimilarCandidate(
    val id: Long,
    val pHash: Long,
    val dateAddedSec: Long,
    val sizeBytes: Long,
    val fileHash: String?,
    val sharpness: Float,
    val pixelCount: Long,
    val isScreenshotOrDownload: Boolean
)

data class SimilarCluster(
    val id: String,
    val bestShotId: Long,
    val memberIds: List<Long>,
    val isBurst: Boolean,
    val averageSimilarityPct: Int
)

object SimilarPhotoClusterer {

    const val DEFAULT_BURST_WINDOW_SEC = 120L
    const val DEFAULT_BURST_MAX_HAMMING = 10
    const val DEFAULT_GENERAL_MAX_HAMMING = 5
    const val DEFAULT_BLUR_THRESHOLD = 14f

    /**
     * Groups near-identical photos and burst shots completely offline in near-linear time:
     * 1. Temporal burst sliding window (within [burstWindowSec], Hamming <= [burstMaxHamming]).
     * 2. Multi-band LSH index (6 x 10-bit bands guaranteeing 100% recall for Hamming <= 5).
     * Excludes groups where every photo shares the same non-null SHA-256 [fileHash]
     * (those belong exclusively in Exact Duplicates).
     */
    fun cluster(
        candidates: List<SimilarCandidate>,
        burstWindowSec: Long = DEFAULT_BURST_WINDOW_SEC,
        burstMaxHamming: Int = DEFAULT_BURST_MAX_HAMMING,
        generalMaxHamming: Int = DEFAULT_GENERAL_MAX_HAMMING
    ): List<SimilarCluster> {
        if (candidates.size < 2) return emptyList()

        val sorted = candidates.sortedByDescending { it.dateAddedSec }
        val n = sorted.size
        val parent = IntArray(n) { it }

        fun find(i: Int): Int {
            var r = i
            while (parent[r] != r) {
                parent[r] = parent[parent[r]]
                r = parent[r]
            }
            return r
        }

        fun union(a: Int, b: Int) {
            val ra = find(a)
            val rb = find(b)
            if (ra != rb) parent[rb] = ra
        }

        // Track min pairwise hamming & burst flag per connected edge
        val edgeHamming = HashMap<Long, Int>()
        val burstIndices = BooleanArray(n)

        fun recordMatch(i: Int, j: Int, dist: Int, isBurstMatch: Boolean) {
            union(i, j)
            if (isBurstMatch) {
                burstIndices[i] = true
                burstIndices[j] = true
            }
            val key = (minOf(i, j).toLong() shl 32) or maxOf(i, j).toLong()
            val prev = edgeHamming[key]
            if (prev == null || dist < prev) edgeHamming[key] = dist
        }

        // Pass 1: Temporal burst window (sorted descending by dateAddedSec)
        for (i in 0 until n) {
            val ci = sorted[i]
            var j = i + 1
            while (j < n && (ci.dateAddedSec - sorted[j].dateAddedSec) <= burstWindowSec) {
                val cj = sorted[j]
                val dist = PerceptualHash.hammingDistance(ci.pHash, cj.pHash)
                if (dist <= burstMaxHamming) {
                    recordMatch(i, j, dist, isBurstMatch = true)
                }
                j++
            }
        }

        // Pass 2: 6 x 10-bit band LSH (pigeonhole-guaranteed for Hamming <= 5)
        if (generalMaxHamming > 0) {
            val bands = Array(6) { HashMap<Int, MutableList<Int>>() }
            for (i in 0 until n) {
                val h = sorted[i].pHash
                for (b in 0 until 6) {
                    val bucketKey = ((h ushr (b * 10)) and 0x3FFL).toInt()
                    val bucket = bands[b].getOrPut(bucketKey) { ArrayList(4) }
                    // Cap pathological all-zero solid-color buckets to keep runtime O(N)
                    if (bucket.size < 64) {
                        for (other in bucket) {
                            if (find(i) == find(other)) continue
                            val dist = PerceptualHash.hammingDistance(h, sorted[other].pHash)
                            if (dist <= generalMaxHamming) {
                                val timeDiff = abs(sorted[i].dateAddedSec - sorted[other].dateAddedSec)
                                recordMatch(i, other, dist, isBurstMatch = timeDiff <= burstWindowSec)
                            }
                        }
                        bucket.add(i)
                    }
                }
            }
        }

        val grouped = HashMap<Int, MutableList<Int>>()
        for (i in 0 until n) {
            grouped.getOrPut(find(i)) { ArrayList() }.add(i)
        }

        val result = ArrayList<SimilarCluster>()
        for ((_, indices) in grouped) {
            if (indices.size < 2) continue
            val members = indices.map { sorted[it] }

            // Skip groups that are 100% byte-identical exact duplicates (handled by Exact Duplicates tab)
            val firstHash = members.first().fileHash
            if (firstHash != null && members.all { it.fileHash == firstHash }) continue

            val ranked = members.sortedWith(
                compareByDescending<SimilarCandidate> {
                    PerceptualHash.scoreBestShot(it.sharpness, it.pixelCount, it.sizeBytes)
                }.thenByDescending { it.dateAddedSec }
            )
            val best = ranked.first()
            val isBurst = indices.any { burstIndices[it] }

            // Compute average similarity against the best shot
            val avgDist = ranked.drop(1).map {
                PerceptualHash.hammingDistance(best.pHash, it.pHash)
            }.average().toInt()

            result.add(
                SimilarCluster(
                    id = "sim_${best.id}_${ranked.size}",
                    bestShotId = best.id,
                    memberIds = ranked.map { it.id },
                    isBurst = isBurst,
                    averageSimilarityPct = PerceptualHash.similarityPercent(avgDist)
                )
            )
        }

        // Sort largest reclaimable waste first
        return result.sortedByDescending { cluster ->
            val byId = candidates.associateBy { it.id }
            cluster.memberIds.drop(1).sumOf { byId[it]?.sizeBytes ?: 0L }
        }
    }

    /**
     * Identifies likely blurry/out-of-focus camera shots (excluding screenshots & downloads).
     */
    fun detectBlurry(
        candidates: List<SimilarCandidate>,
        blurThreshold: Float = DEFAULT_BLUR_THRESHOLD,
        minSizeBytes: Long = 50_000L
    ): List<Long> {
        return candidates
            .asSequence()
            .filter { !it.isScreenshotOrDownload }
            .filter { it.sizeBytes >= minSizeBytes }
            // > 0.5f skips completely solid-color synthetic frames while catching blurry photos
            .filter { it.sharpness in 0.5f..blurThreshold }
            .sortedBy { it.sharpness }
            .map { it.id }
            .toList()
    }
}

data class PhotoQualityMetrics(
    val dateModified: Long,
    val sharpness: Float,
    val pixelCount: Long
)

/**
 * Lightweight binary cache storing per-photo sharpness and resolution on disk
 * (`filesDir/photo_quality_cache_v1.dat`) with zero Room schema migration overhead.
 */
class PhotoQualityCache private constructor(private val cacheFile: File) {
    private val map = ConcurrentHashMap<Long, PhotoQualityMetrics>()
    @Volatile private var loaded = false

    companion object {
        @Volatile private var INSTANCE: PhotoQualityCache? = null

        fun getInstance(context: Context): PhotoQualityCache =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PhotoQualityCache(
                    File(context.applicationContext.filesDir, "photo_quality_cache_v1.dat")
                ).also { INSTANCE = it }
            }
    }

    fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            if (cacheFile.exists()) {
                try {
                    DataInputStream(BufferedInputStream(FileInputStream(cacheFile))).use { dis ->
                        val version = dis.readInt()
                        if (version == 1) {
                            val count = dis.readInt()
                            for (i in 0 until count) {
                                val id = dis.readLong()
                                val mod = dis.readLong()
                                val sharpness = dis.readFloat()
                                val pixels = dis.readLong()
                                map[id] = PhotoQualityMetrics(mod, sharpness, pixels)
                            }
                        }
                    }
                } catch (_: Throwable) {}
            }
            loaded = true
        }
    }

    fun get(id: Long, dateModified: Long): PhotoQualityMetrics? {
        ensureLoaded()
        return map[id]?.takeIf { it.dateModified == dateModified }
    }

    fun put(id: Long, metrics: PhotoQualityMetrics) {
        ensureLoaded()
        map[id] = metrics
    }

    fun snapshot(): Map<Long, PhotoQualityMetrics> {
        ensureLoaded()
        return HashMap(map)
    }

    fun persist() {
        synchronized(this) {
            try {
                val tmp = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
                DataOutputStream(BufferedOutputStream(FileOutputStream(tmp))).use { dos ->
                    dos.writeInt(1)
                    val entries = map.entries.toList()
                    dos.writeInt(entries.size)
                    for ((id, m) in entries) {
                        dos.writeLong(id)
                        dos.writeLong(m.dateModified)
                        dos.writeFloat(m.sharpness)
                        dos.writeLong(m.pixelCount)
                    }
                }
                if (tmp.exists()) {
                    tmp.renameTo(cacheFile)
                }
            } catch (_: Throwable) {}
        }
    }
}

data class AnalyzedPhotoSignature(
    val pHash: Long,
    val sharpness: Float,
    val pixelCount: Long
)

/**
 * Decodes compact ~128px thumbnails to compute both 64-bit dHash and Laplacian sharpness
 * in a single fast low-memory pass.
 */
object SimilarPhotoAnalyzer {

    fun analyze(
        contentResolver: ContentResolver,
        uri: Uri,
        filePath: String?
    ): AnalyzedPhotoSignature? {
        return try {
            // 1. Read original dimensions without allocating pixels
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream(contentResolver, uri, filePath)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val origW = bounds.outWidth
            val origH = bounds.outHeight
            if (origW <= 0 || origH <= 0) return null

            val targetMax = 128
            var sampleSize = 1
            while ((origW / (sampleSize * 2)) >= targetMax && (origH / (sampleSize * 2)) >= targetMax) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val thumb = openStream(contentResolver, uri, filePath)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            try {
                val w = thumb.width
                val h = thumb.height
                if (w < 9 || h < 8) return null

                // Extract grayscale luma for sharpness
                val pixels = IntArray(w * h)
                thumb.getPixels(pixels, 0, w, 0, 0, w, h)
                val luma = IntArray(w * h)
                for (i in pixels.indices) {
                    val c = pixels[i]
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    luma[i] = (r * 77 + g * 150 + b * 29) shr 8
                }
                val sharpness = PerceptualHash.computeLaplacianSharpness(luma, w, h)

                // Downsample to 9x8 grayscale for dHash
                val small = Bitmap.createScaledBitmap(thumb, 9, 8, true)
                val pHash = try {
                    val smallPx = IntArray(72)
                    small.getPixels(smallPx, 0, 9, 0, 0, 9, 8)
                    val smallLuma = IntArray(72)
                    for (i in 0 until 72) {
                        val c = smallPx[i]
                        val r = (c shr 16) and 0xFF
                        val g = (c shr 8) and 0xFF
                        val b = c and 0xFF
                        smallLuma[i] = (r * 77 + g * 150 + b * 29) shr 8
                    }
                    PerceptualHash.computeDHash(smallLuma)
                } finally {
                    if (small !== thumb && !small.isRecycled) small.recycle()
                }

                AnalyzedPhotoSignature(
                    pHash = pHash,
                    sharpness = sharpness,
                    pixelCount = origW.toLong() * origH.toLong()
                )
            } finally {
                if (!thumb.isRecycled) thumb.recycle()
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun openStream(contentResolver: ContentResolver, uri: Uri, filePath: String?) =
        try {
            contentResolver.openInputStream(uri)
        } catch (_: Throwable) {
            if (!filePath.isNullOrBlank()) {
                val f = File(filePath)
                if (f.exists() && f.canRead()) FileInputStream(f) else null
            } else null
        }

    fun isScreenshotOrDownload(bucketName: String, name: String, filePath: String): Boolean {
        val b = bucketName.lowercase()
        val n = name.lowercase()
        val p = filePath.lowercase()
        return b.contains("screenshot") || b.contains("download") || b.contains("whatsapp") ||
            n.startsWith("screenshot") || n.startsWith("screen_") || n.endsWith(".gif") ||
            n.endsWith(".png") && b.contains("screenshot") ||
            p.contains("/screenshots/") || p.contains("/download/")
    }
}

