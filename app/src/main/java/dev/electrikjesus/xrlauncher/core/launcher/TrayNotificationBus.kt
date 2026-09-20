package dev.electrikjesus.xrlauncher.core.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrayNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val body: String,
    val postTimeMs: Long,
)

/** Live notifications for the Home Space tray (fed by NotificationListenerService). */
object TrayNotificationBus {
    private val _notifications = MutableStateFlow<List<TrayNotification>>(emptyList())
    val notificationsFlow: StateFlow<List<TrayNotification>> = _notifications.asStateFlow()

    val notifications: List<TrayNotification> get() = _notifications.value

    var listenerConnected: Boolean = false
        private set

    fun setConnected(connected: Boolean) {
        listenerConnected = connected
        if (!connected) {
            _notifications.value = emptyList()
        }
    }

    fun publish(items: List<TrayNotification>) {
        _notifications.value = items.sortedByDescending { it.postTimeMs }
    }

    fun clear() {
        _notifications.value = emptyList()
        clearAllActive?.invoke()
    }

    /** Bound by the notification listener when connected. */
    var clearAllActive: (() -> Unit)? = null
    var dismissKey: ((String) -> Unit)? = null
    var openKey: ((String) -> Unit)? = null
}
