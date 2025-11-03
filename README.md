# Capacitor Native Biometric 
 <a href="https://capgo.app/"><img src='https://raw.githubusercontent.com/Cap-go/capgo/main/assets/capgo_banner.png' alt='Capgo - Instant updates for capacitor'/></a>
 
<div align="center">
  <h2><a href="https://capgo.app/?ref=plugin"> ➡️ Get Instant updates for your App with Capgo</a></h2>
  <h2><a href="https://capgo.app/consulting/?ref=plugin"> Missing a feature? We’ll build the plugin for you 💪</a></h2>
</div>


Use biometrics confirm device owner presence or authenticate users. A couple of methods are provided to handle user credentials. These are securely stored using Keychain (iOS) and Keystore (Android).

## Documentation

The most complete doc is available here: https://capgo.app/docs/plugins/native-biometric/

## Installation (Only supports Capacitor 7)

- `npm i @capgo/capacitor-native-biometric`

## Usage

```ts
import { NativeBiometric, AuthenticationStrength } from "@capgo/capacitor-native-biometric";

async performBiometricVerification(){
  const result = await NativeBiometric.isAvailable();

  if(!result.isAvailable) return;

  const isStrongAuth = result.authenticationStrength == AuthenticationStrength.STRONG;

  const verified = await NativeBiometric.verifyIdentity({
    reason: "For easy log in",
    title: "Log in",
    subtitle: "Maybe add subtitle here?",
    description: "Maybe a description too?",
  })
    .then(() => true)
    .catch(() => false);

  if(!verified) return;

  const credentials = await NativeBiometric.getCredentials({
    server: "www.example.com",
  });
}

// Save user's credentials
NativeBiometric.setCredentials({
  username: "username",
  password: "password",
  server: "www.example.com",
}).then();

// Delete user's credentials
NativeBiometric.deleteCredentials({
  server: "www.example.com",
}).then();
```

### Biometric Auth Errors

This is a plugin specific list of error codes that can be thrown on verifyIdentity failure, or set as a part of isAvailable. It consolidates Android and iOS specific Authentication Error codes into one combined error list.

| Code | Description                 | Platform                     |
| ---- | --------------------------- | ---------------------------- |
| 0    | Unknown Error               | Android, iOS                 |
| 1    | Biometrics Unavailable      | Android, iOS                 |
| 2    | User Lockout                | Android, iOS                 |
| 3    | Biometrics Not Enrolled     | Android, iOS                 |
| 4    | User Temporary Lockout      | Android (Lockout for 30sec)  |
| 10   | Authentication Failed       | Android, iOS                 |
| 11   | App Cancel                  | iOS                          |
| 12   | Invalid Context             | iOS                          |
| 13   | Not Interactive             | iOS                          |
| 14   | Passcode Not Set            | Android, iOS                 |
| 15   | System Cancel               | Android, iOS                 |
| 16   | User Cancel                 | Android, iOS                 |
| 17   | User Fallback               | Android, iOS                 |

<docgen-index>

* [`isAvailable(...)`](#isavailable)
* [`verifyIdentity(...)`](#verifyidentity)
* [`getCredentials(...)`](#getcredentials)
* [`setCredentials(...)`](#setcredentials)
* [`deleteCredentials(...)`](#deletecredentials)
* [`isCredentialsSaved(...)`](#iscredentialssaved)
* [`getPluginVersion()`](#getpluginversion)
* [Interfaces](#interfaces)
* [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### isAvailable(...)

```typescript
isAvailable(options?: IsAvailableOptions | undefined) => Promise<AvailableResult>
```

Checks if biometric authentication hardware is available.

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#isavailableoptions">IsAvailableOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#availableresult">AvailableResult</a>&gt;</code>

**Since:** 1.0.0

--------------------


### verifyIdentity(...)

```typescript
verifyIdentity(options?: BiometricOptions | undefined) => Promise<void>
```

Prompts the user to authenticate with biometrics.

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code><a href="#biometricoptions">BiometricOptions</a></code> |

**Since:** 1.0.0

--------------------


### getCredentials(...)

```typescript
getCredentials(options: GetCredentialOptions) => Promise<Credentials>
```

Gets the stored credentials for a given server.

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#getcredentialoptions">GetCredentialOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#credentials">Credentials</a>&gt;</code>

**Since:** 1.0.0

--------------------


### setCredentials(...)

```typescript
setCredentials(options: SetCredentialOptions) => Promise<void>
```

Stores the given credentials for a given server.

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#setcredentialoptions">SetCredentialOptions</a></code> |

**Since:** 1.0.0

--------------------


### deleteCredentials(...)

```typescript
deleteCredentials(options: DeleteCredentialOptions) => Promise<void>
```

Deletes the stored credentials for a given server.

| Param         | Type                                                                        |
| ------------- | --------------------------------------------------------------------------- |
| **`options`** | <code><a href="#deletecredentialoptions">DeleteCredentialOptions</a></code> |

**Since:** 1.0.0

--------------------


### isCredentialsSaved(...)

```typescript
isCredentialsSaved(options: IsCredentialsSavedOptions) => Promise<IsCredentialsSavedResult>
```

Checks if credentials are already saved for a given server.

| Param         | Type                                                                            |
| ------------- | ------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#iscredentialssavedoptions">IsCredentialsSavedOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#iscredentialssavedresult">IsCredentialsSavedResult</a>&gt;</code>

**Since:** 7.3.0

--------------------


### getPluginVersion()

```typescript
getPluginVersion() => Promise<{ version: string; }>
```

Get the native Capacitor plugin version.

**Returns:** <code>Promise&lt;{ version: string; }&gt;</code>

**Since:** 1.0.0

--------------------


### Interfaces


#### AvailableResult

Result from isAvailable() method indicating biometric authentication availability.

| Prop                         | Type                                                                      | Description                                                                                                                                                                 |
| ---------------------------- | ------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`isAvailable`**            | <code>boolean</code>                                                      | Whether authentication is available (biometric or fallback if useFallback is true)                                                                                          |
| **`authenticationStrength`** | <code><a href="#authenticationstrength">AuthenticationStrength</a></code> | The strength of available authentication method (STRONG, WEAK, or NONE)                                                                                                     |
| **`errorCode`**              | <code><a href="#biometricautherror">BiometricAuthError</a></code>         | Error code from <a href="#biometricautherror">BiometricAuthError</a> enum. Only present when isAvailable is false. Indicates why biometric authentication is not available. |


#### IsAvailableOptions

| Prop              | Type                 | Description                                                                                           |
| ----------------- | -------------------- | ----------------------------------------------------------------------------------------------------- |
| **`useFallback`** | <code>boolean</code> | Specifies if should fallback to passcode authentication if biometric authentication is not available. |


#### BiometricOptions

| Prop                     | Type                                                                      | Description                                                                                                                                                                                                                                                                     | Default        |
| ------------------------ | ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------- |
| **`reason`**             | <code>string</code>                                                       |                                                                                                                                                                                                                                                                                 |                |
| **`title`**              | <code>string</code>                                                       |                                                                                                                                                                                                                                                                                 |                |
| **`subtitle`**           | <code>string</code>                                                       |                                                                                                                                                                                                                                                                                 |                |
| **`description`**        | <code>string</code>                                                       |                                                                                                                                                                                                                                                                                 |                |
| **`negativeButtonText`** | <code>string</code>                                                       |                                                                                                                                                                                                                                                                                 |                |
| **`useFallback`**        | <code>boolean</code>                                                      | Specifies if should fallback to passcode authentication if biometric authentication fails.                                                                                                                                                                                      |                |
| **`fallbackTitle`**      | <code>string</code>                                                       | Only for iOS. Set the text for the fallback button in the authentication dialog. If this property is not specified, the default text is set by the system.                                                                                                                      |                |
| **`maxAttempts`**        | <code>number</code>                                                       | Only for Android. Set a maximum number of attempts for biometric authentication. The maximum allowed by android is 5.                                                                                                                                                           | <code>1</code> |
| **`requiredStrength`**   | <code><a href="#authenticationstrength">AuthenticationStrength</a></code> | Specify the authentication strength required. - STRONG: Only strong biometrics (fingerprints, Face ID, etc.) - WEAK: Weak biometrics (face on Android devices that classify it as weak) OR PIN/password If not specified, defaults to allowing both strong and weak biometrics. |                |


#### Credentials

| Prop           | Type                |
| -------------- | ------------------- |
| **`username`** | <code>string</code> |
| **`password`** | <code>string</code> |


#### GetCredentialOptions

| Prop         | Type                |
| ------------ | ------------------- |
| **`server`** | <code>string</code> |


#### SetCredentialOptions

| Prop           | Type                |
| -------------- | ------------------- |
| **`username`** | <code>string</code> |
| **`password`** | <code>string</code> |
| **`server`**   | <code>string</code> |


#### DeleteCredentialOptions

| Prop         | Type                |
| ------------ | ------------------- |
| **`server`** | <code>string</code> |


#### IsCredentialsSavedResult

| Prop          | Type                 |
| ------------- | -------------------- |
| **`isSaved`** | <code>boolean</code> |


#### IsCredentialsSavedOptions

| Prop         | Type                |
| ------------ | ------------------- |
| **`server`** | <code>string</code> |


### Enums


#### AuthenticationStrength

| Members      | Value          | Description                                                                                                                                                                                      |
| ------------ | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **`NONE`**   | <code>0</code> | No authentication available, even if PIN is available but useFallback = false                                                                                                                    |
| **`STRONG`** | <code>1</code> | Strong authentication: Face ID on iOS, fingerprints on devices that consider fingerprints strong (Android). Note: PIN/pattern/password is NEVER considered STRONG, even when useFallback = true. |
| **`WEAK`**   | <code>2</code> | Weak authentication: Face authentication on Android devices that consider face weak, or PIN/pattern/password if useFallback = true (PIN is always WEAK, never STRONG).                           |


#### BiometricAuthError

| Members                       | Value           | Description                                                                           |
| ----------------------------- | --------------- | ------------------------------------------------------------------------------------- |
| **`UNKNOWN_ERROR`**           | <code>0</code>  | Unknown error occurred                                                                |
| **`BIOMETRICS_UNAVAILABLE`**  | <code>1</code>  | Biometrics are unavailable (no hardware or hardware error) Platform: Android, iOS     |
| **`USER_LOCKOUT`**            | <code>2</code>  | User has been locked out due to too many failed attempts Platform: Android, iOS       |
| **`BIOMETRICS_NOT_ENROLLED`** | <code>3</code>  | No biometrics are enrolled on the device Platform: Android, iOS                       |
| **`USER_TEMPORARY_LOCKOUT`**  | <code>4</code>  | User is temporarily locked out (Android: 30 second lockout) Platform: Android         |
| **`AUTHENTICATION_FAILED`**   | <code>10</code> | Authentication failed (user did not authenticate successfully) Platform: Android, iOS |
| **`APP_CANCEL`**              | <code>11</code> | App canceled the authentication (iOS only) Platform: iOS                              |
| **`INVALID_CONTEXT`**         | <code>12</code> | Invalid context (iOS only) Platform: iOS                                              |
| **`NOT_INTERACTIVE`**         | <code>13</code> | Authentication was not interactive (iOS only) Platform: iOS                           |
| **`PASSCODE_NOT_SET`**        | <code>14</code> | Passcode/PIN is not set on the device Platform: Android, iOS                          |
| **`SYSTEM_CANCEL`**           | <code>15</code> | System canceled the authentication (e.g., due to screen lock) Platform: Android, iOS  |
| **`USER_CANCEL`**             | <code>16</code> | User canceled the authentication Platform: Android, iOS                               |
| **`USER_FALLBACK`**           | <code>17</code> | User chose to use fallback authentication method Platform: Android, iOS               |

</docgen-api>
## Face ID (iOS)

To use FaceID Make sure to provide a value for NSFaceIDUsageDescription, otherwise your app may crash on iOS devices with FaceID.

This value is just the reason for using FaceID. You can add something like the following example to App/info.plist:

```xml
<key>NSFaceIDUsageDescription</key>
<string>For an easier and faster log in.</string>
```

## Biometric (Android)

To use android's BiometricPrompt api you must add the following permission to your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC">
```

## Contributors

[Jonthia](https://github.com/jonthia)
[QliQ.dev](https://github.com/qliqdev)
[Brian Weasner](https://github.com/brian-weasner)
[Mohamed Diarra](https://github.com/mohdiarra)
### Want to Contribute?

Learn about contributing [HERE](./CONTRIBUTING.md)

## Notes

Hasn't been tested on Android API level 22 or lower.
