import unittest
from fdroid_dependency_audit import audit


class FdroidDependencyAuditTests(unittest.TestCase):
    def test_blocks_mlkit_transitive(self):
        text = "releaseRuntimeClasspath - Resolved runtime classpath\n+--- com.google.mlkit:barcode-scanning:17.3.0\n"
        self.assertIn("com.google.mlkit:barcode-scanning:17.3.0", audit(text))

    def test_does_not_block_open_androidx_and_zxing(self):
        text = ("releaseRuntimeClasspath - Resolved runtime classpath\n"
                "+--- androidx.camera:camera-camera2:1.4.2\n"
                "+--- com.google.zxing:core:3.5.3\n"
                "+--- androidx.compose.ui:ui:1.9.0")
        self.assertEqual([], audit(text))

    def test_empty_report_must_fail(self):
        self.assertTrue(audit("some other output"))

    def test_detects_other_proprietary_sdks(self):
        text = "releaseRuntimeClasspath\n+--- com.google.android.gms:play-services-ads:24.0.0\n"
        self.assertEqual(["com.google.android.gms:play-services-ads:24.0.0"], audit(text))
