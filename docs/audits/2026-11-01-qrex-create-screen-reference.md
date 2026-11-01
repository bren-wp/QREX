# QREX — screen 03: Create QR fidelity and QA (2026-11-01)

## Grounding and scope
The user-provided QREX reference is a composite with multiple perspective-framed
phones, not a flat PNG baseline aligned to emulator pixels. This change improves
the actual Compose screen to match its observable layout. No claim of a proven
mathematical 1:1 pixel match is made.

## Changed production behavior
- Navigation header: centered localizable "Create QR code" title; working
  Back icon (returns to Scanner), and overflow menu (48dp hit targets).
- E-mail, phone, location and event QR categories remain selectable from the
  overflow. The separate "More" disclosure still exposes all advanced chips.
- Four quick choices remain Link/Text/Wi-Fi/Contact; on compact displays they
  use the existing 2×2 accessible layout instead of clipped chips.
- QR preview is a compact centered card with the actual generated QR bitmap.
  The caption is beneath it; the card does not repeat the QR payload, which
  can contain private contact details, passwords or tokenized URLs.
- Generate and Save actions remain genuine, independently enabled callbacks,
  kept outside the scrollable form and away from the soft keyboard.
- No modifications to stored history/favorites, Keystore, package ID,
  permissions, SDK levels, camera, gallery or release signing credentials.

## Regression coverage
- `QREXCreateHeaderTest`: visible 48dp header targets, selectable genuine
  overflow QR type, Back navigation to the live Scanner screen.
- `tool/test_create_reference.py`: Create-specific hierarchy, action ordering,
  preview size and no echo of raw payload.
- `tool/test_ux_dead_code.py`: revised deprecated assumption from the old
  Create app bar while retaining navigation and accessibility assertions.
- All existing Kotlin/JVM, Lint/R8, localization, instrumented API 26/31/34/35/36,
  and actual 12-screen capture checks remain mandatory before merge.

## Acceptance and release
Screen parity must be reviewed visually against rectified flat references at
the same resolution/density if available. This PR prepares v1.0.27/127 but
GitHub Release may be published only after exact-main green CI and signature,
SHA-256 and 12 authentic emulator screenshot checks. Public GitHub signatures
cannot replace the authorized Google Play upload key.
