package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Scanner controls must be operable with or without camera permission. */
class QREXScannerControlsTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun galleryAndTorchMaintainAtLeast48DpTargets() {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag("qrex-scan-gallery", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("qrex-scanner-local-message", useUnmergedTree = true)
            .assertIsDisplayed()
        for (tag in listOf("qrex-scan-gallery", "qrex-scan-torch")) {
            val button = rule.onNodeWithTag(tag, useUnmergedTree = true)
                .assertIsDisplayed()
            val bounds = button.getUnclippedBoundsInRoot()
            assertTrue("$tag width must be >= 48dp", bounds.right - bounds.left >= 48.dp)
            assertTrue("$tag height must be >= 48dp", bounds.bottom - bounds.top >= 48.dp)
        }
        // Camera access is intentionally not requested by this test. If not
        // granted, both actions must remain present with the permission CTA.
        val request = rule.onAllNodesWithTag("qrex-enable-camera", useUnmergedTree = true)
            .fetchSemanticsNodes()
        if (request.isNotEmpty()) {
            val bounds = rule.onNodeWithTag("qrex-open-app-settings", useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            assertTrue(bounds.bottom - bounds.top >= 48.dp)
        } else {
            rule.onNodeWithTag("qrex-scan-controls", useUnmergedTree = true)
                .assertIsDisplayed()
            rule.onNodeWithTag("qrex-scan-hint", useUnmergedTree = true)
                .assertIsDisplayed()
            rule.onNodeWithTag("qrex-scan-hint-card", useUnmergedTree = true)
                .assertIsDisplayed()
            // The reference includes a floating flash action in the
            // camera top-right, with the same 48dp accessibility minimum.
            val floating = rule.onNodeWithTag(
                "qrex-scan-floating-torch", useUnmergedTree = true
            ).assertIsDisplayed()
            val floatingBounds = floating.getUnclippedBoundsInRoot()
            assertTrue(floatingBounds.right - floatingBounds.left >= 48.dp)
            assertTrue(floatingBounds.bottom - floatingBounds.top >= 48.dp)

            val preview = rule.onNodeWithTag("qrex-camera-preview", useUnmergedTree = true)
                .assertIsDisplayed().getUnclippedBoundsInRoot()
            val brackets = rule.onNodeWithTag("qrex-scan-brackets", useUnmergedTree = true)
                .assertIsDisplayed().getUnclippedBoundsInRoot()
            // Overlay must match the actual PreviewView bounds, never become
            // a static bitmap or move outside the rounded camera container.
            assertTrue(brackets.left >= preview.left - 1.dp)
            assertTrue(brackets.right <= preview.right + 1.dp)
            assertTrue(brackets.top >= preview.top - 1.dp)
            assertTrue(brackets.bottom <= preview.bottom + 1.dp)
            assertTrue(floatingBounds.left >= preview.left)
            assertTrue(floatingBounds.right <= preview.right)
        }
    }
}
