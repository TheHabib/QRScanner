package com.example.qrscanner.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qrscanner.R
import com.example.qrscanner.databinding.ActivityHistoryBinding
import com.example.qrscanner.model.HistoryItem
import com.example.qrscanner.model.ScanResultType
import com.example.qrscanner.ui.result.ResultActivity
import com.example.qrscanner.utils.AccentApplier
import com.example.qrscanner.utils.HistoryManager
import com.example.qrscanner.utils.ScanResultConverter

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter

    private var allItems    = listOf<HistoryItem>()
    private var activeFilter: ScanResultType? = null
    private var showFavoritesOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        setupTopMenu()
        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "History"
        binding.toolbar.navigationIcon?.setTint(Color.WHITE)
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            context         = this,
            onItemClick     = { item -> openResult(item) },
            onFavoriteToggle = { item -> toggleFavorite(item) },
            onDelete        = { item -> deleteItem(item) },
            onShare         = { item -> shareItem(item) },
            onCopy          = { item -> copyItem(item) }
        )
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter

        val swipe = SwipeToDeleteCallback(adapter) { item, _ ->
            HistoryManager.deleteItem(this, item.id)
            allItems = HistoryManager.getAll(this)
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
        }
        ItemTouchHelper(swipe).attachToRecyclerView(binding.recyclerHistory)
    }

    private fun setupFilterChips() {
        val filters = listOf(
            null to "All",
            ScanResultType.URL      to "URL",
            ScanResultType.TEXT     to "Text",
            ScanResultType.WIFI     to "Wi-Fi",
            ScanResultType.PHONE    to "Phone",
            ScanResultType.EMAIL    to "Email",
            ScanResultType.SMS      to "SMS",
            ScanResultType.GEO      to "Geo",
            ScanResultType.CONTACT  to "Contact",
            ScanResultType.CALENDAR to "Calendar",
            ScanResultType.APP      to "App"
        )

        filters.forEach { (type, label) ->
            val chip = layoutInflater.inflate(
                R.layout.item_filter_chip, binding.chipGroupFilter, false
            ) as TextView
            chip.text = label
            chip.tag  = type
            chip.setOnClickListener {
                activeFilter = type
                updateChipStyles()
                applyFilter()
            }
            binding.chipGroupFilter.addView(chip)
        }

        updateChipStyles()
    }

    private fun updateChipStyles() {
        val accent = Color.parseColor(
            com.example.qrscanner.utils.AppSettings.getAccentColor(this))
        val cornerRadius = resources.displayMetrics.density * 16
        for (i in 0 until binding.chipGroupFilter.childCount) {
            val chip = binding.chipGroupFilter.getChildAt(i) as? TextView ?: continue
            val isSelected = chip.tag == activeFilter
            if (isSelected) {
                val bg = android.graphics.drawable.GradientDrawable()
                bg.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                bg.cornerRadius = cornerRadius
                bg.setColor(android.graphics.Color.argb(40,
                    android.graphics.Color.red(accent),
                    android.graphics.Color.green(accent),
                    android.graphics.Color.blue(accent)))
                bg.setStroke((resources.displayMetrics.density * 1.5f).toInt(), accent)
                chip.background = bg
                chip.setTextColor(accent)
            } else {
                chip.setBackgroundResource(R.drawable.chip_unselected_bg)
                chip.setTextColor(Color.parseColor("#99FFFFFF"))
            }
        }
    }

    private fun setupTopMenu() {
        // 3-dot toolbar overflow menu
        binding.btnHistoryMenu.setOnClickListener { v ->
            val popup = android.widget.PopupMenu(this, v)
            popup.menuInflater.inflate(R.menu.menu_history_toolbar, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_show_favorites -> {
                        showFavoritesOnly = !showFavoritesOnly
                        item.title = if (showFavoritesOnly) "Show All" else "Show Favorites"
                        applyFilter()
                    }
                    R.id.action_clear_all -> confirmClearAll()
                }
                true
            }
            popup.show()
        }
    }

    private fun loadHistory() {
        allItems = HistoryManager.getAll(this)
        applyFilter()
        updateEmptyState()
    }

    private fun applyFilter() {
        var filtered = if (showFavoritesOnly) allItems.filter { it.isFavorite } else allItems
        activeFilter?.let { type -> filtered = filtered.filter { it.type == type } }
        adapter.submitList(filtered)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val empty = allItems.isEmpty()
        binding.recyclerHistory.visibility = if (empty) View.GONE else View.VISIBLE
        binding.tvEmptyHistory.visibility  = if (empty) View.VISIBLE else View.GONE
    }

    private fun openResult(item: HistoryItem) {
        val result = ScanResultConverter.toScanResult(item)
        startActivity(Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SCAN_RESULT, result)
        })
    }

    private fun toggleFavorite(item: HistoryItem) {
        val nowFav = HistoryManager.toggleFavorite(this, item.id)
        allItems = HistoryManager.getAll(this)
        applyFilter()
        Toast.makeText(
            this,
            if (nowFav) "Added to Favorites" else "Removed from Favorites",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun deleteItem(item: HistoryItem) {
        HistoryManager.deleteItem(this, item.id)
        allItems = HistoryManager.getAll(this)
        applyFilter()
        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
    }

    private fun shareItem(item: HistoryItem) {
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, item.rawValue)
            }, "Share via"
        ))
    }

    private fun copyItem(item: HistoryItem) {
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("Scan Content", item.rawValue))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Delete all scan history? Favorites will also be removed.")
            .setPositiveButton("Clear All") { _, _ ->
                HistoryManager.clearAll(this)
                loadHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
