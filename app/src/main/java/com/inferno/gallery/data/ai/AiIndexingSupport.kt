package com.inferno.gallery.data.ai

import android.content.Context
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.GalleryDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Shared runtime entry points for reading eligible image counts, unindexed IDs,
 * and reactive scope flows across AI workers, triggers, and ViewModels.
 */
object AiIndexingSupport {

    suspend fun loadScope(context: Context): Pair<AiIndexingFolderConfig, Set<String>> {
        val repo = SettingsRepository.getInstance(context)
        return repo.aiIndexingFolderConfigFlow.first() to repo.excludedFoldersFlow.first()
    }

    suspend fun getEligibleTotalCount(
        db: GalleryDatabase,
        feature: AiIndexingFeature,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): Int {
        return db.mediaDao().getIntRaw(
            AiIndexingQueryBuilder.buildEligibleTotalImagesCountQuery(feature, config, galleryExcluded).toSupportQuery()
        )
    }

    suspend fun getEligibleUnindexedSmartIds(
        db: GalleryDatabase,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): List<Long> {
        return db.embeddingDao().getEmbeddingLongListRaw(
            AiIndexingQueryBuilder.buildUnindexedSmartSearchQuery(false, config, galleryExcluded).toSupportQuery()
        )
    }

    suspend fun getEligibleUnindexedSmartCount(
        db: GalleryDatabase,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): Int {
        return db.embeddingDao().getEmbeddingIntRaw(
            AiIndexingQueryBuilder.buildUnindexedSmartSearchQuery(true, config, galleryExcluded).toSupportQuery()
        )
    }

    suspend fun getEligibleUnindexedFaceIds(
        db: GalleryDatabase,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): List<Long> {
        return db.faceDao().getFaceLongListRaw(
            AiIndexingQueryBuilder.buildUnindexedFaceQuery(false, config, galleryExcluded).toSupportQuery()
        )
    }

    suspend fun getEligibleUnindexedFaceCount(
        db: GalleryDatabase,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): Int {
        return db.faceDao().getFaceIntRaw(
            AiIndexingQueryBuilder.buildUnindexedFaceQuery(true, config, galleryExcluded).toSupportQuery()
        )
    }

    suspend fun getEligibleUnindexedOcrMedia(
        db: GalleryDatabase,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): List<CoreMediaEntity> {
        return db.mediaDao().getMediaEntitiesRaw(
            AiIndexingQueryBuilder.buildUnindexedOcrQuery(false, config, galleryExcluded).toSupportQuery()
        )
    }

    suspend fun getEligibleUnindexedOcrCount(
        db: GalleryDatabase,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): Int {
        return db.mediaDao().getIntRaw(
            AiIndexingQueryBuilder.buildUnindexedOcrQuery(true, config, galleryExcluded).toSupportQuery()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeEligibleTotalCount(
        db: GalleryDatabase,
        feature: AiIndexingFeature,
        configFlow: Flow<AiIndexingFolderConfig>,
        galleryExcludedFlow: Flow<Set<String>>
    ): Flow<Int> = combine(configFlow, galleryExcludedFlow) { cfg, exc -> cfg to exc }
        .flatMapLatest { (cfg, exc) ->
            db.mediaDao().observeIntRaw(
                AiIndexingQueryBuilder.buildEligibleTotalImagesCountQuery(feature, cfg, exc).toSupportQuery()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeEligibleUnindexedCount(
        db: GalleryDatabase,
        feature: AiIndexingFeature,
        configFlow: Flow<AiIndexingFolderConfig>,
        galleryExcludedFlow: Flow<Set<String>>
    ): Flow<Int> = combine(configFlow, galleryExcludedFlow) { cfg, exc -> cfg to exc }
        .flatMapLatest { (cfg, exc) ->
            when (feature) {
                AiIndexingFeature.SMART_SEARCH -> db.embeddingDao().observeEmbeddingIntRaw(
                    AiIndexingQueryBuilder.buildUnindexedSmartSearchQuery(true, cfg, exc).toSupportQuery()
                )
                AiIndexingFeature.FACE_RECOGNITION -> db.faceDao().observeFaceIntRaw(
                    AiIndexingQueryBuilder.buildUnindexedFaceQuery(true, cfg, exc).toSupportQuery()
                )
                AiIndexingFeature.OCR_TEXT -> db.mediaDao().observeIntRaw(
                    AiIndexingQueryBuilder.buildUnindexedOcrQuery(true, cfg, exc).toSupportQuery()
                )
            }
        }
}
