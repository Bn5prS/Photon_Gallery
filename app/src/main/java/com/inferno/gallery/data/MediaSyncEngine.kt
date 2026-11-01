package com.inferno.gallery.data

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.inferno.gallery.data.ai.FaceRecognitionEngine
import com.inferno.gallery.data.ai.SmartSearchEngine
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.GalleryDatabase
import com.inferno.gallery.data.db.PhotoSphereEntity
import com.inferno.gallery.workers.FaceIndexWorker
import com.inferno.gallery.workers.OcrIndexWorker
import com.inferno.gallery.workers.ReverseGeocodeWorker
import com.inferno.gallery.workers.SmartSearchIndexWorker
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class SyncResult(
    val inserted: Int,
    val deleted: Int
)

/**
 * Unified synchronization engine reconciling device filesystem changes,
 * Android MediaStore, and Room SSOT database.
 *
 * Handles external modifications (e.g. batch rename/bulk scripts),
 * prunes dead entries without lingering blank thumbnails, and indexes
 * new/modified files via MediaScannerConnection.
 */
class MediaSyncEngine private constructor(private val appContext: Context) {

    private val database: GalleryDatabase = DatabaseProvider.getDatabase(appContext)
    private val mediaRepository: LocalMediaRepository = LocalMediaRepository(appContext.contentResolver)
    private val settingsRepo: SettingsRepository = SettingsRepository.getInstance(appContext)
    private val syncMutex = Mutex()

    companion object {
        private const val TAG = "MediaSyncEngine"

        @Volatile
        private var INSTANCE: MediaSyncEngine? = null

        fun getInstance(context: Context): MediaSyncEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaSyncEngine(context.applicationContext).also { INSTANCE = it }
            }
        }

        private val SUPPORTED_MEDIA_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp", "avif",
            "dng", "cr2", "nef", "arw", "orf", "rw2", "pef", "srw", "raf",
            "mp4", "mkv", "mov", "webm", "3gp", "ts", "avi", "flv", "m4v"
        )

        fun isMediaFile(name: String): Boolean {
            val dot = name.lastIndexOf('.')
            if (dot < 0 || dot >= name.length - 1) return false
            val ext = name.substring(dot + 1).lowercase()
            return SUPPORTED_MEDIA_EXTENSIONS.contains(ext)
        }
    }

    suspend fun sync(forceFullSync: Boolean = false): SyncResult = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting sync (forceFullSync=$forceFullSync)...")
                val prefs = appContext.getSharedPreferences("photon_sync", Context.MODE_PRIVATE)
                val isDateFallbackMigrated = prefs.getBoolean("date_fallback_migrated_v1", false)
                val lastGeneration = if (!isDateFallbackMigrated || forceFullSync) 0L else prefs.getLong("last_generation", 0L)

                val dbList = database.mediaDao().getAllMedia()
                val dbMap = dbList.associateBy { it.id }
                val dbPathToItem = dbList.filter { it.filePath.isNotBlank() }.associateBy { it.filePath }

                // ── 1. Detect dead items on filesystem (non-Trash) ──
                val deadIds = mutableListOf<Long>()
                val deadPaths = mutableListOf<String>()
                val foldersToCheck = mutableSetOf<File>()

                for (dbItem in dbList) {
                    if (dbItem.bucketName == "Trash") continue
                    if (dbItem.filePath.isNotBlank()) {
                        val file = File(dbItem.filePath)
                        if (!file.exists()) {
                            deadIds.add(dbItem.id)
                            deadPaths.add(dbItem.filePath)
                            file.parentFile?.let { if (it.exists() && it.isDirectory) foldersToCheck.add(it) }
                        }
                    }
                }

                // ── 2. Check for renamed/modified files in affected folders only ──
                val newOrModifiedPaths = mutableListOf<String>()
                if (foldersToCheck.isNotEmpty()) {
                    for (folder in foldersToCheck) {
                        if (!folder.exists() || !folder.isDirectory) continue
                        val files = folder.listFiles() ?: continue
                        for (file in files) {
                            if (!file.isFile || file.name.startsWith(".")) continue
                            if (!isMediaFile(file.name)) continue
                            val path = file.absolutePath
                            val known = dbPathToItem[path]
                            if (known == null) {
                                newOrModifiedPaths.add(path)
                            } else {
                                val diskModifiedSec = file.lastModified() / 1000L
                                if (diskModifiedSec > known.dateModified + 2L) {
                                    newOrModifiedPaths.add(path)
                                }
                            }
                        }
                    }
                }

                // ── 4. Rescan dead and new paths through Android MediaScanner ──
                val allPathsToScan = (deadPaths + newOrModifiedPaths).distinct()
                if (allPathsToScan.isNotEmpty()) {
                    Log.d(TAG, "Notifying MediaScanner of ${allPathsToScan.size} paths (${deadPaths.size} dead, ${newOrModifiedPaths.size} new/modified)")
                    scanFilesAsync(allPathsToScan)
                }

                // ── 5. Query MediaStore ──
                val effectiveGen = if (forceFullSync || deadPaths.isNotEmpty() || newOrModifiedPaths.isNotEmpty()) null else (if (lastGeneration > 0) lastGeneration else null)
                var deltaList = mediaRepository.getImagesListForSync(null, effectiveGen)

                val allMediaStoreIds = mediaRepository.getAllMediaIds()
                val trashedMediaIds = mediaRepository.getTrashedMediaIds()
                val deltaIds = deltaList.map { it.id }.toSet()

                if (allMediaStoreIds.any { !dbMap.containsKey(it) && !deltaIds.contains(it) }) {
                    Log.w(TAG, "Detected database drift. Falling back to full MediaStore query.")
                    deltaList = mediaRepository.getImagesListForSync(null, null)
                }

                val toInsert = mutableListOf<CoreMediaEntity>()
                val toDelete = mutableListOf<Long>()
                toDelete.addAll(deadIds)

                val vaultUris = database.vaultDao().getAllOriginalUris().toSet()
                val vaultPaths = database.vaultDao().getAllOriginalPaths().toSet()
                val showHiddenAlbums = settingsRepo.showHiddenAlbumsFlow.first()
                val noMediaCache = HashMap<String, Boolean>()

                for (media in deltaList) {
                    if (vaultUris.contains(media.uri.toString()) || vaultPaths.contains(media.path)) {
                        continue
                    }
                    if (media.bucketName != "Trash" && media.path.isNotBlank()) {
                        val file = File(media.path)
                        if (!file.exists()) {
                            continue
                        }
                        if (!showHiddenAlbums && NoMediaUtils.isPathHiddenByNoMedia(media.path, noMediaCache)) {
                            continue
                        }
                    }
                    val dbItem = dbMap[media.id]
                    if (dbItem != null &&
                        dbItem.dateModified == media.dateModified &&
                        dbItem.size == media.size &&
                        dbItem.filePath == media.path &&
                        dbItem.bucketName == media.bucketName &&
                        dbItem.name == media.name
                    ) {
                        continue
                    }

                    toInsert.add(
                        CoreMediaEntity(
                            id = media.id,
                            uriString = media.uri.toString(),
                            filePath = media.path,
                            bucketName = media.bucketName,
                            dateAdded = media.dateAdded,
                            dateModified = media.dateModified,
                            size = media.size,
                            name = media.name,
                            mimeType = dbItem?.mimeType,
                            isVideo = media.isVideo,
                            durationMs = media.durationMs,
                            isIndexedOcr = dbItem?.isIndexedOcr ?: false,
                            pHash = dbItem?.pHash,
                            latitude = dbItem?.latitude,
                            longitude = dbItem?.longitude,
                            fileHash = dbItem?.fileHash
                        )
                    )
                }

                // Check trash status
                val dbTrashIds = dbList.filter { it.bucketName == "Trash" }.map { it.id }.toSet()
                val newlyTrashedIds = trashedMediaIds.filter { dbMap.containsKey(it) && !dbTrashIds.contains(it) }
                if (newlyTrashedIds.isNotEmpty()) {
                    database.mediaDao().updateBucketByIds(newlyTrashedIds, "Trash")
                }
                val missingTrashIds = trashedMediaIds.filter { !dbMap.containsKey(it) && !deltaIds.contains(it) }
                if (missingTrashIds.isNotEmpty()) {
                    val trashedItems = mediaRepository.getImagesListForSync("Trash", null)
                    for (media in trashedItems) {
                        if (!dbMap.containsKey(media.id)) {
                            toInsert.add(
                                CoreMediaEntity(
                                    id = media.id,
                                    uriString = media.uri.toString(),
                                    filePath = media.path,
                                    bucketName = "Trash",
                                    dateAdded = media.dateAdded,
                                    dateModified = media.dateModified,
                                    size = media.size,
                                    name = media.name,
                                    mimeType = null,
                                    isVideo = media.isVideo,
                                    durationMs = media.durationMs,
                                    isIndexedOcr = false,
                                    pHash = null,
                                    latitude = null,
                                    longitude = null,
                                    fileHash = null
                                )
                            )
                        }
                    }
                }

                val newPhotosCount = toInsert.count { !dbMap.containsKey(it.id) }

                // Find deletions: compare DB against MediaStore, disk existence, and .nomedia
                for (dbItem in dbList) {
                    if (dbItem.bucketName == "Trash") {
                        if (!allMediaStoreIds.contains(dbItem.id)) {
                            toDelete.add(dbItem.id)
                        } else if (!trashedMediaIds.contains(dbItem.id)) {
                            val parentName = if (dbItem.filePath.isNotBlank()) File(dbItem.filePath).parentFile?.name else null
                            val originalBucket = if (parentName != null && !parentName.startsWith(".")) parentName else "Camera"
                            database.mediaDao().updateBucketById(dbItem.id, originalBucket)
                        }
                        continue
                    }

                    val file = if (dbItem.filePath.isNotBlank()) File(dbItem.filePath) else null
                    val fileExists = file != null && file.exists()
                    val isHiddenByNoMedia = !showHiddenAlbums && dbItem.filePath.isNotBlank() &&
                            NoMediaUtils.isPathHiddenByNoMedia(dbItem.filePath, noMediaCache)

                    if (!allMediaStoreIds.contains(dbItem.id) || !fileExists || isHiddenByNoMedia || vaultUris.contains(dbItem.uriString) || vaultPaths.contains(dbItem.filePath)) {
                        toDelete.add(dbItem.id)
                    }
                }

                val uniqueToDelete = toDelete.distinct()
                if (uniqueToDelete.isNotEmpty()) {
                    Log.d(TAG, "Deleting ${uniqueToDelete.size} items from Room SSOT.")
                    database.mediaDao().deleteByIdsChunked(uniqueToDelete)
                }

                if (toInsert.isNotEmpty()) {
                    Log.d(TAG, "Inserting/Updating ${toInsert.size} items in Room SSOT.")
                    database.mediaDao().insertAllChunked(toInsert)
                }

                // AI Indexing & background workers
                val autoIndexSmartEnabled = settingsRepo.smartSearchAutoIndexFlow.first()
                val smartSearchEngine = SmartSearchEngine.getInstance(appContext)
                val unindexedSmartCount = database.embeddingDao().getUnindexedMediaIds().size
                if (autoIndexSmartEnabled && smartSearchEngine.isModelDownloaded() && unindexedSmartCount > 0 && newPhotosCount > 0) {
                    val indexRequest = OneTimeWorkRequestBuilder<SmartSearchIndexWorker>().build()
                    WorkManager.getInstance(appContext).enqueueUniqueWork(
                        "SmartSearchIndexWorker",
                        ExistingWorkPolicy.KEEP,
                        indexRequest
                    )
                }

                val faceRecognitionEnabled = settingsRepo.faceRecognitionEnabledFlow.first()
                val autoIndexFacesEnabled = settingsRepo.faceAutoIndexFlow.first()
                val faceEngine = FaceRecognitionEngine.getInstance(appContext)
                val unindexedFaceCount = database.faceDao().getUnindexedMediaIds().size
                if (faceRecognitionEnabled && autoIndexFacesEnabled && faceEngine.isModelDownloaded() && unindexedFaceCount > 0) {
                    val constraints = androidx.work.Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                    val faceIndexRequest = OneTimeWorkRequestBuilder<FaceIndexWorker>()
                        .setConstraints(constraints)
                        .build()
                    WorkManager.getInstance(appContext).enqueueUniqueWork(
                        "FaceIndexWorker",
                        ExistingWorkPolicy.KEEP,
                        faceIndexRequest
                    )
                }

                val ocrEnabled = settingsRepo.ocrIndexingEnabledFlow.first()
                val unindexedOcrCount = database.mediaDao().getUnindexedImageCount()
                if (ocrEnabled && unindexedOcrCount > 0 && newPhotosCount > 0) {
                    val constraints = androidx.work.Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                    val ocrIndexRequest = OneTimeWorkRequestBuilder<OcrIndexWorker>()
                        .setConstraints(constraints)
                        .build()
                    WorkManager.getInstance(appContext).enqueueUniqueWork(
                        "OcrIndexWorker",
                        ExistingWorkPolicy.KEEP,
                        ocrIndexRequest
                    )
                }

                val geocodeRequest = OneTimeWorkRequestBuilder<ReverseGeocodeWorker>().build()
                WorkManager.getInstance(appContext).enqueueUniqueWork(
                    "ReverseGeocodeWorker",
                    ExistingWorkPolicy.KEEP,
                    geocodeRequest
                )

                // Background photo sphere classification so refresh is instantaneous
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val sphereCandidates = database.mediaDao().getUncheckedSphereCandidates()
                        if (sphereCandidates.isNotEmpty()) {
                            val sphereEntities = sphereCandidates.map { candidate ->
                                val isSphere = PhotoSphereHelper.isPhotoSphere(
                                    context = appContext,
                                    filePath = candidate.filePath,
                                    uriString = candidate.uriString
                                )
                                PhotoSphereEntity(mediaId = candidate.id, isSphere = isSphere)
                            }
                            database.mediaDao().insertPhotoSpheres(sphereEntities)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Photo sphere check failed", e)
                    }
                }

                val currentGen = try {
                    MediaStore.getGeneration(appContext, MediaStore.VOLUME_EXTERNAL)
                } catch (_: Exception) { 0L }

                if (currentGen > 0) {
                    prefs.edit().putLong("last_generation", currentGen).apply()
                }
                if (!isDateFallbackMigrated) {
                    prefs.edit().putBoolean("date_fallback_migrated_v1", true).apply()
                }

                Log.d(TAG, "Sync complete: +${toInsert.size}, -${uniqueToDelete.size}. Gen=$currentGen.")
                SyncResult(inserted = toInsert.size, deleted = uniqueToDelete.size)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed: ${e.message}", e)
                throw e
            }
        }
    }

    private fun scanFilesAsync(paths: List<String>) {
        if (paths.isEmpty()) return
        try {
            for (chunk in paths.chunked(200)) {
                MediaScannerConnection.scanFile(
                    appContext,
                    chunk.toTypedArray(),
                    null,
                    null
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error triggering MediaScanner", e)
        }
    }
}

