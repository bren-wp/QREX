#!/usr/bin/env python3
"""Regression tests: a green build must not hide invalid store metadata."""
from pathlib import Path
import unittest
from check_native_release import ROOT, check_configuration


class NativeReleaseGateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.android = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
        cls.ios = (ROOT / "platforms/ios/project.yml").read_text("utf-8")
        cls.manifest = (ROOT / "platforms/android/app/src/main/AndroidManifest.xml").read_text("utf-8")
        cls.privacy = (ROOT / "platforms/ios/QREX/PrivacyInfo.xcprivacy").read_bytes()

    def check(self, android=None, ios=None, manifest=None, privacy=None, shared=False):
        return check_configuration(
            self.android if android is None else android,
            self.ios if ios is None else ios,
            self.manifest if manifest is None else manifest,
            self.privacy if privacy is None else privacy,
            shared,
        )

    def test_valid_repo_config(self):
        self.assertEqual(self.check(), [])

    def test_outdated_target_api_is_rejected(self):
        issues = self.check(android=self.android.replace("targetSdk = 36", "targetSdk = 35"))
        self.assertTrue(any("targetSdk" in issue for issue in issues), issues)

    def test_ios_signing_disabled_by_default_is_rejected(self):
        issues = self.check(ios=self.ios + '\n    CODE_SIGNING_ALLOWED: "NO"\n')
        self.assertTrue(any("signing" in issue.lower() for issue in issues), issues)

    def test_mismatched_versions_are_rejected(self):
        issues = self.check(ios=self.ios.replace('MARKETING_VERSION: "0.2.2"', 'MARKETING_VERSION: "9.9.9"'))
        self.assertTrue(any("versions differ" in issue for issue in issues), issues)

    def test_privilege_creep_is_rejected(self):
        extra = '    <uses-permission android:name="android.permission.INTERNET"/>\n'
        manifest = self.manifest.replace("<application ", extra + "<application ")
        issues = self.check(manifest=manifest)
        self.assertTrue(any("INTERNET" in issue for issue in issues), issues)

    def test_stale_keychain_entitlement_is_rejected(self):
        issues = self.check(shared=True)
        self.assertTrue(any("Keychain" in issue for issue in issues), issues)

    def test_invalid_privacy_manifest_is_rejected(self):
        self.assertTrue(self.check(privacy=b"not-a-plist"))


if __name__ == "__main__":
    unittest.main()
