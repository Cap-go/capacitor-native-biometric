import LocalAuthentication
import XCTest
@testable import NativeBiometricPlugin

final class KeychainExistenceCheckTests: XCTestCase {

    func testIndicatesItemExistsTreatsInteractionNotAllowedAsPresent() {
        XCTAssertTrue(KeychainExistenceCheck.indicatesItemExists(status: errSecSuccess))
        XCTAssertTrue(KeychainExistenceCheck.indicatesItemExists(status: errSecInteractionNotAllowed))
        XCTAssertFalse(KeychainExistenceCheck.indicatesItemExists(status: errSecItemNotFound))
        XCTAssertFalse(KeychainExistenceCheck.indicatesItemExists(status: errSecAuthFailed))
    }

    func testProtectedItemExistenceQueryUsesNonInteractiveAuthenticationContext() {
        let query = KeychainExistenceCheck.protectedItemExistenceQuery(
            service: "example.com",
            account: "user",
            returnAttributes: true
        )

        XCTAssertEqual(query[kSecClass as String] as? String, kSecClassGenericPassword as String)
        XCTAssertEqual(query[kSecAttrService as String] as? String, "example.com")
        XCTAssertEqual(query[kSecAttrAccount as String] as? String, "user")
        XCTAssertEqual(query[kSecMatchLimit as String] as? String, kSecMatchLimitOne as String)
        XCTAssertEqual(query[kSecReturnAttributes as String] as? Bool, true)

        let context = query[kSecUseAuthenticationContext as String] as? LAContext
        XCTAssertNotNil(context)
        XCTAssertTrue(context?.interactionNotAllowed ?? false)
    }

    func testProtectedItemExistenceQueryOmitsAccountWhenNotProvided() {
        let query = KeychainExistenceCheck.protectedItemExistenceQuery(service: "example.com")
        XCTAssertNil(query[kSecAttrAccount as String])
        XCTAssertNil(query[kSecReturnAttributes as String])
    }
}
