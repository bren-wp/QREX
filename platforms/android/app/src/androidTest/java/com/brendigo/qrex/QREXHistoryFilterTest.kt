package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

        val filter = rule.onNodeWithTag("qrex-history-filter-toggle", useUnmergedTree = true)
        val search = rule.onNodeWithTag("qrex-history-search", useUnmergedTree = true)
        filter.assertIsDisplayed()
        search.assertIsDisplayed()
        // A reference-style search and filter share one aligned row. Both
        // controls remain separate, non-overlapping touch targets.
        val searchBounds = search.getUnclippedBoundsInRoot()
        val filterBounds = filter.getUnclippedBoundsInRoot()
        assertTrue("search and filter must not overlap",
            searchBounds.right <= filterBounds.left)
        assertTrue("search and filter vertical centers must align",
            kotlin.math.abs(((searchBounds.top + searchBounds.bottom) / 2 -
                (filterBounds.top + filterBounds.bottom) / 2).value) <= 2f)
        val initialFilterState =
            filter.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        for (tag in listOf("qrex-history-filter-toggle", "qrex-history-search")) {
            val area = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            assertTrue("$tag must be at least 48dp wide", area.right - area.left >= 48.dp)
            assertTrue("$tag must be at least 48dp tall", area.bottom - area.top >= 48.dp)
        }
        filter.performClick()
        val all = rule.onNodeWithTag("qrex-history-all", useUnmergedTree = true)
        val saved = rule.onNodeWithTag("qrex-history-saved", useUnmergedTree = true)
        all.assertIsDisplayed().assertIsSelected()
        saved.assertIsDisplayed()
        saved.performClick()
        val savedFilterState =
            filter.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertNotEquals("selected filter must be announced", initialFilterState, savedFilterState)
        filter.performClick()
        saved.assertIsDisplayed().assertIsSelected()
        all.performClick()
        assertEquals("All filter must restore original state description",
            initialFilterState,
            filter.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        filter.performClick()
        all.assertIsDisplayed().assertIsSelected()
    }
}
