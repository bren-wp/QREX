#!/usr/bin/env python3
"""Fail closed on Android QREX 1.0.19 release metadata and privacy regressions.

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
LAST_PUBLISHED_NATIVE_BUILD_CODE = 118
PRODUCTION_VERSION = "1.0.19"
PRODUCTION_BUILD_CODE = 119


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
    if not code or not code.isdecimal() or int(code) <= LAST_PUBLISHED_NATIVE_BUILD_CODE:
        issues.append(
            f"Android versionCode must advance beyond published QREX build "
            f"{LAST_PUBLISHED_NATIVE_BUILD_CODE}")
    if version != PRODUCTION_VERSION or code != str(PRODUCTION_BUILD_CODE):
        issues.append("Production candidate must be QREX 1.0.19 / build 119")
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
