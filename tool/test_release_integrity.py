#!/usr/bin/env python3
"""Prevent accidental stable releases or unsigned APK/AAB store claims."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class ReleaseIntegrityTests(unittest.TestCase):
    def test_no_legacy_flutter_automatic_release_workflow(self):
        self.assertFalse((ROOT / ".github/workflows/release.yml").exists())

    def test_release_rc_is_provenance_checked_and_classified(self):
        script = (ROOT / "tool/publish_native_rc.sh").read_text("utf-8")
        workflow = (ROOT / ".github/workflows/release-native-preview.yml").read_text("utf-8")
        self.assertIn("gh release create", script)
        self.assertIn("--prerelease", script)
        self.assertIn("make_latest=false", script)
        self.assertIn("prerelease=true", script)
        self.assertIn("check_current_head", script)
        for check in ("native-platforms.yml", "capture-screenshots.yml",
                      "generate-localizations.yml", "mobile.yml"):
            self.assertIn(check, script)
        for text in ("v0.2.3-rc.1", "v0.2.2-rc.1", "v0.2.1-rc.1", "v0.2.0-rc.1", "gh api -X PATCH",
                     "contents: write"):
            self.assertIn(text, workflow)
        self.assertIn("for tag in v0.2.0-rc.1 v0.2.1-rc.1 v0.2.2-rc.1", workflow)
        guardian = (ROOT / ".github/workflows/guard-release-metadata.yml").read_text("utf-8")
        self.assertIn("release:", guardian)
        self.assertIn("published, edited", guardian)
        self.assertIn("prerelease=true", guardian)
        self.assertIn("make_latest=false", guardian)
        self.assertIn("gh api -X PATCH", guardian)
        self.assertIn("test -s dist/android/aab/app-release.aab", script)
        self.assertIn("test -s dist/ios/archive/Products/Applications/QREX.app/QREX", script)

    def test_public_release_docs_do_not_point_to_renamed_repository(self):
        release_notes = (ROOT / "RELEASE_NOTES.md").read_text("utf-8")
        readme = (ROOT / "README.md").read_text("utf-8")
        checklist = (ROOT / "docs/STORE_CHECKLIST.md").read_text("utf-8")
        for public_file in (release_notes, readme):
            self.assertNotIn("github.com/bren-wp/Qivio", public_file)
            self.assertIn("github.com/bren-wp/QREX/releases", public_file)
        self.assertIn("v0.2.3 RC1", release_notes)
        self.assertIn("version **0.2.3** / build number **8**", checklist)
        self.assertIn("marketing version `0.2.3`, build `8`", checklist)

    def test_native_build_version_monotonicity_and_release_notes(self):
        gradle = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
        ios = (ROOT / "platforms/ios/project.yml").read_text("utf-8")
        self.assertIn("versionCode = 8", gradle)
        self.assertIn('versionName = "0.2.3"', gradle)
        self.assertIn('CURRENT_PROJECT_VERSION: "8"', ios)
        self.assertIn('MARKETING_VERSION: "0.2.3"', ios)
        self.assertTrue((ROOT / "docs/releases/v0.2.3-rc.1.md").exists())


if __name__ == "__main__":
    unittest.main()
