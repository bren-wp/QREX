#!/usr/bin/env python3
"""Report only allowlisted Android test identifiers, never payloads or stack traces.

Runs on CI after an instrumented test failure. XML reports may include scanned
QR contents in failure messages; ignore ALL message, body, system-out/err fields.
"""
from __future__ import annotations

import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path("platforms/android/app/build/outputs/androidTest-results/connected")
ALLOW = re.compile(r"^com[.]brendigo[.]qrex[.]QREX[A-Za-z0-9_]{1,88}$")
METHOD = re.compile(r"^[A-Za-z][A-Za-z0-9_]{1,90}$")



# Diagnostic labels are printed only for QREX-owned, allowlisted test methods.
# The XML exception messages and untrusted stack bodies are never printed.
TYPE = re.compile(
    r"^(?:java[.]lang[.](?:AssertionError|IllegalStateException|"
    r"IllegalArgumentException|NullPointerException|RuntimeException)|"
    r"androidx[.]compose[.]ui[.]test[.](?:AssertionError|"
    r"ComposeTimeoutException|ComposeNotIdleException|"
    r"SemanticsNodeInteractionException)|"
    r"org[.]junit[.]ComparisonFailure)$"
)
FRAME = re.compile(
    r"(?m)^\\s+at com[.]brendigo[.]qrex[.](QREX[A-Za-z0-9_]+Test)"
    r"[.][A-Za-z0-9_$<>]+\\(([A-Za-z0-9_]+[.]kt):([0-9]{1,4})\\)"
)

def failed_test_diagnostics(path: pathlib.Path) -> list[str]:
    safe: dict[str, str] = {}
    for xml in sorted(path.rglob("TEST-*.xml"))[:200]:
        try:
            if xml.stat().st_size > 4_000_000:
                continue
            root = ET.parse(xml).getroot()
        except (OSError, ET.ParseError, ValueError):
            continue
        for case in root.iter("testcase"):
            cls, method = case.attrib.get("classname", ""), case.attrib.get("name", "")
            if not (ALLOW.fullmatch(cls) and METHOD.fullmatch(method)):
                continue
            failure = case.find("failure")
            if failure is None:
                failure = case.find("error")
            if failure is None:
                continue
            identity = cls.rsplit(".", 1)[-1] + "#" + method
            kind = failure.attrib.get("type", "")
            label = kind.rsplit(".", 1)[-1] if TYPE.fullmatch(kind) else "UnknownFailure"
            trace = failure.text or ""
            location = FRAME.search(trace)
            location_str = ""
            if location and location.group(1) == cls.rsplit(".", 1)[-1] and location.group(2) == location.group(1) + ".kt":
                location_str = " @ " + location.group(2) + ":" + location.group(3)
            safe[identity] = identity + " -> " + label + location_str
    return sorted(safe.values())

def failed_test_identifiers(path: pathlib.Path) -> list[str]:
    results: set[str] = set()
    for xml in sorted(path.rglob("TEST-*.xml"))[:200]:
        try:
            if xml.stat().st_size > 4_000_000:
                continue
            root = ET.parse(xml).getroot()
        except (OSError, ET.ParseError, ValueError):
            continue
        for case in root.iter("testcase"):
            cls = case.attrib.get("classname", "")
            name = case.attrib.get("name", "")
            if not (ALLOW.fullmatch(cls) and METHOD.fullmatch(name)):
                continue
            if case.find("failure") is not None or case.find("error") is not None:
                results.add(f"{cls.rsplit('.', 1)[-1]}#{name}")
    return sorted(results)


def main() -> int:
    if len(sys.argv) > 2:
        return 64
    path = pathlib.Path(sys.argv[1]) if len(sys.argv) == 2 else ROOT
    names = failed_test_diagnostics(path)
    print("QREX P03 failing tests (allowlisted IDs, failure types, source locations only):")
    if names:
        for name in names:
            print(f"- {name}")
    else:
        print("- No allowlisted test identifiers available (crash may precede reporting)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
