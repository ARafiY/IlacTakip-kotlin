package com.YucelDigital.ilactakip

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "medicine_alarm_channel"

        /**
         * IS_RESET = true olduğunda bu alarm "ön sıfırlama" alarmıdır.
         * Asıl alarmdan 60 dakika önce tetiklenerek isTaken bayrağını temizler.
         */
        const val EXTRA_IS_RESET = "IS_RESET"

        /**
         * ANA ALARM'ı bir sonraki döngü için kur.
         * Her tetiklenmede bu çağrılarak alarm zinciri korunur.
         */
        @JvmStatic
        fun rescheduleNextMainAlarm(
            context: Context, name: String?, time: String?, note: String?,
            alarmId: Int, startDate: Long, endDate: Long,
            intervalDays: Int, soundUriStr: String?,
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            if (time == null || !time.contains(":")) return

            val parts = time.split(":")
            val hour: Int
            val minute: Int
            try {
                hour = parts[0].toInt()
                minute = parts[1].toInt()
            } catch (e: Exception) {
                return
            }

            val next = Calendar.getInstance()
            next.set(Calendar.HOUR_OF_DAY, hour)
            next.set(Calendar.MINUTE, minute)
            next.set(Calendar.SECOND, 0)
            next.set(Calendar.MILLISECOND, 0)
            next.add(Calendar.DAY_OF_YEAR, intervalDays) // Bir sonraki döngü

            // Bitiş tarihi geçtiyse yeniden kurma
            if (endDate != 0L) {
                val endCal = Calendar.getInstance()
                endCal.timeInMillis = endDate
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                if (next.timeInMillis > endCal.timeInMillis) return
            }

            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("MEDICINE_NAME", name)
            intent.putExtra("MEDICINE_TIME", time)
            intent.putExtra("MEDICINE_NOTE", note)
            intent.putExtra("ALARM_ID", alarmId)
            intent.putExtra("START_DATE", startDate)
            intent.putExtra("END_DATE", endDate)
            intent.putExtra("INTERVAL_DAYS", intervalDays)
            intent.putExtra("SOUND_URI", soundUriStr)
            intent.putExtra(EXTRA_IS_RESET, false)

            val pi = PendingIntent.getBroadcast(
                context, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            setExact(alarmManager, next.timeInMillis, pi)
        }

        /**
         * GÜNE ÖZEL ALARM'ı gelecek haftanın aynı günü için yeniden kur.
         */
        @JvmStatic
        fun rescheduleCustomDayAlarm(
            context: Context, name: String?, time: String?, note: String?,
            alarmId: Int, soundUriStr: String?, calendarDay: Int,
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            if (time == null || !time.contains(":")) return

            val parts = time.split(":")
            val hour: Int
            val minute: Int
            try {
                hour = parts[0].toInt()
                minute = parts[1].toInt()
            } catch (e: Exception) {
                return
            }

            val next = Calendar.getInstance()
            next.set(Calendar.HOUR_OF_DAY, hour)
            next.set(Calendar.MINUTE, minute)
            next.set(Calendar.SECOND, 0)
            next.set(Calendar.MILLISECOND, 0)
            next.set(Calendar.DAY_OF_WEEK, calendarDay)
            next.add(Calendar.WEEK_OF_YEAR, 1) // Gelecek hafta

            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("MEDICINE_NAME", name)
            intent.putExtra("MEDICINE_TIME", time)
            intent.putExtra("MEDICINE_NOTE", note)
            intent.putExtra("ALARM_ID", alarmId)
            intent.putExtra("START_DATE", 0L)
            intent.putExtra("END_DATE", 0L)
            intent.putExtra("INTERVAL_DAYS", 1)
            intent.putExtra("SOUND_URI", soundUriStr)
            intent.putExtra("IS_CUSTOM_DAY", true)
            intent.putExtra("CUSTOM_DAY", calendarDay)
            intent.putExtra(EXTRA_IS_RESET, false)

            val pi = PendingIntent.getBroadcast(
                context, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            setExact(alarmManager, next.timeInMillis, pi)
        }

        /**
         * RESET ALARM'ı bir sonraki döngü için kur.
         * Asıl alarmdan 60 dakika önce tetiklenerek "İlacı Aldım" işaretini temizler.
         */
        @JvmStatic
        fun scheduleNextResetAlarm(
            context: Context, name: String?, time: String?, alarmId: Int,
            startDate: Long, endDate: Long,
            intervalDays: Int, soundUriStr: String?,
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            if (time == null || !time.contains(":")) return

            val parts = time.split(":")
            val hour: Int
            val minute: Int
            try {
                hour = parts[0].toInt()
                minute = parts[1].toInt()
            } catch (e: Exception) {
                return
            }

            val reset = Calendar.getInstance()
            reset.set(Calendar.HOUR_OF_DAY, hour)
            reset.set(Calendar.MINUTE, minute)
            reset.set(Calendar.SECOND, 0)
            reset.set(Calendar.MILLISECOND, 0)

            var resetMillis = reset.timeInMillis - (60 * 60 * 1000) // asıl alarmdan 60 dk öncesi

            // Eğer bu reset saati (bugün için) geçmişse, gelecekteki uygun saate kadar interval ekle
            while (resetMillis <= System.currentTimeMillis()) {
                resetMillis += (intervalDays.toLong() * 24 * 60 * 60 * 1000)
            }

            reset.timeInMillis = resetMillis

            // Bitiş tarihinden sonraya geçtiyse planla
            if (endDate != 0L) {
                val endCal = Calendar.getInstance()
                endCal.timeInMillis = endDate
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                if (reset.timeInMillis > endCal.timeInMillis) return
            }

            val resetAlarmId = AlarmHelper.safeId(name + time + "_reset")

            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("MEDICINE_NAME", name)
            intent.putExtra("MEDICINE_TIME", time)
            intent.putExtra("ALARM_ID", alarmId)
            intent.putExtra("START_DATE", startDate)
            intent.putExtra("END_DATE", endDate)
            intent.putExtra("INTERVAL_DAYS", intervalDays)
            intent.putExtra("SOUND_URI", soundUriStr)
            intent.putExtra(EXTRA_IS_RESET, true)

            val pi = PendingIntent.getBroadcast(
                context, resetAlarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            setExact(alarmManager, reset.timeInMillis, pi)
        }

        /** Ses URI'sini çözümle: null/boş → varsayılan alarm sesi */
        @JvmStatic
        fun resolveSoundUri(soundUriStr: String?): Uri? {
            if (!soundUriStr.isNullOrEmpty()) {
                return Uri.parse(soundUriStr)
            }
            return android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        }

        @JvmStatic
        fun scheduleNextCustomDayResetAlarm(
            context: Context, name: String?, time: String?, alarmId: Int,
            customDay: Int, soundUriStr: String?,
        ) {
            if (time == null || !time.contains(":")) return
            val parts = time.split(":")
            val hour: Int
            val minute: Int
            try {
                hour = parts[0].toInt()
                minute = parts[1].toInt()
            } catch (e: Exception) {
                return
            }

            val reset = Calendar.getInstance()
            reset.set(Calendar.HOUR_OF_DAY, hour)
            reset.set(Calendar.MINUTE, minute)
            reset.set(Calendar.SECOND, 0)
            reset.set(Calendar.MILLISECOND, 0)
            reset.set(Calendar.DAY_OF_WEEK, customDay)

            var resetMillis = reset.timeInMillis - (60 * 60 * 1000) // 1 saat öncesi

            // Eğer bu reset saati (bugün için) geçmişse, 1 hafta ekle
            while (resetMillis <= System.currentTimeMillis()) {
                resetMillis += (7L * 24 * 60 * 60 * 1000)
            }

            reset.timeInMillis = resetMillis

            val resetAlarmId = AlarmHelper.safeId(name + "_day" + customDay + "_" + time + "_reset")

            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("MEDICINE_NAME", name)
            intent.putExtra("MEDICINE_TIME", time)
            intent.putExtra("ALARM_ID", alarmId)
            intent.putExtra("IS_CUSTOM_DAY", true)
            intent.putExtra("CUSTOM_DAY", customDay)
            intent.putExtra("SOUND_URI", soundUriStr)
            intent.putExtra(EXTRA_IS_RESET, true)

            val pi = PendingIntent.getBroadcast(
                context, resetAlarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                setExact(alarmManager, resetMillis, pi)
            }
        }

        private fun setExact(alarmManager: AlarmManager, triggerAt: Long, pi: PendingIntent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Ortak veriler
        val name = intent.getStringExtra("MEDICINE_NAME")
        val time = intent.getStringExtra("MEDICINE_TIME")
        val note = intent.getStringExtra("MEDICINE_NOTE")
        val alarmId = intent.getIntExtra("ALARM_ID", 0)
        val startDate = intent.getLongExtra("START_DATE", 0)
        val endDate = intent.getLongExtra("END_DATE", 0)
        val intervalDays = intent.getIntExtra("INTERVAL_DAYS", 1)
        val soundUriStr = intent.getStringExtra("SOUND_URI")
        val isReset = intent.getBooleanExtra(EXTRA_IS_RESET, false)
        val isCustomDay = intent.getBooleanExtra("IS_CUSTOM_DAY", false)
        val customDay = intent.getIntExtra("CUSTOM_DAY", 0)

        // ---- RESET ALARMIYSA: isTaken'ı temizle, çık ----
        if (isReset) {
            MedicineRepository.resetTakenStatus(context, name)
            if (isCustomDay && customDay != 0) {
                scheduleNextCustomDayResetAlarm(context, name, time, alarmId, customDay, soundUriStr)
            } else {
                scheduleNextResetAlarm(context, name, time, alarmId, startDate, endDate, intervalDays, soundUriStr)
            }
            return
        }

        // ---- ANA ALARM: Tarih / Gün Kontrolü (sadece standart mod) ----
        if (!isCustomDay && startDate != 0L) {
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            val todayMillis = today.timeInMillis

            if (todayMillis < startDate) {
                rescheduleNextMainAlarm(context, name, time, note, alarmId, startDate, endDate, intervalDays, soundUriStr)
                return
            }

            if (endDate != 0L) {
                val endCal = Calendar.getInstance()
                endCal.timeInMillis = endDate
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                if (todayMillis > endCal.timeInMillis) return
            }

            if (intervalDays > 1) {
                val diffMillis = todayMillis - startDate
                val diffDays = diffMillis / (1000L * 60 * 60 * 24)
                if (diffDays % intervalDays != 0L) {
                    rescheduleNextMainAlarm(context, name, time, note, alarmId, startDate, endDate, intervalDays, soundUriStr)
                    return
                }
            }
        }

        // ---- ANA ALARM: Bildirim inşası + isTaken kontrolü disk I/O gerektirir.
        // onReceive'i (ve dolayısıyla ana thread'i) hızlıca serbest bırakmak için
        // bu kısmı arka planda çalıştırıyoruz.
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        Thread {
            try {
                handleMainAlarm(appContext, name, time, note, alarmId, startDate, endDate,
                    intervalDays, soundUriStr, isCustomDay, customDay)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    /**
     * Ana alarm tetiklendiğinde bildirim inşası + kendini yeniden kurma (arka planda çalışır).
     * Test kodu (Robolectric) goAsync()/arka plan thread'ini devreye sokmadan bu mantığı
     * doğrudan ve senkron çağırabilsin diye public bırakıldı.
     */
    fun handleMainAlarm(
        context: Context, name: String?, time: String?, note: String?, alarmId: Int,
        startDate: Long, endDate: Long, intervalDays: Int, soundUriStr: String?,
        isCustomDay: Boolean, customDay: Int,
    ) {
        // ---- ANA ALARM: Bildirim Göster ----
        // İsimle eşleştirilir (bkz. MedicineRepository.markAsTaken açıklaması) —
        // güne özel modda Medicine.time sadece tek bir günün saatini tuttuğu için
        // saat bazlı eşleştirme hatalı biçimde eşleşmeyi kaçırıyordu.
        var isAlreadyTaken = false
        val medicineList = MedicineRepository.loadMedicineList(context)
        for (m in medicineList) {
            if (m.name.equals(name, ignoreCase = true)) {
                if (m.isTaken) {
                    isAlreadyTaken = true
                }
                break
            }
        }

        if (!isAlreadyTaken) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            val fullScreenIntent = Intent(context, AlarmActivity::class.java)
            fullScreenIntent.putExtra("MEDICINE_NAME", name)
            fullScreenIntent.putExtra("MEDICINE_TIME", time)
            fullScreenIntent.putExtra("MEDICINE_NOTE", note)
            fullScreenIntent.putExtra("ALARM_ID", alarmId)
            fullScreenIntent.putExtra("START_DATE", startDate)
            fullScreenIntent.putExtra("END_DATE", endDate)
            fullScreenIntent.putExtra("INTERVAL_DAYS", intervalDays)
            fullScreenIntent.putExtra("SOUND_URI", soundUriStr)
            fullScreenIntent.putExtra("IS_CUSTOM_DAY", isCustomDay)
            fullScreenIntent.putExtra("CUSTOM_DAY", customDay)
            fullScreenIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )

            val fullScreenPI = PendingIntent.getActivity(
                context, alarmId, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID, "İlaç Hatırlatıcı", NotificationManager.IMPORTANCE_HIGH,
                )
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
                channel.enableVibration(true)
                channel.setSound(null, null)
                nm?.createNotificationChannel(channel)
            }
            buildAndNotify(context, nm, CHANNEL_ID, name, time, note,
                alarmId, startDate, endDate, intervalDays, soundUriStr,
                isCustomDay, customDay, fullScreenPI)
        }

        // ---- KENDİNİ YENİDEN KUR ----
        if (isCustomDay && customDay != 0) {
            // Güne özel: Gelecek haftanın aynı günü için yeniden kur
            rescheduleCustomDayAlarm(context, name, time, note, alarmId, soundUriStr, customDay)
            scheduleNextCustomDayResetAlarm(context, name, time, alarmId, customDay, soundUriStr)
        } else {
            // Standart: Bir sonraki döngü
            rescheduleNextMainAlarm(context, name, time, note, alarmId, startDate, endDate, intervalDays, soundUriStr)
            scheduleNextResetAlarm(context, name, time, alarmId, startDate, endDate, intervalDays, soundUriStr)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BİLDİRİM İNŞA
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildAndNotify(
        context: Context, nm: NotificationManager?, channelId: String,
        name: String?, time: String?, note: String?, alarmId: Int,
        startDate: Long, endDate: Long, intervalDays: Int,
        soundUriStr: String?, isCustomDay: Boolean, customDay: Int,
        fullScreenPI: PendingIntent,
    ) {
        // "İlaç Aldım" aksiyon
        val takenIntent = Intent(context, NotificationActionReceiver::class.java)
        takenIntent.action = NotificationActionReceiver.ACTION_TAKEN
        takenIntent.putExtra("MEDICINE_NAME", name)
        takenIntent.putExtra("MEDICINE_TIME", time)
        takenIntent.putExtra("ALARM_ID", alarmId)
        val takenPI = PendingIntent.getBroadcast(
            context, alarmId + 1000, takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "Ertele 5 dk" aksiyon
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java)
        snoozeIntent.action = NotificationActionReceiver.ACTION_SNOOZE
        snoozeIntent.putExtra("MEDICINE_NAME", name)
        snoozeIntent.putExtra("MEDICINE_TIME", time)
        snoozeIntent.putExtra("MEDICINE_NOTE", note)
        snoozeIntent.putExtra("ALARM_ID", alarmId)
        snoozeIntent.putExtra("START_DATE", startDate)
        snoozeIntent.putExtra("END_DATE", endDate)
        snoozeIntent.putExtra("INTERVAL_DAYS", intervalDays)
        snoozeIntent.putExtra("SOUND_URI", soundUriStr)
        snoozeIntent.putExtra("IS_CUSTOM_DAY", isCustomDay)
        snoozeIntent.putExtra("CUSTOM_DAY", customDay)
        snoozeIntent.putExtra("SNOOZE_MINUTES", 5)
        val snoozePI = PendingIntent.getBroadcast(
            context, alarmId + 2000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle("İlaç Vakti: $name")
            .setContentText("$time — İlacınızı almayı unutmayın.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setFullScreenIntent(fullScreenPI, true)
            .addAction(R.drawable.ic_pill, "✓ İlaç Aldım", takenPI)
            .addAction(R.drawable.ic_note, "⏰ Ertele", snoozePI)

        // Bildirime KASITLI OLARAK ses eklenmiyor (builder.setSound(...) çağırmayın).
        // androidx.core.app.NotificationCompatBuilder, channelId'li bir builder'da API 26+
        // için Builder.setSound(...)'u her zaman sessizce mBuilder.setSound(null) ile
        // geçersiz kılıyor (bkz. NotificationCompatBuilder.buildInternal()) — yani bu
        // satıra ne yazarsak yazalım gerçek cihazda (Android 8+) hiçbir zaman çalmaz,
        // sadece yanıltıcı olur. Kanal da (yukarıda) zaten sessiz oluşturuluyor.
        // Sesin TEK kaynağı AlarmActivity.playAlarmSound() — tam ekran intent
        // (yukarıdaki setFullScreenIntent) hem kilitli hem kilitsiz durumda güvenilir
        // şekilde açılıp AlarmActivity'yi tetikler, çift ses riski de böylece ortadan kalkar.

        nm?.notify(alarmId, builder.build())
    }
}
