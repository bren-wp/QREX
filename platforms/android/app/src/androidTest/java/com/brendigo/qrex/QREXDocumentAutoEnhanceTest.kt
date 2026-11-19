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
class QREXDocumentAutoEnhanceTest {
    @Test fun improvesContrastButLeavesSourceUnchangedForUndo() {
        val input = Bitmap.createBitmap(128, 32, Bitmap.Config.ARGB_8888)
        var output: Bitmap? = null
        try {
            for (x in 0 until input.width) {
                val tone = 70 + (x * 110 / (input.width - 1))
                for (y in 0 until input.height) {
                    input.setPixel(x, y, Color.rgb(tone, tone, tone))
                }
            }
            output = QREXDocumentFilters.apply(input, QREXDocumentFilter.AUTO)
            assertNotSame(input, output)
            assertEquals(70, Color.red(input.getPixel(0, 0)))
            assertTrue(Color.red(output.getPixel(0, 0)) < 70)
            assertTrue(Color.red(output.getPixel(127, 0)) > 180)
            assertEquals(input.width, output.width)
            assertEquals(input.height, output.height)
        } finally {
            if (output != null && output !== input && !output.isRecycled) output.recycle()
            input.recycle()
        }
    }

    @Test fun almostFlatPageIsLeftUnchangedRatherThanOverProcessed() {
        val input = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        try {
            input.eraseColor(Color.rgb(241, 241, 241))
            assertTrue(QREXDocumentFilters.apply(input, QREXDocumentFilter.AUTO) === input)
        } finally {
            input.recycle()
        }
    }
}
