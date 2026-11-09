# QREX — Reference, functional, dead-code and security review (2026-11-09)

## Scope and evidence

Baseline: `main` at `4d493943216d5f10ef1cde30a5c1248fa4f00d25` (QREX 1.0.42, Android package `com.brendigo.qrex`), plus fixes on `fix/qrex-qr-result-native-actions-nov2026`. The single supplied marketing board contains six perspective handset renders, a brand icon family and feature captions. An aligned screenshot reference is not supplied, so numerical pixel-perfect error **cannot be reported**. Existing CI produces 12 real Android captures; those captures establish actual UI appearance but cannot by themselves prove zero difference from the angled composite.

This source review covers the core Android files, result/create UI flows, scanner/gallery, manifest, Gradle/release automation and the existing 1.0.42 security/dead-code report. Full device penetration testing, all OEM cameras, live Play upload signing and exhaustive third-party binary vulnerability analysis are **not** performed in this change.

## Screen-by-screen difference priorities

| Priority | User reference | Source-level assessment and next acceptance step |
| --- | --- | --- |
| P0 functional | Links, Wi-Fi, contacts, email, phone, text, locations, calendar | Result previously had native primary actions only for safe HTTP(S) URL and calendar. This branch adds safe, explicit email/dialer/maps/contact actions, and fixes place-name geo generation. Test each with real installed target apps. |
| P1 visual | Scanner with large cyan/white corners, gallery and torch | Existing CameraX + ZXing remain real and local. Camera subject/crop is device-dependent; don't fake the blurred laptop from the marketing mockup. Compare native 393dp capture against rectified reference once available. |
| P1 visual | Splash with centered QREX mark, deep-blue layered waves, footer | Existing vector brand and responsive splash. Exact relative size/baseline and OS insets still require flat same-device reference. |
| P1 visual | Result standalone navigation, rounded summary and six actions | Standalone Result, pinned Back header and no root bottom bar already landed in 1.0.42. New native type actions appear only for those QR formats, leaving the URL example stack unchanged. |
| P1 visual | Create chips, URL input, QR preview, Generate + Save | Four primary types are visible; other four are advanced. True encoded payload/preview remains produced locally. For a place label rather than coordinates the new valid `geo:0,0?q=...` format opens map search. |
| P2 visual | History search, filters, colored type icons, relative date cards | Current Compose implementation has local search/filters, history row actions and saved records; populated screenshot data can vary. Need flat pixel-aligned captures for precise spacing/font/icon matching. |
| P2 visual | More 2x3 settings cards + separate destructive row | Already adjusted in 1.0.41 and responsive: 2 columns at standard handset widths, one column for narrow/landscape/high font scaling. |
| P2 visual | Launcher + dark/light/iOS-style icon variants | Android adaptive launcher assets exist; illustrated iOS/App Store variants in the marketing board are not evidence of a released iOS app. OEM launcher masks make universal pixel identity impossible. |

## Security delta

- Email action uses `ACTION_SENDTO` with an explicit validated single recipient. QR-supplied `cc`, `bcc` and other mailto headers are deliberately ignored.
- Phone action uses **`ACTION_DIAL`, never `ACTION_CALL`**; star/hash USSD strings are excluded and no phone permission is requested.
- Map action accepts finite, in-range coordinates with an optional single `q=` search parameter; malformed URI characters and non-finite coordinates are refused.
- Contact action opens the system contact **editor** using only parsed name, number and email. The application does not directly write contacts or request contact permissions.
- All actions require explicit on-screen selection. Failed dispatch reports a user-visible error; URL and calendar handlers remain separate.
- Calendar text field parsing now handles folded lines and parameterized properties from other standards-compliant producers.
- No changes to QREXStore history format, Android Keystore AES-GCM records, Wi-Fi export consents, QR image generation path or app's minimal manifest permissions.

## Dead-code and dependency observations

- This branch adds one production file and one JVM regression test. Existing `tool/audit_android_security.py` enumerates all `*.kt` production files dynamically and reports unreferenced candidates; it is a lexical/review scanner, **not** proof of runtime reachability or a complete dead-code elimination certificate.
- Existing Compose preview, scanner and release/source-contract tests must continue passing; avoid removing apparently unreferenced callbacks invoked through Android/Compose frameworks.
- Existing 1.0.42 audit documents R8 release shrinking, no network permission in merged manifest, immutable historical publisher archives, CI disposable-signing limitations and vendor advisory residual risk. Those safeguards remain unchanged.

## Verification / pending acceptance

1. JVM regressions: email, phone, geo and vCard action classification; malformed/USSD/address rejection; map search round-trip; parameterized/folded iCalendar field parsing.
2. PR-head Android Kotlin/JVM tests, Lint and R8 with updated dependencies.
3. Android instrumentation/API 26/31/34/35/36 and 23 locale checks, including 12 authentic screenshots on the same PR head.
4. Manual checks in Gmail-compatible mail clients, dialers, map apps, contacts editors, airplane mode, permission-denied scanner, large fonts, compact and landscape devices.
5. For strict visual comparison: obtain unframed reference images and identical status/navigation bars, pixel dimensions, density, locale, font scale and content state, then run `tool/compare_ui_pixels.py` with a documented threshold.

**Status:** Code and regression tests proposed in a development branch. No certification of 0% pixel deviation, successful physical-device QA, complete penetration-test coverage or Google Play upload signing is implied.