#!/usr/bin/env python3
"""Customer-facing QREX language, settings and launcher branding regressions."""
from pathlib import Path
import json
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "platforms/android/app/src/main/res"
IOS = ROOT / "platforms/ios/QREX"


class CustomerProductReadinessTests(unittest.TestCase):
    def test_23_languages_are_complete_including_croatian(self):
        catalog = json.loads((ROOT / "l10n/catalog.json").read_text("utf-8"))
        languages = catalog["languages"]
        self.assertEqual(catalog["default"], "en")
        self.assertEqual(len(languages), 23)
        self.assertIn("hr", languages)
        keys = set(languages["en"]["strings"])
        for locale, entry in languages.items():
            with self.subTest(locale=locale):
                self.assertEqual(set(entry["strings"]), keys)
                for key in ("export_archive", "terms_of_use", "privacy_policy",
                            "export_archive_warning", "read_full_policy"):
                    self.assertTrue(entry["strings"][key].strip())
                self.assertTrue((IOS / f"{locale}.lproj/Localizable.strings").exists())
                values = RES / ("values" if locale == "en" else f"values-{locale}") / "strings.xml"
                self.assertTrue(values.exists())

    def test_install_icon_has_adaptive_and_themable_resources(self):
        manifest = ET.parse(ROOT / "platforms/android/app/src/main/AndroidManifest.xml")
        namespace = "{http://schemas.android.com/apk/res/android}"
        application = manifest.find("application")
        self.assertIsNotNone(application)
        self.assertEqual(application.get(namespace + "icon"), "@mipmap/ic_launcher")
        self.assertEqual(application.get(namespace + "roundIcon"), "@mipmap/ic_launcher_round")
        for name in ("ic_launcher", "ic_launcher_round"):
            path = RES / f"mipmap-anydpi-v26/{name}.xml"
            root = ET.parse(path).getroot()
            self.assertEqual(root.tag, "adaptive-icon")
            self.assertIsNotNone(root.find("monochrome"))
            self.assertIsNotNone(root.find("foreground"))
        self.assertTrue((RES / "drawable/qrex_icon_monochrome.xml").exists())

    def test_document_export_and_legal_actions_are_real_on_both_platforms(self):
        android = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt").read_text("utf-8")
        ios = (IOS / "QREXView.swift").read_text("utf-8")
        for key in ("export_archive", "privacy_policy", "terms_of_use", "developed_by"):
            self.assertIn(key, android)
            self.assertIn(key, ios)
        self.assertIn("QREXDocuments.exportSavedPdfZip(", android)
        self.assertIn("QREXDocuments.exportSavedPdfZip(", ios)
        self.assertIn("CreateDocument(\"application/zip\")", android)
        self.assertIn(".fileExporter(", ios)
        self.assertTrue((ROOT / "docs/PRIVACY.md").exists())
        self.assertTrue((ROOT / "docs/TERMS.md").exists())

    def test_pdf_zip_exports_dont_require_third_party_storage_permissions(self):
        text = (ROOT / "platforms/android/app/src/main/AndroidManifest.xml").read_text("utf-8")
        for forbidden in ("READ_EXTERNAL_STORAGE", "MANAGE_EXTERNAL_STORAGE",
                          "WRITE_EXTERNAL_STORAGE", "READ_MEDIA_IMAGES"):
            self.assertNotIn(forbidden, text)


if __name__ == "__main__":
    unittest.main()
