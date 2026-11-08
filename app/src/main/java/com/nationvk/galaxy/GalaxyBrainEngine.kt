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
        if (matches(q, "ফোনের অবস্থা", "ফোন কেমন আছে", "আমার ফোন কেমন", "phone status", "phone health", "how is my phone", "device status", "ফোনের হেলথ", "ফোনের স্বাস্থ্য")) {
            return phoneStatus(context)
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

        // Resolve short follow-ups against the last saved Brain exchange.
        val previousTurn = GalaxyConversationMemory.getLastTurn(context)
        val followUpPhrases = listOf(
            "আরও বল", "আরো বল", "আরও বিস্তারিত", "আরো বিস্তারিত", "এটা কী", "এটা কি",
            "ওটা কী", "ওটা কি", "কেন", "কীভাবে", "কিভাবে", "ব্যাখ্যা কর", "বুঝিয়ে বল",
            "বুঝিয়ে বল", "continue", "tell me more", "explain more", "why", "how"
        )
        if (previousTurn != null && followUpPhrases.any { q.contains(it) }) {
            return Reply(
                "আগের প্রসঙ্গটি ছিল: ${previousTurn.userText}। আগের উত্তরের ভিত্তিতে বলছি: ${previousTurn.assistantText}। কোন দিকটি আরও বিস্তারিত জানতে চাও?",
                "Contextual follow-up • local memory"
            )
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

    fun phoneStatus(context: Context): Reply {
        val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
            ?: run {
                val raw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (raw >= 0 && scale > 0) raw * 100 / scale else -1
            }
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "চার্জ হচ্ছে"
            BatteryManager.BATTERY_STATUS_FULL -> "সম্পূর্ণ চার্জ"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "চার্জার ছাড়া চলছে"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "চার্জ হচ্ছে না"
            else -> "অজানা"
        }
        val temp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val temperature = if (temp >= 0) String.format(Locale.US, "%.1f ডিগ্রি সেলসিয়াস", temp / 10.0) else "পাওয়া যায়নি"
        val healthCode = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "স্বাভাবিক"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "অতিরিক্ত গরম"
            BatteryManager.BATTERY_HEALTH_COLD -> "অস্বাভাবিক ঠান্ডা"
            BatteryManager.BATTERY_HEALTH_DEAD -> "ত্রুটিপূর্ণ"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "ভোল্টেজ সতর্কতা"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "সিস্টেম সতর্কতা"
            else -> "অজানা"
        }
        val power = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val thermalCode = if (android.os.Build.VERSION.SDK_INT >= 29 && power != null) power.currentThermalStatus else -1
        val thermal = when (thermalCode) {
            android.os.PowerManager.THERMAL_STATUS_NONE -> "স্বাভাবিক"
            android.os.PowerManager.THERMAL_STATUS_LIGHT -> "সামান্য উষ্ণ"
            android.os.PowerManager.THERMAL_STATUS_MODERATE -> "মাঝারি"
            android.os.PowerManager.THERMAL_STATUS_SEVERE -> "বেশি গরম"
            android.os.PowerManager.THERMAL_STATUS_CRITICAL -> "গুরুতর গরম"
            android.os.PowerManager.THERMAL_STATUS_EMERGENCY -> "জরুরি তাপমাত্রা"
            android.os.PowerManager.THERMAL_STATUS_SHUTDOWN -> "বন্ধ হওয়ার ঝুঁকি"
            else -> if (android.os.Build.VERSION.SDK_INT >= 29) "পাওয়া যায়নি" else "সমর্থিত নয়"
        }
        val memory = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        memory?.getMemoryInfo(memoryInfo)
        val ram = if (memoryInfo.totalMem > 0L) {
            val used = ((memoryInfo.totalMem - memoryInfo.availMem).coerceAtLeast(0L) * 100L / memoryInfo.totalMem).toInt()
            "$used শতাংশ"
        } else "পাওয়া যায়নি"
        val storage = runCatching {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val freeMb = stat.availableBytes / (1024L * 1024L)
            if (freeMb >= 1024L) String.format(Locale.US, "%.1f GB ফাঁকা", freeMb / 1024.0) else "$freeMb MB ফাঁকা"
        }.getOrDefault("পাওয়া যায়নি")
        val batteryText = if (level >= 0) "$level শতাংশ" else "পাওয়া যায়নি"
        val advice = buildList {
            if (level in 0..15) add("ব্যাটারি কম, চার্জে দাও।")
            if (temp >= 450) add("ব্যাটারি গরম, ভারী কাজ ও চার্জিং সাময়িক বন্ধ রাখো।")
            if (healthCode == BatteryManager.BATTERY_HEALTH_OVERHEAT || healthCode == BatteryManager.BATTERY_HEALTH_DEAD ||
                healthCode == BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE || healthCode == BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE) {
                add("ব্যাটারি health warning আছে; চার্জার পরীক্ষা করো এবং প্রয়োজনে সার্ভিস সেন্টারে দেখাও।")
            }
            if (thermalCode >= android.os.PowerManager.THERMAL_STATUS_SEVERE) {
                add("ফোন ঠান্ডা হতে দাও।")
            }
        }
        val report = "ফোনের বর্তমান অবস্থা। ব্যাটারি $batteryText, $charging। ব্যাটারির তাপমাত্রা $temperature। ব্যাটারি health $health। ফোনের তাপমাত্রার অবস্থা $thermal। র‍্যাম ব্যবহার $ram। স্টোরেজে $storage। " +
            if (advice.isEmpty()) "কোনো জরুরি সতর্কতা পাওয়া যায়নি।" else "পরামর্শ: " + advice.joinToString(" ")
        return Reply(report, "Phone status • live Android snapshot")
    }

    private fun matches(text: String, vararg phrases: String): Boolean =
        phrases.any { text.contains(it) }
}
