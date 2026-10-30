#!/usr/bin/env python3
"""Grounded QREX screen hierarchy contracts for actual Android Compose UI."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
NATIVE = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


class ScreenFidelityContracts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.files = {name: (NATIVE / name).read_text("utf-8") for name in (
            "MainActivity.kt", "QREXDesign.kt", "QREXScanner.kt",
            "QREXMoreDetails.kt", "QREXSplash.kt")}
        cls.main = cls.files["MainActivity.kt"]

    def test_no_duplicate_brand_icon_on_history_favorites_more(self):
        header = self.main.split("private fun AppHeader(", 1)[1].split(
            "private fun PrimaryAction(", 1)[0]
        self.assertNotIn("painterResource", header)
        self.assertIn("QrexDesign.titleTextSize", header)
        self.assertIn("QrexDesign.supportingTextSize", header)

    def test_reference_blue_action_depth_without_runtime_blur(self):
        self.assertIn("elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp",
                      self.main)
        self.assertIn("QrexDesign.actionTextSize",
                      self.files["QREXDesign.kt"])
        self.assertIn("shadowElevation = if (selected) 3.dp else 0.dp", self.main)
        self.assertNotIn("InfiniteTransition", self.files["QREXDesign.kt"])

    def test_scanner_border_and_live_camera_are_preserved(self):
        scanner = self.files["QREXScanner.kt"]
        self.assertIn(".border(1.dp, QrexDesign.stroke, RoundedCornerShape(24.dp))", scanner)
        self.assertIn("AndroidView(factory = { preview }", scanner)
        self.assertIn("ScannerCornerOverlay(", scanner)

    def test_all_ten_screen_routes_keep_real_controls(self):
        for tag in ("qrex-result-screen", "qrex-create-screen",
                    "qrex-history-screen", "qrex-favorites-screen", "qrex-more-screen"):
            self.assertIn(tag, self.main)
        for tag in ("qrex-language-screen", "qrex-export-screen"):
            self.assertIn(tag, self.files["QREXMoreDetails.kt"])
        self.assertIn("qrex-splash-logo", self.files["QREXSplash.kt"])
        self.assertIn("qrex-camera-preview", self.files["QREXScanner.kt"])
        self.assertIn("qrex-create-wifi-password", self.main)


if __name__ == "__main__":
    unittest.main()
