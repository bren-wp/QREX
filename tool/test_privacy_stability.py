#!/usr/bin/env python3
"""Cross-platform privacy and gallery lifecycle regression contracts.

Static checks supplement Android device-backed and iOS XCTest cases;
they do not claim real-device runtime certification.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"
IOS = ROOT / "platforms/ios/QREX"


class QREXPrivacyStabilityTests(unittest.TestCase):
    def test_android_clipboard_marks_passwords_sensitive(self):
        main = (ANDROID / "MainActivity.kt").read_text("utf-8")
        self.assertIn("qrClipboardSensitive(raw)", main)
        self.assertIn("ClipDescription.EXTRA_IS_SENSITIVE", main)
        self.assertIn("clipboard.setPrimaryClip(makeQrClipboardClip(raw))", main)
        self.assertNotIn('setPrimaryClip(android.content.ClipData.newPlainText("QR", raw))', main)

    def test_ios_wifi_clipboard_expires_but_plain_copy_still_works(self):
        view = (IOS / "QREXView.swift").read_text("utf-8")
        self.assertIn("static let sensitiveLifetime: TimeInterval = 120", view)
        self.assertIn("QREXClipboard.copy(raw)", view)
        self.assertIn("UIPasteboard.general.setItems(", view)
        self.assertIn(".expirationDate: Date().addingTimeInterval(sensitiveLifetime)", view)
        self.assertIn("UIPasteboard.general.string = raw", view)

    def test_wifi_qr_share_requires_confirmation_on_all_three_platforms(self):
        android = (ANDROID / "MainActivity.kt").read_text("utf-8")
        ios = (IOS / "QREXView.swift").read_text("utf-8")
        flutter = (ROOT / "lib/screens/result_page.dart").read_text("utf-8")
        self.assertIn("if (wifi) confirmWifiShare = true", android)
        self.assertIn("if (confirmWifiShare) {", android)
        self.assertIn("shareQrText(context, raw, shareTitle, sharingFailed)", android)
        self.assertIn("isPresented: $confirmWifiShare", ios)
        self.assertIn("QREXConfirmedWifiShareSheet(text: raw)", ios)
        self.assertIn("if QRContent.isWifi(raw) {", ios)
        self.assertIn("ShareLink(item: raw)", ios)
        self.assertIn("final approved = await showDialog<bool>", flutter)
        self.assertIn("if (approved != true || !mounted) return;", flutter)

    def test_android_gallery_bitmap_cleanup_on_all_completion_paths(self):
        scanner = (ANDROID / "QREXScanner.kt").read_text("utf-8")
        self.assertIn("InputImage.fromBitmap(upright, 0) to upright", scanner)
        self.assertIn("image?.second?.recycle()", scanner)
        self.assertIn("}.addOnCompleteListener {", scanner)
        self.assertIn("bitmap.recycle()", scanner)
        self.assertIn("client.close()", scanner)
        self.assertIn("BarcodeScanning.getClient(options) }.getOrElse", scanner)

    def test_privacy_prompts_do_not_block_gallery_flow(self):
        android = (ANDROID / "QREXScanner.kt").read_text("utf-8")
        ios = (IOS / "QREXScanner.swift").read_text("utf-8")
        self.assertNotIn("if (!allowed) permission.launch(Manifest.permission.CAMERA)", android)
        self.assertNotIn(".task { requestCamera() }", ios)
        self.assertIn("onClick = { permission.launch(Manifest.permission.CAMERA) }", android)
        self.assertIn("Button(tr(\"enable_camera\")) { requestCamera() }", ios)
        self.assertIn("PhotosPicker(selection: $selectedPhoto, matching: .images)", ios)


if __name__ == "__main__":
    unittest.main()
