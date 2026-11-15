package com.brendigo.qrex

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QREXDocumentPdfTest {
    @Test fun multiPagePdfOpensWithTwoReadablePagesAndCorrectOrientation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "qrex-test-document.pdf")
        val portrait = Bitmap.createBitmap(280, 440, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.WHITE); setPixel(40, 40, Color.BLACK) }
        val landscape = Bitmap.createBitmap(440, 280, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.WHITE); setPixel(40, 40, Color.BLACK) }
        try {
            QREXDocumentPdf.export(context, listOf(portrait, landscape), Uri.fromFile(file))
            assertTrue(file.length() > 200)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    assertEquals(2, renderer.pageCount)
                    renderer.openPage(0).use { assertTrue(it.height > it.width) }
                    renderer.openPage(1).use { assertTrue(it.width > it.height) }
                }
            }
        } finally {
            portrait.recycle()
            landscape.recycle()
            file.delete()
        }
    }
}
