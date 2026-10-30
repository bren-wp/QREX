# QREX Android — screen 10: About modal / O aplikaciji

**Branch and PR:** `ui/favorites-compact-single-action-20261030`, open GitHub PR #80. Implemented on 2026-10-30. This phase follows screen 09 (Export).

## Target and visual refinements

The About destination is a **real Material 3 dialog opened from More**, not a separately navigable page and not a hard-coded marketing screenshot. It now has a clear typographic hierarchy: localized title; one bordered native brand panel with the same QREX vector brand mark used by the app; centrally aligned installed-app version and localized tagline; then publisher and official domain.

The installed version is read from Android PackageManager and is not fabricated or hardcoded. The dialog preserves both real actions: 48dp minimum height **Close** and an explicit secondary **Open link** action for `https://brendigo.com`. Merely opening the dialog never opens a browser.

The About body scrolls if needed at larger font scale or with long translations, but the dialog's header and action buttons stay accessible. The native design tokens, corners, border, navy / light appearance and translated labels remain unchanged. The tagline wraps rather than truncates.

## Actual screenshot evidence

Debug-only Android activity screenshot routing now recognizes `about`; this renders the *real* About dialog over More, not a fake illustration. The production build rejects screenshot-only extras, and the route does not change persisted history/settings.

Android CI must capture `docs/screenshots/android-about.png` plus `responsive/{compact,reference,largefont,light}-about.png` using the real emulator screenshot pipeline. The hierarchy gate requires localized About, branded QREX, and Close, while the standard-size shot also verifies the publisher is visible. For large font the publisher may be below the scroll fold, so the responsive gate verifies the fixed controls instead. Generated captures are CI artifacts only; this PR does **not** add fabricated static reference screens.

Screenshots of the user-supplied reference concept boards are not flat, registered equal-resolution PNGs in this repo. Therefore **literal zero-difference pixel-perfect 1:1 has not been measured or certified**. Align original PNGs and genuine capture dimensions/font/theme first, then use `tool/compare_ui_pixels.py`.

## Regression / security

- Extended `QREXMoreAboutTest` to inspect real About dialog, branded content, installed version, both 48dp action bounds, and proper dismissal back to More. Tests never open an external browser.
- `tool/check_android_screen.py` and `tool/test_android_screen.py` reject ordinary More and broken/unbranded modal screenshots masquerading as About. Screenshot workflow and screenshot regression tests now demand an eleventh real Android screen and responsive shots.
- No changes to QR scanning, 23-language catalog, saved QR history, Wi-Fi Keystore encryption, app package ID, permissions, backend dependencies, Play versionCode, or signing keys.
- **Required before merge:** exact-head Kotlin compile, JVM checks, Android connected tests across API 26/31/34/35/36, screenshot capture, localization and Lint/R8; inspect CI failures rather than asserting a pass without evidence.

Next: resolve any failing CI test on the same PR and compare actual Android emulator renders against the user reference screens. No release is ready merely because source changes were committed.
