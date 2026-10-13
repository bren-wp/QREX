import XCTest
@testable import QREX

final class QREXCoreTests: XCTestCase {
    func testAllLanguagesAreAvailable() {
        XCTAssertEqual(QREXStrings.supported.count, 23)
        for language in QREXStrings.supported {
            XCTAssertNotNil(Bundle.main.path(forResource: language, ofType: "lproj"))
            XCTAssertNotEqual(QREXStrings.text("scan", language: language), "scan")
        }
        XCTAssertEqual(QREXStrings.text("scan", language: "en"), "Scan")
        XCTAssertEqual(QREXStrings.text("scan", language: "hr"), "Skeniraj")
    }


    func testEscapedWifiFields() {
        let raw = QRContent.wifi(ssid: "Office;P:wrong", password: "a\\b:c")
        XCTAssertEqual(QRContent.wifiField(raw, key: "S"), "Office;P:wrong")
        XCTAssertEqual(QRContent.wifiField(raw, key: "P"), "a\\b:c")
    }

    func testUnsafeUrls() {
        XCTAssertNil(QRContent.safeURL("javascript:alert(1)"))
        XCTAssertNil(QRContent.safeURL("https://trusted.com@malicious.com"))
        XCTAssertNil(QRContent.safeURL("https://example.com/test path"))
        XCTAssertEqual(QRContent.safeURL("https://brendigo.com")?.host, "brendigo.com")
    }

    @MainActor
    func testKeychainHistoryNeverFallsBackToPlainPreferences() {
        let store = QREXStore()
        store.clearAll()
        let payload = QRContent.wifi(ssid: "Private Network", password: "never-in-defaults")
        store.save(payload)
        XCTAssertEqual(store.items.first?.raw, payload)
        XCTAssertNil(UserDefaults.standard.data(forKey: "qrex.history"))
        XCTAssertNotNil(QREXHistoryVault.read())
        store.clearAll()
        XCTAssertNil(QREXHistoryVault.read())
    }

    @MainActor
    func testFlutterHistoryMigrationPreservesSavedCodes() {
        let initial = QREXStore()
        initial.clearAll()
        let input = "[{\"raw\":\"https://brendigo.com\",\"date\":\"2026-10-09T10:12:13.123\",\"saved\":true}]"
        UserDefaults.standard.set(input, forKey: "flutter.qrex_history_v1")
        let restored = QREXStore()
        XCTAssertTrue(restored.items.contains {
            $0.raw == "https://brendigo.com" && $0.saved
        })
        XCTAssertNil(UserDefaults.standard.string(forKey: "flutter.qrex_history_v1"))
        if let migrated = restored.items.first {
            let calendar = Calendar(identifier: .gregorian)
            let components = calendar.dateComponents(in: TimeZone(secondsFromGMT: 0)!, from: migrated.timestamp)
            XCTAssertEqual(components.year, 2026)
            XCTAssertEqual(components.month, 10)
            XCTAssertEqual(components.day, 9)
        } else {
            XCTFail("Missing migrated entry")
        }
        restored.clearAll()
    }

    @MainActor
    func testNoAutomaticWifiHistory() {
        let store = QREXStore()
        store.clearAll()
        let raw = QRContent.wifi(ssid: "Home", password: "secret")
        store.record(raw)
        XCTAssertTrue(store.items.isEmpty)
        store.save(raw)
        XCTAssertEqual(store.items.count, 1)
        store.clearAll()
        XCTAssertTrue(store.items.isEmpty)
    }
    @MainActor
    func testIncompleteFlutterMigrationKeepsOriginalRecords() {
        let initial = QREXStore()
        initial.clearAll()
        let invalidDate = "[{\"raw\":\"WIFI:T:WPA;S:Home;P:secret;;\",\"date\":\"not-a-date\",\"saved\":true}]"
        UserDefaults.standard.set(invalidDate, forKey: "flutter.qrex_history_v1")
        let restored = QREXStore()
        XCTAssertTrue(restored.items.isEmpty)
        XCTAssertEqual(
            UserDefaults.standard.string(forKey: "flutter.qrex_history_v1"),
            invalidDate
        )
        UserDefaults.standard.removeObject(forKey: "flutter.qrex_history_v1")
        restored.clearAll()
    }

    @MainActor
    func testDeleteAllErasesPendingFlutterSecretsAndPreventsResurrection() {
        let store = QREXStore()
        XCTAssertTrue(store.clearAll())
        let original = "[{\"raw\":\"WIFI:T:WPA;S:Office;P:private;;\",\"date\":\"invalid\",\"saved\":true}]"
        UserDefaults.standard.set(original, forKey: "flutter.qrex_history_v1")
        UserDefaults.standard.set(false, forKey: "flutter.qrex_record_v1")
        UserDefaults.standard.set(true, forKey: "flutter.qrex_light_v1")
        let pending = QREXStore()
        XCTAssertTrue(pending.items.isEmpty)
        XCTAssertTrue(pending.clearAll())
        XCTAssertNil(UserDefaults.standard.string(forKey: "flutter.qrex_history_v1"))
        XCTAssertNil(UserDefaults.standard.object(forKey: "flutter.qrex_record_v1"))
        XCTAssertNil(UserDefaults.standard.object(forKey: "flutter.qrex_light_v1"))
        XCTAssertTrue(QREXStore().items.isEmpty)
    }

    @MainActor
    func testFreshInstallDoesNotResurrectOrphanedKeychainHistory() {
        let initial = QREXStore()
        XCTAssertTrue(initial.clearAll())
        initial.save(QRContent.wifi(ssid: "Old install", password: "private"))
        XCTAssertNotNil(QREXHistoryVault.read())

        // Emulate app UserDefaults being reset while Keychain survives uninstall.
        UserDefaults.standard.removeObject(forKey: "qrex.installation.vault")
        let fresh = QREXStore()
        XCTAssertTrue(fresh.items.isEmpty)
        XCTAssertNil(QREXHistoryVault.read())
        XCTAssertTrue(UserDefaults.standard.bool(forKey: "qrex.installation.vault"))
        XCTAssertTrue(fresh.clearAll())
    }

    @MainActor
    func testCoexistingKeychainAndLegacyHistoryMergeWithoutLosingSavedFlags() {
        let initial = QREXStore()
        XCTAssertTrue(initial.clearAll())
        let shared = "https://example.com/shared"
        initial.record(shared)
        let original = QRHistoryItem(id: UUID(), raw: shared,
            timestamp: Date(timeIntervalSince1970: 1_700_000_000), saved: true)
        let extra = QRHistoryItem(id: UUID(), raw: "https://example.com/legacy",
            timestamp: Date(timeIntervalSince1970: 1_700_000_100), saved: true)
        let data = try! JSONEncoder().encode([original, extra])
        UserDefaults.standard.set(data, forKey: "qrex.history")

        let reopened = QREXStore()
        XCTAssertEqual(reopened.items.count, 2)
        XCTAssertTrue(reopened.items.contains { $0.raw == shared && $0.saved })
        XCTAssertTrue(reopened.items.contains { $0.raw == extra.raw && $0.saved })
        XCTAssertNil(UserDefaults.standard.data(forKey: "qrex.history"))
        XCTAssertTrue(reopened.clearAll())
    }

    @MainActor
    func testOversizedQrCannotCorruptKeychainHistory() {
        let store = QREXStore()
        XCTAssertTrue(store.clearAll())
        let oversized = String(repeating: "x", count: 10001)
        XCTAssertFalse(store.save(oversized))
        store.record(oversized)
        XCTAssertTrue(store.items.isEmpty)
        XCTAssertTrue(QREXStore().items.isEmpty)
        XCTAssertTrue(store.clearAll())
    }

}
