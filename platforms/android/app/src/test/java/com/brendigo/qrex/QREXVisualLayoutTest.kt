package com.brendigo.qrex

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure responsive-layout checks: no emulator, device or QR data required. */
class QREXVisualLayoutTest {
    @Test fun moreTilesMatchReferenceAndRemainReadableAtSmallSizes() {
        assertEquals(1, qrexMoreTileColumns(320, 568, 1f))
        assertEquals(2, qrexMoreTileColumns(360, 740, 1f))
        assertEquals(2, qrexMoreTileColumns(393, 852, 1f))
        assertEquals(2, qrexMoreTileColumns(800, 1280, 1f))
        assertEquals(1, qrexMoreTileColumns(393, 852, 1.30f))
        assertEquals(1, qrexMoreTileColumns(393, 852, 2f))
        assertEquals(1, qrexMoreTileColumns(800, 420, 1f))
    }

    @Test fun settingsCardMinHeightIsIndependentOfScreenClass() {
        // Full-width accessible rows retain 72dp normal and 88dp large-font targets.
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
