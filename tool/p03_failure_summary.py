#!/usr/bin/env python3
"""Print ONLY allowlisted instrumented test identifiers, never test failure messages.

The AGP XML failure's message, type, stacktrace, system-out and system-err
can include scanned QR contents or saved Wi-Fi details, so this tool reads
only testcase name/classname attributes and failure presence. It is invoked
only after a failing instrumentation run, and does not mark CI as passing.
"""
from __future__ import annotations

import pathlib
import re
import sys
import xml.etree.ElementTree as ET

TEST_ID = re.compile(r"[A-Za-z_][A-Za-z0-9_]{0,100}\Z")
CLASS_ID = re.compile(r"com\.brendigo\.qrex\.[A-Za-z_][A-Za-z0-9_]{0,100}\Z")
MAX_FILES = 1000
MAX_IDS = 40


def summarize(directory: pathlib.Path, api: str) -> str:
    safe_api = api if re.fullmatch(r"(26|31|34|35|36)", api) else "unknown"
    cases: set[str] = set()
    xml_count = 0
    unreported = 0
    for path in sorted(directory.rglob("TEST-*.xml"))[:MAX_FILES]:
        if not path.is_file() or path.stat().st_size > 8_000_000:
            continue
        try:
            root = ET.parse(path).getroot()
        except (ET.ParseError, OSError):
            unreported += 1
            continue
        xml_count += 1
        for case in root.iter("testcase"):
            if not any(child.tag in ("failure", "error") for child in case):
                continue
            class_name = case.attrib.get("classname", "")
            name = case.attrib.get("name", "")
            # Suppress any dynamic or attacker-controlled QR payload in XML
            # attributes, even if it looks superficially like a test name.
            if CLASS_ID.fullmatch(class_name) and TEST_ID.fullmatch(name):
                cases.add(f"{class_name}.{name}")
            else:
                unreported += 1
    lines = [
        "# P03 sanitized instrumentation failures",
        f"- Android API: {safe_api}",
        f"- Readable JUnit XML reports: {xml_count}",
        f"- Failed test IDs: {len(cases)}",
        f"- Unreported/invalid entries: {unreported}",
        "- All assertion details, stack traces, QR contents and system logs omitted.",
    ]
    lines.extend(f"- {name}" for name in sorted(cases)[:MAX_IDS])
    if not cases:
        lines.append("- No allowlisted failure name found; the instrumentation run still fails closed.")
    return "\n".join(lines) + "\n"


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: p03_failure_summary.py REPORT_ROOT API", file=sys.stderr)
        return 64
    path = pathlib.Path(sys.argv[1])
    if not path.is_dir():
        print("# P03 sanitized instrumentation failures\n- Test report directory unavailable.\n")
        return 0
    print(summarize(path, sys.argv[2]), end="")
    return 0


if __name__ == "__main__":
    sys.exit(main())
