package ee.forgr.biometric;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.util.Base64;
import androidx.activity.result.ActivityResult;
import androidx.biometric.BiometricManager;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONException;

@CapacitorPlugin(name = "NativeBiometric")
public class NativeBiometric extends Plugin {

    private final String PLUGIN_VERSION = "7.3.2";

    //protected final static int AUTH_CODE = 0102;

    private static final int NONE = 0;
    private static final int FINGERPRINT = 3;
    private static final int FACE_AUTHENTICATION = 4;
    private static final int IRIS_AUTHENTICATION = 5;
    private static final int MULTIPLE = 6;

    private KeyStore keyStore;
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String RSA_MODE = "RSA/ECB/PKCS1Padding";
    private static final String AES_MODE = "AES/ECB/PKCS7Padding";
    private static final byte[] FIXED_IV = new byte[12];
    private static final String ENCRYPTED_KEY = "NativeBiometricKey";
    private static final String NATIVE_BIOMETRIC_SHARED_PREFERENCES = "NativeBiometricSharedPreferences";

    private SharedPreferences encryptedSharedPreferences;

    private int getAvailableFeature() {
        // default to none
        BiometricManager biometricManager = BiometricManager.from(getContext());

        // Check for biometric capabilities
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;
        int canAuthenticate = biometricManager.canAuthenticate(authenticators);

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            // Check specific features
            PackageManager pm = getContext().getPackageManager();
            boolean hasFinger = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
            boolean hasIris = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hasIris = pm.hasSystemFeature(PackageManager.FEATURE_IRIS);
            }

            // For face, we rely on BiometricManager since it's more reliable
            boolean hasFace = false;
            try {
                // Try to create a face authentication prompt - if it succeeds, face auth is available
                androidx.biometric.BiometricPrompt.PromptInfo promptInfo = new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Test")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build();
                hasFace = true;
            } catch (Exception e) {
                System.out.println("Error creating face authentication prompt: " + e.getMessage());
            }

            // Determine the type based on available features
            if (hasFinger && (hasFace || hasIris)) {
                return MULTIPLE;
            } else if (hasFinger) {
                return FINGERPRINT;
            } else if (hasFace) {
                return FACE_AUTHENTICATION;
            } else if (hasIris) {
                return IRIS_AUTHENTICATION;
            }
        }

        return NONE;
    }

    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject ret = new JSObject();

        boolean useFallback = Boolean.TRUE.equals(call.getBoolean("useFallback", false));

        BiometricManager biometricManager = BiometricManager.from(getContext());
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;
        if (useFallback) {
            authenticators |= BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        }
        int canAuthenticateResult = biometricManager.canAuthenticate(authenticators);
        // Using deviceHasCredentials instead of canAuthenticate(DEVICE_CREDENTIAL)
        // > "Developers that wish to check for the presence of a PIN, pattern, or password on these versions should instead use isDeviceSecure."
        // @see https://developer.android.com/reference/androidx/biometric/BiometricManager#canAuthenticate(int)
        boolean fallbackAvailable = useFallback && this.deviceHasCredentials();
        if (useFallback && !fallbackAvailable) {
            canAuthenticateResult = BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE;
        }

        boolean isAvailable = (canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS || fallbackAvailable);
        ret.put("isAvailable", isAvailable);

        if (!isAvailable) {
            // BiometricManager Error Constants use the same values as BiometricPrompt's Constants. So we can reuse our
            int pluginErrorCode = AuthActivity.convertToPluginErrorCode(canAuthenticateResult);
            ret.put("errorCode", pluginErrorCode);
        }

        ret.put("biometryType", getAvailableFeature());
        call.resolve(ret);
    }

    @PluginMethod
    public void verifyIdentity(final PluginCall call) throws JSONException {
        Intent intent = new Intent(getContext(), AuthActivity.class);

        intent.putExtra("title", call.getString("title", "Authenticate"));

        String subtitle = call.getString("subtitle");
        if (subtitle != null) {
            intent.putExtra("subtitle", subtitle);
        }

        String description = call.getString("description");
        if (description != null) {
            intent.putExtra("description", description);
        }

        String negativeButtonText = call.getString("negativeButtonText");
        if (negativeButtonText != null) {
            intent.putExtra("negativeButtonText", negativeButtonText);
        }

        Integer maxAttempts = call.getInt("maxAttempts");
        if (maxAttempts != null) {
            intent.putExtra("maxAttempts", maxAttempts);
        }

        // Pass allowed biometry types
        JSArray allowedTypes = call.getArray("allowedBiometryTypes");
        if (allowedTypes != null) {
            int[] types = new int[allowedTypes.length()];
            for (int i = 0; i < allowedTypes.length(); i++) {
                types[i] = (int) allowedTypes.toList().get(i);
            }
            intent.putExtra("allowedBiometryTypes", types);
        }

        boolean useFallback = Boolean.TRUE.equals(call.getBoolean("useFallback", false));
        if (useFallback) {
            useFallback = this.deviceHasCredentials();
        }

        intent.putExtra("useFallback", useFallback);

        startActivityForResult(call, intent, "verifyResult");
    }

    @PluginMethod
    public void setCredentials(final PluginCall call) {
        String username = call.getString("username", null);
        String password = call.getString("password", null);
        String KEY_ALIAS = call.getString("server", null);

        if (username != null && password != null && KEY_ALIAS != null) {
            try {
                SharedPreferences.Editor editor = getContext()
                    .getSharedPreferences(NATIVE_BIOMETRIC_SHARED_PREFERENCES, Context.MODE_PRIVATE)
                    .edit();
                editor.putString(KEY_ALIAS + "-username", encryptString(username, KEY_ALIAS));
                editor.putString(KEY_ALIAS + "-password", encryptString(password, KEY_ALIAS));
                editor.apply();
                call.resolve();
            } catch (GeneralSecurityException | IOException e) {
                call.reject("Failed to save credentials", e);
                System.out.println("Error saving credentials: " + e.getMessage());
            }
        } else {
            call.reject("Missing properties");
        }
    }

    @PluginMethod
    public void getCredentials(final PluginCall call) {
        String KEY_ALIAS = call.getString("server", null);

        SharedPreferences sharedPreferences = getContext().getSharedPreferences(NATIVE_BIOMETRIC_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(KEY_ALIAS + "-username", null);
        String password = sharedPreferences.getString(KEY_ALIAS + "-password", null);
        if (KEY_ALIAS != null) {
            if (username != null && password != null) {
                try {
                    JSObject jsObject = new JSObject();
                    jsObject.put("username", decryptString(username, KEY_ALIAS));
                    jsObject.put("password", decryptString(password, KEY_ALIAS));
                    call.resolve(jsObject);
                } catch (GeneralSecurityException | IOException e) {
                    // Can get here if not authenticated.
                    String errorMessage = "Failed to get credentials";
                    call.reject(errorMessage);
                }
            } else {
                call.reject("No credentials found");
            }
        } else {
            call.reject("No server name was provided");
        }
    }

    @ActivityCallback
    private void verifyResult(PluginCall call, ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            if (data != null && data.hasExtra("result")) {
                switch (Objects.requireNonNull(data.getStringExtra("result"))) {
                    case "success":
                        call.resolve();
                        break;
                    case "failed":
                    case "error":
                        call.reject(data.getStringExtra("errorDetails"), data.getStringExtra("errorCode"));
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

    @PluginMethod
    public void deleteCredentials(final PluginCall call) {
        String KEY_ALIAS = call.getString("server", null);

        if (KEY_ALIAS != null) {
            try {
                getKeyStore().deleteEntry(KEY_ALIAS);
                SharedPreferences.Editor editor = getContext()
                    .getSharedPreferences(NATIVE_BIOMETRIC_SHARED_PREFERENCES, Context.MODE_PRIVATE)
                    .edit();
                editor.clear();
                editor.apply();
                call.resolve();
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
                call.reject("Failed to delete", e);
            }
        } else {
            call.reject("No server name was provided");
        }
    }

    @PluginMethod
    public void isCredentialsSaved(final PluginCall call) {
        String KEY_ALIAS = call.getString("server", null);

        if (KEY_ALIAS != null) {
            SharedPreferences sharedPreferences = getContext()
                .getSharedPreferences(NATIVE_BIOMETRIC_SHARED_PREFERENCES, Context.MODE_PRIVATE);
            String username = sharedPreferences.getString(KEY_ALIAS + "-username", null);
            String password = sharedPreferences.getString(KEY_ALIAS + "-password", null);

            JSObject ret = new JSObject();
            ret.put("isSaved", username != null && password != null);
            call.resolve(ret);
        } else {
            call.reject("No server name was provided");
        }
    }

    private String encryptString(String stringToEncrypt, String KEY_ALIAS) throws GeneralSecurityException, IOException {
        Cipher cipher;
        cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getKey(KEY_ALIAS), new GCMParameterSpec(128, FIXED_IV));
        byte[] encodedBytes = cipher.doFinal(stringToEncrypt.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encodedBytes, Base64.DEFAULT);
    }

    private String decryptString(String stringToDecrypt, String KEY_ALIAS) throws GeneralSecurityException, IOException {
        byte[] encryptedData = Base64.decode(stringToDecrypt, Base64.DEFAULT);

        Cipher cipher;
        cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getKey(KEY_ALIAS), new GCMParameterSpec(128, FIXED_IV));
        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    @SuppressLint("NewAPI") // API level is already checked
    private Key generateKey(String KEY_ALIAS) throws GeneralSecurityException, IOException {
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
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        KeyGenParameterSpec.Builder paramBuilder = new KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(false);

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

    private Key getKey(String KEY_ALIAS) throws GeneralSecurityException, IOException {
        KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) getKeyStore().getEntry(KEY_ALIAS, null);
        if (secretKeyEntry != null) {
            return secretKeyEntry.getSecretKey();
        }
        return generateKey(KEY_ALIAS);
    }

    private KeyStore getKeyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        if (keyStore == null) {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
        }
        return keyStore;
    }

    private Key getAESKey(String KEY_ALIAS)
        throws CertificateException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, UnrecoverableEntryException, IOException, InvalidAlgorithmParameterException {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("", Context.MODE_PRIVATE);
        String encryptedKeyB64 = sharedPreferences.getString(ENCRYPTED_KEY, null);
        if (encryptedKeyB64 == null) {
            byte[] key = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(key);
            byte[] encryptedKey = rsaEncrypt(key, KEY_ALIAS);
            encryptedKeyB64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT);
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putString(ENCRYPTED_KEY, encryptedKeyB64);
            edit.apply();
            return new SecretKeySpec(key, "AES");
        } else {
            byte[] encryptedKey = Base64.decode(encryptedKeyB64, Base64.DEFAULT);
            byte[] key = rsaDecrypt(encryptedKey, KEY_ALIAS);
            return new SecretKeySpec(key, "AES");
        }
    }

    private KeyStore.PrivateKeyEntry getPrivateKeyEntry(String KEY_ALIAS)
        throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, CertificateException, KeyStoreException, IOException, UnrecoverableEntryException {
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) getKeyStore().getEntry(KEY_ALIAS, null);

        if (privateKeyEntry == null) {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
            keyPairGenerator.initialize(
                new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setUserAuthenticationRequired(true)
                    // Set authentication validity duration to 0 to require authentication for every use
                    .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                    .build()
            );
            keyPairGenerator.generateKeyPair();
            // Get the newly generated key entry
            privateKeyEntry = (KeyStore.PrivateKeyEntry) getKeyStore().getEntry(KEY_ALIAS, null);
        }

        return privateKeyEntry;
    }

    private byte[] rsaEncrypt(byte[] secret, String KEY_ALIAS)
        throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableEntryException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        KeyStore.PrivateKeyEntry privateKeyEntry = getPrivateKeyEntry(KEY_ALIAS);
        // Encrypt the text
        Cipher inputCipher = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
        inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, inputCipher);
        cipherOutputStream.write(secret);
        cipherOutputStream.close();

        return outputStream.toByteArray();
    }

    private byte[] rsaDecrypt(byte[] encrypted, String KEY_ALIAS)
        throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IOException, CertificateException, InvalidAlgorithmParameterException {
        KeyStore.PrivateKeyEntry privateKeyEntry = getPrivateKeyEntry(KEY_ALIAS);
        Cipher output = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
        output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
        CipherInputStream cipherInputStream = new CipherInputStream(new ByteArrayInputStream(encrypted), output);
        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte) nextByte);
        }

        byte[] bytes = new byte[values.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i);
        }
        return bytes;
    }

    private boolean deviceHasCredentials() {
        KeyguardManager keyguardManager = (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        // Can only use fallback if the device has a pin/pattern/password lockscreen.
        return keyguardManager.isDeviceSecure();
    }

    @PluginMethod
    public void getPluginVersion(final PluginCall call) {
        try {
            final JSObject ret = new JSObject();
            ret.put("version", this.PLUGIN_VERSION);
            call.resolve(ret);
        } catch (final Exception e) {
            call.reject("Could not get plugin version", e);
        }
    }
}
