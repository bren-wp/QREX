package com.brendigo.qrex

import kotlin.math.max
import kotlin.math.min

/** Normalized [TL, TR, BR, BL] corners, independent of screen density. */
internal val QREX_DOCUMENT_FULL_CORNERS: List<Float> =
    listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)

internal val QREX_DOCUMENT_INSET_CORNERS: List<Float> =
    listOf(.05f, .05f, .95f, .05f, .95f, .95f, .05f, .95f)

/** Switching from a perspective polygon to a rectangular selection never crops pixels. */
internal fun qrexDocumentBoundingRectangle(corners: List<Float>): List<Float> {
    if (!qrexDocumentQuadValid(corners)) return QREX_DOCUMENT_FULL_CORNERS
    val left = (0..3).minOf { corners[it * 2] }
    val right = (0..3).maxOf { corners[it * 2] }
    val top = (0..3).minOf { corners[it * 2 + 1] }
    val bottom = (0..3).maxOf { corners[it * 2 + 1] }
    return listOf(left, top, right, top, right, bottom, left, bottom)
}

/**
 * Move exactly one perspective handle; or resize a true rectangle using its
 * diagonally opposite handle as the fixed anchor. Reject collapsed, crossed,
 * concave, nonfinite and too-small selections immediately during the gesture.
 */
internal fun qrexDocumentMoveCorner(
    corners: List<Float>, selected: Int, x: Float, y: Float,
    rectangle: Boolean, minimumSpan: Float = .02f
): List<Float> {
    if (selected !in 0..3 || !qrexDocumentQuadValid(corners) ||
        !x.isFinite() || !y.isFinite() ||
        !minimumSpan.isFinite() || minimumSpan <= 0f) return corners
    val nx = x.coerceIn(0f, 1f)
    val ny = y.coerceIn(0f, 1f)
    if (rectangle) {
        val opposite = (selected + 2) % 4
        val anchorX = corners[opposite * 2]
        val anchorY = corners[opposite * 2 + 1]
        // Crossing the opposite corner would flip handle identities and make
        // repeated drags jump between corners. Keep the previous valid shape.
        if ((selected == 0 || selected == 3) && nx >= anchorX - minimumSpan) return corners
        if ((selected == 1 || selected == 2) && nx <= anchorX + minimumSpan) return corners
        if ((selected == 0 || selected == 1) && ny >= anchorY - minimumSpan) return corners
        if ((selected == 2 || selected == 3) && ny <= anchorY + minimumSpan) return corners
        val left = min(nx, anchorX)
        val right = max(nx, anchorX)
        val top = min(ny, anchorY)
        val bottom = max(ny, anchorY)
        val rect = listOf(left, top, right, top, right, bottom, left, bottom)
        return if (qrexDocumentQuadValid(rect)) rect else corners
    }
    val updated = corners.toMutableList()
    updated[selected * 2] = nx
    updated[selected * 2 + 1] = ny
    // Avoid numerically unstable tiny output pages on an otherwise valid quad.
    val xs = (0..3).map { updated[it * 2] }
    val ys = (0..3).map { updated[it * 2 + 1] }
    if (xs.max() - xs.min() < minimumSpan ||
        ys.max() - ys.min() < minimumSpan) return corners
    return if (qrexDocumentQuadValid(updated)) updated else corners
}
