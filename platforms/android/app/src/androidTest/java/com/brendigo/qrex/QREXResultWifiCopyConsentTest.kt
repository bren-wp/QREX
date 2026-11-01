package com.brendigo.qrex

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Rule
import org.junit.Test

/**
 * Copying a password-bearing Wi-Fi QR requires explicit opt-in.
 * The test never reads the clipboard or logs user/payload contents.
 */
class QREXResultWifiCopyConsentTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun await(tag: String) {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openSyntheticWifiResult() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-type-2")
        rule.onNodeWithTag("qrex-create-type-2", useUnmergedTree = true)
            .performScrollTo().performClick()
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performScrollTo().performTextReplacement("QREX-Privacy-Test")
        rule.onNodeWithTag("qrex-create-wifi-password", useUnmergedTree = true)
            .performScrollTo().performTextReplacement("fixture-only")
        await("qrex-create-show")
        rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnClick)
        await("qrex-result-screen")
    }

    @Test fun copyActionRequiresConsentAndCanCancelWithoutLeavingResult() {
        openSyntheticWifiResult()
        rule.onNodeWithTag("qrex-result-copy", useUnmergedTree = true)
            .performScrollTo().performClick()
        rule.onNodeWithTag("qrex-result-wifi-copy-warning", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-result-wifi-copy-cancel", useUnmergedTree = true)
            .performClick()
        rule.onNodeWithTag("qrex-result-wifi-copy-warning", useUnmergedTree = true)
            .assertDoesNotExist()
        rule.onNodeWithTag("qrex-result-screen").assertIsDisplayed()
        // Consent is not sticky; a second attempt requires another dialog.
        rule.onNodeWithTag("qrex-result-copy", useUnmergedTree = true)
            .performScrollTo().performClick()
        rule.onNodeWithTag("qrex-result-wifi-copy-warning", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test fun overflowMenuCopyUsesTheSameConsentGate() {
        openSyntheticWifiResult()
        rule.onNodeWithTag("qrex-result-menu", useUnmergedTree = true)
            .performClick()
        await("qrex-result-menu-copy")
        rule.onNodeWithTag("qrex-result-menu-copy", useUnmergedTree = true)
            .performClick()
        rule.onNodeWithTag("qrex-result-wifi-copy-warning", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-result-wifi-copy-cancel", useUnmergedTree = true)
            .performClick()
        rule.onNodeWithTag("qrex-result-screen").assertIsDisplayed()
    }
}
