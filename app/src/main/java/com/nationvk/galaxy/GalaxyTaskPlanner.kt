package com.nationvk.galaxy

import java.util.Locale

/**
 * Safe, deterministic task planner.
 *
 * The planner describes candidate steps only. It never launches apps, sends messages,
 * changes device settings, or performs network actions. A future executor must validate
 * each step and obtain user confirmation where required.
 */
object GalaxyTaskPlanner {
    enum class ActionType {
        OPEN_SETTINGS,
        OPEN_YOUTUBE,
        OPEN_BROWSER,
        OPEN_CAMERA,
        OPEN_DIALER,
        UNKNOWN
    }

    data class PlannedStep(
        val index: Int,
        val action: ActionType,
        val originalInstruction: String,
        val requiresConfirmation: Boolean,
        val supported: Boolean
    )

    data class Plan(
        val originalRequest: String,
        val steps: List<PlannedStep>,
        val isExecutable: Boolean = false,
        val explanation: String
    ) {
        val hasUnknownSteps: Boolean get() = steps.any { !it.supported }
        val requiresConfirmation: Boolean get() = steps.any { it.requiresConfirmation }
    }

    /**
     * Splits common sequential requests and classifies only a small allow-list.
     * Unknown steps are preserved rather than guessed.
     */
    fun createPlan(request: String): Plan? {
        val cleaned = request.trim()
        if (cleaned.isEmpty()) return null

        val parts = cleaned.split(
            Regex("\\\\s+(?:and then|then|তারপর|এরপর|এবং তারপর)\\\\s+|[,;]", RegexOption.IGNORE_CASE)
        ).map { it.trim() }.filter { it.isNotEmpty() }

        val steps = parts.mapIndexed { index, part ->
            val normalized = part.lowercase(Locale.ROOT)
            val action = when {
                containsAny(normalized, "settings", "সেটিংস") -> ActionType.OPEN_SETTINGS
                containsAny(normalized, "youtube", "ইউটিউব") -> ActionType.OPEN_YOUTUBE
                containsAny(normalized, "browser", "ব্রাউজার", "chrome") -> ActionType.OPEN_BROWSER
                containsAny(normalized, "camera", "ক্যামেরা") -> ActionType.OPEN_CAMERA
                containsAny(normalized, "dialer", "phone app", "ডায়ালার", "ডায়ালার") -> ActionType.OPEN_DIALER
                else -> ActionType.UNKNOWN
            }
            PlannedStep(
                index = index + 1,
                action = action,
                originalInstruction = part,
                requiresConfirmation = action != ActionType.UNKNOWN,
                supported = action != ActionType.UNKNOWN
            )
        }

        // Only return a plan for an apparent action request, not ordinary conversation.
        val looksLikeTask = containsAny(
            cleaned.lowercase(Locale.ROOT),
            "open", "খোলো", "চালু কর", "চালু করো", "তারপর", "and then", "then"
        )
        if (!looksLikeTask) return null

        val explanation = when {
            steps.size > 1 && steps.any { !it.supported } ->
                "কিছু ধাপ এখনো সমর্থিত নয়। অজানা ধাপ অনুমান করে চালানো হবে না।"
            steps.size > 1 ->
                "একাধিক ধাপের পরিকল্পনা তৈরি হয়েছে। চালানোর আগে প্রতিটি ধাপ যাচাই ও অনুমোদন প্রয়োজন।"
            steps.first().supported ->
                "সমর্থিত কাজ শনাক্ত হয়েছে। চালানোর আগে ব্যবহারকারীর নিশ্চিতকরণ প্রয়োজন।"
            else ->
                "কাজটি এখনো শনাক্ত করা যায়নি। আরও নির্দিষ্ট নির্দেশনা প্রয়োজন।"
        }

        return Plan(cleaned, steps, isExecutable = false, explanation = explanation)
    }

    private fun containsAny(text: String, vararg values: String): Boolean =
        values.any { text.contains(it) }
}
