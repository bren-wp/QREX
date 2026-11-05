# QREX — CameraX Y-plane buffer audit (2026-11-05)

## Baseline

Base branch: `main`, commit `b51fc351c1cd69e2d6faa46eea1bf30c81d2ed16` (merged PR #92).
At inspection the latest published GitHub release was `v1.0.33`; the exact-main
`v1.0.34` GitHub publisher was still running. This patch does **not** change
`applicationId`, Android version metadata, signing keys, QR record formats,
history migration, permissions, or the 23 language resources.

## Confirmed implementation defect and correction

`QREXQrDecoder.copyLuma` duplicated an `ImageProxy` Y-plane `ByteBuffer` and
then called `rewind()`. A positioned input buffer is a supported ByteBuffer
view and its active plane may begin after a prefix. Rewinding silently starts
luma extraction at byte zero rather than at the active window. Camera frames
with such a layout can produce garbage QR input even though the strides and
dimensions are otherwise valid.

The fix uses the input's current position as the Y-plane origin, performs
64-bit capacity and row-stride bounds checks, preserves the caller's buffer
position/limit, and bulk-copies the common single-byte Y stride. Non-unit pixel
strides still use indexed reads. This avoids any network dependency or change
to decoded payload storage.

## Regression coverage

`QREXQrDecoderTest` now exercises:
- a positioned packed luma buffer with padded rows and a nonzero prefix;
- a positioned, direct, non-unit-pixel-stride buffer;
- truncated/overlapping buffer layouts, with the caller position/limit intact;
- the existing standard/inverted/rotated QR cases remain in the suite.

The Kotlin/JVM tests, Android lint, R8, API 26/31/34/35/36 instrumentation
and screenshot jobs must be checked against the **PR head SHA** before merging.
Do not infer green tests from the prior `v1.0.34` merge. This audit note records
source findings, not a claim of device certification or complete MASVS coverage.

## Documentation drift resolved

`platforms/README.md` incorrectly listed proprietary ML Kit and Gradle 8.13.
The source build uses ZXing/CameraX, AGP 9.0.1, Kotlin Compose plugin 2.2.10,
and GitHub CI sets up Gradle 9.1.0 with JDK 17 and API 36. The documentation
now distinguishes GitHub disposable signing, Google Play upload signing,
and the **unverified** status of official F-Droid distribution.

## Release sequencing

Do not overwrite existing tags. Confirm completion of the previously started
`v1.0.34` workflow and public assets before preparing `v1.0.35`; an
unreviewed/untested branch must not trigger stable publication.
