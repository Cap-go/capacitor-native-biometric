package ee.forgr.biometric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.security.keystore.KeyProperties;
import androidx.biometric.BiometricManager;
import org.junit.Test;

public class BiometricAuthenticatorConfigTest {

    @Test
    public void nullAllowedTypes_defaultsToAnyBiometric() {
        BiometricAuthenticatorConfig config = BiometricAuthenticatorConfig.fromAllowedTypes(null);

        assertEquals(BiometricAuthenticatorConfig.PROMPT_BIOMETRIC_ANY, config.promptAuthenticators);
        assertTrue(config.allowNegativeButton);
        assertTrue(config.requiresCryptoObject);
    }

    @Test
    public void faceAuthentication_allowsWeakAndStrongPrompt() {
        BiometricAuthenticatorConfig config = BiometricAuthenticatorConfig.fromAllowedTypes(new int[] { 4 });

        assertEquals(BiometricAuthenticatorConfig.PROMPT_BIOMETRIC_ANY, config.promptAuthenticators);
        assertTrue(config.allowNegativeButton);
        assertTrue(config.requiresCryptoObject);
    }

    @Test
    public void fingerprintOnly_usesStrongPromptOnly() {
        BiometricAuthenticatorConfig config = BiometricAuthenticatorConfig.fromAllowedTypes(new int[] { 3 });

        assertEquals(BiometricManager.Authenticators.BIOMETRIC_STRONG, config.promptAuthenticators);
        assertTrue(config.allowNegativeButton);
        assertTrue(config.requiresCryptoObject);
    }

    @Test
    public void deviceCredential_disablesNegativeButtonAndCryptoObject() {
        BiometricAuthenticatorConfig config = BiometricAuthenticatorConfig.fromAllowedTypes(new int[] { 7 });

        assertEquals(BiometricManager.Authenticators.DEVICE_CREDENTIAL, config.promptAuthenticators);
        assertFalse(config.allowNegativeButton);
        assertFalse(config.requiresCryptoObject);
    }

    @Test
    public void iosTouchIdWithFingerprint_keepsFingerprintStrongOnly() {
        BiometricAuthenticatorConfig config = BiometricAuthenticatorConfig.fromAllowedTypes(new int[] { 1, 3 });

        assertEquals(BiometricManager.Authenticators.BIOMETRIC_STRONG, config.promptAuthenticators);
    }

    @Test
    public void cryptoBoundCredentials_defaultUsesStrongOnly() {
        BiometricAuthenticatorConfig config = BiometricAuthenticatorConfig.defaultForCryptoBoundCredentials();

        assertEquals(BiometricManager.Authenticators.BIOMETRIC_STRONG, config.promptAuthenticators);
        assertTrue(config.allowNegativeButton);
        assertTrue(config.requiresCryptoObject);
        assertTrue(
            (config.promptAuthenticators & BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.Authenticators.BIOMETRIC_WEAK
        );
    }

    @Test
    public void ensureCryptoCompatible_upgradesWeakDefaultToStrong() {
        BiometricAuthenticatorConfig weakDefault = BiometricAuthenticatorConfig.fromAllowedTypes(null);
        BiometricAuthenticatorConfig config = BiometricAuthenticatorConfig.ensureCryptoCompatible(weakDefault);

        assertEquals(BiometricAuthenticatorConfig.PROMPT_BIOMETRIC_ANY, weakDefault.promptAuthenticators);
        assertEquals(BiometricManager.Authenticators.BIOMETRIC_STRONG, config.promptAuthenticators);
        assertTrue(
            (config.promptAuthenticators & BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.Authenticators.BIOMETRIC_WEAK
        );
    }

    @Test
    public void ensureCryptoCompatible_keepsFingerprintStrong() {
        BiometricAuthenticatorConfig strong = BiometricAuthenticatorConfig.fromAllowedTypes(new int[] { 3 });
        BiometricAuthenticatorConfig config = BiometricAuthenticatorConfig.ensureCryptoCompatible(strong);

        assertEquals(BiometricManager.Authenticators.BIOMETRIC_STRONG, config.promptAuthenticators);
        assertTrue(config.requiresCryptoObject);
    }

    @Test
    public void ensureCryptoCompatible_upgradesFaceAuthenticationToStrong() {
        BiometricAuthenticatorConfig face = BiometricAuthenticatorConfig.fromAllowedTypes(new int[] { 4 });
        BiometricAuthenticatorConfig config = BiometricAuthenticatorConfig.ensureCryptoCompatible(face);

        assertEquals(BiometricAuthenticatorConfig.PROMPT_BIOMETRIC_ANY, face.promptAuthenticators);
        assertEquals(BiometricManager.Authenticators.BIOMETRIC_STRONG, config.promptAuthenticators);
    }

    @Test
    public void keyAuthStrong_mapsToKeyPropertiesBiometricStrong() {
        // Plugin flag 1 is NOT KeyProperties.AUTH_DEVICE_CREDENTIAL (also 1).
        assertEquals(KeyProperties.AUTH_BIOMETRIC_STRONG, BiometricAuthenticatorConfig.toKeyPropertiesAuthTypes(1));
    }

    @Test
    public void keyAuthWeak_collapsesToBiometricStrong() {
        assertEquals(KeyProperties.AUTH_BIOMETRIC_STRONG, BiometricAuthenticatorConfig.toKeyPropertiesAuthTypes(2));
        assertEquals(KeyProperties.AUTH_BIOMETRIC_STRONG, BiometricAuthenticatorConfig.toKeyPropertiesAuthTypes(1 | 2));
    }

    @Test
    public void keyAuthDeviceCredential_mapsToKeyPropertiesDeviceCredential() {
        assertEquals(KeyProperties.AUTH_DEVICE_CREDENTIAL, BiometricAuthenticatorConfig.toKeyPropertiesAuthTypes(4));
        assertEquals(
            KeyProperties.AUTH_BIOMETRIC_STRONG | KeyProperties.AUTH_DEVICE_CREDENTIAL,
            BiometricAuthenticatorConfig.toKeyPropertiesAuthTypes(1 | 4)
        );
    }

    @Test
    public void keyAuthZero_fallsBackToBiometricStrong() {
        assertEquals(KeyProperties.AUTH_BIOMETRIC_STRONG, BiometricAuthenticatorConfig.toKeyPropertiesAuthTypes(0));
    }

    @Test
    public void keyAuthTypesSchemeVersion_isPositive() {
        assertTrue(BiometricAuthenticatorConfig.KEY_AUTH_TYPES_SCHEME_VERSION > 0);
    }
}
