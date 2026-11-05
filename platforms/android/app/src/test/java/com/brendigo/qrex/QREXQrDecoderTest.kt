package com.brendigo.qrex

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class QREXQrDecoderTest {
    private fun qr(value: String, width: Int = 251, height: Int = 197): ByteArray {
        val matrix = QRCodeWriter().encode(
            value, BarcodeFormat.QR_CODE, width, height,
            mapOf(EncodeHintType.MARGIN to 4,
                EncodeHintType.CHARACTER_SET to "UTF-8")
        )
        return ByteArray(width * height) { i ->
            if (matrix[i % width, i / width]) 0 else 255.toByte()
        }
    }

    @Test
    fun camera_frames_are_sampled_without_throttling_gallery() {
        assertTrue(cameraFrameDue(12_000L, 0L))
        assertFalse(cameraFrameDue(12_080L, 12_000L))
        assertFalse(cameraFrameDue(12_179L, 12_000L))
        assertTrue(cameraFrameDue(12_180L, 12_000L))
        assertTrue(cameraFrameDue(13_500L, 12_000L))
        assertFalse(cameraFrameDue(12_000L, 13_500L))
    }

    @Test
    fun gallery_picker_takes_precedence_over_live_camera() {
        assertTrue(cameraFrameAllowed(12_000L, 0L, false, false))
        assertFalse(cameraFrameAllowed(12_000L, 0L, true, false))
        assertFalse(cameraFrameAllowed(12_000L, 0L, false, true))
        assertFalse(cameraFrameAllowed(12_080L, 12_000L, false, false))
        assertTrue(cameraFrameAllowed(12_180L, 12_000L, false, false))
    }

    @Test
    fun scan_standard_qr() {
        val value = "https://brendigo.com/qrex?ref=android"
        assertEquals(value, QREXQrDecoder.decodeLuma(qr(value), 251, 197))
    }

    @Test
    fun scan_utf8_croatian_contact_qr_without_losing_accents() {
        val card = QRContent.contact(
            "Čić Đorđe Šimić Željko", "+385 91 555 1234", "osoba@example.hr"
        )!!
        assertEquals(card, QREXQrDecoder.decodeLuma(qr(card, 420, 420), 420, 420))
    }

    @Test
    fun scan_inverted_qr() {
        val value = "WIFI:T:WPA;S:PrivateNetwork;P:open-source;;"
        val inverted = qr(value).map { (255 - (it.toInt() and 255)).toByte() }.toByteArray()
        assertEquals(value, QREXQrDecoder.decodeLuma(inverted, 251, 197))
    }

    @Test
    fun rotation_90_and_270_preserves_qr() {
        val value = "QREX rotation test"
        val base = qr(value)
        for (degrees in listOf(90, 180, 270)) {
            val (pixels, w, h) = QREXQrDecoder.rotateLuma(base, 251, 197, degrees)
            assertEquals(251 * 197, pixels.size)
            assertEquals(value, QREXQrDecoder.decodeLuma(pixels, w, h))
        }
    }

    @Test
    fun y_plane_padded_row_and_pixel_stride() {
        val input = qr("stride-data", 251, 197)
        val width = 251
        val height = 197
        val rowStride = 520
        val raw = ByteArray(height * rowStride) { 119.toByte() }
        for (y in 0 until height) for (x in 0 until width) {
            raw[y * rowStride + x * 2] = input[y * width + x]
        }
        val output = QREXQrDecoder.copyLuma(ByteBuffer.wrap(raw), width, height, rowStride, 2)
        assertArrayEquals(input, output)
        assertEquals("stride-data", QREXQrDecoder.decodeLuma(output!!, width, height))
    }

    @Test
    fun positioned_packed_y_plane_reads_only_its_window_without_mutating_camerax_buffer() {
        val width = 251
        val height = 197
        val prefix = 17
        val rowStride = width + 7
        val expected = qr("QREX packed plane window", width, height)
        val bytes = ByteArray(prefix + height * rowStride + 11) { 0x5a.toByte() }
        for (y in 0 until height) {
            expected.copyInto(bytes, prefix + y * rowStride, y * width, (y + 1) * width)
        }
        val plane = ByteBuffer.wrap(bytes).apply {
            position(prefix)
            limit(prefix + height * rowStride)
        }
        val actual = QREXQrDecoder.copyLuma(plane, width, height, rowStride, 1)
        assertArrayEquals(expected, actual)
        assertEquals(prefix, plane.position())
        assertEquals(prefix + height * rowStride, plane.limit())
        assertEquals("QREX packed plane window",
            QREXQrDecoder.decodeLuma(actual!!, width, height))
    }

    @Test
    fun positioned_direct_interleaved_y_plane_respects_pixel_stride() {
        val width = 251
        val height = 197
        val prefix = 9
        val rowStride = width * 2 + 3
        val expected = qr("QREX direct Y plane", width, height)
        val plane = ByteBuffer.allocateDirect(prefix + height * rowStride + 4)
        for (y in 0 until height) {
            for (x in 0 until width) {
                plane.put(prefix + y * rowStride + x * 2, expected[y * width + x])
            }
        }
        plane.position(prefix)
        plane.limit(prefix + height * rowStride)
        val actual = QREXQrDecoder.copyLuma(plane, width, height, rowStride, 2)
        assertArrayEquals(expected, actual)
        assertEquals(prefix, plane.position())
        assertEquals("QREX direct Y plane",
            QREXQrDecoder.decodeLuma(actual!!, width, height))
    }

    @Test
    fun y_plane_rejects_truncated_buffer_window_even_with_unused_prefix_capacity() {
        val plane = ByteBuffer.allocate(64).apply {
            position(12)
            limit(21)
        }
        assertNull(QREXQrDecoder.copyLuma(plane, 3, 3, 4, 1))
        assertEquals(12, plane.position())
        assertEquals(21, plane.limit())
    }

    @Test
    fun invalid_images_do_not_decode() {
        assertNull(QREXQrDecoder.decodeLuma(byteArrayOf(), 0, 0))
        assertNull(QREXQrDecoder.decodeLuma(byteArrayOf(0), 20, 20))
        assertNull(QREXQrDecoder.copyLuma(ByteBuffer.wrap(byteArrayOf()), 10, 10, 10, 1))
        assertNull(QREXQrDecoder.decodeLuma(ByteArray(80 * 80) { 255.toByte() }, 80, 80))
    }
}

