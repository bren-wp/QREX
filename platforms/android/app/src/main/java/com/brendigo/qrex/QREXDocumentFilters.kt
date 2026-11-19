package com.brendigo.qrex

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt
import kotlin.math.roundToInt

/** In-memory page enhancement, independent from QR image decoding and PDF export. */
internal enum class QREXDocumentFilter { AUTO, GRAYSCALE, BLACK_WHITE }

internal object QREXDocumentFilters {
    /**
     * Bounded working memory: one output bitmap and one scanline buffer.
     * Transparent pixels are flattened onto white paper, never black.
     * The original is left unchanged so the user's one-step Undo can restore it.
     */
    fun apply(source: Bitmap, mode: QREXDocumentFilter): Bitmap {
        require(!source.isRecycled && source.width > 0 && source.height > 0)
        require(source.width.toLong() * source.height <= QREX_DOCUMENT_MAX_PIXELS)
        if (mode == QREXDocumentFilter.AUTO) return autoEnhance(source)
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

    /**
     * DocuScan's adaptive luminance histogram, ported to QREX's bounded,
     * in-memory document pipeline. Keep the original unmodified for Undo.
     * The OpenCV dependency and the DocuScan UI are intentionally not copied.
     * Attribution: docs/THIRD_PARTY_DOCUSCAN.md
     */
    private fun autoEnhance(source: Bitmap): Bitmap {
        val histogram = IntArray(256)
        val row = IntArray(source.width)
        val step = maxOf(1, sqrt(source.width.toDouble() *
            source.height.toDouble() / 20_000.0).toInt())
        for (y in 0 until source.height step step) {
            source.getPixels(row, 0, source.width, 0, y, source.width, 1)
            for (x in 0 until source.width step step) {
                val p = row[x]
                val alpha = Color.alpha(p)
                val red = (Color.red(p) * alpha + 255 * (255 - alpha) + 127) / 255
                val green = (Color.green(p) * alpha + 255 * (255 - alpha) + 127) / 255
                val blue = (Color.blue(p) * alpha + 255 * (255 - alpha) + 127) / 255
                histogram[(red * 299 + green * 587 + blue * 114 + 500) / 1000]++
            }
        }
        val count = histogram.sum()
        if (count == 0) return source
        val cut = (count * 0.005).toInt()
        var running = 0
        var low = 0
        for (index in histogram.indices) {
            running += histogram[index]
            if (running > cut) { low = index; break }
        }
        running = 0
        var high = 255
        for (index in 255 downTo 0) {
            running += histogram[index]
            if (running > cut) { high = index; break }
        }
        if (high - low < 14) return source
        val scale = 1.05f * 255f / (high - low)
        fun level(value: Int): Int =
            ((value - low) * scale + 2f).roundToInt().coerceIn(0, 255)
        val output = Bitmap.createBitmap(source.width, source.height,
            Bitmap.Config.ARGB_8888)
        try {
            for (y in 0 until source.height) {
                source.getPixels(row, 0, source.width, 0, y, source.width, 1)
                for (x in row.indices) {
                    val p = row[x]
                    val alpha = Color.alpha(p)
                    val red = (Color.red(p) * alpha + 255 * (255 - alpha) + 127) / 255
                    val green = (Color.green(p) * alpha + 255 * (255 - alpha) + 127) / 255
                    val blue = (Color.blue(p) * alpha + 255 * (255 - alpha) + 127) / 255
                    row[x] = Color.rgb(level(red), level(green), level(blue))
                }
                output.setPixels(row, 0, source.width, 0, y, source.width, 1)
            }
            return output
        } catch (failure: Exception) {
            output.recycle()
            throw failure
        }
    }
}
