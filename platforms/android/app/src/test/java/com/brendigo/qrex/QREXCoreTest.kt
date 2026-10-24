package com.brendigo.qrex

import org.junit.Assert.*
import org.junit.Test

class QREXCoreTest {
    @Test fun languageChoicesContainEnglishAndCroatian() {
        assertEquals(23, QREXStrings.languages.size)
        assertEquals("en", QREXStrings.languages.first())
        assertTrue("hr" in QREXStrings.languages)
    }


    @Test fun responsivePhoneLayoutProfilesKeepActionsAvailable() {
        val compact = qrexLayoutProfile(320, 568, 1.0f)
        assertTrue(compact.stackTypeChoices)
        assertTrue(compact.showQrPreview)
        assertEquals(104, compact.darkQrSizeDp)
        assertEquals(12, compact.horizontalPaddingDp)

        val veryShort = qrexLayoutProfile(320, 400, 1.0f)
        assertFalse(veryShort.showQrPreview)
        assertTrue(veryShort.stackTypeChoices)

        val normal = qrexLayoutProfile(393, 852, 1.0f)
        assertFalse(normal.stackTypeChoices)
        assertEquals(156, normal.darkQrSizeDp)

        val largeText = qrexLayoutProfile(393, 852, 1.6f)
        assertTrue(largeText.stackTypeChoices)
        assertEquals(104, largeText.darkQrSizeDp)
        assertFalse(qrexLayoutProfile(393, 852, 1.8f).showQrPreview)
    }

    @Test fun eventDatePickerDoesNotCrashAcrossShorterMonths() {
        val endOfJanuary = java.time.LocalDateTime.of(2026, 1, 31, 23, 45)
        assertEquals(
            java.time.LocalDateTime.of(2026, 2, 28, 23, 45),
            eventDateTimeWithDate(endOfJanuary, 2026, 1, 28)
        )
        assertEquals(
            java.time.LocalDateTime.of(2028, 2, 29, 23, 45),
            eventDateTimeWithDate(endOfJanuary, 2028, 1, 29)
        )
    }

    @Test fun eventDatePickerPreservesTimeAndSwitchesYearSafely() {
        val leapDay = java.time.LocalDateTime.of(2028, 2, 29, 14, 7, 18)
        assertEquals(
            java.time.LocalDateTime.of(2029, 3, 1, 14, 7, 18),
            eventDateTimeWithDate(leapDay, 2029, 2, 1)
        )
    }

    @Test fun eventQrRoundTripRestoresUtcTimesAndEscapedFields() {
        val start = java.time.LocalDateTime.of(2026, 10, 25, 9, 30)
        val end = start.plusHours(2)
        val title = "Sastanak, Šime"
        val location = "Rijeka; Croatia"
        val description = "Prva linija\\nDruga, linija"
        val encoded = QRContent.event(title, start, end, location, description)!!
        assertTrue(encoded.contains("\\r\\nDTSTART:"))
        assertEquals(title, QRContent.eventField(encoded, "SUMMARY"))
        assertEquals(location, QRContent.eventField(encoded, "LOCATION"))
        assertEquals(description, QRContent.eventField(encoded, "DESCRIPTION"))
        assertEquals(
            start.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            QRContent.eventTimeMillis(encoded, "DTSTART")
        )
        assertEquals(
            end.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            QRContent.eventTimeMillis(encoded, "DTEND")
        )
        assertNull(QRContent.eventTimeMillis(encoded, "NOT_A_TIME_FIELD"))
    }

    @Test fun contactVcardRoundTripKeepsCroatianUnicodeAndEscapedDelimiters() {
        val name = "Šime, Željka; Kovačić"
        val phone = "+385 91 123 4567"
        val email = "kontakt@example.hr"
        val card = QRContent.contact(name, phone, email)
        assertNotNull(card)
        assertTrue(card!!.contains("VERSION:3.0"))
        assertEquals(name, QRContent.contactField(card, "FN"))
        assertEquals(phone, QRContent.contactField(card, "TEL"))
        assertEquals(email, QRContent.contactField(card, "EMAIL"))
        assertEquals(name, QRContent.label(card))
        assertTrue(QRContent.canRender(card))
        assertNull(QRContent.contact("   "))
        assertNull(QRContent.contactField("ordinary text", "FN"))
    }

    @Test fun contactNameNewlinesCannotInjectVcardFields() {
        val name = "Ana\nBEGIN:VCARD"
        val card = QRContent.contact(name)!!
        assertEquals(1, card.lines().count { it == "BEGIN:VCARD" })
        assertEquals(name, QRContent.contactField(card, "FN"))
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

    @Test fun validResultNavigationDoesNotRequireAnAllocatedPreviewBitmap() {
        // This is intentionally pure: a cold emulator's render performance
        // must not gate navigation for a valid, bounded QR payload.
        assertTrue(canOpenQrResult("https://brendigo.com"))
        assertTrue(canOpenQrResult(QRContent.wifi("Office", "private")))
        assertFalse(canOpenQrResult(""))
        assertFalse(canOpenQrResult("x".repeat(1501)))
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

    @Test fun historyLabelDisplaysWifiNameWithoutExposingPassword() {
        val ssid = "Guest Network ".repeat(12)
        val secret = "sensitive-wifi-password"
        val record = QRContent.wifi(ssid, secret)
        assertEquals(ssid, QRContent.label(record))
        assertFalse(QRContent.label(record).contains(secret))
        assertTrue(QRHistory.matches(QRRecord(record, 1L, true), "Guest", false))
        assertFalse(QRHistory.matches(QRRecord(record, 1L, true), secret, false))
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
