package ee.forgr.biometric;

import android.content.Intent;
import com.getcapacitor.PluginCall;

/**
 * Forwards optional {@code maxAttempts} from a plugin call to {@link AuthActivity}.
 */
final class MaxAttemptsIntentExtra {

    static final String KEY = "maxAttempts";

    private MaxAttemptsIntentExtra() {}

    static void putFromPluginCall(PluginCall call, Intent intent) {
        putIfPresent(intent, call.getInt(KEY));
    }

    static void putIfPresent(Intent intent, Integer maxAttempts) {
        if (maxAttempts != null) {
            intent.putExtra(KEY, maxAttempts);
        }
    }
}
