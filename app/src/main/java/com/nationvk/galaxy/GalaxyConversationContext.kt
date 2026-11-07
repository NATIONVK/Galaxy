package com.nationvk.galaxy

import android.content.Context

/** Builds a bounded, local context packet for the future Galaxy model gateway. */
data class GalaxyConversationContextPacket(
    val turnCount: Int,
    val memoryCount: Int,
    val isFollowUp: Boolean,
    val resolvedMessage: String,
    val promptContext: String,
    val preview: String
)

object GalaxyConversationContext {
    private const val MAX_TURNS = 12
    private const val MAX_CONTEXT_CHARS = 9000

    fun prepare(
        context: Context,
        history: List<Pair<Boolean, String>> = GalaxyLocalConversationStore.load(context),
        currentMessage: String
    ): GalaxyConversationContextPacket {
        val recent = history.takeLast(MAX_TURNS)
        val query = currentMessage.trim()
        val followUp = query.isNotEmpty() && isFollowUp(query)
        val previousUser = recent.lastOrNull { it.first }?.second.orEmpty()
        val resolved = if (followUp && previousUser.isNotBlank()) {
            "$query\\n[Follow-up to the user's previous request: $previousUser]"
        } else query
        val memoryQuery = if (query.isBlank()) previousUser else query
        val memories = if (memoryQuery.isBlank()) emptyList() else
            GalaxyMemoryStore.search(context, memoryQuery, limit = 3)
        val historyBlock = recent.joinToString("\\n") { (isUser, text) ->
            (if (isUser) "User" else "Galaxy") + ": " + text.take(900)
        }
        val memoryBlock = memories.joinToString("\\n") { "- [${it.category}] ${it.content.take(500)}" }
        val packet = buildString {
            appendLine("You are Galaxy, a context-aware assistant. Reply in the user's language.")
            if (historyBlock.isNotBlank()) appendLine("Recent conversation:\\n$historyBlock")
            if (memoryBlock.isNotBlank()) appendLine("Relevant user-approved local memories:\\n$memoryBlock")
            if (resolved.isNotBlank()) appendLine("Current user message:\\n$resolved")
            append("Use the conversation and memories only when relevant. Do not invent missing facts.")
        }.take(MAX_CONTEXT_CHARS)
        val preview = buildString {
            append("Recent turns: ${recent.size}")
            if (followUp) append("\\nFollow-up reference: ${previousUser.take(180)}")
            if (memories.isNotEmpty()) append("\\nRelated memory: ${memories.joinToString { it.category }.take(120)}")
            if (query.isNotBlank()) append("\\nCurrent message: ${query.take(180)}")
            if (query.isBlank() && previousUser.isBlank()) append("No conversation context yet. Send a message to build context.")
        }
        return GalaxyConversationContextPacket(recent.size, memories.size, followUp, resolved, packet, preview)
    }

    private fun isFollowUp(message: String): Boolean {
        val value = message.trim().lowercase()
        val exact = setOf("why", "how", "what about that", "explain more", "continue", "go on", "আরও বলো", "তারপর", "কেন", "কীভাবে", "এটা বুঝাও", "বিস্তারিত বলো", "আরো বিস্তারিত", "এটার মানে কী")
        return value in exact || value.startsWith("এটা ") || value.startsWith("ওটা ") ||
            value.startsWith("সেটা ") || value.startsWith("আরও ") || value.startsWith("তার ") ||
            value.startsWith("it ") || value.startsWith("that ") || value.startsWith("and ") ||
            value.startsWith("also ") || value.startsWith("what about ") || value.startsWith("how about ")
    }
}
