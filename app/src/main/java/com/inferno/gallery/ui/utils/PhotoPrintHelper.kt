package com.inferno.gallery.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.FileOutputStream

/**
 * Launches the native Android Print dialog for a photo URI using [PrintManager]
 * and [PrintedPdfDocument], scaling the decoded bitmap to fit the printable page.
 */
object PhotoPrintHelper {

    fun printPhoto(context: Context, uri: Uri, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Print service unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = object : PrintDocumentAdapter() {
            private var printAttributes: PrintAttributes? = null
            private var writeJob: Job? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                printAttributes = newAttributes
                val info = PrintDocumentInfo.Builder(jobName.ifBlank { "Photon_Photo" })
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, newAttributes != oldAttributes)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val attrs = printAttributes ?: PrintAttributes.Builder().build()
                writeJob = CoroutineScope(Dispatchers.IO).launch {
                    var pdf: PrintedPdfDocument? = null
                    var bitmap: Bitmap? = null
                    try {
                        if (cancellationSignal?.isCanceled == true) {
                            callback.onWriteCancelled()
                            return@launch
                        }

                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            val maxDim = 2400
                            val w = info.size.width
                            val h = info.size.height
                            if (w > maxDim || h > maxDim) {
                                val scale = maxDim.toFloat() / maxOf(w, h).toFloat()
                                decoder.setTargetSize(
                                    (w * scale).toInt().coerceAtLeast(1),
                                    (h * scale).toInt().coerceAtLeast(1)
                                )
                            }
                        }

                        if (cancellationSignal?.isCanceled == true) {
                            callback.onWriteCancelled()
                            return@launch
                        }

                        pdf = PrintedPdfDocument(context, attrs)
                        val page: PdfDocument.Page = pdf.startPage(0)
                        val contentRect = page.info.contentRect
                        val scale = minOf(
                            contentRect.width().toFloat() / bitmap.width.toFloat(),
                            contentRect.height().toFloat() / bitmap.height.toFloat()
                        )
                        val drawW = bitmap.width * scale
                        val drawH = bitmap.height * scale
                        val dx = contentRect.left + (contentRect.width() - drawW) / 2f
                        val dy = contentRect.top + (contentRect.height() - drawH) / 2f

                        val matrix = Matrix().apply {
                            postScale(scale, scale)
                            postTranslate(dx, dy)
                        }
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                        page.canvas.drawBitmap(bitmap, matrix, paint)
                        pdf.finishPage(page)

                        if (cancellationSignal?.isCanceled == true) {
                            callback.onWriteCancelled()
                            return@launch
                        }

                        FileOutputStream(destination.fileDescriptor).use { out ->
                            pdf.writeTo(out)
                        }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.message ?: "Failed to render photo for print")
                    } finally {
                        bitmap?.recycle()
                        pdf?.close()
                    }
                }
                cancellationSignal?.setOnCancelListener {
                    writeJob?.cancel()
                }
            }

            override fun onFinish() {
                writeJob?.cancel()
            }
        }

        val attributes = PrintAttributes.Builder()
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()
        printManager.print(jobName.ifBlank { "Photon Photo" }, adapter, attributes)
    }
}
