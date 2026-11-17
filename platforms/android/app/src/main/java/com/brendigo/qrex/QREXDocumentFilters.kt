package com.brendigo.qrex

import android.graphics.Bitmap
import android.graphics.Color

/** In-memory page enhancement, independent from QR image decoding and PDF export. */
internal enum class QREXDocumentFilter { GRAYSCALE, BLACK_WHITE }

internal object QREXDocumentFilters {
    /**
     * Bounded working memory: one output bitmap and one scanline buffer.
     * Transparent pixels are flattened onto white paper, never black.
     * The original is left unchanged so the user's one-step Undo can restore it.
     */
    fun apply(source: Bitmap, mode: QREXDocumentFilter): Bitmap {
        require(!source.isRecycled && source.width > 0 && source.height > 0)
        require(source.width.toLong() * source.height <= QREX_DOCUMENT_MAX_PIXELS)
        val output = Bitmap.createBitmap(source.width, source.height,
            Bitmap.Config.ARGB_8888)
        val row = IntArray(source.width)
        try {
            for (y in 0 until source.height) {
                source.getPixels(row, 0, source.width, 0, y, source.width, 1)
                for (x in row.indices) {
                    val p = row[x]
                    val alpha = Color.alpha(p)
                    val red = (Color.red(p) * alpha + 255 * (255 - alpha) + 127) / 255
                    val green = (Color.green(p) * alpha + 255 * (255 - alpha) + 127) / 255
                    val blue = (Color.blue(p) * alpha + 255 * (255 - alpha) + 127) / 255
                    val luminance = (red * 299 + green * 587 + blue * 114 + 500) / 1000
                    val tone = if (mode == QREXDocumentFilter.BLACK_WHITE) {
                        if (luminance >= 170) 255 else 0
                    } else luminance
                    row[x] = Color.rgb(tone, tone, tone)
                }
                output.setPixels(row, 0, source.width, 0, y, source.width, 1)
            }
            return output
        } catch (e: Exception) {
            output.recycle()
            throw e
        }
    }
}
