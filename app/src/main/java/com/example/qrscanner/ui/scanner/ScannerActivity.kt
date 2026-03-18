package com.example.qrscanner.ui.scanner

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.qrscanner.R
import com.example.qrscanner.databinding.ActivityScannerBinding
import com.example.qrscanner.ui.about.AboutActivity
import com.example.qrscanner.ui.result.ResultActivity
import com.example.qrscanner.ui.settings.SettingsActivity
import com.example.qrscanner.ui.history.HistoryActivity
import com.example.qrscanner.ui.favorites.FavoritesActivity
import com.example.qrscanner.utils.AppSettings
import com.example.qrscanner.utils.AccentApplier
import com.example.qrscanner.utils.BarcodeParser
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private var scanLineAnimator: AnimatorSet? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null

    private var isFrontCamera = false
    private var isTorchOn = false
    private var isScanning = true
    private var isProcessing = false

    private val barcodeOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_EAN_8, Barcode.FORMAT_EAN_13, Barcode.FORMAT_ITF,
            Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_PDF417, Barcode.FORMAT_DATA_MATRIX
        ).build()

    private val barcodeScanner = BarcodeScanning.getClient(barcodeOptions)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else { Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show(); finish() }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { processImageFromGallery(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved theme
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            when (com.example.qrscanner.utils.AppSettings.getTheme(this)) {
                com.example.qrscanner.utils.AppSettings.THEME_LIGHT  -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                com.example.qrscanner.utils.AppSettings.THEME_DARK   -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        checkCameraPermission()
        startScanLineAnimation()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onResume() {
        super.onResume()
        isScanning = true
        isProcessing = false
        applyScanFrameColor()
        binding.scanLine.visibility = View.VISIBLE
        startScanLineAnimation()
    }

    // ── Apply accent color to scan frame and scan line ──────────────────────
    private fun applyScanFrameColor() {
        binding.scanFrame.setImageResource(R.drawable.scan_frame)
        val csl = AccentApplier.csl(this)
        binding.scanFrame.imageTintList = csl
        binding.scanLine.backgroundTintList = csl
        // Zoom seekbar dot + track
        AccentApplier.tintSeekBar(binding.seekbarZoom, this)
        // Nav drawer icon tint
        binding.navView.itemIconTintList = csl
    }

    private fun setupUI() {
        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(binding.navView) }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnTorch.setOnClickListener { toggleTorch() }
        binding.btnFlip.setOnClickListener {
            isFrontCamera = !isFrontCamera
            if (isFrontCamera && isTorchOn) {
                isTorchOn = false
                binding.btnTorch.setImageResource(R.drawable.ic_torch_off)
            }
            binding.btnTorch.isEnabled = !isFrontCamera
            binding.btnTorch.alpha = if (isFrontCamera) 0.4f else 1.0f
            startCamera()
        }
        binding.seekbarZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) camera?.cameraControl?.setLinearZoom(progress / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_history   -> startActivity(Intent(this, HistoryActivity::class.java))
                R.id.nav_favorites -> startActivity(Intent(this, FavoritesActivity::class.java))
                R.id.nav_create_qr -> Toast.makeText(this, "QR Generator coming soon", Toast.LENGTH_SHORT).show()
                R.id.nav_settings  -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_about     -> startActivity(Intent(this, AboutActivity::class.java))
            }
            binding.drawerLayout.closeDrawers()
            true
        }
    }

    private fun startScanLineAnimation() {
        scanLineAnimator?.cancel()
        val anim = AnimatorInflater.loadAnimator(this, R.animator.scan_line_anim) as AnimatorSet
        anim.setTarget(binding.scanLine)
        anim.start()
        scanLineAnimator = anim
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) startCamera()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ cameraProvider = future.get(); bindCameraUseCases() },
            ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        provider.unbindAll()
        val cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                             else CameraSelector.DEFAULT_BACK_CAMERA
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy -> analyzeImage(imageProxy) }
            }
        try {
            camera = provider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            binding.seekbarZoom.progress = 0
            camera?.cameraControl?.setLinearZoom(0f)
        } catch (e: Exception) { Log.e(TAG, "Camera bind failed", e) }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(imageProxy: ImageProxy) {
        if (!isScanning || isProcessing) { imageProxy.close(); return }
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        isProcessing = true
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    barcodes.first().rawValue?.let {
                        isScanning = false
                        val result = BarcodeParser.parse(barcodes.first())
                        runOnUiThread {
                            onScanSuccess(result)
                        }
                    }
                }
            }
            .addOnCompleteListener { isProcessing = false; imageProxy.close() }
    }

    private fun onScanSuccess(result: com.example.qrscanner.model.ScanResult) {
        // Visual feedback
        scanLineAnimator?.cancel()
        val accentColor = Color.parseColor(AppSettings.getAccentColor(this))
        val csl = android.content.res.ColorStateList.valueOf(accentColor)
        binding.scanFrame.setImageResource(R.drawable.scan_frame_success)
        binding.scanFrame.imageTintList = csl
        binding.scanLine.visibility = View.INVISIBLE

        // Beep
        if (AppSettings.isBeepEnabled(this)) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } catch (e: Exception) { Log.e(TAG, "Beep failed", e) }
        }

        // Vibrate
        if (AppSettings.isVibrateEnabled(this)) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(100)
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "Vibrate failed", e) }
        }

        // Auto copy to clipboard
        if (AppSettings.isCopyClipboardEnabled(this)) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Scanned Content", result.rawValue))
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        navigateToResult(result)
    }

    private fun processImageFromGallery(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) onScanSuccess(BarcodeParser.parse(barcodes.first()))
                    else Toast.makeText(this, "No QR/Barcode found in image", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleTorch() {
        if (isFrontCamera) return
        isTorchOn = !isTorchOn
        camera?.cameraControl?.enableTorch(isTorchOn)
        binding.btnTorch.setImageResource(if (isTorchOn) R.drawable.ic_torch_on else R.drawable.ic_torch_off)
    }

    private fun navigateToResult(result: com.example.qrscanner.model.ScanResult) {
        startActivity(Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SCAN_RESULT, result)
        })
    }

    override fun onPause() { super.onPause(); scanLineAnimator?.cancel() }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }

    companion object { private const val TAG = "ScannerActivity" }
}
