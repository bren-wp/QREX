import Foundation
import Security

/// Stores all QR history in the iOS Keychain, not in unencrypted UserDefaults.
enum QREXHistoryVault {
    private static let service = "com.brendigo.qrex"
    private static let account = "history.v1"
    private static var query: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }

    static func read() -> Data? {
        var result: CFTypeRef?
        var request = query
        request[kSecReturnData as String] = kCFBooleanTrue
        request[kSecMatchLimit as String] = kSecMatchLimitOne
        guard SecItemCopyMatching(request as CFDictionary, &result) == errSecSuccess else { return nil }
        return result as? Data
    }

    @discardableResult
    static func write(_ data: Data) -> Bool {
        let changes: [String: Any] = [kSecValueData as String: data]
        let status = SecItemUpdate(query as CFDictionary, changes as CFDictionary)
        if status == errSecSuccess { return true }
        guard status == errSecItemNotFound else { return false }
        var insert = query
        insert[kSecValueData as String] = data
        insert[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        let inserted = SecItemAdd(insert as CFDictionary, nil)
        if inserted != errSecSuccess {
            // Error code only: never log the content of encrypted QR records.
            NSLog("QREX Keychain write unavailable (status %d)", inserted)
        }
        return inserted == errSecSuccess
    }

    @discardableResult
    static func delete() -> Bool {
        let status = SecItemDelete(query as CFDictionary)
        // Deleting a record that is already absent is a successful reset.
        return status == errSecSuccess || status == errSecItemNotFound
    }
}
