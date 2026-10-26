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
import com.inferno.gallery.workers.FaceIndexWorker
import com.inferno.gallery.workers.MediaSyncWorker
import com.inferno.gallery.workers.OcrIndexWorker
import com.inferno.gallery.workers.ReverseGeocodeWorker
import com.inferno.gallery.data.db.DatabaseProvider

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
        // Increase Android CursorWindow limit to 32MB to prevent window overflow / paging errors on large libraries
        try {
            val field = android.database.CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 32 * 1024 * 1024)
        } catch (_: Throwable) {
            // Field may be hidden or restricted on some vendor OEM builds; non-fatal
        }

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
            // Always run media sync immediately to keep database SSOT fresh
            WorkManager.getInstance(this@GalleryApplication)
                .enqueueUniqueWork("MediaSyncWorker", androidx.work.ExistingWorkPolicy.KEEP, syncWorkRequest)

            val ocrEnabled = settingsRepo.ocrIndexingEnabledFlow.first()
            if (ocrEnabled) {
                val ocrConstraints = androidx.work.Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
                val ocrIndexRequest = androidx.work.OneTimeWorkRequestBuilder<OcrIndexWorker>()
                    .setConstraints(ocrConstraints)
                    .build()
                WorkManager.getInstance(this@GalleryApplication)
                    .enqueueUniqueWork("OcrIndexWorker", androidx.work.ExistingWorkPolicy.KEEP, ocrIndexRequest)
            }

            val faceEnabled = settingsRepo.faceRecognitionEnabledFlow.first()
            val autoIndexFaces = settingsRepo.faceAutoIndexFlow.first()
            val faceEngine = com.inferno.gallery.data.ai.FaceRecognitionEngine.getInstance(this@GalleryApplication)
            if (faceEnabled && autoIndexFaces && faceEngine.isModelDownloaded()) {
                val db = DatabaseProvider.getDatabase(this@GalleryApplication)
                val prefs = getSharedPreferences("face_scan_meta", android.content.Context.MODE_PRIVATE)
                val scanVersion = prefs.getInt("scan_version", 1)
                if (scanVersion < 2) {
                    db.faceDao().clearZeroFaceStatuses()
                    prefs.edit().putInt("scan_version", 2).apply()
                }

                val unindexedFaces = db.faceDao().getUnindexedMediaIds().isNotEmpty()
                if (unindexedFaces) {
                    val faceConstraints = androidx.work.Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                    val faceRequest = OneTimeWorkRequestBuilder<FaceIndexWorker>()
                        .setConstraints(faceConstraints)
                        .build()
                    WorkManager.getInstance(this@GalleryApplication)
                        .enqueueUniqueWork("FaceIndexWorker", androidx.work.ExistingWorkPolicy.KEEP, faceRequest)
                }
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

            val autoCheckUpdates = settingsRepo.autoCheckUpdatesFlow.first()
            if (autoCheckUpdates) {
                com.inferno.gallery.workers.UpdateCheckWorker.schedulePeriodic(this@GalleryApplication)
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                // Support high-performance system thumbnails for local MediaStore items
                add(MediaStoreThumbnailFetcher.Factory(context))
                add(MediaStoreThumbnailFetcher.AndroidUriFactory(context))
                // Support camera RAW formats (DNG, CR2, CR3, NEF, ARW, ORF, RAF, RW2)
                add(com.inferno.gallery.data.decoder.RawImageFetcher.Factory(context))
                add(com.inferno.gallery.data.decoder.RawImageFetcher.AndroidUriFactory(context))
                // Support video frames
                add(VideoFrameDecoder.Factory())
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
