#!/usr/bin/env python3
"""Regression checks for the authorized Google Play upload workflow."""
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]


class PlayUploadPipelineTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = (ROOT / ".github/workflows/production-release.yml").read_text("utf-8")
        cls.signer = (ROOT / "tool/sign_android_production.sh").read_text("utf-8")
        cls.gate = (ROOT / "tool/verify_production_gate.sh").read_text("utf-8")

    def test_compatible_android_toolchain(self):
        self.assertIn("gradle-version: '9.9.0'", self.workflow)
        self.assertIn("bash tool/install_android_compile_sdk.sh", self.workflow)
        self.assertNotIn("gradle-version: '9.1.0'", self.workflow)

    def test_source_driven_version(self):
        self.assertIn("id: metadata", self.workflow)
        self.assertIn("steps.metadata.outputs.version", self.workflow)
        self.assertIn("needs.verify.outputs.version", self.workflow)
        self.assertNotIn("inputs.version", self.workflow)

    def test_supported_agp9_unit_test_task(self):
        self.assertIn(":app:testDebugUnitTest :app:lintRelease", self.signer)
        self.assertIn(":app:assembleRelease :app:bundleRelease", self.signer)
        self.assertNotIn(":app:testReleaseUnitTest", self.signer)

    def test_four_required_main_commit_checks_and_certificate(self):
        for item in ("native-platforms.yml", "capture-screenshots.yml",
                     "generate-localizations.yml", "p03-stability.yml"):
            self.assertIn(item, self.gate)
        self.assertIn('.event == "push"', self.gate)
        self.assertIn("completed:success", self.gate)
        self.assertIn("QREX_UPLOAD_CERT_SHA256", self.signer)

    def test_bash_syntax(self):
        for name in ("tool/verify_production_gate.sh", "tool/sign_android_production.sh"):
            with self.subTest(name=name):
                subprocess.run(["bash", "-n", str(ROOT / name)], check=True)


if __name__ == "__main__":
    unittest.main()
