import XCTest
import UniformTypeIdentifiers
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

    func testWhitespaceOnlyQrCannotBeGenerated() {
        XCTAssertFalse(QRContent.isSupported("   "))
        XCTAssertFalse(QRContent.isSupported("\n\t"))
        XCTAssertFalse(QRContent.isSupported(String(repeating: "x", count: 1501)))
        XCTAssertTrue(QRContent.isSupported("QR content"))
    }

    func testWhitespaceWifiPasswordMatchesAndroid() {
        let secret = QRContent.wifi(ssid: "Private", password: "        ")
        XCTAssertEqual(QRContent.wifiField(secret, key: "T"), "WPA")
        XCTAssertEqual(QRContent.wifiField(secret, key: "P"), "        ")
        XCTAssertEqual(QRContent.wifiField(
            QRContent.wifi(ssid: "Public", password: ""), key: "T"), "nopass")
    }

    func testGalleryPolicyBoundsHighResolutionFrames() {
        XCTAssertTrue(QREXGalleryPolicy.accepts(byteCount: 3_000_000, width: 4000, height: 3000))
        XCTAssertFalse(QREXGalleryPolicy.accepts(byteCount: 3_000_000, width: 6000, height: 5000))
        XCTAssertFalse(QREXGalleryPolicy.accepts(byteCount: 20_000_001, width: 100, height: 100))
        XCTAssertFalse(QREXGalleryPolicy.accepts(byteCount: 0, width: 100, height: 100))
        XCTAssertFalse(QREXGalleryPolicy.accepts(byteCount: 1000, width: 0, height: 100))
        XCTAssertLessThanOrEqual(QREXGalleryPolicy.maxThumbnailSide, 3072)
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
    func testFlutterFavoriteUpgradesExistingUnsavedQrWithoutChangingIdentity() {
        let initial = QREXStore()
        XCTAssertTrue(initial.clearAll())
        let shared = "https://example.com/shared"
        initial.record(shared)
        let original = try! XCTUnwrap(initial.items.first)
        XCTAssertFalse(original.saved)

        let old = """
        [{"raw":"https://example.com/shared","date":"2026-10-09T10:12:13.123","saved":true}]
        """
        UserDefaults.standard.set(old, forKey: "flutter.qrex_history_v1")
        let migrated = QREXStore()
        XCTAssertEqual(migrated.items.count, 1)
        XCTAssertEqual(migrated.items.first?.id, original.id)
        XCTAssertTrue(migrated.items.first?.saved == true)
        XCTAssertNil(UserDefaults.standard.string(forKey: "flutter.qrex_history_v1"))
        XCTAssertTrue(QREXStore().items.first?.saved == true)
        XCTAssertTrue(migrated.clearAll())
    }

    @MainActor
    func testRepeatedFlutterQrKeepsSavedStatusAcrossDuplicateRows() {
        let initial = QREXStore()
        XCTAssertTrue(initial.clearAll())
        let raw = "https://example.com/legacy-duplicate"
        let old = """
        [{"raw":"https://example.com/legacy-duplicate","date":"2026-10-09T10:12:13.123","saved":false},
         {"raw":"https://example.com/legacy-duplicate","date":"2026-10-09T10:12:14.123","saved":true}]
        """
        UserDefaults.standard.set(old, forKey: "flutter.qrex_history_v1")
        let migrated = QREXStore()
        XCTAssertEqual(migrated.items.count, 1)
        XCTAssertEqual(migrated.items.first?.raw, raw)
        XCTAssertTrue(migrated.items.first?.saved == true)
        XCTAssertTrue(QREXStore().items.first?.saved == true)
        XCTAssertTrue(migrated.clearAll())
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

    @MainActor
    func testCorruptKeychainHistoryIsNeverOverwrittenByNextSave() {
        let store = QREXStore()
        XCTAssertTrue(store.clearAll())
        let original = Data("not-valid-json-private-history".utf8)
        XCTAssertTrue(QREXHistoryVault.write(original))
        let reopened = QREXStore()
        XCTAssertTrue(reopened.items.isEmpty)
        XCTAssertFalse(reopened.save("https://example.com/new"))
        XCTAssertEqual(QREXHistoryVault.read(), original)
        XCTAssertTrue(reopened.clearAll())
        XCTAssertTrue(QREXStore().save("https://example.com/recovered"))
        XCTAssertTrue(QREXStore().clearAll())
    }

    @MainActor
    func testTooManyKeychainRowsCannotBeSilentlyTruncatedAndOverwritten() throws {
        let store = QREXStore()
        XCTAssertTrue(store.clearAll())
        let rows = (0...250).map { i in
            QRHistoryItem(id: UUID(), raw: "https://example.com/\\(i)",
                timestamp: Date(timeIntervalSince1970: Double(i)), saved: true)
        }
        let original = try JSONEncoder().encode(rows)
        XCTAssertTrue(QREXHistoryVault.write(original))
        let reopened = QREXStore()
        XCTAssertTrue(reopened.items.isEmpty)
        XCTAssertFalse(reopened.save("https://example.com/new"))
        XCTAssertEqual(QREXHistoryVault.read(), original)
        XCTAssertTrue(reopened.clearAll())
    }

    @MainActor
    func testFullSavedHistoryNeverEvictsFavorites() throws {
        let initial = QREXStore()
        XCTAssertTrue(initial.clearAll())
        let favourites = (0..<250).map { index in
            QRHistoryItem(id: UUID(), raw: "https://example.com/favorite/\(index)",
                          timestamp: Date(timeIntervalSince1970: Double(index)), saved: true)
        }
        let protected = try JSONEncoder().encode(favourites)
        XCTAssertTrue(QREXHistoryVault.write(protected))
        let store = QREXStore()
        XCTAssertEqual(store.items.count, 250)
        XCTAssertFalse(store.save("https://example.org/251"))
        store.record("https://example.org/auto-scan")
        XCTAssertEqual(store.items.count, 250)
        XCTAssertTrue(store.items.allSatisfy(\.saved))
        XCTAssertTrue(store.items.contains(where: { $0.raw == favourites[249].raw }))
        XCTAssertEqual(QREXHistoryVault.read(), protected)
        XCTAssertTrue(store.clearAll())
    }

    func testPngExportContainsRealPngDataAndUsesPngDocumentType() throws {
        let png = try XCTUnwrap(QREXImage.qr("https://brendigo.com")?.pngData())
        XCTAssertTrue(png.starts(with: Data([137, 80, 78, 71, 13, 10, 26, 10])))
        let document = QREXPNGDocument(contents: png)
        XCTAssertEqual(QREXPNGDocument.readableContentTypes, [.png])
        XCTAssertEqual(document.contents, png)
    }

    func testZipArchiveHasValidHeadersAndExpectedCRC() throws {
        let bytes = try QREXDocuments.archive(file: "test.txt",
                                              payload: Data("hello".utf8))
        XCTAssertGreaterThan(bytes.count, 90)
        let raw = [UInt8](bytes)
        XCTAssertEqual(Array(raw[0..<4]), [0x50, 0x4B, 0x03, 0x04])
        XCTAssertEqual(Array(raw[14..<18]), [0x86, 0xA6, 0x10, 0x36])
        XCTAssertTrue(bytes.range(of: Data("test.txt".utf8)) != nil)
        XCTAssertTrue(bytes.range(of: Data([0x50, 0x4B, 0x01, 0x02])) != nil)
        XCTAssertEqual(Array(raw.suffix(22).prefix(4)), [0x50, 0x4B, 0x05, 0x06])
    }

    func testStandalonePdfIsPrintableAndUsesPdfDocumentType() throws {
        let record = QRHistoryItem(id: UUID(), raw: "https://brendigo.com",
                                   timestamp: Date(), saved: true)
        let data = try QREXDocuments.createSavedCodesPdf([record])
        XCTAssertTrue(data.starts(with: Data("%PDF-".utf8)))
        XCTAssertGreaterThan(data.count, 1000)
        let document = QREXPDFDocument(contents: data)
        XCTAssertEqual(QREXPDFDocument.readableContentTypes, [.pdf])
        XCTAssertEqual(document.contents, data)
        XCTAssertThrowsError(try QREXDocuments.createSavedCodesPdf([]))
    }

    func testSavedQrPdfZipContainsPrintablePdf() throws {
        let record = QRHistoryItem(id: UUID(), raw: "https://brendigo.com",
                                   timestamp: Date(), saved: true)
        let archive = try QREXDocuments.exportSavedPdfZip([record])
        XCTAssertTrue(archive.starts(with: Data([0x50, 0x4B, 0x03, 0x04])))
        XCTAssertNotNil(archive.range(of: Data("%PDF-".utf8)))
        XCTAssertNotNil(archive.range(of: Data("QREX-Saved-Codes.pdf".utf8)))
        XCTAssertThrowsError(try QREXDocuments.exportSavedPdfZip([]))
    }

}
