package com.brendigo.qrex

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import java.io.IOException
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Integrates DocuScan-style existing-PDF import into QREX's native document
 * scanner. No OpenCV, new UI theme, server, persistence or extra permission.
 * A user explicitly selects a PDF with Android Storage Access Framework.
 */
internal object QREXDocumentPdfImport {
    private const val MAX_PDF_BYTES = 48L * 1024L * 1024L

    /** All pages must fit QREX's existing capped bitmap budget. */
    fun targetSize(width: Int, height: Int): Pair<Int, Int> {
        require(width in 1..50_000 && height in 1..50_000)
        val scale = minOf(2.0, sqrt(QREX_DOCUMENT_MAX_PIXELS.toDouble() /
            (width.toDouble() * height.toDouble())))
        val targetWidth = maxOf(1, floor(width * scale).toInt())
        val targetHeight = maxOf(1, floor(height * scale).toInt())
        require(targetWidth.toLong() * targetHeight <= QREX_DOCUMENT_MAX_PIXELS)
        return targetWidth to targetHeight
    }

    /**
     * Whole-document transaction: an oversized, encrypted or corrupt PDF
     * leaves the existing QREX session unchanged. On failures, every allocated
     * preview bitmap is recycled. Caller takes ownership on success.
     */
    fun render(context: Context, uri: Uri, remainingPages: Int): List<Bitmap> {
        require(remainingPages in 1..QREX_DOCUMENT_MAX_PAGES)
        val imported = ArrayList<Bitmap>(remainingPages)
        try {
            val file = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IOException("Cannot open selected PDF")
            file.use { descriptor ->
                if (descriptor.statSize > MAX_PDF_BYTES) {
                    throw IOException("Selected PDF exceeds import file limit")
                }
                PdfRenderer(descriptor).use { renderer ->
                    val count = renderer.pageCount
                    require(count in 1..remainingPages) {
                        "PDF exceeds remaining document page limit"
                    }
                    for (index in 0 until count) {
                        renderer.openPage(index).use { page ->
                            val (width, height) = targetSize(page.width, page.height)
                            val bitmap = Bitmap.createBitmap(
                                width, height, Bitmap.Config.ARGB_8888
                            )
                            try {
                                bitmap.eraseColor(Color.WHITE)
                                page.render(bitmap, null, null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                imported.add(bitmap)
                            } catch (failure: Exception) {
                                bitmap.recycle()
                                throw failure
                            }
                        }
                    }
                }
            }
            return imported
        } catch (failure: Throwable) {
            imported.forEach { if (!it.isRecycled) it.recycle() }
            throw failure
        }
    }
}
