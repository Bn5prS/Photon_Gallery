package com.inferno.gallery.ui.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache

/**
 * Centralized cache eviction utility for in-place photo edits, rotations, and saves.
 *
 * Ensures that Coil 3's in-memory bitmap pool, Coil 3's disk cache, Android MediaProvider's
 * thumbnail cache, and Room/ViewModel observers are cleanly invalidated when an image is overwritten.
 */
object ImageCacheEvictor {

    /**
     * Purges memory and disk cache entries across Coil and triggers OS-level MediaScanner and
     * ContentResolver notifications when a media item has been edited or overwritten in place.
     */
    fun evictMedia(context: Context, uri: Uri, explicitId: String? = null) {
        try {
            val imageLoader = SingletonImageLoader.get(context)
            val uriStr = uri.toString()
            val resolvedId = explicitId ?: if (uri.scheme == "content") uri.lastPathSegment else null

            // 1. Evict from Coil 3 MemoryCache
            imageLoader.memoryCache?.let { mem ->
                val keysToRemove = mem.keys.filter { key ->
                    key.key == uriStr ||
                            key.key.contains(uriStr) ||
                            (resolvedId != null && key.key.startsWith("t_${resolvedId}"))
                }
                keysToRemove.forEach { mem.remove(it) }
            }

            // 2. Evict from Coil 3 DiskCache
            imageLoader.diskCache?.let { disk ->
                disk.remove(uriStr)
                if (resolvedId != null) {
                    disk.remove(resolvedId)
                    disk.remove("t_${resolvedId}")
                    for (size in listOf(128, 160, 240, 320, 512)) {
                        disk.remove("t_${resolvedId}@$size")
                    }
                }
            }

            // 3. Notify ContentResolver observers
            if (uri.scheme == "content") {
                try {
                    context.contentResolver.notifyChange(uri, null)
                } catch (_: Exception) {}
            }

            // 4. Trigger MediaScanner to rescan file on disk and purge OS MediaProvider thumbnail cache
            val filePath = resolveFilePath(context, uri)
            if (!filePath.isNullOrBlank()) {
                try {
                    MediaScannerConnection.scanFile(context.applicationContext, arrayOf(filePath), null, null)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun resolveFilePath(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        if (uri.scheme != "content") return null
        return try {
            val proj = arrayOf(android.provider.MediaStore.Images.Media.DATA)
            context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                val colIdx = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                if (colIdx >= 0 && cursor.moveToFirst()) cursor.getString(colIdx) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}

