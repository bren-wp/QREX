# QREX — Wi-Fi QR screen pass

**Date:** 2026-11-01  
**Pull request:** #83, `feat/splash-scanner-reference-v127`  
**Scope:** existing Create → Wi-Fi form, not a separate new Activity.

## Confirmed defects and improvements

1. The password field previously used the generic text IME. It now requests
   `KeyboardType.Password` to avoid personalized suggestions and ordinary
   text autocorrection while retaining password masking by default.
2. The Show/Hide control now has a minimum 48dp interaction target and
   separate stable Compose semantics test tags. The action still toggles
   `PasswordVisualTransformation` and does not write passwords to logging.
3. The shared form previously called `content.trim()` for every category.
   Wi-Fi SSIDs may legitimately contain leading/trailing spaces; removing
   those spaces can generate a QR code for the wrong network. Wi-Fi input now
   preserves the exact entered string; other QR categories retain trimming.
4. Existing privacy consent for Wi-Fi PNG export remains mandatory and pins
   the originally approved payload. Keystore-encrypted user history, favorites
   and saved data are unchanged.

## Verification

- `QREXCreateInputTest` checks exact SSID round-trip through encoding and
  parsing, ordinary URL/text trimming, and whitespace-only SSID validation.
- `QREXCreateHeaderTest` checks real Show/Hide interaction, label change
  and minimum touch target on a running Android emulator.
- `tool/test_create_reference.py` guards password-specific keyboard input,
  default masking, tag stability, and retention of the explicit Wi-Fi export
  consent flow.
- Existing Kotlin/JVM tests, Lint/R8, 23-localization checks, actual captured
  12-screen verification, and the five-API device matrix remain required.

## Pixel-fidelity limitation

The supplied reference is a composited marketing board with perspective phone
frames, so true rectified baseline pixel comparisons are not yet possible.
These changes implement actual Compose UI and interaction improvements, not
rendered marketing images, and do not claim verified 1:1 equality.

## Release gate

QREX `v1.0.27` (`versionCode 127`) remains a candidate until exact-commit
CI, real device screenshots, version checks, signatures and published GitHub
release artifacts have been confirmed. CI-only signatures cannot authorize
Google Play production updates.
