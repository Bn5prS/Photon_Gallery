package com.inferno.gallery.ui.components

import androidx.compose.ui.geometry.Rect
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lightweight window-coordinate registry bridging grid thumbnails ([NativePhotonGrid],
 * [PhotonGrid], Search, PersonDetailScreen) with Compose [DetailScreen] for pixel-accurate
 * container-transform open/close animations.
 *
 * Uses a LIFO resolver stack so nested screens (such as opening a Person detail screen over the
 * main timeline) register their own resolver on top and cleanly restore the parent screen's
 * resolver when popped.
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

    private val resolvers = CopyOnWriteArrayList<(String) -> Rect?>()

    fun setLaunchBounds(mediaKey: String, rect: Rect, cornerRadiusDp: Float) {
        activeLaunchBounds = LaunchBounds(mediaKey, rect, cornerRadiusDp)
    }

    fun setAlbumLaunchBounds(bucketName: String, rect: Rect, cornerRadiusDp: Float) {
        activeAlbumLaunchBounds = LaunchBounds(bucketName, rect, cornerRadiusDp)
    }

    fun registerResolver(resolver: (String) -> Rect?) {
        resolvers.remove(resolver)
        resolvers.add(resolver)
    }

    fun unregisterResolver(resolver: (String) -> Rect?) {
        resolvers.remove(resolver)
    }

    fun resolveBounds(mediaKey: String): Rect? {
        for (i in resolvers.size - 1 downTo 0) {
            val resolved = runCatching { resolvers[i].invoke(mediaKey) }.getOrNull()
            if (resolved != null) return resolved
        }
        return activeLaunchBounds?.takeIf { it.mediaKey == mediaKey }?.rect
    }

    fun clear() {
        activeLaunchBounds = null
    }

    fun clearAlbum() {
        activeAlbumLaunchBounds = null
    }
}
