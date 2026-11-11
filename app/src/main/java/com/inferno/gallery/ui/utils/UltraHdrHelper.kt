package com.inferno.gallery.ui.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Build
import android.view.Window

/**
 * Helper utility for detecting Ultra HDR gainmaps and managing dynamic window color mode
 * transitions per Google's official Android 14+ HDR guidance.
 */
object UltraHdrHelper {

    /**
     * Checks if the device's current display is HDR capable and running Android 14+ (API 34+).
     */
    fun isHdrSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        val display = context.display ?: return false
        return display.isHdr
    }

    /**
     * Checks if a decoded Bitmap carries an embedded Ultra HDR gainmap.
     */
    fun hasGainmap(bitmap: Bitmap?): Boolean {
        if (bitmap == null) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        return try {
            bitmap.hasGainmap()
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Dynamically switches the Window's color mode between HDR and default SDR.
     * Google recommends switching dynamically only when actively displaying an Ultra HDR image
     * to conserve battery and avoid dimming standard SDR UI elements.
     */
    fun setWindowHdrMode(window: Window?, enableHdr: Boolean) {
        if (window == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        try {
            val targetMode = if (enableHdr) {
                ActivityInfo.COLOR_MODE_HDR
            } else {
                ActivityInfo.COLOR_MODE_DEFAULT
            }
            if (window.colorMode != targetMode) {
                window.colorMode = targetMode
            }
        } catch (e: Exception) {
            android.util.Log.e("UltraHdrHelper", "Failed to set window colorMode to HDR=$enableHdr", e)
        }
    }

    /**
     * Recursively traverses ContextWrappers to find the hosting Activity.
     */
    fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}

