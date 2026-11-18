package com.brendigo.qrex

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QREXDocumentSessionTest {
    private fun bitmap() = Bitmap.createBitmap(80, 100, Bitmap.Config.ARGB_8888)

    @Test fun multiPageOrderAndUndoAreRealSessionOperations() {
        val session = QREXDocumentSession()
        val first = bitmap()
        val second = bitmap()
        val rotated = bitmap()
        try {
            session.updatePending(first)
            assertTrue(session.accept())
            session.updatePending(second)
            assertTrue(session.accept())
            assertSame(first, session.pages[0])
            assertSame(second, session.pages[1])
            session.move(0, 1)
            assertSame(second, session.pages[0])
            assertSame(first, session.pages[1])
            session.updatePending(second)
            session.transformPending(rotated)
            assertTrue(session.canUndo)
            assertSame(rotated, session.pending)
            assertTrue(session.undoPending())
            assertSame(second, session.pending)
            assertFalse(session.canUndo)
            session.replace(0, rotated)
            assertSame(rotated, session.pages[0])
        } finally {
            session.clearSession()
            first.recycle()
            second.recycle()
            rotated.recycle()
        }
    }

    @Test fun staleAsyncTransformCannotOverwriteUndoneOrReplacedDocument() {
        val session = QREXDocumentSession()
        val original = bitmap()
        val replacement = bitmap()
        val stale = bitmap()
        try {
            session.updatePending(original)
            val beforeOperation = session.revision
            assertTrue(session.transformPending(replacement, beforeOperation))
            assertTrue(session.undoPending())
            assertSame(original, session.pending)
            // A background filter based on the pre-Undo pixels arrives late.
            assertFalse(session.transformPending(stale, beforeOperation))
            assertSame(original, session.pending)
            assertFalse(session.canUndo)
            session.updatePending(null)
            assertFalse(session.transformPending(stale, session.revision))
        } finally {
            session.clearSession()
            original.recycle()
            replacement.recycle()
            stale.recycle()
        }
    }

    @Test fun undoCannotRestorePageAfterExplicitDiscard() {
        val session = QREXDocumentSession()
        val before = bitmap()
        val after = bitmap()
        try {
            session.updatePending(before)
            session.transformPending(after)
            assertTrue(session.canUndo)
            session.updatePending(null)
            assertFalse(session.canUndo)
            assertFalse(session.undoPending())
        } finally {
            session.clearSession()
            before.recycle()
            after.recycle()
        }
    }
}
