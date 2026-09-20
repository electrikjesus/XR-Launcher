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
    const val EDIT_CLOSE = "__xr_edit_close__"
    const val EDIT_PAGE_PERSPECTIVE = "__xr_edit_page_perspective__"
    const val EDIT_PAGE_DESKTOP = "__xr_edit_page_desktop__"
    const val EDIT_DESK_ICONS = "__xr_edit_desk_icons__"
    const val EDIT_DESK_PILES = "__xr_edit_desk_piles__"
    const val EDIT_DESK_TILES = "__xr_edit_desk_tiles__"
    const val EDIT_DESK_WIDGETS = "__xr_edit_desk_widgets__"
    const val EDIT_LOOK_FPS = "__xr_edit_look_fps__"
    const val HUD_INPUT_TOUCHPAD = "__xr_hud_input_touchpad__"
    const val HUD_INPUT_HEAD = "__xr_hud_input_head__"
    const val HUD_LOOK_GESTURE = "__xr_hud_look_gesture__"
    const val HUD_LOOK_MODE = "__xr_hud_look_mode__"
    const val HUD_RECENTER = "__xr_hud_recenter__"
    const val HUD_KEYBOARD = "__xr_hud_keyboard__"
    const val HUD_SETTINGS = "__xr_hud_settings__"
    const val QS_WIFI = "__xr_qs_wifi__"
    const val QS_BLUETOOTH = "__xr_qs_bluetooth__"
    const val QS_BRIGHTNESS = "__xr_qs_brightness__"
    const val QS_NOTIFICATIONS = "__xr_qs_notifications__"
    const val NOTIFICATION_LISTENER = "__xr_notification_listener__"
    const val NOTIFICATION_ITEM_PREFIX = "__xr_notif_item_"

    fun notificationItemKey(notificationKey: String): String =
        NOTIFICATION_ITEM_PREFIX + notificationKey

    fun notificationKeyFromHit(hitKey: String): String? =
        if (hitKey.startsWith(NOTIFICATION_ITEM_PREFIX)) {
            hitKey.removePrefix(NOTIFICATION_ITEM_PREFIX).ifBlank { null }
        } else {
            null
        }

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
    const val EDIT_PAGE_PERSPECTIVE_LABEL = "Perspective"
    const val EDIT_PAGE_DESKTOP_LABEL = "Desktop items"
    const val DESK_ICONS_LABEL = "App icons"
    const val DESK_PILES_LABEL = "Piles"
    const val DESK_TILES_LABEL = "Tiles"
    const val DESK_WIDGETS_LABEL = "Widgets"
    const val LOOK_FPS_LABEL = "FPS look"
    const val HUD_INPUT_TOUCHPAD_LABEL = "Touchpad cursor"
    const val HUD_INPUT_HEAD_LABEL = "Head tracking"
    const val HUD_LOOK_GESTURE_LABEL = "Gesture look"
    const val HUD_LOOK_MODE_LABEL = "Mouse look"
    const val HUD_RECENTER_LABEL = "Recenter"
    const val HUD_KEYBOARD_LABEL = "Keyboard"
    const val HUD_SETTINGS_LABEL = "Settings"
    const val QS_WIFI_LABEL = "Wi‑Fi"
    const val QS_BLUETOOTH_LABEL = "Bluetooth"
    const val QS_BRIGHTNESS_LABEL = "Brightness"
    const val QS_NOTIFICATIONS_LABEL = "Notification access"
    const val NOTIFICATION_LISTENER_LABEL = "Enable notification access"

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
            EDIT_CLOSE -> CLOSE_LABEL
            EDIT_PANEL_MINUS -> PANEL_SMALLER_LABEL
            EDIT_PANEL_PLUS -> PANEL_LARGER_LABEL
            EDIT_SPHERE_MINUS -> SPHERE_CLOSER_LABEL
            EDIT_SPHERE_PLUS -> SPHERE_FARTHER_LABEL
            EDIT_ELEMENT_MINUS -> ELEMENT_SMALLER_LABEL
            EDIT_ELEMENT_PLUS -> ELEMENT_LARGER_LABEL
            EDIT_PAGE_PERSPECTIVE -> EDIT_PAGE_PERSPECTIVE_LABEL
            EDIT_PAGE_DESKTOP -> EDIT_PAGE_DESKTOP_LABEL
            EDIT_DESK_ICONS -> DESK_ICONS_LABEL
            EDIT_DESK_PILES -> DESK_PILES_LABEL
            EDIT_DESK_TILES -> DESK_TILES_LABEL
            EDIT_DESK_WIDGETS -> DESK_WIDGETS_LABEL
            EDIT_LOOK_FPS -> LOOK_FPS_LABEL
            HUD_INPUT_TOUCHPAD -> HUD_INPUT_TOUCHPAD_LABEL
            HUD_INPUT_HEAD -> HUD_INPUT_HEAD_LABEL
            HUD_LOOK_GESTURE -> HUD_LOOK_GESTURE_LABEL
            HUD_LOOK_MODE -> HUD_LOOK_MODE_LABEL
            HUD_RECENTER -> HUD_RECENTER_LABEL
            HUD_KEYBOARD -> HUD_KEYBOARD_LABEL
            HUD_SETTINGS -> HUD_SETTINGS_LABEL
            QS_WIFI -> QS_WIFI_LABEL
            QS_BLUETOOTH -> QS_BLUETOOTH_LABEL
            QS_BRIGHTNESS -> QS_BRIGHTNESS_LABEL
            QS_NOTIFICATIONS -> QS_NOTIFICATIONS_LABEL
            NOTIFICATION_LISTENER -> NOTIFICATION_LISTENER_LABEL
            else -> null
        }
    }

    fun actionKeyAt(contains: (String) -> Boolean): String? = listOf(
        EDIT_CLOSE,
        EDIT_PAGE_PERSPECTIVE,
        EDIT_PAGE_DESKTOP,
        EDIT_DESK_ICONS,
        EDIT_DESK_PILES,
        EDIT_DESK_TILES,
        EDIT_DESK_WIDGETS,
        EDIT_LOOK_FPS,
        EDIT_PANEL_MINUS,
        EDIT_PANEL_PLUS,
        EDIT_SPHERE_MINUS,
        EDIT_SPHERE_PLUS,
        EDIT_ELEMENT_MINUS,
        EDIT_ELEMENT_PLUS,
        EDIT_TOGGLE,
        HUD_SETTINGS,
        HUD_KEYBOARD,
        HUD_RECENTER,
        HUD_LOOK_MODE,
        HUD_LOOK_GESTURE,
        HUD_INPUT_HEAD,
        HUD_INPUT_TOUCHPAD,
        QS_WIFI,
        QS_BLUETOOTH,
        QS_BRIGHTNESS,
        QS_NOTIFICATIONS,
        NOTIFICATION_LISTENER,
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

    /**
     * Screen-locked HUD / Edit toggle — Minecraft-style viewport chrome, not world panels.
     * Modal Edit/Settings bodies use Compose clickables while open; these keys still block desk grab.
     */
    fun isScreenLockedChromeKey(key: String): Boolean = when (key) {
        EDIT_TOGGLE,
        HUD_INPUT_TOUCHPAD,
        HUD_INPUT_HEAD,
        HUD_LOOK_GESTURE,
        HUD_LOOK_MODE,
        HUD_RECENTER,
        HUD_KEYBOARD,
        HUD_SETTINGS,
        -> true
        else -> false
    }

    /** Edit card controls. Not the corner Edit toggle. Invalid once the card is gone. */
    fun isEditBodyKey(key: String): Boolean = when (key) {
        EDIT_CLOSE,
        EDIT_PAGE_PERSPECTIVE,
        EDIT_PAGE_DESKTOP,
        EDIT_DESK_ICONS,
        EDIT_DESK_PILES,
        EDIT_DESK_TILES,
        EDIT_DESK_WIDGETS,
        EDIT_LOOK_FPS,
        EDIT_PANEL_MINUS,
        EDIT_PANEL_PLUS,
        EDIT_SPHERE_MINUS,
        EDIT_SPHERE_PLUS,
        EDIT_ELEMENT_MINUS,
        EDIT_ELEMENT_PLUS,
        -> true
        else -> false
    }

    val editBodyKeys: List<String> = listOf(
        EDIT_CLOSE,
        EDIT_PAGE_PERSPECTIVE,
        EDIT_PAGE_DESKTOP,
        EDIT_DESK_ICONS,
        EDIT_DESK_PILES,
        EDIT_DESK_TILES,
        EDIT_DESK_WIDGETS,
        EDIT_LOOK_FPS,
        EDIT_PANEL_MINUS,
        EDIT_PANEL_PLUS,
        EDIT_SPHERE_MINUS,
        EDIT_SPHERE_PLUS,
        EDIT_ELEMENT_MINUS,
        EDIT_ELEMENT_PLUS,
    )
}
