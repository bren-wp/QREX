#!/usr/bin/env python3
"""Production release safety checks: QREX never publishes unsigned QA binaries."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOWS = ROOT / ".github/workflows"


class ReleaseIntegrityTests(unittest.TestCase):
    def test_legacy_automatic_preview_release_is_disabled(self):
        self.assertFalse((WORKFLOWS / "release.yml").exists())
        self.assertFalse((WORKFLOWS / "release-native-preview.yml").exists())
        self.assertFalse((ROOT / "tool/publish_native_rc.sh").exists())
        self.assertTrue((ROOT / "tool/verify_production_gate.sh").is_file())

    def test_production_release_is_manual_main_only_and_signed(self):
        workflow = (WORKFLOWS / "production-release.yml").read_text("utf-8")
        gate = (ROOT / "tool/verify_production_gate.sh").read_text("utf-8")
        self.assertIn("workflow_dispatch:", workflow)
        self.assertNotIn("  push:", workflow)
        self.assertNotIn("  pull_request:", workflow)
        self.assertNotIn("  release:", workflow)
        self.assertIn("environment: production", workflow)
        self.assertIn("device_qa_confirmed:", workflow)
        self.assertIn("store_review_confirmed:", workflow)
        self.assertIn("RELEASE_1_0_0", gate)
        self.assertIn('refs/heads/main', gate)
        self.assertIn("commits/main", gate)
        self.assertIn('v1.0.0 already tagged', gate)
        for check in ("native-platforms.yml", "capture-screenshots.yml",
                      "generate-localizations.yml", "mobile.yml"):
            self.assertIn(check, gate)
        self.assertIn("completed:success", gate)
        self.assertIn("needs: [verify, android, ios]", workflow)
        self.assertIn("QREX-Android-1.0.0.aab", workflow)
        self.assertIn("QREX-iOS-1.0.0.ipa", workflow)
        self.assertIn("SHA256SUMS.txt", workflow)
        self.assertIn("jarsigner -verify -strict", workflow)
        self.assertIn("gh release create v1.0.0", workflow)
        self.assertIn("--latest", workflow)
        self.assertNotIn("--prerelease", workflow)
        self.assertNotIn("QREX-Android-debug", workflow)
        self.assertNotIn("QREX-Android-unsigned", workflow)
        self.assertNotIn("QREX-iOS-unsigned", workflow)

    def test_production_signing_requires_authorized_identities(self):
        android = (ROOT / "tool/sign_android_production.sh").read_text("utf-8")
        ios = (ROOT / "tool/sign_ios_production.sh").read_text("utf-8")
        self.assertIn("QREX_UPLOAD_CERT_SHA256", android)
        self.assertIn("QREX_UPLOAD_KEYSTORE_BASE64", android)
        self.assertIn("jarsigner -verify -strict", android)
        self.assertIn("keytool -printcert -jarfile", android)
        self.assertIn("QREX_APPLE_CERT_SHA256", ios)
        self.assertIn("QREX_APPLE_TEAM_ID", ios)
        self.assertIn("QREX_IOS_PROVISION_PROFILE_NAME", ios)
        self.assertIn("codesign --verify --strict", ios)
        self.assertIn('test "$signed_team" = "$QREX_APPLE_TEAM_ID"', ios)

    def test_prior_rc_releases_stay_correctly_classified(self):
        guardian = (WORKFLOWS / "guard-release-metadata.yml").read_text("utf-8")
        self.assertIn("release:", guardian)
        self.assertIn("published, edited", guardian)
        self.assertIn("prerelease=true", guardian)
        self.assertIn("make_latest=false", guardian)

    def test_public_docs_and_production_metadata(self):
        gradle = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
        ios = (ROOT / "platforms/ios/project.yml").read_text("utf-8")
        readme = (ROOT / "README.md").read_text("utf-8")
        release_notes = (ROOT / "RELEASE_NOTES.md").read_text("utf-8")
        checklist = (ROOT / "docs/STORE_CHECKLIST.md").read_text("utf-8")
        for body in (readme, release_notes):
            self.assertIn("github.com/bren-wp/QREX/releases", body)
            self.assertNotIn("github.com/bren-wp/Qivio", body)
        self.assertIn("1.0.0", release_notes)
        self.assertIn("version **1.0.0** / build number **100**", checklist)
        self.assertIn("marketing version `1.0.0`, build `100`", checklist)
        self.assertIn('versionCode = 100', gradle)
        self.assertIn('versionName = "1.0.0"', gradle)
        self.assertIn('CURRENT_PROJECT_VERSION: "100"', ios)
        self.assertIn('MARKETING_VERSION: "1.0.0"', ios)
        self.assertTrue((ROOT / "docs/releases/v1.0.0.md").exists())


if __name__ == "__main__":
    unittest.main()
