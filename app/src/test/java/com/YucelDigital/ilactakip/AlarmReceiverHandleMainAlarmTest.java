package com.YucelDigital.ilactakip;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35) // Robolectric 4.14.1'in desteklediği en yüksek SDK (proje targetSdk 36)
public class AlarmReceiverHandleMainAlarmTest {

    private static final int ALARM_ID = 4242;

    private Context context() {
        return ApplicationProvider.getApplicationContext();
    }

    private Notification postedNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return Shadows.shadowOf(nm).getNotification(ALARM_ID);
    }

    @Test
    public void notification_neverCarriesItsOwnSound() {
        Context context = context();

        new AlarmReceiver().handleMainAlarm(context, "Parol", "08:00", "", ALARM_ID,
                0L, 0L, 1, "content://media/internal/audio/media/1", false, 0);

        Notification notification = postedNotification(context);
        assertNotNull(notification);
        assertNull("Bildirim kendi sesini çalmamalı — sesin tek kaynağı AlarmActivity olmalı",
                notification.sound);
    }

    @Test
    public void notification_isPostedForNewDose() {
        Context context = context();

        new AlarmReceiver().handleMainAlarm(context, "Parol", "08:00", "", ALARM_ID,
                0L, 0L, 1, null, false, 0);

        assertNotNull("Yeni bir doz için bildirim gösterilmeli", postedNotification(context));
    }

    @Test
    public void alreadyTakenMedicine_doesNotRepostNotification() {
        Context context = context();

        Medicine medicine = new Medicine("Parol", "08:00", "", "");
        medicine.setTaken(true);
        List<Medicine> list = new ArrayList<>();
        list.add(medicine);
        MedicineRepository.saveMedicineList(context, list);

        new AlarmReceiver().handleMainAlarm(context, "Parol", "08:00", "", ALARM_ID,
                0L, 0L, 1, null, false, 0);

        assertNull("İlaç zaten alınmışsa yeni bildirim gösterilmemeli",
                postedNotification(context));
    }
}
