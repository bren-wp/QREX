#!/usr/bin/env python3
"""Reject light-theme Android screenshots mislabeled as QREX dark captures.

Reads a small, fixed header-region sample of *actual PNG pixels*. Does not
generate screenshot images, use OCR, send data, or require Pillow. Existing
verify_screenshots.py separately checks PNG structure/dimensions; this only
checks appearance. Splash is intentionally excluded because its art is blue.
"""
from __future__ import annotations
import struct
import sys
import zlib
from pathlib import Path

SAMPLE_X = (0.26, 0.45, 0.66, 0.82)
SAMPLE_Y = (0.06, 0.09, 0.12)


def header_samples(path: Path) -> list[tuple[int, int, int]]:
    blob = path.read_bytes()
    if blob[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not PNG")
    offset = 8
    width = height = depth = color = None
    idat: list[bytes] = []
    while offset + 12 <= len(blob):
        length = struct.unpack_from(">I", blob, offset)[0]
        if length > 32_000_000 or offset + 12 + length > len(blob):
            raise ValueError("invalid PNG chunk")
        kind = blob[offset + 4:offset + 8]
        payload = blob[offset + 8:offset + 8 + length]
        offset += length + 12
        if kind == b"IHDR":
            width, height, depth, color, compression, filt, interlace = struct.unpack(
                ">IIBBBBB", payload
            )
            if not (100 <= width <= 3000 and 100 <= height <= 5000):
                raise ValueError("unexpected screen dimensions")
            if depth != 8 or color not in (2, 6) or (compression, filt, interlace) != (0, 0, 0):
                raise ValueError("unsupported PNG format")
        elif kind == b"IDAT":
            idat.append(payload)
        elif kind == b"IEND":
            break
    if width is None or not idat:
        raise ValueError("missing PNG header or pixels")
    bpp = 4 if color == 6 else 3
    stride = width * bpp
    raw = zlib.decompress(b"".join(idat))
    if len(raw) != height * (stride + 1):
        raise ValueError("incomplete PNG image")
    sample_rows = {int(height * f) for f in SAMPLE_Y}
    sample_cols = [int(width * f) for f in SAMPLE_X]
    previous = bytearray(stride)
    result: list[tuple[int, int, int]] = []
    for y in range(max(sample_rows) + 1):
        start = y * (stride + 1)
        code = raw[start]
        row = bytearray(raw[start + 1:start + 1 + stride])
        if code > 4:
            raise ValueError("unsupported PNG scanline filter")
        if code:
            for i in range(stride):
                a = row[i - bpp] if i >= bpp else 0
                b = previous[i]
                c = previous[i - bpp] if i >= bpp else 0
                if code == 1:
                    predictor = a
                elif code == 2:
                    predictor = b
                elif code == 3:
                    predictor = (a + b) // 2
                else:
                    p = a + b - c
                    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                    predictor = a if pa <= pb and pa <= pc else b if pb <= pc else c
                row[i] = (row[i] + predictor) & 255
        if y in sample_rows:
            for x in sample_cols:
                rgb = x * bpp
                result.append((row[rgb], row[rgb + 1], row[rgb + 2]))
        previous = row
    return result


def verify_dark(path: Path) -> None:
    colors = header_samples(path)
    # The top header/background region on the dark reference is navy.
    # Small white labels/status icons are expected and tolerated.
    dark = sum((r * 21 + g * 72 + b * 7) < 14_500
               for r, g, b in colors)
    if dark < 8:
        raise ValueError(
            f"dark Android header mismatch: {dark}/{len(colors)} sample points"
        )


if __name__ == "__main__":
    if len(sys.argv) < 2:
        raise SystemExit("Pass actual Android screenshot PNG files")
    for filename in sys.argv[1:]:
        path = Path(filename)
        try:
            verify_dark(path)
        except (ValueError, OSError, zlib.error) as exc:
            raise SystemExit(f"{path.name}: rejected dark screenshot ({exc})")
        print(f"{path.name}: verified dark header pixels")
