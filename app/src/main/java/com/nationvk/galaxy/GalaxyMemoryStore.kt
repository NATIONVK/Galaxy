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


    data class ImportSummary(val imported: Int, val skippedDuplicates: Int, val rejected: Int)

    fun importJson(context: Context, rawJson: String): ImportSummary {
        if (rawJson.length > 5_000_000) throw IllegalArgumentException("Backup file is too large (5 MB limit).")
        val root = try { JSONObject(rawJson) } catch (_: Exception) {
            throw IllegalArgumentException("This file is not valid Galaxy JSON.")
        }
        if (root.optString("app") != "Galaxy") throw IllegalArgumentException("This backup was not created by Galaxy.")
        val version = root.optInt("formatVersion", -1)
        if (version != 1) throw IllegalArgumentException("Unsupported backup format version: $version")
        val array = root.optJSONArray("memories") ?: throw IllegalArgumentException("Backup has no memories list.")
        if (array.length() > 5000) throw IllegalArgumentException("Backup contains too many records.")
        val existing = getAll(context).toMutableList()
        val known = existing.map { dedupeKey(it.category, it.content) }.toMutableSet()
        var imported = 0
        var duplicates = 0
        var rejected = 0
        var nextId = (existing.maxOfOrNull { it.id } ?: 0L) + 1L
        val allowed = setOf("PERSONAL", "PROJECT", "TASK", "CONVERSATION", "IDEAS", "RESEARCH", "LEARNING", "NOTES")
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index)
            if (item == null) { rejected++; continue }
            val content = item.optString("content", "").trim()
            val category = item.optString("category", "NOTES").uppercase().take(24)
            if (content.isEmpty() || content.length > 10000 || category !in allowed) { rejected++; continue }
            val key = dedupeKey(category, content)
            if (!known.add(key)) { duplicates++; continue }
            val timestamp = item.optLong("createdAt", System.currentTimeMillis()).coerceIn(0L, System.currentTimeMillis() + 86_400_000L)
            existing.add(GalaxyMemoryEntry(nextId++, category, content, timestamp))
            imported++
        }
        if (imported > 0 && !save(context, existing.sortedByDescending { it.createdAt }.take(MAX_ENTRIES))) {
            throw IllegalStateException("Could not save imported memories.")
        }
        return ImportSummary(imported, duplicates, rejected)
    }

    private fun dedupeKey(category: String, content: String): String =
        category.trim().uppercase() + "|" + content.trim().replace(Regex("\\s+"), " ").lowercase()

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
