#!/usr/bin/env python3
"""Contracts for explicitly requested GitHub-only QREX 1.0.0 distribution.

A GitHub release is not a Google Play signed or independently audited build.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
RELEASE = ROOT / ".github/workflows/github-release-1.0.0.yml"


class GithubReleaseContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = RELEASE.read_text("utf-8")
        cls.notes = (ROOT / "docs/releases/v1.0.0.md").read_text("utf-8")

    def test_autopublisher_only_runs_after_green_main_push(self):
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
        self.assertIn('if [ "$result" != "completed:success" ]', self.workflow)
        self.assertIn("permissions:", self.workflow)
        self.assertIn("contents: write", self.workflow)
        self.assertIn("actions: read", self.workflow)

    def test_installable_release_apk_and_aab_with_same_certificate(self):
        self.assertIn("keytool -genkeypair", self.workflow)
        self.assertIn("openssl rand -hex 24", self.workflow)
        self.assertIn(":app:assembleRelease :app:bundleRelease", self.workflow)
        self.assertIn("apksigner", self.workflow)
        self.assertIn("jarsigner -verify -strict", self.workflow)
        self.assertIn('test "$cert_aab" = "$cert_apk"', self.workflow)
        self.assertIn("QREX-Android-1.0.0.apk", self.workflow)
        self.assertIn("QREX-Android-1.0.0.aab", self.workflow)
        self.assertIn("SHA256SUMS.txt", self.workflow)
        self.assertIn("gh release create v1.0.0", self.workflow)
        self.assertIn("--target \"$RELEASE_SHA\"", self.workflow)

    def test_no_authorized_play_key_is_forged_or_required(self):
        self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", self.workflow)
        self.assertNotIn("secrets.QREX_UPLOAD_CERT_SHA256", self.workflow)
        self.assertIn("one-time generated GitHub Actions certificate", self.notes)
        self.assertIn("cannot update an existing QREX installation", self.notes)
        self.assertIn("not approved for Google Play", self.notes)
        self.assertIn("SHA256SUMS.txt", self.notes)

    def test_v1_is_not_misrepresented_as_unsigned_installable_apk(self):
        self.assertIn("Android requires APKs to be signed", self.notes)
        self.assertIn("not signed with your original production certificate", self.notes)
        self.assertNotIn("QREX-iOS-", self.workflow)


if __name__ == "__main__":
    unittest.main()
