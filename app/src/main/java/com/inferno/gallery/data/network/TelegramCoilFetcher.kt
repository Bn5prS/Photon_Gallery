package com.inferno.gallery.data.network

import android.net.Uri
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import com.inferno.gallery.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * A custom Coil Fetcher that intercepts 'telegram://' URIs.
 * It resolves the dynamic Telegram download URL using the Bot API and delegates
 * the actual HTTP downloading and image decoding back to Coil's standard pipeline.
 */
class TelegramCoilFetcher(
    private val uri: Uri,
    private val options: Options,
    private val settings: SettingsRepository,
    private val imageLoader: ImageLoader
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        try {
            val fileId = uri.host ?: return@withContext null
            val botTokens = settings.telegramBotTokensFlow.first()
            if (botTokens.isEmpty()) return@withContext null
            
            // Resolve the temporary Telegram file URL
            val client = TelegramClient(botTokens.first(), "")
            val fileUrl = client.getFileUrl(fileId)
            
            // Re-submit the URL to Coil to handle standard caching, streaming, and decoding
            val request = ImageRequest.Builder(options.context)
                .data(fileUrl)
                .size(options.size)
                .scale(options.scale)
                .build()
                
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                ImageFetchResult(
                    image = result.image,
                    isSampled = result.isSampled,
                    dataSource = DataSource.NETWORK
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    class Factory(
        private val settings: SettingsRepository
    ) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != "telegram") return null
            return TelegramCoilFetcher(data, options, settings, imageLoader)
        }
    }
}
