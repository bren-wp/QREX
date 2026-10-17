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
        self.assertIn("internal fun responsiveQrSideDp", main)
        self.assertGreaterEqual(main.count("BoxWithConstraints(Modifier.fillMaxWidth()"), 2)
        self.assertIn("responsiveQrSideDp(maxWidth.value, 230f)", main)
        self.assertIn("responsiveQrSideDp(maxWidth.value, 238f)", main)
        self.assertNotIn("Modifier.size(230.dp).background(white)", main)
        self.assertNotIn("Modifier.size(238.dp).background(white)", main)

    def test_ios_qr_images_shrink_before_viewport_overflow(self):
        ios = (ROOT / "platforms/ios/QREX/QREXView.swift").read_text("utf-8")
        self.assertIn(".scaledToFit().frame(maxWidth: 240, maxHeight: 240)", ios)
        self.assertIn(".scaledToFit().frame(maxWidth: 210, maxHeight: 210)", ios)
        self.assertNotIn(".frame(width: 240, height: 240)", ios)
        self.assertNotIn(".frame(width: 210, height: 210)", ios)

    def test_flutter_scanner_retains_dynamic_frame_and_removes_noop_wrapper(self):
        flutter = (ROOT / "lib/screens/scanner_page.dart").read_text("utf-8")
        self.assertIn("MediaQuery.sizeOf(context).width * .73", flutter)
        self.assertIn("available.maxHeight * .86", flutter)
        self.assertIn("SafeArea(child: Column(children: [", flutter)
        self.assertNotIn("LayoutBuilder(builder: (context, layout)", flutter)

    def test_native_camera_permissions_require_an_explicit_user_action(self):
        android = (ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/QREXScanner.kt").read_text("utf-8")
        ios = (ROOT / "platforms/ios/QREX/QREXScanner.swift").read_text("utf-8")
        self.assertNotIn("LaunchedEffect(Unit) {\\n        if (!allowed) permission.launch", android)
        self.assertIn("LifecycleEventObserver", android)
        self.assertIn("Settings.ACTION_APPLICATION_DETAILS_SETTINGS", android)
        self.assertNotIn(".task { requestCamera() }", ios)
        self.assertIn("UIApplication.openSettingsURLString", ios)
        self.assertIn("access = AVCaptureDevice.authorizationStatus(for: .video)", ios)

    def test_flutter_create_and_result_use_bounded_qr_previews(self):
        create = (ROOT / "lib/screens/create_page.dart").read_text("utf-8")
        result = (ROOT / "lib/screens/result_page.dart").read_text("utf-8")
        nav = (ROOT / "lib/screens/home_shell.dart").read_text("utf-8")
        self.assertIn("LayoutBuilder(builder: (context, limits)", create)
        self.assertIn("responsiveQrSide(limits.maxWidth)", create)
        self.assertIn("responsiveQrSide(", result)
        self.assertIn("viewport.height * .35", result)
        self.assertIn("overflow: TextOverflow.ellipsis", nav)
        self.assertIn("maxLines: 1", nav)


if __name__ == "__main__":
    unittest.main()
