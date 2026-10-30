package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
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

/** Real Compose geometry and working callbacks on the light More reference. */
class QREXMoreThemeLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun lightMoreUsesSixNonOverlappingFullWidthRowsAndSafeDeleteConfirmation() {
        val context = rule.activity.applicationContext
        val store = QREXStore(context)
        val original = store.themeMode
        val originalHistoryCount = store.items.size
        try {
            rule.waitUntil(timeoutMillis = 15_000L) {
                rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("qrex-tab-4").performClick()
            rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()

            rule.onNodeWithTag("qrex-more-theme", useUnmergedTree = true).performClick()
            rule.onNodeWithTag("qrex-theme-dialog", useUnmergedTree = true).assertIsDisplayed()
            rule.onNodeWithTag("qrex-theme-choice-light", useUnmergedTree = true)
                .performClick()

            val tags = listOf(
                "qrex-more-theme", "qrex-more-language", "qrex-more-export",
                "qrex-more-privacy", "qrex-more-terms", "qrex-more-about"
            )
            rule.waitForIdle()
            val positions = tags.map { tag ->
                rule.onNodeWithTag(tag, useUnmergedTree = true)
                    .getUnclippedBoundsInRoot()
            }
            val width = positions.first().right - positions.first().left
            for ((index, rect) in positions.withIndex()) {
                assertTrue("light row must have >=48dp width", rect.right - rect.left >= 48.dp)
                assertTrue("light row must have >=48dp height", rect.bottom - rect.top >= 48.dp)
                assertTrue("all light rows share a leading edge",
                    kotlin.math.abs((rect.left - positions.first().left).value) <= 2f)
                assertTrue("all light rows fill the same available width",
                    kotlin.math.abs(((rect.right - rect.left) - width).value) <= 2f)
                if (index > 0) {
                    assertTrue("light More rows must not overlap",
                        positions[index - 1].bottom <= rect.top)
                }
            }

            // Every action is real. Confirm a destructive action requires a
            // separate user decision; cancel without clearing anything.
            rule.onNodeWithTag("qrex-more-delete", useUnmergedTree = true)
                .performScrollTo().performClick()
            rule.onNodeWithTag("qrex-clear-history-dialog", useUnmergedTree = true)
                .assertIsDisplayed()
            rule.onNodeWithTag("qrex-clear-history-cancel", useUnmergedTree = true)
                .assertIsDisplayed().performClick()
            rule.onNodeWithTag("qrex-clear-history-dialog", useUnmergedTree = true)
                .assertDoesNotExist()
            assertTrue("cancelling clear history must not delete records",
                QREXStore(context).items.size == originalHistoryCount)
        } finally {
            // Do not leave a different app theme for other instrumentation tests.
            assertTrue("original theme must be restored",
                QREXStore(context).updateThemeMode(original))
        }
    }
}
