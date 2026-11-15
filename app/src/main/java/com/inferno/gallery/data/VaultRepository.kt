package com.inferno.gallery.data

import android.content.ContentValues
import android.content.Context
import android.content.ContentUris
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.VaultDao
import com.inferno.gallery.data.db.VaultMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles file operations for Private Space:
 * - hideMedia: encrypt & copy to app-internal vault dir, delete original from storage/MediaStore
 * - unhideMedia: decrypt & copy back to MediaStore with original dates, delete from vault
 * - deleteFromVault: permanently delete vault files
 */
class VaultRepository(
    private val context: Context,
    private val vaultDao: VaultDao
) {
    private val vaultDir: File
        get() = File(context.filesDir, "vault").also { it.mkdirs() }

    private val previewCacheDir: File
        get() = File(context.cacheDir, "vault_preview").also { it.mkdirs() }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "photon_vault_aes_key"
        private val ENC_MAGIC = "PVENC1".toByteArray(Charsets.US_ASCII) // 6 bytes
        private const val GCM_IV_LEN = 12
        private const val GCM_TAG_BITS = 128
    }

    private fun getOrCreateAesKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    private fun writeEncrypted(input: InputStream, destFile: File) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateAesKey())
        val iv = cipher.iv // 12 bytes
        destFile.outputStream().use { rawOut ->
            rawOut.write(ENC_MAGIC)
            rawOut.write(iv)
            CipherOutputStream(rawOut, cipher).use { cipherOut ->
                input.copyTo(cipherOut)
            }
        }
    }

    private fun copyDecrypted(vaultFile: File, output: OutputStream) {
        vaultFile.inputStream().use { rawIn ->
            val header = ByteArray(ENC_MAGIC.size)
            val readMagic = rawIn.read(header)
            if (readMagic == ENC_MAGIC.size && header.contentEquals(ENC_MAGIC)) {
                val iv = ByteArray(GCM_IV_LEN)
                var ivRead = 0
                while (ivRead < GCM_IV_LEN) {
                    val r = rawIn.read(iv, ivRead, GCM_IV_LEN - ivRead)
                    if (r <= 0) break
                    ivRead += r
                }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateAesKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
                CipherInputStream(rawIn, cipher).use { cipherIn ->
                    cipherIn.copyTo(output)
                }
            } else {
                // Legacy unencrypted file fallback
                if (readMagic > 0) {
                    output.write(header, 0, readMagic)
                }
                rawIn.copyTo(output)
            }
        }
    }

    fun clearPreviewCache() {
        try {
            previewCacheDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    /**
     * Returns a decrypted File in cacheDir/vault_preview for in-vault viewing/playback.
     * Legacy unencrypted files are returned directly without copying.
     */
    suspend fun getViewableVaultFile(entity: VaultMediaEntity): File? = withContext(Dispatchers.IO) {
        val vaultFile = File(vaultDir, entity.vaultFileName)
        if (!vaultFile.exists()) return@withContext null

        val isEncrypted = try {
            vaultFile.inputStream().use { ins ->
                val header = ByteArray(ENC_MAGIC.size)
                ins.read(header) == ENC_MAGIC.size && header.contentEquals(ENC_MAGIC)
            }
        } catch (_: Exception) {
            false
        }
        if (!isEncrypted) return@withContext vaultFile

        val cached = File(previewCacheDir, entity.vaultFileName)
        if (cached.exists() && cached.length() > 0L) return@withContext cached
        return@withContext try {
            cached.outputStream().use { out -> copyDecrypted(vaultFile, out) }
            cached
        } catch (e: Exception) {
            cached.delete()
            null
        }
    }

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
                if (uri.scheme == "file") {
                    val srcFile = File(uri.path ?: continue)
                    if (!srcFile.exists() || !srcFile.isFile) continue

                    val displayName = srcFile.name
                    val extension = displayName.substringAfterLast(".", "")
                    val mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(extension.lowercase())
                        ?: if (extension.lowercase() in listOf("mp4", "mov", "mkv", "3gp", "webm")) "video/mp4" else "image/jpeg"
                    val isVideo = mimeType.startsWith("video")
                    val filePath = srcFile.absolutePath
                    val originalBucket = srcFile.parentFile?.name ?: "Gallery"
                    val relativePath = filePath.substringAfter("/storage/emulated/0/", "")
                        .substringBeforeLast("/", "")
                        .let { if (it.isNotBlank()) "$it/" else "" }
                    val size = srcFile.length()
                    val dateModified = srcFile.lastModified() / 1000L
                    val dateAdded = dateModified

                    val vaultFileName = if (extension.isNotBlank()) "${UUID.randomUUID()}.$extension" else UUID.randomUUID().toString()
                    val vaultFile = File(vaultDir, vaultFileName)
                    srcFile.inputStream().use { input ->
                        writeEncrypted(input, vaultFile)
                    }

                    if (vaultFile.exists() && vaultFile.length() > 0L) {
                        val deleted = srcFile.delete()
                        if (deleted) {
                            MediaScannerConnection.scanFile(context.applicationContext, arrayOf(filePath), null, null)
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
                                    durationMs = null,
                                    size = size,
                                    dateAdded = dateAdded,
                                    dateModified = dateModified,
                                    dateHidden = System.currentTimeMillis() / 1000
                                )
                            )
                        } else {
                            // Never leave a duplicate if original could not be deleted
                            vaultFile.delete()
                            android.util.Log.w("VaultRepository", "Aborting hide: could not delete source file $filePath")
                        }
                    }
                    continue
                }

                // Query MediaStore for content:// URI info
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
                        val vaultFileName = if (extension.isNotBlank()) "${UUID.randomUUID()}.$extension" else UUID.randomUUID().toString()

                        // Encrypt & copy file to vault directory
                        val vaultFile = File(vaultDir, vaultFileName)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            writeEncrypted(input, vaultFile)
                        }

                        // Verify copy succeeded, then delete original (abort on delete failure to prevent duplicates)
                        if (vaultFile.exists() && vaultFile.length() > 0) {
                            val deletedFromStore = try {
                                context.contentResolver.delete(uri, null, null) > 0
                            } catch (e: Exception) {
                                android.util.Log.w("VaultRepository", "MediaStore delete failed for $uri, trying direct file delete", e)
                                false
                            }
                            val deletedDirect = if (!deletedFromStore && filePath.isNotBlank()) {
                                val f = File(filePath)
                                val ok = f.exists() && f.delete()
                                if (ok) {
                                    MediaScannerConnection.scanFile(context.applicationContext, arrayOf(filePath), null, null)
                                }
                                ok
                            } else false

                            if (deletedFromStore || deletedDirect) {
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
                            } else {
                                vaultFile.delete()
                                android.util.Log.w("VaultRepository", "Aborting hide: original could not be deleted for $uri")
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

            // Immediately purge hidden media from Room database tables so they NEVER show anywhere in the app
            try {
                val db = DatabaseProvider.getDatabase(context)
                val favManager = FavoritesManager(context)

                val mediaIds = mutableListOf<Long>()
                for (entity in entities) {
                    val parsedId = try { ContentUris.parseId(Uri.parse(entity.originalUri)) } catch (_: Exception) { null }
                    if (parsedId != null) {
                        mediaIds.add(parsedId)
                    } else if (entity.originalPath.isNotBlank()) {
                        db.mediaDao().getMediaByPath(entity.originalPath)?.id?.let { mediaIds.add(it) }
                    }
                    db.mediaDao().deleteByUri(entity.originalUri)
                }

                if (mediaIds.isNotEmpty()) {
                    db.mediaDao().deleteByIds(mediaIds)
                    for (id in mediaIds) {
                        db.embeddingDao().deleteEmbedding(id)
                        db.embeddingStatusDao().deleteStatus(id)
                    }
                    favManager.removeFavorites(mediaIds.map { it.toString() }.toSet())
                    DatabaseProvider.deleteFtsRows(db, mediaIds)
                }
            } catch (e: Exception) {
                android.util.Log.e("VaultRepository", "Error cleaning DB tables after hide: ${e.message}", e)
            }
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
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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
                    // Decrypt & write file content
                    context.contentResolver.openOutputStream(insertUri)?.use { output ->
                        copyDecrypted(vaultFile, output)
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

                    // Resolve actual MediaStore filePath so Room record is immediately complete
                    val resolvedFilePath = try {
                        context.contentResolver.query(insertUri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { c ->
                            if (c.moveToFirst()) c.getString(0) else null
                        }
                    } catch (_: Exception) { null } ?: ""

                    // Pre-insert a CoreMediaEntity with the ORIGINAL dateAdded into Room
                    // so the gallery groups this item correctly (not under "Today")
                    try {
                        val newId = ContentUris.parseId(insertUri)
                        val db = DatabaseProvider.getDatabase(context)
                        db.mediaDao().insertAll(listOf(
                            CoreMediaEntity(
                                id = newId,
                                uriString = insertUri.toString(),
                                filePath = resolvedFilePath,
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
