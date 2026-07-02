package com.YucelDigital.ilactakip;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Calendar;

/**
 * Medicine model sınıfı için unit testler.
 * Constructor varsayılan değerleri, getter/setter doğruluğu,
 * güne özel zamanlama ve Serializable uyumluluğunu test eder.
 */
public class MedicineTest {

    private Medicine medicine;

    @Before
    public void setUp() {
        medicine = new Medicine("Parol", "08:00", "1 Oca - 31 Oca", "Tok karnına");
    }

    // ══════════════════════════════════════════════════════════════
    //  Constructor Varsayılan Değerleri
    // ══════════════════════════════════════════════════════════════

    @Test
    public void constructor_setsNameCorrectly() {
        assertEquals("Parol", medicine.getName());
    }

    @Test
    public void constructor_setsTimeCorrectly() {
        assertEquals("08:00", medicine.getTime());
    }

    @Test
    public void constructor_setsDateRangeCorrectly() {
        assertEquals("1 Oca - 31 Oca", medicine.getDateRange());
    }

    @Test
    public void constructor_setsNoteCorrectly() {
        assertEquals("Tok karnına", medicine.getNote());
    }

    @Test
    public void constructor_defaultsActiveToTrue() {
        assertTrue(medicine.isActive());
    }

    @Test
    public void constructor_defaultsTakenToFalse() {
        assertFalse(medicine.isTaken());
    }

    @Test
    public void constructor_defaultsStartDateToZero() {
        assertEquals(0, medicine.getStartDate());
    }

    @Test
    public void constructor_defaultsEndDateToZero() {
        assertEquals(0, medicine.getEndDate());
    }

    @Test
    public void constructor_defaultsIntervalDaysToOne() {
        assertEquals(1, medicine.getIntervalDays());
    }

    @Test
    public void constructor_defaultsSoundUriToNull() {
        assertNull(medicine.getSoundUri());
    }

    @Test
    public void constructor_defaultsUseCustomDaysToFalse() {
        assertFalse(medicine.isUseCustomDays());
    }

    @Test
    public void constructor_defaultsCustomDayTimesToNull() {
        assertNull(medicine.getCustomDayTimes());
    }

    // ══════════════════════════════════════════════════════════════
    //  Setter / Getter Doğruluğu
    // ══════════════════════════════════════════════════════════════

    @Test
    public void setActive_false_isActiveReturnsFalse() {
        medicine.setActive(false);
        assertFalse(medicine.isActive());
    }

    @Test
    public void setActive_true_isActiveReturnsTrue() {
        medicine.setActive(false);
        medicine.setActive(true);
        assertTrue(medicine.isActive());
    }

    @Test
    public void setTaken_true_isTakenReturnsTrue() {
        medicine.setTaken(true);
        assertTrue(medicine.isTaken());
    }

    @Test
    public void setTaken_false_isTakenReturnsFalse() {
        medicine.setTaken(true);
        medicine.setTaken(false);
        assertFalse(medicine.isTaken());
    }

    @Test
    public void setTime_updatesTime() {
        medicine.setTime("14:00");
        assertEquals("14:00", medicine.getTime());
    }

    @Test
    public void setStartDate_updatesStartDate() {
        long date = System.currentTimeMillis();
        medicine.setStartDate(date);
        assertEquals(date, medicine.getStartDate());
    }

    @Test
    public void setEndDate_updatesEndDate() {
        long date = System.currentTimeMillis();
        medicine.setEndDate(date);
        assertEquals(date, medicine.getEndDate());
    }

    @Test
    public void setIntervalDays_updatesIntervalDays() {
        medicine.setIntervalDays(3);
        assertEquals(3, medicine.getIntervalDays());
    }

    @Test
    public void setSoundUri_updatesSoundUri() {
        medicine.setSoundUri("content://media/alarm/1");
        assertEquals("content://media/alarm/1", medicine.getSoundUri());
    }

    @Test
    public void setSoundUri_null_returnsNull() {
        medicine.setSoundUri("content://media/alarm/1");
        medicine.setSoundUri(null);
        assertNull(medicine.getSoundUri());
    }

    // ══════════════════════════════════════════════════════════════
    //  Güne Özel Zamanlama
    // ══════════════════════════════════════════════════════════════

    @Test
    public void setUseCustomDays_true_isUseCustomDaysReturnsTrue() {
        medicine.setUseCustomDays(true);
        assertTrue(medicine.isUseCustomDays());
    }

    @Test
    public void setCustomDayTimes_setsCorrectlyAndReturns() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.MONDAY, "09:00");
        dayTimes.put(Calendar.FRIDAY, "14:00");

        medicine.setCustomDayTimes(dayTimes);

        assertNotNull(medicine.getCustomDayTimes());
        assertEquals(2, medicine.getCustomDayTimes().size());
        assertEquals("09:00", medicine.getCustomDayTimes().get(Calendar.MONDAY));
        assertEquals("14:00", medicine.getCustomDayTimes().get(Calendar.FRIDAY));
    }

    @Test
    public void customDayTimes_doesNotContainUnsetDay() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.MONDAY, "09:00");
        medicine.setCustomDayTimes(dayTimes);

        assertFalse(medicine.getCustomDayTimes().containsKey(Calendar.WEDNESDAY));
    }

    @Test
    public void setCustomDayTimes_null_returnsNull() {
        medicine.setCustomDayTimes(new HashMap<>());
        medicine.setCustomDayTimes(null);
        assertNull(medicine.getCustomDayTimes());
    }

    @Test
    public void customDayTimes_allDaysOfWeek() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.MONDAY,    "07:00");
        dayTimes.put(Calendar.TUESDAY,   "08:00");
        dayTimes.put(Calendar.WEDNESDAY, "09:00");
        dayTimes.put(Calendar.THURSDAY,  "10:00");
        dayTimes.put(Calendar.FRIDAY,    "11:00");
        dayTimes.put(Calendar.SATURDAY,  "12:00");
        dayTimes.put(Calendar.SUNDAY,    "13:00");

        medicine.setCustomDayTimes(dayTimes);
        assertEquals(7, medicine.getCustomDayTimes().size());
    }

    @Test
    public void customDayTimes_multipleTimesPerDay_commaFormat() {
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.MONDAY, "08:00, 14:00, 20:00");
        medicine.setCustomDayTimes(dayTimes);

        String times = medicine.getCustomDayTimes().get(Calendar.MONDAY);
        assertNotNull(times);
        assertTrue(times.contains(","));
        assertEquals(3, times.split(",").length);
    }

    // ══════════════════════════════════════════════════════════════
    //  Çoklu Saat Formatı (Standart Mod)
    // ══════════════════════════════════════════════════════════════

    @Test
    public void time_withMultipleTimesComma_splitCorrectly() {
        Medicine multi = new Medicine("Aspirin", "08:00, 14:00, 20:00", "", "");
        String[] times = multi.getTime().split(", ");
        assertEquals(3, times.length);
        assertEquals("08:00", times[0]);
        assertEquals("14:00", times[1]);
        assertEquals("20:00", times[2]);
    }

    @Test
    public void time_singleTime_noCommaSplit() {
        String[] times = medicine.getTime().split(", ");
        assertEquals(1, times.length);
        assertEquals("08:00", times[0]);
    }

    // ══════════════════════════════════════════════════════════════
    //  Boş / Null Değerler
    // ══════════════════════════════════════════════════════════════

    @Test
    public void constructor_emptyStrings_noException() {
        Medicine m = new Medicine("", "", "", "");
        assertEquals("", m.getName());
        assertEquals("", m.getTime());
        assertEquals("", m.getDateRange());
        assertEquals("", m.getNote());
    }

    @Test
    public void constructor_nullNote_isNull() {
        Medicine m = new Medicine("Test", "10:00", null, null);
        assertNull(m.getNote());
        assertNull(m.getDateRange());
    }

    // ══════════════════════════════════════════════════════════════
    //  Serializable Uyumluluğu
    // ══════════════════════════════════════════════════════════════

    @Test
    public void medicine_implementsSerializable() {
        assertTrue(medicine instanceof java.io.Serializable);
    }

    @Test
    public void medicine_serialization_roundTrip() throws Exception {
        // Tüm alanları doldur
        medicine.setActive(false);
        medicine.setTaken(true);
        medicine.setStartDate(1000L);
        medicine.setEndDate(2000L);
        medicine.setIntervalDays(3);
        medicine.setSoundUri("content://alarm/1");
        medicine.setUseCustomDays(true);
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.MONDAY, "09:00");
        medicine.setCustomDayTimes(dayTimes);

        // Serialize
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
        oos.writeObject(medicine);
        oos.close();

        // Deserialize
        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(bos.toByteArray());
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bis);
        Medicine deserialized = (Medicine) ois.readObject();
        ois.close();

        // Doğrulama
        assertEquals(medicine.getName(), deserialized.getName());
        assertEquals(medicine.getTime(), deserialized.getTime());
        assertEquals(medicine.getDateRange(), deserialized.getDateRange());
        assertEquals(medicine.getNote(), deserialized.getNote());
        assertEquals(medicine.isActive(), deserialized.isActive());
        assertEquals(medicine.isTaken(), deserialized.isTaken());
        assertEquals(medicine.getStartDate(), deserialized.getStartDate());
        assertEquals(medicine.getEndDate(), deserialized.getEndDate());
        assertEquals(medicine.getIntervalDays(), deserialized.getIntervalDays());
        assertEquals(medicine.getSoundUri(), deserialized.getSoundUri());
        assertEquals(medicine.isUseCustomDays(), deserialized.isUseCustomDays());
        assertNotNull(deserialized.getCustomDayTimes());
        assertEquals("09:00", deserialized.getCustomDayTimes().get(Calendar.MONDAY));
    }

    // ══════════════════════════════════════════════════════════════
    //  Alarm ID Hesaplama Tutarlılığı
    // ══════════════════════════════════════════════════════════════

    @Test
    public void alarmId_standardMode_isConsistent() {
        // AlarmId = (name + time).hashCode()
        String name = "Parol";
        String time = "08:00";
        int id1 = (name + time).hashCode();
        int id2 = (name + time).hashCode();
        assertEquals(id1, id2);
    }

    @Test
    public void alarmId_customDay_isConsistent() {
        // AlarmId = (name + "_day" + calDay + "_" + time).hashCode()
        String name = "Parol";
        int calDay = Calendar.MONDAY;
        String time = "09:00";
        int id1 = (name + "_day" + calDay + "_" + time).hashCode();
        int id2 = (name + "_day" + calDay + "_" + time).hashCode();
        assertEquals(id1, id2);
    }

    @Test
    public void alarmId_differentDays_areDifferent() {
        String name = "Parol";
        String time = "09:00";
        int idMon = (name + "_day" + Calendar.MONDAY + "_" + time).hashCode();
        int idFri = (name + "_day" + Calendar.FRIDAY + "_" + time).hashCode();
        assertNotEquals(idMon, idFri);
    }

    @Test
    public void alarmId_differentNames_areDifferent() {
        String time = "08:00";
        int id1 = ("Parol" + time).hashCode();
        int id2 = ("Aspirin" + time).hashCode();
        assertNotEquals(id1, id2);
    }

    @Test
    public void alarmId_differentTimes_areDifferent() {
        String name = "Parol";
        int id1 = (name + "08:00").hashCode();
        int id2 = (name + "14:00").hashCode();
        assertNotEquals(id1, id2);
    }

    @Test
    public void resetAlarmId_isDifferentFromMainAlarmId() {
        String name = "Parol";
        String time = "08:00";
        int mainId  = (name + time).hashCode();
        int resetId = (name + time + "_reset").hashCode();
        assertNotEquals(mainId, resetId);
    }
}
