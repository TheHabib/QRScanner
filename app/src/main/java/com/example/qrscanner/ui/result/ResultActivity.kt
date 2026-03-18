package com.example.qrscanner.ui.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qrscanner.R
import com.example.qrscanner.databinding.ActivityResultBinding
import com.example.qrscanner.model.ScanResult
import com.example.qrscanner.model.ScanResultType
import com.example.qrscanner.utils.AppSettings
import com.example.qrscanner.utils.AccentApplier
import com.example.qrscanner.utils.HistoryManager
import com.example.qrscanner.utils.ScanResultConverter
import android.graphics.Color

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var scanResult: ScanResult
    private var historyItemId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_SCAN_RESULT, ScanResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_SCAN_RESULT)
        } ?: run {
            finish()
            return
        }

        setupToolbar()
        displayResult()
        setupActions()
        applyAccentColor()
        saveToHistory()
        setupStarButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
    }

    private fun displayResult() {
        // Type icon and label
        binding.tvTypeLabel.text = scanResult.displayTitle
        binding.ivTypeIcon.setImageResource(getTypeIcon(scanResult.type))

        // Main content
        binding.tvResultContent.text = scanResult.rawValue

        // Subtitle
        if (scanResult.displaySubtitle != scanResult.rawValue) {
            binding.tvResultSubtitle.visibility = View.VISIBLE
            binding.tvResultSubtitle.text = scanResult.displaySubtitle
        } else {
            binding.tvResultSubtitle.visibility = View.GONE
        }

        // WiFi specific UI
        if (scanResult.type == ScanResultType.WIFI) {
            setupWifiUI()
        } else {
            binding.btnOpen.visibility = View.VISIBLE
            binding.btnConnect.visibility = View.GONE
            binding.btnCopyPassword.visibility = View.GONE
            binding.tvOpenLabel.text = getOpenLabel(scanResult.type)
            binding.ivOpenIcon.setImageResource(getOpenIcon(scanResult.type))
        }
    }

    private fun setupWifiUI() {
        val wifi = scanResult.wifiData ?: return

        binding.btnOpen.visibility = View.GONE
        binding.btnConnect.visibility = View.VISIBLE
        binding.btnCopyPassword.visibility = if (wifi.password.isNotEmpty()) View.VISIBLE else View.GONE

        // Show wifi details card
        binding.wifiDetailsCard.visibility = View.VISIBLE
        binding.tvWifiSsid.text = wifi.ssid
        binding.tvWifiEncryption.text = wifi.encryptionType
        binding.tvWifiPassword.text = if (wifi.password.isEmpty()) "None (Open)" else "••••••••"

        binding.btnTogglePassword.setOnClickListener {
            val isShowing = binding.tvWifiPassword.tag == "shown"
            if (isShowing) {
                binding.tvWifiPassword.text = "••••••••"
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
                binding.tvWifiPassword.tag = "hidden"
            } else {
                binding.tvWifiPassword.text = wifi.password
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye)
                binding.tvWifiPassword.tag = "shown"
            }
        }
    }

    private fun setupActions() {
        // Open / Connect
        binding.btnOpen.setOnClickListener { openResult() }
        binding.btnConnect.setOnClickListener { connectToWifi() }

        // Share
        binding.btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, scanResult.rawValue)
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        // Copy
        binding.btnCopy.setOnClickListener {
            copyToClipboard("Scanned Content", scanResult.rawValue)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Copy Password (WiFi only)
        binding.btnCopyPassword.setOnClickListener {
            val password = scanResult.wifiData?.password ?: ""
            copyToClipboard("WiFi Password", password)
            Toast.makeText(this, "Password copied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openResult() {
        try {
            val intent = when (scanResult.type) {
                ScanResultType.URL -> {
                    Intent(Intent.ACTION_VIEW, Uri.parse(scanResult.rawValue))
                }
                ScanResultType.EMAIL -> {
                    val email = scanResult.rawValue.removePrefix("mailto:")
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                }
                ScanResultType.PHONE -> {
                    val phone = scanResult.rawValue.removePrefix("tel:")
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                }
                ScanResultType.SMS -> {
                    Intent(Intent.ACTION_VIEW, Uri.parse(scanResult.rawValue))
                }
                ScanResultType.GEO -> {
                    Intent(Intent.ACTION_VIEW, Uri.parse(scanResult.rawValue))
                }
                ScanResultType.CONTACT -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        type = ContactsContract.Contacts.CONTENT_TYPE
                    }
                }
                ScanResultType.CALENDAR -> {
                    Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                    }
                }
                ScanResultType.APP -> {
                    Intent(Intent.ACTION_VIEW, Uri.parse(scanResult.rawValue))
                }
                else -> {
                    // Generic: search the text
                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra("query", scanResult.rawValue)
                    }
                }
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open this content", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun connectToWifi() {
        val wifi = scanResult.wifiData ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses WifiNetworkSuggestion
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(wifi.ssid)
                .apply {
                    when (wifi.encryptionType) {
                        "WPA" -> setWpa2Passphrase(wifi.password)
                        "WEP" -> { /* WEP not supported in suggestions API */ }
                    }
                }
                .build()

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val suggestions = listOf(suggestion)
            wifiManager.removeNetworkSuggestions(suggestions)
            val status = wifiManager.addNetworkSuggestions(suggestions)

            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                Toast.makeText(
                    this,
                    "Network '${wifi.ssid}' added. Check your Wi-Fi settings to connect.",
                    Toast.LENGTH_LONG
                ).show()

                // Open WiFi settings
                val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Failed to add network. Please connect manually.", Toast.LENGTH_LONG).show()
                val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                startActivity(intent)
            }
        } else {
            // Android 9 and below
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val config = WifiConfiguration().apply {
                SSID = "\"${wifi.ssid}\""
                when (wifi.encryptionType) {
                    "WPA" -> {
                        preSharedKey = "\"${wifi.password}\""
                        allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                        allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                        allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                        allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                    }
                    "WEP" -> {
                        wepKeys[0] = "\"${wifi.password}\""
                        wepTxKeyIndex = 0
                        allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                        allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                    }
                    else -> {
                        allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    }
                }
            }

            val networkId = wifiManager.addNetwork(config)
            if (networkId != -1) {
                wifiManager.disconnect()
                wifiManager.enableNetwork(networkId, true)
                wifiManager.reconnect()
                Toast.makeText(this, "Connecting to '${wifi.ssid}'...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to connect. Please try manually.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveToHistory() {
        val item = ScanResultConverter.toHistoryItem(scanResult)
        historyItemId = item.id
        HistoryManager.addItem(this, item)
    }

    private fun setupStarButton() {
        val id = historyItemId ?: return
        updateStarIcon(HistoryManager.getAll(this).find { it.id == id }?.isFavorite == true)
        binding.btnFavorite.setOnClickListener {
            val nowFav = HistoryManager.toggleFavorite(this, id)
            updateStarIcon(nowFav)
            android.widget.Toast.makeText(
                this,
                if (nowFav) "Added to Favorites" else "Removed from Favorites",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateStarIcon(isFavorite: Boolean) {
        binding.btnFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
        if (isFavorite) AccentApplier.tintImage(binding.btnFavorite, this)
        else binding.btnFavorite.imageTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
    }

    private fun applyAccentColor() {
        // Type icon circle tint
        AccentApplier.tintImage(binding.ivTypeIcon, this)
        // "CONTENT" section label
        AccentApplier.tintText(binding.tvContentLabel, this)
        // "WIFI DETAILS" label if visible
        if (binding.wifiDetailsCard.visibility == android.view.View.VISIBLE) {
            AccentApplier.tintText(binding.tvWifiLabel, this)
        }
        // Connect button icon + text
        AccentApplier.tintImage(binding.ivConnectIcon, this)
        AccentApplier.tintText(binding.tvConnectLabel, this)
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    private fun getTypeIcon(type: ScanResultType): Int {
        return when (type) {
            ScanResultType.URL -> R.drawable.ic_type_url
            ScanResultType.WIFI -> R.drawable.ic_type_wifi
            ScanResultType.EMAIL -> R.drawable.ic_type_email
            ScanResultType.PHONE -> R.drawable.ic_type_phone
            ScanResultType.SMS -> R.drawable.ic_type_sms
            ScanResultType.GEO -> R.drawable.ic_type_location
            ScanResultType.CONTACT -> R.drawable.ic_type_contact
            ScanResultType.CALENDAR -> R.drawable.ic_type_calendar
            ScanResultType.APP -> R.drawable.ic_type_app
            ScanResultType.TEXT -> R.drawable.ic_type_text
        }
    }

    private fun getOpenLabel(type: ScanResultType): String {
        return when (type) {
            ScanResultType.URL -> "Open"
            ScanResultType.EMAIL -> "Send Email"
            ScanResultType.PHONE -> "Call"
            ScanResultType.SMS -> "Message"
            ScanResultType.GEO -> "Maps"
            ScanResultType.CONTACT -> "Add Contact"
            ScanResultType.CALENDAR -> "Add Event"
            ScanResultType.APP -> "Open"
            else -> "Open"
        }
    }

    private fun getOpenIcon(type: ScanResultType): Int {
        return when (type) {
            ScanResultType.URL -> R.drawable.ic_open_browser
            ScanResultType.EMAIL -> R.drawable.ic_open_email
            ScanResultType.PHONE -> R.drawable.ic_open_phone
            ScanResultType.SMS -> R.drawable.ic_open_sms
            ScanResultType.GEO -> R.drawable.ic_open_maps
            ScanResultType.CONTACT -> R.drawable.ic_open_contact
            ScanResultType.CALENDAR -> R.drawable.ic_open_calendar
            else -> R.drawable.ic_open_browser
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_SCAN_RESULT = "extra_scan_result"
    }
}
