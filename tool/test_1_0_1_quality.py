#!/usr/bin/env python3
"""QREX 1.0.1 code-audit regression contracts, alongside device/JVM tests."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


class AndroidCodeAuditTests(unittest.TestCase):
    def test_pdf_zip_export_streams_without_full_document_copy(self):
        src = (JAVA / "QREXDocuments.kt").read_text("utf-8")
        self.assertNotIn("ByteArrayOutputStream", src)
        self.assertNotIn("bytes.toByteArray()", src)
        self.assertIn("ZipOutputStream(stream.buffered())", src)
        self.assertIn("writePdf(records, archive)", src)
        self.assertIn("pdf.writeTo(output)", src)
        self.assertIn("bitmap.recycle()", src)
        self.assertIn("QRContent.canRender(it.raw)", src)
        tests = (ROOT / "platforms/android/app/src/androidTest/java/com/brendigo/qrex/QREXStorageTest.kt").read_text("utf-8")
        self.assertIn("PdfRenderer(fd)", tests)
        self.assertIn("page.render(rendered", tests)

    def test_compact_accessibility_fixes_are_retained(self):
        view = (JAVA / "MainActivity.kt").read_text("utf-8")
        scanner = (JAVA / "QREXScanner.kt").read_text("utf-8")
        self.assertIn("TextOverflow.Ellipsis", view)
        self.assertIn("BackHandler(enabled = currentCode != null)", view)
        self.assertIn("FlowRow(modifier = Modifier.fillMaxWidth()", scanner)
        self.assertIn("verticalScroll(rememberScrollState())", scanner)
        self.assertIn("ActivityResultContracts.PickVisualMedia()", scanner)

    def test_no_unreachable_legacy_history_api(self):
        core = (JAVA / "QREXCore.kt").read_text("utf-8")
        self.assertNotIn("fun clearHistory()", core)
        self.assertIn("fun clearAll()", core)
        self.assertIn("migrateFlutterV1()", core)
        self.assertIn("QREXSecretCipher.encrypt(item.raw)", core)

    def test_play_upload_bundle_keeps_real_key_separate_from_github(self):
        play = (ROOT / ".github/workflows/production-release.yml").read_text("utf-8")
        public = (ROOT / ".github/workflows/github-release-1.0.1.yml").read_text("utf-8")
        for required in ("QREX_UPLOAD_CERT_SHA256", "QREX_UPLOAD_KEYSTORE_BASE64",
                         "environment: production", "QREX-Android-1.0.1.aab"):
            self.assertIn(required, play)
        self.assertNotIn("keytool -genkeypair", play)
        self.assertIn("keytool -genkeypair", public)
        self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", public)
        self.assertFalse((ROOT / ".github/workflows/github-release-1.0.0.yml").exists())

    def test_no_unnecessary_network_or_broad_storage_permissions(self):
        manifest = (ROOT / "platforms/android/app/src/main/AndroidManifest.xml").read_text("utf-8")
        for forbidden in ("android.permission.INTERNET", "READ_MEDIA_IMAGES",
                          "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE",
                          "MANAGE_EXTERNAL_STORAGE", "ACCESS_FINE_LOCATION"):
            self.assertNotIn(forbidden, manifest)


if __name__ == "__main__":
    unittest.main()
