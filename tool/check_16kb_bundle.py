#!/usr/bin/env python3
"""Check ELF PT_LOAD alignment in 64-bit .so libraries bundled into Android AAB.

Google Play performs additional bundle and device compatibility checks; this
static ELF gate alone does not guarantee comprehensive 16 KiB support.
"""
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BUNDLE = ROOT / "platforms/android/app/build/outputs/bundle/release/app-release.aab"
ARCHES = ("arm64-v8a", "x86_64")
MIN_ALIGNMENT = 16 * 1024


def load_alignments(output: str) -> list[int]:
    values = []
    for line in output.splitlines():
        fields = line.split()
        if fields and fields[0] == "LOAD":
            try:
                values.append(int(fields[-1], 16))
            except ValueError:
                values.append(0)
    return values


def main() -> int:
    if not BUNDLE.is_file():
        print(f"Missing Android AAB: {BUNDLE}", file=sys.stderr)
        return 1
    readelf = shutil.which("readelf")
    errors = []
    inspected = 0
    with zipfile.ZipFile(BUNDLE) as archive, tempfile.TemporaryDirectory() as folder:
        for entry in archive.infolist():
            if not entry.filename.endswith(".so") or not any(
                f"/lib/{arch}/" in "/" + entry.filename for arch in ARCHES
            ):
                continue
            inspected += 1
            if not readelf:
                print("readelf must be installed to check ELF alignment", file=sys.stderr)
                return 1
            if entry.file_size > 200_000_000:
                errors.append(f"Unexpected large native library: {entry.filename}")
                continue
            tmp = Path(folder) / f"lib-{inspected}.so"
            with archive.open(entry) as source, tmp.open("wb") as output:
                shutil.copyfileobj(source, output)
            check = subprocess.run([readelf, "-lW", str(tmp)], capture_output=True,
                                   text=True, timeout=30)
            if check.returncode != 0:
                errors.append(f"Unreadable ELF native library: {entry.filename}")
                continue
            alignments = load_alignments(check.stdout)
            if not alignments or min(alignments) < MIN_ALIGNMENT:
                errors.append(f"{entry.filename}: 16-KiB PT_LOAD alignment failed: {alignments}")
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"Checked {inspected} 64-bit .so files in Android AAB for 16-KiB ELF alignment.")
    print("Also verify actual Google Play 16-KiB packaging and runtime compatibility.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
