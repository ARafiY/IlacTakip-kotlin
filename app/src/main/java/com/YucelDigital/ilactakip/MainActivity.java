package com.YucelDigital.ilactakip;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements AlarmAdapter.OnMedicineInteractionListener {

    private static final int REQ_NOTIFICATION = 101;

    private ListView listView;
    private LinearLayout emptyStateLayout;
    private FloatingActionButton fab;
    private ArrayList<Medicine> medicineList;
    private AlarmAdapter adapter;

    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView         = findViewById(R.id.alarmListView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        fab              = findViewById(R.id.fabAddAlarm);

        loadData();

        adapter = new AlarmAdapter(this, medicineList, this);
        listView.setAdapter(adapter);
        checkEmptyState();

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        boolean isEditMode = data.getBooleanExtra("is_edit", false);
                        Medicine newMedicine = (Medicine) data.getSerializableExtra("new_medicine");

                        if (newMedicine != null) {
                            if (isEditMode) {
                                int editPosition = data.getIntExtra("edit_position", -1);
                                if (editPosition != -1) {
                                    cancelAlarm(medicineList.get(editPosition));
                                    medicineList.set(editPosition, newMedicine);
                                    Toast.makeText(this, "İlaç güncellendi ✓", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                medicineList.add(newMedicine);
                                Toast.makeText(this, "İlaç eklendi ✓", Toast.LENGTH_SHORT).show();
                            }
                            scheduleAlarm(newMedicine);
                            adapter.notifyDataSetChanged();
                            saveData();
                            checkEmptyState();
                        }
                    }
                });

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddMedicineActivity.class);
            launcher.launch(intent);
        });

        // İzinleri kontrol et
        requestAllPermissions();
    }

    // ══════════════════════════════════════════════════════════════
    //  İZİN YÖNETİMİ
    // ══════════════════════════════════════════════════════════════

    /** Tüm izinleri sırayla kontrol et ve iste */
    private void requestAllPermissions() {
        requestNotificationPermission();
    }

    // ── 1. Bildirim İzni (Android 13+) ──

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Daha önce reddedildiyse neden gerekli olduğunu açıkla
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Bildirim İzni Gerekli")
                            .setMessage("İlaç hatırlatıcıların çalışabilmesi için bildirim izni gereklidir. " +
                                    "Bu izin olmadan ilaç saatlerinizde uyarı alamazsınız.")
                            .setPositiveButton("İzin Ver", (d, w) ->
                                    ActivityCompat.requestPermissions(this,
                                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION))
                            .setNegativeButton("Daha Sonra", (d, w) -> checkAlarmPermission())
                            .setCancelable(false)
                            .show();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
                }
            } else {
                checkAlarmPermission();
            }
        } else {
            checkAlarmPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // İzin verildi, sonraki izne geç
                checkAlarmPermission();
            } else {
                // İzin reddedildi
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.POST_NOTIFICATIONS)) {
                    // "Tekrar sorma" seçildi → kullanıcıyı ayarlara yönlendir
                    new AlertDialog.Builder(this)
                            .setTitle("Bildirim İzni Kapalı")
                            .setMessage("İlaç hatırlatıcılar bildirim gönderemez. " +
                                    "Lütfen uygulama ayarlarından bildirim iznini açın.")
                            .setPositiveButton("Ayarlara Git", (d, w) -> openAppSettings())
                            .setNegativeButton("Kapat", null)
                            .show();
                } else {
                    Toast.makeText(this,
                            "⚠ Bildirim izni olmadan hatırlatıcılar çalışmaz",
                            Toast.LENGTH_LONG).show();
                }
                checkAlarmPermission();
            }
        }
    }

    // ── 2. Kesin Alarm İzni (Android 12+) ──

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Alarm İzni Gerekli")
                        .setMessage("İlaç saatlerinizde kesin alarm kurabilmek için bu izin gereklidir. " +
                                "İzin vermezseniz alarmlar birkaç dakika gecikebilir.")
                        .setPositiveButton("İzin Ver", (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Daha Sonra", (d, w) -> checkFullScreenIntentPermission())
                        .setCancelable(false)
                        .show();
                return;
            }
        }
        checkFullScreenIntentPermission();
    }

    // ── 3. Tam Ekran Alarm İzni (Android 14+) ──

    private void checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && !nm.canUseFullScreenIntent()) {
                new AlertDialog.Builder(this)
                        .setTitle("Kilit Ekranı İzni")
                        .setMessage("İlaç alarmının kilit ekranında tam ekran görünmesi için bu izin gereklidir. " +
                                "İzin vermezseniz alarm sadece bildirim olarak görünür.")
                        .setPositiveButton("İzin Ver", (d, w) -> {
                            Intent intent = new Intent(
                                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Daha Sonra", null)
                        .setCancelable(false)
                        .show();
            }
        }
    }

    /** Uygulama ayarları sayfasını aç (izin "tekrar sorma" seçildiyse) */
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    // ══════════════════════════════════════════════════════════════
    //  İLAÇ YÖNETİMİ
    // ══════════════════════════════════════════════════════════════

    public void openEditPage(Medicine medicine, int position) {
        Intent intent = new Intent(MainActivity.this, AddMedicineActivity.class);
        intent.putExtra("edit_medicine", medicine);
        intent.putExtra("edit_position", position);
        launcher.launch(intent);
    }

    private void cancelAlarm(Medicine medicine) {
        AlarmHelper.cancelAlarm(this, medicine);
    }

    public void saveData() {
        MedicineRepository.saveMedicineList(this, medicineList);
    }

    private void loadData() {
        medicineList = new ArrayList<>(MedicineRepository.loadMedicineList(this));
        sortMedicineList();
    }

    public void checkEmptyState() {
        if (medicineList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    public void scheduleAlarm(Medicine medicine) {
        if (medicine.isUseCustomDays() && medicine.getCustomDayTimes() != null) {
            scheduleCustomDayAlarm(medicine);
        } else {
            scheduleStandardAlarm(medicine);
        }
    }

    private void scheduleStandardAlarm(Medicine medicine) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String[] timeArray = medicine.getTime().split(", ");

        for (String singleTime : timeArray) {
            singleTime = singleTime.trim();

            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("MEDICINE_NAME",  medicine.getName());
            intent.putExtra("MEDICINE_TIME",  singleTime);
            intent.putExtra("MEDICINE_NOTE",  medicine.getNote());
            intent.putExtra("START_DATE",     medicine.getStartDate());
            intent.putExtra("END_DATE",       medicine.getEndDate());
            intent.putExtra("INTERVAL_DAYS",  medicine.getIntervalDays());
            intent.putExtra("SOUND_URI",      medicine.getSoundUri());
            intent.putExtra("IS_CUSTOM_DAY",  false);
            intent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false);

            int alarmId = Math.abs((medicine.getName() + singleTime).hashCode());
            intent.putExtra("ALARM_ID", alarmId);

            PendingIntent pi = PendingIntent.getBroadcast(
                    this, alarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String[] timeParts = singleTime.split(":");
            int hour   = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            setExactAlarm(alarmManager, calendar.getTimeInMillis(), pi);

            AlarmReceiver.scheduleNextResetAlarm(
                    this,
                    medicine.getName(), singleTime, alarmId,
                    medicine.getStartDate(), medicine.getEndDate(),
                    medicine.getIntervalDays(), medicine.getSoundUri()
            );
        }
    }

    private void scheduleCustomDayAlarm(Medicine medicine) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        java.util.HashMap<Integer, String> dayTimes = medicine.getCustomDayTimes();
        if (dayTimes == null) return;

        for (java.util.Map.Entry<Integer, String> entry : dayTimes.entrySet()) {
            int calDay = entry.getKey();
            String[] times = entry.getValue().split(", ");

            for (String singleTime : times) {
                singleTime = singleTime.trim();

                Intent intent = new Intent(this, AlarmReceiver.class);
                intent.putExtra("MEDICINE_NAME",  medicine.getName());
                intent.putExtra("MEDICINE_TIME",  singleTime);
                intent.putExtra("MEDICINE_NOTE",  medicine.getNote());
                intent.putExtra("START_DATE",     0L);
                intent.putExtra("END_DATE",       0L);
                intent.putExtra("INTERVAL_DAYS",  1);
                intent.putExtra("SOUND_URI",      medicine.getSoundUri());
                intent.putExtra("IS_CUSTOM_DAY",  true);
                intent.putExtra("CUSTOM_DAY",     calDay);
                intent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false);

                // Benzersiz alarm ID: isim + gün + saat
                int alarmId = Math.abs((medicine.getName() + "_day" + calDay + "_" + singleTime).hashCode());
                intent.putExtra("ALARM_ID", alarmId);

                PendingIntent pi = PendingIntent.getBroadcast(
                        this, alarmId, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                String[] timeParts = singleTime.split(":");
                int hour   = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                // Sonraki uygun günü hesapla
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                calendar.set(Calendar.DAY_OF_WEEK, calDay);

                // Eğer bu haftanın o günü geçmişse, gelecek haftaya al
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                }

                setExactAlarm(alarmManager, calendar.getTimeInMillis(), pi);

                AlarmReceiver.scheduleNextCustomDayResetAlarm(
                        this,
                        medicine.getName(), singleTime, alarmId,
                        calDay, medicine.getSoundUri()
                );
            }
        }
    }

    /** Kesin alarm kur — API seviyesine göre uygun yöntemi seçer */
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

    private void sortMedicineList() {
        if (medicineList != null) {
            java.util.Collections.sort(medicineList, (m1, m2) -> {
                String t1 = m1.getTime() != null ? m1.getTime() : "";
                String t2 = m2.getTime() != null ? m2.getTime() : "";
                return t1.compareTo(t2);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        adapter.updateData(medicineList);
        checkEmptyState();
    }

    // --- OnMedicineInteractionListener Methods ---
    @Override
    public void onScheduleAlarm(Medicine medicine) {
        scheduleAlarm(medicine);
    }

    @Override
    public void onSaveData() {
        saveData();
    }

    @Override
    public void onCheckEmptyState() {
        checkEmptyState();
    }

    @Override
    public void onOpenEditPage(Medicine medicine, int position) {
        openEditPage(medicine, position);
    }
}