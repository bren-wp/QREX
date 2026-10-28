package com.brendigo.qrex

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test

/**
 * Regression: a Result-screen Wi-Fi PNG is a password-bearing export.
 * The SAF picker must never open before the user explicitly approves it.
 * Use fixture credentials only; do not log device history or semantics trees.
 */
class QREXResultWifiPngConsentTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun wifiResultPngNeedsConsentAndCancelKeepsResultOpen() {
        rule.waitUntil(timeoutMillis = 15_000L) {
            rule.onAllNodesWithTag("qrex-tab-1", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-1").performClick()
        rule.onNodeWithTag("qrex-create-type-2", useUnmergedTree = true)
            .performScrollTo().performClick()
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performScrollTo().performTextReplacement("QREX-Consent-Example")
        rule.onNodeWithTag("qrex-create-wifi-password", useUnmergedTree = true)
            .performScrollTo().performTextReplacement("example-only-password")
        rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("qrex-result-screen").assertIsDisplayed()

        // The export button exists only when a real QR preview was rendered.
        rule.onNodeWithTag("qrex-result-toggle-qr", useUnmergedTree = true)
            .performScrollTo().performClick()
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag("qrex-result-export-png", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-result-export-png", useUnmergedTree = true)
            .performScrollTo().performClick()

        rule.onNodeWithTag("qrex-result-wifi-png-warning", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-result-wifi-png-cancel", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("qrex-result-wifi-png-warning", useUnmergedTree = true)
            .assertDoesNotExist()
        rule.onNodeWithTag("qrex-result-screen").assertIsDisplayed()

        // A second attempt must demand consent again, not reuse old approval.
        rule.onNodeWithTag("qrex-result-export-png", useUnmergedTree = true)
            .performScrollTo().performClick()
        rule.onNodeWithTag("qrex-result-wifi-png-warning", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
