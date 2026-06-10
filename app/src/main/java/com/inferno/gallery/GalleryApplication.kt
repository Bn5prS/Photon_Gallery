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

import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.MediaStoreThumbnailFetcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

class GalleryApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        // Chain: MediaStore sync → AI embedding indexing
        val syncWorkRequest = OneTimeWorkRequestBuilder<MediaSyncWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork("MediaSyncWorker", androidx.work.ExistingWorkPolicy.KEEP, syncWorkRequest)

        val settingsRepo = SettingsRepository(this)
        kotlinx.coroutines.MainScope().launch {
            val ocrEnabled = settingsRepo.ocrIndexingEnabledFlow.first()

            if (ocrEnabled) {
                val ocrIndexRequest = androidx.work.OneTimeWorkRequestBuilder<OcrIndexWorker>()
                    .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                WorkManager.getInstance(this@GalleryApplication).enqueueUniqueWork("OcrIndexWorker", androidx.work.ExistingWorkPolicy.KEEP, ocrIndexRequest)
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                // Support high-performance system thumbnails for local MediaStore items
                add(MediaStoreThumbnailFetcher.Factory(context))
                // Support GIFs, Animated WebP, and Animated HEIF
                if (SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                // Support SVGs
                add(SvgDecoder.Factory())
                // Support Video Frame extraction
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                coil3.memory.MemoryCache.Builder()
                    .maxSizePercent(context, 0.40) // Use 40% of available heap size
                    .build()
            }
            .diskCache {
                coil3.disk.DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(512L * 1024L * 1024L) // 512 MB disk cache
                    .build()
            }
            .crossfade(false) // Premium smooth loading transition
            .build()
    }
}
