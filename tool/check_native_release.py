#!/usr/bin/env python3
"""Fail closed on Android QREX release metadata and privacy regressions.

Checks committed source only; does not verify upload key, app bundle signatures,
Google Play approval, end-to-end migration or physical-device behavior.
"""
from __future__ import annotations
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent.parent
ANDROID_NS = "http://schemas.android.com/apk/res/android"
TOOLS_NS = "http://schemas.android.com/tools"
APP_ID = "com.brendigo.qrex"
def latest_published_version() -> str | None:
    # The README names only an actually released GitHub version. Candidates
    # can advance versionCode without misrepresenting unpublished binaries.
    text = (ROOT / "README.md").read_text("utf-8")
    match = re.search(r"Latest published release: .+?v(1[.]0[.][0-9]+)", text)
    return match.group(1) if match else None


def field(source: str, name: str) -> str | None:
    match = re.search(rf"(?m)^\s*{re.escape(name)}\s*[:=]\s*['\"]?([\w.]+)", source)
    return match.group(1) if match else None


def check_configuration(android_gradle: str, android_manifest: str) -> list[str]:
    issues: list[str] = []
    for key in ("compileSdk", "targetSdk"):
        actual = field(android_gradle, key)
        if actual is None or not actual.isdecimal() or int(actual) < 36:
            issues.append(f"Android {key} must be API 36 or newer")
    if field(android_gradle, "applicationId") != APP_ID:
        issues.append("Android application ID unexpectedly changed")
    code = field(android_gradle, "versionCode")
    version = field(android_gradle, "versionName")
    published = latest_published_version()
    if published is None:
        issues.append("Missing published QREX version baseline in README")
    elif not code or not code.isdecimal():
        issues.append("Android versionCode must be numeric")
    else:
        published_code = 100 + int(published.split(".")[-1])
        # A published source revision remains valid for documentation-only CI.
        # A new candidate must advance the code and the matching version name.
        if int(code) < published_code or (int(code) == published_code and version != published):
            issues.append(f"Android versionCode must advance beyond published QREX build {published_code}")
    if not code or not code.isdecimal() or int(code) < 100 or version != f"1.0.{int(code) - 100}":
        issues.append("Production candidate versionName must match its Android versionCode")
    try:
        root = ET.fromstring(android_manifest)
        app = root.find("application")
        if app is None:
            issues.append("Android application manifest is missing")
        else:
            for prop in ("allowBackup", "usesCleartextTraffic"):
                if app.get(f"{{{ANDROID_NS}}}{prop}") != "false":
                    issues.append(f"Android {prop} must be disabled")
        # A tools:node="remove" element is a build-time rule, not an app
        # permission request. Actual merged manifests are separately audited
        # by check_merged_manifest.py for transitive SDK permissions.
        permissions = {
            e.get(f"{{{ANDROID_NS}}}name")
            for e in root
            if e.tag in ("uses-permission", "uses-permission-sdk-23")
            and e.get(f"{{{TOOLS_NS}}}node") != "remove"
        }
        if "android.permission.CAMERA" not in permissions:
            issues.append("Camera permission is missing")
        forbidden = {
            "android.permission.INTERNET",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
        }
        for extra in sorted(permissions & forbidden):
            issues.append(f"Unexpected permission without feature review: {extra}")
    except ET.ParseError as exc:
        issues.append(f"Android manifest is invalid XML: {exc}")
    return issues


def main() -> int:
    android = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
    manifest = (ROOT / "platforms/android/app/src/main/AndroidManifest.xml").read_text("utf-8")
    issues = check_configuration(android, manifest)
    if issues:
        print("QREX Android distribution metadata rejected:")
        for issue in issues:
            print(" - " + issue)
        return 1
    print("Android app ID, version, API 36 and privacy permissions verified.")
    print("Actual signing, compatibility, release integrity and store approval still require verification.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
