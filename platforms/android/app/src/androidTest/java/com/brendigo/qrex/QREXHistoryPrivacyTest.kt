package com.brendigo.qrex

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** History search/filter/delete: real Compose UI and only synthetic QR records. */
class QREXHistoryPrivacyTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun awaitTag(tag: String) {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun historySearchFilterAndDeleteArePrivateAndCancellable() {
        val host = "qrex-history-${System.nanoTime().toString().removePrefix("-")}.example.org"
        val secretMarker = "synthetic-private-query-marker"
        val raw = "https://$host/example?token=$secretMarker"
        try {
            awaitTag("qrex-tab-1")
            rule.onNodeWithTag("qrex-tab-1").performClick()
            awaitTag("qrex-create-content")
            rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
                .performTextReplacement(raw)
            awaitTag("qrex-create-show")
            rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnClick)
            awaitTag("qrex-result-screen")
            rule.onNodeWithTag("qrex-tab-2").performClick()
            awaitTag("qrex-history-search")
            rule.onNodeWithTag("qrex-history-search", useUnmergedTree = true)
                .performTextReplacement(host)
            awaitTag("qrex-history-row")
            // The category plate is an actual rendered Compose vector surface,
            // not a static illustration covering the working History row.
            val glyph = rule.onNodeWithTag("qrex-history-category-glyph", useUnmergedTree = true)
                .assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue("Category icon must remain legible", glyph.right - glyph.left >= 38.dp)
            rule.onAllNodesWithText(secretMarker, substring = true, useUnmergedTree = true)
                .assertCountEquals(0)

            // A non-saved QR must disappear under Saved and return under All.
            rule.onNodeWithTag("qrex-history-filter-toggle", useUnmergedTree = true)
                .performClick()
            awaitTag("qrex-history-saved")
            rule.onNodeWithTag("qrex-history-saved", useUnmergedTree = true)
                .performClick()
            rule.onAllNodesWithTag("qrex-history-row", useUnmergedTree = true)
                .assertCountEquals(0)
            rule.onNodeWithTag("qrex-history-filter-toggle", useUnmergedTree = true)
                .performClick()
            awaitTag("qrex-history-all")
            rule.onNodeWithTag("qrex-history-all", useUnmergedTree = true)
                .performClick()
            awaitTag("qrex-history-row")

            rule.onNodeWithTag("qrex-history-menu", useUnmergedTree = true)
                .performClick()
            awaitTag("qrex-history-menu-delete")
            rule.onNodeWithTag("qrex-history-menu-delete", useUnmergedTree = true)
                .performClick()
            awaitTag("qrex-history-delete-dialog")
            for (tag in listOf("qrex-history-delete-confirm", "qrex-history-delete-cancel")) {
                val bounds = rule.onNodeWithTag(tag, useUnmergedTree = true)
                    .assertIsDisplayed().getUnclippedBoundsInRoot()
                assertTrue("$tag must have 48dp touch height",
                    bounds.bottom - bounds.top >= 48.dp)
            }
            rule.onNodeWithTag("qrex-history-delete-cancel", useUnmergedTree = true)
                .performClick()
            rule.onNodeWithTag("qrex-history-row", useUnmergedTree = true)
                .assertIsDisplayed()

            rule.onNodeWithTag("qrex-history-menu", useUnmergedTree = true)
                .performClick()
            rule.onNodeWithTag("qrex-history-menu-delete", useUnmergedTree = true)
                .performClick()
            rule.onNodeWithTag("qrex-history-delete-confirm", useUnmergedTree = true)
                .performClick()
            rule.onAllNodesWithTag("qrex-history-row", useUnmergedTree = true)
                .assertCountEquals(0)
        } finally {
            // Cleanup affects only this synthetic record, even on test failure.
            val store = QREXStore(rule.activity.applicationContext)
            store.items.firstOrNull { it.raw == raw }?.let { record ->
                assertTrue("Synthetic QR cleanup must succeed", store.remove(record))
            }
        }
    }
}
