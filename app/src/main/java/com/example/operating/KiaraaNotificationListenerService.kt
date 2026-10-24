package com.example.operating

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService

/**
 * Reads only while the user has enabled Android Notification Access for KIARAA.
 * Notification content is held in system notification objects and is not added to Memory Vault.
 */
class KiaraaNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        activeService = this
    }

    override fun onListenerDisconnected() {
        if (activeService === this) activeService = null
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var activeService: KiaraaNotificationListenerService? = null

        fun hasAccess(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
            val target = ComponentName(context, KiaraaNotificationListenerService::class.java).flattenToString()
            return enabled.split(':').any { it.equals(target, ignoreCase = true) }
        }

        fun readRecent(): DeviceActionResult {
            val service = activeService ?: return DeviceActionResult.needsPermission(
                "Notification access abhi enabled nahi hai. Android Settings mein KIARAA Notification Access enable karo aur phir command repeat karo."
            )
            return runCatching {
                val recent = service.activeNotifications.orEmpty()
                    .filterNot { it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 }
                    .sortedByDescending { it.postTime }
                    .take(5)
                if (recent.isEmpty()) return DeviceActionResult.success("Abhi koi active notification nahi hai.")
                val summary = recent.mapIndexed { index, sbn ->
                    val extras = sbn.notification.extras
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().ifBlank { "App notification" }
                    val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                    val safeTitle = sanitizeNotificationText(title)
                    val safeBody = sanitizeNotificationText(body)
                    "${index + 1}. ${safeTitle.take(100)}: ${safeBody.take(220)}"
                }.joinToString(". ")
                DeviceActionResult.success("Recent notifications: $summary")
            }.getOrElse { DeviceActionResult.failed("Notifications read nahi ho payin: ${it.message ?: "Android notification access error"}") }
        }

        fun captureNotificationKey(indexOneBased: Int): String? {
            val service = activeService ?: return null
            return runCatching {
                service.activeNotifications.orEmpty()
                    .filterNot { it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 }
                    .sortedByDescending { it.postTime }
                    .getOrNull(indexOneBased - 1)?.key
            }.getOrNull()
        }

        fun openRecent(indexOneBased: Int, expectedKey: String?): DeviceActionResult {
            val service = activeService ?: return DeviceActionResult.needsPermission(
                "Notification access enable karna hoga. Settings tab se Notification Access kholkar allow karo."
            )
            return runCatching {
                val items = service.activeNotifications.orEmpty()
                    .filterNot { it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 }
                    .sortedByDescending { it.postTime }
                if (expectedKey.isNullOrBlank()) {
                    return DeviceActionResult.failed("Notification target could not be safely verified before confirmation. Nothing was opened; read notifications again and retry.")
                }
                val item = items.getOrNull(indexOneBased - 1)
                    ?: return DeviceActionResult.failed("Notification number $indexOneBased available nahi hai; “read notifications” se fresh list lo.")
                if (item.key != expectedKey) {
                    return DeviceActionResult.failed("Notification list changed after confirmation. Nothing was opened; read notifications again and retry.")
                }
                val pendingIntent = item.notification.contentIntent
                    ?: return DeviceActionResult.failed("Is notification mein open action available nahi hai.")
                pendingIntent.send()
                DeviceActionResult.success("Notification $indexOneBased open action dispatched.")
            }.getOrElse { DeviceActionResult.failed("Notification open nahi ho payi: ${it.message ?: "system declined the action"}") }
        }

        private fun sanitizeNotificationText(raw: String): String {
            val text = raw.trim()
            if (text.isBlank()) return "No text preview."
            val sensitive = listOf("password", "passcode", "one-time password", "verification code", "security code", "otp", "pin code")
                .any { text.contains(it, ignoreCase = true) }
            if (sensitive) return "[sensitive code or credential hidden]"
            return text.replace(Regex("""\b\d{4,}\b"""), "••••")
        }
    }
}
