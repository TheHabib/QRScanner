package com.example.qrscanner.ui.create

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.qrscanner.R
import com.example.qrscanner.databinding.ActivityCreateQrBinding
import com.example.qrscanner.utils.AccentApplier
import com.example.qrscanner.utils.QrGenerator
import java.io.File
import java.io.FileOutputStream

class CreateQrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateQrBinding
    private var currentBitmap: Bitmap? = null
    private var currentContent: String = ""
    private var selectedType = QrType.URL
    private var isBarcode = false

    enum class QrType {
        URL, TEXT, WIFI, EMAIL, PHONE, SMS, CONTACT, LOCATION, BARCODE, CALENDAR
    }

    private val storagePermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) saveToGallery()
        else Toast.makeText(this, "Storage permission needed to save", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateQrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupTypePicker()
        setupGenerateButton()
        setupActions()
        applyAccentColor()
        showFormFor(QrType.URL)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create QR Code"
    }

    private fun applyAccentColor() {
        AccentApplier.tintText(binding.tvFormLabel, this)
    }

    // ── Type picker chips ────────────────────────────────────────────────────

    private val typeMap = listOf(
        QrType.URL      to "URL",
        QrType.TEXT     to "Text",
        QrType.WIFI     to "Wi-Fi",
        QrType.EMAIL    to "Email",
        QrType.PHONE    to "Phone",
        QrType.SMS      to "SMS",
        QrType.CONTACT  to "Contact",
        QrType.LOCATION to "Location",
        QrType.BARCODE  to "Barcode",
        QrType.CALENDAR to "Calendar"
    )

    private fun setupTypePicker() {
        typeMap.forEach { (type, label) ->
            val chip = layoutInflater.inflate(
                R.layout.item_filter_chip, binding.chipGroupType, false
            ) as android.widget.TextView
            chip.text = label
            chip.tag  = type
            chip.setOnClickListener {
                selectedType = type
                isBarcode = (type == QrType.BARCODE)
                updateTypeChips()
                showFormFor(type)
                binding.cardQrResult.visibility = View.GONE
                currentBitmap = null
            }
            binding.chipGroupType.addView(chip)
        }
        updateTypeChips()
    }

    private fun updateTypeChips() {
        val accent = android.graphics.Color.parseColor(
            com.example.qrscanner.utils.AppSettings.getAccentColor(this))
        val cornerRadius = resources.displayMetrics.density * 16
        for (i in 0 until binding.chipGroupType.childCount) {
            val chip = binding.chipGroupType.getChildAt(i) as? android.widget.TextView ?: continue
            val isSelected = chip.tag == selectedType
            if (isSelected) {
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    this.cornerRadius = cornerRadius
                    setColor(android.graphics.Color.argb(40,
                        android.graphics.Color.red(accent),
                        android.graphics.Color.green(accent),
                        android.graphics.Color.blue(accent)))
                    setStroke((resources.displayMetrics.density * 1.5f).toInt(), accent)
                }
                chip.background = bg
                chip.setTextColor(accent)
            } else {
                chip.setBackgroundResource(R.drawable.chip_unselected_bg)
                chip.setTextColor(android.graphics.Color.parseColor("#99FFFFFF"))
            }
        }
    }

    // ── Form visibility ──────────────────────────────────────────────────────

    private fun showFormFor(type: QrType) {
        // Hide all form groups
        listOf(
            binding.groupUrl, binding.groupText, binding.groupWifi,
            binding.groupEmail, binding.groupPhone, binding.groupSms,
            binding.groupContact, binding.groupLocation,
            binding.groupBarcode, binding.groupCalendar
        ).forEach { it.visibility = View.GONE }

        binding.tvFormLabel.text = when (type) {
            QrType.URL      -> "Website URL"
            QrType.TEXT     -> "Text"
            QrType.WIFI     -> "Wi-Fi Network"
            QrType.EMAIL    -> "Email"
            QrType.PHONE    -> "Phone Number"
            QrType.SMS      -> "SMS"
            QrType.CONTACT  -> "Contact (vCard)"
            QrType.LOCATION -> "Location"
            QrType.BARCODE  -> "Barcode (Code 128)"
            QrType.CALENDAR -> "Calendar Event"
        }

        val group = when (type) {
            QrType.URL      -> binding.groupUrl
            QrType.TEXT     -> binding.groupText
            QrType.WIFI     -> binding.groupWifi
            QrType.EMAIL    -> binding.groupEmail
            QrType.PHONE    -> binding.groupPhone
            QrType.SMS      -> binding.groupSms
            QrType.CONTACT  -> binding.groupContact
            QrType.LOCATION -> binding.groupLocation
            QrType.BARCODE  -> binding.groupBarcode
            QrType.CALENDAR -> binding.groupCalendar
        }
        group.visibility = View.VISIBLE
    }

    // ── Generate ─────────────────────────────────────────────────────────────

    private fun setupGenerateButton() {
        binding.btnGenerate.setOnClickListener { generateQr() }
    }

    private fun generateQr() {
        val content = buildContent() ?: return
        if (content.isBlank()) {
            Toast.makeText(this, "Please fill in the required fields", Toast.LENGTH_SHORT).show()
            return
        }
        currentContent = content

        val bitmap = if (isBarcode) {
            QrGenerator.generateBarcode(content)
        } else {
            QrGenerator.generateQr(content)
        }

        if (bitmap == null) {
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show()
            return
        }
        currentBitmap = bitmap
        binding.ivQrCode.setImageBitmap(bitmap)
        binding.cardQrResult.visibility = View.VISIBLE
        binding.tvGeneratedContent.text = content

        // Scroll to result
        binding.scrollView.post {
            binding.scrollView.smoothScrollTo(0, binding.cardQrResult.top)
        }
    }

    private fun buildContent(): String? = when (selectedType) {
        QrType.URL -> {
            val url = binding.etUrl.text.toString().trim()
            if (url.isEmpty()) { toast("Enter a URL"); null }
            else if (!url.startsWith("http")) "https://$url" else url
        }
        QrType.TEXT -> {
            val t = binding.etText.text.toString().trim()
            if (t.isEmpty()) { toast("Enter some text"); null } else t
        }
        QrType.WIFI -> {
            val ssid = binding.etWifiSsid.text.toString().trim()
            if (ssid.isEmpty()) { toast("Enter network name"); null }
            else {
                val type = when (binding.rgWifiType.checkedRadioButtonId) {
                    R.id.rbWep  -> "WEP"
                    R.id.rbOpen -> "nopass"
                    else        -> "WPA"
                }
                QrGenerator.buildWifiContent(ssid, binding.etWifiPassword.text.toString(), type)
            }
        }
        QrType.EMAIL -> {
            val to = binding.etEmailTo.text.toString().trim()
            if (to.isEmpty()) { toast("Enter email address"); null }
            else QrGenerator.buildEmailContent(
                to,
                binding.etEmailSubject.text.toString(),
                binding.etEmailBody.text.toString()
            )
        }
        QrType.PHONE -> {
            val p = binding.etPhone.text.toString().trim()
            if (p.isEmpty()) { toast("Enter phone number"); null } else "tel:$p"
        }
        QrType.SMS -> {
            val p = binding.etSmsPhone.text.toString().trim()
            if (p.isEmpty()) { toast("Enter phone number"); null }
            else QrGenerator.buildSmsContent(p, binding.etSmsMessage.text.toString())
        }
        QrType.CONTACT -> {
            val first = binding.etContactFirst.text.toString().trim()
            val last  = binding.etContactLast.text.toString().trim()
            if (first.isEmpty() && last.isEmpty()) { toast("Enter a name"); null }
            else QrGenerator.buildContactContent(
                first, last,
                binding.etContactPhone.text.toString(),
                binding.etContactEmail.text.toString(),
                binding.etContactOrg.text.toString(),
                binding.etContactUrl.text.toString()
            )
        }
        QrType.LOCATION -> {
            val lat = binding.etLat.text.toString().trim()
            val lng = binding.etLng.text.toString().trim()
            if (lat.isEmpty() || lng.isEmpty()) { toast("Enter latitude and longitude"); null }
            else QrGenerator.buildLocationContent(lat, lng)
        }
        QrType.BARCODE -> {
            val b = binding.etBarcodeValue.text.toString().trim()
            if (b.isEmpty()) { toast("Enter barcode value"); null } else b
        }
        QrType.CALENDAR -> {
            val title = binding.etCalTitle.text.toString().trim()
            if (title.isEmpty()) { toast("Enter event title"); null }
            else QrGenerator.buildCalendarContent(
                title,
                binding.etCalLocation.text.toString(),
                binding.etCalStart.text.toString(),
                binding.etCalEnd.text.toString(),
                binding.etCalDesc.text.toString()
            )
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private fun setupActions() {
        binding.btnSaveGallery.setOnClickListener {
            if (currentBitmap == null) return@setOnClickListener
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                storagePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                saveToGallery()
            }
        }

        binding.btnShareQr.setOnClickListener {
            shareQr()
        }

        binding.btnCopyContent.setOnClickListener {
            val cb = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cb.setPrimaryClip(android.content.ClipData.newPlainText("QR Content", currentContent))
            Toast.makeText(this, "Content copied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToGallery() {
        val bmp = currentBitmap ?: return
        try {
            val filename = "QR_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/QRScanner")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "QRScanner"
                ).also { it.mkdirs() }
                val file = File(dir, filename)
                FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                // Notify gallery
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
            }
            Toast.makeText(this, "Saved to Gallery", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareQr() {
        val bmp = currentBitmap ?: return
        try {
            val file = File(cacheDir, "qr_share.png")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(
                this, "${packageName}.provider", file
            )
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Share QR Code"
            ))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
