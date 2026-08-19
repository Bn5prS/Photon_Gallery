package com.inferno.gallery.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.graphics.scale
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.sqrt

object WallpaperSeedExtractor {
    private const val TAG = "WallpaperSeedExtractor"
    private const val MAX_WALLPAPER_EXTRACTION_AREA = 112 * 112

    suspend fun getWallpaperSeedColor(context: Context): Int? = withContext(Dispatchers.IO) {
        try {
            // 0. Try native Android 12+ Monet system accent color
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                try {
                    val systemColor = context.getColor(android.R.color.system_accent1_500)
                    if (systemColor != 0) {
                        return@withContext systemColor
                    }
                } catch (e: Exception) {
                    // Fallback to WallpaperManager
                }
            }

            val wallpaperManager = WallpaperManager.getInstance(context)
            
            // 1. Try to get colors natively (Android 8.1+)
            val nativeColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            if (nativeColors != null) {
                return@withContext nativeColors.primaryColor.toArgb()
            }

            // 2. Fallback: extract from raw wallpaper bitmap
            val bitmap = loadWallpaperBitmap(wallpaperManager)
            if (bitmap != null) {
                return@withContext extractBestColor(bitmap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract wallpaper seed color", e)
        }
        return@withContext null
    }

    private fun loadWallpaperBitmap(wallpaperManager: WallpaperManager): Bitmap? {
        try {
            // Static wallpaper
            val fileDesc = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
            if (fileDesc != null) {
                val decoded = BitmapFactory.decodeFileDescriptor(fileDesc.fileDescriptor)
                fileDesc.close()
                if (decoded != null) return decoded
            }
        } catch (e: Exception) {
            // Ignored, try fallback
        }

        try {
            // Built-in wallpaper
            val drawable = wallpaperManager.getBuiltInDrawable(WallpaperManager.FLAG_SYSTEM)
            if (drawable != null) {
                return drawableToBitmap(drawable)
            }
        } catch (e: Exception) {
            // Ignored
        }
        return null
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun extractBestColor(originalBitmap: Bitmap): Int? {
        var bitmap = originalBitmap
        val area = bitmap.width * bitmap.height
        
        // Downscale for performance if too large
        if (area > MAX_WALLPAPER_EXTRACTION_AREA) {
            val scale = sqrt(MAX_WALLPAPER_EXTRACTION_AREA.toDouble() / area)
            var newWidth = (bitmap.width * scale).toInt()
            var newHeight = (bitmap.height * scale).toInt()
            if (newWidth <= 0) newWidth = 1
            if (newHeight <= 0) newHeight = 1
            bitmap = bitmap.scale(newWidth, newHeight, false)
        }

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val quantizerResult = QuantizerCelebi.quantize(pixels, 128)
        val bestColors = Score.score(quantizerResult, 12)
        
        if (bestColors.isNotEmpty()) {
            return bestColors[0]
        }
        return null
    }
}
