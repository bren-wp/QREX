#!/usr/bin/env python3
"""Native Result and shared bottom-navigation visual contracts.

A perspective phone collage is NOT a flat screenshot for zero-pixel comparison.
These checks protect real Compose sizing, input semantics and privacy callbacks.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
NATIVE = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


class ResultNavigationReferenceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.main = (NATIVE / "MainActivity.kt").read_text("utf-8")
        cls.design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        cls.result = cls.main.split("private fun ResultScreen(", 1)[1].split(
            "\nfun openLink(", 1
        )[0]

    def test_result_has_compact_panel_and_working_actions(self):
        s = self.result
        self.assertIn('testTag("qrex-result-summary")', s)
        self.assertIn("min = if (compactResult) 154.dp else if (wideResult) 192.dp else 180.dp", s)
        self.assertIn("Modifier.size(if (wideResult) 62.dp else 54.dp).background(resultGlyphBrush)", s)
        self.assertIn("modifier = Modifier.size(if (wideResult) 32.dp else 28.dp)", s)
        self.assertIn("padding(horizontal = 12.dp, vertical = 12.dp)", s)
        self.assertIn('testTag("qrex-result-primary")', s)
        self.assertIn('testTag("qrex-result-copy")', s)
        self.assertIn('testTag("qrex-result-save-favorite")', s)
        self.assertIn('testTag("qrex-result-toggle-qr")', s)
        self.assertIn('testTag("qrex-result-delete")', s)
        self.assertIn("verticalScroll(rememberScrollState())", s)
        self.assertIn("imePadding().padding(bottom = 8.dp)", s)
        self.assertIn("fillMaxWidth(resultContentFraction)", s)
        self.assertIn("horizontalAlignment = Alignment.CenterHorizontally)", s)
        self.assertIn("qrexResultContentFraction(windowWidthDp)", s)
        self.assertIn("widthIn(max = if (wideResult) 290.dp else 230.dp)", s)
        self.assertIn('testTag("qrex-result-url-origin")', s)
        self.assertIn("publicUrlOrigin", s)
        self.assertIn('testTag("qrex-result-menu-edit")', s)
        self.assertIn("onEdit(raw)", s)
        self.assertNotIn('QrexResultAction(tr("edit")', s)
        self.assertNotIn("heightIn(min = 178.dp)", s)

    def test_result_rows_keep_native_accessibility_and_compact_spacing(self):
        s = self.design
        self.assertIn("internal fun qrexResultActionHeightDp(", s)
        self.assertIn("if (fontScale >= 1.35f) 60 else 52", s)
        self.assertIn("qrexResultActionHeightDp(LocalDensity.current.fontScale).dp", s)
        self.assertIn("shadowElevation = 2.dp", s)
        self.assertIn("onClick = onClick", s)
        self.assertIn("Modifier.padding(horizontal = 12.dp, vertical = 7.dp)", s)

    def test_every_main_screen_uses_one_real_five_tab_navigation(self):
        self.assertEqual(1, self.main.count("NavigationBar("))
        self.assertNotIn("if (currentCode == null) {", self.main)
        self.assertIn("BackHandler(enabled = currentCode != null)", self.main)
        self.assertIn('testTag("qrex-bottom-navigation")', self.main)
        self.assertIn('testTag("qrex-nav-glyph-$index")', self.main)
        self.assertIn("QrexNavigationGlyph(", self.main)
        self.assertIn("selected = selected == target", self.main)
        self.assertIn("onClick = {\n                            focusManager.clearFocus()", self.main)
        self.assertIn("HorizontalDivider(color = if (darkUi)", self.main)
        self.assertIn("NavigationBarItem(", self.main)
        self.assertIn("if (selected)", self.design)
        self.assertIn("if (icon == Icons.Default.QrCodeScanner)", self.design)
        self.assertIn("drawLine(tint, start, end, strokeWidth = stroke", self.design)
        self.assertIn("contentDescription = null", self.design)

    def test_wifi_secrets_and_privacy_confirmations_unchanged(self):
        for marker in (
            "confirmWifiCopy = true", "confirmWifiPngExport = true",
            "confirmWifiShare = true", "confirmWifiSave = true",
            "pendingResultPngPayload = raw", "approvedWifiQrExport(raw)",
            "copyQrToClipboard(context, raw)", "confirmResultDelete = true",
        ):
            self.assertIn(marker, self.result)
        self.assertIn("showQrPreview = !showQrPreview", self.result)


if __name__ == "__main__":
    unittest.main()
