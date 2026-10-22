# QREX 1.0.6 — Android visual and functional regression audit

**Scope:** source review against the four user-provided concept boards on 2026-10-22, plus actual Android 1080x1920 emulator screenshots from prior PR build. Native Android only. **Not** a claim of pixel-for-pixel parity on every device or an independent physical-device test.

## Source-level issues found and corrected

1. **Launcher vs splash artwork mismatch:** Compose splash previously redrew a separate simplified monochrome mark. It now shows the actual full-color adaptive icon source. Launcher, themed icon and branded SVG were updated toward the blue/cyan Q/QR reference, and splash badge enlarged and rounded.
2. **Create QR large blank gap:** the actual QREX emulator screenshot exposed unused space between the QR preview and Generate/Save. Preview is now in the same bottom action group. It is hidden during keyboard display so the scrollable form remains usable; live Generate/Save controls stay pinned.
3. **History/Favorites icons:** list rows previously hid the favorite action inside overflow. A directly tappable heart now calls `QREXStore.setSaved` and has a localized accessibility description. The overflow remains for secondary actions.
4. **Unicode QR payloads:** ZXing's QR encoding now explicitly uses `EncodeHintType.CHARACTER_SET = UTF-8` so `č ć đ š ž` and other Unicode characters can be accurately decoded.
5. **Contact QR completeness:** Create now supports name, phone and email; vCard 3.0 text escapes backslashes, commas, semicolons and newlines and can be edited. Added round-trip unit and Android bitmap tests.
6. **Calendar DatePicker crash:** replacing the year/month on the existing 31st day could temporarily produce an impossible `LocalDateTime`; the new date is now constructed atomically before restoring the time.
7. **Experimental Compose IME API:** new keyboard handling initially caused a compile failure for using an experimental layout property. Replaced with stable `WindowInsets.ime.getBottom(LocalDensity.current)` and retains a source regression check.
8. **Unused Kotlin import:** removed the obsolete splash MaterialTheme import.
9. **Camera screenshot authenticity:** emulator screenshot workflow now requests a virtual-scene back camera (instead of an emulated flat color test pattern) while capturing the actual CameraX UI. This still needs green CI to verify software-renderer compatibility.

## Preserved invariants

- App ID `com.brendigo.qrex`; Android version `1.0.6` / code `106`.
- Offline CameraX/ZXing live and gallery detection; no new INTERNET or broad storage permissions.
- 23 languages and explicit light-theme preference; dark-first visual identity.
- AES-GCM/Android Keystore Wi-Fi protection, record retention/favorites, Flutter Android legacy migration, PDF/ZIP/PNG export.
- Existing Play upload-key identity is not modified and secrets are never checked in.

## Acceptance gates (not inferred from source alone)

1. Android native CI: Kotlin compile + unit tests, lint, R8, APK/AAB, merged manifest and dependency inspection.
2. Emulator screenshots: eight actual captures, screenshot structure and foreground UI identity checks. Visually compare Create, Favorites, Splash with the reference phones; automated structure checks are not a true image pixel diff.
3. P03 lifecycle stress matrix: Android API 26 / 31 / 34 / 35 / 36.
4. Only after exact-commit green checks and review: merge PR, rerun main-commit gates and publish the `v1.0.6` GitHub release assets and checksums.
5. Physical devices, Play Console checks, original authorized Play signing and 1:1 OEM mask comparison require separate validation.

## Distribution warnings

The GitHub release workflow uses a **disposable certificate**. Its APK **cannot replace** an existing differently signed Play/GitHub installation. Uninstall can erase private QR data. The public GitHub AAB is **not Play-ready**. The release's separately labeled QA AAB is **unsigned**. For Play Console use only the protected authorized existing upload-key path and confirm code 106 is unused.

**Status:** changes committed on PR #38; quality gates must be read from the exact final commit. This document never substitutes for test logs.
