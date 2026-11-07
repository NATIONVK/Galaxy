package com.nationvk.galaxy

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Galaxy Brain v1 foundation.
 *
 * This is a deterministic, offline intent layer. It does not claim to be an LLM:
 * unknown or multi-step requests are acknowledged and routed to a safe clarification
 * instead of pretending that an action was completed.
 */
object GalaxyBrainEngine {
    data class Reply(
        val spokenText: String,
        val status: String = "Answered by Galaxy Brain"
    )

    fun respond(context: Context, rawText: String): Reply? {
        val text = rawText.trim()
        if (text.isEmpty()) return null
        val q = text.lowercase(Locale.ROOT)

        if (matches(q, "হ্যালো", "হাই", "hello", "hi", "hey", "কেমন আছ", "কেমন আছো")) {
            return Reply("আমি প্রস্তুত আছি। বলো, কী নিয়ে সাহায্য করব?")
        }
        if (matches(q, "ধন্যবাদ", "থ্যাংক ইউ", "thank you", "thanks")) {
            return Reply("তোমাকে স্বাগতম। আর কিছু লাগলে বলো।")
        }
        if (matches(q, "তোমার নাম", "who are you", "তুমি কে", "নিজের পরিচয়")) {
            return Reply("আমি Galaxy, তোমার Android personal assistant। আমি নিরাপদ ও অনুমোদিত কাজগুলোতে সাহায্য করি।")
        }
        if (matches(q, "কয়টা বাজে", "কত বাজে", "সময় বল", "time now", "what time")) {
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            return Reply("এখন সময় $time।")
        }
        if (matches(q, "আজ কত তারিখ", "আজকের তারিখ", "date today", "what date", "আজ কি বার")) {
            val date = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("bn", "BD")).format(Date())
            return Reply("আজ $date।")
        }
        if (matches(q, "ব্যাটারি কত", "battery level", "battery percentage", "চার্জ কত")) {
            val battery = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            return if (level in 0..100) Reply("তোমার ফোনের ব্যাটারি $level শতাংশ।")
            else Reply("এই মুহূর্তে ব্যাটারির তথ্য পাওয়া যাচ্ছে না।")
        }
        if (matches(q, "সেটিংস খোলো", "open settings", "ফোনের সেটিংস")) {
            return Reply("ফোনের Settings খুলছি।", "Opening Android Settings").also {
                try {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) { }
            }
        }

        // Build a safe preview for sequential requests; planning never executes actions.
        GalaxyTaskPlanner.createPlan(text)?.let { plan ->
            val stepSummary = plan.steps.joinToString("; ") { step ->
                "${step.index}. ${step.originalInstruction}" +
                    if (step.supported) " (সমর্থিত, অনুমোদন লাগবে)" else " (এখনো সমর্থিত নয়)"
            }
            return Reply(
                "তোমার কাজের পরিকল্পনা: $stepSummary। ${plan.explanation} এখনো কোনো কাজ চালানো হয়নি।",
                "Task plan prepared • awaiting safe executor"
            )
        }

        // Acknowledge natural-language tasks without falsely claiming execution.
        val taskWords = listOf(
            "করো", "করে দাও", "শুরু কর", "তৈরি কর", "খুঁজে দাও", "দেখে দাও",
            "organize", "summarize", "research", "create", "find", "check", "prepare",
            "send", "পাঠাও", "লিখে দাও", "বিশ্লেষণ", "analyze", "open", "খোলো"
        )
        if (taskWords.any { q.contains(it) }) {
            return Reply(
                "বুঝেছি: $text। কাজটি করার জন্য কোন অ্যাপ বা তথ্য ব্যবহার করতে হবে তা নিশ্চিত করতে চাই। একটু নির্দিষ্ট করে বলবে?",
                "Needs clarification • no action executed"
            )
        }

        // Natural conversation fallback: keep the dialogue open and ask one useful question.
        return Reply(
            "তোমার কথা বুঝেছি: $text। এই বিষয়ে নির্ভুলভাবে সাহায্য করতে আরেকটু প্রসঙ্গ বলবে?",
            "Conversation continued • clarification needed"
        )
    }

    private fun matches(text: String, vararg phrases: String): Boolean =
        phrases.any { text.contains(it) }
}
