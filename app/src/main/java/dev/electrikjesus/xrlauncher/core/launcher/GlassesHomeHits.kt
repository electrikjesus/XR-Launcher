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
    const val EDIT_TOGGLE = "__xr_edit_toggle__"
    const val EDIT_PANEL_MINUS = "__xr_edit_panel_minus__"
    const val EDIT_PANEL_PLUS = "__xr_edit_panel_plus__"
    const val EDIT_SPHERE_MINUS = "__xr_edit_sphere_minus__"
    const val EDIT_SPHERE_PLUS = "__xr_edit_sphere_plus__"
    const val EDIT_ELEMENT_MINUS = "__xr_edit_element_minus__"
    const val EDIT_ELEMENT_PLUS = "__xr_edit_element_plus__"

    const val HOME_LABEL = "Home"
    const val ALL_APPS_LABEL = "All apps"
    const val RECENTS_LABEL = "Recents"
    const val NOTIFICATIONS_LABEL = "Notifications"
    const val QUICK_SETTINGS_LABEL = "Quick settings"
    const val SETTINGS_LABEL = "Settings"
    const val CLEAR_ALL_LABEL = "Clear all"
    const val CLOSE_LABEL = "Close"
    const val EDIT_LABEL = "Edit space"
    const val DONE_LABEL = "Done editing"
    const val PANEL_SMALLER_LABEL = "Smaller panels"
    const val PANEL_LARGER_LABEL = "Larger panels"
    const val SPHERE_CLOSER_LABEL = "Closer sphere"
    const val SPHERE_FARTHER_LABEL = "Farther sphere"
    const val ELEMENT_SMALLER_LABEL = "Smaller icons"
    const val ELEMENT_LARGER_LABEL = "Larger icons"

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
            EDIT_TOGGLE -> EDIT_LABEL
            EDIT_PANEL_MINUS -> PANEL_SMALLER_LABEL
            EDIT_PANEL_PLUS -> PANEL_LARGER_LABEL
            EDIT_SPHERE_MINUS -> SPHERE_CLOSER_LABEL
            EDIT_SPHERE_PLUS -> SPHERE_FARTHER_LABEL
            EDIT_ELEMENT_MINUS -> ELEMENT_SMALLER_LABEL
            EDIT_ELEMENT_PLUS -> ELEMENT_LARGER_LABEL
            else -> null
        }
    }

    fun actionKeyAt(contains: (String) -> Boolean): String? = listOf(
        EDIT_PANEL_MINUS,
        EDIT_PANEL_PLUS,
        EDIT_SPHERE_MINUS,
        EDIT_SPHERE_PLUS,
        EDIT_ELEMENT_MINUS,
        EDIT_ELEMENT_PLUS,
        EDIT_TOGGLE,
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
