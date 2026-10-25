#!/usr/bin/env python3
"""Source regression checks for the real Android Create QR screen.

These checks complement, but do not replace, emulator Compose tests and
pixel measurements on matching, rectified screenshot references.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt"
DESIGN = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/QREXDesign.kt"


class CreateReferenceRegressionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = MAIN.read_text("utf-8")
        cls.design = DESIGN.read_text("utf-8")

    def test_four_quick_and_four_advanced_types_are_reachable(self):
        self.assertIn("val quickTypes = listOf(0, 1, 2, 6)", self.source)
        self.assertIn("val overflowTypes = listOf(3, 4, 5, 7)", self.source)
        self.assertIn("listOf(3, 4, 5, 7).chunked(2)", self.source)
        self.assertIn('testTag("qrex-create-more-types")', self.source)
        self.assertIn('testTag("qrex-create-advanced-types")', self.source)
        self.assertIn("showAdvancedTypes = true", self.source)
        self.assertIn("selected = kind == index", self.source)

    def test_accessible_disclosure_and_pinned_actions(self):
        self.assertIn(".heightIn(min = 52.dp)", self.source)
        self.assertIn(".heightIn(min = 54.dp).semantics", self.source)
        self.assertIn('testTag("qrex-create-show")', self.source)
        self.assertIn('testTag("qrex-create-save")', self.source)
        self.assertLess(self.source.index('testTag("qrex-create-show")'),
                        self.source.index('testTag("qrex-create-save")'))
        self.assertIn("Modifier.fillMaxSize().imePadding()", self.source)
        self.assertIn("if (!imeVisible && layout.showQrPreview)", self.source)
        self.assertNotIn("BoxWithConstraints(", self.source)

    def test_email_phone_ime_and_user_selected_locale(self):
        self.assertIn("3 -> KeyboardType.Email", self.source)
        self.assertIn("4 -> KeyboardType.Phone", self.source)
        self.assertIn("val displayLanguage = LocalLanguage.current", self.source)
        self.assertIn('displayLanguage == "system"', self.source)
        self.assertIn("Locale.forLanguageTag(displayLanguage)", self.source)

    def test_security_and_existing_export_unchanged(self):
        self.assertIn("confirmWifiSave = true", self.source)
        self.assertIn('tr("wifi_save_warning")', self.source)
        self.assertIn('CreateDocument("image/png")', self.source)
        self.assertIn("reveal = false", self.source)
        self.assertIn("val stackChoices = widthDp < 400 || fontScale >= 1.30f",
                      self.design)


if __name__ == "__main__":
    unittest.main()
