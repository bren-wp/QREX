# QREX v1.0.53 — security and dead-code audit

**Date:** 2026-11-20. **Scope:** Android production Kotlin, Android manifest, Gradle, Android tests, source security gate, release and screenshot workflows. Final test outcomes must be read from the exact GitHub CI commit; source inspection alone is not proof of a penetration test.

## Security checks

- Android manifest forbids INTERNET, network, location, advertising and broad file permissions; CAMERA remains user-initiated.
- Android Keystore with AES-GCM, input size bounds, no private QR payload console logging, explicit user consent before Wi-Fi QR sharing/saving, and independently validated HTTP(S) external URL dispatch.
- Imported gallery bitmaps and PDF pages have resource bounds. Import uses Android system pickers, not broad storage access; no background server upload or plaintext document cache is intended.
- CameraX callbacks close frames and reject orphan images after the UI is disposed. Crop revisions prevent stale worker results from overwriting a newer document; suggestions verify pending identity and revision before touching corners.
- APK/AAB signing checks, R8 shrinking, Android Lint, merged manifest permissions, 16 KiB native alignment, 23-language validation, five-API instrumentation matrix and genuine emulator screenshots are release gates.

## Dead code removed and retained

Removed the formerly reachable light/system MaterialTheme branches, user theme selector/dialog, persisted theme-mode API, unused header document icon and light screenshot capture/publisher branches. The legacy manual auto-edge button was removed because new camera/gallery pages request offline suggestions automatically.

The existing `tool/audit_android_security.py` scans all production Kotlin source and instrumented/JVM test files for lexical unused-function candidates. It **flags** suspects for human call-graph review rather than automatically deleting Kotlin callbacks, reflection targets or APIs. R8 performs an independent release-mode reachability pass. Historical immutable release notes/assets and unused translated theme strings remain archived for compatibility; they are not reachable appearance controls.

## Remaining independent verification

The source gate cannot establish complete binary dependency vulnerability coverage, dynamic penetration test findings, behavior on all OEM cameras, proof of no reflective dead code, or precise 1:1 RGB equivalence without flat reference images. Do not label these unperformed checks as passed. Screenshots use real app pixels and synthetic non-sensitive page content.
