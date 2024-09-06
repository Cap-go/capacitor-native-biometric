# Capacitor Native Biometric 
 <a href="https://capgo.app/"><img src='https://raw.githubusercontent.com/Cap-go/capgo/main/assets/capgo_banner.png' alt='Capgo - Instant updates for capacitor'/></a>

<div align="center">
  <h2><a href="https://capgo.app/?ref=plugin"> ➡️ Get Instant updates for your App with Capgo 🚀</a></h2>
  <h2><a href="https://capgo.app/consulting/?ref=plugin"> Fix your annoying bug now, Hire a Capacitor expert 💪</a></h2>
</div>


Use biometrics confirm device owner presence or authenticate users. A couple of methods are provided to handle user credentials. These are securely stored using Keychain (iOS) and Keystore (Android).

## Installation (Only supports Capacitor 6)

- `npm i @capgo/capacitor-native-biometric`

## Usage

```ts
import { NativeBiometric, BiometryType } from "@capgo/capacitor-native-biometric";

async performBiometricVerification(){
  const result = await NativeBiometric.isAvailable();

  if(!result.isAvailable) return;

  const isFaceID = result.biometryType == BiometryType.FACE_ID;

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
* [Interfaces](#interfaces)
* [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### isAvailable(...)

```typescript
isAvailable(options?: IsAvailableOptions | undefined) => any
```

Checks if biometric authentication hardware is available.

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#isavailableoptions">IsAvailableOptions</a></code> |

**Returns:** <code>any</code>

**Since:** 1.0.0

--------------------


### verifyIdentity(...)

```typescript
verifyIdentity(options?: BiometricOptions | undefined) => any
```

Prompts the user to authenticate with biometrics.

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code><a href="#biometricoptions">BiometricOptions</a></code> |

**Returns:** <code>any</code>

**Since:** 1.0.0

--------------------


### getCredentials(...)

```typescript
getCredentials(options: GetCredentialOptions) => any
```

Gets the stored credentials for a given server.

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#getcredentialoptions">GetCredentialOptions</a></code> |

**Returns:** <code>any</code>

**Since:** 1.0.0

--------------------


### setCredentials(...)

```typescript
setCredentials(options: SetCredentialOptions) => any
```

Stores the given credentials for a given server.

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#setcredentialoptions">SetCredentialOptions</a></code> |

**Returns:** <code>any</code>

**Since:** 1.0.0

--------------------


### deleteCredentials(...)

```typescript
deleteCredentials(options: DeleteCredentialOptions) => any
```

Deletes the stored credentials for a given server.

| Param         | Type                                                                        |
| ------------- | --------------------------------------------------------------------------- |
| **`options`** | <code><a href="#deletecredentialoptions">DeleteCredentialOptions</a></code> |

**Returns:** <code>any</code>

**Since:** 1.0.0

--------------------


### Interfaces


#### IsAvailableOptions

| Prop              | Type                 | Description                                                                                           |
| ----------------- | -------------------- | ----------------------------------------------------------------------------------------------------- |
| **`useFallback`** | <code>boolean</code> | Specifies if should fallback to passcode authentication if biometric authentication is not available. |


#### AvailableResult

| Prop               | Type                                                  |
| ------------------ | ----------------------------------------------------- |
| **`isAvailable`**  | <code>boolean</code>                                  |
| **`biometryType`** | <code><a href="#biometrytype">BiometryType</a></code> |
| **`errorCode`**    | <code>number</code>                                   |


#### BiometricOptions

| Prop                     | Type                 | Description                                                                                                                                                | Default        |
| ------------------------ | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------- |
| **`reason`**             | <code>string</code>  |                                                                                                                                                            |                |
| **`title`**              | <code>string</code>  |                                                                                                                                                            |                |
| **`subtitle`**           | <code>string</code>  |                                                                                                                                                            |                |
| **`description`**        | <code>string</code>  |                                                                                                                                                            |                |
| **`negativeButtonText`** | <code>string</code>  |                                                                                                                                                            |                |
| **`useFallback`**        | <code>boolean</code> | Specifies if should fallback to passcode authentication if biometric authentication fails.                                                                 |                |
| **`fallbackTitle`**      | <code>string</code>  | Only for iOS. Set the text for the fallback button in the authentication dialog. If this property is not specified, the default text is set by the system. |                |
| **`maxAttempts`**        | <code>number</code>  | Only for Android. Set a maximum number of attempts for biometric authentication. The maximum allowed by android is 5.                                      | <code>1</code> |


#### GetCredentialOptions

| Prop         | Type                |
| ------------ | ------------------- |
| **`server`** | <code>string</code> |


#### Credentials

| Prop           | Type                |
| -------------- | ------------------- |
| **`username`** | <code>string</code> |
| **`password`** | <code>string</code> |


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


### Enums


#### BiometryType

| Members                   | Value          |
| ------------------------- | -------------- |
| **`NONE`**                | <code>0</code> |
| **`TOUCH_ID`**            | <code>1</code> |
| **`FACE_ID`**             | <code>2</code> |
| **`FINGERPRINT`**         | <code>3</code> |
| **`FACE_AUTHENTICATION`** | <code>4</code> |
| **`IRIS_AUTHENTICATION`** | <code>5</code> |
| **`MULTIPLE`**            | <code>6</code> |

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

And register the plugin by adding it to you MainActivity's onCreate (Not needed for Capacitor 3):

```java
import ee.forgr.biometric.NativeBiometric;

public class MainActivity extends BridgeActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Initializes the Bridge
    this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {{
      // Additional plugins you've installed go here
      // Ex: add(TotallyAwesomePlugin.class);
      add(NativeBiometric.class);
    }});
  }
}
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
