package com.inferno.gallery.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import coil3.EventListener
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Real-time diagnostic monitor and log catcher for scroll performance, frame drops,
 * main-thread UI freezes, and Coil image decoding timings.
 *
 * Logs to logcat under `[PERF_LOG]` / `[PERF_FRAME]` / `[PERF_COIL]` / `[PERF_SCROLL]`
 * and appends formatted records to `perf_scroll_logs.txt` in the app's files directory.
 */
object ScrollPerformanceMonitor : Choreographer.FrameCallback {

    private const val TAG = "PERF_LOG"
    private var isMonitoring = false
    private var lastFrameTimeNanos = 0L
    private var logFile: File? = null
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    // Current scroll state
    @Volatile var isScrollInProgress: Boolean = false
        private set
    @Volatile var isFastScrolling: Boolean = false
        private set
    @Volatile var currentVelocityItemsPerSec: Int = 0
        private set
    @Volatile var currentVisibleIndex: Int = 0
        private set

    // Performance statistics
    private val frameCount = AtomicInteger(0)
    private val skippedFramesCount = AtomicInteger(0)
    private val maxFrameDurationMs = AtomicInteger(0)
    private val coilRamHits = AtomicInteger(0)
    private val coilDiskLoads = AtomicInteger(0)
    private val coilSlowDecodes = AtomicInteger(0)

    fun init(context: Context) {
        if (isMonitoring) return
        try {
            logFile = File(context.filesDir, "perf_scroll_logs.txt")
            isMonitoring = true
            Choreographer.getInstance().postFrameCallback(this)
            logEvent("PERF_INIT", "ScrollPerformanceMonitor initialized. Log file: ${logFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ScrollPerformanceMonitor", e)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isMonitoring) return
        if (lastFrameTimeNanos != 0L) {
            val deltaMs = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000L).toInt()
            if (isScrollInProgress || deltaMs > 32) {
                frameCount.incrementAndGet()
                if (deltaMs > maxFrameDurationMs.get()) {
                    maxFrameDurationMs.set(deltaMs)
                }
                if (deltaMs > 32) {
                    val skipped = deltaMs / 16
                    skippedFramesCount.addAndGet(skipped)
                    val msg = "Frame drop took ${deltaMs}ms (~$skipped frames dropped). ScrollInProgress=$isScrollInProgress, FastScroll=$isFastScrolling, Velocity=${currentVelocityItemsPerSec} items/sec, Index=$currentVisibleIndex"
                    Log.w("PERF_FRAME", msg)
                    logEvent("PERF_FRAME", msg)

                    if (deltaMs >= 100) {
                        val freezeMsg = "UI FREEZE (>100ms): Frame took ${deltaMs}ms while scrolling! Stack trace:\n${getMainThreadStackTrace()}"
                        Log.e("PERF_FREEZE", freezeMsg)
                        logEvent("PERF_FREEZE", freezeMsg)
                    }
                }
            }
        }
        lastFrameTimeNanos = frameTimeNanos
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun onScrollStateChanged(
        inProgress: Boolean,
        firstVisibleIndex: Int,
        fastScrolling: Boolean,
        velocityItemsPerSec: Int,
        totalItems: Int
    ) {
        val wasInProgress = isScrollInProgress
        isScrollInProgress = inProgress
        isFastScrolling = fastScrolling
        currentVisibleIndex = firstVisibleIndex
        currentVelocityItemsPerSec = velocityItemsPerSec

        if (!wasInProgress && inProgress) {
            // Scroll started
            logEvent("PERF_SCROLL", "SCROLL START: Index=$firstVisibleIndex / $totalItems")
        } else if (wasInProgress && !inProgress) {
            // Scroll stopped
            val summary = "SCROLL STOP: Index=$firstVisibleIndex. Stats during scroll -> Frames: ${frameCount.get()}, Skipped: ${skippedFramesCount.get()}, MaxFrame: ${maxFrameDurationMs.get()}ms | Coil RAM Hits: ${coilRamHits.get()}, Disk/Flash Loads: ${coilDiskLoads.get()}, Slow Decodes (>50ms): ${coilSlowDecodes.get()}"
            Log.i("PERF_SCROLL", summary)
            logEvent("PERF_SCROLL", summary)
            resetStats()
        } else if (fastScrolling) {
            logEvent("PERF_SCROLL", "FAST FLING: Velocity=${velocityItemsPerSec} items/sec at Index=$firstVisibleIndex. Pausing Coil disk decodes.")
        }
    }

    fun createCoilListener(): EventListener {
        return object : EventListener() {
            private val requestStartTimes = java.util.concurrent.ConcurrentHashMap<ImageRequest, Long>()

            override fun onStart(request: ImageRequest) {
                requestStartTimes[request] = System.currentTimeMillis()
            }

            override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                val startTime = requestStartTimes.remove(request) ?: return
                val durationMs = System.currentTimeMillis() - startTime
                val source = result.dataSource.name
                if (source.contains("MEMORY", ignoreCase = true)) {
                    coilRamHits.incrementAndGet()
                } else {
                    coilDiskLoads.incrementAndGet()
                    if (durationMs > 50) {
                        coilSlowDecodes.incrementAndGet()
                        val msg = "SLOW COIL DECODE (${durationMs}ms) from $source for ${request.data} while FastScroll=$isFastScrolling"
                        Log.w("PERF_COIL", msg)
                        logEvent("PERF_COIL", msg)
                    }
                }
            }

            override fun onError(request: ImageRequest, result: ErrorResult) {
                requestStartTimes.remove(request)
                val msg = "COIL ERROR for ${request.data}: ${result.throwable.message}"
                Log.e("PERF_COIL", msg)
                logEvent("PERF_COIL", msg)
            }
        }
    }

    private fun logEvent(tag: String, message: String) {
        val timestamp = dateFormatter.format(Date())
        val formatted = "[$timestamp] [$tag] $message"
        Log.i(TAG, formatted)
        logQueue.offer(formatted)
        flushToFile()
    }

    private fun flushToFile() {
        val file = logFile ?: return
        try {
            var line = logQueue.poll()
            if (line != null) {
                FileWriter(file, true).use { writer ->
                    val pw = PrintWriter(writer)
                    while (line != null) {
                        pw.println(line)
                        line = logQueue.poll()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore file write exceptions in perf monitor
        }
    }

    private fun getMainThreadStackTrace(): String {
        return Looper.getMainLooper().thread.stackTrace
            .take(12)
            .joinToString("\n") { "    at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
    }

    private fun resetStats() {
        frameCount.set(0)
        skippedFramesCount.set(0)
        maxFrameDurationMs.set(0)
        coilRamHits.set(0)
        coilDiskLoads.set(0)
        coilSlowDecodes.set(0)
    }

    fun getLogFilePath(context: Context): String {
        return File(context.filesDir, "perf_scroll_logs.txt").absolutePath
    }

    fun clearLogs(context: Context) {
        try {
            File(context.filesDir, "perf_scroll_logs.txt").writeText("")
            logEvent("PERF_INIT", "Scroll logs cleared.")
        } catch (e: Exception) {
            // ignore
        }
    }
}
