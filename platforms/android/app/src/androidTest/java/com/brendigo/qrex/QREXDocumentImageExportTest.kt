package com.brendigo.qrex

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QREXDocumentImageExportTest {
    @Test fun exportPngWritesSelectedPageWithoutChangingPixels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "qrex-page-export-test.png")
        val page = Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888)
        try {
            page.eraseColor(Color.rgb(12, 34, 56))
            QREXDocumentImageExport.exportPng(context, page, Uri.fromFile(file))
            assertTrue(file.length() > 16)
            val header = ByteArray(8)
            file.inputStream().use { assertEquals(8, it.read(header)) }
            assertEquals("89504e470d0a1a0a",
                header.joinToString("") { "%02x".format(it.toInt() and 255) })
            assertEquals(Color.rgb(12, 34, 56), page.getPixel(0, 0))
        } finally {
            page.recycle()
            file.delete()
        }
    }
}
