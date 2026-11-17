package com.inferno.gallery.data.ai

import android.content.Context
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.GalleryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ExcludedAiDataSummary(
    val smartSearchCount: Int = 0,
    val faceCount: Int = 0,
    val ocrCount: Int = 0
) {
    val totalCount: Int
        get() = smartSearchCount + faceCount + ocrCount
}

/**
 * Counts and cleans up previously indexed AI data (semantic embeddings, OCR text/document rows,
 * and face detections/empty clusters) that belong to folders excluded by the active AI policy.
 */
object AiIndexCleanupManager {

    suspend fun getExcludedIndexedSummary(
        db: GalleryDatabase,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): ExcludedAiDataSummary = withContext(Dispatchers.IO) {
        val smartIds = db.embeddingDao().getEmbeddingLongListRaw(
            AiIndexingQueryBuilder.buildExcludedMediaIdsForCleanupQuery(
                AiIndexingFeature.SMART_SEARCH,
                config,
                galleryExcluded
            ).toSupportQuery()
        )
        val faceIds = db.faceDao().getFaceLongListRaw(
            AiIndexingQueryBuilder.buildExcludedMediaIdsForCleanupQuery(
                AiIndexingFeature.FACE_RECOGNITION,
                config,
                galleryExcluded
            ).toSupportQuery()
        )
        val ocrIds = db.mediaDao().getLongListRaw(
            AiIndexingQueryBuilder.buildExcludedMediaIdsForCleanupQuery(
                AiIndexingFeature.OCR_TEXT,
                config,
                galleryExcluded
            ).toSupportQuery()
        )
        ExcludedAiDataSummary(
            smartSearchCount = smartIds.size,
            faceCount = faceIds.size,
            ocrCount = ocrIds.size
        )
    }

    suspend fun purgeExcludedAiData(
        context: Context,
        db: GalleryDatabase,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>,
        feature: AiIndexingFeature? = null
    ): Int = withContext(Dispatchers.IO) {
        val targets = if (feature != null) listOf(feature) else AiIndexingFeature.entries
        var removedCount = 0

        if (AiIndexingFeature.SMART_SEARCH in targets) {
            val smartIds = db.embeddingDao().getEmbeddingLongListRaw(
                AiIndexingQueryBuilder.buildExcludedMediaIdsForCleanupQuery(
                    AiIndexingFeature.SMART_SEARCH,
                    config,
                    galleryExcluded
                ).toSupportQuery()
            )
            if (smartIds.isNotEmpty()) {
                db.embeddingDao().deleteEmbeddingsByMediaIdsChunked(smartIds)
                db.embeddingStatusDao().deleteStatusesByMediaIdsChunked(smartIds)
                removedCount += smartIds.size
            }
        }

        if (AiIndexingFeature.OCR_TEXT in targets) {
            val ocrIds = db.mediaDao().getLongListRaw(
                AiIndexingQueryBuilder.buildExcludedMediaIdsForCleanupQuery(
                    AiIndexingFeature.OCR_TEXT,
                    config,
                    galleryExcluded
                ).toSupportQuery()
            )
            if (ocrIds.isNotEmpty()) {
                ocrIds.chunked(500).forEach { chunk ->
                    DatabaseProvider.deleteFtsRows(db, chunk)
                }
                db.mediaDao().deleteDocumentClassificationsByMediaIdsChunked(ocrIds)
                db.mediaDao().resetOcrIndexStatusByIdsChunked(ocrIds)
                removedCount += ocrIds.size
            }
        }

        if (AiIndexingFeature.FACE_RECOGNITION in targets) {
            val faceDao = db.faceDao()
            val faceMediaIds = faceDao.getFaceLongListRaw(
                AiIndexingQueryBuilder.buildExcludedMediaIdsForCleanupQuery(
                    AiIndexingFeature.FACE_RECOGNITION,
                    config,
                    galleryExcluded
                ).toSupportQuery()
            )
            if (faceMediaIds.isNotEmpty()) {
                val cropPaths = faceDao.getCropPathsForMediaIdsChunked(faceMediaIds)
                cropPaths.forEach { path ->
                    runCatching { File(path).delete() }
                }
                faceDao.deleteFacesByMediaIdsChunked(faceMediaIds)
                faceDao.deleteFaceStatusesByMediaIdsChunked(faceMediaIds)
                faceDao.recalculateAllPersonFaceCounts()

                val emptyUnnamed = faceDao.getUnnamedEmptyPersons()
                if (emptyUnnamed.isNotEmpty()) {
                    val emptyIds = emptyUnnamed.map { it.id }
                    emptyUnnamed.forEach { person ->
                        person.coverCropPath?.let { path -> runCatching { File(path).delete() } }
                    }
                    faceDao.deleteMergeSuggestionsForPersonsChunked(emptyIds)
                    faceDao.deletePersonsByIdsChunked(emptyIds)
                }

                // Refresh cover crops for named people if their previous cover belonged to a purged folder.
                for (person in faceDao.getNonEmptyPersonsBasic()) {
                    val currentCover = person.coverCropPath
                    if (currentCover.isNullOrBlank() || !File(currentCover).exists()) {
                        val fallbackCover = faceDao.getFirstCropPathForPerson(person.id)
                        if (fallbackCover != null) {
                            faceDao.setPersonCover(person.id, fallbackCover)
                        }
                    }
                }

                FaceClusteringManager.getInstance(context).invalidateCache()
                removedCount += faceMediaIds.size
            }
        }

        removedCount
    }
}
