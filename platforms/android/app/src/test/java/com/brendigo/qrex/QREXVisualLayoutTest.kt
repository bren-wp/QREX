package com.brendigo.qrex

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure responsive-layout checks: no emulator, device or QR data required. */
class QREXVisualLayoutTest {
    @Test fun settingsSwitchToRowsBeforeLargeTextIsTruncated() {
        assertFalse(useCompactMoreList(393, 1.0f))
        assertFalse(useCompactMoreList(320, 1.0f))
        assertTrue(useCompactMoreList(299, 1.0f))
        assertTrue(useCompactMoreList(393, 1.30f))
        assertTrue(useCompactMoreList(320, 1.60f))
    }

    @Test fun createChoicesRemainAccessibleAtLargeFontAndCompactWidth() {
        assertFalse(qrexLayoutProfile(430, 850, 1.0f).stackTypeChoices)
        assertTrue(qrexLayoutProfile(393, 850, 1.0f).stackTypeChoices)
        assertTrue(qrexLayoutProfile(430, 850, 1.30f).stackTypeChoices)
        assertEquals(104, qrexLayoutProfile(320, 600, 1.6f).darkQrSizeDp)
    }
}
