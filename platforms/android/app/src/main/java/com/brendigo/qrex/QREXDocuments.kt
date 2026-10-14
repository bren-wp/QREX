package com.brendigo.qrex

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Creates a ZIP containing one multi-page, printable PDF of saved QR codes.
 * It never creates temporary plaintext files and uses only a user-selected
 * Storage Access Framework destination; credentials never appear in filenames.
 * Call on Dispatchers.IO, not on the Compose main thread.
 */
object QREXDocuments {
    fun exportSavedPdf(context: Context, records: List<QRRecord>, destination: Uri): Boolean {
        if (records.isEmpty() || records.size > 250 || records.any { !it.saved }) return false
        return runCatching {
            val pdf = createSavedCodesPdf(records)
            require(pdf.size <= 60_000_000)
            val output = context.contentResolver.openOutputStream(destination, "w")
                ?: error("Cannot open selected PDF destination")
            output.use { it.write(pdf) }
            true
        }.getOrDefault(false)
    }

    fun exportSavedPdfZip(context: Context, records: List<QRRecord>, destination: Uri): Boolean {
        if (records.isEmpty() || records.size > 250 || records.any { !it.saved }) return false
        return runCatching {
            // Same interoperable structure as iOS: one multi-page PDF in ZIP.
            val pdf = createSavedCodesPdf(records)
            require(pdf.size <= 60_000_000)
            val output = context.contentResolver.openOutputStream(destination, "w")
                ?: error("Cannot open selected ZIP destination")
            output.use { stream ->
                ZipOutputStream(stream.buffered()).use { archive ->
                    archive.putNextEntry(ZipEntry("QREX-Saved-Codes.pdf"))
                    archive.write(pdf)
                    archive.closeEntry()
                    archive.finish()
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun createSavedCodesPdf(records: List<QRRecord>): ByteArray {
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
                require(record.raw.isNotBlank() && record.raw.length <= 10000)
                val bitmap = generateQR(record.raw)
                    ?: error("Saved code cannot be rendered as QR")
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                val canvas: Canvas = page.canvas
                canvas.drawColor(android.graphics.Color.WHITE)
                canvas.drawText("QREX", 58f, 82f, title)
                canvas.drawText("Scan. Create. Share.", 58f, 111f, ink)
                canvas.drawBitmap(bitmap, null, RectF(78f, 188f, 517f, 627f),
                    Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.drawText("Saved code ${index + 1} of ${records.size}", 58f, 743f, ink)
                canvas.drawText("Created with QREX by Brendigo", 58f, 765f, ink)
                pdf.finishPage(page)
            }
            return ByteArrayOutputStream().use { bytes ->
                pdf.writeTo(bytes)
                bytes.toByteArray()
            }
        } finally {
            pdf.close()
        }
    }

}
