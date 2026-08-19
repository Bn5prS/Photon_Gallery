package com.inferno.gallery.workers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import com.inferno.gallery.data.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrecacheThumbnailsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "PrecacheThumbnailsWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting PrecacheThumbnailsWorker...")
            val database = DatabaseProvider.getDatabase(applicationContext)
            val allMedia = database.mediaDao().getAllMedia()
            if (allMedia.isEmpty()) {
                Log.d(TAG, "No media to precache.")
                return@withContext Result.success()
            }

            val imageLoader = SingletonImageLoader.get(applicationContext)
            var count = 0

            for (item in allMedia) {
                if (isStopped) break
                
                val resolvedUri = Uri.parse(item.uriString)

                val request = ImageRequest.Builder(applicationContext)
                    .data(resolvedUri)
                    .size(384, 384)
                    .memoryCacheKey("photo_${resolvedUri}_384")
                    .precision(coil3.size.Precision.EXACT)
                    .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .apply {
                        if (item.isVideo) {
                            videoFrameMillis(0)
                        }
                    }
                    .build()

                try {
                    imageLoader.execute(request)
                    count++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to precache item ${item.id}: ${e.message}")
                }
            }

            Log.d(TAG, "Successfully precached $count thumbnails.")
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "PrecacheThumbnailsWorker failed: ${e.message}")
            Result.retry()
        }
    }
}
