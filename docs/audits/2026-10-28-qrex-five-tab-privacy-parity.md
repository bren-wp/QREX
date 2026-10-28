# QREX Android — five-tab reference parity and clipboard privacy QA

## Grounding

Review of the three supplied QREX visual reference boards, using the **dark Android board** as the mobile navigation reference. It shows Scan, Create, History, Favorites and More as five reachable root destinations. The other boards include a contrasting light Android presentation and an iOS-like concept; they are **not** interchangeable pixel-aligned screenshots. Exact per-pixel comparison of perspective-transformed device mockups cannot be established from this material alone.

## Implemented on top of v1.0.18

- Android's bottom navigation uses five real accessible Compose destinations: Scanner (0), Create (1), History (2), Favorites (3), More (4).
- Favorites is a dedicated, saved-only History surface with real search, QR opening, removal and favorite toggling. It does not silently show unsaved records after Activity recreation or tab changes. The History All/Saved chips remain available under History.
- The previous Android navigation ID 3 maps to Favorites again. No persisted QR contents, history schema, Wi-Fi Keystore alias or stored app settings are migrated or reset.
- Existing labels come from the local Android translation resources (all 23 languages); five labels use compact sizing at narrow widths and large font scales.
- Compose instrumented tests now verify all five tabs, minimum 48dp targets, Favorites isolation, real navigation to More, screen controls and 30 Create/Result cycles.
- QR payloads copied to the system clipboard always carry Android's `EXTRA_IS_SENSITIVE` flag, not only Wi-Fi codes. URL tokens, contact cards and plain text can contain secrets as well. The text remains pasteable; this API flag hides Android 13+ system clipboard previews, **but does not encrypt clipboard contents, protect legacy Android clipboard managers or prevent another authorized app from reading a pasted value**.
- Genuine Android emulator screenshots, not reference illustrations or composites, are linked for all 10 screens from the verified GitHub v1.0.18 release in the public README. The changed Favorites tab requires **fresh** current-commit emulator screenshots and a subsequent release before users should rely on images to describe the updated UI.

## Unverified and non-goals

No claim of all-screen 1:1 pixel equality, iOS implementation, Google Play approval, hardware-specific camera performance, or absence of every potential security vulnerability is made. A full measured visual comparison requires aligned, unwarped target screen captures at known resolution, density, language and font scale. Release gating requires successful Android Kotlin, JUnit, instrumented device tests, P03 API matrix, language checks, Lint/R8 and real screenshot validation. Never publish a release from failing or incomplete CI.
