package ee.forgr.biometric;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.util.Base64;
import androidx.activity.result.ActivityResult;
import androidx.biometric.BiometricManager;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

@CapacitorPlugin(name = "NativeBiometric")
public class NativeBiometric extends Plugin {

  private static final int NONE = 0;
  private static final int FINGERPRINT = 3;
  private static final int FACE_AUTHENTICATION = 4;
  private static final int IRIS_AUTHENTICATION = 5;
  private static final int MULTIPLE = 6;

  private KeyStore keyStore;
  private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final byte[] LEGACY_FIXED_IV = new byte[GCM_IV_LENGTH];
  private static final String NATIVE_BIOMETRIC_SHARED_PREFERENCES =
    "NativeBiometricSharedPreferences";

  private int getAvailableFeature() {
    // default to none
    int type = NONE;

    // if has fingerprint
    if (
      getContext()
        .getPackageManager()
        .hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    ) {
      type = FINGERPRINT;
    }

    // if has face auth
    if (
      getContext()
        .getPackageManager()
        .hasSystemFeature(PackageManager.FEATURE_FACE)
    ) {
      // if also has fingerprint
      if (type != NONE) return MULTIPLE;

      type = FACE_AUTHENTICATION;
    }

    // if has iris auth
    if (
      getContext()
        .getPackageManager()
        .hasSystemFeature(PackageManager.FEATURE_IRIS)
    ) {
      // if also has fingerprint or face auth
      if (type != NONE) return MULTIPLE;

      type = IRIS_AUTHENTICATION;
    }

    return type;
  }

  @PluginMethod
  public void isAvailable(PluginCall call) {
    JSObject ret = new JSObject();

    boolean useFallback = Boolean.TRUE.equals(
      call.getBoolean("useFallback", false)
    );

    boolean isWeakAuthenticatorAllowed = Boolean.TRUE.equals(
      call.getBoolean("isWeakAuthenticatorAllowed", false)
    );

    int allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;
    if (isWeakAuthenticatorAllowed)
      allowedAuthenticators = allowedAuthenticators | BiometricManager.Authenticators.BIOMETRIC_WEAK;

    BiometricManager biometricManager = BiometricManager.from(getContext());
    int canAuthenticateResult = biometricManager.canAuthenticate(allowedAuthenticators);
    // Using deviceHasCredentials instead of canAuthenticate(DEVICE_CREDENTIAL)
    // > "Developers that wish to check for the presence of a PIN, pattern, or password on these versions should instead use isDeviceSecure."
    // @see https://developer.android.com/reference/androidx/biometric/BiometricManager#canAuthenticate(int)
    boolean fallbackAvailable = useFallback && this.deviceHasCredentials();
    if (useFallback && !fallbackAvailable) {
      canAuthenticateResult = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE;
    }

    boolean isAvailable =
      (
        canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS ||
          fallbackAvailable
      );
    ret.put("isAvailable", isAvailable);

    if (!isAvailable) {
      // BiometricManager Error Constants use the same values as BiometricPrompt's Constants. So we can reuse our
      int pluginErrorCode = AuthActivity.convertToPluginErrorCode(
        canAuthenticateResult
      );
      ret.put("errorCode", pluginErrorCode);
    }

    ret.put("biometryType", getAvailableFeature());
    call.resolve(ret);
  }

  @PluginMethod
  public void verifyIdentity(final PluginCall call) {
    Intent intent = new Intent(getContext(), AuthActivity.class);

    intent.putExtra("title", call.getString("title", "Authenticate"));

    if (call.hasOption("subtitle")) {
      intent.putExtra("subtitle", call.getString("subtitle"));
    }

    if (call.hasOption("description")) {
      intent.putExtra("description", call.getString("description"));
    }

    if (call.hasOption("negativeButtonText")) {
      intent.putExtra(
        "negativeButtonText",
        call.getString("negativeButtonText")
      );
    }

    if (call.hasOption("maxAttempts")) {
      intent.putExtra("maxAttempts", call.getInt("maxAttempts"));
    }

    boolean useFallback = Boolean.TRUE.equals(
      call.getBoolean("useFallback", false)
    );
    if (useFallback) {
      useFallback = this.deviceHasCredentials();
    }

    intent.putExtra("useFallback", useFallback);

    if (call.hasOption("isWeakAuthenticatorAllowed")) {
      intent.putExtra("isWeakAuthenticatorAllowed", call.getBoolean("isWeakAuthenticatorAllowed"));
    }

    startActivityForResult(call, intent, "verifyResult");
  }

  @PluginMethod
  public void verifyIdentityAndGetCredentials(final PluginCall call) {
    Intent intent = new Intent(getContext(), AuthActivity.class);

    intent.putExtra("title", call.getString("title", "Authenticate"));

    if (call.hasOption("subtitle")) {
      intent.putExtra("subtitle", call.getString("subtitle"));
    }

    if (call.hasOption("description")) {
      intent.putExtra("description", call.getString("description"));
    }

    if (call.hasOption("negativeButtonText")) {
      intent.putExtra(
        "negativeButtonText",
        call.getString("negativeButtonText")
      );
    }

    if (call.hasOption("maxAttempts")) {
      intent.putExtra("maxAttempts", call.getInt("maxAttempts"));
    }

    boolean useFallback = Boolean.TRUE.equals(
      call.getBoolean("useFallback", false)
    );
    if (useFallback) {
      useFallback = this.deviceHasCredentials();
    }

    intent.putExtra("useFallback", useFallback);

    if (call.hasOption("isWeakAuthenticatorAllowed")) {
      intent.putExtra("isWeakAuthenticatorAllowed", call.getBoolean("isWeakAuthenticatorAllowed"));
    }

    startActivityForResult(call, intent, "verifyIdentityAndGetCredentialsResult");
  }

  @PluginMethod
  public void setCredentials(final PluginCall call) {
    String username = call.getString("username", null);
    String password = call.getString("password", null);
    String KEY_ALIAS = call.getString("server", null);

    if (username != null && password != null && KEY_ALIAS != null) {
      try {
        SharedPreferences.Editor editor = getContext()
          .getSharedPreferences(
            NATIVE_BIOMETRIC_SHARED_PREFERENCES,
            Context.MODE_PRIVATE
          )
          .edit();
        editor.putString(
          KEY_ALIAS + "-username",
          encryptString(username, KEY_ALIAS)
        );
        editor.putString(
          KEY_ALIAS + "-password",
          encryptString(password, KEY_ALIAS)
        );
        editor.apply();
        call.resolve();
      } catch (GeneralSecurityException | IOException e) {
        call.reject("Failed to save credentials", e);
        e.printStackTrace();
      }
    } else {
      call.reject("Missing properties");
    }
  }

  @ActivityCallback
  private void verifyResult(PluginCall call, ActivityResult result) {
    if (result.getResultCode() == Activity.RESULT_OK) {
      Intent data = result.getData();
      if (data != null && data.hasExtra("result")) {
        switch (data.getStringExtra("result")) {
          case "success":
            call.resolve();
            break;
          case "failed":
          case "error":
            call.reject(
              data.getStringExtra("errorDetails"),
              data.getStringExtra("errorCode")
            );
            break;
          default:
            // Should not get to here unless AuthActivity starts returning different Activity Results.
            call.reject("Something went wrong.");
            break;
        }
      }
    } else {
      call.reject("Something went wrong.");
    }
  }

  @ActivityCallback
  private void verifyIdentityAndGetCredentialsResult(PluginCall call, ActivityResult result) {
    if (result.getResultCode() == Activity.RESULT_OK) {
      Intent data = result.getData();
      if (data != null && data.hasExtra("result")) {
        switch (data.getStringExtra("result")) {
          case "success": {
            String KEY_ALIAS = call.getString("server", null);
            if (KEY_ALIAS == null) {
              call.reject("No server name was provided");
              return;
            }
            SharedPreferences sharedPreferences = getContext()
              .getSharedPreferences(
                NATIVE_BIOMETRIC_SHARED_PREFERENCES,
                Context.MODE_PRIVATE
              );
            String username = sharedPreferences.getString(
              KEY_ALIAS + "-username",
              null
            );
            String password = sharedPreferences.getString(
              KEY_ALIAS + "-password",
              null
            );
            if (username == null || password == null) {
              call.reject("No credentials found");
              return;
            }
            try {
              JSObject jsObject = new JSObject();
              jsObject.put("username", decryptString(username, KEY_ALIAS));
              jsObject.put("password", decryptString(password, KEY_ALIAS));
              call.resolve(jsObject);
            } catch (GeneralSecurityException | IOException e) {
              call.reject("Failed to get credentials");
            }
            break;
          }
          case "failed":
          case "error":
            call.reject(
              data.getStringExtra("errorDetails"),
              data.getStringExtra("errorCode")
            );
            break;
          default:
            call.reject("Something went wrong.");
            break;
        }
      }
    } else {
      call.reject("Something went wrong.");
    }
  }

  @PluginMethod
  public void deleteCredentials(final PluginCall call) {
    String KEY_ALIAS = call.getString("server", null);

    if (KEY_ALIAS != null) {
      try {
        getKeyStore().deleteEntry(KEY_ALIAS);
        SharedPreferences.Editor editor = getContext()
          .getSharedPreferences(
            NATIVE_BIOMETRIC_SHARED_PREFERENCES,
            Context.MODE_PRIVATE
          )
          .edit();
        editor.clear();
        editor.apply();
        call.resolve();
      } catch (
        KeyStoreException
        | CertificateException
        | NoSuchAlgorithmException
        | IOException e
      ) {
        call.reject("Failed to delete", e);
      }
    } else {
      call.reject("No server name was provided");
    }
  }

  private String encryptString(String stringToEncrypt, String KEY_ALIAS)
    throws GeneralSecurityException, IOException {
    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(Cipher.ENCRYPT_MODE, getKey(KEY_ALIAS));
    byte[] iv = cipher.getIV();
    byte[] ciphertext = cipher.doFinal(stringToEncrypt.getBytes("UTF-8"));
    byte[] combined = new byte[iv.length + ciphertext.length];
    System.arraycopy(iv, 0, combined, 0, iv.length);
    System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
    return Base64.encodeToString(combined, Base64.DEFAULT);
  }

  private String decryptString(String stringToDecrypt, String KEY_ALIAS)
    throws GeneralSecurityException, IOException {
    byte[] data = Base64.decode(stringToDecrypt, Base64.DEFAULT);

    // Try new format first: iv(12) || ciphertext+tag
    if (data.length > GCM_IV_LENGTH) {
      try {
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(data, 0, iv, 0, GCM_IV_LENGTH);
        byte[] ciphertext = new byte[data.length - GCM_IV_LENGTH];
        System.arraycopy(data, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getKey(KEY_ALIAS), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ciphertext), "UTF-8");
      } catch (GeneralSecurityException e) {
        // Fall through to legacy format
      }
    }
    // Legacy format: fixed zero IV, no IV prefix
    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(Cipher.DECRYPT_MODE, getKey(KEY_ALIAS), new GCMParameterSpec(128, LEGACY_FIXED_IV));
    return new String(cipher.doFinal(data), "UTF-8");
  }

  @SuppressLint("NewAPI") // API level is already checked
  private Key generateKey(String KEY_ALIAS)
    throws GeneralSecurityException, IOException {
    Key key;
    try {
      key = generateKey(KEY_ALIAS, true);
    } catch (StrongBoxUnavailableException e) {
      key = generateKey(KEY_ALIAS, false);
    }
    return key;
  }

  private Key generateKey(String KEY_ALIAS, boolean isStrongBoxBacked)
    throws GeneralSecurityException, IOException, StrongBoxUnavailableException {
    KeyGenerator generator = KeyGenerator.getInstance(
      KeyProperties.KEY_ALGORITHM_AES,
      ANDROID_KEY_STORE
    );
    KeyGenParameterSpec.Builder paramBuilder = new KeyGenParameterSpec.Builder(
      KEY_ALIAS,
      KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setRandomizedEncryptionRequired(true);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || Build.VERSION.SDK_INT > 34) {
        // Avoiding setUnlockedDeviceRequired(true) due to known issues on Android 12-14
        paramBuilder.setUnlockedDeviceRequired(true);
      }
      paramBuilder.setIsStrongBoxBacked(isStrongBoxBacked);
    }

    generator.init(paramBuilder.build());
    return generator.generateKey();
  }

  private Key getKey(String KEY_ALIAS)
    throws GeneralSecurityException, IOException {
    KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) getKeyStore()
      .getEntry(KEY_ALIAS, null);
    if (secretKeyEntry != null) {
      return secretKeyEntry.getSecretKey();
    }
    return generateKey(KEY_ALIAS);
  }

  private KeyStore getKeyStore()
    throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
    if (keyStore == null) {
      keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
      keyStore.load(null);
    }
    return keyStore;
  }

  private KeyStore.PrivateKeyEntry getPrivateKeyEntry(String KEY_ALIAS)
    throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, CertificateException, KeyStoreException, IOException, UnrecoverableEntryException {
    KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) getKeyStore()
      .getEntry(KEY_ALIAS, null);

    if (privateKeyEntry == null) {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_RSA,
        ANDROID_KEY_STORE
      );
      keyPairGenerator.initialize(
        new KeyPairGeneratorSpec.Builder(getContext())
          .setAlias(KEY_ALIAS)
          .build()
      );
      keyPairGenerator.generateKeyPair();
    }

    return privateKeyEntry;
  }

  private boolean deviceHasCredentials() {
    KeyguardManager keyguardManager = (KeyguardManager) getActivity()
      .getSystemService(Context.KEYGUARD_SERVICE);
    // Can only use fallback if the device has a pin/pattern/password lockscreen.
    return keyguardManager.isDeviceSecure();
  }
}
