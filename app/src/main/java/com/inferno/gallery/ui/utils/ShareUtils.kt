package com.inferno.gallery.ui.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.inferno.gallery.data.MediaUriUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object ShareUtils {

    internal fun normalizeMimeType(rawMime: String?, fallback: String = "image/*"): String {
        val cleaned = rawMime
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?: return fallback
        val slashIndex = cleaned.indexOf('/')
        return if (slashIndex > 0 && slashIndex < cleaned.lastIndex) cleaned else fallback
    }

    internal fun computeSharedMimeType(mimeTypes: List<String>): String {
        if (mimeTypes.isEmpty()) return "image/*"
        val normalized = mimeTypes.map { normalizeMimeType(it) }
        val allImages = normalized.all { it.startsWith("image/") }
        val allVideos = normalized.all { it.startsWith("video/") }

        return when {
            normalized.size == 1 -> normalized.first()
            allImages -> if (normalized.distinct().size == 1) normalized.first() else "image/*"
            allVideos -> if (normalized.distinct().size == 1) normalized.first() else "video/*"
            else -> "*/*"
        }
    }

    internal fun isOwnFileProviderUri(uriAuthority: String?, packageName: String): Boolean {
        return !uriAuthority.isNullOrBlank() && uriAuthority == "$packageName.fileprovider"
    }

    fun findActivity(context: Context): Activity? {
        var current: Context? = context
        var depth = 0
        while (current is ContextWrapper && depth < 16) {
            if (current is Activity) return current
            val next = current.baseContext
            if (next === current) break
            current = next
            depth++
        }
        return current as? Activity
    }

    fun resolveMimeType(context: Context, uri: Uri): String {
        val raw = context.contentResolver.getType(uri) ?: run {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (!extension.isNullOrEmpty()) {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT))
            } else null
        }
        return normalizeMimeType(raw)
    }

    private fun getSharedMimeType(context: Context, uris: List<Uri>): String {
        if (uris.isEmpty()) return "image/*"
        return computeSharedMimeType(uris.map { resolveMimeType(context, it) })
    }

    suspend fun shareMedia(
        context: Context,
        uris: List<Uri>,
        stripMetadata: Boolean
    ) {
        val validUris = uris.filterNotNull()
        if (validUris.isEmpty()) return

        val urisToShare: List<Uri> = if (stripMetadata) {
            withContext(Dispatchers.IO) {
                val shareDir = File(context.cacheDir, "shared_images").also { it.mkdirs() }
                // Housekeeping: remove old temporary share files older than 24h
                try {
                    val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                    shareDir.listFiles()?.forEach { oldFile ->
                        if (oldFile.lastModified() < oneDayAgo) oldFile.delete()
                    }
                } catch (_: Exception) {}

                validUris.mapIndexed { idx, rawUri ->
                    val uri = MediaUriUtils.normalizeMediaUri(rawUri)
                    try {
                        val mimeType = resolveMimeType(context, uri)
                        val extension = when {
                            mimeType.contains("png", true) -> "png"
                            mimeType.contains("webp", true) -> "webp"
                            mimeType.contains("heic", true) || mimeType.contains("heif", true) -> "heic"
                            mimeType.contains("gif", true) -> "gif"
                            mimeType.startsWith("video/", true) -> {
                                when {
                                    mimeType.contains("mp4", true) -> "mp4"
                                    mimeType.contains("3gp", true) -> "3gp"
                                    mimeType.contains("mkv", true) -> "mkv"
                                    mimeType.contains("quicktime", true) -> "mov"
                                    else -> "mp4"
                                }
                            }
                            else -> "jpg"
                        }

                        val timeStamp = System.currentTimeMillis()
                        val tempRawFile = File(shareDir, "raw_${timeStamp}_$idx.$extension")
                        val cleanFile = File(shareDir, "share_${timeStamp}_$idx.$extension")

                        context.contentResolver.openInputStream(uri)?.use { input ->
                            tempRawFile.outputStream().use { output -> input.copyTo(output) }
                        }

                        val stripped = MetadataStripper.strip(tempRawFile, cleanFile, mimeType)
                        val fileToShare = if (stripped && cleanFile.exists() && cleanFile.length() > 0L) {
                            try { tempRawFile.delete() } catch (_: Exception) {}
                            cleanFile
                        } else {
                            tempRawFile
                        }

                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            fileToShare
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ShareUtils", "Failed to strip metadata for $uri", e)
                        uri
                    }
                }
            }
        } else {
            validUris.map { MediaUriUtils.normalizeMediaUri(it) }
        }

        if (urisToShare.isEmpty()) return

        val effectiveMimeType = if (urisToShare.size == 1) {
            resolveMimeType(context, urisToShare.first())
        } else {
            getSharedMimeType(context, urisToShare)
        }

        // Use authentic media MIME type (matching AOSP Intent.migrateExtraStreamToClipData)
        // instead of ClipData.newRawUri which emits text/uri-list.
        val clipData = ClipData(
            ClipDescription(null, arrayOf(effectiveMimeType)),
            ClipData.Item(urisToShare.first())
        ).apply {
            for (i in 1 until urisToShare.size) {
                addItem(ClipData.Item(urisToShare[i]))
            }
        }

        val intent = if (urisToShare.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = effectiveMimeType
                putExtra(Intent.EXTRA_STREAM, urisToShare.first())
                this.clipData = clipData
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = effectiveMimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(urisToShare))
                this.clipData = clipData
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        val hostActivity = findActivity(context)
        // Intent.createChooser automatically migrates target ClipData and URI grant flags.
        val chooser = Intent.createChooser(intent, "Share via").apply {
            if (hostActivity == null) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // Only grant explicit read permissions for our own FileProvider URIs (metadata-stripped copies)
        // so Direct Share targets inherit full access without spamming failed grants on MediaStore URIs.
        val ownFileProviderUris = urisToShare.filter { isOwnFileProviderUri(it.authority, context.packageName) }
        if (ownFileProviderUris.isNotEmpty()) {
            try {
                val resInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                }
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    for (uri in ownFileProviderUris) {
                        try {
                            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ShareUtils", "Error granting URI permissions to share targets", e)
            }
        }

        try {
            (hostActivity ?: context).startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            android.util.Log.e("ShareUtils", "No activity found to handle share chooser", e)
        }
    }
}
