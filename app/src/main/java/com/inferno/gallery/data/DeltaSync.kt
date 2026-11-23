package com.inferno.gallery.data

import com.inferno.gallery.data.db.CoreMediaEntity

/**
 * Fast incremental sync: applies only the MediaStore rows changed since the last saved
 * generation, instead of reconciling the whole library.
 *
 * Kept free of Android types so the logic is unit-testable with fakes.
 */

/** One MediaStore row, already converted from the cursor. */
data class DeltaRow(
    val id: Long,
    val uriString: String,
    val path: String,
    val bucketName: String,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val name: String,
    val isVideo: Boolean,
    val durationMs: Long?
)

interface DeltaSource {
    /** MediaStore.getGeneration for the external volume. */
    fun currentGeneration(): Long
    /** Rows added or modified after [generation] (trashed rows included, bucket "Trash"). */
    suspend fun changedSince(generation: Long): List<DeltaRow>
    /** All image/video ids in MediaStore, trashed included. */
    suspend fun allIds(): Set<Long>
    /** Trashed ids, including rows whose MEDIA_TYPE the OEM reset to 0. */
    suspend fun trashedIds(): Set<Long>
}

interface DeltaStore {
    suspend fun roomIds(): Set<Long>
    suspend fun existing(ids: Collection<Long>): Map<Long, CoreMediaEntity>
    suspend fun vaultUris(): Set<String>
    suspend fun vaultPaths(): Set<String>
    suspend fun apply(upserts: List<CoreMediaEntity>, deletes: List<Long>)
}

interface DeltaFiles {
    fun exists(path: String): Boolean
    /** Hidden by a .nomedia file or a dot-folder. Only called when hidden albums are not shown. */
    fun hiddenByNoMedia(path: String): Boolean
}

data class DeltaPlan(
    val upserts: List<CoreMediaEntity>,
    val deletes: List<Long>,
    /** Rows that were not in Room before (new photos), for AI/geocode scheduling. */
    val newCount: Int
)

object DeltaPlanner {

    /**
     * Same per-row rules as the full sync, applied to changed rows only:
     * vault items, missing files and .nomedia-hidden items are not shown (and removed if
     * Room has them); unchanged rows are skipped; changed rows keep their indexing fields.
     * Deletions: Room ids that MediaStore no longer has, trashed or not.
     */
    fun plan(
        changed: List<DeltaRow>,
        existing: Map<Long, CoreMediaEntity>,
        roomIds: Set<Long>,
        storeIds: Set<Long>,
        trashedIds: Set<Long>,
        vaultUris: Set<String>,
        vaultPaths: Set<String>,
        showHiddenAlbums: Boolean,
        files: DeltaFiles
    ): DeltaPlan {
        val upserts = ArrayList<CoreMediaEntity>()
        val deletes = LinkedHashSet<Long>()
        var newCount = 0

        for (row in changed) {
            val inRoom = existing[row.id]
            val inVault = vaultUris.contains(row.uriString) || vaultPaths.contains(row.path)
            val missing = row.path.isNotBlank() && !files.exists(row.path)
            val hidden = row.bucketName != "Trash" && !showHiddenAlbums &&
                row.path.isNotBlank() && files.hiddenByNoMedia(row.path)
            if (inVault || missing || hidden) {
                if (inRoom != null) deletes.add(row.id)
                continue
            }
            if (inRoom != null &&
                inRoom.dateModified == row.dateModified &&
                inRoom.dateAdded == row.dateAdded &&
                inRoom.size == row.size &&
                inRoom.filePath == row.path &&
                inRoom.bucketName == row.bucketName &&
                inRoom.name == row.name
            ) continue

            upserts.add(
                inRoom?.copy(
                    uriString = row.uriString,
                    filePath = row.path,
                    bucketName = row.bucketName,
                    dateAdded = row.dateAdded,
                    dateModified = row.dateModified,
                    size = row.size,
                    name = row.name,
                    isVideo = row.isVideo,
                    durationMs = row.durationMs
                ) ?: CoreMediaEntity(
                    id = row.id,
                    uriString = row.uriString,
                    filePath = row.path,
                    bucketName = row.bucketName,
                    dateAdded = row.dateAdded,
                    dateModified = row.dateModified,
                    size = row.size,
                    name = row.name,
                    mimeType = null,
                    isVideo = row.isVideo,
                    durationMs = row.durationMs
                )
            )
            if (inRoom == null && !roomIds.contains(row.id)) newCount++
        }

        deletes.addAll(goneIds(roomIds, storeIds, trashedIds))
        val upsertIds = upserts.mapTo(HashSet()) { it.id }
        return DeltaPlan(upserts, deletes.filter { it !in upsertIds }, newCount)
    }
}

/** Room ids that MediaStore no longer has (deleted in another app), trashed ones kept. */
fun goneIds(roomIds: Set<Long>, storeIds: Set<Long>, trashedIds: Set<Long>): List<Long> {
    // An empty MediaStore answer means the query failed (e.g. permission revoked),
    // not that every photo is gone: never empty the index because of it.
    if (storeIds.isEmpty()) return emptyList()
    return roomIds.filter { !storeIds.contains(it) && !trashedIds.contains(it) }
}

/** Where the last applied generation is kept (SharedPreferences in the app). */
interface GenerationStore {
    fun load(): Long
    fun save(generation: Long)
}

class DeltaSyncer(
    private val source: DeltaSource,
    private val store: DeltaStore,
    private val files: DeltaFiles,
    private val generations: GenerationStore,
    private val showHiddenAlbums: suspend () -> Boolean
) {
    /**
     * Applies changes since the saved generation (or since [sinceOverride]).
     * The generation is read BEFORE querying, so a photo saved while this runs is picked up
     * by the next run instead of being lost.
     * Phase 1 writes the changed rows right away, so a new photo reaches the grid without
     * waiting; phase 2 then removes items deleted in other apps.
     * Returns null when no generation is saved yet (caller must run a full sync).
     */
    suspend fun run(sinceOverride: Long? = null): DeltaPlan? {
        val saved = generations.load()
        val last = sinceOverride ?: saved
        if (last <= 0L) return null
        val genBefore = source.currentGeneration()

        // Phase 1: added / edited / trashed rows
        val changed = source.changedSince(last)
        val rows = if (changed.isEmpty()) DeltaPlan(emptyList(), emptyList(), 0) else DeltaPlanner.plan(
            changed = changed,
            existing = store.existing(changed.map { it.id }),
            roomIds = emptySet(),
            storeIds = emptySet(),
            trashedIds = emptySet(),
            vaultUris = store.vaultUris(),
            vaultPaths = store.vaultPaths(),
            showHiddenAlbums = showHiddenAlbums(),
            files = files
        )
        if (rows.upserts.isNotEmpty() || rows.deletes.isNotEmpty()) store.apply(rows.upserts, rows.deletes)

        // Phase 2: rows deleted in other apps
        val gone = goneIds(store.roomIds(), source.allIds(), source.trashedIds())
            .filter { id -> rows.upserts.none { it.id == id } }
        if (gone.isNotEmpty()) store.apply(emptyList(), gone)

        if (genBefore > saved) generations.save(genBefore)
        return DeltaPlan(rows.upserts, rows.deletes + gone, rows.newCount)
    }
}
