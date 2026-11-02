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
        pattern = rf"val {re.escape(name)} = Color\(0xFF([A-Fa-f0-9]{{6}})\)"
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

    def test_result_category_glyphs_have_graphic_contrast(self):
        # WCAG non-text contrast: all three gradient stops of each 32dp
        # white category pictogram must be readable (>= 3:1).
        result = self.main.split("val resultGlyphBrush = when {", 1)[1].split(
            "val resultGlyphStroke = when {", 1
        )[0]
        values = re.findall(r"Color\\(0xFF([A-Fa-f0-9]{6})\\)", result)
        self.assertEqual(len(values), 9, "Wi-Fi, telephone, event: three stops each")
        for color in values:
            with self.subTest(glyph_stop=color):
                self.assertGreaterEqual(contrast(color, "FFFFFF"), 3.0)

    def test_primary_ctas_share_one_accessible_brand_token(self):
        # The native reference button has transparent Material container over
        # an opaque three-stop Compose gradient. Verify every declared stop,
        # not just a removed flat-color literal. White text must remain AA.
        self.assertIn("val referenceButtonBrush = Brush.verticalGradient(", self.design)
        gradient = self.design.split(
            "val referenceButtonBrush = Brush.verticalGradient(", 1
        )[1].split("val referenceGlyphBrush", 1)[0]
        stops = re.findall(r"Color\(0xFF([A-Fa-f0-9]{6})\)", gradient)
        self.assertEqual(len(stops), 3, "Expected three explicit gradient stops")
        for color in stops:
            with self.subTest(stop=color):
                self.assertGreaterEqual(contrast(color, "FFFFFF"), 4.5)
        action = self.main.split("private fun PrimaryAction(", 1)[1].split(
            "private fun shareQrText(", 1
        )[0]
        self.assertIn("QrexDesign.referenceButtonBrush", action)
        self.assertIn("containerColor = Color.Transparent, contentColor = Color.White", action)
        self.assertIn("modifier.fillMaxWidth().heightIn(min = 54.dp)", action)
        self.assertIn("onClick = onClick, enabled = enabled", action)
        # The in-camera primary Gallery button overlays a live image and has
        # a dark 88% scrim with high-contrast white text plus cyan icon/
        # border. Outside the preview it retains the WCAG-AA action-blue.
        self.assertIn("else QrexDesign.actionBlue,", self.scanner)
        self.assertIn("QrexDesign.backgroundDeep.copy(alpha = .88f)", self.scanner)
        self.assertIn("contentColor = Color.White", self.scanner)
        self.assertGreaterEqual(contrast(self.color("backgroundDeep"), "FFFFFF"), 4.5)
        self.assertNotIn("Color(0xFF1677FF)", self.main)
        self.assertNotIn("Color(0xFF1677FF)", self.scanner)


if __name__ == "__main__":
    unittest.main()
