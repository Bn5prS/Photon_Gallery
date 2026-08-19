package com.inferno.gallery.data

import android.content.ContentValues
import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.VaultDao
import com.inferno.gallery.data.db.VaultMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Handles file operations for Private Space:
 * - hideMedia: copy to app-internal vault dir, delete from MediaStore
 * - unhideMedia: copy back to MediaStore, delete from vault
 * - deleteFromVault: permanently delete vault files
 */
class VaultRepository(
    private val context: Context,
    private val vaultDao: VaultDao
) {
    private val vaultDir: File
        get() = File(context.filesDir, "vault").also { it.mkdirs() }

    val vaultItems: Flow<List<VaultMediaEntity>> = vaultDao.observeAll()
    val vaultCount: Flow<Int> = vaultDao.observeCount()

    /**
     * Hide media items: copy files to internal vault storage and remove from MediaStore.
     * Returns the list of successfully hidden VaultMediaEntity IDs.
     */
    suspend fun hideMedia(uris: List<Uri>): List<Long> = withContext(Dispatchers.IO) {
        val insertedIds = mutableListOf<Long>()
        val entities = mutableListOf<VaultMediaEntity>()

        for (uri in uris) {
            try {
                // Query MediaStore for file info
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_ADDED,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.DATA,
                    MediaStore.MediaColumns.DURATION
                )

                val cursor = context.contentResolver.query(uri, projection, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val displayName = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)) ?: "unknown"
                        val relativePath = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)) ?: ""
                        val mimeType = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
                        val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                        val dateAdded = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
                        val dateModified = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
                        val filePath = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)) ?: ""
                        val duration = try { c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)) } catch (_: Exception) { null }
                        val isVideo = mimeType?.startsWith("video") == true

                        // Determine original bucket from path
                        val originalBucket = filePath.substringBeforeLast("/").substringAfterLast("/")

                        // Generate vault filename
                        val extension = displayName.substringAfterLast(".", "")
                        val vaultFileName = "${UUID.randomUUID()}.$extension"

                        // Copy file to vault directory
                        val vaultFile = File(vaultDir, vaultFileName)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            vaultFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        // Verify copy succeeded
                        if (vaultFile.exists() && vaultFile.length() > 0) {
                            entities.add(
                                VaultMediaEntity(
                                    originalUri = uri.toString(),
                                    originalPath = filePath,
                                    originalBucket = originalBucket,
                                    originalRelativePath = relativePath,
                                    fileName = displayName,
                                    vaultFileName = vaultFileName,
                                    mimeType = mimeType,
                                    isVideo = isVideo,
                                    durationMs = duration,
                                    size = size,
                                    dateAdded = dateAdded,
                                    dateModified = dateModified,
                                    dateHidden = System.currentTimeMillis() / 1000
                                )
                            )

                            // Delete from MediaStore so it disappears from gallery
                            try {
                                context.contentResolver.delete(uri, null, null)
                            } catch (e: Exception) {
                                android.util.Log.w("VaultRepository", "MediaStore delete failed for $uri, trying recoverableSecurityException", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VaultRepository", "Failed to hide: $uri", e)
            }
        }

        // Insert vault records
        if (entities.isNotEmpty()) {
            insertedIds.addAll(vaultDao.insertAll(entities))
        }

        insertedIds
    }

    /**
     * Unhide media items: copy files back to MediaStore with original dates and remove from vault.
     * Returns the number of successfully restored items.
     */
    suspend fun unhideMedia(ids: List<Long>): Int = withContext(Dispatchers.IO) {
        var count = 0
        val items = vaultDao.getByIds(ids)

        for (item in items) {
            try {
                val vaultFile = File(vaultDir, item.vaultFileName)
                if (!vaultFile.exists()) continue

                // Insert back into MediaStore
                val collection = if (item.isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                // Use original relative path if stored, otherwise fall back to bucket
                val restorePath = if (item.originalRelativePath.isNotBlank()) {
                    item.originalRelativePath
                } else {
                    "${Environment.DIRECTORY_PICTURES}/${item.originalBucket}"
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, item.fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, restorePath)
                    put(MediaStore.MediaColumns.DATE_ADDED, item.dateAdded)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, item.dateModified)
                    // DATE_TAKEN is in milliseconds and is what galleries use for grouping
                    // DATE_ADDED is often read-only on insert (Android overrides it with current time)
                    if (item.isVideo) {
                        put(MediaStore.Video.Media.DATE_TAKEN, item.dateAdded * 1000L)
                    } else {
                        put(MediaStore.Images.Media.DATE_TAKEN, item.dateAdded * 1000L)
                    }
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val insertUri = context.contentResolver.insert(collection, values)
                if (insertUri != null) {
                    // Write file content
                    context.contentResolver.openOutputStream(insertUri)?.use { output ->
                        vaultFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }

                    // Mark as complete, re-set dates (some MediaStore impls reset dates on write)
                    // DATE_ADDED is read-only on API 29+, but DATE_MODIFIED can be updated
                    val finalValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                        put(MediaStore.MediaColumns.DATE_MODIFIED, item.dateModified)
                        // Re-set DATE_TAKEN to ensure grouping stays correct
                        if (item.isVideo) {
                            put(MediaStore.Video.Media.DATE_TAKEN, item.dateAdded * 1000L)
                        } else {
                            put(MediaStore.Images.Media.DATE_TAKEN, item.dateAdded * 1000L)
                        }
                    }
                    context.contentResolver.update(insertUri, finalValues, null, null)

                    // Pre-insert a CoreMediaEntity with the ORIGINAL dateAdded into Room
                    // so the gallery groups this item correctly (not under "Today")
                    try {
                        val newId = ContentUris.parseId(insertUri)
                        val db = DatabaseProvider.getDatabase(context)
                        db.mediaDao().insertAll(listOf(
                            CoreMediaEntity(
                                id = newId,
                                uriString = insertUri.toString(),
                                filePath = "", // Will be filled by next MediaSyncWorker run
                                bucketName = item.originalBucket,
                                dateAdded = item.dateAdded, // Original date!
                                dateModified = item.dateModified,
                                size = item.size,
                                name = item.fileName,
                                mimeType = item.mimeType,
                                isVideo = item.isVideo,
                                durationMs = item.durationMs
                            )
                        ))
                    } catch (dbEx: Exception) {
                        android.util.Log.w("VaultRepository", "Pre-insert Room record failed (sync will fix): ${dbEx.message}")
                    }

                    // Delete vault file and record
                    vaultFile.delete()
                    vaultDao.deleteByIds(listOf(item.id))
                    count++
                }
            } catch (e: Exception) {
                android.util.Log.e("VaultRepository", "Failed to unhide: ${item.fileName}", e)
            }
        }
        count
    }

    /**
     * Permanently delete items from vault (no restore).
     */
    suspend fun deleteFromVault(ids: List<Long>) = withContext(Dispatchers.IO) {
        val items = vaultDao.getByIds(ids)
        for (item in items) {
            try {
                File(vaultDir, item.vaultFileName).delete()
            } catch (e: Exception) {
                android.util.Log.e("VaultRepository", "Failed to delete vault file: ${item.vaultFileName}", e)
            }
        }
        vaultDao.deleteByIds(ids)
    }

    /**
     * Get a FileProvider URI for a vault item (for Coil loading / viewing).
     */
    fun getVaultFileUri(entity: VaultMediaEntity): Uri {
        val file = File(vaultDir, entity.vaultFileName)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Get the raw File for a vault item.
     */
    fun getVaultFile(entity: VaultMediaEntity): File {
        return File(vaultDir, entity.vaultFileName)
    }
}
