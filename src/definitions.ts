export enum BiometryType {
  // Android, iOS
  NONE = 0,
  // iOS
  TOUCH_ID = 1,
  // iOS
  FACE_ID = 2,
  // Android
  FINGERPRINT = 3,
  // Android
  FACE_AUTHENTICATION = 4,
  // Android
  IRIS_AUTHENTICATION = 5,
  // Android
  MULTIPLE = 6,
}

export enum AuthenticationStrength {
  /**
   * No authentication available, even if PIN is available but useFallback = false
   */
  NONE = 0,
  /**
   * Strong authentication: Face ID on iOS, fingerprints on devices that consider fingerprints strong (Android).
   * Note: PIN/pattern/password is NEVER considered STRONG, even when useFallback = true.
   */
  STRONG = 1,
  /**
   * Weak authentication: Face authentication on Android devices that consider face weak,
   * or PIN/pattern/password if useFallback = true (PIN is always WEAK, never STRONG).
   */
  WEAK = 2,
}

export interface Credentials {
  username: string;
  password: string;
}

export interface IsAvailableOptions {
  /**
   * Specifies if should fallback to passcode authentication if biometric authentication is not available.
   */
  useFallback: boolean;
}

/**
 * Result from isAvailable() method indicating biometric authentication availability.
 */
export interface AvailableResult {
  /**
   * Whether authentication is available (biometric or fallback if useFallback is true)
   */
  isAvailable: boolean;
  /**
   * The strength of available authentication method (STRONG, WEAK, or NONE)
   */
  authenticationStrength: AuthenticationStrength;
  /**
   * Error code from BiometricAuthError enum. Only present when isAvailable is false.
   * Indicates why biometric authentication is not available.
   * @see BiometricAuthError
   */
  errorCode?: BiometricAuthError;
}

export interface BiometricOptions {
  reason?: string;
  title?: string;
  subtitle?: string;
  description?: string;
  negativeButtonText?: string;
  /**
   * Specifies if should fallback to passcode authentication if biometric authentication fails.
   */
  useFallback?: boolean;
  /**
   * Only for iOS.
   * Set the text for the fallback button in the authentication dialog.
   * If this property is not specified, the default text is set by the system.
   */
  fallbackTitle?: string;
  /**
   * Only for Android.
   * Set a maximum number of attempts for biometric authentication. The maximum allowed by android is 5.
   * @default 1
   */
  maxAttempts?: number;
  /**
   * Specify the authentication strength required.
   * - STRONG: Only strong biometrics (fingerprints, Face ID, etc.)
   * - WEAK: Weak biometrics (face on Android devices that classify it as weak) OR PIN/password
   * If not specified, defaults to allowing both strong and weak biometrics.
   */
  requiredStrength?: AuthenticationStrength;
}

export interface GetCredentialOptions {
  server: string;
}

export interface SetCredentialOptions {
  username: string;
  password: string;
  server: string;
}

export interface DeleteCredentialOptions {
  server: string;
}

export interface IsCredentialsSavedOptions {
  server: string;
}

export interface IsCredentialsSavedResult {
  isSaved: boolean;
}

/**
 * Biometric authentication error codes.
 * These error codes are used in both isAvailable() and verifyIdentity() methods.
 *
 * Keep this in sync with BiometricAuthError in README.md
 * Update whenever `convertToPluginErrorCode` functions are modified
 */
export enum BiometricAuthError {
  /**
   * Unknown error occurred
   */
  UNKNOWN_ERROR = 0,
  /**
   * Biometrics are unavailable (no hardware or hardware error)
   * Platform: Android, iOS
   */
  BIOMETRICS_UNAVAILABLE = 1,
  /**
   * User has been locked out due to too many failed attempts
   * Platform: Android, iOS
   */
  USER_LOCKOUT = 2,
  /**
   * No biometrics are enrolled on the device
   * Platform: Android, iOS
   */
  BIOMETRICS_NOT_ENROLLED = 3,
  /**
   * User is temporarily locked out (Android: 30 second lockout)
   * Platform: Android
   */
  USER_TEMPORARY_LOCKOUT = 4,
  /**
   * Authentication failed (user did not authenticate successfully)
   * Platform: Android, iOS
   */
  AUTHENTICATION_FAILED = 10,
  /**
   * App canceled the authentication (iOS only)
   * Platform: iOS
   */
  APP_CANCEL = 11,
  /**
   * Invalid context (iOS only)
   * Platform: iOS
   */
  INVALID_CONTEXT = 12,
  /**
   * Authentication was not interactive (iOS only)
   * Platform: iOS
   */
  NOT_INTERACTIVE = 13,
  /**
   * Passcode/PIN is not set on the device
   * Platform: Android, iOS
   */
  PASSCODE_NOT_SET = 14,
  /**
   * System canceled the authentication (e.g., due to screen lock)
   * Platform: Android, iOS
   */
  SYSTEM_CANCEL = 15,
  /**
   * User canceled the authentication
   * Platform: Android, iOS
   */
  USER_CANCEL = 16,
  /**
   * User chose to use fallback authentication method
   * Platform: Android, iOS
   */
  USER_FALLBACK = 17,
}

export interface NativeBiometricPlugin {
  /**
   * Checks if biometric authentication hardware is available.
   * @param {IsAvailableOptions} [options]
   * @returns {Promise<AvailableResult>}
   * @memberof NativeBiometricPlugin
   * @since 1.0.0
   */
  isAvailable(options?: IsAvailableOptions): Promise<AvailableResult>;
  /**
   * Prompts the user to authenticate with biometrics.
   * @param {BiometricOptions} [options]
   * @returns {Promise<any>}
   * @memberof NativeBiometricPlugin
   * @since 1.0.0
   */
  verifyIdentity(options?: BiometricOptions): Promise<void>;
  /**
   * Gets the stored credentials for a given server.
   * @param {GetCredentialOptions} options
   * @returns {Promise<Credentials>}
   * @memberof NativeBiometricPlugin
   * @since 1.0.0
   */
  getCredentials(options: GetCredentialOptions): Promise<Credentials>;
  /**
   * Stores the given credentials for a given server.
   * @param {SetCredentialOptions} options
   * @returns {Promise<any>}
   * @memberof NativeBiometricPlugin
   * @since 1.0.0
   */
  setCredentials(options: SetCredentialOptions): Promise<void>;
  /**
   * Deletes the stored credentials for a given server.
   * @param {DeleteCredentialOptions} options
   * @returns {Promise<any>}
   * @memberof NativeBiometricPlugin
   * @since 1.0.0
   */
  deleteCredentials(options: DeleteCredentialOptions): Promise<void>;
  /**
   * Checks if credentials are already saved for a given server.
   * @param {IsCredentialsSavedOptions} options
   * @returns {Promise<IsCredentialsSavedResult>}
   * @memberof NativeBiometricPlugin
   * @since 7.3.0
   */
  isCredentialsSaved(options: IsCredentialsSavedOptions): Promise<IsCredentialsSavedResult>;

  /**
   * Get the native Capacitor plugin version.
   *
   * @returns Promise that resolves with the plugin version
   * @since 1.0.0
   */
  getPluginVersion(): Promise<{ version: string }>;
}
