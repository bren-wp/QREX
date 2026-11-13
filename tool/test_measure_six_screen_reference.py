"""Six-screen screenshot audit must never assert parity from framed artwork."""
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from types import SimpleNamespace

from measure_six_screen_reference import SCREENS, ALL_SCREENS, ALL_VIEW_STATES, audit


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

    def test_all_twelve_real_screen_captures_have_individual_evidence(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            for name in ALL_SCREENS:
                (folder / f"android-{name}.png").write_bytes(
                    f"actual-{name}".encode("utf-8")
                )
            with patch("measure_six_screen_reference.read_png",
                       return_value=SimpleNamespace(width=1080, height=1920)):
                report = audit(folder, screens=ALL_SCREENS)
            self.assertEqual(12, len(report["screens"]))
            self.assertEqual(set(ALL_SCREENS), set(report["screens"]))
            self.assertIsNone(report["all_exact_1_to_1"])
            for name in ALL_SCREENS:
                row = report["screens"][name]
                self.assertEqual(f"android-{name}.png", row["android_capture"])
                self.assertEqual([1080, 1920], row["viewport_px"])
                self.assertEqual(64, len(row["actual_sha256"]))
                self.assertIsNone(row["exact_mismatch_percent"])

    def test_all_twelve_fail_if_any_flat_reference_differs(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            for name in ALL_SCREENS:
                (folder / f"android-{name}.png").write_bytes(b"mock screenshot")
            with patch("measure_six_screen_reference.read_png",
                       return_value=SimpleNamespace(width=1080, height=1920)):
                with patch("measure_six_screen_reference.compare",
                           side_effect=[
                               SimpleNamespace(
                                   mismatched=1 if i == 11 else 0,
                                   percent=0.000048225 if i == 11 else 0.0,
                                   max_channel_delta=1 if i == 11 else 0,
                                   bounds=(0, 0, 0, 0) if i == 11 else None
                               )
                               for i in range(len(ALL_SCREENS))
                           ]):
                    report = audit(folder, folder, screens=ALL_SCREENS)
            self.assertFalse(report["all_exact_1_to_1"])
            self.assertEqual(1, report["screens"][ALL_SCREENS[-1]]
                             ["exact_mismatched_pixels"])
            self.assertEqual(0, report["screens"][ALL_SCREENS[0]]
                             ["exact_mismatched_pixels"])

    def test_terms_is_measured_from_real_responsive_capture_as_thirteenth_view(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            for name in ALL_SCREENS:
                (folder / f"android-{name}.png").write_bytes(b"real phone fixture")
            responsive = folder / "responsive"
            responsive.mkdir()
            (responsive / "reference-terms.png").write_bytes(b"real Terms fixture")
            with patch("measure_six_screen_reference.read_png",
                       return_value=SimpleNamespace(width=1080, height=2340)):
                report = audit(folder, screens=ALL_VIEW_STATES,
                               responsive_dir=responsive)
            self.assertEqual(13, len(report["screens"]))
            self.assertIsNone(report["all_exact_1_to_1"])
            self.assertEqual("responsive/reference-terms.png",
                             report["screens"]["terms"]["android_capture"])
            self.assertEqual([1080, 2340],
                             report["screens"]["terms"]["viewport_px"])
            self.assertEqual(64, len(report["screens"]["terms"]["actual_sha256"]))

    def test_terms_requires_original_actual_responsive_capture(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            with self.assertRaises(ValueError):
                audit(folder, screens=ALL_VIEW_STATES)

    def test_unknown_or_duplicate_screen_names_are_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            with self.assertRaises(ValueError):
                audit(folder, screens=("splash", "splash"))
            with self.assertRaises(ValueError):
                audit(folder, screens=("unknown",))
            with self.assertRaises(ValueError):
                audit(folder, screens=())

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
