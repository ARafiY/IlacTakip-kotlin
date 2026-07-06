package com.YucelDigital.ilactakip

import android.app.NotificationManager
import android.content.Context

import androidx.test.core.app.ApplicationProvider

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * AlarmReceiver.handleMainAlarm() için Robolectric testleri.
 *
 * Notification sesi hep null olmalı: androidx.core.app.NotificationCompatBuilder,
 * channelId'li bir builder'da API 26+ için Builder.setSound(...)'u her zaman
 * sessizce mBuilder.setSound(null) ile geçersiz kılıyor (bkz. buildAndNotify'daki
 * yorum) — yani builder'a ne yazılırsa yazılsın gerçek cihazda hiç çalmaz. Bu yüzden
 * kod artık hiç setSound() çağırmıyor ve sesin TEK kaynağı AlarmActivity'nin kendi
 * MediaPlayer'ı. Bu test, o kararın kalıcı hale geldiğini (birileri tekrar
 * builder.setSound(...) eklemeye kalkışırsa fark edilsin diye) doğruluyor.
 *
 * Ayrıca Fix #1 ile birlikte: isAlreadyTaken kontrolü artık isimle eşleştiği için,
 * zaten "alındı" işaretli bir ilaç için tekrar bildirim gösterilmediğini doğrular.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35]) // Robolectric 4.14.1'in desteklediği en yüksek SDK (proje targetSdk 36)
class AlarmReceiverHandleMainAlarmTest {

    companion object {
        private const val ALARM_ID = 4242
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun postedNotification(context: Context) =
        Shadows.shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .getNotification(ALARM_ID)

    @Test
    fun notification_neverCarriesItsOwnSound() {
        val context = context()

        AlarmReceiver().handleMainAlarm(
            context, "Parol", "08:00", "", ALARM_ID,
            0L, 0L, 1, "content://media/internal/audio/media/1", false, 0,
        )

        val notification = postedNotification(context)
        assertNotNull(notification)
        assertNull(
            "Bildirim kendi sesini çalmamalı — sesin tek kaynağı AlarmActivity olmalı",
            notification.sound,
        )
    }

    @Test
    fun notification_isPostedForNewDose() {
        val context = context()

        AlarmReceiver().handleMainAlarm(
            context, "Parol", "08:00", "", ALARM_ID,
            0L, 0L, 1, null, false, 0,
        )

        assertNotNull("Yeni bir doz için bildirim gösterilmeli", postedNotification(context))
    }

    @Test
    fun alreadyTakenMedicine_doesNotRepostNotification() {
        val context = context()

        val medicine = Medicine("Parol", "08:00", "", "")
        medicine.isTaken = true
        val list = ArrayList<Medicine>()
        list.add(medicine)
        MedicineRepository.saveMedicineList(context, list)

        AlarmReceiver().handleMainAlarm(
            context, "Parol", "08:00", "", ALARM_ID,
            0L, 0L, 1, null, false, 0,
        )

        assertNull(
            "İlaç zaten alınmışsa yeni bildirim gösterilmemeli",
            postedNotification(context),
        )
    }
}
