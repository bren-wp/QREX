package com.brendigo.qrex

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The complete document page-action row must be accessible and functional. */
class QREXDocumentPagesControlsTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun await(tag: String) {
        rule.waitUntil(timeoutMillis = 25_000L) {
            rule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun allFourPageActionsAre48DpAndReorderAffectsRealPdfPageSequence() {
        await("qrex-document-entry")
        rule.onNodeWithTag("qrex-document-entry", useUnmergedTree = true).performClick()
        await("qrex-document-import")
        val session = ViewModelProvider(rule.activity)[QREXDocumentSession::class.java]
        val first = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
        val second = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
        try {
            rule.runOnIdle {
                session.clearSession()
                session.pages.add(first)
                session.pages.add(second)
            }
            await("qrex-document-open-pages")
            rule.onNodeWithTag("qrex-document-open-pages", useUnmergedTree = true)
                .performClick()
            await("qrex-document-page-0")
            val card = rule.onNodeWithTag("qrex-document-page-0", useUnmergedTree = true)
                .assertIsDisplayed().getUnclippedBoundsInRoot()
            for (suffix in listOf("up", "down", "edit", "delete")) {
                val hit = rule.onNodeWithTag(
                    "qrex-document-page-$suffix-0", useUnmergedTree = true
                ).assertIsDisplayed().getUnclippedBoundsInRoot()
                assertTrue("$suffix action width must be >= 48dp",
                    hit.right - hit.left >= 48.dp)
                assertTrue("$suffix action height must be >= 48dp",
                    hit.bottom - hit.top >= 48.dp)
                assertTrue("$suffix action must remain inside its card",
                    hit.left >= card.left - 1.dp && hit.right <= card.right + 1.dp)
            }
            rule.onNodeWithTag("qrex-document-page-down-0", useUnmergedTree = true)
                .performClick()
            rule.runOnIdle {
                assertSame(second, session.pages[0])
                assertSame(first, session.pages[1])
            }
        } finally {
            rule.runOnIdle { session.clearSession() }
            rule.waitForIdle()
            first.recycle()
            second.recycle()
        }
    }
}
