# QREX v1.0.26 — API 31 Result / IME navigation CI correction

**PR:** https://github.com/bren-wp/QREX/pull/81
**Target:** Android API 31 (Android 12), same release branch, versionCode 126.

## Observed CI failure

At HEAD `4f9ad00b16f9518098e9c7d28e173f880ce64f5a`, workflow `QREX P03 Android API lifecycle matrix` job `P03 API 31 / 3072MB emulator` (run `41133474374`, job `124330119692`) failed two genuine Compose tests:

1. `QREXNavigationStressTest.referenceResultCardAndActionsRemainRealAccessibleControls`: after a synthetic Generate tap, the Create screen remained visible and the `qrex-result-summary` element was absent.
2. `QREXNavigationStressTest.allFiveTabsStayReachableOnResultWithoutOverlappingPinnedActions`: Result scroll bounds were measured overlapping the pinned bottom NavigationBar during the transition.

The same exact SHA's API 26, 34 and 35 emulator jobs completed successfully, along with the native Kotlin CI and the real screenshot workflow. The sanitized API 31 crash log reported zero app fatal exceptions and zero ANR events, so this is a UI synchronization/insets failure, not proof of a crash.

## Production change

Create's real Generate button explicitly calls `LocalSoftwareKeyboardController.hide()` before clearing focus and navigating to Result. `QREXApp` repeats the IME dismissal and clears focus before committing the new current QR state. This addresses Android 12's asynchronous IME/focus and NavigationBar remeasurement. It does not alter QR content, secrets, scanning, persistence, security permissions or export consent.

## Regression strengthening

- The five-tab Result test now waits for a valid enabled Generate action, performs a physical Compose tap, and waits until the **actual** scroll viewport is fully above the bottom tab bounds. The final assertion still strictly rejects overlap with a 1dp rounding tolerance. Its failure message reports only geometric bounds, not private QR payloads.
- The Result summary test uses the same accessible Generate `OnClick` semantics path exercised by the 30-cycle navigation test to avoid an Android 12 keyboard overlay consuming a synthetic coordinate tap. Separate physical tap checks remain mandatory.
- No test was skipped or removed. All 63 real connected instrumentation cases are still executed on each P03 API, including privacy and history regressions.

**Status:** implementation committed; exact-new-HEAD CI must still pass all required Android emulator jobs before merging or publishing. The screenshot comparison to flat original reference PNGs remains a separate, unmet acceptance requirement.
