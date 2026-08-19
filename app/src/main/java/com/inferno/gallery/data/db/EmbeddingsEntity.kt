package com.inferno.gallery.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "media_embeddings")
data class MediaEmbeddingEntity(
    @PrimaryKey val mediaId: Long,
    val embedding: FloatArray,
    val dateModified: Long,
    val size: Long
) {
    companion object {
        const val STATUS_FAILED_PERMANENT = "FAILED_PERMANENT"
        const val STATUS_FAILED_TRANSIENT = "FAILED_TRANSIENT"
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MediaEmbeddingEntity
        if (mediaId != other.mediaId) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (dateModified != other.dateModified) return false
        if (size != other.size) return false
        return true
    }

    override fun hashCode(): Int {
        var result = mediaId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + dateModified.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
}

class EmbeddingConverter {
    @TypeConverter
    fun toByteArray(array: FloatArray?): ByteArray? {
        if (array == null) return null
        val buffer = ByteBuffer.allocate(array.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        array.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    @TypeConverter
    fun toFloatArray(bytes: ByteArray?): FloatArray? {
        if (bytes == null) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val array = FloatArray(bytes.size / 4)
        buffer.asFloatBuffer().get(array)
        return array
    }
}

@Dao
interface EmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: MediaEmbeddingEntity)

    @Query("SELECT * FROM media_embeddings WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getEmbedding(mediaId: Long): MediaEmbeddingEntity?

    @Query("SELECT * FROM media_embeddings")
    suspend fun getAllEmbeddings(): List<MediaEmbeddingEntity>

    @Query(
        """
        SELECT cm.id
        FROM core_media cm
        LEFT JOIN media_embeddings me ON cm.id = me.mediaId
        LEFT JOIN media_embedding_status ms ON cm.id = ms.mediaId
        WHERE cm.isVideo = 0
          AND (me.mediaId IS NULL OR me.dateModified != cm.dateModified OR me.size != cm.size)
          AND (
              ms.mediaId IS NULL
              OR NOT (
                  ms.status = :failedPermanent
                  AND ms.dateModified = cm.dateModified
                  AND ms.size = cm.size
              )
          )
        """
    )
    suspend fun getUnindexedMediaIds(
        failedPermanent: String = MediaEmbeddingEntity.STATUS_FAILED_PERMANENT
    ): List<Long>

    @Query(
        """
        SELECT COUNT(cm.id)
        FROM core_media cm
        LEFT JOIN media_embeddings me ON cm.id = me.mediaId
        LEFT JOIN media_embedding_status ms ON cm.id = ms.mediaId
        WHERE cm.isVideo = 0
          AND (me.mediaId IS NULL OR me.dateModified != cm.dateModified OR me.size != cm.size)
          AND (
              ms.mediaId IS NULL
              OR NOT (
                  ms.status = :failedPermanent
                  AND ms.dateModified = cm.dateModified
                  AND ms.size = cm.size
              )
          )
        """
    )
    fun observeUnindexedCount(
        failedPermanent: String = MediaEmbeddingEntity.STATUS_FAILED_PERMANENT
    ): Flow<Int>

    @Query("DELETE FROM media_embeddings WHERE mediaId = :mediaId")
    suspend fun deleteEmbedding(mediaId: Long)

    @Query("DELETE FROM media_embeddings")
    suspend fun clearAllEmbeddings()
}

@Entity(tableName = "media_embedding_status")
data class MediaEmbeddingStatusEntity(
    @PrimaryKey val mediaId: Long,
    val status: String,
    val dateModified: Long,
    val size: Long,
    val updatedAt: Long
)

@Dao
interface EmbeddingStatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatus(status: MediaEmbeddingStatusEntity)

    @Query("DELETE FROM media_embedding_status WHERE mediaId = :mediaId")
    suspend fun deleteStatus(mediaId: Long)

    @Query("DELETE FROM media_embedding_status")
    suspend fun clearAllStatuses()
}
