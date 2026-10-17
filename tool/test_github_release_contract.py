#!/usr/bin/env python3
"""GitHub 1.0.1 must not be confused with the separately Play-signed release."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github/workflows/github-release-1.0.1.yml"


class GithubReleaseContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text("utf-8")
        cls.notes = (ROOT / "docs/releases/v1.0.1.md").read_text("utf-8")

    def test_require_three_successful_main_sha_checks(self):
        self.assertIn('workflows: ["QREX Android native CI"]', self.workflow)
        self.assertIn("types: [completed]", self.workflow)
        self.assertIn("branches: [main]", self.workflow)
        self.assertIn("github.event.workflow_run.conclusion == 'success'", self.workflow)
        self.assertIn("github.event.workflow_run.event == 'push'", self.workflow)
        self.assertIn("git rev-parse HEAD", self.workflow)
        self.assertIn("commits/main", self.workflow)
        for name in ("native-platforms.yml", "capture-screenshots.yml",
                     "generate-localizations.yml"):
            self.assertIn(name, self.workflow)

    def test_github_build_is_installable_but_not_play_signed(self):
        self.assertIn("keytool -genkeypair", self.workflow)
        self.assertIn("openssl rand -hex 24", self.workflow)
        self.assertIn(":app:assembleRelease :app:bundleRelease", self.workflow)
        self.assertIn("apksigner", self.workflow)
        self.assertIn('jarsigner -verify "$aab"', self.workflow)
        self.assertIn("grep -F 'jar verified.'", self.workflow)
        self.assertNotIn('jarsigner -verify -strict "$aab"', self.workflow)
        self.assertIn('test "$cert_aab" = "$cert_apk"', self.workflow)
        self.assertIn("com.brendigo.qrex 101 1.0.1", self.workflow)
        self.assertIn("QREX-Android-1.0.1.apk", self.workflow)
        self.assertIn("QREX-Android-1.0.1.aab", self.workflow)
        self.assertIn("SHA256SUMS.txt", self.workflow)
        self.assertIn("gh release create v1.0.1", self.workflow)
        self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", self.workflow)
        self.assertNotIn("secrets.QREX_UPLOAD_CERT_SHA256", self.workflow)

    def test_explicit_upgrade_and_play_disclosure(self):
        for expected in ("disposable", "DIFFERENT certificate", "NOT a Google Play upload",
                         "versionCode 101", "SHA256SUMS.txt"):
            self.assertIn(expected, self.notes)
        self.assertIn("QREX-Android-1.0.1.aab", self.notes)
        self.assertIn("QREX-Android-1.0.1.apk", self.notes)
        self.assertFalse((ROOT / ".github/workflows/github-release-1.0.0.yml").exists())


if __name__ == "__main__":
    unittest.main()
