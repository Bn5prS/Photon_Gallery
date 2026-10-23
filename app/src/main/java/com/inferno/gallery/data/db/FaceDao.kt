package com.inferno.gallery.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceDao {

    // ── Person Operations ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deletePerson(id: Long)

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getPersonById(id: Long): PersonEntity?

    @Query("SELECT * FROM persons WHERE isPromoted = 1 AND isHidden = 0 ORDER BY isFavorite DESC, faceCount DESC")
    fun observePromotedPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE isHidden = 0 ORDER BY faceCount DESC")
    fun observeAllVisiblePersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE centroid IS NOT NULL")
    suspend fun getAllPersonsWithCentroids(): List<PersonEntity>

    @Query("UPDATE persons SET name = :name WHERE id = :id")
    suspend fun setPersonName(id: Long, name: String?)

    @Query("UPDATE persons SET coverCropPath = :cropPath WHERE id = :id")
    suspend fun setPersonCover(id: Long, cropPath: String)

    @Query("UPDATE persons SET isHidden = :hidden WHERE id = :id")
    suspend fun setPersonHidden(id: Long, hidden: Boolean)

    @Query("UPDATE persons SET isFavorite = :favorite WHERE id = :id")
    suspend fun setPersonFavorite(id: Long, favorite: Boolean)

    // ── Face Operations ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFace(face: FaceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaces(faces: List<FaceEntity>): List<Long>

    @Update
    suspend fun updateFace(face: FaceEntity)

    @Query("SELECT * FROM faces WHERE id = :id")
    suspend fun getFaceById(id: Long): FaceEntity?

    @Query("SELECT * FROM faces WHERE mediaId = :mediaId")
    suspend fun getFacesByMediaId(mediaId: Long): List<FaceEntity>

    @Query("SELECT * FROM faces WHERE personId = :personId")
    suspend fun getFacesByPersonId(personId: Long): List<FaceEntity>

    @Query("SELECT * FROM faces WHERE personId = :personId AND isExemplar = 1 ORDER BY qualityScore DESC LIMIT 5")
    suspend fun getExemplarFacesForPerson(personId: Long): List<FaceEntity>

    @Query("SELECT * FROM faces WHERE personId IS NULL")
    suspend fun getUnassignedFaces(): List<FaceEntity>

    @Query("SELECT COUNT(DISTINCT mediaId) FROM faces WHERE personId = :personId")
    suspend fun countDistinctMediaForPerson(personId: Long): Int

    @Query("""
        SELECT DISTINCT c.* FROM core_media c
        INNER JOIN faces f ON c.id = f.mediaId
        WHERE f.personId = :personId
          AND c.bucketName != 'Trash'
          AND c.uriString NOT IN (SELECT originalUri FROM vault_media)
        ORDER BY c.dateAdded DESC
    """)
    fun observeMediaForPerson(personId: Long): Flow<List<CoreMediaEntity>>

    @Query("""
        SELECT DISTINCT c.* FROM core_media c
        INNER JOIN faces f ON c.id = f.mediaId
        WHERE f.personId = :personId
          AND c.bucketName != 'Trash'
          AND c.uriString NOT IN (SELECT originalUri FROM vault_media)
        ORDER BY c.dateAdded DESC
    """)
    suspend fun getMediaForPerson(personId: Long): List<CoreMediaEntity>

    @Query("SELECT DISTINCT mediaId FROM faces")
    suspend fun getIndexedMediaIds(): List<Long>

    @Query("""
        SELECT id FROM core_media 
        WHERE isVideo = 0 
          AND bucketName != 'Trash' 
          AND uriString NOT IN (SELECT originalUri FROM vault_media)
          AND id NOT IN (SELECT mediaId FROM media_face_status)
        ORDER BY dateAdded DESC
    """)
    suspend fun getUnindexedMediaIds(): List<Long>

    @Query("""
        SELECT COUNT(id) FROM core_media 
        WHERE isVideo = 0 
          AND bucketName != 'Trash' 
          AND uriString NOT IN (SELECT originalUri FROM vault_media)
          AND id NOT IN (SELECT mediaId FROM media_face_status)
    """)
    fun observeUnindexedCount(): Flow<Int>

    // ── Face Scan Status Operations ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaceStatus(status: MediaFaceStatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaceStatuses(statuses: List<MediaFaceStatusEntity>)

    @Query("DELETE FROM media_face_status WHERE mediaId = :mediaId")
    suspend fun deleteFaceStatus(mediaId: Long)

    @Query("DELETE FROM media_face_status")
    suspend fun clearAllFaceStatuses()

    @Query("UPDATE faces SET personId = :targetPersonId WHERE personId = :sourcePersonId")
    suspend fun reassignFaces(sourcePersonId: Long, targetPersonId: Long)

    @Transaction
    suspend fun mergePersons(
        sourcePersonId: Long,
        targetPersonId: Long,
        mergedCentroid: FloatArray,
        faceCount: Int,
        coverCropPath: String?
    ) {
        reassignFaces(sourcePersonId, targetPersonId)
        val target = getPersonById(targetPersonId) ?: return
        val updated = target.copy(
            centroid = mergedCentroid,
            faceCount = faceCount,
            coverCropPath = coverCropPath ?: target.coverCropPath,
            isPromoted = faceCount >= 3
        )
        updatePerson(updated)
        deletePerson(sourcePersonId)
    }

    @Query("DELETE FROM faces")
    suspend fun clearAllFaces()

    @Query("DELETE FROM persons")
    suspend fun clearAllPersons()
}

