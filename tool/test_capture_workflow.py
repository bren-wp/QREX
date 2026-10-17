#!/usr/bin/env python3
"""Prevent regression to a multiline emulator-runner script.

The runner executes newline-separated commands in separate /bin/sh sessions,
so shell control flow must be called from one standalone script.
"""
from pathlib import Path
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

    def test_workflows_cannot_write_to_obsolete_development_branch(self):
        screenshot = WORKFLOW.read_text(encoding="utf-8")
        locales = (ROOT / ".github/workflows/generate-localizations.yml").read_text(
            encoding="utf-8"
        )
        for body in (screenshot, locales):
            self.assertNotIn("fix/runtime-screenshots-and-qr-export", body)
            self.assertNotIn("contents: write", body)
            self.assertNotIn("git push", body)
            self.assertIn("contents: read", body)
        self.assertIn("pull_request:", screenshot)
        self.assertIn("pull_request:", locales)
        self.assertIn("python3 tool/generate_localizations.py --check", locales)

    def test_capture_shell_syntax(self):
        subprocess.run(["bash", "-n", str(SCRIPT)], check=True)

    def test_all_blocking_adb_capture_calls_have_deadlines(self):
        body = SCRIPT.read_text(encoding="utf-8")
        for invocation in (
            "adb wait-for-device",
            "adb install -r",
            "adb shell am start -W",
            "adb shell uiautomator dump",
            "adb exec-out cat",
            "adb exec-out screencap -p",
        ):
            with self.subTest(invocation=invocation):
                self.assertRegex(
                    body,
                    r"(?m)^\\s*.*timeout -k [0-9]+s [0-9]+s " +
                    __import__("re").escape(invocation),
                )
        self.assertIn('rm -f "$screenshot" "$hierarchy"', body)
        self.assertIn("trap on_exit EXIT", body)
        self.assertIn("No validated QREX screen; fail closed", body)
    def test_emulator_uses_supported_software_gpu_and_kvm_when_available(self):
        body = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("emulator-options: -no-window -gpu software ", body)
        self.assertNotIn("-gpu swiftshader_indirect", body)
        self.assertIn("Enable available KVM acceleration", body)
        self.assertIn("if-no-files-found: error", body)
        self.assertIn("test -s docs/screenshots/android-create.png", body)

    def test_screenshot_is_gated_by_foreground_and_png_checks(self):
        body = SCRIPT.read_text(encoding="utf-8")
        self.assertIn("gradle -p platforms/android", body)
        self.assertIn("python3 tool/check_android_screen.py", body)
        self.assertIn("python3 tool/verify_screenshots.py", body)
        self.assertIn("adb exec-out screencap -p", body)
        self.assertIn("exit 1", body)


if __name__ == "__main__":
    unittest.main()
