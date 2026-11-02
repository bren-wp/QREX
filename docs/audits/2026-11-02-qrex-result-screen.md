# QREX — Result screen 05 audit and UI pass

Date: 2026-11-02  
PR: [#83](https://github.com/bren-wp/QREX/pull/83)  
Android package: `com.brendigo.qrex`

## Verified changes

- Result's primary **Copy** action and the overflow-menu **Copy** action both
  request an explicit Wi-Fi disclosure confirmation before copying a full Wi-Fi
  QR payload which can embed a network password. Cancel keeps the Result visible
  and does not call the clipboard API. A fresh attempt prompts again.
- The actual clipboard copy still marks payloads as sensitive on Android 13+
  via `ClipDescription.EXTRA_IS_SENSITIVE`. Android's clipboard remains external
  OS state: this mechanism does not encrypt copied text or guarantee expiry.
- The summary previously displayed a URL twice, including token-bearing links;
  its duplicate second line has been removed without removing the primary
  link or its normal Open action.
- The QR preview action now truthfully reports **Show QR** when hidden and
  **Hide** when displayed. Existing bitmap generation, result navigation,
  scroll and PNG/Save/Share consent gates remain unchanged.
- No changes to storage, record migration, passwords in logs, camera, locale
  catalog, SDK minimum, permissions, app ID, or Play upload certificates.

## Verification

- `QREXResultWifiCopyConsentTest`: Compose emulator interactions open a
  synthetic password-bearing Wi-Fi QR, tap Copy, verify disclosure and Cancel,
  and verify the overflow Copy uses the same gate. This does not copy secrets
  to the instrumented emulator's clipboard during the test.
- `tool/test_privacy_stability.py`: validates both Copy paths, warning
  controls, sensitive clipboard metadata, correct Show/Hide labels and lack of
  duplicate URL display.
- Retain required Kotlin/JVM, Lint/R8, 23-language, actual 12-screen emulator
  captures and P03 API 26/31/34/35/36 checks before any merge or publication.

## Image comparison limitation

The provided visual reference is a composite with perspective phone frames
rather than rectified, pixel-aligned device PNGs. Improvements are implemented
in real Compose and tested on Android, but mathematical 1:1 identity to the
composite is not claimed.

## Release status

The `v1.0.27`/`127` code is a candidate in an open draft PR. Public
GitHub CI artifacts use a disposable certificate, not the authorized
Google Play upload key. Do not publish or advertise a new release until
all final exact-commit checks and assets are verified.
