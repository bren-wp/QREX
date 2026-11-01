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
        self.assertIn('ClipData.newPlainText("QR", raw).apply', main)
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

    def test_result_wifi_copy_requires_opt_in_on_both_action_paths(self):
        source = (ANDROID / "MainActivity.kt").read_text("utf-8")
        result = source.split("private fun ResultScreen(", 1)[1].split(
            "fun openLink(", 1
        )[0]
        self.assertIn("var confirmWifiCopy by remember(raw)", result)
        self.assertIn('testTag("qrex-result-copy")', result)
        self.assertIn('testTag("qrex-result-menu-copy")', result)
        self.assertEqual(result.count("if (wifi) confirmWifiCopy = true else {"), 2)
        self.assertIn('testTag("qrex-result-wifi-copy-warning")', result)
        self.assertIn('testTag("qrex-result-wifi-copy-confirm")', result)
        self.assertIn('testTag("qrex-result-wifi-copy-cancel")', result)
        self.assertIn("Text(tr(\"wifi_warning\"))", result)
        self.assertIn("copyQrToClipboard(context, raw)", result)
        self.assertIn("makeQrClipboardClip(raw)", source)
        self.assertIn("if (showQrPreview) tr(\"hide\") else tr(\"show_qr\")", result)
        # An arbitrary URL QR is visible in the summary only once; no
        # second URL copy that might unnecessarily expose private tokens.
        self.assertNotIn("url.toString(),", result)

    def test_android_gallery_bitmap_cleanup_on_all_completion_paths(self):
        scanner = (ANDROID / "QREXScanner.kt").read_text("utf-8")
        reader = (ANDROID / "QREXQrDecoder.kt").read_text("utf-8")
        # Recycle on the same worker that owns the bitmap, including when
        # the suspended Compose caller is cancelled before it can resume.
        self.assertIn("internal fun decodeGalleryQr(context: Context, uri: Uri)", scanner)
        self.assertIn("withContext(Dispatchers.IO) { decodeGalleryQr(context, uri) }", scanner)
        self.assertIn("if (!bitmap.isRecycled) bitmap.recycle()", scanner)
        self.assertIn("val boundsStream = resolver.openInputStream(uri) ?: return@runCatching null", scanner)
        self.assertIn("boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }", scanner)
        self.assertIn("width * height > 24_000_000L", scanner)
        self.assertNotIn("bitmap?.recycle()", scanner)
        self.assertIn("frame.close()", scanner)
        self.assertIn("QREXQrDecoder.decodeBitmap(bitmap)", scanner)
        self.assertIn("QREXQrDecoder.decodeFrame(frame)", scanner)
        self.assertIn("QREXQrDecoder", reader)
        self.assertNotIn("BarcodeScanning.getClient", scanner)
        self.assertNotIn("InputImage.fromBitmap", scanner)

    def test_camera_permission_does_not_block_gallery(self):
        android = (ANDROID / "QREXScanner.kt").read_text("utf-8")
        self.assertNotIn("if (!allowed) permission.launch(Manifest.permission.CAMERA)", android)
        self.assertIn("onClick = { permission.launch(Manifest.permission.CAMERA) }", android)
        self.assertIn("photoPicker.launch(", android)



if __name__ == "__main__":
    unittest.main()
