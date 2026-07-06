package com.YucelDigital.ilactakip

import android.content.Context

import androidx.test.core.app.ApplicationProvider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import java.util.Calendar

/**
 * MedicineRepository.markAsTaken() / resetTakenStatus() için Robolectric testleri.
 *
 * Asıl regresyon: güne özel modda Medicine.time alanı sadece TEK bir günün saatini
 * tutuyor (bkz. AddMedicineActivity.saveCustomDayAlarm — displayTime, HashMap'ten
 * rastgele/ilk elemanın saati). Eski markAsTaken, isim + "getTime().contains(saat)"
 * ile eşleştirdiği için, tetiklenen alarmın saati o rastgele saklanan saatten farklı
 * olduğunda hiçbir şeyi güncellemiyordu. Fix isimle eşleştirmeye geçti.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35]) // Robolectric 4.14.1'in desteklediği en yüksek SDK (proje targetSdk 36)
class MedicineRepositoryRobolectricTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun markAsTaken_customDay_differentTimeThanStoredDisplayTime_stillMarksTaken() {
        val context = context()

        // Medicine.time = "09:00" (Pazartesi'nin saati, displayTime olarak saklanmış),
        // ama gerçek alarm Çarşamba 14:00 için tetikleniyor.
        val medicine = Medicine("Parol", "09:00", "", "")
        medicine.isUseCustomDays = true
        val dayTimes = HashMap<Int, String>()
        dayTimes[Calendar.MONDAY] = "09:00"
        dayTimes[Calendar.WEDNESDAY] = "14:00"
        medicine.customDayTimes = dayTimes

        val list = ArrayList<Medicine>()
        list.add(medicine)
        MedicineRepository.saveMedicineList(context, list)

        MedicineRepository.markAsTaken(context, "Parol", "14:00")

        val reloaded = MedicineRepository.loadMedicineList(context)
        assertEquals(1, reloaded.size)
        assertTrue(
            "Güne özel ilaçta, saklanan displayTime'dan farklı saatli doz " +
                "'alındı' olarak işaretlenmedi (regresyon)",
            reloaded[0].isTaken,
        )
    }

    @Test
    fun markAsTaken_standardMode_stillWorks() {
        val context = context()
        val medicine = Medicine("Aspirin", "08:00, 20:00", "", "")

        val list = ArrayList<Medicine>()
        list.add(medicine)
        MedicineRepository.saveMedicineList(context, list)

        MedicineRepository.markAsTaken(context, "Aspirin", "20:00")

        val reloaded = MedicineRepository.loadMedicineList(context)
        assertTrue(reloaded[0].isTaken)
    }

    @Test
    fun markAsTaken_unknownName_doesNothing() {
        val context = context()
        val medicine = Medicine("Aspirin", "08:00", "", "")
        val list = ArrayList<Medicine>()
        list.add(medicine)
        MedicineRepository.saveMedicineList(context, list)

        MedicineRepository.markAsTaken(context, "OlmayanIlac", "08:00")

        val reloaded = MedicineRepository.loadMedicineList(context)
        assertFalse(reloaded[0].isTaken)
    }

    @Test
    fun resetTakenStatus_clearsTakenFlagByName() {
        val context = context()
        val medicine = Medicine("Parol", "08:00", "", "")
        medicine.isTaken = true
        val list = ArrayList<Medicine>()
        list.add(medicine)
        MedicineRepository.saveMedicineList(context, list)

        MedicineRepository.resetTakenStatus(context, "Parol")

        val reloaded = MedicineRepository.loadMedicineList(context)
        assertFalse(reloaded[0].isTaken)
    }
}
