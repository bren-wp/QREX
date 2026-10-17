#!/usr/bin/env python3
"""Reject user-visible placeholder and technology-promotion labels in platform UIs.

This is a targeted text guard, not a runtime screenshot or localization test.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
FILES = [
    *ROOT.glob("platforms/android/app/src/main/java/**/*.kt"),
]
PATTERN = re.compile(r'\b(?:Text|Button|Label|TextButton|PrimaryAction)\s*\(\s*"([^"\n]*)"')
PLACEHOLDERS = re.compile(
    r"\b(?:demo|dev build|developer mode|testna aplikacija|nativno|nativna|native app|"
    r"flutter app|swiftui app|compose app|work in progress|placeholder)\b",
    re.IGNORECASE,
)

def main() -> int:
    violations = []
    for path in FILES:
        source = path.read_text(encoding="utf-8")
        for m in PATTERN.finditer(source):
            if PLACEHOLDERS.search(m.group(1)):
                violations.append(f"{path.relative_to(ROOT)}: {m.group(1)}")
    if violations:
        print("Neprimjereni tekstovi u korisničkom sučelju:\n" + "\n".join(violations))
        return 1
    print(f"QREX korisnički tekstovi provjereni u {len(FILES)} platformskih datoteka.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
