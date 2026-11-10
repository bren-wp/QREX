"""Regression gate: README must show one real, published QREX screenshot set.

This is a source-level consistency check, not proof that remote release assets
exist. The GitHub release publisher separately verifies actual assets and hashes.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parent.parent
README = ROOT / "README.md"
GRADLE = ROOT / "platforms/android/app/build.gradle.kts"
EXPECTED_CAPTURE_NAMES = {
    "splash", "create", "scan", "wifi", "result", "history", "saved",
    "more", "language", "export", "about", "privacy"
}


def readme_capture_versions(text: str) -> dict[str, str]:
    captures = re.findall(
        r'releases/download/(v\d+\.\d+\.\d+)/android-([a-z]+)\.png',
        text
    )
    result: dict[str, str] = {}
    for version, name in captures:
        if name in result:
            raise ValueError(f"Duplicate README capture: {name}")
        result[name] = version
    return result


class ReadmeScreenshotIntegrityTest(unittest.TestCase):
    def test_all_twelve_images_use_the_declared_published_release(self):
        readme = README.read_text("utf-8")
        found = readme_capture_versions(readme)
        self.assertEqual(EXPECTED_CAPTURE_NAMES, set(found))
        declared = re.search(
            r'Latest published version: QREX v(\d+\.\d+\.\d+)', readme
        )
        self.assertIsNotNone(declared, "README must identify the last actual release")
        self.assertEqual({"v" + declared.group(1)}, set(found.values()))
        self.assertIn(
            f"https://github.com/bren-wp/QREX/releases/tag/v{declared.group(1)}",
            readme,
        )

    def test_readme_release_is_not_a_future_unbuilt_version(self):
        readme = README.read_text("utf-8")
        gradle = GRADLE.read_text("utf-8")
        declared = re.search(r'Latest published version: QREX v(\d+\.\d+\.\d+)', readme)
        configured = re.search(r'versionName\s*=\s*"(\d+\.\d+\.\d+)"', gradle)
        self.assertIsNotNone(declared)
        self.assertIsNotNone(configured)
        self.assertLessEqual(
            tuple(map(int, declared.group(1).split("."))),
            tuple(map(int, configured.group(1).split("."))),
        )


if __name__ == "__main__":
    unittest.main()
