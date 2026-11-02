package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The production Compose splash, not a pre-rendered marketing phone image. */
class QREXSplashReferenceTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun brandAndFooterAreVisibleAndDoNotOverlapOnReferenceViewport() {
        rule.setContent {
            QrexSplashScreen(subtitle = "Skener. Stvara. Dijeli.")
        }
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
    }
}
