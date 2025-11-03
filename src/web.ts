import { WebPlugin } from '@capacitor/core';

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
} from './definitions';

export class NativeBiometricWeb extends WebPlugin implements NativeBiometricPlugin {
  constructor() {
    super();
  }
  isAvailable(): Promise<AvailableResult> {
    return Promise.resolve({
      isAvailable: false,
      authenticationStrength: 0, // NONE
    });
  }

  verifyIdentity(_options?: BiometricOptions): Promise<void> {
    console.log('verifyIdentity', _options);
    throw new Error('Method not implemented.');
  }
  getCredentials(_options: GetCredentialOptions): Promise<Credentials> {
    console.log('getCredentials', _options);
    throw new Error('Method not implemented.');
  }
  setCredentials(_options: SetCredentialOptions): Promise<void> {
    console.log('setCredentials', _options);
    throw new Error('Method not implemented.');
  }
  deleteCredentials(_options: DeleteCredentialOptions): Promise<void> {
    console.log('deleteCredentials', _options);
    throw new Error('Method not implemented.');
  }
  isCredentialsSaved(_options: IsCredentialsSavedOptions): Promise<IsCredentialsSavedResult> {
    console.log('isCredentialsSaved', _options);
    throw new Error('Method not implemented.');
  }

  async getPluginVersion(): Promise<{ version: string }> {
    return { version: 'web' };
  }
}
