package com.brendigo.qrex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Result rows remain compact but accessible at every supported font scale. */
class QREXResultLayoutProfileTest {
    @Test fun resultColumnIsUnchangedOnSmallPhonesAndCappedOnTablets() {
        assertEquals(1f, qrexResultContentFraction(320), 0.0001f)
        assertEquals(1f, qrexResultContentFraction(393), 0.0001f)
        assertEquals(1f, qrexResultContentFraction(520), 0.0001f)
        assertEquals(0.65f, qrexResultContentFraction(800), 0.0001f)
        assertEquals(0.5078125f, qrexResultContentFraction(1024), 0.0001f)
        assertTrue(qrexResultContentFraction(1) <= 1f)
    }

    @Test fun normalFontUsesNative52DpActionTargets() {
        assertEquals(52, qrexResultActionHeightDp(1f))
        assertEquals(52, qrexResultActionHeightDp(1.20f))
        assertEquals(52, qrexResultActionHeightDp(1.34f))
        assertTrue(qrexResultActionHeightDp(1f) >= 48)
    }

    @Test fun largerFontReceives60DpWithoutCollapsingActionRows() {
        assertEquals(60, qrexResultActionHeightDp(1.35f))
        assertEquals(60, qrexResultActionHeightDp(1.60f))
        assertEquals(60, qrexResultActionHeightDp(2.00f))
        assertTrue(qrexResultActionHeightDp(2f) > qrexResultActionHeightDp(1f))
    }
}
