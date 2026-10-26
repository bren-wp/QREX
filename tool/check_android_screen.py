#!/usr/bin/env python3
"""Verify the actual Android accessibility hierarchy, not merely PNG validity.

A black screen or a System UI ANR may still be a well-formed PNG. Android
screenshots must depict the real, interactive QREX screen in Croatian.
No OCR, third-party dependencies, test payload uploads, or fabricated images.
"""
from __future__ import annotations
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def validate(xml_content: str, screen: str = "create") -> None:
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
    # Require screen-specific actual app text and control markers. These
    # are screenshots of production Compose UI with *debug-only* neutral data.
    # Splash's old English-only root content-desc was removed for TalkBack:
    # check the actual localized footer as well as the branded wordmark and
    # tagline, so the regular Scan/Create header cannot impersonate Splash.
    # Result's pinned Back icon now correctly announces localized "Natrag".
    expected = {
        "splash": ("qrex", "skener. stvara. dijeli.", "sve što može qr"),
        "create": ("stvori qr kod", "https://brendigo.com", "wi-fi"),
        "wifi": ("stvori qr kod", "qrex-demo", "wi-fi"),
        "result": ("rezultat", "https://brendigo.com", "natrag"),
        "history": ("povijest", "brendigo.com", "pretraži povijest..."),
        "saved": ("favoriti", "brendigo.com", "spremljeno"),
        "more": ("postavke", "pdf", "jezik"),
        "language": ("jezik", "hrvatski", "english"),
        "export": ("izvoz arhive", "pdf"),
        "responsive-create": ("stvori qr kod",),
        "responsive-scan": ("skener. stvara. dijeli.", "iz galerije"),
        "responsive-more": ("postavke", "jezik"),
        "scan": ("skener. stvara. dijeli.", "iz galerije", "usmjeri kameru prema qr kodu"),
    }
    if screen not in expected:
        raise ValueError("Unsupported screenshot screen")
    missing = tuple(value for value in expected[screen] if value not in texts)
    if missing:
        # Log ONLY static fixture marker names, never captured accessibility
        # text: scanned QR/Keystore contents must not leak to CI logs.
        raise ValueError(
            f"QREX {screen} screen not fully visible; absent expected markers: "
            + ", ".join(missing)
        )


def main() -> int:
    if len(sys.argv) not in (2, 3):
        print("Usage: check_android_screen.py <uiautomator XML> [screen]", file=sys.stderr)
        return 2
    try:
        validate(Path(sys.argv[1]).read_text(encoding="utf-8"),
                 sys.argv[2] if len(sys.argv) == 3 else "create")
    except (ValueError, ET.ParseError) as exc:
        print(f"Android screenshot rejected: {exc}", file=sys.stderr)
        return 1
    print("Android screen verified: QREX active, correct controls, no ANR")
    return 0


if __name__ == "__main__":
    sys.exit(main())

