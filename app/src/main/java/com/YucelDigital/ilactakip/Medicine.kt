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

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
