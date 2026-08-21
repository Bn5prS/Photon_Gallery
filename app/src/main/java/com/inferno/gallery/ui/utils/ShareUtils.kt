package com.inferno.gallery.ui.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ShareUtils {

    fun resolveMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: run {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (!extension.isNullOrEmpty()) {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            } else null
        } ?: "image/*"
    }

    private fun getSharedMimeType(context: Context, uris: List<Uri>): String {
        if (uris.isEmpty()) return "image/*"
        val types = uris.map { resolveMimeType(context, it) }
        val allImages = types.all { it.startsWith("image/") }
        val allVideos = types.all { it.startsWith("video/") }

        return when {
            types.size == 1 -> types.first()
            allImages -> if (types.distinct().size == 1) types.first() else "image/*"
            allVideos -> if (types.distinct().size == 1) types.first() else "video/*"
            else -> "*/*"
        }
    }

    suspend fun shareMedia(
        context: Context,
        uris: List<Uri>,
        stripMetadata: Boolean
    ) {
        val validUris = uris.filterNotNull()
        if (validUris.isEmpty()) return

        val urisToShare: List<Uri> = if (stripMetadata) {
            withContext(Dispatchers.IO) {
                val shareDir = File(context.cacheDir, "shared_images").also { it.mkdirs() }
                validUris.mapIndexed { idx, uri ->
                    try {
                        val mimeType = resolveMimeType(context, uri)
                        val isImage = mimeType.startsWith("image/")

                        val extension = when {
                            mimeType.contains("png", true) -> "png"
                            mimeType.contains("webp", true) -> "webp"
                            mimeType.contains("heic", true) || mimeType.contains("heif", true) -> "heic"
                            mimeType.contains("gif", true) -> "gif"
                            mimeType.startsWith("video/", true) -> {
                                when {
                                    mimeType.contains("mp4", true) -> "mp4"
                                    mimeType.contains("3gp", true) -> "3gp"
                                    mimeType.contains("mkv", true) -> "mkv"
                                    mimeType.contains("quicktime", true) -> "mov"
                                    else -> "mp4"
                                }
                            }
                            else -> "jpg"
                        }

                        val tempFile = File(shareDir, "share_${System.currentTimeMillis()}_$idx.$extension")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        }

                        if (isImage && !mimeType.contains("gif", true)) {
                            val exif = androidx.exifinterface.media.ExifInterface(tempFile.absolutePath)
                            val piiTags = listOf(
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_TIMESTAMP,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_DATESTAMP,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_PROCESSING_METHOD,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_BEARING,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_DISTANCE,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_SPEED,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_TRACK,
                                androidx.exifinterface.media.ExifInterface.TAG_GPS_IMG_DIRECTION,
                                androidx.exifinterface.media.ExifInterface.TAG_MAKE,
                                androidx.exifinterface.media.ExifInterface.TAG_MODEL,
                                androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE,
                                androidx.exifinterface.media.ExifInterface.TAG_ARTIST,
                                androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT,
                                androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT,
                                androidx.exifinterface.media.ExifInterface.TAG_DATETIME,
                                androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL,
                                androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED,
                                androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME,
                                androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_ORIGINAL
                            )
                            piiTags.forEach { tag -> exif.setAttribute(tag, null) }
                            exif.saveAttributes()
                        }

                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ShareUtils", "Failed to strip metadata for $uri", e)
                        uri
                    }
                }
            }
        } else {
            validUris
        }

        if (urisToShare.isEmpty()) return

        val mimeType = getSharedMimeType(context, urisToShare)

        val intent = if (urisToShare.size == 1) {
            val singleUri = urisToShare.first()
            val singleMime = resolveMimeType(context, singleUri)
            Intent(Intent.ACTION_SEND).apply {
                type = singleMime
                putExtra(Intent.EXTRA_STREAM, singleUri)
                clipData = ClipData.newUri(context.contentResolver, "Media", singleUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(urisToShare))
                val firstUri = urisToShare.first()
                val clip = ClipData.newRawUri("Shared Media", firstUri)
                for (i in 1 until urisToShare.size) {
                    clip.addItem(ClipData.Item(urisToShare[i]))
                }
                clipData = clip
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        val chooser = Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }
}
