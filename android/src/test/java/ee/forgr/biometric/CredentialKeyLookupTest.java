package ee.forgr.biometric;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.security.KeyStore;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.Test;

public class CredentialKeyLookupTest {

    @Test
    public void missingAliasIsNotCreatedByRead() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null);

        assertNull(AuthActivity.findCredentialKey(keyStore, "test"));
        assertEquals(0, keyStore.size());
    }

    @Test
    public void secondReadKeepsKeyReplacedByAnInterveningWrite() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null);
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        SecretKey original = generator.generateKey();
        SecretKey replacement = generator.generateKey();
        KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(new char[0]);
        String alias = "NativeBiometricSecure_test";

        keyStore.setEntry(alias, new KeyStore.SecretKeyEntry(original), protection);
        assertArrayEquals(original.getEncoded(), AuthActivity.findCredentialKey(keyStore, "test").getEncoded());

        keyStore.setEntry(alias, new KeyStore.SecretKeyEntry(replacement), protection);
        assertArrayEquals(replacement.getEncoded(), AuthActivity.findCredentialKey(keyStore, "test").getEncoded());
        assertArrayEquals(replacement.getEncoded(), keyStore.getKey(alias, null).getEncoded());
        assertEquals(1, keyStore.size());
    }
}
