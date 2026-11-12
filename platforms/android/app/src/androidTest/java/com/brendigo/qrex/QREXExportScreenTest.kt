package com.brendigo.qrex

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Export must remain readable and avoid invoking SAF without consent. */
class QREXExportScreenTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun awaitTag(tag: String) {
        rule.waitUntil(timeoutMillis = 20_000L) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openExport() {
        awaitTag("qrex-tab-4")
        rule.onNodeWithTag("qrex-tab-4").performClick()
        awaitTag("qrex-more-export")
        rule.onNodeWithTag("qrex-more-export", useUnmergedTree = true).performClick()
        awaitTag("qrex-export-screen")
    }

    @Test
    fun exportHasPinnedBackAndScrollableUntruncatedPrivacy() {
        openExport()
        val pdf = rule.onNodeWithTag("qrex-export-pdf", useUnmergedTree = true)
            .assertIsDisplayed()
        val bounds = pdf.getUnclippedBoundsInRoot()
        assertTrue("PDF action must be >=48dp wide", bounds.right - bounds.left >= 48.dp)
        assertTrue("PDF action must be >=48dp tall", bounds.bottom - bounds.top >= 48.dp)
        if (QREXStore(rule.activity.applicationContext).items.none { it.saved }) {
            pdf.assertIsNotEnabled()
            val reason = pdf.fetchSemanticsNode().config[
                SemanticsProperties.StateDescription
            ]
            assertTrue("Disabled PDF must explain why there are no saved QR codes",
                !reason.isNullOrBlank())
            rule.onNodeWithTag("qrex-export-no-saved", useUnmergedTree = true)
                .assertIsDisplayed()
        }
        rule.onNodeWithTag("qrex-export-warning", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("qrex-export-warning-text", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-export-privacy", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("qrex-export-privacy-text", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("qrex-detail-back", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
    }


    @Test
    fun creatingQrAndSavingFavoriteMakesPdfAvailable() {
        // Keep the real Create → Result → Save flow independently tested.
        // PDF consent must not silently pass because Create navigation worked.
        val context = rule.activity.applicationContext
        val host = "qrex-create-favorite-${System.nanoTime().toString().removePrefix("-")}.example.org"
        val raw = "https://$host/test"
        try {
            awaitTag("qrex-tab-1")
            rule.onNodeWithTag("qrex-tab-1").performClick()
            awaitTag("qrex-create-content")
            rule.onNodeWithTag("qrex-create-content", useUnmergedTree = true)
                .performTextReplacement(raw)
            rule.waitUntil(timeoutMillis = 45_000L) {
                runCatching {
                    rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
                        .assertIsEnabled()
                }.isSuccess
            }
            rule.onNodeWithTag("qrex-create-show", useUnmergedTree = true)
                .performClick()
            awaitTag("qrex-result-save-favorite")
            rule.onNodeWithTag("qrex-result-save-favorite", useUnmergedTree = true)
                .performScrollTo().performClick()

            // A real saved favorite must be durably committed before navigating.
            rule.waitUntil(timeoutMillis = 20_000L) {
                QREXStore(context).items.any { it.raw == raw && it.saved }
            }
            rule.onNodeWithTag("qrex-result-close", useUnmergedTree = true)
                .assertIsDisplayed().performClick()
            awaitTag("qrex-tab-4")
            openExport()
            rule.waitUntil(timeoutMillis = 20_000L) {
                runCatching {
                    rule.onNodeWithTag("qrex-export-pdf", useUnmergedTree = true)
                        .assertIsEnabled()
                }.isSuccess
            }
            rule.onNodeWithTag("qrex-export-pdf", useUnmergedTree = true)
                .assertIsDisplayed().assertIsEnabled()
        } finally {
            val store = QREXStore(context)
            store.items.firstOrNull { it.raw == raw }?.let { record ->
                assertTrue("remove only synthetic Create favorite", store.remove(record))
            }
        }
    }

    @Test
    fun savedPdfRequiresExplicitConsentAndCancelRetainsAllHistory() {
        // Seed the actual production store, not a fake PDF or screenshot.
        // Recreate the Activity so its observable store sees the saved entry.
        // This isolates PDF consent/cancel from unrelated Create/IME navigation.
        val context = rule.activity.applicationContext
        val host = "qrex-export-${System.nanoTime().toString().removePrefix("-")}.example.org"
        val raw = "https://$host/test"
        val before = QREXStore(context).items.map { it.raw }
        try {
            assertTrue("synthetic favorite must persist", QREXStore(context).save(raw))
            rule.activityRule.scenario.recreate()
            awaitTag("qrex-tab-4")
            openExport()
            rule.waitUntil(timeoutMillis = 20_000L) {
                runCatching {
                    rule.onNodeWithTag("qrex-export-pdf", useUnmergedTree = true)
                        .assertIsEnabled()
                }.isSuccess
            }
            rule.onNodeWithTag("qrex-export-pdf", useUnmergedTree = true)
                .assertIsDisplayed().assertIsEnabled().performClick()
            awaitTag("qrex-export-consent")
            rule.onNodeWithTag("qrex-export-consent", useUnmergedTree = true)
                .assertIsDisplayed()
            for (tag in listOf("qrex-export-consent-confirm",
                               "qrex-export-consent-cancel")) {
                val bounds = rule.onNodeWithTag(tag, useUnmergedTree = true)
                    .assertIsDisplayed().getUnclippedBoundsInRoot()
                assertTrue("$tag must be at least 48dp tall",
                    bounds.bottom - bounds.top >= 48.dp)
            }
            rule.onNodeWithTag("qrex-export-consent-cancel", useUnmergedTree = true)
                .assertIsDisplayed().performClick()
            rule.waitUntil(timeoutMillis = 10_000L) {
                rule.onAllNodesWithTag("qrex-export-consent", useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty()
            }
            rule.onAllNodesWithTag("qrex-export-consent", useUnmergedTree = true)
                .assertCountEquals(0)
            rule.onNodeWithTag("qrex-export-screen").assertIsDisplayed()
            val after = QREXStore(context).items.map { it.raw }
            assertTrue("original QR records must be preserved",
                after.containsAll(before))
            assertEquals("only synthetic record should have been added",
                before.size + 1, after.size)
            assertTrue("cancel must not delete saved favorite", after.contains(raw))
        } finally {
            val store = QREXStore(context)
            store.items.firstOrNull { it.raw == raw }?.let { record ->
                assertTrue("remove only synthetic PDF favorite", store.remove(record))
            }
        }
    }

}
