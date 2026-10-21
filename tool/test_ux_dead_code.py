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
        self.assertIn(".widthIn(max = 176.dp).fillMaxWidth().aspectRatio(1f)", self.main)
        self.assertNotIn("BoxWithConstraints(", self.main)

    def test_light_theme_navigation_has_legible_separate_selection_color(self):
        self.assertIn('if (store.lightMode) Color(0xFF1156AE) else cyan', self.main)
        self.assertIn("selectedIconColor = selectedNavigationColor", self.main)
        self.assertIn("selectedTextColor = selectedNavigationColor", self.main)
        self.assertIn("QrexBrandHeader(tr(\"scan_qr\"), tr(\"point_camera\"))", self.scanner)
        self.assertIn("color = MaterialTheme.colorScheme.primary",
                      (NATIVE / "QREXDesign.kt").read_text("utf-8"))

    def test_primary_actions_have_explicit_readable_light_text(self):
        self.assertIn("containerColor = Color(0xFF1677FF), contentColor = Color.White", self.main)

    def test_history_truncates_long_labels_without_removing_delete(self):
        self.assertIn("Text(QRContent.label(item.raw), maxLines = 2", self.main)
        self.assertIn("overflow = TextOverflow.Ellipsis", self.main)
        self.assertIn("Icon(Icons.Default.Delete, contentDescription = tr(\"delete\")", self.main)
        self.assertIn("pendingDelete = item", self.main)
        self.assertIn("store.remove(actual)", self.main)

    def test_settings_grid_has_real_actions_and_accessible_touch_targets(self):
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        self.assertIn("QrexSettingsTile(", self.main)
        self.assertIn('Text(tr("save_history"), Modifier.weight(1f),', self.main)
        self.assertIn("Modifier.fillMaxWidth().heightIn(min = 56.dp)", self.main)
        self.assertIn("min = 88.dp", design)
        self.assertIn("store.updateLightMode(!store.lightMode)", self.main)
        self.assertIn('store.updateLanguage("system")', self.main)
        self.assertIn('confirmPdfExport = true', self.main)
        self.assertIn('confirmExport = true', self.main)
        self.assertIn('showDelete = true', self.main)

    def test_dark_first_five_tab_navigation_and_saved_filter(self):
        self.assertIn('listOf("scan", "create", "history", "favorites", "more")', self.main)
        self.assertIn("QrexDesign.heroBrush", self.main)
        self.assertIn("favoritesPage = true", self.main)
        self.assertIn("QRHistory.matches(it, search, savedOnly || favoritesPage)", self.main)
        self.assertIn("private fun AppHeader(", self.main)
        self.assertIn("enabled = store.items.any { it.saved }", self.main)

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
