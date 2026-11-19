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
import kotlinx.coroutines.sync.withLock
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
    private val hideMutex = kotlinx.coroutines.sync.Mutex()
    private val previewMutex = kotlinx.coroutines.sync.Mutex()
    private val vaultDir: File
        get() = VaultPersistentStorage.getOrCreateVaultDir(context)

    private val legacyVaultDir: File
        get() = File(context.filesDir, "vault")

    private fun resolveExistingVaultFile(vaultFileName: String): File {
        val primary = File(vaultDir, vaultFileName)
        if (primary.exists()) return primary
        val legacy = File(legacyVaultDir, vaultFileName)
        return if (legacy.exists()) legacy else primary
    }

    private val previewCacheDir: File
        get() = File(context.cacheDir, "vault_preview").also { it.mkdirs() }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "photon_vault_aes_key"
        private val ENC_MAGIC = "PVENC1".toByteArray(Charsets.US_ASCII) // 6 bytes
        private val ENC_MAGIC_V2 = "PVENC2".toByteArray(Charsets.US_ASCII) // 6 bytes (DEK)
        private val BACKUP_MAGIC = "PVBKP1".toByteArray(Charsets.US_ASCII) // 6 bytes
        private const val BACKUP_SALT_LEN = 16
        private const val BACKUP_PBKDF2_ITERATIONS = 100_000
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
        val dek = VaultPersistentStorage.activeDek
        val magic = if (dek != null) ENC_MAGIC_V2 else ENC_MAGIC
        val key = dek ?: getOrCreateAesKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv // 12 bytes
        destFile.outputStream().use { rawOut ->
            rawOut.write(magic)
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
            val isV2 = readMagic == ENC_MAGIC_V2.size && header.contentEquals(ENC_MAGIC_V2)
            val isV1 = readMagic == ENC_MAGIC.size && header.contentEquals(ENC_MAGIC)
            if (isV2 || isV1) {
                val iv = ByteArray(GCM_IV_LEN)
                var ivRead = 0
                while (ivRead < GCM_IV_LEN) {
                    val r = rawIn.read(iv, ivRead, GCM_IV_LEN - ivRead)
                    if (r <= 0) break
                    ivRead += r
                }
                val key = if (isV2) {
                    VaultPersistentStorage.activeDek ?: throw IllegalStateException("Private Space locked")
                } else {
                    getOrCreateAesKey()
                }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
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

    private fun removePreviewFiles(vaultFileName: String) {
        try {
            previewCacheDir.listFiles()?.forEach { file ->
                if (file.name == vaultFileName || file.name.startsWith("${vaultFileName}.")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Returns a decrypted File in cacheDir/vault_preview for in-vault viewing/playback.
     * Legacy unencrypted files are returned directly without copying.
     */
    suspend fun getViewableVaultFile(entity: VaultMediaEntity): File? = withContext(Dispatchers.IO) {
        val vaultFile = resolveExistingVaultFile(entity.vaultFileName)
        if (!vaultFile.exists()) return@withContext null

        val isEncrypted = try {
            vaultFile.inputStream().use { ins ->
                val header = ByteArray(ENC_MAGIC.size)
                ins.read(header) == ENC_MAGIC.size &&
                    (header.contentEquals(ENC_MAGIC) || header.contentEquals(ENC_MAGIC_V2))
            }
        } catch (_: Exception) {
            false
        }
        if (!isEncrypted) return@withContext vaultFile

        val existingExt = entity.vaultFileName.substringAfterLast('.', "")
        val fallbackExt = entity.fileName.substringAfterLast('.', "").ifBlank {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(entity.mimeType).orEmpty()
        }
        val previewName = if (existingExt.isNotBlank()) {
            entity.vaultFileName
        } else if (fallbackExt.isNotBlank()) {
            "${entity.vaultFileName}.$fallbackExt"
        } else {
            entity.vaultFileName
        }

        val cached = File(previewCacheDir, previewName)
        if (cached.exists() && cached.length() > 0L) return@withContext cached

        previewMutex.withLock {
            if (cached.exists() && cached.length() > 0L) return@withLock cached
            val tmp = File(previewCacheDir, "tmp_${System.nanoTime()}_$previewName")
            try {
                tmp.outputStream().use { out -> copyDecrypted(vaultFile, out) }
                if (tmp.length() <= 0L) {
                    tmp.delete()
                    return@withLock null
                }
                if (cached.exists()) cached.delete()
                if (tmp.renameTo(cached)) cached else tmp
            } catch (_: Exception) {
                tmp.delete()
                cached.delete()
                null
            }
        }
    }

    val vaultItems: Flow<List<VaultMediaEntity>> = vaultDao.observeAll()
    val vaultCount: Flow<Int> = vaultDao.observeCount()

    data class StagedVaultItem(
        val deleteUri: Uri,
        val entity: VaultMediaEntity
    )

    data class HideMediaResult(
        val hiddenIds: List<Long>,
        val stagedForDelete: List<StagedVaultItem>
    )

    private fun resolveMediaStoreContentUri(filePath: String, isVideo: Boolean): Uri? {
        if (filePath.isBlank()) return null
        return try {
            val baseUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                baseUri,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DATA} = ?",
                arrayOf(filePath),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    ContentUris.withAppendedId(baseUri, c.getLong(0))
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun commitEntitiesAndPurgeDb(entities: List<VaultMediaEntity>): List<Long> {
        if (entities.isEmpty()) return emptyList()
        val insertedIds = vaultDao.insertAll(entities)
        persistManifest()

        // Immediately purge hidden media from Room database tables so they NEVER show anywhere in the app
        try {
            val db = DatabaseProvider.getDatabase(context)
            val favManager = FavoritesManager(context)

            val mediaIds = mutableListOf<Long>()
            val uriVariants = mutableListOf<String>()
            for (entity in entities) {
                val parsedId = try { ContentUris.parseId(Uri.parse(entity.originalUri)) } catch (_: Exception) { null }
                if (parsedId != null) {
                    mediaIds.add(parsedId)
                } else if (entity.originalPath.isNotBlank()) {
                    db.mediaDao().getMediaByPath(entity.originalPath)?.id?.let { mediaIds.add(it) }
                }
                uriVariants.add(entity.originalUri)
                uriVariants.add(MediaUriUtils.normalizeUriString(entity.originalUri))
                if (entity.originalUri.startsWith("content://media/external/")) {
                    uriVariants.add(entity.originalUri.replaceFirst("content://media/external/", "content://media/external_primary/"))
                }
            }

            if (uriVariants.isNotEmpty()) {
                db.mediaDao().deleteByUrisChunked(uriVariants.distinct())
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
        return insertedIds
    }

    suspend fun hideMedia(uris: List<Uri>): List<Long> = hideMediaWithStaging(uris).hiddenIds

    /**
     * Encrypts and moves media into Private Space. Items whose originals cannot be silently
     * deleted under Scoped Storage are staged with their encrypted copy kept intact so the UI
     * can trigger a single MediaStore delete confirmation and finish via [commitStagedHide].
     */
    suspend fun hideMediaWithStaging(uris: List<Uri>): HideMediaResult = withContext(Dispatchers.IO) {
        hideMutex.lock()
        try {
            val entities = mutableListOf<VaultMediaEntity>()
            val staged = mutableListOf<StagedVaultItem>()

            for (uri in uris.distinct()) {
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
                        val encryptedOk = try {
                            srcFile.inputStream().use { input ->
                                writeEncrypted(input, vaultFile)
                            }
                            vaultFile.exists() && vaultFile.length() > 0L
                        } catch (e: Exception) {
                            vaultFile.delete()
                            throw e
                        }
                        if (!encryptedOk) {
                            vaultFile.delete()
                        } else {
                            val entity = VaultMediaEntity(
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
                            val deleted = srcFile.delete()
                            if (deleted) {
                                MediaScannerConnection.scanFile(context.applicationContext, arrayOf(filePath), null, null)
                                entities.add(entity)
                            } else {
                                val resolvedContentUri = resolveMediaStoreContentUri(filePath, isVideo)
                                if (resolvedContentUri != null) {
                                    staged.add(StagedVaultItem(resolvedContentUri, entity))
                                } else {
                                    vaultFile.delete()
                                    android.util.Log.w("VaultRepository", "Aborting hide: could not delete source file $filePath")
                                }
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
                            fun safeString(col: String): String? {
                                val idx = c.getColumnIndex(col)
                                return if (idx >= 0 && !c.isNull(idx)) c.getString(idx) else null
                            }
                            fun safeLong(col: String): Long? {
                                val idx = c.getColumnIndex(col)
                                return if (idx >= 0 && !c.isNull(idx)) c.getLong(idx) else null
                            }

                            val filePath = safeString(MediaStore.MediaColumns.DATA) ?: ""
                            val displayName = safeString(MediaStore.MediaColumns.DISPLAY_NAME)
                                ?: filePath.substringAfterLast('/', "").ifBlank { uri.lastPathSegment ?: "unknown" }
                            val relativePath = safeString(MediaStore.MediaColumns.RELATIVE_PATH) ?: ""
                            val mimeType = safeString(MediaStore.MediaColumns.MIME_TYPE)
                            val size = safeLong(MediaStore.MediaColumns.SIZE) ?: 0L
                            val nowSec = System.currentTimeMillis() / 1000L
                            val dateAdded = safeLong(MediaStore.MediaColumns.DATE_ADDED) ?: nowSec
                            val dateModified = safeLong(MediaStore.MediaColumns.DATE_MODIFIED) ?: nowSec
                            val duration = safeLong(MediaStore.MediaColumns.DURATION)
                            val isVideo = mimeType?.startsWith("video") == true ||
                                displayName.substringAfterLast('.', "").lowercase() in setOf("mp4", "mkv", "mov", "webm", "avi", "3gp")

                            val originalBucket = filePath.substringBeforeLast("/").substringAfterLast("/").ifBlank { "Gallery" }
                            val extension = displayName.substringAfterLast(".", "")
                            val vaultFileName = if (extension.isNotBlank()) "${UUID.randomUUID()}.$extension" else UUID.randomUUID().toString()

                            val vaultFile = File(vaultDir, vaultFileName)
                            val encryptedOk = try {
                                val opened = context.contentResolver.openInputStream(uri)?.use { input ->
                                    writeEncrypted(input, vaultFile)
                                    true
                                } ?: false
                                opened && vaultFile.exists() && vaultFile.length() > 0L
                            } catch (e: Exception) {
                                vaultFile.delete()
                                throw e
                            }

                            if (!encryptedOk) {
                                vaultFile.delete()
                            } else {
                                val entity = VaultMediaEntity(
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
                                    dateHidden = nowSec
                                )
                                val deletedFromStore = try {
                                    context.contentResolver.delete(uri, null, null) > 0
                                } catch (e: Exception) {
                                    android.util.Log.w("VaultRepository", "MediaStore delete failed for $uri, trying direct/staged delete", e)
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
                                    entities.add(entity)
                                } else {
                                    // Keep encrypted copy staged so MediaStore.createDeleteRequest can remove the original
                                    staged.add(StagedVaultItem(uri, entity))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VaultRepository", "Failed to hide: $uri", e)
                }
            }

            val insertedIds = commitEntitiesAndPurgeDb(entities)
            HideMediaResult(hiddenIds = insertedIds, stagedForDelete = staged)
        } finally {
            hideMutex.unlock()
        }
    }

    suspend fun commitStagedHide(staged: List<StagedVaultItem>): List<Long> = withContext(Dispatchers.IO) {
        if (staged.isEmpty()) return@withContext emptyList()
        hideMutex.lock()
        try {
            val validEntities = staged.map { it.entity }.filter { entity ->
                resolveExistingVaultFile(entity.vaultFileName).let { it.exists() && it.length() > 0L }
            }
            val paths = validEntities.mapNotNull { it.originalPath.takeIf { p -> p.isNotBlank() } }.toTypedArray()
            if (paths.isNotEmpty()) {
                MediaScannerConnection.scanFile(context.applicationContext, paths, null, null)
            }
            commitEntitiesAndPurgeDb(validEntities)
        } finally {
            hideMutex.unlock()
        }
    }

    suspend fun rollbackStagedHide(staged: List<StagedVaultItem>) = withContext(Dispatchers.IO) {
        for (item in staged) {
            try {
                File(vaultDir, item.entity.vaultFileName).delete()
                File(legacyVaultDir, item.entity.vaultFileName).delete()
            } catch (_: Exception) {}
        }
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
                val vaultFile = resolveExistingVaultFile(item.vaultFileName)
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
                    val wrotePlaintext = try {
                        context.contentResolver.openOutputStream(insertUri)?.use { output ->
                            copyDecrypted(vaultFile, output)
                            true
                        } ?: false
                    } catch (e: Exception) {
                        try { context.contentResolver.delete(insertUri, null, null) } catch (_: Exception) {}
                        throw e
                    }
                    if (!wrotePlaintext) {
                        try { context.contentResolver.delete(insertUri, null, null) } catch (_: Exception) {}
                        continue
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

                    // Delete vault file, preview cache, and record
                    vaultFile.delete()
                    removePreviewFiles(item.vaultFileName)
                    vaultDao.deleteByIds(listOf(item.id))
                    count++
                }
            } catch (e: Exception) {
                android.util.Log.e("VaultRepository", "Failed to unhide: ${item.fileName}", e)
            }
        }
        if (count > 0) {
            persistManifest()
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
                File(legacyVaultDir, item.vaultFileName).delete()
                removePreviewFiles(item.vaultFileName)
            } catch (e: Exception) {
                android.util.Log.e("VaultRepository", "Failed to delete vault file: ${item.vaultFileName}", e)
            }
        }
        vaultDao.deleteByIds(ids)
        persistManifest()
    }

    private suspend fun persistManifest() {
        val dek = VaultPersistentStorage.activeDek ?: return
        VaultPersistentStorage.saveEncryptedManifest(vaultDao.getAll(), dek)
    }

    /**
     * Restores persistent manifest into Room after Clear Data / Reinstall, and migrates any
     * legacy internal `filesDir/vault` files into `/storage/emulated/0/.PhotonPrivateSpace`.
     */
    suspend fun syncAndMigrateOnUnlock() = withContext(Dispatchers.IO) {
        val dek = VaultPersistentStorage.activeDek ?: return@withContext
        val currentItems = vaultDao.getAll()
        val restored = VaultPersistentStorage.loadEncryptedManifest(dek)
        if (restored.isNotEmpty()) {
            val existingNames = currentItems.mapTo(HashSet()) { it.vaultFileName }
            val missing = restored.filter { it.vaultFileName !in existingNames }
            if (missing.isNotEmpty()) {
                vaultDao.insertAll(missing)
            }
        }

        val persistentDir = VaultPersistentStorage.getPersistentVaultDir() ?: return@withContext
        val legacyDir = legacyVaultDir
        var migratedAny = false
        for (item in vaultDao.getAll()) {
            val inPersistent = File(persistentDir, item.vaultFileName)
            val inLegacy = File(legacyDir, item.vaultFileName)
            if (!inPersistent.exists() && inLegacy.exists()) {
                val tmp = File(previewCacheDir, "mig_${System.nanoTime()}_${item.vaultFileName}")
                try {
                    tmp.outputStream().use { out -> copyDecrypted(inLegacy, out) }
                    tmp.inputStream().use { inp -> writeEncrypted(inp, inPersistent) }
                    if (inPersistent.exists() && inPersistent.length() > 0L) {
                        inLegacy.delete()
                        migratedAny = true
                    }
                } catch (_: Exception) {
                    if (inPersistent.exists() && inPersistent.length() == 0L) {
                        inPersistent.delete()
                    }
                } finally {
                    tmp.delete()
                }
            } else if (inPersistent.exists()) {
                val isV1 = try {
                    inPersistent.inputStream().use { ins ->
                        val header = ByteArray(ENC_MAGIC.size)
                        ins.read(header) == ENC_MAGIC.size && header.contentEquals(ENC_MAGIC)
                    }
                } catch (_: Exception) { false }
                if (isV1) {
                    val tmp = File(previewCacheDir, "upg_${System.nanoTime()}_${item.vaultFileName}")
                    val encTmp = File(persistentDir, "upg_tmp_${item.vaultFileName}")
                    try {
                        tmp.outputStream().use { out -> copyDecrypted(inPersistent, out) }
                        tmp.inputStream().use { inp -> writeEncrypted(inp, encTmp) }
                        if (encTmp.exists() && encTmp.length() > 0L) {
                            if (inPersistent.delete() && encTmp.renameTo(inPersistent)) {
                                migratedAny = true
                            }
                        }
                    } catch (_: Exception) {
                        encTmp.delete()
                    } finally {
                        tmp.delete()
                        if (encTmp.exists()) encTmp.delete()
                    }
                }
            }
        }
        if (migratedAny || !File(persistentDir, "vault_manifest.enc").exists()) {
            persistManifest()
        }
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

    private fun deriveBackupKey(password: String, salt: ByteArray): SecretKey {
        val spec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt,
            BACKUP_PBKDF2_ITERATIONS,
            256
        )
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Exports all Private Space items and metadata into a password-protected portable backup (.pvault).
     * Returns the number of exported items.
     */
    suspend fun exportEncryptedBackup(outputUri: Uri, password: String): Int = withContext(Dispatchers.IO) {
        require(password.length >= 4) { "Backup password must be at least 4 characters" }
        val items = vaultDao.getAll()
        require(items.isNotEmpty()) { "Private Space is empty" }

        val salt = ByteArray(BACKUP_SALT_LEN).apply { java.security.SecureRandom().nextBytes(this) }
        val backupKey = deriveBackupKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, backupKey)
        val iv = cipher.iv

        var exportedCount = 0
        val manifest = org.json.JSONArray()

        val rawOut = context.contentResolver.openOutputStream(outputUri)
            ?: throw IllegalStateException("Could not open backup destination")

        rawOut.use { out ->
            out.write(BACKUP_MAGIC)
            out.write(salt)
            out.write(iv)
            CipherOutputStream(out, cipher).use { cipherOut ->
                java.util.zip.ZipOutputStream(cipherOut).use { zipOut ->
                    for (item in items) {
                        val vaultFile = resolveExistingVaultFile(item.vaultFileName)
                        if (!vaultFile.exists()) continue

                        zipOut.putNextEntry(java.util.zip.ZipEntry("media/${item.vaultFileName}"))
                        copyDecrypted(vaultFile, zipOut)
                        zipOut.closeEntry()

                        manifest.put(org.json.JSONObject().apply {
                            put("vaultFileName", item.vaultFileName)
                            put("originalUri", item.originalUri)
                            put("originalPath", item.originalPath)
                            put("originalBucket", item.originalBucket)
                            put("originalRelativePath", item.originalRelativePath)
                            put("fileName", item.fileName)
                            put("mimeType", item.mimeType ?: org.json.JSONObject.NULL)
                            put("isVideo", item.isVideo)
                            put("durationMs", item.durationMs ?: org.json.JSONObject.NULL)
                            put("size", item.size)
                            put("dateAdded", item.dateAdded)
                            put("dateModified", item.dateModified)
                            put("dateHidden", item.dateHidden)
                        })
                        exportedCount++
                    }

                    zipOut.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
                    zipOut.write(manifest.toString().toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()
                }
            }
        }
        exportedCount
    }

    /**
     * Restores items from a password-protected portable backup (.pvault) into Private Space.
     * Returns the number of newly restored items (skipping duplicates already in the vault).
     */
    suspend fun importEncryptedBackup(inputUri: Uri, password: String): Int = withContext(Dispatchers.IO) {
        require(password.isNotEmpty()) { "Enter your backup password" }

        val rawIn = context.contentResolver.openInputStream(inputUri)
            ?: throw IllegalStateException("Could not open backup file")

        val tempDir = File(context.cacheDir, "vault_restore_${UUID.randomUUID()}").apply { mkdirs() }
        try {
            var manifestJson: String? = null
            rawIn.use { stream ->
                val magic = ByteArray(BACKUP_MAGIC.size)
                if (stream.read(magic) != BACKUP_MAGIC.size || !magic.contentEquals(BACKUP_MAGIC)) {
                    throw IllegalArgumentException("Not a valid Private Space backup file")
                }

                val salt = ByteArray(BACKUP_SALT_LEN)
                var saltRead = 0
                while (saltRead < BACKUP_SALT_LEN) {
                    val r = stream.read(salt, saltRead, BACKUP_SALT_LEN - saltRead)
                    if (r <= 0) break
                    saltRead += r
                }
                val iv = ByteArray(GCM_IV_LEN)
                var ivRead = 0
                while (ivRead < GCM_IV_LEN) {
                    val r = stream.read(iv, ivRead, GCM_IV_LEN - ivRead)
                    if (r <= 0) break
                    ivRead += r
                }
                if (saltRead != BACKUP_SALT_LEN || ivRead != GCM_IV_LEN) {
                    throw IllegalArgumentException("Backup file is incomplete")
                }

                val backupKey = deriveBackupKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, backupKey, GCMParameterSpec(GCM_TAG_BITS, iv))

                try {
                    CipherInputStream(stream, cipher).use { cipherIn ->
                        java.util.zip.ZipInputStream(cipherIn).use { zipIn ->
                            val tempCanonical = tempDir.canonicalPath + File.separator
                            var entry = zipIn.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory) {
                                    if (entry.name == "manifest.json") {
                                        manifestJson = zipIn.readBytes().toString(Charsets.UTF_8)
                                    } else if (entry.name.startsWith("media/")) {
                                        val fileName = entry.name.removePrefix("media/")
                                        val targetFile = File(tempDir, fileName)
                                        if (targetFile.canonicalPath.startsWith(tempCanonical)) {
                                            targetFile.parentFile?.mkdirs()
                                            targetFile.outputStream().use { out -> zipIn.copyTo(out) }
                                        }
                                    }
                                }
                                zipIn.closeEntry()
                                entry = zipIn.nextEntry
                            }
                        }
                    }
                } catch (e: java.io.IOException) {
                    if (e.cause is javax.crypto.AEADBadTagException || e is javax.crypto.AEADBadTagException) {
                        throw IllegalArgumentException("Incorrect backup password or corrupted backup file")
                    }
                    throw e
                } catch (_: javax.crypto.AEADBadTagException) {
                    throw IllegalArgumentException("Incorrect backup password or corrupted backup file")
                }
            }

            val manifestText = manifestJson ?: throw IllegalArgumentException("Backup manifest missing or invalid")
            val array = org.json.JSONArray(manifestText)
            val existingKeys = vaultDao.getAll().map { "${it.fileName}|${it.size}|${it.dateAdded}" }.toSet()
            val toInsert = mutableListOf<VaultMediaEntity>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val origVaultName = obj.getString("vaultFileName")
                val srcFile = File(tempDir, origVaultName)
                if (!srcFile.exists()) continue

                val fileName = obj.getString("fileName")
                val size = obj.getLong("size")
                val dateAdded = obj.getLong("dateAdded")
                val dedupKey = "$fileName|$size|$dateAdded"
                if (dedupKey in existingKeys) continue

                val ext = fileName.substringAfterLast(".", "")
                val newVaultFileName = if (ext.isNotBlank()) "${UUID.randomUUID()}.$ext" else UUID.randomUUID().toString()
                val destVaultFile = File(vaultDir, newVaultFileName)
                srcFile.inputStream().use { input -> writeEncrypted(input, destVaultFile) }

                val mimeType = if (obj.isNull("mimeType")) null else obj.getString("mimeType")
                val durationMs = if (obj.isNull("durationMs")) null else obj.getLong("durationMs")

                toInsert.add(
                    VaultMediaEntity(
                        originalUri = obj.optString("originalUri", ""),
                        originalPath = obj.optString("originalPath", ""),
                        originalBucket = obj.optString("originalBucket", "Gallery"),
                        originalRelativePath = obj.optString("originalRelativePath", ""),
                        fileName = fileName,
                        vaultFileName = newVaultFileName,
                        mimeType = mimeType,
                        isVideo = obj.optBoolean("isVideo", false),
                        durationMs = durationMs,
                        size = size,
                        dateAdded = dateAdded,
                        dateModified = obj.optLong("dateModified", dateAdded),
                        dateHidden = obj.optLong("dateHidden", System.currentTimeMillis() / 1000L)
                    )
                )
            }

            if (toInsert.isNotEmpty()) {
                vaultDao.insertAll(toInsert)
                persistManifest()
            }
            toInsert.size
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
