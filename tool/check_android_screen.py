#!/usr/bin/env python3
"""Verify the actual Android accessibility hierarchy, not merely PNG validity.

A black screen or a System UI ANR may still be a well-formed PNG. Android
screenshots must depict the real, interactive QREX create view in English.
No OCR, third-party dependencies, test payload uploads, or fabricated images.
"""
from __future__ import annotations
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def validate(xml_content: str) -> None:
    root = ET.fromstring(xml_content)
    nodes = list(root.iter("node"))
    if not nodes:
        raise ValueError("No accessibility nodes; emulator UI not ready")
    texts = " ".join(
        " ".join((node.attrib.get("text", ""), node.attrib.get("content-desc", "")))
        for node in nodes
    ).lower()
    errors = (
        "system ui isn't responding", "system ui is not responding",
        "qrex isn't responding", "close app", "app isn't responding",
    )
    if any(error in texts for error in errors):
        raise ValueError("System ANR dialog detected — reject screenshot")
    packages = {n.attrib.get("package", "") for n in nodes}
    if "com.brendigo.qrex" not in packages:
        raise ValueError(f"QREX is not the foreground package: {sorted(packages)}")
    if "create qr code" not in texts or "https://brendigo.com" not in texts:
        raise ValueError("QREX create screen not fully visible")
    if "more options" not in texts:
        raise ValueError("QR creation controls not ready")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: check_android_screen.py <uiautomator XML>", file=sys.stderr)
        return 2
    try:
        validate(Path(sys.argv[1]).read_text(encoding="utf-8"))
    except (ValueError, ET.ParseError) as exc:
        print(f"Android screenshot rejected: {exc}", file=sys.stderr)
        return 1
    print("Android screen verified: QREX active, correct controls, no ANR")
    return 0


if __name__ == "__main__":
    sys.exit(main())
