package com.brendigo.qrex

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.nio.ByteBuffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** QR decoder smoke cases exercise the actual offline ZXing implementation. */
class QREXQrDecoderInstrumentedTest {
    @Test fun centeredQrFromLargeGalleryFrameDecodesNormalAndInverted() {
        val payload = "https://example.org/qrex/scan-check"
        val width = 420
        val height = 420
        val gray = ByteArray(width * height) { 0xff.toByte() }
        val modules = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 120, 120)
        val left = (width - modules.width) / 2
        val top = (height - modules.height) / 2
        for (y in 0 until modules.height) for (x in 0 until modules.width) {
            gray[(y + top) * width + x + left] =
                if (modules.get(x, y)) 0.toByte() else 0xff.toByte()
        }
        assertEquals(payload, QREXQrDecoder.decodeLuma(gray, width, height, centerFallback = true))
        val inverted = ByteArray(gray.size) { index ->
            (gray[index].toInt() xor 0xff).toByte()
        }
        assertEquals(payload, QREXQrDecoder.decodeLuma(inverted, width, height, centerFallback = true))
        assertNull(QREXQrDecoder.decodeLuma(ByteArray(width * height), 0, height))
    }

    @Test fun yPlaneRespectsStrideAndPositionWithoutMutatingCameraBuffer() {
        val frame = ByteBuffer.wrap(byteArrayOf(
            90, 90, 90, 11, 22, 33, 0, 44, 55, 66, 0
        ))
        frame.position(3)
        val observed = QREXQrDecoder.copyLuma(frame, 3, 2, 4, 1)
        assertArrayEquals(byteArrayOf(11, 22, 33, 44, 55, 66), observed)
        assertEquals(3, frame.position())
    }
}
