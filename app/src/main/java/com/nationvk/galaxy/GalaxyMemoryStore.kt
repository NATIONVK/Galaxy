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
        val previous = entries[index]
        if (previous.category != cleanCategory || previous.content != clean || (priority != null && previous.priority != priority.coerceIn(1, 3))) recordVersion(context, previous)
        entries[index] = previous.copy(category = cleanCategory, content = clean, priority = priority?.coerceIn(1, 3) ?: previous.priority)
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
                val priorityBonus = entry.priority * 1.5
                val confidenceBonus = entry.confidence * 2.0
                val score = matched * 3.0 + coverage * 5.0 + phraseBonus + categoryBonus + prefixBonus + recencyBonus + priorityBonus + confidenceBonus
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

    data class MemoryVersion(val entry: GalaxyMemoryEntry, val savedAt: Long)
    data class TrashedMemory(val entry: GalaxyMemoryEntry, val deletedAt: Long)

    private const val ARCHIVE_PREFS = "galaxy_memory_timeline"
    private const val KEY_VERSIONS = "versions"
    private const val KEY_TRASH = "trash"
    private const val MAX_VERSIONS_PER_MEMORY = 10
    private const val MAX_TRASH_ITEMS = 100

    private fun entryJson(entry: GalaxyMemoryEntry): JSONObject =
        JSONObject().put("id", entry.id).put("category", entry.category).put("content", entry.content)
            .put("createdAt", entry.createdAt).put("priority", entry.priority).put("confidence", entry.confidence.toDouble())

    private fun parseEntry(item: JSONObject): GalaxyMemoryEntry? {
        val content = item.optString("content", "").trim()
        val category = item.optString("category", "NOTES").uppercase().take(24)
        if (content.isEmpty() || content.length > 10000 || category !in ALLOWED_CATEGORIES) return null
        return GalaxyMemoryEntry(item.optLong("id", 0L).coerceAtLeast(0L), category, content,
            item.optLong("createdAt", 0L).coerceIn(0L, System.currentTimeMillis() + 86_400_000L),
            item.optInt("priority", 2).coerceIn(1, 3), item.optDouble("confidence", 1.0).toFloat().coerceIn(0f, 1f))
    }

    private fun archivePrefs(context: Context) = context.getSharedPreferences(ARCHIVE_PREFS, Context.MODE_PRIVATE)

    private fun recordVersion(context: Context, entry: GalaxyMemoryEntry) {
        val prefs = archivePrefs(context)
        val old = try { JSONArray(prefs.getString(KEY_VERSIONS, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        val kept = JSONArray()
        val history = mutableListOf<JSONObject>()
        for (i in 0 until old.length()) {
            val item = old.optJSONObject(i) ?: continue
            if (item.optLong("memoryId", -1L) == entry.id) history.add(item) else kept.put(item)
        }
        history.add(JSONObject().put("memoryId", entry.id).put("savedAt", System.currentTimeMillis()).put("entry", entryJson(entry)))
        history.takeLast(MAX_VERSIONS_PER_MEMORY).forEach { kept.put(it) }
        prefs.edit().putString(KEY_VERSIONS, kept.toString()).commit()
    }

    fun versions(context: Context, memoryId: Long): List<MemoryVersion> {
        val raw = archivePrefs(context).getString(KEY_VERSIONS, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                if (item.optLong("memoryId", -1L) != memoryId) return@mapNotNull null
                val entry = parseEntry(item.optJSONObject("entry") ?: return@mapNotNull null) ?: return@mapNotNull null
                MemoryVersion(entry, item.optLong("savedAt", 0L))
            }.sortedByDescending { it.savedAt }
        } catch (_: Exception) { emptyList() }
    }

    fun restoreVersion(context: Context, memoryId: Long, version: MemoryVersion): Boolean {
        val entries = getAll(context).toMutableList()
        val index = entries.indexOfFirst { it.id == memoryId }
        if (index < 0 || isDuplicate(context, version.entry.category, version.entry.content, memoryId)) return false
        val current = entries[index]
        recordVersion(context, current)
        entries[index] = current.copy(category = version.entry.category, content = version.entry.content,
            priority = version.entry.priority, confidence = version.entry.confidence)
        return save(context, entries)
    }

    fun delete(context: Context, id: Long): Boolean {
        val entries = getAll(context)
        val target = entries.firstOrNull { it.id == id } ?: return false
        val prefs = archivePrefs(context)
        val old = try { JSONArray(prefs.getString(KEY_TRASH, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        val items = mutableListOf<JSONObject>()
        for (i in 0 until old.length()) {
            val item = old.optJSONObject(i) ?: continue
            if (item.optJSONObject("entry")?.optLong("id", -1L) != id) items.add(item)
        }
        items.add(JSONObject().put("deletedAt", System.currentTimeMillis()).put("entry", entryJson(target)))
        val bounded = JSONArray()
        items.takeLast(MAX_TRASH_ITEMS).forEach { bounded.put(it) }
        if (!prefs.edit().putString(KEY_TRASH, bounded.toString()).commit()) return false
        return save(context, entries.filterNot { it.id == id })
    }

    fun getTrash(context: Context): List<TrashedMemory> {
        val raw = archivePrefs(context).getString(KEY_TRASH, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                val entry = parseEntry(item.optJSONObject("entry") ?: return@mapNotNull null) ?: return@mapNotNull null
                TrashedMemory(entry, item.optLong("deletedAt", 0L))
            }.sortedByDescending { it.deletedAt }
        } catch (_: Exception) { emptyList() }
    }

    fun restoreDeleted(context: Context, memoryId: Long): Boolean {
        val target = getTrash(context).firstOrNull { it.entry.id == memoryId } ?: return false
        if (isDuplicate(context, target.entry.category, target.entry.content)) return false
        val entries = getAll(context).toMutableList()
        val nextId = maxOf(entries.maxOfOrNull { it.id } ?: 0L, memoryId) + 1L
        entries.add(0, target.entry.copy(id = nextId, createdAt = System.currentTimeMillis()))
        if (!save(context, entries.take(MAX_ENTRIES))) return false
        val raw = try { JSONArray(archivePrefs(context).getString(KEY_TRASH, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        val kept = JSONArray()
        for (i in 0 until raw.length()) {
            val item = raw.optJSONObject(i) ?: continue
            if (item.optJSONObject("entry")?.optLong("id", -1L) != memoryId) kept.put(item)
        }
        return archivePrefs(context).edit().putString(KEY_TRASH, kept.toString()).commit()
    }

    fun clear(context: Context) {
        // Explicit Clear All also clears timeline and trash.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ENTRIES).remove(KEY_CORRUPT_BACKUP).apply()
        archivePrefs(context).edit().remove(KEY_VERSIONS).remove(KEY_TRASH).apply()
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
