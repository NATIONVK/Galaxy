package com.nationvk.galaxy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class GalaxyMemoryEntry(
    val id: Long,
    val category: String,
    val content: String,
    val createdAt: Long
)

object GalaxyMemoryStore {
    private const val PREFS = "galaxy_long_term_memory"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 1000

    fun getAll(context: Context): List<GalaxyMemoryEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ENTRIES, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                GalaxyMemoryEntry(
                    id = item.optLong("id"),
                    category = item.optString("category", "NOTES"),
                    content = item.optString("content"),
                    createdAt = item.optLong("createdAt")
                )
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, category: String, content: String): Boolean {
        val clean = content.trim().take(10000)
        if (clean.isEmpty()) return false
        val entries = getAll(context).toMutableList()
        val nextId = (entries.maxOfOrNull { it.id } ?: 0L) + 1L
        entries.add(0, GalaxyMemoryEntry(nextId, category.take(24), clean, System.currentTimeMillis()))
        return save(context, entries.take(MAX_ENTRIES))
    }

    fun search(context: Context, query: String, category: String? = null, limit: Int = 8): List<GalaxyMemoryEntry> {
        val terms = query.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length > 1 }
        return getAll(context).asSequence()
            .filter { category == null || it.category.equals(category, ignoreCase = true) }
            .map { entry ->
                val haystack = (entry.content + " " + entry.category).lowercase()
                val score = if (terms.isEmpty()) 1 else terms.count { haystack.contains(it) }
                entry to score
            }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<GalaxyMemoryEntry, Int>> { it.second }.thenByDescending { it.first.createdAt })
            .take(limit).map { it.first }.toList()
    }

    fun recent(context: Context, limit: Int = 5): List<GalaxyMemoryEntry> = getAll(context).take(limit)

    fun delete(context: Context, id: Long): Boolean =
        save(context, getAll(context).filterNot { it.id == id })

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ENTRIES).apply()
    }

    fun exportJson(context: Context): String {
        val root = JSONObject()
        root.put("app", "Galaxy")
        root.put("formatVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val array = JSONArray()
        getAll(context).forEach { entry ->
            array.put(JSONObject().put("id", entry.id).put("category", entry.category)
                .put("content", entry.content).put("createdAt", entry.createdAt))
        }
        root.put("memories", array)
        return root.toString(2)
    }

    private fun save(context: Context, entries: List<GalaxyMemoryEntry>): Boolean {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().put("id", entry.id).put("category", entry.category)
                .put("content", entry.content).put("createdAt", entry.createdAt))
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ENTRIES, array.toString()).commit()
    }
}
