#!/usr/bin/env python3
"""Privacy-safe API26/P03 result metadata regression tests."""
import tempfile
import unittest
from pathlib import Path
from report_failed_instrumentation_tests import failed_test_identifiers


class SanitizedInstrumentationTests(unittest.TestCase):
    def test_allowlisted_name_only_and_never_print_qr_payload(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            (folder / "TEST-QREX.xml").write_text(
                """<testsuite><testcase classname="com.brendigo.qrex.QREXNavigationStressTest" name="thirtyCreateResultBackCyclesKeepAppResponsive"><failure message="WIFI:T:WPA;S:Secret;P:do-not-log;;">token=super-secret</failure></testcase><testcase classname="com.brendigo.qrex.QREXHistoryFilterTest" name="historyCanBeFiltered"/></testsuite>""",
                encoding="utf-8")
            ids = failed_test_identifiers(folder)
            self.assertEqual(
                ["QREXNavigationStressTest#thirtyCreateResultBackCyclesKeepAppResponsive"], ids)
            self.assertNotIn("Secret", str(ids))
            self.assertNotIn("token", str(ids))

    def test_malformed_and_non_allowlisted_names_are_ignored(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            (folder / "TEST-invalid.xml").write_text("<broken>", encoding="utf-8")
            (folder / "TEST-untrusted.xml").write_text(
                '<testsuite><testcase classname="bad.evil.Test" name="bad"><error>secret</error></testcase></testsuite>',
                encoding="utf-8")
            self.assertEqual([], failed_test_identifiers(folder))


if __name__ == "__main__":
    unittest.main()
