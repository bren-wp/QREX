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
        self.assertEqual(len(svg_paths), 13)
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

    def test_round_adaptive_and_monochrome_icons_keep_matching_safe_zone(self):
        foreground = (RES / "drawable/qrex_launcher_foreground.xml").read_text("utf-8")
        monochrome = (RES / "drawable/qrex_icon_monochrome.xml").read_text("utf-8")
        contour = (
            "M29,17 H79 C86,17 91,22 91,29 V79 C91,86 86,91 79,91 "
            "H29 C22,91 17,86 17,79 V29 C17,22 22,17 29,17 Z"
        )
        for icon in (foreground, monochrome):
            self.assertIn('android:scaleX="0.91"', icon)
            self.assertIn(contour, icon)
            self.assertIn('android:strokeWidth="0.8"', icon)
            self.assertIn("M44,39 C34.1,39", icon)
        splash = (KOTLIN / "QREXSplash.kt").read_text("utf-8")
        self.assertIn("CornerRadius((metrics.logoDp * .22f).dp.toPx())", splash)
        self.assertIn(".clip(RoundedCornerShape((metrics.logoDp * .22f).dp))", splash)

    def test_splash_reference_layers_keep_wordmark_clear_of_lower_ribbon(self):
        splash = (KOTLIN / "QREXSplash.kt").read_text("utf-8")
        self.assertIn("moveTo(0f, h * .76f)", splash)
        self.assertIn("w * .34f, h * .70f", splash)
        self.assertNotIn("w * .34f, h * .54f", splash)
        self.assertIn("moveTo(0f, h * .86f)", splash)
        self.assertIn("style = Stroke(width = 10.dp.toPx())", splash)
        self.assertIn("style = Stroke(width = 5.dp.toPx())", splash)
        self.assertIn("style = Stroke(width = 1.5.dp.toPx())", splash)
        self.assertIn("qrexSplashFooterLines(tr(\"splash_message\"), compactWidth)", splash)
        self.assertIn("bottomPaddingDp = if (heightDp < 620) 20 else 40", splash)

    def test_compact_scroll_brand_and_footer_use_entire_viewport(self):
        splash = (KOTLIN / "QREXSplash.kt").read_text("utf-8")
        self.assertIn('testTag("qrex-splash-viewport-distribution")', splash)
        self.assertIn(".heightIn(min = maxHeight)", splash)
        self.assertIn("verticalArrangement = Arrangement.SpaceBetween", splash)
        self.assertIn("Spacer(Modifier.height(0.dp))", splash)
        self.assertIn(".verticalScroll(rememberScrollState())", splash)
        self.assertNotIn("verticalArrangement = Arrangement.spacedBy(26.dp)", splash)

    def test_splash_never_overlays_footer_at_high_font_scale(self):
        splash = (KOTLIN / "QREXSplash.kt").read_text("utf-8")
        self.assertIn("qrexSplashUsesScrollLayout(", splash)
        self.assertIn("heightDp < 700 || fontScale >= 1.4f", splash)
        self.assertIn(".verticalScroll(rememberScrollState())", splash)
        self.assertIn('testTag("qrex-splash-scroll-content")', splash)
        self.assertIn("allowFullText = true", splash)
        self.assertNotIn('contentDescription = "QREX splash:', splash)
        self.assertNotIn('subtitle: String = "Scan. Create. Share."', splash)
        main = (KOTLIN / "MainActivity.kt").read_text("utf-8")
        self.assertIn('QrexSplashScreen(subtitle = tr("tagline"))', main)


if __name__ == "__main__":
    unittest.main()
