package com.YucelDigital.ilactakip

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.util.Calendar

/**
 * Bildirim üzerindeki "İlaç Aldım" ve "Ertele" aksiyonlarını işler.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKEN = "com.YucelDigital.ilactakip.ACTION_TAKEN"
        const val ACTION_SNOOZE = "com.YucelDigital.ilactakip.ACTION_SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val medicineId = intent.getStringExtra("MEDICINE_ID")
        val medicineName = intent.getStringExtra("MEDICINE_NAME")
        val medicineTime = intent.getStringExtra("MEDICINE_TIME")
        val medicineNote = intent.getStringExtra("MEDICINE_NOTE")
        val alarmId = intent.getIntExtra("ALARM_ID", 0)
        val startDate = intent.getLongExtra("START_DATE", 0)
        val endDate = intent.getLongExtra("END_DATE", 0)
        val intervalDays = intent.getIntExtra("INTERVAL_DAYS", 1)
        val soundUri = intent.getStringExtra("SOUND_URI")
        val isCustomDay = intent.getBooleanExtra("IS_CUSTOM_DAY", false)
        val customDay = intent.getIntExtra("CUSTOM_DAY", 0)
        val mealTiming = intent.getStringExtra("MEAL_TIMING")
        val snoozeMinutes = intent.getIntExtra("SNOOZE_MINUTES", 10)

        // Bildirimi kapat
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(alarmId)

        if (ACTION_TAKEN == action) {
            MedicineRepository.markAsTaken(context, medicineName, medicineTime, medicineId)
            Toast.makeText(context, "$medicineName alındı ✓", Toast.LENGTH_SHORT).show()
        } else if (ACTION_SNOOZE == action) {
            scheduleSnooze(
                context, medicineId, medicineName, medicineTime, medicineNote,
                alarmId, startDate, endDate, intervalDays, soundUri, snoozeMinutes,
                isCustomDay, customDay, mealTiming,
            )
            Toast.makeText(context, "$snoozeMinutes dakika ertelendi", Toast.LENGTH_SHORT).show()
        }
    }

    /** Belirtilen dakika kadar ertele alarmını kur */
    private fun scheduleSnooze(
        context: Context,
        medicineId: String?,
        medicineName: String?,
        medicineTime: String?,
        medicineNote: String?,
        alarmId: Int,
        startDate: Long,
        endDate: Long,
        intervalDays: Int,
        soundUri: String?,
        snoozeMinutes: Int,
        isCustomDay: Boolean,
        customDay: Int,
        mealTiming: String?,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("MEDICINE_ID", medicineId)
        intent.putExtra("MEDICINE_NAME", medicineName)
        intent.putExtra("MEDICINE_TIME", medicineTime)
        intent.putExtra("MEDICINE_NOTE", medicineNote)
        intent.putExtra("ALARM_ID", alarmId)
        intent.putExtra("START_DATE", startDate)
        intent.putExtra("END_DATE", endDate)
        intent.putExtra("INTERVAL_DAYS", intervalDays)
        intent.putExtra("SOUND_URI", soundUri)
        intent.putExtra("IS_CUSTOM_DAY", isCustomDay)
        intent.putExtra("CUSTOM_DAY", customDay)
        intent.putExtra("MEAL_TIMING", mealTiming)
        intent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false)

        val pi = PendingIntent.getBroadcast(
            context, alarmId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, snoozeMinutes)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }
}
