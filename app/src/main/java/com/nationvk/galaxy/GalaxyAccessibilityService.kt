package com.nationvk.galaxy

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * User-enabled accessibility bridge for explicitly requested Galaxy settings actions.
 * This foundation observes system Settings UI events only. It does not click, type,
 * inspect unrelated app content, or perform actions autonomously.
 */
class GalaxyAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Allowlisted Settings state verification will be added in the next update.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: GalaxyAccessibilityService? = null
            private set
    }
}
