package com.inferno.gallery.workers.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Deterministic batch-cadence pacer for background AI scanning tasks (CLIP Smart Search,
 * ML Kit OCR, ONNX Face Recognition).
 *
 * Implements a reliable, hardware-friendly 50-image cadence:
 * - Every 50 items: Mandatory 4-second cooldown pause to allow CPU clock frequencies to scale
 *   down, flush memory allocations, and let hardware dissipate heat.
 * - Between individual items: Gentle 25ms micro-yield so the UI thread and 120Hz display flings
 *   never compete with inference.
 * - Progress Guarantee: Completely deterministic and immune to ambient temperature fluctuations
 *   or charging heat, ensuring scanning never gets trapped in an indefinite wait loop.
 * - Cooperative cancellation: Evaluates `isStopped()` before and during every pause.
 */
class ScanThermalPacer(
    private val context: Context,
    private val workerTag: String,
    private val batchSize: Int = 50,
    private val cooldownMs: Long = 4000L
) {
    companion object {
        private const val TAG = "ScanThermalPacer"
        private const val INTER_ITEM_YIELD_MS = 25L
    }

    private var itemsInCurrentBatch = 0
    private var totalProcessedInSession = 0

    /**
     * Called by workers after finishing each image.
     * Enforces the 50-image cooldown and micro-yield.
     */
    suspend fun recordItemAndThrottle(isStopped: () -> Boolean) {
        if (isStopped()) return

        itemsInCurrentBatch++
        totalProcessedInSession++

        // 1. Mandatory 50-image cooldown
        if (itemsInCurrentBatch >= batchSize) {
            Log.i(
                TAG,
                "[$workerTag] Completed batch of $itemsInCurrentBatch images (Total processed: $totalProcessedInSession). " +
                        "Pausing ${cooldownMs}ms for hardware cooldown."
            )
            itemsInCurrentBatch = 0

            // Sleep cooperatively in 500ms intervals so user/system cancellation is instant
            var elapsed = 0L
            val chunk = 500L
            while (elapsed < cooldownMs && !isStopped()) {
                delay(chunk)
                elapsed += chunk
            }
            return
        }

        // 2. Inter-item micro yield to keep thread scheduler and UI smooth
        delay(INTER_ITEM_YIELD_MS)
    }
}
