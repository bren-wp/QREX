package com.brendigo.qrex

import org.junit.Assert.*
import org.junit.Test

class QREXExternalActionsTest {
    @Test fun resultOffersNativeActionsForAllSupportedStructuredQrFormats() {
        assertEquals(QrexExternalKind.EMAIL, qrexExternalKind("mailto:help@brendigo.com"))
        assertEquals(QrexExternalKind.PHONE, qrexExternalKind("tel:+385 91 123 4567"))
        assertEquals(QrexExternalKind.LOCATION, qrexExternalKind("geo:45.3271,14.4422"))
        assertEquals(QrexExternalKind.CONTACT, qrexExternalKind(
            QRContent.contact("Ana Kovačić", "+385911234567", "ana@example.hr")!!))
        assertNull(qrexExternalKind("plain text"))
        assertNull(qrexExternalKind("https://brendigo.com"))
    }

    @Test fun nativeActionsRejectUsSdHeaderInjectionAndMalformedAddresses() {
        assertNull(qrexExternalKind("tel:*21*12345#"))
        assertNull(qrexExternalKind("tel:+385123\n456"))
        assertNull(qrexExternalKind("mailto:attacker@example.org,second@example.org"))
        assertNull(qrexExternalKind("mailto:attacker@example.org\nBcc:someone@example.org"))
        assertNull(qrexExternalKind("mailto:nobody"))
        assertNull(qrexExternalKind("geo:91,14"))
        assertNull(qrexExternalKind("geo:45,181"))
        assertNull(qrexExternalKind("geo:NaN,Infinity"))
        assertNull(qrexExternalKind("geo:45,14?q=Rijeka&extra=unexpected"))
        assertNull(qrexExternalKind("geo:45,14?q=%0A"))
        assertNull(qrexExternalKind("BEGIN:VCARD\nVERSION:3.0\nEND:VCARD"))
    }

    @Test fun mapSearchGeneratorEscapesPlacesAndRestoresEditableInput() {
        val address = "Rijeka, Trg 128. brigade Hrvatske vojske"
        val generated = QRContent.location(address)!!
        assertTrue(generated.startsWith("geo:0,0?q="))
        assertEquals(QrexExternalKind.LOCATION, qrexExternalKind(generated))
        assertEquals(address, QRContent.locationInput(generated))
        assertEquals(generated, QRContent.location(QRContent.locationInput(generated)))
        assertEquals("geo:45.3271,14.4422", QRContent.location("45.3271,14.4422"))
        assertNull(QRContent.location(" \n "))
        assertNull(QRContent.location(""))
    }

    @Test fun eventReaderSupportsParametersAndFoldedExternalIcalendarLines() {
        val raw = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n" +
            "SUMMARY;LANGUAGE=hr:Projekt Brendigo\r\n sastanak\r\n" +
            "LOCATION;LANGUAGE=hr:Rijeka\r\n" +
            "DTSTART:20261110T100000Z\r\nDTEND:20261110T110000Z\r\n" +
            "END:VEVENT\r\nEND:VCALENDAR"
        assertEquals("Projekt Brendigosastanak", QRContent.eventField(raw, "SUMMARY"))
        assertEquals("Rijeka", QRContent.eventField(raw, "LOCATION"))
        assertNull(QRContent.eventField(raw, "ORGANIZER"))
    }
}
