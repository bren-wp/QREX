#!/usr/bin/env python3
"""Auditable structural deltas for the user's Splash and Home phone boards."""
import unittest
from measure_splash_home_geometry import geometry_report, REFERENCE


class SplashHomeGeometryReportTests(unittest.TestCase):
    def test_reference_fractions_and_new_timer_are_recorded(self):
        report = geometry_report()
        self.assertEqual(3500, report["derived_source_landmarks"]["splash_duration_ms"])
        self.assertEqual(
            set(REFERENCE),
            set(report["reference_landmarks"]),
        )
        self.assertIsNone(report["pixel_mismatch_percent"])
        self.assertEqual("requires matching unframed screenshots",
                         report["pixel_measurement_status"])
        self.assertEqual([[320, 568], [393, 852], [800, 1280]],
                         report["responsive_test_profiles_dp"])

    def test_mobile_layout_matches_measured_landmarks_with_documented_tolerance(self):
        gap = geometry_report()["absolute_geometry_gap_percentage_points"]
        # This tests source geometry only, never asserts exact screenshot parity.
        self.assertLess(gap["home_bracket_width_fraction"], 1.0)
        self.assertLess(gap["home_bracket_center_in_camera_fraction"], 2.0)
        self.assertLess(gap["splash_logo_width_fraction"], 3.0)
        self.assertLess(gap["splash_progress_width_fraction"], 3.0)


if __name__ == "__main__":
    unittest.main()
