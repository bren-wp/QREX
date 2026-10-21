package com.brendigo.qrex

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

/**
 * FLOSS QR reader shared by the camera and gallery.
 *
 * No Google ML Kit / Play Services dependency. Never retains a camera frame or Bitmap.
 * CameraX calls this on its background analyzer executor, and gallery calls it on Default.
 */
internal object QREXQrDecoder {
    private const val MAX_PIXELS = 8_000_000L
    private val HINTS = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE
    )

    internal fun decodeFrame(frame: ImageProxy): String? {
        val w = frame.width
        val h = frame.height
        if (w <= 0 || h <= 0 || w.toLong() * h > MAX_PIXELS) return null
        val yPlane = frame.planes.firstOrNull() ?: return null
        val gray = copyLuma(
            yPlane.buffer, w, h, yPlane.rowStride, yPlane.pixelStride
        ) ?: return null
        val (upright, outWidth, outHeight) =
            rotateLuma(gray, w, h, frame.imageInfo.rotationDegrees)
        return decodeLuma(upright, outWidth, outHeight)
    }

    /** Android Bitmap#getPixels only needs one row; limits memory on large gallery images. */
    internal fun decodeBitmap(bitmap: Bitmap): String? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0 || w.toLong() * h > MAX_PIXELS) return null
        val gray = ByteArray(w * h)
        val row = IntArray(w)
        for (y in 0 until h) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                val color = row[x]
                val luminance = ((color ushr 16 and 255) * 77 +
                    (color ushr 8 and 255) * 150 + (color and 255) * 29) ushr 8
                val alpha = color ushr 24 and 255
                gray[y * w + x] =
                    ((luminance * alpha + 255 * (255 - alpha)) / 255).toByte()
            }
        }
        return decodeLuma(gray, w, h)
    }

    /** Extract Y bytes while correctly respecting CameraX row and pixel strides. */
    internal fun copyLuma(
        plane: ByteBuffer, width: Int, height: Int, rowStride: Int, pixelStride: Int
    ): ByteArray? {
        if (width <= 0 || height <= 0 || rowStride <= 0 || pixelStride <= 0 ||
            width.toLong() * height > MAX_PIXELS) return null
        val buffer = plane.duplicate()
        buffer.rewind()
        val lastIndex = (height - 1).toLong() * rowStride +
            (width - 1).toLong() * pixelStride
        if (lastIndex >= buffer.limit()) return null
        val result = ByteArray(width * height)
        for (y in 0 until height) {
            val start = y * rowStride
            for (x in 0 until width) {
                result[y * width + x] = buffer.get(start + x * pixelStride)
            }
        }
        return result
    }

    /** Returns pixels and dimensions oriented clockwise by the CameraX rotation. */
    internal fun rotateLuma(
        data: ByteArray, width: Int, height: Int, degrees: Int
    ): Triple<ByteArray, Int, Int> {
        require(width > 0 && height > 0 && data.size == width * height)
        if (degrees !in listOf(90, 180, 270)) return Triple(data, width, height)
        val outWidth = if (degrees == 180) width else height
        val outHeight = if (degrees == 180) height else width
        val rotated = ByteArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val outX: Int
                val outY: Int
                when (degrees) {
                    90 -> { outX = height - 1 - y; outY = x }
                    180 -> { outX = width - 1 - x; outY = height - 1 - y }
                    else -> { outX = y; outY = width - 1 - x }
                }
                rotated[outY * outWidth + outX] = data[y * width + x]
            }
        }
        return Triple(rotated, outWidth, outHeight)
    }

    /** Try ordinary and inverse/white-on-black QR codes. */
    internal fun decodeLuma(data: ByteArray, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || width.toLong() * height != data.size.toLong() ||
            data.size.toLong() > MAX_PIXELS) return null
        val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
        val reader = MultiFormatReader()
        fun read(bitmap: BinaryBitmap): String? {
            return try {
                reader.decode(bitmap, HINTS).text?.takeIf { it.isNotEmpty() }
            } catch (_: ReaderException) {
                null
            } finally {
                reader.reset()
            }
        }
        return read(BinaryBitmap(HybridBinarizer(source)))
            ?: read(BinaryBitmap(HybridBinarizer(source.invert())))
    }
}
