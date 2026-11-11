#!/usr/bin/env python3
"""QREX More / Settings: single-column native reference and action contracts.

Source guards supplement emulator geometry and genuine captured screenshots.
They do not establish pixel equality with perspective marketing imagery.
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
        cls.cards = cls.design.split("internal fun QrexSettingsListRow(", 1)[1].split(
            "/** Window-class layout rules", 1
        )[0]

    def test_all_seven_cards_have_unique_real_callbacks(self):
        mapping = (
            ("theme", "Brightness6", "showTheme = true"),
            ("language", "Language", 'detailPage = "language"'),
            ("export", "Download", 'detailPage = "export"'),
            ("privacy", "Lock", "showPrivacy = true"),
            ("terms", "Description", "showTerms = true"),
            ("about", "Info", "showAbout = true"),
            ("delete", "DeleteForever", "showDelete = true"),
        )
        self.assertEqual(self.more.count("QrexSettingsListRow("), 7)
        for tag, icon, callback in mapping:
            with self.subTest(card=tag):
                self.assertEqual(
                    self.more.count(f'testTag("qrex-more-{tag}")'), 1
                )
                self.assertIn(f"Icons.Default.{icon}", self.more)
                self.assertIn(callback, self.more)

    def test_reference_six_tiles_and_safe_accessible_fallback(self):
        self.assertIn('testTag("qrex-more-settings-stack")', self.more)
        self.assertIn('testTag("qrex-more-reference-grid")', self.more)
        self.assertIn("FlowRow(", self.more)
        self.assertIn("maxItemsInEachRow = moreColumns", self.more)
        self.assertIn("horizontalArrangement = Arrangement.spacedBy(10.dp)", self.more)
        self.assertIn("qrexMoreTileColumns(", self.more)
        self.assertIn("tileMode = moreTiles", self.more)
        self.assertEqual(self.more.count("tileMode = moreTiles"), 6)
        self.assertEqual(self.more.count("Modifier.weight(1f).testTag"), 6)
        self.assertIn("if (tileMode) 104.dp", self.cards)
        self.assertIn("verticalArrangement = Arrangement.spacedBy(4.dp)", self.cards)
        self.assertIn("widthDp < 360 || heightDp < 500 || fontScale >= 1.30f", self.cards)
        self.assertIn("if (fontScale >= 1.35f) 88 else 72", self.design)
        self.assertIn("shape = RoundedCornerShape(QrexDesign.cardRadius)", self.cards)
        self.assertIn("Spacer(Modifier.height(4.dp))", self.more)
        self.assertIn("destructive = true, showChevron = false", self.more)
        self.assertNotIn("QrexSettingsTile(", self.main)
        self.assertNotIn("useCompactMoreList(", self.main)

    def test_raised_3d_icon_plate_is_decorative_not_an_extra_button(self):
        self.assertIn('testTag("qrex-more-icon-plate")', self.cards)
        self.assertIn("Modifier.size(if (tileMode || destructive) 48.dp else 40.dp)", self.cards)
        self.assertIn("shape = if (tileMode || destructive) CircleShape", self.cards)
        self.assertIn(".background(Brush.verticalGradient(", self.cards)
        self.assertIn("listOf(Color.White.copy(alpha = .10f), Color.Transparent)", self.cards)
        self.assertIn("shadowElevation = 4.dp", self.cards)
        self.assertIn("border = BorderStroke(1.dp, if (tileMode) accent.copy(alpha = .12f)", self.cards)
        self.assertIn("else accent.copy(alpha = .39f)", self.cards)
        self.assertIn("onClick = onClick", self.cards)
        self.assertIn("Icon(icon, contentDescription = null", self.cards)
        self.assertNotIn("IconButton(", self.cards)
        self.assertNotIn(".clickable(", self.cards)

    def test_about_version_is_installed_not_marketing_sample(self):
        self.assertIn("val currentVersionLabel = remember(context)", self.more)
        self.assertIn("getPackageInfo(context.packageName, 0)", self.more)
        self.assertIn('if (version.isBlank()) "QREX" else "QREX v$version"', self.more)
        self.assertIn("subtitle = currentVersionLabel", self.more)
        self.assertNotIn('subtitle = "QREX v1.0.0"', self.more)

    def test_confirmations_and_local_only_actions_survive(self):
        for action in ('confirmPdfExport = true', "showPrivacy = true",
                       "showTerms = true", "showDelete = true"):
            self.assertIn(action, self.main)
        self.assertIn("if (store.clearHistory()) showDelete = false", self.main)
        self.assertIn("QrexSettingsListRow(", self.more)


if __name__ == "__main__":
    unittest.main()
