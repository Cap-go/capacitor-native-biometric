import Foundation
import Capacitor
import LocalAuthentication

// swiftlint:disable type_body_length cyclomatic_complexity

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitor.ionicframework.com/docs/plugins/ios
 */

@objc(NativeBiometricPlugin)
public class NativeBiometricPlugin: CAPPlugin, CAPBridgedPlugin {
    private let pluginVersion: String = "8.3.4"
    public let identifier = "NativeBiometricPlugin"
    public let jsName = "NativeBiometric"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "isAvailable", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "verifyIdentity", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "unlock", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "lock", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isLocked", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "updateConfig", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getVaultKey", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteVaultKey", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hasSecureHardware", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isLockedOutOfBiometrics", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isSystemPasscodeSet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getCredentials", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setCredentials", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteCredentials", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isCredentialsSaved", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    private var isLocked = true
    private var failedUnlockAttempts = 0
    private var lockAfterBackgrounded: Double = -1
    private var allowDeviceCredential = false
    private var maxFailedAttempts = -1
    private var invalidateOnBiometryChange = false
    private var backgroundedAt: Date?
    private var cachedVaultKey: Data?
    private var cachedVaultKeyId: String?
    private let vaultKeyService = "NativeBiometricVaultKey"
    private let vaultKeyAccount = "default"
    private let vaultKeyIdDefaultsKey = "NativeBiometricVaultKeyId"
    private let configDefaultsKey = "NativeBiometricConfig"

    override public func load() {
        loadConfig()
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleAppDidBecomeActive),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleAppDidEnterBackground),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    @objc private func handleAppDidBecomeActive() {
        handleBackgroundAutoLock()
        // Notify listeners when app becomes active (resumes from background)
        let result = checkBiometryAvailability(useFallback: false)
        notifyListeners("biometryChange", data: result)
    }

    @objc private func handleAppDidEnterBackground() {
        backgroundedAt = Date()
        if lockAfterBackgrounded == 0 {
            lockInternal(notify: true)
        }
    }

    private func checkBiometryAvailability(useFallback: Bool) -> JSObject {
        let context = LAContext()
        var error: NSError?
        var obj = JSObject()

        obj["isAvailable"] = false
        obj["authenticationStrength"] = 0 // NONE
        obj["biometryType"] = 0 // NONE
        obj["deviceIsSecure"] = false
        obj["strongBiometryIsAvailable"] = false

        // Check biometric-only policy first
        let biometricPolicy = LAPolicy.deviceOwnerAuthenticationWithBiometrics
        let hasBiometric = context.canEvaluatePolicy(biometricPolicy, error: &error)

        // Determine biometry type
        let biometryType: Int
        switch context.biometryType {
        case .touchID:
            biometryType = 1 // TOUCH_ID
        case .faceID:
            biometryType = 2 // FACE_ID
        case .opticID:
            biometryType = 2 // Treat opticID as FACE_ID for compatibility
        default:
            biometryType = 0 // NONE
        }
        obj["biometryType"] = biometryType

        // Check if device has passcode set (device is secure)
        let devicePolicy = LAPolicy.deviceOwnerAuthentication
        var deviceError: NSError?
        let deviceIsSecure = context.canEvaluatePolicy(devicePolicy, error: &deviceError)
        obj["deviceIsSecure"] = deviceIsSecure

        // Check device credentials policy if fallback is enabled
        var hasDeviceCredentials = false
        if useFallback {
            hasDeviceCredentials = deviceIsSecure
        }

        // Strong biometry is available if biometric authentication works
        // On iOS, both Face ID and Touch ID are considered STRONG
        obj["strongBiometryIsAvailable"] = hasBiometric

        if hasBiometric {
            obj["authenticationStrength"] = 1 // STRONG
            obj["isAvailable"] = true
        } else if hasDeviceCredentials {
            obj["authenticationStrength"] = 2 // WEAK
            obj["isAvailable"] = true
        } else {
            if let authError = error {
                let pluginErrorCode = convertToPluginErrorCode(authError.code)
                obj["errorCode"] = pluginErrorCode
            } else {
                obj["errorCode"] = 0
            }
        }

        return obj
    }
    struct Credentials {
        var username: String
        var password: String
    }

    enum KeychainError: Error {
        case noPassword
        case unexpectedPasswordData
        case duplicateItem
        case unhandledError(status: OSStatus)
    }

    typealias JSObject = [String: Any]

    @objc func isAvailable(_ call: CAPPluginCall) {
        let useFallback = call.getBool("useFallback", false)
        let result = checkBiometryAvailability(useFallback: useFallback)
        call.resolve(result)
    }

    @objc func verifyIdentity(_ call: CAPPluginCall) {
        let context = LAContext()
        var canEvaluateError: NSError?

        let useFallback = call.getBool("useFallback", false)
        context.localizedFallbackTitle = ""

        if useFallback {
            context.localizedFallbackTitle = nil
            if let fallbackTitle = call.getString("fallbackTitle") {
                context.localizedFallbackTitle = fallbackTitle
            }
        }

        let policy = useFallback ? LAPolicy.deviceOwnerAuthentication : LAPolicy.deviceOwnerAuthenticationWithBiometrics

        if context.canEvaluatePolicy(policy, error: &canEvaluateError) {

            let reason = call.getString("reason") ?? "For biometric authentication"

            context.evaluatePolicy(policy, localizedReason: reason) { (success, evaluateError) in

                if success {
                    call.resolve()
                } else {
                    guard let error = evaluateError else {
                        call.reject("Biometrics Error", "0")
                        return
                    }

                    var pluginErrorCode = self.convertToPluginErrorCode(error._code)
                    // use pluginErrorCode.description to convert Int to String
                    call.reject(error.localizedDescription, pluginErrorCode.description, error )
                }

            }

        } else {
            call.reject("Authentication not available")
        }
    }

    @objc func unlock(_ call: CAPPluginCall) {
        let context = LAContext()
        var canEvaluateError: NSError?

        let useFallback = call.getBool("useFallback", false) || allowDeviceCredential
        context.localizedFallbackTitle = ""

        if useFallback {
            context.localizedFallbackTitle = nil
            if let fallbackTitle = call.getString("fallbackTitle") {
                context.localizedFallbackTitle = fallbackTitle
            }
        }

        let policy = useFallback ? LAPolicy.deviceOwnerAuthentication : LAPolicy.deviceOwnerAuthenticationWithBiometrics

        if context.canEvaluatePolicy(policy, error: &canEvaluateError) {
            let reason = call.getString("reason") ?? "Authenticate"

            context.evaluatePolicy(policy, localizedReason: reason) { (success, evaluateError) in
                DispatchQueue.main.async {
                    if success {
                        self.isLocked = false
                        self.failedUnlockAttempts = 0
                        do {
                            try self.ensureVaultKeyCached()
                            self.notifyListeners("unlock", data: [:])
                            call.resolve()
                        } catch {
                            self.lockInternal(notify: false)
                            call.reject("Failed to unlock", nil, error)
                        }
                    } else {
                        let code = (evaluateError as NSError?)?._code
                        let message = evaluateError?.localizedDescription ?? "Authentication failed"
                        self.handleUnlockFailure(message: message, code: code)
                        call.reject(message, code?.description, evaluateError as NSError?)
                    }
                }
            }
        } else {
            call.reject("Authentication not available")
        }
    }

    @objc func lock(_ call: CAPPluginCall) {
        lockInternal(notify: true)
        call.resolve()
    }

    @objc func isLocked(_ call: CAPPluginCall) {
        call.resolve(["isLocked": isLocked])
    }

    @objc func updateConfig(_ call: CAPPluginCall) {
        if let value = call.getDouble("lockAfterBackgrounded") {
            lockAfterBackgrounded = value
        }
        if call.hasOption("allowDeviceCredential") {
            allowDeviceCredential = call.getBool("allowDeviceCredential", false)
        }
        if let value = call.getInt("maxFailedAttempts") {
            maxFailedAttempts = value
        }
        if call.hasOption("invalidateOnBiometryChange") {
            invalidateOnBiometryChange = call.getBool("invalidateOnBiometryChange", false)
        }
        persistConfig()
        notifyListeners("configChanged", data: serializeConfig())
        call.resolve()
    }

    @objc func getVaultKey(_ call: CAPPluginCall) {
        if isLocked {
            call.reject("LOCKED")
            return
        }
        do {
            try ensureVaultKeyCached()
            guard let key = cachedVaultKey, let keyId = cachedVaultKeyId else {
                call.reject("NO_VAULT_KEY")
                return
            }
            call.resolve([
                "key": key.base64EncodedString(),
                "keyId": keyId
            ])
        } catch {
            call.reject("Failed to load vault key", nil, error)
        }
    }

    @objc func deleteVaultKey(_ call: CAPPluginCall) {
        do {
            try deleteVaultKeyFromKeychain()
            UserDefaults.standard.removeObject(forKey: vaultKeyIdDefaultsKey)
            cachedVaultKey = nil
            cachedVaultKeyId = nil
            call.resolve()
        } catch {
            call.reject(error.localizedDescription)
        }
    }

    @objc func hasSecureHardware(_ call: CAPPluginCall) {
        let context = LAContext()
        var error: NSError?
        let hasBiometrics = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
        if hasBiometrics {
            call.resolve(["hasSecureHardware": true])
            return
        }
        if let authError = error {
            let code = authError.code
            let hasHardware = code == LAError.biometryNotEnrolled.rawValue || code == LAError.biometryLockout.rawValue
            call.resolve(["hasSecureHardware": hasHardware])
            return
        }
        call.resolve(["hasSecureHardware": false])
    }

    @objc func isLockedOutOfBiometrics(_ call: CAPPluginCall) {
        let context = LAContext()
        var error: NSError?
        _ = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
        let lockedOut = error?.code == LAError.biometryLockout.rawValue
        call.resolve(["isLockedOut": lockedOut])
    }

    @objc func isSystemPasscodeSet(_ call: CAPPluginCall) {
        let context = LAContext()
        var error: NSError?
        let canEvaluate = context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error)
        if canEvaluate {
            call.resolve(["isSet": true])
            return
        }
        if let authError = error, authError.code == LAError.passcodeNotSet.rawValue {
            call.resolve(["isSet": false])
            return
        }
        call.resolve(["isSet": true])
    }

    @objc func getCredentials(_ call: CAPPluginCall) {
        guard let server = call.getString("server") else {
            call.reject("No server name was provided")
            return
        }
        do {
            let credentials = try getCredentialsFromKeychain(server)
            var obj = JSObject()
            obj["username"] = credentials.username
            obj["password"] = credentials.password
            call.resolve(obj)
        } catch {
            call.reject(error.localizedDescription)
        }
    }

    @objc func setCredentials(_ call: CAPPluginCall) {

        guard let server = call.getString("server"), let username = call.getString("username"), let password = call.getString("password") else {
            call.reject("Missing properties")
            return
        }

        let credentials = Credentials(username: username, password: password)

        do {
            try storeCredentialsInKeychain(credentials, server)
            call.resolve()
        } catch KeychainError.duplicateItem {
            do {
                try updateCredentialsInKeychain(credentials, server)
                call.resolve()
            } catch {
                call.reject(error.localizedDescription)
            }
        } catch {
            call.reject(error.localizedDescription)
        }
    }

    @objc func deleteCredentials(_ call: CAPPluginCall) {
        guard let server = call.getString("server") else {
            call.reject("No server name was provided")
            return
        }

        do {
            try deleteCredentialsFromKeychain(server)
            call.resolve()
        } catch {
            call.reject(error.localizedDescription)
        }
    }

    @objc func isCredentialsSaved(_ call: CAPPluginCall) {
        guard let server = call.getString("server") else {
            call.reject("No server name was provided")
            return
        }

        var obj = JSObject()
        obj["isSaved"] = checkCredentialsExist(server)
        call.resolve(obj)
    }

    // Check if credentials exist in Keychain
    func checkCredentialsExist(_ server: String) -> Bool {
        let query: [String: Any] = [kSecClass as String: kSecClassInternetPassword,
                                    kSecAttrServer as String: server,
                                    kSecMatchLimit as String: kSecMatchLimitOne]

        let status = SecItemCopyMatching(query as CFDictionary, nil)
        return status == errSecSuccess
    }

    // Store user Credentials in Keychain
    func storeCredentialsInKeychain(_ credentials: Credentials, _ server: String) throws {
        guard let passwordData = credentials.password.data(using: .utf8) else {
            throw KeychainError.unexpectedPasswordData
        }

        let query: [String: Any] = [kSecClass as String: kSecClassInternetPassword,
                                    kSecAttrAccount as String: credentials.username,
                                    kSecAttrServer as String: server,
                                    kSecValueData as String: passwordData]

        let status = SecItemAdd(query as CFDictionary, nil)

        guard status != errSecDuplicateItem else { throw KeychainError.duplicateItem }
        guard status == errSecSuccess else { throw KeychainError.unhandledError(status: status) }
    }

    // Update user Credentials in Keychain
    func updateCredentialsInKeychain(_ credentials: Credentials, _ server: String) throws {
        let query: [String: Any] = [kSecClass as String: kSecClassInternetPassword,
                                    kSecAttrServer as String: server]

        let account = credentials.username
        guard let password = credentials.password.data(using: String.Encoding.utf8) else {
            throw KeychainError.unexpectedPasswordData
        }
        let attributes: [String: Any] = [kSecAttrAccount as String: account,
                                         kSecValueData as String: password]

        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        guard status != errSecItemNotFound else { throw KeychainError.noPassword }
        guard status == errSecSuccess else { throw KeychainError.unhandledError(status: status) }
    }

    // Get user Credentials from Keychain
    func getCredentialsFromKeychain(_ server: String) throws -> Credentials {
        let query: [String: Any] = [kSecClass as String: kSecClassInternetPassword,
                                    kSecAttrServer as String: server,
                                    kSecMatchLimit as String: kSecMatchLimitOne,
                                    kSecReturnAttributes as String: true,
                                    kSecReturnData as String: true]

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status != errSecItemNotFound else { throw KeychainError.noPassword }
        guard status == errSecSuccess else { throw KeychainError.unhandledError(status: status) }

        guard let existingItem = item as? [String: Any],
              let passwordData = existingItem[kSecValueData as String] as? Data,
              let password = String(data: passwordData, encoding: .utf8),
              let username = existingItem[kSecAttrAccount as String] as? String
        else {
            throw KeychainError.unexpectedPasswordData
        }

        let credentials = Credentials(username: username, password: password)
        return credentials
    }

    // Delete user Credentials from Keychain
    func deleteCredentialsFromKeychain(_ server: String)throws {
        let query: [String: Any] = [kSecClass as String: kSecClassInternetPassword,
                                    kSecAttrServer as String: server]

        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else { throw KeychainError.unhandledError(status: status) }
    }

    /**
     * Convert Auth Error Codes to plugin expected Biometric Auth Errors (in README.md)
     * This way both iOS and Android return the same error codes for the soame authentication failure reasons.
     * !!IMPORTANT!!: Whenever this if modified, check if similar function in Android AuthActitivy.java needs to be modified as well.
     * @see https://developer.apple.com/documentation/localauthentication/laerror/code
     */
    func convertToPluginErrorCode(_ errorCode: Int) -> Int {
        switch errorCode {
        case LAError.biometryNotAvailable.rawValue:
            return 1

        case LAError.biometryLockout.rawValue:
            return 2

        case LAError.biometryNotEnrolled.rawValue:
            return 3

        case LAError.authenticationFailed.rawValue:
            return 10

        case LAError.appCancel.rawValue:
            return 11

        case LAError.invalidContext.rawValue:
            return 12

        case LAError.notInteractive.rawValue:
            return 13

        case LAError.passcodeNotSet.rawValue:
            return 14

        case LAError.systemCancel.rawValue:
            return 15

        case LAError.userCancel.rawValue:
            return 16

        case LAError.userFallback.rawValue:
            return 17

        default:
            return 0
        }
    }

    private func loadConfig() {
        if let config = UserDefaults.standard.dictionary(forKey: configDefaultsKey) {
            lockAfterBackgrounded = config["lockAfterBackgrounded"] as? Double ?? -1
            allowDeviceCredential = config["allowDeviceCredential"] as? Bool ?? false
            maxFailedAttempts = config["maxFailedAttempts"] as? Int ?? -1
            invalidateOnBiometryChange = config["invalidateOnBiometryChange"] as? Bool ?? false
        }
    }

    private func persistConfig() {
        UserDefaults.standard.set(serializeConfig(), forKey: configDefaultsKey)
    }

    private func serializeConfig() -> JSObject {
        return [
            "lockAfterBackgrounded": lockAfterBackgrounded,
            "allowDeviceCredential": allowDeviceCredential,
            "maxFailedAttempts": maxFailedAttempts,
            "invalidateOnBiometryChange": invalidateOnBiometryChange
        ]
    }

    private func handleBackgroundAutoLock() {
        guard let backgroundedAt = backgroundedAt, lockAfterBackgrounded >= 0 else {
            self.backgroundedAt = nil
            return
        }
        let elapsed = Date().timeIntervalSince(backgroundedAt) * 1000
        if elapsed >= lockAfterBackgrounded {
            lockInternal(notify: true)
        }
        self.backgroundedAt = nil
    }

    private func lockInternal(notify: Bool) {
        isLocked = true
        cachedVaultKey = nil
        cachedVaultKeyId = nil
        if notify {
            notifyListeners("lock", data: [:])
        }
    }

    private func handleUnlockFailure(message: String, code: Int?) {
        failedUnlockAttempts += 1
        var errorObj: JSObject = ["message": message]
        if let code = code {
            errorObj["code"] = String(code)
        }
        notifyListeners("error", data: errorObj)
        if maxFailedAttempts > 0 && failedUnlockAttempts >= maxFailedAttempts {
            lockInternal(notify: true)
            failedUnlockAttempts = 0
            notifyListeners("wipe", data: ["reason": "maxFailedAttempts"])
        }
    }

    private func ensureVaultKeyCached() throws {
        if cachedVaultKey != nil && cachedVaultKeyId != nil {
            return
        }
        let keyData: Data
        do {
            keyData = try getVaultKeyFromKeychain()
        } catch KeychainError.noPassword {
            var data = Data(count: 32)
            _ = data.withUnsafeMutableBytes { bytes in
                SecRandomCopyBytes(kSecRandomDefault, 32, bytes.baseAddress!)
            }
            try upsertVaultKeyInKeychain(data)
            keyData = data
        }

        cachedVaultKey = keyData
        if let existingId = UserDefaults.standard.string(forKey: vaultKeyIdDefaultsKey) {
            cachedVaultKeyId = existingId
        } else {
            let newId = UUID().uuidString
            UserDefaults.standard.set(newId, forKey: vaultKeyIdDefaultsKey)
            cachedVaultKeyId = newId
        }
    }

    private func getVaultKeyFromKeychain() throws -> Data {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: vaultKeyService,
            kSecAttrAccount as String: vaultKeyAccount,
            kSecMatchLimit as String: kSecMatchLimitOne,
            kSecReturnData as String: true
        ]

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status != errSecItemNotFound else { throw KeychainError.noPassword }
        guard status == errSecSuccess else { throw KeychainError.unhandledError(status: status) }
        guard let data = item as? Data else { throw KeychainError.unexpectedPasswordData }
        return data
    }

    private func upsertVaultKeyInKeychain(_ data: Data) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: vaultKeyService,
            kSecAttrAccount as String: vaultKeyAccount
        ]

        let attributes: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]

        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            var addQuery = query
            addQuery[kSecValueData as String] = data
            addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            let addStatus = SecItemAdd(addQuery as CFDictionary, nil)
            guard addStatus == errSecSuccess else { throw KeychainError.unhandledError(status: addStatus) }
            return
        }
        guard status == errSecSuccess else { throw KeychainError.unhandledError(status: status) }
    }

    private func deleteVaultKeyFromKeychain() throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: vaultKeyService,
            kSecAttrAccount as String: vaultKeyAccount
        ]
        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.unhandledError(status: status)
        }
    }

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve(["version": self.pluginVersion])
    }
}

// swiftlint:enable type_body_length cyclomatic_complexity
