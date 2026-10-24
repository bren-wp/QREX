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
        cls.details = (NATIVE / "QREXMoreDetails.kt").read_text("utf-8")

    def test_obsolete_viewport_helper_is_removed_but_compose_layout_remains(self):
        self.assertNotIn("responsiveQrSideDp", self.main)
        self.assertIn(".widthIn(max = 230.dp).fillMaxWidth().aspectRatio(1f)", self.main)
        self.assertIn(".size(qrSize).background(Color.White).padding(7.dp)", self.main)
        self.assertIn("qrexLayoutProfile(", self.main)
        self.assertNotIn("BoxWithConstraints(", self.main)

    def test_dark_theme_is_default_and_navigation_contrast_is_adaptive(self):
        # Dark is the persisted fresh-install default, but an existing user's
        # explicit light-mode choice must continue to be respected.
        self.assertIn('readBoolean("lightMode", false)', self.core)
        self.assertIn('runCatching { preferences.getBoolean(key, fallback) }', self.core)
        self.assertIn('fun readLanguage()', self.core)
        self.assertIn(") else darkColorScheme(", self.main)
        self.assertIn("QrexDesign.backgroundDeep", self.main)
        self.assertIn("onSurfaceVariant = QrexDesign.muted", self.main)
        self.assertIn(
            "if (lightTheme) Color(0xFF1156AE) else QrexDesign.accentBright",
            self.main,
        )
        self.assertIn("selectedIconColor = selectedNavigationColor", self.main)
        self.assertIn("selectedTextColor = selectedNavigationColor", self.main)
        self.assertIn("QrexBrandHeader(onMore = onMore)", self.scanner)
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        self.assertIn("val accentBright = Color(0xFF22C7FF)", design)
        self.assertIn("color = MaterialTheme.colorScheme.primary", design)

    def test_reference_splash_tagline_and_android_26_window_theme(self):
        splash = (NATIVE / "QREXSplash.kt").read_text("utf-8")
        styles = (ROOT / "platforms/android/app/src/main/res/values/styles.xml").read_text("utf-8")
        self.assertIn('"Sve što može QR — jednostavno, brzo i bez prijave."', splash)
        self.assertIn('Text("QRE"', splash)
        self.assertIn('Text("X"', splash)
        self.assertIn('android:windowBackground', styles)
        self.assertIn('#030A18', styles)
        # Android API 26 must lint/build without referencing API 27-only attrs.
        self.assertNotIn('android:windowLightNavigationBar', styles)
        self.assertIn('val systemLanguage = LocalConfiguration.current.locales[0].language', self.main)
        self.assertNotIn('context.resources.configuration.locales[0].language', self.main)

    def test_primary_actions_have_explicit_readable_light_text(self):
        self.assertIn("containerColor = Color(0xFF1677FF), contentColor = Color.White", self.main)

    def test_reference_dark_navigation_and_result_hierarchy(self):
        self.assertIn("else QrexDesign.accentBright", self.main)
        self.assertIn("else QrexDesign.backgroundDeep", self.main)
        self.assertIn("indicatorColor = if (lightTheme)", self.main)
        self.assertIn("textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold", self.main)
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        self.assertIn("modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)", design)

    def test_history_truncates_long_labels_without_removing_delete(self):
        self.assertIn("QRContent.label(item.raw),\n                                maxLines = 1", self.main)
        self.assertIn("overflow = TextOverflow.Ellipsis", self.main)
        # The reference uses a real overflow menu and favorite heart rather
        # than an always-visible trash icon on each History row.
        self.assertIn("historyMenu = true", self.main)
        self.assertIn('Text(tr("delete"))', self.main)
        self.assertIn("DropdownMenu(", self.main)
        self.assertIn("store.setSaved(item, false)", self.main)
        self.assertIn("store.setSaved(item, true)", self.main)
        self.assertIn('testTag("qrex-history-menu")', self.main)
        self.assertIn("pendingDelete = item", self.main)
        self.assertIn("store.remove(actual)", self.main)

    def test_settings_grid_has_real_actions_and_accessible_touch_targets(self):
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        self.assertIn("QrexSettingsTile(", self.main)
        self.assertIn('Text(tr("save_history"), Modifier.weight(1f),', self.main)
        self.assertIn("Modifier.fillMaxWidth().heightIn(min = 48.dp)", self.main)
        self.assertIn("heightIn(min = 84.dp)", design)
        self.assertIn("store.updateLightMode(!store.lightMode)", self.main)
        self.assertIn("QrexSettingsListRow(", self.main)
        self.assertIn('store.updateLanguage(code)', self.details)
        self.assertIn('"language" -> QrexLanguageDetails(store,', self.main)
        self.assertIn('"export" -> QrexExportDetails(', self.main)
        self.assertIn('confirmPdfExport = true', self.main)
        self.assertIn('confirmExport = true', self.main)
        self.assertIn('showDelete = true', self.main)

    def test_four_main_tabs_keep_favorites_one_tap_from_history(self):
        self.assertIn('listOf("scan", "create", "history", "more")', self.main)
        self.assertIn("navTargets = listOf(0, 1, 2, 4)", self.main)
        self.assertIn("QrexDesign.heroBrush", self.main)
        self.assertIn('favoritesPage = screenshotMode == "saved"', self.main)
        self.assertIn('testTag("qrex-history-segments")', self.main)
        self.assertIn('testTag("qrex-history-saved")', self.main)
        self.assertIn("QRHistory.matches(it, search, savedOnly || favoritesPage)", self.main)
        self.assertIn("private fun AppHeader(", self.main)
        self.assertIn("hasSaved = store.items.any { it.saved }", self.main)

    def test_dark_splash_and_reference_app_bars_are_real_compose_actions(self):
        splash = (NATIVE / "QREXSplash.kt").read_text("utf-8")
        self.assertIn("internal fun QrexSplashScreen(", splash)
        self.assertIn("QrexDesign.backgroundDeep", splash)
        self.assertIn("R.drawable.qrex_icon", splash)
        self.assertIn("screenshotMode != \"splash\"", self.main)
        # Create is a root tab in the reference; its app bar starts with
        # the QREX wordmark, then privacy and overflow actions, with no extra
        # navigation arrow before the brand.
        self.assertNotIn('testTag("qrex-create-back")', self.main)
        self.assertIn("QrexBrandMark(logoSize = 32.dp, textSize = 18.sp)", self.main)
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        self.assertIn("painterResource(R.drawable.qrex_icon)", design)
        self.assertIn('Text("X"', self.main)
        self.assertIn("Icons.Default.Shield", self.main)
        self.assertIn('contentDescription = tr("more")', self.main)
        self.assertIn("showResultMenu", self.main)
        self.assertIn('testTag("qrex-result-close")', self.main)

    def test_reference_create_controls_keep_real_pinned_touch_targets(self):
        self.assertIn("QrexCreateTypeChoice(types[index], typeIcons[index]", self.main)
        self.assertIn("Icon(icon, contentDescription = null", self.main)
        self.assertIn("layout.stackTypeChoices", self.main)
        self.assertIn("listOf(0, 1, 2, 6)", self.main)
        self.assertIn('testTag("qrex-create-show")', self.main)
        self.assertIn('testTag("qrex-create-save")', self.main)
        self.assertLess(self.main.index('testTag("qrex-create-show")'),
                        self.main.index('testTag("qrex-create-save")'))
        self.assertIn('focusManager.clearFocus(force = true)', self.main)

    def test_result_actions_have_full_width_real_callbacks(self):
        self.assertIn('QrexResultAction(tr("copy"), Icons.Default.ContentCopy)', self.main)
        self.assertIn('QrexResultAction(tr("share"), Icons.Default.Share)', self.main)
        self.assertIn('testTag("qrex-result-toggle-qr")', self.main)
        self.assertIn('if (alreadySaved) tr("remove_favorite") else tr("add_favorite")', self.main)
        self.assertIn("if (currentCode == null)", self.main)

    def test_scan_first_has_one_torch_button_and_a_primary_gallery_action(self):
        self.assertIn('testTag("qrex-scan-gallery")', self.scanner)
        self.assertIn('testTag("qrex-scan-torch")', self.scanner)
        self.assertIn("ButtonDefaults.buttonColors(", self.scanner)
        self.assertNotIn("modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)", self.scanner)
        self.assertIn("ActivityResultContracts.PickVisualMedia()", self.scanner)

    def test_scanner_torch_waits_for_actual_camerax_completion(self):
        self.assertIn("var torchBusy by remember { mutableStateOf(false) }", self.scanner)
        self.assertIn("activeCamera.cameraControl.enableTorch(next)", self.scanner)
        self.assertIn("request.addListener({", self.scanner)
        self.assertIn("runCatching { request.get() }.isSuccess", self.scanner)
        self.assertIn("enabled = allowed && !torchBusy &&", self.scanner)
        self.assertIn(".clip(RoundedCornerShape(24.dp))", self.scanner)

    def test_reference_create_preview_and_favorite_hearts_are_live_actions(self):
        # The QR preview and Generate/Save actions share a fixed bottom group.
        self.assertIn("WindowInsets.ime.getBottom(LocalDensity.current) > 0", self.main)
        self.assertIn('testTag("qrex-create-preview-ready")', self.main)
        self.assertIn('testTag("qrex-create-show")', self.main)
        self.assertIn('testTag("qrex-create-save")', self.main)
        self.assertIn('testTag("qrex-history-favorite")', self.main)
        self.assertIn("store.setSaved(item, !item.saved)", self.main)
        self.assertIn("QRContent.contact(input, contactPhone, contactEmail)", self.main)
        self.assertIn('EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name()', self.main)
        splash = (NATIVE / "QREXSplash.kt").read_text("utf-8")
        self.assertIn("Modifier.size(192.dp).clip(RoundedCornerShape(42.dp))", splash)

    def test_irreplaceable_history_migration_and_wifi_encryption_are_retained(self):
        self.assertIn('getSharedPreferences("FlutterSharedPreferences"', self.core)
        self.assertIn("migrateFlutterV1()", self.core)
        self.assertIn("QREXSecretCipher.encrypt(item.raw)", self.core)
        self.assertIn("QREXSecretCipher.decrypt(value)", self.core)
        self.assertIn("if (!historyReadable) return false", self.core)
        self.assertIn("protectedWifi", self.core)


if __name__ == "__main__":
    unittest.main()

