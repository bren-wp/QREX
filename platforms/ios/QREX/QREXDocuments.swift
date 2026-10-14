import Foundation
import SwiftUI
import UniformTypeIdentifiers
import UIKit

/// A document that can be exported with the native Files document picker.
struct QREXZipDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.zip] }
    var contents: Data

    init(contents: Data) {
        self.contents = contents
    }

    init(configuration: ReadConfiguration) throws {
        guard let contents = configuration.file.regularFileContents else {
            throw CocoaError(.fileReadCorruptFile)
        }
        self.contents = contents
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: contents)
    }
}

/// A standalone printable PDF for the iOS Files document picker.
struct QREXPDFDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.pdf] }
    var contents: Data

    init(contents: Data) {
        self.contents = contents
    }

    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents,
              data.starts(with: Data("%PDF-".utf8)) else {
            throw CocoaError(.fileReadCorruptFile)
        }
        contents = data
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        guard contents.starts(with: Data("%PDF-".utf8)) else {
            throw CocoaError(.fileReadCorruptFile)
        }
        return FileWrapper(regularFileWithContents: contents)
    }
}

/// A real PNG file that users can save through the iOS Files picker.
struct QREXPNGDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.png] }
    var contents: Data

    init(contents: Data) {
        self.contents = contents
    }

    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents,
              data.starts(with: Data([137, 80, 78, 71, 13, 10, 26, 10])) else {
            throw CocoaError(.fileReadCorruptFile)
        }
        contents = data
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        guard contents.starts(with: Data([137, 80, 78, 71, 13, 10, 26, 10])) else {
            throw CocoaError(.fileReadCorruptFile)
        }
        return FileWrapper(regularFileWithContents: contents)
    }
}

/// Local-only document export. QR payloads are embedded in PDF QR artwork;
/// no raw passwords in ZIP entries, file names, logs, or cloud requests.
enum QREXDocuments {
    enum ExportError: Error { case invalidHistory, invalidCode, oversizedArchive }

    static func exportSavedPdfZip(_ records: [QRHistoryItem]) throws -> Data {
        let pdf = try createSavedCodesPdf(records)
        return try archive(file: "QREX-Saved-Codes.pdf", payload: pdf)
    }

    static func createSavedCodesPdf(_ records: [QRHistoryItem]) throws -> Data {
        guard !records.isEmpty, records.count <= 250,
              records.allSatisfy({ $0.saved && !$0.raw.isEmpty && $0.raw.count <= 10_000 }) else {
            throw ExportError.invalidHistory
        }
        // A4-ish PDF pages: one readable and printable QR code on each page.
        // Render one QR image per page to bound peak memory usage when users
        // have hundreds of saved codes. Do not hand out partial PDFs on failure.
        var failed = false
        let pdf = UIGraphicsPDFRenderer(bounds: CGRect(x: 0, y: 0, width: 595, height: 842))
        let result = pdf.pdfData { context in
            for (index, record) in records.enumerated() {
                guard let image = QREXImage.qr(record.raw) else {
                    failed = true
                    break
                }
                context.beginPage()
                let titleAttributes: [NSAttributedString.Key: Any] = [
                    .font: UIFont.boldSystemFont(ofSize: 30),
                    .foregroundColor: UIColor(red: 3 / 255, green: 12 / 255, blue: 27 / 255, alpha: 1)
                ]
                ("QREX" as NSString).draw(at: CGPoint(x: 58, y: 60), withAttributes: titleAttributes)
                let details: [NSAttributedString.Key: Any] = [
                    .font: UIFont.systemFont(ofSize: 13),
                    .foregroundColor: UIColor.darkGray
                ]
                ("Scan. Create. Share." as NSString).draw(at: CGPoint(x: 58, y: 109),
                                                          withAttributes: details)
                image.draw(in: CGRect(x: 78, y: 188, width: 439, height: 439))
                ("Saved code \(index + 1) of \(records.count)" as NSString)
                    .draw(at: CGPoint(x: 58, y: 737), withAttributes: details)
                ("Created with QREX by Brendigo" as NSString)
                    .draw(at: CGPoint(x: 58, y: 762), withAttributes: details)
            }
        }
        guard !failed else { throw ExportError.invalidCode }
        guard result.count <= 60_000_000 else { throw ExportError.oversizedArchive }
        return result
    }

    /// ZIP stored-entry writer (PKZIP 2.0) to avoid a third-party SDK.
    /// CRC32 and central-directory offsets are covered by XCTest.
    static func archive(file: String, payload: Data) throws -> Data {
        guard payload.count <= 60_000_000,
              let name = file.data(using: .utf8), name.count <= 255 else {
            throw ExportError.oversizedArchive
        }
        let checksum = crc32(payload)
        let size = UInt32(payload.count)
        var output = Data()
        func u16(_ n: UInt16) {
            output.append(UInt8(truncatingIfNeeded: n))
            output.append(UInt8(truncatingIfNeeded: n >> 8))
        }
        func u32(_ n: UInt32) {
            u16(UInt16(truncatingIfNeeded: n))
            u16(UInt16(truncatingIfNeeded: n >> 16))
        }
        let flags: UInt16 = 0x0800 // UTF-8 filename
        // Local file header
        u32(0x04034b50)
        u16(20); u16(flags); u16(0) // ZIP version, flags, stored compression
        u16(0); u16(0) // deterministic timestamp/date
        u32(checksum); u32(size); u32(size)
        u16(UInt16(name.count)); u16(0)
        output.append(name)
        output.append(payload)
        let centralOffset = UInt32(output.count)
        u32(0x02014b50)
        u16(20); u16(20); u16(flags); u16(0)
        u16(0); u16(0)
        u32(checksum); u32(size); u32(size)
        u16(UInt16(name.count)); u16(0); u16(0)
        u16(0); u16(0); u32(0); u32(0)
        output.append(name)
        let centralSize = UInt32(output.count) - centralOffset
        // End of central directory
        u32(0x06054b50)
        u16(0); u16(0); u16(1); u16(1)
        u32(centralSize); u32(centralOffset); u16(0)
        return output
    }

    private static let crcTable: [UInt32] = (0..<256).map { value in
        var result = UInt32(value)
        for _ in 0..<8 {
            result = (result >> 1) ^ ((result & 1) == 1 ? 0xEDB88320 : 0)
        }
        return result
    }

    private static func crc32(_ bytes: Data) -> UInt32 {
        var crc: UInt32 = 0xFFFF_FFFF
        for byte in bytes {
            let index = Int((crc ^ UInt32(byte)) & 0xFF)
            crc = (crc >> 8) ^ crcTable[index]
        }
        return ~crc
    }
}
