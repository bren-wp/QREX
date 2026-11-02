# QREX — new branding reference: screens 01–02

Date: 2026-11-02. Source: user-supplied composite `QREX_ Sve što može QR(20261102-200600).png`.

## Scope

This is a **native Android Compose implementation pass**, not a generated picture,
a screenshot pasted behind controls, or a change to functional QR payload handling.
The source is a marketing board with device frames and an illustrated camera
view, not independently aligned device screenshots; zero-pixel deviation cannot
be asserted from this composite.

### 01 — Splash

- Keep the production Q/QR Android vector and the same two-tone QRE/X wordmark.
- Add a restrained cyan vector-drawn halo around the real icon, matching the
  luminous rim in the supplied reference without bundling a bitmap.
- The old progress track was fixed at 94dp and visibly undersized relative to
  the reference phone. Its width is now approximately half the available
  footer column (166dp for a 333dp available region), capped at 184dp and
  never wider than the container. Cyan fill occupies 51% of that track.
- Preserve translated subtitle and footer, scroll-mode fallback at <640dp
  viewport height or >=1.5x font scale, and existing debug-only capture mode.

### 02 — Home / Scanner

- Keep live, on-device CameraX/ZXing preview, existing framing corners and
  working Gallery / Torch buttons.
- Add the missing **round 56dp floating flash button** at the camera viewport's
  top-right with 14dp padding and a navy translucent surface.
- Both flash controls use the existing single asynchronous CameraX toggle
  callback, are disabled if no camera/flash is available and wait for hardware
  success before indicating the new state.
- The new button carries a localized accessible label, toggleable state and
  `qrex-scan-floating-torch` test tag.
- Do not replace the live camera with the blurred laptop/plant picture from
  the marketing artwork; the actual scene must come from the user's camera.

## Verification

- `QREXSplashLayoutTest`: standard, compact, larger-font and progress widths.
- `QREXSplashReferenceTest`: actual Compose Splash element visibility,
  separation, bounds and track size.
- `QREXScannerControlsTest`: 48dp+ native actions, now including the
  top-right torch when the real camera is available.
- Required before merge: Kotlin/JVM tests, Android emulator instrumentation,
  Lint, R8 and 12 genuine responsive screenshot fixtures; compare screenshots
  on the same viewport, locale, display density, status/navigation insets and
  camera state. On devices without a flash, the control is correctly disabled.

## Unchanged

Application ID `com.brendigo.qrex`, versionCode 127, signing keys, history,
encrypted Wi-Fi records, migration formats, locale catalog, Play / F-Droid
metadata, camera/gallery decoding and all existing screens.
