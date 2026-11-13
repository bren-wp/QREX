#!/usr/bin/env python3
"""Android-only production metadata and minimal-permissions regression tests."""
from pathlib import Path
import unittest
from check_native_release import ROOT, check_configuration, field


class NativeReleaseGateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.android = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
        cls.manifest = (ROOT / "platforms/android/app/src/main/AndroidManifest.xml").read_text("utf-8")

    def check(self, android=None, manifest=None, published_version=None):
        return check_configuration(
            self.android if android is None else android,
            self.manifest if manifest is None else manifest,
            published_version=published_version,
        )

    def test_androidx_fragment_updated_and_r8_optimized_with_agp9(self):
        root_gradle = (ROOT / "platforms/android/build.gradle.kts").read_text("utf-8")
        properties = (ROOT / "platforms/android/gradle.properties").read_text("utf-8")
        self.assertIn('id("com.android.application") version "9.5.0"', root_gradle)
        self.assertNotIn("org.jetbrains.kotlin.android", root_gradle)
        self.assertNotIn("org.jetbrains.kotlin.android", self.android)
        self.assertIn('implementation("androidx.fragment:fragment:1.9.1")', self.android)
        self.assertIn("isMinifyEnabled = true", self.android)
        self.assertIn("isShrinkResources = true", self.android)
        self.assertNotIn("android.r8.optimizedResourceShrinking=false", properties)

    def test_generic_github_publisher_uses_registered_agp9_tasks(self):
        release = (ROOT / ".github/workflows/github-release.yml").read_text("utf-8")
        ci = (ROOT / ".github/workflows/native-platforms.yml").read_text("utf-8")
        self.assertIn(":app:testDebugUnitTest", ci)
        self.assertIn(":app:testDebugUnitTest :app:lintRelease", release)
        self.assertNotIn(":app:testReleaseUnitTest", release)
        self.assertIn(":app:assembleRelease :app:bundleRelease", release)
        self.assertIn("Require all 4 green exact-commit Android checks", release)
        self.assertIn("p03-stability.yml", release)
        self.assertIn("needs.preflight.outputs.version", release)
        p03 = (ROOT / ".github/workflows/p03-stability.yml").read_text("utf-8")
        self.assertIn("  push:\n    branches: [main]", p03)
        self.assertIn("QREX-Android-$RELEASE_VERSION-Play-UNSIGNED-QA.aab", release)

    def test_generic_publisher_validates_dynamic_android_identity(self):
        release = (ROOT / ".github/workflows/github-release.yml").read_text("utf-8")
        self.assertIn("com.brendigo.qrex $RELEASE_CODE $RELEASE_VERSION", release)
        self.assertNotIn('com.brendigo.qrex 146 1.0.46', release)
        self.assertIn("versionCode =", release)
        self.assertIn("versionName =", release)

    def test_valid_repo_config(self):
        self.assertEqual(self.check(), [])

    def test_outdated_target_api_is_rejected(self):
        issues = self.check(android=self.android.replace("targetSdk = 36", "targetSdk = 35"))
        self.assertTrue(any("targetSdk" in issue for issue in issues), issues)

    def test_published_version_code_is_rejected(self):
        # Assert from fixed test inputs, not the README modified after release.
        published_code = int(field(self.android, "versionCode"))
        published_version = f"1.0.{published_code - 100}"
        old_source = self.android.replace(
            f"versionCode = {published_code}", f"versionCode = {published_code - 1}"
        )
        issues = self.check(android=old_source, published_version=published_version)
        self.assertTrue(
            any(f"published QREX build {published_code}" in issue for issue in issues),
            issues,
        )

    def test_published_and_candidate_sources_are_separately_checked(self):
        code = int(field(self.android, "versionCode"))
        self.assertEqual([], self.check(published_version=f"1.0.{code - 101}"))
        self.assertEqual([], self.check(published_version=f"1.0.{code - 100}"))
        issues = self.check(published_version=f"1.0.{code - 99}")
        self.assertTrue(
            any(f"published QREX build {code + 1}" in issue for issue in issues),
            issues,
        )

    def test_prerelease_version_metadata_is_rejected(self):
        code = int(field(self.android, "versionCode"))
        issues = self.check(
            android=self.android.replace(
                f'versionName = "1.0.{code - 100}"', 'versionName = "0.2.4"'
            )
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
