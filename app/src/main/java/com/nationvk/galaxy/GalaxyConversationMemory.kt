package com.nationvk.galaxy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Private, bounded on-device voice conversation history.
 * Stores only recent Brain exchanges; it is not uploaded or synced.
 */
object GalaxyConversationMemory {
    private const val PREFS = "galaxy_conversation_memory"
    private const val KEY_USER_TEXT = "last_user_text"
    private const val KEY_ASSISTANT_TEXT = "last_assistant_text"
    private const val KEY_SAVED_AT = "saved_at"
    private const val KEY_HISTORY = "conversation_history"
    private const val MAX_TEXT_LENGTH = 500
    private const val MAX_TURNS = 12
    private const val MAX_AGE_MS = 24L * 60L * 60L * 1000L

    data class Turn(
        val userText: String,
        val assistantText: String,
        val savedAt: Long = System.currentTimeMillis()
    )

    fun saveTurn(context: Context, userText: String, assistantText: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val history = readHistory(prefs.getString(KEY_HISTORY, null))
            .filter { now - it.savedAt in 0..MAX_AGE_MS }
            .toMutableList()
        history.add(
            Turn(
                userText = userText.take(MAX_TEXT_LENGTH),
                assistantText = assistantText.take(MAX_TEXT_LENGTH),
                savedAt = now
            )
        )
        val json = JSONArray()
        history.takeLast(MAX_TURNS).forEach { turn ->
            json.put(JSONObject().apply {
                put("user", turn.userText)
                put("assistant", turn.assistantText)
                put("at", turn.savedAt)
            })
        }
        prefs.edit()
            .putString(KEY_HISTORY, json.toString())
            // Keep legacy keys readable for older app code and upgrades.
            .putString(KEY_USER_TEXT, userText.take(MAX_TEXT_LENGTH))
            .putString(KEY_ASSISTANT_TEXT, assistantText.take(MAX_TEXT_LENGTH))
            .putLong(KEY_SAVED_AT, now)
            .apply()
    }

    fun getRecentTurns(context: Context, limit: Int = MAX_TURNS): List<Turn> {
        if (limit <= 0) return emptyList()
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val history = readHistory(prefs.getString(KEY_HISTORY, null))
            .filter { now - it.savedAt in 0..MAX_AGE_MS }
        if (history.isNotEmpty()) return history.takeLast(limit)

        // One-time compatibility fallback for history saved by earlier Galaxy versions.
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        if (savedAt <= 0L || now - savedAt !in 0..MAX_AGE_MS) return emptyList()
        val user = prefs.getString(KEY_USER_TEXT, null)?.takeIf { it.isNotBlank() } ?: return emptyList()
        return listOf(Turn(user, prefs.getString(KEY_ASSISTANT_TEXT, null).orEmpty(), savedAt))
    }

    fun getLastTurn(context: Context): Turn? = getRecentTurns(context, 1).lastOrNull()

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun readHistory(raw: String?): List<Turn> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.optJSONObject(index) ?: continue
                    val user = item.optString("user").takeIf { it.isNotBlank() } ?: continue
                    add(
                        Turn(
                            userText = user.take(MAX_TEXT_LENGTH),
                            assistantText = item.optString("assistant").take(MAX_TEXT_LENGTH),
                            savedAt = item.optLong("at", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
