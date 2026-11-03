#!/usr/bin/env python3
"""Verify the native QREX Language UI remains a functioning offline destination.

These source contracts supplement (not replace) on-device screenshot and
instrumentation tests.  They must not depend on hardcoded sample version text.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MORE = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/QREXMoreDetails.kt"
MAIN = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt"


class LanguageReferenceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = MORE.read_text("utf-8")
        cls.main = MAIN.read_text("utf-8")
        cls.language = cls.source.split("internal fun QrexLanguageDetails(", 1)[1].split(
            "/** PDF export", 1
        )[0]

    def test_every_language_row_has_a_real_tappable_persistent_action(self):
        self.assertIn("listOf(\"system\") + QREXStrings.languages", self.language)
        self.assertIn("store.updateLanguage(code)", self.language)
        self.assertIn("this.selected = selected", self.language)
        self.assertIn("onClick = {", self.language)
        self.assertIn("modifier = Modifier.clearAndSetSemantics {}", self.language)
        self.assertIn("onClick = null", self.language)
        self.assertIn("LazyColumn(", self.language)
        self.assertIn('testTag("qrex-language-list")', self.language)
        self.assertIn('testTag("qrex-language-option-$code")', self.language)

    def test_3d_flag_plates_have_opaque_surface_and_real_rgb_highlight(self):
        self.assertIn('testTag("qrex-language-flag-$code")', self.language)
        self.assertIn("Modifier.size(36.dp)", self.language)
        self.assertIn("shadowElevation = if (selected) 4.dp else 2.dp", self.language)
        self.assertIn("border = BorderStroke(1.dp,", self.language)
        self.assertIn("QrexDesign.actionBlue", self.language)
        self.assertIn("QrexDesign.accentBright.copy(alpha = .70f)", self.language)
        self.assertIn("Brush.verticalGradient(", self.language)
        self.assertIn("Color.White.copy(alpha = if (selected) .20f else .10f)", self.language)
        self.assertIn("Color.Transparent", self.language)
        self.assertNotIn("background.copy(alpha", self.language)

    def test_real_detail_back_and_clean_locale_selection_preserved(self):
        self.assertIn("QrexDetailHeader(tr(\"language\"), onBack)", self.language)
        self.assertIn('detailPage = "language"', self.main)
        self.assertIn("if (!store.updateLanguage(code))", self.language)
        self.assertNotIn("clearHistory()", self.language)
        self.assertNotIn("CreateDocument(", self.language)
        self.assertNotIn("http", self.language)


if __name__ == "__main__":
    unittest.main()
