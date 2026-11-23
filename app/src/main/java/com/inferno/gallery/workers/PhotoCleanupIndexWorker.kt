package com.inferno.gallery.workers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inferno.gallery.data.PhotoQualityCache
import com.inferno.gallery.data.PhotoQualityMetrics
import com.inferno.gallery.data.SimilarPhotoAnalyzer
import com.inferno.gallery.data.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background charging/idle worker that computes 64-bit perceptual hashes (dHash) and
 * Laplacian sharpness scores offline so Smart Clean Up is instant when opened (#51).
 */
class PhotoCleanupIndexWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        } catch (_: Throwable) {}

        try {
            val db = DatabaseProvider.getDatabase(applicationContext)
            val qualityCache = PhotoQualityCache.getInstance(applicationContext)
            qualityCache.ensureLoaded()

            val batchLimit = 600
            val pending = db.mediaDao().getImagesNeedingPHash(limit = batchLimit)
            if (pending.isEmpty()) return@withContext Result.success()

            val hashBatch = ArrayList<Pair<Long, Long>>(50)
            for (entity in pending) {
                if (isStopped) break
                val sig = SimilarPhotoAnalyzer.analyze(
                    contentResolver = applicationContext.contentResolver,
                    uri = Uri.parse(entity.uriString),
                    filePath = entity.filePath
                )
                if (sig != null) {
                    hashBatch.add(entity.id to sig.pHash)
                    qualityCache.put(
                        entity.id,
                        PhotoQualityMetrics(
                            dateModified = entity.dateModified,
                            sharpness = sig.sharpness,
                            pixelCount = sig.pixelCount
                        )
                    )
                }
                if (hashBatch.size >= 50) {
                    db.mediaDao().updatePHashBatch(hashBatch)
                    hashBatch.clear()
                }
            }
            if (hashBatch.isNotEmpty()) {
                db.mediaDao().updatePHashBatch(hashBatch)
            }
            qualityCache.persist()
            Log.d("PhotoCleanupIndexWorker", "Indexed perceptual hashes for ${pending.size} photos")
            Result.success()
        } catch (e: Exception) {
            Log.e("PhotoCleanupIndexWorker", "Background perceptual hash pass failed: ${e.message}")
            Result.retry()
        }
    }
}

