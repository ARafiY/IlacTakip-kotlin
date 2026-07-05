package com.YucelDigital.ilactakip;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * MedicineRepository.markAsTaken() / resetTakenStatus() için Robolectric testleri.
 *
 * Asıl regresyon: güne özel modda Medicine.time alanı sadece TEK bir günün saatini
 * tutuyor (bkz. AddMedicineActivity.saveCustomDayAlarm — displayTime, HashMap'ten
 * rastgele/ilk elemanın saati). Eski markAsTaken, isim + "getTime().contains(saat)"
 * ile eşleştirdiği için, tetiklenen alarmın saati o rastgele saklanan saatten farklı
 * olduğunda hiçbir şeyi güncellemiyordu. Fix isimle eşleştirmeye geçti.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35) // Robolectric 4.14.1'in desteklediği en yüksek SDK (proje targetSdk 36)
public class MedicineRepositoryRobolectricTest {

    private Context context() {
        return ApplicationProvider.getApplicationContext();
    }

    @Test
    public void markAsTaken_customDay_differentTimeThanStoredDisplayTime_stillMarksTaken() {
        Context context = context();

        // Medicine.time = "09:00" (Pazartesi'nin saati, displayTime olarak saklanmış),
        // ama gerçek alarm Çarşamba 14:00 için tetikleniyor.
        Medicine medicine = new Medicine("Parol", "09:00", "", "");
        medicine.setUseCustomDays(true);
        HashMap<Integer, String> dayTimes = new HashMap<>();
        dayTimes.put(Calendar.MONDAY, "09:00");
        dayTimes.put(Calendar.WEDNESDAY, "14:00");
        medicine.setCustomDayTimes(dayTimes);

        List<Medicine> list = new ArrayList<>();
        list.add(medicine);
        MedicineRepository.saveMedicineList(context, list);

        MedicineRepository.markAsTaken(context, "Parol", "14:00");

        List<Medicine> reloaded = MedicineRepository.loadMedicineList(context);
        assertEquals(1, reloaded.size());
        assertTrue("Güne özel ilaçta, saklanan displayTime'dan farklı saatli doz "
                        + "'alındı' olarak işaretlenmedi (regresyon)",
                reloaded.get(0).isTaken());
    }

    @Test
    public void markAsTaken_standardMode_stillWorks() {
        Context context = context();
        Medicine medicine = new Medicine("Aspirin", "08:00, 20:00", "", "");

        List<Medicine> list = new ArrayList<>();
        list.add(medicine);
        MedicineRepository.saveMedicineList(context, list);

        MedicineRepository.markAsTaken(context, "Aspirin", "20:00");

        List<Medicine> reloaded = MedicineRepository.loadMedicineList(context);
        assertTrue(reloaded.get(0).isTaken());
    }

    @Test
    public void markAsTaken_unknownName_doesNothing() {
        Context context = context();
        Medicine medicine = new Medicine("Aspirin", "08:00", "", "");
        List<Medicine> list = new ArrayList<>();
        list.add(medicine);
        MedicineRepository.saveMedicineList(context, list);

        MedicineRepository.markAsTaken(context, "OlmayanIlac", "08:00");

        List<Medicine> reloaded = MedicineRepository.loadMedicineList(context);
        assertFalse(reloaded.get(0).isTaken());
    }

    @Test
    public void resetTakenStatus_clearsTakenFlagByName() {
        Context context = context();
        Medicine medicine = new Medicine("Parol", "08:00", "", "");
        medicine.setTaken(true);
        List<Medicine> list = new ArrayList<>();
        list.add(medicine);
        MedicineRepository.saveMedicineList(context, list);

        MedicineRepository.resetTakenStatus(context, "Parol");

        List<Medicine> reloaded = MedicineRepository.loadMedicineList(context);
        assertFalse(reloaded.get(0).isTaken());
    }
}
