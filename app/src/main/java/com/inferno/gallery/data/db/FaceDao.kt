package com.inferno.gallery.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class MediaPersonInfo(
    val faceId: Long,
    val personId: Long?,
    val name: String?,
    val cropPath: String?,
    val faceCount: Int
)

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

    @Query("""
        SELECT p.id, p.name, p.coverCropPath, NULL AS centroid, p.isPromoted, p.isHidden, p.isFavorite, p.createdAt,
               (SELECT COUNT(DISTINCT f.mediaId) FROM faces f 
                INNER JOIN core_media c ON f.mediaId = c.id 
                WHERE f.personId = p.id AND c.bucketName != 'Trash' 
                  AND c.uriString NOT IN (SELECT originalUri FROM vault_media)) AS faceCount
        FROM persons p
        WHERE p.isPromoted = 1 AND p.isHidden = 0
        ORDER BY p.isFavorite DESC, faceCount DESC
    """)
    fun observePromotedPersons(): Flow<List<PersonEntity>>

    @Query("""
        SELECT p.id, p.name, p.coverCropPath, NULL AS centroid, p.isPromoted, p.isHidden, p.isFavorite, p.createdAt,
               (SELECT COUNT(DISTINCT f.mediaId) FROM faces f 
                INNER JOIN core_media c ON f.mediaId = c.id 
                WHERE f.personId = p.id AND c.bucketName != 'Trash' 
                  AND c.uriString NOT IN (SELECT originalUri FROM vault_media)) AS faceCount
        FROM persons p
        WHERE p.isHidden = 0
        ORDER BY faceCount DESC
    """)
    fun observeAllVisiblePersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE centroid IS NOT NULL")
    suspend fun getAllPersonsWithCentroids(): List<PersonEntity>

    @Query("""
        SELECT p.id, p.name, p.coverCropPath, NULL AS centroid, p.isPromoted, p.isHidden, p.isFavorite, p.createdAt,
               (SELECT COUNT(DISTINCT f.mediaId) FROM faces f 
                INNER JOIN core_media c ON f.mediaId = c.id 
                WHERE f.personId = p.id AND c.bucketName != 'Trash' 
                  AND c.uriString NOT IN (SELECT originalUri FROM vault_media)) AS faceCount
        FROM persons p
        WHERE p.isHidden = 1
        ORDER BY faceCount DESC
    """)
    fun observeHiddenPersons(): Flow<List<PersonEntity>>

    @Query("""
        SELECT p.id, p.name, p.coverCropPath, NULL AS centroid, p.isPromoted, p.isHidden, p.isFavorite, p.createdAt,
               (SELECT COUNT(DISTINCT f.mediaId) FROM faces f 
                INNER JOIN core_media c ON f.mediaId = c.id 
                WHERE f.personId = p.id AND c.bucketName != 'Trash' 
                  AND c.uriString NOT IN (SELECT originalUri FROM vault_media)) AS faceCount
        FROM persons p
        ORDER BY p.isHidden ASC, faceCount DESC
    """)
    fun observeAllPersonsIncludingHidden(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND id != :excludePersonId LIMIT 1")
    suspend fun findPersonByName(name: String, excludePersonId: Long): PersonEntity?

    @Query("""
        SELECT p.id, p.name, p.coverCropPath, NULL AS centroid, p.isPromoted, p.isHidden, p.isFavorite, p.createdAt,
               (SELECT COUNT(DISTINCT f.mediaId) FROM faces f 
                INNER JOIN core_media c ON f.mediaId = c.id 
                WHERE f.personId = p.id AND c.bucketName != 'Trash' 
                  AND c.uriString NOT IN (SELECT originalUri FROM vault_media)) AS faceCount
        FROM persons p
        WHERE p.name IS NOT NULL AND LOWER(p.name) LIKE '%' || LOWER(:query) || '%' AND p.isHidden = 0
        ORDER BY faceCount DESC
    """)
    suspend fun searchPersonsByName(query: String): List<PersonEntity>

    @Query("UPDATE persons SET name = :name WHERE id = :id")
    suspend fun setPersonName(id: Long, name: String?)

    @Query("UPDATE persons SET coverCropPath = :cropPath WHERE id = :id")
    suspend fun setPersonCover(id: Long, cropPath: String)

    @Query("UPDATE persons SET isHidden = :hidden WHERE id = :id")
    suspend fun setPersonHidden(id: Long, hidden: Boolean)

    @Query("UPDATE persons SET isFavorite = :favorite WHERE id = :id")
    suspend fun setPersonFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE persons SET faceCount = :count WHERE id = :personId")
    suspend fun updatePersonFaceCount(personId: Long, count: Int)

    @Query("SELECT COUNT(*) FROM faces WHERE personId = :personId")
    suspend fun getFaceCountForPerson(personId: Long): Int

    @Query("UPDATE faces SET personId = NULL WHERE mediaId IN (:mediaIds) AND personId = :personId")
    suspend fun unassignFacesFromPerson(mediaIds: List<Long>, personId: Long)

    @Query("SELECT cropCachePath FROM faces WHERE mediaId = :mediaId AND personId = :personId LIMIT 1")
    suspend fun getCropCachePathForMedia(mediaId: Long, personId: Long): String?

    @Query("SELECT * FROM faces WHERE mediaId = :mediaId AND personId = :personId LIMIT 1")
    suspend fun getFaceForMediaAndPerson(mediaId: Long, personId: Long): FaceEntity?

    @Query("""
        SELECT DISTINCT p.id, p.name, p.coverCropPath, NULL AS centroid, p.isPromoted, p.isHidden, p.isFavorite, p.faceCount, p.createdAt FROM persons p
        INNER JOIN faces f ON p.id = f.personId
        WHERE f.mediaId = :mediaId AND p.isHidden = 0
    """)
    fun observePersonsForMedia(mediaId: Long): Flow<List<PersonEntity>>

    @Query("""
        SELECT DISTINCT p.id, p.name, p.coverCropPath, NULL AS centroid, p.isPromoted, p.isHidden, p.isFavorite, p.faceCount, p.createdAt FROM persons p
        INNER JOIN faces f ON p.id = f.personId
        WHERE f.mediaId = :mediaId AND p.isHidden = 0
    """)
    suspend fun getPersonsForMedia(mediaId: Long): List<PersonEntity>

    @Query("""
        SELECT 
            f.id AS faceId,
            f.personId AS personId,
            p.name AS name,
            COALESCE(f.cropCachePath, p.coverCropPath) AS cropPath,
            COALESCE((
                SELECT COUNT(DISTINCT f2.mediaId) 
                FROM faces f2 
                INNER JOIN core_media c ON f2.mediaId = c.id 
                WHERE f2.personId = p.id AND c.bucketName != 'Trash' 
                  AND c.uriString NOT IN (SELECT originalUri FROM vault_media)
                  AND c.filePath NOT IN (SELECT originalPath FROM vault_media)
            ), 1) AS faceCount
        FROM faces f
        LEFT JOIN persons p ON f.personId = p.id
        WHERE f.mediaId = :mediaId AND (p.isHidden IS NULL OR p.isHidden = 0)
        GROUP BY COALESCE(f.personId, -f.id)
    """)
    fun observePeopleForMedia(mediaId: Long): Flow<List<MediaPersonInfo>>

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

    @Query("""
        SELECT COUNT(DISTINCT f.mediaId) FROM faces f 
        INNER JOIN core_media c ON f.mediaId = c.id 
        WHERE f.personId = :personId 
          AND c.bucketName != 'Trash' 
          AND c.uriString NOT IN (SELECT originalUri FROM vault_media)
          AND c.filePath NOT IN (SELECT originalPath FROM vault_media)
    """)
    suspend fun countDistinctMediaForPerson(personId: Long): Int

    @Query("""
        UPDATE persons SET faceCount = COALESCE((
            SELECT COUNT(DISTINCT f.mediaId) FROM faces f 
            INNER JOIN core_media c ON f.mediaId = c.id 
            WHERE f.personId = persons.id AND c.bucketName != 'Trash' 
              AND c.uriString NOT IN (SELECT originalUri FROM vault_media)
              AND c.filePath NOT IN (SELECT originalPath FROM vault_media)
        ), 0)
    """)
    suspend fun recalculateAllPersonFaceCounts()

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

    @Query("SELECT COUNT(*) FROM faces")
    suspend fun getFaceCount(): Int

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

    @Query("DELETE FROM media_face_status WHERE faceCount = 0")
    suspend fun clearZeroFaceStatuses(): Int

    @Query("UPDATE faces SET personId = :targetPersonId WHERE personId = :sourcePersonId")
    suspend fun reassignFaces(sourcePersonId: Long, targetPersonId: Long)

    @Query("UPDATE faces SET personId = :targetPersonId WHERE mediaId IN (:mediaIds) AND personId = :sourcePersonId")
    suspend fun reassignFacesForMedia(mediaIds: List<Long>, sourcePersonId: Long, targetPersonId: Long)

    @Query("""
        SELECT COUNT(id) FROM core_media 
        WHERE isVideo = 0 
          AND bucketName != 'Trash' 
          AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    suspend fun getTotalNonVaultMediaCount(): Int

    @Transaction
    suspend fun mergePersons(
        sourcePersonId: Long,
        targetPersonId: Long,
        mergedCentroid: FloatArray,
        faceCount: Int,
        coverCropPath: String?
    ) {
        val source = getPersonById(sourcePersonId)
        val target = getPersonById(targetPersonId) ?: return
        deleteMergeSuggestionsForPerson(sourcePersonId)
        reassignFaces(sourcePersonId, targetPersonId)
        val distinctPhotoCount = countDistinctMediaForPerson(targetPersonId)
        val finalName = if (!target.name.isNullOrBlank()) target.name else source?.name
        val updated = target.copy(
            name = finalName,
            centroid = mergedCentroid,
            faceCount = distinctPhotoCount,
            coverCropPath = coverCropPath ?: target.coverCropPath ?: source?.coverCropPath,
            isPromoted = true
        )
        updatePerson(updated)
        deletePerson(sourcePersonId)
    }

    @Query("DELETE FROM faces")
    suspend fun clearAllFaces()

    @Query("DELETE FROM persons")
    suspend fun clearAllPersons()

    // ── Merge Suggestions ──

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMergeSuggestions(suggestions: List<PersonMergeSuggestionEntity>)

    @Query("UPDATE person_merge_suggestions SET status = :status WHERE personIdA = :personIdA AND personIdB = :personIdB")
    suspend fun setMergeSuggestionStatus(personIdA: Long, personIdB: Long, status: String)

    @Query("SELECT status FROM person_merge_suggestions WHERE personIdA = :personIdA AND personIdB = :personIdB LIMIT 1")
    suspend fun getExistingSuggestionStatus(personIdA: Long, personIdB: Long): String?

    @Query("DELETE FROM person_merge_suggestions WHERE personIdA = :personId OR personIdB = :personId")
    suspend fun deleteMergeSuggestionsForPerson(personId: Long)

    @Transaction
    @Query("""
        SELECT s.* FROM person_merge_suggestions s
        INNER JOIN persons pa ON s.personIdA = pa.id
        INNER JOIN persons pb ON s.personIdB = pb.id
        WHERE s.status = 'PENDING' AND pa.isHidden = 0 AND pb.isHidden = 0
          AND ((pa.faceCount >= 2 OR (pa.name IS NOT NULL AND pa.name != '')) 
               OR (pb.faceCount >= 2 OR (pb.name IS NOT NULL AND pb.name != '')))
        ORDER BY s.similarity DESC
    """)
    fun observePendingMergeSuggestions(): Flow<List<MergeSuggestionWithPersons>>
}

