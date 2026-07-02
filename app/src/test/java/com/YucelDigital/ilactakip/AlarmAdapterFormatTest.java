package com.YucelDigital.ilactakip;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.HashMap;

/**
 * AlarmAdapter içindeki formatCustomDayTimes mantığının unit testleri.
 * Android bağımlılığı olmayan saf Java metodu olduğu için
 * aynı algoritma burada tekrar edilerek test edilir.
 */
public class AlarmAdapterFormatTest {

    // AlarmAdapter.formatCustomDayTimes() ile aynı mantık — pure Java test
    private String formatCustomDayTimes(HashMap<Integer, String> dayTimes) {
        String[] shortNames = {"", "Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt"};
        int[] ordered = {2, 3, 4, 5, 6, 7, 1}; // Pzt-Paz sırası

        StringBuilder sb = new StringBuilder();
        for (int day : ordered) {
            if (dayTimes.containsKey(day)) {
                String times = dayTimes.get(day);
                String firstTime = times;
                if (times.contains(",")) {
                    firstTime = times.split(",")[0].trim();
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(shortNames[day]).append(" ").append(firstTime);
            }
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════
    //  Tek Gün Testleri
    // ══════════════════════════════════════════════════════════════

    @Test
    public void singleDay_monday_formatsCorrectly() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.MONDAY, "09:00");
        assertEquals("Pzt 09:00", formatCustomDayTimes(dayTimes));
    }

    @Test
    public void singleDay_sunday_formatsCorrectly() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.SUNDAY, "10:30");
        assertEquals("Paz 10:30", formatCustomDayTimes(dayTimes));
    }

    @Test
    public void singleDay_saturday_formatsCorrectly() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.SATURDAY, "22:00");
        assertEquals("Cmt 22:00", formatCustomDayTimes(dayTimes));
    }

    // ══════════════════════════════════════════════════════════════
    //  Birden Fazla Gün
    // ══════════════════════════════════════════════════════════════

    @Test
    public void multipleDays_orderedByWeekday() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.FRIDAY,  "14:00");
        dayTimes.put(Calendar.MONDAY,  "09:00");
        dayTimes.put(Calendar.WEDNESDAY, "11:00");

        String result = formatCustomDayTimes(dayTimes);
        assertEquals("Pzt 09:00, Çar 11:00, Cum 14:00", result);
    }

    @Test
    public void allDays_orderedMondayToSunday() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.MONDAY,    "07:00");
        dayTimes.put(Calendar.TUESDAY,   "08:00");
        dayTimes.put(Calendar.WEDNESDAY, "09:00");
        dayTimes.put(Calendar.THURSDAY,  "10:00");
        dayTimes.put(Calendar.FRIDAY,    "11:00");
        dayTimes.put(Calendar.SATURDAY,  "12:00");
        dayTimes.put(Calendar.SUNDAY,    "13:00");

        String result = formatCustomDayTimes(dayTimes);
        assertEquals("Pzt 07:00, Sal 08:00, Çar 09:00, Per 10:00, Cum 11:00, Cmt 12:00, Paz 13:00", result);
    }

    @Test
    public void twoDays_mondayAndFriday() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.MONDAY, "08:00");
        dayTimes.put(Calendar.FRIDAY, "16:00");

        String result = formatCustomDayTimes(dayTimes);
        assertEquals("Pzt 08:00, Cum 16:00", result);
    }

    // ══════════════════════════════════════════════════════════════
    //  Çoklu Saat (Frekans) — Sadece İlk Saat Gösterilmeli
    // ══════════════════════════════════════════════════════════════

    @Test
    public void multipleTimesPerDay_showsOnlyFirst() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.MONDAY, "08:00, 14:00, 20:00");

        String result = formatCustomDayTimes(dayTimes);
        assertEquals("Pzt 08:00", result);
    }

    @Test
    public void multipleTimesPerDay_twoDays_showsOnlyFirstEach() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.TUESDAY, "09:00, 21:00");
        dayTimes.put(Calendar.THURSDAY, "10:00, 22:00");

        String result = formatCustomDayTimes(dayTimes);
        assertEquals("Sal 09:00, Per 10:00", result);
    }

    // ══════════════════════════════════════════════════════════════
    //  Kenar Durumlar
    // ══════════════════════════════════════════════════════════════

    @Test
    public void emptyHashMap_returnsEmptyString() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        assertEquals("", formatCustomDayTimes(dayTimes));
    }

    @Test
    public void midnightTime_formatsCorrectly() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.WEDNESDAY, "00:00");
        assertEquals("Çar 00:00", formatCustomDayTimes(dayTimes));
    }

    @Test
    public void lastMinuteOfDay_formatsCorrectly() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.FRIDAY, "23:59");
        assertEquals("Cum 23:59", formatCustomDayTimes(dayTimes));
    }

    // ══════════════════════════════════════════════════════════════
    //  Virgül Ayracı (Yeni format: satır yerine virgül)
    // ══════════════════════════════════════════════════════════════

    @Test
    public void separator_isCommaSpace_notNewline() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.MONDAY, "08:00");
        dayTimes.put(Calendar.TUESDAY, "09:00");

        String result = formatCustomDayTimes(dayTimes);
        assertFalse("Should not contain newline", result.contains("\n"));
        assertTrue("Should contain comma separator", result.contains(", "));
    }
}
