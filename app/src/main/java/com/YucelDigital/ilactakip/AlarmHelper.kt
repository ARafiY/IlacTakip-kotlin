package com.YucelDigital.ilactakip

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Alarm iptal ve ID üretim işlemleri için merkezi yardımcı sınıf.
 * Duplicate kodları önler — tek bir yerden yönetilir.
 */
object AlarmHelper {

    /**
     * hashCode()'dan negatif olmayan bir kimlik üretir.
     */
    @JvmStatic
    fun safeId(key: String): Int = key.hashCode() and 0x7fffffff

    @JvmStatic
    fun getStandardAlarmId(medicine: Medicine, singleTime: String): Int {
        val id = if (!medicine.id.isNullOrEmpty()) medicine.id else medicine.name
        return safeId("${id}_$singleTime")
    }

    @JvmStatic
    fun getCustomDayAlarmId(medicine: Medicine, calDay: Int, singleTime: String): Int {
        val id = if (!medicine.id.isNullOrEmpty()) medicine.id else medicine.name
        return safeId("${id}_day${calDay}_$singleTime")
    }

    /**
     * Bir ilaca ait tüm alarmları (ana alarm + reset alarm) iptal eder.
     * Güne özel ve standart mod destekler.
     */
    @JvmStatic
    fun cancelAlarm(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val customDayTimes = medicine.customDayTimes
        if (medicine.isUseCustomDays && customDayTimes != null) {
            cancelCustomDayAlarms(context, alarmManager, medicine, customDayTimes)
        } else {
            cancelStandardAlarms(context, alarmManager, medicine)
        }
    }

    /** Standart mod alarmlarını iptal et */
    private fun cancelStandardAlarms(context: Context, alarmManager: AlarmManager, medicine: Medicine) {
        val time = medicine.time ?: return

        val timeArray = time.split(", ")
        for (rawTime in timeArray) {
            val singleTime = rawTime.trim()
            val intent = Intent(context, AlarmReceiver::class.java)

            // Hem yeni (id tabanlı) hem eski (isim tabanlı) alarmları iptal et (geriye dönük uyumluluk)
            val idsToCancel = listOf(
                getStandardAlarmId(medicine, singleTime),
                safeId(medicine.name + singleTime),
            )
            val resetIdsToCancel = listOf(
                safeId("${if (!medicine.id.isNullOrEmpty()) medicine.id else medicine.name}_${singleTime}_reset"),
                safeId(medicine.name + singleTime + "_reset"),
            )

            for (alarmId in idsToCancel) {
                val pi = PendingIntent.getBroadcast(
                    context, alarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.cancel(pi)
                pi.cancel()
            }

            for (resetAlarmId in resetIdsToCancel) {
                val resetPi = PendingIntent.getBroadcast(
                    context, resetAlarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.cancel(resetPi)
                resetPi.cancel()
            }
        }
    }

    /** Güne özel mod alarmlarını iptal et */
    private fun cancelCustomDayAlarms(
        context: Context,
        alarmManager: AlarmManager,
        medicine: Medicine,
        dayTimes: HashMap<Int, String>,
    ) {
        val intent = Intent(context, AlarmReceiver::class.java)

        for ((calDay, timesForDay) in dayTimes) {
            val times = timesForDay.split(", ")

            for (rawTime in times) {
                val singleTime = rawTime.trim()

                val idsToCancel = listOf(
                    getCustomDayAlarmId(medicine, calDay, singleTime),
                    safeId(medicine.name + "_day" + calDay + "_" + singleTime),
                )
                val resetIdsToCancel = listOf(
                    safeId("${if (!medicine.id.isNullOrEmpty()) medicine.id else medicine.name}_day${calDay}_${singleTime}_reset"),
                    safeId(medicine.name + "_day" + calDay + "_" + singleTime + "_reset"),
                )

                for (alarmId in idsToCancel) {
                    val pi = PendingIntent.getBroadcast(
                        context, alarmId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    alarmManager.cancel(pi)
                    pi.cancel()
                }

                for (resetAlarmId in resetIdsToCancel) {
                    val resetPi = PendingIntent.getBroadcast(
                        context, resetAlarmId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    alarmManager.cancel(resetPi)
                    resetPi.cancel()
                }
            }
        }
    }
}
