package com.brendigo.qrex

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Checks actual Android bitmap pixels, not merely the crop outline. */
@RunWith(AndroidJUnit4::class)
class QREXDocumentCropInstrumentedTest {
    @Test fun rectangleCropKeepsChosenPixelsWithoutMutatingOriginal() {
        val source = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        Canvas(source).apply {
            drawColor(Color.WHITE)
            drawRect(100f, 100f, 300f, 300f, Paint().apply { color = Color.RED })
        }
        val corners = listOf(.25f,.25f,.75f,.25f,.75f,.75f,.25f,.75f)
        val cropped = QREXDocumentProcessing.cropPerspective(source, corners)
        try {
            assertEquals(200, cropped.width)
            assertEquals(200, cropped.height)
            assertEquals(Color.RED, cropped.getPixel(100, 100))
            assertEquals(Color.WHITE, source.getPixel(50, 50))
            assertEquals(400, source.width)
        } finally {
            cropped.recycle()
            source.recycle()
        }
    }

    @Test fun perspectiveCropProducesBoundedReadableImage() {
        val source = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.BLUE)
        val corners = listOf(.12f,.10f,.90f,.16f,.82f,.89f,.18f,.82f)
        val cropped = QREXDocumentProcessing.cropPerspective(source, corners)
        try {
            assertTrue(cropped.width > 200)
            assertTrue(cropped.height > 200)
            assertTrue(cropped.width.toLong() * cropped.height <= QREX_DOCUMENT_MAX_PIXELS)
            assertEquals(Color.BLUE, cropped.getPixel(cropped.width/2, cropped.height/2))
        } finally {
            cropped.recycle()
            source.recycle()
        }
    }
}
