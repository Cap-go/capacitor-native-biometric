package ee.forgr.biometric;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NativeBiometricBiometryTypeTest {

    private static final int NONE = 0;
    private static final int FINGERPRINT = 3;
    private static final int FACE_AUTHENTICATION = 4;
    private static final int IRIS_AUTHENTICATION = 5;
    private static final int MULTIPLE = 6;
    private static final int DEVICE_CREDENTIAL = 7;

    private static final boolean PREFER_MULTIPLE = true;
    private static final boolean PREFER_FINGERPRINT = false;

    @Test
    public void fingerprintOnlyHardware_enrolled_returnsFingerprint() {
        assertEquals(FINGERPRINT, NativeBiometric.resolveBiometryType(true, false, false, true, false, true, PREFER_FINGERPRINT));
    }

    @Test
    public void fingerprintOnlyHardware_enrolled_ignoresPreferMultiple() {
        assertEquals(FINGERPRINT, NativeBiometric.resolveBiometryType(true, false, false, true, false, true, PREFER_MULTIPLE));
    }

    @Test
    public void faceOnlyHardware_enrolled_returnsFace() {
        assertEquals(FACE_AUTHENTICATION, NativeBiometric.resolveBiometryType(false, true, false, false, true, true, PREFER_FINGERPRINT));
    }

    /** Issue #49: a face sensor advertised but never enrolled must not report MULTIPLE. */
    @Test
    public void dualHardware_onlyFingerprintEnrolled_returnsFingerprint() {
        assertEquals(FINGERPRINT, NativeBiometric.resolveBiometryType(true, true, false, true, false, true, PREFER_FINGERPRINT));
    }

    /** Issue #110: apps covering several biometrics can opt into MULTIPLE for the ambiguous case. */
    @Test
    public void dualHardware_fingerprintEnrolled_preferMultiple_returnsMultiple() {
        assertEquals(MULTIPLE, NativeBiometric.resolveBiometryType(true, true, false, true, false, true, PREFER_MULTIPLE));
    }

    @Test
    public void fingerprintAndIrisHardware_fingerprintEnrolled_preferMultiple_returnsMultiple() {
        assertEquals(MULTIPLE, NativeBiometric.resolveBiometryType(true, false, true, true, false, true, PREFER_MULTIPLE));
    }

    /** Face enrollment is unambiguous once no fingerprint is enrolled. */
    @Test
    public void dualHardware_onlyFaceEnrolled_returnsFace() {
        assertEquals(FACE_AUTHENTICATION, NativeBiometric.resolveBiometryType(true, true, false, false, true, true, PREFER_FINGERPRINT));
    }

    @Test
    public void dualHardware_onlyFaceEnrolled_ignoresPreferMultiple() {
        assertEquals(FACE_AUTHENTICATION, NativeBiometric.resolveBiometryType(true, true, false, false, true, true, PREFER_MULTIPLE));
    }

    @Test
    public void fingerprintAndIrisHardware_onlyIrisEnrolled_returnsIris() {
        assertEquals(IRIS_AUTHENTICATION, NativeBiometric.resolveBiometryType(true, false, true, false, true, true, PREFER_FINGERPRINT));
    }

    /** Nothing enrolled: keep the previous hardware-feature fallback. */
    @Test
    public void dualHardware_nothingEnrolled_fallsBackToHardwareCount() {
        assertEquals(MULTIPLE, NativeBiometric.resolveBiometryType(true, true, false, false, false, true, PREFER_FINGERPRINT));
    }

    @Test
    public void singleHardware_nothingEnrolled_fallsBackToThatType() {
        assertEquals(FINGERPRINT, NativeBiometric.resolveBiometryType(true, false, false, false, false, true, PREFER_FINGERPRINT));
    }

    @Test
    public void noBiometricHardware_withCredentials_returnsDeviceCredential() {
        assertEquals(DEVICE_CREDENTIAL, NativeBiometric.resolveBiometryType(false, false, false, false, false, true, PREFER_FINGERPRINT));
    }

    @Test
    public void noBiometricHardware_withoutCredentials_returnsNone() {
        assertEquals(NONE, NativeBiometric.resolveBiometryType(false, false, false, false, false, false, PREFER_FINGERPRINT));
    }
}
