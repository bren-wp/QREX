#!/usr/bin/env python3
"""Fail if translated platform UI uses missing keys or Croatian literals."""
from __future__ import annotations
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
catalog = json.loads((ROOT / "l10n/catalog.json").read_text(encoding="utf-8"))
keys = set(catalog["languages"]["en"]["strings"])
files = [
    *ROOT.glob("platforms/android/app/src/main/java/**/*.kt"),
]
issues = []
for path in files:
    text = path.read_text(encoding="utf-8")
    for match in re.finditer(r'\btr\("([^"]+)"\)', text):
        if match.group(1) not in keys:
            issues.append(f"{path.relative_to(ROOT)}: unknown key {match.group(1)}")
    for number, line in enumerate(text.splitlines(), 1):
        if re.search(r'(?:(?:Text|Button|Label)\s*\(|contentDescription\s*=\s*|title\s*=\s*\{\s*Text\()\s*"[^"]*[čćđšžČĆĐŠŽ]', line):
            issues.append(f"{path.relative_to(ROOT)}:{number}: untranslated UI literal")
if issues:
    print("\n".join(issues))
    sys.exit(1)
print(f"Translation usage verified: {len(files)} Android source files.")
