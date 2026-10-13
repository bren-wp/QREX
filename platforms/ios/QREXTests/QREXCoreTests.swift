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

}
