package com.nationvk.galaxy

import android.content.Context

/**
 * Explicit, device-local personalization settings.
 * No conversation is silently scanned or used to change these values.
 */
data class GalaxyPersonalization(
    val responseStyle: String = "Balanced",
    val tone: String = "Friendly",
    val assistantMode: String = "General",
    val learningEnabled: Boolean = false
)

object GalaxyPersonalizationStore {
    private const val PREFS = "galaxy_personalization"
    private const val STYLE = "response_style"
    private const val TONE = "tone"
    private const val MODE = "assistant_mode"
    private const val LEARNING = "learning_enabled"

    val responseStyles = listOf("Concise", "Balanced", "Detailed")
    val tones = listOf("Friendly", "Professional", "Playful")
    val modes = listOf("General", "Study", "Coding", "Research")

    fun load(context: Context): GalaxyPersonalization {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return GalaxyPersonalization(
            responseStyle = prefs.getString(STYLE, "Balanced")?.takeIf { it in responseStyles } ?: "Balanced",
            tone = prefs.getString(TONE, "Friendly")?.takeIf { it in tones } ?: "Friendly",
            assistantMode = prefs.getString(MODE, "General")?.takeIf { it in modes } ?: "General",
            learningEnabled = prefs.getBoolean(LEARNING, false)
        )
    }

    fun save(context: Context, value: GalaxyPersonalization): Boolean {
        if (value.responseStyle !in responseStyles || value.tone !in tones || value.assistantMode !in modes) return false
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(STYLE, value.responseStyle)
            .putString(TONE, value.tone)
            .putString(MODE, value.assistantMode)
            .putBoolean(LEARNING, value.learningEnabled)
            .commit()
    }

    fun promptGuidance(context: Context): String {
        val p = load(context)
        val style = when (p.responseStyle) {
            "Concise" -> "Keep answers concise and direct unless the user asks for detail."
            "Detailed" -> "Give structured, step-by-step explanations with useful examples."
            else -> "Use a balanced level of detail and explain important points clearly."
        }
        val tone = when (p.tone) {
            "Professional" -> "Use a clear, respectful and professional tone."
            "Playful" -> "Use a light, playful tone when appropriate; never undermine serious topics."
            else -> "Use a warm, friendly and respectful tone."
        }
        val mode = when (p.assistantMode) {
            "Study" -> "Study mode: teach concepts progressively, define terms and include a small practice example when useful."
            "Coding" -> "Coding mode: prioritize practical implementation, explain assumptions and mention testing or safety."
            "Research" -> "Research mode: distinguish evidence from assumptions, structure methods and state uncertainty."
            else -> "General mode: adapt to the user's current request."
        }
        return listOf("User-controlled personalization:", style, tone, mode,
            if (p.learningEnabled) "Preference learning is enabled, but ask before saving or changing any preference." else "Preference learning is disabled; do not infer or save new preferences.")
            .joinToString("\n")
    }
}
