package com.brendigo.qrex

import org.junit.Assert.*
import org.junit.Test

class QREXCoreTest {
    @Test fun languageChoicesContainEnglishAndCroatian() {
        assertEquals(23, QREXStrings.languages.size)
        assertEquals("en", QREXStrings.languages.first())
        assertTrue("hr" in QREXStrings.languages)
    }


    @Test fun wifiEscapesDelimiters() {
        val raw = QRContent.wifi("Office;P:fake", "a\\b:c")
        assertEquals("Office;P:fake", QRContent.wifiField(raw, "S"))
        assertEquals("a\\b:c", QRContent.wifiField(raw, "P"))
    }

    @Test fun longQrIsRejected() {
        assertFalse(QRContent.canRender(""))
        assertFalse(QRContent.canRender("x".repeat(1501)))
        assertTrue(QRContent.canRender("hello"))
    }
    @Test fun wifiEscapesDoubleQuotesAcrossPlatforms() {
        val ssid = "Guest\"Room"
        val password = "a\\b\"c;d"
        val raw = QRContent.wifi(ssid, password)
        assertEquals(ssid, QRContent.wifiField(raw, "S"))
        assertEquals(password, QRContent.wifiField(raw, "P"))
    }

}
