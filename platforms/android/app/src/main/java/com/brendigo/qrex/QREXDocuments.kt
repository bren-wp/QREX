package com.brendigo.qrex

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Creates a ZIP containing one genuine, scan-ready PDF per manually saved QR.
 * It never creates temporary plaintext files and uses only a user-selected
 * Storage Access Framework destination; credentials never appear in filenames.
 * Call on Dispatchers.IO, not on the Compose main thread.
 */
object QREXDocuments {
    fun exportSavedPdfZip(context: Context, records: List<QRRecord>, destination: Uri): Boolean {
        if (records.isEmpty() || records.size > 250 || records.any { !it.saved }) return false
        return runCatching {
            // A stream to the selected document only; no shared folders or backups.
            val stream = context.contentResolver.openOutputStream(destination, "w")
                ?: error("Unable to open document destination")
            stream.use { output ->
                ZipOutputStream(output.buffered()).use { archive ->
                    records.forEachIndexed { index, record ->
                        require(record.raw.isNotBlank() && record.raw.length <= 10000)
                        val bitmap = generateQR(record.raw)
                            ?: error("A saved QR code is too large to render")
                        val page = pdfPage(bitmap, index + 1, records.size)
                        val entry = ZipEntry("QREX-Code-%03d.pdf".format(java.util.Locale.ROOT, index + 1))
                        archive.putNextEntry(entry)
                        archive.write(page)
                        archive.closeEntry()
                    }
                    archive.finish()
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun pdfPage(bitmap: Bitmap, index: Int, total: Int): ByteArray {
        val document = PdfDocument()
        try {
            val info = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(info)
            val canvas: Canvas = page.canvas
            canvas.drawColor(android.graphics.Color.WHITE)
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(3, 12, 27)
                textSize = 28f
                isFakeBoldText = true
            }
            canvas.drawText("QREX", 58f, 82f, label)
            label.textSize = 15f
            label.isFakeBoldText = false
            canvas.drawText("Scan. Create. Share.", 58f, 111f, label)
            canvas.drawBitmap(bitmap, null, RectF(78f, 188f, 517f, 627f),
                Paint(Paint.FILTER_BITMAP_FLAG))
            label.textSize = 12f
            canvas.drawText("Saved code $index of $total", 58f, 743f, label)
            canvas.drawText("Created with QREX by Brendigo", 58f, 765f, label)
            document.finishPage(page)
            return ByteArrayOutputStream().use { output ->
                document.writeTo(output)
                output.toByteArray()
            }
        } finally {
            document.close()
        }
    }
}
