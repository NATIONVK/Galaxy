package com.nationvk.galaxy

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject

data class GalaxyMailPreview(
    val title: String,
    val preview: String,
    val receivedAt: Long
)

object GalaxyMailStore {
    private const val PREFS = "galaxy_mail_reader"
    private const val KEY_ITEMS = "gmail_previews"
    private const val MAX_ITEMS = 20

    @Synchronized
    fun save(context: Context, title: String, preview: String, receivedAt: Long) {
        if (title.isBlank() && preview.isBlank()) return
        val current = read(context).toMutableList()
        current.add(0, GalaxyMailPreview(title.take(180), preview.take(1200), receivedAt))
        val json = JSONArray()
        current.distinctBy { "${it.receivedAt}|${it.title}|${it.preview}" }
            .take(MAX_ITEMS)
            .forEach {
                json.put(JSONObject().put("title", it.title).put("preview", it.preview).put("time", it.receivedAt))
            }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, json.toString()).apply()
    }

    @Synchronized
    fun read(context: Context): List<GalaxyMailPreview> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                GalaxyMailPreview(item.optString("title"), item.optString("preview"), item.optLong("time"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ITEMS).apply()
    }
}

data class GalaxyMessagePreview(
    val appName: String,
    val sender: String,
    val content: String,
    val receivedAt: Long,
    val isVoiceMessage: Boolean
)

object GalaxyMessageStore {
    private const val PREFS = "galaxy_message_reader"
    private const val KEY_ITEMS = "message_previews"
    private const val MAX_ITEMS = 40

    @Synchronized
    fun save(context: Context, appName: String, sender: String, content: String, receivedAt: Long, isVoiceMessage: Boolean) {
        if (sender.isBlank() && content.isBlank()) return
        val current = read(context).toMutableList()
        current.add(0, GalaxyMessagePreview(appName.take(60), sender.take(180), content.take(1600), receivedAt, isVoiceMessage))
        val json = JSONArray()
        current.distinctBy { "${it.receivedAt}|${it.appName}|${it.sender}|${it.content}" }
            .take(MAX_ITEMS).forEach {
                json.put(JSONObject().put("app", it.appName).put("sender", it.sender)
                    .put("content", it.content).put("time", it.receivedAt).put("voice", it.isVoiceMessage))
            }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, json.toString()).apply()
    }

    @Synchronized
    fun read(context: Context): List<GalaxyMessagePreview> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                GalaxyMessagePreview(item.optString("app"), item.optString("sender"),
                    item.optString("content"), item.optLong("time"), item.optBoolean("voice"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun latest(context: Context): GalaxyMessagePreview? = read(context).firstOrNull()
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ITEMS).apply()
    }
}

class GalaxyGmailNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName == packageName) return
        val notification = sbn.notification ?: return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return
        val supportedApps = setOf(
            "com.google.android.gm", "com.whatsapp", "com.facebook.orca",
            "org.telegram.messenger", "com.google.android.apps.messaging",
            "com.samsung.android.messaging", "com.android.messaging",
            "com.instagram.android", "com.viber.voip", "jp.naver.line.android",
            "com.discord", "com.skype.raider"
        )
        if (sbn.packageName !in supportedApps) return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val preview = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val appName = try {
            val app = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(app).toString()
        } catch (_: Exception) { sbn.packageName }
        val voiceText = "$title $preview".lowercase(java.util.Locale.ROOT)
        val isVoice = listOf("voice message", "audio message", "voice note", "ভয়েস মেসেজ",
            "ভয়েস মেসেজ", "অডিও মেসেজ", "🎤", "🎙").any { voiceText.contains(it) }
        GalaxyMessageStore.save(applicationContext, appName, title, preview, sbn.postTime, isVoice)
        if (sbn.packageName == "com.google.android.gm") {
            GalaxyMailStore.save(applicationContext, title, preview, sbn.postTime)
        }
    }
}
