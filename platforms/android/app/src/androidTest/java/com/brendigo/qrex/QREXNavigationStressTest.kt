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
        } catch (error: Throwable) {
            // Report only stable screen flags; never dump the semantics tree
            // because it may contain a Wi-Fi QR password or scanned content.
            fun visible(name: String): Boolean = runCatching {
                rule.onAllNodesWithTag(name, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
            val create = visible("qrex-create-screen")
            val result = visible("qrex-result-screen")
            val tab = runCatching {
                rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
            }.isSuccess
            throw AssertionError(
                "P03 UI cycle ${cycle + 1}/30: $phase " +
                    "(tag=$tag, createVisible=$create, resultVisible=$result, " +
                    "createTabSelected=$tab)", error
            )
        }
    }

    @Test
    fun thirtyCreateResultBackCyclesKeepAppResponsive() {
        rule.onNodeWithTag("qrex-tab-1").performClick()
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
        repeat(30) { cycle ->
            requireScreenTag("qrex-create-screen", cycle, "await Create screen")
            requireScreenTag("qrex-create-content", cycle, "await Create input")
            rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
                .performTextReplacement("https://example.org/qrex/p03/$cycle")
            try {
                rule.waitUntil(timeoutMillis = 20_000L) {
                    runCatching {
                        rule.onNodeWithTag("qrex-create-show").assertIsEnabled()
                    }.isSuccess
                }
            } catch (error: Throwable) {
                throw AssertionError("P03 UI cycle ${cycle + 1}/30: QR generation not ready", error)
            }
            rule.onNodeWithTag("qrex-create-show").performScrollTo().performClick()
            requireScreenTag("qrex-result-close", cycle, "await pinned Result close")
            rule.onNodeWithTag("qrex-result-close", useUnmergedTree = true).performClick()
            requireScreenTag("qrex-create-screen", cycle, "await return to Create")
            requireScreenTag("qrex-create-content", cycle, "await returned Create input")
        }
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
    }

    @Test
    fun closeAfterTextInputHidesImeAndRestoresCreate() {
        rule.onNodeWithTag("qrex-tab-1").performClick()
        requireScreenTag("qrex-create-screen", 0, "open Create")
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performTextReplacement("https://example.org/p03/ime")
        rule.waitUntil(timeoutMillis = 20_000L) {
            runCatching {
                rule.onNodeWithTag("qrex-create-show").assertIsEnabled()
            }.isSuccess
        }
        rule.onNodeWithTag("qrex-create-show").performScrollTo().performClick()
        requireScreenTag("qrex-result-screen", 0, "show Result")
        rule.onNodeWithTag("qrex-result-close", useUnmergedTree = true).performClick()
        requireScreenTag("qrex-create-screen", 0, "close Result")
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
    }

    @Test
    fun bottomCloseStillReturnsToCreateAfterScrolling() {
        rule.onNodeWithTag("qrex-tab-1").performClick()
        requireScreenTag("qrex-create-screen", 0, "open Create")
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performTextReplacement("https://example.org/p03/bottom-close")
        rule.waitUntil(timeoutMillis = 20_000L) {
            runCatching {
                rule.onNodeWithTag("qrex-create-show").assertIsEnabled()
            }.isSuccess
        }
        rule.onNodeWithTag("qrex-create-show").performScrollTo().performClick()
        requireScreenTag("qrex-result-screen", 0, "open Result")
        rule.onNodeWithTag("qrex-result-close-bottom")
            .performScrollTo().performClick()
        requireScreenTag("qrex-create-screen", 0, "bottom Close returns to Create")
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
