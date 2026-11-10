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

    @Test fun resultRetainsReferenceNavigationAndAllFunctionalActions() {
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

        // The latest supplied Result reference has the same five bottom tabs.
        // Their selected page tracks the actual source of the QR result.
        rule.onNodeWithTag("qrex-bottom-navigation", useUnmergedTree = true)
            .assertIsDisplayed()
        for (index in 0..4) {
            rule.onNodeWithTag("qrex-tab-$index", useUnmergedTree = true)
                .assertIsDisplayed()
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
            card.bottom - card.top <= 260.dp)
        // On wide Android windows the Result remains a centered, phone-like
        // readable panel; on small phones it still fills the available width.
        val viewport = rule.onNodeWithTag(
            "qrex-result-screen", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        assertTrue("Result card must remain capped to a readable tablet width",
            card.right - card.left <= 520.dp)
        if (viewport.right - viewport.left >= 600.dp) {
            val centerDelta = kotlin.math.abs(
                (card.left.value + card.right.value) -
                    (viewport.left.value + viewport.right.value)
            )
            assertTrue("Tablet result panel must be horizontally centered", centerDelta < 4f)
        }
        val open = rule.onNodeWithTag(
            "qrex-result-primary", useUnmergedTree = true
        ).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Primary action must fit inside Result viewport",
            open.bottom <= viewport.bottom)
        rule.onNodeWithTag("qrex-result-url-origin", useUnmergedTree = true)
            .assertIsDisplayed()

        // Bottom navigation must never hide Favorite/Delete. The Result
        // scroll viewport remains independent when IME/font sizes increase.
        val favorite = rule.onNodeWithTag(
            "qrex-result-save-favorite", useUnmergedTree = true
        ).performScrollTo().assertIsDisplayed().getUnclippedBoundsInRoot()
        val activeViewport = rule.onNodeWithTag(
            "qrex-result-screen", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        assertTrue("Favorite must stay inside Result viewport",
            favorite.bottom <= activeViewport.bottom)
        rule.onNodeWithTag("qrex-result-delete", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("qrex-result-menu", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("qrex-result-menu-edit", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        // Edit is not removed; it returns to Create and its real tabs.
        rule.onNodeWithTag("qrex-result-screen", useUnmergedTree = true)
            .assertDoesNotExist()
        rule.onNodeWithTag("qrex-bottom-navigation", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
