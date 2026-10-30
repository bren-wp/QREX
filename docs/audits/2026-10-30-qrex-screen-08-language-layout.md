# QREX Android — Screen 08: Language (native Compose)

Date: 2026-10-30. Work continues exclusively on open PR #80.

## Visual structure and exactness limits

The Language destination remains an actual full native Android screen, not a popup.
The heading and 52dp Back action stay outside the scrolling language list. A uniform
36dp glyph slot aligns the 23 flags and the System globe; names and single radio
selection line up in rows on dark and light appearances. Each row remains at least
64dp high, has consistent 14dp horizontal and 9dp vertical padding, and supports
up to two lines for long native names without overlaying its radio control.
The LazyColumn has bounded weight in its parent, 8dp vertical rhythm and 20dp
bottom content padding so the final Norwegian Bokmål entry is reachable above
the five-tab bottom navigation and window insets.

Original reference images in older discussions were perspective-transformed
marketing boards, not rectified identical-density screenshots. Numerical
per-pixel equality is **not yet verified**. Do not use layout source checks or
real emulator captures as evidence for exact zero pixel difference without
aligned unmodified target PNGs of matching dimensions, font scale and state.

## Functional guarantees

- All 23 offline languages and System remain; no new translations or assets.
- Selected state is exposed via semantics on the real clickable Surface, not
  a second click listener on the decorative RadioButton.
- Selecting a language still requires a committed local preference before
  QREX updates the displayed choice; failure displays the localized error.
- No changes to QR scans, favorites, history schema, Wi-Fi Keystore ciphertext,
  app identity, Play signing, analytics/network permissions or language catalog.

## Regression checks

- `QREXLanguageLayoutTest.languageRowsAreAlignedScrollableAndHeaderStaysAccessible`:
  authentic Compose layout widths and left edges, >=48dp touch dimensions,
  absence of overlap, ability to scroll to the last language, pinned Back.
- `QREXLanguageLayoutTest.languageSelectionPersistsAndCanBeRestoredWithoutClearingHistory`:
  live user selection via rows, selected semantics, fresh-store durable preference,
  unchanged history count, and restoration of prior language in a cleanup block.
- Native CI, 23-language checker, R8/Lint and connected Android API 26/31/34/35/36
  matrix remain mandatory. Existing real screenshot capture includes Language
  in compact, reference and large-font states and a 1080x1920 dark state.
- Previous CI failure in PR #80 was test-only: `assertDoesNotExist` is not
  available in this Compose test toolchain. Updated More test to use the
  supported `onAllNodesWithTag(...).assertCountEquals(0)` assertion.

## Next

Review exact-head CI and screenshot reports before merge. After Language passes
proceed to screen 09 (Export) within the **same** PR, without mixing unrelated
design changes into this screen.
