import type { PluginListenerHandle } from '@capacitor/core';

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
  // Android - Device credentials (PIN, pattern, or password)
  DEVICE_CREDENTIAL = 7,
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
   * Only for iOS.
   * Specifies if should fallback to passcode authentication if biometric authentication is not available.
   * On Android, this parameter is ignored due to BiometricPrompt API constraints:
   * DEVICE_CREDENTIAL authenticator and negative button (cancel) are mutually exclusive.
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
   * The primary biometry type available on the device.
   * On Android devices with multiple biometry types, this returns MULTIPLE.
   * Use this for display purposes only - always use isAvailable for logic decisions.
   */
  biometryType: BiometryType;
  /**
   * Whether the device has a secure lock screen (PIN, pattern, or password).
   * This is independent of biometric enrollment.
   */
  deviceIsSecure: boolean;
  /**
   * Whether strong biometry (Face ID, Touch ID, or fingerprint on devices that consider it strong)
   * is specifically available, separate from weak biometry or device credentials.
   */
  strongBiometryIsAvailable: boolean;
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
   * Only for iOS.
   * Specifies if should fallback to passcode authentication if biometric authentication fails.
   * On Android, this parameter is ignored due to BiometricPrompt API constraints:
   * DEVICE_CREDENTIAL authenticator and negative button (cancel) are mutually exclusive.
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
   * Only for Android.
   * Specify which biometry types are allowed for authentication.
   * If not specified, all available types will be allowed.
   * @example [BiometryType.FINGERPRINT, BiometryType.FACE_AUTHENTICATION]
   */
  allowedBiometryTypes?: BiometryType[];
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

export interface DeviceSecurityConfig {
  /**
   * Auto-lock after the app has been in background for this many milliseconds.
   * Use -1 to disable (default).
   */
  lockAfterBackgrounded?: number;
  /**
   * Allow device credential fallback (PIN/pattern/password) where supported.
   * Default: false.
   */
  allowDeviceCredential?: boolean;
  /**
   * Maximum number of failed unlock attempts before wiping secure memory.
   * Use -1 to disable (default).
   */
  maxFailedAttempts?: number;
  /**
   * Invalidate keys on biometric changes (optional, not enabled by default).
   */
  invalidateOnBiometryChange?: boolean;
}

export interface VaultKeyResult {
  /**
   * Base64-encoded key bytes.
   */
  key: string;
  /**
   * Stable identifier for the key.
   */
  keyId: string;
}

export interface IsLockedResult {
  isLocked: boolean;
}

export interface HasSecureHardwareResult {
  hasSecureHardware: boolean;
}

export interface IsLockedOutResult {
  isLockedOut: boolean;
}

export interface IsSystemPasscodeSetResult {
  isSet: boolean;
}

export interface WipeEvent {
  reason: string;
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

/**
 * Callback type for biometry change listener
 */
export type BiometryChangeListener = (result: AvailableResult) => void;
export type LockListener = () => void;
export type UnlockListener = () => void;
export type ErrorListener = (error: { message: string; code?: string }) => void;
export type ConfigChangeListener = (config: DeviceSecurityConfig) => void;
export type WipeListener = (event: WipeEvent) => void;

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
   * Adds a listener that is called when the app resumes from background.
   * This is useful to detect if biometry availability has changed while
   * the app was in the background (e.g., user enrolled/unenrolled biometrics).
   *
   * @param eventName - Must be 'biometryChange'
   * @param {BiometryChangeListener} listener - Callback function that receives the updated AvailableResult
   * @returns {Promise<PluginListenerHandle>} Handle to remove the listener
   * @since 7.6.0
   *
   * @example
   * ```typescript
   * const handle = await NativeBiometric.addListener('biometryChange', (result) => {
   *   console.log('Biometry availability changed:', result.isAvailable);
   * });
   *
   * // To remove the listener:
   * await handle.remove();
   * ```
   */
  addListener(eventName: 'biometryChange', listener: BiometryChangeListener): Promise<PluginListenerHandle>;
  addListener(eventName: 'lock', listener: LockListener): Promise<PluginListenerHandle>;
  addListener(eventName: 'unlock', listener: UnlockListener): Promise<PluginListenerHandle>;
  addListener(eventName: 'error', listener: ErrorListener): Promise<PluginListenerHandle>;
  addListener(eventName: 'configChanged', listener: ConfigChangeListener): Promise<PluginListenerHandle>;
  addListener(eventName: 'wipe', listener: WipeListener): Promise<PluginListenerHandle>;
  /**
   * Prompts the user to authenticate with biometrics.
   * @param {BiometricOptions} [options]
   * @returns {Promise<any>}
   * @memberof NativeBiometricPlugin
   * @since 1.0.0
   */
  verifyIdentity(options?: BiometricOptions): Promise<void>;
  /**
   * Unlocks the secure session using biometrics (and optionally device credentials).
   * @param {BiometricOptions} [options]
   * @returns {Promise<void>}
   * @since 9.0.0
   */
  unlock(options?: BiometricOptions): Promise<void>;
  /**
   * Locks the secure session and clears secure memory.
   * @returns {Promise<void>}
   * @since 9.0.0
   */
  lock(): Promise<void>;
  /**
   * Returns current lock state.
   * @returns {Promise<IsLockedResult>}
   * @since 9.0.0
   */
  isLocked(): Promise<IsLockedResult>;
  /**
   * Updates device security configuration at runtime.
   * @param {DeviceSecurityConfig} config
   * @returns {Promise<void>}
   * @since 9.0.0
   */
  updateConfig(config: DeviceSecurityConfig): Promise<void>;
  /**
   * Returns the base64-encoded vault key (only when unlocked).
   * @returns {Promise<VaultKeyResult>}
   * @since 9.0.0
   */
  getVaultKey(): Promise<VaultKeyResult>;
  /**
   * Deletes the stored vault key.
   * @returns {Promise<void>}
   * @since 9.0.0
   */
  deleteVaultKey(): Promise<void>;
  /**
   * Returns whether the device has secure biometric hardware.
   * @returns {Promise<HasSecureHardwareResult>}
   * @since 9.0.0
   */
  hasSecureHardware(): Promise<HasSecureHardwareResult>;
  /**
   * Returns whether biometrics are currently locked out.
   * @returns {Promise<IsLockedOutResult>}
   * @since 9.0.0
   */
  isLockedOutOfBiometrics(): Promise<IsLockedOutResult>;
  /**
   * Returns whether a system passcode/PIN is set.
   * @returns {Promise<IsSystemPasscodeSetResult>}
   * @since 9.0.0
   */
  isSystemPasscodeSet(): Promise<IsSystemPasscodeSetResult>;
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
