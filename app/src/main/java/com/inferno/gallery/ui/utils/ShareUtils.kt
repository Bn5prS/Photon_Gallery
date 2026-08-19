package com.inferno.gallery.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ShareUtils {
    suspend fun shareMedia(
        context: Context,
        uris: List<Uri>,
        stripMetadata: Boolean
    ) {
        val urisToShare: List<Uri> = if (stripMetadata) {
            withContext(Dispatchers.IO) {
                val shareDir = File(context.cacheDir, "shared_images").also { it.mkdirs() }
                uris.filterNotNull().mapIndexed { idx, uri ->
                    try {
                        val extension = context.contentResolver.getType(uri)?.substringAfter("/") ?: "jpg"
                        val tempFile = File(shareDir, "share_${System.currentTimeMillis()}_$idx.$extension")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        }
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
            uris
        }

        if (urisToShare.isEmpty()) return

        val intent = if (urisToShare.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, urisToShare.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(urisToShare))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        
        // Use Intent.createChooser for the system default share sheet
        val chooser = Intent.createChooser(intent, "Share via")
        // Required if starting from outside an Activity context in some cases, but generally fine inside Compose
        context.startActivity(chooser)
    }
}
