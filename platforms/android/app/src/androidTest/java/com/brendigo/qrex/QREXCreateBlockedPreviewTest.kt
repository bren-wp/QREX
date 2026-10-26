package com.brendigo.qrex

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Regresses the report that an empty preview covers Generate on the Create tab.
 * Uses physical Compose tap dispatch, not synthetic callback invocation.
 */
class QREXCreateBlockedPreviewTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun emptyPreviewDoesNotHideActionsAndValidUrlOpensResult() {
        rule.waitUntil(timeoutMillis = 15_000L) {
            rule.onAllNodesWithTag("qrex-tab-1", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-1").performClick()
        rule.onNodeWithTag("qrex-create-screen").assertIsDisplayed()

        // An empty placeholder must not steal vertical space or touch input.
        rule.onNodeWithTag("qrex-create-preview-card", useUnmergedTree = true)
            .assertDoesNotExist()
        rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
            .assertIsDisplayed().assertIsNotEnabled()
        val save = rule.onNodeWithTag("qrex-create-save", useUnmergedTree = true)
            .assertIsDisplayed()
        val bounds = save.getUnclippedBoundsInRoot()
        assertTrue("Save must be at least 48dp high", bounds.bottom - bounds.top >= 48.dp)

        // A valid value must enable the pinned button immediately, without
        // waiting for a QR bitmap and even if the keyboard is still showing.
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performTextReplacement("https://example.org/create-from-small-screen")
        rule.waitUntil(timeoutMillis = 15_000L) {
            runCatching {
                rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
                    .assertIsEnabled()
            }.isSuccess
        }
        rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("qrex-result-screen").assertIsDisplayed()
    }
}
