package com.YucelDigital.ilactakip

import android.content.Context
import android.text.format.DateFormat
import java.util.Calendar

/**
 * Saklanan 24 saatlik "HH:mm" saatini, cihazın 12/24 saat tercihine göre biçimlendirir
 * (örn. 24h cihazda "20:00", 12h cihazda "8:00 PM").
 *
 * ÖNEMLİ: Saklama her zaman 24 saatlik "HH:mm" olarak kalır — bu fonksiyon YALNIZCA gösterim
 * içindir. Alarm kurulurken kullanılan mutlak an, biçimden tamamen bağımsızdır. Bozuk/eksik
 * girdide ham metni geri döndürür (asla çökmez).
 */
fun formatTimeForDisplay(context: Context, hhmm: String): String {
    val parts = hhmm.trim().split(":")
    if (parts.size != 2) return hhmm.trim()
    val hour = parts[0].toIntOrNull() ?: return hhmm.trim()
    val minute = parts[1].toIntOrNull() ?: return hhmm.trim()
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    return DateFormat.getTimeFormat(context).format(cal.time)
}

/**
 * Virgülle ayrılmış çoklu saat metnini ("08:00, 20:00") her parçasını cihaz formatına
 * çevirerek biçimlendirir.
 */
fun formatTimesForDisplay(context: Context, times: String): String =
    times.split(", ").joinToString(", ") { formatTimeForDisplay(context, it) }
