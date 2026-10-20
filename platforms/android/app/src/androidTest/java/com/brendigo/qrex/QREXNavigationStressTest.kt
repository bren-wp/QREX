package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
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

    /**
     * A valid QR can now navigate immediately, even while its preview bitmap
     * is being rendered. This verifies action responsiveness independently
     * of rendering and logs no QR contents.
     */
    private fun awaitCreateAction(cycle: Int, phase: String) {
        val startedAt = android.os.SystemClock.elapsedRealtime()
        rule.waitForIdle()
        try {
            rule.waitUntil(timeoutMillis = 45_000L) {
                runCatching {
                    rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
                        .assertIsEnabled()
                }.isSuccess
            }
        } catch (error: Throwable) {
            val elapsed = android.os.SystemClock.elapsedRealtime() - startedAt
            val createVisible = rule.onAllNodesWithTag(
                "qrex-create-screen", useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
            throw AssertionError(
                "P03 cycle ${cycle + 1}/30: $phase valid QR action still disabled " +
                    "(elapsedMs=$elapsed, createVisible=$createVisible)",
                error
            )
        }
    }

    /** Exercise 30 real Compose navigation callbacks, independent of flaky
     * emulator coordinate injection; separate tests use physical tap injection.
     */
    @Test
    fun thirtyCreateResultBackCyclesKeepAppResponsive() {
        rule.onNodeWithTag("qrex-tab-1").performClick()
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
        repeat(30) { cycle ->
            requireScreenTag("qrex-create-screen", cycle, "await Create screen")
            requireScreenTag("qrex-create-content", cycle, "await Create input")
            rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
                .performTextReplacement("https://example.org/qrex/p03/$cycle")
            awaitCreateAction(cycle, "30-cycle stress")
            rule.onNodeWithTag("qrex-create-show").assertIsEnabled()
                .performSemanticsAction(SemanticsActions.OnClick)
            requireScreenTag("qrex-result-close", cycle, "await pinned Result close")
            rule.onNodeWithTag("qrex-result-close", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnClick)
            requireScreenTag("qrex-create-screen", cycle, "await return to Create")
            requireScreenTag("qrex-create-content", cycle, "await returned Create input")
        }
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
    }

    @Test
    fun closeAfterTextInputHidesImeAndRestoresCreate() {
        rule.onNodeWithTag("qrex-tab-1").performClick()
        requireScreenTag("qrex-create-screen", 0, "open Create")
        // Touch the actual field to request IME focus before text entry.
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performClick()
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performTextInput("https://example.org/p03/ime")
        awaitCreateAction(0, "single-cycle pre-result")
        // Coordinate-based touch still must open Result while the input has focus.
        rule.onNodeWithTag("qrex-create-show").assertIsDisplayed().performClick()
        requireScreenTag("qrex-result-screen", 0, "show Result")
        // Valid input must produce an actual QR bitmap, not merely enable a
        // navigation button. Use an independent bounded render assertion.
        try {
            rule.waitUntil(timeoutMillis = 45_000L) {
                rule.onAllNodesWithTag("qrex-result-qr-ready", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        } catch (error: Throwable) {
            throw AssertionError(
                "P03 actual QR bitmap not rendered after opening Result", error
            )
        }
        rule.onNodeWithTag("qrex-result-close", useUnmergedTree = true).performClick()
        requireScreenTag("qrex-create-screen", 0, "close Result")
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
    }

    @Test
    fun bottomCloseStillReturnsToCreateAfterScrolling() {
        rule.onNodeWithTag("qrex-tab-1").performClick()
        requireScreenTag("qrex-create-screen", 0, "open Create")
        // Retain text-field focus / IME during navigation. On Android 16
        // this previously left the scrolled bottom Close tap obstructed.
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performClick()
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performTextReplacement("https://example.org/p03/bottom-close")
        awaitCreateAction(0, "single-cycle pre-result")
        rule.onNodeWithTag("qrex-create-show").assertIsDisplayed().performClick()
        requireScreenTag("qrex-result-screen", 0, "open Result")
        // Scroll real Result content, then tap its permanently visible
        // bottom Close target with physical injection. An OnClick semantics
        // fallback could mask a keyboard-occluded touch target.
        rule.onNodeWithTag("qrex-result-scroll")
            .performTouchInput { swipeUp() }
        rule.onNodeWithTag("qrex-result-close-bottom")
            .assertIsDisplayed().performClick()
        requireScreenTag("qrex-create-screen", 0, "bottom Close returns to Create")
    }

    @Test
    fun pinnedCloseRemainsAvailableAfterScrollingResultToBottom() {
        rule.onNodeWithTag("qrex-tab-1").performClick()
        requireScreenTag("qrex-create-screen", 0, "open Create")
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performTextReplacement("https://example.org/p03/pinned-close")
        awaitCreateAction(0, "single-cycle pre-result")
        rule.onNodeWithTag("qrex-create-show").assertIsDisplayed().performClick()
        requireScreenTag("qrex-result-close", 0, "pinned close before scrolling")
        rule.onNodeWithTag("qrex-result-scroll").performTouchInput { swipeUp() }
        rule.onNodeWithTag("qrex-result-close", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        requireScreenTag("qrex-create-screen", 0, "return after Result scroll")
    }

    @Test
    fun createActionRemainsVisibleAfterScrollingFormToBottom() {
        rule.onNodeWithTag("qrex-tab-1").performClick()
        requireScreenTag("qrex-create-screen", 0, "open Create")
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .performTextReplacement("https://example.org/p03/sticky-create")
        awaitCreateAction(0, "primary action")
        rule.onNodeWithTag("qrex-create-save").performScrollTo()
        rule.onNodeWithTag("qrex-create-show").assertIsDisplayed().performClick()
        requireScreenTag("qrex-result-screen", 0, "sticky Create result transition")
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
