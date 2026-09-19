package ee.forgr.biometric;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import androidx.biometric.BiometricPrompt;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

/**
 * Asymmetric Keystore storage for secure data ({@code setData}/{@code getSecureData}) when
 * {@code authValidityDuration} is 0. Writes use the RSA public key (no biometric prompt);
 * reads unwrap the AES key with the authenticated private key via BiometricPrompt.
 */
final class AsymmetricSecureDataHelper {

    static final String ASYMMETRIC_KEY_PREFIX = "NativeBiometricAsymmetric_";
    static final String FORMAT_SUFFIX = "_format";
    static final int FORMAT_SYMMETRIC = 0;
    static final int FORMAT_ASYMMETRIC = 1;

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE_BYTES = 32;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int WRAPPED_KEY_LENGTH_BYTES = 2;

    private AsymmetricSecureDataHelper() {}

    static boolean isAsymmetricFormat(SharedPreferences prefs, String storageKey) {
        return prefs.getInt(secureFormatKey(storageKey), FORMAT_SYMMETRIC) == FORMAT_ASYMMETRIC;
    }

    static String secureFormatKey(String storageKey) {
        return "secure_" + storageKey + FORMAT_SUFFIX;
    }

    static String asymmetricAlias(String storageKey) {
        return ASYMMETRIC_KEY_PREFIX + storageKey;
    }

    /**
     * Encrypts {@code plaintext} with a fresh AES-256-GCM key, wraps that key with the RSA public
     * key (no user authentication), and returns the Base64-encoded blob.
     */
    static String encryptAndEncode(Context context, String storageKey, byte[] plaintext, int accessControl)
        throws GeneralSecurityException, IOException {
        byte[] aesKey = new byte[AES_KEY_SIZE_BYTES];
        new SecureRandom().nextBytes(aesKey);

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
        aesCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] ciphertext = aesCipher.doFinal(plaintext);

        KeyStore.PrivateKeyEntry entry = getOrCreateKeyPair(context, storageKey, accessControl);
        byte[] wrappedKey = rsaWrap(aesKey, entry.getCertificate().getPublicKey());

        return Base64.encodeToString(packBlob(iv, wrappedKey, ciphertext), Base64.DEFAULT);
    }

    /**
     * Builds a {@link BiometricPrompt.CryptoObject} whose RSA cipher unwraps the stored AES key.
     * The caller must finish decryption with {@link #decryptPayload}.
     */
    static BiometricPrompt.CryptoObject createDecryptCryptoObject(Context context, String storageKey)
        throws GeneralSecurityException, IOException {
        KeyStore.PrivateKeyEntry entry = getExistingPrivateKeyEntry(context, storageKey);
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        try {
            cipher.init(Cipher.DECRYPT_MODE, entry.getPrivateKey(), oaepSpec());
        } catch (InvalidKeyException e) {
            if (e instanceof KeyPermanentlyInvalidatedException) {
                throw e;
            }
            throw new GeneralSecurityException("Failed to init RSA decrypt cipher", e);
        }
        return new BiometricPrompt.CryptoObject(cipher);
    }

    /**
     * Completes decryption after BiometricPrompt succeeds: unwraps the AES key and decrypts the payload.
     */
    static String decryptPayload(Cipher rsaCipher, String encodedData) throws GeneralSecurityException {
        ParsedBlob parsed = parseBlob(Base64.decode(encodedData, Base64.DEFAULT));
        byte[] aesKey = rsaCipher.doFinal(parsed.wrappedKey);
        Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
        aesCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, parsed.iv));
        byte[] plaintext = aesCipher.doFinal(parsed.ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    static void deleteAsymmetricKey(Context context, String storageKey) {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
            ks.load(null);
            ks.deleteEntry(asymmetricAlias(storageKey));
        } catch (GeneralSecurityException | IOException ignored) {
            // Best effort
        }
    }

    static void deleteSymmetricLegacyKey(Context context, String storageKey) {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
            ks.load(null);
            ks.deleteEntry("NativeBiometricSecure_" + storageKey);
        } catch (GeneralSecurityException | IOException ignored) {
            // Best effort
        }
    }

    private static KeyStore.PrivateKeyEntry getOrCreateKeyPair(Context context, String storageKey, int accessControl)
        throws GeneralSecurityException, IOException {
        String alias = asymmetricAlias(storageKey);
        KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
        ks.load(null);

        SharedPreferences prefs = context.getSharedPreferences("NativeBiometricSharedPreferences", Context.MODE_PRIVATE);
        int storedAccessControl = prefs.getInt("secure_" + storageKey + "_access_control", 0);
        if (ks.containsAlias(alias) && accessControl > 0 && storedAccessControl != accessControl) {
            ks.deleteEntry(alias);
            prefs.edit().remove("secure_" + storageKey).apply();
        }

        if (ks.containsAlias(alias)) {
            KeyStore.PrivateKeyEntry entry = requirePrivateKeyEntry(ks, alias);
            try {
                Cipher probe = Cipher.getInstance(RSA_TRANSFORMATION);
                probe.init(Cipher.DECRYPT_MODE, entry.getPrivateKey(), oaepSpec());
                return entry;
            } catch (KeyPermanentlyInvalidatedException e) {
                ks.deleteEntry(alias);
                prefs.edit().remove("secure_" + storageKey).remove(secureFormatKey(storageKey)).apply();
            }
        }

        int effectiveAccessControl = accessControl > 0 ? accessControl : storedAccessControl;
        boolean invalidatedByEnrollment = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && effectiveAccessControl == 1;
        try {
            generateKeyPair(alias, invalidatedByEnrollment);
        } catch (ProviderException e) {
            if (invalidatedByEnrollment) {
                try {
                    generateKeyPair(alias, false);
                } catch (ProviderException retryError) {
                    throw new GeneralSecurityException("Keystore key generation failed", retryError);
                }
            } else {
                throw new GeneralSecurityException("Keystore key generation failed", e);
            }
        }
        return requirePrivateKeyEntry(ks, alias);
    }

    private static KeyStore.PrivateKeyEntry getExistingPrivateKeyEntry(Context context, String storageKey)
        throws GeneralSecurityException, IOException {
        String alias = asymmetricAlias(storageKey);
        KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
        ks.load(null);
        if (!ks.containsAlias(alias)) {
            throw new GeneralSecurityException("Asymmetric key not found");
        }
        return requirePrivateKeyEntry(ks, alias);
    }

    private static KeyStore.PrivateKeyEntry requirePrivateKeyEntry(KeyStore ks, String alias) throws GeneralSecurityException {
        try {
            KeyStore.Entry entry = ks.getEntry(alias, null);
            if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                throw new GeneralSecurityException("Invalid key entry type");
            }
            return (KeyStore.PrivateKeyEntry) entry;
        } catch (GeneralSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to load keystore key", e);
        }
    }

    private static void generateKeyPair(String alias, boolean invalidatedByEnrollment) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setKeySize(2048)
            .setUserAuthenticationRequired(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(-1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(invalidatedByEnrollment);
        }

        generator.initialize(builder.build());
        generator.generateKeyPair();
    }

    private static byte[] rsaWrap(byte[] secret, java.security.PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepSpec());
        return cipher.doFinal(secret);
    }

    private static OAEPParameterSpec oaepSpec() {
        return new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
    }

    private static byte[] packBlob(byte[] iv, byte[] wrappedKey, byte[] ciphertext) {
        ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + WRAPPED_KEY_LENGTH_BYTES + wrappedKey.length + ciphertext.length);
        buffer.put(iv);
        buffer.putShort((short) wrappedKey.length);
        buffer.put(wrappedKey);
        buffer.put(ciphertext);
        return buffer.array();
    }

    private static ParsedBlob parseBlob(byte[] combined) throws GeneralSecurityException {
        if (combined.length < GCM_IV_LENGTH + WRAPPED_KEY_LENGTH_BYTES + 1) {
            throw new GeneralSecurityException("Invalid asymmetric secure data blob");
        }
        ByteBuffer buffer = ByteBuffer.wrap(combined);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        int wrappedLen = buffer.getShort() & 0xFFFF;
        if (buffer.remaining() < wrappedLen + 1) {
            throw new GeneralSecurityException("Invalid asymmetric secure data blob");
        }
        byte[] wrappedKey = new byte[wrappedLen];
        buffer.get(wrappedKey);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        return new ParsedBlob(iv, wrappedKey, ciphertext);
    }

    private static final class ParsedBlob {

        final byte[] iv;
        final byte[] wrappedKey;
        final byte[] ciphertext;

        ParsedBlob(byte[] iv, byte[] wrappedKey, byte[] ciphertext) {
            this.iv = iv;
            this.wrappedKey = wrappedKey;
            this.ciphertext = ciphertext;
        }
    }
}
