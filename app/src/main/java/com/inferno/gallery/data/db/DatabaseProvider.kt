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
            val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE telegram_backups ADD COLUMN telegramMessageId INTEGER DEFAULT NULL")
                }
            }

            val instance = Room.databaseBuilder(
                context.applicationContext,
                GalleryDatabase::class.java,
                "gallery_database.db"
            )
            .addMigrations(MIGRATION_4_5)
            .fallbackToDestructiveMigration(dropAllTables = true)
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
                    // Ensure trigger exists for existing users
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
