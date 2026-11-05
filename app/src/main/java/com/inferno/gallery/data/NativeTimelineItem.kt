package com.inferno.gallery.data

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
sealed class NativeTimelineItem {
    abstract val key: String

    @Immutable
    data class Header(
        val id: String,
        val title: String,
        val timestamp: Long
    ) : NativeTimelineItem() {
        override val key: String get() = "header_$id"
    }

    @Immutable
    data class Media(
        val id: String,
        val uri: Uri,
        val uriString: String,
        val path: String,
        val dateAdded: Long,
        val dateModified: Long,
        val isVideo: Boolean,
        val durationMs: Long?,
        val bucketName: String,
        val size: Long = 0L
    ) : NativeTimelineItem() {
        override val key: String get() = id
    }
}

