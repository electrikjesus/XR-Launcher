package dev.electrikjesus.xrlauncher.core.launcher

/** Pointer-bus hit keys for Android XR-style Home Space chrome. */
object GlassesHomeHits {
    const val HOME = "__xr_home__"
    const val ALL_APPS = "__xr_all_apps_pill__"
    const val RECENTS = "__xr_recents__"
    const val NOTIFICATIONS = "__xr_notifications__"
    const val QUICK_SETTINGS = "__xr_quick_settings__"
    const val SETTINGS = "__xr_settings_pill__"
    const val RECENTS_CLEAR = "__xr_recents_clear__"
    const val NOTIFICATIONS_CLEAR = "__xr_notifications_clear__"
    const val OVERLAY_CLOSE = "__xr_overlay_close__"
    const val APP_CLOSE_PREFIX = "__xr_app_close_"

    const val HOME_LABEL = "Home"
    const val ALL_APPS_LABEL = "All apps"
    const val RECENTS_LABEL = "Recents"
    const val NOTIFICATIONS_LABEL = "Notifications"
    const val QUICK_SETTINGS_LABEL = "Quick settings"
    const val SETTINGS_LABEL = "Settings"
    const val CLEAR_ALL_LABEL = "Clear all"
    const val CLOSE_LABEL = "Close"

    fun appCloseKey(panelId: String): String = APP_CLOSE_PREFIX + panelId

    fun appClosePanelId(key: String): String? =
        if (key.startsWith(APP_CLOSE_PREFIX)) {
            key.removePrefix(APP_CLOSE_PREFIX).ifBlank { null }
        } else {
            null
        }

    fun hoverLabel(key: String): String? = when {
        key.startsWith(APP_CLOSE_PREFIX) -> CLOSE_LABEL
        else -> when (key) {
            HOME -> HOME_LABEL
            ALL_APPS -> ALL_APPS_LABEL
            RECENTS -> RECENTS_LABEL
            NOTIFICATIONS -> NOTIFICATIONS_LABEL
            QUICK_SETTINGS -> QUICK_SETTINGS_LABEL
            SETTINGS -> SETTINGS_LABEL
            RECENTS_CLEAR, NOTIFICATIONS_CLEAR -> CLEAR_ALL_LABEL
            OVERLAY_CLOSE -> CLOSE_LABEL
            else -> null
        }
    }

    fun actionKeyAt(contains: (String) -> Boolean): String? = listOf(
        OVERLAY_CLOSE,
        RECENTS_CLEAR,
        NOTIFICATIONS_CLEAR,
        HOME,
        ALL_APPS,
        RECENTS,
        NOTIFICATIONS,
        QUICK_SETTINGS,
        SETTINGS,
    ).firstOrNull(contains)

    fun actionKeyAt(
        pointX: Float,
        pointY: Float,
        contains: (String) -> Boolean,
    ): String? = actionKeyAt(contains)
}
