package com.brendigo.qrex

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure responsive-layout checks: no emulator, device or QR data required. */
class QREXVisualLayoutTest {
    @Test fun settingsCardMinHeightIsIndependentOfScreenClass() {
        // The previous grid-versus-row switch no longer exists. Actual
        // seven-card stacking is tested in QREXMoreVerticalStackTest.
        assertEquals(72, qrexSettingsCardHeightDp(1.0f))
        assertEquals(88, qrexSettingsCardHeightDp(1.45f))
        assertTrue(qrexSettingsCardHeightDp(1.45f) >= qrexSettingsCardHeightDp(1.0f))
    }

    @Test fun createChoicesRemainAccessibleAtLargeFontAndCompactWidth() {
        assertFalse(qrexLayoutProfile(430, 850, 1.0f).stackTypeChoices)
        assertFalse(qrexLayoutProfile(393, 850, 1.0f).stackTypeChoices)
        assertFalse(qrexLayoutProfile(360, 850, 1.0f).stackTypeChoices)
        assertTrue(qrexLayoutProfile(320, 850, 1.0f).stackTypeChoices)
        assertTrue(qrexLayoutProfile(430, 850, 1.30f).stackTypeChoices)
        assertEquals(104, qrexLayoutProfile(320, 600, 1.6f).darkQrSizeDp)
    }
}
