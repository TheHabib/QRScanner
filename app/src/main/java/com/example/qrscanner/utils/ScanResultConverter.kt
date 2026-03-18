package com.example.qrscanner.utils

import com.example.qrscanner.model.HistoryItem
import com.example.qrscanner.model.ScanResult
import com.example.qrscanner.model.ScanResultType
import com.example.qrscanner.model.WifiData

object ScanResultConverter {
    fun toScanResult(item: HistoryItem): ScanResult {
        val wifiData = if (item.type == ScanResultType.WIFI && item.wifiSsid != null) {
            WifiData(
                ssid           = item.wifiSsid,
                password       = item.wifiPassword ?: "",
                encryptionType = item.wifiEncryption ?: "WPA"
            )
        } else null

        return ScanResult(
            rawValue       = item.rawValue,
            type           = item.type,
            displayTitle   = item.displayTitle,
            displaySubtitle = item.displaySubtitle,
            wifiData       = wifiData,
            timestamp      = item.timestamp
        )
    }

    fun toHistoryItem(result: ScanResult): HistoryItem = HistoryItem(
        id             = HistoryManager.newId(),
        rawValue       = result.rawValue,
        type           = result.type,
        displayTitle   = result.displayTitle,
        displaySubtitle = result.displaySubtitle,
        timestamp      = result.timestamp,
        isFavorite     = false,
        wifiSsid       = result.wifiData?.ssid,
        wifiPassword   = result.wifiData?.password,
        wifiEncryption = result.wifiData?.encryptionType
    )
}
