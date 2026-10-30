#!/usr/bin/env python3
"""Pure-standard-library pixel comparison tests for QREX reference captures."""
from pathlib import Path
import struct
import sys
import tempfile
import unittest
import zlib

sys.path.insert(0, str(Path(__file__).resolve().parent))
from compare_ui_pixels import compare, read_png, main, Pixels


def sample_png(width, height, rgba, color_type=6):
    channels = 4 if color_type == 6 else 3
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data))
    raw = b"".join(b"\x00" + rgba[i * width * channels:(i + 1) * width * channels]
                   for i in range(height))
    ihdr = struct.pack(">IIBBBBB", width, height, 8, color_type, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


class PixelAuditTests(unittest.TestCase):
    def test_exact_match_and_one_pixel_change(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "test.png"
            p.write_bytes(sample_png(2, 1, bytes((0, 0, 0, 255, 2, 4, 6, 255))))
            original = read_png(p)
            self.assertEqual((original.width, original.height), (2, 1))
            self.assertEqual(compare(original, original).mismatched, 0)
            changed = Pixels(2, 1, bytes((0, 0, 0, 255, 7, 4, 6, 255)))
            d = compare(original, changed)
            self.assertEqual(d.mismatched, 1)
            self.assertEqual(d.max_channel_delta, 5)
            self.assertEqual(d.bounds, (1, 0, 1, 0))
            self.assertEqual(d.percent, 50.0)
            self.assertEqual(compare(original, changed, tolerance=5).mismatched, 0)

    def test_rgb_is_converted_to_opaque_rgba(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "rgb.png"
            p.write_bytes(sample_png(1, 1, bytes((11, 33, 99)), color_type=2))
            self.assertEqual(read_png(p).rgba, bytes((11, 33, 99, 255)))

    def test_different_viewport_fails_closed(self):
        with self.assertRaisesRegex(ValueError, "Viewport"):
            compare(Pixels(1, 1, b"\x00\x00\x00\xff"),
                    Pixels(2, 1, b"\x00\x00\x00\xff" * 2))

    def test_invalid_png_does_not_produce_false_parity(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "invalid.png"
            p.write_bytes(sample_png(1, 1, b"\xff" * 4)[:-2])
            with self.assertRaises(ValueError):
                read_png(p)

    def test_main_checks_real_file_names_and_exits_on_mismatch(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            ref, actual = root / "reference", root / "actual"
            ref.mkdir()
            actual.mkdir()
            a = sample_png(1, 1, bytes((1, 2, 3, 255)))
            b = sample_png(1, 1, bytes((1, 2, 4, 255)))
            (ref / "android-create.png").write_bytes(a)
            (actual / "android-create.png").write_bytes(a)
            args = ["--reference-dir", str(ref), "--actual-dir", str(actual),
                    "--screen", "create"]
            self.assertEqual(main(args), 0)
            (actual / "android-create.png").write_bytes(b)
            self.assertEqual(main(args), 1)
            self.assertEqual(main(args + ["--channel-tolerance", "1"]), 0)

    def test_privacy_modal_is_a_selectable_exact_pixel_audit_target(self):
        with tempfile.TemporaryDirectory() as td:
            folder = Path(td)
            ref, actual = folder / "reference", folder / "emulator"
            ref.mkdir()
            actual.mkdir()
            sample = sample_png(2, 1, bytes((6, 11, 31, 255)) * 2)
            (ref / "android-privacy.png").write_bytes(sample)
            (actual / "android-privacy.png").write_bytes(sample)
            args = ["--reference-dir", str(ref), "--actual-dir", str(actual),
                    "--screen", "privacy", "--max-mismatch-percent", "0"]
            self.assertEqual(main(args), 0)
            (actual / "android-privacy.png").write_bytes(
                sample_png(2, 1, bytes((6, 11, 31, 255)) +
                           bytes((7, 11, 31, 255))))
            self.assertEqual(main(args), 1)

    def test_about_modal_is_a_selectable_exact_pixel_audit_target(self):
        with tempfile.TemporaryDirectory() as td:
            folder = Path(td)
            ref, actual = folder / "target", folder / "emulator"
            ref.mkdir()
            actual.mkdir()
            target = sample_png(2, 1, bytes((6, 11, 31, 255)) * 2)
            (ref / "android-about.png").write_bytes(target)
            (actual / "android-about.png").write_bytes(target)
            flags = ["--reference-dir", str(ref), "--actual-dir", str(actual),
                     "--screen", "about", "--max-mismatch-percent", "0"]
            self.assertEqual(main(flags), 0)
            (actual / "android-about.png").write_bytes(
                sample_png(2, 1, bytes((6, 11, 31, 255)) +
                           bytes((7, 11, 31, 255))))
            self.assertEqual(main(flags), 1)


if __name__ == "__main__":
    unittest.main()
