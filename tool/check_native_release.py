#!/usr/bin/env python3
"""Fail closed on native QREX release metadata and privacy contract regressions.

This is a static release-readiness gate. It does NOT sign builds, validate
provisioning profiles, substitute for physical device testing or claim that a
store has approved the app.
"""
from __future__ import annotations

import pathlib
import plistlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
ANDROID_NS = "http://schemas.android.com/apk/res/android"
APP_ID = "com.brendigo.qrex"
# Last published native candidate is v0.2.4-rc.1 / build 9.
LAST_PUBLISHED_NATIVE_BUILD_CODE = 9
PRODUCTION_VERSION = "1.0.0"
PRODUCTION_BUILD_CODE = 100


def field(source: str, name: str) -> str | None:
    match = re.search(rf"(?m)^\s*{re.escape(name)}\s*[:=]\s*['\"]?([\w.]+)", source)
    return match.group(1) if match else None


def check_configuration(
    android_gradle: str,
    ios_project: str,
    android_manifest: str,
    ios_privacy: bytes,
    has_shared_entitlements: bool,
) -> list[str]:
    issues: list[str] = []
    for key in ("compileSdk", "targetSdk"):
        actual = field(android_gradle, key)
        if actual is None or not actual.isdecimal() or int(actual) < 36:
            issues.append(f"Android {key} must be API 36 or newer")
    if field(android_gradle, "applicationId") != APP_ID:
        issues.append("Android application ID unexpectedly changed")
    if field(ios_project, "PRODUCT_BUNDLE_IDENTIFIER") != APP_ID:
        issues.append("iOS bundle ID unexpectedly changed")
    code = field(android_gradle, "versionCode")
    version = field(android_gradle, "versionName")
    if not code or not code.isdecimal() or int(code) <= LAST_PUBLISHED_NATIVE_BUILD_CODE:
        issues.append(f"Android versionCode must advance beyond published native RC build {LAST_PUBLISHED_NATIVE_BUILD_CODE}")
    if version != PRODUCTION_VERSION or code != str(PRODUCTION_BUILD_CODE):
        issues.append("Production candidate must be QREX 1.0.0 / build 100")
    if not version or version != field(ios_project, "MARKETING_VERSION"):
        issues.append("Android/iOS marketing versions differ")
    if code != field(ios_project, "CURRENT_PROJECT_VERSION"):
        issues.append("Android versionCode/iOS build numbers differ")
    if re.search(r"(?m)^\s*CODE_SIGNING_ALLOWED\s*:\s*['\"]?NO", ios_project):
        issues.append("iOS Xcode project must allow real device signing by default")
    if has_shared_entitlements or "CODE_SIGN_ENTITLEMENTS:" in ios_project:
        issues.append("Unused/invalid Keychain-sharing entitlements are not permitted")
    try:
        root = ET.fromstring(android_manifest)
        app = root.find("application")
        if app is None:
            issues.append("Android application manifest is missing")
        else:
            for prop in ("allowBackup", "usesCleartextTraffic"):
                if app.get(f"{{{ANDROID_NS}}}{prop}") != "false":
                    issues.append(f"Android {prop} must be disabled")
        permissions = {e.get(f"{{{ANDROID_NS}}}name") for e in root.findall("uses-permission")}
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
            issues.append(f"Unexpected permission without approved feature review: {extra}")
    except ET.ParseError as exc:
        issues.append(f"Android manifest is invalid XML: {exc}")
    try:
        privacy = plistlib.loads(ios_privacy)
        if privacy.get("NSPrivacyTracking") is not False:
            issues.append("iOS app privacy manifest must disable tracking")
        if privacy.get("NSPrivacyTrackingDomains") != []:
            issues.append("iOS privacy manifest must not list tracking domains")
        if not isinstance(privacy.get("NSPrivacyCollectedDataTypes"), list):
            issues.append("iOS collected-data declaration is missing")
        accessed = privacy.get("NSPrivacyAccessedAPITypes")
        if not isinstance(accessed, list) or not any(
            row.get("NSPrivacyAccessedAPIType")
            == "NSPrivacyAccessedAPICategoryUserDefaults"
            and "CA92.1" in row.get("NSPrivacyAccessedAPITypeReasons", [])
            for row in accessed if isinstance(row, dict)
        ):
            issues.append("iOS UserDefaults required-reason API declaration is missing")
    except (ValueError, TypeError, ET.ParseError) as exc:
        issues.append(f"iOS privacy manifest is invalid: {exc}")
    return issues


def main() -> int:
    android = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
    ios = (ROOT / "platforms/ios/project.yml").read_text("utf-8")
    manifest = (ROOT / "platforms/android/app/src/main/AndroidManifest.xml").read_text("utf-8")
    privacy = (ROOT / "platforms/ios/QREX/PrivacyInfo.xcprivacy").read_bytes()
    shared = (ROOT / "platforms/ios/QREX/QREX.entitlements").exists()
    issues = check_configuration(android, ios, manifest, privacy, shared)
    if issues:
        print("Native QREX distribution metadata rejected:")
        for issue in issues:
            print(" - " + issue)
        return 1
    print("Native identifiers, version/build parity, API 36, app privacy and iOS signing configuration verified.")
    print("Distribution signatures, SDK transitive declarations and store acceptance remain unverified.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
