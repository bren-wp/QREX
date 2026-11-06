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
        # Settings category plates intentionally have a stronger native lift.
        self.assertIn("shadowElevation = 4.dp", self.design)
        self.assertGreaterEqual(self.design.count("shadowElevation = 2.dp"), 1)
        self.assertNotIn("InfiniteTransition", self.design)

    def test_phone_red_is_separate_from_destructive_actions(self):
        self.assertIn("val success = Color(0xFF19CFAB)", self.design)
        self.assertIn("val phoneTint = Color(0xFFFF7488)", self.design)
        self.assertIn('item.raw.startsWith("tel:", ignoreCase = true) -> QrexDesign.phoneTint', self.main)
        self.assertIn('item.raw.contains("BEGIN:VCARD", ignoreCase = true) -> QrexDesign.success', self.main)
        self.assertIn("color = if (destructive)", self.design)
        self.assertNotIn('item.raw.startsWith("tel:", ignoreCase = true) -> QrexDesign.danger', self.main)

    def test_more_uses_accessible_rows_with_large_font(self):
        self.assertIn('testTag("qrex-more-settings-stack")', self.main)
        self.assertNotIn("useCompactMoreList(", self.main)
        self.assertIn("modifier = modifier.fillMaxWidth().heightIn(min = 72.dp)", self.design)
        self.assertIn('testTag("qrex-more-delete")', self.main)
        self.assertIn("titleTextSize", self.design)
        self.assertIn("supportingTextSize", self.design)


if __name__ == "__main__":
    unittest.main()
