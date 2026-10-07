import Foundation
import LocalAuthentication

enum KeychainExistenceCheck {
    static func indicatesItemExists(status: OSStatus) -> Bool {
        return status == errSecSuccess || status == errSecInteractionNotAllowed
    }

    static func protectedItemExistenceQuery(
        service: String,
        account: String? = nil,
        returnAttributes: Bool = false
    ) -> [String: Any] {
        let context = LAContext()
        context.interactionNotAllowed = true

        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecMatchLimit as String: kSecMatchLimitOne,
            kSecUseAuthenticationContext as String: context
        ]

        if let account = account {
            query[kSecAttrAccount as String] = account
        }

        if returnAttributes {
            query[kSecReturnAttributes as String] = true
        }

        return query
    }

    static func protectedItemExists(
        service: String,
        account: String? = nil,
        returnAttributes: Bool = false
    ) -> Bool {
        let query = protectedItemExistenceQuery(
            service: service,
            account: account,
            returnAttributes: returnAttributes
        )

        var item: CFTypeRef?
        let status = SecItemCopyMatching(
            query as CFDictionary,
            returnAttributes ? &item : nil
        )
        return indicatesItemExists(status: status)
    }
}
