"""Synthetic P03 parser fixtures; not evidence of a live QREX device crash."""
import unittest
from p03_crash_triage import report


class P03CrashLogTests(unittest.TestCase):
    def test_emulator_workflow_runs_a_single_bash_process(self):
        from pathlib import Path
        import subprocess
        root = Path(__file__).resolve().parents[1]
        workflow = (root / ".github/workflows/p03-stability.yml").read_text("utf-8")
        runner_script = root / "tool/run_p03_emulator.sh"
        self.assertIn("script: bash tool/run_p03_emulator.sh ${{ matrix.api }}", workflow)
        self.assertNotIn("script: |", workflow)
        self.assertIn("max-parallel: 2", workflow)
        self.assertIn("bash tool/prepare_android_emulator.sh", workflow)
        preflight = (root / "tool/prepare_android_emulator.sh").read_text("utf-8")
        self.assertIn('cmdline-tools/latest/bin/sdkmanager', preflight)
        self.assertIn('ANDROID_HOME:-', preflight)
        self.assertIn('QREX_SDKMANAGER:-', preflight)
        subprocess.run(["bash", "-n", str(root / "tool/prepare_android_emulator.sh")], check=True)
        subprocess.run(["bash", "-n", str(runner_script)], check=True)
        source = runner_script.read_text("utf-8")
        for expected in (":app:connectedDebugAndroidTest", "adb logcat -b crash",
                         "adb logcat -b events", "p03_crash_triage.py",
                         "timeout -k 15s 1200s"):
            self.assertIn(expected, source)

    def test_emulator_uses_kvm_when_available_and_software_gpu_fallback(self):
        from pathlib import Path
        workflow = (Path(__file__).resolve().parents[1] /
                    ".github/workflows/p03-stability.yml").read_text("utf-8")
        self.assertIn("test -r /dev/kvm && test -w /dev/kvm", workflow)
        self.assertIn("sudo udevadm trigger --name-match=kvm", workflow)
        options = next(line.strip() for line in workflow.splitlines()
                       if line.strip().startswith("emulator-options:"))
        self.assertIn("-gpu software", options)
        self.assertNotIn("-gpu swiftshader_indirect", options)
        self.assertIn("-no-snapshot", options)
        self.assertIn("disable-spellchecker: true", workflow)

    def test_p03_changes_run_full_android_release_quality_workflows(self):
        from pathlib import Path
        workflows = Path(__file__).resolve().parents[1] / ".github/workflows"
        for name in ("native-platforms.yml", "capture-screenshots.yml"):
            source = (workflows / name).read_text("utf-8")
            for path in ("tool/p03_crash_triage.py", "tool/test_p03_crash_triage.py"):
                self.assertIn('      - "' + path + '"', source)

    def test_compose_qr_previews_are_not_force_recycled(self):
        # RenderThread may still reference a frame after composable disposal.
        from pathlib import Path
        source = (Path(__file__).resolve().parents[1] /
                  "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt"
                  ).read_text("utf-8")
        self.assertNotIn("awaitDispose { bitmap?.recycle() }", source)
        self.assertNotIn("awaitDispose { image?.recycle() }", source)
        self.assertIn("bitmap.recycle()", source)  # independent SAF PNG bitmap


    def test_sdk_archive_retry_is_bounded_and_does_not_hide_app_failures(self):
        """Mock SDK downloads: only recognized transport corruption can retry."""
        from pathlib import Path
        import os
        import subprocess
        import tempfile

        root = Path(__file__).resolve().parents[1]
        script = root / "tool/prepare_android_emulator.sh"
        with tempfile.TemporaryDirectory(prefix="qrex-sdk-test-") as directory:
            work = Path(directory)
            sdk = work / "sdkmanager"
            emu = work / "emulator"
            state = work / "attempts"
            emu.write_text("#!/usr/bin/env bash\nexit 42\n", encoding="utf-8")
            emu.chmod(0o755)

            def run_mock(failure: str, fails_before_success: int):
                state.write_text("0", encoding="utf-8")
                sdk.write_text(
                    '#!/usr/bin/env bash\n'
                    'n=$(cat "$QREX_TEST_ATTEMPTS")\n'
                    'n=$((n+1))\n'
                    'printf "%s" "$n" > "$QREX_TEST_ATTEMPTS"\n'
                    f"if (( n <= {fails_before_success} )); then "
                    f"echo '{failure}' >&2; exit 1; fi\n"
                    'exit 0\n', encoding="utf-8"
                )
                sdk.chmod(0o755)
                env = dict(os.environ)
                env.update({
                    "QREX_SDKMANAGER": str(sdk),
                    "QREX_EMULATOR_BIN": str(emu),
                    "QREX_RETRY_SLEEP_SECONDS": "0",
                    "QREX_TEST_ATTEMPTS": str(state),
                    "RUNNER_TEMP": directory,
                })
                result = subprocess.run(["bash", str(script)], env=env,
                                        capture_output=True, text=True, timeout=15)
                return result, int(state.read_text(encoding="utf-8"))

            fixed, attempts = run_mock("Error on ZipFile unknown archive", 1)
            self.assertEqual(0, fixed.returncode)
            self.assertEqual(2, attempts)
            self.assertNotIn("ZipFile unknown archive", fixed.stderr)
            persistent, attempts = run_mock("Error on ZipFile unknown archive", 3)
            self.assertNotEqual(0, persistent.returncode)
            self.assertEqual(3, attempts)
            permanent, attempts = run_mock("QREX genuine SDK error", 3)
            self.assertNotEqual(0, permanent.returncode)
            self.assertEqual(1, attempts)

            # A headless emulator may return a failure for -version while
            # the runner can still boot it. Package validation checks the
            # executable path; the live AVD/ADB check remains mandatory.
            # The mock emulator exits 42 if run; it must not be invoked here.
            installed, attempts = run_mock("ignored", 0)
            self.assertEqual(0, installed.returncode)
            self.assertEqual(1, attempts)
            emu.unlink()
            missing, attempts = run_mock("ignored", 0)
            self.assertNotEqual(0, missing.returncode)
            self.assertEqual(1, attempts)

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
        self.assertIn("QREX FATAL EXCEPTION blocks: 1", text)
        self.assertNotIn("java.lang.IllegalArgumentException", text)
        self.assertNotIn("com.brendigo.qrex.MainActivity.onCreate", text)
        self.assertNotIn("super-secret", text)
        self.assertNotIn("example.org", text)
        self.assertNotIn("Home", text)
        self.assertNotIn("PID: 30", text)


    def test_forged_class_and_frame_inside_qr_message_are_never_uploaded(self):
        password = "com.private.SecretError\n at com.brendigo.qrex.Fake.run(Fake.kt:998)"
        raw = ("FATAL EXCEPTION: main\n"
               "Process: com.brendigo.qrex, PID: 31\n"
               "java.lang.RuntimeException: WIFI:S:Home;P:" + password + ";;\n"
               " at com.brendigo.qrex.MainActivity.onCreate(MainActivity.kt:113)\n")
        output, count = report(raw, "31")
        self.assertEqual(1, count)
        for dangerous in ("com.private.SecretError", "Fake.kt", "MainActivity",
                          "RuntimeException", "Home", "WIFI:", "PID: 31"):
            self.assertNotIn(dangerous, output)

    def test_qrex_anr_is_detected_without_leaking_event_details(self):
        events = ("10-18 I am_anr: [0,123,com.brendigo.qrex, WiFi password=not-for-ci]\n"
                  "10-18 I am_anr: [0,45,com.android.systemui, other details]\n")
        text, count = report("", "36", events)
        self.assertEqual(1, count)
        self.assertIn("QREX am_anr events: 1", text)
        self.assertNotIn("not-for-ci", text)
        self.assertNotIn("systemui", text)


    def test_threadtime_padded_anr_tag_is_counted_and_sanitized(self):
        # adb logcat -b events -v threadtime pads the short event tag before ':'.
        events = (
            "10-20 21:33:00.123  1000  1001 I am_anr : [0,123,com.brendigo.qrex, WIFI:P:secret-one;;]\n"
            "10-20 21:33:01.123  1000  1001 I am_anr   : [0,124,com.brendigo.qrex, secret-two]\n"
            "10-20 21:33:02.123  1000  1001 I am_anr: [0,125,com.brendigo.qrex, secret-three]\n"
            "10-20 21:33:03.123  1000  1001 I am_anr : [0,126,com.android.systemui, other]\n"
            "10-20 21:33:04.123  1000  1001 I am_anr : [0,127,com.brendigo.qrex.evil, other]\n"
        )
        summary, count = report("", "31", events)
        self.assertEqual(3, count)
        self.assertIn("QREX am_anr events: 3", summary)
        for secret in ("secret-one", "secret-two", "secret-three", "WIFI:", "systemui", "qrex.evil"):
            self.assertNotIn(secret, summary)



if __name__ == "__main__":
    unittest.main()
