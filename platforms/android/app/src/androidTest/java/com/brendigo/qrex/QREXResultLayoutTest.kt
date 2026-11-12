package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Functional, layout-only test: never reads or prints private QR payloads. */
class QREXResultLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    /**
     * Every UI check starts with a real persisted favorite. Result Delete is
     * intentionally hidden for QR codes that are not in History; relying on
     * history recording being enabled makes tests order-dependent.
     */
    private fun withPersistedResult(assertions: () -> Unit) {
        val context = rule.activity.applicationContext
        val raw = "https://qrex-result-${System.nanoTime().toString().removePrefix("-")}.example.org/test"
        assertTrue("Test QR must be saved", QREXStore(context).save(raw))
        try {
            // The Activity's store is lazy and must reload the persisted seed.
            rule.activityRule.scenario.recreate()
            rule.waitUntil(timeoutMillis = 20_000L) {
                rule.onAllNodesWithTag("qrex-tab-1", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("qrex-tab-1").performClick()
            rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
                .performTextReplacement(raw)
            rule.waitUntil(timeoutMillis = 20_000L) {
                runCatching {
                    rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
                        .assertIsEnabled()
                }.isSuccess
            }
            rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
                .performClick()
            rule.waitUntil(timeoutMillis = 20_000L) {
                rule.onAllNodesWithTag("qrex-result-summary", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            assertions()
        } finally {
            // Do not clear real History, and never leak the synthetic favorite.
            val store = QREXStore(context)
            store.items.firstOrNull { it.raw == raw }?.let { record ->
                assertTrue("Only synthetic Result record may be removed",
                    store.remove(record))
            }
        }
    }

    @Test fun resultNavigationHasFiveFunctionalVisibleTabs() = withPersistedResult {
        // The Result screen has the same five live bottom tabs, and the
        // selected state still tracks its originating Create destination.
        rule.onNodeWithTag("qrex-bottom-navigation", useUnmergedTree = true)
            .assertIsDisplayed()
        for (index in 0..4) {
            rule.onNodeWithTag("qrex-tab-$index", useUnmergedTree = true)
                .assertIsDisplayed()
        }
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
    }

    @Test fun resultSummaryGlyphAndOriginFitReferenceViewport() = withPersistedResult {
        val card = rule.onNodeWithTag(
            "qrex-result-summary", useUnmergedTree = true
        ).assertIsDisplayed().getUnclippedBoundsInRoot()
        val glyph = rule.onNodeWithTag(
            "qrex-result-glyph", useUnmergedTree = true
        ).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Result icon must stay inside the summary",
            glyph.top >= card.top && glyph.bottom <= card.bottom)
        assertTrue("Summary must fit the reference-style compact layout",
            card.bottom - card.top <= 260.dp)
        val viewport = rule.onNodeWithTag(
            "qrex-result-screen", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        assertTrue("Result card must remain capped to a readable tablet width",
            card.right - card.left <= 520.dp)
        if (viewport.right - viewport.left >= 600.dp) {
            val centerDelta = kotlin.math.abs(
                (card.left.value + card.right.value) -
                    (viewport.left.value + viewport.right.value)
            )
            assertTrue("Tablet result panel must be horizontally centered",
                centerDelta < 4f)
        }
        rule.onNodeWithTag("qrex-result-url-origin", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test fun resultPrimaryFavoriteAndDeleteRemainReachable() = withPersistedResult {
        val viewport = rule.onNodeWithTag(
            "qrex-result-screen", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        val open = rule.onNodeWithTag(
            "qrex-result-primary", useUnmergedTree = true
        ).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Primary action must fit inside Result viewport",
            open.bottom <= viewport.bottom)

        // Check 'without scrolling' before any performScrollTo moves the
        // content. The previous combined test measured after scrolling.
        val config = rule.activity.resources.configuration
        if (config.screenWidthDp >= 360 && config.screenHeightDp >= 650 &&
            config.fontScale <= 1.15f) {
            val deleteAtRest = rule.onNodeWithTag(
                "qrex-result-delete", useUnmergedTree = true
            ).getUnclippedBoundsInRoot()
            val bottomNav = rule.onNodeWithTag(
                "qrex-bottom-navigation", useUnmergedTree = true
            ).getUnclippedBoundsInRoot()
            assertTrue("Delete must fit above the bottom navigation without scrolling",
                deleteAtRest.bottom <= bottomNav.top)
        }
        val favorite = rule.onNodeWithTag(
            "qrex-result-save-favorite", useUnmergedTree = true
        ).performScrollTo().assertIsDisplayed().getUnclippedBoundsInRoot()
        val activeViewport = rule.onNodeWithTag(
            "qrex-result-screen", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        assertTrue("Favorite must stay inside Result viewport",
            favorite.bottom <= activeViewport.bottom)
        rule.onNodeWithTag("qrex-result-delete", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun resultOverflowEditReturnsToTheWorkingCreateField() = withPersistedResult {
        rule.onNodeWithTag("qrex-result-menu", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("qrex-result-menu-edit", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag("qrex-result-screen", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithTag("qrex-result-screen", useUnmergedTree = true)
            .assertDoesNotExist()
        rule.onNodeWithTag("qrex-bottom-navigation", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-tab-1").assertIsSelected()
        rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
