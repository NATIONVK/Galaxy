package com.nationvk.galaxy

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Opt-in, narrowly scoped observer for explicitly supported Android Settings switches.
 * It never clicks, types, changes settings, or reads content from other apps.
 */
class GalaxyAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in ALLOWED_SETTINGS_PACKAGES) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val root = rootInActiveWindow ?: return
        val found = linkedMapOf<String, Boolean>()
        inspect(root, found, 0, intArrayOf(0))
        if (found.isNotEmpty()) {
            synchronized(lock) {
                latest = found.toMap()
                updatedAt = SystemClock.elapsedRealtime()
            }
        }
    }

    private fun inspect(
        node: AccessibilityNodeInfo,
        found: MutableMap<String, Boolean>,
        depth: Int,
        visited: IntArray
    ) {
        if (depth > MAX_DEPTH || visited[0] >= MAX_NODES || found.size >= LABELS.size) return
        visited[0]++
        val label = listOfNotNull(
            node.text?.toString(),
            node.contentDescription?.toString()
        ).joinToString(" ").lowercase(Locale.ROOT)

        if (node.isCheckable) {
            val key = LABELS.entries.firstOrNull { (_, aliases) ->
                aliases.any { alias -> label.contains(alias) }
            }?.key
            if (key != null) found[key] = node.isChecked
        }

        for (index in 0 until node.childCount) {
            if (visited[0] >= MAX_NODES || found.size >= LABELS.size) break
            val child = node.getChild(index) ?: continue
            inspect(child, found, depth + 1, visited)
            child.recycle()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val MAX_DEPTH = 12
        private const val MAX_NODES = 180
        private val lock = Any()
        private val ALLOWED_SETTINGS_PACKAGES = setOf("com.android.settings")
        private val LABELS = linkedMapOf(
            "Wi-Fi" to listOf("wi-fi", "wifi"),
            "Bluetooth" to listOf("bluetooth"),
            "Location" to listOf("location"),
            "Dark theme" to listOf("dark theme", "dark mode"),
            "Auto brightness" to listOf("adaptive brightness", "auto brightness"),
            "Do Not Disturb" to listOf("do not disturb"),
            "Battery Saver" to listOf("battery saver", "power saving"),
            "Airplane mode" to listOf("airplane mode", "flight mode")
        )

        @Volatile
        var instance: GalaxyAccessibilityService? = null
            private set

        private var latest: Map<String, Boolean> = emptyMap()
        private var updatedAt: Long = 0L

        fun getLatestSwitchSnapshot(): Pair<Map<String, Boolean>, Long> =
            synchronized(lock) { latest.toMap() to updatedAt }
    }
}
