package ee.forgr.biometric;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

final class SecureCredentialPreferences {

    private static final String PREFERENCES_NAME = "NativeBiometricSharedPreferences";
    private static final String MIGRATED = "secureMetadata:migrated";
    private static final String VALIDITY_SUFFIX = "_validity";
    private static final String AUTH_SCHEME_SUFFIX = "_auth_scheme";
    private static final String ACCESS_CONTROL_SUFFIX = "_access_control";
    private static final String FORMAT_SUFFIX = "_format";

    private SecureCredentialPreferences() {}

    static synchronized SharedPreferences open(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (!preferences.getBoolean(MIGRATED, false)) {
            SharedPreferences.Editor editor = preferences.edit();
            for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
                String key = entry.getKey();
                // A legacy metadata key can also contain another alias's ciphertext.
                if (entry.getValue() instanceof Integer && isLegacyMetadata(key)) {
                    editor.putInt(metadataKey(key), (Integer) entry.getValue());
                    editor.remove(key);
                }
            }
            editor.putBoolean(MIGRATED, true).apply();
        }
        return preferences;
    }

    private static boolean isLegacyMetadata(String key) {
        return (
            key.startsWith("secure_") &&
            (key.endsWith(VALIDITY_SUFFIX) ||
                key.endsWith(AUTH_SCHEME_SUFFIX) ||
                key.endsWith(ACCESS_CONTROL_SUFFIX) ||
                key.endsWith(FORMAT_SUFFIX))
        );
    }

    private static String metadataKey(String legacyKey) {
        // Protected payloads start with secure_, data starts with data_, and legacy
        // usernames and passwords have fixed suffixes. This key cannot match any of them.
        return "secureMetadata:" + legacyKey + ":value";
    }

    static void removeMetadata(SharedPreferences.Editor editor, String alias) {
        editor.remove(validityKey(alias));
        editor.remove(authSchemeKey(alias));
        editor.remove(accessControlKey(alias));
        editor.remove(formatKey(alias));
    }

    static String validityKey(String alias) {
        return metadataKey("secure_" + alias + VALIDITY_SUFFIX);
    }

    static String authSchemeKey(String alias) {
        return metadataKey("secure_" + alias + AUTH_SCHEME_SUFFIX);
    }

    static String accessControlKey(String alias) {
        return metadataKey("secure_" + alias + ACCESS_CONTROL_SUFFIX);
    }

    static String formatKey(String alias) {
        return metadataKey("secure_" + alias + FORMAT_SUFFIX);
    }
}
