package dev.electrikjesus.xrlauncher.core.display

/**
 * When Expanded host Home Space is already running and glasses appear as a secondary
 * display, ask what to put on that display instead of silently doing nothing.
 */
object HostGlassesAttachLogic {
    enum class Choice {
        /** XR Launcher Home Space on the glasses ([ExternalDisplayActivity]). */
        XR_GLASSES_UI,
        /** Leave glasses on OEM Android Desktop; open companion touchpad on the tablet. */
        ANDROID_DESKTOP,
    }

    /**
     * @param dismissedDisplayIds secondary display IDs the user already declined this session
     * @param externalWorkspaceActive true while [ExternalDisplayActivity] is alive
     */
    fun shouldPrompt(
        hostImmersive: Boolean,
        secondaryDisplayIds: List<Int>,
        dismissedDisplayIds: Set<Int>,
        externalWorkspaceActive: Boolean,
    ): Boolean {
        if (!hostImmersive) return false
        if (externalWorkspaceActive) return false
        if (secondaryDisplayIds.isEmpty()) return false
        return secondaryDisplayIds.any { it !in dismissedDisplayIds }
    }

    /** Drop dismissed IDs that are no longer connected so a replug can ask again. */
    fun pruneDismissed(
        dismissedDisplayIds: Set<Int>,
        secondaryDisplayIds: List<Int>,
    ): Set<Int> {
        if (secondaryDisplayIds.isEmpty()) return emptySet()
        val live = secondaryDisplayIds.toSet()
        return dismissedDisplayIds.filter { it in live }.toSet()
    }
}
