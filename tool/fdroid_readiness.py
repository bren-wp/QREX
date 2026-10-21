#!/usr/bin/env python3
"""QREX F-Droid source preflight: static hints, NOT dependency/SBOM or build proof."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
GRADLE = ROOT / "platforms/android/app/build.gradle.kts"
SCANNER = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/QREXScanner.kt"
LICENSE = ROOT / "LICENSE"

def main() -> int:
    issues = []
    if not LICENSE.is_file():
        issues.append("Missing LICENSE file")
    if not GRADLE.is_file():
        issues.append("Missing Android app Gradle file")
    else:
        gradle = GRADLE.read_text(encoding="utf-8")
        if "com.google.mlkit:" in gradle:
            issues.append("Non-FOSS ML Kit dependency in Android Gradle configuration")
        if "applicationId = \"com.brendigo.qrex\"" not in gradle:
            issues.append("Confirm Android application ID matches com.brendigo.qrex")
    if SCANNER.is_file():
        scanner = SCANNER.read_text(encoding="utf-8")
        if "com.google.mlkit." in scanner or "BarcodeScanning.getClient" in scanner:
            issues.append("Scanner source still requires ML Kit for camera/gallery")
    else:
        print("NOTICE: Scanner source moved: inspect product flavors and resolved dependencies")
    if issues:
        for issue in issues:
            print("BLOCKER/CHECK:", issue)
        print("Status: NOT ready for official F-Droid. See docs/FDROID_DISTRIBUTION.md.")
        return 1 if "--strict" in sys.argv else 0
    print("Static checks clear; real license/transitive-dependency/build/device audits still required.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
