#!/usr/bin/env python3
"""Regression gates for GitHub 1.0.2/1.0.3 and authorized Google Play 1.0.1 paths.

No public GitHub release signed with a disposable key is a Play upload artifact.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOWS = ROOT / ".github/workflows"


class ReleaseIntegrityTests(unittest.TestCase):
    def test_old_1_0_0_publisher_and_dangerous_cleanup_are_retired(self):
        self.assertFalse((WORKFLOWS / "github-release-1.0.0.yml").exists())
        self.assertFalse((WORKFLOWS / "release.yml").exists())
        self.assertFalse((WORKFLOWS / "release-native-preview.yml").exists())
        self.assertFalse((ROOT / "tool/retire_legacy_releases.sh").exists())
        self.assertFalse((ROOT / "tool/test_retire_legacy_releases.py").exists())
        self.assertFalse((WORKFLOWS / "retire-legacy-releases.yml").exists())

    def test_google_play_bundle_requires_real_authorized_key(self):
        play = (WORKFLOWS / "production-release.yml").read_text("utf-8")
        gate = (ROOT / "tool/verify_production_gate.sh").read_text("utf-8")
        signer = (ROOT / "tool/sign_android_production.sh").read_text("utf-8")
        self.assertIn("workflow_dispatch:", play)
        self.assertNotIn("  push:", play)
        self.assertNotIn("  pull_request:", play)
        self.assertIn("environment: production", play)
        self.assertIn("device_qa_confirmed:", play)
        self.assertIn("store_review_confirmed:", play)
        self.assertIn("PLAY_UPLOAD_1_0_1", gate)
        self.assertIn("refs/heads/main", gate)
        self.assertIn("commits/main", gate)
        self.assertIn("completed:success", gate)
        self.assertIn("needs: verify", play)
        self.assertIn("QREX-Android-1.0.1.aab", play)
        self.assertIn("QREX_UPLOAD_KEYSTORE_BASE64", signer)
        self.assertIn("QREX_UPLOAD_CERT_SHA256", signer)
        self.assertIn("keytool -printcert -jarfile", signer)
        self.assertIn("jarsigner -verify -strict", signer)
        self.assertNotIn("gh release create", play)
        self.assertNotIn("retire_legacy_releases.sh", play)
        self.assertNotIn("keytool -genkeypair", signer)

    def test_public_github_release_has_no_play_key_and_is_labeled(self):
        public = (WORKFLOWS / "github-release-1.0.1.yml").read_text("utf-8")
        self.assertIn("workflow_run:", public)
        self.assertIn("types: [completed]", public)
        self.assertIn("branches: [main]", public)
        self.assertIn("github.event.workflow_run.conclusion == 'success'", public)
        self.assertIn("keytool -genkeypair", public)
        self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", public)
        self.assertIn("gh release create v1.0.1", public)
        self.assertIn("QREX-Android-1.0.1.apk", public)
        self.assertIn("QREX-Android-1.0.1.aab", public)
        self.assertIn("SHA256SUMS.txt", public)

    def test_new_github_1_0_2_release_is_separate_and_signature_labeled(self):
        published = (WORKFLOWS / "github-release-1.0.2.yml").read_text("utf-8")
        notes = (ROOT / "docs/releases/v1.0.2.md").read_text("utf-8")
        self.assertIn("workflow_run:", published)
        self.assertIn("types: [completed]", published)
        self.assertIn("branches: [main]", published)
        self.assertIn("github.event.workflow_run.head_sha", published)
        self.assertIn("keytool -genkeypair", published)
        self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", published)
        self.assertIn("com.brendigo.qrex 102 1.0.2", published)
        self.assertIn("gh release create v1.0.2", published)
        # The existing tag/assets are immutable: future main commits must not
        # start a publication job that can only fail as already-released.
        self.assertIn(
            "github.event.workflow_run.head_sha == "
            "'b15a95a18820cbe8588b4d098a0719a3a5ce7b4a'",
            published,
        )
        self.assertIn("QREX-Android-1.0.2.apk", published)
        self.assertIn("QREX-Android-1.0.2.aab", published)
        self.assertIn("SHA256SUMS.txt", published)
        self.assertIn("cannot be installed in place", notes)

    def test_github_1_0_3_publisher_remains_separate_and_immutable(self):
        published = (WORKFLOWS / "github-release-1.0.3.yml").read_text("utf-8")
        notes = (ROOT / "docs/releases/v1.0.3.md").read_text("utf-8")
        self.assertIn("workflow_run:", published)
        self.assertIn("branches: [main]", published)
        self.assertIn("github.event.workflow_run.conclusion == 'success'", published)
        self.assertIn("github.event.workflow_run.head_sha", published)
        self.assertIn("commits/main", published)
        for workflow in ("native-platforms.yml", "capture-screenshots.yml",
                         "generate-localizations.yml"):
            self.assertIn(workflow, published)
        self.assertIn("keytool -genkeypair", published)
        self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", published)
        self.assertIn("com.brendigo.qrex 103 1.0.3", published)
        self.assertIn("gh release create v1.0.3", published)
        self.assertIn("QREX-Android-1.0.3.apk", published)
        self.assertIn("QREX-Android-1.0.3.aab", published)
        self.assertIn("SHA256SUMS.txt", published)
        self.assertIn("cannot be installed in place", notes)
        self.assertIn("Google Play", notes)
        self.assertNotIn("b15a95a18820cbe8588b4d098a0719a3a5ce7b4a", published)

    def test_github_1_0_4_release_requires_real_emulator_images_and_no_play_key(self):
        published = (WORKFLOWS / "github-release-1.0.4.yml").read_text("utf-8")
        screenshots = (WORKFLOWS / "capture-screenshots.yml").read_text("utf-8")
        main = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt").read_text("utf-8")
        for workflow in ("native-platforms.yml", "capture-screenshots.yml",
                         "generate-localizations.yml"):
            self.assertIn(workflow, published)
        self.assertIn('com.brendigo.qrex 104 1.0.4', published)
        self.assertIn("gh release create v1.0.4", published)
        self.assertIn("gh run download", published)
        self.assertIn("tool/verify_screenshots.py", published)
        self.assertIn("android-splash.png", published)
        self.assertIn("android-scan.png", published)
        self.assertIn("android-history.png", published)
        self.assertIn("android-more.png", published)
        self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", published)
        self.assertIn("android-*.png", screenshots)
        self.assertIn("ApplicationInfo.FLAG_DEBUGGABLE", main)
        self.assertIn("screenshotMode", main)
        self.assertIn("screenshotItems", main)
        self.assertIn("No validated QREX", (ROOT / "tool/capture_android_screen.sh").read_text("utf-8"))

    def test_github_1_0_5_dark_ui_release_is_signed_separately_from_play(self):
        published = (WORKFLOWS / "github-release-1.0.5.yml").read_text("utf-8")
        notes = (ROOT / "docs/releases/v1.0.5.md").read_text("utf-8")
        self.assertIn("branches: [main]", published)
        self.assertIn("gh release create v1.0.5", published)
        self.assertIn("com.brendigo.qrex 105 1.0.5", published)
        self.assertIn("github.event.workflow_run.head_sha", published)
        self.assertIn("keytool -genkeypair", published)
        self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", published)
        for artifact in (
            "QREX-Android-1.0.5.apk",
            "QREX-Android-1.0.5.aab",
            "QREX-Android-1.0.5-Play-UNSIGNED-QA.aab",
            "QREX-Android-1.0.5-FDroid-UNSIGNED-QA.apk",
            "SHA256SUMS.txt",
            "android-create.png",
            "android-scan.png",
            "android-more.png",
        ):
            self.assertIn(artifact, published)
        self.assertIn("F-Droid", notes)
        self.assertIn("cannot update in place", notes)
        self.assertIn("v1.0.5/android-create.png",
                      (ROOT / "README.md").read_text("utf-8"))

    def test_readme_only_updates_run_all_exact_commit_ci(self):
        for name in ("native-platforms.yml", "capture-screenshots.yml",
                     "generate-localizations.yml"):
            workflow = (WORKFLOWS / name).read_text("utf-8")
            with self.subTest(workflow=name):
                self.assertIn("  pull_request:", workflow)
                self.assertIn("  push:", workflow)
                self.assertGreaterEqual(workflow.count('- "README.md"'), 2)
                self.assertGreaterEqual(workflow.count('- "docs/**"'), 2)

    def test_android_only_and_correct_play_version(self):
        for path in ("platforms/ios", "lib", "test", "pubspec.yaml"):
            self.assertFalse((ROOT / path).exists())
        gradle = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
        readme = (ROOT / "README.md").read_text("utf-8")
        notes = (ROOT / "RELEASE_NOTES.md").read_text("utf-8")
        self.assertIn("versionCode = 105", gradle)
        self.assertIn('versionName = "1.0.5"', gradle)
        self.assertIn("1.0.1", readme)
        self.assertIn("1.0.1", notes)
        self.assertTrue((ROOT / "docs/releases/v1.0.0.md").exists())
        self.assertTrue((ROOT / "docs/releases/v1.0.1.md").exists())
        self.assertTrue((ROOT / "docs/releases/v1.0.2.md").exists())
        self.assertTrue((ROOT / "docs/releases/v1.0.3.md").exists())
        self.assertTrue((ROOT / "docs/releases/v1.0.4.md").exists())
        self.assertTrue((ROOT / "docs/releases/v1.0.5.md").exists())
        self.assertTrue((ROOT / "docs/PLAY_STORE_SUBMISSION.md").exists())


if __name__ == "__main__":
    unittest.main()
