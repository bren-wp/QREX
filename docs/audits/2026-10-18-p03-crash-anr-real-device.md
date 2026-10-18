# QREX P03 — crash, ANR, lifecycle and real-device acceptance

**Date:** 2026-10-18. **Base:** main `e049bef3add53119c406d95b252de5dbdeba0472`. **Scope:** native Android Kotlin/Compose only. Audit findings QX-03 / QX-08; no signing/version change.

## Incident verdict

**App crash: UNCONFIRMED — NIJE POTVRĐENO.** No reproducible `FATAL EXCEPTION` with `Process: com.brendigo.qrex` was supplied. Do not state an observed crash was fixed. In the earlier [emulator-stall audit](2026-10-17-android-emulator-screenshot-stall.md), `Failed to find ColorBuffer` and ADB/screenshot hangs were emulator/host symptoms, not a proven app crash. A later `Publish QREX 1.0.1 GitHub APK + AAB` [run #39161563777](https://github.com/bren-wp/QREX/actions/runs/39161563777) fails the existing-release gate, not runtime startup.

### Source-level risk addressed (preventive, not trace-confirmed)
- Create/Result `produceState` generated native `Bitmap` buffers on each QR payload without explicit disposal. The new producer `awaitDispose` recycles its owned bitmap on composition exit, payload change, or recreation. Do not share a reclaimed preview bitmap with asynchronous SAF export; PNG export renders a separate bitmap on `Dispatchers.IO` and releases it in `finally`.
- Navigation page index survives configuration recreation with `rememberSaveable`; sensitive QR contents and saved Wi-Fi secrets are **not** added to `savedInstanceState`. The existing encrypted local history is untouched.
- Synthetic QR navigation regression: 30 Create → Result → Close cycles. `ActivityScenario.recreate()` tests Activity recreation, **not** operating-system process death or actual hardware rotation.

## Automated Android compatibility matrix
`.github/workflows/p03-stability.yml` is a read-only pull-request/manual CI job with x86_64 Pixel 6 emulator profile, five Android API levels and a constrained emulator memory setting on API 36.

| Android | API | CI memory | Scope |
| --- | --- | --- | --- |
| 8.0 | 26 | 2048 MiB | 30-cycle Compose synthetic QR test |
| 12 | 31 | 3072 MiB | same |
| 14 | 34 | 3072 MiB | same |
| 15 | 35 | 3072 MiB | same |
| 16 | 36 | 1536 MiB | same (RAM-constrained emulator, **not** a physical low-RAM phone) |

The existing `capture-screenshots.yml` runs full Android instrumentation and a genuine Create-screen emulator screenshot on API 34. `native-platforms.yml` separately checks unit tests, lint, R8 release APK/AAB, final merged manifest permissions, 16-KiB libraries and release metadata. Localization workflow checks all 23 languages. Evaluate results only for the final PR head commit; queued/cancelled/failed checks are never passes.

On each stress emulator the workflow captures Android's **crash ring buffer** after running the tests. `tool/p03_crash_triage.py` outputs only exception *class* and QREX stack method/file/line for QREX `FATAL EXCEPTION` blocks; full exception messages, Wi-Fi strings, URLs, intent extras, other processes and the raw logcat are excluded. Sanitized summaries are uploaded as CI artifacts. This does **not** collect ANR traces and an empty crash buffer does not prove the app never crashed.

## Physical acceptance still required — NIJE POTVRĐENO

Use owned, authorized devices with preserved user data. A minimum representative run must include a Pixel, a Samsung Galaxy and a real low-RAM phone, together spanning Android 8, 12, 14, 15 and 16. Track make/model, OS/build, memory, application package/cert fingerprint, permissions, test operator and start/end timestamps. An emulator profile or memory option does not prove Galaxy/low-RAM OEM stability.

1. On each device, run **30 full live camera Scan → Result → Back cycles** with real QR targets (include Wi-Fi with private fields; never publish its value); then 30 Create → Result → Back cycles. Confirm the camera unbinds, torch shuts off, no blank/locked preview and no dropped output. The CI's synthetic Create cycle does not substitute for scanning.
2. Deny camera permission, open gallery without camera permission, select valid/invalid/very large EXIF-rotated photos, cancel picker and repeat. Grant camera in system Settings and return; verify permission restore and live preview.
3. Test rotation in Scan, Result and export picker, including repeated portrait/landscape changes, system font scaling and background/foreground switches. Compare display on Pixel, Galaxy and low RAM.
4. Test true process death after putting app in the background (capture PID before/after; `am kill` only requests kill and must be verified; do **not** use `pm clear` or uninstall). Check that saved history/favorites/Keystore Wi-Fi content remain intact. The current transient unsaved Result view is not guaranteed to resume after process death.
5. Test SAF PNG/PDF/ZIP to permitted and revoked destinations, cancellation, disk-full/storage errors, background interruption, 250 saved items, and compare exported file content. PDF/ZIP exports are plaintext by design and should not enter CI log artifacts.
6. Force-stop/restart without clearing app data; repeat on a memory-constrained handset under realistic background pressure. Collect crash/ANR logs privately and redact before sharing. Measure `dumpsys meminfo` across iterations, verify no sustained increasing allocation trend.

### Evidence commands (private test host; never commit raw output)
```bash
adb devices -l
adb shell getprop ro.build.version.sdk
adb shell getprop ro.product.model
adb shell dumpsys package com.brendigo.qrex
adb shell pidof com.brendigo.qrex
adb logcat -c
# Perform reproduction; then:
adb logcat -b crash -v threadtime -d > qrex-crash-PRIVATE.log
python3 tool/p03_crash_triage.py qrex-crash-PRIVATE.log qrex-crash-REDACTED.md 34
rm -f qrex-crash-PRIVATE.log
adb shell dumpsys meminfo com.brendigo.qrex
adb shell dumpsys activity exit-info com.brendigo.qrex  # Android 11+
adb bugreport qrex-device-PRIVATE.zip
```
For ANR, retain private `bugreport` and `logcat -b events -v threadtime` and look for `am_anr`, app PID, blocked main thread and competing binder/I/O; share only a manually reviewed, redacted subset. Android 8 does not provide modern `exit-info`, so record `logcat`, process PID and `dumpsys` instead. Capture `adb shell dumpsys activity activities` if you must differentiate app focus from frozen emulator/ADB. Strip private QR content, SSIDs, passwords, URIs and account/person data.

## Classification / acceptance

- **Confirmed app crash:** `Process: com.brendigo.qrex` + Java exception/NDK tombstone associated with test build + minimal steps + affected SHA + failing-before / passing-after regression. A Kotlin catch-all alone is NOT an accepted fix.
- **Confirmed ANR:** app PID and timed-out main thread in ANR trace/system evidence; provide sanitized blocked stack and repro.
- **Infrastructure issue:** emulator GPU or ADB timeout with no matching app crash/ANR trace; attach hosted-run link and host symptoms, do not disguise as app test pass.
- **Unconfirmed:** no matching evidence or test not physically run. This is the current physical-device verdict.

**Release gate: BLOCKED — NIJE POTVRĐENO.** Five API CI tests, screenshot, localizations, merged manifest, lint and R8 must all pass on the final SHA. Real Pixel/Galaxy/low-RAM, hardware permission lifecycle, genuine process death, actual SAF provider failures and Play Console remain unverified until reproducible records exist. Do not increase versionCode, uninstall QREX, touch signing keys or distribute a temporary GitHub AAB to Play.
