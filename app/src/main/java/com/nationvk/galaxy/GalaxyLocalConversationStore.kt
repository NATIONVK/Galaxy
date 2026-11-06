package com.nationvk.galaxy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Small, device-local conversation cache for the offline chat workspace. */
object GalaxyLocalConversationStore {
    private const val PREFS = "galaxy_local_conversations"
    private const val KEY_MESSAGES = "messages"
    private const val MAX_MESSAGES = 500
    private const val MAX_TEXT_LENGTH = 10000

    fun load(context: Context): List<Pair<Boolean, String>> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MESSAGES, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val text = item.optString("text", "").trim()
                if (text.isEmpty() || text.length > MAX_TEXT_LENGTH) return@mapNotNull null
                item.optBoolean("user", true) to text
            }.takeLast(MAX_MESSAGES)
        } catch (_: Exception) { emptyList() }
    }

    fun save(context: Context, messages: List<Pair<Boolean, String>>): Boolean {
        val array = JSONArray()
        messages.takeLast(MAX_MESSAGES).forEach { (isUser, text) ->
            val clean = text.trim().take(MAX_TEXT_LENGTH)
            if (clean.isNotEmpty()) array.put(JSONObject().put("user", isUser).put("text", clean))
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_MESSAGES, array.toString()).commit()
    }

    fun clear(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .remove(KEY_MESSAGES).commit()
}
