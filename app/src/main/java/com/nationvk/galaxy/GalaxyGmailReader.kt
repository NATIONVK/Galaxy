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

class GalaxyGmailNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName != "com.google.android.gm") return
        val notification = sbn.notification ?: return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val preview = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        GalaxyMailStore.save(applicationContext, title, preview, sbn.postTime)
    }
}
