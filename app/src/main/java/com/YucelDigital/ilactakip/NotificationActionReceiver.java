package com.YucelDigital.ilactakip;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Bildirim üzerindeki "İlaç Aldım" ve "Ertele" aksiyonlarını işler.
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_TAKEN  = "com.YucelDigital.ilactakip.ACTION_TAKEN";
    public static final String ACTION_SNOOZE = "com.YucelDigital.ilactakip.ACTION_SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        String medicineName = intent.getStringExtra("MEDICINE_NAME");
        String medicineTime = intent.getStringExtra("MEDICINE_TIME");
        String medicineNote = intent.getStringExtra("MEDICINE_NOTE");
        int    alarmId      = intent.getIntExtra("ALARM_ID", 0);
        long   startDate    = intent.getLongExtra("START_DATE", 0);
        long   endDate      = intent.getLongExtra("END_DATE", 0);
        int    intervalDays = intent.getIntExtra("INTERVAL_DAYS", 1);
        String soundUri     = intent.getStringExtra("SOUND_URI");
        boolean isCustomDay = intent.getBooleanExtra("IS_CUSTOM_DAY", false);
        int    customDay    = intent.getIntExtra("CUSTOM_DAY", 0);

        // Bildirimi kapat
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(alarmId);

        if (ACTION_TAKEN.equals(action)) {
            MedicineRepository.markAsTaken(context, medicineName, medicineTime);
            Toast.makeText(context, medicineName + " alındı ✓", Toast.LENGTH_SHORT).show();

        } else if (ACTION_SNOOZE.equals(action)) {
            int[] possibleMinutes = {5, 10, 15, 20, 25, 30, 35, 40, 45};
            int snoozeMinutes = possibleMinutes[new java.util.Random().nextInt(possibleMinutes.length)];
            scheduleSnooze(context, medicineName, medicineTime, medicineNote,
                    alarmId, startDate, endDate, intervalDays, soundUri, snoozeMinutes,
                    isCustomDay, customDay);
            Toast.makeText(context, snoozeMinutes + " dakika ertelendi", Toast.LENGTH_SHORT).show();
        }
    }

    /** Belirtilen dakika kadar ertele alarmını yeniden kur */
    private void scheduleSnooze(Context context, String medicineName, String medicineTime,
                                String medicineNote, int alarmId, long startDate, long endDate,
                                int intervalDays, String soundUri, int snoozeMinutes,
                                boolean isCustomDay, int customDay) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("MEDICINE_NAME",  medicineName);
        intent.putExtra("MEDICINE_TIME",  medicineTime);
        intent.putExtra("MEDICINE_NOTE",  medicineNote);
        intent.putExtra("ALARM_ID",       alarmId);
        intent.putExtra("START_DATE",     startDate);
        intent.putExtra("END_DATE",       endDate);
        intent.putExtra("INTERVAL_DAYS",  intervalDays);
        intent.putExtra("SOUND_URI",      soundUri);
        intent.putExtra("IS_CUSTOM_DAY",  isCustomDay);
        intent.putExtra("CUSTOM_DAY",     customDay);
        intent.putExtra("IS_RESET",       false);

        PendingIntent pi = PendingIntent.getBroadcast(context, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, snoozeMinutes);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }
}
