package com.YucelDigital.ilactakip

import java.io.Serializable

class Medicine(
    var name: String,
    var time: String?,
    var dateRange: String?,
    var note: String?,
) : Serializable {

    var isActive: Boolean = true
    var isTaken: Boolean = false
    var startDate: Long = 0
    var endDate: Long = 0
    var intervalDays: Int = 1
    var soundUri: String? = null

    // Güne özel zamanlama
    var isUseCustomDays: Boolean = false
    // Key: Calendar gün sabiti (1=Pazar, 2=Pazartesi, ... 7=Cumartesi)
    // Value: Başlangıç saati "09:00"
    var customDayTimes: HashMap<Int, String>? = null

    /**
     * Tüm alanları kopyalayan bir klon üretir (customDayTimes derin kopyalanır).
     *
     * Neden gerekli: Medicine, Compose tarafından gözlemlenen bir tip değil (düz sınıf,
     * `var` alanlar). Bir ilacın alanını yerinde değiştirmek (`medicine.isActive = ...`)
     * `mutableStateListOf`'un yeniden çizimini TETİKLEMEZ — sadece yapısal liste değişimi
     * (eleman ekle/çıkar/değiştir) tetikler. Bu yüzden UI'da bir alanı güncellerken,
     * listedeki elemanı bu `copy()` ile üretilmiş yeni bir örnekle DEĞİŞTİRİYORUZ; böylece
     * liste yapısal olarak değişmiş sayılıp ilgili kart yeniden çiziliyor.
     */
    fun copy(): Medicine {
        val c = Medicine(name, time, dateRange, note)
        c.isActive = isActive
        c.isTaken = isTaken
        c.startDate = startDate
        c.endDate = endDate
        c.intervalDays = intervalDays
        c.soundUri = soundUri
        c.isUseCustomDays = isUseCustomDays
        c.customDayTimes = customDayTimes?.let { HashMap(it) }
        return c
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
