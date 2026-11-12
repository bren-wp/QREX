package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Rule
import org.junit.Test

/** Exercises actual Compose controls, never static screenshots or inert illustrations. */
class QREXCreateReferenceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun await(tag: String) {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Keeping these checks separate makes API-specific failures actionable
    // without exposing typed QR values or JUnit exception messages in CI.
    @Test fun urlFieldHasLocalizedAccessibleName() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-content")
        val input = rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
        val label = input.fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
        assertTrue("URL field requires a localized accessible name",
            label.isNotEmpty() && label.first().isNotBlank())
    }

    /**
     * The Android 15 emulator uses a different IME inset lifecycle from the
     * older matrix entries. Finish the actual edit and wait for the keyboard
     * to stop occluding the TextField before evaluating the clear icon.
     * Dedicated test names keep the public P03 report privacy-safe but precise.
     */
    private fun prepareClearUrlTouchTarget() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-content")
        val input = rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
        input.performTextReplacement("https://brendigo.com")
        input.performImeAction()
        rule.waitUntil(timeoutMillis = 15_000L) {
            ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
        rule.waitForIdle()
        await("qrex-create-clear-url")
    }

    @Test fun urlClearGlyphRemainsVisibleAfterImeDone() {
        prepareClearUrlTouchTarget()
        rule.onNodeWithTag("qrex-create-clear-url", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test fun urlClearGlyphHasAtLeast48DpWidth() {
        prepareClearUrlTouchTarget()
        val hit = rule.onNodeWithTag(
            "qrex-create-clear-url", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        assertTrue("Clear URL touch target must be >=48dp wide",
            hit.right - hit.left >= 48.dp)
    }

    @Test fun urlClearGlyphHasAtLeast48DpHeight() {
        prepareClearUrlTouchTarget()
        val hit = rule.onNodeWithTag(
            "qrex-create-clear-url", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        assertTrue("Clear URL touch target must be >=48dp tall",
            hit.bottom - hit.top >= 48.dp)
    }

    @Test fun urlClearGlyphRemovesActualDraftWithoutNavigation() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-content")
        val input = rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)

        // Exercise the real text editor and its Done IME action before the
        // physical clear tap; do not synthesize a state mutation in the test.
        input.performTextReplacement("https://brendigo.com")
        await("qrex-create-clear-url")
        input.performImeAction()
        rule.waitForIdle()
        rule.onNodeWithTag("qrex-create-clear-url", useUnmergedTree = true)
            .assertIsDisplayed().performClick()

        // This only passes when the actual URL field becomes empty and
        // the generate action stops accepting a now-empty payload.
        rule.waitUntil(timeoutMillis = 10_000L) {
            rule.onAllNodesWithTag("qrex-create-clear-url", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
        input.assertTextEquals("")
        rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
            .assertIsNotEnabled()
        rule.onNodeWithTag("qrex-create-title", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test fun unifiedCardEnclosesTheRealUrlField() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-unified-card")
        await("qrex-create-content")
        val card = rule.onNodeWithTag("qrex-create-unified-card", useUnmergedTree = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val field = rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Form must be inset within the single reference card",
            field.left >= card.left && field.right <= card.right)
        assertTrue("Field must be vertically contained in the reference card",
            field.top >= card.top && field.bottom <= card.bottom)
        assertTrue("Card must not collapse on compact devices",
            card.right - card.left >= 200.dp)
        rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-create-save", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test fun standardCreateSubtitleIsNotCrowdedByAdvancedDisclosure() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-title")
        rule.onNodeWithTag("qrex-create-more-types", useUnmergedTree = true)
            .assertDoesNotExist()

        // All advanced types remain discoverable via the genuine overflow.
        rule.onNodeWithTag("qrex-create-overflow", useUnmergedTree = true)
            .performClick()
        await("qrex-create-overflow-type-3")
        rule.onNodeWithTag("qrex-create-overflow-type-3", useUnmergedTree = true)
            .performClick()
        await("qrex-create-more-types")
        rule.onNodeWithTag("qrex-create-more-types", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test fun quickTypeTabsFitSingleRowOnReferenceWidth() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        // Actual quick IDs: Link, Text, Wi-Fi, Contact (not advanced E-mail).
        val bounds = listOf(0, 1, 2, 6).map { index ->
            val tag = "qrex-create-type-$index"
            await(tag)
            rule.onNodeWithTag(tag, useUnmergedTree = true)
                .assertIsDisplayed().getUnclippedBoundsInRoot()
        }
        val config = rule.activity.resources.configuration
        if (config.screenWidthDp >= 340 && config.fontScale < 1.30f) {
            for (rect in bounds.drop(1)) {
                assertTrue("Reference four type tabs must occupy one row",
                    (rect.top - bounds[0].top).value in -2f..2f)
            }
        } else {
            assertTrue("Compact/large-font quick tabs should wrap to 2x2",
                bounds[2].top > bounds[0].top)
        }
        for (rect in bounds) {
            assertTrue(rect.bottom - rect.top >= 48.dp)
        }
    }
}
