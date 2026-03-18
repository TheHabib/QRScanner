package com.example.qrscanner.ui.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.qrscanner.R
import com.example.qrscanner.databinding.ActivitySettingsBinding
import com.example.qrscanner.utils.AppSettings
import com.example.qrscanner.utils.AccentApplier

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var selectedColor: String = AppSettings.DEFAULT_ACCENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        loadSettings()
        setupColorGrid()
        setupListeners()
        applyAccentColor()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
    }

    private fun loadSettings() {
        selectedColor = AppSettings.getAccentColor(this)
        when (AppSettings.getTheme(this)) {
            AppSettings.THEME_LIGHT -> binding.rgTheme.check(R.id.rbLight)
            AppSettings.THEME_DARK  -> binding.rgTheme.check(R.id.rbDark)
            else                    -> binding.rgTheme.check(R.id.rbSystem)
        }
        binding.switchBeep.isChecked          = AppSettings.isBeepEnabled(this)
        binding.switchVibrate.isChecked       = AppSettings.isVibrateEnabled(this)
        binding.switchCopyClipboard.isChecked = AppSettings.isCopyClipboardEnabled(this)
        binding.switchUrlInfo.isChecked       = AppSettings.isUrlInfoEnabled(this)
    }

    private fun setupColorGrid() {
        val container = binding.colorGrid
        container.removeAllViews()

        // Calculate swatch size to fit all colors in one screen width
        val columns = 7
        val cardPaddingPx = (20 * resources.displayMetrics.density).toInt() * 2 // 20dp padding on each side of card
        val availableWidth = resources.displayMetrics.widthPixels - cardPaddingPx - (40 * resources.displayMetrics.density).toInt() // extra outer padding
        val marginPx = (4 * resources.displayMetrics.density).toInt()
        val sizePx = (availableWidth / columns) - (marginPx * 2)

        AppSettings.COLOR_OPTIONS.forEachIndexed { index, hex ->
            val swatch = ImageView(this)
            val lp = android.widget.GridLayout.LayoutParams().apply {
                width  = sizePx
                height = sizePx
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
            swatch.layoutParams = lp
            updateSwatch(swatch, hex, hex == selectedColor)
            swatch.setOnClickListener {
                selectedColor = hex
                AppSettings.setAccentColor(this, hex)
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i) as? ImageView ?: continue
                    updateSwatch(child, AppSettings.COLOR_OPTIONS[i], AppSettings.COLOR_OPTIONS[i] == selectedColor)
                }
            }
            container.addView(swatch)
        }
    }

    private fun updateSwatch(view: ImageView, hex: String, selected: Boolean) {
        val sizePx = resources.getDimensionPixelSize(R.dimen.color_swatch_size)
        val color  = Color.parseColor(hex)
        if (selected) {
            view.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setStroke(5, Color.WHITE)
            }
            view.setImageResource(R.drawable.ic_check_white)
            val pad = sizePx / 4
            view.setPadding(pad, pad, pad, pad)
            view.imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        } else {
            view.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            view.setImageDrawable(null)
            view.setPadding(0, 0, 0, 0)
        }
    }

    private fun setupListeners() {
        binding.rgTheme.setOnCheckedChangeListener { _, id ->
            val theme = when (id) {
                R.id.rbLight -> AppSettings.THEME_LIGHT
                R.id.rbDark  -> AppSettings.THEME_DARK
                else         -> AppSettings.THEME_SYSTEM
            }
            AppSettings.setTheme(this, theme)
            AppCompatDelegate.setDefaultNightMode(when (theme) {
                AppSettings.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                AppSettings.THEME_DARK  -> AppCompatDelegate.MODE_NIGHT_YES
                else                    -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            })
        }
        binding.switchBeep.setOnCheckedChangeListener          { _, c -> AppSettings.setBeep(this, c) }
        binding.switchVibrate.setOnCheckedChangeListener       { _, c -> AppSettings.setVibrate(this, c) }
        binding.switchCopyClipboard.setOnCheckedChangeListener { _, c -> AppSettings.setCopyClipboard(this, c) }
        binding.switchUrlInfo.setOnCheckedChangeListener       { _, c -> AppSettings.setUrlInfo(this, c) }

        binding.rowBeep.setOnClickListener          { binding.switchBeep.toggle() }
        binding.rowVibrate.setOnClickListener       { binding.switchVibrate.toggle() }
        binding.rowCopyClipboard.setOnClickListener { binding.switchCopyClipboard.toggle() }
        binding.rowUrlInfo.setOnClickListener       { binding.switchUrlInfo.toggle() }
    }

    private fun applyAccentColor() {
        // Section labels
        AccentApplier.tintText(binding.tvLabelColorScheme, this)
        AccentApplier.tintText(binding.tvLabelTheme, this)
        AccentApplier.tintText(binding.tvLabelFeedback, this)
        AccentApplier.tintText(binding.tvLabelResult, this)
        // Radio buttons
        AccentApplier.tintRadio(binding.rbLight, this)
        AccentApplier.tintRadio(binding.rbDark, this)
        AccentApplier.tintRadio(binding.rbSystem, this)
        // Switches
        AccentApplier.tintSwitch(binding.switchBeep, this)
        AccentApplier.tintSwitch(binding.switchVibrate, this)
        AccentApplier.tintSwitch(binding.switchCopyClipboard, this)
        AccentApplier.tintSwitch(binding.switchUrlInfo, this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
