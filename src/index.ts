import { registerPlugin } from '@capacitor/core';

import type { NativeBiometricPlugin } from './definitions';

const NativeBiometric = registerPlugin<NativeBiometricPlugin>('NativeBiometric', {
  web: () => import('./web').then((m) => new m.NativeBiometricWeb()),
});

// Web CI packed-example verify installs the published tarball instead of file:../.

export * from './definitions';
export { NativeBiometric };
