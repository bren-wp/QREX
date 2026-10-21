#!/usr/bin/env python3
"""Source-level QREX accessibility / dead-code regression checks.

These contracts complement Kotlin lint, unit and emulator UI tests. They do
not claim that lexical reference counting proves all runtime reachability.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
NATIVE = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


class UXDeadCodeRegressionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.main = (NATIVE / "MainActivity.kt").read_text("utf-8")
        cls.scanner = (NATIVE / "QREXScanner.kt").read_text("utf-8")
        cls.core = (NATIVE / "QREXCore.kt").read_text("utf-8")

    def test_obsolete_viewport_helper_is_removed_but_compose_layout_remains(self):
        self.assertNotIn("responsiveQrSideDp", self.main)
        self.assertIn(".widthIn(max = 230.dp).fillMaxWidth().aspectRatio(1f)", self.main)
        self.assertIn(".widthIn(max = 238.dp).fillMaxWidth().aspectRatio(1f)", self.main)
        self.assertNotIn("BoxWithConstraints(", self.main)

    def test_light_theme_navigation_has_legible_separate_selection_color(self):
        self.assertIn('if (store.lightMode) Color(0xFF1156AE) else cyan', self.main)
        self.assertIn("selectedIconColor = selectedNavigationColor", self.main)
        self.assertIn("selectedTextColor = selectedNavigationColor", self.main)
        self.assertIn("color = MaterialTheme.colorScheme.primary", self.scanner)

    def test_primary_actions_have_explicit_readable_light_text(self):
        self.assertIn("containerColor = electricBlue, contentColor = Color.White", self.main)

    def test_dark_first_reference_palette_and_card_hierarchy(self):
        self.assertIn('Color(0xFF050D1D)', self.main)
        self.assertIn('Color(0xFF101B30)', self.main)
        self.assertIn('Color(0xFF1378FF)', self.main)
        self.assertIn('Color(0xFF10CBFA)', self.main)
        self.assertIn('darkColorScheme(primary = electricBlue, secondary = cyan', self.main)
        self.assertIn('selectedContainerColor = MaterialTheme.colorScheme.primary', self.main)
        self.assertIn('indicatorColor = if (store.lightMode)', self.main)
        self.assertIn('border = androidx.compose.foundation.BorderStroke(', self.main)
        self.assertIn('contentColor = Color.White', self.main)
        self.assertNotIn('Color(0xFFF3F6FF), surface = Color.White,', self.main.split(') else darkColorScheme', 1)[1])

    def test_history_truncates_long_labels_without_removing_delete(self):
        self.assertIn("Text(QRContent.label(item.raw), maxLines = 2", self.main)
        self.assertIn("overflow = TextOverflow.Ellipsis", self.main)
        self.assertIn("Icon(Icons.Default.Delete, contentDescription = tr(\"delete\")", self.main)
        self.assertIn("pendingDelete = item", self.main)
        self.assertIn("store.remove(actual)", self.main)

    def test_settings_actions_have_minimum_touch_height(self):
        self.assertIn('QREXTile(Icons.Default.DarkMode, tr("light_theme")', self.main)
        self.assertIn('QREXTile(Icons.Default.Language, tr("language"), languageName', self.main)
        self.assertIn('Text(tr("save_history"), Modifier.weight(1f), fontSize = 14.sp)', self.main)
        self.assertIn('modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)', self.main)
        self.assertIn('colors = ButtonDefaults.outlinedButtonColors(', self.main)
        self.assertIn('QREXTile(Icons.Default.Security, tr("privacy_policy")', self.main)
        self.assertIn('QREXTile(Icons.Default.Article, tr("terms_of_use")', self.main)

    def test_scanner_torch_waits_for_actual_camerax_completion(self):
        self.assertIn("var torchBusy by remember { mutableStateOf(false) }", self.scanner)
        self.assertIn("activeCamera.cameraControl.enableTorch(next)", self.scanner)
        self.assertIn("request.addListener({", self.scanner)
        self.assertIn("runCatching { request.get() }.isSuccess", self.scanner)
        self.assertIn("enabled = allowed && !torchBusy &&", self.scanner)
        self.assertIn(".clip(RoundedCornerShape(28.dp))", self.scanner)

    def test_irreplaceable_history_migration_and_wifi_encryption_are_retained(self):
        self.assertIn('getSharedPreferences("FlutterSharedPreferences"', self.core)
        self.assertIn("migrateFlutterV1()", self.core)
        self.assertIn("QREXSecretCipher.encrypt(item.raw)", self.core)
        self.assertIn("QREXSecretCipher.decrypt(value)", self.core)
        self.assertIn("if (!historyReadable) return false", self.core)
        self.assertIn("protectedWifi", self.core)


if __name__ == "__main__":
    unittest.main()
