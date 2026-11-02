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
        # Advanced disclosure must not add a fifth row ahead of the URL field.
        self.assertLess(self.source.index('testTag("qrex-create-more-types")'),
                        self.source.index("val quickTypes = listOf(0, 1, 2, 6)"))
        self.assertIn('testTag("qrex-create-advanced-types")', self.source)
        self.assertIn("showAdvancedTypes = true", self.source)
        self.assertIn("selected = kind == index", self.source)

    def test_create_reference_header_navigates_and_preview_is_private(self):
        create = self.source.split("private fun CreateScreen(", 1)[1].split(
            "private fun HistoryScreen(", 1
        )[0]
        header = create.split("val quickTypes = listOf(0, 1, 2, 6)", 1)[0]
        self.assertIn("onBack: () -> Unit", create)
        self.assertIn('testTag("qrex-create-back")', header)
        self.assertIn('testTag("qrex-create-title")', header)
        self.assertIn('testTag("qrex-create-overflow")', header)
        self.assertIn('testTag("qrex-create-overflow-type-$type")', header)
        self.assertIn("onBack()", header)
        self.assertNotIn("QrexBrandMark(", header,
                         "Create reference has a centered title, not a second app logo")
        self.assertIn('Modifier.fillMaxWidth(0.60f).widthIn(max = 208.dp)', create)
        self.assertIn('testTag("qrex-create-preview-ready")', create)
        self.assertNotIn("QRContent.label(payload)", create,
                         "The preview must not redundantly reveal sensitive QR content")
        self.assertLess(create.index('testTag("qrex-create-title")'),
                        create.index('testTag("qrex-create-content")'))
        self.assertLess(create.index('testTag("qrex-create-preview-ready")'),
                        create.index('testTag("qrex-create-show")'))

    def test_accessible_disclosure_and_pinned_actions(self):
        self.assertIn(".heightIn(min = 52.dp)", self.source)
        self.assertIn(".heightIn(min = 54.dp).semantics", self.source)
        self.assertIn('testTag("qrex-create-show")', self.source)
        self.assertIn('testTag("qrex-create-save")', self.source)
        self.assertLess(self.source.index('testTag("qrex-create-show")'),
                        self.source.index('testTag("qrex-create-save")'))
        self.assertIn("Modifier.fillMaxSize().imePadding()", self.source)
        self.assertIn("val previewVisible = !imeVisible && layout.showQrPreview", self.source)
        self.assertIn("if (previewVisible && qr != null)", self.source)
        self.assertIn('testTag("qrex-create-preview-card")', self.source)
        create = self.source.split("private fun CreateScreen(", 1)[1].split(
            "private fun HistoryScreen(", 1)[0]
        self.assertLess(create.index('testTag("qrex-create-preview-card")'),
                        create.index('PrimaryAction(tr("generate_qr")'))
        self.assertLess(create.index('PrimaryAction(tr("generate_qr")'),
                        create.index('testTag("qrex-create-save")'))
        self.assertIn("// Primary actions stay in their own non-scrollable footer", create)
        self.assertNotIn('if (qr != null) {', create)
        self.assertIn("shouldGenerateCreateQrPreview(imeVisible, layout.showQrPreview, canGenerate)", self.source)
        self.assertIn("key2 = previewVisible", self.source)
        self.assertIn("if (!shouldGenerateCreateQrPreview(", self.source)
        self.assertNotIn("BoxWithConstraints(", self.source)

    def test_wifi_password_privacy_and_exact_ssid_round_trip(self):
        create = self.source.split("private fun CreateScreen(", 1)[1].split(
            "private fun HistoryScreen(", 1
        )[0]
        self.assertIn("createQrInputText(kind, content)", create)
        self.assertIn("if (kind == 2) enteredText else enteredText.trim()", self.source)
        self.assertIn('testTag("qrex-create-wifi-password")', create)
        self.assertIn('keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)', create)
        self.assertIn("else PasswordVisualTransformation()", create)
        self.assertIn('testTag("qrex-wifi-visibility")', create)
        self.assertIn('testTag("qrex-wifi-visibility-label")', create)
        self.assertIn('Modifier.heightIn(min = 48.dp)', create)
        self.assertIn("confirmWifiSave = true", create)
        self.assertIn("pendingWifiExportPayload = requested", create)

    def test_email_phone_ime_and_user_selected_locale(self):
        self.assertIn("3 -> KeyboardType.Email", self.source)
        self.assertIn("4 -> KeyboardType.Phone", self.source)
        self.assertIn("val displayLanguage = LocalLanguage.current", self.source)
        self.assertIn('displayLanguage == "system"', self.source)
        self.assertIn("Locale.forLanguageTag(displayLanguage)", self.source)

    def test_security_and_existing_export_unchanged(self):
        self.assertIn("confirmWifiSave = true", self.source)
        self.assertIn("pendingWifiExportPayload = requested", self.source)
        self.assertIn("val approved = approvedWifiQrExport(pendingWifiExportPayload)", self.source)
        self.assertIn("pendingExportPayload = approved", self.source)
        self.assertIn("pendingWifiExportPayload = null", self.source)
        self.assertIn('tr("wifi_save_warning")', self.source)
        self.assertIn('CreateDocument("image/png")', self.source)
        self.assertIn("reveal = false", self.source)
        self.assertIn("val stackChoices = widthDp < 340 || fontScale >= 1.30f",
                      self.design)


if __name__ == "__main__":
    unittest.main()
