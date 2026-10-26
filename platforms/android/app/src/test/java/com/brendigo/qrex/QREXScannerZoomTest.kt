package com.brendigo.qrex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Real CameraX bounds determine which zoom chips are available. */
class QREXScannerZoomTest {
    @Test
    fun normalCameraIncludesOneAndTwoTimesOnly() {
        assertEquals(listOf(1f, 2f), scannerZoomPresets(1f, 6f))
    }

    @Test
    fun ultrawideCameraOffersHalfOneTwoTimes() {
        assertEquals(listOf(.5f, 1f, 2f), scannerZoomPresets(.5f, 3f))
    }

    @Test
    fun smallMaxZoomStillHasASecondUsefulControl() {
        assertEquals(listOf(1f, 1.6f), scannerZoomPresets(1f, 1.6f))
    }

    @Test
    fun cameraWithoutZoomDoesNotOfferUnsupportedControls() {
        assertEquals(listOf(1f), scannerZoomPresets(1f, 1f))
    }

    @Test
    fun invalidBoundsCannotGenerateButtons() {
        assertTrue(scannerZoomPresets(0f, 2f).isEmpty())
        assertTrue(scannerZoomPresets(2f, 1f).isEmpty())
        assertTrue(scannerZoomPresets(Float.NaN, 2f).isEmpty())
        assertTrue(scannerZoomPresets(1f, Float.POSITIVE_INFINITY).isEmpty())
    }
}
