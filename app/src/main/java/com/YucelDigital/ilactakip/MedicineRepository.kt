package com.YucelDigital.ilactakip

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * İlaç verileri için thread-safe merkezi erişim noktası.
 * Tüm SharedPreferences okuma/yazma işlemleri bu sınıf üzerinden yapılır.
 * synchronized bloklar ile race condition önlenir.
 */
object MedicineRepository {

    private const val PREFS_NAME = "MedicineApp"
    private const val KEY_MEDICINE_LIST = "medicine_list"
    private val LOCK = Any()
    private val gson = Gson()
    private val listType = object : TypeToken<ArrayList<Medicine>>() {}.type

    /** İlaç listesini oku (thread-safe) */
    @JvmStatic
    fun loadMedicineList(context: Context): List<Medicine> {
        synchronized(LOCK) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_MEDICINE_LIST, null) ?: return ArrayList()
            val list: List<Medicine>? = gson.fromJson(json, listType)
            return list ?: ArrayList()
        }
    }

    /** İlaç listesini kaydet (thread-safe) */
    @JvmStatic
    fun saveMedicineList(context: Context, list: List<Medicine>) {
        synchronized(LOCK) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_MEDICINE_LIST, gson.toJson(list)).apply() // apply: asenkron yazma
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
    @JvmStatic
    fun markAsTaken(context: Context, medicineName: String?, medicineTime: String?) {
        synchronized(LOCK) {
            val list = loadMedicineListInternal(context) ?: return

            var changed = false
            for (m in list) {
                if (m.name.equals(medicineName, ignoreCase = true)) {
                    m.isTaken = true
                    changed = true
                    break
                }
            }
            if (changed) {
                saveMedicineListInternal(context, list)
            }
        }
    }

    /** Belirli bir ilacın isTaken durumunu sıfırla (thread-safe) */
    @JvmStatic
    fun resetTakenStatus(context: Context, medicineName: String?) {
        synchronized(LOCK) {
            val list = loadMedicineListInternal(context) ?: return

            var changed = false
            for (m in list) {
                if (m.name.equals(medicineName, ignoreCase = true)) {
                    m.isTaken = false
                    changed = true
                    break
                }
            }
            if (changed) {
                saveMedicineListInternal(context, list)
            }
        }
    }

    // ── Internal (LOCK zaten tutulmuş durumda) ──

    private fun loadMedicineListInternal(context: Context): List<Medicine>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MEDICINE_LIST, null) ?: return null
        return gson.fromJson(json, listType)
    }

    private fun saveMedicineListInternal(context: Context, list: List<Medicine>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MEDICINE_LIST, gson.toJson(list)).apply()
    }
}
