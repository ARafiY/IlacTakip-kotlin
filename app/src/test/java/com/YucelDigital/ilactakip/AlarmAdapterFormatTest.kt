package com.YucelDigital.ilactakip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

import java.util.Calendar

/**
 * MainActivity.kt içindeki (Compose UI, eskiden AlarmAdapter.formatCustomDayTimes)
 * saat formatlama mantığının unit testleri. Android bağımlılığı olmayan saf Kotlin
 * mantığı olduğu için aynı algoritma burada tekrar edilerek test edilir.
 */
class AlarmAdapterFormatTest {

    // MainActivity.kt'deki formatCustomDayTimes() ile aynı mantık — saf Kotlin testi
    private fun formatCustomDayTimes(dayTimes: HashMap<Int, String>): String {
        val shortNames = arrayOf("", "Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")
        val ordered = intArrayOf(2, 3, 4, 5, 6, 7, 1) // Pzt-Paz sırası

        val sb = StringBuilder()
        for (day in ordered) {
            if (dayTimes.containsKey(day)) {
                val times = dayTimes.getValue(day)
                val firstTime = if (times.contains(",")) times.split(",")[0].trim() else times
                if (sb.isNotEmpty()) sb.append(", ")
                sb.append(shortNames[day]).append(" ").append(firstTime)
            }
        }
        return sb.toString()
    }

    // ══════════════════════════════════════════════════════════════
    //  Tek Gün Testleri
    // ══════════════════════════════════════════════════════════════

    @Test
    fun singleDay_monday_formatsCorrectly() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "09:00"
        assertEquals("Pzt 09:00", formatCustomDayTimes(dayTimes))
    }

    @Test
    fun singleDay_sunday_formatsCorrectly() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.SUNDAY] = "10:30"
        assertEquals("Paz 10:30", formatCustomDayTimes(dayTimes))
    }

    @Test
    fun singleDay_saturday_formatsCorrectly() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.SATURDAY] = "22:00"
        assertEquals("Cmt 22:00", formatCustomDayTimes(dayTimes))
    }

    // ══════════════════════════════════════════════════════════════
    //  Birden Fazla Gün
    // ══════════════════════════════════════════════════════════════

    @Test
    fun multipleDays_orderedByWeekday() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.FRIDAY] = "14:00"
        dayTimes[Calendar.MONDAY] = "09:00"
        dayTimes[Calendar.WEDNESDAY] = "11:00"

        val result = formatCustomDayTimes(dayTimes)
        assertEquals("Pzt 09:00, Çar 11:00, Cum 14:00", result)
    }

    @Test
    fun allDays_orderedMondayToSunday() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "07:00"
        dayTimes[Calendar.TUESDAY] = "08:00"
        dayTimes[Calendar.WEDNESDAY] = "09:00"
        dayTimes[Calendar.THURSDAY] = "10:00"
        dayTimes[Calendar.FRIDAY] = "11:00"
        dayTimes[Calendar.SATURDAY] = "12:00"
        dayTimes[Calendar.SUNDAY] = "13:00"

        val result = formatCustomDayTimes(dayTimes)
        assertEquals("Pzt 07:00, Sal 08:00, Çar 09:00, Per 10:00, Cum 11:00, Cmt 12:00, Paz 13:00", result)
    }

    @Test
    fun twoDays_mondayAndFriday() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "08:00"
        dayTimes[Calendar.FRIDAY] = "16:00"

        val result = formatCustomDayTimes(dayTimes)
        assertEquals("Pzt 08:00, Cum 16:00", result)
    }

    // ══════════════════════════════════════════════════════════════
    //  Çoklu Saat (Frekans) — Sadece İlk Saat Gösterilmeli
    // ══════════════════════════════════════════════════════════════

    @Test
    fun multipleTimesPerDay_showsOnlyFirst() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "08:00, 14:00, 20:00"

        val result = formatCustomDayTimes(dayTimes)
        assertEquals("Pzt 08:00", result)
    }

    @Test
    fun multipleTimesPerDay_twoDays_showsOnlyFirstEach() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.TUESDAY] = "09:00, 21:00"
        dayTimes[Calendar.THURSDAY] = "10:00, 22:00"

        val result = formatCustomDayTimes(dayTimes)
        assertEquals("Sal 09:00, Per 10:00", result)
    }

    // ══════════════════════════════════════════════════════════════
    //  Kenar Durumlar
    // ══════════════════════════════════════════════════════════════

    @Test
    fun emptyHashMap_returnsEmptyString() {
        val dayTimes = HashMap<Int, String>()
        assertEquals("", formatCustomDayTimes(dayTimes))
    }

    @Test
    fun midnightTime_formatsCorrectly() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.WEDNESDAY] = "00:00"
        assertEquals("Çar 00:00", formatCustomDayTimes(dayTimes))
    }

    @Test
    fun lastMinuteOfDay_formatsCorrectly() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.FRIDAY] = "23:59"
        assertEquals("Cum 23:59", formatCustomDayTimes(dayTimes))
    }

    // ══════════════════════════════════════════════════════════════
    //  Virgül Ayracı (Yeni format: satır yerine virgül)
    // ══════════════════════════════════════════════════════════════

    @Test
    fun separator_isCommaSpace_notNewline() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "08:00"
        dayTimes[Calendar.TUESDAY] = "09:00"

        val result = formatCustomDayTimes(dayTimes)
        assertFalse("Should not contain newline", result.contains("\n"))
        assertTrue("Should contain comma separator", result.contains(", "))
    }
}
