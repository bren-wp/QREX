package com.brendigo.qrex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Physical Canvas geometry — production CameraX preview remains untouched. */
class QREXScannerFrameTest {
    @Test fun referenceBracketOccupiesSixtyThreePercentOfNormalPreview() {
        val side = scannerFrameSidePx(340f, 365f)
        assertEquals(214.2f, side, 0.1f)
        assertEquals(.63f, side / 340f, .001f)
    }

    @Test fun shortOrNarrowViewportReservesSpaceForWorkingCameraActions() {
        val short = scannerFrameSidePx(320f, 180f)
        assertEquals(149.4f, short, 0.1f)
        assertTrue(short <= 180f * .83f)
        val tiny = scannerFrameSidePx(320f, 85f)
        assertEquals(70.55f, tiny, 0.1f)
        assertTrue(tiny < 85f)
    }

    @Test fun tabletViewfinderCapsAt440DpAndKeepsCanonicalPhoneRatio() {
        assertEquals(440f, scannerFrameSidePx(900f, 700f, 440f), 0.1f)
        assertEquals(214.2f, scannerFrameSidePx(340f, 400f, 440f), 0.1f)
        assertEquals(0f, scannerFrameSidePx(900f, 700f, Float.NaN), 0f)
        assertEquals(0f, scannerFrameSidePx(900f, 700f, -1f), 0f)
    }

    @Test fun bracketVerticalPositionTracksReferenceAndClampsOverButtons() {
        // User reference: the bracket is centered ~42% down the camera panel.
        assertEquals(192f, scannerFrameTopPx(933f, 668f, 400f), 0.1f)
        assertEquals(0f, scannerFrameTopPx(180f, 100f, 120f), 0.1f)
        assertEquals(0f, scannerFrameTopPx(0f, 100f, 40f), 0.1f)
    }

    @Test fun shortLandscapeAndLargeFontUseScrollInsteadOfSqueezedCameraPanel() {
        assertTrue(qrexScannerNeedsScrollLayout(568, 1f))
        assertTrue(qrexScannerNeedsScrollLayout(619, 1f))
        assertTrue(!qrexScannerNeedsScrollLayout(620, 1f))
        assertTrue(!qrexScannerNeedsScrollLayout(852, 1.49f))
        assertTrue(qrexScannerNeedsScrollLayout(852, 1.5f))
        assertEquals(304, qrexScannerScrollablePreviewHeightDp(320, 568))
        assertEquals(378, qrexScannerScrollablePreviewHeightDp(800, 600))
        assertEquals(440, qrexScannerScrollablePreviewHeightDp(800, 1280))
        assertEquals(228, qrexScannerScrollablePreviewHeightDp(240, 420))
        assertEquals(300, qrexScannerScrollablePreviewHeightDp(240, 420, 1.5f))
        assertEquals(360, qrexScannerScrollablePreviewHeightDp(240, 420, 2.0f))
        assertEquals(520, qrexScannerScrollablePreviewHeightDp(800, 1280, 2.0f))
    }

    @Test fun invalidCanvasDimensionsCannotPaintOutsidePreview() {
        assertEquals(0f, scannerFrameSidePx(0f, 300f), 0f)
        assertEquals(0f, scannerFrameSidePx(300f, -2f), 0f)
        assertEquals(0f, scannerFrameSidePx(Float.NaN, 300f), 0f)
        assertEquals(0f, scannerFrameSidePx(300f, Float.POSITIVE_INFINITY), 0f)
    }
}
