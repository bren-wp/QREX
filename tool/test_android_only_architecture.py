#!/usr/bin/env python3
"""Guard against resurrecting retired app platforms or build pipelines.

This is a source-level structural audit, not a promise that every dependency
or user journey is crash-free. Android migration readers must stay in place.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOWS = ROOT / ".github/workflows"
KOTLIN = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


class AndroidOnlyArchitectureTests(unittest.TestCase):
    def test_discontinued_native_ios_and_flutter_runtime_are_deleted(self):
        for name in (
            "platforms/ios", "lib", "test", "pubspec.yaml",
            "analysis_options.yaml", "tool/sign_ios_production.sh",
            "tool/prepare_platforms.py", "tool/verify_platforms.py",
            "docs/screenshots/ios-create.png", ".github/workflows/mobile.yml",
        ):
            with self.subTest(path=name):
                self.assertFalse((ROOT / name).exists(), name)

    def test_only_android_jobs_and_signed_android_release_remain(self):
        native = (WORKFLOWS / "native-platforms.yml").read_text("utf-8")
        screenshots = (WORKFLOWS / "capture-screenshots.yml").read_text("utf-8")
        publish = (WORKFLOWS / "production-release.yml").read_text("utf-8")
        gate = (ROOT / "tool/verify_production_gate.sh").read_text("utf-8")
        for name, source in (
            ("native", native), ("screenshots", screenshots),
            ("release", publish), ("gate", gate),
        ):
            with self.subTest(workflow=name):
                self.assertNotIn("\n  ios:\n", source)
                self.assertNotIn("platforms/ios/", source)
                self.assertNotIn("QREX-iOS-1.0.0.ipa", source)
                self.assertNotIn("tool/sign_ios_production.sh", source)
                self.assertNotIn("mobile.yml", source)
        self.assertIn("needs: verify", publish)
        self.assertIn("QREX-Android-${{ inputs.version }}.aab", publish)
        self.assertIn("production/SHA256SUMS-PLAY.txt", publish)
        self.assertIn('jarsigner -verify "$bundle"',
                      (ROOT / "tool/sign_android_production.sh").read_text("utf-8"))
        self.assertIn("environment: production", publish)
        self.assertIn("completed:success", gate)

    def test_old_flutter_android_data_migration_is_not_dead_code(self):
        core = (KOTLIN / "QREXCore.kt").read_text("utf-8")
        instrumentation = (
            ROOT / "platforms/android/app/src/androidTest/java/com/brendigo/qrex/QREXStorageTest.kt"
        ).read_text("utf-8")
        self.assertIn('getSharedPreferences("FlutterSharedPreferences"', core)
        self.assertIn("migrateFlutterV1()", core)
        self.assertIn('"flutter.qrex_history_v1"', core)
        self.assertIn('QREXSecretCipher.encrypt(item.raw)', core)
        self.assertIn("flutterV013HistoryMigratesWithoutLosingRecords", instrumentation)
        self.assertIn("flutterFavoritePromotesExistingUnsavedScanWithoutDroppingRecord",
                      instrumentation)

    def test_localizations_generated_exclusively_for_android(self):
        generated = (ROOT / "tool/generate_localizations.py").read_text("utf-8")
        usage = (ROOT / "tool/check_translation_usage.py").read_text("utf-8")
        self.assertNotIn("IOS =", generated)
        self.assertNotIn("QREXStrings.swift", generated)
        self.assertNotIn("platforms/ios/", usage)
        self.assertIn("platforms/android/app/src/main/res", generated)


if __name__ == "__main__":
    unittest.main()
