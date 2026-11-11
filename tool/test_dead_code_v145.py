#!/usr/bin/env python3
"""QREX 1.0.45 dead-code regression checks for native Compose screens.

This is a targeted source gate; full reachability also requires Kotlin compilation,
R8 and Android instrumentation. Never infer implicit Kotlin operators are unused.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
NATIVE = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


class Qrex145DeadCodeRegressionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.main = (NATIVE / "MainActivity.kt").read_text("utf-8")
        cls.core = (NATIVE / "QREXCore.kt").read_text("utf-8")
        cls.design = (NATIVE / "QREXDesign.kt").read_text("utf-8")

    def test_no_dead_legacy_main_activity_imports(self):
        for symbol in (
            "androidx.compose.ui.res.painterResource",
            "androidx.compose.material.icons.filled.Translate",
            "androidx.compose.material.icons.filled.Archive",
            "androidx.compose.material.icons.filled.PictureAsPdf",
            "androidx.compose.material.icons.filled.Palette",
        ):
            with self.subTest(symbol=symbol):
                self.assertNotIn("import " + symbol, self.main)

    def test_result_back_is_unique_and_cleans_transient_state(self):
        # With both outer and inner BackHandler enabled, the outer handler is
        # shadowed while Result is visible and does not perform its cleanup.
        handlers = re.findall(
            r"BackHandler\(enabled\s*=\s*currentCode\s*!=\s*null\)",
            self.main,
        )
        self.assertEqual(len(handlers), 1)
        self.assertIn("focusManager.clearFocus(force = true)", self.main)
        self.assertIn("resultShowsQr = false", self.main)

    def test_compose_state_delegate_operators_are_not_deleted_as_dead_code(self):
        self.assertRegex(self.core, r"\bvar\s+language\s+by\s+mutableStateOf")
        self.assertRegex(self.core, r"\bvar\s+themeMode\s+by\s+mutableStateOf")
        # These symbols may have no literal call sites: the Kotlin compiler
        # resolves them implicitly from property delegation.
        self.assertIn("import androidx.compose.runtime.getValue", self.core)
        self.assertIn("import androidx.compose.runtime.setValue", self.core)

    def test_six_reference_screens_keep_real_functional_compose_routes(self):
        for marker in (
            'testTag("qrex-result-screen")',
            'testTag("qrex-create-screen")',
            '"qrex-history-screen"',
            'testTag("qrex-more-screen")',
        ):
            self.assertIn(marker, self.main)
        self.assertIn("QrexDesign.titleTextSize", self.main)
        self.assertIn("val titleTextSize = 27.sp", self.design)
        self.assertIn("val supportingTextSize = 14.sp", self.design)

    def test_marketing_images_match_the_documented_published_version(self):
        readme = (ROOT / "README.md").read_text("utf-8")
        gradle = (ROOT / "platforms/android/app/build.gradle.kts").read_text("utf-8")
        match = re.search(
            r"Latest published release: \*\*\[v([0-9]+\.[0-9]+\.[0-9]+)\]",
            readme,
        )
        self.assertIsNotNone(match)
        published_version = match.group(1)
        self.assertIn(
            f"releases/download/v{published_version}/android-history.png",
            readme,
        )
        configured = re.search(r'versionName\s*=\s*"([0-9.]+)"', gradle)
        self.assertIsNotNone(configured)
        self.assertLessEqual(
            tuple(map(int, published_version.split("."))),
            tuple(map(int, configured.group(1).split("."))),
        )


if __name__ == "__main__":
    unittest.main()
