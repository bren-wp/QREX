#!/usr/bin/env python3
"""Fail-closed checks for manually authorized Play 1.0.1 signing."""
import os
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
GATE = ROOT / "tool/verify_production_gate.sh"
SIGNER = ROOT / "tool/sign_android_production.sh"


class ProductionGateSafetyTests(unittest.TestCase):
    def test_play_shell_scripts_parse(self):
        for script in (GATE, SIGNER):
            with self.subTest(script=script.name):
                subprocess.run(["bash", "-n", str(script)], check=True)

    def run_gate(self, **updates):
        env = dict(os.environ)
        env.update({
            "GH_TOKEN": "fake-not-used-before-ref-check",
            "GITHUB_REPOSITORY": "bren-wp/QREX",
            "GITHUB_SHA": "0" * 40,
            "GITHUB_REF": "refs/heads/main",
            "PRODUCTION_APPROVED": "PLAY_UPLOAD_1.0.9",
            "RELEASE_VERSION": "1.0.9",
            "DEVICE_QA_CONFIRMED": "true",
            "STORE_REVIEW_CONFIRMED": "true",
        })
        env.update(updates)
        return subprocess.run(["bash", str(GATE)], cwd=ROOT, env=env,
                              text=True, capture_output=True, timeout=10)

    def test_rejects_non_main_branch_without_network(self):
        self.assertNotEqual(self.run_gate(GITHUB_REF="refs/heads/test").returncode, 0)

    def test_rejects_wrong_manual_release_confirmation(self):
        self.assertNotEqual(self.run_gate(PRODUCTION_APPROVED="RELEASE_1_0_0").returncode, 0)

    def test_rejects_unapproved_device_qa(self):
        self.assertNotEqual(self.run_gate(DEVICE_QA_CONFIRMED="false").returncode, 0)

    def test_rejects_unapproved_play_listing_and_certificate_review(self):
        self.assertNotEqual(self.run_gate(STORE_REVIEW_CONFIRMED="false").returncode, 0)

    def test_signing_fails_without_real_key_material(self):
        env = dict(os.environ)
        for key in ("QREX_UPLOAD_KEYSTORE_BASE64", "QREX_UPLOAD_STORE_PASSWORD",
                    "QREX_UPLOAD_KEY_ALIAS", "QREX_UPLOAD_KEY_PASSWORD",
                    "QREX_UPLOAD_CERT_SHA256"):
            env.pop(key, None)
        env["RELEASE_VERSION"] = "1.0.9"
        env["GITHUB_REF"] = "refs/heads/main"
        result = subprocess.run(["bash", str(SIGNER)], cwd=ROOT, env=env,
                                text=True, capture_output=True, timeout=10)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Missing approved Android", result.stderr)


if __name__ == "__main__":
    unittest.main()
