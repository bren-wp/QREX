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

/** About must show QREX details without navigating away to a website. */
class QREXMoreAboutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun darkReferenceMoreUsesSixFunctionalTilesInResponsiveRows() {
        rule.waitUntil(timeoutMillis = 15_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        val tags = listOf("qrex-more-theme", "qrex-more-language",
            "qrex-more-export", "qrex-more-privacy",
            "qrex-more-terms", "qrex-more-about")
        val config = rule.activity.resources.configuration
        val columns = qrexMoreTileColumns(config.screenWidthDp,
            config.screenHeightDp, config.fontScale)
        tags.forEach { tag ->
            val node = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .performScrollTo().assertIsDisplayed()
            assertTrue("Every reference card must be a real touch target",
                node.fetchSemanticsNode().size.height >= with(rule.density) {
                    48.dp.roundToPx()
                })
        }
        val first = rule.onNodeWithTag(tags[0], useUnmergedTree = true)
            .performScrollTo().getUnclippedBoundsInRoot()
        val second = rule.onNodeWithTag(tags[1], useUnmergedTree = true)
            .performScrollTo().getUnclippedBoundsInRoot()
        if (columns == 2) {
            assertTrue("theme and language are in the same reference row",
                kotlin.math.abs((first.top - second.top).value) <= 2f)
            assertTrue("reference pair has distinct non-overlapping columns",
                first.right <= second.left)
        } else {
            assertTrue("large font and narrow phones use non-overlapping rows",
                first.bottom <= second.top)
        }
    }

    @Test
    fun moreDeleteActionRemainsReachableByScrollingOnStandardPhone() {
        rule.waitUntil(timeoutMillis = 15_000L) {
            rule.onAllNodesWithTag("qrex-tab-4", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-tab-4").performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
        // Seven cards intentionally scroll above persistent bottom navigation.
        rule.onNodeWithTag("qrex-more-delete", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
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
            .performScrollTo().assertIsDisplayed()
        val rect = about.getUnclippedBoundsInRoot()
        assertTrue(rect.right - rect.left >= 48.dp)
        assertTrue(rect.bottom - rect.top >= 48.dp)

        about.performClick()
        rule.onNodeWithTag("qrex-about-dialog", useUnmergedTree = true)
            .assertIsDisplayed()
        // Verify actual branding and the currently installed APK version,
        // never an assumed release number or a static mock-up.
        rule.onNodeWithTag("qrex-about-brand", useUnmergedTree = true)
            .assertIsDisplayed()
        @Suppress("DEPRECATION")
        val version = rule.activity.packageManager
            .getPackageInfo(rule.activity.packageName, 0).versionName.orEmpty()
        if (version.isNotBlank()) {
            rule.onNodeWithTag("qrex-about-version", useUnmergedTree = true)
                .assertIsDisplayed()
        }
        rule.onNodeWithTag("qrex-about-tagline", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("qrex-about-domain", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("qrex-about-developer-card", useUnmergedTree = true)
            .assertIsDisplayed()

        // Only the secondary user action may open the website; do not open
        // the browser during this regression. Measure both real controls.
        for (tag in listOf("qrex-about-website", "qrex-about-close")) {
            val control = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .assertIsDisplayed()
            val bounds = control.getUnclippedBoundsInRoot()
            assertTrue("$tag must be at least 48dp wide",
                bounds.right - bounds.left >= 48.dp)
            assertTrue("$tag must be at least 48dp tall",
                bounds.bottom - bounds.top >= 48.dp)
        }
        rule.onNodeWithTag("qrex-about-close", useUnmergedTree = true)
            .performClick()
        rule.onAllNodesWithTag("qrex-about-dialog", useUnmergedTree = true)
            .assertCountEquals(0)
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
        rule.onNodeWithTag("qrex-tab-4").assertIsDisplayed()
    }
}
