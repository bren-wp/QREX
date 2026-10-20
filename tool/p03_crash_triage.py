#!/usr/bin/env python3
"""Count QREX fatal exceptions and ANR events without exposing private QR content.

CI artifacts contain only aggregate counts. Exception messages can contain
untrusted QR/Wi-Fi strings, including forged stack traces and class-like names,
so not even variable frame fields are exported by this tool.
"""
from __future__ import annotations

import pathlib
import re
import sys

PROCESS = re.compile(r"\bProcess:\s*com\.brendigo\.qrex(?:,|\s|$)")
START = re.compile(r"FATAL EXCEPTION:")
ANR = re.compile(
    r"\bam_anr\s*:\s*\[[^,\]\n]{1,32},[^,\]\n]{1,32},"
    r"com\.brendigo\.qrex(?:,|\])"
)


def report(raw: str, api: str = "unknown", events: str = "") -> tuple[str, int]:
    blocks = re.split(r"(?=FATAL EXCEPTION:)", raw)
    crashes = sum(
        1 for block in blocks
        if START.search(block) and PROCESS.search(block)
    )
    anr_count = sum(bool(ANR.search(line)) for line in events.splitlines())
    safe_api = api if re.fullmatch(r"\d{2}", api) else "unknown"
    lines = [
        "# QREX P03 — redacted crash/ANR summary",
        "",
        f"- Android API under test: {safe_api}",
        f"- QREX FATAL EXCEPTION blocks: {crashes}",
        f"- QREX am_anr events: {anr_count}",
        "- Raw logcat, exception messages, stack frames and event details omitted.",
        "- An empty log buffer is not evidence of crash/ANR-free physical devices.",
        "",
        (
            "QREX fatal crash captured: inspect the private, non-uploaded log."
            if crashes else "No QREX fatal crash captured in this buffer."
        ),
    ]
    return "\n".join(lines) + "\n", crashes + anr_count


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
        print(f"Crash/ANR report unavailable: {type(exc).__name__}", file=sys.stderr)
        return 3


if __name__ == "__main__":
    sys.exit(main())
