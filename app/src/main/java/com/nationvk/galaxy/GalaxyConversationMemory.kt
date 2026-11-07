package com.nationvk.galaxy

import android.content.Context

/**
 * Small, private on-device conversation context.
 * Keeps only the most recent Brain exchange to support short follow-up questions.
 */
object GalaxyConversationMemory {
    private const val PREFS = "galaxy_conversation_memory"
    private const val KEY_USER_TEXT = "last_user_text"
    private const val KEY_ASSISTANT_TEXT = "last_assistant_text"
    private const val KEY_SAVED_AT = "saved_at"
    private const val MAX_TEXT_LENGTH = 500
    private const val MAX_AGE_MS = 24L * 60L * 60L * 1000L

    data class Turn(val userText: String, val assistantText: String)

    fun saveTurn(context: Context, userText: String, assistantText: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_USER_TEXT, userText.take(MAX_TEXT_LENGTH))
            .putString(KEY_ASSISTANT_TEXT, assistantText.take(MAX_TEXT_LENGTH))
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    fun getLastTurn(context: Context): Turn? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        if (savedAt <= 0L || System.currentTimeMillis() - savedAt > MAX_AGE_MS) return null
        val userText = prefs.getString(KEY_USER_TEXT, null)?.takeIf { it.isNotBlank() } ?: return null
        val assistantText = prefs.getString(KEY_ASSISTANT_TEXT, null).orEmpty()
        return Turn(userText, assistantText)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
