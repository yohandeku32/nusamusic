package com.yohandeku32.nusamusic.data

import android.content.Context
import android.content.SharedPreferences

object PlaybackHistory {
    fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences("listening_history", Context.MODE_PRIVATE)

    fun counts(preferences: SharedPreferences): Map<String, Long> = preferences.all
        .mapNotNull { (key, value) -> (value as? Long)?.let { key to it } }.toMap()

    fun recentPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences("listening_history_recent", Context.MODE_PRIVATE)

    fun record(context: Context, uri: String) {
        val prefs = preferences(context)
        prefs.edit().putLong(uri, prefs.getLong(uri, 0L) + 1L).apply()
        recordRecent(context, uri)
    }

    fun recordRecent(context: Context, uri: String) {
        val recent = recentPreferences(context)
        val entries = counts(recent)
        val edit = recent.edit().putLong(uri, System.currentTimeMillis())
        if (entries.size >= 200 && uri !in entries) {
            entries.minByOrNull { it.value }?.key?.let { edit.remove(it) }
        }
        edit.apply()
    }
}
