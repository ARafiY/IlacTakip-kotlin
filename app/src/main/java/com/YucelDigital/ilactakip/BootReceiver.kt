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
                // Bitiş tarihi geçmiş ilaçları yeniden kurma!
                if (medicine.endDate != 0L) {
                    val endCal = Calendar.getInstance()
                    endCal.timeInMillis = medicine.endDate
                    endCal.set(Calendar.HOUR_OF_DAY, 23)
                    endCal.set(Calendar.MINUTE, 59)
                    if (System.currentTimeMillis() > endCal.timeInMillis) {
                        continue
                    }
                }

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

            val now = System.currentTimeMillis()
            val interval = if (medicine.intervalDays > 0) medicine.intervalDays else 1

            // Başlangıç tarihi gelecekte ise başlangıç tarihine ayarla
            if (medicine.startDate != 0L && medicine.startDate > now) {
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = medicine.startDate
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                calendar.timeInMillis = startCal.timeInMillis
            } else {
                // Geçmiş zamandaysa aralık gün sayısı kadar ilerlet
                while (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, interval)
                }
            }

            // Bitiş tarihini aştıysa kurma
            if (medicine.endDate != 0L) {
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = medicine.endDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }
                if (calendar.timeInMillis > endCal.timeInMillis) continue
            }

            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            alarmIntent.putExtra("MEDICINE_ID", medicine.id)
            alarmIntent.putExtra("MEDICINE_NAME", medicine.name)
            alarmIntent.putExtra("MEDICINE_TIME", singleTime)
            alarmIntent.putExtra("MEDICINE_NOTE", medicine.note)
            alarmIntent.putExtra("START_DATE", medicine.startDate)
            alarmIntent.putExtra("END_DATE", medicine.endDate)
            alarmIntent.putExtra("INTERVAL_DAYS", medicine.intervalDays)
            alarmIntent.putExtra("SOUND_URI", medicine.soundUri)
            alarmIntent.putExtra("MEAL_TIMING", medicine.mealTiming)
            alarmIntent.putExtra("IS_CUSTOM_DAY", false)
            alarmIntent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false)

            val alarmId = AlarmHelper.getStandardAlarmId(medicine, singleTime)
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
                medicine.id,
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

                // Bitiş tarihini aştıysa kurma
                if (medicine.endDate != 0L) {
                    val endCal = Calendar.getInstance().apply {
                        timeInMillis = medicine.endDate
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                    }
                    if (calendar.timeInMillis > endCal.timeInMillis) continue
                }

                val alarmIntent = Intent(context, AlarmReceiver::class.java)
                alarmIntent.putExtra("MEDICINE_ID", medicine.id)
                alarmIntent.putExtra("MEDICINE_NAME", medicine.name)
                alarmIntent.putExtra("MEDICINE_TIME", singleTime)
                alarmIntent.putExtra("MEDICINE_NOTE", medicine.note)
                alarmIntent.putExtra("START_DATE", 0L)
                alarmIntent.putExtra("END_DATE", medicine.endDate)
                alarmIntent.putExtra("INTERVAL_DAYS", 1)
                alarmIntent.putExtra("SOUND_URI", medicine.soundUri)
                alarmIntent.putExtra("MEAL_TIMING", medicine.mealTiming)
                alarmIntent.putExtra("IS_CUSTOM_DAY", true)
                alarmIntent.putExtra("CUSTOM_DAY", calDay)
                alarmIntent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false)

                val alarmId = AlarmHelper.getCustomDayAlarmId(medicine, calDay, singleTime)
                alarmIntent.putExtra("ALARM_ID", alarmId)

                val pi = PendingIntent.getBroadcast(
                    context, alarmId, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                setExactAlarm(alarmManager, calendar.timeInMillis, pi)

                AlarmReceiver.scheduleNextCustomDayResetAlarm(
                    context,
                    medicine.name, singleTime, alarmId,
                    calDay, medicine.soundUri, medicine.endDate,
                    medicine.id,
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
