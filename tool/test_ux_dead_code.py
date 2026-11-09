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
        self.assertIn(".widthIn(max = if (wideResult) 290.dp else 230.dp).fillMaxWidth().aspectRatio(1f)", self.main)
        self.assertIn(".size(qrSize).background(Color.White).padding(9.dp)", self.main)
        self.assertIn("qrexLayoutProfile(", self.main)
        self.assertNotIn("BoxWithConstraints(", self.main)

    def test_gallery_picker_excludes_camera_and_compact_nav_keeps_labels(self):
        self.assertIn("galleryActive.compareAndSet(false, true)", self.scanner)
        self.assertIn("enabled = !galleryBusy", self.scanner)
        self.assertIn("if (galleryBusy) {", self.scanner)
        self.assertIn("CircularProgressIndicator(", self.scanner)
        self.assertIn("cameraFrameAllowed(", self.scanner)
        self.assertIn("!disposed.get() && !galleryActive.get()", self.scanner)
        self.assertIn("finally {", self.scanner)
        self.assertIn("galleryActive.set(false)", self.scanner)
        self.assertIn("alwaysShowLabel = true", self.main)
        self.assertIn("fontSize = if (compactNavigation) 10.sp else 11.sp", self.main)

    def test_system_dark_light_palette_and_pdf_export(self):
        self.assertIn("if (darkUi) darkColorScheme(", self.main)
        self.assertIn("else lightColorScheme(", self.main)
        self.assertIn('"system" -> isSystemInDarkTheme()', self.main)
        self.assertIn('store.themeMode', self.main)
        self.assertIn('testTag("qrex-more-theme")', self.main)
        self.assertIn('testTag("qrex-more-settings-stack")', self.main)
        self.assertNotIn("useCompactMoreList(", self.main)
        self.assertIn('testTag("qrex-more-settings-stack")', self.main)
        self.assertIn('testTag("qrex-theme-dialog")', self.main)
        self.assertNotIn("updateLightMode(", self.main)
        self.assertNotIn("updateLightMode(", self.core)
        self.assertNotIn("exportSavedPdfZip(", self.main)
        self.assertIn('CreateDocument("application/pdf")', self.main)
        self.assertNotIn('CreateDocument("application/zip")', self.main)
        self.assertIn("QrexDesign.backgroundDeep", self.main)
        self.assertIn("onSurfaceVariant = QrexDesign.muted", self.main)
        self.assertIn("QrexBrandHeader(onMore = onMore)", self.scanner)

    def test_reference_splash_tagline_and_android_26_window_theme(self):
        splash = (NATIVE / "QREXSplash.kt").read_text("utf-8")
        styles = (ROOT / "platforms/android/app/src/main/res/values/styles.xml").read_text("utf-8")
        self.assertIn('tr("splash_message")', splash)
        self.assertIn('"QRE", color = Color.White', splash)
        self.assertIn('"X",', splash)
        self.assertIn('android:windowBackground', styles)
        self.assertIn('#030A18', styles)
        # Android API 26 must lint/build without referencing API 27-only attrs.
        self.assertNotIn('android:windowLightNavigationBar', styles)
        self.assertIn('val appLanguage = screenshotLanguage ?: store.language', self.main)
        self.assertNotIn('context.resources.configuration.locales[0].language', self.main)

    def test_user_reference_splash_brand_spacing_and_badge_geometry(self):
        splash = (NATIVE / "QREXSplash.kt").read_text("utf-8")
        icon = (ROOT / "platforms/android/app/src/main/res/drawable/qrex_icon.xml").read_text("utf-8")
        foreground = (ROOT / "platforms/android/app/src/main/res/drawable/qrex_launcher_foreground.xml").read_text("utf-8")
        for anchor in ("qrexSplashMetrics(", "qrex-splash-logo", "qrex-splash-wordmark",
                       "qrex-splash-tagline", "qrex-splash-footer",
                       'tr("splash_message")', "upperRibbon", "lowerRibbon",
                       "Brush.horizontalGradient(", "TextStyle("):
            self.assertIn(anchor, splash)
        self.assertNotIn('Text("Brendigo"', splash)
        self.assertIn('M25,1 H83 C96.25,1', icon)
        for symbol in ("M44,39 C34.1,39", "M51,64 L59,57 L76,77 L68,84 Z",
                       "M68,36 H83 V51 H68 Z"):
            self.assertIn(symbol, icon)
            self.assertIn(symbol, foreground)

    def test_primary_actions_have_explicit_readable_light_text(self):
        action = self.main.split("private fun PrimaryAction(", 1)[1].split(
            "private fun shareQrText(", 1
        )[0]
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        self.assertIn("QrexDesign.referenceButtonBrush", action)
        self.assertIn("containerColor = Color.Transparent, contentColor = Color.White", action)
        self.assertIn("disabledContainerColor = Color.Transparent", action)
        self.assertIn("if (enabled) QrexDesign.referenceButtonBrush", action)
        self.assertIn("BorderStroke(1.dp", action)
        self.assertIn("val referenceButtonBrush = Brush.verticalGradient(", design)

    def test_reference_dark_navigation_and_result_hierarchy(self):
        self.assertIn("val selectedNavigationColor = if (!darkUi) QrexDesign.actionBlue", self.main)
        self.assertIn('testTag("qrex-bottom-navigation")', self.main)
        self.assertIn("containerColor = if (darkUi) QrexDesign.backgroundDeep", self.main)
        self.assertIn("HorizontalDivider(color = if (darkUi)", self.main)
        self.assertIn("indicatorColor = if (darkUi) Color.Transparent else colors.primaryContainer", self.main)
        self.assertIn("textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold", self.main)
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        self.assertIn("modifier = modifier.fillMaxWidth().heightIn(", design)
        self.assertIn("qrexResultActionHeightDp(LocalDensity.current.fontScale).dp", design)

    def test_history_truncates_long_labels_without_removing_delete(self):
        self.assertIn('val rowTitle = when {', self.main)
        self.assertIn('val safePreview = QRHistory.preview(item.raw)', self.main)
        self.assertIn("Text(\n                                rowTitle,", self.main)
        self.assertIn("maxLines = if (largeHistoryText) 2 else 1", self.main)
        self.assertIn("overflow = TextOverflow.Ellipsis", self.main)
        # The reference History uses a single overflow; Favorites keeps its
        # direct heart. Both menu Favorite and Delete actions remain live.
        self.assertIn("historyMenu = true", self.main)
        self.assertIn('Text(tr("delete"))', self.main)
        self.assertIn("DropdownMenu(", self.main)
        self.assertIn("store.setSaved(item, false)", self.main)
        self.assertIn("store.setSaved(item, true)", self.main)
        self.assertIn('testTag("qrex-history-menu")', self.main)
        self.assertIn("pendingDelete = item", self.main)
        self.assertIn("store.remove(actual)", self.main)

    def test_settings_stack_has_real_actions_and_accessible_touch_targets(self):
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        self.assertNotIn("QrexSettingsTile(", self.main)
        self.assertNotIn("QrexSettingsTile(", design)
        self.assertIn("qrexSettingsCardHeightDp(LocalDensity.current.fontScale).dp", design)
        self.assertIn('tr("save_history"), Modifier.weight(1f)', self.main)
        self.assertIn('testTag("qrex-privacy-history-switch")', self.main)
        self.assertIn("Modifier.fillMaxWidth().heightIn(min = 64.dp)", self.main)
        self.assertIn("Modifier.heightIn(min = 48.dp)", self.main)
        self.assertIn('testTag("qrex-more-icon-plate")', design)
        self.assertNotIn("store.updateLightMode(", self.main)
        self.assertIn("QrexSettingsListRow(", self.main)
        self.assertIn('store.updateLanguage(code)', self.details)
        self.assertIn('"language" -> QrexLanguageDetails(store,', self.main)
        self.assertIn('"export" -> QrexExportDetails(', self.main)
        self.assertIn('confirmPdfExport = true', self.main)
        self.assertNotIn('confirmExport = true', self.main)
        self.assertIn('showDelete = true', self.main)

    def test_five_main_tabs_have_dedicated_favorites_and_accessible_history_filters(self):
        self.assertIn('listOf("scan", "create", "history", "favorites", "more")', self.main)
        self.assertIn("navTargets = listOf(0, 1, 2, 3, 4)", self.main)
        self.assertIn("QrexDesign.heroBrush", self.main)
        self.assertIn('initiallySavedOnly = true, favoritesPage = true', self.main)
        self.assertIn('"qrex-favorites-screen" else "qrex-history-screen"', self.main)
        self.assertIn('testTag("qrex-history-filter-toggle")', self.main)
        self.assertIn('testTag("qrex-history-saved")', self.main)
        self.assertIn("QRHistory.matches(it, search, favoritesPage || savedOnly)", self.main)
        self.assertIn("private fun AppHeader(", self.main)
        self.assertIn("hasSaved = store.items.any { it.saved }", self.main)

    def test_dark_splash_and_reference_app_bars_are_real_compose_actions(self):
        splash = (NATIVE / "QREXSplash.kt").read_text("utf-8")
        self.assertIn("internal fun QrexSplashScreen(", splash)
        self.assertIn("QrexDesign.backgroundDeep", splash)
        self.assertIn("R.drawable.qrex_icon", splash)
        self.assertIn("screenshotMode != \"splash\"", self.main)
        # Reference 03 shows a real Back arrow, centered Create title, and
        # overflow menu. Ensure Back calls the Scanner navigation callback.
        create = self.main.split("private fun CreateScreen(", 1)[1].split(
            "private fun HistoryScreen(", 1
        )[0]
        self.assertIn('testTag("qrex-create-back")', create)
        self.assertIn('testTag("qrex-create-title")', create)
        self.assertIn('testTag("qrex-create-overflow")', create)
        self.assertIn("onBack()", create)
        self.assertIn("selected = 0", self.main)
        # Plain History and More headings must not duplicate launcher artwork.
        self.assertIn("private fun AppHeader(", self.main)
        self.assertNotIn("painter = painterResource(R.drawable.qrex_icon)", self.main)
        self.assertIn("val overflowTypes = listOf(3, 4, 5, 7)", self.main)
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        self.assertIn("painterResource(R.drawable.qrex_icon)", design)
        self.assertIn('Text("X"', design)
        self.assertIn("Icons.Default.Shield", self.main)
        self.assertIn('contentDescription = tr("more")', self.main)
        self.assertIn("showResultMenu", self.main)
        self.assertIn('testTag("qrex-result-close")', self.main)

    def test_compact_create_quick_types_are_not_hidden_by_unreachable_branch(self):
        # Before this fix, an empty if swallowed the entire compact layout,
        # making the 2x2 type grid unreachable on narrow phones.
        self.assertNotIn("if (layout.stackTypeChoices) {            }", self.main)
        self.assertNotIn("else if (layout.stackTypeChoices)", self.main)
        self.assertEqual(self.main.count("if (layout.stackTypeChoices) {"), 1)
        self.assertIn('modifier = Modifier.weight(1f).testTag("qrex-create-type-$index")',
                      self.main)
        self.assertIn('.testTag("qrex-create-type-$index")', self.main)
        self.assertIn('testTag("qrex-create-wifi-password")', self.main)

    def test_premium_navigation_accessibility_and_calendar_failure_feedback(self):
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        self.assertIn("val focusManager = LocalFocusManager.current", self.main)
        self.assertIn("focusManager.clearFocus()", self.main)
        self.assertIn("this.selected = selected", self.main)
        self.assertIn("this.selected = kind == index", self.main)
        self.assertIn("heightIn(min = 54.dp)", self.main)
        self.assertIn("fontSize = 12.sp, fontWeight = FontWeight.SemiBold", self.main)
        self.assertIn("Text(label, maxLines = 2,", self.main)
        self.assertIn("softWrap = true, overflow = TextOverflow.Ellipsis", self.main)
        self.assertIn("QrexNavigationGlyph(", self.main)
        self.assertIn('testTag("qrex-nav-glyph-$index")', self.main)
        # A clickable Material Surface gives each settings card its single
        # accessible Button role; the icon plate has no nested click handler.
        self.assertIn("onClick = onClick", design)
        self.assertIn('if (wifiPassword.isNotEmpty()) {', self.main)
        self.assertIn("onClick = { createMenu = true }", self.main)
        self.assertIn('Modifier.size(48.dp).testTag("qrex-create-overflow")',
                      self.main)
        self.assertIn("private fun openCalendarEvent(context: Context, raw: String): Boolean", self.main)
        self.assertIn("if (end <= start) return false", self.main)
        self.assertIn("if (!openCalendarEvent(context, raw))", self.main)
        self.assertIn("Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()", self.main)
        self.assertIn("qrexSettingsCardHeightDp(LocalDensity.current.fontScale).dp", design)

    def test_pixel_audit_icons_buttons_compact_options_and_language(self):
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        details = (NATIVE / "QREXMoreDetails.kt").read_text("utf-8")
        vectors = ROOT / "platforms/android/app/src/main/res/drawable"
        background = (vectors / "qrex_launcher_background.xml").read_text("utf-8")
        splash_icon = (vectors / "qrex_icon.xml").read_text("utf-8")
        monochrome = (vectors / "qrex_icon_monochrome.xml").read_text("utf-8")
        for color in ("#19CAFF", "#0868EE", "#07187E"):
            self.assertIn(color, splash_icon)
            self.assertIn(color, background)
        self.assertIn('android:fillType="evenOdd"', monochrome)
        self.assertIn('android:scaleX="0.91"', monochrome)
        self.assertIn('android:pivotX="54"', monochrome)
        self.assertIn("M72,40 H79 V47 H72 Z", monochrome)
        self.assertIn("val stackChoices = widthDp < 340", design)
        self.assertIn("qrexSettingsCardHeightDp(LocalDensity.current.fontScale).dp", design)
        self.assertIn("qrexResultActionHeightDp(LocalDensity.current.fontScale).dp", design)
        self.assertIn("if (fontScale >= 1.35f) 60 else 52", design)
        self.assertIn('testTag("qrex-history-menu").size(48.dp)', self.main)
        self.assertIn('.testTag("qrex-favorites-remove")', self.main)
        self.assertNotIn('qrex-history-favorite', self.main)
        self.assertIn('if (favoritesPage) {', self.main)
        self.assertIn('.testTag("qrex-favorites-remove")', self.main)
        self.assertIn("heightIn(min = 60.dp)", self.main)
        self.assertIn("heightIn(min = 64.dp)", details)
        self.assertIn("Text(types[index], maxLines = 2", self.main)

    def test_reference_create_controls_keep_real_pinned_touch_targets(self):
        self.assertIn("QrexCreateTypeChoice(types[index], typeIcons[index]", self.main)
        self.assertIn("Icon(icon, contentDescription = null", self.main)
        self.assertIn("layout.stackTypeChoices", self.main)
        self.assertIn("showIcon = false", self.main)
        self.assertIn('testTag("qrex-create-clear-url")', self.main)
        self.assertIn('testTag("qrex-create-url-caption")', self.main)
        self.assertIn("icon = Icons.Default.QrCode", self.main)
        self.assertIn("Icons.Default.Download", self.main)
        self.assertIn("listOf(0, 1, 2, 6)", self.main)
        self.assertIn('testTag("qrex-create-show")', self.main)
        self.assertIn('testTag("qrex-create-save")', self.main)
        self.assertLess(self.main.index('testTag("qrex-create-show")'),
                        self.main.index('testTag("qrex-create-save")'))
        self.assertIn('focusManager.clearFocus(force = true)', self.main)

    def test_result_actions_have_full_width_real_callbacks(self):
        result = self.main.split("private fun ResultScreen(", 1)[1].split(
            "fun openLink(", 1
        )[0]
        self.assertIn('tr("copy"), Icons.Default.ContentCopy,', result)
        self.assertIn('testTag("qrex-result-copy")', result)
        self.assertIn('testTag("qrex-result-menu-copy")', result)
        self.assertEqual(result.count("if (wifi) confirmWifiCopy = true else {"), 2)
        self.assertIn('testTag("qrex-result-wifi-copy-warning")', result)
        self.assertIn('QrexResultAction(tr("share"), Icons.Default.Share)', self.main)
        self.assertIn('testTag("qrex-result-toggle-qr")', self.main)
        self.assertIn('if (alreadySaved) tr("remove_favorite") else tr("add_favorite")', self.main)
        self.assertIn("Scaffold(containerColor = colors.background, bottomBar = {", self.main)
        self.assertNotIn("if (currentCode == null) {\\n            NavigationBar", self.main)

    def test_reference_result_single_hero_card_and_destructive_row(self):
        design = (NATIVE / "QREXDesign.kt").read_text("utf-8")
        self.assertIn('testTag("qrex-result-summary")', self.main)
        self.assertIn('modifier = Modifier.size(52.dp).testTag("qrex-result-menu")', self.main)
        self.assertIn('testTag("qrex-result-primary")', self.main)
        self.assertIn('testTag("qrex-result-delete")', self.main)
        self.assertIn("min = if (compactResult) 154.dp else if (wideResult) 192.dp else 180.dp", self.main)
        self.assertNotIn("heightIn(min = 178.dp)", self.main)
        result = self.main.split("private fun ResultScreen(", 1)[1].split(
            "fun openLink(", 1
        )[0]
        self.assertIn("shape = RoundedCornerShape(14.dp)", result)
        self.assertIn("color = QrexDesign.actionBlue", result)
        self.assertIn("Modifier.size(if (wideResult) 62.dp else 54.dp).background(resultGlyphBrush)", result)
        self.assertIn("val resultGlyphBrush = when {", result)
        self.assertIn("val resultGlyphStroke = when {", result)
        self.assertIn('testTag("qrex-result-glyph")', result)
        self.assertIn("shadowElevation = 5.dp", result)
        self.assertIn("modifier = Modifier.size(52.dp).testTag(\"qrex-result-close\")", self.main)
        self.assertIn("destructive = true", self.main)
        self.assertIn('showChevron = false', self.main)
        self.assertIn("val foreground = if (destructive) MaterialTheme.colorScheme.error", design)
        self.assertIn("if (showChevron && !destructive)", design)
        self.assertIn("qrexResultActionHeightDp(LocalDensity.current.fontScale).dp", design)
        self.assertIn("if (fontScale >= 1.35f) 60 else 52", design)
        self.assertIn('if (wifi) confirmWifiShare = true', self.main)
        self.assertIn('if (wifi && !alreadySaved) confirmWifiSave = true', self.main)
        self.assertIn('if (confirmResultDelete)', self.main)
        self.assertIn('if (showQrPreview)', self.main)

    def test_scan_first_has_one_torch_button_and_a_primary_gallery_action(self):
        self.assertIn('testTag("qrex-scan-gallery")', self.scanner)
        self.assertIn('testTag("qrex-scan-torch")', self.scanner)
        self.assertIn("ButtonDefaults.buttonColors(", self.scanner)
        self.assertNotIn("modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)", self.scanner)
        self.assertIn("ActivityResultContracts.PickVisualMedia()", self.scanner)

    def test_scanner_reference_actions_overlay_real_live_preview(self):
        self.assertIn('testTag("qrex-camera-preview")', self.scanner)
        self.assertIn('testTag("qrex-scan-hint")', self.scanner)
        self.assertIn('scannerActions(true)', self.scanner)
        self.assertIn('scannerActions(false)', self.scanner)
        self.assertIn('val scannerActions: @Composable (Boolean) -> Unit', self.scanner)
        # Reserve space for real buttons; never center scan corners
        # over the footer on a short phone.
        self.assertIn("maxSidePx = 440.dp.toPx()", self.scanner)
        self.assertIn("scannerFrameSidePx(", self.scanner)
        self.assertIn("size.width, usableHeight, maxSidePx", self.scanner)
        self.assertIn("widthPx * .73f", self.scanner)
        self.assertIn("usableHeightPx * .83f", self.scanner)
        self.assertIn('testTag("qrex-scan-brackets")', self.scanner)
        self.assertIn("size.height * 0.45f", self.scanner)
        self.assertIn('testTag("qrex-scan-controls")', self.scanner)
        # Show only zoom values advertised by the active CameraX camera.
        self.assertIn("scannerZoomPresets(it.minZoomRatio, it.maxZoomRatio)", self.scanner)
        self.assertIn("zoomPresets.size > 1", self.scanner)
        self.assertIn("setZoomRatio(ratio)", self.scanner)
        self.assertIn("QrexDesign.backgroundDeep.copy(alpha = .88f)", self.scanner)
        self.assertIn("galleryActive.compareAndSet(false, true)", self.scanner)
        self.assertIn("onClick = toggleTorch", self.scanner)

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
        self.assertIn('val safePreview = QRHistory.preview(item.raw)', self.main)
        self.assertIn('"qrex-favorites-remove"', self.main)
        self.assertIn("store.setSaved(item, !item.saved)", self.main)
        self.assertIn("QRContent.contact(input, contactPhone, contactEmail)", self.main)
        self.assertIn('EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name()', self.main)
        splash = (NATIVE / "QREXSplash.kt").read_text("utf-8")
        self.assertIn("Modifier.size(metrics.logoDp.dp)", splash)
        self.assertIn("qrexSplashMetrics(", splash)

    def test_irreplaceable_history_migration_and_wifi_encryption_are_retained(self):
        self.assertIn('getSharedPreferences("FlutterSharedPreferences"', self.core)
        self.assertIn("migrateFlutterV1()", self.core)
        self.assertIn("QREXSecretCipher.encrypt(item.raw)", self.core)
        self.assertIn("QREXSecretCipher.decrypt(value)", self.core)
        self.assertIn("if (!historyReadable) return false", self.core)
        self.assertIn("protectedWifi", self.core)


if __name__ == "__main__":
    unittest.main()

