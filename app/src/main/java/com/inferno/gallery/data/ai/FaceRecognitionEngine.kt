package com.inferno.gallery.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.sqrt

class FaceRecognitionEngine(private val context: Context) {

    companion object {
        private const val TAG = "FaceRecognitionEngine"

        @Volatile
        private var INSTANCE: FaceRecognitionEngine? = null

        fun isSupported(): Boolean {
            return Build.SUPPORTED_ABIS.any { it == "arm64-v8a" || it == "x86_64" }
        }

        fun getInstance(context: Context): FaceRecognitionEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FaceRecognitionEngine(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size || a.isEmpty()) return 0f
            var dot = 0f
            var normA = 0f
            var normB = 0f
            for (i in a.indices) {
                dot += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            val denom = sqrt(normA.toDouble()) * sqrt(normB.toDouble())
            return if (denom > 1e-6) (dot / denom).toFloat() else 0f
        }

        fun cosineDistance(a: FloatArray, b: FloatArray): Float {
            return 1f - cosineSimilarity(a, b)
        }

        fun normalizeL2(vector: FloatArray): FloatArray {
            var sum = 0.0
            for (v in vector) sum += v * v
            val norm = sqrt(sum).toFloat()
            if (norm < 1e-6f) return vector
            val normalized = FloatArray(vector.size)
            for (i in vector.indices) {
                normalized[i] = vector[i] / norm
            }
            return normalized
        }

        /**
         * Reverses each horizontal row of an ARGB pixel buffer for flip test-time augmentation.
         */
        fun mirrorPixelsHorizontally(
            pixels: IntArray,
            width: Int,
            height: Int,
            out: IntArray = IntArray(width * height)
        ): IntArray {
            for (y in 0 until height) {
                val rowStart = y * width
                for (x in 0 until width) {
                    out[rowStart + x] = pixels[rowStart + (width - 1 - x)]
                }
            }
            return out
        }

        /**
         * InsightFace CFP-FP flip-feature fusion: summing L2-normalized original + horizontally
         * mirrored embeddings cancels asymmetric left/right yaw components while sharpening
         * symmetric identity features.
         */
        fun fuseMirrorEmbeddings(primary: FloatArray, mirrored: FloatArray): FloatArray {
            if (primary.size != mirrored.size || primary.isEmpty()) {
                return normalizeL2(primary)
            }
            val normPrimary = normalizeL2(primary)
            val normMirrored = normalizeL2(mirrored)
            val sum = FloatArray(normPrimary.size)
            for (i in sum.indices) {
                sum[i] = normPrimary[i] + normMirrored[i]
            }
            val fused = normalizeL2(sum)
            var energy = 0.0
            for (v in fused) energy += v * v
            return if (energy < 1e-6) normPrimary else fused
        }
    }

    private val modelDirFile = File(context.filesDir, FaceModelRegistry.MODEL_DIR)
    private val sessionLock = Any()
    @Volatile
    private var env: OrtEnvironment? = null
    private var faceSession: OrtSession? = null
    @Volatile
    private var isLoaded = false
    private var reusablePixels: IntArray? = null
    private var reusableMirroredPixels: IntArray? = null
    private var reusableFloatData: FloatArray? = null

    fun getModelFile(): File = File(modelDirFile, FaceModelRegistry.MODEL_NAME)

    fun isModelDownloaded(): Boolean {
        if (!isSupported()) return false
        val modelFile = getModelFile()
        return modelFile.exists() && modelFile.length() > 5 * 1024 * 1024L
    }

    fun loadModel() {
        synchronized(sessionLock) {
            if (isLoaded && faceSession != null) return
            if (!isSupported()) return
            val modelFile = getModelFile()
            if (!modelFile.exists()) return

            try {
                val environment = env ?: OrtEnvironment.getEnvironment().also { env = it }
                OrtSession.SessionOptions().use { options ->
                    options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                    options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
                    options.setIntraOpNumThreads(2)
                    // Disable busy-wait spinning on mobile big.LITTLE CPUs to prevent thread contention
                    // with active video decoders (MediaCodec / ExoPlayer) and reduce thermal throttling.
                    options.addConfigEntry("session.intra_op.allow_spinning", "0")
                    this.faceSession = environment.createSession(modelFile.absolutePath, options)
                }
                isLoaded = true
                Log.d(TAG, "MobileFaceNet loaded successfully (${modelFile.length() / 1024} KB).")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load MobileFaceNet: ${e.message}", e)
                closeLocked()
            }
        }
    }

    fun extractEmbedding(aligned112: Bitmap): FloatArray? {
        if (aligned112.isRecycled) return null
        val width = aligned112.width
        val height = aligned112.height
        if (width != FaceNormalizer.CROP_SIZE || height != FaceNormalizer.CROP_SIZE) {
            Log.w(TAG, "Bitmap must be 112×112, got ${width}×${height}")
            return null
        }

        return synchronized(sessionLock) {
            if (!isLoaded || faceSession == null) {
                loadModel()
                if (!isLoaded || faceSession == null) return@synchronized null
            }
            val currentEnv = env ?: return@synchronized null
            val currentSession = faceSession ?: return@synchronized null

            val planeSize = width * height
            val pixels = reusablePixels?.takeIf { it.size == planeSize }
                ?: IntArray(planeSize).also { reusablePixels = it }
            val mirrored = reusableMirroredPixels?.takeIf { it.size == planeSize }
                ?: IntArray(planeSize).also { reusableMirroredPixels = it }

            aligned112.getPixels(pixels, 0, width, 0, 0, width, height)

            val primaryEmbedding = runSingleInference(pixels, width, height, currentEnv, currentSession)
                ?: return@synchronized null
            mirrorPixelsHorizontally(pixels, width, height, mirrored)
            val mirroredEmbedding = runSingleInference(mirrored, width, height, currentEnv, currentSession)

            if (mirroredEmbedding != null) {
                fuseMirrorEmbeddings(primaryEmbedding, mirroredEmbedding)
            } else {
                primaryEmbedding
            }
        }
    }

    private fun runSingleInference(
        pixels: IntArray,
        width: Int,
        height: Int,
        currentEnv: OrtEnvironment,
        currentSession: OrtSession
    ): FloatArray? {
        val planeSize = width * height
        val floatData = reusableFloatData?.takeIf { it.size == 3 * planeSize }
            ?: FloatArray(3 * planeSize).also { reusableFloatData = it }

        for (i in 0 until planeSize) {
            val p = pixels[i]
            floatData[i] = ((p shr 16 and 0xFF) - 127.5f) / 128.0f
            floatData[i + planeSize] = ((p shr 8 and 0xFF) - 127.5f) / 128.0f
            floatData[i + 2 * planeSize] = ((p and 0xFF) - 127.5f) / 128.0f
        }

        val shape = longArrayOf(1, 3, width.toLong(), height.toLong())
        val tensor = OnnxTensor.createTensor(currentEnv, FloatBuffer.wrap(floatData), shape)

        return try {
            val inputName = currentSession.inputNames.firstOrNull() ?: FaceModelRegistry.INPUT_NAME
            currentSession.run(mapOf(inputName to tensor)).use { results ->
                val onnxTensor = runCatching { results.get(0) as? OnnxTensor }.getOrNull()
                    ?: (results.firstOrNull()?.value as? OnnxTensor)

                val tensorData = onnxTensor?.value
                val rawVector: FloatArray? = when (tensorData) {
                    is Array<*> -> {
                        val firstElem = tensorData.firstOrNull()
                        when (firstElem) {
                            is FloatArray -> firstElem
                            is Array<*> -> firstElem.firstOrNull() as? FloatArray
                            else -> null
                        }
                    }
                    is FloatArray -> tensorData
                    else -> {
                        onnxTensor?.floatBuffer?.let { buf ->
                            val readBuf = buf.asReadOnlyBuffer()
                            readBuf.rewind()
                            if (readBuf.remaining() == FaceModelRegistry.EMBEDDING_DIM) {
                                val floats = FloatArray(FaceModelRegistry.EMBEDDING_DIM)
                                readBuf.get(floats)
                                floats
                            } else null
                        }
                    }
                }
                if (rawVector != null && rawVector.size == FaceModelRegistry.EMBEDDING_DIM) {
                    normalizeL2(rawVector)
                } else {
                    Log.w(TAG, "Failed extracting embedding: data=${tensorData?.javaClass?.name}, size=${rawVector?.size}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}", e)
            null
        } finally {
            tensor.close()
        }
    }

    fun close() {
        synchronized(sessionLock) {
            closeLocked()
        }
    }

    private fun closeLocked() {
        try {
            faceSession?.close()
            faceSession = null
            // Never close OrtEnvironment.getEnvironment() — it is a process-wide native singleton.
            isLoaded = false
            reusablePixels = null
            reusableMirroredPixels = null
            reusableFloatData = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing face session: ${e.message}", e)
        }
    }
}
