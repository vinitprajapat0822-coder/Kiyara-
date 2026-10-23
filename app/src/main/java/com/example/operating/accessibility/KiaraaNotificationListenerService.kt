package com.example.operating.accessibility

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Optional listener for notification summaries. It keeps a small in-memory snapshot only;
 * callers decide when to display it. No notification content is persisted or sent to AI.
 */
class KiaraaNotificationListenerService : NotificationListenerService() {

    @Volatile
    private var summaries: List<String> = emptyList()

    override fun onListenerConnected() {
        super.onListenerConnected()
        connectedListener = this
        refreshSnapshot()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshSnapshot()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshSnapshot()
    }

    override fun onListenerDisconnected() {
        if (connectedListener === this) connectedListener = null
        summaries = emptyList()
        super.onListenerDisconnected()
    }

    private fun refreshSnapshot() {
        summaries = try {
            activeNotifications.orEmpty().mapNotNull { item ->
                val extras = item.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?.replace(Regex("\\s+"), " ")?.trim()?.take(100).orEmpty()
                val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    ?.replace(Regex("\\s+"), " ")?.trim()?.take(240).orEmpty()
                if (title.isBlank() && body.isBlank()) null
                else "${item.packageName}: ${title.ifBlank { "Notification" }}${if (body.isNotBlank()) " — $body" else ""}"
            }.takeLast(MAX_NOTIFICATION_SUMMARIES)
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val MAX_NOTIFICATION_SUMMARIES = 30

        @Volatile
        private var connectedListener: KiaraaNotificationListenerService? = null

        fun isAccessGranted(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val expected = ComponentName(
                context,
                KiaraaNotificationListenerService::class.java
            ).flattenToString()
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        fun currentNotificationSummaries(): List<String>? {
            return connectedListener?.summaries?.toList()
        }

        fun openSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
