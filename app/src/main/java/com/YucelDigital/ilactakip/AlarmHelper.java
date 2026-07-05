package com.YucelDigital.ilactakip;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Alarm iptal işlemleri için merkezi yardımcı sınıf.
 * Duplicate kodları önler — tek bir yerden yönetilir.
 */
public class AlarmHelper {

    /**
     * hashCode()'dan negatif olmayan bir kimlik üretir.
     * Math.abs(hashCode()) kullanılmıyor çünkü hashCode() tam olarak
     * Integer.MIN_VALUE döndürürse Math.abs onu pozitife çeviremez
     * (Integer.MIN_VALUE'nin mutlak değeri int aralığında temsil edilemez).
     */
    public static int safeId(String key) {
        return key.hashCode() & 0x7fffffff;
    }

    /**
     * Bir ilaca ait tüm alarmları (ana alarm + reset alarm) iptal eder.
     * Güne özel ve standart mod destekler.
     */
    public static void cancelAlarm(Context context, Medicine medicine) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (medicine.isUseCustomDays() && medicine.getCustomDayTimes() != null) {
            cancelCustomDayAlarms(context, alarmManager, medicine);
        } else {
            cancelStandardAlarms(context, alarmManager, medicine);
        }
    }

    /** Standart mod alarmlarını iptal et */
    private static void cancelStandardAlarms(Context context, AlarmManager alarmManager, Medicine medicine) {
        if (medicine.getTime() == null) return;

        String[] timeArray = medicine.getTime().split(", ");
        for (String singleTime : timeArray) {
            singleTime = singleTime.trim();
            int alarmId      = safeId(medicine.getName() + singleTime);
            int resetAlarmId = safeId(medicine.getName() + singleTime + "_reset");

            Intent intent = new Intent(context, AlarmReceiver.class);

            PendingIntent pi = PendingIntent.getBroadcast(
                    context, alarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pi);
            pi.cancel();

            PendingIntent resetPi = PendingIntent.getBroadcast(
                    context, resetAlarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(resetPi);
            resetPi.cancel();
        }
    }

    /** Güne özel mod alarmlarını iptal et */
    private static void cancelCustomDayAlarms(Context context, AlarmManager alarmManager, Medicine medicine) {
        HashMap<Integer, String> dayTimes = medicine.getCustomDayTimes();
        if (dayTimes == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);

        for (Map.Entry<Integer, String> entry : dayTimes.entrySet()) {
            int calDay = entry.getKey();
            String[] times = entry.getValue().split(", ");

            for (String singleTime : times) {
                singleTime = singleTime.trim();
                int alarmId = safeId(medicine.getName() + "_day" + calDay + "_" + singleTime);
                int resetAlarmId = safeId(medicine.getName() + "_day" + calDay + "_" + singleTime + "_reset");

                PendingIntent pi = PendingIntent.getBroadcast(
                        context, alarmId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarmManager.cancel(pi);
                pi.cancel();

                // Reset alarm'ı da iptal et
                PendingIntent resetPi = PendingIntent.getBroadcast(
                        context, resetAlarmId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarmManager.cancel(resetPi);
                resetPi.cancel();
            }
        }
    }
}
