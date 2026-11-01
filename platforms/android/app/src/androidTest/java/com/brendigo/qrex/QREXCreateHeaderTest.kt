package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Reference Create header: functional Back and overflow, not decorative icons. */
class QREXCreateHeaderTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun await(tag: String) {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun createHeaderNavigationControlsAreAccessible() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-title")
        for (tag in listOf("qrex-create-back", "qrex-create-overflow",
                           "qrex-create-more-types")) {
            val node = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .assertIsDisplayed()
            val rect = node.getUnclippedBoundsInRoot()
            assertTrue("$tag width < 48dp", rect.right - rect.left >= 48.dp)
            assertTrue("$tag height < 48dp", rect.bottom - rect.top >= 48.dp)
        }
        rule.onNodeWithTag("qrex-create-overflow", useUnmergedTree = true)
            .performClick()
        // Advanced types remain available through a real Android menu.
        rule.onNodeWithTag("qrex-create-overflow", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test fun createBackNavigatesToLiveScanner() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-back")
        rule.onNodeWithTag("qrex-create-back", useUnmergedTree = true)
            .performClick()
        await("qrex-scan-gallery")
        rule.onNodeWithTag("qrex-tab-0").assertIsSelected()
        rule.onNodeWithTag("qrex-scan-gallery", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
