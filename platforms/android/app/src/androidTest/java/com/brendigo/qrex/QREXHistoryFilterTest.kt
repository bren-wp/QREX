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

/** Real Compose checks for the reference History All/Saved selector. */
class QREXHistoryFilterTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun historyAllSavedAndSearchHaveAccessibleMeasuredTargets() {
        rule.waitUntil(timeoutMillis = 15_000L) {
            rule.onAllNodesWithTag("qrex-tab-2", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-2").performClick()
        rule.onNodeWithTag("qrex-history-screen").assertIsDisplayed()

        val all = rule.onNodeWithTag("qrex-history-all", useUnmergedTree = true)
        val saved = rule.onNodeWithTag("qrex-history-saved", useUnmergedTree = true)
        val search = rule.onNodeWithTag("qrex-history-search", useUnmergedTree = true)
        all.assertIsDisplayed().assertIsSelected()
        saved.assertIsDisplayed()
        search.assertIsDisplayed()

        for (tag in listOf("qrex-history-all", "qrex-history-saved", "qrex-history-search")) {
            val area = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            val width = area.right - area.left
            val height = area.bottom - area.top
            assertTrue("$tag must be at least 48dp wide", width >= 48.dp)
            assertTrue("$tag must be at least 48dp tall", height >= 48.dp)
        }

        saved.performClick().assertIsSelected()
        all.performClick().assertIsSelected()
    }
}
