#!/usr/bin/env python3
"""Policy gates are fail-closed even when a dependency modifies manifests."""
import unittest
from check_merged_manifest import validate
from check_16kb_bundle import load_alignments


class PlayPolicyGateTests(unittest.TestCase):
    def test_valid_minimal_manifest(self):
        xml = ('<manifest xmlns:android="http://schemas.android.com/apk/res/android">'
               '<uses-permission android:name="android.permission.CAMERA"/>'
               '<application android:allowBackup="false" '
               'android:usesCleartextTraffic="false"/></manifest>')
        self.assertEqual(validate(xml), [])

    def test_rejects_transitive_tracking_or_network_permission(self):
        for permission in ("android.permission.INTERNET",
                           "android.permission.ACCESS_NETWORK_STATE",
                           "com.google.android.gms.permission.AD_ID"):
            xml = ('<manifest xmlns:android="http://schemas.android.com/apk/res/android">'
                   f'<uses-permission android:name="{permission}"/>'
                   '<application android:allowBackup="false" '
                   'android:usesCleartextTraffic="false"/></manifest>')
            with self.subTest(permission=permission):
                self.assertTrue(validate(xml))

    def test_rejects_enabled_android_backup(self):
        xml = ('<manifest xmlns:android="http://schemas.android.com/apk/res/android">'
               '<application android:allowBackup="true" '
               'android:usesCleartextTraffic="false"/></manifest>')
        self.assertTrue(any("Backups" in item for item in validate(xml)))

    def test_valid_16k_alignment_readelf_lines(self):
        output = "  LOAD  0x0000 0x0000 0x0000 0x000120 0x000120 R E 0x4000\n"
        self.assertEqual(load_alignments(output), [0x4000])

    def test_small_alignment_readelf_lines(self):
        output = ("  LOAD  0x0000 0x0000 0x0000 0x000120 0x000120 R E 0x1000\n"
                  "  LOAD  0x3000 0x4000 0x4000 0x000200 0x000200 RW 0x4000\n")
        self.assertEqual(load_alignments(output), [0x1000, 0x4000])


if __name__ == "__main__":
    unittest.main()
