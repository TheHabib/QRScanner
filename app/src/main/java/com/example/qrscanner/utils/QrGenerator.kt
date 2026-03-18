package com.example.qrscanner.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

object QrGenerator {

    // ── QR Code ─────────────────────────────────────────────────────────────

    fun generateQr(content: String, size: Int = 800): Bitmap? {
        return try {
            val matrix  = QrEncoder.encode(content)
            val modules = matrix.size
            // Add 4-module quiet zone on each side (required by QR spec)
            val quietZone   = 4
            val totalModules = modules + quietZone * 2
            val scale       = size / totalModules
            val actualSize  = scale * totalModules

            val bmp = Bitmap.createBitmap(actualSize, actualSize, Bitmap.Config.RGB_565)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE) // quiet zone is white

            val paint = Paint().apply { color = Color.BLACK; isAntiAlias = false }
            val offset = (quietZone * scale).toFloat()
            for (r in 0 until modules) {
                for (c in 0 until modules) {
                    if (matrix[r][c]) {
                        val x = offset + c * scale
                        val y = offset + r * scale
                        canvas.drawRect(x, y, x + scale, y + scale, paint)
                    }
                }
            }
            bmp
        } catch (e: Exception) {
            android.util.Log.e("QrGenerator", "QR encode failed: ${e.message}", e)
            null
        }
    }

    // ── Barcode (Code 128) ────────────────────────────────────────────────────

    fun generateBarcode(content: String, width: Int = 800, height: Int = 300): Bitmap? {
        return try {
            val bars = encodeCode128(content)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)

            val totalBars = bars.size
            val barWidth = width.toFloat() / totalBars
            val paint = Paint().apply { isAntiAlias = false }

            bars.forEachIndexed { i, dark ->
                paint.color = if (dark) Color.BLACK else Color.WHITE
                canvas.drawRect(
                    i * barWidth, 0f,
                    (i + 1) * barWidth, height.toFloat(),
                    paint
                )
            }
            bmp
        } catch (e: Exception) { null }
    }

    // Code 128B encoding
    private fun encodeCode128(content: String): BooleanArray {
        val CODE128_PATTERNS = arrayOf(
            "11011001100","11001101100","11001100110","10010011000","10010001100",
            "10001001100","10011001000","10011000100","10001100100","11001001000",
            "11001000100","11000100100","10110011100","10011011100","10011001110",
            "10111001100","10011101100","10011100110","11001110010","11001011100",
            "11001001110","11011100100","11001110100","11101101110","11101001100",
            "11100101100","11100100110","11101100100","11100110100","11100110010",
            "11011011000","11011000110","11000110110","10100011000","10001011000",
            "10001000110","10110001000","10001101000","10001100010","11010001000",
            "11000101000","11000100010","10110111000","10110001110","10001101110",
            "10111011000","10111000110","10001110110","11101110110","11010001110",
            "11000101110","11011101000","11011100010","11011101110","11101011000",
            "11101000110","11100010110","11101101000","11101100010","11100011010",
            "11101111010","11001000010","11110001010","10100110000","10100001100",
            "10010110000","10010000110","10000101100","10000100110","10110010000",
            "10110000100","10011010000","10011000010","10000110100","10000110010",
            "11000010010","11001010000","11110111010","11000010100","10001111010",
            "10100111100","10010111100","10010011110","10111100100","10011110100",
            "10011110010","11110100100","11110010100","11110010010","11011011110",
            "11011110110","11110110110","10101111000","10100011110","10001011110",
            "10111101000","10111100010","11110101000","11110100010","10111011110",
            "10111101110","11101011110","11110101110","11010000100","11010010000",
            "11010011100","1100011101011"  // stop pattern
        )
        val START_B = 104
        val STOP    = 106

        val bits = StringBuilder()
        bits.append(CODE128_PATTERNS[START_B])

        var checksum = START_B
        content.forEachIndexed { i, ch ->
            val code = ch.code - 32
            checksum += (i + 1) * code
            bits.append(CODE128_PATTERNS[code])
        }

        bits.append(CODE128_PATTERNS[checksum % 103])
        bits.append(CODE128_PATTERNS[STOP])
        bits.append("11") // final bar

        return BooleanArray(bits.length) { bits[it] == '1' }
    }

    // ── Content builders ─────────────────────────────────────────────────────

    fun buildWifiContent(ssid: String, password: String, type: String): String {
        val t = when (type.uppercase()) {
            "WEP"          -> "WEP"
            "NONE", "OPEN" -> "nopass"
            else           -> "WPA"
        }
        return "WIFI:T:$t;S:$ssid;P:$password;;"
    }

    fun buildEmailContent(to: String, subject: String, body: String): String {
        val parts = mutableListOf<String>()
        if (subject.isNotBlank()) parts.add("SUBJECT:$subject")
        if (body.isNotBlank()) parts.add("BODY:$body")
        return if (parts.isEmpty()) "mailto:$to"
        else "mailto:$to?${parts.joinToString("&")}"
    }

    fun buildSmsContent(phone: String, message: String): String =
        if (message.isBlank()) "sms:$phone" else "smsto:$phone:$message"

    fun buildContactContent(
        firstName: String, lastName: String,
        phone: String, email: String,
        org: String, url: String
    ): String = buildString {
        append("BEGIN:VCARD\nVERSION:3.0\n")
        if (firstName.isNotBlank() || lastName.isNotBlank())
            append("FN:${firstName.trim()} ${lastName.trim()}\nN:$lastName;$firstName;;;\n")
        if (phone.isNotBlank()) append("TEL:$phone\n")
        if (email.isNotBlank()) append("EMAIL:$email\n")
        if (org.isNotBlank()) append("ORG:$org\n")
        if (url.isNotBlank()) append("URL:$url\n")
        append("END:VCARD")
    }

    fun buildLocationContent(lat: String, lng: String): String = "geo:$lat,$lng"

    fun buildCalendarContent(
        title: String, location: String,
        startDate: String, endDate: String, description: String
    ): String = buildString {
        append("BEGIN:VEVENT\n")
        append("SUMMARY:$title\n")
        if (location.isNotBlank()) append("LOCATION:$location\n")
        if (startDate.isNotBlank()) append("DTSTART:$startDate\n")
        if (endDate.isNotBlank()) append("DTEND:$endDate\n")
        if (description.isNotBlank()) append("DESCRIPTION:$description\n")
        append("END:VEVENT")
    }
}
