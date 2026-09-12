package com.YucelDigital.ilactakip

import java.io.Serializable

class Medicine(
    var name: String,
    var time: String?,
    var dateRange: String?,
    var note: String?,
) : Serializable {

    var id: String = java.util.UUID.randomUUID().toString()
    var isActive: Boolean = true
    var isTaken: Boolean = false
    var startDate: Long = 0
    var endDate: Long = 0
    var intervalDays: Int = 1
    var soundUri: String? = null

    // Açlık / Tokluk durumu (Aç Karnına, Tok Karnına, Fark Etmez)
    var mealTiming: String? = null

    // Kalan tablet / adet sayısı (null = stok takibi kapalı)
    var stockCount: Int? = null

    // Güne özel zamanlama
    var isUseCustomDays: Boolean = false
    // Key: Calendar gün sabiti (1=Pazar, 2=Pazartesi, ... 7=Cumartesi)
    // Value: Başlangıç saati "09:00"
    var customDayTimes: HashMap<Int, String>? = null

    /**
     * Tüm alanları kopyalayan bir klon üretir (customDayTimes derin kopyalanır).
     */
    fun copy(): Medicine {
        val c = Medicine(name, time, dateRange, note)
        c.id = if (id.isNullOrEmpty()) java.util.UUID.randomUUID().toString() else id
        c.isActive = isActive
        c.isTaken = isTaken
        c.startDate = startDate
        c.endDate = endDate
        c.intervalDays = intervalDays
        c.soundUri = soundUri
        c.mealTiming = mealTiming
        c.stockCount = stockCount
        c.isUseCustomDays = isUseCustomDays
        c.customDayTimes = customDayTimes?.let { HashMap(it) }
        return c
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
