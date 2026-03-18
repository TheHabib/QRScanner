package com.example.qrscanner.ui.favorites

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qrscanner.databinding.ActivityFavoritesBinding
import com.example.qrscanner.ui.history.HistoryAdapter
import com.example.qrscanner.ui.history.SwipeToDeleteCallback
import com.example.qrscanner.ui.result.ResultActivity
import com.example.qrscanner.utils.HistoryManager
import com.example.qrscanner.utils.ScanResultConverter

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupRecyclerView()
        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Favorites"
        binding.toolbar.navigationIcon?.setTint(Color.WHITE)
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            context          = this,
            onItemClick      = { item ->
                startActivity(Intent(this, ResultActivity::class.java).apply {
                    putExtra(ResultActivity.EXTRA_SCAN_RESULT, ScanResultConverter.toScanResult(item))
                })
            },
            onFavoriteToggle = { item ->
                HistoryManager.toggleFavorite(this, item.id)
                loadFavorites()
                Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show()
            },
            onDelete         = { item ->
                HistoryManager.deleteItem(this, item.id)
                loadFavorites()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            },
            onShare          = { item ->
                startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, item.rawValue)
                    }, "Share via"
                ))
            },
            onCopy           = { item ->
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("Scan Content", item.rawValue))
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerFavorites.layoutManager = LinearLayoutManager(this)
        binding.recyclerFavorites.adapter = adapter

        val swipe = SwipeToDeleteCallback(adapter) { item, _ ->
            HistoryManager.deleteItem(this, item.id)
            loadFavorites()
        }
        ItemTouchHelper(swipe).attachToRecyclerView(binding.recyclerFavorites)
    }

    private fun loadFavorites() {
        val favs = HistoryManager.getFavorites(this)
        adapter.submitList(favs)
        binding.recyclerFavorites.visibility = if (favs.isEmpty()) View.GONE else View.VISIBLE
        binding.tvEmptyFavorites.visibility  = if (favs.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
