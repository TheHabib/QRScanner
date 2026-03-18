package com.example.qrscanner.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import android.view.View
import com.google.android.material.switchmaterial.SwitchMaterial

object AccentApplier {

    fun color(context: Context): Int =
        Color.parseColor(AppSettings.getAccentColor(context))

    fun csl(context: Context): ColorStateList =
        ColorStateList.valueOf(color(context))

    fun tintSeekBar(seekBar: SeekBar, context: Context) {
        val c = csl(context)
        seekBar.progressTintList = c
        seekBar.thumbTintList = c
    }

    fun tintText(tv: TextView, context: Context) {
        tv.setTextColor(color(context))
    }

    fun tintRadio(rb: RadioButton, context: Context) {
        rb.buttonTintList = csl(context)
    }

    fun tintImage(iv: ImageView, context: Context) {
        iv.imageTintList = csl(context)
    }

    fun tintBackground(view: View, context: Context) {
        view.backgroundTintList = csl(context)
    }

    fun tintSwitch(sw: SwitchMaterial, context: Context) {
        val accent = color(context)
        val accentAlpha = Color.argb(128,
            Color.red(accent), Color.green(accent), Color.blue(accent))
        sw.thumbTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(accent, 0xFFAAAAAA.toInt())
        )
        sw.trackTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(accentAlpha, 0x33FFFFFF)
        )
    }
}
