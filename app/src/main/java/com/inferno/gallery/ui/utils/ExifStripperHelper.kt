package com.inferno.gallery.ui.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.inferno.gallery.ui.GalleryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ExifStripperHelper {

    sealed class StripResult {
        data class Success(val newUri: Uri?, val isCopy: Boolean) : StripResult()
        data class Error(val message: String, val cause: Throwable? = null) : StripResult()
    }

    /**
     * Generates a clean filename appending `_no_exif` before the extension.
     * E.g. "photo.jpg" -> "photo_no_exif.jpg"
     */
    fun generateCleanFilename(originalName: String): String {
        val clean = originalName.trim()
        if (clean.isBlank()) return "photo_no_exif.jpg"
        return if (clean.contains(".")) {
            val baseName = clean.substringBeforeLast(".")
            val ext = clean.substringAfterLast(".")
            "${baseName}_no_exif.${ext}"
        } else {
            "${clean}_no_exif.jpg"
        }
    }

    /**
     * Strips all metadata from the given [item] and writes the result to a new copy
     * in the same relative directory (or fallback to Pictures/Photon).
     */
    suspend fun stripSaveCopy(context: Context, item: GalleryItem): StripResult = withContext(Dispatchers.IO) {
        val tempRaw = try {
            File.createTempFile("raw_strip_", ".tmp", context.cacheDir)
        } catch (e: Exception) {
            return@withContext StripResult.Error("Failed to allocate cache storage for stripping", e)
        }
        val tempClean = try {
            File.createTempFile("clean_strip_", ".tmp", context.cacheDir)
        } catch (e: Exception) {
            tempRaw.delete()
            return@withContext StripResult.Error("Failed to allocate cache storage for stripping", e)
        }

        try {
            // 1. Copy original source stream to tempRaw
            val inputCopied = copyUriToFile(context, item.uri, tempRaw)
            if (!inputCopied || tempRaw.length() == 0L) {
                return@withContext StripResult.Error("Failed to read image content from storage")
            }

            // 2. Resolve mime type
            val mimeType = resolveMimeType(context, item)

            // 3. Strip metadata
            val stripped = MetadataStripper.strip(tempRaw, tempClean, mimeType)
            val fileToCommit = if (stripped && tempClean.exists() && tempClean.length() > 0L) tempClean else tempRaw

            // 4. Determine destination relative path and name
            val newDisplayName = generateCleanFilename(item.name.ifBlank { "photo.jpg" })
            val relativePath = resolveRelativePath(context, item)

            // 5. Insert new entry in MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, newDisplayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val targetUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext StripResult.Error("Failed to create MediaStore entry for clean copy")

            // 6. Write clean bytes to the new URI
            val writeSuccess = context.contentResolver.openOutputStream(targetUri)?.use { out ->
                fileToCommit.inputStream().use { it.copyTo(out) }
                true
            } ?: false

            if (!writeSuccess) {
                context.contentResolver.delete(targetUri, null, null)
                return@withContext StripResult.Error("Failed to write sanitized image to storage")
            }

            // 7. Clear pending state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(targetUri, updateValues, null, null)
            }

            // 8. Invalidate Coil caches and notify MediaScanner
            ImageCacheEvictor.evictMedia(context, targetUri)

            StripResult.Success(newUri = targetUri, isCopy = true)
        } catch (e: Exception) {
            StripResult.Error("Failed to save clean copy: ${e.localizedMessage ?: "Unknown error"}", e)
        } finally {
            tempRaw.delete()
            tempClean.delete()
        }
    }

    /**
     * Strips all metadata from the given [item] and overwrites the original file in-place.
     */
    suspend fun stripOverwrite(context: Context, item: GalleryItem): StripResult = withContext(Dispatchers.IO) {
        val tempRaw = try {
            File.createTempFile("raw_strip_", ".tmp", context.cacheDir)
        } catch (e: Exception) {
            return@withContext StripResult.Error("Failed to allocate cache storage for stripping", e)
        }
        val tempClean = try {
            File.createTempFile("clean_strip_", ".tmp", context.cacheDir)
        } catch (e: Exception) {
            tempRaw.delete()
            return@withContext StripResult.Error("Failed to allocate cache storage for stripping", e)
        }

        try {
            // 1. Copy original source stream to tempRaw
            val inputCopied = copyUriToFile(context, item.uri, tempRaw)
            if (!inputCopied || tempRaw.length() == 0L) {
                return@withContext StripResult.Error("Failed to read image content from storage")
            }

            // 2. Resolve mime type
            val mimeType = resolveMimeType(context, item)

            // 3. Strip metadata
            val stripped = MetadataStripper.strip(tempRaw, tempClean, mimeType)
            if (!stripped || !tempClean.exists() || tempClean.length() == 0L) {
                return@withContext StripResult.Error("Metadata stripping engine did not generate a cleaned file")
            }

            // 4. Overwrite original file
            val writeSuccess = if (item.uri.scheme == "file" || item.uri.scheme == null) {
                val file = File(item.uri.path ?: item.path)
                FileOutputStream(file).use { out ->
                    tempClean.inputStream().use { it.copyTo(out) }
                }
                true
            } else {
                context.contentResolver.openOutputStream(item.uri, "rwt")?.use { out ->
                    tempClean.inputStream().use { it.copyTo(out) }
                    true
                } ?: false
            }

            if (!writeSuccess) {
                return@withContext StripResult.Error("Failed to open output stream to overwrite original photo")
            }

            // 5. Update DATE_MODIFIED in MediaStore
            if (item.uri.scheme == "content") {
                try {
                    val cv = ContentValues().apply {
                        put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                    }
                    context.contentResolver.update(item.uri, cv, null, null)
                } catch (_: Exception) {}
            }

            // 6. Evict caches across Coil and rescan
            ImageCacheEvictor.evictMedia(context, item.uri, item.id)

            StripResult.Success(newUri = item.uri, isCopy = false)
        } catch (e: SecurityException) {
            StripResult.Error("Write permission denied for original photo", e)
        } catch (e: Exception) {
            StripResult.Error("Failed to overwrite photo: ${e.localizedMessage ?: "Unknown error"}", e)
        } finally {
            tempRaw.delete()
            tempClean.delete()
        }
    }

    private fun copyUriToFile(context: Context, uri: Uri, destFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveMimeType(context: Context, item: GalleryItem): String {
        val resolverType = try { context.contentResolver.getType(item.uri) } catch (_: Exception) { null }
        if (!resolverType.isNullOrBlank()) return resolverType
        val lowerName = item.name.lowercase()
        return when {
            lowerName.endsWith(".png") -> "image/png"
            lowerName.endsWith(".webp") -> "image/webp"
            lowerName.endsWith(".gif") -> "image/gif"
            else -> "image/jpeg"
        }
    }

    private fun resolveRelativePath(context: Context, item: GalleryItem): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val projection = arrayOf(MediaStore.MediaColumns.RELATIVE_PATH)
                context.contentResolver.query(item.uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val path = cursor.getString(0)
                        if (!path.isNullOrBlank()) return path
                    }
                }
            } catch (_: Exception) {}
        }
        return Environment.DIRECTORY_PICTURES + "/Photon"
    }
}

