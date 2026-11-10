"""Six-screen screenshot audit must never assert parity from framed artwork."""
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from types import SimpleNamespace

from measure_six_screen_reference import SCREENS, audit


class SixScreenReferenceTests(unittest.TestCase):
    def create_inputs(self, directory: Path):
        for name in SCREENS:
            (directory / f"android-{name}.png").write_bytes(b"real capture fixture placeholder")

    def test_all_six_present_but_no_false_pixel_parity(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            self.create_inputs(folder)
            with patch("measure_six_screen_reference.read_png",
                       return_value=SimpleNamespace(width=1080, height=1920)):
                result = audit(folder)
            self.assertEqual(set(SCREENS), set(result["screens"]))
            self.assertIsNone(result["all_exact_1_to_1"])
            for row in result["screens"].values():
                self.assertEqual("illustrated_reference_not_pixel_comparable",
                                 row["status"])
                self.assertIsNone(row["exact_mismatch_percent"])
                self.assertEqual([1080, 1920], row["viewport_px"])
                self.assertEqual(64, len(row["actual_sha256"]))

    def test_real_flat_reference_reports_actual_zero_or_nonzero_mismatch(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            self.create_inputs(folder)
            with patch("measure_six_screen_reference.read_png",
                       return_value=SimpleNamespace(width=1080, height=1920)):
                with patch("measure_six_screen_reference.compare",
                           return_value=SimpleNamespace(
                               mismatched=5, percent=0.00024113,
                               max_channel_delta=3, bounds=(10, 20, 11, 22))):
                    result = audit(folder, folder)
            self.assertFalse(result["all_exact_1_to_1"])
            self.assertEqual(5, result["screens"]["splash"]["exact_mismatched_pixels"])
            self.assertEqual(0.000241, result["screens"]["splash"]["exact_mismatch_percent"])


if __name__ == "__main__":
    unittest.main()
