package ee.forgr.biometric;

import android.os.Build;
import android.security.keystore.KeyProperties;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricManager;

/**
 * Maps plugin {@code allowedBiometryTypes} values to Android BiometricPrompt authenticators
 * and matching Keystore user-authentication requirements.
 */
public final class BiometricAuthenticatorConfig {

    private static final int FINGERPRINT = 3;
    private static final int FACE_AUTHENTICATION = 4;
    private static final int IRIS_AUTHENTICATION = 5;
    private static final int MULTIPLE = 6;
    private static final int DEVICE_CREDENTIAL = 7;

    // Mirrors KeyProperties auth-type flags when older compile stubs omit symbols.
    private static final int KEY_AUTH_BIOMETRIC_STRONG = 1;
    private static final int KEY_AUTH_BIOMETRIC_WEAK = 2;
    private static final int KEY_AUTH_DEVICE_CREDENTIAL = 4;

    public static final int PROMPT_BIOMETRIC_ANY =
        BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK;

    /**
     * Stored alongside Keystore auth-key metadata. Version 0 (missing) means plugin {@code KEY_AUTH_*}
     * flags were passed straight into {@code setUserAuthenticationParameters}; version 1 maps through
     * {@link #toKeyPropertiesAuthTypes(int)} first.
     */
    public static final int KEY_AUTH_TYPES_SCHEME_VERSION = 1;

    public final int promptAuthenticators;
    public final int keyAuthTypes;
    public final boolean allowNegativeButton;
    public final boolean requiresCryptoObject;

    BiometricAuthenticatorConfig(int promptAuthenticators, int keyAuthTypes, boolean allowNegativeButton, boolean requiresCryptoObject) {
        this.promptAuthenticators = promptAuthenticators;
        this.keyAuthTypes = keyAuthTypes;
        this.allowNegativeButton = allowNegativeButton;
        this.requiresCryptoObject = requiresCryptoObject;
    }

    public static BiometricAuthenticatorConfig fromAllowedTypes(int[] allowedTypes) {
        if (allowedTypes == null || allowedTypes.length == 0) {
            return defaultBiometric();
        }

        int promptAuth = 0;
        int keyAuth = 0;
        boolean hasBiometric = false;
        boolean hasDeviceCredential = false;
        boolean fingerprintOnly = true;

        for (int type : allowedTypes) {
            switch (type) {
                case FINGERPRINT:
                    promptAuth |= BiometricManager.Authenticators.BIOMETRIC_STRONG;
                    keyAuth |= keyAuthStrong();
                    hasBiometric = true;
                    break;
                case FACE_AUTHENTICATION:
                case IRIS_AUTHENTICATION:
                    promptAuth |= PROMPT_BIOMETRIC_ANY;
                    keyAuth |= keyAuthAny();
                    hasBiometric = true;
                    fingerprintOnly = false;
                    break;
                case MULTIPLE:
                    promptAuth |= PROMPT_BIOMETRIC_ANY;
                    keyAuth |= keyAuthAny();
                    hasBiometric = true;
                    fingerprintOnly = false;
                    break;
                case DEVICE_CREDENTIAL:
                    promptAuth |= BiometricManager.Authenticators.DEVICE_CREDENTIAL;
                    keyAuth |= keyAuthDeviceCredential();
                    hasDeviceCredential = true;
                    fingerprintOnly = false;
                    break;
                default:
                    // Ignore iOS-only enum values (TOUCH_ID, FACE_ID).
                    break;
            }
        }

        if (promptAuth == 0) {
            return defaultBiometric();
        }

        if (hasBiometric && fingerprintOnly && !hasDeviceCredential) {
            promptAuth = BiometricManager.Authenticators.BIOMETRIC_STRONG;
            keyAuth = keyAuthStrong();
        }

        boolean allowNegative = !hasDeviceCredential;
        boolean deviceCredentialOnly = hasDeviceCredential && !hasBiometric;

        return new BiometricAuthenticatorConfig(promptAuth, keyAuth > 0 ? keyAuth : keyAuthAny(), allowNegative, !deviceCredentialOnly);
    }

    private static BiometricAuthenticatorConfig defaultBiometric() {
        return new BiometricAuthenticatorConfig(PROMPT_BIOMETRIC_ANY, keyAuthAny(), true, true);
    }

    /**
     * Default for modes that bind a {@code CryptoObject} to {@code BiometricPrompt}
     * ({@code setSecureCredentials}, {@code getSecureCredentials}, and secure data variants).
     * AndroidX rejects crypto-based auth when Class 2 (Weak) authenticators are allowed, so
     * this must be strong-only — never {@link #defaultBiometric()}.
     */
    static BiometricAuthenticatorConfig defaultForCryptoBoundCredentials() {
        return new BiometricAuthenticatorConfig(BiometricManager.Authenticators.BIOMETRIC_STRONG, keyAuthStrong(), true, true);
    }

    /**
     * Ensures the config is legal for {@code BiometricPrompt.authenticate(promptInfo, cryptoObject)}.
     * Crypto-based auth only supports Class 3 (Strong) biometrics.
     * <p>
     * Note: {@code BIOMETRIC_STRONG} (0x0F) is a subset of {@code BIOMETRIC_WEAK} (0xFF), so "allows
     * weak" must compare equality to {@code BIOMETRIC_WEAK}, not a non-zero mask.
     */
    static BiometricAuthenticatorConfig ensureCryptoCompatible(BiometricAuthenticatorConfig config) {
        if (config == null) {
            return defaultForCryptoBoundCredentials();
        }
        boolean allowsWeak =
            (config.promptAuthenticators & BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.Authenticators.BIOMETRIC_WEAK;
        boolean hasDeviceCredential = (config.promptAuthenticators & BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0;
        boolean hasStrong =
            (config.promptAuthenticators & BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.Authenticators.BIOMETRIC_STRONG;
        if (allowsWeak || hasDeviceCredential || !hasStrong) {
            return defaultForCryptoBoundCredentials();
        }
        return config;
    }

    private static int keyAuthStrong() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return KEY_AUTH_BIOMETRIC_STRONG;
        }
        return 0;
    }

    private static int keyAuthAny() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return KEY_AUTH_BIOMETRIC_STRONG | KEY_AUTH_BIOMETRIC_WEAK;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return KEY_AUTH_BIOMETRIC_STRONG;
        }
        return 0;
    }

    private static int keyAuthDeviceCredential() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return KEY_AUTH_DEVICE_CREDENTIAL;
        }
        return 0;
    }

    /**
     * Translates this class's {@code KEY_AUTH_*} flags into the {@code KeyProperties.AUTH_*} values
     * that {@code KeyGenParameterSpec.Builder#setUserAuthenticationParameters} expects. The two sets
     * use different bit values ({@code KEY_AUTH_BIOMETRIC_STRONG} is 1, but
     * {@code KeyProperties.AUTH_DEVICE_CREDENTIAL} is 1 and {@code AUTH_BIOMETRIC_STRONG} is 2), so
     * passing the plugin flags straight through mints a device-credential key for a
     * biometric-only request — a biometric auth token then cannot satisfy it, and Android exempts
     * such keys from biometric-enrollment invalidation.
     * <p>
     * Keystore keys can only be bound to Class 3 (Strong) biometrics, so {@code KEY_AUTH_BIOMETRIC_WEAK}
     * collapses to {@code AUTH_BIOMETRIC_STRONG}.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    static int toKeyPropertiesAuthTypes(int pluginTypes) {
        int types = 0;
        if ((pluginTypes & (KEY_AUTH_BIOMETRIC_STRONG | KEY_AUTH_BIOMETRIC_WEAK)) != 0) {
            types |= KeyProperties.AUTH_BIOMETRIC_STRONG;
        }
        if ((pluginTypes & KEY_AUTH_DEVICE_CREDENTIAL) != 0) {
            types |= KeyProperties.AUTH_DEVICE_CREDENTIAL;
        }
        return types == 0 ? KeyProperties.AUTH_BIOMETRIC_STRONG : types;
    }
}
