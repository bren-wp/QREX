package com.brendigo.qrex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prevent sensitive QR payloads leaking into Compose LazyColumn keys. */
class QREXHistoryUiKeyTest {
    @Test
    fun wifiPasswordsAndNetworkNamesNeverBecomeRowKeys() {
        val secret = "private-guest-password-2026"
        val record = QRRecord(QRContent.wifi("Office WiFi", secret), 1_760_000_000L, true)
        val key = QRHistory.uiRowKey(record, 0)
        assertEquals("1760000000:0", key)
        assertFalse(key.contains(secret))
        assertFalse(key.contains("Office"))
        assertFalse(key.contains("WIFI"))
    }

    @Test
    fun rowsWithEqualTimestampsStillHaveDistinctKeys() {
        val first = QRRecord("https://example.org/first", 42L, false)
        val second = QRRecord("https://example.org/second", 42L, true)
        assertNotEquals(QRHistory.uiRowKey(first, 0), QRHistory.uiRowKey(second, 1))
        assertEquals(QRHistory.uiRowKey(first, 0), QRHistory.uiRowKey(first, 0))
    }

    @Test
    fun arbitraryPersonalContentIsNotEmbeddedInKey() {
        val raw = "name@example.com private meeting notes"
        val key = QRHistory.uiRowKey(QRRecord(raw, 600L, false), 3)
        assertEquals("600:3", key)
        assertFalse(key.contains(raw))
        assertTrue(key.length < raw.length)
    }
}
