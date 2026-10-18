package com.brendigo.qrex

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * Synthetic, privacy-safe UI stress checks; these do not constitute live
 * camera, hardware rotation, genuine process death, or OEM device certification.
 */
class QREXNavigationStressTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun thirtyCreateResultBackCyclesKeepAppResponsive() {
        rule.onNodeWithTag("qrex-tab-1").performClick()
        repeat(30) { cycle ->
            rule.onNodeWithTag("qrex-create-content")
                .performTextInput("https://example.org/qrex/p03/${cycle}")
            rule.waitUntil(timeoutMillis = 20_000L) {
                runCatching {
                    rule.onNodeWithTag("qrex-create-show").assertIsEnabled()
                }.isSuccess
            }
            rule.onNodeWithTag("qrex-create-show")
                .performScrollTo().performClick()
            rule.onNodeWithTag("qrex-result-close")
                .performScrollTo().performClick()
            rule.onNodeWithTag("qrex-create-content").assertExists()
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
