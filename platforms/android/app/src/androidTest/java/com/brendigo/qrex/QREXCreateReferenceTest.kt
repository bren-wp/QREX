package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
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

    @Test fun urlClearGlyphRemovesActualDraftWithoutNavigation() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        await("qrex-create-content")

        val input = rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
        input.performTextInput("https://brendigo.com")
        await("qrex-create-clear-url")
        val clear = rule.onNodeWithTag("qrex-create-clear-url", useUnmergedTree = true)
            .assertIsDisplayed()
        val hit = clear.getUnclippedBoundsInRoot()
        assertTrue("Clear URL icon has inadequate width", hit.right - hit.left >= 48.dp)
        assertTrue("Clear URL icon has inadequate height", hit.bottom - hit.top >= 48.dp)

        clear.performClick()
        rule.waitForIdle()
        input.assertTextEquals("")
        rule.onNodeWithTag("qrex-create-title", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test fun quickTypeTabsFitSingleRowOnReferenceWidth() {
        await("qrex-tab-1")
        rule.onNodeWithTag("qrex-tab-1").performClick()
        val bounds = (0..3).map { index ->
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
