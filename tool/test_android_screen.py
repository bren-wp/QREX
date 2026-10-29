#!/usr/bin/env python3
"""Regression checks for the Android foreground screenshot gate."""
import unittest
from check_android_screen import validate


def screen(body: str) -> str:
    return '<?xml version="1.0"?><hierarchy rotation="0">' + body + '</hierarchy>'


class ForegroundTests(unittest.TestCase):
    def test_actual_dark_splash(self):
        validate(screen(
            '<node package="com.brendigo.qrex" content-desc="QREX" />'
            '<node package="com.brendigo.qrex" text="Skener. Stvara. Dijeli." />'
            '<node package="com.brendigo.qrex" text="Sve što može QR — jednostavno, brzo i bez prijave." />'
        ), "splash")

    def test_splash_is_not_confused_with_normal_qrex_header(self):
        # Scan and Create repeat QREX + tagline, but not the Splash footer.
        with self.assertRaisesRegex(ValueError, "sve što može qr"):
            validate(screen(
                '<node package="com.brendigo.qrex" content-desc="QREX" />'
                '<node package="com.brendigo.qrex" text="Skener. Stvara. Dijeli." />'
                '<node package="com.brendigo.qrex" text="Iz galerije" />'
            ), "splash")

    def test_result_old_close_label_does_not_pass_back_button_check(self):
        with self.assertRaisesRegex(ValueError, "natrag"):
            validate(screen(
                '<node package="com.brendigo.qrex" text="Rezultat" />'
                '<node package="com.brendigo.qrex" text="https://brendigo.com" />'
                '<node package="com.brendigo.qrex" content-desc="Zatvori" />'
            ), "result")

    def test_real_app(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="Stvori QR kod" />'
            '<node package="com.brendigo.qrex" text="https://brendigo.com" />'
            '<node package="com.brendigo.qrex" text="Wi-Fi" />'
            '<node package="com.brendigo.qrex" text="Generiraj QR kod" />'
            '<node package="com.brendigo.qrex" text="Spremi na uređaj" />'
        ))

    def test_create_screen_without_generate_and_save_is_rejected(self):
        # The screenshot gate must fail when an over-sized blank QR preview
        # replaces or pushes the essential actions off the real screen.
        obscured = screen(
            '<node package="com.brendigo.qrex" text="Stvori QR kod" />'
            '<node package="com.brendigo.qrex" text="https://brendigo.com" />'
            '<node package="com.brendigo.qrex" text="Wi-Fi" />'
        )
        for mode in ("create", "responsive-create"):
            with self.subTest(mode=mode):
                with self.assertRaisesRegex(ValueError, "generiraj qr kod"):
                    validate(obscured, mode)

    def test_genuine_result_screen_is_accepted(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="Rezultat" />'
            '<node package="com.brendigo.qrex" text="https://brendigo.com" />'
            '<node package="com.brendigo.qrex" content-desc="Natrag" />'
        ), "result")

    def test_genuine_history_screen_is_accepted(self):
        validate(screen(
            '<node package="com.brendigo.qrex" text="Favoriti" />'
            '<node package="com.brendigo.qrex" text="brendigo.com" />'
            '<node package="com.brendigo.qrex" text="Pretraži povijest..." />'
            '<node package="com.brendigo.qrex" text="Spremljeno" />'
        ), "saved")


    def test_all_additional_responsive_screens_validate_real_content(self):
        cases = {
            "responsive-splash": "QREX Skener. Stvara. Dijeli.",
            "responsive-result": "Rezultat https://brendigo.com Natrag",
            "responsive-history": "Povijest brendigo.com Pretraži povijest...",
            "responsive-saved": "Favoriti brendigo.com Spremljeno",
            "responsive-wifi": "Stvori QR kod QREX-Demo Wi-Fi",
            "responsive-language": "Jezik Hrvatski English",
            "responsive-export": "Izvoz arhive PDF",
        }
        for mode, content in cases.items():
            with self.subTest(mode=mode):
                validate(screen('<node package="com.brendigo.qrex" text="' +
                                content + '" />'), mode)

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

    def test_missing_expected_markers_report_only_fixed_fixture_strings(self):
        wrong = screen(
            '<node package="com.brendigo.qrex" text="Stvori QR kod" />'
            '<node package="com.brendigo.qrex" text="private_user_data_do_not_log" />'
        )
        with self.assertRaisesRegex(ValueError, "absent expected markers:"):
            validate(wrong, "create")
        try:
            validate(wrong, "create")
        except ValueError as failure:
            self.assertNotIn("private_user_data_do_not_log", str(failure))

    def test_empty_black_screen_is_rejected(self):
        with self.assertRaises(ValueError):
            validate(screen('<node package="com.android.systemui" text="" />'))

    def test_wrong_view_is_rejected(self):
        with self.assertRaises(ValueError):
            validate(screen('<node package="com.brendigo.qrex" text="History" />'))


if __name__ == "__main__":
    unittest.main()
