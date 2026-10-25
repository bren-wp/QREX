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
    def test_only_the_new_version_can_run_publisher_jobs(self):
        for minor in range(4, 16):
            src = (WORKFLOWS / f"github-release-1.0.{minor}.yml").read_text("utf-8")
            with self.subTest(version=minor):
                self.assertIn(
                    "github.event.workflow_run.event == 'push' && false", src
                )
                self.assertIn("RETIRED:", src)
        new = (WORKFLOWS / "github-release-1.0.16.yml").read_text("utf-8")
        self.assertNotIn("&& false", new)
        self.assertIn('workflows: ["QREX Android native CI"]', new)
        self.assertIn("gh release create v1.0.16", new)
        self.assertIn("com.brendigo.qrex 116 1.0.16", new)
        self.assertIn("sha256sum --check SHA256SUMS.txt", new)
        self.assertIn("assert len(screens) == 10", new)
        self.assertIn("QREX-Android-1.0.16-Play-UNSIGNED-QA.aab", new)
        for gate in ("native-platforms.yml", "capture-screenshots.yml",
                     "generate-localizations.yml", "jarsigner -verify",
                     "apksigner", "tool/verify_screenshots.py"):
            self.assertIn(gate, new)
        self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", new)

    def test_version_and_native_navigation(self):
        gradle = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
        self.assertIn("versionCode = 116", gradle)
        self.assertIn('versionName = "1.0.16"', gradle)
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
