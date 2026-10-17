#!/usr/bin/env python3
"""Native Android privacy and gallery lifecycle regression contracts.

Static checks supplement Android emulator instrumentation;
they do not claim real-device runtime certification.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


class QREXPrivacyStabilityTests(unittest.TestCase):
    def test_android_clipboard_marks_passwords_sensitive(self):
        main = (ANDROID / "MainActivity.kt").read_text("utf-8")
        self.assertIn("qrClipboardSensitive(raw)", main)
        self.assertIn("ClipDescription.EXTRA_IS_SENSITIVE", main)
        self.assertIn("clipboard.setPrimaryClip(makeQrClipboardClip(raw))", main)
        self.assertNotIn('setPrimaryClip(android.content.ClipData.newPlainText("QR", raw))', main)

    def test_android_wifi_share_requires_explicit_confirmation(self):
        android = (ANDROID / "MainActivity.kt").read_text("utf-8")
        self.assertIn("if (wifi) confirmWifiShare = true", android)
        self.assertIn("if (confirmWifiShare) {", android)
        self.assertIn("shareQrText(context, raw, shareTitle, sharingFailed)", android)
        self.assertIn("Text(tr(\"wifi_warning\"))", android)
        self.assertIn("Text(tr(\"cancel\"))", android)

    def test_android_gallery_bitmap_cleanup_on_all_completion_paths(self):
        scanner = (ANDROID / "QREXScanner.kt").read_text("utf-8")
        self.assertIn("InputImage.fromBitmap(upright, 0) to upright", scanner)
        self.assertIn("image?.second?.recycle()", scanner)
        self.assertIn("}.addOnCompleteListener {", scanner)
        self.assertIn("bitmap.recycle()", scanner)
        self.assertIn("client.close()", scanner)
        self.assertIn("BarcodeScanning.getClient(options) }.getOrElse", scanner)

    def test_camera_permission_does_not_block_gallery(self):
        android = (ANDROID / "QREXScanner.kt").read_text("utf-8")
        self.assertNotIn("if (!allowed) permission.launch(Manifest.permission.CAMERA)", android)
        self.assertIn("onClick = { permission.launch(Manifest.permission.CAMERA) }", android)
        self.assertIn("photoPicker.launch(", android)



if __name__ == "__main__":
    unittest.main()
