import Foundation
import Combine

enum QRContent {
    static func isWifi(_ raw: String) -> Bool { raw.uppercased().hasPrefix("WIFI:") }

    static func wifiField(_ raw: String, key: String) -> String? {
        guard isWifi(raw), ["T", "S", "P", "H"].contains(key) else { return nil }
        var values: [String: String] = [:]
        var part = ""
        var parts: [String] = []
        var escaping = false
        for character in raw.dropFirst(5) {
            if escaping {
                part.append(character)
                escaping = false
            } else if character == "\\" {
                escaping = true
            } else if character == ";" {
                parts.append(part)
                part = ""
            } else {
                part.append(character)
            }
        }
        if escaping { return nil }
        if !part.isEmpty { parts.append(part) }
        for item in parts {
            if let index = item.firstIndex(of: ":") {
                values[String(item[..<index])] = String(item[item.index(after: index)...])
            }
        }
        return values[key]
    }

    static func wifi(ssid: String, password: String) -> String {
        func escape(_ text: String) -> String {
            text.replacingOccurrences(of: "\\", with: "\\\\")
                .replacingOccurrences(of: ";", with: "\\;")
                .replacingOccurrences(of: ":", with: "\\:")
                .replacingOccurrences(of: ",", with: "\\,")
                .replacingOccurrences(of: "\"", with: "\\\"")
        }
        return "WIFI:T:\(password.isEmpty ? "nopass" : "WPA");S:\(escape(ssid));P:\(escape(password));;"
    }

    static func safeURL(_ raw: String) -> URL? {
        guard raw == raw.trimmingCharacters(in: .whitespacesAndNewlines),
              !raw.contains("\\"), !raw.unicodeScalars.contains(where: { CharacterSet.controlCharacters.contains($0) }),
              !raw.unicodeScalars.contains(where: { CharacterSet.whitespacesAndNewlines.contains($0) }),
              let components = URLComponents(string: raw),
              ["http", "https"].contains(components.scheme?.lowercased() ?? ""),
              components.host?.isEmpty == false,
              components.user == nil, components.password == nil else { return nil }
        return components.url
    }

    static func title(_ raw: String) -> String {
        if isWifi(raw) { return wifiField(raw, key: "S") ?? "Wi-Fi" }
        if let url = safeURL(raw), let host = url.host { return host }
        return String(raw.replacingOccurrences(of: "\n", with: " ").prefix(56))
    }

    static func isSupported(_ raw: String) -> Bool {
        !raw.isEmpty && raw.utf8.count <= 1500
    }
}

struct QRHistoryItem: Codable, Identifiable, Equatable {
    let id: UUID
    let raw: String
    let timestamp: Date
    let saved: Bool
}

@MainActor
final class QREXStore: ObservableObject {
    @Published private(set) var items: [QRHistoryItem] = []
    @Published var historyEnabled: Bool {
        didSet { UserDefaults.standard.set(historyEnabled, forKey: "qrex.history.enabled") }
    }
    @Published var lightMode: Bool {
        didSet { UserDefaults.standard.set(lightMode, forKey: "qrex.light") }
    }
    @Published var language: String {
        didSet { UserDefaults.standard.set(language, forKey: "qrex.language") }
    }

    init(defaults: UserDefaults = .standard) {
        historyEnabled = defaults.object(forKey: "qrex.history.enabled") as? Bool ?? true
        lightMode = defaults.bool(forKey: "qrex.light")
        language = defaults.string(forKey: "qrex.language") ?? "en"
        let legacy = defaults.data(forKey: "qrex.history")
        // Device-only Keychain items can survive reinstall on some OS versions.
        // Reset orphaned items on a truly fresh installation, but keep legacy data.
        if defaults.object(forKey: "qrex.installation.vault") == nil {
            if legacy == nil { QREXHistoryVault.delete() }
            defaults.set(true, forKey: "qrex.installation.vault")
        }
        var stored = QREXHistoryVault.read()
        if let legacy, stored == nil {
            // Never delete a legacy record before proving it is readable.
            // A corrupt payload needs to remain available for recovery.
            if (try? JSONDecoder().decode([QRHistoryItem].self, from: legacy)) != nil,
               QREXHistoryVault.write(legacy) {
                defaults.removeObject(forKey: "qrex.history")
                stored = legacy
            } else {
                // A failed Keychain write is NOT a completed migration.
                // Preserve the original legacy record for a later retry;
                // silently filtering Wi-Fi values here would lose user data.
            }
        } else if stored != nil && legacy != nil {
            defaults.removeObject(forKey: "qrex.history")
        }
        if let data = stored,
           let loaded = try? JSONDecoder().decode([QRHistoryItem].self, from: data) {
            var unique: Set<String> = []
            items = Array(loaded.filter { unique.insert($0.raw).inserted && $0.raw.count <= 10000 }.prefix(250))
        }
        // A failed legacy Keychain migration must not be bypassed by writing
        // a second Flutter payload and later deleting the first record.
        if legacy == nil || stored != nil {
            migrateFlutterV1(defaults)
        }
    }

    private func migrateFlutterV1(_ defaults: UserDefaults) {
        guard let oldText = defaults.string(forKey: "flutter.qrex_history_v1"),
              let data = oldText.data(using: .utf8),
              let rows = (try? JSONSerialization.jsonObject(with: data)) as? [[String: Any]]
        else { return }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let basicFormatter = ISO8601DateFormatter()
        basicFormatter.formatOptions = [.withInternetDateTime]
        // Preserve the legacy input if any record cannot be parsed or retained.
        // Deleting it after migrating only a prefix would lose user data.
        guard rows.count <= 250 else { return }
        var known = Set(items.map(\.raw))
        var merged = items
        for row in rows {
            guard let raw = row["raw"] as? String,
                  !raw.isEmpty, raw.count <= 10000,
                  let dateText = row["date"] as? String,
                  let date = formatter.date(from: dateText)
                    ?? formatter.date(from: dateText + "Z")
                    ?? basicFormatter.date(from: dateText)
                    ?? basicFormatter.date(from: dateText + "Z") else { return }
            if !known.insert(raw).inserted { continue }
            guard merged.count < 250 else { return }
            merged.append(QRHistoryItem(id: UUID(), raw: raw,
                                        timestamp: date,
                                        saved: row["saved"] as? Bool ?? false))
        }
        // Do not remove the Flutter value until a protected Keychain write succeeds.
        guard persist(merged) else { return }
        items = merged
        if defaults.object(forKey: "qrex.light") == nil {
            lightMode = defaults.bool(forKey: "flutter.qrex_light_v1")
        }
        if defaults.object(forKey: "qrex.history.enabled") == nil {
            historyEnabled = defaults.object(forKey: "flutter.qrex_record_v1") as? Bool ?? true
        }
        defaults.removeObject(forKey: "flutter.qrex_history_v1")
    }

    func record(_ raw: String) {
        guard historyEnabled, !QRContent.isWifi(raw), !raw.isEmpty else { return }
        update(raw, saved: items.first(where: { $0.raw == raw })?.saved ?? false)
    }

    func save(_ raw: String) {
        guard !raw.isEmpty else { return }
        update(raw, saved: true)
    }

    private func update(_ raw: String, saved: Bool) {
        var next = items.filter { $0.raw != raw }
        next.insert(QRHistoryItem(id: UUID(), raw: raw, timestamp: Date(), saved: saved), at: 0)
        if next.count > 250 {
            if let lastUnsaved = next.lastIndex(where: { !$0.saved }) {
                next.remove(at: lastUnsaved)
            } else {
                next.removeLast()
            }
        }
        if persist(next) { items = next }
    }

    func delete(_ item: QRHistoryItem) {
        let next = items.filter { $0.id != item.id }
        if persist(next) { items = next }
    }

    func clearHistory() {
        let next = items.filter { $0.saved }
        if persist(next) { items = next }
    }

    func clearAll() {
        items.removeAll()
        QREXHistoryVault.delete()
        UserDefaults.standard.removeObject(forKey: "qrex.history")
        historyEnabled = true
        lightMode = false
        language = "en"
    }

    private func persist(_ records: [QRHistoryItem]) -> Bool {
        guard let data = try? JSONEncoder().encode(records) else { return false }
        return QREXHistoryVault.write(data)
    }
}
