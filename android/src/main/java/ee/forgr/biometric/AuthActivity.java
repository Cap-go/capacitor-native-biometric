package ee.forgr.biometric;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import ee.forgr.biometric.capacitornativebiometric.R;
import java.util.concurrent.Executor;

public class AuthActivity extends AppCompatActivity {

  private Executor executor;
  private int maxAttempts;
  private int counter = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_auth_acitivy);

    maxAttempts = getIntent().getIntExtra("maxAttempts", 1);

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

    BiometricPrompt.PromptInfo.Builder builder =
      new BiometricPrompt.PromptInfo.Builder()
        .setTitle(
          getIntent().hasExtra("title")
            ? getIntent().getStringExtra("title")
            : "Authenticate"
        )
        .setSubtitle(
          getIntent().hasExtra("subtitle")
            ? getIntent().getStringExtra("subtitle")
            : null
        )
        .setDescription(
          getIntent().hasExtra("description")
            ? getIntent().getStringExtra("description")
            : null
        );

    boolean useFallback = getIntent().getBooleanExtra("useFallback", false);
    int[] allowedTypes = getIntent().getIntArrayExtra("allowedBiometryTypes");

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;
      if (useFallback) {
        authenticators |= BiometricManager.Authenticators.DEVICE_CREDENTIAL;
      }
      if (allowedTypes != null) {
        // Filter authenticators based on allowed types
        authenticators = getAllowedAuthenticators(allowedTypes);
      }
      builder.setAllowedAuthenticators(authenticators);
    } else {
      if (useFallback) {
        builder.setDeviceCredentialAllowed(true);
      } else {
        builder.setNegativeButtonText(
          getIntent().hasExtra("negativeButtonText")
            ? getIntent().getStringExtra("negativeButtonText")
            : "Cancel"
        );
      }
    }

    BiometricPrompt.PromptInfo promptInfo = builder.build();

    BiometricPrompt biometricPrompt = new BiometricPrompt(
      this,
      executor,
      new BiometricPrompt.AuthenticationCallback() {
        @Override
        public void onAuthenticationError(
          int errorCode,
          @NonNull CharSequence errString
        ) {
          super.onAuthenticationError(errorCode, errString);
          int pluginErrorCode = AuthActivity.convertToPluginErrorCode(
            errorCode
          );
          finishActivity("error", pluginErrorCode, errString.toString());
        }

        @Override
        public void onAuthenticationSucceeded(
          @NonNull BiometricPrompt.AuthenticationResult result
        ) {
          super.onAuthenticationSucceeded(result);
          finishActivity("success");
        }

        @Override
        public void onAuthenticationFailed() {
          super.onAuthenticationFailed();
          counter++;
          // Don't finish activity here, let Android's ERROR_LOCKOUT trigger through onAuthenticationError
          // This ensures proper error propagation for too many attempts
          if (counter == maxAttempts) {
            // Android will automatically trigger ERROR_LOCKOUT after too many attempts
            // The error will be handled in onAuthenticationError with proper error code
            finishActivity(
              "failed",
              10,
              "Authentication failed. Too many attempts."
            );
          }
        }
      }
    );

    biometricPrompt.authenticate(promptInfo);
  }

  void finishActivity(String result) {
    finishActivity(result, null, null);
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
   * Error code flow:
   * - ERROR_LOCKOUT (7) -> USER_TEMPORARY_LOCKOUT (4)
   * - ERROR_LOCKOUT_PERMANENT (9) -> USER_LOCKOUT (2)
   * - AUTHENTICATION_RESULT_TYPE_BIOMETRIC (2) is not an error, ignored
   * @see <a href="https://developer.android.com/reference/androidx/biometric/BiometricPrompt#constants">...</a>
   * @return BiometricAuthError
   */
  public static int convertToPluginErrorCode(int errorCode) {
    switch (errorCode) {
      case BiometricPrompt.ERROR_HW_UNAVAILABLE:
      case BiometricPrompt.ERROR_HW_NOT_PRESENT:
        return 1;
      case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
        return 2;
      case BiometricPrompt.ERROR_NO_BIOMETRICS:
        return 3;
      case BiometricPrompt.ERROR_LOCKOUT:
        // ERROR_LOCKOUT (code 7) maps to temporary lockout (4)
        return 4;
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
      default:
        return 0;
    }
  }

  private int getAllowedAuthenticators(int[] allowedTypes) {
    int authenticators = 0;
    for (int type : allowedTypes) {
      switch (type) {
        case 3: // FINGERPRINT
          authenticators |= BiometricManager.Authenticators.BIOMETRIC_STRONG;
          break;
        case 4: // FACE_AUTHENTICATION
          authenticators |= BiometricManager.Authenticators.BIOMETRIC_STRONG;
          break;
        case 5: // IRIS_AUTHENTICATION
          authenticators |= BiometricManager.Authenticators.BIOMETRIC_STRONG;
          break;
      }
    }
    return authenticators > 0 ? authenticators : BiometricManager.Authenticators.BIOMETRIC_STRONG;
  }
}
