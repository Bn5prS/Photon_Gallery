package com.inferno.gallery.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.roundToInt
import kotlin.math.sqrt
import android.os.Build

class SmartSearchEngine(private val context: Context) {

    companion object {
        private const val TAG = "SmartSearchEngine"
        private const val MODEL_DIR = "smart_search_model_v3"
        private const val TEXT_MODEL_NAME = "text_model_quantized.onnx"
        private const val VISION_MODEL_NAME = "vision_model_quantized.onnx"
        private const val TOKENIZER_NAME = "tokenizer.json"

        @Volatile
        private var INSTANCE: SmartSearchEngine? = null

        /**
         * ONNX Runtime's libonnxruntime.so uses arm64-v8a SIMD instructions and
         * triggers SIGBUS (Bus error) on 32-bit armeabi-v7a devices.
         * Smart Search is only supported on 64-bit arm64 or x86_64 architectures.
         */
        fun isSupported(): Boolean {
            val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return false
            return abi == "arm64-v8a" || abi == "x86_64"
        }

        fun getInstance(context: Context): SmartSearchEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmartSearchEngine(context.applicationContext).also { INSTANCE = it }
            }
        }

        private val PHRASE_EXPANSIONS = listOf(
            Regex("""\b(iron\s*box|ironbox)\b""", RegexOption.IGNORE_CASE) to "clothes iron steam iron",
            Regex("""\b(fridge)\b""", RegexOption.IGNORE_CASE) to "refrigerator fridge",
            Regex("""\b(specs|spectacles|cooling\s*glass(es)?)\b""", RegexOption.IGNORE_CASE) to "eyeglasses sunglasses spectacles",
            Regex("""\b(scooty|moped)\b""", RegexOption.IGNORE_CASE) to "motor scooter moped",
            Regex("""\b(ac)\b""", RegexOption.IGNORE_CASE) to "air conditioner",
            Regex("""\b(autorickshaw|rickshaw)\b""", RegexOption.IGNORE_CASE) to "auto rickshaw",
            Regex("""\b(chappal(s)?|slippers)\b""", RegexOption.IGNORE_CASE) to "slippers sandals footwear",
            Regex("""\b(mobile|cellphone|cell\s*phone)\b""", RegexOption.IGNORE_CASE) to "smartphone mobile phone",
            Regex("""\b(doggie|doggy)\b""", RegexOption.IGNORE_CASE) to "dog puppy",
            Regex("""\b(kitty)\b""", RegexOption.IGNORE_CASE) to "cat kitten",
            Regex("""\b(aadhaar|aadhar|pan\s*card|voter\s*id|ration\s*card)\b""", RegexOption.IGNORE_CASE) to "identity card government id document",
            Regex("""\b(driving\s*licen[sc]e|dl)\b""", RegexOption.IGNORE_CASE) to "driving license card id document",
            Regex("""\b(bill|receipt|invoice)\b""", RegexOption.IGNORE_CASE) to "printed receipt bill paper invoice",
            Regex("""\b(screen\s*shot(s)?|screenshots?)\b""", RegexOption.IGNORE_CASE) to "phone screenshot screen capture interface",
            Regex("""\b(bike|two\s*wheeler)\b""", RegexOption.IGNORE_CASE) to "motorcycle motorbike",
            Regex("""\b(gadi|gaadi)\b""", RegexOption.IGNORE_CASE) to "automobile car passenger vehicle",
            Regex("""\b(laptop|notebook\s*pc)\b""", RegexOption.IGNORE_CASE) to "laptop computer keyboard",
            Regex("""\b(earphones|earbuds|airpods|headset)\b""", RegexOption.IGNORE_CASE) to "headphones earbuds audio device",
            Regex("""\b(charger|adapter|data\s*cable)\b""", RegexOption.IGNORE_CASE) to "phone charger power adapter cable",
            Regex("""\b(selfie|selfies)\b""", RegexOption.IGNORE_CASE) to "selfie portrait front camera photo",
            Regex("""\b(baby|infant|toddler)\b""", RegexOption.IGNORE_CASE) to "cute baby infant child",
            Regex("""\b(biryani|biriyani)\b""", RegexOption.IGNORE_CASE) to "spiced rice biryani dish food",
            Regex("""\b(id\s*card|identity\s*card)\b""", RegexOption.IGNORE_CASE) to "identity card badge document"
        )

        fun normalizeQuery(query: String): String {
            var q = query.trim()
            if (q.isEmpty()) return ""
            for ((regex, replacement) in PHRASE_EXPANSIONS) {
                if (regex.containsMatchIn(q)) {
                    q = regex.replace(q, replacement)
                }
            }
            return q
        }
    }

    private val modelDirFile = File(context.filesDir, MODEL_DIR)
    private var env: OrtEnvironment? = null
    private var textSession: OrtSession? = null
    private var visionSession: OrtSession? = null
    private var tokenizer: BPETokenizer? = null
    private var isLoaded = false
    private val queryVectorCache = android.util.LruCache<String, FloatArray>(64)

    fun getModelDir(): File = modelDirFile

    @Volatile
    private var hasCheckedBundledAssets = false
    @Volatile
    private var bundledAssetsAvailable = false

    fun isModelDownloaded(): Boolean {
        if (!isSupported()) return false
        val textFile = File(modelDirFile, TEXT_MODEL_NAME)
        val visionFile = File(modelDirFile, VISION_MODEL_NAME)
        val tokenizerFile = File(modelDirFile, TOKENIZER_NAME)
        
        val filesExist = textFile.exists() && textFile.length() > 10 * 1024 * 1024L &&
               visionFile.exists() && visionFile.length() > 20 * 1024 * 1024L &&
               tokenizerFile.exists() && tokenizerFile.length() > 100 * 1024L
               
        if (filesExist) return true
        
        // Check once if models are bundled in assets. If not, never attempt asset copying again.
        if (!hasCheckedBundledAssets) {
            hasCheckedBundledAssets = true
            bundledAssetsAvailable = try {
                val assetList = context.assets.list("models") ?: emptyArray()
                assetList.contains(TEXT_MODEL_NAME) && assetList.contains(VISION_MODEL_NAME)
            } catch (_: Exception) {
                false
            }
        }

        if (!bundledAssetsAvailable) return false

        // Fallback to bundled assets only if they exist in APK
        try {
            if (!modelDirFile.exists()) modelDirFile.mkdirs()
            
            val filesToCopy = listOf(TEXT_MODEL_NAME, VISION_MODEL_NAME, TOKENIZER_NAME)
            for (fileName in filesToCopy) {
                val targetFile = File(modelDirFile, fileName)
                if (!targetFile.exists() || targetFile.length() < 100 * 1024L) {
                    context.assets.open("models/$fileName").use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy models from assets", e)
        }
        
        return false
    }

    @Synchronized
    fun loadModel() {
        if (isLoaded) return
        if (!isSupported()) {
            Log.w(TAG, "Smart Search is not supported on this device architecture (${Build.SUPPORTED_ABIS.firstOrNull()}). Requires arm64-v8a.")
            return
        }
        if (!isModelDownloaded()) {
            throw IllegalStateException("Model files are not fully downloaded yet")
        }

        try {
            val environment = OrtEnvironment.getEnvironment()
            this.env = environment

            // Load Tokenizer
            val tokenizerFile = File(modelDirFile, TOKENIZER_NAME)
            this.tokenizer = BPETokenizer(tokenizerFile.readText(Charsets.UTF_8))

            // Initialize Text Session options (optimized for mobile CPU)
            val textFile = File(modelDirFile, TEXT_MODEL_NAME)
            OrtSession.SessionOptions().use { textOptions ->
                textOptions.setIntraOpNumThreads(4)
                this.textSession = environment.createSession(textFile.absolutePath, textOptions)
            }

            // Initialize Vision Session options (optimized for mobile CPU)
            val visionFile = File(modelDirFile, VISION_MODEL_NAME)
            OrtSession.SessionOptions().use { visionOptions ->
                visionOptions.setIntraOpNumThreads(4)
                this.visionSession = environment.createSession(visionFile.absolutePath, visionOptions)
            }

            isLoaded = true
            Log.d(TAG, "SmartSearchEngine models loaded successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load SmartSearchEngine models: ${e.message}", e)
            close()
            throw e
        }
    }

    @Synchronized
    fun close() {
        try {
            synchronized(queryVectorCache) {
                queryVectorCache.evictAll()
            }
            textSession?.close()
            textSession = null
            visionSession?.close()
            visionSession = null
            env?.close()
            env = null
            tokenizer = null
            isLoaded = false
            Log.d(TAG, "SmartSearchEngine closed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing SmartSearchEngine: ${e.message}", e)
        }
    }

    fun isLoaded(): Boolean = isLoaded

    fun encodeText(query: String): FloatArray {
        val trimmed = normalizeQuery(query)
        if (trimmed.isEmpty()) return FloatArray(512)

        synchronized(queryVectorCache) {
            val cached = queryVectorCache.get(trimmed)
            if (cached != null) return cached
        }

        if (trimmed.startsWith("a photo of", ignoreCase = true) ||
            trimmed.startsWith("a picture of", ignoreCase = true) ||
            trimmed.startsWith("photo of", ignoreCase = true)
        ) {
            val res = encodeSingleText(trimmed)
            synchronized(queryVectorCache) { queryVectorCache.put(trimmed, res) }
            return res
        }

        // Canonical CLIP multi-template prompt ensembling with category awareness
        val templates = mutableListOf(
            trimmed,
            "a photo of $trimmed",
            "a photo of the $trimmed",
            "a close-up photo of $trimmed"
        )

        val lower = trimmed.lowercase()
        when {
            // Nature, Landscapes, Weather
            lower.contains("sunset") || lower.contains("sunrise") || lower.contains("beach") ||
            lower.contains("mountain") || lower.contains("sky") || lower.contains("forest") ||
            lower.contains("waterfall") || lower.contains("sea") || lower.contains("ocean") ||
            lower.contains("river") || lower.contains("garden") || lower.contains("flowers") -> {
                templates.add("a beautiful scenic photo of $trimmed")
                templates.add("a wide landscape shot of $trimmed")
            }
            // Animals, Pets
            lower.contains("dog") || lower.contains("puppy") || lower.contains("cat") ||
            lower.contains("kitten") || lower.contains("bird") || lower.contains("pet") ||
            lower.contains("horse") || lower.contains("cow") || lower.contains("wildlife") -> {
                templates.add("a photo of a cute $trimmed")
                templates.add("a clear picture of the animal $trimmed")
            }
            // Food, Drinks
            lower.contains("food") || lower.contains("pizza") || lower.contains("burger") ||
            lower.contains("biryani") || lower.contains("cake") || lower.contains("coffee") ||
            lower.contains("tea") || lower.contains("dish") || lower.contains("meal") ||
            lower.contains("dessert") || lower.contains("snack") || lower.contains("breakfast") ||
            lower.contains("dinner") || lower.contains("lunch") -> {
                templates.add("a delicious photo of $trimmed food")
                templates.add("a plate of $trimmed")
            }
            // Documents, Bills, IDs, Cards
            lower.contains("receipt") || lower.contains("bill") || lower.contains("invoice") ||
            lower.contains("document") || lower.contains("card") || lower.contains("passport") ||
            lower.contains("license") || lower.contains("statement") || lower.contains("notes") ||
            lower.contains("ticket") -> {
                templates.add("a photo of a printed document $trimmed")
                templates.add("a clean scan or document of $trimmed")
            }
            // Vehicles, Transportation
            lower.contains("car") || lower.contains("bike") || lower.contains("motorcycle") ||
            lower.contains("scooter") || lower.contains("truck") || lower.contains("bus") ||
            lower.contains("train") || lower.contains("airplane") || lower.contains("vehicle") -> {
                templates.add("a photo of the vehicle $trimmed")
            }
        }

        val ensembled = FloatArray(512)
        for (tpl in templates) {
            val vec = encodeSingleText(tpl)
            for (i in 0 until 512) {
                ensembled[i] += vec[i]
            }
        }
        val normalized = l2Normalize(ensembled)
        synchronized(queryVectorCache) {
            queryVectorCache.put(trimmed, normalized)
        }
        return normalized
    }

    private fun encodeSingleText(text: String): FloatArray {
        if (!isLoaded) loadModel()
        val currentTokenizer = tokenizer ?: throw IllegalStateException("Tokenizer not loaded")
        val currentEnv = env ?: throw IllegalStateException("Environment not loaded")
        val currentTextSession = textSession ?: throw IllegalStateException("Text session not loaded")

        val tokenIds = currentTokenizer.encode(text)

        // Generate attention mask (1s for valid tokens, 0s for trailing padding)
        val attentionMask = LongArray(77)
        for (i in 0 until 77) {
            attentionMask[i] = if (tokenIds[i] != 0L) 1L else 0L
        }

        val shape = longArrayOf(1, 77)
        val inputNames = currentTextSession.inputNames
        val inputs = mutableMapOf<String, OnnxTensor>()

        val inputIdsTensor = OnnxTensor.createTensor(currentEnv, LongBuffer.wrap(tokenIds), shape)
        inputs["input_ids"] = inputIdsTensor

        var maskTensor: OnnxTensor? = null
        if (inputNames.contains("attention_mask")) {
            maskTensor = OnnxTensor.createTensor(currentEnv, LongBuffer.wrap(attentionMask), shape)
            inputs["attention_mask"] = maskTensor
        }

        try {
            currentTextSession.run(inputs).use { result ->
                val embedsObj = result[0].value
                val rawEmbeds = when (embedsObj) {
                    is Array<*> -> {
                        val firstElem = embedsObj[0]
                        when (firstElem) {
                            is FloatArray -> firstElem
                            is Array<*> -> firstElem[0] as FloatArray
                            else -> throw IllegalStateException("Unexpected element type: ${firstElem?.javaClass}")
                        }
                    }
                    else -> {
                        throw IllegalStateException("Unexpected output type from text model: ${embedsObj?.javaClass}")
                    }
                }
                return l2Normalize(rawEmbeds)
            }
        } finally {
            inputIdsTensor.close()
            maskTensor?.close()
        }
    }

    fun encodeImage(bitmap: Bitmap): FloatArray {
        if (bitmap.isRecycled) return FloatArray(512)
        if (!isLoaded) loadModel()
        val currentEnv = env ?: throw IllegalStateException("Environment not loaded")
        val currentVisionSession = visionSession ?: throw IllegalStateException("Vision session not loaded")

        // Ensure we have a software-backed bitmap to extract pixels without hardware buffer crashes
        val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return FloatArray(512)
        } else {
            bitmap
        }

        val aspect = softwareBitmap.width.toFloat() / softwareBitmap.height.toFloat()
        val isNonSquare = aspect < 0.82f || aspect > 1.22f

        val centerCropBitmap = prepareCenterCropInputBitmap(softwareBitmap, 224)
        val centerEmbedding = runVisionInference(centerCropBitmap, currentEnv, currentVisionSession)
        if (centerCropBitmap != softwareBitmap && centerCropBitmap != bitmap) {
            centerCropBitmap.recycle()
        }

        if (!isNonSquare) {
            if (softwareBitmap != bitmap) softwareBitmap.recycle()
            return l2Normalize(centerEmbedding)
        }

        // Dual-Crop Ensembling for non-square photos (e.g. 9:16 vertical portraits or 16:9 landscapes):
        // Blend 70% center crop (high-resolution central subject) + 30% full-aspect letterbox (global composition context)
        val letterboxBitmap = prepareLetterboxInputBitmap(softwareBitmap, 224)
        val letterboxEmbedding = runVisionInference(letterboxBitmap, currentEnv, currentVisionSession)
        if (letterboxBitmap != softwareBitmap && letterboxBitmap != bitmap) {
            letterboxBitmap.recycle()
        }
        if (softwareBitmap != bitmap) softwareBitmap.recycle()

        val blended = FloatArray(512) { i ->
            0.70f * centerEmbedding[i] + 0.30f * letterboxEmbedding[i]
        }
        return l2Normalize(blended)
    }

    private fun runVisionInference(
        inputBitmap: Bitmap,
        environment: OrtEnvironment,
        session: OrtSession
    ): FloatArray {
        // Preprocess pixels into RGB flat NCHW buffer for CLIP ViT-B/16 (224x224)
        val planeSize = 224 * 224
        val pixels = IntArray(planeSize)
        inputBitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224)

        val floatBuffer = FloatArray(3 * planeSize)
        for (i in 0 until planeSize) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            // Official OpenAI CLIP Image Normalization
            floatBuffer[i] = (r - 0.48145466f) / 0.26862954f
            floatBuffer[i + planeSize] = (g - 0.4578275f) / 0.26130258f
            floatBuffer[i + 2 * planeSize] = (b - 0.40821073f) / 0.2757771f
        }

        val shape = longArrayOf(1, 3, 224, 224)
        val imageTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(floatBuffer), shape)
        val inputs = mapOf("pixel_values" to imageTensor)

        try {
            session.run(inputs).use { result ->
                val embedsObj = result[0].value
                val rawEmbeds = when (embedsObj) {
                    is Array<*> -> {
                        val firstElem = embedsObj[0]
                        when (firstElem) {
                            is FloatArray -> firstElem
                            is Array<*> -> firstElem[0] as FloatArray
                            else -> throw IllegalStateException("Unexpected element type: ${firstElem?.javaClass}")
                        }
                    }
                    else -> {
                        throw IllegalStateException("Unexpected output type from vision model: ${embedsObj?.javaClass}")
                    }
                }
                return l2Normalize(rawEmbeds)
            }
        } finally {
            imageTensor.close()
        }
    }

    /**
     * Canonical aspect-preserving center crop for CLIP ViT-B/16.
     * Scales image so min(width, height) = targetSize, then extracts central targetSize x targetSize square.
     * Prevents artificial black letterbox borders that distort vision transformer attention.
     */
    fun prepareCenterCropInputBitmap(bitmap: Bitmap, targetSize: Int = 224): Bitmap {
        val sourceWidth = bitmap.width
        val sourceHeight = bitmap.height
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw IllegalArgumentException("Bitmap has invalid dimensions: ${sourceWidth}x$sourceHeight")
        }

        if (sourceWidth == targetSize && sourceHeight == targetSize) {
            return bitmap
        }

        val scale = targetSize.toFloat() / minOf(sourceWidth, sourceHeight).toFloat()
        val scaledWidth = maxOf(targetSize, (sourceWidth * scale).roundToInt())
        val scaledHeight = maxOf(targetSize, (sourceHeight * scale).roundToInt())

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        val xOffset = ((scaledWidth - targetSize) / 2).coerceIn(0, scaledWidth - targetSize)
        val yOffset = ((scaledHeight - targetSize) / 2).coerceIn(0, scaledHeight - targetSize)

        val output = Bitmap.createBitmap(scaledBitmap, xOffset, yOffset, targetSize, targetSize)

        if (scaledBitmap != bitmap && scaledBitmap != output) {
            scaledBitmap.recycle()
        }

        return output
    }

    fun prepareLetterboxInputBitmap(bitmap: Bitmap, targetSize: Int = 224): Bitmap {
        val sourceWidth = bitmap.width
        val sourceHeight = bitmap.height
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw IllegalArgumentException("Bitmap has invalid dimensions: ${sourceWidth}x$sourceHeight")
        }

        if (sourceWidth == targetSize && sourceHeight == targetSize) {
            return bitmap
        }

        val scale = targetSize.toFloat() / maxOf(sourceWidth, sourceHeight).toFloat()
        val scaledWidth = maxOf(1, (sourceWidth * scale).roundToInt())
        val scaledHeight = maxOf(1, (sourceHeight * scale).roundToInt())

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)

        val left = (targetSize - scaledWidth) / 2f
        val top = (targetSize - scaledHeight) / 2f
        canvas.drawBitmap(scaledBitmap, left, top, null)

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        return output
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumOfSquares = 0.0f
        for (v in vector) {
            sumOfSquares += v * v
        }
        val norm = sqrt(sumOfSquares)
        if (norm == 0.0f) return vector
        val result = FloatArray(vector.size)
        for (i in vector.indices) {
            result[i] = vector[i] / norm
        }
        return result
    }

    fun dotProduct(a: FloatArray, b: FloatArray): Float {
        var sum = 0.0f
        val size = a.size
        val limit = size - 7
        var i = 0
        while (i < limit) {
            sum += a[i] * b[i] +
                   a[i + 1] * b[i + 1] +
                   a[i + 2] * b[i + 2] +
                   a[i + 3] * b[i + 3] +
                   a[i + 4] * b[i + 4] +
                   a[i + 5] * b[i + 5] +
                   a[i + 6] * b[i + 6] +
                   a[i + 7] * b[i + 7]
            i += 8
        }
        while (i < size) {
            sum += a[i] * b[i]
            i++
        }
        return sum
    }
}
