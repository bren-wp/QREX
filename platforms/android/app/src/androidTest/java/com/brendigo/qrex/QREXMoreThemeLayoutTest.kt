package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
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

/** The dark-only More grid preserves all five settings and safe deletion. */
class QREXMoreThemeLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun darkOnlyMoreGridHasNoThemeControlAndCancelKeepsHistory() {
        val context = rule.activity.applicationContext
        val before = QREXStore(context).items.size
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
        rule.onAllNodesWithTag("qrex-more-theme", useUnmergedTree = true)
            .assertCountEquals(0)
        val tags = listOf("qrex-more-language", "qrex-more-export",
            "qrex-more-privacy", "qrex-more-terms", "qrex-more-about")
        val cfg = rule.activity.resources.configuration
        val columns = qrexMoreTileColumns(cfg.screenWidthDp, cfg.screenHeightDp,
            cfg.fontScale)
        val positions = tags.map { tag ->
            rule.onNodeWithTag(tag, useUnmergedTree = true)
                .performScrollTo().assertIsDisplayed().getUnclippedBoundsInRoot()
        }
        for ((index, rect) in positions.withIndex()) {
            assertTrue("Dark settings card must have a 48dp touch target",
                rect.right - rect.left >= 48.dp && rect.bottom - rect.top >= 48.dp)
            if (index > 0 && columns == 2 && index % 2 == 1) {
                assertTrue("Settings pair must share a row",
                    kotlin.math.abs((positions[index - 1].top - rect.top).value) <= 2f)
            }
        }
        rule.onNodeWithTag("qrex-more-delete", useUnmergedTree = true)
            .performScrollTo().performClick()
        rule.onNodeWithTag("qrex-clear-history-dialog", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-clear-history-cancel", useUnmergedTree = true)
            .performClick()
        rule.onAllNodesWithTag("qrex-clear-history-dialog", useUnmergedTree = true)
            .assertCountEquals(0)
        assertTrue(QREXStore(context).items.size == before)
    }
}
