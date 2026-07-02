package com.YucelDigital.ilactakip;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddMedicineActivity extends AppCompatActivity {

    private long selectedStartDate = 0;
    private long selectedEndDate   = 0;
    private String selectedSoundUri = null;

    private com.google.android.material.textfield.TextInputEditText nameInput, noteInput;
    private TimePicker           timePicker;
    private MaterialButton       btnDateRange, btnSetAlarm, btnSelectSound;
    private TextView             tvSelectedDate, tvSelectedSound;
    private AutoCompleteTextView frequencyDropdown, customFrequencyDropdown;
    private ImageView            btnBack;

    // Mod toggle
    private MaterialButtonToggleGroup toggleScheduleMode;
    private View standardModeCard, customDayModeCard;
    private boolean isCustomDayMode = false;

    // Güne özel: Chip → Calendar gün sabiti eşlemesi
    private final LinkedHashMap<Integer, Integer> chipToDayMap = new LinkedHashMap<>();
    // Calendar gün sabiti → seçili saat
    private final HashMap<Integer, String> customDayTimes = new HashMap<>();
    private LinearLayout customDayTimesList;

    private boolean isEditMode   = false;
    private int     editPosition = -1;
    private boolean isLoadingEditData = false; // Edit yükleme sırasında dialog'ları engelle

    private ActivityResultLauncher<Intent> soundPickerLauncher;

    // Gün isimleri (Calendar sabiti → Türkçe)
    private static final String[] DAY_NAMES = {
            "", "Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        // View bağlantıları
        nameInput         = findViewById(R.id.medicineNameEditText);
        noteInput         = findViewById(R.id.medicineNoteEditText);
        timePicker        = findViewById(R.id.timePicker);
        btnDateRange      = findViewById(R.id.btnSelectDateRange);
        btnSetAlarm       = findViewById(R.id.setAlarmButton);
        tvSelectedDate    = findViewById(R.id.tvSelectedDateRange);
        frequencyDropdown = findViewById(R.id.frequencyDropdown);
        btnSelectSound    = findViewById(R.id.btnSelectSound);
        tvSelectedSound   = findViewById(R.id.tvSelectedSound);
        btnBack           = findViewById(R.id.btnBack);

        // Güne özel mod view'ları
        toggleScheduleMode    = findViewById(R.id.toggleScheduleMode);
        standardModeCard      = findViewById(R.id.standardModeCard);
        customDayModeCard     = findViewById(R.id.customDayModeCard);
        customDayTimesList    = findViewById(R.id.customDayTimesList);
        customFrequencyDropdown = findViewById(R.id.customFrequencyDropdown);

        // Chip → Calendar gün sabiti eşlemesi
        chipToDayMap.put(R.id.chipMonday,    Calendar.MONDAY);
        chipToDayMap.put(R.id.chipTuesday,   Calendar.TUESDAY);
        chipToDayMap.put(R.id.chipWednesday, Calendar.WEDNESDAY);
        chipToDayMap.put(R.id.chipThursday,  Calendar.THURSDAY);
        chipToDayMap.put(R.id.chipFriday,    Calendar.FRIDAY);
        chipToDayMap.put(R.id.chipSaturday,  Calendar.SATURDAY);
        chipToDayMap.put(R.id.chipSunday,    Calendar.SUNDAY);

        // Frekans dropdown
        String[] frequencies = {
                "Günde 1 defa",
                "6 Saatte Bir",
                "8 Saatte Bir",
                "12 Saatte Bir",
                "2 Günde Bir",
                "3 Günde Bir",
                "4 Günde Bir",
                "5 Günde Bir",
                "6 Günde Bir",
                "Haftada 1 Defa"
        };

        // Güne özel modda sadece saat bazlı frekanslar mantıklı
        String[] customFrequencies = {
                "Günde 1 defa",
                "6 Saatte Bir",
                "8 Saatte Bir",
                "12 Saatte Bir"
        };

        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, frequencies);
        frequencyDropdown.setAdapter(freqAdapter);

        ArrayAdapter<String> customFreqAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, customFrequencies);
        customFrequencyDropdown.setAdapter(customFreqAdapter);

        timePicker.setIs24HourView(true);

        // Back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // ── Mod Toggle ──
        setupModeToggle();

        // ── Chip Listeners ──
        setupDayChips();

        // Sound picker launcher
        soundPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        if (uri != null) {
                            selectedSoundUri = uri.toString();
                            String title = RingtoneManager.getRingtone(this, uri).getTitle(this);
                            if (tvSelectedSound != null) tvSelectedSound.setText("🎵 " + title);
                        } else {
                            selectedSoundUri = null;
                            if (tvSelectedSound != null) tvSelectedSound.setText("🔔 Varsayılan Alarm Sesi");
                        }
                    }
                });

        if (btnSelectSound != null) {
            btnSelectSound.setOnClickListener(v -> openSoundPicker());
        }

        // Edit mode
        if (getIntent().hasExtra("edit_medicine")) {
            isEditMode   = true;
            editPosition = getIntent().getIntExtra("edit_position", -1);
            Medicine medicine = (Medicine) getIntent().getSerializableExtra("edit_medicine");

            if (medicine != null) {
                loadEditData(medicine);
            }
        }

        btnDateRange.setOnClickListener(v -> showDateRangePicker());
        btnSetAlarm.setOnClickListener(v -> saveAlarm());
    }

    // ══════════════════════════════════════════════════════════════
    //  MOD TOGGLE
    // ══════════════════════════════════════════════════════════════

    private void setupModeToggle() {
        toggleScheduleMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnModeStandard) {
                isCustomDayMode = false;
                standardModeCard.setVisibility(View.VISIBLE);
                customDayModeCard.setVisibility(View.GONE);
                // Standart modda tarih aralığı göster
                btnDateRange.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.btnModeCustomDay) {
                isCustomDayMode = true;
                standardModeCard.setVisibility(View.GONE);
                customDayModeCard.setVisibility(View.VISIBLE);
                // Güne özel modda tarih aralığı gizle (süresiz çalar)
                btnDateRange.setVisibility(View.GONE);
                if (tvSelectedDate != null) tvSelectedDate.setVisibility(View.GONE);
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  GÜNE ÖZEL: CHIP VE SAAT SEÇİMİ
    // ══════════════════════════════════════════════════════════════

    private void setupDayChips() {
        for (Map.Entry<Integer, Integer> entry : chipToDayMap.entrySet()) {
            Chip chip = findViewById(entry.getKey());
            int calDay = entry.getValue();

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isLoadingEditData) return; // Edit yüklerken dialog açma
                if (isChecked) {
                    showTimePickerForDay(calDay);
                } else {
                    customDayTimes.remove(calDay);
                    refreshCustomDayTimesList();
                }
            });
        }
    }

    private void showTimePickerForDay(int calendarDay) {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    customDayTimes.put(calendarDay, time);
                    refreshCustomDayTimesList();
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        ).show();
    }

    /** Seçili günlerin listesini güncelle */
    private void refreshCustomDayTimesList() {
        customDayTimesList.removeAllViews();

        // Sıralı göster: Pzt, Sal, Çar, ...
        int[] dayOrder = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};

        for (int day : dayOrder) {
            if (!customDayTimes.containsKey(day)) continue;

            String time = customDayTimes.get(day);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView dayText = new TextView(this);
            dayText.setText(DAY_NAMES[day]);
            dayText.setTextSize(16);
            dayText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            dayText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView timeText = new TextView(this);
            timeText.setText(time);
            timeText.setTextSize(16);
            timeText.setTextColor(ContextCompat.getColor(this, R.color.primary));
            timeText.setPadding(16, 0, 16, 0);

            MaterialButton editBtn = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            editBtn.setText("Düzenle");
            editBtn.setTextSize(12);
            editBtn.setAllCaps(false);
            editBtn.setOnClickListener(v -> showTimePickerForDay(day));

            row.addView(dayText);
            row.addView(timeText);
            row.addView(editBtn);
            customDayTimesList.addView(row);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  EDIT MODE
    // ══════════════════════════════════════════════════════════════

    private void loadEditData(Medicine medicine) {
        if (nameInput != null) nameInput.setText(medicine.getName());
        if (noteInput != null) noteInput.setText(medicine.getNote());

        // Güne özel mod kontrolü
        if (medicine.isUseCustomDays() && medicine.getCustomDayTimes() != null) {
            isCustomDayMode = true;

            // Günleri ve saatleri yükle
            customDayTimes.clear();
            customDayTimes.putAll(medicine.getCustomDayTimes());

            // Dialog'ları engelle — toggle ve chip tetiklemelerinden önce ayarla
            isLoadingEditData = true;
            toggleScheduleMode.check(R.id.btnModeCustomDay);

            // Chip'leri işaretle (dialog açmadan)
            for (Map.Entry<Integer, Integer> entry : chipToDayMap.entrySet()) {
                Chip chip = findViewById(entry.getKey());
                int calDay = entry.getValue();
                chip.setChecked(customDayTimes.containsKey(calDay));
            }
            refreshCustomDayTimesList();

            // Post ile flag'i kaldır — chip listener'ların async callback'lerini yakala
            findViewById(android.R.id.content).post(() -> isLoadingEditData = false);

            // Frekans
            String currentFreq = resolveFrequencyText(medicine);
            if (customFrequencyDropdown != null) customFrequencyDropdown.setText(currentFreq, false);

        } else {
            // Standart mod
            toggleScheduleMode.check(R.id.btnModeStandard);

            // Frequency
            String currentFreq = resolveFrequencyText(medicine);
            View frequencyLayout = findViewById(R.id.frequencyInputLayout);
            if (frequencyLayout != null) frequencyLayout.setVisibility(View.VISIBLE);
            if (frequencyDropdown != null) frequencyDropdown.setText(currentFreq, false);

            // Time picker
            String firstTime = medicine.getTime();
            if (firstTime != null && firstTime.contains(",")) {
                firstTime = firstTime.split(",")[0].trim();
            }
            if (firstTime != null && firstTime.contains(":")) {
                try {
                    String[] parts = firstTime.split(":");
                    int hour   = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        timePicker.setHour(hour);
                        timePicker.setMinute(minute);
                    } else {
                        timePicker.setCurrentHour(hour);
                        timePicker.setCurrentMinute(minute);
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Dates
            selectedStartDate = medicine.getStartDate();
            selectedEndDate   = medicine.getEndDate();
            if (medicine.getDateRange() != null && !medicine.getDateRange().isEmpty()) {
                selectedDateRangeString = medicine.getDateRange();
                if (tvSelectedDate != null) {
                    tvSelectedDate.setVisibility(View.VISIBLE);
                    tvSelectedDate.setText(selectedDateRangeString);
                }
            }
        }

        // Sound (her iki modda da aynı)
        selectedSoundUri = medicine.getSoundUri();
        if (tvSelectedSound != null) {
            if (selectedSoundUri != null) {
                try {
                    String title = RingtoneManager.getRingtone(this, Uri.parse(selectedSoundUri)).getTitle(this);
                    tvSelectedSound.setText("🎵 " + title);
                } catch (Exception e) {
                    tvSelectedSound.setText("🔔 Varsayılan Alarm Sesi");
                }
            } else {
                tvSelectedSound.setText("🔔 Varsayılan Alarm Sesi");
            }
        }

        // Title and button
        TextView titleText = findViewById(R.id.titleTextView);
        if (titleText != null) titleText.setText("İlacı Düzenle");
        if (btnSetAlarm != null) btnSetAlarm.setText("Güncelle");
    }

    private String resolveFrequencyText(Medicine medicine) {
        String currentFreq = "Günde 1 defa";
        int interval = medicine.getIntervalDays();
        String originalTimeString = medicine.getTime();

        if (interval > 1) {
            if (interval == 7) currentFreq = "Haftada 1 Defa";
            else currentFreq = interval + " Günde Bir";
        } else if (originalTimeString != null && originalTimeString.contains(",")) {
            int count = originalTimeString.split(",").length;
            if (count == 2) currentFreq = "12 Saatte Bir";
            else if (count == 3) currentFreq = "8 Saatte Bir";
            else if (count == 4) currentFreq = "6 Saatte Bir";
        }
        return currentFreq;
    }

    // ══════════════════════════════════════════════════════════════
    //  KAYDETME
    // ══════════════════════════════════════════════════════════════

    private String selectedDateRangeString = "";

    private void openSoundPicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_ALARM | RingtoneManager.TYPE_NOTIFICATION | RingtoneManager.TYPE_RINGTONE);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Bildirim Sesini Seç");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        if (selectedSoundUri != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedSoundUri));
        }
        soundPickerLauncher.launch(intent);
    }

    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("İlaç Kullanım Aralığı")
                        .build();

        dateRangePicker.show(getSupportFragmentManager(), "DATE_PICKER");

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            selectedDateRangeString = dateRangePicker.getHeaderText();
            tvSelectedDate.setVisibility(View.VISIBLE);
            tvSelectedDate.setText(selectedDateRangeString);

            if (selection.first != null && selection.second != null) {
                selectedStartDate = selection.first;
                selectedEndDate   = selection.second;
            }
        });
    }

    private void saveAlarm() {
        String name = nameInput.getText().toString().trim();
        String note = noteInput.getText().toString().trim();

        if (name.isEmpty()) {
            nameInput.setError("İlaç adı gerekli");
            return;
        }

        // Aynı isimde ilaç kontrolü
        if (isDuplicateName(name)) {
            nameInput.setError("Bu isimde bir ilaç zaten mevcut");
            Toast.makeText(this, "Aynı isimde ilaç eklenemez", Toast.LENGTH_SHORT).show();
            return;
        }

        Medicine med;

        if (isCustomDayMode) {
            med = saveCustomDayAlarm(name, note);
        } else {
            med = saveStandardAlarm(name, note);
        }

        if (med == null) return; // Validasyon hatası

        med.setSoundUri(selectedSoundUri);

        if (isEditMode) {
            Medicine oldMedicine = (Medicine) getIntent().getSerializableExtra("edit_medicine");
            if (oldMedicine != null) med.setTaken(oldMedicine.isTaken());
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("is_edit",      isEditMode);
        resultIntent.putExtra("new_medicine", med);
        if (isEditMode) resultIntent.putExtra("edit_position", editPosition);

        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    /** Standart mod kaydetme (mevcut mantık) */
    private Medicine saveStandardAlarm(String name, String note) {
        int hour, minute;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hour   = timePicker.getHour();
            minute = timePicker.getMinute();
        } else {
            hour   = timePicker.getCurrentHour();
            minute = timePicker.getCurrentMinute();
        }

        int freqCount   = 1;
        int intervalDays = 1;
        String freqText  = frequencyDropdown.getText().toString();

        if      (freqText.equals("6 Saatte Bir"))   freqCount   = 4;
        else if (freqText.equals("8 Saatte Bir"))   freqCount   = 3;
        else if (freqText.equals("12 Saatte Bir"))  freqCount   = 2;
        else if (freqText.equals("2 Günde Bir"))    intervalDays = 2;
        else if (freqText.equals("3 Günde Bir"))    intervalDays = 3;
        else if (freqText.equals("4 Günde Bir"))    intervalDays = 4;
        else if (freqText.equals("5 Günde Bir"))    intervalDays = 5;
        else if (freqText.equals("6 Günde Bir"))    intervalDays = 6;
        else if (freqText.equals("Haftada 1 Defa")) intervalDays = 7;

        StringBuilder timesBuilder = new StringBuilder();
        for (int i = 0; i < freqCount; i++) {
            int currentHour = (hour + (i * (24 / freqCount))) % 24;
            if (i > 0) timesBuilder.append(", ");
            timesBuilder.append(String.format(Locale.getDefault(), "%02d:%02d", currentHour, minute));
        }
        String finalTimes = timesBuilder.toString();

        long finalStartDate = selectedStartDate;
        if (finalStartDate == 0) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            finalStartDate = c.getTimeInMillis();
        }

        Medicine med = new Medicine(name, finalTimes, selectedDateRangeString, note);
        med.setStartDate(finalStartDate);
        med.setEndDate(selectedEndDate);
        med.setIntervalDays(intervalDays);
        med.setUseCustomDays(false);
        return med;
    }

    /** Güne özel mod kaydetme */
    private Medicine saveCustomDayAlarm(String name, String note) {
        if (customDayTimes.isEmpty()) {
            Toast.makeText(this, "En az bir gün seçmelisiniz", Toast.LENGTH_SHORT).show();
            return null;
        }

        // Frekans hesapla
        int freqCount = 1;
        String freqText = customFrequencyDropdown.getText().toString();
        if      (freqText.equals("6 Saatte Bir"))  freqCount = 4;
        else if (freqText.equals("8 Saatte Bir"))  freqCount = 3;
        else if (freqText.equals("12 Saatte Bir")) freqCount = 2;

        // Her gün için frekansa göre zaman listesi oluştur
        HashMap<Integer, String> expandedDayTimes = new HashMap<>();
        for (Map.Entry<Integer, String> entry : customDayTimes.entrySet()) {
            String baseTime = entry.getValue();
            String[] parts = baseTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            StringBuilder dayTimesBuilder = new StringBuilder();
            for (int i = 0; i < freqCount; i++) {
                int currentHour = (hour + (i * (24 / freqCount))) % 24;
                if (i > 0) dayTimesBuilder.append(", ");
                dayTimesBuilder.append(String.format(Locale.getDefault(), "%02d:%02d", currentHour, minute));
            }
            expandedDayTimes.put(entry.getKey(), dayTimesBuilder.toString());
        }

        // Gösterim amaçlı time string oluştur (ilk günün saati)
        String displayTime = customDayTimes.values().iterator().next();

        Medicine med = new Medicine(name, displayTime, "", note);
        med.setUseCustomDays(true);
        med.setCustomDayTimes(expandedDayTimes);
        med.setIntervalDays(1);
        med.setStartDate(0);
        med.setEndDate(0);
        return med;
    }

    /** Aynı isimde ilaç var mı kontrol eder */
    private boolean isDuplicateName(String name) {
        List<Medicine> list = MedicineRepository.loadMedicineList(this);
        if (list == null) return false;

        Medicine editingMedicine = null;
        if (isEditMode) {
            editingMedicine = (Medicine) getIntent().getSerializableExtra("edit_medicine");
        }

        for (Medicine m : list) {
            if (m.getName().equalsIgnoreCase(name)) {
                if (editingMedicine != null && m.getName().equalsIgnoreCase(editingMedicine.getName())) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }
}