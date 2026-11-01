package com.inferno.gallery.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inferno.gallery.data.MediaSyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        } catch (_: Throwable) {}
        try {
            Log.d("MediaSyncWorker", "Starting MediaStore sync via MediaSyncEngine...")
            MediaSyncEngine.getInstance(applicationContext).sync(forceFullSync = false)
            Result.success()
        } catch (e: Exception) {
            Log.e("MediaSyncWorker", "Sync failed: ${e.message}", e)
            Result.retry()
        }
    }
}
