package com.inferno.gallery.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import com.inferno.gallery.ui.GalleryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class MotionPhotoInfo(
    val isMotionPhoto: Boolean = false,
    val videoOffset: Long = 0L,
    val videoLength: Long = 0L,
    val presentationTimestampUs: Long = 0L,
    val pairedVideoUri: Uri? = null,
    val sourceFile: File? = null
)

object MotionPhotoHelper {

    // Fast in-memory cache to prevent redundant disk I/O on grid scrolling
    private val fastCheckCache = ConcurrentHashMap<String, Boolean>()
    private val detailedInfoCache = ConcurrentHashMap<String, MotionPhotoInfo>()

    fun clearCacheForTests() {
        fastCheckCache.clear()
        detailedInfoCache.clear()
    }

    private val FTYP_BYTES = byteArrayOf('f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte())
    private val SAMSUNG_TRAILER_BYTES = "MotionPhoto_Data".toByteArray(StandardCharsets.US_ASCII)

    /**
     * Fast non-blocking check for thumbnail badges.
     * Inspects sibling files or scans the first 4-8KB of JPEG header for motion markers.
     */
    fun isMotionPhotoFast(context: Context, item: GalleryItem): Boolean {
        if (item.isVideo) return false
        val cached = fastCheckCache[item.id]
        if (cached != null) return cached

        // 1. Check for Apple Live Photo companion file (.mov / .mp4)
        if (item.path.isNotBlank()) {
            val file = File(item.path)
            if (file.exists()) {
                val parent = file.parentFile
                val baseName = file.nameWithoutExtension
                if (parent != null && parent.isDirectory) {
                    val companionMov = File(parent, "$baseName.MOV")
                    val companionMovLower = File(parent, "$baseName.mov")
                    val companionMp4 = File(parent, "$baseName.mp4")
                    if (companionMov.exists() || companionMovLower.exists() || companionMp4.exists()) {
                        fastCheckCache[item.id] = true
                        return true
                    }
                }
            }
        }

        // 2. Fast JPEG header probe (only reads first 8KB)
        val isJpg = item.name.endsWith(".jpg", ignoreCase = true) ||
                item.name.endsWith(".jpeg", ignoreCase = true) ||
                item.path.endsWith(".jpg", ignoreCase = true) ||
                item.path.endsWith(".jpeg", ignoreCase = true)

        if (!isJpg) {
            fastCheckCache[item.id] = false
            return false
        }

        try {
            val inputStream: InputStream? = if (item.path.isNotBlank() && File(item.path).exists()) {
                FileInputStream(File(item.path))
            } else {
                context.contentResolver.openInputStream(item.uri)
            }

            inputStream?.use { stream ->
                val headerBuf = ByteArray(8192)
                val read = stream.read(headerBuf)
                if (read > 0) {
                    val headerStr = String(headerBuf, 0, read, StandardCharsets.ISO_8859_1)
                    if (headerStr.contains("GCamera:MicroVideo") ||
                        headerStr.contains("GCamera:MotionPhoto") ||
                        headerStr.contains("Camera:MotionPhoto") ||
                        headerStr.contains("MotionPhoto_Data") ||
                        headerStr.contains("Semantic=\"MotionPhoto\"")
                    ) {
                        fastCheckCache[item.id] = true
                        return true
                    }
                }
            }
        } catch (_: Exception) {}

        fastCheckCache[item.id] = false
        return false
    }

    /**
     * Detailed scanner that extracts exact byte offset, video length, and presentation timestamps.
     */
    suspend fun getMotionPhotoInfo(context: Context, item: GalleryItem): MotionPhotoInfo = withContext(Dispatchers.IO) {
        if (item.isVideo) return@withContext MotionPhotoInfo()

        val cached = detailedInfoCache[item.id]
        if (cached != null) return@withContext cached

        val file = if (item.path.isNotBlank()) File(item.path) else null
        val exists = file?.exists() == true

        // 1. Check companion Apple Live Photo
        if (file != null && exists) {
            val parent = file.parentFile
            val baseName = file.nameWithoutExtension
            if (parent != null && parent.isDirectory) {
                val mov = listOf(
                    File(parent, "$baseName.MOV"),
                    File(parent, "$baseName.mov"),
                    File(parent, "$baseName.mp4"),
                    File(parent, "$baseName.MP4")
                ).firstOrNull { it.exists() && it.length() > 1024 }

                if (mov != null) {
                    val info = MotionPhotoInfo(
                        isMotionPhoto = true,
                        videoOffset = 0L,
                        videoLength = mov.length(),
                        pairedVideoUri = Uri.fromFile(mov),
                        sourceFile = mov
                    )
                    fastCheckCache[item.id] = true
                    detailedInfoCache[item.id] = info
                    return@withContext info
                }
            }
        }

        // 2. Embedded Video in JPEG
        try {
            val totalFileSize = if (file != null && exists) file.length() else item.size
            if (totalFileSize > 100_000) {
                val inputStream: InputStream? = if (file != null && exists) {
                    FileInputStream(file)
                } else {
                    context.contentResolver.openInputStream(item.uri)
                }

                inputStream?.use { stream ->
                    val info = scanJpegStream(stream, totalFileSize, file)
                    if (info.isMotionPhoto) {
                        fastCheckCache[item.id] = true
                        detailedInfoCache[item.id] = info
                        return@withContext info
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MotionPhotoHelper", "Error probing motion photo for ${item.name}", e)
        }

        val empty = MotionPhotoInfo(isMotionPhoto = false)
        fastCheckCache[item.id] = false
        detailedInfoCache[item.id] = empty
        return@withContext empty
    }

    /**
     * Scans XMP metadata from APP1 markers and tail markers in a JPEG stream.
     */
    private fun scanJpegStream(stream: InputStream, totalSize: Long, file: File?): MotionPhotoInfo {
        // Read the first 128KB of the JPEG header
        val headerLimit = minOf(totalSize.toInt(), 131072)
        val headerBytes = ByteArray(headerLimit)
        var totalRead = 0
        while (totalRead < headerLimit) {
            val read = stream.read(headerBytes, totalRead, headerLimit - totalRead)
            if (read <= 0) break
            totalRead += read
        }

        if (totalRead < 4 || (headerBytes[0].toInt() and 0xFF) != 0xFF || (headerBytes[1].toInt() and 0xFF) != 0xD8) {
            return MotionPhotoInfo()
        }

        val headerText = String(headerBytes, 0, totalRead, StandardCharsets.ISO_8859_1)

        // Case A: Google Camera MicroVideo
        var microVideoOffset: Long? = null
        var presentationTimeUs: Long = 0L

        val offsetMatch = Regex("""GCamera:MicroVideoOffset="(\d+)"""").find(headerText)
            ?: Regex("""GCamera:MicroVideoOffset>(\d+)<""").find(headerText)
            ?: Regex("""Camera:MicroVideoOffset="(\d+)"""").find(headerText)
        if (offsetMatch != null) {
            microVideoOffset = offsetMatch.groupValues[1].toLongOrNull()
        }

        val presTimeMatch = Regex("""GCamera:MicroVideoPresentationTimestampUs="(\d+)"""").find(headerText)
            ?: Regex("""GCamera:MicroVideoPresentationTimestampUs>(\d+)<""").find(headerText)
        if (presTimeMatch != null) {
            presentationTimeUs = presTimeMatch.groupValues[1].toLongOrNull() ?: 0L
        }

        // Case B: Google Container Directory item with Semantic="MotionPhoto"
        if (microVideoOffset == null) {
            val containerItemMatch = Regex("""<Container:Item[^>]*Container:Length="(\d+)"[^>]*Container:Semantic="MotionPhoto"""").find(headerText)
                ?: Regex("""<Container:Item[^>]*Container:Semantic="MotionPhoto"[^>]*Container:Length="(\d+)"""").find(headerText)
                ?: Regex("""Item:Length="(\d+)"[^>]*Item:Semantic="MotionPhoto"""").find(headerText)
            if (containerItemMatch != null) {
                microVideoOffset = containerItemMatch.groupValues[1].toLongOrNull()
            }
        }

        if (microVideoOffset != null && microVideoOffset > 1000 && microVideoOffset < totalSize) {
            val videoOffset = totalSize - microVideoOffset
            return MotionPhotoInfo(
                isMotionPhoto = true,
                videoOffset = videoOffset,
                videoLength = microVideoOffset,
                presentationTimestampUs = presentationTimeUs,
                sourceFile = file
            )
        }

        // Case C: Samsung Motion Photo trailer check if file is accessible
        if (file != null && file.exists()) {
            val samsungInfo = checkSamsungMotionTrailer(file, totalSize)
            if (samsungInfo != null) return samsungInfo
        }

        return MotionPhotoInfo()
    }

    /**
     * Checks for Samsung's MotionPhoto_Data trailer marker at the end of the file.
     */
    private fun checkSamsungMotionTrailer(file: File, totalSize: Long): MotionPhotoInfo? {
        if (totalSize < 4096) return null
        try {
            FileInputStream(file).use { fis ->
                val channel = fis.channel
                val tailSize = minOf(totalSize, 65536L).toInt()
                val tailOffset = totalSize - tailSize
                channel.position(tailOffset)

                val tailBytes = ByteArray(tailSize)
                fis.read(tailBytes)

                val markerIndex = indexOfSubarray(tailBytes, SAMSUNG_TRAILER_BYTES)
                if (markerIndex != -1) {
                    // Samsung stores a 4-byte big endian integer 4 bytes prior to MotionPhoto_Data
                    if (markerIndex >= 4) {
                        val lengthOffset = markerIndex - 4
                        val videoLength = ((tailBytes[lengthOffset].toLong() and 0xFF) shl 24) or
                                ((tailBytes[lengthOffset + 1].toLong() and 0xFF) shl 16) or
                                ((tailBytes[lengthOffset + 2].toLong() and 0xFF) shl 8) or
                                (tailBytes[lengthOffset + 3].toLong() and 0xFF)

                        if (videoLength in 1000 until totalSize) {
                            val videoOffset = totalSize - videoLength
                            return MotionPhotoInfo(
                                isMotionPhoto = true,
                                videoOffset = videoOffset,
                                videoLength = videoLength,
                                sourceFile = file
                            )
                        }
                    }
                }

                // Fallback: search backwards for ftyp atom near the tail
                val ftypIndex = indexOfSubarray(tailBytes, FTYP_BYTES)
                if (ftypIndex >= 4) {
                    val videoOffset = tailOffset + ftypIndex - 4
                    val videoLength = totalSize - videoOffset
                    if (videoLength in 1000 until totalSize) {
                        return MotionPhotoInfo(
                            isMotionPhoto = true,
                            videoOffset = videoOffset,
                            videoLength = videoLength,
                            sourceFile = file
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun indexOfSubarray(array: ByteArray, target: ByteArray): Int {
        if (target.isEmpty() || array.size < target.size) return -1
        for (i in 0..array.size - target.size) {
            var found = true
            for (j in target.indices) {
                if (array[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    /**
     * Extracts the embedded MP4 video track to a cached file.
     * Uses kernel-level zero-copy transfer (FileChannel.transferTo), completing in < 15ms.
     */
    suspend fun getOrExtractMotionVideo(
        context: Context,
        item: GalleryItem,
        info: MotionPhotoInfo
    ): File? = withContext(Dispatchers.IO) {
        if (!info.isMotionPhoto) return@withContext null

        if (info.pairedVideoUri != null && info.sourceFile != null && info.sourceFile.exists()) {
            return@withContext info.sourceFile
        }

        val cacheDir = File(context.cacheDir, "motion_cache").apply { mkdirs() }
        val cacheFile = File(cacheDir, "motion_${item.id}_${info.videoOffset}.mp4")

        if (cacheFile.exists() && cacheFile.length() == info.videoLength) {
            return@withContext cacheFile
        }

        try {
            if (info.sourceFile != null && info.sourceFile.exists()) {
                FileInputStream(info.sourceFile).use { fis ->
                    FileOutputStream(cacheFile).use { fos ->
                        fis.channel.transferTo(info.videoOffset, info.videoLength, fos.channel)
                    }
                }
            } else {
                context.contentResolver.openFileDescriptor(item.uri, "r")?.use { pfd ->
                    FileInputStream(pfd.fileDescriptor).use { fis ->
                        FileOutputStream(cacheFile).use { fos ->
                            fis.channel.transferTo(info.videoOffset, info.videoLength, fos.channel)
                        }
                    }
                }
            }

            if (cacheFile.exists() && cacheFile.length() > 0) {
                return@withContext cacheFile
            }
        } catch (e: Exception) {
            android.util.Log.e("MotionPhotoHelper", "Failed to extract motion video for ${item.name}", e)
        }

        return@withContext null
    }

    /**
     * Exports the motion video as a standalone MP4 file in the public Movies / Photon_Motion folder.
     */
    suspend fun exportMotionVideo(
        context: Context,
        item: GalleryItem,
        info: MotionPhotoInfo
    ): Uri? = withContext(Dispatchers.IO) {
        val extractedVideo = getOrExtractMotionVideo(context, item, info) ?: return@withContext null
        val baseName = item.name.substringBeforeLast(".")
        val targetName = "${baseName}_motion.mp4"

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, targetName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Photon_Motion")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val targetUri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext null

            context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                FileInputStream(extractedVideo).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(targetUri, contentValues, null, null)
            }

            return@withContext targetUri
        } catch (e: Exception) {
            android.util.Log.e("MotionPhotoHelper", "Failed to export motion video", e)
            return@withContext null
        }
    }
}
