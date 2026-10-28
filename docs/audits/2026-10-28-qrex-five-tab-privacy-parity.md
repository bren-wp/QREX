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

## Additional history and export hardening

- **PDF consent snapshot:** before opening Android's Storage Access Framework document picker, the user-approved manually saved list is copied into an in-memory only snapshot. On return, the picker callback uses and clears that snapshot; it never reads a newly changed History list. If the Activity is recreated and loses the snapshot, the export fails closed rather than writing unrelated QR or newly saved Wi-Fi credentials. `QREXCoreTest.pdfSnapshotKeepsExactlyTheApprovedFavoritesAfterHistoryChanges` exercises the exact selection under changes to History.
- **Durable settings:** language and optional History preference changes now use checked, synchronous `SharedPreferences.Editor.commit()` and update the displayed Compose value only if the durable write succeeds. A failed save triggers a localized error toast. `QREXStorageTest.languageAndHistorySettingsCommitDurablyBeforeUiStateChanges` checks both choices across Store recreation. This preserves the user's previous value on a write failure rather than displaying a choice that may revert after restart.

## Reference-board versus actual Android layout review

Source boards in this conversation: **QREX_ Sve što može QR(8).png** (1672×941, six dark Android phone panels), **QREX – Skeniraj, izradi, podijeli(10).png** (1794×876, two light Android perspective renders), and **QREX_ Sve QR funkcije na jednom mjestu(9).png** (1536×1024, iOS-like dark concept). The dark Android board is the canonical native Android target; it and the light board cannot simultaneously define one fixed screenshot.

The actual 10-screen Android UI was inspected from the successful `QREX Android screen capture` workflow artifact on the QREX branch at commit `cc80eb8d` (1080×1920 per image). Confirmed visible structural differences, addressed in this PR:

- **More:** the old screen lacked the reference Theme card and showed an uneven two-column/two-row-plus-wide-About layout. It now uses six interactive cards in three equal-width rows, colored icon groups, and a distinct delete action. Theme offers System / Dark / Light and survives app restart. Native coordinate tests measure row alignment, gaps, and minimum touch targets.
- **History:** the old UI had two wide All/Saved chips above search, displacing results downward. A search-field-plus-filter button is now in one row. Its menu preserves both functions and accessibility, while the separate Favorites destination remains filtered. The filter button also reports whether All or Saved is currently selected to Android accessibility services, and its icon changes accent when Saved is active; an instrumented geometry test measures same-row alignment, minimum 48dp targets and non-overlap.
- **Bottom navigation:** five real localized destinations are preserved. Active indicator tints distinguish the destinations while the unselected glyph/text remain legible.

The reference board uses perspective-transformed phone screens with synthetic camera scenes, unknown density, fonts, status-bar dimensions and content. It is not a byte-aligned source for absolute pixel equality. New build screenshots are captured from the actual Compose app; the mobile camera feed itself is device-dependent. **Do not mark the complete suite pixel-perfect without same-device, same-state, perspective-corrected targets and a measured diff for each screen.**

All native destinations requiring screenshot and functional checking: Splash, Scan, Create (URL), Create (Wi-Fi), Result, History, Favorites, More, Language, and Export. Existing CI captures ten actual Android PNGs, plus compact/large-font responsive captures. In addition, the real Android screenshot pipeline now captures **Light Create, Light Scan and Light More** at 1080×2340 as actual Compose screenshots, covering the supplied light Android reference. The light-only screenshot override is allowed only for debug screenshot intents, without changing saved Theme or enabling fixture routes in production builds. Scanner, gallery, flash, all QR generators, SAF exports, privacy prompts and on-device data retention require behavior tests in addition to visual comparison.

## Unverified and non-goals

No claim of all-screen 1:1 pixel equality, iOS implementation, Google Play approval, hardware-specific camera performance, or absence of every potential security vulnerability is made. A full measured visual comparison requires aligned, unwarped target screen captures at known resolution, density, language and font scale. Release gating requires successful Android Kotlin, JUnit, instrumented device tests, P03 API matrix, language checks, Lint/R8 and real screenshot validation. Never publish a release from failing or incomplete CI.

## P03 Android API 34 SDK infrastructure triage

During PR #69 commit `af556669`, Android API 34 stress testing failed **before emulator boot**: GitHub's `sdkmanager --install emulator` returned `Error on ZipFile unknown archive`, then the action could not reach emulator TCP port 5554. This is a truncated/corrupt SDK download, **not an observed QREX exception or ANR**. The P03 workflow now runs a bounded, fail-closed emulator SDK preflight with a maximum of three retries for specifically recognized transient archive/transport failures. A missing executable, unknown SDK error, failed app test, genuine crash or ANR still fails CI. Synthetic retry tests verify recovery, three-attempt limit and nontransient fast failure; redacted failure categories are logged without dumping raw SDK transport URLs. Re-run all five live API jobs and check instrumented results before merging or publishing any new release.

**SDK lookup fix:** The first preflight incorrectly depended on `sdkmanager` being present on `PATH` before Android Emulator Runner initialization; API 26, 31 and 34 therefore failed before launching QREX. The preflight now locates SDK tools through `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager` (falling back to `$ANDROID_SDK_ROOT`). Unknown SDK/setup errors remain blocking. Test coverage verifies explicit SDK root resolution and Bash syntax. This does not yet prove that the full API matrix passes; new live runs are required.
