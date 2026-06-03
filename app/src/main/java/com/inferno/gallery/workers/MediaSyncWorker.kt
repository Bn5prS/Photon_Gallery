package com.inferno.gallery.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inferno.gallery.data.LocalMediaRepository
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.CoreMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("MediaSyncWorker", "Starting MediaStore sync...")
            val mediaRepository = LocalMediaRepository(applicationContext.contentResolver)
            val database = DatabaseProvider.getDatabase(applicationContext)
            
            val mediaStoreList = mediaRepository.getImagesListForSync()
            val dbList = database.mediaDao().getAllMedia()
            
            val mediaStoreMap = mediaStoreList.associateBy { it.id }
            val dbMap = dbList.associateBy { it.id }
            
            val toInsert = mutableListOf<CoreMediaEntity>()
            val toDelete = mutableListOf<Long>()
            
            // Find items in MediaStore not in DB
            for (media in mediaStoreList) {
                if (!dbMap.containsKey(media.id)) {
                    toInsert.add(
                        CoreMediaEntity(
                            id = media.id,
                            uriString = media.uri.toString(),
                            filePath = media.path,
                            bucketName = media.bucketName,
                            dateAdded = media.dateAdded,
                            dateModified = media.dateModified,
                            size = media.size,
                            name = media.name,
                            mimeType = null, 
                            isVideo = media.isVideo,
                            durationMs = media.durationMs,
                            isIndexedClip = false,
                            isIndexedOcr = false
                        )
                    )
                }
            }
            
            // Find items in DB not in MediaStore
            for (dbItem in dbList) {
                if (!mediaStoreMap.containsKey(dbItem.id)) {
                    toDelete.add(dbItem.id)
                }
            }
            
            if (toInsert.isNotEmpty()) {
                Log.d("MediaSyncWorker", "Inserting ${toInsert.size} new items into Room SSOT.")
                database.mediaDao().insertAll(toInsert)
            }
            
            if (toDelete.isNotEmpty()) {
                Log.d("MediaSyncWorker", "Deleting ${toDelete.size} items from Room SSOT.")
                database.mediaDao().deleteByIds(toDelete)
            }
            
            Log.d("MediaSyncWorker", "Sync complete.")
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MediaSyncWorker", "Sync failed: ${e.message}")
            Result.retry()
        }
    }
}
