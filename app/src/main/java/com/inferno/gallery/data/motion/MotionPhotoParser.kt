package com.inferno.gallery.data.motion

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.regex.Pattern

data class MotionPhotoInfo(
    val isMotionPhoto: Boolean,
    val videoOffsetFromStart: Long,
    val videoLength: Long,
    val presentationTimestampUs: Long = 0L
)

object MotionPhotoParser {

    private val GOOGLE_OFFSET_REGEX = Pattern.compile("GCamera:MicroVideoOffset=\"(\\d+)\"")
    private val GOOGLE_TIMESTAMP_REGEX = Pattern.compile("GCamera:MicroVideoPresentationTimestampUs=\"(\\d+)\"")
    private val GOOGLE_MOTION_PHOTO_REGEX = Pattern.compile("GCamera:(?:MotionPhoto|MicroVideo)=\"1\"")
    private val SAMSUNG_MARKER = "MotionPhoto_Data".toByteArray(Charsets.US_ASCII)
    private val FTYP_BYTES = byteArrayOf('f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte())

    /**
     * Parse an input stream to detect if it contains a Motion Photo container.
     */
    fun parseMotionPhoto(inputStream: InputStream, fileLength: Long): MotionPhotoInfo? {
        if (fileLength <= 32) return null
        val bytes = inputStream.readBytes()
        val len = bytes.size.toLong()

        // 1. Check Samsung MotionPhoto_Data marker at the tail
        val samsungInfo = parseSamsungTrailer(bytes, len)
        if (samsungInfo != null) return samsungInfo

        // 2. Check Google Camera XMP MicroVideo metadata
        val googleInfo = parseGoogleXmp(bytes, len)
        if (googleInfo != null) return googleInfo

        // 3. Universal MP4 ftyp atom fallback
        return parseUniversalFtyp(bytes, len)
    }

    private fun parseSamsungTrailer(bytes: ByteArray, fileLength: Long): MotionPhotoInfo? {
        val searchStart = maxOf(0, bytes.size - 256 * 1024)
        val markerIndex = indexOf(bytes, SAMSUNG_MARKER, searchStart)
        if (markerIndex != -1 && markerIndex + SAMSUNG_MARKER.size + 4 <= bytes.size) {
            val lenOffset = markerIndex + SAMSUNG_MARKER.size
            val videoLength = (((bytes[lenOffset].toInt() and 0xFF) shl 24) or
                    ((bytes[lenOffset + 1].toInt() and 0xFF) shl 16) or
                    ((bytes[lenOffset + 2].toInt() and 0xFF) shl 8) or
                    (bytes[lenOffset + 3].toInt() and 0xFF)).toLong()

            if (videoLength > 0 && markerIndex - videoLength >= 0) {
                val videoOffset = (markerIndex - videoLength).toLong()
                return MotionPhotoInfo(
                    isMotionPhoto = true,
                    videoOffsetFromStart = videoOffset,
                    videoLength = videoLength,
                    presentationTimestampUs = 0L
                )
            }
        }
        return null
    }

    private fun parseGoogleXmp(bytes: ByteArray, fileLength: Long): MotionPhotoInfo? {
        // XMP header is typically in APP1 marker within the first 256KB
        val scanLen = minOf(bytes.size, 256 * 1024)
        val headerString = String(bytes, 0, scanLen, Charsets.ISO_8859_1)

        val isMotion = GOOGLE_MOTION_PHOTO_REGEX.matcher(headerString).find()
        if (isMotion) {
            val offsetMatcher = GOOGLE_OFFSET_REGEX.matcher(headerString)
            if (offsetMatcher.find()) {
                val offsetFromEnd = offsetMatcher.group(1)?.toLongOrNull() ?: 0L
                if (offsetFromEnd in 1 until fileLength) {
                    val tsMatcher = GOOGLE_TIMESTAMP_REGEX.matcher(headerString)
                    val ts = if (tsMatcher.find()) tsMatcher.group(1)?.toLongOrNull() ?: 0L else 0L
                    val videoOffset = fileLength - offsetFromEnd

                    return MotionPhotoInfo(
                        isMotionPhoto = true,
                        videoOffsetFromStart = videoOffset,
                        videoLength = offsetFromEnd,
                        presentationTimestampUs = ts
                    )
                }
            }
        }
        return null
    }

    private fun parseUniversalFtyp(bytes: ByteArray, fileLength: Long): MotionPhotoInfo? {
        // Look for the ftyp box in the file body (past JPEG SOI 0xFFD8)
        if (bytes.size < 16) return null
        val searchStart = 4
        val ftypIndex = indexOf(bytes, FTYP_BYTES, searchStart)
        if (ftypIndex >= 4) {
            val boxStart = ftypIndex - 4
            val videoLength = fileLength - boxStart
            if (videoLength >= 12) {
                return MotionPhotoInfo(
                    isMotionPhoto = true,
                    videoOffsetFromStart = boxStart.toLong(),
                    videoLength = videoLength,
                    presentationTimestampUs = 0L
                )
            }
        }
        return null
    }

    /**
     * Check cache or extract the motion video MP4 into a cached file on [Dispatchers.IO].
     */
    suspend fun getOrExtractMotionVideo(context: Context, uri: Uri, mediaId: Long): File? = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "motion_videos").apply { if (!exists()) mkdirs() }
        val targetFile = File(cacheDir, "${mediaId}.mp4")
        if (targetFile.exists() && targetFile.length() > 0) {
            return@withContext targetFile
        }

        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            val fileLength = pfd.statSize
            if (fileLength <= 1024) {
                pfd.close()
                return@withContext null
            }

            pfd.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).use { fis ->
                    val info = parseMotionPhoto(fis, fileLength) ?: return@withContext null
                    if (info.isMotionPhoto && info.videoLength > 0) {
                        // Re-open to seek directly to the video offset
                        FileInputStream(descriptor.fileDescriptor).use { seekStream ->
                            seekStream.channel.position(info.videoOffsetFromStart)
                            val tmpFile = File(cacheDir, "${mediaId}.mp4.tmp")
                            FileOutputStream(tmpFile).use { fos ->
                                val buffer = ByteArray(64 * 1024)
                                var remaining = info.videoLength
                                while (remaining > 0) {
                                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                                    val read = seekStream.read(buffer, 0, toRead)
                                    if (read <= 0) break
                                    fos.write(buffer, 0, read)
                                    remaining -= read
                                }
                            }
                            if (tmpFile.length() > 0) {
                                tmpFile.renameTo(targetFile)
                                return@withContext targetFile
                            } else {
                                tmpFile.delete()
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        null
    }

    private fun indexOf(source: ByteArray, target: ByteArray, startIndex: Int = 0): Int {
        if (target.isEmpty()) return 0
        val max = source.size - target.size
        for (i in startIndex..max) {
            var match = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }
}
