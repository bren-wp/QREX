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

    def test_androidx_fragment_updated_and_r8_optimized_with_agp9(self):
        root_gradle = (ROOT / "platforms/android/build.gradle.kts").read_text("utf-8")
        properties = (ROOT / "platforms/android/gradle.properties").read_text("utf-8")
        self.assertIn('id("com.android.application") version "9.0.1"', root_gradle)
        self.assertNotIn("org.jetbrains.kotlin.android", root_gradle)
        self.assertNotIn("org.jetbrains.kotlin.android", self.android)
        self.assertIn('implementation("androidx.fragment:fragment:1.9.1")', self.android)
        self.assertIn("isMinifyEnabled = true", self.android)
        self.assertIn("isShrinkResources = true", self.android)
        self.assertNotIn("android.r8.optimizedResourceShrinking=false", properties)

    def test_release_workflow_uses_registered_agp9_unit_test_task(self):
        # A missing Gradle task previously blocked a fully green CI build
        # from publishing the signed GitHub-only APK/AAB.
        release = (ROOT / ".github/workflows/github-release-1.0.23.yml").read_text(
            "utf-8"
        )
        ci = (ROOT / ".github/workflows/native-platforms.yml").read_text("utf-8")
        self.assertIn(":app:testDebugUnitTest", ci)
        self.assertIn(":app:testDebugUnitTest :app:lintRelease", release)
        self.assertNotIn(":app:testReleaseUnitTest", release)
        self.assertIn(":app:assembleRelease :app:bundleRelease", release)
        self.assertIn("Require all 3 green exact-commit Android checks", release)
        self.assertIn("QREX-Android-1.0.23-Play-UNSIGNED-QA.aab", release)
        # Publisher must inspect the ACTUAL new Android versionCode, not the
        # previous release's code. The v1.0.23 publisher once built correct
        # packages and then failed only at this stale aapt badging assertion.
        self.assertIn('= "com.brendigo.qrex 123 1.0.23"', release)
        self.assertNotIn('= "com.brendigo.qrex 122 1.0.23"', release)

    def test_valid_repo_config(self):
        self.assertEqual(self.check(), [])

    def test_outdated_target_api_is_rejected(self):
        issues = self.check(android=self.android.replace("targetSdk = 36", "targetSdk = 35"))
        self.assertTrue(any("targetSdk" in issue for issue in issues), issues)

    def test_published_version_code_is_rejected(self):
        issues = self.check(android=self.android.replace("versionCode = 123", "versionCode = 122"))
        self.assertTrue(any("published QREX build 122" in issue for issue in issues), issues)

    def test_prerelease_version_metadata_is_rejected(self):
        issues = self.check(
            android=self.android.replace('versionName = "1.0.23"', 'versionName = "0.2.4"')
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
