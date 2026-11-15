package com.inferno.gallery.ui.components

import androidx.compose.ui.geometry.Rect

/**
 * Lightweight window-coordinate registry bridging native RecyclerView grid thumbnails
 * ([NativePhotonGrid]) with Compose [DetailScreen] for pixel-accurate container-transform
 * open/close animations.
 */
object MediaTransitionRegistry {
    data class LaunchBounds(
        val mediaKey: String,
        val rect: Rect,
        val cornerRadiusDp: Float
    )

    @Volatile
    var activeLaunchBounds: LaunchBounds? = null
        private set

    @Volatile
    var activeAlbumLaunchBounds: LaunchBounds? = null
        private set

    @Volatile
    private var activeResolver: ((String) -> Rect?)? = null

    fun setLaunchBounds(mediaKey: String, rect: Rect, cornerRadiusDp: Float) {
        activeLaunchBounds = LaunchBounds(mediaKey, rect, cornerRadiusDp)
    }

    fun setAlbumLaunchBounds(bucketName: String, rect: Rect, cornerRadiusDp: Float) {
        activeAlbumLaunchBounds = LaunchBounds(bucketName, rect, cornerRadiusDp)
    }

    fun registerResolver(resolver: (String) -> Rect?) {
        activeResolver = resolver
    }

    fun unregisterResolver(resolver: (String) -> Rect?) {
        if (activeResolver === resolver) {
            activeResolver = null
        }
    }

    fun resolveBounds(mediaKey: String): Rect? {
        return activeResolver?.invoke(mediaKey)
            ?: activeLaunchBounds?.takeIf { it.mediaKey == mediaKey }?.rect
    }

    fun clear() {
        activeLaunchBounds = null
    }

    fun clearAlbum() {
        activeAlbumLaunchBounds = null
    }
}
