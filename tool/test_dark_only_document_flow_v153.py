#!/usr/bin/env python3
"""Release-blocking dark-only interface, direct crop and distribution contracts."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"
class QREXDarkDocumentV153Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.main = (ANDROID / "MainActivity.kt").read_text("utf-8")
        cls.core = (ANDROID / "QREXCore.kt").read_text("utf-8")
        cls.scan = (ANDROID / "QREXScanner.kt").read_text("utf-8")
        cls.doc = (ANDROID / "QREXDocumentScanner.kt").read_text("utf-8")
    def test_single_dark_palette_and_no_theme_selector(self):
        self.assertIn("val colors = darkColorScheme(", self.main)
        for forbidden in ("lightColorScheme(", "isSystemInDarkTheme(",
                          "themeMode", "showTheme", "qrex-more-theme"):
            self.assertNotIn(forbidden, self.main)
        self.assertNotIn("themeMode", self.core)
        self.assertNotIn("updateThemeMode", self.core)
        self.assertIn("isAppearanceLightStatusBars = false", self.main)
        self.assertIn("isAppearanceLightNavigationBars = false", self.main)
    def test_visible_document_entry_and_auto_crop(self):
        self.assertIn('testTag("qrex-document-entry")', self.scan)
        self.assertIn('Text(tr("document_scan")', self.scan)
        self.assertIn("heightIn(min = 56.dp)", self.scan)
        self.assertIn("val openCropFor: (Bitmap) -> Unit", self.doc)
        self.assertIn("openCropFor(it)", self.doc)
        self.assertIn("openCropFor(fitted)", self.doc)
        self.assertIn("qrexDocumentSuggestEdges(page)", self.doc)
        self.assertIn("session.revision == revision", self.doc)
        self.assertNotIn('testTag("qrex-document-auto-edges")', self.doc)
        self.assertIn("else if (session.accept())", self.doc)
    def test_release_retains_true_dark_screenshots_and_security_gates(self):
        publisher = (ROOT / ".github/workflows/github-release.yml").read_text("utf-8")
        capture = (ROOT / "tool/capture_android_screen.sh").read_text("utf-8")
        self.assertIn('(.assets | length) == 20', publisher)
        self.assertNotIn("document-light-", publisher)
        self.assertIn("for appearance in dark; do", capture)
        self.assertIn("sha256sum --check SHA256SUMS.txt", publisher)
        manifest = (ROOT / "platforms/android/app/src/main/AndroidManifest.xml").read_text("utf-8")
        self.assertIn('android:allowBackup="false"', manifest)
        self.assertIn('android:usesCleartextTraffic="false"', manifest)
if __name__ == "__main__":
    unittest.main()
