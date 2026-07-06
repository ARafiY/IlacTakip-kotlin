package com.YucelDigital.ilactakip

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED != action && "android.intent.action.QUICKBOOT_POWERON" != action) return

        val medicineList = MedicineRepository.loadMedicineList(context)

        for (medicine in medicineList) {
            if (medicine.isActive) {
                val customDayTimes = medicine.customDayTimes
                if (medicine.isUseCustomDays && customDayTimes != null) {
                    scheduleCustomDayAlarms(context, medicine, customDayTimes)
                } else {
                    scheduleStandardAlarms(context, medicine)
                }
            }
        }
    }

    /** Standart mod alarmlarını boot sonrası yeniden kur */
    private fun scheduleStandardAlarms(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val time = medicine.time ?: return

        for (rawTime in time.split(", ")) {
            val singleTime = rawTime.trim()
            if (!singleTime.contains(":")) continue

            val timeParts = singleTime.split(":")
            val hour: Int
            val minute: Int
            try {
                hour = timeParts[0].toInt()
                minute = timeParts[1].toInt()
            } catch (e: NumberFormatException) {
                continue
            }

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            alarmIntent.putExtra("MEDICINE_NAME", medicine.name)
            alarmIntent.putExtra("MEDICINE_TIME", singleTime)
            alarmIntent.putExtra("MEDICINE_NOTE", medicine.note)
            alarmIntent.putExtra("START_DATE", medicine.startDate)
            alarmIntent.putExtra("END_DATE", medicine.endDate)
            alarmIntent.putExtra("INTERVAL_DAYS", medicine.intervalDays)
            alarmIntent.putExtra("SOUND_URI", medicine.soundUri)
            alarmIntent.putExtra("IS_CUSTOM_DAY", false)
            alarmIntent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false)

            val alarmId = AlarmHelper.safeId(medicine.name + singleTime)
            alarmIntent.putExtra("ALARM_ID", alarmId)

            val pi = PendingIntent.getBroadcast(
                context, alarmId, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            setExactAlarm(alarmManager, calendar.timeInMillis, pi)

            AlarmReceiver.scheduleNextResetAlarm(
                context,
                medicine.name, singleTime, alarmId,
                medicine.startDate, medicine.endDate,
                medicine.intervalDays, medicine.soundUri,
            )
        }
    }

    /** Güne özel mod alarmlarını boot sonrası yeniden kur */
    private fun scheduleCustomDayAlarms(context: Context, medicine: Medicine, dayTimes: HashMap<Int, String>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for ((calDay, timesForDay) in dayTimes) {
            for (rawTime in timesForDay.split(", ")) {
                val singleTime = rawTime.trim()
                if (!singleTime.contains(":")) continue

                val timeParts = singleTime.split(":")
                val hour: Int
                val minute: Int
                try {
                    hour = timeParts[0].toInt()
                    minute = timeParts[1].toInt()
                } catch (e: NumberFormatException) {
                    continue
                }

                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.set(Calendar.DAY_OF_WEEK, calDay)

                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }

                val alarmIntent = Intent(context, AlarmReceiver::class.java)
                alarmIntent.putExtra("MEDICINE_NAME", medicine.name)
                alarmIntent.putExtra("MEDICINE_TIME", singleTime)
                alarmIntent.putExtra("MEDICINE_NOTE", medicine.note)
                alarmIntent.putExtra("START_DATE", 0L)
                alarmIntent.putExtra("END_DATE", 0L)
                alarmIntent.putExtra("INTERVAL_DAYS", 1)
                alarmIntent.putExtra("SOUND_URI", medicine.soundUri)
                alarmIntent.putExtra("IS_CUSTOM_DAY", true)
                alarmIntent.putExtra("CUSTOM_DAY", calDay)
                alarmIntent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false)

                val alarmId = AlarmHelper.safeId(medicine.name + "_day" + calDay + "_" + singleTime)
                alarmIntent.putExtra("ALARM_ID", alarmId)

                val pi = PendingIntent.getBroadcast(
                    context, alarmId, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                setExactAlarm(alarmManager, calendar.timeInMillis, pi)

                AlarmReceiver.scheduleNextCustomDayResetAlarm(
                    context,
                    medicine.name, singleTime, alarmId,
                    calDay, medicine.soundUri,
                )
            }
        }
    }

    private fun setExactAlarm(am: AlarmManager, triggerAt: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
}
