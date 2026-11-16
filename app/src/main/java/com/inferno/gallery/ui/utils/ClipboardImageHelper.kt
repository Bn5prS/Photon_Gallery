package com.inferno.gallery.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.inferno.gallery.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies an image [Uri] into the system clipboard as a FileProvider content URI
 * so messaging apps, keyboards, and editors can paste it directly.
 */
object ClipboardImageHelper {

    suspend fun copyImageToClipboard(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        val clipUri = withContext(Dispatchers.IO) {
            runCatching {
                val clipDir = File(appContext.cacheDir, "clipboard_images").apply {
                    if (exists()) {
                        listFiles()?.forEach { old -> runCatching { old.delete() } }
                    } else {
                        mkdirs()
                    }
                }
                val mimeType = appContext.contentResolver.getType(uri)
                val ext = when (mimeType) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    "image/gif" -> "gif"
                    "image/heic", "image/heif" -> "heic"
                    else -> "jpg"
                }
                val destFile = File(clipDir, "clip_${System.currentTimeMillis()}.$ext")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@runCatching null

                if (!destFile.exists() || destFile.length() == 0L) return@runCatching null

                FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    destFile
                )
            }.getOrNull()
        }

        withContext(Dispatchers.Main) {
            if (clipUri != null) {
                val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newUri(appContext.contentResolver, "Image", clipUri)
                clipboard.setPrimaryClip(clip)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(
                        appContext,
                        appContext.getString(R.string.toast_copied_to_clipboard),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    appContext,
                    appContext.getString(R.string.toast_copy_clipboard_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
