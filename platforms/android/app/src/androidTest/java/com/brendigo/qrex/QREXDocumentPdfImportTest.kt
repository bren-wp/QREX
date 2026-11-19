package com.brendigo.qrex

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android PdfRenderer exercised with generated, non-sensitive documents. */
@RunWith(AndroidJUnit4::class)
class QREXDocumentPdfImportTest {
    private fun fixture(pageCount: Int): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "qrex-docuscan-import-test.pdf")
        val document = PdfDocument()
        try {
            for (number in 1..pageCount) {
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(595, 842, number).create()
                )
                page.canvas.drawColor(Color.WHITE)
                page.canvas.drawText("QREX page", 36f, 48f,
                    Paint().apply { color = Color.BLACK; textSize = 20f })
                document.finishPage(page)
            }
            file.outputStream().use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return file
    }

    @Test fun importsPagesWithoutChangingQrexLayoutOrMemoryBudget() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = fixture(2)
        var imported = emptyList<Bitmap>()
        try {
            imported = QREXDocumentPdfImport.render(context, Uri.fromFile(file), 8)
            assertEquals(2, imported.size)
            assertTrue(imported.all {
                it.width.toLong() * it.height <= QREX_DOCUMENT_MAX_PIXELS
            })
            val session = QREXDocumentSession()
            assertTrue(session.appendImportedPages(imported))
            assertEquals(2, session.pages.size)
            session.clearSession()
        } finally {
            imported.forEach { if (!it.isRecycled) it.recycle() }
            file.delete()
        }
    }

    @Test fun rejectsPdfLargerThanRemainingPageCapacity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = fixture(2)
        try {
            try {
                QREXDocumentPdfImport.render(context, Uri.fromFile(file), 1)
                fail("Expected bounded import to reject the PDF")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("page"))
            }
        } finally {
            file.delete()
        }
    }
}
