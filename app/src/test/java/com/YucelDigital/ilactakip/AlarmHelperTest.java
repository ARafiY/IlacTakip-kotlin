package com.YucelDigital.ilactakip;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * AlarmHelper.safeId() için testler — Math.abs(hashCode()) yerine
 * (hashCode() &amp; 0x7fffffff) kullanan alarm ID üretiminin doğruluğunu kontrol eder.
 */
public class AlarmHelperTest {

    @Test
    public void safeId_neverNegative_forKnownMinValueHashCode() {
        // "polygenelubricants".hashCode() == Integer.MIN_VALUE (-2147483648) — bilinen
        // bir Java hashCode() örneği. Math.abs(Integer.MIN_VALUE) overflow yüzünden hâlâ
        // negatif kalır; safeId bunu doğru şekilde pozitife çevirmeli.
        String key = "polygenelubricants";
        assertEquals(Integer.MIN_VALUE, key.hashCode());

        int id = AlarmHelper.safeId(key);

        assertTrue("safeId negatif dönmemeli: " + id, id >= 0);
    }

    @Test
    public void safeId_isDeterministic() {
        String key = "Parol08:00";
        assertEquals(AlarmHelper.safeId(key), AlarmHelper.safeId(key));
    }

    @Test
    public void safeId_alwaysNonNegative_forVariousInputs() {
        String[] samples = {
                "", "a", "Parol", "Parol08:00", "Aspirin_day2_09:00",
                "İlaç", "çşğüöı", "polygenelubricants_reset"
        };
        for (String s : samples) {
            int id = AlarmHelper.safeId(s);
            assertTrue("Negatif ID üretildi: \"" + s + "\" -> " + id, id >= 0);
        }
    }

    @Test
    public void safeId_differentInputs_generallyDifferentIds() {
        assertNotEquals(AlarmHelper.safeId("Parol08:00"), AlarmHelper.safeId("Aspirin08:00"));
        assertNotEquals(AlarmHelper.safeId("Parol08:00"), AlarmHelper.safeId("Parol14:00"));
    }

    @Test
    public void safeId_matchesRawHashCodeMaskedToPositiveRange() {
        String key = "Parol_day2_09:00";
        int expected = key.hashCode() & 0x7fffffff;
        assertEquals(expected, AlarmHelper.safeId(key));
    }
}
