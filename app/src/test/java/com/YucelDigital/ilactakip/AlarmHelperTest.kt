package com.YucelDigital.ilactakip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AlarmHelper.safeId() için testler — Math.abs(hashCode()) yerine
 * (hashCode() &amp; 0x7fffffff) kullanan alarm ID üretiminin doğruluğunu kontrol eder.
 */
class AlarmHelperTest {

    @Test
    fun safeId_neverNegative_forKnownMinValueHashCode() {
        // "polygenelubricants".hashCode() == Integer.MIN_VALUE (-2147483648) — bilinen
        // bir Java hashCode() örneği. Math.abs(Integer.MIN_VALUE) overflow yüzünden hâlâ
        // negatif kalır; safeId bunu doğru şekilde pozitife çevirmeli.
        val key = "polygenelubricants"
        assertEquals(Int.MIN_VALUE, key.hashCode())

        val id = AlarmHelper.safeId(key)

        assertTrue("safeId negatif dönmemeli: $id", id >= 0)
    }

    @Test
    fun safeId_isDeterministic() {
        val key = "Parol08:00"
        assertEquals(AlarmHelper.safeId(key), AlarmHelper.safeId(key))
    }

    @Test
    fun safeId_alwaysNonNegative_forVariousInputs() {
        val samples = arrayOf(
            "", "a", "Parol", "Parol08:00", "Aspirin_day2_09:00",
            "İlaç", "çşğüöı", "polygenelubricants_reset",
        )
        for (s in samples) {
            val id = AlarmHelper.safeId(s)
            assertTrue("Negatif ID üretildi: \"$s\" -> $id", id >= 0)
        }
    }

    @Test
    fun safeId_differentInputs_generallyDifferentIds() {
        assertNotEquals(AlarmHelper.safeId("Parol08:00"), AlarmHelper.safeId("Aspirin08:00"))
        assertNotEquals(AlarmHelper.safeId("Parol08:00"), AlarmHelper.safeId("Parol14:00"))
    }

    @Test
    fun safeId_matchesRawHashCodeMaskedToPositiveRange() {
        val key = "Parol_day2_09:00"
        val expected = key.hashCode() and 0x7fffffff
        assertEquals(expected, AlarmHelper.safeId(key))
    }
}
