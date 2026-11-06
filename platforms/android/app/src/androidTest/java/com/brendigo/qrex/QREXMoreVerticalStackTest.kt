package com.brendigo.qrex

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
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
import kotlin.math.abs

/** Real Android geometry for both appearances; not a promotional screenshot. */
class QREXMoreVerticalStackTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val tags = listOf(
        "qrex-more-theme", "qrex-more-language", "qrex-more-export",
        "qrex-more-privacy", "qrex-more-terms", "qrex-more-about",
        "qrex-more-delete"
    )

    private fun openMore() {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
    }

    private fun setAppearance(mode: String) {
        rule.onNodeWithTag("qrex-more-theme", useUnmergedTree = true)
            .performScrollTo().performClick()
        rule.onNodeWithTag("qrex-theme-dialog", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-theme-choice-$mode", useUnmergedTree = true)
            .performClick()
        rule.waitForIdle()
    }

    private fun assertStackLayout() {
        rule.onNodeWithTag("qrex-more-settings-stack", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onAllNodesWithTag("qrex-more-icon-plate", useUnmergedTree = true)
            .assertCountEquals(7)
        val cards = tags.map {
            rule.onNodeWithTag(it, useUnmergedTree = true).getUnclippedBoundsInRoot()
        }
        val left = cards.first().left
        val width = cards.first().right - left
        for ((index, rect) in cards.withIndex()) {
            assertTrue("card ${tags[index]} must be >= 72dp high",
                rect.bottom - rect.top >= 72.dp)
            assertTrue("full-width cards must align on the left",
                abs((rect.left - left).value) <= 2f)
            assertTrue("all cards must have the same width",
                abs(((rect.right - rect.left) - width).value) <= 2f)
            if (index > 0) {
                assertTrue("cards must not overlap, including terms and privacy",
                    cards[index - 1].bottom <= rect.top)
            }
        }
        assertTrue("destructive history action must be separated",
            cards.last().top - cards[cards.lastIndex - 1].bottom >= 16.dp)
    }

    @Test
    fun darkAndLightMoreKeepSevenFullWidthCardsAndWorkingPrivacyTermsActions() {
        val context = rule.activity.applicationContext
        val original = QREXStore(context).themeMode
        val before = QREXStore(context).items.toList()
        try {
            openMore()
            for (mode in listOf("dark", "light")) {
                setAppearance(mode)
                assertStackLayout()
            }
            rule.onNodeWithTag("qrex-more-privacy", useUnmergedTree = true)
                .performScrollTo().performClick()
            rule.onNodeWithTag("qrex-privacy-dialog", useUnmergedTree = true)
                .assertIsDisplayed()
            rule.onNodeWithTag("qrex-privacy-close", useUnmergedTree = true)
                .performClick()
            rule.onNodeWithTag("qrex-more-terms", useUnmergedTree = true)
                .performScrollTo().performClick()
            rule.onNodeWithTag("qrex-terms-dialog", useUnmergedTree = true)
                .assertIsDisplayed()
            rule.onNodeWithTag("qrex-terms-close", useUnmergedTree = true)
                .performClick()
            assertTrue("opening policy cards must never mutate history",
                QREXStore(context).items.toList() == before)
        } finally {
            assertTrue("restore theme for unrelated emulator tests",
                QREXStore(context).updateThemeMode(original))
        }
    }
}
