package com.nationvk.galaxy

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GalaxyVoiceHistory {
    private const val PREFS = "galaxy_voice_history"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 30

    fun add(context: Context, command: String, result: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = prefs.getString(KEY_ENTRIES, "").orEmpty()
        val stamp = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
        val safeCommand = command.replace("\\n", " ").replace("|", " ").take(100)
        val safeResult = result.replace("\\n", " ").replace("|", " ").take(60)
        val entry = "$stamp  |  $safeCommand  |  $safeResult"
        val updated = (listOf(entry) + old.lines().filter { it.isNotBlank() }).take(MAX_ENTRIES)
        prefs.edit().putString(KEY_ENTRIES, updated.joinToString("\\n")).apply()
    }

    fun getEntries(context: Context): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ENTRIES, "").orEmpty()
            .lines().filter { it.isNotBlank() }.take(MAX_ENTRIES)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ENTRIES).apply()
    }
}
