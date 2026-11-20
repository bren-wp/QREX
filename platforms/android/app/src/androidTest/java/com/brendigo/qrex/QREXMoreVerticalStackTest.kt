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

/** Dark-only More screen: five settings and separate destructive action. */
class QREXMoreVerticalStackTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val tags = listOf(
        "qrex-more-language", "qrex-more-export", "qrex-more-privacy",
        "qrex-more-terms", "qrex-more-about", "qrex-more-delete"
    )
    private fun openMore() {
        rule.waitUntil(20_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
    }

    @Test fun darkMoreKeepsAccessibleCardsAndSafeDestructiveAction() {
        openMore()
        rule.onAllNodesWithTag("qrex-more-icon-plate", useUnmergedTree = true)
            .assertCountEquals(6)
        val config = rule.activity.resources.configuration
        val columns = qrexMoreTileColumns(
            config.screenWidthDp, config.screenHeightDp, config.fontScale
        )
        tags.forEach { tag ->
            val node = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .performScrollTo().assertIsDisplayed()
            assertTrue("$tag should remain at least 48dp high",
                node.getUnclippedBoundsInRoot().bottom -
                    node.getUnclippedBoundsInRoot().top >= 48.dp)
        }
        if (columns == 2) {
            for (index in listOf(0, 2)) {
                val left = rule.onNodeWithTag(tags[index], useUnmergedTree = true)
                    .performScrollTo().getUnclippedBoundsInRoot()
                val right = rule.onNodeWithTag(tags[index + 1], useUnmergedTree = true)
                    .performScrollTo().getUnclippedBoundsInRoot()
                assertTrue(left.right <= right.left)
                assertTrue(kotlin.math.abs((left.top - right.top).value) <= 2f)
            }
        }
        val delete = rule.onNodeWithTag("qrex-more-delete", useUnmergedTree = true)
            .performScrollTo().getUnclippedBoundsInRoot()
        val about = rule.onNodeWithTag("qrex-more-about", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue("Delete must remain below settings", delete.top >= about.bottom)
    }

    @Test fun privacyAndTermsStillOpenWithoutChangingHistory() {
        val context = rule.activity.applicationContext
        val before = QREXStore(context).items.toList()
        openMore()
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
        assertTrue(QREXStore(context).items.toList() == before)
    }
}
