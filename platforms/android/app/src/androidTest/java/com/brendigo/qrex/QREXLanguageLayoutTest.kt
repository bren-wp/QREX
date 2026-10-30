package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Language screen 1:1 layout regression: genuine Compose rows and selection.
 * Does not log user content or create a QR entry.
 */
class QREXLanguageLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun openLanguageScreen() {
        rule.waitUntil(timeoutMillis = 15_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        rule.onNodeWithTag("qrex-more-language", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("qrex-language-screen").assertIsDisplayed()
    }

    @Test
    fun languageRowsAreAlignedScrollableAndHeaderStaysAccessible() {
        openLanguageScreen()
        val list = rule.onNodeWithTag("qrex-language-list", useUnmergedTree = true)
            .assertIsDisplayed()
        val tags = listOf("system", "en", "hr", "de")
        val bounds = tags.map { code ->
            rule.onNodeWithTag("qrex-language-option-$code", useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
        }
        bounds.forEachIndexed { index, rect ->
            assertTrue("language row must be at least 48dp tall", rect.bottom - rect.top >= 48.dp)
            assertTrue("language row must be at least 48dp wide", rect.right - rect.left >= 48.dp)
            assertTrue("language row must start at the same x",
                kotlin.math.abs((rect.left - bounds.first().left).value) <= 2f)
            assertTrue("language row must use the same width",
                kotlin.math.abs(((rect.right - rect.left) -
                    (bounds.first().right - bounds.first().left)).value) <= 2f)
            if (index > 0) assertTrue("language rows must not overlap",
                bounds[index - 1].bottom <= rect.top)
        }
        // Scroll to the last of the 23 languages (after the System option).
        // A pinned header must remain reachable even when the list scrolls.
        list.performScrollToIndex(QREXStrings.languages.size)
        val lastCode = QREXStrings.languages.last()
        rule.onNodeWithTag("qrex-language-option-$lastCode", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-detail-back", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
    }

    @Test
    fun languageSelectionPersistsAndCanBeRestoredWithoutClearingHistory() {
        val context = rule.activity.applicationContext
        val original = QREXStore(context).language
        val beforeCount = QREXStore(context).items.size
        val selectedCode = if (original == "hr") "de" else "hr"
        try {
            openLanguageScreen()
            rule.onNodeWithTag("qrex-language-option-$selectedCode", useUnmergedTree = true)
                .performClick()
            rule.onNodeWithTag("qrex-language-option-$selectedCode", useUnmergedTree = true)
                .assertIsSelected()
            assertEquals("chosen language must be durable", selectedCode,
                QREXStore(context).language)
            assertEquals("language selection must not change QR records",
                beforeCount, QREXStore(context).items.size)
        } finally {
            // Restore only the language setting. Never clear all preferences
            // or history in a UI test, even if an assertion fails.
            assertTrue("restore previous language",
                QREXStore(context).updateLanguage(original))
        }
    }
}
