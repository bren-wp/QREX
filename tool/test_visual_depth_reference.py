#!/usr/bin/env python3
"""Visual hierarchy regressions; do not claim mathematical pixel equality."""
from pathlib import Path
import unittest

ANDROID = Path(__file__).resolve().parents[1] / "platforms/android/app/src/main/java/com/brendigo/qrex"


class VisualDepthRegressionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.design = (ANDROID / "QREXDesign.kt").read_text("utf-8")
        cls.main = (ANDROID / "MainActivity.kt").read_text("utf-8")

    def test_card_depth_remains_native_and_static(self):
        self.assertIn("shadowElevation = 3.dp", self.design)
        self.assertGreaterEqual(self.design.count("shadowElevation = 2.dp"), 2)
        self.assertNotIn("InfiniteTransition", self.design)

    def test_contact_and_phone_are_not_destructive_red(self):
        self.assertIn("val success = Color(0xFF19CFAB)", self.design)
        self.assertIn('item.raw.startsWith("tel:", ignoreCase = true) -> QrexDesign.success', self.main)
        self.assertIn('item.raw.contains("BEGIN:VCARD", ignoreCase = true) -> QrexDesign.success', self.main)
        self.assertNotIn('item.raw.startsWith("tel:", ignoreCase = true) -> QrexDesign.danger', self.main)

    def test_more_uses_accessible_rows_with_large_font(self):
        self.assertIn("screenWidthDp < 300 || fontScale >= 1.30f", self.main)
        self.assertIn("useCompactMoreList(", self.main)
        self.assertIn('testTag("qrex-more-delete")', self.main)
        self.assertIn("titleTextSize", self.design)
        self.assertIn("supportingTextSize", self.design)


if __name__ == "__main__":
    unittest.main()
