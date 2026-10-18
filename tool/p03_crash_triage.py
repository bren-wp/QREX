#!/usr/bin/env python3
"""Summarize QREX Java crash blocks without publishing QR payloads or raw logcat.

Reads Android's *crash* ring buffer, not the ANR traces. The output intentionally
drops exception messages, intent extras, Wi-Fi strings, URLs and non-app logs.
A missing QREX crash block is NOT proof of zero ANRs or device crashes.
"""
from __future__ import annotations

import pathlib
import re
import sys

PROCESS = re.compile(r"\bProcess:\s*com\.brendigo\.qrex(?:,|\s|$)")
START = re.compile(r"FATAL EXCEPTION:")
EXCEPTION_CLASS = re.compile(
    r"\b(?:java|kotlin|android|com)\.[\w.$]*(?:Exception|Error)\b"
)
APP_FRAME = re.compile(
    r"\bat\s+(com\.brendigo\.qrex\.[\w.$]+)\("
    r"([\w.$]+)(?::(\d+))?\)"
)


def report(raw: str, api: str = "unknown") -> tuple[str, int]:
    blocks = re.split(r"(?=FATAL EXCEPTION:)", raw)
    crashes = []
    for block in blocks:
        if not START.search(block) or not PROCESS.search(block):
            continue
        classes = list(dict.fromkeys(EXCEPTION_CLASS.findall(block)))[:4]
        frames = APP_FRAME.findall(block)[:16]
        details = ["- Exception classes: " + (", ".join(classes) if classes else "unknown")]
        details += [
            f"  - {method} ({filename}:{line or '?'})"
            for method, filename, line in frames
        ]
        crashes.append("\n".join(details))
    summary = [
        "# QREX P03 — sanitized crash-buffer summary",
        "",
        f"- Android API under test: {api}",
        "- Source: adb logcat -b crash -v threadtime -d",
        f"- QREX FATAL EXCEPTION blocks: {len(crashes)}",
        "- Exception messages and all raw log content intentionally omitted.",
        "- Empty crash buffer does not prove absence of ANR/process death.",
        "",
    ]
    summary.extend(crashes if crashes else ["No QREX FATAL EXCEPTION block captured."])
    return "\n".join(summary) + "\n", len(crashes)


def main() -> int:
    if len(sys.argv) != 4:
        print("usage: p03_crash_triage.py INPUT OUTPUT API", file=sys.stderr)
        return 64
    raw_path, output_path, api = sys.argv[1:]
    try:
        raw = pathlib.Path(raw_path).read_text(encoding="utf-8", errors="replace")
        cleaned, count = report(raw, api)
        pathlib.Path(output_path).write_text(cleaned, encoding="utf-8")
        print(f"Sanitized QREX crash blocks: {count}; raw log is not uploaded.")
        return 2 if count else 0
    except OSError as exc:
        print(f"Crash-buffer report unavailable: {type(exc).__name__}", file=sys.stderr)
        return 3


if __name__ == "__main__":
    sys.exit(main())
