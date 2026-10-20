package com.inferno.gallery.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inferno.gallery.BuildConfig
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.network.UpdateChecker
import com.inferno.gallery.update.UpdateNotificationHelper
import kotlinx.coroutines.flow.first

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "UpdateCheckWorkerPeriodic"
        private const val TAG = "UpdateCheckWorker"

        fun schedulePeriodic(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val workRequest = androidx.work.PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancel(context: Context) {
            androidx.work.WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val settingsRepo = SettingsRepository.getInstance(applicationContext)

        val autoCheck = settingsRepo.autoCheckUpdatesFlow.first()
        if (!autoCheck) {
            Log.d(TAG, "Auto update check disabled by user setting")
            return Result.success()
        }

        return try {
            val currentVersion = BuildConfig.VERSION_NAME
            Log.d(TAG, "Checking for updates. Current version: $currentVersion")

            val updateInfo = UpdateChecker.checkForUpdates(currentVersion)
            settingsRepo.updateLastUpdateCheckTimestamp(System.currentTimeMillis())

            if (updateInfo != null && updateInfo.isAvailable) {
                Log.i(TAG, "New update found: ${updateInfo.latestVersion}. Showing notification.")
                UpdateNotificationHelper.showUpdateNotification(applicationContext, updateInfo)
            } else {
                Log.d(TAG, "Photon Gallery is up to date ($currentVersion)")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Update check worker failed: ${e.message}", e)
            Result.retry()
        }
    }
}
