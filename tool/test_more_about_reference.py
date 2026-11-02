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

    def test_more_theme_semantics_and_destructive_consent_targets(self):
        more = self.main.split("private fun MoreScreen(", 1)[1]
        self.assertIn('testTag("qrex-more-theme")', more)
        self.assertIn('testTag("qrex-theme-dialog")', more)
        self.assertIn('testTag("qrex-theme-choice-$mode")', more)
        self.assertIn('selected = store.themeMode == mode', more)
        self.assertIn('testTag("qrex-theme-close")', more)
        for tag in ("qrex-export-consent-confirm", "qrex-export-consent-cancel",
                    "qrex-clear-history-confirm", "qrex-clear-history-cancel"):
            self.assertIn('testTag("' + tag + '")', more)
        self.assertGreaterEqual(more.count("Modifier.heightIn(min = 48.dp)"), 7)
        self.assertIn('if (store.clearHistory()) showDelete = false', more)
        self.assertIn('onClick = { showDelete = false }', more)
        self.assertIn('snapshotSavedQrForPdf(store.items)', more)

    def test_language_reference_uses_aligned_noninteractive_flag_and_radio(self):
        language = self.detail.split("internal fun QrexLanguageDetails(", 1)[1].split(
            "internal fun QrexExportDetails(", 1
        )[0]
        self.assertIn('testTag("qrex-language-screen")', language)
        self.assertIn('testTag("qrex-language-list")', language)
        self.assertIn('testTag("qrex-language-option-$code")', language)
        self.assertIn('testTag("qrex-language-flag-$code")', language)
        self.assertIn('Modifier.size(36.dp)', language)
        self.assertIn('Modifier.fillMaxWidth().heightIn(min = 64.dp)', language)
        self.assertIn('.semantics { this.selected = selected }', language)
        self.assertGreaterEqual(language.count('.clearAndSetSemantics {}'), 2)
        self.assertIn('RadioButton(', language)
        self.assertIn('onClick = null,', language)
        self.assertIn('store.updateLanguage(code)', language)
        self.assertIn('items(listOf("system") + QREXStrings.languages, key = { it })', language)

    def test_about_privacy_and_terms_are_responsive_native_dialogs(self):
        more = self.main.split("private fun MoreScreen(", 1)[1]
        for tag in ("qrex-about-dialog", "qrex-about-version",
                    "qrex-about-close", "qrex-privacy-summary-text",
                    "qrex-privacy-content", "qrex-terms-dialog",
                    "qrex-terms-content", "qrex-terms-summary",
                    "qrex-terms-close", "qrex-terms-full-document"):
            self.assertIn('testTag("' + tag + '")', more)
        self.assertIn('initialShowTerms = screenshotMode == "terms"', self.main)
        self.assertIn('showTerms by remember(initialShowTerms)', more)
        self.assertIn("Modifier.fillMaxWidth().heightIn(max = 340.dp)", more)
        self.assertIn('Modifier.weight(1f)', more)
        self.assertIn('"https://github.com/bren-wp/QREX/blob/main/docs/TERMS.md"', more)

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

    def test_export_small_screen_accessibility_without_external_storage(self):
        export = self.detail.split("internal fun QrexExportDetails(", 1)[1].split(
            "private fun QrexDetailHeader(", 1
        )[0]
        self.assertIn('val pdfAvailability = if (hasSaved) null else tr("no_saved")', export)
        self.assertIn('stateDescription = pdfAvailability', export)
        self.assertIn('testTag("qrex-export-warning-text")', export)
        self.assertIn('testTag("qrex-export-privacy-text")', export)
        self.assertGreaterEqual(export.count("Modifier.weight(1f)"), 3)
        self.assertIn('QrexDetailHeader(tr("export_archive"), onBack)', export)
        self.assertIn('enabled = hasSaved', export)
        self.assertNotIn("onClick = { context.startActivity", export)

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
