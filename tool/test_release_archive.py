#!/usr/bin/env python3
"""Prevent previously published GitHub release publishers from respawning."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ACTIVE = ROOT / ".github/workflows"
ARCHIVE = ROOT / "docs/releases/workflows"


class ArchivedPublisherTests(unittest.TestCase):
    def test_all_published_publishers_are_inert_and_preserved(self):
        self.assertEqual(["github-release-1.0.17.yml"],
                         sorted(p.name for p in ACTIVE.glob("github-release-1.0.*.yml")))
        expected = {f"github-release-1.0.{n}.yml" for n in range(1, 17)}
        self.assertEqual(expected, {p.name for p in ARCHIVE.glob("github-release-1.0.*.yml")})
        for name in sorted(expected):
            with self.subTest(name=name):
                recipe = (ARCHIVE / name).read_text("utf-8")
                self.assertIn("gh release create v1.0.", recipe)
                self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", recipe)

    def test_current_published_release_metadata_is_not_a_candidate(self):
        readme = (ROOT / "README.md").read_text("utf-8")
        self.assertIn("Latest published release: **[v1.0.17]", readme)
        self.assertIn("/releases/download/v1.0.17/android-create.png", readme)
        self.assertNotIn("Next QA candidate — QREX v1.0.17", readme)
        self.assertIn("| 1.0.17 |", (ROOT / "docs/releases/INDEX.md").read_text("utf-8"))


if __name__ == "__main__":
    unittest.main()
