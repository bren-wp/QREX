#!/usr/bin/env python3
"""QREX offline source security and dead-code audit gate.

This is a deterministic source gate, not a substitute for binary SCA, manual
penetration testing, OEM testing, or R8's actual reachable-code analysis.
No QR payloads, credential contents or logcat data are printed.
"""
from __future__ import annotations
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
KOTLIN = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"
MANIFEST = ROOT / "platforms/android/app/src/main/AndroidManifest.xml"
APP = ROOT / "platforms/android/app/build.gradle.kts"
GRADLE = ROOT / "platforms/android/build.gradle.kts"
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
TOOLS_NS = "{http://schemas.android.com/tools}"
FORBIDDEN = {
    "android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.READ_MEDIA_IMAGES",
    "com.google.android.gms.permission.AD_ID",
}
WEAK_CIPHERS = (
    'Cipher.getInstance("AES/ECB', 'Cipher.getInstance("AES/CBC',
    'Cipher.getInstance("DES', 'Cipher.getInstance("RC4',
)


def kotlin_sources(root: Path = ROOT) -> dict[str, str]:
    directory = root / "platforms/android/app/src/main/java"
    return {
        str(path.relative_to(root)): path.read_text("utf-8")
        for path in sorted(directory.rglob("*.kt"))
    }


def security_findings(root: Path = ROOT) -> list[str]:
    """Return policy failures, no secret values or QR payloads."""
    issues: list[str] = []
    sources = kotlin_sources(root)
    combined = "\n".join(sources.values())
    gradle = (root / "platforms/android/app/build.gradle.kts").read_text("utf-8")
    plugin = (root / "platforms/android/build.gradle.kts").read_text("utf-8")
    try:
        manifest = ET.parse(root / "platforms/android/app/src/main/AndroidManifest.xml").getroot()
    except (ET.ParseError, OSError):
        return ["Android manifest is unreadable or invalid"]
    permissions = {
        node.get(ANDROID_NS + "name")
        for node in manifest
        if node.tag in ("uses-permission", "uses-permission-sdk-23")
        and node.get(TOOLS_NS + "node") != "remove"
    }
    if "android.permission.CAMERA" not in permissions:
        issues.append("Missing expected Camera permission")
    if permissions & FORBIDDEN:
        issues.append("Forbidden network/location/advertising/media permission in source manifest")
    application = manifest.find("application")
    if application is None:
        issues.append("Application element missing")
    else:
        if application.get(ANDROID_NS + "allowBackup") != "false":
            issues.append("Android backup must be disabled")
        if application.get(ANDROID_NS + "usesCleartextTraffic") != "false":
            issues.append("Android cleartext traffic must be disabled")
        for component in application:
            if component.tag not in {"activity", "service", "receiver", "provider"}:
                continue
            if component.get(ANDROID_NS + "exported") == "true":
                if component.tag != "activity" or component.get(ANDROID_NS + "name") != ".MainActivity":
                    issues.append("Unexpected exported Android component")
    if "compileSdk = 37" not in gradle:
        issues.append("Compile SDK 37 is required for updated stable Compose libraries")
    if "targetSdk = 36" not in gradle or "minSdk = 26" not in gradle:
        issues.append("Review changes to the tested targetSdk/minSdk security baseline")
    if "isMinifyEnabled = true" not in gradle or "isShrinkResources = true" not in gradle:
        issues.append("Release shrinking or dead-resource elimination disabled")
    for candidate in re.findall(r'(?:implementation|androidTestImplementation|testImplementation)\("([^"]+)"\)', gradle):
        if candidate.endswith(":+") or re.search(r":\d+[\w.]*-(alpha|beta|rc)\d*", candidate, re.I):
            issues.append("Dynamic or prerelease Gradle runtime dependency")
    if "mavenLocal()" in plugin or "flatDir" in plugin:
        issues.append("Untrusted local or flat-directory plugin repository")
    for cipher in WEAK_CIPHERS:
        if cipher in combined:
            issues.append("Weak encryption primitive detected")
    secret = sources.get(
        "platforms/android/app/src/main/java/com/brendigo/qrex/QREXSecretCipher.kt", ""
    )
    for required in (
        "AndroidKeyStore", "AES/GCM/NoPadding", "GCMParameterSpec(128, iv)",
        "MAX_PLAINTEXT_BYTES", "MAX_ENCODED_CHARS",
        "encoded.length > MAX_ENCODED_CHARS",
    ):
        if required not in secret:
            issues.append("Keystore/GCM length/identity guard missing")
            break
    core = sources.get(
        "platforms/android/app/src/main/java/com/brendigo/qrex/QREXCore.kt", ""
    )
    main = sources.get(
        "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt", ""
    )
    scanner = sources.get(
        "platforms/android/app/src/main/java/com/brendigo/qrex/QREXScanner.kt", ""
    )
    for rule in ("uri.scheme?.lowercase() !in setOf(\"http\", \"https\")",
                 'uri.encodedAuthority?.contains("@")'):
        if rule not in core:
            issues.append("QR URL scheme or credential-bearing authority validation absent")
            break
    if "QRContent.safeUrl(url.toString()) ?: return" not in main:
        issues.append("External URL dispatch missing independent validation")
    for rule in ("confirmWifiCopy = true", "confirmWifiShare = true",
                 "confirmWifiSave = true", "confirmWifiPngExport = true"):
        if rule not in main:
            issues.append("Wi-Fi disclosure confirmation gate missing")
            break
    for rule in ("24_000_000L", "openInputStream(uri)", "bitmap.recycle()"):
        if rule not in scanner:
            issues.append("Gallery image size or bitmap disposal guard missing")
            break
    if re.search(r"\bimport\s+android\.util\.Log\b", combined):
        issues.append("Runtime Android logging may expose personal QR content")
    if re.search(r"\b(?:printStackTrace|System\.out\.print(?:ln)?|println)\s*\(", combined):
        issues.append("Console/error output is not approved for private QR payloads")
    return issues


def potential_dead_code(root: Path = ROOT) -> list[tuple[str, str]]:
    """Lexical candidates only; platform callbacks, method refs and tests count."""
    sources = kotlin_sources(root)
    tests = {
        str(path.relative_to(root)): path.read_text("utf-8")
        for path in (root / "platforms/android/app/src").rglob("*.kt")
        if "test" in path.parts or "androidTest" in path.parts
    }
    all_text = "\n".join([*sources.values(), *tests.values()])
    suspects: list[tuple[str, str]] = []
    for path, content in sources.items():
        for match in re.finditer(r"(?m)^\s*(?:(?:private|internal|public|override|suspend|inline|tailrec|operator)\s+)*fun\s+([A-Za-z]\w*)\s*\(", content):
            name = match.group(1)
            if name in {"onCreate", "analyze", "close", "invoke", "equals", "hashCode", "toString"}:
                continue
            # Count ordinary calls, method references (::function) and
            # references in tests; a source-only result is NEVER proof of dead code.
            count = len(re.findall(r"\b" + re.escape(name) + r"\b", all_text))
            if count == 1:
                suspects.append((path, name))
    return sorted(suspects)


def main() -> int:
    issues = security_findings()
    sources = kotlin_sources()
    candidates = potential_dead_code()
    print(f"QREX security audit: {len(sources)} Kotlin production source files; "
          f"{len(issues)} blocking source policy findings.")
    for issue in issues:
        print(f"  FAIL: {issue}", file=sys.stderr)
    print(f"Dead-code lexical review: {len(candidates)} functions warrant manual call-graph review.")
    for path, name in candidates:
        print(f"  REVIEW: {path}::{name} (not automatically removed)")
    print("Coverage: manifest, exported components, permission baseline, "
          "offline behavior, Keystore AES-GCM, URL dispatch, Wi-Fi consents, "
          "gallery image bounds, release shrinking and source-level unused methods.")
    print("Limitations: binary SCA/CVEs, transitive dependency graph, "
          "device penetration testing and runtime reflection need separate verification.")
    return 1 if issues else 0


if __name__ == "__main__":
    raise SystemExit(main())
