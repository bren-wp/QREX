#!/usr/bin/env python3
"""Scanner reference contracts on real Compose + CameraX code.

Complements JVM frame-math and Android instrumentation.  This intentionally
does not validate marketing collage pixels as if they were camera frames.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex/QREXScanner.kt"


class ScannerReferenceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.scanner = SRC.read_text("utf-8")

    def test_neon_corner_geometry_matches_reference_without_clipping_actions(self):
        s = self.scanner
        self.assertIn("internal fun scannerFrameSidePx(", s)
        self.assertIn("widthPx * .63f", s)
        self.assertIn("usableHeightPx * .83f", s)
        self.assertIn("maxSidePx = 440.dp.toPx()", s)
        self.assertIn("widthPx * .63f, usableHeightPx * .83f, maxSidePx", s)
        self.assertIn("reservedBottomDp.dp.toPx()", s)
        self.assertIn("scannerFrameTopPx(size.height, usableHeight, side)", s)
        self.assertIn("size.height * 0.45f", s)
        self.assertIn("Color(0xFFD9FAFF)", s)
        self.assertIn("QrexDesign.actionBlue.copy(alpha = .26f)", s)
        self.assertIn("tint.copy(alpha = .78f)", s)
        self.assertIn('testTag("qrex-scan-brackets")', s)

    def test_short_and_large_font_scanner_never_weights_unbounded_scroll(self):
        s = self.scanner
        self.assertIn("qrexScannerNeedsScrollLayout(", s)
        self.assertIn("heightDp < 620 || fontScale >= 1.5f", s)
        self.assertIn('testTag("qrex-scan-scroll-root")', s)
        self.assertIn('testTag("qrex-scan-fixed-root")', s)
        self.assertIn("Modifier.fillMaxWidth().height(qrexScannerScrollablePreviewHeightDp(", s)
        self.assertIn("else previewModifier.weight(1f)", s)
        self.assertIn("Box(cameraPanel", s)
        self.assertIn("scannerActions(true)", s)
        self.assertIn("scannerActions(false)", s)

    def test_3d_torch_is_a_real_camera_action_and_gallery_stays_local(self):
        s = self.scanner
        self.assertIn(".shadow(4.dp, CircleShape, clip = false)", s)
        self.assertIn("Brush.verticalGradient(", s)
        self.assertEqual(2, s.count("onClick = toggleTorch"))
        self.assertIn("activeCamera.cameraControl.enableTorch(next)", s)
        self.assertIn("if (runCatching { request.get() }.isSuccess)", s)
        self.assertIn("PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)", s)
        self.assertIn("QREXQrDecoder.decodeFrame(frame)", s)
        self.assertIn("QREXQrDecoder.decodeBitmap(bitmap)", s)
        self.assertIn("frame.close()", s)
        self.assertIn("AndroidView(factory = { preview }", s)
        self.assertNotIn("painterResource(", s)


if __name__ == "__main__":
    unittest.main()
