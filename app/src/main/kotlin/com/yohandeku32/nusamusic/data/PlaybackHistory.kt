package com.yohandeku32.nusamusic.data

import android.content.Context
import android.content.SharedPreferences

object PlaybackHistory {
    fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences("listening_history", Context.MODE_PRIVATE)

    fun counts(preferences: SharedPreferences): Map<String, Long> = preferences.all
        .mapNotNull { (key, value) -> (value as? Long)?.let { key to it } }.toMap()

    fun record(context: Context, uri: String) {
        val prefs = preferences(context)
        prefs.edit().putLong(uri, prefs.getLong(uri, 0L) + 1L).apply()
    }
}
