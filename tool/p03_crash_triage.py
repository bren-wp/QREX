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


def report(raw: str, api: str = "unknown", events: str = "") -> tuple[str, int]:
    # Fail closed: exception *messages* may contain arbitrary QR contents,
    # including forged Java-looking classes, source paths and fake stack frames.
    # No variable class, method, file, line, exception message, or event payload
    # from logcat may enter a public CI artifact. Keep only aggregate counts.
    blocks = re.split(r"(?=FATAL EXCEPTION:)", raw)
    crashes = sum(
        1 for block in blocks
        if START.search(block) and PROCESS.search(block)
    )
    anr_count = sum(
        1 for line in events.splitlines()
        if re.search(r"\\bam_anr:\\s*\\[[^,\\]\\n]{1,32},[^,\\]\\n]{1,32},"
                     r"com\\.brendigo\\.qrex(?:,|\\])", line)
    )
    api = api if re.fullmatch(r"\\d{2}", api) else "unknown"
    summary = [
        "# QREX P03 — sanitized crash-buffer summary",
        "",
        f"- Android API under test: {api}",
        "- Source: adb logcat -b crash -v threadtime -d",
        f"- QREX FATAL EXCEPTION blocks: {crashes}",
        f"- QREX am_anr events: {anr_count}",
        "- Exception class, message, frames, file paths and raw events omitted.",
        "- Empty crash buffer does not prove absence of ANR/process death.",
        "",
    ]
    summary.append("QREX FATAL EXCEPTION captured: inspect PRIVATE log locally."\n                   if crashes else "No QREX FATAL EXCEPTION block captured.")
    return "\n".join(summary) + "\n", crashes + anr_count


def main() -> int:
    if len(sys.argv) != 5:
        print("usage: p03_crash_triage.py CRASH_LOG OUTPUT API EVENT_LOG", file=sys.stderr)
        return 64
    raw_path, output_path, api, event_path = sys.argv[1:]
    try:
        raw = pathlib.Path(raw_path).read_text(encoding="utf-8", errors="replace")
        events = pathlib.Path(event_path).read_text(encoding="utf-8", errors="replace")
        cleaned, count = report(raw, api, events)
        pathlib.Path(output_path).write_text(cleaned, encoding="utf-8")
        print(f"Sanitized QREX crash/ANR events: {count}; raw logs not uploaded.")
        return 2 if count else 0
    except OSError as exc:
        print(f"Crash-buffer report unavailable: {type(exc).__name__}", file=sys.stderr)
        return 3


if __name__ == "__main__":
    sys.exit(main())
