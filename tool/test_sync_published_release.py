#!/usr/bin/env python3
"""Guard the post-publication README and release index refresh."""
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from sync_published_release import SCREENS, updated_documents


class PublishedDocsSyncTests(unittest.TestCase):
    def test_requires_source_truth_and_existing_screen_provenance(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "platforms/android/app").mkdir(parents=True)
            (root / "docs/releases").mkdir(parents=True)
            (root / "platforms/android/app/build.gradle.kts").write_text(
                'versionName = "1.0.47"\nversionCode = 147\n', "utf-8")
            screenshot_lines = "\n".join(
                "https://github.com/bren-wp/QREX/releases/download/v1.0.46/android-"
                + screen + ".png" for screen in SCREENS
            )
            (root / "README.md").write_text(
                "Latest published release: **[v1.0.46]\n" + screenshot_lines, "utf-8")
            (root / "docs/releases/INDEX.md").write_text(
                "| 1.0.46 | [v1.0.46](url) | APK |\n"
                "Published releases through v1.0.46 were checked against the GitHub release API on 2026-11-12.",
                "utf-8")
            (root / "RELEASE_NOTES.md").write_text("## Published: v1.0.46\n", "utf-8")
            (root / "docs/releases/v1.0.47.md").write_text(
                "**Status:** GitHub publication must pass all checks before this version is marked released.",
                "utf-8")
            updates = updated_documents(root, "1.0.46", "1.0.47", 147, "a" * 40)
            self.assertEqual(4, len(updates))
            self.assertIn("Latest published release: **[v1.0.47]", updates[root / "README.md"])
            self.assertEqual(12, updates[root / "README.md"].count("/releases/download/v1.0.47/android-"))
            self.assertIn("| 1.0.47 |", updates[root / "docs/releases/INDEX.md"])
            self.assertIn("## Published: v1.0.47", updates[root / "RELEASE_NOTES.md"])
            self.assertIn("**Status:** Published", updates[root / "docs/releases/v1.0.47.md"])
            with self.assertRaises(ValueError):
                updated_documents(root, "1.0.46", "1.0.47", 146, "a" * 40)


    def test_partial_writes_and_repeated_recovery_are_idempotent(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "platforms/android/app").mkdir(parents=True)
            (root / "docs/releases").mkdir(parents=True)
            (root / "platforms/android/app/build.gradle.kts").write_text(
                'versionName = "1.0.47"\nversionCode = 147\n', "utf-8"
            )
            links = "\n".join(
                f"https://github.com/bren-wp/QREX/releases/download/v1.0.46/android-{screen}.png"
                for screen in SCREENS
            )
            (root / "README.md").write_text(
                "Latest published release: **[v1.0.46]\n" + links, "utf-8"
            )
            (root / "docs/releases/INDEX.md").write_text(
                "| 1.0.46 | [v1.0.46](url) | APK |\n"
                "Published releases through v1.0.46 were checked against the GitHub release API on 2026-11-12.",
                "utf-8"
            )
            (root / "RELEASE_NOTES.md").write_text("## Published: v1.0.46\n", "utf-8")
            (root / "docs/releases/v1.0.47.md").write_text(
                "**Status:** GitHub publication must pass all checks before this version is marked released.",
                "utf-8"
            )
            sha = "a" * 40
            first = updated_documents(root, "1.0.46", "1.0.47", 147, sha)
            # Crash after writing two documents, before index and release status.
            for name in ("README.md", "RELEASE_NOTES.md"):
                path = root / name
                path.write_text(first[path], "utf-8")
            fixed = updated_documents(root, "1.0.47", "1.0.47", 147, sha)
            for path, content in fixed.items():
                path.write_text(content, "utf-8")
            self.assertEqual(fixed, updated_documents(root, "1.0.47", "1.0.47", 147, sha))
            self.assertEqual(1, fixed[root / "docs/releases/INDEX.md"].count("| 1.0.47 |"))
            self.assertEqual(12, fixed[root / "README.md"].count("/releases/download/v1.0.47/android-"))
            bad = root / "docs/releases/v1.0.47.md"
            bad.write_text(fixed[bad].replace(sha, "b" * 40), "utf-8")
            with self.assertRaises(ValueError):
                updated_documents(root, "1.0.47", "1.0.47", 147, sha)

if __name__ == "__main__":
    unittest.main()
