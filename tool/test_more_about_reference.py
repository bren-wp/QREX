#!/usr/bin/env python3
"""Source checks for the native About and Export reference-screen behavior."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


class MoreAboutRegressionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.main = (ANDROID / "MainActivity.kt").read_text("utf-8")
        cls.detail = (ANDROID / "QREXMoreDetails.kt").read_text("utf-8")

    def test_about_is_native_and_site_is_optional(self):
        self.assertIn("var showAbout by remember", self.main)
        self.assertIn('testTag("qrex-more-about")', self.main)
        self.assertIn('testTag("qrex-about-dialog")', self.main)
        self.assertIn('testTag("qrex-about-website")', self.main)
        self.assertIn("getPackageInfo(context.packageName, 0)", self.main)
        self.assertIn('Text(tr("developed_by")', self.main)
        self.assertIn('Text(tr("open_link"))', self.main)

    def test_export_preserves_full_translated_privacy_notice(self):
        self.assertIn("Text(tr(\"privacy_summary\"), fontSize = 14.sp", self.detail)
        self.assertNotIn("maxLines = 4,\n                    color = MaterialTheme.colorScheme.onPrimaryContainer",
                         self.detail)
        self.assertIn('testTag("qrex-export-pdf")', self.detail)
        self.assertIn("enabled = hasSaved", self.detail)
        self.assertIn('CreateDocument("application/pdf")', self.main)


if __name__ == "__main__":
    unittest.main()
