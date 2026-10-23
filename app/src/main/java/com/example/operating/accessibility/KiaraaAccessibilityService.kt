package com.example.operating.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent

/**
 * Optional, user-enabled service for discrete navigation commands only.
 * It does not retrieve window content, inspect text, click controls, or type into other apps.
 */
class KiaraaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        connectedService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally do not inspect or retain events or screen contents.
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        if (connectedService === this) connectedService = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (connectedService === this) connectedService = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var connectedService: KiaraaAccessibilityService? = null

        fun isEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expected = ComponentName(context, KiaraaAccessibilityService::class.java).flattenToString()
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServices)
            return splitter.any { it.equals(expected, ignoreCase = true) }
        }

        fun performUserAction(context: Context, action: String): Boolean {
            if (!isEnabled(context)) return false
            val service = connectedService ?: return false
            val globalAction = when (action.trim().lowercase()) {
                "back" -> AccessibilityService.GLOBAL_ACTION_BACK
                "home" -> AccessibilityService.GLOBAL_ACTION_HOME
                "recents" -> AccessibilityService.GLOBAL_ACTION_RECENTS
                "notifications" -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
                else -> return false
            }
            return service.performGlobalAction(globalAction)
        }

        fun openSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
