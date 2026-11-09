#!/usr/bin/env python3
"""Localized History reference-screen regression contracts."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt"
CATALOG = ROOT / "l10n/catalog.json"


class HistoryReferenceRegressionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = MAIN.read_text(encoding="utf-8")
        cls.catalog = json.loads(CATALOG.read_text(encoding="utf-8"))

    def test_all_23_languages_have_real_filter_and_empty_state(self):
        langs = self.catalog["languages"]
        self.assertEqual(23, len(langs))
        en_keys = tuple(langs["en"]["strings"])
        for locale, info in langs.items():
            strings = info["strings"]
            self.assertEqual(en_keys, tuple(strings), locale)
            for key in ("all", "saved", "no_history", "no_saved"):
                with self.subTest(locale=locale, key=key):
                    self.assertTrue(strings[key].strip())

    def test_history_matches_reference_labels_and_state_specific_empty_copy(self):
        self.assertIn('text = { Text(tr("all")) }', self.source)
        self.assertIn('text = { Text(tr("saved")) }', self.source)
        self.assertIn('testTag("qrex-history-filter-toggle")', self.source)
        self.assertIn('testTag("qrex-history-search-row")', self.source)
        self.assertIn('if (savedOnly) tr("no_saved") else tr("no_history")',
                      self.source)
        for tag in ("qrex-history-all", "qrex-history-saved",
                    "qrex-history-search"):
            self.assertIn(f'testTag("{tag}")', self.source)

    def test_history_cards_redact_private_qr_details_and_guard_delete(self):
        history = self.source.split("private fun HistoryScreen(", 1)[1].split(
            "internal fun useCompactMoreList(", 1
        )[0]
        self.assertIn('val detail = when {', history)
        self.assertIn('QRContent.isEvent(item.raw) -> tr("event")', history)
        self.assertIn('QRContent.safeUrl(item.raw) != null -> tr("url")', history)
        self.assertIn('else -> tr("text")', history)
        self.assertNotIn('else -> item.raw', history)
        self.assertNotIn('QRContent.eventField(item.raw, "LOCATION").orEmpty()', history)
        self.assertIn('val subtitle = listOf(detail, date)', history)
        self.assertIn('"qrex-favorites-subtitle"', history)
        self.assertIn('"qrex-history-subtitle"', history)
        self.assertIn('val savedStateDescription =', history)
        self.assertIn('stateDescription = savedStateDescription', history)
        self.assertIn('val rowTitle = when {', history)
        self.assertIn('val safePreview = QRHistory.preview(item.raw)', history)
        self.assertIn('testTag("qrex-history-preview")', history)
        self.assertIn('color = tint.copy(alpha = .15f)', history)
        self.assertIn('if (favoritesPage) {\n                        IconButton(', history)
        self.assertIn("Text(\n                                rowTitle,", history)
        for tag in ("qrex-history-menu-delete", "qrex-history-delete-dialog",
                    "qrex-history-delete-confirm", "qrex-history-delete-cancel"):
            self.assertIn(f'testTag("{tag}")', history)
        self.assertIn("store.remove(actual)", history)
        self.assertIn("onDismissRequest = { pendingDelete = null }", history)
        self.assertIn("modifier = Modifier.heightIn(min = 48.dp)", history)

    def test_history_glyphs_use_real_category_color_and_three_dimensional_depth(self):
        history = self.source.split("private fun HistoryScreen(", 1)[1].split(
            "internal fun useCompactMoreList(", 1
        )[0]
        design = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/QREXDesign.kt").read_text("utf-8")
        self.assertIn("val glyphBackground = when {", history)
        self.assertIn("QrexHistoryGlyph(icon, tint, background = glyphBackground)", history)
        for color in ("0xFF056E64", "0xFFAF214B", "0xFF5534AD", "0xFF0753C4"):
            self.assertIn(color, history)
        self.assertIn('testTag("qrex-history-category-glyph")', design)
        self.assertIn("Modifier.size(38.dp).background(Brush.verticalGradient(", design)
        self.assertIn("listOf(Color.White.copy(alpha = .14f), Color.Transparent)", design)
        self.assertNotIn("listOf(background.copy(alpha = .78f), background)", design)
        self.assertIn("tint = Color.White", design)
        self.assertIn("shadowElevation = 3.dp", design)

    def test_controls_remain_functional_and_responsive(self):
        self.assertIn("savedOnly = false", self.source)
        self.assertIn("savedOnly = true", self.source)
        self.assertIn(".semantics { selected = !savedOnly }", self.source)
        self.assertIn(".semantics { selected = savedOnly }", self.source)
        self.assertGreaterEqual(self.source.count(".heightIn(min = 52.dp)"), 3)
        self.assertIn("QRHistory.matches(it, search, favoritesPage || savedOnly)", self.source)
        self.assertIn("itemsIndexed(found, key = { index, item -> QRHistory.uiRowKey(item, index) })", self.source)
        self.assertNotIn("items(found, key = { it.raw })", self.source)
        # History and Favorites have separate semantic empty-state tags.
        self.assertIn('"qrex-history-empty"', self.source)
        self.assertIn('"qrex-favorites-empty"', self.source)
        self.assertIn('if (favoritesPage) "qrex-favorites-empty"', self.source)
        self.assertIn("largeHistoryText = LocalDensity.current.fontScale >= 1.35f", self.source)
        self.assertIn("maxLines = if (largeHistoryText) 2 else 1", self.source)
        self.assertIn("confirm_delete", self.source)


if __name__ == "__main__":
    unittest.main()
