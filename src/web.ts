import { WebPlugin } from "@capacitor/core";

import type {
  NativeBiometricPlugin,
  AvailableResult,
  BiometricOptions,
  GetCredentialOptions,
  SetCredentialOptions,
  DeleteCredentialOptions,
  Credentials,
} from "./definitions";

export class NativeBiometricWeb
  extends WebPlugin
  implements NativeBiometricPlugin
{
  constructor() {
    super();
  }
  isAvailable(): Promise<AvailableResult> {
    throw new Error("Method not implemented.");
  }

  verifyIdentity(_options?: BiometricOptions): Promise<void> {
    throw new Error("Method not implemented.");
  }
  verifyIdentityAndGetCredentials(_options: BiometricOptions & GetCredentialOptions): Promise<Credentials> {
    throw new Error("Method not implemented.");
  }
  setCredentials(_options: SetCredentialOptions): Promise<void> {
    throw new Error("Method not implemented.");
  }
  deleteCredentials(_options: DeleteCredentialOptions): Promise<void> {
    throw new Error("Method not implemented.");
  }
}
