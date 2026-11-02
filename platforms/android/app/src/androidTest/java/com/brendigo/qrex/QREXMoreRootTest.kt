package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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

/** Real More root controls; no external URL, deletion or preference change. */
class QREXMoreRootTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun await(tag: String) {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openMore() {
        await("qrex-tab-4")
        rule.onNodeWithTag("qrex-tab-4").performClick()
        await("qrex-more-screen")
    }

    private fun assertTarget(tag: String) {
        val node = rule.onNodeWithTag(tag, useUnmergedTree = true)
            .assertIsDisplayed()
        val bounds = node.getUnclippedBoundsInRoot()
        assertTrue("$tag must be 48dp high", bounds.bottom - bounds.top >= 48.dp)
    }

    @Test fun themeDialogShowsActualSelectedModeAndClosesWithoutSaving() {
        val initial = QREXStore(rule.activity.applicationContext).themeMode
        openMore()
        rule.onNodeWithTag("qrex-more-theme", useUnmergedTree = true)
            .performScrollTo().performClick()
        await("qrex-theme-dialog")
        for (mode in listOf("system", "dark", "light")) {
            assertTarget("qrex-theme-choice-$mode")
        }
        rule.onNodeWithTag("qrex-theme-choice-$initial", useUnmergedTree = true)
            .assertIsSelected()
        assertTarget("qrex-theme-close")
        rule.onNodeWithTag("qrex-theme-close", useUnmergedTree = true)
            .performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
        assertTrue("Dismissing theme must not change preference",
            QREXStore(rule.activity.applicationContext).themeMode == initial)
    }

    @Test fun clearHistoryDialogCancelDoesNotDeleteAnyRecords() {
        val before = QREXStore(rule.activity.applicationContext).items.toList()
        openMore()
        rule.onNodeWithTag("qrex-more-delete", useUnmergedTree = true)
            .performScrollTo().performClick()
        await("qrex-clear-history-dialog")
        assertTarget("qrex-clear-history-confirm")
        assertTarget("qrex-clear-history-cancel")
        rule.onNodeWithTag("qrex-clear-history-cancel", useUnmergedTree = true)
            .performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
        assertTrue("Cancelling Clear History must preserve records",
            QREXStore(rule.activity.applicationContext).items.toList() == before)
    }
}
