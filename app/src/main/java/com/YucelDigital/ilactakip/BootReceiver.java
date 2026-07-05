package com.YucelDigital.ilactakip;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
                !"android.intent.action.QUICKBOOT_POWERON".equals(action)) return;

        List<Medicine> medicineList = MedicineRepository.loadMedicineList(context);
        if (medicineList == null) return;

        for (Medicine medicine : medicineList) {
            if (medicine.isActive()) {
                if (medicine.isUseCustomDays() && medicine.getCustomDayTimes() != null) {
                    scheduleCustomDayAlarms(context, medicine);
                } else {
                    scheduleStandardAlarms(context, medicine);
                }
            }
        }
    }

    /** Standart mod alarmlarını boot sonrası yeniden kur */
    private void scheduleStandardAlarms(Context context, Medicine medicine) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (medicine.getTime() == null) return;
        String[] timeArray = medicine.getTime().split(", ");

        for (String singleTime : timeArray) {
            singleTime = singleTime.trim();
            if (!singleTime.contains(":")) continue;

            String[] timeParts = singleTime.split(":");
            int hour, minute;
            try {
                hour   = Integer.parseInt(timeParts[0]);
                minute = Integer.parseInt(timeParts[1]);
            } catch (NumberFormatException e) {
                continue;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            alarmIntent.putExtra("MEDICINE_NAME",  medicine.getName());
            alarmIntent.putExtra("MEDICINE_TIME",  singleTime);
            alarmIntent.putExtra("MEDICINE_NOTE",  medicine.getNote());
            alarmIntent.putExtra("START_DATE",     medicine.getStartDate());
            alarmIntent.putExtra("END_DATE",       medicine.getEndDate());
            alarmIntent.putExtra("INTERVAL_DAYS",  medicine.getIntervalDays());
            alarmIntent.putExtra("SOUND_URI",      medicine.getSoundUri());
            alarmIntent.putExtra("IS_CUSTOM_DAY",  false);
            alarmIntent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false);

            int alarmId = AlarmHelper.safeId(medicine.getName() + singleTime);
            alarmIntent.putExtra("ALARM_ID", alarmId);

            PendingIntent pi = PendingIntent.getBroadcast(
                    context, alarmId, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            setExactAlarm(alarmManager, calendar.getTimeInMillis(), pi);

            AlarmReceiver.scheduleNextResetAlarm(
                    context,
                    medicine.getName(), singleTime, alarmId,
                    medicine.getStartDate(), medicine.getEndDate(),
                    medicine.getIntervalDays(), medicine.getSoundUri()
            );
        }
    }

    /** Güne özel mod alarmlarını boot sonrası yeniden kur */
    private void scheduleCustomDayAlarms(Context context, Medicine medicine) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        HashMap<Integer, String> dayTimes = medicine.getCustomDayTimes();
        if (dayTimes == null) return;

        for (Map.Entry<Integer, String> entry : dayTimes.entrySet()) {
            int calDay = entry.getKey();
            String[] times = entry.getValue().split(", ");

            for (String singleTime : times) {
                singleTime = singleTime.trim();
                if (!singleTime.contains(":")) continue;

                String[] timeParts = singleTime.split(":");
                int hour, minute;
                try {
                    hour   = Integer.parseInt(timeParts[0]);
                    minute = Integer.parseInt(timeParts[1]);
                } catch (NumberFormatException e) {
                    continue;
                }

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                calendar.set(Calendar.DAY_OF_WEEK, calDay);

                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                }

                Intent alarmIntent = new Intent(context, AlarmReceiver.class);
                alarmIntent.putExtra("MEDICINE_NAME",  medicine.getName());
                alarmIntent.putExtra("MEDICINE_TIME",  singleTime);
                alarmIntent.putExtra("MEDICINE_NOTE",  medicine.getNote());
                alarmIntent.putExtra("START_DATE",     0L);
                alarmIntent.putExtra("END_DATE",       0L);
                alarmIntent.putExtra("INTERVAL_DAYS",  1);
                alarmIntent.putExtra("SOUND_URI",      medicine.getSoundUri());
                alarmIntent.putExtra("IS_CUSTOM_DAY",  true);
                alarmIntent.putExtra("CUSTOM_DAY",     calDay);
                alarmIntent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false);

                int alarmId = AlarmHelper.safeId(medicine.getName() + "_day" + calDay + "_" + singleTime);
                alarmIntent.putExtra("ALARM_ID", alarmId);

                PendingIntent pi = PendingIntent.getBroadcast(
                        context, alarmId, alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                setExactAlarm(alarmManager, calendar.getTimeInMillis(), pi);
                
                AlarmReceiver.scheduleNextCustomDayResetAlarm(
                        context,
                        medicine.getName(), singleTime, alarmId,
                        calDay, medicine.getSoundUri()
                );
            }
        }
    }

    private void setExactAlarm(AlarmManager am, long triggerAt, PendingIntent pi) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}