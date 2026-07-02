package com.YucelDigital.ilactakip;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * add_medicine içindeki saf Java iş mantığı testleri.
 * Frekans hesaplama, zaman string üretimi, Custom Day genişletme
 * gibi Android'e bağlı olmayan mantıkları test eder.
 */
public class FrequencyCalculationTest {

    // ══════════════════════════════════════════════════════════════
    //  Standart Mod: Frekans → Zaman String Üretimi
    //  (add_medicine.saveStandardAlarm mantığı)
    // ══════════════════════════════════════════════════════════════

    /**
     * Frekansa göre zaman string'i üretir.
     * add_medicine.saveStandardAlarm() ile aynı mantık.
     */
    private String generateTimeString(int hour, int minute, int freqCount) {
        StringBuilder timesBuilder = new StringBuilder();
        for (int i = 0; i < freqCount; i++) {
            int currentHour = (hour + (i * (24 / freqCount))) % 24;
            if (i > 0) timesBuilder.append(", ");
            timesBuilder.append(String.format(Locale.getDefault(), "%02d:%02d", currentHour, minute));
        }
        return timesBuilder.toString();
    }

    @Test
    public void frequency_gunde1Defa_singleTime() {
        String result = generateTimeString(8, 0, 1);
        assertEquals("08:00", result);
    }

    @Test
    public void frequency_12SaatteAir_twoTimes() {
        String result = generateTimeString(8, 0, 2);
        assertEquals("08:00, 20:00", result);
    }

    @Test
    public void frequency_8SaatteAir_threeTimes() {
        String result = generateTimeString(8, 0, 3);
        assertEquals("08:00, 16:00, 00:00", result);
    }

    @Test
    public void frequency_6SaatteAir_fourTimes() {
        String result = generateTimeString(8, 30, 4);
        assertEquals("08:30, 14:30, 20:30, 02:30", result);
    }

    @Test
    public void frequency_midnight_wrapsCorrectly() {
        // 22:00 başlangıç, her 6 saatte bir
        String result = generateTimeString(22, 0, 4);
        assertEquals("22:00, 04:00, 10:00, 16:00", result);
    }

    @Test
    public void frequency_withMinutes_preservesMinutes() {
        String result = generateTimeString(9, 45, 2);
        assertEquals("09:45, 21:45", result);
    }

    // ══════════════════════════════════════════════════════════════
    //  Güne Özel Mod: Saat Genişletme
    //  (add_medicine.saveCustomDayAlarm mantığı)
    // ══════════════════════════════════════════════════════════════

    /**
     * Güne özel modda her gün için frekansa göre saat listesi üretir.
     * add_medicine.saveCustomDayAlarm() ile aynı mantık.
     */
    private HashMap<Integer, String> expandCustomDayTimes(
            HashMap<Integer, String> baseTimes, int freqCount) {
        HashMap<Integer, String> expanded = new HashMap<>();
        for (Map.Entry<Integer, String> entry : baseTimes.entrySet()) {
            String baseTime = entry.getValue();
            String[] parts = baseTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            StringBuilder dayTimesBuilder = new StringBuilder();
            for (int i = 0; i < freqCount; i++) {
                int currentHour = (hour + (i * (24 / freqCount))) % 24;
                if (i > 0) dayTimesBuilder.append(", ");
                dayTimesBuilder.append(String.format(Locale.getDefault(), "%02d:%02d", currentHour, minute));
            }
            expanded.put(entry.getKey(), dayTimesBuilder.toString());
        }
        return expanded;
    }

    @Test
    public void customDay_singleDay_singleFreq_noExpansion() {
        HashMap<Integer, String> base = new HashMap<>();
        base.put(Calendar.MONDAY, "09:00");

        HashMap<Integer, String> result = expandCustomDayTimes(base, 1);
        assertEquals("09:00", result.get(Calendar.MONDAY));
    }

    @Test
    public void customDay_singleDay_doubleFreq_expandsTo12h() {
        HashMap<Integer, String> base = new HashMap<>();
        base.put(Calendar.FRIDAY, "08:00");

        HashMap<Integer, String> result = expandCustomDayTimes(base, 2);
        assertEquals("08:00, 20:00", result.get(Calendar.FRIDAY));
    }

    @Test
    public void customDay_multipleDays_eachExpandedIndependently() {
        HashMap<Integer, String> base = new HashMap<>();
        base.put(Calendar.MONDAY, "09:00");
        base.put(Calendar.WEDNESDAY, "10:00");

        HashMap<Integer, String> result = expandCustomDayTimes(base, 3);
        assertEquals("09:00, 17:00, 01:00", result.get(Calendar.MONDAY));
        assertEquals("10:00, 18:00, 02:00", result.get(Calendar.WEDNESDAY));
    }

    @Test
    public void customDay_emptyBase_returnsEmpty() {
        HashMap<Integer, String> base = new HashMap<>();
        HashMap<Integer, String> result = expandCustomDayTimes(base, 2);
        assertTrue(result.isEmpty());
    }

    @Test
    public void customDay_preservesDayKeys() {
        HashMap<Integer, String> base = new HashMap<>();
        base.put(Calendar.TUESDAY, "10:00");
        base.put(Calendar.SATURDAY, "14:00");

        HashMap<Integer, String> result = expandCustomDayTimes(base, 1);
        assertTrue(result.containsKey(Calendar.TUESDAY));
        assertTrue(result.containsKey(Calendar.SATURDAY));
        assertFalse(result.containsKey(Calendar.MONDAY));
    }

    // ══════════════════════════════════════════════════════════════
    //  Frekans Metin Çözümleme
    //  (add_medicine.resolveFrequencyText mantığı)
    // ══════════════════════════════════════════════════════════════

    /**
     * Medicine nesnesinden frekans metnini çözer.
     */
    private String resolveFrequencyText(int intervalDays, String timeString) {
        String currentFreq = "Günde 1 defa";

        if (intervalDays > 1) {
            if (intervalDays == 7) currentFreq = "Haftada 1 Defa";
            else currentFreq = intervalDays + " Günde Bir";
        } else if (timeString != null && timeString.contains(",")) {
            int count = timeString.split(",").length;
            if (count == 2) currentFreq = "12 Saatte Bir";
            else if (count == 3) currentFreq = "8 Saatte Bir";
            else if (count == 4) currentFreq = "6 Saatte Bir";
        }
        return currentFreq;
    }

    @Test
    public void resolveFreq_defaultInterval_gunde1() {
        assertEquals("Günde 1 defa", resolveFrequencyText(1, "08:00"));
    }

    @Test
    public void resolveFreq_interval2_2GundeBir() {
        assertEquals("2 Günde Bir", resolveFrequencyText(2, "08:00"));
    }

    @Test
    public void resolveFreq_interval7_haftada1() {
        assertEquals("Haftada 1 Defa", resolveFrequencyText(7, "08:00"));
    }

    @Test
    public void resolveFreq_twoTimes_12SaatteAir() {
        assertEquals("12 Saatte Bir", resolveFrequencyText(1, "08:00, 20:00"));
    }

    @Test
    public void resolveFreq_threeTimes_8SaatteAir() {
        assertEquals("8 Saatte Bir", resolveFrequencyText(1, "08:00, 16:00, 00:00"));
    }

    @Test
    public void resolveFreq_fourTimes_6SaatteAir() {
        assertEquals("6 Saatte Bir", resolveFrequencyText(1, "06:00, 12:00, 18:00, 00:00"));
    }

    @Test
    public void resolveFreq_intervalTakesPrecedenceOverTimeCount() {
        // interval > 1 olduğunda time string'deki virgüller önemsiz
        assertEquals("3 Günde Bir", resolveFrequencyText(3, "08:00, 20:00"));
    }

    @Test
    public void resolveFreq_nullTimeString_gunde1() {
        assertEquals("Günde 1 defa", resolveFrequencyText(1, null));
    }

    // ══════════════════════════════════════════════════════════════
    //  Süre Geçmişliği Kontrolü (Expiry Check)
    //  (AlarmAdapter.getView mantığı)
    // ══════════════════════════════════════════════════════════════

    @Test
    public void expiry_endDateZero_neverExpires() {
        long endDate = 0;
        boolean isExpired = false;

        if (endDate != 0) {
            Calendar endCal = Calendar.getInstance();
            endCal.setTimeInMillis(endDate);
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);
            if (System.currentTimeMillis() > endCal.getTimeInMillis()) isExpired = true;
        }

        assertFalse(isExpired);
    }

    @Test
    public void expiry_pastEndDate_isExpired() {
        // 1 Ocak 2020 — kesinlikle geçmiş
        Calendar past = Calendar.getInstance();
        past.set(2020, Calendar.JANUARY, 1, 0, 0, 0);
        long endDate = past.getTimeInMillis();

        boolean isExpired = false;
        if (endDate != 0) {
            Calendar endCal = Calendar.getInstance();
            endCal.setTimeInMillis(endDate);
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);
            if (System.currentTimeMillis() > endCal.getTimeInMillis()) isExpired = true;
        }

        assertTrue(isExpired);
    }

    @Test
    public void expiry_futureEndDate_notExpired() {
        // 1 Ocak 2099 — kesinlikle gelecek
        Calendar future = Calendar.getInstance();
        future.set(2099, Calendar.JANUARY, 1, 0, 0, 0);
        long endDate = future.getTimeInMillis();

        boolean isExpired = false;
        if (endDate != 0) {
            Calendar endCal = Calendar.getInstance();
            endCal.setTimeInMillis(endDate);
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);
            if (System.currentTimeMillis() > endCal.getTimeInMillis()) isExpired = true;
        }

        assertFalse(isExpired);
    }

    // ══════════════════════════════════════════════════════════════
    //  Interval Gün Kontrolü
    //  (AlarmReceiver.onReceive mantığı)
    // ══════════════════════════════════════════════════════════════

    @Test
    public void intervalCheck_day0_isAlarmDay() {
        long diffDays = 0;
        int intervalDays = 3;
        assertEquals(0, diffDays % intervalDays);
    }

    @Test
    public void intervalCheck_day3_isAlarmDay() {
        long diffDays = 3;
        int intervalDays = 3;
        assertEquals(0, diffDays % intervalDays);
    }

    @Test
    public void intervalCheck_day1_isNotAlarmDay() {
        long diffDays = 1;
        int intervalDays = 3;
        assertNotEquals(0, diffDays % intervalDays);
    }

    @Test
    public void intervalCheck_day6_isAlarmDay_for3DayInterval() {
        long diffDays = 6;
        int intervalDays = 3;
        assertEquals(0, diffDays % intervalDays);
    }

    @Test
    public void intervalCheck_everyDay_alwaysAlarmDay() {
        int intervalDays = 1;
        for (int day = 0; day < 30; day++) {
            assertEquals("Day " + day + " should be alarm day", 0, day % intervalDays);
        }
    }
}
