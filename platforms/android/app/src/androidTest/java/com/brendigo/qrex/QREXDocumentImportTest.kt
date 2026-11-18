package com.brendigo.qrex

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android JPEG/EXIF import; fixtures never contain user documents. */
@RunWith(AndroidJUnit4::class)
class QREXDocumentImportTest {
    @Test fun pickedJpegRotationIsAppliedWithinPrivateBitmapBudget() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "qrex-import-orientation-test.jpg")
        val original = Bitmap.createBitmap(240, 160, Bitmap.Config.ARGB_8888)
        var imported: Bitmap? = null
        try {
            original.eraseColor(Color.WHITE)
            file.outputStream().use {
                assertTrue(original.compress(Bitmap.CompressFormat.JPEG, 95, it))
            }
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            imported = QREXDocumentProcessing.readFromPicker(context, Uri.fromFile(file))
            assertEquals(160, imported.width)
            assertEquals(240, imported.height)
            assertTrue(imported.width.toLong() * imported.height <= QREX_DOCUMENT_MAX_PIXELS)
        } finally {
            if (imported != null && !imported.isRecycled) imported.recycle()
            original.recycle()
            file.delete()
        }
    }
}
