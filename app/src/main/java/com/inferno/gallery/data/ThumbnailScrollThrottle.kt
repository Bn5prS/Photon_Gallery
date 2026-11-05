package com.inferno.gallery.data

/**
 * High-performance thread-safe throttle controller for thumbnail decoding.
 * 100ms fling throttle: during active scrolling or fast scrubbing,
 * thumbnail fetch requests are delayed by 80-100ms. If an item scrolls off-screen before
 * the delay expires, the coroutine is cancelled, preventing MediaProvider IPC calls,
 * hardware bitmap decodes, and GC pressure.
 */
object ThumbnailScrollThrottle {
    @Volatile
    var isFastScrolling: Boolean = false
}
