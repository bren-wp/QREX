#!/usr/bin/env python3
"""Regression checks for the Android foreground screenshot gate."""
import unittest
from check_android_screen import validate


def screen(body: str) -> str:
    return '<?xml version="1.0"?><hierarchy rotation="0">' + body + '</hierarchy>'


class ForegroundTests(unittest.TestCase):
    def test_actual_dark_splash(self):
        validate(screen(
            '<node package="com.brendigo.qrex" content-desc="QREX splash: Skener. Stvara. Dijeli." />'
            '<node package="com.brendigo.qrex" text="Skener. Stvara. Dijeli." />'
        ), "splash")

    def test_real_app(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="Stvori QR kod" />'
            '<node package="com.brendigo.qrex" text="https://brendigo.com" />'
            '<node package="com.brendigo.qrex" text="Wi-Fi" />'
        ))

    def test_genuine_result_screen_is_accepted(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="Rezultat" />'
            '<node package="com.brendigo.qrex" text="https://brendigo.com" />'
            '<node package="com.brendigo.qrex" content-desc="Zatvori" />'
        ), "result")

    def test_genuine_history_screen_is_accepted(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="Favoriti" />'
            '<node package="com.brendigo.qrex" text="brendigo.com" />'
            '<node package="com.brendigo.qrex" text="Pretraži povijest..." />'
            '<node package="com.brendigo.qrex" text="Spremljeno" />'
        ), "saved")


    def test_wifi_create_fixture_is_visible(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="Stvori QR kod" />'
            '<node package="com.brendigo.qrex" text="QREX-Demo" />'
            '<node package="com.brendigo.qrex" text="Wi-Fi" />'
        ), "wifi")

    def test_live_scan_screen_has_camera_actions(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="Skener. Stvara. Dijeli." />'
            '<node package="com.brendigo.qrex" text="Iz galerije" />'
            '<node package="com.brendigo.qrex" text="Usmjeri kameru prema QR kodu" />'
        ), "scan")

    def test_wrong_screen_or_anr_is_rejected_even_with_qrex_package(self):
        with self.assertRaises(ValueError):
            validate(screen(
                '<node package="com.brendigo.qrex" text="Rezultat" />'
                '<node package="com.brendigo.qrex" text="https://brendigo.com" />'
            ), "create")
        with self.assertRaises(ValueError):
            validate(screen(
                '<node package="com.brendigo.qrex" text="Skeniraj QR kod" />'
                '<node package="com.android.systemui" text="App isn\'t responding" />'
            ), "scan")

    def test_system_ui_anr_is_rejected(self):
        with self.assertRaises(ValueError):
            validate(screen(
                '<node package="com.android.systemui" text="System UI isn\'t responding" />'
                '<node package="com.android.systemui" text="Wait" />'
            ))

    def test_empty_black_screen_is_rejected(self):
        with self.assertRaises(ValueError):
            validate(screen('<node package="com.android.systemui" text="" />'))

    def test_wrong_view_is_rejected(self):
        with self.assertRaises(ValueError):
            validate(screen('<node package="com.brendigo.qrex" text="History" />'))


if __name__ == "__main__":
    unittest.main()
