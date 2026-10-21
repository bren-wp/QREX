#!/usr/bin/env python3
"""Regression checks for the Android foreground screenshot gate."""
import unittest
from check_android_screen import validate


def screen(body: str) -> str:
    return '<?xml version="1.0"?><hierarchy rotation="0">' + body + '</hierarchy>'


class ForegroundTests(unittest.TestCase):
    def test_real_app(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="Create QR code" />'
            '<node package="com.brendigo.qrex" text="https://brendigo.com" />'
            '<node package="com.brendigo.qrex" text="More options" />'
        ))

    def test_genuine_result_screen_is_accepted(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="Scan complete" />'
            '<node package="com.brendigo.qrex" text="https://brendigo.com" />'
            '<node package="com.brendigo.qrex" text="Close" />'
        ), "result")

    def test_dedicated_saved_screen_is_accepted(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="Saved" />'
            '<node package="com.brendigo.qrex" text="brendigo.com" />'
        ), "saved")

    def test_normal_history_screen_is_accepted(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="History" />'
            '<node package="com.brendigo.qrex" text="brendigo.com" />'
            '<node package="com.brendigo.qrex" text="Saved" />'
        ), "history")

    def test_wrong_screen_or_anr_is_rejected_even_with_qrex_package(self):
        with self.assertRaises(ValueError):
            validate(screen(
                '<node package="com.brendigo.qrex" text="Scan complete" />'
                '<node package="com.brendigo.qrex" text="https://brendigo.com" />'
            ), "create")
        with self.assertRaises(ValueError):
            validate(screen(
                '<node package="com.brendigo.qrex" text="Scan QR code" />'
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
