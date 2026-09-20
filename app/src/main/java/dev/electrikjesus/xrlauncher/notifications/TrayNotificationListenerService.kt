package dev.electrikjesus.xrlauncher.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.provider.Settings
import dev.electrikjesus.xrlauncher.core.launcher.TrayNotification
import dev.electrikjesus.xrlauncher.core.launcher.TrayNotificationBus

/**
 * Play-friendly notification access for the Home Space tray.
 * User must enable this service in Settings → Notifications → Device & app notifications.
 */
class TrayNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        TrayNotificationBus.setConnected(true)
        TrayNotificationBus.clearAllActive = {
            runCatching { cancelAllNotifications() }
        }
        TrayNotificationBus.dismissKey = { key ->
            runCatching {
                activeNotifications?.firstOrNull { it.key == key }?.let { cancelNotification(it.key) }
            }
        }
        TrayNotificationBus.openKey = { key ->
            runCatching {
                activeNotifications?.firstOrNull { it.key == key }
                    ?.notification
                    ?.contentIntent
                    ?.send()
            }
        }
        publishActive()
    }

    override fun onListenerDisconnected() {
        TrayNotificationBus.clearAllActive = null
        TrayNotificationBus.dismissKey = null
        TrayNotificationBus.openKey = null
        TrayNotificationBus.setConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publishActive()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publishActive()
    }

    private fun publishActive() {
        val items = activeNotifications
            ?.asSequence()
            ?.filter { !it.isOngoing && it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
            ?.map { it.toTrayNotification() }
            ?.toList()
            .orEmpty()
        TrayNotificationBus.publish(items)
    }

    private fun StatusBarNotification.toTrayNotification(): TrayNotification {
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: packageName
        val body = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""
        return TrayNotification(
            key = key,
            packageName = packageName,
            title = title,
            body = body,
            postTimeMs = postTime,
        )
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            val component = ComponentName(context, TrayNotificationListenerService::class.java)
            return flat.split(':', ';').any { token ->
                ComponentName.unflattenFromString(token.trim()) == component ||
                    token.equals(component.flattenToString(), ignoreCase = true)
            }
        }

        fun settingsIntent(): android.content.Intent =
            android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
