package com.example.qrscanner.utils

import com.example.qrscanner.model.ScanResult
import com.example.qrscanner.model.ScanResultType
import com.example.qrscanner.model.WifiData
import com.google.mlkit.vision.barcode.common.Barcode

object BarcodeParser {

    fun parse(barcode: Barcode): ScanResult {
        val rawValue = barcode.rawValue ?: ""

        return when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                val url = barcode.url
                ScanResult(
                    rawValue = rawValue,
                    type = ScanResultType.URL,
                    displayTitle = "Web URL",
                    displaySubtitle = url?.url ?: rawValue
                )
            }
            Barcode.TYPE_WIFI -> {
                val wifi = barcode.wifi
                val ssid = wifi?.ssid ?: ""
                val password = wifi?.password ?: ""
                val encType = when (wifi?.encryptionType) {
                    Barcode.WiFi.TYPE_WEP -> "WEP"
                    Barcode.WiFi.TYPE_WPA -> "WPA"
                    else -> "Open"
                }
                ScanResult(
                    rawValue = rawValue,
                    type = ScanResultType.WIFI,
                    displayTitle = "Wi-Fi Network",
                    displaySubtitle = ssid,
                    wifiData = WifiData(ssid, password, encType)
                )
            }
            Barcode.TYPE_EMAIL -> {
                val email = barcode.email
                ScanResult(
                    rawValue = rawValue,
                    type = ScanResultType.EMAIL,
                    displayTitle = "Email",
                    displaySubtitle = email?.address ?: rawValue
                )
            }
            Barcode.TYPE_PHONE -> {
                val phone = barcode.phone
                ScanResult(
                    rawValue = rawValue,
                    type = ScanResultType.PHONE,
                    displayTitle = "Phone Number",
                    displaySubtitle = phone?.number ?: rawValue
                )
            }
            Barcode.TYPE_SMS -> {
                val sms = barcode.sms
                ScanResult(
                    rawValue = rawValue,
                    type = ScanResultType.SMS,
                    displayTitle = "SMS",
                    displaySubtitle = sms?.phoneNumber ?: rawValue
                )
            }
            Barcode.TYPE_GEO -> {
                val geo = barcode.geoPoint
                ScanResult(
                    rawValue = rawValue,
                    type = ScanResultType.GEO,
                    displayTitle = "Location",
                    displaySubtitle = "Lat: ${geo?.lat}, Lng: ${geo?.lng}"
                )
            }
            Barcode.TYPE_CONTACT_INFO -> {
                val contact = barcode.contactInfo
                val name = contact?.name?.formattedName ?: "Unknown"
                ScanResult(
                    rawValue = rawValue,
                    type = ScanResultType.CONTACT,
                    displayTitle = "Contact",
                    displaySubtitle = name
                )
            }
            Barcode.TYPE_CALENDAR_EVENT -> {
                val cal = barcode.calendarEvent
                ScanResult(
                    rawValue = rawValue,
                    type = ScanResultType.CALENDAR,
                    displayTitle = "Calendar Event",
                    displaySubtitle = cal?.summary ?: rawValue
                )
            }
            else -> {
                // Try to detect app store links or other patterns from raw text
                val type = detectTypeFromRaw(rawValue)
                ScanResult(
                    rawValue = rawValue,
                    type = type,
                    displayTitle = getLabelForType(type),
                    displaySubtitle = rawValue
                )
            }
        }
    }

    private fun detectTypeFromRaw(raw: String): ScanResultType {
        return when {
            raw.startsWith("http://") || raw.startsWith("https://") -> ScanResultType.URL
            raw.startsWith("mailto:") -> ScanResultType.EMAIL
            raw.startsWith("tel:") -> ScanResultType.PHONE
            raw.startsWith("smsto:") || raw.startsWith("sms:") -> ScanResultType.SMS
            raw.startsWith("geo:") -> ScanResultType.GEO
            raw.startsWith("BEGIN:VCARD") -> ScanResultType.CONTACT
            raw.startsWith("BEGIN:VEVENT") -> ScanResultType.CALENDAR
            raw.startsWith("WIFI:") -> ScanResultType.WIFI
            else -> ScanResultType.TEXT
        }
    }

    private fun getLabelForType(type: ScanResultType): String {
        return when (type) {
            ScanResultType.URL -> "Web URL"
            ScanResultType.EMAIL -> "Email"
            ScanResultType.PHONE -> "Phone Number"
            ScanResultType.SMS -> "SMS"
            ScanResultType.GEO -> "Location"
            ScanResultType.CONTACT -> "Contact"
            ScanResultType.CALENDAR -> "Calendar Event"
            ScanResultType.WIFI -> "Wi-Fi Network"
            ScanResultType.APP -> "App Link"
            ScanResultType.TEXT -> "Text"
        }
    }
}
