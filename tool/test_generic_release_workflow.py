#!/usr/bin/env python3
"""Source-derived GitHub publisher must preserve the exact-SHA QA gates."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / ".github/workflows/github-release.yml"


class GenericReleaseWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.body = SOURCE.read_text("utf-8")

    def test_only_green_main_push_may_publish(self):
        self.assertIn("workflow_run:", self.body)
        self.assertIn("github.event.workflow_run.event == 'push'", self.body)
        self.assertIn("head_branch == 'main'", self.body)
        self.assertIn("cancel-in-progress: false", self.body)
        self.assertIn("git rev-parse HEAD", self.body)
        self.assertIn("commits/main", self.body)

    def test_version_is_derived_from_gradle_and_tag_not_reused(self):
        self.assertIn("versionName =", self.body)
        self.assertIn("versionCode =", self.body)
        self.assertIn("needs.preflight.outputs.version", self.body)
        self.assertIn("needs.preflight.outputs.code", self.body)
        self.assertIn('gh release view "v$version"', self.body)
        self.assertNotIn("gradle-version: '9.1.0'", self.body)

    def test_hard_release_gates_and_disposable_signing(self):
        for value in ("native-platforms.yml", "capture-screenshots.yml",
                      "generate-localizations.yml", "p03-stability.yml",
                      ":app:testDebugUnitTest :app:lintRelease",
                      ":app:assembleRelease :app:bundleRelease",
                      "keytool -genkeypair", "jarsigner -verify",
                      "tool/verify_screenshots.py",
                      "sha256sum --check SHA256SUMS.txt",
                      "gh release create"):
            self.assertIn(value, self.body)
        self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", self.body)
        self.assertIn("QREX-Android-$RELEASE_VERSION-Play-UNSIGNED-QA.aab", self.body)


if __name__ == "__main__":
    unittest.main()
