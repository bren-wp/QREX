package com.brendigo.qrex

import org.junit.Assert.assertEquals
import org.junit.Test

/** Form data must round-trip without silently changing the Wi-Fi network. */
class QREXCreateInputTest {
    @Test fun wifiSsidPreservesLeadingAndTrailingSpaces() {
        val original = "  Office Wi-Fi  "
        val normalized = createQrInputText(2, original)
        assertEquals(original, normalized)
        val payload = QRContent.wifi(normalized, "synthetic-test-only-password")
        assertEquals(original, QRContent.wifiField(payload, "S"))
    }

    @Test fun nonWifiInputStillRemovesAccidentalOuterWhitespace() {
        assertEquals("https://example.org/", createQrInputText(0, "  https://example.org/  "))
        assertEquals("hello", createQrInputText(1, "\nhello\n"))
        assertEquals("test@example.org", createQrInputText(3, " test@example.org "))
    }

    @Test fun whitespaceOnlyWifiNameRemainsInvalidForGeneration() {
        val input = createQrInputText(2, "    ")
        assertEquals(true, input.isBlank())
        // The screen keeps its explicit non-blank SSID guard.
    }
}
