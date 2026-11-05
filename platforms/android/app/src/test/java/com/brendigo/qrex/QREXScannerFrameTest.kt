package com.brendigo.qrex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Physical Canvas geometry — production CameraX preview remains untouched. */
class QREXScannerFrameTest {
    @Test fun referenceBracketOccupiesNearlyThreeQuartersOfNormalPreview() {
        val side = scannerFrameSidePx(340f, 365f)
        assertEquals(248.2f, side, 0.1f)
        assertEquals(.73f, side / 340f, .001f)
    }

    @Test fun shortOrNarrowViewportReservesSpaceForWorkingCameraActions() {
        val short = scannerFrameSidePx(320f, 180f)
        assertEquals(149.4f, short, 0.1f)
        assertTrue(short <= 180f * .83f)
        val tiny = scannerFrameSidePx(320f, 85f)
        assertEquals(70.55f, tiny, 0.1f)
        assertTrue(tiny < 85f)
    }

    @Test fun invalidCanvasDimensionsCannotPaintOutsidePreview() {
        assertEquals(0f, scannerFrameSidePx(0f, 300f), 0f)
        assertEquals(0f, scannerFrameSidePx(300f, -2f), 0f)
        assertEquals(0f, scannerFrameSidePx(Float.NaN, 300f), 0f)
        assertEquals(0f, scannerFrameSidePx(300f, Float.POSITIVE_INFINITY), 0f)
    }
}
