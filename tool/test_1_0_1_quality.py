#!/usr/bin/env python3
"""QREX 1.0.1 code-audit regression contracts, alongside device/JVM tests."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"


class AndroidCodeAuditTests(unittest.TestCase):
    def test_pdf_export_streams_without_full_document_copy(self):
        src = (JAVA / "QREXDocuments.kt").read_text("utf-8")
        self.assertNotIn("ByteArrayOutputStream", src)
        self.assertNotIn("bytes.toByteArray()", src)
        self.assertNotIn("ZipOutputStream", src)
        self.assertNotIn("ZipEntry", src)
        self.assertNotIn("exportSavedPdfZip", src)
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
        # Gallery is the highlighted primary action, while Torch stays
        # a separate control inside the live preview. On compact phones
        # both are >=64dp and >=80dp on normal-width phones.
        self.assertIn("Row(Modifier.fillMaxWidth(),", scanner)
        self.assertIn("horizontalArrangement = Arrangement.spacedBy(if (compactScan) 6.dp else 12.dp)", scanner)
        self.assertEqual(
            scanner.count(".heightIn(min = if (compactScan) 64.dp else 80.dp)"), 2)
        self.assertIn('testTag("qrex-scan-gallery")', scanner)
        self.assertIn('testTag("qrex-scan-torch")', scanner)
        self.assertIn("ButtonDefaults.buttonColors(", scanner)
        self.assertIn("onClick = toggleTorch", scanner)
        self.assertIn('Text(tr("from_gallery"), maxLines = 2,', scanner)
        self.assertIn("verticalScroll(rememberScrollState())", scanner)
        self.assertIn("ActivityResultContracts.PickVisualMedia()", scanner)

    def test_history_clear_preserves_language_and_encrypted_data_rules(self):
        core = (JAVA / "QREXCore.kt").read_text("utf-8")
        self.assertIn("fun clearHistory(): Boolean", core)
        self.assertNotIn("fun clearAll()", core)
        clear = core.split("fun clearHistory(): Boolean {", 1)[1].split("\n    }", 1)[0]
        self.assertIn('putString("history", "[]")', clear)
        self.assertNotIn('putBoolean("lightMode"', clear)
        self.assertNotIn('putString("language"', clear)
        self.assertIn("migrateFlutterV1()", core)
        self.assertIn("QREXSecretCipher.encrypt(item.raw)", core)

    def test_play_upload_bundle_keeps_real_key_separate_from_github(self):
        play = (ROOT / ".github/workflows/production-release.yml").read_text("utf-8")
        public = (ROOT / "docs/releases/workflows/github-release-1.0.1.yml").read_text("utf-8")
        for required in ("QREX_UPLOAD_CERT_SHA256", "QREX_UPLOAD_KEYSTORE_BASE64",
                         "environment: production", "RELEASE_VERSION",
                         "production/QREX-Android-${{ inputs.version }}.aab"):
            self.assertIn(required, play)
        self.assertNotIn("keytool -genkeypair", play)
        self.assertIn("keytool -genkeypair", public)
        self.assertNotIn("secrets.QREX_UPLOAD_KEYSTORE_BASE64", public)
        self.assertFalse((ROOT / ".github/workflows/github-release-1.0.0.yml").exists())

    def test_no_unnecessary_network_or_broad_storage_permissions(self):
        manifest = ET.parse(ROOT / "platforms/android/app/src/main/AndroidManifest.xml")
        android = "{http://schemas.android.com/apk/res/android}"
        tools = "{http://schemas.android.com/tools}"
        declared = {permission.get(android + "name")
                    for permission in manifest.getroot().findall("uses-permission")
                    if permission.get(tools + "node") != "remove"}
        for forbidden in ("android.permission.INTERNET",
                          "android.permission.ACCESS_NETWORK_STATE",
                          "android.permission.READ_MEDIA_IMAGES",
                          "android.permission.READ_EXTERNAL_STORAGE",
                          "android.permission.WRITE_EXTERNAL_STORAGE",
                          "android.permission.MANAGE_EXTERNAL_STORAGE",
                          "android.permission.ACCESS_FINE_LOCATION"):
            with self.subTest(permission=forbidden):
                self.assertNotIn(forbidden, declared)

    def test_transitive_network_permissions_are_explicitly_removed(self):
        manifest = ET.parse(ROOT / "platforms/android/app/src/main/AndroidManifest.xml")
        android = "{http://schemas.android.com/apk/res/android}"
        tools = "{http://schemas.android.com/tools}"
        removed = {p.get(android + "name") for p in manifest.getroot().findall("uses-permission")
                   if p.get(tools + "node") == "remove"}
        self.assertIn("android.permission.INTERNET", removed)
        self.assertIn("android.permission.ACCESS_NETWORK_STATE", removed)


if __name__ == "__main__":
    unittest.main()

