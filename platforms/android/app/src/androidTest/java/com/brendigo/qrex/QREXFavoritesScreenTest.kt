package com.brendigo.qrex

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Real UI navigation for Favorites. Uses a unique, synthetic public URL;
 * never reads or logs the user's QR payloads.
 */
class QREXFavoritesScreenTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun awaitTag(tag: String) {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun favoritesHasOneDirectRemoveActionAndRetainsHistoryOnUnsave() {
        awaitTag("qrex-tab-1")
        val host = "qrex-favorite-${System.nanoTime().toString().removePrefix("-")}.example.org"
        val url = "https://$host/test"

        // Generate with the same real Compose entry point used by customers.
        rule.onNodeWithTag("qrex-tab-1").performClick()
        awaitTag("qrex-create-content")
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performTextReplacement(url)
        rule.waitUntil(timeoutMillis = 45_000L) {
            runCatching {
                rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
                    .assertIsEnabled()
            }.isSuccess
        }
        rule.onNodeWithTag("qrex-create-show").performClick()
        awaitTag("qrex-result-save-favorite")
        rule.onNodeWithTag("qrex-result-save-favorite", useUnmergedTree = true)
            .performScrollTo().performClick()

        rule.onNodeWithTag("qrex-tab-3").performClick()
        awaitTag("qrex-favorites-screen")
        // Favorite-specific UI must not repeat the History dropdown/filter.
        rule.onAllNodesWithTag("qrex-history-menu", useUnmergedTree = true)
            .assertCountEquals(0)
        rule.onAllNodesWithTag("qrex-history-filter-toggle", useUnmergedTree = true)
            .assertCountEquals(0)

        rule.onNodeWithTag("qrex-history-search", useUnmergedTree = true)
            .performTextReplacement(host)
        val row = rule.onNodeWithTag("qrex-favorites-row", useUnmergedTree = true)
        row.assertIsDisplayed().performClick()
        awaitTag("qrex-result-screen")
        rule.onNodeWithTag("qrex-result-close", useUnmergedTree = true).performClick()
        awaitTag("qrex-favorites-screen")

        val remove = rule.onNodeWithTag("qrex-favorites-remove", useUnmergedTree = true)
        remove.assertIsDisplayed()
        val bounds = remove.getUnclippedBoundsInRoot()
        assertTrue("Favorites removal must remain at least 48dp wide",
            bounds.right - bounds.left >= 48.dp)
        assertTrue("Favorites removal must remain at least 48dp tall",
            bounds.bottom - bounds.top >= 48.dp)
        remove.performClick()

        // Only the saved flag changes: there is no destructive delete or
        // irreversible disappearance from the local History record.
        rule.onAllNodesWithTag("qrex-favorites-row", useUnmergedTree = true)
            .assertCountEquals(0)
        rule.onNodeWithTag("qrex-favorites-empty").assertIsDisplayed()
        rule.onNodeWithTag("qrex-tab-2").performClick()
        awaitTag("qrex-history-screen")
        rule.onNodeWithTag("qrex-history-search", useUnmergedTree = true)
            .performTextReplacement(host)
        rule.onNodeWithTag("qrex-history-row", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
