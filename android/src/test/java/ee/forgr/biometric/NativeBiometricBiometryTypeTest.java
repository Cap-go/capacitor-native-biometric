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

    @Test
    public void fingerprintOnlyHardware_enrolled_returnsFingerprint() {
        assertEquals(FINGERPRINT, NativeBiometric.resolveBiometryType(true, false, false, true, false, false, true));
    }

    @Test
    public void faceOnlyHardware_enrolled_returnsFace() {
        assertEquals(FACE_AUTHENTICATION, NativeBiometric.resolveBiometryType(false, true, false, false, true, false, true));
    }

    /** Issue #49: face hardware advertised but never enrolled must not report MULTIPLE. */
    @Test
    public void dualHardware_onlyFingerprintEnrolled_returnsFingerprint() {
        assertEquals(FINGERPRINT, NativeBiometric.resolveBiometryType(true, true, false, true, false, false, true));
    }

    /** Issue #110: both modalities actually enrolled must report MULTIPLE. */
    @Test
    public void dualHardware_fingerprintAndFaceEnrolled_returnsMultiple() {
        assertEquals(MULTIPLE, NativeBiometric.resolveBiometryType(true, true, false, true, true, false, true));
    }

    @Test
    public void dualHardware_fingerprintAndIrisEnrolled_returnsMultiple() {
        assertEquals(MULTIPLE, NativeBiometric.resolveBiometryType(true, false, true, true, false, true, true));
    }

    @Test
    public void dualHardware_onlyFaceEnrolled_returnsFace() {
        assertEquals(FACE_AUTHENTICATION, NativeBiometric.resolveBiometryType(true, true, false, false, true, false, true));
    }

    @Test
    public void irisOnlyEnrolled_returnsIris() {
        assertEquals(IRIS_AUTHENTICATION, NativeBiometric.resolveBiometryType(true, false, true, false, false, true, true));
    }

    /** Enrollment state unknown (reflection unavailable): fall back to hardware features. */
    @Test
    public void dualHardware_noEnrollmentDetected_fallsBackToHardwareCount() {
        assertEquals(MULTIPLE, NativeBiometric.resolveBiometryType(true, true, false, false, false, false, true));
    }

    @Test
    public void singleHardware_noEnrollmentDetected_fallsBackToThatType() {
        assertEquals(FINGERPRINT, NativeBiometric.resolveBiometryType(true, false, false, false, false, false, true));
    }

    @Test
    public void noBiometricHardware_withCredentials_returnsDeviceCredential() {
        assertEquals(DEVICE_CREDENTIAL, NativeBiometric.resolveBiometryType(false, false, false, false, false, false, true));
    }

    @Test
    public void noBiometricHardware_withoutCredentials_returnsNone() {
        assertEquals(NONE, NativeBiometric.resolveBiometryType(false, false, false, false, false, false, false));
    }
}
