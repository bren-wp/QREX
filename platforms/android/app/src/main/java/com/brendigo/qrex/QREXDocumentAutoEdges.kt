package com.brendigo.qrex

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

/**
 * Offline, conservative suggestion of a paper-shaped rectangle.
 *
 * This is intentionally NOT described as perfect page detection:
 * pages with similar colors to their background, heavy shadows or complex
 * perspective can require the user's manual four-corner crop.
 * The routine samples at most 96 x 128 pixels and never stores a photo.
 */
internal fun qrexDocumentSuggestEdges(source: Bitmap): List<Float>? {
    if (source.isRecycled || source.width < 64 || source.height < 64 ||
        source.width.toLong() * source.height > QREX_DOCUMENT_MAX_PIXELS) return null
    val cols = minOf(96, source.width)
    val rows = minOf(128, source.height)
    val luminance = IntArray(cols * rows)
    val border = ArrayList<Int>(2 * cols + 2 * rows)
    val center = ArrayList<Int>(cols * rows / 8)
    for (y in 0 until rows) {
        val sy = ((y + .5) * source.height / rows).toInt().coerceAtMost(source.height - 1)
        for (x in 0 until cols) {
            val sx = ((x + .5) * source.width / cols).toInt().coerceAtMost(source.width - 1)
            val pixel = source.getPixel(sx, sy)
            val alpha = Color.alpha(pixel)
            val r = (Color.red(pixel) * alpha + 255 * (255 - alpha) + 127) / 255
            val g = (Color.green(pixel) * alpha + 255 * (255 - alpha) + 127) / 255
            val b = (Color.blue(pixel) * alpha + 255 * (255 - alpha) + 127) / 255
            val luma = (299 * r + 587 * g + 114 * b + 500) / 1000
            luminance[y * cols + x] = luma
            if (x < 2 || y < 2 || x >= cols - 2 || y >= rows - 2) border.add(luma)
            if (x in (cols / 3)..(2 * cols / 3) &&
                y in (rows / 3)..(2 * rows / 3)) center.add(luma)
        }
    }
    if (border.isEmpty() || center.isEmpty()) return null
    border.sort()
    center.sort()
    val surroundings = border[border.size / 2]
    // Upper quartile tolerates a few black text lines on white paper.
    val paper = center[(center.size * 3 / 4).coerceAtMost(center.lastIndex)]
    if (abs(paper - surroundings) < 40) return null
    val threshold = (paper + surroundings) / 2
    val isLightPaper = paper > surroundings
    val rowCounts = IntArray(rows)
    val colCounts = IntArray(cols)
    for (y in 0 until rows) for (x in 0 until cols) {
        val light = luminance[y * cols + x] >= threshold
        if (light == isLightPaper) {
            rowCounts[y]++
            colCounts[x]++
        }
    }
    val firstRow = rowCounts.indexOfFirst { it > cols / 5 }
    val lastRow = rowCounts.indexOfLast { it > cols / 5 }
    val firstCol = colCounts.indexOfFirst { it > rows / 5 }
    val lastCol = colCounts.indexOfLast { it > rows / 5 }
    if (firstRow < 0 || firstCol < 0 || lastRow - firstRow < 8 ||
        lastCol - firstCol < 8) return null
    // One grid cell of external margin prevents clipping paper edge pixels.
    val left = (firstCol - 1).coerceAtLeast(0).toFloat() / cols
    val right = (lastCol + 2).coerceAtMost(cols).toFloat() / cols
    val top = (firstRow - 1).coerceAtLeast(0).toFloat() / rows
    val bottom = (lastRow + 2).coerceAtMost(rows).toFloat() / rows
    if (right - left < .10f || bottom - top < .10f) return null
    val result = listOf(left, top, right, top, right, bottom, left, bottom)
    return result.takeIf(::qrexDocumentQuadValid)
}
