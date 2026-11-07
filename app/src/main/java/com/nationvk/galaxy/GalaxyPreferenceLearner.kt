package com.nationvk.galaxy

import android.content.Context

/** Suggests preferences only from memories the user explicitly saved. Nothing is auto-promoted. */
data class GalaxyPreferenceSuggestion(
    val sourceMemoryId: Long,
    val statement: String,
    val confidence: Float,
    val reason: String
)

object GalaxyPreferenceLearner {
    private val markers = listOf(
        "i prefer", "i like", "i love", "i want", "i always", "my preference",
        "আমার পছন্দ", "আমি পছন্দ করি", "আমি চাই", "সবসময়", "সব সময়", "ভালো লাগে"
    )

    fun suggestions(context: Context, limit: Int = 5): List<GalaxyPreferenceSuggestion> {
        return GalaxyMemoryStore.getAll(context)
            .asSequence()
            .filter { it.category == "PERSONAL" }
            .mapNotNull { memory ->
                val text = memory.content.trim()
                val marker = markers.firstOrNull { text.contains(it, ignoreCase = true) }
                    ?: return@mapNotNull null
                val confidence = (0.55f + (marker.length.coerceAtMost(12) / 40f))
                    .coerceIn(0.55f, 0.85f)
                GalaxyPreferenceSuggestion(
                    sourceMemoryId = memory.id,
                    statement = text,
                    confidence = confidence,
                    reason = "Explicit preference wording found in a saved PERSONAL memory"
                )
            }
            .take(limit.coerceIn(1, 10))
            .toList()
    }

    /** Promotion requires an explicit tap from the user. */
    fun approve(context: Context, suggestion: GalaxyPreferenceSuggestion): Boolean =
        GalaxyMemoryStore.addOrRefresh(
            context = context,
            category = "PERSONAL",
            content = "Approved preference: ${suggestion.statement}",
            priority = 3,
            confidence = suggestion.confidence
        )
}
