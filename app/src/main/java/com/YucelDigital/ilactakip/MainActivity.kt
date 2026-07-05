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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog as ComposeAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.YucelDigital.ilactakip.ui.theme.IlacTakipTheme
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_NOTIFICATION = 101
    }

    private val medicineList = mutableStateListOf<Medicine>()

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val isEditMode = data.getBooleanExtra("is_edit", false)
            val newMedicine = data.getSerializableExtra("new_medicine") as? Medicine

            if (newMedicine != null) {
                if (isEditMode) {
                    val editPosition = data.getIntExtra("edit_position", -1)
                    if (editPosition != -1) {
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
                        medicine.isActive = isChecked
                        if (!isChecked) {
                            cancelAlarm(medicine)
                            Toast.makeText(this, "Alarm pasif edildi", Toast.LENGTH_SHORT).show()
                        } else {
                            scheduleAlarm(medicine)
                            Toast.makeText(this, "Alarm aktif edildi", Toast.LENGTH_SHORT).show()
                        }
                        saveData()
                    },
                    onTakenChanged = { medicine, isChecked ->
                        medicine.isTaken = isChecked
                        saveData()
                    },
                    onDeleteConfirmed = { medicine ->
                        cancelAlarm(medicine)
                        medicineList.remove(medicine)
                        saveData()
                        Toast.makeText(this, "İlaç silindi", Toast.LENGTH_SHORT).show()
                    },
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

    /** Tüm izinleri sırayla kontrol et ve iste */
    private fun requestAllPermissions() {
        requestNotificationPermission()
    }

    // ── 1. Bildirim İzni (Android 13+) ──

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Daha önce reddedildiyse neden gerekli olduğunu açıkla
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
                // İzin verildi, sonraki izne geç
                checkAlarmPermission()
            } else {
                // İzin reddedildi
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    // "Tekrar sorma" seçildi → kullanıcıyı ayarlara yönlendir
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

    // ── 2. Kesin Alarm İzni (Android 12+) ──

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

    // ── 3. Tam Ekran Alarm İzni (Android 14+) ──

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

    /** Uygulama ayarları sayfasını aç (izin "tekrar sorma" seçildiyse) */
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
        medicineList.clear()
        medicineList.addAll(MedicineRepository.loadMedicineList(this))
        medicineList.sortBy { it.time ?: "" }
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

        val timeArray = medicine.time.split(", ")

        for (rawTime in timeArray) {
            val singleTime = rawTime.trim()

            val intent = Intent(this, AlarmReceiver::class.java)
            intent.putExtra("MEDICINE_NAME", medicine.name)
            intent.putExtra("MEDICINE_TIME", singleTime)
            intent.putExtra("MEDICINE_NOTE", medicine.note)
            intent.putExtra("START_DATE", medicine.startDate)
            intent.putExtra("END_DATE", medicine.endDate)
            intent.putExtra("INTERVAL_DAYS", medicine.intervalDays)
            intent.putExtra("SOUND_URI", medicine.soundUri)
            intent.putExtra("IS_CUSTOM_DAY", false)
            intent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false)

            val alarmId = AlarmHelper.safeId(medicine.name + singleTime)
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

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            setExactAlarm(alarmManager, calendar.timeInMillis, pi)

            AlarmReceiver.scheduleNextResetAlarm(
                this,
                medicine.name, singleTime, alarmId,
                medicine.startDate, medicine.endDate,
                medicine.intervalDays, medicine.soundUri,
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

                val intent = Intent(this, AlarmReceiver::class.java)
                intent.putExtra("MEDICINE_NAME", medicine.name)
                intent.putExtra("MEDICINE_TIME", singleTime)
                intent.putExtra("MEDICINE_NOTE", medicine.note)
                intent.putExtra("START_DATE", 0L)
                intent.putExtra("END_DATE", 0L)
                intent.putExtra("INTERVAL_DAYS", 1)
                intent.putExtra("SOUND_URI", medicine.soundUri)
                intent.putExtra("IS_CUSTOM_DAY", true)
                intent.putExtra("CUSTOM_DAY", calDay)
                intent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false)

                // Benzersiz alarm ID: isim + gün + saat
                val alarmId = AlarmHelper.safeId(medicine.name + "_day" + calDay + "_" + singleTime)
                intent.putExtra("ALARM_ID", alarmId)

                val pi = PendingIntent.getBroadcast(
                    this, alarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val timeParts = singleTime.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                // Sonraki uygun günü hesapla
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.set(Calendar.DAY_OF_WEEK, calDay)

                // Eğer bu haftanın o günü geçmişse, gelecek haftaya al
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }

                setExactAlarm(alarmManager, calendar.timeInMillis, pi)

                AlarmReceiver.scheduleNextCustomDayResetAlarm(
                    this,
                    medicine.name, singleTime, alarmId,
                    calDay, medicine.soundUri,
                )
            }
        }
    }

    /** Kesin alarm kur — API seviyesine göre uygun yöntemi seçer */
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

@Composable
private fun MainScreen(
    medicines: List<Medicine>,
    onAddClick: () -> Unit,
    onEditClick: (Medicine, Int) -> Unit,
    onActiveChanged: (Medicine, Boolean) -> Unit,
    onTakenChanged: (Medicine, Boolean) -> Unit,
    onDeleteConfirmed: (Medicine) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "Yeni İlaç Ekle")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Header()

            if (medicines.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    itemsIndexed(medicines) { index, medicine ->
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
    }
}

@Composable
private fun Header() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(primary, secondary)))
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 24.dp),
    ) {
        Text(
            text = "💊 İlaç Takip",
            color = onPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Sağlığınız bizim önceliğimiz",
            color = onPrimary.copy(alpha = 0.7f),
            fontSize = 14.sp,
        )
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

    val timeText = if (medicine.isUseCustomDays && medicine.customDayTimes != null) {
        formatCustomDayTimes(medicine.customDayTimes)
    } else {
        medicine.time ?: ""
    }
    val dateRange = medicine.dateRange
    val note = medicine.note

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                    )
                    Switch(
                        checked = medicine.isActive,
                        onCheckedChange = onActiveChanged,
                        enabled = !isExpired,
                        // Özel renk vermiyoruz — M3'ün varsayılan Switch renkleri zaten
                        // MaterialTheme.colorScheme.primary'den geliyor, dynamic color'ı
                        // otomatik takip eder.
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
                    if (!medicine.isUseCustomDays && !dateRange.isNullOrEmpty()) {
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
private fun formatCustomDayTimes(dayTimes: Map<Int, String>): String {
    val shortNames = arrayOf("", "Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")
    val ordered = intArrayOf(2, 3, 4, 5, 6, 7, 1) // Pzt-Paz sırası

    val sb = StringBuilder()
    for (day in ordered) {
        val times = dayTimes[day] ?: continue
        val firstTime = if (times.contains(",")) times.split(",")[0].trim() else times
        if (sb.isNotEmpty()) sb.append(", ")
        sb.append(shortNames[day]).append(" ").append(firstTime)
    }
    return sb.toString()
}
