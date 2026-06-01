package com.inferno.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaData(
    val mediaStoreId: Long,
    val uri: Uri,
    val bucketName: String,
    val dateAdded: Long,
    val size: Long,
    val name: String,
    val dateModified: Long,
    val path: String,
    val isVideo: Boolean = false,
    val durationMs: Long? = null
)



/**
 * Repository that queries the device's MediaStore for locally stored images.
 *
 * All queries run on [Dispatchers.IO] to keep the UI thread free
 * (per GEMINI.md rule §4 — Background Data/AI isolation).
 */
class LocalMediaRepository(
    private val contentResolver: ContentResolver
) {

    suspend fun getImages(folderName: String? = null): List<MediaData> = withContext(Dispatchers.IO) {
        val images = mutableListOf<MediaData>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.DURATION
        )

        val baseSelection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        val selection = if (folderName != null) {
            "$baseSelection AND ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        } else baseSelection
        
        val selectionArgs = folderName?.let { arrayOf(it) }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val durationColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val bucketName = cursor.getString(bucketColumn) ?: "Unknown"
                val dateAdded = cursor.getLong(dateAddedColumn)
                val size = cursor.getLong(sizeColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val dateModified = cursor.getLong(dateModifiedColumn)
                val path = cursor.getString(pathColumn) ?: ""
                val mediaType = cursor.getInt(mediaTypeColumn)
                val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                
                var durationMs: Long? = null
                if (durationColumn >= 0) {
                    val durationStr = cursor.getString(durationColumn)
                    durationMs = durationStr?.toLongOrNull()
                }
                
                val baseUri = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                
                val uri = ContentUris.withAppendedId(baseUri, id)
                images.add(MediaData(mediaStoreId = id, uri = uri, bucketName = bucketName, dateAdded = dateAdded, size = size, name = name, dateModified = dateModified, path = path, isVideo = isVideo, durationMs = durationMs))
            }
        }

        if (folderName == null || folderName == "WhatsApp Statuses") {
            images.addAll(getWhatsAppStatuses())
        }

        images.sortByDescending { it.dateAdded }

        images
    }



    private fun getWhatsAppStatuses(): List<MediaData> {
        val statuses = mutableListOf<MediaData>()
        val paths = listOf(
            Environment.getExternalStorageDirectory().absolutePath + "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            Environment.getExternalStorageDirectory().absolutePath + "/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
        )
        
        val validExtensions = listOf("jpg", "jpeg", "png", "mp4")
        
        for (path in paths) {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    val ext = file.extension.lowercase()
                    if (ext in validExtensions) {
                        val uri = Uri.fromFile(file)
                        val dateAdded = file.lastModified() / 1000
                        statuses.add(
                            MediaData(
                                mediaStoreId = (file.absolutePath + file.lastModified()).hashCode().toLong(),
                                uri = uri,
                                bucketName = "WhatsApp Statuses",
                                dateAdded = dateAdded,
                                size = file.length(),
                                name = file.name,
                                dateModified = file.lastModified() / 1000,
                                path = file.absolutePath,
                                isVideo = ext == "mp4",
                                durationMs = null
                            )
                        )
                    }
                }
            }
        }
        return statuses
    }
}
