# QREX — archived GitHub release recipes (v1.0.1–v1.0.19)

These YAML files are **historical, already published** release recipes, preserved as text for reproducibility and audit. Their original GitHub tags, binaries, checksums and release notes remain unchanged. They are intentionally stored **outside** `.github/workflows/` so a new Android CI pass never schedules sixteen obsolete `workflow_run` pipelines, even as skipped runs.

Do not move a published version back into active workflows. For a future version, author **one** new audited publisher only after updating Android version metadata and its exact-commit verification gates. The CI test suite checks that every historical release recipe is present here and no published-version publisher remains active.

GitHub APK/AAB artifacts for historical versions may use disposable certificates and must never be used as a Google Play upload or assumed to upgrade an older installation in place.
