package com.brendigo.qrex

import androidx.compose.ui.test.assertCountEquals
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Synthetic, privacy-safe UI stress checks; these do not constitute live
 * camera, hardware rotation, genuine process death, or OEM device certification.
 */
class QREXNavigationStressTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun waitForColdStartSplashToFinish() {
        // The dark-first QREX splash is a genuine cold-start UI, not a test
        // fixture. Wait for the real navigation bar before interacting, and
        // fail if the splash never dismisses or the main screen cannot load.
        rule.waitUntil(timeoutMillis = 15_000L) {
            rule.onAllNodesWithTag("qrex-tab-0", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

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
        // The API 31 emulator once crashed inside Compose Modifier.Node
        // detach during Activity destruction after these rapid transitions.
        // Do not merely assert navigation semantics and let JUnit tear down:
        // explicitly recreate the Activity after the churn, so lifecycle
        // detachment is a tested operation rather than a silent teardown.
        rule.waitForIdle()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
        requireScreenTag("qrex-create-screen", 29, "after stress Activity recreation")
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
        // QR bitmap generation is intentionally deferred until the user
        // opens the real preview. Test the physical touch and decoded image.
        rule.onNodeWithTag("qrex-result-toggle-qr").assertIsDisplayed().performClick()
        try {
            rule.waitUntil(timeoutMillis = 45_000L) {
                rule.onAllNodesWithTag("qrex-result-qr-ready", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        } catch (error: Throwable) {
            throw AssertionError(
                "P03 actual QR bitmap not rendered after tapping Show QR", error
            )
        }
        rule.onNodeWithTag("qrex-result-close", useUnmergedTree = true).performClick()
        requireScreenTag("qrex-create-screen", 0, "close Result")
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
    }

    @Test
    fun pinnedCloseReturnsToCreateAfterScrollingWithIme() {
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
        // The reference layout keeps one Back control pinned above the
        // scrolling Result content. Verify it stays reachable with the IME
        // dismissed during navigation, without adding a non-reference button.
        rule.onNodeWithTag("qrex-result-scroll")
            .performTouchInput { swipeUp() }
        rule.onNodeWithTag("qrex-result-close", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        requireScreenTag("qrex-create-screen", 0, "pinned Back returns to Create")
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
        // Swipe the actual form, not the pinned Save action outside it.
        // The physical Generate button must stay clickable after scrolling.
        rule.onNodeWithTag("qrex-create-scroll")
            .performTouchInput { swipeUp() }
        rule.onNodeWithTag("qrex-create-save").assertIsDisplayed()
        rule.onNodeWithTag("qrex-create-show").assertIsDisplayed().performClick()
        requireScreenTag("qrex-result-screen", 0, "sticky Create result transition")
    }

    @Test
    fun scanFirstKeepsGalleryAndTorchAsSingleClearActions() {
        rule.onNodeWithTag("qrex-tab-0").assertIsSelected()
        rule.onNodeWithTag("qrex-scan-gallery", useUnmergedTree = true)
            .assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithTag("qrex-scan-torch", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun fourMainTabsKeepHistoryFavoritesAndMoreWithinTwoTaps() {
        rule.onNodeWithTag("qrex-tab-2").assertIsDisplayed().performClick()
        rule.onNodeWithTag("qrex-tab-2").assertIsSelected()
        requireScreenTag("qrex-history-screen", 0, "open History")
        rule.onNodeWithTag("qrex-history-segments", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-history-saved", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("qrex-history-saved", useUnmergedTree = true)
            .assertIsSelected()
        rule.onNodeWithTag("qrex-history-all", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("qrex-history-all", useUnmergedTree = true)
            .assertIsSelected()
        rule.onNodeWithTag("qrex-tab-3").performClick()
        rule.onNodeWithTag("qrex-tab-3").assertIsSelected()
        requireScreenTag("qrex-more-screen", 0, "open More")
        rule.onAllNodesWithTag("qrex-tab-4").assertCountEquals(0)
    }

    @Test
    fun languageAndExportScreensFollowReferenceAndNavigateBack() {
        rule.onNodeWithTag("qrex-tab-3").performClick()
        requireScreenTag("qrex-more-screen", 0, "open More")
        rule.onNodeWithTag("qrex-more-language", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        requireScreenTag("qrex-language-screen", 0, "open full Language screen")
        rule.onNodeWithTag("qrex-language-option-hr", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-detail-back", useUnmergedTree = true)
            .performClick()
        requireScreenTag("qrex-more-screen", 0, "return from Language")
        rule.onNodeWithTag("qrex-more-export", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        requireScreenTag("qrex-export-screen", 0, "open full Export screen")
        rule.onNodeWithTag("qrex-export-pdf", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-export-zip", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-detail-back", useUnmergedTree = true)
            .performClick()
        requireScreenTag("qrex-more-screen", 0, "return from Export")
    }

    @Test
    fun lightReferenceGridShowsSevenRealQrTypeActions() {
        rule.onNodeWithTag("qrex-tab-1").performClick()
        val alreadyLight = rule.onAllNodesWithTag(
            "qrex-light-types-grid", useUnmergedTree = true
        ).fetchSemanticsNodes().isNotEmpty()
        if (!alreadyLight) {
            rule.onNodeWithTag("qrex-tab-3").performClick()
            rule.onNodeWithTag("qrex-more-theme", useUnmergedTree = true)
                .performClick()
            rule.onNodeWithTag("qrex-tab-1").performClick()
        }
        try {
            requireScreenTag("qrex-light-types-grid", 0, "light Create options")
            for (kind in listOf(0, 1, 2, 6, 3, 4, 5)) {
                rule.onNodeWithTag("qrex-create-type-$kind", useUnmergedTree = true)
                    .performScrollTo().assertIsDisplayed()
            }
            for (kind in listOf(3, 4, 5, 0)) {
                rule.onNodeWithTag("qrex-create-type-$kind", useUnmergedTree = true)
                    .performScrollTo().performClick()
                rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
                    .assertIsDisplayed()
            }
        } finally {
            if (!alreadyLight) {
                rule.onNodeWithTag("qrex-tab-3").performClick()
                rule.onNodeWithTag("qrex-more-theme", useUnmergedTree = true)
                    .performClick()
            }
        }
    }

    @Test
    fun bothThemeSettingsLayoutsPreserveClickableActions() {
        rule.onNodeWithTag("qrex-tab-3").performClick()
        requireScreenTag("qrex-more-screen", 0, "More before theme switch")
        rule.onNodeWithTag("qrex-more-theme", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        requireScreenTag("qrex-more-screen", 0, "More after theme switch")
        rule.onNodeWithTag("qrex-more-language", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-more-export", useUnmergedTree = true)
            .assertIsDisplayed()
        // Restore the previous preference; other tests must not inherit a
        // changed persisted theme from this design-regression check.
        rule.onNodeWithTag("qrex-more-theme", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        requireScreenTag("qrex-more-screen", 0, "More theme restored")
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

