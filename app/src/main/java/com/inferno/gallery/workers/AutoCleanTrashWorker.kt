package com.inferno.gallery.workers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class AutoCleanTrashWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AutoCleanTrashWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting AutoCleanTrashWorker...")
            val settings = SettingsRepository.getInstance(applicationContext)
            val enabled = settings.autoCleanTrashEnabledFlow.first()
            if (!enabled) {
                Log.d(TAG, "Auto clean trash is disabled. Exiting.")
                return@withContext Result.success()
            }

            val days = settings.autoCleanTrashDaysFlow.first()
            Log.d(TAG, "Auto clean trash is enabled. Retention days: $days")

            val database = DatabaseProvider.getDatabase(applicationContext)
            val allMedia = database.mediaDao().getAllMedia()
            val trashItems = allMedia.filter { it.bucketName == "Trash" }
            if (trashItems.isEmpty()) {
                Log.d(TAG, "No items in trash. Exiting.")
                return@withContext Result.success()
            }

            val cutoffSeconds = (System.currentTimeMillis() / 1000L) - (days * 24L * 60L * 60L)
            val expiredItems = trashItems.filter { it.dateModified < cutoffSeconds }

            if (expiredItems.isEmpty()) {
                Log.d(TAG, "No expired items found in trash (cutoff timestamp: $cutoffSeconds). Exiting.")
                return@withContext Result.success()
            }

            Log.d(TAG, "Found ${expiredItems.size} expired items in trash to delete.")
            val deletedIds = mutableListOf<Long>()

            for (item in expiredItems) {
                Log.d(TAG, "Deleting expired trash item: ${item.name} (modified: ${item.dateModified})")
                try {
                    val file = File(item.filePath)
                    if (file.exists()) {
                        val deleted = file.delete()
                        if (deleted) {
                            applicationContext.contentResolver.delete(Uri.parse(item.uriString), null, null)
                        }
                    } else {
                        applicationContext.contentResolver.delete(Uri.parse(item.uriString), null, null)
                    }
                    deletedIds.add(item.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete file/MediaStore entry for item ${item.id}: ${e.message}")
                    try {
                        applicationContext.contentResolver.delete(Uri.parse(item.uriString), null, null)
                        deletedIds.add(item.id)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Second deletion attempt failed for item ${item.id}: ${ex.message}")
                    }
                }
            }

            if (deletedIds.isNotEmpty()) {
                database.mediaDao().deleteByIds(deletedIds)
                Log.d(TAG, "Successfully cleaned ${deletedIds.size} expired items from trash.")
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "AutoCleanTrashWorker failed: ${e.message}")
            Result.retry()
        }
    }
}
