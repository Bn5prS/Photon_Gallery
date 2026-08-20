package com.inferno.gallery

import android.app.Application
import android.os.Build.VERSION.SDK_INT
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.inferno.gallery.workers.MediaSyncWorker
import com.inferno.gallery.workers.OcrIndexWorker
import com.inferno.gallery.workers.ReverseGeocodeWorker

import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.MediaStoreThumbnailFetcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath


class GalleryApplication : Application(), SingletonImageLoader.Factory, androidx.work.Configuration.Provider {
    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    override fun onCreate() {
        super.onCreate()
        // Install global crash handler — must be first to catch startup crashes
        com.inferno.gallery.crash.CrashHandler.install(this)
        // Chain: MediaStore sync → OCR indexing (OCR must wait for sync to populate the DB)
        val syncWorkRequest = OneTimeWorkRequestBuilder<MediaSyncWorker>()
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                30, java.util.concurrent.TimeUnit.SECONDS
            )
            .build()

        val settingsRepo = SettingsRepository.getInstance(this)
        kotlinx.coroutines.MainScope().launch {
            val ocrEnabled = settingsRepo.ocrIndexingEnabledFlow.first()

            if (ocrEnabled) {
                val ocrIndexRequest = androidx.work.OneTimeWorkRequestBuilder<OcrIndexWorker>()
                    .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                // Chain: sync first, then OCR — prevents OCR from seeing an empty DB
                WorkManager.getInstance(this@GalleryApplication)
                    .beginUniqueWork("MediaSyncWorker", androidx.work.ExistingWorkPolicy.KEEP, syncWorkRequest)
                    .then(ocrIndexRequest)
                    .enqueue()
            } else {
                // No OCR needed, just run sync alone
                WorkManager.getInstance(this@GalleryApplication)
                    .enqueueUniqueWork("MediaSyncWorker", androidx.work.ExistingWorkPolicy.KEEP, syncWorkRequest)
            }

            // Periodic sync every 6 hours
            val syncPeriodicRequest = androidx.work.PeriodicWorkRequestBuilder<MediaSyncWorker>(
                6, java.util.concurrent.TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(this@GalleryApplication).enqueueUniquePeriodicWork(
                "MediaSyncWorkerPeriodic",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncPeriodicRequest
            )
            

            // Geocode photos to populate Places clusters
            val geocodeRequest = androidx.work.OneTimeWorkRequestBuilder<ReverseGeocodeWorker>().build()
            WorkManager.getInstance(this@GalleryApplication)
                .enqueueUniqueWork("ReverseGeocodeWorker", androidx.work.ExistingWorkPolicy.KEEP, geocodeRequest)
            
            val geocodePeriodicRequest = androidx.work.PeriodicWorkRequestBuilder<ReverseGeocodeWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(this@GalleryApplication).enqueueUniquePeriodicWork(
                "ReverseGeocodeWorkerPeriodic",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                geocodePeriodicRequest
            )

            val autoCleanEnabled = settingsRepo.autoCleanTrashEnabledFlow.first()
            if (autoCleanEnabled) {
                val days = settingsRepo.autoCleanTrashDaysFlow.first()
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
                val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.inferno.gallery.workers.AutoCleanTrashWorker>(
                    24, java.util.concurrent.TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(this@GalleryApplication).enqueueUniquePeriodicWork(
                    "AutoCleanTrashWorker",
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                // Support high-performance system thumbnails for local MediaStore items
                add(MediaStoreThumbnailFetcher.Factory(context))
                add(MediaStoreThumbnailFetcher.AndroidUriFactory(context))
                // Support SVGs
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                coil3.memory.MemoryCache.Builder()
                    .maxSizePercent(context, 0.40) // 40% — extra headroom for large galleries; GPU-resident HARDWARE bitmaps live in GPU memory, so Java heap stays healthy
                    .build()
            }
            .diskCache {
                coil3.disk.DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(512L * 1024L * 1024L) // 512 MB disk cache
                    .build()
            }
            // NOTE: Global crossfade intentionally removed — it caused 40+ simultaneous
            // crossfade animation frames during fast scroll, flooding the main thread.
            // Per-request crossfade is already disabled in GalleryGridItem (crossfade=false).
            .build()
    }
}
