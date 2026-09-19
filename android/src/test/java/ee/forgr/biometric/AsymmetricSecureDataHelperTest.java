package ee.forgr.biometric;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AsymmetricSecureDataHelperTest {

    @Test
    public void secureFormatKey_usesExpectedSuffix() {
        assertEquals("secure_data_mykey_format", AsymmetricSecureDataHelper.secureFormatKey("data_mykey"));
    }

    @Test
    public void asymmetricAlias_usesExpectedPrefix() {
        assertEquals("NativeBiometricAsymmetric_data_mykey", AsymmetricSecureDataHelper.asymmetricAlias("data_mykey"));
    }
}
