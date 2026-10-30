# QREX Android — Favorites screen refinement (30 October 2026)

## Scope

The **Favorites** destination is the next isolated screen after the existing Scan / Create / Result / History / More design work. It reuses the existing dark-first native Compose styling and localized labels, without introducing a new rendering stack.

## Changes

- Removed the per-item three-dot overflow menu on **Favorites only**. It duplicated the two actions already available: tap the card to open its real QR Result; tap the 48dp heart to remove its saved flag.
- Kept the **History** per-item menu and its confirmation-backed permanent Delete action. Nothing is removed from History when the favorite heart is tapped on the Favorites screen.
- The empty Favorites card now uses the Favorites icon and semantic violet tint instead of the History clock.
- Favorites rows show only the localized timestamp beneath their short label instead of showing full QR payloads, including sensitive URL query tokens. The QR Result still opens on tap for explicitly requested full details.
- The Favorites heart uses violet (saved state) rather than the destructive red reserved for true deletion.
- Stable, non-payload UI test tags distinguish Favorites row, unsave control and empty state; the Result's existing favorite action also receives a stable semantic test tag.
- Added a real device UI regression that creates and saves a synthetic QR, opens it from Favorites, unsaves it and verifies it still exists in History. It also verifies the absent duplicate menu/filter and the 48dp touch target. The regression verifies that a synthetic URL query token is not printed in Favorites, and cleans up only its own test record in `finally`, never wiping unrelated history.

## Unchanged

- Package ID `com.brendigo.qrex`, database schema, Keystore Wi-Fi encryption, user history, settings, favorites, camera permissions, 23 languages, scanner and QR generation, bottom navigation, Android target, release code and signing.
- Debug-only screenshot fixtures remain in-memory and cannot be used from production builds.
- Local-only QR handling, offline operation and manual consent before destructive deletion are preserved.

## Validation required

Run Kotlin/JUnit + Android Lint/R8, the connected Compose instrumented tests including `QREXFavoritesScreenTest`, all existing required API matrices, and capture **real** emulator screenshots for Favorites at reference width, 320dp, large-font and optional light appearance. Confirm no overflow, keyboard occlusion or touch target clipping before merge. A mathematically measured **pixel-by-pixel 1:1** claim is impossible from perspective-transformed marketing mockups without flat reference PNGs of matching density, insets and UI state.

This is a UI-only PR: do **not** mint a new production version or reuse an unauthorized Google Play signing key before the normal release gates pass.
