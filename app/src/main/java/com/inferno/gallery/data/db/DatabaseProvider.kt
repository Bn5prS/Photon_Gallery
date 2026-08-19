package com.inferno.gallery.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

            val instance = Room.databaseBuilder(
                context.applicationContext,
                GalleryDatabase::class.java,
                "gallery_database.db"
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21)
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
                }
            })
            .build()
            INSTANCE = instance
            instance
        }
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

    /**
     * FTS5 full-text search — queries image_fts and JOINs with core_media.
     * Returns a list of matching [CoreMediaEntity] ordered by date.
     * Must be called from a coroutine on Dispatchers.IO.
     */
    suspend fun searchFts(
        db: GalleryDatabase,
        query: String
    ): List<CoreMediaEntity> = withContext(Dispatchers.IO) {
        val rawDb = db.openHelper.readableDatabase
        val terms = query.trim().split("\\s+".toRegex())
            .map { it.filter { c -> c.isLetterOrDigit() } }
            .filter { it.isNotBlank() }
        if (terms.isEmpty()) return@withContext emptyList()

        val ftsQuery = terms.map { "$it*" }.joinToString(" AND ")

        val sql = """
            SELECT cm.* FROM core_media cm
            INNER JOIN image_fts fts ON CAST(cm.id AS TEXT) = fts.mediaId
            WHERE image_fts MATCH ?
            ORDER BY cm.dateAdded DESC
        """.trimIndent()

        val cursor: Cursor = rawDb.query(sql, arrayOf(ftsQuery))
        val results = mutableListOf<CoreMediaEntity>()
        cursor.use { c ->
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
            val ocrIdx           = c.getColumnIndexOrThrow("is_indexed_ocr")
            while (c.moveToNext()) {
                results += CoreMediaEntity(
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
                    isIndexedOcr    = c.getInt(ocrIdx) != 0,
                )
            }
        }
        results
    }
}
