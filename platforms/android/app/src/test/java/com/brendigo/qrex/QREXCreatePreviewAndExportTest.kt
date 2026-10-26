package com.brendigo.qrex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression coverage for Compose Create preview work and consent boundary. */
class QREXCreatePreviewAndExportTest {
    @Test
    fun bitmapGenerationRequiresVisibleEligiblePreview() {
        assertTrue(shouldGenerateCreateQrPreview(false, true, true))
        assertFalse(shouldGenerateCreateQrPreview(true, true, true))
        assertFalse(shouldGenerateCreateQrPreview(false, false, true))
        assertFalse(shouldGenerateCreateQrPreview(false, true, false))
        assertFalse(shouldGenerateCreateQrPreview(true, false, false))
    }

    @Test
    fun wifiExportUsesOnlyTheExactPreviouslyApprovedPayload() {
        val requested = QRContent.wifi("QREX office", "original-secret")
        val draftChangedAfterWarning = QRContent.wifi("Guest", "new-secret")
        val approved = approvedWifiQrExport(requested)
        assertEquals(requested, approved)
        assertFalse(approved == draftChangedAfterWarning)
        assertEquals("original-secret", QRContent.wifiField(approved!!, "P"))
    }

    @Test
    fun cancelledAndNonWifiExportsAreNeverApproved() {
        assertNull(approvedWifiQrExport(null))
        assertNull(approvedWifiQrExport(""))
        assertNull(approvedWifiQrExport("https://brendigo.com"))
        assertNull(approvedWifiQrExport("ordinary text"))
        assertNull(approvedWifiQrExport("x".repeat(1501)))
    }

    @Test
    fun openWifiStillRequiresSnapshotConsent() {
        val openWifi = QRContent.wifi("Visitors", "")
        assertEquals(openWifi, approvedWifiQrExport(openWifi))
        assertNull(approvedWifiQrExport(null))
    }
}
