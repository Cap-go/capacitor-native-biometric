import { BiometryType, AuthenticationStrength } from './dist/esm/index.js';

console.log('✅ Successfully imported BiometryType');
console.log('BiometryType values:', {
  NONE: BiometryType.NONE,
  TOUCH_ID: BiometryType.TOUCH_ID,
  FACE_ID: BiometryType.FACE_ID,
  FINGERPRINT: BiometryType.FINGERPRINT,
  FACE_AUTHENTICATION: BiometryType.FACE_AUTHENTICATION,
  IRIS_AUTHENTICATION: BiometryType.IRIS_AUTHENTICATION,
  MULTIPLE: BiometryType.MULTIPLE
});

console.log('\n✅ Successfully imported AuthenticationStrength');
console.log('AuthenticationStrength values:', {
  NONE: AuthenticationStrength.NONE,
  STRONG: AuthenticationStrength.STRONG,
  WEAK: AuthenticationStrength.WEAK
});

console.log('\n🎉 All exports verified successfully!');
