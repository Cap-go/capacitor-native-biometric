package ee.forgr.biometric;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import ee.forgr.biometric.capacitornativebiometric.R;
import java.util.Objects;
import java.util.concurrent.Executor;

public class AuthActivity extends AppCompatActivity {

    private BiometricPrompt biometricPrompt;
    private int maxAttempts;
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_acitivy);

        maxAttempts = getIntent().getIntExtra("maxAttempts", 1);

        Executor executor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            executor = this.getMainExecutor();
        } else {
            executor = new Executor() {
                @Override
                public void execute(Runnable command) {
                    new Handler().post(command);
                }
            };
        }

        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
            .setTitle(getIntent().hasExtra("title") ? Objects.requireNonNull(getIntent().getStringExtra("title")) : "Authenticate")
            .setSubtitle(getIntent().hasExtra("subtitle") ? getIntent().getStringExtra("subtitle") : null)
            .setDescription(getIntent().hasExtra("description") ? getIntent().getStringExtra("description") : null);

        boolean useFallback = getIntent().getBooleanExtra("useFallback", false);
        int requiredStrength = getIntent().getIntExtra("requiredStrength", -1);

        int leftAuthenticators;
        // AuthenticationStrength enum: NONE = 0, STRONG = 1, WEAK = 2
        // -1 means not specified, default to allowing both STRONG and WEAK
        if (requiredStrength == 2) {
            // WEAK: Weak biometrics OR PIN/password
            // CRITICAL: ONLY use BIOMETRIC_WEAK - NEVER include BIOMETRIC_STRONG
            // This prevents Android from trying fingerprints when only weak biometrics are requested
            leftAuthenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK;
            if (useFallback) {
                leftAuthenticators |= BiometricManager.Authenticators.DEVICE_CREDENTIAL;
            }
        } else if (requiredStrength == 1) {
            // STRONG: Only strong biometrics (fingerprints, Face ID on iOS)
            leftAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;
            if (useFallback) {
                leftAuthenticators |= BiometricManager.Authenticators.DEVICE_CREDENTIAL;
            }
        } else {
            // Default (-1 or invalid): allow both STRONG and WEAK biometrics
            leftAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK;
            if (useFallback) {
                leftAuthenticators |= BiometricManager.Authenticators.DEVICE_CREDENTIAL;
            }
        }
        
        builder.setAllowedAuthenticators(leftAuthenticators);

        // Set negative button text if DEVICE_CREDENTIAL is not allowed
        // When allowedAuthenticators includes DEVICE_CREDENTIAL, negative button text must not be set
        boolean deviceCredentialAllowed = (leftAuthenticators & BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0;
        if (!deviceCredentialAllowed) {
            String negativeText = getIntent().getStringExtra("negativeButtonText");
            builder.setNegativeButtonText(negativeText != null ? negativeText : "Cancel");
        }

        BiometricPrompt.PromptInfo promptInfo = builder.build();

        biometricPrompt = new BiometricPrompt(
            this,
            executor,
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    // Handle lockout cases explicitly
                    if (errorCode == BiometricPrompt.ERROR_LOCKOUT || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                        int pluginErrorCode = convertToPluginErrorCode(errorCode);
                        finishActivity("error", pluginErrorCode, errString.toString());
                        return;
                    }
                    int pluginErrorCode = convertToPluginErrorCode(errorCode);
                    finishActivity("error", pluginErrorCode, errString.toString());
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    finishActivity();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    counter++;
                    if (counter >= maxAttempts) {
                        biometricPrompt.cancelAuthentication();
                        // Use error code 4 for too many attempts to match iOS behavior
                        finishActivity("error", 4, "Too many failed attempts");
                    }
                }
            }
        );

        biometricPrompt.authenticate(promptInfo);
    }

    void finishActivity() {
        finishActivity("success", null, null);
    }

    void finishActivity(String result, Integer errorCode, String errorDetails) {
        Intent intent = new Intent();
        intent.putExtra("result", result);
        if (errorCode != null) {
            intent.putExtra("errorCode", String.valueOf(errorCode));
        }
        if (errorDetails != null) {
            intent.putExtra("errorDetails", errorDetails);
        }
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Convert Auth Error Codes to plugin expected Biometric Auth Errors (in README.md)
     * This way both iOS and Android return the same error codes for the same authentication failure reasons.
     * !!IMPORTANT!!: Whenever this is modified, check if similar function in iOS Plugin.swift needs to be modified as well
     * @see <a href="https://developer.android.com/reference/androidx/biometric/BiometricPrompt#constants">...</a>
     * @return BiometricAuthError
     */
    public static int convertToPluginErrorCode(int errorCode) {
        switch (errorCode) {
            case BiometricPrompt.ERROR_HW_UNAVAILABLE:
            case BiometricPrompt.ERROR_HW_NOT_PRESENT:
                return 1;
            case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                return 2; // Permanent lockout
            case BiometricPrompt.ERROR_NO_BIOMETRICS:
                return 3;
            case BiometricPrompt.ERROR_LOCKOUT:
                return 4; // Temporary lockout (too many attempts)
            // Authentication Failure (10) Handled by `onAuthenticationFailed`.
            // App Cancel (11), Invalid Context (12), and Not Interactive (13) are not valid error codes for Android.
            case BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL:
                return 14;
            case BiometricPrompt.ERROR_TIMEOUT:
            case BiometricPrompt.ERROR_CANCELED:
                return 15;
            case BiometricPrompt.ERROR_USER_CANCELED:
            case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                return 16;
            case BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC:
                return 0; // Success case, should not be handled here
            default:
                return 0;
        }
    }

}
