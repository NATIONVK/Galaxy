package com.nationvk.galaxy

/** Local-only review hints. These are suggestions, never automatic edits or deletions. */
data class GalaxyMemoryConflict(
    val first: GalaxyMemoryEntry,
    val second: GalaxyMemoryEntry,
    val sharedTerms: List<String>
)

data class GalaxyMemoryReview(
    val staleMemories: List<GalaxyMemoryEntry>,
    val conflicts: List<GalaxyMemoryConflict>
)

object GalaxyMemoryIntelligence {
    private val stopWords = setOf(
        "the", "and", "for", "with", "that", "this", "from", "have", "has", "had",
        "about", "your", "you", "are", "was", "were", "not", "dont", "doesnt",
        "আমি", "আমার", "তুমি", "এবং", "করে", "হবে", "হলো", "একটা", "একটি"
    )
    private val positiveMarkers = listOf("i like", "i love", "i prefer", "আমার পছন্দ", "ভালো লাগে", "পছন্দ করি")
    private val negativeMarkers = listOf("i don't like", "i do not like", "i dislike", "i hate", "ভালো লাগে না", "পছন্দ করি না", "পছন্দ নয়")

    fun review(entries: List<GalaxyMemoryEntry>, now: Long = System.currentTimeMillis()): GalaxyMemoryReview {
        val stale = entries.filter { entry ->
            if (entry.priority >= 3 || entry.createdAt <= 0L) return@filter false
            val age = (now - entry.createdAt).coerceAtLeast(0L)
            val thresholdDays = when (entry.category) {
                "TASK" -> 30L
                "PROJECT", "CONVERSATION" -> 120L
                else -> 180L
            }
            age >= thresholdDays * 86_400_000L
        }.sortedBy { it.createdAt }

        val conflicts = mutableListOf<GalaxyMemoryConflict>()
        val candidates = entries.filter { it.priority < 3 }
        for (i in candidates.indices) {
            for (j in i + 1 until candidates.size) {
                val first = candidates[i]
                val second = candidates[j]
                if (first.category != second.category) continue
                val firstText = first.content.lowercase()
                val secondText = second.content.lowercase()
                val firstPositive = positiveMarkers.any { firstText.contains(it) }
                val secondPositive = positiveMarkers.any { secondText.contains(it) }
                val firstNegative = negativeMarkers.any { firstText.contains(it) }
                val secondNegative = negativeMarkers.any { secondText.contains(it) }
                val opposite = (firstPositive && secondNegative) || (firstNegative && secondPositive)
                if (!opposite) continue
                val shared = tokens(first.content).intersect(tokens(second.content)).filter { it.length >= 3 }
                if (shared.size >= 2) {
                    conflicts.add(GalaxyMemoryConflict(first, second, shared.take(5)))
                    if (conflicts.size >= 20) break
                }
            }
            if (conflicts.size >= 20) break
        }
        return GalaxyMemoryReview(stale, conflicts)
    }

    private fun tokens(value: String): Set<String> =
        value.lowercase().split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 1 && it !in stopWords }.toSet()
}
