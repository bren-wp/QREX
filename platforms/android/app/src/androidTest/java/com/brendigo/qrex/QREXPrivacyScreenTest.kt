package com.brendigo.qrex

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Privacy dialog controls must be real, accessible and non-destructive. */
class QREXPrivacyScreenTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun openPrivacy() {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        rule.onNodeWithTag("qrex-more-privacy", useUnmergedTree = true)
            .performScrollTo().performClick()
        rule.onNodeWithTag("qrex-privacy-dialog", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun disclosuresScrollButCloseAndFullPolicyStayAccessible() {
        openPrivacy()
        rule.onNodeWithTag("qrex-privacy-summary", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("qrex-privacy-history-row", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Primary actions are outside the scroll region; all are native
        // click targets and must remain at least 48dp in both dimensions.
        for (tag in listOf("qrex-privacy-close", "qrex-privacy-full-policy")) {
            val button = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .assertIsDisplayed()
            val bounds = button.getUnclippedBoundsInRoot()
            assertTrue("$tag should be at least 48dp wide",
                bounds.right - bounds.left >= 48.dp)
            assertTrue("$tag should be at least 48dp high",
                bounds.bottom - bounds.top >= 48.dp)
        }
        // Never open a browser as part of privacy smoke tests.
        rule.onNodeWithTag("qrex-privacy-close", useUnmergedTree = true)
            .performClick()
        rule.onAllNodesWithTag("qrex-privacy-dialog", useUnmergedTree = true)
            .assertCountEquals(0)
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
    }

    @Test
    fun historyTogglePersistsWithoutDeletingAnyQrAndCanBeReverted() {
        val context = rule.activity.applicationContext
        val originalStore = QREXStore(context)
        val original = originalStore.historyEnabled
        val records = originalStore.items.toList()
        try {
            openPrivacy()
            val toggle = rule.onNodeWithTag(
                "qrex-privacy-history-switch", useUnmergedTree = true
            ).performScrollTo().assertIsDisplayed()
            val bounds = toggle.getUnclippedBoundsInRoot()
            assertTrue("history switch width should be at least 48dp",
                bounds.right - bounds.left >= 48.dp)
            assertTrue("history switch height should be at least 48dp",
                bounds.bottom - bounds.top >= 48.dp)
            if (original) toggle.assertIsOn() else toggle.assertIsOff()

            toggle.performClick()
            rule.waitForIdle()
            if (original) toggle.assertIsOff() else toggle.assertIsOn()
            assertEquals("history setting should survive a new Store instance",
                !original, QREXStore(context).historyEnabled)
            // Never show user payloads in failing test output.
            assertTrue("disabling history must preserve all existing QR records",
                QREXStore(context).items.toList() == records)

            toggle.performClick()
            rule.waitForIdle()
            if (original) toggle.assertIsOn() else toggle.assertIsOff()
            assertEquals("history option should be restored", original,
                QREXStore(context).historyEnabled)
        } finally {
            assertTrue("restore only the History preference",
                QREXStore(context).updateHistoryEnabled(original))
        }
    }
}
