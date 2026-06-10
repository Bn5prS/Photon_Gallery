package com.inferno.gallery.data.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramClient(private val botToken: String, private val chatId: String) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Uploads a file to Telegram as a document to preserve original quality.
     * Returns an UploadResult containing file_id, thumb_file_id, and message_id.
     */
    @Throws(IOException::class, RateLimitException::class)
    fun uploadDocument(file: File, mimeType: String): UploadResult {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart(
                "document",
                file.name,
                file.asRequestBody(mimeType.toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendDocument")
            .header("User-Agent", "PhotonGalleryApp/1.0 (Android; Jetpack Compose)")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toIntOrNull() ?: 10
                throw RateLimitException(retryAfterSeconds, "Rate limit hit on upload")
            }
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val json = try { JSONObject(bodyString) } catch (e: Exception) { null }
                val description = json?.optString("description") ?: ""
                val migrateToChatId = json?.optJSONObject("parameters")?.optLong("migrate_to_chat_id", 0L) ?: 0L
                if (migrateToChatId != 0L) {
                    throw TelegramMigrationException(migrateToChatId, "group chat was upgraded to a supergroup chat")
                }
                val detail = if (description.isNotBlank()) ": $description" else ""
                throw TelegramApiException(response.code, description, "Telegram upload failed with HTTP code ${response.code}$detail")
            }
            val json = JSONObject(bodyString)
            if (!json.getBoolean("ok")) {
                throw IOException("Telegram API error: ${json.optString("description")}")
            }
            
            val result = json.getJSONObject("result")
            val messageId = result.getLong("message_id")
            val document = result.getJSONObject("document")
            val fileId = document.getString("file_id")
            
            // Try to extract thumbnail file_id. In Telegram Bot API, the field is "thumbnail" or "thumb"
            val thumbObject = document.optJSONObject("thumbnail") ?: document.optJSONObject("thumb")
            val thumbFileId = thumbObject?.optString("file_id") ?: fileId

            return UploadResult(fileId, thumbFileId, messageId)
        }
    }

    /**
     * Obtains the direct temporary download URL from Telegram for a given file_id.
     */
    @Throws(IOException::class, RateLimitException::class)
    fun getFileUrl(fileId: String): String {
        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/getFile?file_id=$fileId")
            .header("User-Agent", "PhotonGalleryApp/1.0 (Android; Jetpack Compose)")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toIntOrNull() ?: 10
                throw RateLimitException(retryAfterSeconds, "Rate limit hit on getFile")
            }
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val json = try { JSONObject(bodyString) } catch (e: Exception) { null }
                val description = json?.optString("description") ?: ""
                val migrateToChatId = json?.optJSONObject("parameters")?.optLong("migrate_to_chat_id", 0L) ?: 0L
                if (migrateToChatId != 0L) {
                    throw TelegramMigrationException(migrateToChatId, "group chat was upgraded to a supergroup chat")
                }
                val detail = if (description.isNotBlank()) ": $description" else ""
                throw TelegramApiException(response.code, description, "Telegram getFile failed with HTTP code ${response.code}$detail")
            }
            val json = JSONObject(bodyString)
            if (!json.getBoolean("ok")) {
                throw IOException("Telegram API error: ${json.optString("description")}")
            }
            
            val result = json.getJSONObject("result")
            val filePath = result.getString("file_path")
            return "https://api.telegram.org/file/bot$botToken/$filePath"
        }
    }

    /**
     * Checks if a message exists in the Telegram chat using the forwardMessage trick.
     */
    @Throws(IOException::class, RateLimitException::class)
    fun checkMessageExists(messageId: Long): Boolean {
        // chat_id = 999999999 (invalid dummy target chat)
        val url = "https://api.telegram.org/bot$botToken/forwardMessage?chat_id=999999999&from_chat_id=$chatId&message_id=$messageId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PhotonGalleryApp/1.0 (Android; Jetpack Compose)")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toIntOrNull() ?: 10
                throw RateLimitException(retryAfterSeconds, "Rate limit hit on checkMessageExists")
            }
            val bodyString = response.body?.string() ?: ""
            val json = try { JSONObject(bodyString) } catch (e: Exception) { null }
            val description = json?.optString("description") ?: ""
            val migrateToChatId = json?.optJSONObject("parameters")?.optLong("migrate_to_chat_id", 0L) ?: 0L
            if (migrateToChatId != 0L) {
                throw TelegramMigrationException(migrateToChatId, "group chat was upgraded to a supergroup chat")
            }

            if (response.isSuccessful) {
                return true
            }

            if (description.contains("message to forward not found", ignoreCase = true)) {
                return false
            }
            if (description.contains("chat not found", ignoreCase = true)) {
                return true
            }

            val detail = if (description.isNotBlank()) ": $description" else ""
            throw TelegramApiException(response.code, description, "checkMessageExists failed with HTTP code ${response.code}$detail")
        }
    }

    /**
     * Deletes a message from the Telegram chat.
     */
    @Throws(IOException::class)
    fun deleteMessage(messageId: Long): Boolean {
        val url = "https://api.telegram.org/bot$botToken/deleteMessage?chat_id=$chatId&message_id=$messageId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PhotonGalleryApp/1.0 (Android; Jetpack Compose)")
            .build()
            
        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: ""
            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toIntOrNull() ?: 10
                throw RateLimitException(retryAfterSeconds, "Rate limit hit on deleteMessage")
            }
            if (!response.isSuccessful) {
                val json = try { JSONObject(bodyString) } catch (e: Exception) { null }
                val description = json?.optString("description") ?: ""
                val migrateToChatId = json?.optJSONObject("parameters")?.optLong("migrate_to_chat_id", 0L) ?: 0L
                if (migrateToChatId != 0L) {
                    throw TelegramMigrationException(migrateToChatId, "group chat was upgraded to a supergroup chat")
                }
                return false
            }
            val json = JSONObject(bodyString)
            return json.optBoolean("ok", false) && json.optBoolean("result", false)
        }
    }

    /**
     * Pins a message in the Telegram chat.
     */
    @Throws(IOException::class)
    fun pinChatMessage(messageId: Long): Boolean {
        val url = "https://api.telegram.org/bot$botToken/pinChatMessage?chat_id=$chatId&message_id=$messageId&disable_notification=true"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PhotonGalleryApp/1.0 (Android; Jetpack Compose)")
            .build()
            
        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: ""
            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toIntOrNull() ?: 10
                throw RateLimitException(retryAfterSeconds, "Rate limit hit on pinChatMessage")
            }
            if (!response.isSuccessful) {
                val json = try { JSONObject(bodyString) } catch (e: Exception) { null }
                val description = json?.optString("description") ?: ""
                val migrateToChatId = json?.optJSONObject("parameters")?.optLong("migrate_to_chat_id", 0L) ?: 0L
                if (migrateToChatId != 0L) {
                    throw TelegramMigrationException(migrateToChatId, "group chat was upgraded to a supergroup chat")
                }
                val detail = if (description.isNotBlank()) ": $description" else ""
                throw TelegramApiException(response.code, description, "pinChatMessage failed with HTTP code ${response.code}$detail")
            }
            val json = JSONObject(bodyString)
            return json.optBoolean("ok", false)
        }
    }

    /**
     * Unpins a message in the Telegram chat.
     */
    @Throws(IOException::class)
    fun unpinChatMessage(messageId: Long): Boolean {
        val url = "https://api.telegram.org/bot$botToken/unpinChatMessage?chat_id=$chatId&message_id=$messageId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PhotonGalleryApp/1.0 (Android; Jetpack Compose)")
            .build()
            
        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: ""
            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toIntOrNull() ?: 10
                throw RateLimitException(retryAfterSeconds, "Rate limit hit on unpinChatMessage")
            }
            if (!response.isSuccessful) {
                val json = try { JSONObject(bodyString) } catch (e: Exception) { null }
                val description = json?.optString("description") ?: ""
                val migrateToChatId = json?.optJSONObject("parameters")?.optLong("migrate_to_chat_id", 0L) ?: 0L
                if (migrateToChatId != 0L) {
                    throw TelegramMigrationException(migrateToChatId, "group chat was upgraded to a supergroup chat")
                }
                val detail = if (description.isNotBlank()) ": $description" else ""
                throw TelegramApiException(response.code, description, "unpinChatMessage failed with HTTP code ${response.code}$detail")
            }
            val json = JSONObject(bodyString)
            return json.optBoolean("ok", false)
        }
    }

    /**
     * Gets the current pinned message ID in the Telegram chat, if any.
     */
    @Throws(IOException::class)
    fun getPinnedMessageIdOrNull(): Long? {
        val url = "https://api.telegram.org/bot$botToken/getChat?chat_id=$chatId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PhotonGalleryApp/1.0 (Android; Jetpack Compose)")
            .build()
            
        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: ""
            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toIntOrNull() ?: 10
                throw RateLimitException(retryAfterSeconds, "Rate limit hit on getChat")
            }
            if (!response.isSuccessful) {
                val json = try { JSONObject(bodyString) } catch (e: Exception) { null }
                val description = json?.optString("description") ?: ""
                val migrateToChatId = json?.optJSONObject("parameters")?.optLong("migrate_to_chat_id", 0L) ?: 0L
                if (migrateToChatId != 0L) {
                    throw TelegramMigrationException(migrateToChatId, "group chat was upgraded to a supergroup chat")
                }
                return null
            }
            val json = JSONObject(bodyString)
            if (!json.getBoolean("ok")) return null
            val result = json.getJSONObject("result")
            val pinnedMessage = result.optJSONObject("pinned_message") ?: return null
            return pinnedMessage.getLong("message_id")
        }
    }

    /**
     * Gets the current pinned manifest file_id in the Telegram chat, if any.
     */
    @Throws(IOException::class)
    fun getPinnedManifestFileIdOrNull(): String? {
        val url = "https://api.telegram.org/bot$botToken/getChat?chat_id=$chatId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PhotonGalleryApp/1.0 (Android; Jetpack Compose)")
            .build()
            
        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: ""
            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toIntOrNull() ?: 10
                throw RateLimitException(retryAfterSeconds, "Rate limit hit on getChat")
            }
            if (!response.isSuccessful) {
                val json = try { JSONObject(bodyString) } catch (e: Exception) { null }
                val description = json?.optString("description") ?: ""
                val migrateToChatId = json?.optJSONObject("parameters")?.optLong("migrate_to_chat_id", 0L) ?: 0L
                if (migrateToChatId != 0L) {
                    throw TelegramMigrationException(migrateToChatId, "group chat was upgraded to a supergroup chat")
                }
                return null
            }
            val json = JSONObject(bodyString)
            if (!json.getBoolean("ok")) return null
            val result = json.getJSONObject("result")
            val pinnedMessage = result.optJSONObject("pinned_message") ?: return null
            val document = pinnedMessage.optJSONObject("document") ?: return null
            val fileName = document.optString("file_name")
            if (fileName == "sync_manifest.json") {
                return document.getString("file_id")
            }
            return null
        }
    }

    /**
     * Downloads the text content of a file from Telegram.
     */
    @Throws(IOException::class)
    fun downloadFileText(fileUrl: String): String {
        val request = Request.Builder()
            .url(fileUrl)
            .header("User-Agent", "PhotonGalleryApp/1.0 (Android; Jetpack Compose)")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toIntOrNull() ?: 10
                throw RateLimitException(retryAfterSeconds, "Rate limit hit on downloadFileText")
            }
            if (!response.isSuccessful) {
                throw IOException("Failed to download file with HTTP code ${response.code}")
            }
            return response.body?.string() ?: ""
        }
    }

    /**
     * Downloads the raw bytes of a file from Telegram to an output stream.
     */
    @Throws(IOException::class)
    fun downloadFileStream(fileUrl: String, outputStream: java.io.OutputStream) {
        val request = Request.Builder()
            .url(fileUrl)
            .header("User-Agent", "PhotonGalleryApp/1.0 (Android; Jetpack Compose)")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (response.code == 429) {
                val retryAfterSeconds = response.header("Retry-After")?.toIntOrNull() ?: 10
                throw RateLimitException(retryAfterSeconds, "Rate limit hit on downloadFileStream")
            }
            if (!response.isSuccessful) {
                throw IOException("Failed to download file with HTTP code ${response.code}")
            }
            response.body?.byteStream()?.use { input ->
                input.copyTo(outputStream)
            }
        }
    }
}

data class UploadResult(val fileId: String, val thumbFileId: String, val messageId: Long)

class TelegramApiException(val code: Int, val description: String, message: String) : IOException(message)

class RateLimitException(val retryAfterSeconds: Int, message: String) : IOException(message)

class TelegramMigrationException(val newChatId: Long, message: String) : IOException(message)
