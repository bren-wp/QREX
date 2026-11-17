package com.brendigo.qrex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class QREXDocumentCropGeometryTest {
    private val full = QREX_DOCUMENT_FULL_CORNERS

    @Test fun fullImageResetIsValidAndIncludesAllPixels() {
        assertTrue(qrexDocumentQuadValid(full))
        assertEquals(listOf(0f,0f,1f,0f,1f,1f,0f,1f), full)
    }

    @Test fun rectangleModeConvertsSkewedCornersWithoutCroppingYet() {
        val perspective = listOf(.1f,.2f,.9f,.1f,.8f,.8f,.2f,.9f)
        assertEquals(
            listOf(.1f,.1f,.9f,.1f,.9f,.9f,.1f,.9f),
            qrexDocumentBoundingRectangle(perspective)
        )
    }

    @Test fun rectangleDragKeepsOppositeCornerFixedAndOtherEdgesAligned() {
        val dragged = qrexDocumentMoveCorner(full, 0, .2f, .3f, true)
        assertEquals(listOf(.2f,.3f,1f,.3f,1f,1f,.2f,1f), dragged)
        assertTrue(qrexDocumentQuadValid(dragged))
        val draggedBottom = qrexDocumentMoveCorner(dragged, 2, .8f, .9f, true)
        assertEquals(listOf(.2f,.3f,.8f,.3f,.8f,.9f,.2f,.9f), draggedBottom)
    }

    @Test fun perspectiveMoveOnlyChangesRequestedCorner() {
        val newCorners = qrexDocumentMoveCorner(full, 0, .1f, .17f, false)
        assertEquals(.1f, newCorners[0], .00001f)
        assertEquals(.17f, newCorners[1], .00001f)
        assertEquals(full.subList(2,8), newCorners.subList(2,8))
    }

    @Test fun rejectCollapsedInvertedCrossedOrNonfiniteShapes() {
        val current = qrexDocumentMoveCorner(full, 0, .1f, .1f, false)
        assertSame(current, qrexDocumentMoveCorner(current, 0, .95f, .95f, true))
        assertSame(current, qrexDocumentMoveCorner(current, 0, Float.NaN, .3f, false))
        assertSame(current, qrexDocumentMoveCorner(current, 7, .2f, .2f, false))
        assertSame(current, qrexDocumentMoveCorner(current, 0, 2f, 1f, false))
        assertFalse(qrexDocumentQuadValid(listOf(.1f,.1f,.9f,.9f,.1f,.9f,.9f,.1f)))
    }

    @Test fun tightCropCanSelectSmallReceiptOrPhotoDetail() {
        val tight = listOf(.3f,.3f,.33f,.3f,.33f,.33f,.3f,.33f)
        assertTrue(qrexDocumentQuadValid(tight))
        val smaller = qrexDocumentMoveCorner(full, 2, .04f, .04f, true)
        assertTrue(qrexDocumentQuadValid(smaller))
        assertEquals(.04f, smaller[4], .00001f)
        assertEquals(.04f, smaller[5], .00001f)
    }
}
