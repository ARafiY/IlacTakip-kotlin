package com.YucelDigital.ilactakip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import java.util.Calendar

/**
 * Medicine model sınıfı için unit testler.
 * Constructor varsayılan değerleri, getter/setter doğruluğu,
 * güne özel zamanlama ve Serializable uyumluluğunu test eder.
 */
class MedicineTest {

    private lateinit var medicine: Medicine

    @Before
    fun setUp() {
        medicine = Medicine("Parol", "08:00", "1 Oca - 31 Oca", "Tok karnına")
    }

    // ══════════════════════════════════════════════════════════════
    //  Constructor Varsayılan Değerleri
    // ══════════════════════════════════════════════════════════════

    @Test
    fun constructor_setsNameCorrectly() {
        assertEquals("Parol", medicine.name)
    }

    @Test
    fun constructor_setsTimeCorrectly() {
        assertEquals("08:00", medicine.time)
    }

    @Test
    fun constructor_setsDateRangeCorrectly() {
        assertEquals("1 Oca - 31 Oca", medicine.dateRange)
    }

    @Test
    fun constructor_setsNoteCorrectly() {
        assertEquals("Tok karnına", medicine.note)
    }

    @Test
    fun constructor_defaultsActiveToTrue() {
        assertTrue(medicine.isActive)
    }

    @Test
    fun constructor_defaultsTakenToFalse() {
        assertFalse(medicine.isTaken)
    }

    @Test
    fun constructor_defaultsStartDateToZero() {
        assertEquals(0L, medicine.startDate)
    }

    @Test
    fun constructor_defaultsEndDateToZero() {
        assertEquals(0L, medicine.endDate)
    }

    @Test
    fun constructor_defaultsIntervalDaysToOne() {
        assertEquals(1, medicine.intervalDays)
    }

    @Test
    fun constructor_defaultsSoundUriToNull() {
        assertNull(medicine.soundUri)
    }

    @Test
    fun constructor_defaultsUseCustomDaysToFalse() {
        assertFalse(medicine.isUseCustomDays)
    }

    @Test
    fun constructor_defaultsCustomDayTimesToNull() {
        assertNull(medicine.customDayTimes)
    }

    // ══════════════════════════════════════════════════════════════
    //  Setter / Getter Doğruluğu
    // ══════════════════════════════════════════════════════════════

    @Test
    fun setActive_false_isActiveReturnsFalse() {
        medicine.isActive = false
        assertFalse(medicine.isActive)
    }

    @Test
    fun setActive_true_isActiveReturnsTrue() {
        medicine.isActive = false
        medicine.isActive = true
        assertTrue(medicine.isActive)
    }

    @Test
    fun setTaken_true_isTakenReturnsTrue() {
        medicine.isTaken = true
        assertTrue(medicine.isTaken)
    }

    @Test
    fun setTaken_false_isTakenReturnsFalse() {
        medicine.isTaken = true
        medicine.isTaken = false
        assertFalse(medicine.isTaken)
    }

    @Test
    fun setTime_updatesTime() {
        medicine.time = "14:00"
        assertEquals("14:00", medicine.time)
    }

    @Test
    fun setStartDate_updatesStartDate() {
        val date = System.currentTimeMillis()
        medicine.startDate = date
        assertEquals(date, medicine.startDate)
    }

    @Test
    fun setEndDate_updatesEndDate() {
        val date = System.currentTimeMillis()
        medicine.endDate = date
        assertEquals(date, medicine.endDate)
    }

    @Test
    fun setIntervalDays_updatesIntervalDays() {
        medicine.intervalDays = 3
        assertEquals(3, medicine.intervalDays)
    }

    @Test
    fun setSoundUri_updatesSoundUri() {
        medicine.soundUri = "content://media/alarm/1"
        assertEquals("content://media/alarm/1", medicine.soundUri)
    }

    @Test
    fun setSoundUri_null_returnsNull() {
        medicine.soundUri = "content://media/alarm/1"
        medicine.soundUri = null
        assertNull(medicine.soundUri)
    }

    // ══════════════════════════════════════════════════════════════
    //  Güne Özel Zamanlama
    // ══════════════════════════════════════════════════════════════

    @Test
    fun setUseCustomDays_true_isUseCustomDaysReturnsTrue() {
        medicine.isUseCustomDays = true
        assertTrue(medicine.isUseCustomDays)
    }

    @Test
    fun setCustomDayTimes_setsCorrectlyAndReturns() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "09:00"
        dayTimes[Calendar.FRIDAY] = "14:00"

        medicine.customDayTimes = dayTimes

        assertNotNull(medicine.customDayTimes)
        assertEquals(2, medicine.customDayTimes!!.size)
        assertEquals("09:00", medicine.customDayTimes!![Calendar.MONDAY])
        assertEquals("14:00", medicine.customDayTimes!![Calendar.FRIDAY])
    }

    @Test
    fun customDayTimes_doesNotContainUnsetDay() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "09:00"
        medicine.customDayTimes = dayTimes

        assertFalse(medicine.customDayTimes!!.containsKey(Calendar.WEDNESDAY))
    }

    @Test
    fun setCustomDayTimes_null_returnsNull() {
        medicine.customDayTimes = HashMap()
        medicine.customDayTimes = null
        assertNull(medicine.customDayTimes)
    }

    @Test
    fun customDayTimes_allDaysOfWeek() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "07:00"
        dayTimes[Calendar.TUESDAY] = "08:00"
        dayTimes[Calendar.WEDNESDAY] = "09:00"
        dayTimes[Calendar.THURSDAY] = "10:00"
        dayTimes[Calendar.FRIDAY] = "11:00"
        dayTimes[Calendar.SATURDAY] = "12:00"
        dayTimes[Calendar.SUNDAY] = "13:00"

        medicine.customDayTimes = dayTimes
        assertEquals(7, medicine.customDayTimes!!.size)
    }

    @Test
    fun customDayTimes_multipleTimesPerDay_commaFormat() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "08:00, 14:00, 20:00"
        medicine.customDayTimes = dayTimes

        val times = medicine.customDayTimes!![Calendar.MONDAY]
        assertNotNull(times)
        assertTrue(times!!.contains(","))
        assertEquals(3, times.split(",").size)
    }

    // ══════════════════════════════════════════════════════════════
    //  Çoklu Saat Formatı (Standart Mod)
    // ══════════════════════════════════════════════════════════════

    @Test
    fun time_withMultipleTimesComma_splitCorrectly() {
        val multi = Medicine("Aspirin", "08:00, 14:00, 20:00", "", "")
        val times = multi.time!!.split(", ")
        assertEquals(3, times.size)
        assertEquals("08:00", times[0])
        assertEquals("14:00", times[1])
        assertEquals("20:00", times[2])
    }

    @Test
    fun time_singleTime_noCommaSplit() {
        val times = medicine.time!!.split(", ")
        assertEquals(1, times.size)
        assertEquals("08:00", times[0])
    }

    // ══════════════════════════════════════════════════════════════
    //  Boş / Null Değerler
    // ══════════════════════════════════════════════════════════════

    @Test
    fun constructor_emptyStrings_noException() {
        val m = Medicine("", "", "", "")
        assertEquals("", m.name)
        assertEquals("", m.time)
        assertEquals("", m.dateRange)
        assertEquals("", m.note)
    }

    @Test
    fun constructor_nullNote_isNull() {
        val m = Medicine("Test", "10:00", null, null)
        assertNull(m.note)
        assertNull(m.dateRange)
    }

    // ══════════════════════════════════════════════════════════════
    //  Serializable Uyumluluğu
    // ══════════════════════════════════════════════════════════════

    @Test
    fun medicine_implementsSerializable() {
        assertTrue(medicine is java.io.Serializable)
    }

    @Test
    fun medicine_serialization_roundTrip() {
        // Tüm alanları doldur
        medicine.isActive = false
        medicine.isTaken = true
        medicine.startDate = 1000L
        medicine.endDate = 2000L
        medicine.intervalDays = 3
        medicine.soundUri = "content://alarm/1"
        medicine.isUseCustomDays = true
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "09:00"
        medicine.customDayTimes = dayTimes

        // Serialize
        val bos = java.io.ByteArrayOutputStream()
        val oos = java.io.ObjectOutputStream(bos)
        oos.writeObject(medicine)
        oos.close()

        // Deserialize
        val bis = java.io.ByteArrayInputStream(bos.toByteArray())
        val ois = java.io.ObjectInputStream(bis)
        val deserialized = ois.readObject() as Medicine
        ois.close()

        // Doğrulama
        assertEquals(medicine.name, deserialized.name)
        assertEquals(medicine.time, deserialized.time)
        assertEquals(medicine.dateRange, deserialized.dateRange)
        assertEquals(medicine.note, deserialized.note)
        assertEquals(medicine.isActive, deserialized.isActive)
        assertEquals(medicine.isTaken, deserialized.isTaken)
        assertEquals(medicine.startDate, deserialized.startDate)
        assertEquals(medicine.endDate, deserialized.endDate)
        assertEquals(medicine.intervalDays, deserialized.intervalDays)
        assertEquals(medicine.soundUri, deserialized.soundUri)
        assertEquals(medicine.isUseCustomDays, deserialized.isUseCustomDays)
        assertNotNull(deserialized.customDayTimes)
        assertEquals("09:00", deserialized.customDayTimes!![Calendar.MONDAY])
    }

    // ══════════════════════════════════════════════════════════════
    //  Alarm ID Hesaplama Tutarlılığı
    // ══════════════════════════════════════════════════════════════

    @Test
    fun alarmId_standardMode_isConsistent() {
        // AlarmId = (name + time).hashCode()
        val name = "Parol"
        val time = "08:00"
        val id1 = (name + time).hashCode()
        val id2 = (name + time).hashCode()
        assertEquals(id1, id2)
    }

    @Test
    fun alarmId_customDay_isConsistent() {
        // AlarmId = (name + "_day" + calDay + "_" + time).hashCode()
        val name = "Parol"
        val calDay = Calendar.MONDAY
        val time = "09:00"
        val id1 = (name + "_day" + calDay + "_" + time).hashCode()
        val id2 = (name + "_day" + calDay + "_" + time).hashCode()
        assertEquals(id1, id2)
    }

    @Test
    fun alarmId_differentDays_areDifferent() {
        val name = "Parol"
        val time = "09:00"
        val idMon = (name + "_day" + Calendar.MONDAY + "_" + time).hashCode()
        val idFri = (name + "_day" + Calendar.FRIDAY + "_" + time).hashCode()
        assertNotEquals(idMon, idFri)
    }

    @Test
    fun alarmId_differentNames_areDifferent() {
        val time = "08:00"
        val id1 = ("Parol" + time).hashCode()
        val id2 = ("Aspirin" + time).hashCode()
        assertNotEquals(id1, id2)
    }

    @Test
    fun alarmId_differentTimes_areDifferent() {
        val name = "Parol"
        val id1 = (name + "08:00").hashCode()
        val id2 = (name + "14:00").hashCode()
        assertNotEquals(id1, id2)
    }

    @Test
    fun resetAlarmId_isDifferentFromMainAlarmId() {
        val name = "Parol"
        val time = "08:00"
        val mainId = (name + time).hashCode()
        val resetId = (name + time + "_reset").hashCode()
        assertNotEquals(mainId, resetId)
    }

    // ══════════════════════════════════════════════════════════════
    //  copy() — Compose'da yeniden çizim için kullanılan klon
    // ══════════════════════════════════════════════════════════════

    @Test
    fun copy_producesDistinctInstance() {
        val copy = medicine.copy()
        assertNotSame(medicine, copy)
    }

    @Test
    fun copy_copiesAllScalarFields() {
        medicine.isActive = false
        medicine.isTaken = true
        medicine.startDate = 1000L
        medicine.endDate = 2000L
        medicine.intervalDays = 3
        medicine.soundUri = "content://alarm/1"
        medicine.isUseCustomDays = true

        val copy = medicine.copy()

        assertEquals(medicine.name, copy.name)
        assertEquals(medicine.time, copy.time)
        assertEquals(medicine.dateRange, copy.dateRange)
        assertEquals(medicine.note, copy.note)
        assertEquals(medicine.isActive, copy.isActive)
        assertEquals(medicine.isTaken, copy.isTaken)
        assertEquals(medicine.startDate, copy.startDate)
        assertEquals(medicine.endDate, copy.endDate)
        assertEquals(medicine.intervalDays, copy.intervalDays)
        assertEquals(medicine.soundUri, copy.soundUri)
        assertEquals(medicine.isUseCustomDays, copy.isUseCustomDays)
    }

    @Test
    fun copy_customDayTimes_isDeepCopied() {
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "09:00"
        medicine.customDayTimes = dayTimes

        val copy = medicine.copy()

        // İçerik aynı ama harita farklı bir örnek (birini değiştirmek diğerini etkilemesin)
        assertNotSame(medicine.customDayTimes, copy.customDayTimes)
        assertEquals("09:00", copy.customDayTimes!![Calendar.MONDAY])

        copy.customDayTimes!![Calendar.TUESDAY] = "10:00"
        assertFalse(medicine.customDayTimes!!.containsKey(Calendar.TUESDAY))
    }

    @Test
    fun copy_nullCustomDayTimes_staysNull() {
        assertNull(medicine.customDayTimes)
        assertNull(medicine.copy().customDayTimes)
    }

    @Test
    fun copy_thenMutate_doesNotAffectOriginal() {
        val original = Medicine("Parol", "08:00", "", "")
        original.isTaken = false

        val updated = original.copy().apply { isTaken = true }

        assertTrue(updated.isTaken)
        assertFalse("Klonu değiştirmek orijinali etkilememeli", original.isTaken)
    }
}
