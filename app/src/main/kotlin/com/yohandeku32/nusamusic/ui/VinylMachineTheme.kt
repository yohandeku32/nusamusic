package com.yohandeku32.nusamusic.ui

import android.content.SharedPreferences

enum class VinylMachineTheme(val preferenceValue: String) {
    CLASSIC("classic"),
    STUDIO_BLACK("studio_black");

    companion object {
        fun fromPreference(value: String?): VinylMachineTheme =
            entries.firstOrNull { it.preferenceValue == value } ?: CLASSIC
    }
}

internal object VinylThemePreference {
    private const val KEY = "vinyl_machine_theme"

    fun read(preferences: SharedPreferences): VinylMachineTheme = try {
        VinylMachineTheme.fromPreference(preferences.getString(KEY, null))
    } catch (_: ClassCastException) {
        VinylMachineTheme.CLASSIC
    }

    fun write(preferences: SharedPreferences, theme: VinylMachineTheme) {
        preferences.edit().putString(KEY, theme.preferenceValue).apply()
    }
}
