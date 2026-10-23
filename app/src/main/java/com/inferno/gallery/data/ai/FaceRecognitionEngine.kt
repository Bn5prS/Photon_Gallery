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
            val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return false
            return abi == "arm64-v8a" || abi == "x86_64"
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
    }

    private val modelDirFile = File(context.filesDir, FaceModelRegistry.MODEL_DIR)
    private var env: OrtEnvironment? = null
    private var faceSession: OrtSession? = null
    private var isLoaded = false

    fun getModelFile(): File = File(modelDirFile, FaceModelRegistry.MODEL_NAME)

    fun isModelDownloaded(): Boolean {
        if (!isSupported()) return false
        val modelFile = getModelFile()
        return modelFile.exists() && modelFile.length() > 5 * 1024 * 1024L
    }

    @Synchronized
    fun loadModel() {
        if (isLoaded) return
        if (!isSupported()) return
        val modelFile = getModelFile()
        if (!modelFile.exists()) return

        try {
            val environment = OrtEnvironment.getEnvironment()
            this.env = environment
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
            }
            this.faceSession = environment.createSession(modelFile.absolutePath, options)
            isLoaded = true
            Log.d(TAG, "MobileFaceNet loaded successfully (${modelFile.length() / 1024} KB).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load MobileFaceNet: ${e.message}", e)
            close()
        }
    }

    @Synchronized
    fun extractEmbedding(aligned112: Bitmap): FloatArray? {
        if (!isLoaded) {
            loadModel()
            if (!isLoaded) return null
        }
        val currentEnv = env ?: return null
        val currentSession = faceSession ?: return null

        val width = aligned112.width
        val height = aligned112.height
        if (width != FaceNormalizer.CROP_SIZE || height != FaceNormalizer.CROP_SIZE) {
            Log.w(TAG, "Bitmap must be 112×112, got ${width}×${height}")
            return null
        }

        val pixels = IntArray(width * height)
        aligned112.getPixels(pixels, 0, width, 0, 0, width, height)

        val floatBuffer = FloatBuffer.allocate(1 * 3 * width * height)
        // Red channel
        for (p in pixels) floatBuffer.put(((p shr 16 and 0xFF) - 127.5f) / 128.0f)
        // Green channel
        for (p in pixels) floatBuffer.put(((p shr 8 and 0xFF) - 127.5f) / 128.0f)
        // Blue channel
        for (p in pixels) floatBuffer.put(((p and 0xFF) - 127.5f) / 128.0f)
        floatBuffer.rewind()

        val shape = longArrayOf(1, 3, width.toLong(), height.toLong())
        val tensor = OnnxTensor.createTensor(currentEnv, floatBuffer, shape)

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

    @Synchronized
    fun close() {
        try {
            faceSession?.close()
            faceSession = null
            env = null
            isLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Error closing face session: ${e.message}", e)
        }
    }
}
