package com.YucelDigital.ilactakip

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import java.util.Locale

class AddMedicineActivity : AppCompatActivity() {

    private var selectedStartDate: Long = 0
    private var selectedEndDate: Long = 0
    private var selectedSoundUri: String? = null
    private var selectedDateRangeString: String = ""

    private lateinit var nameInput: TextInputEditText
    private lateinit var noteInput: TextInputEditText
    private lateinit var timePicker: TimePicker
    private lateinit var btnDateRange: MaterialButton
    private lateinit var btnSetAlarm: MaterialButton
    private var btnSelectSound: MaterialButton? = null
    private lateinit var tvSelectedDate: TextView
    private var tvSelectedSound: TextView? = null
    private lateinit var frequencyDropdown: AutoCompleteTextView
    private lateinit var customFrequencyDropdown: AutoCompleteTextView
    private var btnBack: ImageView? = null

    // Mod toggle
    private lateinit var toggleScheduleMode: MaterialButtonToggleGroup
    private lateinit var standardModeCard: View
    private lateinit var customDayModeCard: View
    private var isCustomDayMode = false

    // Güne özel: Chip → Calendar gün sabiti eşlemesi
    private val chipToDayMap = LinkedHashMap<Int, Int>()

    // Calendar gün sabiti → seçili saat
    private val customDayTimes = HashMap<Int, String>()
    private lateinit var customDayTimesList: LinearLayout

    private var isEditMode = false
    private var editPosition = -1
    private var isLoadingEditData = false // Edit yükleme sırasında dialog'ları engelle

    private lateinit var soundPickerLauncher: ActivityResultLauncher<Intent>

    companion object {
        // Gün isimleri (Calendar sabiti → Türkçe)
        private val DAY_NAMES = arrayOf(
            "", "Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        // View bağlantıları
        nameInput = findViewById(R.id.medicineNameEditText)
        noteInput = findViewById(R.id.medicineNoteEditText)
        timePicker = findViewById(R.id.timePicker)
        btnDateRange = findViewById(R.id.btnSelectDateRange)
        btnSetAlarm = findViewById(R.id.setAlarmButton)
        tvSelectedDate = findViewById(R.id.tvSelectedDateRange)
        frequencyDropdown = findViewById(R.id.frequencyDropdown)
        btnSelectSound = findViewById(R.id.btnSelectSound)
        tvSelectedSound = findViewById(R.id.tvSelectedSound)
        btnBack = findViewById(R.id.btnBack)

        // Güne özel mod view'ları
        toggleScheduleMode = findViewById(R.id.toggleScheduleMode)
        standardModeCard = findViewById(R.id.standardModeCard)
        customDayModeCard = findViewById(R.id.customDayModeCard)
        customDayTimesList = findViewById(R.id.customDayTimesList)
        customFrequencyDropdown = findViewById(R.id.customFrequencyDropdown)

        // Chip → Calendar gün sabiti eşlemesi
        chipToDayMap[R.id.chipMonday] = Calendar.MONDAY
        chipToDayMap[R.id.chipTuesday] = Calendar.TUESDAY
        chipToDayMap[R.id.chipWednesday] = Calendar.WEDNESDAY
        chipToDayMap[R.id.chipThursday] = Calendar.THURSDAY
        chipToDayMap[R.id.chipFriday] = Calendar.FRIDAY
        chipToDayMap[R.id.chipSaturday] = Calendar.SATURDAY
        chipToDayMap[R.id.chipSunday] = Calendar.SUNDAY

        // Frekans dropdown
        val frequencies = arrayOf(
            "Günde 1 defa",
            "6 Saatte Bir",
            "8 Saatte Bir",
            "12 Saatte Bir",
            "2 Günde Bir",
            "3 Günde Bir",
            "4 Günde Bir",
            "5 Günde Bir",
            "6 Günde Bir",
            "Haftada 1 Defa",
        )

        // Güne özel modda sadece saat bazlı frekanslar mantıklı
        val customFrequencies = arrayOf(
            "Günde 1 defa",
            "6 Saatte Bir",
            "8 Saatte Bir",
            "12 Saatte Bir",
        )

        val freqAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, frequencies)
        frequencyDropdown.setAdapter(freqAdapter)

        val customFreqAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, customFrequencies)
        customFrequencyDropdown.setAdapter(customFreqAdapter)

        timePicker.setIs24HourView(true)

        // Back button
        btnBack?.setOnClickListener { finish() }

        // ── Mod Toggle ──
        setupModeToggle()

        // ── Chip Listeners ──
        setupDayChips()

        // Sound picker launcher
        soundPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri: Uri? = result.data!!.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (uri != null) {
                    selectedSoundUri = uri.toString()
                    val title = RingtoneManager.getRingtone(this, uri).getTitle(this)
                    tvSelectedSound?.text = "🎵 $title"
                } else {
                    selectedSoundUri = null
                    tvSelectedSound?.text = "🔔 Varsayılan Alarm Sesi"
                }
            }
        }

        btnSelectSound?.setOnClickListener { openSoundPicker() }

        // Edit mode
        if (intent.hasExtra("edit_medicine")) {
            isEditMode = true
            editPosition = intent.getIntExtra("edit_position", -1)
            val medicine = intent.getSerializableExtra("edit_medicine") as? Medicine

            if (medicine != null) {
                loadEditData(medicine)
            }
        }

        btnDateRange.setOnClickListener { showDateRangePicker() }
        btnSetAlarm.setOnClickListener { saveAlarm() }
    }

    // ══════════════════════════════════════════════════════════════
    //  MOD TOGGLE
    // ══════════════════════════════════════════════════════════════

    private fun setupModeToggle() {
        toggleScheduleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (checkedId == R.id.btnModeStandard) {
                isCustomDayMode = false
                standardModeCard.visibility = View.VISIBLE
                customDayModeCard.visibility = View.GONE
                // Standart modda tarih aralığı göster
                btnDateRange.visibility = View.VISIBLE
            } else if (checkedId == R.id.btnModeCustomDay) {
                isCustomDayMode = true
                standardModeCard.visibility = View.GONE
                customDayModeCard.visibility = View.VISIBLE
                // Güne özel modda tarih aralığı gizle (süresiz çalar)
                btnDateRange.visibility = View.GONE
                tvSelectedDate.visibility = View.GONE
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GÜNE ÖZEL: CHIP VE SAAT SEÇİMİ
    // ══════════════════════════════════════════════════════════════

    private fun setupDayChips() {
        for ((chipId, calDay) in chipToDayMap) {
            val chip = findViewById<Chip>(chipId)

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isLoadingEditData) return@setOnCheckedChangeListener // Edit yüklerken dialog açma
                if (isChecked) {
                    showTimePickerForDay(calDay)
                } else {
                    customDayTimes.remove(calDay)
                    refreshCustomDayTimesList()
                }
            }
        }
    }

    private fun showTimePickerForDay(calendarDay: Int) {
        val now = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                customDayTimes[calendarDay] = time
                refreshCustomDayTimesList()
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true,
        ).show()
    }

    /** Seçili günlerin listesini güncelle */
    private fun refreshCustomDayTimesList() {
        customDayTimesList.removeAllViews()

        // Sıralı göster: Pzt, Sal, Çar, ...
        val dayOrder = intArrayOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
        )

        for (day in dayOrder) {
            val time = customDayTimes[day] ?: continue

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, 8, 0, 8)

            val dayText = TextView(this)
            dayText.text = DAY_NAMES[day]
            dayText.textSize = 16f
            dayText.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            dayText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val timeText = TextView(this)
            timeText.text = time
            timeText.textSize = 16f
            timeText.setTextColor(ContextCompat.getColor(this, R.color.primary))
            timeText.setPadding(16, 0, 16, 0)

            val editBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
            editBtn.text = "Düzenle"
            editBtn.textSize = 12f
            editBtn.isAllCaps = false
            editBtn.setOnClickListener { showTimePickerForDay(day) }

            row.addView(dayText)
            row.addView(timeText)
            row.addView(editBtn)
            customDayTimesList.addView(row)
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  EDIT MODE
    // ══════════════════════════════════════════════════════════════

    private fun loadEditData(medicine: Medicine) {
        nameInput.setText(medicine.name)
        noteInput.setText(medicine.note)

        // Güne özel mod kontrolü
        val medicineCustomDayTimes = medicine.customDayTimes
        if (medicine.isUseCustomDays && medicineCustomDayTimes != null) {
            isCustomDayMode = true

            // Günleri ve saatleri yükle
            customDayTimes.clear()
            customDayTimes.putAll(medicineCustomDayTimes)

            // Dialog'ları engelle — toggle ve chip tetiklemelerinden önce ayarla
            isLoadingEditData = true
            toggleScheduleMode.check(R.id.btnModeCustomDay)

            // Chip'leri işaretle (dialog açmadan)
            for ((chipId, calDay) in chipToDayMap) {
                val chip = findViewById<Chip>(chipId)
                chip.isChecked = customDayTimes.containsKey(calDay)
            }
            refreshCustomDayTimesList()

            // Post ile flag'i kaldır — chip listener'ların async callback'lerini yakala
            findViewById<View>(android.R.id.content).post { isLoadingEditData = false }

            // Frekans
            val currentFreq = resolveFrequencyText(medicine)
            customFrequencyDropdown.setText(currentFreq, false)
        } else {
            // Standart mod
            toggleScheduleMode.check(R.id.btnModeStandard)

            // Frequency
            val currentFreq = resolveFrequencyText(medicine)
            findViewById<View>(R.id.frequencyInputLayout).visibility = View.VISIBLE
            frequencyDropdown.setText(currentFreq, false)

            // Time picker
            var firstTime = medicine.time
            if (firstTime != null && firstTime.contains(",")) {
                firstTime = firstTime.split(",")[0].trim()
            }
            if (firstTime != null && firstTime.contains(":")) {
                try {
                    val parts = firstTime.split(":")
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        timePicker.hour = hour
                        timePicker.minute = minute
                    } else {
                        @Suppress("DEPRECATION")
                        timePicker.currentHour = hour
                        @Suppress("DEPRECATION")
                        timePicker.currentMinute = minute
                    }
                } catch (ignored: NumberFormatException) {
                }
            }

            // Dates
            selectedStartDate = medicine.startDate
            selectedEndDate = medicine.endDate
            val dateRange = medicine.dateRange
            if (!dateRange.isNullOrEmpty()) {
                selectedDateRangeString = dateRange
                tvSelectedDate.visibility = View.VISIBLE
                tvSelectedDate.text = selectedDateRangeString
            }
        }

        // Sound (her iki modda da aynı)
        selectedSoundUri = medicine.soundUri
        val soundUri = selectedSoundUri
        if (soundUri != null) {
            try {
                val title = RingtoneManager.getRingtone(this, Uri.parse(soundUri)).getTitle(this)
                tvSelectedSound?.text = "🎵 $title"
            } catch (e: Exception) {
                tvSelectedSound?.text = "🔔 Varsayılan Alarm Sesi"
            }
        } else {
            tvSelectedSound?.text = "🔔 Varsayılan Alarm Sesi"
        }

        // Title and button
        findViewById<TextView>(R.id.titleTextView).text = "İlacı Düzenle"
        btnSetAlarm.text = "Güncelle"
    }

    private fun resolveFrequencyText(medicine: Medicine): String {
        var currentFreq = "Günde 1 defa"
        val interval = medicine.intervalDays
        val originalTimeString = medicine.time

        if (interval > 1) {
            currentFreq = if (interval == 7) "Haftada 1 Defa" else "$interval Günde Bir"
        } else if (originalTimeString != null && originalTimeString.contains(",")) {
            when (originalTimeString.split(",").size) {
                2 -> currentFreq = "12 Saatte Bir"
                3 -> currentFreq = "8 Saatte Bir"
                4 -> currentFreq = "6 Saatte Bir"
            }
        }
        return currentFreq
    }

    // ══════════════════════════════════════════════════════════════
    //  KAYDETME
    // ══════════════════════════════════════════════════════════════

    private fun openSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(
            RingtoneManager.EXTRA_RINGTONE_TYPE,
            RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE,
        )
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Bildirim Sesini Seç")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        val soundUri = selectedSoundUri
        if (soundUri != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundUri))
        }
        soundPickerLauncher.launch(intent)
    }

    private fun showDateRangePicker() {
        val dateRangePicker: MaterialDatePicker<androidx.core.util.Pair<Long, Long>> =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("İlaç Kullanım Aralığı")
                .build()

        dateRangePicker.show(supportFragmentManager, "DATE_PICKER")

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            selectedDateRangeString = dateRangePicker.headerText
            tvSelectedDate.visibility = View.VISIBLE
            tvSelectedDate.text = selectedDateRangeString

            if (selection.first != null && selection.second != null) {
                selectedStartDate = selection.first!!
                selectedEndDate = selection.second!!
            }
        }
    }

    private fun saveAlarm() {
        val name = nameInput.text.toString().trim()
        val note = noteInput.text.toString().trim()

        if (name.isEmpty()) {
            nameInput.error = "İlaç adı gerekli"
            return
        }

        // Aynı isimde ilaç kontrolü
        if (isDuplicateName(name)) {
            nameInput.error = "Bu isimde bir ilaç zaten mevcut"
            Toast.makeText(this, "Aynı isimde ilaç eklenemez", Toast.LENGTH_SHORT).show()
            return
        }

        val med = if (isCustomDayMode) {
            saveCustomDayAlarm(name, note)
        } else {
            saveStandardAlarm(name, note)
        } ?: return // Validasyon hatası

        med.soundUri = selectedSoundUri

        if (isEditMode) {
            val oldMedicine = intent.getSerializableExtra("edit_medicine") as? Medicine
            if (oldMedicine != null) med.isTaken = oldMedicine.isTaken
        }

        val resultIntent = Intent()
        resultIntent.putExtra("is_edit", isEditMode)
        resultIntent.putExtra("new_medicine", med)
        if (isEditMode) resultIntent.putExtra("edit_position", editPosition)

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    /** Standart mod kaydetme (mevcut mantık) */
    private fun saveStandardAlarm(name: String, note: String): Medicine {
        val hour: Int
        val minute: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hour = timePicker.hour
            minute = timePicker.minute
        } else {
            @Suppress("DEPRECATION")
            hour = timePicker.currentHour
            @Suppress("DEPRECATION")
            minute = timePicker.currentMinute
        }

        var freqCount = 1
        var intervalDays = 1
        val freqText = frequencyDropdown.text.toString()

        when (freqText) {
            "6 Saatte Bir" -> freqCount = 4
            "8 Saatte Bir" -> freqCount = 3
            "12 Saatte Bir" -> freqCount = 2
            "2 Günde Bir" -> intervalDays = 2
            "3 Günde Bir" -> intervalDays = 3
            "4 Günde Bir" -> intervalDays = 4
            "5 Günde Bir" -> intervalDays = 5
            "6 Günde Bir" -> intervalDays = 6
            "Haftada 1 Defa" -> intervalDays = 7
        }

        val timesBuilder = StringBuilder()
        for (i in 0 until freqCount) {
            val currentHour = (hour + (i * (24 / freqCount))) % 24
            if (i > 0) timesBuilder.append(", ")
            timesBuilder.append(String.format(Locale.getDefault(), "%02d:%02d", currentHour, minute))
        }
        val finalTimes = timesBuilder.toString()

        var finalStartDate = selectedStartDate
        if (finalStartDate == 0L) {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            finalStartDate = c.timeInMillis
        }

        val med = Medicine(name, finalTimes, selectedDateRangeString, note)
        med.startDate = finalStartDate
        med.endDate = selectedEndDate
        med.intervalDays = intervalDays
        med.isUseCustomDays = false
        return med
    }

    /** Güne özel mod kaydetme */
    private fun saveCustomDayAlarm(name: String, note: String): Medicine? {
        if (customDayTimes.isEmpty()) {
            Toast.makeText(this, "En az bir gün seçmelisiniz", Toast.LENGTH_SHORT).show()
            return null
        }

        // Frekans hesapla
        var freqCount = 1
        val freqText = customFrequencyDropdown.text.toString()
        when (freqText) {
            "6 Saatte Bir" -> freqCount = 4
            "8 Saatte Bir" -> freqCount = 3
            "12 Saatte Bir" -> freqCount = 2
        }

        // Her gün için frekansa göre zaman listesi oluştur
        val expandedDayTimes = HashMap<Int, String>()
        for ((day, baseTime) in customDayTimes) {
            val parts = baseTime.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            val dayTimesBuilder = StringBuilder()
            for (i in 0 until freqCount) {
                val currentHour = (hour + (i * (24 / freqCount))) % 24
                if (i > 0) dayTimesBuilder.append(", ")
                dayTimesBuilder.append(String.format(Locale.getDefault(), "%02d:%02d", currentHour, minute))
            }
            expandedDayTimes[day] = dayTimesBuilder.toString()
        }

        // Gösterim amaçlı time string oluştur (ilk günün saati)
        val displayTime = customDayTimes.values.iterator().next()

        val med = Medicine(name, displayTime, "", note)
        med.isUseCustomDays = true
        med.customDayTimes = expandedDayTimes
        med.intervalDays = 1
        med.startDate = 0
        med.endDate = 0
        return med
    }

    /** Aynı isimde ilaç var mı kontrol eder */
    private fun isDuplicateName(name: String): Boolean {
        val list = MedicineRepository.loadMedicineList(this)

        val editingMedicine: Medicine? = if (isEditMode) {
            intent.getSerializableExtra("edit_medicine") as? Medicine
        } else {
            null
        }

        for (m in list) {
            if (m.name.equals(name, ignoreCase = true)) {
                if (editingMedicine != null && m.name.equals(editingMedicine.name, ignoreCase = true)) {
                    continue
                }
                return true
            }
        }
        return false
    }
}
