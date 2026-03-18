package com.example.qrscanner.utils

import android.content.Context
import android.content.SharedPreferences

object AppSettings {

    private const val PREFS_NAME = "qrscanner_prefs"

    const val KEY_ACCENT_COLOR = "accent_color"
    const val KEY_THEME = "theme"
    const val KEY_BEEP = "beep"
    const val KEY_VIBRATE = "vibrate"
    const val KEY_COPY_CLIPBOARD = "copy_clipboard"
    const val KEY_URL_INFO = "url_info"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"

    const val DEFAULT_ACCENT = "#00E676"

    // 13 colors: existing green default + 12 from screenshot
    val COLOR_OPTIONS = listOf(
        "#00E676", // Default green (existing app color)
        "#2196F3", // Blue
        "#E53935", // Red
        "#F4511E", // Orange-red
        "#F6BF26", // Yellow
        "#0F9D58", // Dark green
        "#33B679", // Medium green
        "#039BE5", // Light blue
        "#3F51B5", // Indigo
        "#7986CB", // Periwinkle
        "#9E69AF", // Lavender
        "#8E24AA", // Deep purple
        "#E67C73", // Salmon
        "#4FC3F7"  // Pale blue
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAccentColor(context: Context): String =
        prefs(context).getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT) ?: DEFAULT_ACCENT

    fun setAccentColor(context: Context, color: String) =
        prefs(context).edit().putString(KEY_ACCENT_COLOR, color).apply()

    fun getTheme(context: Context): String =
        prefs(context).getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setTheme(context: Context, theme: String) =
        prefs(context).edit().putString(KEY_THEME, theme).apply()

    fun isBeepEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BEEP, true)

    fun setBeep(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_BEEP, enabled).apply()

    fun isVibrateEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VIBRATE, true)

    fun setVibrate(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_VIBRATE, enabled).apply()

    fun isCopyClipboardEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COPY_CLIPBOARD, false)

    fun setCopyClipboard(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_COPY_CLIPBOARD, enabled).apply()

    fun isUrlInfoEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_URL_INFO, false)

    fun setUrlInfo(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_URL_INFO, enabled).apply()
}
