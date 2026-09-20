package dev.electrikjesus.xrlauncher.core.display

/**
 * When Expanded host Home Space is already running and glasses appear (as a secondary
 * display and/or RayNeo USB), ask what to put on that display instead of silently
 * doing nothing.
 */
object HostGlassesAttachLogic {
    /** Sentinel dismiss id when RayNeo USB is attached but Android exposes no secondary display. */
    const val USB_ONLY_DISMISS_ID = -1

    enum class Choice {
        /** XR Launcher Home Space on the glasses ([ExternalDisplayActivity]). */
        XR_GLASSES_UI,
        /** Leave glasses on OEM Android Desktop; open companion touchpad on the tablet. */
        ANDROID_DESKTOP,
    }

    /**
     * @param dismissedDisplayIds secondary display IDs (or [USB_ONLY_DISMISS_ID]) already handled
     * @param externalWorkspaceActive true while [ExternalDisplayActivity] is alive
     * @param rayNeoUsbAttached HID/IMU present even when DisplayManager has no EXTERNAL display
     */
    fun shouldPrompt(
        hostImmersive: Boolean,
        secondaryDisplayIds: List<Int>,
        dismissedDisplayIds: Set<Int>,
        externalWorkspaceActive: Boolean,
        rayNeoUsbAttached: Boolean = false,
    ): Boolean {
        if (!hostImmersive) return false
        if (externalWorkspaceActive) return false
        if (secondaryDisplayIds.isNotEmpty()) {
            return secondaryDisplayIds.any { it !in dismissedDisplayIds }
        }
        // Tablet + RayNeo often only attach USB (IMU) — no DisplayManager secondary until
        // Desktop Mode / DP alt-mode actually presents a display.
        return rayNeoUsbAttached && USB_ONLY_DISMISS_ID !in dismissedDisplayIds
    }

    /** True when [DisplayLaunchHelper] can target a live secondary display. */
    fun xrGlassesUiAvailable(secondaryDisplayIds: List<Int>): Boolean =
        secondaryDisplayIds.isNotEmpty()

    /** Ids to remember after the user dismisses or picks Android Desktop. */
    fun dismissIds(
        secondaryDisplayIds: List<Int>,
        rayNeoUsbAttached: Boolean,
    ): Set<Int> = buildSet {
        addAll(secondaryDisplayIds)
        if (rayNeoUsbAttached && secondaryDisplayIds.isEmpty()) {
            add(USB_ONLY_DISMISS_ID)
        }
    }

    /** Drop dismissed IDs that are no longer connected so a replug can ask again. */
    fun pruneDismissed(
        dismissedDisplayIds: Set<Int>,
        secondaryDisplayIds: List<Int>,
        rayNeoUsbAttached: Boolean = false,
    ): Set<Int> {
        if (secondaryDisplayIds.isEmpty() && !rayNeoUsbAttached) return emptySet()
        val live = buildSet {
            addAll(secondaryDisplayIds)
            if (rayNeoUsbAttached && secondaryDisplayIds.isEmpty()) {
                add(USB_ONLY_DISMISS_ID)
            }
        }
        return dismissedDisplayIds.filter { it in live }.toSet()
    }
}
