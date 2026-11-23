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

    companion object {
        const val KEY_FULL = "full_reconcile"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        } catch (_: Throwable) {}
        try {
            // Fast incremental sync by default; the full filesystem reconcile only when asked
            // (the 6-hour periodic job sets KEY_FULL). Callers that just saved a photo stay fast.
            val full = inputData.getBoolean(KEY_FULL, false)
            Log.d("MediaSyncWorker", "Starting MediaStore sync via MediaSyncEngine (full=$full)...")
            val engine = MediaSyncEngine.getInstance(applicationContext)
            if (full) engine.sync(forceFullSync = false) else engine.syncDelta()
            Result.success()
        } catch (e: Exception) {
            Log.e("MediaSyncWorker", "Sync failed: ${e.message}", e)
            Result.retry()
        }
    }
}
