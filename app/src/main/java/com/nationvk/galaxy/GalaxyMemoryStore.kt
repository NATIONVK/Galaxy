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
    private const val KEY_CORRUPT_BACKUP = "corrupt_entries_backup"
    private const val MAX_ENTRIES = 1000
    private val ALLOWED_CATEGORIES = setOf("PERSONAL", "PROJECT", "TASK", "CONVERSATION", "IDEAS", "RESEARCH", "LEARNING", "NOTES")

    fun getAll(context: Context): List<GalaxyMemoryEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ENTRIES, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val content = item.optString("content", "").trim()
                val category = item.optString("category", "NOTES").uppercase().take(24)
                if (content.isEmpty() || content.length > 10000 || category !in ALLOWED_CATEGORIES) return@mapNotNull null
                GalaxyMemoryEntry(
                    id = item.optLong("id", 0L).coerceAtLeast(0L),
                    category = category,
                    content = content,
                    createdAt = item.optLong("createdAt", 0L).coerceIn(0L, System.currentTimeMillis() + 86_400_000L)
                )
            }.distinctBy { it.id }.sortedByDescending { it.createdAt }.take(MAX_ENTRIES)
        } catch (_: Exception) {
            // Preserve the original bytes before returning an empty view; never overwrite
            // a damaged vault silently when the user next saves a memory.
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_CORRUPT_BACKUP, raw).commit()
            emptyList()
        }
    }

    fun add(context: Context, category: String, content: String): Boolean {
        val clean = content.trim().take(10000)
        val cleanCategory = category.trim().uppercase().take(24)
        if (clean.isEmpty() || cleanCategory !in ALLOWED_CATEGORIES) return false
        val entries = getAll(context).toMutableList()
        val nextId = (entries.maxOfOrNull { it.id } ?: 0L) + 1L
        entries.add(0, GalaxyMemoryEntry(nextId, cleanCategory, clean, System.currentTimeMillis()))
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
        // A deliberate user clear removes both the active vault and any recovery copy.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_ENTRIES).remove(KEY_CORRUPT_BACKUP).apply()
    }

    fun hasRecoveryBackup(context: Context): Boolean =
        !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CORRUPT_BACKUP, null).isNullOrBlank()

    fun exportRecoveryBackup(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CORRUPT_BACKUP, null)

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
        val allowed = ALLOWED_CATEGORIES
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
