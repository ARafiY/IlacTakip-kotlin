package com.YucelDigital.ilactakip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.YucelDigital.ilactakip.ui.theme.IlacTakipTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddMedicineActivity : AppCompatActivity() {

    private var isEditMode = false
    private var editPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val editingMedicine = intent.getSerializableExtra("edit_medicine") as? Medicine
        isEditMode = editingMedicine != null
        editPosition = intent.getIntExtra("edit_position", -1)

        setContent {
            IlacTakipTheme {
                val state = remember {
                    AddMedicineFormState().apply {
                        editingMedicine?.let { loadFrom(it, this@AddMedicineActivity) }
                    }
                }

                AddMedicineScreen(
                    state = state,
                    isEditMode = isEditMode,
                    onBack = { finish() },
                    onSave = { saveAlarm(state) },
                )
            }
        }
    }

    private fun saveAlarm(state: AddMedicineFormState) {
        val name = state.name.trim()
        val note = state.note.trim()

        if (name.isEmpty()) {
            state.nameError = "İlaç adı gerekli"
            return
        }

        // Aynı isimde ilaç kontrolü
        if (isDuplicateName(name)) {
            state.nameError = "Bu isimde bir ilaç zaten mevcut"
            Toast.makeText(this, "Aynı isimde ilaç eklenemez", Toast.LENGTH_SHORT).show()
            return
        }

        val med = if (state.isCustomDayMode) {
            saveCustomDayAlarm(state, name, note)
        } else {
            saveStandardAlarm(state, name, note)
        } ?: return // Validasyon hatası

        med.soundUri = state.soundUri

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
    private fun saveStandardAlarm(state: AddMedicineFormState, name: String, note: String): Medicine {
        val hour = state.hour
        val minute = state.minute

        var freqCount = 1
        var intervalDays = 1

        when (state.freqText) {
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

        var finalStartDate = state.selectedStartDate
        if (finalStartDate == 0L) {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            finalStartDate = c.timeInMillis
        }

        val med = Medicine(name, finalTimes, state.dateRangeText, note)
        med.startDate = finalStartDate
        med.endDate = state.selectedEndDate
        med.intervalDays = intervalDays
        med.isUseCustomDays = false
        return med
    }

    /** Güne özel mod kaydetme */
    private fun saveCustomDayAlarm(state: AddMedicineFormState, name: String, note: String): Medicine? {
        if (state.customDayTimes.isEmpty()) {
            Toast.makeText(this, "En az bir gün seçmelisiniz", Toast.LENGTH_SHORT).show()
            return null
        }

        // Frekans hesapla
        var freqCount = 1
        when (state.customFreqText) {
            "6 Saatte Bir" -> freqCount = 4
            "8 Saatte Bir" -> freqCount = 3
            "12 Saatte Bir" -> freqCount = 2
        }

        // Her gün için frekansa göre zaman listesi oluştur
        val expandedDayTimes = HashMap<Int, String>()
        for ((day, baseTime) in state.customDayTimes) {
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
        val displayTime = state.customDayTimes.values.iterator().next()

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

// ══════════════════════════════════════════════════════════════
//  FORM DURUMU
// ══════════════════════════════════════════════════════════════

/** Gün isimleri (Calendar sabiti → Türkçe) */
private val DAY_NAMES = arrayOf("", "Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi")

/** Chip sırası: Pzt → Paz, her biri Calendar gün sabitine eşleniyor */
private val DAY_CHIPS = listOf(
    Calendar.MONDAY to "Pzt",
    Calendar.TUESDAY to "Sal",
    Calendar.WEDNESDAY to "Çar",
    Calendar.THURSDAY to "Per",
    Calendar.FRIDAY to "Cum",
    Calendar.SATURDAY to "Cmt",
    Calendar.SUNDAY to "Paz",
)
private val DAY_ORDER = intArrayOf(
    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
    Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
)

/**
 * Medicine nesnesinden frekans metnini çözer (isim: eskiden AddMedicineActivity'nin özel metoduydu,
 * artık hem form state'inin edit-mode yüklemesi hem de saf mantık olarak burada duruyor).
 */
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

private fun formatDateRange(startMillis: Long, endMillis: Long): String {
    val fmt = SimpleDateFormat("d MMM", Locale.forLanguageTag("tr"))
    return "${fmt.format(Date(startMillis))} - ${fmt.format(Date(endMillis))}"
}

private fun buildSoundPickerIntent(currentSoundUri: String?): Intent {
    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
    intent.putExtra(
        RingtoneManager.EXTRA_RINGTONE_TYPE,
        RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE,
    )
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Bildirim Sesini Seç")
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
    if (currentSoundUri != null) {
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentSoundUri))
    }
    return intent
}

private class AddMedicineFormState {
    var name by mutableStateOf("")
    var note by mutableStateOf("")
    var nameError by mutableStateOf<String?>(null)

    var isCustomDayMode by mutableStateOf(false)

    var hour by mutableIntStateOf(8)
    var minute by mutableIntStateOf(0)
    var freqText by mutableStateOf(FREQUENCIES[0])

    val customDayTimes = mutableStateMapOf<Int, String>()
    var customFreqText by mutableStateOf(CUSTOM_FREQUENCIES[0])

    var selectedStartDate by mutableLongStateOf(0L)
    var selectedEndDate by mutableLongStateOf(0L)
    var dateRangeText by mutableStateOf("")

    var soundUri by mutableStateOf<String?>(null)
    var soundTitle by mutableStateOf("🔔 Varsayılan Alarm Sesi")

    var dayTimeDialogFor by mutableStateOf<Int?>(null)
    var showDateRangeDialog by mutableStateOf(false)

    /** Düzenleme modunda formu var olan bir ilacın verileriyle doldurur. */
    fun loadFrom(medicine: Medicine, context: Context) {
        name = medicine.name
        note = medicine.note ?: ""

        val medicineCustomDayTimes = medicine.customDayTimes
        if (medicine.isUseCustomDays && medicineCustomDayTimes != null) {
            isCustomDayMode = true
            customDayTimes.clear()
            customDayTimes.putAll(medicineCustomDayTimes)
            customFreqText = resolveFrequencyText(medicine)
        } else {
            isCustomDayMode = false
            freqText = resolveFrequencyText(medicine)

            var firstTime = medicine.time
            if (firstTime != null && firstTime.contains(",")) {
                firstTime = firstTime.split(",")[0].trim()
            }
            if (firstTime != null && firstTime.contains(":")) {
                try {
                    val parts = firstTime.split(":")
                    hour = parts[0].toInt()
                    minute = parts[1].toInt()
                } catch (ignored: NumberFormatException) {
                }
            }

            selectedStartDate = medicine.startDate
            selectedEndDate = medicine.endDate
            val dr = medicine.dateRange
            if (!dr.isNullOrEmpty()) dateRangeText = dr
        }

        soundUri = medicine.soundUri
        val uri = soundUri
        soundTitle = if (uri != null) {
            try {
                "🎵 " + RingtoneManager.getRingtone(context, Uri.parse(uri)).getTitle(context)
            } catch (e: Exception) {
                "🔔 Varsayılan Alarm Sesi"
            }
        } else {
            "🔔 Varsayılan Alarm Sesi"
        }
    }

    companion object {
        val FREQUENCIES = arrayOf(
            "Günde 1 defa", "6 Saatte Bir", "8 Saatte Bir", "12 Saatte Bir",
            "2 Günde Bir", "3 Günde Bir", "4 Günde Bir", "5 Günde Bir",
            "6 Günde Bir", "Haftada 1 Defa",
        )

        // Güne özel modda sadece saat bazlı frekanslar mantıklı
        val CUSTOM_FREQUENCIES = arrayOf(
            "Günde 1 defa", "6 Saatte Bir", "8 Saatte Bir", "12 Saatte Bir",
        )
    }
}

// ══════════════════════════════════════════════════════════════
//  COMPOSE UI
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddMedicineScreen(
    state: AddMedicineFormState,
    isEditMode: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current

    val soundPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri: Uri? = result.data!!.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                state.soundUri = uri.toString()
                state.soundTitle = "🎵 " + RingtoneManager.getRingtone(context, uri).getTitle(context)
            } else {
                state.soundUri = null
                state.soundTitle = "🔔 Varsayılan Alarm Sesi"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "İlacı Düzenle" else "Yeni İlaç Ekle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Kapat",
                            // 45° döndürülmüş "+" ikonu bir "×" (kapat) simgesi veriyor —
                            // ayrı bir kapat vektörü eklemeye gerek kalmıyor.
                            modifier = Modifier.rotate(45f),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            FormCard(title = "💊 İlaç Bilgileri") {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = {
                            state.name = it
                            state.nameError = null
                        },
                        label = { Text("İlaç Adı") },
                        singleLine = true,
                        isError = state.nameError != null,
                        supportingText = state.nameError?.let { msg -> { Text(msg) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = { state.note = it },
                        label = { Text("Not (İsteğe bağlı)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                FormCard(title = "🕐 Zamanlama Modu") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !state.isCustomDayMode,
                            onClick = { state.isCustomDayMode = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text("Standart") }
                        SegmentedButton(
                            selected = state.isCustomDayMode,
                            onClick = { state.isCustomDayMode = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text("Güne Özel") }
                    }
                }

                if (!state.isCustomDayMode) {
                    FormCard(title = "⏰ Alarm Zamanı") {
                        val timePickerState = rememberTimePickerState(
                            initialHour = state.hour,
                            initialMinute = state.minute,
                            is24Hour = DateFormat.is24HourFormat(context),
                        )
                        LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                            state.hour = timePickerState.hour
                            state.minute = timePickerState.minute
                        }
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TimeInput(state = timePickerState)
                        }
                        Spacer(Modifier.height(12.dp))
                        FrequencyDropdown(
                            label = "Kullanım Sıklığı",
                            options = AddMedicineFormState.FREQUENCIES,
                            selected = state.freqText,
                            onSelect = { state.freqText = it },
                        )
                    }
                }

                if (state.isCustomDayMode) {
                    FormCard(title = "📅 Günleri ve Saatleri Seçin") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DAY_CHIPS.forEach { (calDay, label) ->
                                FilterChip(
                                    selected = state.customDayTimes.containsKey(calDay),
                                    onClick = {
                                        if (state.customDayTimes.containsKey(calDay)) {
                                            state.customDayTimes.remove(calDay)
                                        } else {
                                            state.dayTimeDialogFor = calDay
                                        }
                                    },
                                    label = { Text(label) },
                                )
                            }
                        }

                        val selectedDays = DAY_ORDER.filter { state.customDayTimes.containsKey(it) }
                        if (selectedDays.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                selectedDays.forEach { day ->
                                    val time = state.customDayTimes.getValue(day)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(DAY_NAMES[day], modifier = Modifier.weight(1f), fontSize = 16.sp)
                                        Text(
                                            formatTimeForDisplay(context, time),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                        )
                                        TextButton(onClick = { state.dayTimeDialogFor = day }) {
                                            Text("Düzenle", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        FrequencyDropdown(
                            label = "Kullanım Sıklığı",
                            options = AddMedicineFormState.CUSTOM_FREQUENCIES,
                            selected = state.customFreqText,
                            onSelect = { state.customFreqText = it },
                        )
                    }
                }

                FormCard(title = "📅 Tarih & Ses Ayarları") {
                    if (!state.isCustomDayMode) {
                        OutlinedButton(
                            onClick = { state.showDateRangeDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Tarih Aralığı Seç")
                        }
                        if (state.dateRangeText.isNotEmpty()) {
                            Text(
                                text = state.dateRangeText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedButton(
                        onClick = { soundPickerLauncher.launch(buildSoundPickerIntent(state.soundUri)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("🎵 Bildirim Sesini Seç")
                    }
                    Text(
                        text = state.soundTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        // padding'i height'tan ÖNCE veriyoruz: aksi halde height(56dp) önce
                        // uygulanıp toplam yüksekliği 56dp'ye sabitliyor, sonra padding onun
                        // içinden ~32dp yiyor ve butona metin için sadece ~24dp kalıyordu
                        // (yazı dikey kırpılıyordu). Bu sırada padding dış boşluk (margin),
                        // height ise butonun kendi yüksekliği olur.
                        .padding(top = 8.dp, bottom = 24.dp)
                        .height(56.dp),
                    // Şekli elle vermiyoruz: M3'ün varsayılan (hap/stadium) buton şekli
                    // aşağıdaki OutlinedButton'larla ve segmented button'la aynı — böylece
                    // ekrandaki tüm butonlar tutarlı, tasarlanmış bir aile gibi görünüyor.
                ) {
                    Text(
                        text = if (isEditMode) "Güncelle" else "İlacı Kaydet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
        }
    }

    // Güne özel: gün başına saat seçme dialogu
    state.dayTimeDialogFor?.let { day ->
        val existing = state.customDayTimes[day]
        val (initHour, initMinute) = remember(day) {
            if (existing != null) {
                val parts = existing.split(":")
                parts[0].toInt() to parts[1].toInt()
            } else {
                val now = Calendar.getInstance()
                now.get(Calendar.HOUR_OF_DAY) to now.get(Calendar.MINUTE)
            }
        }
        val dayPickerState = rememberTimePickerState(
            initialHour = initHour,
            initialMinute = initMinute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { state.dayTimeDialogFor = null },
            confirmButton = {
                TextButton(onClick = {
                    state.customDayTimes[day] = String.format(
                        Locale.getDefault(), "%02d:%02d", dayPickerState.hour, dayPickerState.minute,
                    )
                    state.dayTimeDialogFor = null
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { state.dayTimeDialogFor = null }) { Text("İptal") }
            },
            text = { TimePicker(state = dayPickerState) },
        )
    }

    // Tarih aralığı seçme dialogu
    if (state.showDateRangeDialog) {
        val rangeState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = state.selectedStartDate.takeIf { it != 0L },
            initialSelectedEndDateMillis = state.selectedEndDate.takeIf { it != 0L },
        )
        DatePickerDialog(
            onDismissRequest = { state.showDateRangeDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = rangeState.selectedStartDateMillis
                    val end = rangeState.selectedEndDateMillis
                    if (start != null && end != null) {
                        state.selectedStartDate = start
                        state.selectedEndDate = end
                        state.dateRangeText = formatDateRange(start, end)
                    }
                    state.showDateRangeDialog = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { state.showDateRangeDialog = false }) { Text("İptal") }
            },
        ) {
            DateRangePicker(state = rangeState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyDropdown(
    label: String,
    options: Array<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FormCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        // containerColor'ı elle vermiyoruz — bkz. MainActivity.MedicineCard'daki aynı yorum:
        // M3 varsayılanı (surfaceContainerLow) arka plandan otomatik ayrışan bir ton veriyor.
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            content()
        }
    }
}
