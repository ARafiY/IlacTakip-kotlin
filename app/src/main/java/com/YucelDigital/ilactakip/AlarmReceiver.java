package com.YucelDigital.ilactakip;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medicine_alarm_channel";

    /**
     * IS_RESET = true olduğunda bu alarm "ön sıfırlama" alarmıdır.
     * Asıl alarmdan 60 dakika önce tetiklenerek isTaken bayrağını temizler.
     */
    public static final String EXTRA_IS_RESET = "IS_RESET";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Ortak veriler
        String name = intent.getStringExtra("MEDICINE_NAME");
        String time = intent.getStringExtra("MEDICINE_TIME");
        String note = intent.getStringExtra("MEDICINE_NOTE");
        int alarmId = intent.getIntExtra("ALARM_ID", 0);
        long startDate = intent.getLongExtra("START_DATE", 0);
        long endDate = intent.getLongExtra("END_DATE", 0);
        int intervalDays = intent.getIntExtra("INTERVAL_DAYS", 1);
        String soundUriStr = intent.getStringExtra("SOUND_URI");
        boolean isReset = intent.getBooleanExtra(EXTRA_IS_RESET, false);
        boolean isCustomDay = intent.getBooleanExtra("IS_CUSTOM_DAY", false);
        int customDay = intent.getIntExtra("CUSTOM_DAY", 0);

        // ---- RESET ALARMIYSA: isTaken'ı temizle, çık ----
        if (isReset) {
            MedicineRepository.resetTakenStatus(context, name);
            if (isCustomDay && customDay != 0) {
                scheduleNextCustomDayResetAlarm(context, name, time, alarmId, customDay, soundUriStr);
            } else {
                scheduleNextResetAlarm(context, name, time, alarmId,
                        startDate, endDate, intervalDays, soundUriStr);
            }
            return;
        }

        // ---- ANA ALARM: Tarih / Gün Kontrolü (sadece standart mod) ----
        if (!isCustomDay && startDate != 0) {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            long todayMillis = today.getTimeInMillis();

            if (todayMillis < startDate) {
                rescheduleNextMainAlarm(context, name, time, note, alarmId,
                        startDate, endDate, intervalDays, soundUriStr);
                return;
            }

            if (endDate != 0) {
                Calendar endCal = Calendar.getInstance();
                endCal.setTimeInMillis(endDate);
                endCal.set(Calendar.HOUR_OF_DAY, 23);
                endCal.set(Calendar.MINUTE, 59);
                if (todayMillis > endCal.getTimeInMillis())
                    return;
            }

            if (intervalDays > 1) {
                long diffMillis = todayMillis - startDate;
                long diffDays = diffMillis / (1000L * 60 * 60 * 24);
                if (diffDays % intervalDays != 0) {
                    rescheduleNextMainAlarm(context, name, time, note, alarmId,
                            startDate, endDate, intervalDays, soundUriStr);
                    return;
                }
            }
        }

        // ---- ANA ALARM: Bildirim inşası + isTaken kontrolü disk I/O gerektirir.
        // onReceive'i (ve dolayısıyla ana thread'i) hızlıca serbest bırakmak için
        // bu kısmı arka planda çalıştırıyoruz.
        Context appContext = context.getApplicationContext();
        PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                handleMainAlarm(appContext, name, time, note, alarmId, startDate, endDate,
                        intervalDays, soundUriStr, isCustomDay, customDay);
            } finally {
                pendingResult.finish();
            }
        }).start();
    }

    /** Ana alarm tetiklendiğinde bildirim inşası + kendini yeniden kurma (arka planda çalışır). */
    private void handleMainAlarm(Context context, String name, String time, String note, int alarmId,
            long startDate, long endDate, int intervalDays, String soundUriStr,
            boolean isCustomDay, int customDay) {

        // ---- ANA ALARM: Bildirim Göster ----
        // İsimle eşleştirilir (bkz. MedicineRepository.markAsTaken açıklaması) —
        // güne özel modda Medicine.time sadece tek bir günün saatini tuttuğu için
        // saat bazlı eşleştirme hatalı biçimde eşleşmeyi kaçırıyordu.
        boolean isAlreadyTaken = false;
        List<Medicine> medicineList = MedicineRepository.loadMedicineList(context);
        for (Medicine m : medicineList) {
            if (m.getName().equalsIgnoreCase(name)) {
                if (m.isTaken()) {
                    isAlreadyTaken = true;
                }
                break;
            }
        }

        if (!isAlreadyTaken) {
            Uri soundUri = resolveSoundUri(soundUriStr);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            Intent fullScreenIntent = new Intent(context, AlarmActivity.class);
            fullScreenIntent.putExtra("MEDICINE_NAME", name);
            fullScreenIntent.putExtra("MEDICINE_TIME", time);
            fullScreenIntent.putExtra("MEDICINE_NOTE", note);
            fullScreenIntent.putExtra("ALARM_ID", alarmId);
            fullScreenIntent.putExtra("START_DATE", startDate);
            fullScreenIntent.putExtra("END_DATE", endDate);
            fullScreenIntent.putExtra("INTERVAL_DAYS", intervalDays);
            fullScreenIntent.putExtra("SOUND_URI", soundUriStr);
            fullScreenIntent.putExtra("IS_CUSTOM_DAY", isCustomDay);
            fullScreenIntent.putExtra("CUSTOM_DAY", customDay);
            fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent fullScreenPI = PendingIntent.getActivity(
                    context, alarmId, fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "İlaç Hatırlatıcı", NotificationManager.IMPORTANCE_HIGH);
                channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
                channel.enableVibration(true);
                channel.setSound(null, null);
                if (nm != null)
                    nm.createNotificationChannel(channel);
                buildAndNotify(context, nm, CHANNEL_ID, name, time, note,
                        alarmId, startDate, endDate, intervalDays, soundUriStr,
                        isCustomDay, customDay, fullScreenPI, soundUri);
            } else {
                buildAndNotify(context, nm, CHANNEL_ID, name, time, note,
                        alarmId, startDate, endDate, intervalDays, soundUriStr,
                        isCustomDay, customDay, fullScreenPI, soundUri);
            }
        }

        // ---- KENDİNİ YENİDEN KUR ----
        if (isCustomDay && customDay != 0) {
            // Güne özel: Gelecek haftanın aynı günü için yeniden kur
            rescheduleCustomDayAlarm(context, name, time, note, alarmId,
                    soundUriStr, customDay);
            scheduleNextCustomDayResetAlarm(context, name, time, alarmId, customDay, soundUriStr);
        } else {
            // Standart: Bir sonraki döngü
            rescheduleNextMainAlarm(context, name, time, note, alarmId,
                    startDate, endDate, intervalDays, soundUriStr);
            scheduleNextResetAlarm(context, name, time, alarmId,
                    startDate, endDate, intervalDays, soundUriStr);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BİLDİRİM İNŞA
    // ──────────────────────────────────────────────────────────────────────────

    private void buildAndNotify(Context context, NotificationManager nm, String channelId,
            String name, String time, String note, int alarmId,
            long startDate, long endDate, int intervalDays,
            String soundUriStr, boolean isCustomDay, int customDay,
            PendingIntent fullScreenPI, Uri soundUri) {

        // "İlaç Aldım" aksiyon
        Intent takenIntent = new Intent(context, NotificationActionReceiver.class);
        takenIntent.setAction(NotificationActionReceiver.ACTION_TAKEN);
        takenIntent.putExtra("MEDICINE_NAME", name);
        takenIntent.putExtra("MEDICINE_TIME", time);
        takenIntent.putExtra("ALARM_ID", alarmId);
        PendingIntent takenPI = PendingIntent.getBroadcast(
                context, alarmId + 1000, takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // "Ertele 5 dk" aksiyon
        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction(NotificationActionReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra("MEDICINE_NAME", name);
        snoozeIntent.putExtra("MEDICINE_TIME", time);
        snoozeIntent.putExtra("MEDICINE_NOTE", note);
        snoozeIntent.putExtra("ALARM_ID", alarmId);
        snoozeIntent.putExtra("START_DATE", startDate);
        snoozeIntent.putExtra("END_DATE", endDate);
        snoozeIntent.putExtra("INTERVAL_DAYS", intervalDays);
        snoozeIntent.putExtra("SOUND_URI", soundUriStr);
        snoozeIntent.putExtra("IS_CUSTOM_DAY", isCustomDay);
        snoozeIntent.putExtra("CUSTOM_DAY", customDay);
        snoozeIntent.putExtra("SNOOZE_MINUTES", 5);
        PendingIntent snoozePI = PendingIntent.getBroadcast(
                context, alarmId + 2000, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_medicine)
                .setContentTitle("İlaç Vakti: " + name)
                .setContentText(time + " — İlacınızı almayı unutmayın.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(new long[] { 0, 500, 200, 500 })
                .setFullScreenIntent(fullScreenPI, true)
                .addAction(R.drawable.ic_medicine_white, "✓ İlaç Aldım", takenPI)
                .addAction(R.drawable.ic_note, "⏰ Ertele", snoozePI);

        // Cihaz kilitliyse full-screen intent otomatik açılır ve AlarmActivity kendi
        // sesini çalar — bildirime ayrıca ses eklersek çift ses duyulur. Kilitli
        // değilse full-screen intent genelde sadece heads-up bildirim olarak
        // gösterilir, bu yüzden sesi bildirim üzerinden çalmamız gerekir.
        // (Not: PowerManager.isInteractive() burada yanıltıcıdır — ekran açık ama
        // kilitli olabilir.)
        android.app.KeyguardManager km =
                (android.app.KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean isLocked = km != null && km.isKeyguardLocked();
        if (isLocked) {
            builder.setSound(null);
        } else {
            builder.setSound(soundUri);
        }

        if (nm != null)
            nm.notify(alarmId, builder.build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // YENIDEN KURGULAMA
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * ANA ALARM'ı bir sonraki döngü için kur.
     * Her tetiklenmede bu çağrılarak alarm zinciri korunur.
     */
    static void rescheduleNextMainAlarm(Context context, String name, String time, String note,
            int alarmId, long startDate, long endDate,
            int intervalDays, String soundUriStr) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null)
            return;

        if (time == null || !time.contains(":")) return;

        String[] parts = time.split(":");
        int hour, minute;
        try {
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return;
        }

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        next.add(Calendar.DAY_OF_YEAR, intervalDays); // Bir sonraki döngü

        // Bitiş tarihi geçtiyse yeniden kurma
        if (endDate != 0) {
            Calendar endCal = Calendar.getInstance();
            endCal.setTimeInMillis(endDate);
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);
            if (next.getTimeInMillis() > endCal.getTimeInMillis())
                return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("MEDICINE_NAME", name);
        intent.putExtra("MEDICINE_TIME", time);
        intent.putExtra("MEDICINE_NOTE", note);
        intent.putExtra("ALARM_ID", alarmId);
        intent.putExtra("START_DATE", startDate);
        intent.putExtra("END_DATE", endDate);
        intent.putExtra("INTERVAL_DAYS", intervalDays);
        intent.putExtra("SOUND_URI", soundUriStr);
        intent.putExtra(EXTRA_IS_RESET, false);

        PendingIntent pi = PendingIntent.getBroadcast(context, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        }
    }

    /**
     * GÜNE ÖZEL ALARM'ı gelecek haftanın aynı günü için yeniden kur.
     */
    static void rescheduleCustomDayAlarm(Context context, String name, String time, String note,
            int alarmId, String soundUriStr, int calendarDay) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        if (time == null || !time.contains(":")) return;

        String[] parts = time.split(":");
        int hour, minute;
        try {
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return;
        }

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        next.set(Calendar.DAY_OF_WEEK, calendarDay);
        next.add(Calendar.WEEK_OF_YEAR, 1); // Gelecek hafta

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("MEDICINE_NAME", name);
        intent.putExtra("MEDICINE_TIME", time);
        intent.putExtra("MEDICINE_NOTE", note);
        intent.putExtra("ALARM_ID", alarmId);
        intent.putExtra("START_DATE", 0L);
        intent.putExtra("END_DATE", 0L);
        intent.putExtra("INTERVAL_DAYS", 1);
        intent.putExtra("SOUND_URI", soundUriStr);
        intent.putExtra("IS_CUSTOM_DAY", true);
        intent.putExtra("CUSTOM_DAY", calendarDay);
        intent.putExtra(EXTRA_IS_RESET, false);

        PendingIntent pi = PendingIntent.getBroadcast(context, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        }
    }

    /**
     * RESET ALARM'ı bir sonraki döngü için kur.
     * Asıl alarmdan 60 dakika önce tetiklenerek "İlacı Aldım" işaretini temizler.
     */
    static void scheduleNextResetAlarm(Context context, String name, String time, int alarmId,
            long startDate, long endDate,
            int intervalDays, String soundUriStr) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null)
            return;

        if (time == null || !time.contains(":")) return;

        String[] parts = time.split(":");
        int hour, minute;
        try {
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return;
        }

        Calendar reset = Calendar.getInstance();
        reset.set(Calendar.HOUR_OF_DAY, hour);
        reset.set(Calendar.MINUTE, minute);
        reset.set(Calendar.SECOND, 0);
        reset.set(Calendar.MILLISECOND, 0);
        
        long resetMillis = reset.getTimeInMillis() - (60 * 60 * 1000); // asıl alarmdan 60 dk öncesi
        
        // Eğer bu reset saati (bugün için) geçmişse, gelecekteki uygun saate kadar interval ekle
        while (resetMillis <= System.currentTimeMillis()) {
            resetMillis += ((long) intervalDays * 24 * 60 * 60 * 1000);
        }

        reset.setTimeInMillis(resetMillis);

        // Bitiş tarihinden sonraya geçtiyse planla
        if (endDate != 0) {
            Calendar endCal = Calendar.getInstance();
            endCal.setTimeInMillis(endDate);
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);
            if (reset.getTimeInMillis() > endCal.getTimeInMillis())
                return;
        }

        int resetAlarmId = AlarmHelper.safeId(name + time + "_reset");

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("MEDICINE_NAME", name);
        intent.putExtra("MEDICINE_TIME", time);
        intent.putExtra("ALARM_ID", alarmId);
        intent.putExtra("START_DATE", startDate);
        intent.putExtra("END_DATE", endDate);
        intent.putExtra("INTERVAL_DAYS", intervalDays);
        intent.putExtra("SOUND_URI", soundUriStr);
        intent.putExtra(EXTRA_IS_RESET, true);

        PendingIntent pi = PendingIntent.getBroadcast(context, resetAlarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reset.getTimeInMillis(), pi);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reset.getTimeInMillis(), pi);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reset.getTimeInMillis(), pi);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // YARDIMCI METOTLAR
    // ──────────────────────────────────────────────────────────────────────────

    /** Ses URI'sini çözümle: null/boş → varsayılan alarm sesi */
    static Uri resolveSoundUri(String soundUriStr) {
        if (soundUriStr != null && !soundUriStr.isEmpty()) {
            return Uri.parse(soundUriStr);
        }
        Uri alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
        }
        return alarmUri;
    }

    static void scheduleNextCustomDayResetAlarm(Context context, String name, String time, int alarmId, int customDay, String soundUriStr) {
        if (time == null || !time.contains(":")) return;
        String[] parts = time.split(":");
        int hour, minute;
        try {
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return;
        }

        Calendar reset = Calendar.getInstance();
        reset.set(Calendar.HOUR_OF_DAY, hour);
        reset.set(Calendar.MINUTE, minute);
        reset.set(Calendar.SECOND, 0);
        reset.set(Calendar.MILLISECOND, 0);
        reset.set(Calendar.DAY_OF_WEEK, customDay);

        long resetMillis = reset.getTimeInMillis() - (60 * 60 * 1000); // 1 saat öncesi

        // Eğer bu reset saati (bugün için) geçmişse, 1 hafta ekle
        while (resetMillis <= System.currentTimeMillis()) {
            resetMillis += (7L * 24 * 60 * 60 * 1000);
        }

        reset.setTimeInMillis(resetMillis);

        int resetAlarmId = AlarmHelper.safeId(name + "_day" + customDay + "_" + time + "_reset");

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("MEDICINE_NAME", name);
        intent.putExtra("MEDICINE_TIME", time);
        intent.putExtra("ALARM_ID", alarmId);
        intent.putExtra("IS_CUSTOM_DAY", true);
        intent.putExtra("CUSTOM_DAY", customDay);
        intent.putExtra("SOUND_URI", soundUriStr);
        intent.putExtra(EXTRA_IS_RESET, true);

        PendingIntent pi = PendingIntent.getBroadcast(context, resetAlarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, resetMillis, pi);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, resetMillis, pi);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, resetMillis, pi);
            }
        }
    }
}