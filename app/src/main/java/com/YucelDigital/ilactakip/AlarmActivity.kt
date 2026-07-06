package com.YucelDigital.ilactakip

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.util.Calendar
import java.util.Random

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_SCREEN_OFF == intent.action) {
                stopAlarmSound()
            }
        }
    }

    private var nameText: TextView? = null
    private var timeText: TextView? = null
    private var noteText: TextView? = null
    private var stopButton: MaterialButton? = null
    private var snoozeButton: MaterialButton? = null

    private var medicineName: String? = null
    private var medicineTime: String? = null
    private var medicineNote: String? = null
    private var notificationId = 0
    private var startDate: Long = 0
    private var endDate: Long = 0
    private var intervalDays = 1
    private var soundUriStr: String? = null
    private var isCustomDay = false
    private var customDay = 0

    companion object {
        private const val ALARM_TIMEOUT_MS = 5 * 60 * 1000L // 5 dakika
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, filter)
        }

        // Kilit Ekranında Açılma
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // Kilidi aktif olarak kaldır (Android 8.1+)
            val km = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        // Ekranın hemen tekrar kapanmasını önle
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Intent Verilerini Al
        medicineName = intent.getStringExtra("MEDICINE_NAME")
        medicineTime = intent.getStringExtra("MEDICINE_TIME")
        medicineNote = intent.getStringExtra("MEDICINE_NOTE")
        notificationId = intent.getIntExtra("ALARM_ID", 0)
        startDate = intent.getLongExtra("START_DATE", 0)
        endDate = intent.getLongExtra("END_DATE", 0)
        intervalDays = intent.getIntExtra("INTERVAL_DAYS", 1)
        soundUriStr = intent.getStringExtra("SOUND_URI")
        isCustomDay = intent.getBooleanExtra("IS_CUSTOM_DAY", false)
        customDay = intent.getIntExtra("CUSTOM_DAY", 0)

        // View'ları Bağla
        nameText = findViewById(R.id.medicineNameTextView)
        timeText = findViewById(R.id.timeTextView)
        noteText = findViewById(R.id.noteTextView)
        stopButton = findViewById(R.id.stopButton)
        snoozeButton = findViewById(R.id.snoozeButton)

        nameText?.text = medicineName ?: "İlaç Zamanı"
        timeText?.text = medicineTime
        noteText?.text = if (!medicineNote.isNullOrEmpty()) "Not: $medicineNote" else ""

        // Özel Sesi Başlat
        playAlarmSound()

        // DURDUR
        stopButton?.setOnClickListener {
            timeoutHandler.removeCallbacksAndMessages(null)
            stopAlarmSound()
            clearNotification()
            markAsTaken()
            finish()
        }

        // ERTELE — Rastgele Süre
        snoozeButton?.setOnClickListener { handleRandomSnooze() }

        // 5 dakika sonra otomatik ertele (kullanıcı cevap vermezse)
        timeoutHandler.postDelayed({
            stopAlarmSound()
            clearNotification()
            snoozeAlarm(5)
            finish()
        }, ALARM_TIMEOUT_MS)
    }

    private fun clearNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(notificationId)
    }

    private fun handleRandomSnooze() {
        timeoutHandler.removeCallbacksAndMessages(null)
        stopAlarmSound()
        val possibleMinutes = intArrayOf(5, 10, 15, 20, 25, 30, 35, 40, 45)
        val randomMinute = possibleMinutes[Random().nextInt(possibleMinutes.size)]
        clearNotification()
        snoozeAlarm(randomMinute)
        finish()
    }

    private fun markAsTaken() {
        MedicineRepository.markAsTaken(this, medicineName, medicineTime)
    }

    private fun playAlarmSound() {
        val soundUri = AlarmReceiver.resolveSoundUri(soundUriStr)
        if (!startPlayback(soundUri, isFallback = false)) {
            // Seçilen/çözümlenen ses hiç kurulamadıysa (uri null, dosya silinmiş vb.)
            // sessiz kalmak yerine sistemin varsayılan alarm sesine düş.
            playDefaultAlarmSound()
        }
    }

    private fun playDefaultAlarmSound() {
        val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        startPlayback(fallbackUri, isFallback = true)
    }

    /**
     * Verilen sesle çalmayı dener. Kurulum senkron olarak başarısız olursa (setDataSource
     * fırlatırsa) false döner ve çağıran taraf varsayılana düşebilir. Kurulum kabul edilip
     * hazırlık asenkron olarak başarısız olursa (bozuk/silinmiş özel ses dosyası gibi),
     * setOnErrorListener kendisi varsayılana düşer — bu yüzden fallback denemesinde
     * (isFallback = true) sonsuz döngüye girmemek için hata dinleyicisi eklenmiyor.
     */
    private fun startPlayback(soundUri: Uri?, isFallback: Boolean): Boolean {
        if (soundUri == null) return false
        return try {
            mediaPlayer?.release()
            val player = MediaPlayer()
            mediaPlayer = player
            player.setDataSource(applicationContext, soundUri)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            player.isLooping = true // Ses tekrar tekrar çalar
            // prepare() ana thread'i bloklar (özellikle content:// URI'lerde ANR riski);
            // prepareAsync() + listener kullanılıyor.
            player.setOnPreparedListener { mp -> mp.start() }
            if (!isFallback) {
                player.setOnErrorListener { mp, _, _ ->
                    mp.release()
                    if (mediaPlayer === mp) mediaPlayer = null
                    playDefaultAlarmSound()
                    true
                }
            }
            player.prepareAsync()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            mediaPlayer?.release()
            mediaPlayer = null
            false
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
    }

    /** Alarmı belirli dakika sonraya ertele, tarih metadata'sını koruyarak */
    private fun snoozeAlarm(minutes: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val snoozeIntent = Intent(this, AlarmReceiver::class.java)
        snoozeIntent.putExtra("MEDICINE_NAME", medicineName)
        snoozeIntent.putExtra("MEDICINE_TIME", medicineTime)
        snoozeIntent.putExtra("MEDICINE_NOTE", medicineNote)
        snoozeIntent.putExtra("ALARM_ID", notificationId)
        snoozeIntent.putExtra("START_DATE", startDate)
        snoozeIntent.putExtra("END_DATE", endDate)
        snoozeIntent.putExtra("INTERVAL_DAYS", intervalDays)
        snoozeIntent.putExtra("SOUND_URI", soundUriStr)
        snoozeIntent.putExtra("IS_CUSTOM_DAY", isCustomDay)
        snoozeIntent.putExtra("CUSTOM_DAY", customDay)

        val pi = PendingIntent.getBroadcast(
            this, notificationId, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, minutes)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
        Toast.makeText(this, "$minutes dakika ertelendi", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler.removeCallbacksAndMessages(null)
        stopAlarmSound()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
        ) {
            stopAlarmSound()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
