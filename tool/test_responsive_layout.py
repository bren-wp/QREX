#!/usr/bin/env python3
"""Static guardrails for QREX compact-screen QR layouts.

These checks do not replace pixel comparisons or physical-device QA.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class ResponsiveLayoutRegressionTests(unittest.TestCase):
    def test_android_qr_images_fit_measured_compose_width(self):
        main = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/MainActivity.kt").read_text("utf-8")
        # Old viewport arithmetic helper was only referenced by tests.
        # Current Compose image modifiers perform the sizing directly.
        self.assertNotIn("responsiveQrSideDp", main)
        # Fixed aspect-ratio, capped-width QR images fit narrow devices
        # without SubcomposeLayout, whose repeated disposal crashed API 26.
        self.assertNotIn("BoxWithConstraints(", main)
        self.assertIn("Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center)", main)
        self.assertIn(".widthIn(max = 230.dp).fillMaxWidth().aspectRatio(1f)", main)
        self.assertIn(".size(104.dp).background(Color.White).padding(7.dp)", main)
        self.assertNotIn("Modifier.size(230.dp).background(white)", main)
        self.assertNotIn("Modifier.size(238.dp).background(white)", main)

    def test_camera_permissions_require_explicit_user_action(self):
        android = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/QREXScanner.kt").read_text("utf-8")
        self.assertNotIn("if (!allowed) permission.launch(Manifest.permission.CAMERA)", android)
        self.assertIn("LifecycleEventObserver", android)
        self.assertIn("Settings.ACTION_APPLICATION_DETAILS_SETTINGS", android)

    def test_android_scanner_scales_and_supports_gallery_without_permission(self):
        android = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/QREXScanner.kt").read_text("utf-8")
        self.assertIn(".fillMaxWidth().weight(1f)", android)
        self.assertIn("PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)", android)
        self.assertIn("if (allowed) {", android)


if __name__ == "__main__":
    unittest.main()

