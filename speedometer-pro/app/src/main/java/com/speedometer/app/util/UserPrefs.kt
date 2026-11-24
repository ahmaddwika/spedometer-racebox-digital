package com.speedometer.app.util

import android.content.Context
import com.speedometer.app.ui.settings.SettingsFragment

object UserPrefs {

    /** Kalibrasi kecepatan: 1.00 = GPS asli, 1.05 = +5% (samakan dengan speedometer motor). */
    fun speedCalibration(context: Context): Float {
        val prefs = context.getSharedPreferences(SettingsFragment.PREFS, Context.MODE_PRIVATE)
        return prefs.getFloat(SettingsFragment.KEY_CALIBRATION, 1.0f)
    }

    fun setCalibration(context: Context, value: Float) {
        val prefs = context.getSharedPreferences(SettingsFragment.PREFS, Context.MODE_PRIVATE)
        prefs.edit().putFloat(SettingsFragment.KEY_CALIBRATION, value).apply()
    }

    fun useMph(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SettingsFragment.PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(SettingsFragment.KEY_MPH, false)
    }
}
