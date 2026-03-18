package com.example.qrscanner.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.qrscanner.model.HistoryItem
import com.example.qrscanner.model.ScanResultType
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object HistoryManager {

    private const val PREFS_NAME = "scan_history"
    private const val KEY_HISTORY = "history"
    private const val MAX_ITEMS = 500

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Read ────────────────────────────────────────────────────────────────

    fun getAll(context: Context): List<HistoryItem> {
        val json = prefs(context).getString(KEY_HISTORY, "[]") ?: "[]"
        return parseList(json)
    }

    fun getFavorites(context: Context): List<HistoryItem> =
        getAll(context).filter { it.isFavorite }

    // ── Write ───────────────────────────────────────────────────────────────

    fun addItem(context: Context, item: HistoryItem) {
        val list = getAll(context).toMutableList()

        // Duplicate check
        if (!AppSettings.isKeepDuplicatesEnabled(context)) {
            val existing = list.indexOfFirst { it.rawValue == item.rawValue }
            if (existing != -1) {
                // Move to top with updated timestamp, keep favorite state
                val old = list.removeAt(existing)
                list.add(0, item.copy(
                    id = old.id,
                    isFavorite = old.isFavorite
                ))
                saveList(context, list)
                return
            }
        }

        list.add(0, item)

        // Cap at MAX_ITEMS (protect non-favorite items from being trimmed first)
        if (list.size > MAX_ITEMS) {
            val trimIndex = list.indexOfLast { !it.isFavorite }
            if (trimIndex != -1) list.removeAt(trimIndex)
            else list.removeAt(list.size - 1)
        }

        saveList(context, list)
    }

    fun deleteItem(context: Context, id: String) {
        val list = getAll(context).toMutableList()
        list.removeAll { it.id == id }
        saveList(context, list)
    }

    fun toggleFavorite(context: Context, id: String): Boolean {
        val list = getAll(context).toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index == -1) return false
        val updated = list[index].copy(isFavorite = !list[index].isFavorite)
        list[index] = updated
        saveList(context, list)
        return updated.isFavorite
    }

    fun clearAll(context: Context) {
        prefs(context).edit().putString(KEY_HISTORY, "[]").apply()
    }

    fun clearNonFavorites(context: Context) {
        val list = getAll(context).filter { it.isFavorite }.toMutableList()
        saveList(context, list)
    }

    // ── Serialization ───────────────────────────────────────────────────────

    private fun saveList(context: Context, list: List<HistoryItem>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs(context).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private fun parseList(json: String): List<HistoryItem> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull {
                runCatching { fromJson(arr.getJSONObject(it)) }.getOrNull()
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun toJson(item: HistoryItem): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("rawValue", item.rawValue)
        put("type", item.type.name)
        put("displayTitle", item.displayTitle)
        put("displaySubtitle", item.displaySubtitle)
        put("timestamp", item.timestamp)
        put("isFavorite", item.isFavorite)
        item.wifiSsid?.let { put("wifiSsid", it) }
        item.wifiPassword?.let { put("wifiPassword", it) }
        item.wifiEncryption?.let { put("wifiEncryption", it) }
    }

    private fun fromJson(obj: JSONObject): HistoryItem = HistoryItem(
        id            = obj.getString("id"),
        rawValue      = obj.getString("rawValue"),
        type          = runCatching { ScanResultType.valueOf(obj.getString("type")) }
                            .getOrDefault(ScanResultType.TEXT),
        displayTitle  = obj.getString("displayTitle"),
        displaySubtitle = obj.getString("displaySubtitle"),
        timestamp     = obj.getLong("timestamp"),
        isFavorite    = obj.optBoolean("isFavorite", false),
        wifiSsid      = obj.optString("wifiSsid").takeIf { it.isNotEmpty() },
        wifiPassword  = obj.optString("wifiPassword").takeIf { it.isNotEmpty() },
        wifiEncryption = obj.optString("wifiEncryption").takeIf { it.isNotEmpty() }
    )

    // ── Helper ──────────────────────────────────────────────────────────────

    fun newId(): String = UUID.randomUUID().toString()
}
