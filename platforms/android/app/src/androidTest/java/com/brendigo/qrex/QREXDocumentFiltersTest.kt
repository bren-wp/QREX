package com.brendigo.qrex

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QREXDocumentFiltersTest {
    @Test fun grayscaleMakesAllChannelsEqualAndLeavesOriginalUnchanged() {
        val source = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888)
        source.setPixel(0, 0, Color.RED)
        source.setPixel(1, 0, Color.TRANSPARENT)
        val result = QREXDocumentFilters.apply(source, QREXDocumentFilter.GRAYSCALE)
        try {
            assertNotSame(source, result)
            assertEquals(2, result.width)
            assertEquals(1, result.height)
            val shade = result.getPixel(0, 0)
            assertEquals(Color.red(shade), Color.green(shade))
            assertEquals(Color.green(shade), Color.blue(shade))
            assertEquals(Color.WHITE, result.getPixel(1, 0))
            assertEquals(Color.RED, source.getPixel(0, 0))
        } finally {
            source.recycle()
            result.recycle()
        }
    }

    @Test fun blackWhiteRemovesPaperNoiseButPreservesDarkText() {
        val source = Bitmap.createBitmap(3, 1, Bitmap.Config.ARGB_8888)
        source.setPixel(0, 0, Color.rgb(55, 55, 55))
        source.setPixel(1, 0, Color.rgb(240, 240, 240))
        source.setPixel(2, 0, Color.rgb(160, 160, 160))
        val result = QREXDocumentFilters.apply(source, QREXDocumentFilter.BLACK_WHITE)
        try {
            assertEquals(Color.BLACK, result.getPixel(0, 0))
            assertEquals(Color.WHITE, result.getPixel(1, 0))
            assertEquals(Color.BLACK, result.getPixel(2, 0))
            assertEquals(Color.rgb(160, 160, 160), source.getPixel(2, 0))
        } finally {
            source.recycle()
            result.recycle()
        }
    }
}
