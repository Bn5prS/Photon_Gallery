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
    // Two lanes: the fast sync never waits behind a full reconcile (which can take
    // 30 s or more on a large library); a catch-up pass after each full sync covers overlap.
    private val fullMutex = Mutex()
    private val deltaMutex = Mutex()
    @Volatile private var lastFullGenBefore = 0L

    companion object {
        private const val TAG = "MediaSyncEngine"
        /** Set by the fast first-run import, which skips reading EXIF dates; cleared by the next full sync. */
        const val KEY_EXIF_PENDING = "exif_dates_pending"

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

    /**
     * Full reconcile (filesystem checks over every item). It runs in its own lane, and
     * afterwards a catch-up fast sync re-reads everything that changed since the full sync
     * started, so a photo changed while it was busy is never missed or left stale.
     */
    suspend fun sync(forceFullSync: Boolean = false): SyncResult {
        val result = fullSync(forceFullSync)
        try {
            syncDelta(fallbackToFull = false, sinceOverride = lastFullGenBefore.takeIf { it > 0 })
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Follow-up fast sync failed: ${e.message}")
        }
        return result
    }

    private val deltaSyncer = DeltaSyncer(
        source = object : DeltaSource {
            override fun currentGeneration(): Long =
                try { MediaStore.getGeneration(appContext, MediaStore.VOLUME_EXTERNAL) } catch (_: Exception) { 0L }

            override suspend fun changedSince(generation: Long): List<DeltaRow> {
                // Only changed rows come back here, so there are no unchanged dates to reuse.
                return mediaRepository.getImagesListForSync(null, generation).map {
                    DeltaRow(it.id, it.uri.toString(), it.path, it.bucketName, it.dateAdded,
                        it.dateModified, it.size, it.name, it.isVideo, it.durationMs)
                }
            }

            override suspend fun allIds(): Set<Long> = mediaRepository.getAllMediaIds()
            override suspend fun trashedIds(): Set<Long> = mediaRepository.getTrashedMediaIds()
        },
        store = object : DeltaStore {
            override suspend fun roomIds(): Set<Long> = database.mediaDao().getAllMediaIds().toHashSet()
            override suspend fun existing(ids: Collection<Long>): Map<Long, CoreMediaEntity> =
                ids.chunked(500).flatMap { database.mediaDao().getMediaByIdsList(it) }.associateBy { it.id }
            override suspend fun vaultUris(): Set<String> = database.vaultDao().getAllOriginalUris().toHashSet()
            override suspend fun vaultPaths(): Set<String> = database.vaultDao().getAllOriginalPaths().toHashSet()
            override suspend fun apply(upserts: List<CoreMediaEntity>, deletes: List<Long>) {
                if (deletes.isNotEmpty()) database.mediaDao().deleteByIdsChunked(deletes)
                if (upserts.isNotEmpty()) database.mediaDao().insertAllChunked(upserts)
            }
        },
        files = object : DeltaFiles {
            private val noMediaCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
            override fun exists(path: String): Boolean = File(path).exists()
            override fun hiddenByNoMedia(path: String): Boolean {
                noMediaCache.clear() // a .nomedia file may have been added since the last run
                return NoMediaUtils.isPathHiddenByNoMedia(path, noMediaCache)
            }
        },
        generations = object : GenerationStore {
            private val prefs get() = appContext.getSharedPreferences("photon_sync", Context.MODE_PRIVATE)
            override fun load(): Long =
                if (!prefs.getBoolean("date_fallback_migrated_v2", false)) 0L else prefs.getLong("last_generation", 0L)
            override fun save(generation: Long) { prefs.edit().putLong("last_generation", generation).apply() }
        },
        showHiddenAlbums = { settingsRepo.showHiddenAlbumsFlow.first() }
    )

    /**
     * Fast incremental sync for MediaStore change events, app resume and app start.
     * Reads only rows changed since the saved generation plus one id-only list for deletions;
     * no per-file checks over the whole library. Falls back to [sync] when no generation is
     * saved yet (first run).
     */
    suspend fun syncDelta(fallbackToFull: Boolean = true, sinceOverride: Long? = null): SyncResult {
        val plan = deltaMutex.withLock {
            withContext(Dispatchers.IO) {
                if (sinceOverride == null && needsFirstImport()) firstImport() else deltaSyncer.run(sinceOverride)
            }
        }
        if (plan == null) {
            return if (fallbackToFull) sync(forceFullSync = false) else SyncResult(0, 0)
        }
        if (plan.newCount > 0) scheduleIndexing(plan.newCount)
        return SyncResult(inserted = plan.upserts.size, deleted = plan.deletes.size)
    }

    private fun syncPrefs() = appContext.getSharedPreferences("photon_sync", Context.MODE_PRIVATE)

    private suspend fun needsFirstImport(): Boolean =
        !syncPrefs().getBoolean("date_fallback_migrated_v2", false) && database.mediaDao().getAllMediaCount() == 0

    /**
     * First-run import that fills the grid in seconds, like the system gallery: it uses
     * MediaStore's own dates and skips opening every file for EXIF. A background full sync
     * (own lane) then reads the EXIF dates. Saves the generation, so fast syncs work at once.
     */
    private suspend fun firstImport(): DeltaPlan {
        val genBefore = try { MediaStore.getGeneration(appContext, MediaStore.VOLUME_EXTERNAL) } catch (_: Exception) { 0L }
        val vaultUris = database.vaultDao().getAllOriginalUris().toHashSet()
        val vaultPaths = database.vaultDao().getAllOriginalPaths().toHashSet()
        val entities = mediaRepository.getImagesListForSync(readExif = false)
            .filter { !vaultUris.contains(it.uri.toString()) && !vaultPaths.contains(it.path) }
            .map {
                CoreMediaEntity(
                    id = it.id, uriString = it.uri.toString(), filePath = it.path, bucketName = it.bucketName,
                    dateAdded = it.dateAdded, dateModified = it.dateModified, size = it.size, name = it.name,
                    mimeType = null, isVideo = it.isVideo, durationMs = it.durationMs
                )
            }
        // Nothing came back (e.g. photo access not granted yet, or an empty library): record
        // nothing, so the import runs again next time instead of skipping existing photos.
        if (entities.isEmpty()) return DeltaPlan(emptyList(), emptyList(), 0)
        database.mediaDao().insertAllChunked(entities)
        syncPrefs().edit()
            .putLong("last_generation", genBefore)
            .putBoolean("date_fallback_migrated_v2", true)
            .putBoolean(KEY_EXIF_PENDING, true)
            .apply()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "MediaSyncFullAfterImport",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<com.inferno.gallery.workers.MediaSyncWorker>()
                .setInputData(androidx.work.workDataOf(com.inferno.gallery.workers.MediaSyncWorker.KEY_FULL to true))
                .build()
        )
        return DeltaPlan(entities, emptyList(), entities.size)
    }

    private suspend fun fullSync(forceFullSync: Boolean): SyncResult = fullMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting sync (forceFullSync=$forceFullSync)...")
                val prefs = appContext.getSharedPreferences("photon_sync", Context.MODE_PRIVATE)
                val isDateFallbackMigrated = prefs.getBoolean("date_fallback_migrated_v2", false)
                val lastGeneration = if (!isDateFallbackMigrated || forceFullSync) 0L else prefs.getLong("last_generation", 0L)

                database.mediaDao().normalizeExternalPrimaryUris()

                val dbList = database.mediaDao().getAllMedia()
                val dbMap = dbList.associateBy { it.id }
                val dbPathToItem = dbList.filter { it.filePath.isNotBlank() }.associateBy { it.filePath }

                // ── 1. Detect dead items on filesystem (non-Trash) & .nomedia hidden items ──
                val deadIds = mutableListOf<Long>()
                val deadPaths = mutableListOf<String>()
                val foldersToCheck = mutableSetOf<File>()
                val newOrModifiedPaths = mutableListOf<String>()
                val showHiddenAlbums = settingsRepo.showHiddenAlbumsFlow.first()
                val noMediaCache = HashMap<String, Boolean>()

                for (dbItem in dbList) {
                    if (dbItem.bucketName == "Trash") {
                        // Trash items are reconciled authoritatively via MediaStore trashedMediaIds below
                        // because Android renames trashed files on disk to .trashed-<ts>-<name>.
                        continue
                    }
                    if (dbItem.filePath.isNotBlank()) {
                        val file = File(dbItem.filePath)
                        val fileExists = file.exists()
                        val isHiddenByNoMedia = !showHiddenAlbums && NoMediaUtils.isPathHiddenByNoMedia(dbItem.filePath, noMediaCache)
                        if (!fileExists || isHiddenByNoMedia) {
                            deadIds.add(dbItem.id)
                            deadPaths.add(dbItem.filePath)
                            file.parentFile?.let { if (it.exists() && it.isDirectory) foldersToCheck.add(it) }
                        }
                    }
                }

                // ── 2. Check for renamed/modified files in affected folders only ──
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
                val effectiveGen = if (forceFullSync || prefs.getBoolean(KEY_EXIF_PENDING, false) || deadPaths.isNotEmpty() || newOrModifiedPaths.isNotEmpty()) null else (if (lastGeneration > 0) lastGeneration else null)
                // Read the generation BEFORE the queries (see where it is saved at the end).
                val genBeforeQuery = try { MediaStore.getGeneration(appContext, MediaStore.VOLUME_EXTERNAL) } catch (_: Exception) { 0L }
                // Reuse the stored date of unchanged files instead of re-reading their EXIF;
                // while the first-run import's dates are still pending, read EXIF again.
                val exifPending = prefs.getBoolean(KEY_EXIF_PENDING, false)
                val knownDate: ((Long, String, Long, Long) -> Long?)? = if (exifPending) null else { id, path, modified, size ->
                    dbMap[id]?.takeIf { it.filePath == path && it.dateModified == modified && it.size == size }?.dateAdded
                }
                var deltaList = mediaRepository.getImagesListForSync(null, effectiveGen, knownDate)

                val allMediaStoreIds = mediaRepository.getAllMediaIds()
                val trashedMediaIds = mediaRepository.getTrashedMediaIds()
                val deltaIds = deltaList.map { it.id }.toSet()

                // Only a generation-limited query can miss rows; re-running a full query
                // right after a full query (effectiveGen == null) just doubled the time.
                if (effectiveGen != null && allMediaStoreIds.any { !dbMap.containsKey(it) && !deltaIds.contains(it) && !deadIds.contains(it) }) {
                    Log.w(TAG, "Detected database drift. Falling back to full MediaStore query.")
                    deltaList = mediaRepository.getImagesListForSync(null, null, knownDate)
                }

                val toInsert = mutableListOf<CoreMediaEntity>()
                val toDelete = mutableListOf<Long>()
                toDelete.addAll(deadIds)

                val vaultUris = database.vaultDao().getAllOriginalUris().toSet()
                val vaultPaths = database.vaultDao().getAllOriginalPaths().toSet()

                for (media in deltaList) {
                    if (vaultUris.contains(media.uri.toString()) || vaultPaths.contains(media.path)) {
                        continue
                    }
                    if (media.path.isNotBlank()) {
                        val file = File(media.path)
                        if (!file.exists()) {
                            continue
                        }
                        if (media.bucketName != "Trash" && !showHiddenAlbums && NoMediaUtils.isPathHiddenByNoMedia(media.path, noMediaCache)) {
                            continue
                        }
                    }
                    val dbItem = dbMap[media.id]
                    if (dbItem != null &&
                        dbItem.dateModified == media.dateModified &&
                        dbItem.dateAdded == media.dateAdded &&
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
                    database.mediaDao().updateBucketByIdsChunked(newlyTrashedIds, "Trash")
                }
                val missingTrashIds = trashedMediaIds.filter { !dbMap.containsKey(it) && !deltaIds.contains(it) }
                if (missingTrashIds.isNotEmpty()) {
                    val trashedItems = mediaRepository.getImagesListForSync("Trash", null)
                    for (media in trashedItems) {
                        if (vaultUris.contains(media.uri.toString()) || vaultPaths.contains(media.path)) {
                            continue
                        }
                        if (media.path.isNotBlank() && !File(media.path).exists()) {
                            continue
                        }
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

                // Find deletions & restored items: compare DB against MediaStore, disk existence, and .nomedia
                val restoredByBucket = mutableMapOf<String, MutableList<Long>>()
                for (dbItem in dbList) {
                    if (dbItem.bucketName == "Trash") {
                        if (deadIds.contains(dbItem.id)) {
                            // File no longer exists on disk -> permanently deleted
                            toDelete.add(dbItem.id)
                        } else if (trashedMediaIds.contains(dbItem.id)) {
                            // Still confirmed in MediaStore trash and on disk -> retain in Room SSOT
                            continue
                        } else if (allMediaStoreIds.contains(dbItem.id)) {
                            // Restored in MediaStore! Restore to original parent bucket
                            val parentName = if (dbItem.filePath.isNotBlank()) File(dbItem.filePath).parentFile?.name else null
                            val originalBucket = if (parentName != null && !parentName.startsWith(".")) parentName else "Camera"
                            restoredByBucket.getOrPut(originalBucket) { mutableListOf() }.add(dbItem.id)
                        } else {
                            // Not in trash and not in active media -> permanently deleted/expired
                            toDelete.add(dbItem.id)
                        }
                        continue
                    }

                    val isMissingFromMediaStore = !allMediaStoreIds.contains(dbItem.id)
                    val isHiddenVault = vaultUris.contains(dbItem.uriString) || vaultPaths.contains(dbItem.filePath)
                    val file = if (dbItem.filePath.isNotBlank()) File(dbItem.filePath) else null
                    val fileExists = file != null && file.exists()
                    val isHiddenByNoMedia = !showHiddenAlbums && dbItem.filePath.isNotBlank() &&
                            NoMediaUtils.isPathHiddenByNoMedia(dbItem.filePath, noMediaCache)
                    val shouldDelete = isMissingFromMediaStore || !fileExists || isHiddenByNoMedia || isHiddenVault

                    if (shouldDelete) {
                        toDelete.add(dbItem.id)
                        if (!fileExists && dbItem.filePath.isNotBlank()) {
                            deadPaths.add(dbItem.filePath)
                        }
                    }
                }

                val additionalDead = deadPaths.filter { !allPathsToScan.contains(it) }.distinct()
                if (additionalDead.isNotEmpty()) {
                    scanFilesAsync(additionalDead)
                }

                if (restoredByBucket.isNotEmpty()) {
                    database.mediaDao().updateBucketByIdsMap(restoredByBucket)
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

                scheduleIndexing(newPhotosCount)

                // Save the generation read BEFORE the MediaStore queries. Saving the value read
                // here at the end skipped every photo saved while this sync was running.
                val currentGen = genBeforeQuery
                lastFullGenBefore = genBeforeQuery

                // Never move the saved generation backwards past what a fast sync already applied.
                if (currentGen > 0 && (!isDateFallbackMigrated || currentGen > prefs.getLong("last_generation", 0L))) {
                    prefs.edit().putLong("last_generation", currentGen).apply()
                }
                if (exifPending) prefs.edit().putBoolean(KEY_EXIF_PENDING, false).apply()
                if (!isDateFallbackMigrated) {
                    prefs.edit().putBoolean("date_fallback_migrated_v2", true).apply()
                }

                Log.d(TAG, "Sync complete: +${toInsert.size}, -${uniqueToDelete.size}. Gen=$currentGen.")
                SyncResult(inserted = toInsert.size, deleted = uniqueToDelete.size)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed: ${e.message}", e)
                throw e
            }
        }
    }

    /** AI indexing, geocoding and photo-sphere jobs after new photos (shared by full and fast sync). */
    private suspend fun scheduleIndexing(newPhotosCount: Int) {
        // AI Indexing & background workers (respect per-feature AI folder policy)
        val (aiConfig, galleryExcluded) = com.inferno.gallery.data.ai.AiIndexingSupport.loadScope(appContext)

        val autoIndexSmartEnabled = settingsRepo.smartSearchAutoIndexFlow.first()
        val smartSearchEngine = SmartSearchEngine.getInstance(appContext)
        // Count unindexed items only when this feature would actually use the count
        if (autoIndexSmartEnabled && newPhotosCount > 0 && smartSearchEngine.isModelDownloaded() &&
            com.inferno.gallery.data.ai.AiIndexingSupport.getEligibleUnindexedSmartCount(database, aiConfig, galleryExcluded) > 0
        ) {
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
        if (faceRecognitionEnabled && autoIndexFacesEnabled && faceEngine.isModelDownloaded() &&
            com.inferno.gallery.data.ai.AiIndexingSupport.getEligibleUnindexedFaceCount(database, aiConfig, galleryExcluded) > 0
        ) {
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
        if (ocrEnabled && newPhotosCount > 0 &&
            com.inferno.gallery.data.ai.AiIndexingSupport.getEligibleUnindexedOcrCount(database, aiConfig, galleryExcluded) > 0
        ) {
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

        if (newPhotosCount > 0) {
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

