package com.brendigo.qrex

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.IOException

/** Explicit, single-page PNG export through Android SAF; never stores in cache. */
internal object QREXDocumentImageExport {
    fun exportPng(context: Context, bitmap: Bitmap, uri: Uri) {
        require(!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0)
        require(bitmap.width.toLong() * bitmap.height <= QREX_DOCUMENT_MAX_PIXELS)
        val output = context.contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("PNG destination unavailable")
        output.use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw IOException("Unable to encode PNG")
            }
            stream.flush()
        }
    }
}
