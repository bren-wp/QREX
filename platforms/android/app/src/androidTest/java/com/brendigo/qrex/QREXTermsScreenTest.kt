package com.brendigo.qrex

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Terms stays inside QREX; opening the external policy requires a user tap. */
class QREXTermsScreenTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun termsDisclosureScrollsAndCanCloseWithoutOpeningBrowserOrChangingHistory() {
        val context = rule.activity.applicationContext
        val before = QREXStore(context).items.toList()
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        rule.onNodeWithTag("qrex-more-terms", useUnmergedTree = true)
            .performScrollTo().performClick()

        rule.onNodeWithTag("qrex-terms-dialog", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-terms-content", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-terms-summary", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-terms-summary-card", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-terms-icon-plate", useUnmergedTree = true)
            .assertIsDisplayed()

        for (tag in listOf("qrex-terms-close", "qrex-terms-full-document")) {
            val bounds = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue("$tag must have a 48dp touch target width",
                bounds.right - bounds.left >= 48.dp)
            assertTrue("$tag must have a 48dp touch target height",
                bounds.bottom - bounds.top >= 48.dp)
        }

        // Never activate the browser link during instrumentation; Close must
        // dismiss native UI with no change to any saved QR history.
        rule.onNodeWithTag("qrex-terms-close", useUnmergedTree = true)
            .performClick()
        rule.onAllNodesWithTag("qrex-terms-dialog", useUnmergedTree = true)
            .assertCountEquals(0)
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
        assertTrue("Reading Terms may not change QR history",
            QREXStore(context).items.toList() == before)
    }
}
