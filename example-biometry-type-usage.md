# BiometryType Feature - Usage Examples

This document demonstrates how to use the **BiometryType** feature that is available in the `isAvailable()` method response.

## Overview

The `isAvailable()` method returns an `AvailableResult` object that includes:

- `biometryType`: The type of biometric authentication hardware available
- `authenticationStrength`: Whether authentication is STRONG or WEAK
- `isAvailable`: Whether any form of authentication is available
- `deviceIsSecure`: Whether device has PIN/pattern/password set
- `strongBiometryIsAvailable`: Whether strong biometry specifically is available

## BiometryType Values

```typescript
enum BiometryType {
  NONE = 0, // No biometry available
  TOUCH_ID = 1, // iOS Touch ID
  FACE_ID = 2, // iOS Face ID (also Optic ID)
  FINGERPRINT = 3, // Android Fingerprint
  FACE_AUTHENTICATION = 4, // Android Face Authentication
  IRIS_AUTHENTICATION = 5, // Android Iris Authentication
  MULTIPLE = 6, // Android - Multiple biometry types available
}
```

## Example 1: Display Appropriate Icon

```typescript
import { NativeBiometric, BiometryType } from '@capgo/capacitor-native-biometric';

async function getBiometryIcon() {
  const result = await NativeBiometric.isAvailable();

  // Get the appropriate icon based on biometry type
  switch (result.biometryType) {
    case BiometryType.FACE_ID:
      return '👤 Face ID'; // or use face-id icon
    case BiometryType.TOUCH_ID:
      return '👆 Touch ID'; // or use fingerprint icon
    case BiometryType.FINGERPRINT:
      return '👆 Fingerprint'; // or use fingerprint icon
    case BiometryType.FACE_AUTHENTICATION:
      return '👤 Face Recognition'; // or use face icon
    case BiometryType.IRIS_AUTHENTICATION:
      return '👁️ Iris Scanner'; // or use iris icon
    case BiometryType.MULTIPLE:
      return '🔐 Biometric Authentication'; // generic icon
    default:
      return '🔒 Authentication'; // fallback icon
  }
}
```

## Example 2: Display Appropriate Text

```typescript
async function getBiometryPromptText() {
  const result = await NativeBiometric.isAvailable();

  if (!result.isAvailable) {
    return 'Authentication not available';
  }

  // Customize the authentication prompt based on biometry type
  switch (result.biometryType) {
    case BiometryType.FACE_ID:
      return 'Use Face ID to log in';
    case BiometryType.TOUCH_ID:
      return 'Use Touch ID to log in';
    case BiometryType.FINGERPRINT:
      return 'Use fingerprint to log in';
    case BiometryType.FACE_AUTHENTICATION:
      return 'Use face recognition to log in';
    case BiometryType.IRIS_AUTHENTICATION:
      return 'Use iris scanner to log in';
    case BiometryType.MULTIPLE:
      return 'Use biometric authentication to log in';
    default:
      // If biometryType is NONE but isAvailable is true,
      // user has fallback (PIN/pattern/password) enabled
      return 'Use device credentials to log in';
  }
}
```

## Example 3: Complete Authentication Flow

```typescript
import { NativeBiometric, BiometryType, AuthenticationStrength } from '@capgo/capacitor-native-biometric';

async function authenticateUser() {
  try {
    // Check what's available
    const availability = await NativeBiometric.isAvailable({
      useFallback: true, // Allow PIN/pattern/password fallback
    });

    console.log('Authentication Details:', {
      isAvailable: availability.isAvailable,
      biometryType: availability.biometryType,
      authenticationStrength: availability.authenticationStrength,
      deviceIsSecure: availability.deviceIsSecure,
      strongBiometryIsAvailable: availability.strongBiometryIsAvailable,
    });

    // IMPORTANT: Always check isAvailable for logic decisions
    if (!availability.isAvailable) {
      console.error('Authentication not available', availability.errorCode);
      return false;
    }

    // Use biometryType for display purposes only
    let promptMessage = 'Authenticate to continue';
    if (availability.biometryType === BiometryType.FACE_ID) {
      promptMessage = 'Look at your device to authenticate';
    } else if (
      availability.biometryType === BiometryType.TOUCH_ID ||
      availability.biometryType === BiometryType.FINGERPRINT
    ) {
      promptMessage = 'Place your finger on the sensor';
    }

    // Perform authentication
    await NativeBiometric.verifyIdentity({
      reason: promptMessage,
      title: 'Authentication Required',
      subtitle: 'Please authenticate to access your account',
    });

    console.log('✅ Authentication successful!');
    return true;
  } catch (error) {
    console.error('❌ Authentication failed:', error);
    return false;
  }
}
```

## Example 4: Conditional UI Rendering (React)

```typescript
import React, { useEffect, useState } from 'react';
import { NativeBiometric, BiometryType } from "@capgo/capacitor-native-biometric";

function BiometricLoginButton() {
  const [biometryInfo, setBiometryInfo] = useState<{
    available: boolean;
    type: BiometryType;
    icon: string;
    label: string;
  }>({
    available: false,
    type: BiometryType.NONE,
    icon: '🔒',
    label: 'Login'
  });

  useEffect(() => {
    checkBiometry();
  }, []);

  async function checkBiometry() {
    const result = await NativeBiometric.isAvailable({ useFallback: true });

    let icon = '🔒';
    let label = 'Login';

    if (result.isAvailable) {
      switch (result.biometryType) {
        case BiometryType.FACE_ID:
          icon = '👤';
          label = 'Login with Face ID';
          break;
        case BiometryType.TOUCH_ID:
        case BiometryType.FINGERPRINT:
          icon = '👆';
          label = 'Login with Fingerprint';
          break;
        case BiometryType.IRIS_AUTHENTICATION:
          icon = '👁️';
          label = 'Login with Iris';
          break;
        case BiometryType.FACE_AUTHENTICATION:
          icon = '👤';
          label = 'Login with Face';
          break;
        case BiometryType.MULTIPLE:
          icon = '🔐';
          label = 'Login with Biometrics';
          break;
        default:
          // Fallback to device credentials
          icon = '🔐';
          label = 'Login with PIN';
      }
    }

    setBiometryInfo({
      available: result.isAvailable,
      type: result.biometryType,
      icon,
      label
    });
  }

  async function handleLogin() {
    if (!biometryInfo.available) {
      alert('Biometric authentication not available');
      return;
    }

    try {
      await NativeBiometric.verifyIdentity({
        reason: 'Authenticate to access your account',
        title: 'Login Required'
      });
      // Handle successful authentication
      console.log('User authenticated successfully');
    } catch (error) {
      console.error('Authentication failed:', error);
    }
  }

  return (
    <button onClick={handleLogin} disabled={!biometryInfo.available}>
      <span>{biometryInfo.icon}</span>
      <span>{biometryInfo.label}</span>
    </button>
  );
}
```

## Important Notes

### ⚠️ Use biometryType for Display Only

**Always use `isAvailable` for logic decisions**, not `biometryType`:

```typescript
// ✅ CORRECT
const result = await NativeBiometric.isAvailable();
if (result.isAvailable) {
  // Proceed with authentication
  await NativeBiometric.verifyIdentity({ ... });
}

// ❌ WRONG - Don't use biometryType for logic
const result = await NativeBiometric.isAvailable();
if (result.biometryType !== BiometryType.NONE) {
  // This is unreliable - hardware presence ≠ availability
  await NativeBiometric.verifyIdentity({ ... });
}
```

### Android Considerations

On Android:

- `biometryType` indicates what hardware is **present**, not what is **enrolled**
- A device may report `FACE_AUTHENTICATION` hardware but the user hasn't enrolled their face
- Some devices report multiple biometry types and return `MULTIPLE`
- Always check `isAvailable` to determine if authentication will actually work

### iOS Considerations

On iOS:

- `biometryType` reliably indicates Touch ID, Face ID, or Optic ID
- Optic ID (Vision Pro) is reported as `FACE_ID` for compatibility
- `isAvailable` will be `false` if biometrics aren't enrolled

## Listening for Changes

You can also listen for biometry availability changes when the app resumes:

```typescript
// Add listener for biometry changes
const handle = await NativeBiometric.addListener('biometryChange', (result) => {
  console.log('Biometry changed:', {
    isAvailable: result.isAvailable,
    biometryType: result.biometryType,
    strongBiometryIsAvailable: result.strongBiometryIsAvailable,
  });

  // Update your UI based on the new biometry status
  updateBiometryUI(result);
});

// Later, remove the listener
await handle.remove();
```

## Summary

The BiometryType feature allows you to:

1. ✅ Display appropriate icons (fingerprint, face, iris)
2. ✅ Show user-friendly text ("Use Face ID" vs "Use Fingerprint")
3. ✅ Customize authentication prompts based on available biometry
4. ✅ Provide better UX by matching the device's capabilities

Remember: **Use for display purposes only. Always check `isAvailable` for logic decisions.**
