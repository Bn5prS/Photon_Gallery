package com.inferno.gallery.data

/**
 * Pure-function SQL query builder for media queries.
 * Extracted from GalleryViewModel for testability.
 *
 * All methods are stateless and produce SQL fragments + bind args
 * that can be verified without Android framework dependencies.
 */
object MediaQueryBuilder {

    private const val NOT_HIDDEN = "cm.bucketName != 'Trash'"

    data class QueryConditions(
        val conditions: List<String>,
        val args: List<Any>
    )

    /**
     * Builds SQL WHERE conditions and bind args for media queries.
     *
     * @param bucket       The target bucket name (e.g. "Camera", "Favorites", "search_text")
     * @param filterIndex  The dock filter index (0=All, 1=Camera, 2=Screenshots)
     * @param excluded     Set of folder names to exclude
     * @param favIds       Set of favorite media ID strings
     * @param ftsIds       List of media ID strings from FTS text search
     * @param smartIds     List of media ID strings from smart/semantic search
     */
    fun buildMediaConditions(
        bucket: String?,
        filterIndex: Int,
        excluded: Set<String> = emptySet(),
        favIds: Set<String> = emptySet(),
        ftsIds: List<String> = emptyList(),
        smartIds: List<String> = emptyList()
    ): QueryConditions {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any>()

        val folderName: String? = null

        when {
            bucket == BucketNames.SEARCH_TEXT -> {
                if (ftsIds.isEmpty()) {
                    conditions.add("cm.id IN (0)")
                } else {
                    conditions.add("cm.id IN (${ftsIds.joinToString(",")})")
                    conditions.add(NOT_HIDDEN)
                }
            }
            bucket == BucketNames.SEARCH_SMART -> {
                if (smartIds.isEmpty()) {
                    conditions.add("cm.id IN (0)")
                } else {
                    conditions.add("cm.id IN (${smartIds.joinToString(",")})")
                    conditions.add(NOT_HIDDEN)
                }
            }
            bucket == BucketNames.FAVORITES -> {
                val numericIds = favIds.mapNotNull { it.toLongOrNull() }
                if (numericIds.isEmpty()) {
                    conditions.add("cm.id IN (0)")
                } else {
                    conditions.add("cm.id IN (${numericIds.joinToString(",")})")
                    conditions.add(NOT_HIDDEN)
                }
            }
            bucket == BucketNames.ALL || (bucket == null && folderName == null) -> {
                conditions.add("cm.bucketName != 'Trash'")
            }
            bucket == BucketNames.VIDEOS -> {
                conditions.add("cm.isVideo = 1")
                conditions.add(NOT_HIDDEN)
            }
            bucket?.startsWith("place:") == true -> {
                val placeName = bucket.removePrefix("place:")
                conditions.add("cm.id IN (SELECT mediaId FROM geocoded_locations WHERE placeName = ?)")
                args.add(placeName)
            }
            bucket == BucketNames.MEDIA_TYPE_RAW -> {
                conditions.add("(cm.mimeType LIKE '%raw%' OR cm.mimeType LIKE '%dng%' OR cm.name LIKE '%.dng' OR cm.name LIKE '%.raw' OR cm.name LIKE '%.cr2' OR cm.name LIKE '%.nef' OR cm.name LIKE '%.arw' OR cm.name LIKE '%.orf' OR cm.name LIKE '%.raf')")
                conditions.add(NOT_HIDDEN)
            }
            bucket == BucketNames.MEDIA_TYPE_PANORAMAS -> {
                conditions.add("(cm.name LIKE '%PANO%' OR cm.name LIKE '%PANORAMA%' OR cm.filePath LIKE '%PANO%' OR cm.filePath LIKE '%panorama%') AND cm.isVideo = 0")
                conditions.add(NOT_HIDDEN)
            }
            bucket == BucketNames.MEDIA_TYPE_SLOW_MO -> {
                conditions.add("(cm.name LIKE '%SLOW%' OR cm.name LIKE '%HFR%' OR cm.filePath LIKE '%slow_motion%') AND cm.isVideo = 1")
                conditions.add(NOT_HIDDEN)
            }
            bucket == BucketNames.MEDIA_TYPE_ANIMATIONS -> {
                conditions.add("(cm.mimeType = 'image/gif' OR cm.name LIKE '%.gif') AND cm.isVideo = 0")
                conditions.add(NOT_HIDDEN)
            }
            bucket != null -> {
                conditions.add("cm.bucketName = ?")
                args.add(bucket)
            }
            folderName != null -> {
                conditions.add("cm.bucketName = ?")
                args.add(folderName)
            }
            else -> {
                conditions.add(NOT_HIDDEN)
            }
        }

        // Always ensure items locked in Private Space (Vault) NEVER appear in any gallery query
        conditions.add("cm.uriString NOT IN (SELECT originalUri FROM vault_media)")
        conditions.add("cm.filePath NOT IN (SELECT originalPath FROM vault_media)")

        // Apply excluded folders filter across all feed, search, favorite, media type, and general views
        val isSpecificAlbum = bucket != null &&
            bucket != BucketNames.ALL &&
            bucket != BucketNames.VIDEOS &&
            bucket != BucketNames.FAVORITES &&
            bucket != BucketNames.SEARCH_TEXT &&
            bucket != BucketNames.SEARCH_SMART &&
            bucket != BucketNames.MEDIA_TYPE_RAW &&
            bucket != BucketNames.MEDIA_TYPE_PANORAMAS &&
            bucket != BucketNames.MEDIA_TYPE_SLOW_MO &&
            bucket != BucketNames.MEDIA_TYPE_ANIMATIONS &&
            !bucket.startsWith("place:")

        if (!isSpecificAlbum && excluded.isNotEmpty()) {
            val placeholders = excluded.joinToString(",") { "?" }
            conditions.add("cm.bucketName NOT IN ($placeholders)")
            args.addAll(excluded)
        }

        return QueryConditions(conditions, args)
    }

    fun buildWhereClause(qc: QueryConditions): String {
        return if (qc.conditions.isNotEmpty()) "WHERE ${qc.conditions.joinToString(" AND ")} " else ""
    }

    fun buildOrderClause(order: String): String = when (order) {
        "NewToOld" -> "ORDER BY cm.dateAdded DESC"
        "OldToNew" -> "ORDER BY cm.dateAdded ASC"
        "SmallToBig" -> "ORDER BY cm.size ASC"
        "BigToSmall" -> "ORDER BY cm.size DESC"
        "NameAsc" -> "ORDER BY cm.name ASC"
        else -> "ORDER BY cm.dateAdded DESC"
    }

    fun buildOrderClause(order: String, bucket: String?, smartIds: List<String>): String {
        if (bucket == BucketNames.SEARCH_SMART) {
            val numericIds = smartIds.mapNotNull { it.toLongOrNull() }
            if (numericIds.isNotEmpty()) {
                val cases = numericIds.mapIndexed { index, id -> "WHEN $id THEN $index" }
                    .joinToString(" ")
                return "ORDER BY CASE cm.id $cases ELSE ${numericIds.size} END"
            }
        }
        return buildOrderClause(order)
    }
}
