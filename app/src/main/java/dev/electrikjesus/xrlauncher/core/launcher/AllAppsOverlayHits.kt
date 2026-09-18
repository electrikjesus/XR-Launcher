package dev.electrikjesus.xrlauncher.core.launcher

/** Hit-test helpers for the glasses All Apps overlay (pointer bus, not Compose clicks). */
object AllAppsOverlayHits {
    const val CLOSE_BOUNDS_KEY = "__all_apps_close__"
    const val CLOSE_HOVER_LABEL = "__all_apps_close__"

    /**
     * Whether a glasses pointer click should dismiss All Apps.
     *
     * Close always dismisses. Hits on overlay apps or pagination keep it open
     * (those bounds win over the dock All Apps cell behind the sheet). Empty
     * scrim and the dock All Apps control toggle it closed.
     */
    fun shouldDismiss(
        overlayVisible: Boolean,
        hitClose: Boolean,
        hitOtherTarget: Boolean,
    ): Boolean {
        if (!overlayVisible) return false
        if (hitClose) return true
        return !hitOtherTarget
    }
}
