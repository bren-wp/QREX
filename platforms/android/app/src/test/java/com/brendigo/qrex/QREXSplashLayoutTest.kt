package com.brendigo.qrex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure geometry checks complement real Android screenshot / accessibility QA. */
class QREXSplashLayoutTest {
    @Test
    fun referenceSizedDeviceRetainsCenteredSplashGeometry() {
        val metrics = qrexSplashMetrics(393, 852, 1f)
        assertEquals(214, metrics.logoDp)
        assertEquals(60, metrics.titleSp)
        assertEquals(40, metrics.bottomPaddingDp)
        assertFalse(qrexSplashUsesScrollLayout(852, 1f))
    }

    @Test
    fun compactDeviceUsesNonOverlappingScrollableLayout() {
        val metrics = qrexSplashMetrics(320, 568, 1f)
        assertEquals(156, metrics.logoDp)
        assertEquals(50, metrics.titleSp)
        assertTrue(qrexSplashUsesScrollLayout(568, 1f))
    }

    @Test
    fun progressTrackFollowsReferenceRatioWithoutOverflow() {
        // Footer content width = display width minus horizontal spacing.
        assertEquals(166, qrexSplashProgressWidthDp(333))
        assertEquals(140, qrexSplashProgressWidthDp(280))
        assertEquals(184, qrexSplashProgressWidthDp(800))
        assertEquals(70, qrexSplashProgressWidthDp(70))
        assertEquals(0, qrexSplashProgressWidthDp(0))
    }

    @Test
    fun enlargedAccessibilityTextForcesScrollEvenOnTallDevices() {
        val metrics = qrexSplashMetrics(393, 852, 1.6f)
        assertEquals(38, metrics.titleSp)
        assertTrue(qrexSplashUsesScrollLayout(852, 1.6f))
        assertTrue(qrexSplashUsesScrollLayout(568, 2f))
    }

    @Test
    fun thresholdsAreDeterministic() {
        assertTrue(qrexSplashUsesScrollLayout(639, 1f))
        assertFalse(qrexSplashUsesScrollLayout(640, 1f))
        assertTrue(qrexSplashUsesScrollLayout(640, 1.5f))
        assertFalse(qrexSplashUsesScrollLayout(640, 1.49f))
    }
    @Test
    fun footerUsesReferenceTwoLineWrapWithoutBreakingCompactOrLocalisedText() {
        val message = "Sve što može QR — jednostavno, brzo i bez prijave."
        assertEquals(
            "Sve što može QR —\\njednostavno, brzo i bez prijave.",
            qrexSplashFooterLines(message, compactWidth = false)
        )
        assertEquals(message, qrexSplashFooterLines(message, compactWidth = true))
        assertEquals("Other languages remain unchanged",
            qrexSplashFooterLines("Other languages remain unchanged", false))
        assertEquals("Already\\nwrapped", qrexSplashFooterLines("Already\\nwrapped", false))
        assertEquals(40, qrexSplashMetrics(393, 852, 1f).bottomPaddingDp)
        assertEquals(20, qrexSplashMetrics(320, 568, 1f).bottomPaddingDp)
    }

    @Test
    fun localisedSplashAccentsOnlyTheMiddleBrandWord() {
        val croatian = "Skener. Stvara. Dijeli."
        val english = "Scan. Create. Share."
        assertEquals("Stvara.", croatian.substring(qrexSplashAccentRange(croatian)!!))
        assertEquals("Create.", english.substring(qrexSplashAccentRange(english)!!))
        assertNull(qrexSplashAccentRange("Only two"))
        assertNull(qrexSplashAccentRange("One two three four"))
        assertNull(qrexSplashAccentRange(""))
    }

}
