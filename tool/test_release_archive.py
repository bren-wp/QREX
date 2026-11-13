#!/usr/bin/env python3
"""Prevent previously published GitHub release publishers from respawning."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
ACTIVE = ROOT / ".github/workflows"
ARCHIVE = ROOT / "docs/releases/workflows"


class ArchivedPublisherTests(unittest.TestCase):
    def test_all_published_publishers_are_inert_and_preserved(self):
        gradle = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
        version = re.search(r'(?m)^\s*versionCode\s*=\s*(\d+)\s*$', gradle)
        self.assertIsNotNone(version)
        patch = int(version.group(1)) - 100
        self.assertGreaterEqual(patch, 44)
        self.assertIn(f'versionName = "1.0.{patch}"', gradle)
        self.assertEqual([], sorted(p.name for p in ACTIVE.glob("github-release-1.0.*.yml")))
        self.assertTrue((ACTIVE / "github-release.yml").is_file())
        # Per-version publishers were retired with the generic publisher at v1.0.47.
        # Requiring an archived v1.0.47 recipe when preparing v1.0.48 is wrong.
        expected = {f"github-release-1.0.{n}.yml" for n in range(1, min(patch, 47))}
        self.assertEqual(expected, {p.name for p in ARCHIVE.glob("github-release-1.0.*.yml")})
        for name in sorted(expected):
            with self.subTest(name=name):
                recipe = (ARCHIVE / name).read_text("utf-8")
                self.assertIn("gh release create v1.0.", recipe)
                self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", recipe)

    def test_generic_publisher_has_no_per_version_archive_after_46(self):
        """Future candidates must not require a removed v1.0.47 YAML recipe."""
        archive = sorted(p.name for p in ARCHIVE.glob("github-release-1.0.*.yml"))
        expected = sorted(f"github-release-1.0.{n}.yml" for n in range(1, 47))
        self.assertEqual(expected, archive)
        self.assertTrue((ACTIVE / "github-release.yml").is_file())
        for patch in (47, 48, 49, 100):
            with self.subTest(candidate=patch):
                self.assertEqual(expected, sorted(
                    f"github-release-1.0.{n}.yml"
                    for n in range(1, min(patch, 47))
                ))

    def test_current_published_release_metadata_is_not_a_candidate(self):
        # Read the current published version from the README; it changes
        # after a successful release. Hardcoding the prior version made
        # a documentation-only update fail the entire Android CI workflow.
        readme = (ROOT / "README.md").read_text("utf-8")
        archive = (ROOT / "docs/releases/INDEX.md").read_text("utf-8")
        notes = (ROOT / "RELEASE_NOTES.md").read_text("utf-8")
        latest = re.search(
            r"Latest published release: \*\*\[v(\d+\.\d+\.\d+)\]",
            readme,
        )
        self.assertIsNotNone(latest, "README must name the published release")
        version = latest.group(1)
        self.assertIn(f"| {version} | [v{version}]", archive)
        self.assertIn(f"## Published: v{version} · Android versionCode ", notes)
        self.assertTrue((ROOT / f"docs/releases/v{version}.md").is_file())
        self.assertIn(f"releases/tag/v{version}", readme)
        self.assertNotIn("Next QA candidate —", readme)

        # All twelve verified Android captures must point at the *same* release.
        screens = re.findall(
            r"/releases/download/v([0-9.]+)/android-([a-z]+)\.png", readme
        )
        baseline = re.search(
            r"Last complete screenshot release: v([0-9]+\.[0-9]+\.[0-9]+)", readme
        )
        self.assertIsNotNone(baseline, "README must ground screenshot asset version")
        screenshot_version = baseline.group(1)
        expected = {
            (screenshot_version, name) for name in (
                "splash", "scan", "create", "wifi", "result",
                "history", "saved", "more", "language", "export",
                "about", "privacy",
            )
        }
        self.assertEqual(expected, set(screens))
        self.assertEqual(len(screens), len(expected))
        self.assertIn(
            f"| {screenshot_version} | [v{screenshot_version}]", archive
        )
        self.assertLessEqual(
            tuple(int(n) for n in screenshot_version.split(".")),
            tuple(int(n) for n in version.split(".")),
        )
        if screenshot_version != version:
            # An incomplete published release must NEVER masquerade as a
            # verified source of screenshots; explicitly document the gap.
            self.assertIn(f"v{version} GitHub release currently", readme)
            self.assertIn("not present in the live release asset list", readme)



if __name__ == "__main__":
    unittest.main()
