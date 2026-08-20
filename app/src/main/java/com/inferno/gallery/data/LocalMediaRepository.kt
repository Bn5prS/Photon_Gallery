package com.inferno.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaData(
    val id: Long,
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

    fun observeImages(folderName: String? = null): Flow<List<MediaData>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                launch { trySend(getImagesListForSync(folderName)) }
            }
        }
        contentResolver.registerContentObserver(MediaStore.Files.getContentUri("external"), true, observer)
        trySend(getImagesListForSync(folderName))
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }

    suspend fun getImagesListForSync(folderName: String? = null, lastGeneration: Long? = null): List<MediaData> = withContext(Dispatchers.IO) {
        val images = mutableListOf<MediaData>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.IS_TRASHED
        )

        val baseSelection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        
        val isTrashRequest = folderName == "Trash"
        val isAllRequest = folderName == null
        
        var selection = if (folderName != null && !isTrashRequest) {
            "$baseSelection AND ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        } else baseSelection
        
        val selectionArgsList = mutableListOf<String>()
        if (folderName != null && !isTrashRequest) selectionArgsList.add(folderName)

        if (lastGeneration != null && lastGeneration > 0) {
            selection += " AND (${MediaStore.MediaColumns.GENERATION_ADDED} > ? OR ${MediaStore.MediaColumns.GENERATION_MODIFIED} > ?)"
            selectionArgsList.add(lastGeneration.toString())
            selectionArgsList.add(lastGeneration.toString())
        }
        
        val selectionArgs = if (selectionArgsList.isEmpty()) null else selectionArgsList.toTypedArray()

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val bundle = android.os.Bundle().apply {
            val matchTrashed = when {
                isTrashRequest -> MediaStore.MATCH_ONLY
                isAllRequest -> 1 // MATCH_INCLUDE
                else -> MediaStore.MATCH_EXCLUDE
            }
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, matchTrashed)
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
        }

        contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            bundle,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateTakenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val durationColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
            val isTrashedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val isTrashed = if (isTrashedColumn >= 0) cursor.getInt(isTrashedColumn) == 1 else false
                val bucketName = if (isTrashed) "Trash" else (cursor.getString(bucketColumn) ?: "Unknown")
                val rawDateAdded = cursor.getLong(dateAddedColumn)
                val dateTakenMs = if (dateTakenColumn >= 0) cursor.getLong(dateTakenColumn) else 0L
                val dateTakenSec = if (dateTakenMs > 0) dateTakenMs / 1000L else 0L
                val dateAdded = if (dateTakenSec > 0) dateTakenSec else rawDateAdded
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
                images.add(MediaData(id, uri, bucketName, dateAdded, size, name, dateModified, path, isVideo, durationMs))
            }
        }

        images.sortByDescending { it.dateAdded }

        images
    }

    suspend fun getAllMediaIds(): Set<Long> = withContext(Dispatchers.IO) {
        val ids = mutableSetOf<Long>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        
        val bundle = android.os.Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, 1) // 1 = MATCH_INCLUDE
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
        }

        contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            bundle,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(idCol))
            }
        }
        ids
    }
}
