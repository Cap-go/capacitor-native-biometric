package ee.forgr.biometric;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AsymmetricSecureDataHelperTest {

    @Test
    public void asymmetricAlias_usesExpectedPrefix() {
        assertEquals("NativeBiometricAsymmetric_data_mykey", AsymmetricSecureDataHelper.asymmetricAlias("data_mykey"));
    }
}
