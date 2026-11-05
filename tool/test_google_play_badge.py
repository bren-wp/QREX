#!/usr/bin/env python3
"""Guard the official clickable Google Play badge and published screenshot links."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
PLAY_URL = "https://play.google.com/store/apps/details?id=com.brendigo.qrex"
BADGE = "https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"


class GooglePlayReadmeBadgeTest(unittest.TestCase):
    def test_google_play_image_is_clickable_and_not_a_false_release_asset(self):
        readme = README.read_text("utf-8")
        markup = re.findall(r'<a\s+[^>]*href="([^"]+)"[^>]*>\s*<img\s+([^>]+)/?>\s*</a>', readme)
        badges = [(url, tag) for url, tag in markup if BADGE in tag]
        self.assertGreaterEqual(len(badges), 1, "A rendered clickable Play badge is required")
        for destination, tag in badges:
            self.assertEqual(PLAY_URL, destination)
            self.assertIn('alt="', tag)
            self.assertIn('height="64"', tag)
        self.assertNotIn("QREX-Android-1.0.29-Play-UNSIGNED-QA.aab](%s)" % PLAY_URL, readme)

    def test_marketing_screens_use_latest_published_genuine_release(self):
        readme = README.read_text("utf-8")
        # Previous source-level test froze the published version at 1.0.33,
        # breaking CI after the verified 1.0.34 release. Resolve the version
        # from the documented release/archive rather than keeping a stale tag.
        published = re.search(
            r"Latest published release: \*\*\[v([0-9]+\.[0-9]+\.[0-9]+)\]",
            readme,
        )
        screenshots = re.search(
            r"Last complete screenshot release: v([0-9]+\.[0-9]+\.[0-9]+)",
            readme,
        )
        self.assertIsNotNone(published)
        self.assertIsNotNone(screenshots)
        latest = published.group(1)
        image_version = screenshots.group(1)
        self.assertLessEqual(
            tuple(map(int, image_version.split("."))),
            tuple(map(int, latest.split("."))),
        )
        archive = (ROOT / "docs/releases/INDEX.md").read_text("utf-8")
        self.assertIn(f"| {latest} | [v{latest}]", archive)
        self.assertIn(f"| {image_version} | [v{image_version}]", archive)
        for screen in ("splash", "scan", "create", "wifi", "result", "history",
                       "saved", "more", "language", "export", "about", "privacy"):
            self.assertIn(
                f"https://github.com/bren-wp/QREX/releases/download/v{image_version}/android-{screen}.png",
                readme,
            )


if __name__ == "__main__":
    unittest.main()
