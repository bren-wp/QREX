package com.brendigo.qrex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
            val input = rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
            input.performTextReplacement(raw)
            // The Result has no editor; complete the actual IME transaction
            // before measuring visible bounds. API 26's keyboard animation
            // may outlive the Compose recomposition that reveals Result.
            input.performImeAction()
            rule.waitUntil(timeoutMillis = 10_000L) {
                ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) != true
            }
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

    @Test fun resultPrimaryActionFitsVisibleViewport() = withPersistedResult {
        val viewport = rule.onNodeWithTag(
            "qrex-result-screen", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        val primary = rule.onNodeWithTag(
            "qrex-result-primary", useUnmergedTree = true
        ).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Primary action must fit inside Result viewport",
            primary.bottom <= viewport.bottom)
    }

    @Test fun resultFavoriteRemainsReachableAboveBottomNavigation() = withPersistedResult {
        val favorite = rule.onNodeWithTag(
            "qrex-result-save-favorite", useUnmergedTree = true
        ).performScrollTo().assertIsDisplayed().getUnclippedBoundsInRoot()
        val viewport = rule.onNodeWithTag(
            "qrex-result-screen", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        val nav = rule.onNodeWithTag(
            "qrex-bottom-navigation", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        assertTrue("Favorite must stay inside Result viewport",
            favorite.bottom <= viewport.bottom)
        assertTrue("Favorite must not be covered by the bottom navigation",
            favorite.bottom <= nav.top)
    }

    @Test fun resultDeleteIsReachableAndFitsAtRestOnTallViewports() = withPersistedResult {
        val viewport = rule.onNodeWithTag(
            "qrex-result-screen", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        val nav = rule.onNodeWithTag(
            "qrex-bottom-navigation", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        val config = rule.activity.resources.configuration

        // ScreenHeightDp includes areas consumed by platform/system bars
        // and can differ from the actual Compose viewport on API 26.
        // Require the at-rest match where the real viewport has enough
        // room for the 156dp card, 54dp primary, five >=48dp action rows,
        // spacing and the fixed 56dp Result header.
        if (config.screenWidthDp >= 360 && config.fontScale <= 1.15f &&
            viewport.bottom - viewport.top >= 550.dp) {
            val atRest = rule.onNodeWithTag(
                "qrex-result-delete", useUnmergedTree = true
            ).getUnclippedBoundsInRoot()
            assertTrue("Delete must fit above navigation without scrolling on tall screens",
                atRest.bottom <= nav.top)
        }

        // On compact devices, the content is intentionally scrollable;
        // never allow the bottom bar to hide or intercept Delete.
        val delete = rule.onNodeWithTag(
            "qrex-result-delete", useUnmergedTree = true
        ).performScrollTo().assertIsDisplayed().getUnclippedBoundsInRoot()
        val visibleViewport = rule.onNodeWithTag(
            "qrex-result-screen", useUnmergedTree = true
        ).getUnclippedBoundsInRoot()
        assertTrue("Delete must stay within the Result viewport",
            delete.bottom <= visibleViewport.bottom)
        assertTrue("Delete must not be covered by the bottom navigation",
            delete.bottom <= nav.top)
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
