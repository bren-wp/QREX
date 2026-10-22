#!/usr/bin/env python3
"""Validate real PNG screenshot artifacts without OCR or external dependencies.

This checks PNG signature, dimensions and integrity, not 1:1 visual parity.
Source authenticity is established by the preceding emulator/simulator capture.
"""
from __future__ import annotations

import struct
import sys
import zlib
from pathlib import Path

SIGNATURE = bytes((137, 80, 78, 71, 13, 10, 26, 10))


def verify(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if len(data) < 20000 or not data.startswith(SIGNATURE):
        raise ValueError(f"{path}: missing or unexpectedly small PNG")
    offset = 8
    width = height = 0
    has_end = False
    while offset + 12 <= len(data):
        size = struct.unpack_from(">I", data, offset)[0]
        if size > 30_000_000 or offset + size + 12 > len(data):
            raise ValueError(f"{path}: invalid chunk length")
        name = data[offset + 4: offset + 8]
        chunk = data[offset + 8: offset + 8 + size]
        crc = struct.unpack_from(">I", data, offset + 8 + size)[0]
        if zlib.crc32(name + chunk) != crc:
            raise ValueError(f"{path}: invalid CRC in {name!r}")
        if name == b"IHDR":
            width, height = struct.unpack_from(">II", chunk)
        offset += size + 12
        if name == b"IEND":
            has_end = True
            break
    if not has_end or offset != len(data) or width < 700 or height < 1200:
        raise ValueError(f"{path}: incomplete or implausible phone capture")
    return width, height


if __name__ == "__main__":
    if len(sys.argv) < 2:
        raise SystemExit("Provide one or more screenshot filenames.")
    for arg in sys.argv[1:]:
        path = Path(arg)
        w, h = verify(path)
        print(f"Verified genuine PNG structure: {path} ({w} x {h})")

