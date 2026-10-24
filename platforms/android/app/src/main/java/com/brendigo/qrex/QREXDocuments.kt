package com.brendigo.qrex

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.OutputStream
import java.util.Locale

/**
 * Streams exported PDFs directly into a user-selected SAF destination.
 *
 * There is no intermediate plaintext file and no full-PDF ByteArray.
 * Run exports on Dispatchers.IO; the caller owns the destination URI.
 */
object QREXDocuments {
    private fun valid(records: List<QRRecord>): Boolean =
        records.isNotEmpty() && records.size <= 250 &&
            records.all { it.saved && it.raw.isNotBlank() && QRContent.canRender(it.raw) }

    fun exportSavedPdf(context: Context, records: List<QRRecord>, destination: Uri): Boolean {
        if (!valid(records)) return false
        return runCatching {
            val output = context.contentResolver.openOutputStream(destination, "w")
                ?: error("Cannot open selected PDF destination")
            output.use { writePdf(context, records, it) }
            true
        }.getOrDefault(false)
    }

     /** Keep each generated bitmap in memory only for the duration of its PDF page. */
    private fun writePdf(context: Context, records: List<QRRecord>, output: OutputStream) {
        val preferred = runCatching {
            context.getSharedPreferences("qrex.user", Context.MODE_PRIVATE)
                .getString("language", "system")
        }.getOrNull()?.takeIf { it == "system" || it in QREXStrings.languages } ?: "system"
        val locale = if (preferred == "system") context.resources.configuration.locales[0]
            else Locale.forLanguageTag(preferred)
        val slogan = QREXStrings.get(context, preferred, "tagline")
        val counter = QREXStrings.get(context, preferred, "pdf_saved_code")
        val pdf = PdfDocument()
        try {
            val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(3, 12, 27)
                textSize = 28f
                isFakeBoldText = true
            }
            val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(3, 12, 27)
                textSize = 13f
            }
            records.forEachIndexed { index, record ->
                val bitmap: Bitmap = generateQR(record.raw)
                    ?: error("Saved code cannot be rendered as QR")
                try {
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                    val canvas: Canvas = page.canvas
                    canvas.drawColor(android.graphics.Color.WHITE)
                    canvas.drawText("QREX", 58f, 82f, title)
                    canvas.drawText(slogan, 58f, 111f, ink)
                    canvas.drawBitmap(bitmap, null, RectF(78f, 188f, 517f, 627f),
                        Paint(Paint.FILTER_BITMAP_FLAG))
                    canvas.drawText(String.format(locale, counter, index + 1, records.size), 58f, 743f, ink)
                    canvas.drawText("QREX · Brendigo", 58f, 765f, ink)
                    pdf.finishPage(page)
                } finally {
                    bitmap.recycle()
                }
            }
            pdf.writeTo(output)
        } finally {
            pdf.close()
        }
    }
}
