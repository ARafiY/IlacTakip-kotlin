package com.YucelDigital.ilactakip;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Calendar;

public class AlarmActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private final android.os.Handler timeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private android.content.BroadcastReceiver screenOffReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                stopAlarmSound();
            }
        }
    };
    private static final long ALARM_TIMEOUT_MS = 5 * 60 * 1000; // 5 dakika
    private TextView nameText, timeText, noteText;
    private MaterialButton stopButton, snoozeButton;
    private String medicineName;
    private String medicineTime;
    private String medicineNote;
    private int notificationId;
    private long startDate;
    private long endDate;
    private int intervalDays;
    private String soundUriStr;
    private boolean isCustomDay;
    private int customDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        android.content.IntentFilter filter = new android.content.IntentFilter(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenOffReceiver, filter);
        }

        // Kilit Ekranında Açılma
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            // Kilidi aktif olarak kaldır (Android 8.1+)
            android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        // Ekranın hemen tekrar kapanmasını önle
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Intent Verilerini Al
        medicineName = getIntent().getStringExtra("MEDICINE_NAME");
        medicineTime = getIntent().getStringExtra("MEDICINE_TIME");
        medicineNote = getIntent().getStringExtra("MEDICINE_NOTE");
        notificationId = getIntent().getIntExtra("ALARM_ID", 0);
        startDate = getIntent().getLongExtra("START_DATE", 0);
        endDate = getIntent().getLongExtra("END_DATE", 0);
        intervalDays = getIntent().getIntExtra("INTERVAL_DAYS", 1);
        soundUriStr = getIntent().getStringExtra("SOUND_URI");
        isCustomDay = getIntent().getBooleanExtra("IS_CUSTOM_DAY", false);
        customDay = getIntent().getIntExtra("CUSTOM_DAY", 0);

        // View'ları Bağla
        nameText = findViewById(R.id.medicineNameTextView);
        timeText = findViewById(R.id.timeTextView);
        noteText = findViewById(R.id.noteTextView);
        stopButton = findViewById(R.id.stopButton);
        snoozeButton = findViewById(R.id.snoozeButton);

        if (nameText != null)
            nameText.setText(medicineName != null ? medicineName : "İlaç Zamanı");
        if (timeText != null)
            timeText.setText(medicineTime);
        if (noteText != null)
            noteText.setText(medicineNote != null && !medicineNote.isEmpty() ? "Not: " + medicineNote : "");

        // Özel Sesi Başlat
        playAlarmSound();

        // DURDUR
        if (stopButton != null) {
            stopButton.setOnClickListener(v -> {
                timeoutHandler.removeCallbacksAndMessages(null);
                stopAlarmSound();
                clearNotification();
                markAsTaken();
                finish();
            });
        }

        // ERTELE — Rastgele Süre
        if (snoozeButton != null) {
            snoozeButton.setOnClickListener(v -> handleRandomSnooze());
        }

        // 5 dakika sonra otomatik ertele (kullanıcı cevap vermezse)
        timeoutHandler.postDelayed(() -> {
            stopAlarmSound();
            clearNotification();
            snoozeAlarm(5);
            finish();
        }, ALARM_TIMEOUT_MS);
    }

    private void clearNotification() {
        android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(notificationId);
        }
    }

    private void handleRandomSnooze() {
        timeoutHandler.removeCallbacksAndMessages(null);
        stopAlarmSound();
        int[] possibleMinutes = {5, 10, 15, 20, 25, 30, 35, 40, 45};
        int randomMinute = possibleMinutes[new java.util.Random().nextInt(possibleMinutes.length)];
        clearNotification();
        snoozeAlarm(randomMinute);
        finish();
    }

    private void markAsTaken() {
        MedicineRepository.markAsTaken(this, medicineName, medicineTime);
    }

    private void playAlarmSound() {
        try {
            Uri soundUri = AlarmReceiver.resolveSoundUri(soundUriStr);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getApplicationContext(), soundUri);
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
            mediaPlayer.setLooping(true); // Ses tekrar tekrar çalar
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }

    private void stopAlarmSound() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /** Alarmı belirli dakika sonraya ertele, tarih metadata'sını koruyarak */
    private void snoozeAlarm(int minutes) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null)
            return;

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("MEDICINE_NAME", medicineName);
        intent.putExtra("MEDICINE_TIME", medicineTime);
        intent.putExtra("MEDICINE_NOTE", medicineNote);
        intent.putExtra("ALARM_ID", notificationId);
        intent.putExtra("START_DATE", startDate);
        intent.putExtra("END_DATE", endDate);
        intent.putExtra("INTERVAL_DAYS", intervalDays);
        intent.putExtra("SOUND_URI", soundUriStr);
        intent.putExtra("IS_CUSTOM_DAY", isCustomDay);
        intent.putExtra("CUSTOM_DAY", customDay);

        PendingIntent pi = PendingIntent.getBroadcast(
                this, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minutes);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
        Toast.makeText(this, minutes + " dakika ertelendi", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeoutHandler.removeCallbacksAndMessages(null);
        stopAlarmSound();
        try {
            unregisterReceiver(screenOffReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_MUTE) {
            stopAlarmSound();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}