# QREX 1.0.0 — Android screenshot emulator stall investigation

**Date:** 17 October 2026  
**Baseline:** `main` `e9abea2ff717afa178e8e22b74a6f89b26627758`  
**Affected run:** [Capture verified QREX screens #38992923435](https://github.com/bren-wp/QREX/actions/runs/38992923435)

## Evidence from the failed run

- The Android debug application built successfully.
- Android emulator instrumentation completed **18 of 18 tests, 0 failed**.
- The Android emulator reported `Failed to find ColorBuffer` and `Failed to bind to texture: invalid color buffer` during graphics rendering.
- After the app was reinstalled and QREX started, the screenshot operation ceased making progress until the GitHub Actions job was cancelled approximately 50 minutes after startup.
- The Android screenshot upload step did not run; the iPhone simulator screenshot job passed.
- Kotlin/Swift compilation and Flutter CI for the same main SHA passed, as did the localization workflow.

These facts support an **emulator rendering or ADB/UI automation hang**, not proof of an actual QREX runtime crash on a physical Android device. A real device crash cannot be ruled out without an exact app crash log or reproduction steps.

## Confirmed CI weaknesses

1. Screenshot capture used `-gpu swiftshader_indirect`, a graphics mode now deprecated by the Android Emulator.
2. The `uiautomator dump` command had a timeout but `adb exec-out cat`, `adb exec-out screencap`, installation, launch and wait-for-device could hang indefinitely.
3. When an ADB command stalled, the entire job could be cancelled without producing useful bounded failure diagnostics or a clear screenshot-specific error.

## Fixes

- Use the documented `-gpu software` rendering mode and enable KVM device access when available.
- Set explicit timeouts for all device commands, instrumentation, installation, launch, XML retrieval and screenshot capture. Commands killed by the timeout are treated as failures.
- Clear stale screenshots before starting; if checks fail, delete incomplete files and exit nonzero.
- Retain the foreground accessibility-hierarchy validation, anti-ANR checks and actual PNG validation. Never manufacture or substitute screenshots.
- Emit minimal non-sensitive emulator state diagnostics on failure, without uploading unrestricted logcat contents that might expose QR data.
- Add workflow tests to catch missing bounds or regression to a deprecated GPU mode.
- Remove references to the deleted RC publishing workflow. This work does not publish any new RC build or weaken the `1.0.0` signed-production policy.

## Remaining acceptance

A successful fresh Android screenshot run on the final PR SHA is still required. A passing screenshot and instrumentation check do not replace physical-device camera/gallery/lifecycle/migration testing or production signing. If the user is experiencing an **actual app crash on a phone**, obtain Android logcat's relevant exception and reproduction steps before asserting its cause.
