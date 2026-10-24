#!/usr/bin/env python3
"""Deterministic pixel-by-pixel QREX UI reference comparator.

Use identical viewport, font scale, locale, state and screenshot dimensions.
This does NOT declare 1:1 parity unless original reference PNGs are supplied.
No OCR, cloud upload, image generation or Pillow dependency is required.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
import struct
import sys
import zlib

SIGNATURE = b"\x89PNG\r\n\x1a\n"
SCREENS = (
    "splash", "scan", "create", "wifi", "result",
    "history", "saved", "more", "language", "export",
)


@dataclass(frozen=True)
class Pixels:
    width: int
    height: int
    rgba: bytes


@dataclass(frozen=True)
class Difference:
    width: int
    height: int
    mismatched: int
    max_channel_delta: int
    bounds: tuple[int, int, int, int] | None

    @property
    def percent(self) -> float:
        return (100.0 * self.mismatched) / (self.width * self.height)


def _paeth(left: int, above: int, upper_left: int) -> int:
    predicted = left + above - upper_left
    a, b, c = abs(predicted - left), abs(predicted - above), abs(predicted - upper_left)
    return left if a <= b and a <= c else above if b <= c else upper_left


def read_png(path: Path) -> Pixels:
    data = path.read_bytes()
    if not data.startswith(SIGNATURE):
        raise ValueError("Invalid PNG signature")
    pos, dimensions, chunks, ended = 8, None, [], False
    while pos + 12 <= len(data):
        size = struct.unpack_from(">I", data, pos)[0]
        end = pos + 12 + size
        if size > 36_000_000 or end > len(data):
            raise ValueError("Invalid PNG chunk boundary")
        tag = data[pos + 4:pos + 8]
        value = data[pos + 8:pos + 8 + size]
        stored_crc = struct.unpack_from(">I", data, pos + 8 + size)[0]
        if zlib.crc32(tag + value) != stored_crc:
            raise ValueError("Invalid PNG chunk CRC")
        pos = end
        if tag == b"IHDR":
            if dimensions is not None or len(value) != 13:
                raise ValueError("Invalid IHDR")
            w, h, depth, color, compression, filtering, interlace = struct.unpack(">IIBBBBB", value)
            if w < 1 or h < 1 or w * h > 8_000_000:
                raise ValueError("Unsupported image dimensions")
            if depth != 8 or color not in (2, 6) or (compression, filtering, interlace) != (0, 0, 0):
                raise ValueError("Requires 8-bit RGB or RGBA non-interlaced PNG")
            dimensions = (w, h, 3 if color == 2 else 4)
        elif tag == b"IDAT":
            chunks.append(value)
        elif tag == b"IEND":
            ended = True
            break
    if dimensions is None or not ended or pos != len(data) or not chunks:
        raise ValueError("Incomplete PNG")
    width, height, bpp = dimensions
    stride = width * bpp
    expected = height * (stride + 1)
    inflater = zlib.decompressobj()
    raw = inflater.decompress(b"".join(chunks), expected + 1)
    if len(raw) != expected or not inflater.eof or inflater.unconsumed_tail or inflater.unused_data:
        raise ValueError("Invalid or oversized PNG image data")
    rgba = bytearray(width * height * 4)
    previous = bytearray(stride)
    offset = 0
    for row_no in range(height):
        filter_type = raw[offset]
        offset += 1
        scanline = bytearray(raw[offset:offset + stride])
        offset += stride
        if filter_type not in range(5):
            raise ValueError("Unsupported PNG filter")
        for i in range(stride):
            left = scanline[i - bpp] if i >= bpp else 0
            above = previous[i]
            upper_left = previous[i - bpp] if i >= bpp else 0
            if filter_type == 1:
                scanline[i] = (scanline[i] + left) & 255
            elif filter_type == 2:
                scanline[i] = (scanline[i] + above) & 255
            elif filter_type == 3:
                scanline[i] = (scanline[i] + ((left + above) // 2)) & 255
            elif filter_type == 4:
                scanline[i] = (scanline[i] + _paeth(left, above, upper_left)) & 255
        for x in range(width):
            i = x * bpp
            k = (row_no * width + x) * 4
            rgba[k:k + 3] = scanline[i:i + 3]
            rgba[k + 3] = scanline[i + 3] if bpp == 4 else 255
        previous = scanline
    return Pixels(width, height, bytes(rgba))


def compare(reference: Pixels, actual: Pixels, tolerance: int = 0) -> Difference:
    if (reference.width, reference.height) != (actual.width, actual.height):
        raise ValueError("Viewport dimensions differ; refuse to rescale reference pixels")
    if not 0 <= tolerance <= 255:
        raise ValueError("Per-channel tolerance must be 0..255")
    width, height = reference.width, reference.height
    changed, max_delta, bounds = 0, 0, None
    for pixel in range(width * height):
        pos = 4 * pixel
        delta = max(abs(reference.rgba[pos + channel] - actual.rgba[pos + channel])
                    for channel in range(4))
        max_delta = max(max_delta, delta)
        if delta > tolerance:
            changed += 1
            x, y = pixel % width, pixel // width
            if bounds is None:
                bounds = (x, y, x, y)
            else:
                left, top, right, bottom = bounds
                bounds = (min(left, x), min(top, y), max(right, x), max(bottom, y))
    return Difference(width, height, changed, max_delta, bounds)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference-dir", type=Path, required=True)
    parser.add_argument("--actual-dir", type=Path, required=True)
    parser.add_argument("--screen", action="append", choices=SCREENS,
                        help="Repeat to check a subset; all ten by default")
    parser.add_argument("--channel-tolerance", type=int, default=0)
    parser.add_argument("--max-mismatch-percent", type=float, default=0.0)
    args = parser.parse_args(argv)
    if not 0 <= args.channel_tolerance <= 255 or not 0 <= args.max_mismatch_percent <= 100:
        parser.error("Tolerance and mismatch percentage outside valid range")
    failed = False
    for screen in args.screen or SCREENS:
        name = f"android-{screen}.png"
        try:
            diff = compare(read_png(args.reference_dir / name),
                           read_png(args.actual_dir / name), args.channel_tolerance)
            print(f"{name}: different={diff.mismatched}/{diff.width * diff.height} "
                  f"({diff.percent:.6f}%), maxChannelDelta={diff.max_channel_delta}, "
                  f"changedBounds={diff.bounds}")
            failed |= diff.percent > args.max_mismatch_percent
        except (OSError, ValueError, zlib.error) as exc:
            print(f"{name}: cannot compare: {type(exc).__name__}", file=sys.stderr)
            failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
