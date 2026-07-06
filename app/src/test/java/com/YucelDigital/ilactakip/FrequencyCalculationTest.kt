package com.YucelDigital.ilactakip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

import java.util.Calendar
import java.util.Locale

/**
 * add_medicine içindeki saf Java iş mantığı testleri.
 * Frekans hesaplama, zaman string üretimi, Custom Day genişletme
 * gibi Android'e bağlı olmayan mantıkları test eder.
 */
class FrequencyCalculationTest {

    // ══════════════════════════════════════════════════════════════
    //  Standart Mod: Frekans → Zaman String Üretimi
    //  (add_medicine.saveStandardAlarm mantığı)
    // ══════════════════════════════════════════════════════════════

    /**
     * Frekansa göre zaman string'i üretir.
     * add_medicine.saveStandardAlarm() ile aynı mantık.
     */
    private fun generateTimeString(hour: Int, minute: Int, freqCount: Int): String {
        val timesBuilder = StringBuilder()
        for (i in 0 until freqCount) {
            val currentHour = (hour + (i * (24 / freqCount))) % 24
            if (i > 0) timesBuilder.append(", ")
            timesBuilder.append(String.format(Locale.getDefault(), "%02d:%02d", currentHour, minute))
        }
        return timesBuilder.toString()
    }

    @Test
    fun frequency_gunde1Defa_singleTime() {
        val result = generateTimeString(8, 0, 1)
        assertEquals("08:00", result)
    }

    @Test
    fun frequency_12SaatteAir_twoTimes() {
        val result = generateTimeString(8, 0, 2)
        assertEquals("08:00, 20:00", result)
    }

    @Test
    fun frequency_8SaatteAir_threeTimes() {
        val result = generateTimeString(8, 0, 3)
        assertEquals("08:00, 16:00, 00:00", result)
    }

    @Test
    fun frequency_6SaatteAir_fourTimes() {
        val result = generateTimeString(8, 30, 4)
        assertEquals("08:30, 14:30, 20:30, 02:30", result)
    }

    @Test
    fun frequency_midnight_wrapsCorrectly() {
        // 22:00 başlangıç, her 6 saatte bir
        val result = generateTimeString(22, 0, 4)
        assertEquals("22:00, 04:00, 10:00, 16:00", result)
    }

    @Test
    fun frequency_withMinutes_preservesMinutes() {
        val result = generateTimeString(9, 45, 2)
        assertEquals("09:45, 21:45", result)
    }

    // ══════════════════════════════════════════════════════════════
    //  Güne Özel Mod: Saat Genişletme
    //  (add_medicine.saveCustomDayAlarm mantığı)
    // ══════════════════════════════════════════════════════════════

    /**
     * Güne özel modda her gün için frekansa göre saat listesi üretir.
     * add_medicine.saveCustomDayAlarm() ile aynı mantık.
     */
    private fun expandCustomDayTimes(baseTimes: HashMap<Int, String>, freqCount: Int): HashMap<Int, String> {
        val expanded = HashMap<Int, String>()
        for ((day, baseTime) in baseTimes) {
            val parts = baseTime.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            val dayTimesBuilder = StringBuilder()
            for (i in 0 until freqCount) {
                val currentHour = (hour + (i * (24 / freqCount))) % 24
                if (i > 0) dayTimesBuilder.append(", ")
                dayTimesBuilder.append(String.format(Locale.getDefault(), "%02d:%02d", currentHour, minute))
            }
            expanded[day] = dayTimesBuilder.toString()
        }
        return expanded
    }

    @Test
    fun customDay_singleDay_singleFreq_noExpansion() {
        val base = HashMap<Int, String>()
        base[Calendar.MONDAY] = "09:00"

        val result = expandCustomDayTimes(base, 1)
        assertEquals("09:00", result[Calendar.MONDAY])
    }

    @Test
    fun customDay_singleDay_doubleFreq_expandsTo12h() {
        val base = HashMap<Int, String>()
        base[Calendar.FRIDAY] = "08:00"

        val result = expandCustomDayTimes(base, 2)
        assertEquals("08:00, 20:00", result[Calendar.FRIDAY])
    }

    @Test
    fun customDay_multipleDays_eachExpandedIndependently() {
        val base = HashMap<Int, String>()
        base[Calendar.MONDAY] = "09:00"
        base[Calendar.WEDNESDAY] = "10:00"

        val result = expandCustomDayTimes(base, 3)
        assertEquals("09:00, 17:00, 01:00", result[Calendar.MONDAY])
        assertEquals("10:00, 18:00, 02:00", result[Calendar.WEDNESDAY])
    }

    @Test
    fun customDay_emptyBase_returnsEmpty() {
        val base = HashMap<Int, String>()
        val result = expandCustomDayTimes(base, 2)
        assertTrue(result.isEmpty())
    }

    @Test
    fun customDay_preservesDayKeys() {
        val base = HashMap<Int, String>()
        base[Calendar.TUESDAY] = "10:00"
        base[Calendar.SATURDAY] = "14:00"

        val result = expandCustomDayTimes(base, 1)
        assertTrue(result.containsKey(Calendar.TUESDAY))
        assertTrue(result.containsKey(Calendar.SATURDAY))
        assertFalse(result.containsKey(Calendar.MONDAY))
    }

    // ══════════════════════════════════════════════════════════════
    //  Frekans Metin Çözümleme
    //  (add_medicine.resolveFrequencyText mantığı)
    // ══════════════════════════════════════════════════════════════

    /**
     * Medicine nesnesinden frekans metnini çözer.
     */
    private fun resolveFrequencyText(intervalDays: Int, timeString: String?): String {
        var currentFreq = "Günde 1 defa"

        if (intervalDays > 1) {
            currentFreq = if (intervalDays == 7) "Haftada 1 Defa" else "$intervalDays Günde Bir"
        } else if (timeString != null && timeString.contains(",")) {
            when (timeString.split(",").size) {
                2 -> currentFreq = "12 Saatte Bir"
                3 -> currentFreq = "8 Saatte Bir"
                4 -> currentFreq = "6 Saatte Bir"
            }
        }
        return currentFreq
    }

    @Test
    fun resolveFreq_defaultInterval_gunde1() {
        assertEquals("Günde 1 defa", resolveFrequencyText(1, "08:00"))
    }

    @Test
    fun resolveFreq_interval2_2GundeBir() {
        assertEquals("2 Günde Bir", resolveFrequencyText(2, "08:00"))
    }

    @Test
    fun resolveFreq_interval7_haftada1() {
        assertEquals("Haftada 1 Defa", resolveFrequencyText(7, "08:00"))
    }

    @Test
    fun resolveFreq_twoTimes_12SaatteAir() {
        assertEquals("12 Saatte Bir", resolveFrequencyText(1, "08:00, 20:00"))
    }

    @Test
    fun resolveFreq_threeTimes_8SaatteAir() {
        assertEquals("8 Saatte Bir", resolveFrequencyText(1, "08:00, 16:00, 00:00"))
    }

    @Test
    fun resolveFreq_fourTimes_6SaatteAir() {
        assertEquals("6 Saatte Bir", resolveFrequencyText(1, "06:00, 12:00, 18:00, 00:00"))
    }

    @Test
    fun resolveFreq_intervalTakesPrecedenceOverTimeCount() {
        // interval > 1 olduğunda time string'deki virgüller önemsiz
        assertEquals("3 Günde Bir", resolveFrequencyText(3, "08:00, 20:00"))
    }

    @Test
    fun resolveFreq_nullTimeString_gunde1() {
        assertEquals("Günde 1 defa", resolveFrequencyText(1, null))
    }

    // ══════════════════════════════════════════════════════════════
    //  Süre Geçmişliği Kontrolü (Expiry Check)
    //  (AlarmAdapter.getView mantığı)
    // ══════════════════════════════════════════════════════════════

    @Test
    fun expiry_endDateZero_neverExpires() {
        val endDate = 0L
        var isExpired = false

        if (endDate != 0L) {
            val endCal = Calendar.getInstance()
            endCal.timeInMillis = endDate
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            if (System.currentTimeMillis() > endCal.timeInMillis) isExpired = true
        }

        assertFalse(isExpired)
    }

    @Test
    fun expiry_pastEndDate_isExpired() {
        // 1 Ocak 2020 — kesinlikle geçmiş
        val past = Calendar.getInstance()
        past.set(2020, Calendar.JANUARY, 1, 0, 0, 0)
        val endDate = past.timeInMillis

        var isExpired = false
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = endDate
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        if (System.currentTimeMillis() > endCal.timeInMillis) isExpired = true

        assertTrue(isExpired)
    }

    @Test
    fun expiry_futureEndDate_notExpired() {
        // 1 Ocak 2099 — kesinlikle gelecek
        val future = Calendar.getInstance()
        future.set(2099, Calendar.JANUARY, 1, 0, 0, 0)
        val endDate = future.timeInMillis

        var isExpired = false
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = endDate
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        if (System.currentTimeMillis() > endCal.timeInMillis) isExpired = true

        assertFalse(isExpired)
    }

    // ══════════════════════════════════════════════════════════════
    //  Interval Gün Kontrolü
    //  (AlarmReceiver.onReceive mantığı)
    // ══════════════════════════════════════════════════════════════

    @Test
    fun intervalCheck_day0_isAlarmDay() {
        val diffDays = 0L
        val intervalDays = 3
        assertEquals(0L, diffDays % intervalDays)
    }

    @Test
    fun intervalCheck_day3_isAlarmDay() {
        val diffDays = 3L
        val intervalDays = 3
        assertEquals(0L, diffDays % intervalDays)
    }

    @Test
    fun intervalCheck_day1_isNotAlarmDay() {
        val diffDays = 1L
        val intervalDays = 3
        assertNotEquals(0L, diffDays % intervalDays)
    }

    @Test
    fun intervalCheck_day6_isAlarmDay_for3DayInterval() {
        val diffDays = 6L
        val intervalDays = 3
        assertEquals(0L, diffDays % intervalDays)
    }

    @Test
    fun intervalCheck_everyDay_alwaysAlarmDay() {
        val intervalDays = 1
        for (day in 0 until 30) {
            assertEquals("Day $day should be alarm day", 0, day % intervalDays)
        }
    }
}
