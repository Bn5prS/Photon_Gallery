package com.inferno.gallery.data

import android.content.Context
import android.util.Log
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.TelegramBackupEntity
import com.inferno.gallery.data.network.TelegramClient
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SyncManifestManager {
    private const val TAG = "SyncManifestManager"
    private const val MANIFEST_FILE_NAME = "sync_manifest.json"

    suspend fun updateManifest(context: Context, botToken: String, chatId: String) {
        try {
            val db = DatabaseProvider.getDatabase(context)
            val successfulBackups = db.telegramBackupDao().observeAllBackups().first()
                .filter { it.backupStatus == "SUCCESS" }
            
            val jsonArray = JSONArray()
            val allLocalMedia = db.mediaDao().getAllMedia()

            for (backup in successfulBackups) {
                val mediaItem = allLocalMedia.firstOrNull { it.id == backup.mediaId }
                val obj = JSONObject().apply {
                    put("fileId", backup.telegramFileId)
                    put("thumbId", backup.telegramThumbFileId)
                    put("messageId", backup.telegramMessageId ?: JSONObject.NULL)
                    put("timestamp", backup.backupTimestamp)
                    put("size", mediaItem?.size ?: 0L)
                    put("name", mediaItem?.name ?: "")
                    put("filePath", mediaItem?.filePath ?: "")
                }
                jsonArray.put(obj)
            }

            val tempFile = File(context.cacheDir, MANIFEST_FILE_NAME)
            tempFile.writeText(jsonArray.toString(2))

            val client = TelegramClient(botToken, chatId)
            
            // 1. Upload new manifest
            val uploadResult = client.uploadDocument(tempFile, "application/json")
            
            // 2. Fetch the old pinned message ID
            val oldPinnedId = client.getPinnedMessageIdOrNull()
            
            // 3. Pin the new manifest message
            try {
                client.pinChatMessage(uploadResult.messageId)
            } catch (pinEx: Exception) {
                Log.e(TAG, "Failed to pin chat message: ${pinEx.message}")
            }
            
            // 4. Delete the old pinned message (to save space and reduce clutter)
            if (oldPinnedId != null) {
                try {
                    client.deleteMessage(oldPinnedId)
                } catch (delEx: Exception) {
                    Log.e(TAG, "Failed to delete old pinned manifest message $oldPinnedId: ${delEx.message}")
                }
            }
            
            if (tempFile.exists()) tempFile.delete()
            Log.d(TAG, "Sync manifest updated successfully. New pinned message ID: ${uploadResult.messageId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating sync manifest: ${e.message}", e)
        }
    }

    suspend fun restoreFromManifest(context: Context, botToken: String, chatId: String): Boolean {
        try {
            val client = TelegramClient(botToken, chatId)
            val fileId = client.getPinnedManifestFileIdOrNull() ?: return false
            val fileUrl = client.getFileUrl(fileId)
            val manifestText = client.downloadFileText(fileUrl)
            if (manifestText.isBlank()) return false
            
            val jsonArray = JSONArray(manifestText)
            val db = DatabaseProvider.getDatabase(context)
            val localMedia = db.mediaDao().getAllMedia()
            
            var restoreCount = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val fileIdVal = obj.getString("fileId")
                val thumbIdVal = obj.getString("thumbId")
                val messageIdVal = if (obj.isNull("messageId")) null else obj.getLong("messageId")
                val timestampVal = obj.getLong("timestamp")
                val sizeVal = obj.optLong("size", 0L)
                val nameVal = obj.optString("name", "")
                val filePathVal = obj.optString("filePath", "")
                
                // Match priority:
                // 1. Same filePath
                // 2. Same name and size
                var matchedMedia = localMedia.firstOrNull { it.filePath == filePathVal }
                    ?: localMedia.firstOrNull { it.name == nameVal && it.size == sizeVal }
                
                if (matchedMedia == null) {
                    val newMedia = com.inferno.gallery.data.db.CoreMediaEntity(
                        id = timestampVal + i, // Generate unique fake ID
                        uriString = "telegram://$fileIdVal",
                        filePath = filePathVal,
                        bucketName = "telegram_cloud",
                        dateAdded = timestampVal / 1000,
                        dateModified = timestampVal / 1000,
                        size = sizeVal,
                        name = nameVal,
                        isVideo = nameVal.endsWith(".mp4", true) || nameVal.endsWith(".webm", true),
                        durationMs = null,
                        mimeType = if (nameVal.endsWith(".mp4", true) || nameVal.endsWith(".webm", true)) "video/mp4" else "image/jpeg"
                    )
                    db.mediaDao().insertAll(listOf(newMedia))
                    matchedMedia = newMedia
                }

                if (matchedMedia != null) {
                    db.telegramBackupDao().insertOrUpdate(
                        TelegramBackupEntity(
                            mediaId = matchedMedia.id,
                            telegramFileId = fileIdVal,
                            telegramThumbFileId = thumbIdVal,
                            telegramMessageId = messageIdVal,
                            backupStatus = "SUCCESS",
                            backupTimestamp = timestampVal
                        )
                    )
                    restoreCount++
                }
            }
            Log.i(TAG, "Restored $restoreCount backup records from Telegram sync manifest.")
            return restoreCount > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from sync manifest: ${e.message}", e)
            return false
        }
    }
}
