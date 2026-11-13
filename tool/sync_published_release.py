#!/usr/bin/env python3
"""Synchronize public QREX release docs only after a verified GitHub publication.

The release publisher checks all binaries, checksums and screenshots first.
This script never uploads or alters the release and never edits Android data.
"""
from __future__ import annotations

from datetime import datetime, timezone
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parent.parent
SCREENS = (
    "splash", "create", "scan", "wifi", "result", "history",
    "saved", "more", "language", "export", "about", "privacy",
)


def updated_documents(root: Path, previous: str, current: str, code: int, sha: str) -> dict[Path, str]:
    if not re.fullmatch(r"1\.0\.\d+", previous) or not re.fullmatch(r"1\.0\.\d+", current):
        raise ValueError("Versions must be stable QREX Android releases")
    if int(current.split(".")[-1]) < int(previous.split(".")[-1]):
        raise ValueError("The release version must not go backwards")
    if code != 100 + int(current.split(".")[-1]) or not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("VersionCode or exact commit SHA mismatch")
    gradle = (root / "platforms/android/app/build.gradle.kts").read_text("utf-8")
    if f'versionName = "{current}"' not in gradle or f"versionCode = {code}" not in gradle:
        raise ValueError("Android Gradle metadata does not match the published release")

    readme_path = root / "README.md"
    readme = readme_path.read_text("utf-8")
    previous_marker = f"Latest published release: **[v{previous}]"
    current_marker = f"Latest published release: **[v{current}]"
    if previous != current and previous_marker in readme:
        for screen in SCREENS:
            if readme.count(f"/releases/download/v{previous}/android-{screen}.png") != 1:
                raise ValueError("Missing or duplicated previous real screenshot reference")
        old_code = 100 + int(previous.split(".")[-1])
        readme = readme.replace(previous, current)
        readme = readme.replace(f"versionCode {old_code}", f"versionCode {code}")
        readme = readme.replace(f"Android build {old_code}", f"Android build {code}")
        readme = readme.replace(f"build {old_code}", f"build {code}")
    elif current_marker not in readme:
        raise ValueError("README does not identify the previous or current publication")
    for screen in SCREENS:
        if readme.count(f"/releases/download/v{current}/android-{screen}.png") != 1:
            raise ValueError("README must link exactly twelve real release screenshots")

    index_path = root / "docs/releases/INDEX.md"
    index = index_path.read_text("utf-8")
    current_row = (f"| {current} | [v{current}](https://github.com/bren-wp/QREX/releases/tag/v{current}) "
                   "| APK · AAB · Play-unsigned QA AAB · 12 genuine Android screenshots · SHA256SUMS |")
    rows = [line for line in index.splitlines() if re.match(r"^\| 1\.0\.\d+ \| ", line)]
    found = [row for row in rows if row.startswith(f"| {current} | ")]
    if len(found) > 1 or (found and found[0] != current_row):
        raise ValueError("Duplicated or conflicting published release index entry")
    if not found:
        earlier = [row for row in rows
                   if int(row.split(" |", 1)[0].removeprefix("| ").split(".")[-1])
                   < int(current.split(".")[-1])]
        if not earlier:
            raise ValueError("Release archive has no earlier release entry")
        anchor = max(
            earlier, key=lambda row: int(row.split(" |", 1)[0].removeprefix("| ").split(".")[-1])
        )
        index = index.replace(anchor, anchor + "\n" + current_row, 1)
    if not re.search(rf"Published releases through v{re.escape(current)} were checked against the GitHub release API on \d{{4}}-\d{{2}}-\d{{2}}\.", index):
        index, substitutions = re.subn(
            r"Published releases through v[0-9.]+ were checked against the GitHub release API on [0-9-]+\.",
            f"Published releases through v{current} were checked against the GitHub release API on "
            f"{datetime.now(timezone.utc).date().isoformat()}.",
            index, count=1,
        )
        if substitutions != 1:
            raise ValueError("Release archive is missing the verified-publication statement")

    notes_path = root / "RELEASE_NOTES.md"
    notes = notes_path.read_text("utf-8")
    published = (
        f"## Published: v{current} · Android versionCode {code}\n\n"
        f"Published from verified main commit {sha}. The source revision passed native "
        "Kotlin tests, R8/Lint, locale checks, twelve real Android screenshots and "
        "the API 26/31/34/35/36 emulator lifecycle matrix. The downloadable APK/AAB "
        "use a disposable GitHub-only signing certificate; neither is an authorized "
        "Google Play upload bundle. Assets have SHA-256 checksums.\n\n"
        f"[Download QREX v{current}](https://github.com/bren-wp/QREX/releases/tag/v{current}) · "
        f"[Release details](docs/releases/v{current}.md).\n\n"
    )
    if notes.startswith(f"## Published: v{current} "):
        first_section = notes.split("## Published:", 2)[1]
        if sha not in first_section:
            raise ValueError("Already-published release notes point to another SHA")
    else:
        notes = published + notes

    version_path = root / f"docs/releases/v{current}.md"
    version_text = version_path.read_text("utf-8")
    marker = "**Status:** GitHub publication must pass all checks before this version is marked released."
    status = (
        f"**Status:** Published from verified commit {sha}. "
        f"[GitHub Release v{current}](https://github.com/bren-wp/QREX/releases/tag/v{current})."
    )
    if version_text.count(marker) == 1:
        version_text = version_text.replace(marker, status)
    elif version_text.count(status) != 1:
        raise ValueError("Published release status does not match the exact SHA")

    return {
        readme_path: readme,
        index_path: index,
        notes_path: notes,
        version_path: version_text,
    }

def main(argv: list[str]) -> int:
    if len(argv) != 5:
        raise ValueError("Usage: sync_published_release.py PREVIOUS CURRENT CODE HEAD_SHA")
    outputs = updated_documents(ROOT, argv[1], argv[2], int(argv[3]), argv[4])
    for path, content in outputs.items():
        path.write_text(content, encoding="utf-8")
    print(f"QREX v{argv[2]}: synchronized {len(outputs)} source documents after verified publication")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
