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
        self.assertEqual(["github-release-1.0.24.yml"],
                         sorted(p.name for p in ACTIVE.glob("github-release-1.0.*.yml")))
        expected = {f"github-release-1.0.{n}.yml" for n in range(1, 24)}
        self.assertEqual(expected, {p.name for p in ARCHIVE.glob("github-release-1.0.*.yml")})
        for name in sorted(expected):
            with self.subTest(name=name):
                recipe = (ARCHIVE / name).read_text("utf-8")
                self.assertIn("gh release create v1.0.", recipe)
                self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", recipe)

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

        # All ten real Android captures must point at the *same* release.
        screens = re.findall(
            r"/releases/download/v([0-9.]+)/android-([a-z]+)\.png", readme
        )
        expected = {
            (version, name) for name in (
                "splash", "scan", "create", "wifi", "result",
                "history", "saved", "more", "language", "export",
            )
        }
        self.assertEqual(expected, set(screens))
        self.assertEqual(len(screens), len(expected))



if __name__ == "__main__":
    unittest.main()
