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
    // A corrupt or unavailable Keychain vault is read-only until erased or
    // restored; silently replacing it with an empty list loses customer data.
    private var historyWritable = true
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
        // Keychain entries can survive an uninstall; prevent an orphaned vault
        // from reappearing when there is no earlier app data to migrate.
        if defaults.object(forKey: "qrex.installation.vault") == nil {
            if legacy == nil && !QREXHistoryVault.delete() {
                historyWritable = false
                return
            }
            defaults.set(true, forKey: "qrex.installation.vault")
        }

        var existing: [QRHistoryItem] = []
        switch QREXHistoryVault.load() {
        case .missing:
            break
        case .unavailable:
            historyWritable = false
            return
        case .available(let data):
            guard let loaded = try? JSONDecoder().decode([QRHistoryItem].self, from: data),
                  Self.validHistory(loaded) else {
                // Never replace corrupt, unreadable or oversized Keychain data.
                historyWritable = false
                return
            }
            existing = loaded
            items = loaded
        }

        if let legacy {
            // Do not delete or rewrite the older preference until *all*
            // records have been parsed, merged, and protected successfully.
            guard let previous = try? JSONDecoder().decode([QRHistoryItem].self, from: legacy),
                  Self.validHistory(previous) else {
                historyWritable = false
                return
            }
            var merged = existing
            for item in previous {
                if let index = merged.firstIndex(where: { $0.raw == item.raw }) {
                    if item.saved && !merged[index].saved {
                        let original = merged[index]
                        merged[index] = QRHistoryItem(id: original.id, raw: original.raw,
                            timestamp: original.timestamp, saved: true)
                    }
                } else {
                    guard merged.count < 250 else {
                        historyWritable = false
                        return
                    }
                    merged.append(item)
                }
            }
            guard let data = try? JSONEncoder().encode(merged),
                  QREXHistoryVault.write(data) else {
                historyWritable = false
                return
            }
            defaults.removeObject(forKey: "qrex.history")
            items = merged
        }

        // Legacy Flutter data remains in place if parsing or a protected write
        // fails. No other format is silently discarded during migration.
        migrateFlutterV1(defaults)
    }

    private static func validHistory(_ history: [QRHistoryItem]) -> Bool {
        guard history.count <= 250 else { return false }
        var seen = Set<String>()
        for record in history {
            guard !record.raw.isEmpty, record.raw.count <= 10000,
                  seen.insert(record.raw).inserted else { return false }
        }
        return true
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
        var merged = items
        for row in rows {
            guard let raw = row["raw"] as? String,
                  !raw.isEmpty, raw.count <= 10000,
                  let dateText = row["date"] as? String,
                  let date = formatter.date(from: dateText)
                    ?? formatter.date(from: dateText + "Z")
                    ?? basicFormatter.date(from: dateText)
                    ?? basicFormatter.date(from: dateText + "Z") else { return }
            let saved = row["saved"] as? Bool ?? false
            if let index = merged.firstIndex(where: { $0.raw == raw }) {
                // Keep the identity and timestamp of an existing record,
                // but never drop a legacy favorite when its QR already exists.
                if saved && !merged[index].saved {
                    let original = merged[index]
                    merged[index] = QRHistoryItem(id: original.id, raw: original.raw,
                                                  timestamp: original.timestamp, saved: true)
                }
            } else {
                guard merged.count < 250 else { return }
                merged.append(QRHistoryItem(id: UUID(), raw: raw,
                                            timestamp: date, saved: saved))
            }
        }
        // Do not remove the Flutter value until a protected Keychain write succeeds.
        guard persist(merged) else {
            historyWritable = false
            return
        }
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
        guard historyEnabled, !QRContent.isWifi(raw), !raw.isEmpty,
              raw.count <= 10000 else { return }
        update(raw, saved: items.first(where: { $0.raw == raw })?.saved ?? false)
    }

    @discardableResult
    func save(_ raw: String) -> Bool {
        guard !raw.isEmpty, raw.count <= 10000 else { return false }
        return update(raw, saved: true)
    }

    @discardableResult
    private func update(_ raw: String, saved: Bool) -> Bool {
        var next = items.filter { $0.raw != raw }
        next.insert(QRHistoryItem(id: UUID(), raw: raw, timestamp: Date(), saved: saved), at: 0)
        if next.count > 250 {
            // Never discard a manually saved QR code just to add a new entry.
            // The first item is the new record; evict older unsaved scans only.
            guard let lastUnsaved = next.lastIndex(where: { !$0.saved }),
                  lastUnsaved > 0 else { return false }
            next.remove(at: lastUnsaved)
        }
        guard persist(next) else { return false }
        items = next
        return true
    }

    @discardableResult
    func delete(_ item: QRHistoryItem) -> Bool {
        let next = items.filter { $0.id != item.id }
        guard persist(next) else { return false }
        items = next
        return true
    }

    @discardableResult
    func clearHistory() -> Bool {
        let next = items.filter { $0.saved }
        guard persist(next) else { return false }
        items = next
        return true
    }

    /** Remove both protected history and every known pre-migration QR record. */
    @discardableResult
    func clearAll() -> Bool {
        // A Keychain failure cannot be reported as a successful erase.
        guard QREXHistoryVault.delete() else { return false }
        let defaults = UserDefaults.standard
        defaults.removeObject(forKey: "qrex.history")
        defaults.removeObject(forKey: "flutter.qrex_history_v1")
        defaults.removeObject(forKey: "flutter.qrex_light_v1")
        defaults.removeObject(forKey: "flutter.qrex_record_v1")
        items.removeAll()
        historyWritable = true
        historyEnabled = true
        lightMode = false
        language = "en"
        return true
    }

    private func persist(_ records: [QRHistoryItem]) -> Bool {
        guard historyWritable, Self.validHistory(records),
              let data = try? JSONEncoder().encode(records) else { return false }
        return QREXHistoryVault.write(data)
    }
}
