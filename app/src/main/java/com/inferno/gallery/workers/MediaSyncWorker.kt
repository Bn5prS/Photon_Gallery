package com.inferno.gallery.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inferno.gallery.data.LocalMediaRepository
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.CoreMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class MediaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("MediaSyncWorker", "Starting MediaStore sync...")
            val mediaRepository = LocalMediaRepository(applicationContext.contentResolver)
            val database = DatabaseProvider.getDatabase(applicationContext)
            val settingsRepo = SettingsRepository.getInstance(applicationContext)

            val prefs = applicationContext.getSharedPreferences("photon_sync", Context.MODE_PRIVATE)
            val lastGeneration = prefs.getLong("last_generation", 0L)
            
            // Get delta inserts/updates
            var deltaList = mediaRepository.getImagesListForSync(null, lastGeneration)
            
            val dbList = database.mediaDao().getAllMedia()
            val dbMap = dbList.associateBy { it.id }
            
            val allMediaStoreIds = mediaRepository.getAllMediaIds()
            val deltaIds = deltaList.map { it.id }.toSet()
            
            // Safety net: If there are IDs in MediaStore that are neither in our local DB nor in the delta,
            // our incremental sync drifted (e.g. from a past bug). Fall back to full sync to repair DB.
            val hasMissingItems = allMediaStoreIds.any { !dbMap.containsKey(it) && !deltaIds.contains(it) }
            
            if (hasMissingItems) {
                Log.w("MediaSyncWorker", "Detected database drift (missing items). Falling back to full sync.")
                deltaList = mediaRepository.getImagesListForSync(null, null)
            }

            val toInsert = mutableListOf<CoreMediaEntity>()
            val toDelete = mutableListOf<Long>()

            val vaultUris = database.vaultDao().getAllOriginalUris().toSet()
            val vaultPaths = database.vaultDao().getAllOriginalPaths().toSet()

            // Find items in MediaStore not in DB or changed (using deltaList)
            for (media in deltaList) {
                if (vaultUris.contains(media.uri.toString()) || vaultPaths.contains(media.path)) {
                    continue
                }
                val dbItem = dbMap[media.id]
                // For incremental, anything in deltaList needs updating/inserting
                toInsert.add(
                    CoreMediaEntity(
                        id = media.id,
                        uriString = media.uri.toString(),
                        filePath = media.path,
                        bucketName = media.bucketName,
                        dateAdded = dbItem?.dateAdded ?: media.dateAdded,
                        dateModified = media.dateModified,
                        size = media.size,
                        name = media.name,
                        mimeType = dbItem?.mimeType,
                        isVideo = media.isVideo,
                        durationMs = media.durationMs,
                        isIndexedOcr = dbItem?.isIndexedOcr ?: false,
                        pHash = dbItem?.pHash,
                        latitude = dbItem?.latitude,
                        longitude = dbItem?.longitude,
                        fileHash = dbItem?.fileHash
                    )
                )
            }

            // Find deletions: compare DB against lightweight ID list and active vault items
            for (dbItem in dbList) {
                if (!allMediaStoreIds.contains(dbItem.id) || vaultUris.contains(dbItem.uriString) || vaultPaths.contains(dbItem.filePath)) {
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

            val autoIndexSmartEnabled = settingsRepo.smartSearchAutoIndexFlow.first()
            val smartSearchEngine = com.inferno.gallery.data.ai.SmartSearchEngine.getInstance(applicationContext)
            val unindexedSmartCount = database.embeddingDao().getUnindexedMediaIds().size
            if (autoIndexSmartEnabled && smartSearchEngine.isModelDownloaded() && unindexedSmartCount > 0) {
                Log.d("MediaSyncWorker", "Auto-indexing $unindexedSmartCount images for Smart Search...")
                val indexRequest = androidx.work.OneTimeWorkRequestBuilder<SmartSearchIndexWorker>().build()
                androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "SmartSearchIndexWorker",
                    androidx.work.ExistingWorkPolicy.KEEP,
                    indexRequest
                )
            }

            // Auto-trigger Face Indexing only when model is downloaded and unindexed photos exist
            val faceEngine = com.inferno.gallery.data.ai.FaceRecognitionEngine.getInstance(applicationContext)
            val unindexedFaceCount = try {
                database.faceDao().getUnindexedFaceMedia().size
            } catch (e: Exception) { 0 }
            if (faceEngine.isModelDownloaded() && unindexedFaceCount > 0) {
                Log.d("MediaSyncWorker", "Auto-indexing $unindexedFaceCount images for Face Recognition...")
                val faceRequest = androidx.work.OneTimeWorkRequestBuilder<FaceIndexWorker>().build()
                androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "FaceIndexWorker",
                    androidx.work.ExistingWorkPolicy.KEEP,
                    faceRequest
                )
            }

            val currentGen = try {
                android.provider.MediaStore.getGeneration(applicationContext, android.provider.MediaStore.VOLUME_EXTERNAL)
            } catch (e: Exception) { 0L }
            
            if (currentGen > 0) {
                prefs.edit().putLong("last_generation", currentGen).apply()
            }

            Log.d("MediaSyncWorker", "Sync complete. Generation updated to $currentGen.")
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MediaSyncWorker", "Sync failed: ${e.message}")
            Result.retry()
        }
    }
}
