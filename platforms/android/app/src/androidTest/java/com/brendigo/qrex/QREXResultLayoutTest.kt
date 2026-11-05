package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
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

/** Functional, layout-only test: never reads or prints private QR payloads. */
class QREXResultLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun compactResultActionsAndFiveTabsNeverOverlap() {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag("qrex-tab-1", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-1").performClick()
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performTextReplacement("https://example.org/qrex/layout-test")
        rule.waitUntil(timeoutMillis = 20_000L) {
            runCatching {
                rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
                    .assertIsEnabled()
            }.isSuccess
        }
        rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true).performClick()
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag("qrex-result-summary", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        val nav = rule.onNodeWithTag(
            "qrex-bottom-navigation", useUnmergedTree = true
        ).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Navigation must be inset above system buttons", nav.bottom > nav.top)
        for (index in 0..4) {
            rule.onNodeWithTag("qrex-tab-$index", useUnmergedTree = true)
                .assertIsDisplayed()
            val glyph = rule.onNodeWithTag(
                "qrex-nav-glyph-$index", useUnmergedTree = true
            ).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue("Navigation icon $index must be visible", glyph.right > glyph.left)
        }
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()

        val card = rule.onNodeWithTag(
            "qrex-result-summary", useUnmergedTree = true
        ).assertIsDisplayed().getUnclippedBoundsInRoot()
        val glyph = rule.onNodeWithTag(
            "qrex-result-glyph", useUnmergedTree = true
        ).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Result icon must stay inside the summary",
            glyph.top >= card.top && glyph.bottom <= card.bottom)
        assertTrue("Summary must fit the reference-style compact layout",
            card.bottom - card.top <= 250.dp)
        val open = rule.onNodeWithTag(
            "qrex-result-primary", useUnmergedTree = true
        ).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Primary action should fit above navigation", open.bottom <= nav.top)

        // Verify long action stacks scroll instead of covering a navigation tab.
        val favorite = rule.onNodeWithTag(
            "qrex-result-save-favorite", useUnmergedTree = true
        ).performScrollTo().assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Favorite action must scroll above persistent navigation",
            favorite.bottom <= nav.top)
        rule.onNodeWithTag("qrex-result-close", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
