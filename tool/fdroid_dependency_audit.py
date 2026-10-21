#!/usr/bin/env python3
"""Fail-closed checks for QREX's resolved Android release runtime dependency graph.

This is a narrow denylist, not a full SPDX/SBOM legal determination.
Pass in the text produced by Gradle's :app:dependencies --configuration releaseRuntimeClasspath.
"""
from pathlib import Path
import re
import sys

DENYLIST = (
    "com.google.mlkit:",
    "com.google.android.gms:",
    "com.google.firebase:",
    "com.google.android.play:",
    "com.google.android.libraries.vision:",
    "com.google.android.libraries.places:",
    "com.google.ads:",
    "com.android.billingclient:",
    "com.facebook.android:audience-network-sdk",
)
def audit(text: str) -> list[str]:
    if "releaseRuntimeClasspath" not in text:
        return ["Missing resolved releaseRuntimeClasspath report"]
    if "No dependencies" in text:
        return ["Empty resolved release dependency graph"]
    tokens = set(re.findall(r"[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+:[A-Za-z0-9_.+\-]+", text))
    return sorted(item for item in tokens if item.startswith(DENYLIST))


def main() -> int:
    if len(sys.argv) != 2 or not Path(sys.argv[1]).is_file():
        print("Usage: fdroid_dependency_audit.py <gradle-dependencies-report>", file=sys.stderr)
        return 2
    issues = audit(Path(sys.argv[1]).read_text(encoding="utf-8"))
    if issues:
        for issue in issues:
            print("F-DROID DEPENDENCY BLOCKER:", issue, file=sys.stderr)
        return 1
    print("Known non-FLOSS runtime coordinates not detected in Gradle release graph.")
    print("Manual third-party licensing, build tools, assets and bundled binary audit is still required.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
