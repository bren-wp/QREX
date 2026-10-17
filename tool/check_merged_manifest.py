#!/usr/bin/env python3
"""Verify the actual merged Android debug/release manifests, not only source XML.

Run after Android Gradle has built the variants. This is a source-level
privacy gate; developers must also inspect Play SDK Index and Data safety.
"""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
INTERMEDIATES = ROOT / "platforms/android/app/build/intermediates"
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
FORBIDDEN = {
    "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE", "android.permission.READ_MEDIA_IMAGES",
    "com.google.android.gms.permission.AD_ID",
}


def validate(text: str) -> list[str]:
    issues = []
    root = ET.fromstring(text)
    permissions = {
        node.get(ANDROID_NS + "name") for node in root
        if node.tag in ("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")
    }
    for permission in sorted(permissions & FORBIDDEN):
        issues.append(f"Undeclared/broad permission in merged manifest: {permission}")
    app = root.find("application")
    if app is None:
        issues.append("Missing application element")
    else:
        if app.get(ANDROID_NS + "allowBackup") != "false":
            issues.append("Backups must remain disabled for locally protected QR history")
        if app.get(ANDROID_NS + "usesCleartextTraffic") != "false":
            issues.append("Cleartext traffic must remain disabled")
    return issues


def main() -> int:
    if not INTERMEDIATES.exists():
        print("Android merged manifests are not built", file=sys.stderr)
        return 1
    candidates = [
        p for p in INTERMEDIATES.rglob("AndroidManifest.xml")
        if any(part.startswith("merged_manifest") for part in p.parts)
        and any(part in ("debug", "release") for part in p.parts)
    ]
    candidates = sorted(set(candidates))
    variants = {
        "debug" if "debug" in path.parts else "release"
        for path in candidates
    }
    if variants != {"debug", "release"}:
        print(f"Missing merged manifest variants: {variants}", file=sys.stderr)
        return 1
    issues = []
    for path in candidates:
        try:
            issues.extend(f"{path}: {issue}" for issue in validate(path.read_text("utf-8")))
        except (ValueError, ET.ParseError) as error:
            issues.append(f"{path}: invalid XML: {error}")
    if issues:
        print("\n".join(issues), file=sys.stderr)
        return 1
    print(f"Verified {len(candidates)} actual debug/release Android merged manifests.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
