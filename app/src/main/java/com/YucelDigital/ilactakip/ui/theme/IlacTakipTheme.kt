package com.YucelDigital.ilactakip.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.YucelDigital.ilactakip.R

/**
 * Android 12+ (API 31) cihazlarda, kullanıcı izin verdiyse Material You dynamic color
 * kullanılır (duvar kağıdından türetilen palet — sistem ayarları > duvar kağıdı ve stil).
 * Daha eski cihazlarda veya dynamicColor=false verilirse, aşağıdaki sabit palete düşülür.
 * Sabit palet res/values/colors.xml + res/values-night/colors.xml üzerinden colorResource()
 * ile okunuyor — gece/gündüz varyantı Android kaynak sistemi tarafından otomatik seçiliyor,
 * bu yüzden burada hex değer tekrarlanmıyor. XML tabanlı ekranlarla (AddMedicineActivity,
 * AlarmActivity) aynı paleti paylaşır.
 */
@Composable
fun IlacTakipTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
        darkColorScheme(
            primary = colorResource(R.color.primary),
            onPrimary = colorResource(R.color.text_on_primary),
            secondary = colorResource(R.color.secondary),
            onSecondary = colorResource(R.color.black),
            background = colorResource(R.color.app_background),
            onBackground = colorResource(R.color.text_primary),
            surface = colorResource(R.color.surface_color),
            onSurface = colorResource(R.color.text_primary),
            surfaceVariant = colorResource(R.color.card_background),
            onSurfaceVariant = colorResource(R.color.text_secondary),
            error = colorResource(R.color.error),
            onError = colorResource(R.color.white),
        )
    } else {
        lightColorScheme(
            primary = colorResource(R.color.primary),
            onPrimary = colorResource(R.color.text_on_primary),
            secondary = colorResource(R.color.secondary),
            onSecondary = colorResource(R.color.black),
            background = colorResource(R.color.app_background),
            onBackground = colorResource(R.color.text_primary),
            surface = colorResource(R.color.surface_color),
            onSurface = colorResource(R.color.text_primary),
            surfaceVariant = colorResource(R.color.card_background),
            onSurfaceVariant = colorResource(R.color.text_secondary),
            error = colorResource(R.color.error),
            onError = colorResource(R.color.white),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
