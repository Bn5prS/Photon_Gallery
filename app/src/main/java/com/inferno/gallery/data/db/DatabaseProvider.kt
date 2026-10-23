package com.inferno.gallery.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SearchMediaResult(
    val entity: CoreMediaEntity,
    val matchType: String,
    val matchSnippet: String? = null
)

object SearchMatchType {
    const val OCR = "OCR"
    const val FILE_NAME = "NAME"
    const val FORMAT = "FORMAT"
    const val SEMANTIC = "SEMANTIC"
    const val ALBUM = "ALBUM"
    const val LOCATION = "LOCATION"
    const val DATE = "DATE"
}

object DatabaseProvider {
    @Volatile
    private var INSTANCE: GalleryDatabase? = null

    fun getDatabase(context: Context): GalleryDatabase {
        return INSTANCE ?: synchronized(this) {
            // ── Database Migrations ──
            // Migrations 1→4 cover early schema evolution before public release.
            // These are no-ops since any user on v1-3 already went through destructive
            // fallback. They exist to maintain a complete migration chain going forward.
            val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Early dev: no schema changes tracked
                }
            }
            val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Early dev: no schema changes tracked
                }
            }
            val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Telegram backups table + embeddings table added
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS telegram_backups (
                            mediaId INTEGER PRIMARY KEY NOT NULL,
                            telegramFileId TEXT,
                            telegramThumbFileId TEXT,
                            telegramMessageId INTEGER,
                            backupStatus TEXT NOT NULL,
                            backupTimestamp INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS media_embeddings (
                            mediaId INTEGER PRIMARY KEY NOT NULL,
                            embedding BLOB NOT NULL
                        )
                    """.trimIndent())
                }
            }
            val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE telegram_backups ADD COLUMN telegramMessageId INTEGER DEFAULT NULL")
                }
            }
            val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Ensure embeddings table exists for smart search
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS media_embeddings (
                            mediaId INTEGER PRIMARY KEY NOT NULL,
                            embedding BLOB NOT NULL
                        )
                    """.trimIndent())
                }
            }

            val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS vault_media (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            originalUri TEXT NOT NULL,
                            originalPath TEXT NOT NULL,
                            originalBucket TEXT NOT NULL,
                            fileName TEXT NOT NULL,
                            vaultFileName TEXT NOT NULL,
                            mimeType TEXT,
                            isVideo INTEGER NOT NULL DEFAULT 0,
                            durationMs INTEGER,
                            size INTEGER NOT NULL DEFAULT 0,
                            dateAdded INTEGER NOT NULL DEFAULT 0,
                            dateHidden INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())
                }
            }

            val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE vault_media ADD COLUMN originalRelativePath TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE vault_media ADD COLUMN dateModified INTEGER NOT NULL DEFAULT 0")
                }
            }

            val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE core_media ADD COLUMN pHash INTEGER")
                    db.execSQL("ALTER TABLE core_media ADD COLUMN latitude REAL")
                    db.execSQL("ALTER TABLE core_media ADD COLUMN longitude REAL")
                }
            }

            val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE core_media ADD COLUMN fileHash TEXT")
                    // Reset pHash so dHash is recomputed (aHash -> dHash algorithm change)
                    db.execSQL("UPDATE core_media SET pHash = NULL")
                }
            }

            val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE media_embeddings ADD COLUMN dateModified INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE media_embeddings ADD COLUMN size INTEGER NOT NULL DEFAULT 0")
                }
            }

            val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS media_embedding_status (
                            mediaId INTEGER PRIMARY KEY NOT NULL,
                            status TEXT NOT NULL,
                            dateModified INTEGER NOT NULL DEFAULT 0,
                            size INTEGER NOT NULL DEFAULT 0,
                            updatedAt INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                }
            }

            val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sync_queue (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            mediaId INTEGER NOT NULL,
                            operationType TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            status TEXT NOT NULL,
                            retryCount INTEGER NOT NULL,
                            errorReason TEXT
                        )
                        """.trimIndent()
                    )
                }
            }

            val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS manual_unstack (
                            mediaId TEXT PRIMARY KEY NOT NULL
                        )
                        """.trimIndent()
                    )
                }
            }

            val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE telegram_backups ADD COLUMN viewCount INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE telegram_backups ADD COLUMN lastViewedAt INTEGER NOT NULL DEFAULT 0")
                }
            }

            val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE telegram_backups ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE telegram_backups ADD COLUMN nextRetryAt INTEGER NOT NULL DEFAULT 0")
                }
            }

            val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE telegram_backups ADD COLUMN minithumbnail TEXT DEFAULT NULL")
                }
            }

            val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE telegram_backups ADD COLUMN chatId INTEGER NOT NULL DEFAULT 0")
                }
            }

            val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE telegram_backups ADD COLUMN isEncrypted INTEGER NOT NULL DEFAULT 0")
                }
            }

            // v20: Telegram cloud feature removed. Drop telegram_backups and sync_queue tables.
            val MIGRATION_19_20 = object : androidx.room.migration.Migration(19, 20) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS telegram_backups")
                    db.execSQL("DROP TABLE IF EXISTS sync_queue")
                }
            }

            // v21: Photo Stacks feature fully pruned. Drop manual_unstack table.
            val MIGRATION_20_21 = object : androidx.room.migration.Migration(20, 21) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS manual_unstack")
                }
            }

            // v22: Face Detection & Recognition with Person Clustering
            val MIGRATION_21_22 = object : androidx.room.migration.Migration(21, 22) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE core_media ADD COLUMN is_indexed_faces INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("DROP TABLE IF EXISTS faces")
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS faces (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            mediaId INTEGER NOT NULL,
                            clusterId INTEGER NOT NULL,
                            boundingBoxLeft REAL NOT NULL DEFAULT 0.0,
                            boundingBoxTop REAL NOT NULL DEFAULT 0.0,
                            boundingBoxRight REAL NOT NULL DEFAULT 0.0,
                            boundingBoxBottom REAL NOT NULL DEFAULT 0.0,
                            cropCachePath TEXT,
                            embedding BLOB,
                            FOREIGN KEY(mediaId) REFERENCES core_media(id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_faces_mediaId ON faces(mediaId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_faces_clusterId ON faces(clusterId)")
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS person_clusters (
                            clusterId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT,
                            coverFaceId INTEGER,
                            coverMediaId INTEGER,
                            coverCropPath TEXT,
                            faceCount INTEGER NOT NULL DEFAULT 1,
                            centroid BLOB,
                            isFavorite INTEGER NOT NULL DEFAULT 0,
                            isHidden INTEGER NOT NULL DEFAULT 0,
                            updatedAt INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())
                }
            }

            // v23: Composite indices for keyset pagination and high-speed filtering
            val MIGRATION_22_23 = object : androidx.room.migration.Migration(22, 23) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP INDEX IF EXISTS index_core_media_dateAdded_id")
                    db.execSQL("DROP INDEX IF EXISTS index_core_media_bucketName_dateAdded_id")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_dateAdded_id ON core_media(dateAdded, id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_bucketName_dateAdded_id ON core_media(bucketName, dateAdded, id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_uriString ON core_media(uriString)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_filePath ON core_media(filePath)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_isVideo_bucketName ON core_media(isVideo, bucketName)")
                }
            }

            // v24: Ensure index schema matches Room expectations exactly
            val MIGRATION_23_24 = object : androidx.room.migration.Migration(23, 24) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP INDEX IF EXISTS index_core_media_dateAdded_id")
                    db.execSQL("DROP INDEX IF EXISTS index_core_media_bucketName_dateAdded_id")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_dateAdded_id ON core_media(dateAdded, id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_bucketName_dateAdded_id ON core_media(bucketName, dateAdded, id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_uriString ON core_media(uriString)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_filePath ON core_media(filePath)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_isVideo_bucketName ON core_media(isVideo, bucketName)")
                }
            }

            val MIGRATION_22_24 = object : androidx.room.migration.Migration(22, 24) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP INDEX IF EXISTS index_core_media_dateAdded_id")
                    db.execSQL("DROP INDEX IF EXISTS index_core_media_bucketName_dateAdded_id")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_dateAdded_id ON core_media(dateAdded, id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_bucketName_dateAdded_id ON core_media(bucketName, dateAdded, id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_uriString ON core_media(uriString)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_filePath ON core_media(filePath)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_isVideo_bucketName ON core_media(isVideo, bucketName)")
                }
            }

            val MIGRATION_24_25 = object : androidx.room.migration.Migration(24, 25) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS faces")
                    db.execSQL("DROP TABLE IF EXISTS person_clusters")
                }
            }

            val MIGRATION_23_25 = object : androidx.room.migration.Migration(23, 25) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP INDEX IF EXISTS index_core_media_dateAdded_id")
                    db.execSQL("DROP INDEX IF EXISTS index_core_media_bucketName_dateAdded_id")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_dateAdded_id ON core_media(dateAdded, id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_bucketName_dateAdded_id ON core_media(bucketName, dateAdded, id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_uriString ON core_media(uriString)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_filePath ON core_media(filePath)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_isVideo_bucketName ON core_media(isVideo, bucketName)")
                    db.execSQL("DROP TABLE IF EXISTS faces")
                    db.execSQL("DROP TABLE IF EXISTS person_clusters")
                }
            }

            val MIGRATION_22_25 = object : androidx.room.migration.Migration(22, 25) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP INDEX IF EXISTS index_core_media_dateAdded_id")
                    db.execSQL("DROP INDEX IF EXISTS index_core_media_bucketName_dateAdded_id")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_dateAdded_id ON core_media(dateAdded, id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_bucketName_dateAdded_id ON core_media(bucketName, dateAdded, id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_uriString ON core_media(uriString)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_filePath ON core_media(filePath)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_core_media_isVideo_bucketName ON core_media(isVideo, bucketName)")
                    db.execSQL("DROP TABLE IF EXISTS faces")
                    db.execSQL("DROP TABLE IF EXISTS person_clusters")
                }
            }

            val MIGRATION_25_26 = object : androidx.room.migration.Migration(25, 26) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS photo_spheres (
                            mediaId INTEGER PRIMARY KEY NOT NULL,
                            isSphere INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }
            }

            val MIGRATION_26_27 = object : androidx.room.migration.Migration(26, 27) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS persons (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT,
                            coverCropPath TEXT,
                            isHidden INTEGER NOT NULL DEFAULT 0,
                            isFavorite INTEGER NOT NULL DEFAULT 0,
                            isPromoted INTEGER NOT NULL DEFAULT 0,
                            faceCount INTEGER NOT NULL DEFAULT 0,
                            centroid BLOB,
                            createdAt INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS faces (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            mediaId INTEGER NOT NULL,
                            personId INTEGER,
                            bboxLeft REAL NOT NULL DEFAULT 0.0,
                            bboxTop REAL NOT NULL DEFAULT 0.0,
                            bboxRight REAL NOT NULL DEFAULT 0.0,
                            bboxBottom REAL NOT NULL DEFAULT 0.0,
                            qualityScore REAL NOT NULL DEFAULT 0.0,
                            isExemplar INTEGER NOT NULL DEFAULT 0,
                            cropCachePath TEXT,
                            embedding BLOB NOT NULL,
                            FOREIGN KEY(personId) REFERENCES persons(id) ON DELETE SET NULL,
                            FOREIGN KEY(mediaId) REFERENCES core_media(id) ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_faces_personId ON faces(personId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_faces_mediaId ON faces(mediaId)")
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS media_face_status (
                            mediaId INTEGER PRIMARY KEY NOT NULL,
                            faceCount INTEGER NOT NULL DEFAULT 0,
                            scannedAt INTEGER NOT NULL DEFAULT 0,
                            FOREIGN KEY(mediaId) REFERENCES core_media(id) ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                }
            }

            val MIGRATION_27_28 = object : androidx.room.migration.Migration(27, 28) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS person_merge_suggestions (
                            personIdA INTEGER NOT NULL,
                            personIdB INTEGER NOT NULL,
                            similarity REAL NOT NULL,
                            status TEXT NOT NULL DEFAULT 'PENDING',
                            createdAt INTEGER NOT NULL,
                            PRIMARY KEY(personIdA, personIdB),
                            FOREIGN KEY(personIdA) REFERENCES persons(id) ON DELETE CASCADE,
                            FOREIGN KEY(personIdB) REFERENCES persons(id) ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_person_merge_suggestions_personIdA ON person_merge_suggestions(personIdA)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_person_merge_suggestions_personIdB ON person_merge_suggestions(personIdB)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_person_merge_suggestions_status ON person_merge_suggestions(status)")
                }
            }

            val instance = Room.databaseBuilder(
                context.applicationContext,
                GalleryDatabase::class.java,
                "gallery_database.db"
            )
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
                MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
                MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
                MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_22_24,
                MIGRATION_24_25, MIGRATION_23_25, MIGRATION_22_25, MIGRATION_25_26,
                MIGRATION_26_27, MIGRATION_27_28
            )
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .fallbackToDestructiveMigration(true)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // FTS5 virtual table created via raw SQL callback.
                    // Intentionally NOT in @Database entities — the @Fts5 annotation has a
                    // KSP 2.2.x incompatibility ([MissingType] / compile-time validator errors).
                    // Porter tokenizer enables stemmed searches (run/running/ran all match).
                    db.execSQL(
                        """
                        CREATE VIRTUAL TABLE IF NOT EXISTS image_fts
                        USING fts4(
                            mediaId,
                            extractedText,
                            generatedTags,
                            tokenize=porter
                        )
                        """.trimIndent()
                    )

                    // Create trigger to clean up FTS index when core_media is deleted
                    db.execSQL(
                        """
                        CREATE TRIGGER IF NOT EXISTS core_media_fts_delete
                        AFTER DELETE ON core_media
                        BEGIN
                            DELETE FROM image_fts WHERE mediaId = CAST(old.id AS TEXT);
                        END;
                        """.trimIndent()
                    )

                    // Photo Spheres classification cache table
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS photo_spheres (
                            mediaId INTEGER PRIMARY KEY,
                            isSphere INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TRIGGER IF NOT EXISTS core_media_spheres_delete
                        AFTER DELETE ON core_media
                        BEGIN
                            DELETE FROM photo_spheres WHERE mediaId = old.id;
                        END;
                        """.trimIndent()
                    )
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Create FTS table if it doesn't exist — in onOpen (not just onCreate)
                    // so that users who upgraded through migrations also get the table.
                    db.execSQL(
                        """
                        CREATE VIRTUAL TABLE IF NOT EXISTS image_fts
                        USING fts4(
                            mediaId,
                            extractedText,
                            generatedTags,
                            tokenize=porter
                        )
                        """.trimIndent()
                    )
                    // Ensure trigger exists for cleaning up FTS index on media deletion
                    db.execSQL(
                        """
                        CREATE TRIGGER IF NOT EXISTS core_media_fts_delete
                        AFTER DELETE ON core_media
                        BEGIN
                            DELETE FROM image_fts WHERE mediaId = CAST(old.id AS TEXT);
                        END;
                        """.trimIndent()
                    )

                    // Ensure photo_spheres table and trigger exist
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS photo_spheres (
                            mediaId INTEGER PRIMARY KEY,
                            isSphere INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TRIGGER IF NOT EXISTS core_media_spheres_delete
                        AFTER DELETE ON core_media
                        BEGIN
                            DELETE FROM photo_spheres WHERE mediaId = old.id;
                        END;
                        """.trimIndent()
                    )
                }
            })
            .build()
            INSTANCE = instance
            instance
        }
    }

    suspend fun getUncheckedSphereCandidates(db: GalleryDatabase): List<Pair<Long, String>> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Pair<Long, String>>()
        try {
            val rawDb = db.openHelper.readableDatabase
            val cursor = rawDb.query(
                """
                SELECT id, filePath, uriString FROM core_media
                WHERE (name LIKE '%PANO%' OR name LIKE '%PANORAMA%' OR filePath LIKE '%PANO%' OR filePath LIKE '%panorama%')
                  AND isVideo = 0
                  AND id NOT IN (SELECT mediaId FROM photo_spheres)
                LIMIT 100
                """.trimIndent()
            )
            cursor.use {
                val idCol = it.getColumnIndex("id")
                val pathCol = it.getColumnIndex("filePath")
                val uriCol = it.getColumnIndex("uriString")
                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val path = if (pathCol >= 0) it.getString(pathCol) else ""
                    val uri = if (uriCol >= 0) it.getString(uriCol) else ""
                    val target = if (path.isNotEmpty()) path else uri
                    list.add(id to target)
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun recordPhotoSphereClassification(
        db: GalleryDatabase,
        mediaId: Long,
        isSphere: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val rawDb = db.openHelper.writableDatabase
            val cv = ContentValues().apply {
                put("mediaId", mediaId)
                put("isSphere", if (isSphere) 1 else 0)
            }
            rawDb.insert("photo_spheres", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
        } catch (_: Exception) {}
    }

    /**
     * Insert or replace an FTS5 row for a given media item.
     * Uses openHelper directly since the table is not in the @Database schema.
     * Must be called from a coroutine on Dispatchers.IO.
     */
    suspend fun insertFtsRow(
        db: GalleryDatabase,
        mediaId: Long,
        extractedText: String,
        generatedTags: String
    ) = withContext(Dispatchers.IO) {
        val rawDb = db.openHelper.writableDatabase
        // Delete existing row first (FTS5 UPDATE is an INSERT + DELETE)
        rawDb.execSQL("DELETE FROM image_fts WHERE mediaId = ?", arrayOf(mediaId.toString()))
        val cv = ContentValues().apply {
            put("mediaId", mediaId.toString())
            put("extractedText", extractedText)
            put("generatedTags", generatedTags)
        }
        rawDb.insert("image_fts", 0, cv)
    }

    suspend fun deleteFtsRows(
        db: GalleryDatabase,
        mediaIds: List<Long>
    ) = withContext(Dispatchers.IO) {
        if (mediaIds.isEmpty()) return@withContext
        val rawDb = db.openHelper.writableDatabase
        val placeholders = mediaIds.joinToString(",") { "?" }
        rawDb.execSQL("DELETE FROM image_fts WHERE mediaId IN ($placeholders)", mediaIds.map { it.toString() }.toTypedArray())
    }

    private val monthMap = mapOf(
        "january" to "01", "jan" to "01",
        "february" to "02", "feb" to "02",
        "march" to "03", "mar" to "03",
        "april" to "04", "apr" to "04",
        "may" to "05",
        "june" to "06", "jun" to "06",
        "july" to "07", "jul" to "07",
        "august" to "08", "aug" to "08",
        "september" to "09", "sep" to "09", "sept" to "09",
        "october" to "10", "oct" to "10",
        "november" to "11", "nov" to "11",
        "december" to "12", "dec" to "12"
    )

    /**
     * Unified media search — queries core_media by:
     * 1. Filename (exact, prefix, and substring)
     * 2. File type / extensions (e.g. video, gif, png, raw, mp4, etc.)
     * 3. Folder / Album name (bucketName)
     * 4. Date (year, month name, e.g. "2024", "October", "July 2024")
     * 5. Geocoded locations (placeName)
     * 6. OCR full-text search (image_fts)
     *
     * Orders results by relevance (exact/filename matches first, followed by
     * album/type/date matches, followed by OCR text matches) with newest items first.
     */
    suspend fun searchMedia(
        db: GalleryDatabase,
        query: String,
        excluded: Set<String> = emptySet()
    ): List<SearchMediaResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val rawDb = db.openHelper.readableDatabase

        // 1. Perform metadata search on core_media
        val metadataResults = searchMetadata(rawDb, cleanQuery, excluded)

        // 2. Perform OCR text search on image_fts
        val ftsResults = searchFtsInternal(rawDb, cleanQuery, excluded)

        // 3. Score and merge results
        val tokens = cleanQuery.split(Regex("[\\s_\\-\\.]+")).filter { it.isNotBlank() }
        val ftsMap = ftsResults.associate { it.first.id to it.second }.toMutableMap()

        // Check if any metadata candidates also have matching OCR text in image_fts
        // that FTS prefix search might have missed (e.g. substrings, symbols, multi-token variations)
        val missingFtsCandidates = metadataResults.filterNot { it.id in ftsMap.keys }
        if (missingFtsCandidates.isNotEmpty()) {
            val candidateChunks = missingFtsCandidates.chunked(100)
            for (chunk in candidateChunks) {
                val placeholders = chunk.joinToString(",") { "?" }
                val checkSql = "SELECT mediaId, extractedText FROM image_fts WHERE mediaId IN ($placeholders)"
                try {
                    rawDb.query(checkSql, chunk.map { it.id.toString() }.toTypedArray()).use { c ->
                        val mIdCol = c.getColumnIndex("mediaId")
                        val textCol = c.getColumnIndex("extractedText")
                        while (c.moveToNext()) {
                            val mId = if (mIdCol >= 0) c.getString(mIdCol)?.toLongOrNull() else null
                            val text = if (textCol >= 0 && !c.isNull(textCol)) c.getString(textCol) else null
                            if (mId != null && !text.isNullOrBlank()) {
                                val containsDirect = text.contains(cleanQuery, ignoreCase = true)
                                val containsToken = tokens.any { t -> t.length >= 2 && text.contains(t, ignoreCase = true) }
                                if (containsDirect || containsToken) {
                                    ftsMap[mId] = text
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DatabaseProvider", "Failed to check candidate OCR text: ${e.message}", e)
                }
            }
        }

        val ftsIds = ftsMap.keys

        val scoredMetadata = metadataResults.map { entity ->
            val isAlsoFts = entity.id in ftsIds
            val score = calculateRelevanceScore(entity, cleanQuery, tokens) + (if (isAlsoFts) 50 else 0)
            entity to score
        }

        val existingIds = metadataResults.map { it.id }.toSet()
        val nonOverlappingFts = ftsResults.filter { it.first.id !in existingIds }.map { (entity, _) ->
            entity to 20 // Base score for OCR-only matches
        }

        val combinedEntities = (scoredMetadata + nonOverlappingFts)
            .sortedWith(
                compareByDescending<Pair<CoreMediaEntity, Int>> { it.second }
                    .thenByDescending { it.first.dateAdded }
            )
            .map { it.first }

        // Find geocoded locations
        val locationMap = mutableMapOf<Long, String>()
        try {
            val locCursor = rawDb.query(
                "SELECT mediaId, placeName FROM geocoded_locations WHERE placeName LIKE ?",
                arrayOf("%$cleanQuery%")
            )
            locCursor.use { c ->
                val mIdIdx = c.getColumnIndex("mediaId")
                val pNameIdx = c.getColumnIndex("placeName")
                while (c.moveToNext()) {
                    if (mIdIdx >= 0 && pNameIdx >= 0 && !c.isNull(pNameIdx)) {
                        locationMap[c.getLong(mIdIdx)] = c.getString(pNameIdx)
                    }
                }
            }
        } catch (_: Exception) {}

        combinedEntities.map { entity ->
            classifySearchResult(entity, cleanQuery, ftsMap, locationMap)
        }
    }

    private fun searchMetadata(
        rawDb: SupportSQLiteDatabase,
        query: String,
        excluded: Set<String>
    ): List<CoreMediaEntity> {
        val cleanQuery = query.trim()
        val lower = cleanQuery.lowercase()
        val tokens = cleanQuery.split(Regex("[\\s_\\-\\.]+")).filter { it.isNotBlank() }

        val baseConditions = mutableListOf<String>()
        val baseArgs = mutableListOf<String>()

        baseConditions.add("cm.bucketName != 'Trash'")
        baseConditions.add("cm.uriString NOT IN (SELECT originalUri FROM vault_media)")
        baseConditions.add("cm.filePath NOT IN (SELECT originalPath FROM vault_media)")

        if (excluded.isNotEmpty()) {
            val placeholders = excluded.joinToString(",") { "?" }
            baseConditions.add("cm.bucketName NOT IN ($placeholders)")
            baseArgs.addAll(excluded)
        }

        val matchClauses = mutableListOf<String>()
        val matchArgs = mutableListOf<String>()

        // 1. Full query substring match on filename, path, or album
        matchClauses.add("(cm.name LIKE ? OR cm.filePath LIKE ? OR cm.bucketName LIKE ?)")
        matchArgs.add("%$cleanQuery%")
        matchArgs.add("%$cleanQuery%")
        matchArgs.add("%$cleanQuery%")

        // 2. Geocoded location match
        matchClauses.add("cm.id IN (SELECT mediaId FROM geocoded_locations WHERE placeName LIKE ?)")
        matchArgs.add("%$cleanQuery%")

        // 3. File type / media type / extension matching
        when {
            lower in setOf("video", "videos", "movie", "movies", "clip", "clips", "vid", "vids", "recording", "recordings") -> {
                matchClauses.add("cm.isVideo = 1")
            }
            lower in setOf("photo", "photos", "image", "images", "picture", "pictures", "pic", "pics") -> {
                matchClauses.add("cm.isVideo = 0")
            }
            lower in setOf("gif", "gifs", "animation", "animations") -> {
                matchClauses.add("(cm.mimeType = 'image/gif' OR cm.name LIKE '%.gif')")
            }
            lower in setOf("raw", "dng") -> {
                matchClauses.add("(cm.mimeType LIKE '%raw%' OR cm.mimeType LIKE '%dng%' OR cm.name LIKE '%.dng' OR cm.name LIKE '%.raw' OR cm.name LIKE '%.cr2' OR cm.name LIKE '%.nef' OR cm.name LIKE '%.arw' OR cm.name LIKE '%.orf' OR cm.name LIKE '%.raf')")
            }
            lower in setOf("panorama", "panoramas", "pano") -> {
                matchClauses.add("((cm.name LIKE '%PANO%' OR cm.name LIKE '%PANORAMA%' OR cm.filePath LIKE '%PANO%' OR cm.filePath LIKE '%panorama%') AND cm.isVideo = 0)")
            }
            lower in setOf("slowmo", "slow-mo", "slow motion", "slow_motion") -> {
                matchClauses.add("((cm.name LIKE '%SLOW%' OR cm.name LIKE '%HFR%' OR cm.filePath LIKE '%slow_motion%') AND cm.isVideo = 1)")
            }
            lower in setOf("screenshot", "screenshots") -> {
                matchClauses.add("(cm.name LIKE '%Screenshot%' OR cm.bucketName LIKE '%Screenshot%' OR cm.filePath LIKE '%Screenshot%')")
            }
            lower.removePrefix(".") in setOf("jpg", "jpeg", "png", "webp", "mp4", "mkv", "mov", "avi", "heic", "heif", "3gp") -> {
                val ext = lower.removePrefix(".")
                matchClauses.add("(cm.name LIKE '%.' || ? OR cm.mimeType LIKE '%' || ? || '%')")
                matchArgs.add(ext)
                matchArgs.add(ext)
            }
        }

        // 4. Date matching (Year / Month)
        val yearMatch = Regex("""\b(19\d\d|20\d\d)\b""").find(lower)?.value
        val monthMatch = monthMap.entries.firstOrNull { Regex("""\b${it.key}\b""").containsMatchIn(lower) }?.value

        if (yearMatch != null && monthMatch != null) {
            matchClauses.add("(strftime('%Y', datetime(cm.dateAdded, 'unixepoch', 'localtime')) = ? AND strftime('%m', datetime(cm.dateAdded, 'unixepoch', 'localtime')) = ?)")
            matchArgs.add(yearMatch)
            matchArgs.add(monthMatch)
        } else if (yearMatch != null) {
            matchClauses.add("(strftime('%Y', datetime(cm.dateAdded, 'unixepoch', 'localtime')) = ? OR cm.name LIKE ?)")
            matchArgs.add(yearMatch)
            matchArgs.add("%$yearMatch%")
        } else if (monthMatch != null) {
            matchClauses.add("strftime('%m', datetime(cm.dateAdded, 'unixepoch', 'localtime')) = ?")
            matchArgs.add(monthMatch)
        }

        // 5. Multi-token combination: ALL tokens must be satisfied
        if (tokens.size > 1) {
            val tokenClauses = mutableListOf<String>()
            val tokenArgs = mutableListOf<String>()
            for (token in tokens) {
                val tLower = token.lowercase()
                when {
                    tLower in setOf("video", "videos", "movie", "movies", "clip", "clips", "vid", "vids") -> {
                        tokenClauses.add("cm.isVideo = 1")
                    }
                    tLower in setOf("photo", "photos", "image", "images", "picture", "pictures", "pic", "pics") -> {
                        tokenClauses.add("cm.isVideo = 0")
                    }
                    tLower in setOf("gif", "gifs", "animation") -> {
                        tokenClauses.add("(cm.mimeType = 'image/gif' OR cm.name LIKE '%.gif')")
                    }
                    tLower in setOf("raw", "dng") -> {
                        tokenClauses.add("(cm.mimeType LIKE '%raw%' OR cm.mimeType LIKE '%dng%' OR cm.name LIKE '%.dng' OR cm.name LIKE '%.raw')")
                    }
                    tLower in setOf("screenshot", "screenshots") -> {
                        tokenClauses.add("(cm.name LIKE '%Screenshot%' OR cm.bucketName LIKE '%Screenshot%' OR cm.filePath LIKE '%Screenshot%')")
                    }
                    tLower.removePrefix(".") in setOf("jpg", "jpeg", "png", "webp", "mp4", "mkv", "mov", "avi", "heic", "heif") -> {
                        val ext = tLower.removePrefix(".")
                        tokenClauses.add("(cm.name LIKE '%.' || ? OR cm.mimeType LIKE '%' || ? || '%')")
                        tokenArgs.add(ext)
                        tokenArgs.add(ext)
                    }
                    Regex("""\b(19\d\d|20\d\d)\b""").matches(tLower) -> {
                        tokenClauses.add("(strftime('%Y', datetime(cm.dateAdded, 'unixepoch', 'localtime')) = ? OR cm.name LIKE ?)")
                        tokenArgs.add(tLower)
                        tokenArgs.add("%$tLower%")
                    }
                    monthMap.containsKey(tLower) -> {
                        tokenClauses.add("strftime('%m', datetime(cm.dateAdded, 'unixepoch', 'localtime')) = ?")
                        tokenArgs.add(monthMap[tLower]!!)
                    }
                    else -> {
                        tokenClauses.add("(cm.name LIKE ? OR cm.filePath LIKE ? OR cm.bucketName LIKE ? OR cm.id IN (SELECT mediaId FROM geocoded_locations WHERE placeName LIKE ?))")
                        tokenArgs.add("%$token%")
                        tokenArgs.add("%$token%")
                        tokenArgs.add("%$token%")
                        tokenArgs.add("%$token%")
                    }
                }
            }
            if (tokenClauses.isNotEmpty()) {
                matchClauses.add("(${tokenClauses.joinToString(" AND ")})")
                matchArgs.addAll(tokenArgs)
            }
        }

        val allWhere = (baseConditions + "(${matchClauses.joinToString(" OR ")})").joinToString(" AND ")
        val sql = "SELECT cm.* FROM core_media cm WHERE $allWhere ORDER BY cm.dateAdded DESC LIMIT 1000"
        val finalArgs = (baseArgs + matchArgs).toTypedArray()

        return try {
            val cursor: Cursor = rawDb.query(sql, finalArgs)
            val results = mutableListOf<CoreMediaEntity>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    results += parseCoreMediaEntity(c)
                }
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateRelevanceScore(
        entity: CoreMediaEntity,
        query: String,
        tokens: List<String>
    ): Int {
        val q = query.trim()
        val name = entity.name
        val bucket = entity.bucketName
        val nameWithoutExt = name.substringBeforeLast(".")

        if (name.equals(q, ignoreCase = true) || nameWithoutExt.equals(q, ignoreCase = true)) {
            return 1000
        }
        if (name.startsWith(q, ignoreCase = true)) {
            return 850
        }
        if (name.contains(q, ignoreCase = true)) {
            return 750
        }
        if (tokens.isNotEmpty() && tokens.all { name.contains(it, ignoreCase = true) }) {
            return 650
        }
        if (bucket.equals(q, ignoreCase = true)) {
            return 550
        }
        if (bucket.contains(q, ignoreCase = true)) {
            return 450
        }
        if (tokens.any { name.contains(it, ignoreCase = true) }) {
            return 350
        }
        return 200
    }

    private fun searchFtsInternal(
        rawDb: SupportSQLiteDatabase,
        query: String,
        excluded: Set<String>
    ): List<Pair<CoreMediaEntity, String?>> {
        val ftsKeywords = setOf("AND", "OR", "NOT", "NEAR")
        val cleanQuery = query.trim()
        val terms = cleanQuery.split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.isNotBlank() && it.uppercase() !in ftsKeywords }

        val excludedClause = if (excluded.isNotEmpty()) {
            val placeholders = excluded.joinToString(",") { "?" }
            " AND cm.bucketName NOT IN ($placeholders)"
        } else ""

        val resultsMap = LinkedHashMap<Long, Pair<CoreMediaEntity, String?>>()

        fun executeFtsQuery(ftsExpr: String) {
            val sql = """
                SELECT cm.*, fts.extractedText AS ocrText FROM core_media cm
                INNER JOIN image_fts fts ON CAST(cm.id AS TEXT) = fts.mediaId
                WHERE image_fts MATCH ?
                  AND cm.bucketName != 'Trash'
                  AND cm.uriString NOT IN (SELECT originalUri FROM vault_media)
                  AND cm.filePath NOT IN (SELECT originalPath FROM vault_media)
                  $excludedClause
                ORDER BY cm.dateAdded DESC
                LIMIT 500
            """.trimIndent()
            val args = (listOf(ftsExpr) + excluded).toTypedArray()
            try {
                rawDb.query(sql, args).use { c ->
                    val ocrIdx = c.getColumnIndex("ocrText")
                    while (c.moveToNext()) {
                        val entity = parseCoreMediaEntity(c)
                        if (!resultsMap.containsKey(entity.id)) {
                            val ocrText = if (ocrIdx >= 0 && !c.isNull(ocrIdx)) c.getString(ocrIdx) else null
                            resultsMap[entity.id] = entity to ocrText
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseProvider", "executeFtsQuery failed for '$ftsExpr': ${e.message}", e)
            }
        }

        // 1. Run FTS prefix search with AND (all terms match prefix)
        if (terms.isNotEmpty()) {
            val andQuery = terms.map { "$it*" }.joinToString(" AND ")
            executeFtsQuery(andQuery)

            // If multi-term and we have few results, also run OR to catch photos where some terms match in OCR
            if (terms.size > 1 && resultsMap.size < 50) {
                val orQuery = terms.map { "$it*" }.joinToString(" OR ")
                executeFtsQuery(orQuery)
            }
        }

        // 2. Substring / literal LIKE check directly on fts.extractedText
        // This catches symbols, numbers, punctuation phrases (e.g. "$50", "Wi-Fi", "01/02"),
        // and substrings inside longer words that FTS prefix search misses
        if (cleanQuery.length >= 2) {
            val likeSql = """
                SELECT cm.*, fts.extractedText AS ocrText FROM core_media cm
                INNER JOIN image_fts fts ON CAST(cm.id AS TEXT) = fts.mediaId
                WHERE fts.extractedText LIKE ?
                  AND cm.bucketName != 'Trash'
                  AND cm.uriString NOT IN (SELECT originalUri FROM vault_media)
                  AND cm.filePath NOT IN (SELECT originalPath FROM vault_media)
                  $excludedClause
                ORDER BY cm.dateAdded DESC
                LIMIT 500
            """.trimIndent()
            val likeArgs = (listOf("%$cleanQuery%") + excluded).toTypedArray()
            try {
                rawDb.query(likeSql, likeArgs).use { c ->
                    val ocrIdx = c.getColumnIndex("ocrText")
                    while (c.moveToNext()) {
                        val entity = parseCoreMediaEntity(c)
                        if (!resultsMap.containsKey(entity.id)) {
                            val ocrText = if (ocrIdx >= 0 && !c.isNull(ocrIdx)) c.getString(ocrIdx) else null
                            resultsMap[entity.id] = entity to ocrText
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseProvider", "searchFtsInternal LIKE query failed: ${e.message}", e)
            }
        }

        return resultsMap.values.toList()
    }

    fun extractOcrSnippet(text: String, query: String): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        val index = clean.indexOf(query, ignoreCase = true)
        if (index >= 0) {
            val start = (index - 18).coerceAtLeast(0)
            val end = (index + query.length + 18).coerceAtMost(clean.length)
            val prefix = if (start > 0) "…" else ""
            val suffix = if (end < clean.length) "…" else ""
            return prefix + clean.substring(start, end).trim() + suffix
        }
        val tokens = query.split(Regex("[\\s_\\-\\.]+")).filter { it.isNotBlank() }
        for (t in tokens) {
            val tIndex = clean.indexOf(t, ignoreCase = true)
            if (tIndex >= 0) {
                val start = (tIndex - 18).coerceAtLeast(0)
                val end = (tIndex + t.length + 18).coerceAtMost(clean.length)
                val prefix = if (start > 0) "…" else ""
                val suffix = if (end < clean.length) "…" else ""
                return prefix + clean.substring(start, end).trim() + suffix
            }
        }
        return clean.take(40) + if (clean.length > 40) "…" else ""
    }

    private fun classifySearchResult(
        entity: CoreMediaEntity,
        cleanQuery: String,
        ftsMap: Map<Long, String?>,
        locationMap: Map<Long, String>
    ): SearchMediaResult {
        val lower = cleanQuery.lowercase()
        val name = entity.name
        val lowerName = name.lowercase()
        val ext = name.substringAfterLast(".", "").lowercase()
        val nameWithoutExt = name.substringBeforeLast(".")

        // 1. FORMAT / EXTENSION MATCH
        val isFormatQuery = when {
            lower in setOf("video", "videos", "movie", "movies", "clip", "clips", "vid", "vids", "recording", "recordings") -> true
            lower in setOf("photo", "photos", "image", "images", "picture", "pictures", "pic", "pics") -> true
            lower in setOf("gif", "gifs", "animation", "animations") -> true
            lower in setOf("raw", "dng", "cr2", "nef", "arw", "orf", "raf") -> true
            lower in setOf("panorama", "panoramas", "pano") -> true
            lower in setOf("slowmo", "slow-mo", "slow motion", "slow_motion") -> true
            lower in setOf("screenshot", "screenshots") -> true
            lower.removePrefix(".") in setOf("jpg", "jpeg", "png", "webp", "mp4", "mkv", "mov", "avi", "heic", "heif", "3gp") -> true
            else -> false
        }

        if (isFormatQuery) {
            val formatLabel = when {
                lower in setOf("video", "videos", "movie", "movies", "clip", "clips", "vid", "vids") -> "Video"
                lower in setOf("photo", "photos", "image", "images", "picture", "pictures", "pic", "pics") -> "Photo"
                lower in setOf("gif", "gifs", "animation") -> "GIF"
                lower in setOf("raw", "dng", "cr2", "nef", "arw", "orf", "raf") -> "RAW"
                lower in setOf("panorama", "panoramas", "pano") -> "Panorama"
                lower in setOf("slowmo", "slow-mo", "slow motion") -> "Slow-mo"
                lower in setOf("screenshot", "screenshots") -> "Screenshot"
                else -> ext.uppercase()
            }
            return SearchMediaResult(entity, SearchMatchType.FORMAT, formatLabel)
        }

        // 2. OCR MATCH (Prioritize recognized text when matched via FTS or OCR text)
        if (entity.id in ftsMap) {
            val ocrText = ftsMap[entity.id]
            val snippet = if (!ocrText.isNullOrBlank()) extractOcrSnippet(ocrText, cleanQuery) else "Recognized text"
            return SearchMediaResult(entity, SearchMatchType.OCR, snippet)
        }

        // 3. FILE NAME MATCH
        if (nameWithoutExt.contains(cleanQuery, ignoreCase = true) || name.contains(cleanQuery, ignoreCase = true)) {
            return SearchMediaResult(entity, SearchMatchType.FILE_NAME, name)
        }

        // 4. ALBUM / FOLDER MATCH
        if (entity.bucketName.contains(cleanQuery, ignoreCase = true)) {
            return SearchMediaResult(entity, SearchMatchType.ALBUM, entity.bucketName)
        }

        // 5. LOCATION MATCH
        if (entity.id in locationMap) {
            return SearchMediaResult(entity, SearchMatchType.LOCATION, locationMap[entity.id])
        }

        // 6. DATE MATCH
        val yearMatch = Regex("""\b(19\d\d|20\d\d)\b""").find(lower)?.value
        val monthMatch = monthMap.entries.firstOrNull { Regex("""\b${it.key}\b""").containsMatchIn(lower) }?.key
        if (yearMatch != null || monthMatch != null) {
            val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date(entity.dateAdded * 1000L))
            return SearchMediaResult(entity, SearchMatchType.DATE, dateStr)
        }

        // 7. Token-based fallback check:
        val tokens = cleanQuery.split(Regex("[\\s_\\-\\.]+")).filter { it.isNotBlank() }
        if (tokens.any { lowerName.contains(it.lowercase()) }) {
            return SearchMediaResult(entity, SearchMatchType.FILE_NAME, name)
        }
        if (tokens.any { entity.bucketName.contains(it, ignoreCase = true) }) {
            return SearchMediaResult(entity, SearchMatchType.ALBUM, entity.bucketName)
        }
        if (entity.filePath.contains(cleanQuery, ignoreCase = true) || tokens.any { entity.filePath.contains(it, ignoreCase = true) }) {
            return SearchMediaResult(entity, SearchMatchType.ALBUM, entity.bucketName)
        }

        // Fallback
        return SearchMediaResult(entity, SearchMatchType.FILE_NAME, name)
    }

    private fun parseCoreMediaEntity(c: Cursor): CoreMediaEntity {
        val idIdx            = c.getColumnIndexOrThrow("id")
        val uriIdx           = c.getColumnIndexOrThrow("uriString")
        val fileIdx          = c.getColumnIndexOrThrow("filePath")
        val bucketIdx        = c.getColumnIndexOrThrow("bucketName")
        val addedIdx         = c.getColumnIndexOrThrow("dateAdded")
        val modIdx           = c.getColumnIndexOrThrow("dateModified")
        val sizeIdx          = c.getColumnIndexOrThrow("size")
        val nameIdx          = c.getColumnIndexOrThrow("name")
        val mimeIdx          = c.getColumnIndexOrThrow("mimeType")
        val videoIdx         = c.getColumnIndexOrThrow("isVideo")
        val durIdx           = c.getColumnIndexOrThrow("durationMs")
        val ocrIdx           = c.getColumnIndex("is_indexed_ocr")
        val latIdx           = c.getColumnIndex("latitude")
        val lonIdx           = c.getColumnIndex("longitude")
        val hashIdx          = c.getColumnIndex("fileHash")
        return CoreMediaEntity(
            id              = c.getLong(idIdx),
            uriString       = c.getString(uriIdx),
            filePath        = c.getString(fileIdx),
            bucketName      = c.getString(bucketIdx),
            dateAdded       = c.getLong(addedIdx),
            dateModified    = c.getLong(modIdx),
            size            = c.getLong(sizeIdx),
            name            = c.getString(nameIdx),
            mimeType        = if (c.isNull(mimeIdx)) null else c.getString(mimeIdx),
            isVideo         = c.getInt(videoIdx) != 0,
            durationMs      = if (c.isNull(durIdx)) null else c.getLong(durIdx),
            isIndexedOcr    = if (ocrIdx >= 0) c.getInt(ocrIdx) != 0 else false,
            latitude        = if (latIdx >= 0 && !c.isNull(latIdx)) c.getDouble(latIdx) else null,
            longitude       = if (lonIdx >= 0 && !c.isNull(lonIdx)) c.getDouble(lonIdx) else null,
            fileHash        = if (hashIdx >= 0 && !c.isNull(hashIdx)) c.getString(hashIdx) else null,
        )
    }

    /**
     * Legacy FTS5 full-text search wrapper.
     * Maintained for backward compatibility.
     */
    suspend fun searchFts(
        db: GalleryDatabase,
        query: String,
        excluded: Set<String> = emptySet()
    ): List<CoreMediaEntity> = withContext(Dispatchers.IO) {
        val rawDb = db.openHelper.readableDatabase
        searchFtsInternal(rawDb, query, excluded).map { it.first }
    }
}
