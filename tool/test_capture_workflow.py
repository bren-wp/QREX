#!/usr/bin/env python3
"""Prevent regression to a multiline emulator-runner script.

The runner executes newline-separated commands in separate /bin/sh sessions,
so shell control flow must be called from one standalone script.
"""
from pathlib import Path
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github/workflows/capture-screenshots.yml"
SCRIPT = ROOT / "tool/capture_android_screen.sh"


class ScreenshotWorkflowTests(unittest.TestCase):
    def test_emulator_runner_uses_one_script_invocation(self):
        body = WORKFLOW.read_text(encoding="utf-8")
        self.assertRegex(
            body,
            r"(?m)^\s+script: bash tool/capture_android_screen\.sh\s*$",
        )
        # The inline loop caused /bin/sh to receive a partial for statement.
        self.assertNotIn("script: |\n            for attempt", body)
        self.assertIn('"tool/capture_android_screen.sh"', body)

    def test_capture_shell_syntax(self):
        subprocess.run(["bash", "-n", str(SCRIPT)], check=True)

    def test_screenshot_is_gated_by_foreground_and_png_checks(self):
        body = SCRIPT.read_text(encoding="utf-8")
        self.assertIn("gradle -p platforms/android", body)
        self.assertIn("python3 tool/check_android_screen.py", body)
        self.assertIn("python3 tool/verify_screenshots.py", body)
        self.assertIn("adb exec-out screencap -p", body)
        self.assertIn("exit 1", body)


if __name__ == "__main__":
    unittest.main()
