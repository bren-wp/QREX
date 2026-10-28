#!/usr/bin/env python3
"""QREX 1.0.16 single-release and reference accessibility contracts."""
from pathlib import Path
import json
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOWS = ROOT / ".github/workflows"
ANDROID = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


class Production116ReferenceTests(unittest.TestCase):
    def test_published_versions_are_archived_outside_active_workflows(self):
        archived = ROOT / "docs/releases/workflows"
        self.assertEqual(
            ["github-release-1.0.18.yml"], sorted(p.name for p in WORKFLOWS.glob("github-release-1.0.*.yml")),
            "No already-published version must trigger on future main pushes"
        )
        for minor in range(1, 18):
            src = (archived / f"github-release-1.0.{minor}.yml").read_text("utf-8")
            with self.subTest(version=minor):
                self.assertIn(f"gh release create v1.0.{minor}", src)
                self.assertIn("github.event.workflow_run", src)
                self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", src)
        latest = (archived / "github-release-1.0.16.yml").read_text("utf-8")
        self.assertIn('workflows: ["QREX Android native CI"]', latest)
        self.assertIn("com.brendigo.qrex 116 1.0.16", latest)
        self.assertIn("sha256sum --check SHA256SUMS.txt", latest)
        self.assertIn("assert len(screens) == 10", latest)
        self.assertIn("QREX-Android-1.0.16-Play-UNSIGNED-QA.aab", latest)
        for gate in ("native-platforms.yml", "capture-screenshots.yml",
                     "generate-localizations.yml", "jarsigner -verify",
                     "apksigner", "tool/verify_screenshots.py"):
            self.assertIn(gate, latest)

    def test_version_and_native_navigation(self):
        gradle = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
        self.assertIn("versionCode = 118", gradle)
        self.assertIn('versionName = "1.0.18"', gradle)
        details = (ANDROID / "QREXMoreDetails.kt").read_text("utf-8")
        self.assertIn('Modifier.size(52.dp).testTag("qrex-detail-back")', details)
        self.assertIn('contentDescription = tr("back")', details)
        main = (ANDROID / "MainActivity.kt").read_text("utf-8")
        self.assertIn("Icons.Default.Search", main)
        self.assertIn('qrex-history-search', main)
        self.assertIn('qrex-create-more-types', main)
        self.assertIn('qrex-more-about', main)

    def test_all_back_translations_exist(self):
        j = json.loads((ROOT / "l10n/catalog.json").read_text("utf-8"))
        self.assertEqual(len(j["languages"]), 23)
        for lang,entry in j["languages"].items():
            with self.subTest(locale=lang):
                self.assertTrue(entry["strings"]["back"].strip())
        mapper = (ANDROID / "QREXStrings.kt").read_text("utf-8")
        self.assertIn('"back" -> R.string.back', mapper)

    def test_old_images_do_not_claim_pixel_equality(self):
        release = (ROOT / "docs/releases/v1.0.16.md").read_text("utf-8")
        self.assertIn("does **not claim**", release)
        self.assertIn("perspective-transformed", release)
        self.assertIn("disposable", release)


if __name__ == "__main__":
    unittest.main()
