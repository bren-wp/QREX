package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** About must show QREX details without navigating away to a website. */
class QREXMoreAboutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun darkReferenceMoreUsesSixRealCardsInAlignedThreeByTwoGrid() {
        rule.waitUntil(timeoutMillis = 15_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        val tags = listOf(
            "qrex-more-theme", "qrex-more-language", "qrex-more-export",
            "qrex-more-privacy", "qrex-more-terms", "qrex-more-about"
        )
        tags.forEach { tag ->
            val bounds = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            assertTrue("$tag needs >=48dp width", bounds.right - bounds.left >= 48.dp)
            assertTrue("$tag needs >=48dp height", bounds.bottom - bounds.top >= 48.dp)
        }

        // On standard-width phones the dark reference has three rows of two
        // evenly sized native tiles; narrow/large-text layouts intentionally
        // switch to a vertical list to prevent overlapping or clipped text.
        val config = rule.activity.resources.configuration
        if (config.screenWidthDp >= 360 && config.fontScale < 1.30f) {
            val positions = tags.map { tag ->
                rule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
            }
            for (row in 0..2) {
                val left = positions[row * 2]
                val right = positions[row * 2 + 1]
                assertTrue("reference row top must align",
                    kotlin.math.abs((left.top - right.top).value) < 2f)
                assertTrue("reference pair must not overlap", left.right <= right.left)
                assertTrue("reference pair needs equal widths",
                    kotlin.math.abs(((left.right - left.left) -
                        (right.right - right.left)).value) < 2f)
                if (row > 0) {
                    assertTrue("reference rows must not overlap",
                        positions[(row - 1) * 2].bottom <= left.top)
                }
            }
        }
    }

    @Test
    fun moreDeleteActionIsVisibleWithoutScrollingOnStandardPhone() {
        rule.waitUntil(timeoutMillis = 15_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
        // Main More is a single screen on a reference-size Android phone.
        // Lists of 23 languages/history records can still scroll by design.
        rule.onNodeWithTag("qrex-more-delete", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun aboutOpensNativeDialogAndKeepsWebsiteAnExplicitSecondaryAction() {
        rule.waitUntil(timeoutMillis = 15_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
        val about = rule.onNodeWithTag("qrex-more-about", useUnmergedTree = true)
            .assertIsDisplayed()
        val rect = about.getUnclippedBoundsInRoot()
        assertTrue(rect.right - rect.left >= 48.dp)
        assertTrue(rect.bottom - rect.top >= 48.dp)

        about.performClick()
        rule.onNodeWithTag("qrex-about-dialog", useUnmergedTree = true)
            .assertIsDisplayed()
        // Do not open the browser in test. Verify it is a real optional button,
        // rather than the only behavior attached to the About tile.
        rule.onNodeWithTag("qrex-about-website", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
