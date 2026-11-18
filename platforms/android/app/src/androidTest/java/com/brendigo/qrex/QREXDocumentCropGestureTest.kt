package com.brendigo.qrex

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Exercise the actual Android touch gesture layer, not just geometry helpers. */
class QREXDocumentCropGestureTest {
    @get:Rule val rule = createComposeRule()

    @Test fun draggingPerspectiveHandleUpdatesSourceNormalizedCorners() {
        val source = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(AndroidColor.WHITE) }
        val selected = mutableStateOf(QREX_DOCUMENT_INSET_CORNERS)
        rule.setContent {
            MaterialTheme {
                QREXDocumentCorners(source, selected.value, rectangle = false) {
                    selected.value = it
                }
            }
        }
        val editor = rule.onNodeWithTag("qrex-document-crop-canvas").assertIsDisplayed()
        val size = editor.fetchSemanticsNode().size
        val rect = qrexDocumentPreviewRect(
            size.width.toFloat(), size.height.toFloat(), source.width, source.height
        )
        val x = rect.left + .05f * rect.width()
        val y = rect.top + .05f * rect.height()
        editor.performTouchInput {
            swipe(start = Offset(x, y), end = Offset(x + 45f, y + 60f),
                durationMillis = 650)
        }
        rule.runOnIdle {
            assertTrue(selected.value[0] > .05f)
            assertTrue(selected.value[1] > .05f)
            assertTrue(qrexDocumentQuadValid(selected.value))
            // Perspective mode changes only the dragged handle.
            assertEquals(QREX_DOCUMENT_INSET_CORNERS.subList(2, 8),
                selected.value.subList(2, 8))
        }
    }

    @Test fun rectangleDragAlwaysKeepsAdjacentEdgesAligned() {
        val source = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(AndroidColor.WHITE) }
        val selected = mutableStateOf(QREX_DOCUMENT_INSET_CORNERS)
        rule.setContent {
            MaterialTheme {
                QREXDocumentCorners(source, selected.value, rectangle = true) {
                    selected.value = it
                }
            }
        }
        val editor = rule.onNodeWithTag("qrex-document-crop-canvas").assertIsDisplayed()
        val size = editor.fetchSemanticsNode().size
        val rect = qrexDocumentPreviewRect(
            size.width.toFloat(), size.height.toFloat(), source.width, source.height
        )
        val x = rect.left + .05f * rect.width()
        val y = rect.top + .05f * rect.height()
        editor.performTouchInput {
            swipe(start = Offset(x, y), end = Offset(x + 45f, y + 60f),
                durationMillis = 650)
        }
        rule.runOnIdle {
            assertTrue(selected.value[0] > .05f)
            assertEquals(selected.value[0], selected.value[6], .00001f)
            assertEquals(selected.value[1], selected.value[3], .00001f)
            assertEquals(selected.value[2], selected.value[4], .00001f)
            assertEquals(selected.value[5], selected.value[7], .00001f)
            assertTrue(qrexDocumentQuadValid(selected.value))
        }
    }
}
