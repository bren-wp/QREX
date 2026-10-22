#!/usr/bin/env python3
"""Customer-facing QREX language, settings and launcher branding regressions."""
from pathlib import Path
import json
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "platforms/android/app/src/main/res"


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

    def test_approved_qrex_icon_geometry_is_shared_with_splash(self):
        ns = "{http://schemas.android.com/apk/res/android}"
        launcher = ET.parse(RES / "drawable/qrex_icon.xml").getroot()
        mono = ET.parse(RES / "drawable/qrex_icon_monochrome.xml").getroot()
        self.assertEqual(launcher.get(ns + "viewportWidth"), "108")
        self.assertEqual(mono.get(ns + "viewportWidth"), "108")
        paths = launcher.findall("path")
        mono_paths = mono.findall("path")
        # Q stroke and QR pixel cells must be real vector geometry,
        # not a screenshot of an app UI or a white rectangle placeholder.
        for path_list in (paths, mono_paths):
            self.assertTrue(any(p.get(ns + "strokeWidth") == "10" for p in path_list))
            self.assertTrue(any("M68,36" in p.get(ns + "pathData", "") for p in path_list))
            self.assertTrue(any("M51,64" in p.get(ns + "pathData", "") for p in path_list))
        splash = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/QREXSplash.kt").read_text("utf-8")
        self.assertIn("painterResource(R.drawable.qrex_icon)", splash)
        self.assertNotIn("painterResource(R.drawable.qrex_icon_monochrome)", splash)

    def test_document_export_and_legal_actions_are_real_on_android(self):
        android = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt").read_text("utf-8")
        for key in ("export_archive", "privacy_policy", "terms_of_use", "developed_by"):
            self.assertIn(key, android)
        self.assertIn("QREXDocuments.exportSavedPdfZip(", android)
        self.assertIn("QREXDocuments.exportSavedPdf(", android)
        self.assertIn('CreateDocument("application/pdf")', android)
        self.assertIn('CreateDocument("application/zip")', android)
        self.assertTrue((ROOT / "docs/PRIVACY.md").exists())
        self.assertTrue((ROOT / "docs/TERMS.md").exists())

    def test_current_android_links_and_privacy_summary(self):
        readme = (ROOT / "README.md").read_text("utf-8")
        android = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt").read_text("utf-8")
        for source in (readme, android):
            self.assertNotIn("github.com/bren-wp/Qivio", source)
        self.assertIn("https://github.com/bren-wp/QREX/blob/main/docs/PRIVACY.md", android)
        self.assertIn("https://github.com/bren-wp/QREX/blob/main/docs/TERMS.md", android)

    def test_android_screenshot_fixture_only_works_in_debug(self):
        android = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt").read_text("utf-8")
        screenshots = (ROOT / ".github/workflows/capture-screenshots.yml").read_text("utf-8")
        self.assertIn("ApplicationInfo.FLAG_DEBUGGABLE", android)
        self.assertIn("Actual Android emulator screenshot", screenshots)
        self.assertNotIn("Actual iPhone simulator screenshot", screenshots)

    def test_pdf_zip_exports_dont_require_third_party_storage_permissions(self):
        text = (ROOT / "platforms/android/app/src/main/AndroidManifest.xml").read_text("utf-8")
        for forbidden in ("READ_EXTERNAL_STORAGE", "MANAGE_EXTERNAL_STORAGE",
                          "WRITE_EXTERNAL_STORAGE", "READ_MEDIA_IMAGES"):
            self.assertNotIn(forbidden, text)


if __name__ == "__main__":
    unittest.main()
