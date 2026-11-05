package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Rule
import org.junit.Test

/** Reference Create header: functional Back and overflow, not decorative icons. */
class QREXCreateHeaderTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun await(tag: String) {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun createHeaderNavigationControlsAreAccessible() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-title")
        // Default Create has a clean subtitle. Advanced actions are opened
        // from the functional overflow, not from a competing inline button.
        for (tag in listOf("qrex-create-back", "qrex-create-overflow")) {
            val node = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .assertIsDisplayed()
            val rect = node.getUnclippedBoundsInRoot()
            assertTrue("$tag width < 48dp", rect.right - rect.left >= 48.dp)
            assertTrue("$tag height < 48dp", rect.bottom - rect.top >= 48.dp)
        }
        rule.onNodeWithTag("qrex-create-overflow", useUnmergedTree = true)
            .performClick()
        // Select the real E-mail overflow action; this must reveal its
        // advanced type chip and not just dismiss an inert popup.
        await("qrex-create-overflow-type-3")
        rule.onNodeWithTag("qrex-create-overflow-type-3", useUnmergedTree = true)
            .performClick()
        await("qrex-create-type-3")
        rule.onNodeWithTag("qrex-create-type-3", useUnmergedTree = true)
            .assertIsDisplayed()
        await("qrex-create-more-types")
        val collapse = rule.onNodeWithTag(
            "qrex-create-more-types", useUnmergedTree = true
        ).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Advanced collapse width < 48dp", collapse.right - collapse.left >= 48.dp)
        assertTrue("Advanced collapse height < 48dp", collapse.bottom - collapse.top >= 48.dp)
    }

    @Test fun createBackNavigatesToLiveScanner() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-back")
        rule.onNodeWithTag("qrex-create-back", useUnmergedTree = true)
            .performClick()
        await("qrex-scan-gallery")
        rule.onNodeWithTag("qrex-tab-0").assertIsSelected()
        rule.onNodeWithTag("qrex-scan-gallery", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test fun wifiPasswordRevealIsAccessibleAndChangesActualLabel() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-type-2")
        rule.onNodeWithTag("qrex-create-type-2", useUnmergedTree = true)
            .performClick()
        await("qrex-create-wifi-password")
        rule.onNodeWithTag("qrex-create-wifi-password", useUnmergedTree = true)
            .assertIsDisplayed()
        val toggle = rule.onNodeWithTag("qrex-wifi-visibility", useUnmergedTree = true)
            .assertIsDisplayed()
        val rect = toggle.getUnclippedBoundsInRoot()
        assertTrue("Wi-Fi visibility action must be at least 48dp high",
            rect.bottom - rect.top >= 48.dp)
        fun label(): String = rule.onNodeWithTag(
            "qrex-wifi-visibility-label", useUnmergedTree = true
        ).fetchSemanticsNode().config[SemanticsProperties.Text]
            .joinToString(separator = "") { it.text }
        val initiallyMasked = label()
        toggle.performClick()
        rule.waitForIdle()
        assertNotEquals("Wi-Fi visibility toggle must change its actual label",
            initiallyMasked, label())
        rule.onNodeWithTag("qrex-wifi-visibility", useUnmergedTree = true)
            .performClick()
        rule.waitForIdle()
        assertTrue("Visibility toggle must restore its original masked mode",
            initiallyMasked == label())
    }
}
