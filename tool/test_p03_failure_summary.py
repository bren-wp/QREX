"""Sanitized P03 diagnostics: never expose test assertions or QR contents."""
import tempfile
import unittest
from pathlib import Path
from p03_failure_summary import summarize


class TestInstrumentedFailureSummary(unittest.TestCase):
    def test_prints_only_allowlisted_failed_testcase_name(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "TEST-qrex.xml").write_text(
                '<testsuite tests="2" failures="1">'
                '<testcase classname="com.brendigo.qrex.QREXMoreVerticalStackTest"'
                ' name="darkAndLightMoreKeepReferenceTilesAndWorkingPrivacyTermsActions">'
                '<failure message="WIFI:P:super-secret;; https://private.example/token">'
                'java.lang.AssertionError: super-secret at Host.foo()</failure>'
                '</testcase>'
                '<testcase classname="com.brendigo.qrex.QREXMoreVerticalStackTest"'
                ' name="passingTest"/>'
                '</testsuite>', encoding="utf-8"
            )
            summary = summarize(root, "35", {"com.brendigo.qrex.QREXMoreVerticalStackTest.darkAndLightMoreKeepReferenceTilesAndWorkingPrivacyTermsActions"})
            self.assertIn("QREXMoreVerticalStackTest.darkAndLightMore", summary)
            self.assertIn("Failed test IDs: 1", summary)
            for secret in ("super-secret", "WIFI:", "private.example", "AssertionError"):
                self.assertNotIn(secret, summary)
            self.assertNotIn("passingTest", summary)

    def test_does_not_print_untrusted_dynamic_test_ids(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "TEST-hostile.xml").write_text(
                '<testsuite>'
                '<testcase classname="com.brendigo.qrex.Foo"'
                ' name="testSecret_WiFi_password_should_not_go_in_logs_123456">'
                '<failure>secret-value</failure></testcase>'
                '<testcase classname="com.other.vendor.Foo" name="password123">'
                '<error>secret-value</error></testcase>'
                '<testcase classname="com.brendigo.qrex.Bad"'
                ' name="test/https://private.example/abc">'
                '<failure>secret-value</failure></testcase>'
                '</testsuite>', encoding="utf-8")
            summary = summarize(root, "31", set())
            self.assertNotIn("password123", summary)
            self.assertNotIn("private.example", summary)
            self.assertNotIn("secret-value", summary)
            self.assertIn("Unreported/invalid entries: 3", summary)

    def test_handles_broken_reports_without_failure_or_leaks(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "TEST-broken.xml").write_text("<testcase password=private>", encoding="utf-8")
            summary = summarize(root, "99")
            self.assertIn("Android API: unknown", summary)
            self.assertIn("Readable JUnit XML reports: 0", summary)
            self.assertNotIn("private", summary)
