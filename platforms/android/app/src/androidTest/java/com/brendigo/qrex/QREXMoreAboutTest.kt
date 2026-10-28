package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** About must show QREX details without navigating away to a website. */
class QREXMoreAboutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun aboutOpensNativeDialogAndKeepsWebsiteAnExplicitSecondaryAction() {
        rule.waitUntil(timeoutMillis = 15_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
        val about = rule.onNodeWithTag("qrex-more-about", useUnmergedTree = true)
            .assertIsDisplayed()
        val rect = about.getUnclippedBoundsInRoot()
        assertTrue(rect.right - rect.left >= 48.dp)
        assertTrue(rect.bottom - rect.top >= 48.dp)

        about.performClick()
        rule.onNodeWithTag("qrex-about-dialog", useUnmergedTree = true)
            .assertIsDisplayed()
        // Do not open the browser in test. Verify it is a real optional button,
        // rather than the only behavior attached to the About tile.
        rule.onNodeWithTag("qrex-about-website", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
