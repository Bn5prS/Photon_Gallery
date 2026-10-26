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
import com.inferno.gallery.data.db.GalleryDatabase
import com.inferno.gallery.data.db.MediaFaceStatusEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object FaceScanHelper {
    private const val TAG = "FaceScanHelper"
    const val MAX_THUMBNAIL_DIM = 1024

    // Session cache so we don't repeatedly scan the same zero-face photo in ExifDetailsSheet
    val scannedSessionIds: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    fun createDetectorOptions(): FaceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setMinFaceSize(0.02f) // Broadened from 0.05f to 0.02f (2%) so group and candid faces are detected
        .build()

    fun getFaceCropDir(context: Context): File = File(context.filesDir, "face_crops").apply {
        if (!exists()) mkdirs()
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
        if (sampledBitmap == null || sampledBitmap.width < 24 || sampledBitmap.height < 24) {
            faceDao.insertFaceStatus(
                MediaFaceStatusEntity(
                    mediaId = mediaId,
                    faceCount = 0,
                    scannedAt = System.currentTimeMillis()
                )
            )
            sampledBitmap?.recycle()
            if (ownDetector) {
                runCatching { activeDetector.close() }
            }
            return@withContext 0
        }

        try {
            val inputImage = InputImage.fromBitmap(sampledBitmap, 0)
            val detectedFaces = Tasks.await(activeDetector.process(inputImage))
            var savedFaceCount = 0

            for ((faceIndex, face) in detectedFaces.withIndex()) {
                val box = face.boundingBox
                if (box.width() < 20 || box.height() < 20) continue
                // Relaxed angle thresholds for natural candid shots (yaw 60°, pitch 48°)
                if (abs(face.headEulerAngleY) > 60f || abs(face.headEulerAngleX) > 48f) continue

                val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
                val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position

                val aligned112: Bitmap? = if (leftEye != null && rightEye != null) {
                    val landmarks = mutableListOf(
                        FacePoint(leftEye.x, leftEye.y),
                        FacePoint(rightEye.x, rightEye.y)
                    )
                    if (nose != null) landmarks.add(FacePoint(nose.x, nose.y))
                    if (mouthLeft != null) landmarks.add(FacePoint(mouthLeft.x, mouthLeft.y))
                    if (mouthRight != null) landmarks.add(FacePoint(mouthRight.x, mouthRight.y))
                    FaceNormalizer.alignFace(sampledBitmap, landmarks) ?: cropFaceBoundingBox(sampledBitmap, box)
                } else {
                    cropFaceBoundingBox(sampledBitmap, box)
                }
                if (aligned112 == null) continue

                try {
                    val embedding = engine.extractEmbedding(aligned112) ?: continue

                    val cropFile = File(faceCropDir, "face_${mediaId}_${faceIndex}.webp")
                    val avatarBitmap = extractNaturalPortraitCrop(sampledBitmap, box, 160)
                    try {
                        FileOutputStream(cropFile).use { out ->
                            avatarBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
                        }
                    } finally {
                        avatarBitmap.recycle()
                    }

                    val normBboxLeft = (box.left.toFloat() / sampledBitmap.width).coerceIn(0f, 1f)
                    val normBboxTop = (box.top.toFloat() / sampledBitmap.height).coerceIn(0f, 1f)
                    val normBboxRight = (box.right.toFloat() / sampledBitmap.width).coerceIn(0f, 1f)
                    val normBboxBottom = (box.bottom.toFloat() / sampledBitmap.height).coerceIn(0f, 1f)

                    val poseScore = 1f - (abs(face.headEulerAngleY) / 60f * 0.5f + abs(face.headEulerAngleX) / 48f * 0.5f)
                    val qualityScore = poseScore.coerceIn(0.1f, 1.0f)

                    clusteringManager.assignFaceToCluster(
                        mediaId = mediaId,
                        embedding = embedding,
                        bboxLeft = normBboxLeft,
                        bboxTop = normBboxTop,
                        bboxRight = normBboxRight,
                        bboxBottom = normBboxBottom,
                        qualityScore = qualityScore,
                        cropCachePath = cropFile.absolutePath
                    )
                    savedFaceCount++
                } finally {
                    aligned112.recycle()
                }
            }

            faceDao.insertFaceStatus(
                MediaFaceStatusEntity(
                    mediaId = mediaId,
                    faceCount = savedFaceCount,
                    scannedAt = System.currentTimeMillis()
                )
            )
            savedFaceCount
        } catch (e: Exception) {
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
            sampledBitmap.recycle()
            if (ownDetector) {
                runCatching { activeDetector.close() }
            }
        }
    }

    fun cropFaceBoundingBox(source: Bitmap, box: Rect): Bitmap? {
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
            if (scaled != cropped) cropped.recycle()
            scaled
        } catch (_: Exception) {
            null
        }
    }

    fun extractNaturalPortraitCrop(bitmap: Bitmap, box: Rect, targetSize: Int = 160): Bitmap {
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
                Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
            } else {
                val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
                val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
                if (scaled != cropped) cropped.recycle()
                scaled
            }
        } catch (_: Exception) {
            Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
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
            if (rotated != bitmap) bitmap.recycle()
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
