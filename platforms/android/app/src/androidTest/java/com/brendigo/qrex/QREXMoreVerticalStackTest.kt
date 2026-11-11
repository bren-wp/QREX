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

    private fun assertReferenceOrAccessibleLayout() {
        rule.onNodeWithTag("qrex-more-settings-stack", useUnmergedTree = true)
            .assertExists()
        rule.onNodeWithTag("qrex-more-reference-grid", useUnmergedTree = true)
            .assertExists()
        rule.onAllNodesWithTag("qrex-more-icon-plate", useUnmergedTree = true)
            .assertCountEquals(7)

        val config = rule.activity.resources.configuration
        val columns = qrexMoreTileColumns(
            config.screenWidthDp, config.screenHeightDp, config.fontScale
        )
        // Match the tested production tile minimum after v1.0.43's
        // reference layout reduction; keep the 48dp touch target checks.
        val minHeight = if (columns == 2) 104.dp else 72.dp
        val minPx = with(rule.density) { minHeight.roundToPx() }

        tags.forEach { tag ->
            val node = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .performScrollTo().assertIsDisplayed()
            assertTrue("$tag has accessible full-card touch height",
                node.fetchSemanticsNode().size.height >= minPx ||
                    tag == "qrex-more-delete")
        }
        if (columns == 2) {
            // Six functional tiles: Theme/Language, Export/Privacy, Terms/About.
            for (pair in 0..2) {
                val left = rule.onNodeWithTag(tags[pair * 2], useUnmergedTree = true)
                    .performScrollTo().getUnclippedBoundsInRoot()
                val right = rule.onNodeWithTag(tags[pair * 2 + 1], useUnmergedTree = true)
                    .performScrollTo().getUnclippedBoundsInRoot()
                assertTrue("reference tile pair must be side by side",
                    left.right <= right.left)
                assertTrue("reference tile pair must share top alignment",
                    abs((left.top - right.top).value) <= 2f)
                assertTrue("reference tile widths must match",
                    abs(((left.right - left.left) -
                        (right.right - right.left)).value) <= 2f)
            }
        } else {
            for (index in 1..5) {
                val previous = rule.onNodeWithTag(tags[index - 1], useUnmergedTree = true)
                    .performScrollTo().getUnclippedBoundsInRoot()
                val next = rule.onNodeWithTag(tags[index], useUnmergedTree = true)
                    .performScrollTo().getUnclippedBoundsInRoot()
                assertTrue("compact rows must not overlap", previous.bottom <= next.top)
            }
        }
        rule.onNodeWithTag("qrex-more-delete", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        val delete = rule.onNodeWithTag("qrex-more-delete", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val about = rule.onNodeWithTag("qrex-more-about", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue("delete must stay below the six settings cards",
            delete.top >= about.bottom)
    }

    @Test
    fun clearHistoryIsFullyAboveBottomNavigationWithoutScrollingOnStandardPhone() {
        openMore()
        val config = rule.activity.resources.configuration
        if (config.screenWidthDp < 360 || config.screenHeightDp < 650 ||
            config.fontScale > 1.15f ||
            qrexMoreTileColumns(config.screenWidthDp, config.screenHeightDp, config.fontScale) != 2) {
            return // The accessible single-column layout is intentionally scrollable.
        }
        val delete = rule.onNodeWithTag("qrex-more-delete", useUnmergedTree = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val nav = rule.onNodeWithTag("qrex-bottom-navigation", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue("Clear history must be fully visible above navigation on reference phones",
            delete.bottom <= nav.top)
    }

    @Test
    fun darkAndLightMoreKeepReferenceTilesAndWorkingPrivacyTermsActions() {
        val context = rule.activity.applicationContext
        val original = QREXStore(context).themeMode
        val before = QREXStore(context).items.toList()
        try {
            openMore()
            for (mode in listOf("dark", "light")) {
                setAppearance(mode)
                assertReferenceOrAccessibleLayout()
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
