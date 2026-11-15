package com.brendigo.qrex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QREXDocumentProcessingTest {
    @Test fun portraitPdfPreservesImageRatioAndMargins() {
        val r = qrexDocumentFit(1000, 2000, 595, 842)
        assertTrue(r[0] >= 24f && r[1] >= 24f)
        assertTrue(r[2] <= 571f && r[3] <= 818f)
        assertEquals(0.5f, (r[2] - r[0]) / (r[3] - r[1]), .0001f)
    }

    @Test fun landscapePdfPreservesImageRatio() {
        val r = qrexDocumentFit(2000, 1000, 842, 595)
        assertEquals(2f, (r[2] - r[0]) / (r[3] - r[1]), .0001f)
    }

    @Test fun invalidDimensionsYieldEmptyBounds() {
        val r = qrexDocumentFit(0, 100, 595, 842)
        assertTrue(r.all { it == 0f })
    }

    @Test fun validQuadrilateralIncludesPerspectiveTrapezoid() {
        assertTrue(qrexDocumentQuadValid(listOf(.1f,.1f,.9f,.2f,.8f,.85f,.2f,.9f)))
        assertFalse(qrexDocumentQuadValid(listOf(0f,0f,1f,1f,0f,1f,1f,0f)))
        assertFalse(qrexDocumentQuadValid(listOf(0f,0f,1f,0f,1f,0f,0f,1f)))
        assertFalse(qrexDocumentQuadValid(listOf(0f,0f,2f,0f,1f,1f,0f,1f)))
        assertFalse(qrexDocumentQuadValid(listOf(Float.NaN,0f,1f,0f,1f,1f,0f,1f)))
    }

    @Test fun documentNameNeverContainsPathOrUnsafeCharacters() {
        assertEquals("scanned_page.pdf", qrexDocumentSafeName(" scanned/page "))
        assertEquals("paper.pdf", qrexDocumentSafeName("paper.PDF").lowercase())
        assertEquals("QREX-document.pdf", qrexDocumentSafeName("../"))
        assertTrue(qrexDocumentSafeName("x".repeat(200)).length <= 68)
    }
}
