package com.inferno.gallery.data

import com.inferno.gallery.data.db.CoreMediaEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeltaSyncTest {

    // ── Fakes: a tiny MediaStore with generations, and a Room table ──

    private class FakeStore(rows: List<DeltaRow>) {
        data class Entry(val row: DeltaRow, val gen: Long, val trashedTypeZero: Boolean = false)
        var generation = 0L
        val entries = LinkedHashMap<Long, Entry>()
        init { rows.forEach { put(it) } }
        fun put(row: DeltaRow, trashedTypeZero: Boolean = false) {
            generation++
            entries[row.id] = Entry(row, generation, trashedTypeZero)
        }
        fun remove(id: Long) { generation++; entries.remove(id) }
    }

    private class FakeSource(val ms: FakeStore) : DeltaSource {
        /** Runs right after the changed-rows query has been answered: a photo saved mid-sync. */
        var afterQuery: (() -> Unit)? = null
        var failAllIds = false
        /** Called when the deletion check starts (phase 2). */
        var onAllIds: (() -> Unit)? = null
        override fun currentGeneration() = ms.generation
        override suspend fun changedSince(generation: Long): List<DeltaRow> {
            val result = ms.entries.values.filter { it.gen > generation }.map { it.row }
            afterQuery?.invoke(); afterQuery = null
            return result
        }
        override suspend fun allIds(): Set<Long> =
            onAllIds?.invoke().let { if (failAllIds) emptySet() else null } ?:
            if (failAllIds) emptySet() else ms.entries.values.filter { !it.trashedTypeZero }.map { it.row.id }.toSet()
        override suspend fun trashedIds(): Set<Long> =
            ms.entries.values.filter { it.row.bucketName == "Trash" }.map { it.row.id }.toSet()
    }

    private class FakeRoom : DeltaStore {
        val rows = LinkedHashMap<Long, CoreMediaEntity>()
        var vaultUris = setOf<String>()
        var applyCalls = 0
        override suspend fun roomIds() = rows.keys.toSet()
        override suspend fun existing(ids: Collection<Long>) = ids.mapNotNull { rows[it] }.associateBy { it.id }
        override suspend fun vaultUris() = vaultUris
        override suspend fun vaultPaths() = emptySet<String>()
        override suspend fun apply(upserts: List<CoreMediaEntity>, deletes: List<Long>) {
            applyCalls++
            deletes.forEach { rows.remove(it) }
            upserts.forEach { rows[it.id] = it }
        }
    }

    private class FakeFiles(val missing: Set<String> = emptySet(), val hidden: Set<String> = emptySet()) : DeltaFiles {
        override fun exists(path: String) = path !in missing
        override fun hiddenByNoMedia(path: String) = path in hidden
    }

    private class Gen(var value: Long) : GenerationStore {
        override fun load() = value
        override fun save(generation: Long) { value = generation }
    }

    private fun row(id: Long, name: String = "IMG_$id.jpg", bucket: String = "Camera", size: Long = 100, modified: Long = 1000) =
        DeltaRow(id, "content://media/external/images/media/$id", "/sdcard/DCIM/$bucket/$name",
            bucket, 5000 + id, modified, size, name, false, null)

    private fun entity(r: DeltaRow) = CoreMediaEntity(r.id, r.uriString, r.path, r.bucketName, r.dateAdded,
        r.dateModified, r.size, r.name, null, r.isVideo, r.durationMs)

    private fun syncer(src: FakeSource, room: FakeRoom, gen: Gen, files: FakeFiles = FakeFiles(), showHidden: Boolean = false) =
        DeltaSyncer(src, room, files, gen) { showHidden }

    /** A library already in sync: Room has every row and the generation is saved. */
    private fun synced(vararg ids: Long): Triple<FakeStore, FakeRoom, Gen> {
        val ms = FakeStore(ids.map { row(it) })
        val room = FakeRoom().apply { ids.forEach { rows[it] = entity(row(it)) } }
        return Triple(ms, room, Gen(ms.generation))
    }

    // ── The bug this fixes ──

    @Test
    fun photoSavedWhileSyncRuns_isPickedUpByNextRun_notLost() = runBlocking {
        val (ms, room, gen) = synced(1, 2, 3)
        ms.put(row(10)) // screenshot 1
        val src = FakeSource(ms).apply { afterQuery = { ms.put(row(11)) } } // screenshot 2 saved mid-sync
        val s = syncer(src, room, gen)

        s.run()
        assertTrue("first screenshot applied", 10L in room.rows)

        s.run() // the next change event
        assertTrue("screenshot saved during the previous run must not be skipped", 11L in room.rows)
    }

    @Test
    fun savedGeneration_isTheOneReadBeforeTheQuery() = runBlocking {
        val (ms, room, gen) = synced(1)
        val before = ms.generation
        ms.put(row(2))
        val genBeforeRun = ms.generation
        val src = FakeSource(ms).apply { afterQuery = { ms.put(row(3)) } }
        syncer(src, room, gen).run()
        assertTrue(gen.value > before)
        assertEquals(genBeforeRun, gen.value)
    }

    @Test
    fun noSavedGeneration_returnsNull_soCallerRunsFullSync() = runBlocking {
        val (ms, room, _) = synced(1)
        assertNull(syncer(FakeSource(ms), room, Gen(0)).run())
    }

    @Test
    fun nothingChanged_writesNothing() = runBlocking {
        val (ms, room, gen) = synced(1, 2, 3)
        val plan = syncer(FakeSource(ms), room, gen).run()!!
        assertEquals(0, plan.upserts.size)
        assertEquals(0, plan.deletes.size)
        assertEquals(0, room.applyCalls)
    }

    // ── Same per-row rules as the full sync ──

    @Test
    fun newPhoto_isInserted_andCountedAsNew() = runBlocking {
        val (ms, room, gen) = synced(1)
        ms.put(row(2))
        val plan = syncer(FakeSource(ms), room, gen).run()!!
        assertEquals(1, plan.newCount)
        assertEquals(entity(row(2)), room.rows[2L])
    }

    @Test
    fun editedPhoto_keepsIndexingFields() = runBlocking {
        val (ms, room, gen) = synced(1)
        room.rows[1L] = room.rows[1L]!!.copy(isIndexedOcr = true, isIndexedFaces = true, pHash = 42, latitude = 1.5, mimeType = "image/jpeg")
        ms.put(row(1, size = 200, modified = 2000))
        val plan = syncer(FakeSource(ms), room, gen).run()!!
        val r = room.rows[1L]!!
        assertEquals(200L, r.size)
        assertEquals(2000L, r.dateModified)
        assertTrue(r.isIndexedOcr && r.isIndexedFaces)
        assertEquals(42L, r.pHash)
        assertEquals(1.5, r.latitude!!, 0.0)
        assertEquals("image/jpeg", r.mimeType)
        assertEquals(0, plan.newCount)
    }

    @Test
    fun deletedInMediaStore_isRemovedFromRoom() = runBlocking {
        val (ms, room, gen) = synced(1, 2, 3)
        ms.remove(2)
        syncer(FakeSource(ms), room, gen).run()
        assertEquals(setOf(1L, 3L), room.rows.keys)
    }

    @Test
    fun trashedPhoto_staysInRoomAsTrash_evenWhenOemResetsMediaType() = runBlocking {
        val (ms, room, gen) = synced(1, 2)
        ms.put(row(2, bucket = "Trash"), trashedTypeZero = true) // not in allIds(), only in trashedIds()
        syncer(FakeSource(ms), room, gen).run()
        assertEquals("Trash", room.rows[2L]?.bucketName)
    }

    @Test
    fun vaultItem_isNotShown_andRemovedIfPresent() = runBlocking {
        val (ms, room, gen) = synced(1)
        ms.put(row(1, size = 300)) // touched while being moved to the vault
        ms.put(row(5))
        room.vaultUris = setOf(row(1).uriString, row(5).uriString)
        syncer(FakeSource(ms), room, gen).run()
        assertTrue(1L !in room.rows)
        assertTrue(5L !in room.rows)
    }

    @Test
    fun missingFile_isNotShown() = runBlocking {
        val (ms, room, gen) = synced(1)
        ms.put(row(7))
        syncer(FakeSource(ms), room, gen, FakeFiles(missing = setOf(row(7).path))).run()
        assertTrue(7L !in room.rows)
    }

    @Test
    fun nomediaHidden_isNotShown_unlessHiddenAlbumsAreOn() = runBlocking {
        val (ms, room, gen) = synced(1)
        ms.put(row(8))
        val files = FakeFiles(hidden = setOf(row(8).path))
        syncer(FakeSource(ms), room, Gen(gen.value), files, showHidden = false).run()
        assertTrue(8L !in room.rows)
        syncer(FakeSource(ms), room, Gen(gen.value), files, showHidden = true).run()
        assertTrue(8L in room.rows)
    }

    @Test
    fun failedMediaStoreQuery_neverEmptiesTheIndex() = runBlocking {
        val (ms, room, gen) = synced(1, 2, 3)
        ms.put(row(4))
        val src = FakeSource(ms).apply { failAllIds = true }
        syncer(src, room, gen).run()
        assertEquals(setOf(1L, 2L, 3L, 4L), room.rows.keys)
    }

    @Test
    fun manyChanges_allApplied_inOneRun() = runBlocking {
        val (ms, room, gen) = synced(1)
        (100L..400L).forEach { ms.put(row(it)) }
        val plan = syncer(FakeSource(ms), room, gen).run()!!
        assertEquals(301, plan.newCount)
        assertEquals(302, room.rows.size)
        assertEquals(1, room.applyCalls)
    }

    // ── Speed: a new photo is written before the slower deletion check ──

    @Test
    fun newPhoto_isInRoom_beforeTheDeletionCheckRuns() = runBlocking {
        val (ms, room, gen) = synced(1, 2)
        ms.put(row(9))
        var inRoomWhenDeletionCheckStarted = false
        val src = FakeSource(ms).apply { onAllIds = { inRoomWhenDeletionCheckStarted = 9L in room.rows } }
        syncer(src, room, gen).run()
        assertTrue(inRoomWhenDeletionCheckStarted)
    }

    // ── Catch-up pass after a full sync (separate lanes) ──

    @Test
    fun catchUpPass_fixesARowAFullSyncOverwroteWithOldData() = runBlocking {
        val (ms, room, gen) = synced(1)
        val fullSyncStartGen = ms.generation           // full sync reads MediaStore here
        val oldRow1 = entity(row(1))
        ms.put(row(1, size = 999, modified = 5000))    // photo edited while the full sync is busy
        syncer(FakeSource(ms), room, gen).run()        // fast lane applies the edit
        assertEquals(999L, room.rows[1L]!!.size)
        room.rows[1L] = oldRow1                        // full sync then writes its older copy
        val savedBefore = gen.value

        syncer(FakeSource(ms), room, gen).run(sinceOverride = fullSyncStartGen)

        assertEquals("catch-up re-reads changes since the full sync started", 999L, room.rows[1L]!!.size)
        assertTrue("saved generation never moves backwards", gen.value >= savedBefore)
    }

    @Test
    fun catchUpPass_worksBeforeAnyGenerationIsSaved() = runBlocking {
        val ms = FakeStore(listOf(row(1), row(2)))
        val room = FakeRoom()
        val gen = Gen(0)
        assertTrue(syncer(FakeSource(ms), room, gen).run(sinceOverride = 0L) == null)
        val plan = syncer(FakeSource(ms), room, gen).run(sinceOverride = 1L)!!
        assertTrue(2L in room.rows)
        assertEquals(1, plan.newCount)
        assertEquals(ms.generation, gen.value)
    }
}
