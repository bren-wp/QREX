#!/usr/bin/env python3
"""Require genuine document captures to ship with the next stable QREX release."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
PUBLISHER = ROOT / ".github/workflows/github-release.yml"
CAPTURE = ROOT / ".github/workflows/capture-screenshots.yml"
GRADLE = ROOT / "platforms/android/app/build.gradle.kts"

class DocumentScreenshotReleaseTests(unittest.TestCase):
    def test_editor_gallery_has_eight_disambiguated_actual_capture_links(self):
        readme = README.read_text("utf-8")
        # The README embeds proven, *published* screenshot assets.
        # A newer Gradle candidate must not imply its future captures exist.
        published = re.search(
            r'Current release: v(1\.0\.\d+) \(published\)', readme
        )
        candidate = re.search(
            r'versionName\s*=\s*"(1\.0\.\d+)"',
            GRADLE.read_text("utf-8")
        )
        self.assertIsNotNone(published)
        self.assertIsNotNone(candidate)
        self.assertGreaterEqual(
            int(candidate.group(1).split(".")[-1]),
            int(published.group(1).split(".")[-1])
        )
        if candidate.group(1) != published.group(1):
            self.assertIn("Upcoming v" + candidate.group(1), readme)
        expected = [f"document-{theme}-{screen}.png"
                    for theme in ("dark", "light")
                    for screen in ("review", "crop", "pages", "export")]
        for name in expected:
            with self.subTest(name=name):
                self.assertEqual(1, readme.count(
                    "/releases/download/v" + published.group(1) + "/" + name))
                self.assertIn("actual", readme.lower())
        self.assertIn("synthetic paper", readme)
        self.assertIn("0% pixel-difference", readme)

    def test_release_requires_exact_sha_document_capture_artifacts(self):
        publisher = PUBLISHER.read_text("utf-8")
        capture = CAPTURE.read_text("utf-8")
        self.assertIn("qrex-actual-document-editor", publisher)
        self.assertIn("qrex-actual-document-editor", capture)
        self.assertIn('gh run download "$capture_run"', publisher)
        self.assertIn('test -s "$original"', publisher)
        self.assertIn('assert {shot.name for shot in documents} == expected', publisher)
        self.assertIn("production/document-*.png", publisher)
        self.assertIn("document-*.png > SHA256SUMS.txt", publisher)
        self.assertIn('(.assets | length) == 24', publisher)
        self.assertIn("sha256sum --check SHA256SUMS.txt", publisher)

if __name__ == "__main__":
    unittest.main()
