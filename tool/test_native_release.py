#!/usr/bin/env python3
"""Android-only production metadata and minimal-permissions regression tests."""
from pathlib import Path
import unittest
from check_native_release import ROOT, check_configuration


class NativeReleaseGateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.android = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
        cls.manifest = (ROOT / "platforms/android/app/src/main/AndroidManifest.xml").read_text("utf-8")

    def check(self, android=None, manifest=None):
        return check_configuration(
            self.android if android is None else android,
            self.manifest if manifest is None else manifest,
        )

    def test_valid_repo_config(self):
        self.assertEqual(self.check(), [])

    def test_outdated_target_api_is_rejected(self):
        issues = self.check(android=self.android.replace("targetSdk = 36", "targetSdk = 35"))
        self.assertTrue(any("targetSdk" in issue for issue in issues), issues)

    def test_published_version_code_is_rejected(self):
        issues = self.check(android=self.android.replace("versionCode = 121", "versionCode = 120"))
        self.assertTrue(any("published QREX build 120" in issue for issue in issues), issues)

    def test_prerelease_version_metadata_is_rejected(self):
        issues = self.check(
            android=self.android.replace('versionName = "1.0.21"', 'versionName = "0.2.4"')
        )
        self.assertTrue(any("Production candidate" in issue for issue in issues), issues)

    def test_privilege_creep_is_rejected(self):
        extra = '    <uses-permission android:name="android.permission.INTERNET"/>\n'
        manifest = self.manifest.replace("<application ", extra + "<application ")
        issues = self.check(manifest=manifest)
        self.assertTrue(any("INTERNET" in issue for issue in issues), issues)

    def test_backups_cannot_be_enabled(self):
        issues = self.check(manifest=self.manifest.replace('android:allowBackup="false"',
                                                           'android:allowBackup="true"'))
        self.assertTrue(any("allowBackup" in issue for issue in issues), issues)

    def test_invalid_android_manifest_is_rejected(self):
        self.assertTrue(self.check(manifest="<not-well-formed"))


if __name__ == "__main__":
    unittest.main()
