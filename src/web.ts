import { WebPlugin } from '@capacitor/core';
import type { PluginListenerHandle } from '@capacitor/core';

import type {
  NativeBiometricPlugin,
  AvailableResult,
  BiometricOptions,
  GetCredentialOptions,
  SetCredentialOptions,
  DeleteCredentialOptions,
  IsCredentialsSavedOptions,
  IsCredentialsSavedResult,
  Credentials,
  BiometryChangeListener,
} from './definitions';
import { BiometryType, AuthenticationStrength } from './definitions';

export class NativeBiometricWeb extends WebPlugin implements NativeBiometricPlugin {
  constructor() {
    super();
  }

  isAvailable(): Promise<AvailableResult> {
    // Web platform: biometrics not available, but return structured response
    return Promise.resolve({
      isAvailable: false,
      authenticationStrength: AuthenticationStrength.NONE,
      biometryType: BiometryType.NONE,
      deviceIsSecure: false,
      strongBiometryIsAvailable: false,
    });
  }

  async addListener(_eventName: 'biometryChange', _listener: BiometryChangeListener): Promise<PluginListenerHandle> {
    // Web platform: no-op, but return a valid handle
    return {
      remove: async () => {
        // Nothing to remove on web
      },
    };
  }

  verifyIdentity(_options?: BiometricOptions): Promise<void> {
    console.log('verifyIdentity', _options);
    throw new Error('Biometric authentication is not available on web platform.');
  }

  getCredentials(_options: GetCredentialOptions): Promise<Credentials> {
    console.log('getCredentials', _options);
    throw new Error('Credential storage is not available on web platform.');
  }

  setCredentials(_options: SetCredentialOptions): Promise<void> {
    console.log('setCredentials', _options);
    throw new Error('Credential storage is not available on web platform.');
  }

  deleteCredentials(_options: DeleteCredentialOptions): Promise<void> {
    console.log('deleteCredentials', _options);
    throw new Error('Credential storage is not available on web platform.');
  }

  isCredentialsSaved(_options: IsCredentialsSavedOptions): Promise<IsCredentialsSavedResult> {
    console.log('isCredentialsSaved', _options);
    // Return false on web - no credentials can be saved
    return Promise.resolve({ isSaved: false });
  }

  async getPluginVersion(): Promise<{ version: string }> {
    return { version: 'web' };
  }
}
