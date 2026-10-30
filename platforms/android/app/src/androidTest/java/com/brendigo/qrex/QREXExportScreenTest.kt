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
            rule.onNodeWithTag("qrex-export-no-saved", useUnmergedTree = true)
                .assertIsDisplayed()
        }
        rule.onNodeWithTag("qrex-export-warning", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("qrex-export-privacy", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("qrex-detail-back", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("qrex-more-screen").assertIsDisplayed()
    }

    @Test
    fun savedPdfRequiresExplicitConsentAndCancelRetainsAllHistory() {
        val context = rule.activity.applicationContext
        val host = "qrex-export-${System.nanoTime().toString().removePrefix("-")}.example.org"
        val raw = "https://$host/test"
        val before = QREXStore(context).items.map { it.raw }
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
            openExport()
            rule.onNodeWithTag("qrex-export-pdf", useUnmergedTree = true)
                .assertIsEnabled().performClick()
            rule.onNodeWithTag("qrex-export-consent", useUnmergedTree = true)
                .assertIsDisplayed()
            rule.onNodeWithTag("qrex-export-consent-cancel", useUnmergedTree = true)
                .assertIsDisplayed().performClick()
            rule.onAllNodesWithTag("qrex-export-consent", useUnmergedTree = true)
                .assertCountEquals(0)
            rule.onNodeWithTag("qrex-export-screen").assertIsDisplayed()
            val after = QREXStore(context).items.map { it.raw }
            assertTrue("original QR records must be preserved",
                after.containsAll(before))
            assertEquals("only synthetic record should have been added",
                before.size + 1, after.size)
        } finally {
            val store = QREXStore(context)
            val testRecord = store.items.firstOrNull { it.raw == raw }
            if (testRecord != null) {
                assertTrue("remove only test record", store.remove(testRecord))
            }
        }
    }
}
