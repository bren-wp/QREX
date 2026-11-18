package com.brendigo.qrex

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal const val QREX_DOCUMENT_MAX_PAGES = 8
internal const val QREX_DOCUMENT_MAX_PIXELS = 1_800_000

/** UI and PDF share these pure dimension calculations. */
internal fun qrexDocumentFit(
    imageWidth: Int, imageHeight: Int,
    pageWidth: Int, pageHeight: Int, margin: Int = 24
): FloatArray {
    if (imageWidth <= 0 || imageHeight <= 0 ||
        pageWidth <= 2 * margin || pageHeight <= 2 * margin || margin < 0) {
        return floatArrayOf(0f, 0f, 0f, 0f)
    }
    val scale = min(
        (pageWidth - 2f * margin) / imageWidth,
        (pageHeight - 2f * margin) / imageHeight
    )
    val w = imageWidth * scale
    val h = imageHeight * scale
    return floatArrayOf((pageWidth - w) / 2f, (pageHeight - h) / 2f,
        (pageWidth + w) / 2f, (pageHeight + h) / 2f)
}

internal fun qrexDocumentSafeName(name: String): String {
    val cleaned = name.trim().replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
        .trim('.', '_').take(64).ifBlank { "QREX-document" }
    return if (cleaned.endsWith(".pdf", ignoreCase = true)) cleaned else "$cleaned.pdf"
}

/**
 * Four normalized corners: top-left, top-right, bottom-right, bottom-left.
 * Reject self-intersections, collapsed edges and shapes outside the bitmap.
 * Allow small user-selected regions (down to a narrow ~2% image span) without
 * rounding the user's tight crop back into a large default rectangle.
 */
internal fun qrexDocumentQuadValid(p: List<Float>): Boolean {
    if (p.size != 8 || p.any { !it.isFinite() || it !in 0f..1f }) return false
    var sign = 0
    var signedArea = 0f
    for (i in 0..3) {
        val j = (i + 1) % 4
        val k = (i + 2) % 4
        val ax = p[j * 2] - p[i * 2]
        val ay = p[j * 2 + 1] - p[i * 2 + 1]
        val bx = p[k * 2] - p[j * 2]
        val by = p[k * 2 + 1] - p[j * 2 + 1]
        val cross = ax * by - ay * bx
        if (abs(cross) < 0.00001f) return false
        val direction = if (cross > 0) 1 else -1
        if (sign != 0 && sign != direction) return false
        sign = direction
        signedArea += p[i * 2] * p[j * 2 + 1] - p[j * 2] * p[i * 2 + 1]
    }
    return abs(signedArea) >= 0.0005f
}

/** In-memory only: neither imported photos nor camera captures reach app storage. */
internal object QREXDocumentProcessing {
    fun downsize(source: Bitmap): Bitmap {
        require(source.width > 0 && source.height > 0)
        val pixels = source.width.toLong() * source.height
        if (pixels <= QREX_DOCUMENT_MAX_PIXELS && source.config == Bitmap.Config.ARGB_8888) {
            return source
        }
        val ratio = min(1.0, kotlin.math.sqrt(QREX_DOCUMENT_MAX_PIXELS.toDouble() / pixels))
        val width = max(1, (source.width * ratio).roundToInt())
        val height = max(1, (source.height * ratio).roundToInt())
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)
        if (scaled.config == Bitmap.Config.ARGB_8888) return scaled
        val converted = scaled.copy(Bitmap.Config.ARGB_8888, false)
        // The resize intermediate must not remain allocated alongside the
        // original camera image and the final PDF page.
        if (scaled !== source) scaled.recycle()
        return converted
    }

    fun rotate(source: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return source
        require(normalized == 90 || normalized == 180 || normalized == 270)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
            Matrix().apply { postRotate(normalized.toFloat()) }, true)
    }

    fun readFromPicker(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val sourceStream = resolver.openInputStream(uri)
            ?: error("Document source unavailable")
        // A bounds-only decode intentionally returns null. Check the metadata,
        // not the (absent) Bitmap, before opening the stream a second time.
        sourceStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth in 1..30000 && bounds.outHeight in 1..30000)
        // Image decoders may round sampling down to a power of two. Choose
        // the next power explicitly so a huge provider image is never decoded
        // at a size above the in-memory page budget.
        var sample = 1
        while ((bounds.outWidth.toLong() / sample) *
            (bounds.outHeight.toLong() / sample) > QREX_DOCUMENT_MAX_PIXELS) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("Cannot decode document image")
        // Full EXIF support matters for photographed paper documents: simple
        // rotationDegrees silently drops horizontally mirrored phone images.
        // Reuse the tested QR-gallery orientation helper (all eight variants).
        val orientation = resolver.openInputStream(uri)?.use {
            runCatching {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val oriented = orientGalleryBitmap(decoded, orientation)
        val sized = downsize(oriented)
        if (oriented !== sized) oriented.recycle()
        return sized
    }

    fun cropPerspective(source: Bitmap, p: List<Float>): Bitmap {
        require(qrexDocumentQuadValid(p)) { "Invalid document corners" }
        val width = source.width.toFloat()
        val height = source.height.toFloat()
        val src = FloatArray(8) { i -> p[i] * if (i % 2 == 0) width else height }
        fun distance(a: Int, b: Int): Float = hypot(
            src[a * 2] - src[b * 2], src[a * 2 + 1] - src[b * 2 + 1]
        )
        val naturalW = ((distance(0, 1) + distance(2, 3)) / 2f).coerceAtLeast(1f)
        val naturalH = ((distance(1, 2) + distance(3, 0)) / 2f).coerceAtLeast(1f)
        val ratio = min(1.0, kotlin.math.sqrt(
            QREX_DOCUMENT_MAX_PIXELS.toDouble() / (naturalW * naturalH)
        ))
        val targetW = max(1, (naturalW * ratio).roundToInt())
        val targetH = max(1, (naturalH * ratio).roundToInt())
        val dest = floatArrayOf(0f, 0f, targetW.toFloat(), 0f,
            targetW.toFloat(), targetH.toFloat(), 0f, targetH.toFloat())
        val projection = Matrix()
        require(projection.setPolyToPoly(src, 0, dest, 0, 4))
        val result = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        Canvas(result).apply {
            drawColor(android.graphics.Color.WHITE)
            drawBitmap(source, projection, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        return result
    }
}

/** Independent of the existing saved QR code PDF export. */
internal object QREXDocumentPdf {
    suspend fun export(context: Context, pages: List<Bitmap>, destination: Uri) {
        require(pages.isNotEmpty() && pages.size <= QREX_DOCUMENT_MAX_PAGES)
        require(pages.all { it.width > 0 && it.height > 0 &&
            it.width.toLong() * it.height <= QREX_DOCUMENT_MAX_PIXELS })
        val pdf = PdfDocument()
        try {
            pages.forEachIndexed { index, image ->
                currentCoroutineContext().ensureActive()
                val landscape = image.width > image.height
                val pageWidth = if (landscape) 842 else 595
                val pageHeight = if (landscape) 595 else 842
                val page = pdf.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                )
                try {
                    val canvas = page.canvas
                    canvas.drawColor(android.graphics.Color.WHITE)
                    val fit = qrexDocumentFit(image.width, image.height, pageWidth, pageHeight)
                    canvas.drawBitmap(image, null, RectF(fit[0], fit[1], fit[2], fit[3]),
                        Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                } finally {
                    pdf.finishPage(page)
                }
            }
            currentCoroutineContext().ensureActive()
            val stream: OutputStream = context.contentResolver.openOutputStream(destination, "w")
                ?: error("PDF destination unavailable")
            stream.use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }
}
