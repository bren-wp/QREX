# QREX P02 — Android Flutter-to-native migration acceptance

Baseline: `84234819cf5b10fd18892d6392838f5cf9c4b714` (2026-10-18 audit). Scope: native Kotlin application only; no versionCode change.

## Behavior in this change
- The Flutter `flutter.qrex_history_v1` JSON is parsed completely before any replacement.
- Records are merged by exact QR payload; favorites are never demoted. Over-capacity (>250 distinct items), malformed dates/settings, unreadable native ciphertext and encryption failures abort without marking the migration complete.
- The native history JSON, migration marker and previously unset native display/history choices are written in one synchronous SharedPreferences `commit()`. Existing native language, theme and history settings win over legacy values.
- Saved Wi-Fi QR payloads are AES-GCM encrypted by Android Keystore before the commit. Original legacy history is deleted only after the native commit succeeds. If terminated after the native commit but before this cleanup, a new launch completes cleanup without repeating the merge or rotating ciphertext.
- Explicit Delete all remains intentionally destructive; it is not an upgrade/recovery instruction.

## Non-destructive recovery protocol
1. **Do not uninstall QREX, clear app storage or replace app signing identity.** Stop attempts to save new QR entries when unreadable/corrupted history is detected.
2. Record the installed app's package, versionCode and APK signing certificate fingerprints. Verify the old and proposed installer have the same package and Android-recognized signing lineage before attempting an in-place upgrade.
3. On a protected test device or cloned data environment, snapshot app-private preferences and relevant Keystore continuity before upgrade. Do not put decrypted Wi-Fi secrets into CI artifacts or GitHub.
4. If migration is interrupted before the atomic native commit, restart the app: legacy data remains in `FlutterSharedPreferences` and is retried. If interrupted after commit, native history and completion marker survive together; restart finishes stale legacy-history cleanup.
5. If the legacy JSON, native history or Keystore cannot be parsed/decrypted, preserve the installation and data, investigate offline with explicit user consent, and **do not uninstall**. The migration is not accepted merely because the screen opens.

## Validation matrix
| Scenario | Instrumentation coverage | Real signed upgrade |
| --- | --- | --- |
| Legacy history and favorites | `flutterV013HistoryMigratesWithoutLosingRecords`, `flutterFavoritePromotesExistingUnsavedScanWithoutDroppingRecord` | NIJE POTVRĐENO |
| 250 saved records, Wi-Fi ciphertext, user settings | `flutterMigrationAtomicallyPreservesExistingChoicesAnd250Favorites` | NIJE POTVRĐENO |
| Restart after native commit before legacy cleanup | `migrationAfterInterruptedLegacyCleanupDoesNotDuplicateOrEraseData` | NIJE POTVRĐENO |
| Malformed history/settings and capacity abort | `malformedFlutterHistoryMustNotBeDeletedOrMarkedMigrated`, `malformedLegacySettingsLeaveOriginalSourceAndNoCompletionMarker`, `migrationKeepsOriginalWhenHistoryHasNoRoomForNewRecords` | NIJE POTVRĐENO |
| Corrupt native ciphertext / duplicate Wi-Fi rows | `corruptEncryptedHistoryIsNeverOverwrittenByNewSave`, `duplicatePlaintextWifiAfterEncryptedRecordCannotSurviveMigration` | NIJE POTVRĐENO |
| Physical power-loss at storage boundary | Retry simulation only, not device power cut | NIJE POTVRĐENO |

### Mandatory external acceptance — still blocked
- Install the authentic historical Flutter APK (and separately the existing Play-delivered APK) without wiping data. Upgrade to the candidate signed with an Android-compatible **same signing identity/lineage**, recording pre/post digests, rows, saved flags and settings. The temporarily signed GitHub 1.0.1 APK **cannot** update Play-signed installations.
- Android instrumentation tests, JVM unit tests, lint, R8 release build/bundle, 23-language generation/parity, merged-manifest permission gate, 16-KiB bundle gate and emulator screenshot workflow must be green on this PR **final SHA**. GitHub CI is not a physical Android upgrade proof.
- Verify approved upload certificate fingerprint, available versionCode and actual track status in Play Console. Play signing key material must never be uploaded to GitHub issues or PRs. Do not submit a release until verified.

Status: **RELEASE BLOCKED — NIJE POTVRĐENO** for Play Console, actual Play-signed upgrade, physical device, hardware power-loss and no-data-loss acceptance. This PR only proposes the code/test change, not distribution.
