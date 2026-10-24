#!/usr/bin/env python3
"""WCAG AA contrast guardrails for QREX premium Android action surfaces.

These are deterministic calculations against real Kotlin design tokens, not
subjective visual checks or a substitute for screen-reader/device QA.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
NATIVE = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


def luminance(hex_rgb: str) -> float:
    values = [int(hex_rgb[i:i + 2], 16) / 255.0 for i in (0, 2, 4)]
    channels = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4
                for v in values]
    return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]


def contrast(first: str, second: str) -> float:
    high, low = sorted((luminance(first), luminance(second)), reverse=True)
    return (high + 0.05) / (low + 0.05)


class PremiumAccessibilityContrastTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        cls.main = (NATIVE / "MainActivity.kt").read_text("utf-8")
        cls.scanner = (NATIVE / "QREXScanner.kt").read_text("utf-8")

    @classmethod
    def color(cls, name):
        pattern = rf"val {re.escape(name)} = Color\\(0xFF([A-Fa-f0-9]{{6}})\\)"
        match = re.search(pattern, cls.design)
        if match is None:
            raise AssertionError(f"Missing Kotlin color token {name}")
        return match.group(1)

    def test_white_on_primary_action_meets_wcag_aa(self):
        self.assertGreaterEqual(contrast(self.color("actionBlue"), "FFFFFF"), 4.5)
        self.assertIn("val accent = actionBlue", self.design)

    def test_muted_text_and_cyan_navigation_readable_on_dark(self):
        for color in ("muted", "accentBright"):
            self.assertGreaterEqual(contrast(self.color(color), self.color("background")), 4.5)
        self.assertGreaterEqual(
            contrast(self.color("muted"), self.color("surface")), 4.5
        )

    def test_primary_ctas_share_one_accessible_brand_token(self):
        self.assertIn(
            "containerColor = QrexDesign.actionBlue, contentColor = Color.White",
            self.main,
        )
        self.assertIn("containerColor = QrexDesign.actionBlue,", self.scanner)
        self.assertNotIn("Color(0xFF1677FF)", self.main)
        self.assertNotIn("Color(0xFF1677FF)", self.scanner)


if __name__ == "__main__":
    unittest.main()
