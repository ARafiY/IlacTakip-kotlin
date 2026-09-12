package com.YucelDigital.ilactakip

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog as ComposeAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.YucelDigital.ilactakip.ui.theme.IlacTakipTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_NOTIFICATION = 101
    }

    private val medicineList = mutableStateListOf<Medicine>()

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val isEditMode = data.getBooleanExtra("is_edit", false)
            @Suppress("DEPRECATION")
            val newMedicine = data.getSerializableExtra("new_medicine") as? Medicine

            if (newMedicine != null) {
                if (isEditMode) {
                    val editPosition = data.getIntExtra("edit_position", -1)
                    if (editPosition != -1 && editPosition < medicineList.size) {
                        cancelAlarm(medicineList[editPosition])
                        medicineList[editPosition] = newMedicine
                        Toast.makeText(this, "İlaç güncellendi ✓", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    medicineList.add(newMedicine)
                    Toast.makeText(this, "İlaç eklendi ✓", Toast.LENGTH_SHORT).show()
                }
                scheduleAlarm(newMedicine)
                saveData()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loadData()

        setContent {
            IlacTakipTheme {
                MainScreen(
                    medicines = medicineList,
                    onAddClick = {
                        launcher.launch(Intent(this, AddMedicineActivity::class.java))
                    },
                    onEditClick = { medicine, position ->
                        val intent = Intent(this, AddMedicineActivity::class.java)
                        intent.putExtra("edit_medicine", medicine)
                        intent.putExtra("edit_position", position)
                        launcher.launch(intent)
                    },
                    onActiveChanged = { medicine, isChecked ->
                        val index = medicineList.indexOf(medicine)
                        if (index >= 0) {
                            val updated = medicine.copy().apply { isActive = isChecked }
                            if (!isChecked) {
                                cancelAlarm(updated)
                                Toast.makeText(this, "Alarm pasif edildi", Toast.LENGTH_SHORT).show()
                            } else {
                                scheduleAlarm(updated)
                                Toast.makeText(this, "Alarm aktif edildi", Toast.LENGTH_SHORT).show()
                            }
                            medicineList[index] = updated
                            saveData()
                        }
                    },
                    onTakenChanged = { medicine, isChecked ->
                        val index = medicineList.indexOf(medicine)
                        if (index >= 0) {
                            if (isChecked) {
                                MedicineRepository.markAsTaken(this, medicine.name, medicine.time, medicine.id)
                            } else {
                                MedicineRepository.resetTakenStatus(this, medicine.name, medicine.id)
                            }
                            val currentStock = medicine.stockCount
                            val newStock = if (isChecked && currentStock != null && currentStock > 0) currentStock - 1 else currentStock
                            medicineList[index] = medicine.copy().apply {
                                isTaken = isChecked
                                stockCount = newStock
                            }
                            saveData()
                        }
                    },
                    onDeleteConfirmed = { medicine ->
                        cancelAlarm(medicine)
                        medicineList.remove(medicine)
                        saveData()
                        Toast.makeText(this, "İlaç silindi", Toast.LENGTH_SHORT).show()
                    },
                    onOpenSettings = { openAppSettings() },
                )
            }
        }

        requestAllPermissions()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    // ══════════════════════════════════════════════════════════════
    //  İZİN YÖNETİMİ
    // ══════════════════════════════════════════════════════════════

    private fun requestAllPermissions() {
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    AlertDialog.Builder(this)
                        .setTitle("Bildirim İzni Gerekli")
                        .setMessage(
                            "İlaç hatırlatıcıların çalışabilmesi için bildirim izni gereklidir. " +
                                "Bu izin olmadan ilaç saatlerinizde uyarı alamazsınız."
                        )
                        .setPositiveButton("İzin Ver") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION
                            )
                        }
                        .setNegativeButton("Daha Sonra") { _, _ -> checkAlarmPermission() }
                        .setCancelable(false)
                        .show()
                } else {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION
                    )
                }
            } else {
                checkAlarmPermission()
            }
        } else {
            checkAlarmPermission()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_NOTIFICATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAlarmPermission()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    AlertDialog.Builder(this)
                        .setTitle("Bildirim İzni Kapalı")
                        .setMessage(
                            "İlaç hatırlatıcılar bildirim gönderemez. " +
                                "Lütfen uygulama ayarlarından bildirim iznini açın."
                        )
                        .setPositiveButton("Ayarlara Git") { _, _ -> openAppSettings() }
                        .setNegativeButton("Kapat", null)
                        .show()
                } else {
                    Toast.makeText(this, "⚠ Bildirim izni olmadan hatırlatıcılar çalışmaz", Toast.LENGTH_LONG).show()
                }
                checkAlarmPermission()
            }
        }
    }

    private fun checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Alarm İzni Gerekli")
                    .setMessage(
                        "İlaç saatlerinizde kesin alarm kurabilmek için bu izin gereklidir. " +
                            "İzin vermezseniz alarmlar birkaç dakika gecikebilir."
                    )
                    .setPositiveButton("İzin Ver") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                    .setNegativeButton("Daha Sonra") { _, _ -> checkFullScreenIntentPermission() }
                    .setCancelable(false)
                    .show()
                return
            }
        }
        checkFullScreenIntentPermission()
    }

    private fun checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (nm != null && !nm.canUseFullScreenIntent()) {
                AlertDialog.Builder(this)
                    .setTitle("Kilit Ekranı İzni")
                    .setMessage(
                        "İlaç alarmının kilit ekranında tam ekran görünmesi için bu izin gereklidir. " +
                            "İzin vermezseniz alarm sadece bildirim olarak görünür."
                    )
                    .setPositiveButton("İzin Ver") { _, _ ->
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse("package:$packageName"),
                            )
                        )
                    }
                    .setNegativeButton("Daha Sonra", null)
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    // ══════════════════════════════════════════════════════════════
    //  İLAÇ YÖNETİMİ
    // ══════════════════════════════════════════════════════════════

    private fun cancelAlarm(medicine: Medicine) {
        AlarmHelper.cancelAlarm(this, medicine)
    }

    private fun saveData() {
        MedicineRepository.saveMedicineList(this, medicineList)
    }

    private fun loadData() {
        val saved = MedicineRepository.loadMedicineList(this).sortedBy { it.time ?: "" }
        if (medicineList != saved) {
            medicineList.clear()
            medicineList.addAll(saved)
        }
    }

    private fun scheduleAlarm(medicine: Medicine) {
        if (medicine.isUseCustomDays && medicine.customDayTimes != null) {
            scheduleCustomDayAlarm(medicine)
        } else {
            scheduleStandardAlarm(medicine)
        }
    }

    private fun scheduleStandardAlarm(medicine: Medicine) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val timeArray = medicine.time?.split(", ") ?: return

        for (rawTime in timeArray) {
            val singleTime = rawTime.trim()
            val alarmId = AlarmHelper.getStandardAlarmId(medicine, singleTime)

            val intent = Intent(this, AlarmReceiver::class.java)
            intent.putExtra("MEDICINE_ID", medicine.id)
            intent.putExtra("MEDICINE_NAME", medicine.name)
            intent.putExtra("MEDICINE_TIME", singleTime)
            intent.putExtra("MEDICINE_NOTE", medicine.note)
            intent.putExtra("START_DATE", medicine.startDate)
            intent.putExtra("END_DATE", medicine.endDate)
            intent.putExtra("INTERVAL_DAYS", medicine.intervalDays)
            intent.putExtra("SOUND_URI", medicine.soundUri)
            intent.putExtra("MEAL_TIMING", medicine.mealTiming)
            intent.putExtra("IS_CUSTOM_DAY", false)
            intent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false)
            intent.putExtra("ALARM_ID", alarmId)

            val pi = PendingIntent.getBroadcast(
                this, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val timeParts = singleTime.split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val now = System.currentTimeMillis()
            val interval = if (medicine.intervalDays > 0) medicine.intervalDays else 1

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
                while (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, interval)
                }
            }

            setExactAlarm(alarmManager, calendar.timeInMillis, pi)

            AlarmReceiver.scheduleNextResetAlarm(
                this,
                medicine.name, singleTime, alarmId,
                medicine.startDate, medicine.endDate,
                medicine.intervalDays, medicine.soundUri,
                medicine.id,
            )
        }
    }

    private fun scheduleCustomDayAlarm(medicine: Medicine) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val dayTimes = medicine.customDayTimes ?: return

        for ((calDay, timesForDay) in dayTimes) {
            val times = timesForDay.split(", ")

            for (rawTime in times) {
                val singleTime = rawTime.trim()
                val alarmId = AlarmHelper.getCustomDayAlarmId(medicine, calDay, singleTime)

                val intent = Intent(this, AlarmReceiver::class.java)
                intent.putExtra("MEDICINE_ID", medicine.id)
                intent.putExtra("MEDICINE_NAME", medicine.name)
                intent.putExtra("MEDICINE_TIME", singleTime)
                intent.putExtra("MEDICINE_NOTE", medicine.note)
                intent.putExtra("START_DATE", medicine.startDate)
                intent.putExtra("END_DATE", medicine.endDate)
                intent.putExtra("INTERVAL_DAYS", 1)
                intent.putExtra("SOUND_URI", medicine.soundUri)
                intent.putExtra("MEAL_TIMING", medicine.mealTiming)
                intent.putExtra("IS_CUSTOM_DAY", true)
                intent.putExtra("CUSTOM_DAY", calDay)
                intent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false)
                intent.putExtra("ALARM_ID", alarmId)

                val pi = PendingIntent.getBroadcast(
                    this, alarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val timeParts = singleTime.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.set(Calendar.DAY_OF_WEEK, calDay)

                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }

                setExactAlarm(alarmManager, calendar.timeInMillis, pi)

                AlarmReceiver.scheduleNextCustomDayResetAlarm(
                    this,
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

// ══════════════════════════════════════════════════════════════
//  COMPOSE UI
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    medicines: List<Medicine>,
    onAddClick: () -> Unit,
    onEditClick: (Medicine, Int) -> Unit,
    onActiveChanged: (Medicine, Boolean) -> Unit,
    onTakenChanged: (Medicine, Boolean) -> Unit,
    onDeleteConfirmed: (Medicine) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showHistorySheet by remember { mutableStateOf(false) }
    var showBatteryGuide by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("İlaç Takip") },
                actions = {
                    IconButton(onClick = { showHistorySheet = true }) {
                        Icon(painterResource(R.drawable.ic_clock), contentDescription = "İlaç Geçmişi")
                    }
                    IconButton(onClick = { showBatteryGuide = true }) {
                        Icon(painterResource(R.drawable.ic_info), contentDescription = "Pil & Alarm Rehberi")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "Yeni İlaç Ekle")
            }
        },
    ) { innerPadding ->
        if (medicines.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(medicines, key = { _, item -> item.id }) { index, medicine ->
                    MedicineCard(
                        medicine = medicine,
                        onClick = { onEditClick(medicine, index) },
                        onActiveChanged = { onActiveChanged(medicine, it) },
                        onTakenChanged = { onTakenChanged(medicine, it) },
                        onDelete = { onDeleteConfirmed(medicine) },
                    )
                }
            }
        }
    }

    if (showHistorySheet) {
        HistoryBottomSheet(onDismiss = { showHistorySheet = false })
    }

    if (showBatteryGuide) {
        ComposeAlertDialog(
            onDismissRequest = { showBatteryGuide = false },
            title = { Text("⏰ Alarm & Pil Rehberi") },
            text = {
                Text(
                    "Xiaomi, Huawei, Samsung gibi cihazlarda kilit ekranında alarmların sorunsuz açılması için:\n\n" +
                        "1. 'Otomatik Başlatma' (Autostart) iznini verin.\n" +
                        "2. Pil ayarlarından 'Kısıtlama Yok' seçin.\n" +
                        "3. 'Kilit ekranında göster' iznini açın.",
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryGuide = false
                    onOpenSettings()
                }) { Text("Ayarları Aç") }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryGuide = false }) { Text("Anladım") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(MedicineRepository.loadLogs(context)) }
    val sheetState = rememberModalBottomSheetState()
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy HH:mm", Locale.forLanguageTag("tr")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("📜 İlaç Kullanım Geçmişi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (logs.isNotEmpty()) {
                    TextButton(onClick = {
                        MedicineRepository.clearLogs(context)
                        logs = emptyList()
                    }) {
                        Text("Temizle", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Henüz kaydedilmiş kullanım geçmişi yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(logs, key = { it.id }) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(log.medicineName, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                    Text(dateFormat.format(Date(log.timestamp)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("Alındı ✓", color = colorResource(R.color.status_taken), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("💊", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Henüz ilaç eklenmedi",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Aşağıdaki + butonuna basarak\nilk ilacınızı ekleyin",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 19.5.sp,
        )
    }
}

@Composable
private fun MedicineCard(
    medicine: Medicine,
    onClick: () -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onTakenChanged: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isExpired = remember(medicine.endDate) {
        val endDate = medicine.endDate
        if (endDate == 0L) {
            false
        } else {
            val endCal = Calendar.getInstance()
            endCal.timeInMillis = endDate
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            System.currentTimeMillis() > endCal.timeInMillis
        }
    }

    val statusText: String
    val statusColor: Color
    var indicatorColor: Color

    when {
        isExpired -> {
            statusText = "Tarihi Geçti"
            statusColor = colorResource(R.color.indicator_expired)
            indicatorColor = colorResource(R.color.indicator_expired)
        }
        medicine.isTaken -> {
            statusText = "İlacını Aldın ✓"
            statusColor = colorResource(R.color.status_taken)
            indicatorColor = colorResource(R.color.indicator_taken)
        }
        else -> {
            statusText = "İlacını Almadın"
            statusColor = colorResource(R.color.status_missed)
            indicatorColor = colorResource(R.color.indicator_missed)
        }
    }
    if (!medicine.isActive && !isExpired) {
        indicatorColor = colorResource(R.color.indicator_expired)
    }

    val customDayTimes = medicine.customDayTimes
    val timeText = if (medicine.isUseCustomDays && customDayTimes != null) {
        formatCustomDayTimes(context, customDayTimes)
    } else {
        formatTimesForDisplay(context, medicine.time ?: "")
    }
    val dateRange = medicine.dateRange
    val note = medicine.note
    val mealTiming = medicine.mealTiming
    val stockCount = medicine.stockCount

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(indicatorColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor,
                        )
                        if (!mealTiming.isNullOrEmpty() && mealTiming != "Fark Etmez") {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = "🍽️ $mealTiming",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    Switch(
                        checked = medicine.isActive,
                        onCheckedChange = onActiveChanged,
                        enabled = !isExpired,
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = medicine.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = timeText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (!dateRange.isNullOrEmpty()) {
                        Spacer(Modifier.width(12.dp))
                        Text("📅 ", fontSize = 12.sp)
                        Text(
                            text = dateRange,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (!note.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("📝 ", fontSize = 12.sp)
                        Text(
                            text = note,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (stockCount != null) {
                    val isLow = stockCount <= 3
                    Surface(
                        color = if (isLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        Text(
                            text = if (isLow) "⚠ Az Kaldı: $stockCount adet" else "📦 Kalan: $stockCount adet",
                            fontSize = 11.sp,
                            fontWeight = if (isLow) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLow) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(end = 12.dp, top = 14.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Checkbox(
                    checked = medicine.isTaken,
                    onCheckedChange = { if (!isExpired) onTakenChanged(it) },
                    enabled = !isExpired,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.height(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { showDeleteDialog = true },
                )
            }
        }
    }

    if (showDeleteDialog) {
        ComposeAlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("İlacı Sil") },
            text = { Text("${medicine.name} ilacını silmek istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("İptal") }
            },
        )
    }
}

/** HashMap<calDay, times> → "Pzt 09:00, Sal 10:30" formatına dönüştür */
private fun formatCustomDayTimes(context: Context, dayTimes: Map<Int, String>): String {
    val shortNames = arrayOf("", "Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")
    val ordered = intArrayOf(2, 3, 4, 5, 6, 7, 1)

    val sb = StringBuilder()
    for (day in ordered) {
        val times = dayTimes[day] ?: continue
        val firstTime = if (times.contains(",")) times.split(",")[0].trim() else times
        if (sb.isNotEmpty()) sb.append(", ")
        sb.append(shortNames[day]).append(" ").append(formatTimeForDisplay(context, firstTime))
    }
    return sb.toString()
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenPreview() {
    IlacTakipTheme {
        MainScreen(
            medicines = listOf(
                Medicine("Aspirin", "09:00", "1 Eki - 10 Eki", "Yemekten sonra").apply {
                    mealTiming = "Tok Karnına"
                    stockCount = 15
                    isActive = true
                    isTaken = false
                },
                Medicine("Parol", "14:00", null, "Ağrı olursa").apply {
                    mealTiming = "Aç Karnına"
                    stockCount = 2
                    isActive = true
                    isTaken = true
                },
                Medicine("Vitamin D", "19:00", null, null).apply {
                    mealTiming = "Fark Etmez"
                    stockCount = 5
                    isActive = false
                    isTaken = false
                }
            ),
            onAddClick = {},
            onEditClick = { _, _ -> },
            onActiveChanged = { _, _ -> },
            onTakenChanged = { _, _ -> },
            onDeleteConfirmed = {},
            onOpenSettings = {},
        )
    }
}
