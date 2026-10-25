#!/usr/bin/env python3
"""QREX splash / brand identity guards; no image or emulator fabrication."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "platforms/android/app/src/main/res"
KOTLIN = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"
ANDROID = "{http://schemas.android.com/apk/res/android}"


class SplashBrandParityTest(unittest.TestCase):
    def test_svg_has_same_geometry_as_full_color_android_vector(self):
        vector = ET.parse(RES / "drawable/qrex_icon.xml").getroot()
        svg = ET.parse(ROOT / "assets/brand/qrex-icon.svg").getroot()
        android_paths = [
            element.attrib[f"{ANDROID}pathData"]
            for element in vector.iter()
            if f"{ANDROID}pathData" in element.attrib
        ]
        svg_paths = [
            element.attrib["d"]
            for element in svg.iter()
            if element.tag.endswith("}path") and "d" in element.attrib
        ]
        self.assertEqual(android_paths, svg_paths,
                         "The external brand SVG drifted from the in-app QREX mark")
        self.assertEqual(len(svg_paths), 12)
        vtext = (RES / "drawable/qrex_icon.xml").read_text("utf-8")
        stext = (ROOT / "assets/brand/qrex-icon.svg").read_text("utf-8")
        for color in ("#19CAFF", "#0868EE", "#07187E",
                      "#54E9FF", "#E5F9FF", "#41C9FF",
                      "#3FD0FF", "#13E3F1", "#1151BA"):
            with self.subTest(color=color):
                self.assertIn(color, vtext)
                self.assertIn(color, stext)

    def test_launcher_and_android_12_splash_keep_canonical_brand(self):
        for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
            xml = (RES / "mipmap-anydpi-v26" / name).read_text("utf-8")
            self.assertIn("@drawable/qrex_launcher_background", xml)
            self.assertIn("@drawable/qrex_launcher_foreground", xml)
            self.assertIn("@drawable/qrex_icon_monochrome", xml)
        for name in ("qrex_launcher_foreground.xml", "qrex_icon_monochrome.xml"):
            self.assertIn("M51,64 L59,57 L76,77 L68,84 Z",
                          (RES / "drawable" / name).read_text("utf-8"))
        styles = (RES / "values-v31/styles.xml").read_text("utf-8")
        self.assertIn("@drawable/qrex_icon", styles)

    def test_splash_never_overlays_footer_at_high_font_scale(self):
        splash = (KOTLIN / "QREXSplash.kt").read_text("utf-8")
        self.assertIn("qrexSplashUsesScrollLayout(", splash)
        self.assertIn("heightDp < 640 || fontScale >= 1.5f", splash)
        self.assertIn(".verticalScroll(rememberScrollState())", splash)
        self.assertIn('testTag("qrex-splash-scroll-content")', splash)
        self.assertIn("allowFullText = true", splash)
        self.assertNotIn('contentDescription = "QREX splash:', splash)
        self.assertNotIn('subtitle: String = "Scan. Create. Share."', splash)
        main = (KOTLIN / "MainActivity.kt").read_text("utf-8")
        self.assertIn('QrexSplashScreen(subtitle = tr("tagline"))', main)


if __name__ == "__main__":
    unittest.main()
