package com.inferno.gallery.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.inferno.gallery.data.db.FaceEntity
import com.inferno.gallery.data.db.GalleryDatabase
import com.inferno.gallery.data.db.MediaFaceStatusEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure 2D bounding box independent of Android [Rect] stubs so tile geometry and NMS are JVM-testable.
 */
data class FaceBox2D(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val area: Float get() = width * height
    val centerX: Float get() = (left + right) * 0.5f
    val centerY: Float get() = (top + bottom) * 0.5f

    fun intersectionArea(other: FaceBox2D): Float {
        val ix1 = max(left, other.left)
        val iy1 = max(top, other.top)
        val ix2 = min(right, other.right)
        val iy2 = min(bottom, other.bottom)
        if (ix2 <= ix1 || iy2 <= iy1) return 0f
        return (ix2 - ix1) * (iy2 - iy1)
    }

    fun iou(other: FaceBox2D): Float {
        val inter = intersectionArea(other)
        if (inter <= 0f) return 0f
        val union = area + other.area - inter
        return if (union > 1e-6f) inter / union else 0f
    }

    fun ioMin(other: FaceBox2D): Float {
        val inter = intersectionArea(other)
        if (inter <= 0f) return 0f
        val minArea = min(area, other.area)
        return if (minArea > 1e-6f) inter / minArea else 0f
    }
}

data class RankedFaceBox(
    val index: Int,
    val box: FaceBox2D,
    val score: Float
)

object FaceScanHelper {
    private const val TAG = "FaceScanHelper"
    const val MAX_THUMBNAIL_DIM = 2048
    private const val MAX_YAW_DEG = 78f
    private const val MAX_PITCH_DEG = 58f

    // Session cache so we don't repeatedly scan the same zero-face photo in ExifDetailsSheet
    val scannedSessionIds: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    private data class CandidateDetection(
        val box: FaceBox2D,
        val rect: Rect,
        val landmarks: DetectedFaceLandmarks,
        val qualityScore: Float,
        val rankScore: Float
    )

    fun createDetectorOptions(): FaceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setMinFaceSize(0.015f)
        .build()

    fun getFaceCropDir(context: Context): File = File(context.filesDir, "face_crops").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Triggers overlapping high-resolution tile passes ONLY on multi-person scenes (`>= 2` faces)
     * or wide shots with small distant faces (`< 14%` of max image dimension). Single-person
     * portraits and 0-face photos pay zero extra detection cost.
     */
    fun shouldRunTileScan(
        imageWidth: Int,
        imageHeight: Int,
        fullPassBoxes: List<FaceBox2D>
    ): Boolean {
        if (min(imageWidth, imageHeight) < 800) return false
        if (fullPassBoxes.isEmpty()) return false
        if (fullPassBoxes.size >= 2) return true
        val maxDim = max(imageWidth, imageHeight).toFloat()
        return fullPassBoxes.any { max(it.width, it.height) < maxDim * 0.14f }
    }

    /**
     * Generates 5 overlapping ~62%-span tiles (4 corners + center window) so every small face in a
     * group photo — including faces straddling the horizontal/vertical midlines — sits uncut inside
     * at least one tile before ML Kit's internal detector downscaler runs.
     */
    fun computeScanTiles(imageWidth: Int, imageHeight: Int): List<FaceBox2D> {
        if (imageWidth < 600 || imageHeight < 600) return emptyList()
        val tileW = (imageWidth * 0.62f).roundToInt().coerceIn(1, imageWidth)
        val tileH = (imageHeight * 0.62f).roundToInt().coerceIn(1, imageHeight)
        val rightX = (imageWidth - tileW).coerceAtLeast(0)
        val bottomY = (imageHeight - tileH).coerceAtLeast(0)
        val midX = (rightX / 2).coerceAtLeast(0)
        val midY = (bottomY / 2).coerceAtLeast(0)

        return listOf(
            FaceBox2D(0f, 0f, tileW.toFloat(), tileH.toFloat()),
            FaceBox2D(rightX.toFloat(), 0f, (rightX + tileW).toFloat(), tileH.toFloat()),
            FaceBox2D(0f, bottomY.toFloat(), tileW.toFloat(), (bottomY + tileH).toFloat()),
            FaceBox2D(rightX.toFloat(), bottomY.toFloat(), (rightX + tileW).toFloat(), (bottomY + tileH).toFloat()),
            FaceBox2D(midX.toFloat(), midY.toFloat(), (midX + tileW).toFloat(), (midY + tileH).toFloat())
        ).distinct()
    }

    /**
     * Non-maximum suppression across full-frame and tile detections. Prefers candidates with richer
     * landmarks / sharper tile crops while preserving closely-spaced neighbors in dense crowds.
     */
    fun deduplicateRankedBoxes(candidates: List<RankedFaceBox>): List<RankedFaceBox> {
        if (candidates.size <= 1) return candidates
        val sorted = candidates.sortedByDescending { it.score }
        val kept = ArrayList<RankedFaceBox>(sorted.size)
        for (cand in sorted) {
            val isDuplicate = kept.any { winner ->
                val iou = cand.box.iou(winner.box)
                if (iou >= 0.38f) {
                    true
                } else {
                    val ioMin = cand.box.ioMin(winner.box)
                    val centerDist = hypot(
                        (cand.box.centerX - winner.box.centerX).toDouble(),
                        (cand.box.centerY - winner.box.centerY).toDouble()
                    ).toFloat()
                    val refDim = min(
                        max(cand.box.width, cand.box.height),
                        max(winner.box.width, winner.box.height)
                    )
                    ioMin >= 0.68f && centerDist < 0.45f * refDim
                }
            }
            if (!isDuplicate) {
                kept.add(cand)
            }
        }
        return kept
    }

    private fun extractCandidate(
        face: Face,
        offsetX: Int,
        offsetY: Int,
        imageWidth: Int,
        imageHeight: Int,
        fromTile: Boolean
    ): CandidateDetection? {
        val rawBox = face.boundingBox
        val left = (rawBox.left + offsetX).coerceIn(0, imageWidth)
        val top = (rawBox.top + offsetY).coerceIn(0, imageHeight)
        val right = (rawBox.right + offsetX).coerceIn(0, imageWidth)
        val bottom = (rawBox.bottom + offsetY).coerceIn(0, imageHeight)
        val w = right - left
        val h = bottom - top
        if (w < 20 || h < 20) return null

        val yaw = face.headEulerAngleY
        val pitch = face.headEulerAngleX
        val roll = face.headEulerAngleZ
        if (abs(yaw) > MAX_YAW_DEG || abs(pitch) > MAX_PITCH_DEG) return null

        fun pointOf(type: Int): FacePoint? = face.getLandmark(type)?.position?.let {
            FacePoint(it.x + offsetX, it.y + offsetY)
        }

        val leftEye = pointOf(FaceLandmark.LEFT_EYE)
        val rightEye = pointOf(FaceLandmark.RIGHT_EYE)
        val nose = pointOf(FaceLandmark.NOSE_BASE)
        val mouthLeft = pointOf(FaceLandmark.MOUTH_LEFT)
        val mouthRight = pointOf(FaceLandmark.MOUTH_RIGHT)

        val landmarks = DetectedFaceLandmarks(
            leftEye = leftEye,
            rightEye = rightEye,
            nose = nose,
            mouthLeft = mouthLeft,
            mouthRight = mouthRight,
            yawDeg = yaw,
            pitchDeg = pitch,
            rollDeg = roll
        )

        // Precision guard: reject tiny landmarkless pareidolia patches
        if (max(w, h) < 30 && landmarks.count < 2) return null

        // Anatomical guard on upright faces: eyes must sit above mouth
        if (abs(roll) < 35f) {
            val eyeY = leftEye?.y ?: rightEye?.y
            val mouthY = mouthLeft?.y ?: mouthRight?.y
            if (eyeY != null && mouthY != null && eyeY >= mouthY) return null
        }

        val poseScore = 1f - (abs(yaw) / MAX_YAW_DEG * 0.55f + abs(pitch) / MAX_PITCH_DEG * 0.45f)
        val sizeBonus = (min(w, h).toFloat() / 90f).coerceIn(0f, 1f) * 0.15f
        val qualityScore = (poseScore * 0.85f + sizeBonus).coerceIn(0.15f, 1.0f)
        val rankScore = landmarks.count * 10f + qualityScore * 4f + (if (fromTile) 1.5f else 0f)

        return CandidateDetection(
            box = FaceBox2D(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat()),
            rect = Rect(left, top, right, bottom),
            landmarks = landmarks,
            qualityScore = qualityScore,
            rankScore = rankScore
        )
    }

    suspend fun scanSingleMedia(
        context: Context,
        mediaId: Long,
        filePath: String,
        uri: Uri,
        database: GalleryDatabase,
        engine: FaceRecognitionEngine,
        clusteringManager: FaceClusteringManager,
        detector: FaceDetector? = null
    ): Int = withContext(Dispatchers.IO) {
        if (!FaceRecognitionEngine.isSupported() || !engine.isModelDownloaded()) {
            return@withContext 0
        }
        engine.loadModel()

        val faceDao = database.faceDao()
        val ownDetector = detector == null
        val activeDetector = detector ?: FaceDetection.getClient(createDetectorOptions())

        val faceCropDir = getFaceCropDir(context)
        val sampledBitmap = decodeSampledBitmap(context, filePath, uri, MAX_THUMBNAIL_DIM)
        if (sampledBitmap == null || sampledBitmap.isRecycled || sampledBitmap.width < 24 || sampledBitmap.height < 24) {
            faceDao.insertFaceStatus(
                MediaFaceStatusEntity(
                    mediaId = mediaId,
                    faceCount = 0,
                    scannedAt = System.currentTimeMillis()
                )
            )
            if (sampledBitmap != null && !sampledBitmap.isRecycled) {
                sampledBitmap.recycle()
            }
            if (ownDetector) {
                runCatching { activeDetector.close() }
            }
            return@withContext 0
        }

        try {
            val imgW = sampledBitmap.width
            val imgH = sampledBitmap.height
            val rawCandidates = ArrayList<CandidateDetection>()

            // 1. Full-frame pass
            val fullInput = InputImage.fromBitmap(sampledBitmap, 0)
            val fullDetected = Tasks.await(activeDetector.process(fullInput))
            val fullBoxes = ArrayList<FaceBox2D>(fullDetected.size)
            for (face in fullDetected) {
                val b = face.boundingBox
                fullBoxes += FaceBox2D(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
                extractCandidate(face, 0, 0, imgW, imgH, fromTile = false)?.let { rawCandidates += it }
            }

            // 2. Conditional overlapping tile pass for multi-face / distant-face group photos
            if (!sampledBitmap.isRecycled && shouldRunTileScan(imgW, imgH, fullBoxes)) {
                val tiles = computeScanTiles(imgW, imgH)
                for (tile in tiles) {
                    if (sampledBitmap.isRecycled) break
                    val tx = tile.left.toInt().coerceIn(0, imgW - 1)
                    val ty = tile.top.toInt().coerceIn(0, imgH - 1)
                    val tw = (tile.right.toInt() - tx).coerceIn(1, imgW - tx)
                    val th = (tile.bottom.toInt() - ty).coerceIn(1, imgH - ty)
                    if (tw < 64 || th < 64) continue

                    val tileBitmap = try {
                        Bitmap.createBitmap(sampledBitmap, tx, ty, tw, th)
                    } catch (_: Throwable) {
                        null
                    } ?: continue

                    try {
                        val tileFaces = Tasks.await(activeDetector.process(InputImage.fromBitmap(tileBitmap, 0)))
                        for (face in tileFaces) {
                            extractCandidate(face, tx, ty, imgW, imgH, fromTile = true)?.let {
                                rawCandidates += it
                            }
                        }
                    } finally {
                        if (tileBitmap !== sampledBitmap && !tileBitmap.isRecycled) {
                            tileBitmap.recycle()
                        }
                    }
                }
            }

            val deduplicated = deduplicateRankedBoxes(
                rawCandidates.mapIndexed { idx, c -> RankedFaceBox(idx, c.box, c.rankScore) }
            ).map { rawCandidates[it.index] }
                .sortedByDescending { it.qualityScore }

            // Track existing FaceEntity rows on this mediaId so rescans upgrade embeddings and
            // add newly found group faces without creating duplicates or losing manual tags.
            val remainingExisting = faceDao.getFacesByMediaId(mediaId).toMutableList()
            val usedPersonIdsInMedia = HashSet<Long>()
            for (existing in remainingExisting) {
                existing.personId?.let { usedPersonIdsInMedia.add(it) }
            }

            var savedFaceCount = 0
            for ((faceIndex, det) in deduplicated.withIndex()) {
                if (sampledBitmap.isRecycled) break
                val box = det.rect

                val aligned112: Bitmap? = FaceNormalizer.alignFacePoseAware(sampledBitmap, det.landmarks)
                    ?: cropFaceBoundingBox(sampledBitmap, box)
                if (aligned112 == null || aligned112.isRecycled) continue

                try {
                    val embedding = engine.extractEmbedding(aligned112) ?: continue

                    val normBboxLeft = (box.left.toFloat() / imgW).coerceIn(0f, 1f)
                    val normBboxTop = (box.top.toFloat() / imgH).coerceIn(0f, 1f)
                    val normBboxRight = (box.right.toFloat() / imgW).coerceIn(0f, 1f)
                    val normBboxBottom = (box.bottom.toFloat() / imgH).coerceIn(0f, 1f)
                    val normCandidateBox = FaceBox2D(normBboxLeft, normBboxTop, normBboxRight, normBboxBottom)

                    // Match against existing stored faces on this mediaId (if rescanning)
                    val matchedExisting: FaceEntity? = remainingExisting
                        .map { ex ->
                            val exBox = FaceBox2D(ex.bboxLeft, ex.bboxTop, ex.bboxRight, ex.bboxBottom)
                            ex to max(normCandidateBox.iou(exBox), normCandidateBox.ioMin(exBox) * 0.8f)
                        }
                        .filter { (_, overlap) -> overlap >= 0.40f }
                        .maxByOrNull { it.second }
                        ?.first

                    if (matchedExisting != null) {
                        remainingExisting.remove(matchedExisting)
                    }

                    val existingCropValid = matchedExisting?.cropCachePath?.let { File(it).exists() } == true
                    val cropFile = if (existingCropValid) {
                        File(matchedExisting!!.cropCachePath!!)
                    } else {
                        val target = File(faceCropDir, "face_${mediaId}_${faceIndex}_${System.nanoTime() and 0xFFFF}.webp")
                        val avatarBitmap = extractNaturalPortraitCrop(sampledBitmap, box, 160)
                        try {
                            FileOutputStream(target).use { out ->
                                avatarBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
                            }
                        } finally {
                            if (avatarBitmap !== sampledBitmap && !avatarBitmap.isRecycled) {
                                avatarBitmap.recycle()
                            }
                        }
                        target
                    }

                    if (matchedExisting != null && matchedExisting.personId != null) {
                        faceDao.updateFace(
                            matchedExisting.copy(
                                bboxLeft = normBboxLeft,
                                bboxTop = normBboxTop,
                                bboxRight = normBboxRight,
                                bboxBottom = normBboxBottom,
                                qualityScore = det.qualityScore,
                                cropCachePath = cropFile.absolutePath,
                                embedding = embedding
                            )
                        )
                        usedPersonIdsInMedia.add(matchedExisting.personId)
                    } else {
                        if (matchedExisting != null) {
                            faceDao.deleteFaceById(matchedExisting.id)
                        }
                        val assignedPersonId = clusteringManager.assignFaceToCluster(
                            mediaId = mediaId,
                            embedding = embedding,
                            bboxLeft = normBboxLeft,
                            bboxTop = normBboxTop,
                            bboxRight = normBboxRight,
                            bboxBottom = normBboxBottom,
                            qualityScore = det.qualityScore,
                            cropCachePath = cropFile.absolutePath,
                            excludedPersonIds = usedPersonIdsInMedia
                        )
                        usedPersonIdsInMedia.add(assignedPersonId)
                    }
                    savedFaceCount++
                } finally {
                    if (aligned112 !== sampledBitmap && !aligned112.isRecycled) {
                        aligned112.recycle()
                    }
                }
            }

            val totalFacesForMedia = savedFaceCount + remainingExisting.count { it.personId != null }
            faceDao.insertFaceStatus(
                MediaFaceStatusEntity(
                    mediaId = mediaId,
                    faceCount = totalFacesForMedia,
                    scannedAt = System.currentTimeMillis()
                )
            )
            totalFacesForMedia
        } catch (e: Exception) {
            val isMlKitUnavailable = e is com.google.mlkit.common.MlKitException &&
                e.errorCode == com.google.mlkit.common.MlKitException.UNAVAILABLE
            if (isMlKitUnavailable) {
                Log.w(TAG, "ML Kit Face module is still downloading or unavailable. Skipping mediaId $mediaId without marking as scanned.")
                return@withContext 0
            }
            Log.w(TAG, "Error scanning faces for media $mediaId: ${e.message}")
            faceDao.insertFaceStatus(
                MediaFaceStatusEntity(
                    mediaId = mediaId,
                    faceCount = 0,
                    scannedAt = System.currentTimeMillis()
                )
            )
            0
        } finally {
            if (!sampledBitmap.isRecycled) {
                sampledBitmap.recycle()
            }
            if (ownDetector) {
                runCatching { activeDetector.close() }
            }
        }
    }

    fun cropFaceBoundingBox(source: Bitmap, box: Rect): Bitmap? {
        if (source.isRecycled) return null
        return try {
            val marginX = (box.width() * 0.15f).toInt()
            val marginY = (box.height() * 0.15f).toInt()
            val left = (box.left - marginX).coerceIn(0, source.width - 1)
            val top = (box.top - marginY).coerceIn(0, source.height - 1)
            val right = (box.right + marginX).coerceIn(left + 1, source.width)
            val bottom = (box.bottom + marginY).coerceIn(top + 1, source.height)
            val cropWidth = right - left
            val cropHeight = bottom - top
            if (cropWidth < 20 || cropHeight < 20) return null
            val cropped = Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
            val scaled = Bitmap.createScaledBitmap(cropped, FaceNormalizer.CROP_SIZE, FaceNormalizer.CROP_SIZE, true)
            if (scaled !== cropped && cropped !== source && !cropped.isRecycled) {
                cropped.recycle()
            }
            if (scaled === source) {
                scaled.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                scaled
            }
        } catch (_: Exception) {
            null
        }
    }

    fun extractNaturalPortraitCrop(bitmap: Bitmap, box: Rect, targetSize: Int = 160): Bitmap {
        if (bitmap.isRecycled) {
            return Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        }
        return try {
            val centerX = box.centerX()
            val centerY = box.centerY()
            val halfDim = (max(box.width(), box.height()) * 0.75f).roundToInt()

            val left = (centerX - halfDim).coerceAtLeast(0)
            val top = (centerY - halfDim).coerceAtLeast(0)
            val right = (centerX + halfDim).coerceAtMost(bitmap.width)
            val bottom = (centerY + halfDim).coerceAtMost(bitmap.height)

            val width = right - left
            val height = bottom - top
            if (width <= 0 || height <= 0) {
                Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true).let {
                    if (it === bitmap) it.copy(Bitmap.Config.ARGB_8888, false) else it
                }
            } else {
                val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
                val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
                if (scaled !== cropped && cropped !== bitmap && !cropped.isRecycled) {
                    cropped.recycle()
                }
                if (scaled === bitmap) {
                    scaled.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    scaled
                }
            }
        } catch (_: Exception) {
            if (!bitmap.isRecycled) {
                Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true).let {
                    if (it === bitmap) it.copy(Bitmap.Config.ARGB_8888, false) else it
                }
            } else {
                Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            }
        }
    }

    fun openInputStream(context: Context, filePath: String, uri: Uri): InputStream? {
        if (filePath.isNotBlank()) {
            val file = File(filePath)
            if (file.exists() && file.canRead()) {
                return file.inputStream()
            }
        }
        return context.contentResolver.openInputStream(uri)
    }

    fun readExifOrientation(context: Context, filePath: String, uri: Uri): Int {
        return try {
            if (filePath.isNotBlank()) {
                val file = File(filePath)
                if (file.exists() && file.canRead()) {
                    return ExifInterface(filePath).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }
            }
            openInputStream(context, filePath, uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap && !bitmap.isRecycled) bitmap.recycle()
            rotated
        } catch (_: Exception) {
            bitmap
        }
    }

    fun decodeSampledBitmap(context: Context, filePath: String, uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            val orientation = readExifOrientation(context, filePath, uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openInputStream(context, filePath, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var inSampleSize = 1
            val maxOriginal = max(origWidth, origHeight)
            while (maxOriginal / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = openInputStream(context, filePath, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            applyExifOrientation(decoded, orientation)
        } catch (_: Exception) {
            null
        }
    }
}
