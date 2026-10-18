"""Synthetic P03 parser fixtures; not evidence of a live QREX device crash."""
import unittest
from p03_crash_triage import report


class P03CrashLogTests(unittest.TestCase):
    def test_non_app_crashes_are_not_attributed_to_qrex(self):
        raw = ("FATAL EXCEPTION: main\n"
               "Process: com.android.systemui, PID: 20\n"
               "java.lang.IllegalStateException: emulator issue\n")
        text, count = report(raw, "34")
        self.assertEqual(0, count)
        self.assertNotIn("emulator issue", text)

    def test_app_crash_keeps_only_safe_class_and_stack_frames(self):
        raw = ("FATAL EXCEPTION: main\n"
               "Process: com.brendigo.qrex, PID: 30\n"
               "java.lang.IllegalArgumentException: WIFI:T:WPA;S:Home;P:super-secret;; "
               "https://example.org/secret\n"
               " at com.brendigo.qrex.MainActivity.onCreate(MainActivity.kt:114)\n"
               " at android.app.Activity.performCreate(Activity.java:8100)\n")
        text, count = report(raw, "26")
        self.assertEqual(1, count)
        self.assertIn("java.lang.IllegalArgumentException", text)
        self.assertIn("com.brendigo.qrex.MainActivity.onCreate", text)
        self.assertNotIn("super-secret", text)
        self.assertNotIn("example.org", text)
        self.assertNotIn("Home", text)
        self.assertNotIn("PID: 30", text)


if __name__ == "__main__":
    unittest.main()
