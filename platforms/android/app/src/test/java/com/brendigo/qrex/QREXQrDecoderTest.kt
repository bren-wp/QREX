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
            mapOf(EncodeHintType.MARGIN to 4)
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
    fun scan_standard_qr_without_mlkit() {
        val value = "https://brendigo.com/qrex?ref=fdroid"
        assertEquals(value, QREXQrDecoder.decodeLuma(qr(value), 251, 197))
    }

    @Test
    fun scan_inverted_qr() {
        val value = "WIFI:T:WPA;S:PrivateNetwork;P:open-source;;"
        val inverted = qr(value).map { (255 - (it.toInt() and 255)).toByte() }.toByteArray()
        assertEquals(value, QREXQrDecoder.decodeLuma(inverted, 251, 197))
    }

    @Test
    fun rotation_90_and_270_preserves_qr() {
        val value = "QREX F-Droid rotation test"
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
    fun invalid_images_do_not_decode() {
        assertNull(QREXQrDecoder.decodeLuma(byteArrayOf(), 0, 0))
        assertNull(QREXQrDecoder.decodeLuma(byteArrayOf(0), 20, 20))
        assertNull(QREXQrDecoder.copyLuma(ByteBuffer.wrap(byteArrayOf()), 10, 10, 10, 1))
        assertNull(QREXQrDecoder.decodeLuma(ByteArray(80 * 80) { 255.toByte() }, 80, 80))
    }
}
