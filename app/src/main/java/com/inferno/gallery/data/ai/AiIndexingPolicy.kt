package com.inferno.gallery.data.ai

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.inferno.gallery.data.db.MediaEmbeddingEntity

/**
 * The on-device AI processing pipelines whose folder scope can be configured independently.
 */
enum class AiIndexingFeature(val key: String) {
    SMART_SEARCH("smart_search"),
    FACE_RECOGNITION("face_recognition"),
    OCR_TEXT("ocr_text");

    companion object {
        fun fromKey(key: String?): AiIndexingFeature? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Determines whether a folder list acts as a blocklist or allowlist.
 */
enum class AiFolderScopeMode(val storageValue: String) {
    /** Index all non-Trash/non-Vault folders except those explicitly excluded. */
    EXCLUDE_SELECTED("EXCLUDE_SELECTED"),

    /** Index only folders explicitly included by the user. */
    ONLY_SELECTED("ONLY_SELECTED");

    companion object {
        fun fromStorage(raw: String?): AiFolderScopeMode {
            return entries.firstOrNull { it.storageValue == raw } ?: EXCLUDE_SELECTED
        }
    }
}

/**
 * Per-feature folder filtering rule. Both sets are retained independently so
 * switching modes never discards the user's previous selections.
 */
data class AiFeatureFolderRule(
    val mode: AiFolderScopeMode = AiFolderScopeMode.EXCLUDE_SELECTED,
    val excludedFolders: Set<String> = emptySet(),
    val includedFolders: Set<String> = emptySet()
) {
    val activeFolders: Set<String>
        get() = when (mode) {
            AiFolderScopeMode.EXCLUDE_SELECTED -> excludedFolders
            AiFolderScopeMode.ONLY_SELECTED -> includedFolders
        }

    fun allowsBucket(
        bucketName: String?,
        galleryExcluded: Set<String>,
        respectGalleryExcluded: Boolean
    ): Boolean {
        val normalized = bucketName?.trim().orEmpty()
        if (normalized.isEmpty() || normalized == "Trash") return false
        if (respectGalleryExcluded && normalized in galleryExcluded) return false
        return when (mode) {
            AiFolderScopeMode.EXCLUDE_SELECTED -> normalized !in excludedFolders
            AiFolderScopeMode.ONLY_SELECTED -> normalized in includedFolders
        }
    }

    fun withBucketToggled(bucketName: String, currentlyIncluded: Boolean): AiFeatureFolderRule {
        val targetIncluded = !currentlyIncluded
        return withBucketIncluded(bucketName, targetIncluded)
    }

    fun withBucketIncluded(bucketName: String, included: Boolean): AiFeatureFolderRule {
        return when (mode) {
            AiFolderScopeMode.EXCLUDE_SELECTED -> copy(
                excludedFolders = if (included) excludedFolders - bucketName else excludedFolders + bucketName
            )
            AiFolderScopeMode.ONLY_SELECTED -> copy(
                includedFolders = if (included) includedFolders + bucketName else includedFolders - bucketName
            )
        }
    }

    /**
     * Switches between blocklist and allowlist modes while keeping every currently visible
     * folder's included/excluded status identical so UI switches do not jump.
     */
    fun convertingModePreservingSelection(
        targetMode: AiFolderScopeMode,
        knownBuckets: Collection<String>
    ): AiFeatureFolderRule {
        if (mode == targetMode) return this
        val normalizedBuckets = knownBuckets.map { it.trim() }.filter { it.isNotEmpty() && it != "Trash" }
        return when (targetMode) {
            AiFolderScopeMode.ONLY_SELECTED -> copy(
                mode = AiFolderScopeMode.ONLY_SELECTED,
                includedFolders = normalizedBuckets.filter { it !in excludedFolders }.toSet()
            )
            AiFolderScopeMode.EXCLUDE_SELECTED -> copy(
                mode = AiFolderScopeMode.EXCLUDE_SELECTED,
                excludedFolders = normalizedBuckets.filter { it !in includedFolders }.toSet()
            )
        }
    }
}

/**
 * Full configuration controlling which folders each AI model processes.
 */
data class AiIndexingFolderConfig(
    val respectGalleryExcluded: Boolean = true,
    val rules: Map<AiIndexingFeature, AiFeatureFolderRule> = emptyMap()
) {
    fun ruleFor(feature: AiIndexingFeature): AiFeatureFolderRule {
        return rules[feature] ?: AiFeatureFolderRule()
    }

    fun isBucketEligible(
        feature: AiIndexingFeature,
        bucketName: String?,
        galleryExcluded: Set<String>
    ): Boolean {
        return ruleFor(feature).allowsBucket(bucketName, galleryExcluded, respectGalleryExcluded)
    }

    fun areAllRulesUniform(): Boolean {
        val first = ruleFor(AiIndexingFeature.SMART_SEARCH)
        return ruleFor(AiIndexingFeature.FACE_RECOGNITION) == first &&
            ruleFor(AiIndexingFeature.OCR_TEXT) == first
    }

    fun countCustomizedFeatures(): Int {
        return AiIndexingFeature.entries.count { feature ->
            val rule = ruleFor(feature)
            rule.mode == AiFolderScopeMode.ONLY_SELECTED || rule.excludedFolders.isNotEmpty()
        }
    }

    fun countEligiblePhotos(
        feature: AiIndexingFeature,
        bucketCounts: Map<String, Int>,
        galleryExcluded: Set<String>
    ): Int {
        var total = 0
        for ((bucket, count) in bucketCounts) {
            if (isBucketEligible(feature, bucket, galleryExcluded)) {
                total += count
            }
        }
        return total
    }

    fun hasAnyCustomRule(): Boolean {
        return AiIndexingFeature.entries.any { feature ->
            val rule = ruleFor(feature)
            rule.mode != AiFolderScopeMode.EXCLUDE_SELECTED ||
                rule.excludedFolders.isNotEmpty() ||
                rule.includedFolders.isNotEmpty()
        }
    }
}

data class SqlQuerySpec(
    val sql: String,
    val args: List<Any> = emptyList()
) {
    fun toSupportQuery(): SupportSQLiteQuery = SimpleSQLiteQuery(sql, args.toTypedArray())
}

/**
 * Pure SQL query builder shared across AI workers, ViewModels, search, and cleanup
 * so eligible photo counts, pending IDs, and cleanup queries always stay consistent.
 */
object AiIndexingQueryBuilder {

    // Guard against SQLite's 999 host-parameter limit on large bucket sets.
    private const val SQL_IN_CHUNK_SIZE = 400

    private const val BASE_MEDIA_PREDICATE =
        "cm.isVideo = 0 " +
            "AND cm.bucketName IS NOT NULL " +
            "AND cm.bucketName != 'Trash' " +
            "AND cm.uriString NOT IN (SELECT originalUri FROM vault_media) " +
            "AND cm.filePath NOT IN (SELECT originalPath FROM vault_media)"

    fun buildBucketInclusionCondition(
        alias: String,
        rule: AiFeatureFolderRule,
        galleryExcluded: Set<String>,
        respectGalleryExcluded: Boolean
    ): SqlQuerySpec {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<Any>()

        clauses.add("$alias.bucketName IS NOT NULL")
        clauses.add("$alias.bucketName != 'Trash'")

        if (respectGalleryExcluded && galleryExcluded.isNotEmpty()) {
            val cleanExcluded = galleryExcluded.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
            for (chunk in cleanExcluded.chunked(SQL_IN_CHUNK_SIZE)) {
                val placeholders = chunk.joinToString(",") { "?" }
                clauses.add("$alias.bucketName NOT IN ($placeholders)")
                args.addAll(chunk)
            }
        }

        when (rule.mode) {
            AiFolderScopeMode.EXCLUDE_SELECTED -> {
                val cleanExcluded = rule.excludedFolders.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
                for (chunk in cleanExcluded.chunked(SQL_IN_CHUNK_SIZE)) {
                    val placeholders = chunk.joinToString(",") { "?" }
                    clauses.add("$alias.bucketName NOT IN ($placeholders)")
                    args.addAll(chunk)
                }
            }
            AiFolderScopeMode.ONLY_SELECTED -> {
                val cleanIncluded = rule.includedFolders.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
                val effectiveIncluded = if (respectGalleryExcluded && galleryExcluded.isNotEmpty()) {
                    cleanIncluded.filter { it !in galleryExcluded }
                } else {
                    cleanIncluded
                }
                if (effectiveIncluded.isEmpty()) {
                    clauses.add("1 = 0")
                } else {
                    val orGroups = mutableListOf<String>()
                    for (chunk in effectiveIncluded.chunked(SQL_IN_CHUNK_SIZE)) {
                        val placeholders = chunk.joinToString(",") { "?" }
                        orGroups.add("$alias.bucketName IN ($placeholders)")
                        args.addAll(chunk)
                    }
                    clauses.add(if (orGroups.size == 1) orGroups.first() else "(${orGroups.joinToString(" OR ")})")
                }
            }
        }

        return SqlQuerySpec(clauses.joinToString(" AND "), args)
    }

    fun buildBucketExclusionCondition(
        alias: String,
        rule: AiFeatureFolderRule,
        galleryExcluded: Set<String>,
        respectGalleryExcluded: Boolean
    ): SqlQuerySpec {
        val inclusion = buildBucketInclusionCondition(alias, rule, galleryExcluded, respectGalleryExcluded)
        // Return non-Trash media that fails the active inclusion predicate.
        return SqlQuerySpec(
            sql = "$alias.bucketName IS NOT NULL AND $alias.bucketName != 'Trash' AND NOT (${inclusion.sql})",
            args = inclusion.args
        )
    }

    fun buildEligibleTotalImagesCountQuery(
        feature: AiIndexingFeature,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): SqlQuerySpec {
        val bucketFilter = buildBucketInclusionCondition(
            alias = "cm",
            rule = config.ruleFor(feature),
            galleryExcluded = galleryExcluded,
            respectGalleryExcluded = config.respectGalleryExcluded
        )
        return SqlQuerySpec(
            sql = """
                SELECT COUNT(cm.id)
                FROM core_media cm
                WHERE $BASE_MEDIA_PREDICATE
                  AND ${bucketFilter.sql}
            """.trimIndent(),
            args = bucketFilter.args
        )
    }

    fun buildUnindexedSmartSearchQuery(
        countOnly: Boolean,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>,
        failedPermanent: String = MediaEmbeddingEntity.STATUS_FAILED_PERMANENT
    ): SqlQuerySpec {
        val bucketFilter = buildBucketInclusionCondition(
            alias = "cm",
            rule = config.ruleFor(AiIndexingFeature.SMART_SEARCH),
            galleryExcluded = galleryExcluded,
            respectGalleryExcluded = config.respectGalleryExcluded
        )
        val projection = if (countOnly) "SELECT COUNT(cm.id)" else "SELECT cm.id"
        val args = mutableListOf<Any>()
        args.add(failedPermanent)
        args.addAll(bucketFilter.args)
        val orderClause = if (countOnly) "" else "\nORDER BY cm.dateAdded DESC"
        return SqlQuerySpec(
            sql = """
                $projection
                FROM core_media cm
                LEFT JOIN media_embeddings me ON cm.id = me.mediaId
                LEFT JOIN media_embedding_status ms ON cm.id = ms.mediaId
                WHERE $BASE_MEDIA_PREDICATE
                  AND (me.mediaId IS NULL OR me.dateModified != cm.dateModified OR me.size != cm.size)
                  AND (
                      ms.mediaId IS NULL
                      OR NOT (
                          ms.status = ?
                          AND ms.dateModified = cm.dateModified
                          AND ms.size = cm.size
                      )
                  )
                  AND ${bucketFilter.sql}$orderClause
            """.trimIndent(),
            args = args
        )
    }

    fun buildUnindexedFaceQuery(
        countOnly: Boolean,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): SqlQuerySpec {
        val bucketFilter = buildBucketInclusionCondition(
            alias = "cm",
            rule = config.ruleFor(AiIndexingFeature.FACE_RECOGNITION),
            galleryExcluded = galleryExcluded,
            respectGalleryExcluded = config.respectGalleryExcluded
        )
        val projection = if (countOnly) "SELECT COUNT(cm.id)" else "SELECT cm.id"
        val orderClause = if (countOnly) "" else "\nORDER BY cm.dateAdded DESC"
        return SqlQuerySpec(
            sql = """
                $projection
                FROM core_media cm
                WHERE $BASE_MEDIA_PREDICATE
                  AND cm.id NOT IN (SELECT mediaId FROM media_face_status)
                  AND ${bucketFilter.sql}$orderClause
            """.trimIndent(),
            args = bucketFilter.args
        )
    }

    fun buildUnindexedOcrQuery(
        countOnly: Boolean,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): SqlQuerySpec {
        val bucketFilter = buildBucketInclusionCondition(
            alias = "cm",
            rule = config.ruleFor(AiIndexingFeature.OCR_TEXT),
            galleryExcluded = galleryExcluded,
            respectGalleryExcluded = config.respectGalleryExcluded
        )
        val projection = if (countOnly) "SELECT COUNT(cm.id)" else "SELECT cm.*"
        val orderClause = if (countOnly) "" else "\nORDER BY cm.dateAdded DESC"
        return SqlQuerySpec(
            sql = """
                $projection
                FROM core_media cm
                WHERE $BASE_MEDIA_PREDICATE
                  AND cm.is_indexed_ocr = 0
                  AND ${bucketFilter.sql}$orderClause
            """.trimIndent(),
            args = bucketFilter.args
        )
    }

    fun buildExcludedMediaIdsForCleanupQuery(
        feature: AiIndexingFeature,
        config: AiIndexingFolderConfig,
        galleryExcluded: Set<String>
    ): SqlQuerySpec {
        val exclusion = buildBucketExclusionCondition(
            alias = "cm",
            rule = config.ruleFor(feature),
            galleryExcluded = galleryExcluded,
            respectGalleryExcluded = config.respectGalleryExcluded
        )
        val indexedPredicate = when (feature) {
            AiIndexingFeature.SMART_SEARCH ->
                "(cm.id IN (SELECT mediaId FROM media_embeddings) OR cm.id IN (SELECT mediaId FROM media_embedding_status))"
            AiIndexingFeature.FACE_RECOGNITION ->
                "(cm.id IN (SELECT mediaId FROM media_face_status) OR cm.id IN (SELECT mediaId FROM faces))"
            AiIndexingFeature.OCR_TEXT ->
                "(cm.is_indexed_ocr = 1 OR cm.id IN (SELECT mediaId FROM document_classifications) OR CAST(cm.id AS TEXT) IN (SELECT mediaId FROM image_fts))"
        }
        return SqlQuerySpec(
            sql = """
                SELECT cm.id
                FROM core_media cm
                WHERE cm.isVideo = 0
                  AND ${exclusion.sql}
                  AND $indexedPredicate
            """.trimIndent(),
            args = exclusion.args
        )
    }
}
