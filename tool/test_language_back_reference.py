#!/usr/bin/env python3
"""Language & Export back-control accessibility regression contract."""
from pathlib import Path
import json
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"
LANGS = ROOT / "l10n/catalog.json"


class LanguageNavigationRegressionTests(unittest.TestCase):
    def test_back_uses_a_52dp_real_touch_surface_not_close_label(self):
        source = (MAIN / "QREXMoreDetails.kt").read_text("utf-8")
        self.assertIn('Modifier.size(52.dp).testTag("qrex-detail-back")', source)
        self.assertIn('Icons.Default.ArrowBack, contentDescription = tr("back")', source)
        self.assertNotIn('Icons.Default.ArrowBack, contentDescription = tr("close")', source)
        self.assertIn('testTag("qrex-language-option-$code")', source)
        self.assertIn('heightIn(min = 64.dp)', source)

    def test_every_language_includes_a_localized_back_label(self):
        catalog = json.loads(LANGS.read_text("utf-8"))
        self.assertEqual(23, len(catalog["languages"]))
        keys = tuple(catalog["languages"]["en"]["strings"])
        for code, info in catalog["languages"].items():
            with self.subTest(language=code):
                self.assertEqual(keys, tuple(info["strings"]))
                self.assertTrue(info["strings"]["back"].strip())

    def test_language_and_export_share_same_back_component(self):
        source = (MAIN / "QREXMoreDetails.kt").read_text("utf-8")
        self.assertEqual(source.count("QrexDetailHeader(tr("), 2)
        self.assertIn('QrexDetailHeader(tr("language"), onBack)', source)
        self.assertIn('QrexDetailHeader(tr("export_archive"), onBack)', source)


if __name__ == "__main__":
    unittest.main()
