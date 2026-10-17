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

    @Test fun historyFilteringPreservesFavoritesAndMatchesPlainContent() {
        val favorite = QRRecord("Important meeting", 1L, true)
        val scan = QRRecord("Shopping list", 2L, false)
        assertTrue(QRHistory.matches(favorite, "meeting", true))
        assertTrue(QRHistory.matches(favorite, "IMPORTANT", true))
        assertFalse(QRHistory.matches(scan, "shopping", true))
        assertTrue(QRHistory.matches(scan, "shopping", false))
        assertFalse(QRHistory.matches(favorite, "missing", false))
    }

    @Test fun wifiHistorySearchCannotMatchPasswords() {
        val secret = QRRecord(QRContent.wifi("Guest WiFi", "verySecretPass"), 1L, true)
        assertTrue(QRHistory.matches(secret, "guest", true))
        assertFalse(QRHistory.matches(secret, "verySecretPass", false))
    }

    @Test fun longQrIsRejected() {
        assertFalse(QRContent.canRender(""))
        assertFalse(QRContent.canRender("x".repeat(1501)))
        assertTrue(QRContent.canRender("hello"))
    }
    @Test fun whitespaceWifiPasswordIsNotMistakenForAnOpenNetwork() {
        val raw = QRContent.wifi("Private", "        ")
        assertEquals("WPA", QRContent.wifiField(raw, "T"))
        assertEquals("        ", QRContent.wifiField(raw, "P"))
        assertEquals("nopass", QRContent.wifiField(QRContent.wifi("Public", ""), "T"))
    }

    @Test fun galleryImagesAreSampledBeforeMlKitDecoding() {
        assertEquals(1, gallerySampleSize(1600, 1200))
        assertEquals(2, gallerySampleSize(4000, 3000))
        assertEquals(2, gallerySampleSize(6000, 4000))
        assertEquals(4, gallerySampleSize(6000, 6000))
    }

    @Test fun responsivePreviewNeverOverflowsCompactAndroidViewport() {
        assertEquals(238f, responsiveQrSideDp(420f, 238f), 0.01f)
        assertEquals(238f, responsiveQrSideDp(238f, 238f), 0.01f)
        assertEquals(176f, responsiveQrSideDp(176f, 238f), 0.01f)
        assertEquals(0f, responsiveQrSideDp(-20f, 238f), 0.01f)
        assertEquals(230f, responsiveQrSideDp(400f, 230f), 0.01f)
    }

    @Test fun wifiQrClipboardCopiesAreSensitiveButNormalQrTextIsNot() {
        assertTrue(qrClipboardSensitive(QRContent.wifi("Office", "private-password")))
        assertTrue(qrClipboardSensitive("wifi:T:WPA;S:Guest;P:secret;;"))
        assertFalse(qrClipboardSensitive("https://brendigo.com"))
        assertFalse(qrClipboardSensitive("Normal QR message"))
    }

    @Test fun whitespaceOnlyQrCannotBeRendered() {
        assertFalse(QRContent.canRender("   "))
        assertFalse(QRContent.canRender("\n\t"))
    }

    @Test fun wifiEscapesDoubleQuotesAcrossPlatforms() {
        val ssid = "Guest\"Room"
        val password = "a\\b\"c;d"
        val raw = QRContent.wifi(ssid, password)
        assertEquals(ssid, QRContent.wifiField(raw, "S"))
        assertEquals(password, QRContent.wifiField(raw, "P"))
    }

}
