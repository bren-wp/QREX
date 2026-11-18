package com.brendigo.qrex

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QREXDocumentAutoEdgesTest {
    @Test fun suggestsPaperBoundsOnContrastingSurfaceWithoutEditingThePhoto() {
        val source = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.rgb(30, 34, 40))
        Canvas(source).drawRect(65f, 100f, 340f, 510f,
            Paint().apply { color = Color.WHITE })
        try {
            val corners = requireNotNull(qrexDocumentSuggestEdges(source))
            assertTrue(qrexDocumentQuadValid(corners))
            assertTrue(kotlin.math.abs(corners[0] - .1625f) < .045f)
            assertTrue(kotlin.math.abs(corners[1] - .1667f) < .045f)
            assertTrue(kotlin.math.abs(corners[4] - .85f) < .045f)
            assertTrue(kotlin.math.abs(corners[5] - .85f) < .045f)
            assertEquals(Color.rgb(30, 34, 40), source.getPixel(0, 0))
            assertEquals(Color.WHITE, source.getPixel(200, 300))
        } finally { source.recycle() }
    }

    @Test fun rejectsUniformOrTinyImagesInsteadOfFabricatingCorners() {
        val plain = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
        val tiny = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        plain.eraseColor(Color.WHITE)
        try {
            assertNull(qrexDocumentSuggestEdges(plain))
            assertNull(qrexDocumentSuggestEdges(tiny))
        } finally {
            plain.recycle()
            tiny.recycle()
        }
    }

    @Test fun alsoFindsDarkPageOnLightBackground() {
        val source = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.WHITE)
        Canvas(source).drawRect(80f, 120f, 320f, 470f,
            Paint().apply { color = Color.rgb(38, 40, 44) })
        try {
            val corners = requireNotNull(qrexDocumentSuggestEdges(source))
            assertTrue(qrexDocumentQuadValid(corners))
            assertTrue(kotlin.math.abs(corners[0] - .20f) < .045f)
            assertTrue(kotlin.math.abs(corners[5] - .783f) < .045f)
        } finally { source.recycle() }
    }
}
