package ee.forgr.biometric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SecureCredentialPreferencesTest {

    @Test
    public void migrationPreservesLegacyPayloadAndDoesNotOverwriteOtherAliases() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences legacy = context.getSharedPreferences("NativeBiometricSharedPreferences", Context.MODE_PRIVATE);
        legacy
            .edit()
            .putString("secure_first", "first ciphertext")
            .putString("secure_first_validity", "second ciphertext")
            .putInt("secure_first_auth_scheme", 1)
            .putInt("secure_first_access_control", 2)
            .putInt("secure_data_existing_format", 1)
            .apply();

        SharedPreferences preferences = SecureCredentialPreferences.open(context);
        assertEquals("first ciphertext", preferences.getString("secure_first", null));
        assertEquals(1, preferences.getInt(SecureCredentialPreferences.authSchemeKey("first"), 0));
        assertEquals(2, preferences.getInt(SecureCredentialPreferences.accessControlKey("first"), 0));
        assertEquals(1, preferences.getInt(SecureCredentialPreferences.formatKey("data_existing"), 0));
        assertFalse(preferences.contains("secure_first_auth_scheme"));
        assertFalse(preferences.contains("secure_first_access_control"));
        assertFalse(preferences.contains("secure_data_existing_format"));

        preferences.edit().putInt(SecureCredentialPreferences.validityKey("first"), 5).apply();
        SharedPreferences.Editor editor = preferences.edit().remove("secure_first");
        SecureCredentialPreferences.removeMetadata(editor, "first");
        editor.apply();

        assertEquals("second ciphertext", preferences.getString("secure_first_validity", null));
        assertFalse(preferences.contains("secure_first"));
        assertFalse(preferences.contains(SecureCredentialPreferences.validityKey("first")));
        assertEquals(1, preferences.getInt(SecureCredentialPreferences.formatKey("data_existing"), 0));
    }
}
