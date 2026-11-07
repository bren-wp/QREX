package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.compose.setContent
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The production Compose splash, not a pre-rendered marketing phone image. */
class QREXSplashReferenceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun brandAndFooterAreVisibleAndDoNotOverlapOnReferenceViewport() {
        // The real MainActivity is declared in AndroidManifest. Use its
        // Compose host instead of the undeclared default ComponentActivity
        // (which previously failed on API 26 and 31 before any assertions).
        rule.activity.runOnUiThread {
            rule.activity.setContent {
                QrexSplashScreen(subtitle = "Skener. Stvara. Dijeli.")
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("qrex-splash-native-waves", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-splash-brand-halo", useUnmergedTree = true)
            .assertIsDisplayed()
        val logo = rule.onNodeWithTag("qrex-splash-logo", useUnmergedTree = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val title = rule.onNodeWithTag("qrex-splash-wordmark", useUnmergedTree = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val slogan = rule.onNodeWithTag("qrex-splash-tagline", useUnmergedTree = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val footer = rule.onNodeWithTag("qrex-splash-footer", useUnmergedTree = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val progress = rule.onNodeWithTag("qrex-splash-progress", useUnmergedTree = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val viewport = rule.onRoot().getUnclippedBoundsInRoot()

        assertTrue("Logo and wordmark overlap", logo.bottom <= title.top)
        assertTrue("Wordmark and slogan overlap", title.bottom <= slogan.top)
        assertTrue("Slogan and footer overlap", slogan.bottom <= footer.top)
        val trackWidth = progress.right - progress.left
        val viewportWidth = viewport.right - viewport.left
        assertTrue("Track exceeds available width", trackWidth < viewportWidth)
        assertTrue("Track too small versus supplied reference", trackWidth >= 80.dp)
        assertTrue("Footer must remain above viewport bottom", footer.bottom <= viewport.bottom)
        val scrollLayout = rule.onAllNodesWithTag(
            "qrex-splash-viewport-distribution", useUnmergedTree = true
        ).fetchSemanticsNodes()
        if (scrollLayout.isNotEmpty()) {
            // The original v1.0.38 genuine capture put the progress indicator
            // far above the bottom of a compact 1080×1920 screen, leaving a
            // large empty region. A scroll layout that fits must retain the
            // same bottom anchoring as the regular centered reference layout.
            val bottomGap = viewport.bottom - footer.bottom
            assertTrue(
                "Compact splash footer must be near the safe viewport bottom, gap=$bottomGap",
                bottomGap >= 0.dp && bottomGap <= 64.dp
            )
            rule.onNodeWithTag("qrex-splash-viewport-distribution", useUnmergedTree = true)
                .assertExists()
        }
    }
}
