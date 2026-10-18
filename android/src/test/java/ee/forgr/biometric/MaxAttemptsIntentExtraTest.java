package ee.forgr.biometric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MaxAttemptsIntentExtraTest {

    @Test
    public void putIfPresent_setsExtraWhenValueProvided() {
        Intent intent = new Intent();
        MaxAttemptsIntentExtra.putIfPresent(intent, 3);
        assertEquals(3, intent.getIntExtra(MaxAttemptsIntentExtra.KEY, -1));
    }

    @Test
    public void putIfPresent_leavesExtraAbsentWhenNull() {
        Intent intent = new Intent();
        MaxAttemptsIntentExtra.putIfPresent(intent, null);
        assertFalse(intent.hasExtra(MaxAttemptsIntentExtra.KEY));
    }

    @Test
    public void key_matchesAuthActivityExtraName() {
        assertEquals("maxAttempts", MaxAttemptsIntentExtra.KEY);
    }
}
