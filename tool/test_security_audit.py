#!/usr/bin/env python3
"""Prevent source-level security gates from silently regressing."""
import unittest

from audit_android_security import ROOT, security_findings, potential_dead_code


class QrexSourceSecurityTests(unittest.TestCase):
    def test_full_production_source_security_gate(self):
        self.assertEqual([], security_findings())

    def test_audit_covers_all_real_android_kotlin_files(self):
        sources = sorted((ROOT / "platforms/android/app/src/main/java").rglob("*.kt"))
        self.assertGreaterEqual(len(sources), 8)
        self.assertTrue(any(p.name == "MainActivity.kt" for p in sources))
        self.assertTrue(any(p.name == "QREXSecretCipher.kt" for p in sources))
        self.assertTrue(any(p.name == "QREXScanner.kt" for p in sources))

    def test_dead_code_review_never_automatically_removes_source(self):
        candidates = potential_dead_code()
        self.assertTrue(all(path.endswith(".kt") and name.isidentifier()
                            for path, name in candidates))
        self.assertFalse(any(name in {"onCreate", "analyze", "close"}
                             for _, name in candidates))

    def test_external_url_and_keystore_input_bounds(self):
        main = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt"
                ).read_text("utf-8")
        cipher = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/QREXSecretCipher.kt"
                  ).read_text("utf-8")
        self.assertIn("val verified = QRContent.safeUrl(url.toString()) ?: return", main)
        self.assertIn("Intent(Intent.ACTION_VIEW, verified)", main)
        self.assertIn("encoded.length > MAX_ENCODED_CHARS", cipher)
        self.assertIn("ciphertext.size > MAX_PLAINTEXT_BYTES + 16", cipher)
        self.assertIn("GCMParameterSpec(128, iv)", cipher)


if __name__ == "__main__":
    unittest.main()
