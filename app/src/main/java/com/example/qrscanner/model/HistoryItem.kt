package com.example.qrscanner.model

data class HistoryItem(
    val id: String,                    // UUID
    val rawValue: String,
    val type: ScanResultType,
    val displayTitle: String,          // e.g. "URL", "Wi-Fi", "Text"
    val displaySubtitle: String,       // short preview
    val timestamp: Long,               // epoch millis
    val isFavorite: Boolean = false,
    val wifiSsid: String? = null,
    val wifiPassword: String? = null,
    val wifiEncryption: String? = null
)
