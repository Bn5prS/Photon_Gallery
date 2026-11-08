package com.inferno.gallery.workers.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Intelligent hardware-aware pacer for background AI scanning tasks (CLIP Smart Search,
 * ML Kit OCR, ONNX Face Recognition).
 *
 * Implements a strict 50-image batch cadence:
 * - Every 50 items: Mandatory 3.5s cooldown pause to let CPU frequencies idle down.
 * - Adaptive thermal protection:
 *     - Normal (<38°C): 50 items -> 3500ms cooldown + 20ms inter-item micro-yield.
 *     - Warm (38°C-41.5°C): 25 items -> 5000ms cooldown + 75ms inter-item delay.
 *     - Hot (>=42°C or THERMAL_STATUS_SEVERE): Enforces safety sleep (pauses in 10s increments
 *       until temperatures subside below 39.5°C).
 * - Cooperative cancellation: Evaluates `isStopped()` before and during every pause.
 */
class ScanThermalPacer(
    private val context: Context,
    private val workerTag: String,
    private val normalBatchSize: Int = 50,
    private val normalCooldownMs: Long = 3500L
) {
    companion object {
        private const val TAG = "ScanThermalPacer"
        private const val WARM_TEMP_CELSIUS = 38.0f
        private const val HOT_TEMP_CELSIUS = 42.0f
        private const val RESUME_TEMP_CELSIUS = 39.5f
    }

    private var itemsInCurrentBatch = 0
    private var totalProcessedInSession = 0

    private val powerManager: PowerManager? = try {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    } catch (_: Exception) {
        null
    }

    /**
     * Reads current battery temperature in Celsius via sticky battery intent.
     * Returns Float.NaN if unavailable.
     */
    fun getBatteryTemperature(): Float {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            if (tempTenths > 0) {
                tempTenths / 10.0f
            } else {
                Float.NaN
            }
        } catch (_: Exception) {
            Float.NaN
        }
    }

    /**
     * Reads Android 10+ OS thermal status (0 = NONE, 1 = LIGHT, 2 = MODERATE, 3 = SEVERE, 4 = CRITICAL).
     */
    fun getThermalStatus(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            try {
                powerManager.currentThermalStatus
            } catch (_: Exception) {
                0
            }
        } else {
            0
        }
    }

    /**
     * Called by workers after finishing each image.
     * Evaluates counts, hardware thermals, and executes appropriate cooldown.
     */
    suspend fun recordItemAndThrottle(isStopped: () -> Boolean) {
        if (isStopped()) return

        itemsInCurrentBatch++
        totalProcessedInSession++

        val currentTemp = getBatteryTemperature()
        val thermalStatus = getThermalStatus()

        val isSevereThermal = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) ||
                (!currentTemp.isNaN() && currentTemp >= HOT_TEMP_CELSIUS)

        val isModerateThermal = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalStatus >= PowerManager.THERMAL_STATUS_LIGHT) ||
                (!currentTemp.isNaN() && currentTemp >= WARM_TEMP_CELSIUS)

        // 1. Extreme Heat Safety Suspension (>=42°C or SEVERE)
        if (isSevereThermal) {
            Log.w(
                TAG,
                "[$workerTag] High thermal threshold reached! Temp: ${currentTemp}°C, Status: $thermalStatus. Suspending scanning for cooldown."
            )
            var elapsedWaitMs = 0L
            val maxWaitMs = 60_000L
            while (elapsedWaitMs < maxWaitMs && !isStopped()) {
                delay(10_000L)
                elapsedWaitMs += 10_000L
                val refreshedTemp = getBatteryTemperature()
                val refreshedStatus = getThermalStatus()
                val isNowSafe = (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || refreshedStatus <= PowerManager.THERMAL_STATUS_LIGHT) &&
                        (refreshedTemp.isNaN() || refreshedTemp <= RESUME_TEMP_CELSIUS)
                if (isNowSafe) {
                    Log.i(TAG, "[$workerTag] Device cooled to ${refreshedTemp}°C. Resuming scanning.")
                    break
                }
            }
            itemsInCurrentBatch = 0
            return
        }

        // 2. Batch Cooldown Trigger (every 50 images, or every 25 images when warm)
        val targetBatchSize = if (isModerateThermal) normalBatchSize / 2 else normalBatchSize
        if (itemsInCurrentBatch >= targetBatchSize) {
            val cooldownDuration = if (isModerateThermal) 5000L else normalCooldownMs
            Log.i(
                TAG,
                "[$workerTag] Completed batch of $itemsInCurrentBatch images (Total: $totalProcessedInSession). " +
                        "Applying cooldown for ${cooldownDuration}ms (Temp: ${if (currentTemp.isNaN()) "N/A" else "${currentTemp}°C"}, Status: $thermalStatus)"
            )
            itemsInCurrentBatch = 0

            // Sleep cooperatively in small chunks so cancellation stops immediately
            var elapsed = 0L
            val chunk = 500L
            while (elapsed < cooldownDuration && !isStopped()) {
                delay(chunk)
                elapsed += chunk
            }
            return
        }

        // 3. Inter-item micro yield to keep thread scheduler responsive
        val interItemDelay = if (isModerateThermal) 75L else 20L
        delay(interItemDelay)
    }
}

