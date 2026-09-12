package com.YucelDigital.ilactakip

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable
import java.util.UUID

/**
 * İlaç kullanım geçmiş kaydı modeli.
 */
data class MedicineLog(
    val id: String = UUID.randomUUID().toString(),
    val medicineId: String?,
    val medicineName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String = "TAKEN",
) : Serializable

/**
 * İlaç verileri ve geçmiş logları için thread-safe merkezi erişim noktası.
 * Tüm SharedPreferences okuma/yazma işlemleri bu sınıf üzerinden yapılır.
 * synchronized bloklar ile race condition önlenir.
 */
object MedicineRepository {

    private const val PREFS_NAME = "MedicineApp"
    private const val KEY_MEDICINE_LIST = "medicine_list"
    private const val KEY_MEDICINE_LOGS = "medicine_logs"
    private val LOCK = Any()
    private val gson = Gson()
    private val listType = object : TypeToken<ArrayList<Medicine>>() {}.type
    private val logListType = object : TypeToken<ArrayList<MedicineLog>>() {}.type

    /** İlaç listesini oku (thread-safe, id boş ise otomatik atar) */
    @JvmStatic
    fun loadMedicineList(context: Context): List<Medicine> {
        synchronized(LOCK) {
            val list = loadMedicineListInternal(context) ?: return ArrayList()
            var modified = false
            for (m in list) {
                if (m.id.isNullOrEmpty()) {
                    m.id = UUID.randomUUID().toString()
                    modified = true
                }
            }
            if (modified) {
                saveMedicineListInternal(context, list)
            }
            return list
        }
    }

    /** İlaç listesini kaydet (thread-safe) */
    @JvmStatic
    fun saveMedicineList(context: Context, list: List<Medicine>) {
        synchronized(LOCK) {
            saveMedicineListInternal(context, list)
        }
    }

    /**
     * Belirli bir ilacı "alındı" olarak işaretle (thread-safe).
     * İlaç ID'si varsa ID ile, yoksa isimle eşleştirir.
     * Stok takibi açıksa stok sayısını 1 azaltır ve geçmişe log kaydı ekler.
     */
    @JvmStatic
    @JvmOverloads
    fun markAsTaken(context: Context, medicineName: String?, medicineTime: String? = null, medicineId: String? = null) {
        synchronized(LOCK) {
            val list = loadMedicineListInternal(context) ?: return

            var changed = false
            var targetMed: Medicine? = null
            for (m in list) {
                val matchesId = !medicineId.isNullOrEmpty() && m.id == medicineId
                val matchesName = (medicineId.isNullOrEmpty() || m.id.isNullOrEmpty()) && m.name.equals(medicineName, ignoreCase = true)

                if (matchesId || matchesName) {
                    m.isTaken = true
                    val currentStock = m.stockCount
                    if (currentStock != null && currentStock > 0) {
                        m.stockCount = currentStock - 1
                    }
                    targetMed = m
                    changed = true
                    break
                }
            }
            if (changed) {
                saveMedicineListInternal(context, list)
                targetMed?.let {
                    recordLogInternal(
                        context,
                        MedicineLog(
                            medicineId = it.id,
                            medicineName = it.name,
                            timestamp = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }
    }

    /** Belirli bir ilacın isTaken durumunu sıfırla (thread-safe) */
    @JvmStatic
    @JvmOverloads
    fun resetTakenStatus(context: Context, medicineName: String?, medicineId: String? = null) {
        synchronized(LOCK) {
            val list = loadMedicineListInternal(context) ?: return

            var changed = false
            for (m in list) {
                val matchesId = !medicineId.isNullOrEmpty() && m.id == medicineId
                val matchesName = (medicineId.isNullOrEmpty() || m.id.isNullOrEmpty()) && m.name.equals(medicineName, ignoreCase = true)

                if (matchesId || matchesName) {
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

    /** İlaç geçmiş loglarını oku */
    @JvmStatic
    fun loadLogs(context: Context): List<MedicineLog> {
        synchronized(LOCK) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_MEDICINE_LOGS, null) ?: return ArrayList()
            val list: List<MedicineLog>? = gson.fromJson(json, logListType)
            return list?.sortedByDescending { it.timestamp } ?: ArrayList()
        }
    }

    /** Geçmiş log kaydı ekle */
    @JvmStatic
    fun recordLog(context: Context, log: MedicineLog) {
        synchronized(LOCK) {
            recordLogInternal(context, log)
        }
    }

    /** Tüm geçmiş logları temizle */
    @JvmStatic
    fun clearLogs(context: Context) {
        synchronized(LOCK) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_MEDICINE_LOGS).apply()
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

    private fun recordLogInternal(context: Context, log: MedicineLog) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MEDICINE_LOGS, null)
        val list: ArrayList<MedicineLog> = if (json != null) {
            gson.fromJson(json, logListType) ?: ArrayList()
        } else {
            ArrayList()
        }
        list.add(log)
        // En fazla son 500 kaydı sakla
        if (list.size > 500) {
            list.removeAt(0)
        }
        prefs.edit().putString(KEY_MEDICINE_LOGS, gson.toJson(list)).apply()
    }
}
