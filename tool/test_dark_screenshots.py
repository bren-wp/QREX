#!/usr/bin/env python3
"""Small standard-library fixtures for real Android screenshot dark-pixel gate."""
import struct
import tempfile
import unittest
import zlib
from pathlib import Path

from check_dark_screenshots import verify_dark


def png(color: tuple[int, int, int]) -> bytes:
    w, h = 120, 160
    rgba = bytes((*color, 255))
    raw = (b"\x00" + rgba * w) * h

    def chunk(t: bytes, data: bytes) -> bytes:
        return (struct.pack(">I", len(data)) + t + data
                + struct.pack(">I", zlib.crc32(t + data) & 0xFFFFFFFF))

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw))
            + chunk(b"IEND", b""))


class DarkScreenshotGateTests(unittest.TestCase):
    def test_midnight_and_brand_blue_pass(self):
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder) / "android-more.png"
            for color in ((4, 13, 32), (15, 56, 126)):
                p.write_bytes(png(color))
                verify_dark(p)

    def test_light_theme_fails_closed(self):
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder) / "android-create.png"
            p.write_bytes(png((249, 251, 255)))
            with self.assertRaisesRegex(ValueError, "dark Android header mismatch"):
                verify_dark(p)

    def test_corrupt_png_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder) / "android-history.png"
            p.write_bytes(b"not a PNG")
            with self.assertRaises(ValueError):
                verify_dark(p)


if __name__ == "__main__":
    unittest.main()
