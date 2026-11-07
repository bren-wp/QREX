#!/usr/bin/env python3
"""QREX v1.0.37 native five-screen safety and visual structure contracts.

These are source regression guards, not a claim of pixel parity.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
NATIVE = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


class QrexFiveScreenReference(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.main = (NATIVE / "MainActivity.kt").read_text(encoding="utf-8")
        cls.scanner = (NATIVE / "QREXScanner.kt").read_text(encoding="utf-8")
        cls.splash = (NATIVE / "QREXSplash.kt").read_text(encoding="utf-8")
        cls.disclosures = cls.main.split('    if (showAbout) {', 1)[1]

    def test_privacy_icon_and_real_consent_preserved(self):
        for token in (
            'testTag("qrex-privacy-icon-plate")',
            'testTag("qrex-privacy-history-switch")',
            'testTag("qrex-privacy-summary-text")',
            'store.updateHistoryEnabled(enabled)',
            'testTag("qrex-privacy-full-policy")',
            'testTag("qrex-privacy-close")',
            '"https://github.com/bren-wp/QREX/blob/main/docs/PRIVACY.md"',
        ):
            self.assertIn(token, self.disclosures)

    def test_terms_matching_bordered_card_and_live_external_action(self):
        for token in (
            'testTag("qrex-terms-summary-card")',
            'testTag("qrex-terms-icon-plate")',
            'testTag("qrex-terms-summary")',
            'testTag("qrex-terms-close")',
            'testTag("qrex-terms-full-document")',
            '"https://github.com/bren-wp/QREX/blob/main/docs/TERMS.md"',
        ):
            self.assertIn(token, self.disclosures)
        self.assertIn('shadowElevation = 3.dp', self.disclosures)

    def test_about_native_installed_version_and_opt_in_website(self):
        for token in (
            'testTag("qrex-about-developer-card")',
            'testTag("qrex-about-domain")',
            'testTag("qrex-about-version")',
            'testTag("qrex-about-brand")',
            'getPackageInfo(context.packageName, 0)',
            'testTag("qrex-about-website")',
            'Uri.parse("https://brendigo.com")',
        ):
            self.assertIn(token, self.disclosures)
        self.assertNotIn('subtitle = "QREX v1.0.0"', self.disclosures)

    def test_home_camera_is_live_not_reference_picture(self):
        for token in (
            'testTag("qrex-scan-hint-card")',
            'testTag("qrex-scan-hint")',
            'testTag("qrex-camera-preview")',
            'AndroidView(factory = { preview }',
            'testTag("qrex-scan-brackets")',
            'testTag("qrex-scan-floating-torch")',
            'testTag("qrex-scan-gallery")',
            'frame.close()',
        ):
            self.assertIn(token, self.scanner)
        self.assertNotIn("painterResource(", self.scanner)

    def test_splash_uses_native_wave_and_accessible_scroll(self):
        for token in (
            'testTag("qrex-splash-native-waves")',
            'testTag("qrex-splash-brand-halo")',
            'testTag("qrex-splash-logo")',
            'testTag("qrex-splash-wordmark")',
            'testTag("qrex-splash-progress")',
            'qrexSplashUsesScrollLayout(',
            '.verticalScroll(rememberScrollState())',
            'qrexSplashAccentRange(subtitle)',
        ):
            self.assertIn(token, self.splash)


if __name__ == "__main__":
    unittest.main()
