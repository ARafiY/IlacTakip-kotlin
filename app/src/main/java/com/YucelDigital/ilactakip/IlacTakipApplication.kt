package com.YucelDigital.ilactakip

import android.app.Application
import com.google.android.material.color.DynamicColors

/**
 * Hâlâ XML tabanlı olan ekranlarda (AddMedicineActivity, AlarmActivity) Material You
 * dynamic color'ı (Android 12+ duvar kağıdı paleti) otomatik uygular — Compose tarafında
 * (MainActivity) bunu IlacTakipTheme kendi başına, dynamicColorScheme() ile yapıyor zaten.
 */
class IlacTakipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
