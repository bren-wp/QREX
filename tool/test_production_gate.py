#!/usr/bin/env python3
"""Runnable failure-mode tests for the manual QREX production release gate."""
import os
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = [
    ROOT / "tool/verify_production_gate.sh",
    ROOT / "tool/sign_android_production.sh",
    ROOT / "tool/sign_ios_production.sh",
]


class ProductionGateSafetyTests(unittest.TestCase):
    def test_all_signing_scripts_parse_as_bash(self):
        for script in SCRIPTS:
            with self.subTest(script=script.name):
                subprocess.run(["bash", "-n", str(script)], check=True)

    def run_gate(self, **updates):
        env = dict(os.environ)
        env.update({
            "GH_TOKEN": "fake-token-unused-before-ref-check",
            "GITHUB_REPOSITORY": "bren-wp/QREX",
            "GITHUB_SHA": "0" * 40,
            "GITHUB_REF": "refs/heads/main",
            "PRODUCTION_APPROVED": "RELEASE_1_0_0",
            "DEVICE_QA_CONFIRMED": "true",
            "STORE_REVIEW_CONFIRMED": "true",
        })
        env.update(updates)
        return subprocess.run(
            ["bash", str(SCRIPTS[0])], cwd=ROOT, env=env,
            text=True, capture_output=True, timeout=10,
        )

    def test_rejects_non_main_branch_without_network_access(self):
        result = self.run_gate(GITHUB_REF="refs/heads/other")
        self.assertNotEqual(result.returncode, 0)

    def test_rejects_missing_manual_confirmation_without_network_access(self):
        result = self.run_gate(PRODUCTION_APPROVED="RELEASE_TEST")
        self.assertNotEqual(result.returncode, 0)

    def test_rejects_unapproved_device_qa_without_network_access(self):
        result = self.run_gate(DEVICE_QA_CONFIRMED="false")
        self.assertNotEqual(result.returncode, 0)

    def test_rejects_unapproved_store_review_without_network_access(self):
        result = self.run_gate(STORE_REVIEW_CONFIRMED="false")
        self.assertNotEqual(result.returncode, 0)

    def test_android_signing_fails_without_real_key_material(self):
        env = dict(os.environ)
        for key in (
            "QREX_UPLOAD_KEYSTORE_BASE64", "QREX_UPLOAD_STORE_PASSWORD",
            "QREX_UPLOAD_KEY_ALIAS", "QREX_UPLOAD_KEY_PASSWORD",
            "QREX_UPLOAD_CERT_SHA256",
        ):
            env.pop(key, None)
        result = subprocess.run(["bash", str(SCRIPTS[1])], cwd=ROOT,
                                env=env, text=True, capture_output=True, timeout=10)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Missing approved Android", result.stderr)

    def test_ios_signing_fails_without_authorized_certificate(self):
        env = dict(os.environ)
        for key in (
            "QREX_APPLE_CERT_P12_BASE64", "QREX_APPLE_CERT_PASSWORD",
            "QREX_APPLE_CERT_SHA256", "QREX_APPLE_PROVISION_PROFILE_BASE64",
            "QREX_APPLE_TEAM_ID", "QREX_IOS_PROVISION_PROFILE_NAME",
        ):
            env.pop(key, None)
        result = subprocess.run(["bash", str(SCRIPTS[2])], cwd=ROOT,
                                env=env, text=True, capture_output=True, timeout=10)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Missing approved Apple", result.stderr)


if __name__ == "__main__":
    unittest.main()
