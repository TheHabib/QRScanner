package com.example.qrscanner.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class ScanResultType {
    URL,
    WIFI,
    EMAIL,
    PHONE,
    SMS,
    GEO,
    CONTACT,
    CALENDAR,
    APP,
    TEXT
}

@Parcelize
data class WifiData(
    val ssid: String,
    val password: String,
    val encryptionType: String // WPA, WEP, nopass
) : Parcelable

@Parcelize
data class ScanResult(
    val rawValue: String,
    val type: ScanResultType,
    val displayTitle: String,
    val displaySubtitle: String,
    val wifiData: WifiData? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
