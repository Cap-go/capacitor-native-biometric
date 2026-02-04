import { WebPlugin } from '@capacitor/core';
import type { PluginListenerHandle } from '@capacitor/core';

import type {
  NativeBiometricPlugin,
  AvailableResult,
  BiometricOptions,
  IsAvailableOptions,
  GetCredentialOptions,
  SetCredentialOptions,
  DeleteCredentialOptions,
  IsCredentialsSavedOptions,
  IsCredentialsSavedResult,
  Credentials,
  BiometryChangeListener,
  DeviceSecurityConfig,
  VaultKeyResult,
  IsLockedResult,
  HasSecureHardwareResult,
  IsLockedOutResult,
  IsSystemPasscodeSetResult,
} from './definitions';
import { BiometryType, AuthenticationStrength } from './definitions';

export class NativeBiometricWeb extends WebPlugin implements NativeBiometricPlugin {
  /**
   * In-memory credential storage for browser development/testing.
   * Credentials are stored temporarily and cleared on page refresh.
   * This is NOT secure storage and should only be used for development purposes.
   */
  private credentialStore: Map<string, Credentials> = new Map();
  private locked = true;
  private vaultKey: string | null = null;
  private vaultKeyId: string | null = null;
  private failedUnlockAttempts = 0;
  private config: DeviceSecurityConfig = {
    lockAfterBackgrounded: -1,
    allowDeviceCredential: false,
    maxFailedAttempts: -1,
    invalidateOnBiometryChange: false,
  };

  constructor() {
    super();
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  isAvailable(_options?: IsAvailableOptions): Promise<AvailableResult> {
    // Web platform: return a dummy implementation for development/testing
    // Using TOUCH_ID as a generic placeholder for simulated biometric authentication
    return Promise.resolve({
      isAvailable: true,
      authenticationStrength: AuthenticationStrength.STRONG,
      biometryType: BiometryType.TOUCH_ID,
      deviceIsSecure: true,
      strongBiometryIsAvailable: true,
    });
  }

  async addListener(
    eventName: 'biometryChange',
    listener: BiometryChangeListener
  ): Promise<PluginListenerHandle>;
  async addListener(eventName: string, listener: (...args: unknown[]) => void): Promise<PluginListenerHandle> {
    return super.addListener(eventName, listener);
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  verifyIdentity(_options?: BiometricOptions): Promise<void> {
    console.log('verifyIdentity (dummy implementation)');
    // Dummy implementation: always succeeds for browser testing
    return Promise.resolve();
  }

  async unlock(_options?: BiometricOptions): Promise<void> {
    console.log('unlock (dummy implementation)');
    this.locked = false;
    this.failedUnlockAttempts = 0;
    if (!this.vaultKey) {
      const bytes = new Uint8Array(32);
      crypto.getRandomValues(bytes);
      this.vaultKey = btoa(String.fromCharCode(...bytes));
      this.vaultKeyId = crypto.randomUUID();
    }
    this.notifyListeners('unlock', {});
  }

  async lock(): Promise<void> {
    this.locked = true;
    this.vaultKey = null;
    this.vaultKeyId = null;
    this.notifyListeners('lock', {});
  }

  async isLocked(): Promise<IsLockedResult> {
    return { isLocked: this.locked };
  }

  async updateConfig(config: DeviceSecurityConfig): Promise<void> {
    this.config = { ...this.config, ...config };
    this.notifyListeners('configChanged', this.config);
  }

  async getVaultKey(): Promise<VaultKeyResult> {
    if (this.locked || !this.vaultKey || !this.vaultKeyId) {
      throw new Error('LOCKED');
    }
    return { key: this.vaultKey, keyId: this.vaultKeyId };
  }

  async deleteVaultKey(): Promise<void> {
    this.vaultKey = null;
    this.vaultKeyId = null;
  }

  async hasSecureHardware(): Promise<HasSecureHardwareResult> {
    return { hasSecureHardware: true };
  }

  async isLockedOutOfBiometrics(): Promise<IsLockedOutResult> {
    return { isLockedOut: false };
  }

  async isSystemPasscodeSet(): Promise<IsSystemPasscodeSetResult> {
    return { isSet: true };
  }

  getCredentials(_options: GetCredentialOptions): Promise<Credentials> {
    console.log('getCredentials (dummy implementation)', { server: _options.server });
    // Dummy implementation: retrieve from in-memory store
    const credentials = this.credentialStore.get(_options.server);
    if (!credentials) {
      throw new Error('No credentials found for the specified server');
    }
    return Promise.resolve(credentials);
  }

  setCredentials(_options: SetCredentialOptions): Promise<void> {
    console.log('setCredentials (dummy implementation)', { server: _options.server });
    // Dummy implementation: store in memory
    this.credentialStore.set(_options.server, {
      username: _options.username,
      password: _options.password,
    });
    return Promise.resolve();
  }

  deleteCredentials(_options: DeleteCredentialOptions): Promise<void> {
    console.log('deleteCredentials (dummy implementation)', { server: _options.server });
    // Dummy implementation: remove from in-memory store
    this.credentialStore.delete(_options.server);
    return Promise.resolve();
  }

  isCredentialsSaved(_options: IsCredentialsSavedOptions): Promise<IsCredentialsSavedResult> {
    console.log('isCredentialsSaved (dummy implementation)', { server: _options.server });
    // Dummy implementation: check in-memory store
    return Promise.resolve({ isSaved: this.credentialStore.has(_options.server) });
  }

  async getPluginVersion(): Promise<{ version: string }> {
    return { version: 'web' };
  }
}
