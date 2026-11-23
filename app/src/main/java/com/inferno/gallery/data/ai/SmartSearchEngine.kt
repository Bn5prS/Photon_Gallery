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
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt
import android.os.Build

/**
 * Thread-safe lifecycle gate for native resources (such as ONNX Runtime [OrtSession]s) that must
 * never be closed concurrently with an in-flight native call or while another worker holds a lease.
 */
internal class RefCountedResourceGate<T : Any> {
    private val lock = Any()
    private var resource: T? = null
    private var activeLeases: Int = 0

    fun isLoaded(): Boolean = synchronized(lock) { resource != null }

    fun activeLeaseCount(): Int = synchronized(lock) { activeLeases }

    fun ensureLoaded(loader: () -> T): T = synchronized(lock) {
        resource ?: loader().also { resource = it }
    }

    fun acquire(loader: () -> T): T = synchronized(lock) {
        val loaded = resource ?: loader().also { resource = it }
        activeLeases++
        loaded
    }

    fun release(closer: (T) -> Unit, onClosed: () -> Unit = {}) = synchronized(lock) {
        if (activeLeases > 0) {
            activeLeases--
        }
        if (activeLeases == 0) {
            val current = resource
            resource = null
            try {
                if (current != null) closer(current)
            } finally {
                onClosed()
            }
        }
    }

    fun <R> withResource(loader: () -> T, block: (T) -> R): R = synchronized(lock) {
        val active = resource ?: loader().also { resource = it }
        block(active)
    }

    fun unloadIfUnused(closer: (T) -> Unit, onClosed: () -> Unit = {}): Boolean = synchronized(lock) {
        if (activeLeases > 0) return false
        val current = resource
        resource = null
        try {
            if (current != null) closer(current)
        } finally {
            onClosed()
        }
        true
    }

    fun forceClose(closer: (T) -> Unit, onClosed: () -> Unit = {}) = synchronized(lock) {
        activeLeases = 0
        val current = resource
        resource = null
        try {
            if (current != null) closer(current)
        } finally {
            onClosed()
        }
    }
}

class SmartSearchEngine(private val context: Context) {

    companion object {
        private const val TAG = "SmartSearchEngine"
        private const val MODEL_DIR = "smart_search_model_v7"
        private const val TEXT_MODEL_NAME = "text.onnx"
        private const val TEXT_DATA_NAME = "text.onnx.data"
        private const val VISION_MODEL_NAME = "visual.onnx"
        private const val VISION_DATA_NAME = "visual.onnx.data"
        private const val TOKENIZER_NAME = "tokenizer.json"
        const val IMAGE_SIZE = 256
        const val EMBEDDING_DIM = 512

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

        data class WeightedPrompt(val text: String, val weight: Float)

        private val WHITESPACE_REGEX = Regex("\\s+")

        private val DOCUMENT_OR_DIGITAL_REGEX = Regex(
            """\b(screenshot|screenshots|screen\s*shot|receipt|bill|invoice|document|certificate|menu|ticket|card|id|license|licence|aadhaar|aadhar|pan|passport|qr|barcode|note|notes|chat|message)\b""",
            RegexOption.IGNORE_CASE
        )

        private val PHRASE_EXPANSIONS = listOf(
            Regex("""\b(phone\s*screen\s*shot(s)?|screen\s*shot(s)?|screenshots?)\b""", RegexOption.IGNORE_CASE) to "phone screenshot screen capture interface",
            Regex("""\b(mobile\s*phone|cell\s*phone|cellphone|mobile)\b""", RegexOption.IGNORE_CASE) to "smartphone mobile phone",
            Regex("""\b(iron\s*box|ironbox)\b""", RegexOption.IGNORE_CASE) to "clothes iron steam iron",
            Regex("""\b(fridge)\b""", RegexOption.IGNORE_CASE) to "refrigerator fridge",
            Regex("""\b(cooling\s*glass(es)?|specs|spectacles)\b""", RegexOption.IGNORE_CASE) to "eyeglasses sunglasses spectacles",
            Regex("""\b(scooty|moped)\b""", RegexOption.IGNORE_CASE) to "motor scooter moped",
            Regex("""\b(ac)\b""", RegexOption.IGNORE_CASE) to "air conditioner",
            Regex("""\b(autorickshaw|rickshaw)\b""", RegexOption.IGNORE_CASE) to "auto rickshaw",
            Regex("""\b(chappal(s)?|slippers)\b""", RegexOption.IGNORE_CASE) to "slippers sandals footwear",
            Regex("""\b(doggie|doggy)\b""", RegexOption.IGNORE_CASE) to "dog puppy",
            Regex("""\b(kitty)\b""", RegexOption.IGNORE_CASE) to "cat kitten",
            Regex("""\b(aadhaar\s*card|aadhar\s*card|aadhaar|aadhar|pan\s*card|voter\s*id|ration\s*card)\b""", RegexOption.IGNORE_CASE) to "identity card government id document",
            Regex("""\b(driving\s*licen[sc]e|dl)\b""", RegexOption.IGNORE_CASE) to "driving license card id document",
            Regex("""\b(bill|receipt|invoice)\b""", RegexOption.IGNORE_CASE) to "printed receipt bill paper invoice",
            Regex("""\b(bike|two\s*wheeler)\b""", RegexOption.IGNORE_CASE) to "motorcycle motorbike",
            Regex("""\b(gadi|gaadi)\b""", RegexOption.IGNORE_CASE) to "automobile car passenger vehicle",
            Regex("""\b(notebook\s*pc|laptop)\b""", RegexOption.IGNORE_CASE) to "laptop computer keyboard",
            Regex("""\b(earphones|earbuds|airpods|headset)\b""", RegexOption.IGNORE_CASE) to "headphones earbuds audio device",
            Regex("""\b(data\s*cable|charger|adapter)\b""", RegexOption.IGNORE_CASE) to "phone charger power adapter cable",
            Regex("""\b(selfie|selfies)\b""", RegexOption.IGNORE_CASE) to "selfie portrait front camera photo",
            Regex("""\b(baby|infant|toddler)\b""", RegexOption.IGNORE_CASE) to "cute baby infant child",
            Regex("""\b(biryani|biriyani)\b""", RegexOption.IGNORE_CASE) to "spiced rice biryani dish food",
            Regex("""\b(identity\s*card|id\s*card)\b""", RegexOption.IGNORE_CASE) to "identity card badge document",
            Regex("""\b(bday|b'day)\b""", RegexOption.IGNORE_CASE) to "birthday celebration cake",
            Regex("""\b(xmas)\b""", RegexOption.IGNORE_CASE) to "christmas holiday tree"
        )

        fun normalizeQuery(query: String): String {
            val trimmed = query.trim().replace(WHITESPACE_REGEX, " ")
            if (trimmed.isEmpty()) return ""

            // Single-pass placeholder substitution avoids recursive expansion when one replacement
            // happens to contain keywords matched by a later expansion rule (e.g. "pan card").
            var working = trimmed
            val replacements = ArrayList<String>(4)
            for ((regex, replacement) in PHRASE_EXPANSIONS) {
                working = regex.replace(working) {
                    val token = "\u0000EXP_${replacements.size}\u0000"
                    replacements.add(replacement)
                    token
                }
            }
            for (i in replacements.indices) {
                working = working.replace("\u0000EXP_$i\u0000", replacements[i])
            }
            return working.replace(WHITESPACE_REGEX, " ").trim()
        }

        /**
         * Builds a compact, gallery-aware CLIP prompt ensemble (at most 4 prompts) for zero-shot
         * retrieval. Multi-template ensembling stabilizes cosine similarity without invalidating
         * already-indexed image embeddings on disk.
         */
        fun buildWeightedPrompts(rawQuery: String): List<WeightedPrompt> {
            val rawClean = rawQuery.trim().replace(WHITESPACE_REGEX, " ")
            if (rawClean.isEmpty()) return emptyList()

            val expandedClean = normalizeQuery(rawClean)
            val wordCount = rawClean.split(' ').count { it.isNotBlank() }
            val isDocumentOrDigital = DOCUMENT_OR_DIGITAL_REGEX.containsMatchIn(rawClean)

            val rawCandidates = ArrayList<WeightedPrompt>(4)
            if (!expandedClean.equals(rawClean, ignoreCase = true)) {
                rawCandidates += WeightedPrompt("a photo of $expandedClean", 1.4f)
                rawCandidates += WeightedPrompt(expandedClean, 1.0f)
                if (isDocumentOrDigital) {
                    rawCandidates += WeightedPrompt("a clear image of $expandedClean", 0.8f)
                }
                rawCandidates += WeightedPrompt("a photo of $rawClean", 0.9f)
            } else if (wordCount >= 4) {
                rawCandidates += WeightedPrompt(rawClean, 1.6f)
                rawCandidates += WeightedPrompt("a photo of $rawClean", 1.1f)
            } else if (isDocumentOrDigital) {
                rawCandidates += WeightedPrompt("a photo of $rawClean", 1.4f)
                rawCandidates += WeightedPrompt(rawClean, 1.2f)
                rawCandidates += WeightedPrompt("a clear image of $rawClean", 0.9f)
            } else {
                rawCandidates += WeightedPrompt("a photo of $rawClean", 1.5f)
                rawCandidates += WeightedPrompt(rawClean, 1.1f)
                rawCandidates += WeightedPrompt("a good photo of $rawClean", 0.8f)
            }

            val deduped = LinkedHashMap<String, WeightedPrompt>(rawCandidates.size)
            for (candidate in rawCandidates) {
                val key = candidate.text.lowercase(Locale.ROOT)
                val existing = deduped[key]
                if (existing == null || candidate.weight > existing.weight) {
                    deduped[key] = candidate
                }
            }
            return deduped.values.take(4)
        }
    }

    private val modelDirFile = File(context.filesDir, MODEL_DIR)
    @Volatile
    private var env: OrtEnvironment? = null
    private val textSessionGate = RefCountedResourceGate<OrtSession>()
    private val visionSessionGate = RefCountedResourceGate<OrtSession>()
    private val tokenizerLock = Any()
    @Volatile
    private var tokenizer: BPETokenizer? = null
    private val queryVectorCache = android.util.LruCache<String, FloatArray>(64)
    private var reusablePixels: IntArray? = null
    private var reusableFloatBuffer: FloatArray? = null

    fun getModelDir(): File = modelDirFile

    @Volatile
    private var hasCheckedBundledAssets = false
    @Volatile
    private var bundledAssetsAvailable = false

    fun isModelDownloaded(): Boolean {
        if (!isSupported()) return false
        val textFile = File(modelDirFile, TEXT_MODEL_NAME)
        val textDataFile = File(modelDirFile, TEXT_DATA_NAME)
        val visionFile = File(modelDirFile, VISION_MODEL_NAME)
        val visionDataFile = File(modelDirFile, VISION_DATA_NAME)
        val tokenizerFile = File(modelDirFile, TOKENIZER_NAME)

        val filesExist = textFile.exists() && textFile.length() > 500 * 1024L &&
               textDataFile.exists() && textDataFile.length() > 100 * 1024 * 1024L &&
               visionFile.exists() && visionFile.length() > 500 * 1024L &&
               visionDataFile.exists() && visionDataFile.length() > 50 * 1024 * 1024L &&
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

    private fun createCpuSessionOptions(intraOpThreads: Int): OrtSession.SessionOptions {
        return OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
            setIntraOpNumThreads(intraOpThreads)
            // Disable busy-wait spinning on mobile big.LITTLE CPUs to reduce thermal throttling & battery drain
            addConfigEntry("session.intra_op.allow_spinning", "0")
        }
    }

    private fun ensurePrerequisites(): OrtEnvironment {
        if (!isSupported()) {
            throw IllegalStateException("Smart Search requires 64-bit arm64-v8a or x86_64 architecture.")
        }
        if (!isModelDownloaded()) {
            throw IllegalStateException("Model files are not fully downloaded yet")
        }
        return env ?: synchronized(this) {
            env ?: OrtEnvironment.getEnvironment().also { env = it }
        }
    }

    private fun ensureTokenizerLoaded(): BPETokenizer {
        tokenizer?.let { return it }
        return synchronized(tokenizerLock) {
            tokenizer ?: run {
                val tokenizerFile = File(modelDirFile, TOKENIZER_NAME)
                BPETokenizer(tokenizerFile.readText(Charsets.UTF_8)).also { tokenizer = it }
            }
        }
    }

    fun prewarmTokenizer() {
        if (tokenizer != null || !isModelDownloaded()) return
        synchronized(tokenizerLock) {
            if (tokenizer != null) return
            val tokenizerFile = File(modelDirFile, TOKENIZER_NAME)
            if (tokenizerFile.exists()) {
                tokenizer = BPETokenizer(tokenizerFile.readText(Charsets.UTF_8))
                Log.d(TAG, "MobileCLIP2-S2 tokenizer pre-warmed.")
            }
        }
    }

    private fun createTextSession(): OrtSession {
        val environment = ensurePrerequisites()
        ensureTokenizerLoaded()
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val threads = cores.coerceIn(2, 4)
        val textFile = File(modelDirFile, TEXT_MODEL_NAME)
        return createCpuSessionOptions(threads).use { textOptions ->
            environment.createSession(textFile.absolutePath, textOptions)
        }.also {
            Log.d(TAG, "MobileCLIP2-S2 text model loaded successfully.")
        }
    }

    private fun createVisionSession(): OrtSession {
        val environment = ensurePrerequisites()
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        // Use half the logical cores (2..4) during background indexing to avoid UI starvation & thermal throttling
        val threads = (cores / 2).coerceIn(2, 4)
        val visionFile = File(modelDirFile, VISION_MODEL_NAME)
        return createCpuSessionOptions(threads).use { visionOptions ->
            environment.createSession(visionFile.absolutePath, visionOptions)
        }.also {
            Log.d(TAG, "SigLIP 2 vision model loaded successfully ($threads CPU threads).")
        }
    }

    fun loadTextModel() {
        try {
            ensureTokenizerLoaded()
            textSessionGate.ensureLoaded(::createTextSession)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load MobileCLIP2-S2 text model: ${e.message}", e)
            throw e
        }
    }

    fun loadVisionModel() {
        try {
            visionSessionGate.ensureLoaded(::createVisionSession)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load SigLIP 2 vision model: ${e.message}", e)
            throw e
        }
    }

    /**
     * Acquires a counted lease on the shared vision [OrtSession], loading it if needed.
     * Pair with [releaseVisionModel] in a `finally` block so overlapping WorkManager retries or
     * replacements cannot destroy the native session while another worker is inside `OrtSession.run`.
     */
    fun acquireVisionModel() {
        try {
            visionSessionGate.acquire(::createVisionSession)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire SigLIP 2 vision model: ${e.message}", e)
            throw e
        }
    }

    fun releaseVisionModel() {
        try {
            visionSessionGate.release(
                closer = { session ->
                    session.close()
                    Log.d(TAG, "SigLIP 2 vision model unloaded to free RAM.")
                },
                onClosed = {
                    reusablePixels = null
                    reusableFloatBuffer = null
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing vision model: ${e.message}", e)
        }
    }

    fun unloadVisionModel() {
        try {
            visionSessionGate.unloadIfUnused(
                closer = { session ->
                    session.close()
                    Log.d(TAG, "SigLIP 2 vision model unloaded to free RAM.")
                },
                onClosed = {
                    reusablePixels = null
                    reusableFloatBuffer = null
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading vision model: ${e.message}", e)
        }
    }

    fun loadModel() {
        if (!isSupported()) {
            Log.w(TAG, "Smart Search is not supported on this device architecture (${Build.SUPPORTED_ABIS.firstOrNull()}). Requires arm64-v8a.")
            return
        }
        loadTextModel()
        loadVisionModel()
    }

    fun close() {
        try {
            synchronized(queryVectorCache) {
                queryVectorCache.evictAll()
            }
            textSessionGate.forceClose(
                closer = { it.close() },
                onClosed = {
                    synchronized(tokenizerLock) {
                        tokenizer = null
                    }
                }
            )
            visionSessionGate.forceClose(
                closer = { it.close() },
                onClosed = {
                    reusablePixels = null
                    reusableFloatBuffer = null
                }
            )
            // Never call OrtEnvironment.close(): OrtEnvironment.getEnvironment() is a process-wide
            // native singleton shared with FaceRecognitionEngine; destroying it causes native
            // FORTIFY destroyed-mutex aborts in concurrent or subsequent ONNX Runtime sessions.
            Log.d(TAG, "SmartSearchEngine closed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing SmartSearchEngine: ${e.message}", e)
        }
    }

    fun isLoaded(): Boolean = textSessionGate.isLoaded() && visionSessionGate.isLoaded()

    fun encodeText(query: String): FloatArray {
        val cleanQuery = query.trim().replace(WHITESPACE_REGEX, " ")
        if (cleanQuery.isEmpty()) return FloatArray(EMBEDDING_DIM)

        val cacheKey = cleanQuery.lowercase(Locale.ROOT)
        synchronized(queryVectorCache) {
            val cached = queryVectorCache.get(cacheKey)
            if (cached != null) return cached
        }

        val prompts = buildWeightedPrompts(cleanQuery)
        if (prompts.isEmpty()) return FloatArray(EMBEDDING_DIM)

        val normalized = if (prompts.size == 1) {
            encodeSingleText(prompts[0].text)
        } else {
            val weightedSum = FloatArray(EMBEDDING_DIM)
            for (prompt in prompts) {
                val vec = encodeSingleText(prompt.text)
                val limit = minOf(EMBEDDING_DIM, vec.size)
                for (i in 0 until limit) {
                    weightedSum[i] += prompt.weight * vec[i]
                }
            }
            l2Normalize(weightedSum)
        }

        synchronized(queryVectorCache) {
            queryVectorCache.put(cacheKey, normalized)
        }
        return normalized
    }

    private fun extractEmbeddingOutput(result: OrtSession.Result): FloatArray {
        val value = result[0].value
        return when (value) {
            is Array<*> -> {
                val firstElem = value[0]
                when (firstElem) {
                    is FloatArray -> firstElem
                    is Array<*> -> firstElem[0] as FloatArray
                    else -> throw IllegalStateException("Unexpected element type: ${firstElem?.javaClass}")
                }
            }
            is FloatArray -> value
            else -> throw IllegalStateException("Unexpected embedding output type: ${value?.javaClass}")
        }
    }

    private fun encodeSingleText(text: String): FloatArray {
        val currentEnv = ensurePrerequisites()
        val tokenIds = ensureTokenizerLoaded().encode(text)
        val shape = longArrayOf(1, 77)

        return textSessionGate.withResource(::createTextSession) { currentTextSession ->
            val inputName = currentTextSession.inputNames.firstOrNull() ?: "input_ids"
            val inputIdsTensor = OnnxTensor.createTensor(currentEnv, LongBuffer.wrap(tokenIds), shape)
            try {
                currentTextSession.run(mapOf(inputName to inputIdsTensor)).use { result ->
                    l2Normalize(extractEmbeddingOutput(result))
                }
            } finally {
                inputIdsTensor.close()
            }
        }
    }

    fun encodeImage(bitmap: Bitmap): FloatArray {
        if (bitmap.isRecycled) return FloatArray(EMBEDDING_DIM)
        val currentEnv = ensurePrerequisites()

        val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return FloatArray(EMBEDDING_DIM)
        } else {
            bitmap
        }

        val resizedBitmap = prepareCenterCropInputBitmap(softwareBitmap, IMAGE_SIZE)

        return try {
            runVisionInference(resizedBitmap, currentEnv)
        } finally {
            if (resizedBitmap != softwareBitmap && resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            if (softwareBitmap != bitmap) {
                softwareBitmap.recycle()
            }
        }
    }

    private fun runVisionInference(
        inputBitmap: Bitmap,
        environment: OrtEnvironment
    ): FloatArray {
        val planeSize = IMAGE_SIZE * IMAGE_SIZE
        val shape = longArrayOf(1, 3, IMAGE_SIZE.toLong(), IMAGE_SIZE.toLong())

        return visionSessionGate.withResource(::createVisionSession) { session ->
            val inputName = session.inputNames.firstOrNull() ?: "pixel_values"
            val pixels = reusablePixels ?: IntArray(planeSize).also { reusablePixels = it }
            val floatBuffer = reusableFloatBuffer ?: FloatArray(3 * planeSize).also { reusableFloatBuffer = it }

            inputBitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)

            // MobileCLIP2-S2 OpenCLIP preprocessing: mean=[0,0,0], std=[1,1,1] -> pixel / 255.0
            for (i in 0 until planeSize) {
                val pixel = pixels[i]
                floatBuffer[i] = ((pixel shr 16) and 0xFF) / 255.0f
                floatBuffer[i + planeSize] = ((pixel shr 8) and 0xFF) / 255.0f
                floatBuffer[i + 2 * planeSize] = (pixel and 0xFF) / 255.0f
            }

            val imageTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(floatBuffer), shape)
            try {
                session.run(mapOf(inputName to imageTensor)).use { result ->
                    l2Normalize(extractEmbeddingOutput(result))
                }
            } finally {
                imageTensor.close()
            }
        }
    }

    /**
     * Canonical aspect-preserving center crop for MobileCLIP-S2.
     * Scales image so min(width, height) = targetSize, then extracts central targetSize x targetSize square.
     * Prevents artificial black letterbox borders that distort vision transformer attention.
     */
    fun prepareCenterCropInputBitmap(bitmap: Bitmap, targetSize: Int = IMAGE_SIZE): Bitmap {
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

    fun prepareLetterboxInputBitmap(bitmap: Bitmap, targetSize: Int = IMAGE_SIZE): Bitmap {
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
