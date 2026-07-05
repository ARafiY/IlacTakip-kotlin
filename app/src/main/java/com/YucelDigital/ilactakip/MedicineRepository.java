package com.YucelDigital.ilactakip;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * İlaç verileri için thread-safe merkezi erişim noktası.
 * Tüm SharedPreferences okuma/yazma işlemleri bu sınıf üzerinden yapılır.
 * synchronized bloklar ile race condition önlenir.
 */
public class MedicineRepository {

    private static final String PREFS_NAME = "MedicineApp";
    private static final String KEY_MEDICINE_LIST = "medicine_list";
    private static final Object LOCK = new Object();
    private static final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<ArrayList<Medicine>>() {}.getType();

    /** İlaç listesini oku (thread-safe) */
    public static List<Medicine> loadMedicineList(Context context) {
        synchronized (LOCK) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_MEDICINE_LIST, null);
            if (json == null) return new ArrayList<>();
            List<Medicine> list = gson.fromJson(json, LIST_TYPE);
            return list != null ? list : new ArrayList<>();
        }
    }

    /** İlaç listesini kaydet (thread-safe) */
    public static void saveMedicineList(Context context, List<Medicine> list) {
        synchronized (LOCK) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_MEDICINE_LIST, gson.toJson(list)).apply(); // apply: asenkron yazma
        }
    }

    /**
     * Belirli bir ilacı "alındı" olarak işaretle (thread-safe).
     * İsimle eşleştirilir — güne özel modda Medicine.time sadece tek bir günün
     * saatini tuttuğu için (bkz. AddMedicineActivity.saveCustomDayAlarm), saat
     * bazlı eşleştirme farklı günler/saatler için hatalı biçimde eşleşmeyi
     * kaçırıyordu. İsim tekilliği zaten UI tarafında (isDuplicateName) garanti
     * edildiğinden sadece isimle eşleştirmek güvenlidir.
     */
    public static void markAsTaken(Context context, String medicineName, String medicineTime) {
        synchronized (LOCK) {
            List<Medicine> list = loadMedicineListInternal(context);
            if (list == null) return;

            boolean changed = false;
            for (Medicine m : list) {
                if (m.getName().equalsIgnoreCase(medicineName)) {
                    m.setTaken(true);
                    changed = true;
                    break;
                }
            }
            if (changed) {
                saveMedicineListInternal(context, list);
            }
        }
    }

    /** Belirli bir ilacın isTaken durumunu sıfırla (thread-safe) */
    public static void resetTakenStatus(Context context, String medicineName) {
        synchronized (LOCK) {
            List<Medicine> list = loadMedicineListInternal(context);
            if (list == null) return;

            boolean changed = false;
            for (Medicine m : list) {
                if (m.getName().equalsIgnoreCase(medicineName)) {
                    m.setTaken(false);
                    changed = true;
                    break;
                }
            }
            if (changed) {
                saveMedicineListInternal(context, list);
            }
        }
    }

    // ── Internal (LOCK zaten tutulmuş durumda) ──

    private static List<Medicine> loadMedicineListInternal(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_MEDICINE_LIST, null);
        if (json == null) return null;
        return gson.fromJson(json, LIST_TYPE);
    }

    private static void saveMedicineListInternal(Context context, List<Medicine> list) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_MEDICINE_LIST, gson.toJson(list)).apply();
    }
}
