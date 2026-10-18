package com.brendigo.qrex

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test

/**
 * Synthetic, privacy-safe UI stress checks; these do not constitute live
 * camera, hardware rotation, genuine process death, or OEM device certification.
 */
class QREXNavigationStressTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    /**
     * Wait for a semantic tag without retaining or logging any QR contents.
     * A failed iteration reports its phase, not user data.
     */
    private fun requireScreenTag(tag: String, cycle: Int, phase: String) {
        try {
            rule.waitUntil(timeoutMillis = 20_000L) {
                rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        } catch (error: AssertionError) {
            throw AssertionError("P03 UI cycle $cycle: $phase (missing tag $tag)", error)
        }
    }

    @Test
    fun thirtyCreateResultBackCyclesKeepAppResponsive() {
        rule.onNodeWithTag("qrex-tab-1").performClick()
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
        repeat(30) { cycle ->
            requireScreenTag("qrex-create-content", cycle, "await Create input")
            rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
                .performTextReplacement("https://example.org/qrex/p03/$cycle")
            try {
                rule.waitUntil(timeoutMillis = 20_000L) {
                    runCatching {
                        rule.onNodeWithTag("qrex-create-show").assertIsEnabled()
                    }.isSuccess
                }
            } catch (error: AssertionError) {
                throw AssertionError("P03 UI cycle $cycle: QR generation not ready", error)
            }
            rule.onNodeWithTag("qrex-create-show").performScrollTo().performClick()
            requireScreenTag("qrex-result-close", cycle, "await Result")
            rule.onNodeWithTag("qrex-result-close").performScrollTo().performClick()
            requireScreenTag("qrex-create-content", cycle, "await return to Create")
        }
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
    }

    @Test
    fun activityRecreationKeepsNonSensitiveTabSelection() {
        rule.onNodeWithTag("qrex-tab-2").performClick()
        rule.onNodeWithTag("qrex-tab-2").assertIsSelected()
        // ActivityScenario.recreate() exercises configuration-style recreation,
        // not an OS process kill. Sensitive current QR payload is not saved.
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("qrex-tab-2").assertIsSelected()
        rule.onNodeWithTag("qrex-tab-0").performClick()
        rule.onNodeWithTag("qrex-tab-0").assertIsSelected()
    }
}
