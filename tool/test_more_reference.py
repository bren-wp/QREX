#!/usr/bin/env python3
"""Native QREX More screen regression checks for the supplied visual reference.

Source contracts complement (not replace) real emulator screenshots, touch
semantics, and Android instrumentation tests.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt"
DESIGN = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/QREXDesign.kt"


class MoreReferenceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.main = MAIN.read_text("utf-8")
        cls.design = DESIGN.read_text("utf-8")
        cls.more = cls.main.split("private fun MoreScreen(", 1)[1].split(
            "    if (showTheme) {", 1
        )[0]
        cls.tiles = cls.design.split("internal fun QrexSettingsTile(", 1)[1].split(
            "internal fun QrexSettingsListRow(", 1
        )[0]

    def test_six_tiles_use_reference_icons_with_real_callbacks(self):
        mapping = (
            ("theme", "Brightness6"),
            ("language", "Language"),
            ("export", "Download"),
            ("privacy", "Lock"),
            ("terms", "Description"),
            ("about", "Info"),
        )
        for tag, icon in mapping:
            with self.subTest(tile=tag):
                self.assertIn('testTag("qrex-more-%s")' % tag, self.more)
                self.assertIn("Icons.Default.%s" % icon, self.more)
        for callback in ("showTheme = true", 'detailPage = "language"',
                         'detailPage = "export"', "showPrivacy = true",
                         "showTerms = true", "showAbout = true"):
            self.assertIn(callback, self.more)
        self.assertIn('testTag("qrex-more-delete")', self.more)
        self.assertIn("showDelete = true", self.more)

    def test_native_3d_plate_has_true_rgb_highlight_and_no_fake_click_layer(self):
        self.assertIn('testTag("qrex-more-icon-plate")', self.tiles)
        self.assertIn("Modifier.size(40.dp).background(Brush.verticalGradient(", self.tiles)
        self.assertIn("listOf(Color.White.copy(alpha = .10f), Color.Transparent)", self.tiles)
        self.assertIn("shadowElevation = 4.dp", self.tiles)
        self.assertIn("border = BorderStroke(1.dp, foreground.copy(alpha = .39f))", self.tiles)
        self.assertIn("role = Role.Button, onClick = onClick", self.tiles)
        self.assertNotIn("IconButton(", self.tiles)

    def test_about_version_is_installed_not_marketing_sample(self):
        self.assertIn("val currentVersionLabel = remember(context)", self.more)
        self.assertIn("getPackageInfo(context.packageName, 0)", self.more)
        self.assertIn('if (version.isBlank()) "QREX" else "QREX v$version"', self.more)
        self.assertIn("subtitle = currentVersionLabel", self.more)
        self.assertNotIn('subtitle = "QREX v1.0.0"', self.more)

    def test_small_screen_fallback_preserves_actions_and_privacy(self):
        self.assertIn("val compactSettings = useCompactMoreList(", self.more)
        self.assertIn("preferRows = !isDarkAppearance", self.more)
        self.assertIn("if (compactSettings)", self.more)
        self.assertIn("QrexSettingsListRow(", self.more)
        self.assertIn("QrexSettingsTile(", self.more)
        self.assertIn('modifier = Modifier.testTag("qrex-more-delete")', self.more)
        self.assertIn("confirmPdfExport = true", self.main)
        self.assertIn("if (store.clearHistory()) showDelete = false", self.main)


if __name__ == "__main__":
    unittest.main()
