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
        self.assertIn("-gpu swiftshader_indirect", workflow)
        self.assertIn("disable-spellchecker: true", workflow)
        self.assertNotIn("-gpu software", workflow)

    def test_compose_qr_previews_are_not_force_recycled(self):
        # RenderThread may still reference a frame after composable disposal.
        from pathlib import Path
        source = (Path(__file__).resolve().parents[1] /
                  "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt"
                  ).read_text("utf-8")
        self.assertNotIn("awaitDispose { bitmap?.recycle() }", source)
        self.assertNotIn("awaitDispose { image?.recycle() }", source)
        self.assertIn("bitmap.recycle()", source)  # independent SAF PNG bitmap

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



if __name__ == "__main__":
    unittest.main()
