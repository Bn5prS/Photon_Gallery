package com.inferno.gallery.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.inferno.gallery.MainActivity
import com.inferno.gallery.R
import com.inferno.gallery.data.network.UpdateInfo

object UpdateNotificationHelper {
    const val CHANNEL_ID = "app_updates"
    const val NOTIFICATION_ID = 2026

    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies when a new version of Photon Gallery is available"
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showUpdateNotification(context: Context, updateInfo: UpdateInfo) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_update_dialog", true)
            putExtra("update_version", updateInfo.latestVersion)
            putExtra("update_title", updateInfo.releaseTitle)
            putExtra("update_changelog", updateInfo.changelog)
            putExtra("update_download_url", updateInfo.downloadUrl)
            putExtra("update_apk_name", updateInfo.apkFileName)
            putExtra("update_apk_size", updateInfo.apkSizeBytes)
            putExtra("update_release_url", updateInfo.releasePageUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryText = if (updateInfo.releaseTitle.isNotBlank() && updateInfo.releaseTitle != updateInfo.latestVersion) {
            "${updateInfo.releaseTitle} • Tap to download and install"
        } else {
            "Version ${updateInfo.latestVersion} is ready to download and install"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ms_download)
            .setContentTitle("Photon Gallery ${updateInfo.latestVersion} Available")
            .setContentText(summaryText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(if (updateInfo.changelog.isNotBlank()) updateInfo.changelog.take(300) else summaryText)
                    .setSummaryText("Update Available")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    fun dismissNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}
