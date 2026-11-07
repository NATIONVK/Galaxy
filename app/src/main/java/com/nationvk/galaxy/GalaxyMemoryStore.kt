package com.nationvk.galaxy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class GalaxyMemoryEntry(
    val id: Long,
    val category: String,
    val content: String,
    val createdAt: Long,
    val priority: Int = 2,
    val confidence: Float = 1f
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
                    createdAt = item.optLong("createdAt", 0L).coerceIn(0L, System.currentTimeMillis() + 86_400_000L),
                    priority = item.optInt("priority", 2).coerceIn(1, 3),
                    confidence = item.optDouble("confidence", 1.0).toFloat().coerceIn(0f, 1f)
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

    fun add(context: Context, category: String, content: String, priority: Int = 2, confidence: Float = 1f): Boolean {
        val clean = content.trim().take(10000)
        val cleanCategory = category.trim().uppercase().take(24)
        if (clean.isEmpty() || cleanCategory !in ALLOWED_CATEGORIES || isDuplicate(context, cleanCategory, clean)) return false
        val entries = getAll(context).toMutableList()
        val nextId = (entries.maxOfOrNull { it.id } ?: 0L) + 1L
        entries.add(0, GalaxyMemoryEntry(nextId, cleanCategory, clean, System.currentTimeMillis(), priority.coerceIn(1, 3), confidence.coerceIn(0f, 1f)))
        return save(context, entries.take(MAX_ENTRIES))
    }

    fun isDuplicate(context: Context, category: String, content: String, excludingId: Long? = null): Boolean {
        val key = dedupeKey(category, content)
        return getAll(context).any { it.id != excludingId && dedupeKey(it.category, it.content) == key }
    }

    fun update(context: Context, id: Long, category: String, content: String, priority: Int? = null): Boolean {
        val clean = content.trim().take(10000)
        val cleanCategory = category.trim().uppercase().take(24)
        if (clean.isEmpty() || cleanCategory !in ALLOWED_CATEGORIES || isDuplicate(context, cleanCategory, clean, id)) return false
        val entries = getAll(context).toMutableList()
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return false
        entries[index] = entries[index].copy(category = cleanCategory, content = clean, priority = priority?.coerceIn(1, 3) ?: entries[index].priority)
        return save(context, entries)
    }

    /** Relevance-ranked local retrieval. No memory content leaves the device. */
    fun search(context: Context, query: String, category: String? = null, limit: Int = 8): List<GalaxyMemoryEntry> {
        val cleanQuery = normalizeText(query)
        val terms = tokenize(query)
        if (terms.isEmpty()) return recent(context, limit).filter {
            category == null || it.category.equals(category, ignoreCase = true)
        }
        return getAll(context).asSequence()
            .filter { category == null || it.category.equals(category, ignoreCase = true) }
            .mapNotNull { entry ->
                val cleanContent = normalizeText(entry.content)
                val contentTerms = tokenize(entry.content).toSet()
                val matched = terms.count { term ->
                    term in contentTerms || contentTerms.any { it.startsWith(term) }
                }
                if (matched == 0) return@mapNotNull null
                val coverage = matched.toDouble() / terms.size.coerceAtLeast(1)
                val phraseBonus = if (cleanContent.contains(cleanQuery)) 8.0 else 0.0
                val categoryBonus = if (terms.any { normalizeText(entry.category).contains(it) }) 2.0 else 0.0
                val prefixBonus = if (contentTerms.any { word -> terms.any { it.length >= 3 && word.startsWith(it) } }) 1.0 else 0.0
                val ageDays = ((System.currentTimeMillis() - entry.createdAt).coerceAtLeast(0L) / 86_400_000L).toDouble()
                val recencyBonus = (1.0 / (1.0 + ageDays / 30.0))
                val score = matched * 3.0 + coverage * 5.0 + phraseBonus + categoryBonus + prefixBonus + recencyBonus
                Triple(entry, score, matched)
            }
            .sortedWith(
                compareByDescending<Triple<GalaxyMemoryEntry, Double, Int>> { it.second }
                    .thenByDescending { it.third }
                    .thenByDescending { it.first.createdAt }
            )
            .take(limit.coerceAtLeast(0))
            .map { it.first }
            .toList()
    }

    /** Add a memory, or refresh an existing normalized exact match instead of duplicating it. */
    fun addOrRefresh(context: Context, category: String, content: String, priority: Int = 2, confidence: Float = 1f): Boolean {
        val clean = content.trim().take(10000)
        val cleanCategory = category.trim().uppercase().take(24)
        if (clean.isEmpty() || cleanCategory !in ALLOWED_CATEGORIES) return false
        val entries = getAll(context).toMutableList()
        val key = dedupeKey(cleanCategory, clean)
        val existingIndex = entries.indexOfFirst {
            it.category == cleanCategory && dedupeKey(it.category, it.content) == key
        }
        if (existingIndex >= 0) {
            entries[existingIndex] = entries[existingIndex].copy(createdAt = System.currentTimeMillis(), priority = priority.coerceIn(1, 3), confidence = confidence.coerceIn(0f, 1f))
            return save(context, entries.sortedByDescending { it.createdAt })
        }
        val nextId = (entries.maxOfOrNull { it.id } ?: 0L) + 1L
        entries.add(0, GalaxyMemoryEntry(nextId, cleanCategory, clean, System.currentTimeMillis(), priority.coerceIn(1, 3), confidence.coerceIn(0f, 1f)))
        return save(context, entries.take(MAX_ENTRIES))
    }

    data class MemoryInsights(
        val total: Int,
        val categoryCounts: Map<String, Int>,
        val newestAt: Long?,
        val recoveryAvailable: Boolean
    )

    fun insights(context: Context): MemoryInsights {
        val entries = getAll(context)
        return MemoryInsights(
            total = entries.size,
            categoryCounts = entries.groupingBy { it.category }.eachCount(),
            newestAt = entries.maxOfOrNull { it.createdAt },
            recoveryAvailable = hasRecoveryBackup(context)
        )
    }

    private val STOP_WORDS = setOf(
        "the", "and", "for", "with", "that", "this", "from", "your", "you", "are", "was",
        "what", "when", "where", "how", "why", "about", "into", "have", "has", "had",
        "একটা", "একটি", "এবং", "আর", "এই", "ওই", "সেই", "আমার", "তোমার", "তার", "হলো", "হবে", "করে", "করা", "কি", "কী"
    )

    private fun tokenize(value: String): List<String> =
        normalizeText(value).split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 1 && it !in STOP_WORDS }
            .distinct()

    private fun normalizeText(value: String): String =
        java.text.Normalizer.normalize(value.trim(), java.text.Normalizer.Form.NFC)
            .lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]+"), " ")
            .replace(Regex("\\s+"), " ").trim()

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
                .put("content", entry.content).put("createdAt", entry.createdAt)
                .put("priority", entry.priority).put("confidence", entry.confidence.toDouble()))
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
            existing.add(GalaxyMemoryEntry(nextId++, category, content, timestamp,
                item.optInt("priority", 2).coerceIn(1, 3), item.optDouble("confidence", 1.0).toFloat().coerceIn(0f, 1f)))
            imported++
        }
        if (imported > 0 && !save(context, existing.sortedByDescending { it.createdAt }.take(MAX_ENTRIES))) {
            throw IllegalStateException("Could not save imported memories.")
        }
        return ImportSummary(imported, duplicates, rejected)
    }

    private fun dedupeKey(category: String, content: String): String =
        category.trim().uppercase() + "|" + normalizeText(content)

    private fun save(context: Context, entries: List<GalaxyMemoryEntry>): Boolean {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().put("id", entry.id).put("category", entry.category)
                .put("content", entry.content).put("createdAt", entry.createdAt)
                .put("priority", entry.priority).put("confidence", entry.confidence.toDouble()))
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ENTRIES, array.toString()).commit()
    }
}
