package com.example.qrscanner.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.qrscanner.utils.AccentApplier
import com.example.qrscanner.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupContent()
        setupLinks()
        applyAccentColor()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About"
    }

    private fun setupContent() {
        binding.tvAppVersion.text = "Version 1.0"
    }

    private fun setupLinks() {
        binding.btnGithub.setOnClickListener {
            openUrl("https://github.com/TheHabib")
        }
        binding.btnLinkedin.setOnClickListener {
            openUrl("https://bd.linkedin.com/in/mdhabibulislam")
        }
    }

    private fun applyAccentColor() {
        AccentApplier.tintImage(binding.ivAppIcon, this)
        AccentApplier.tintText(binding.tvAppVersion, this)
        AccentApplier.tintText(binding.tvDeveloperLabel, this)
        AccentApplier.tintText(binding.tvLibrariesLabel, this)
        // Tint library bullet dots
        AccentApplier.tintBackground(binding.dotMlKit, this)
        AccentApplier.tintBackground(binding.dotCameraX, this)
        AccentApplier.tintBackground(binding.dotMaterial, this)
        AccentApplier.tintBackground(binding.dotKotlin, this)
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            // No browser available
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
