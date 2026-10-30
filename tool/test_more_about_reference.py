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

    def test_privacy_dialog_has_real_persisted_switch_and_explicit_actions(self):
        main = self.main
        core = (ANDROID / "QREXCore.kt").read_text("utf-8")
        self.assertIn('testTag("qrex-more-privacy")', main)
        self.assertIn('testTag("qrex-privacy-dialog")', main)
        self.assertIn('testTag("qrex-privacy-content")', main)
        self.assertIn('testTag("qrex-privacy-summary")', main)
        self.assertIn('testTag("qrex-privacy-history-row")', main)
        self.assertIn('testTag("qrex-privacy-history-switch")', main)
        self.assertIn('testTag("qrex-privacy-close")', main)
        self.assertIn('testTag("qrex-privacy-full-policy")', main)
        self.assertIn('store.updateHistoryEnabled(enabled)', main)
        self.assertIn('preferences.edit().putBoolean("historyEnabled", value).commit()', core)
        self.assertIn('if (committed) historyEnabled = value', core)
        self.assertIn('tr("privacy_summary")', main)
        self.assertNotIn('maxLines = 4,\n                    color = MaterialTheme.colorScheme.onPrimaryContainer', main)

    def test_export_preserves_full_translated_privacy_notice(self):
        self.assertIn("Text(tr(\"privacy_summary\"), fontSize = 14.sp", self.detail)
        self.assertNotIn("maxLines = 4,\n                    color = MaterialTheme.colorScheme.onPrimaryContainer",
                         self.detail)
        self.assertIn('testTag("qrex-export-pdf")', self.detail)
        self.assertIn("enabled = hasSaved", self.detail)
        self.assertIn('CreateDocument("application/pdf")', self.main)

    def test_export_screen_has_pinned_header_explicit_consent_and_empty_state(self):
        self.assertIn('testTag("qrex-export-screen")', self.detail)
        self.assertIn('testTag("qrex-export-content")', self.detail)
        self.assertIn('testTag("qrex-export-warning")', self.detail)
        self.assertIn('testTag("qrex-export-privacy")', self.detail)
        self.assertIn('testTag("qrex-export-no-saved")', self.detail)
        self.assertLess(
            self.detail.index('QrexDetailHeader(tr("export_archive"), onBack)'),
            self.detail.index('testTag("qrex-export-content")')
        )
        self.assertIn('testTag("qrex-export-consent")', self.main)
        self.assertIn('testTag("qrex-export-consent-confirm")', self.main)
        self.assertIn('testTag("qrex-export-consent-cancel")', self.main)
        self.assertIn('snapshotSavedQrForPdf(store.items)', self.main)
        self.assertIn('pendingPdfRecords = approvedRecords', self.main)


if __name__ == "__main__":
    unittest.main()
