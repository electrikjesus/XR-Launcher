package dev.electrikjesus.xrlauncher.core.input

/**
 * Pure hit-test for whether an accessibility gesture should target a foreign/PIP
 * window instead of the launcher Compose path.
 */
object ForeignWindowInjectLogic {
    data class WindowHit(
        val displayId: Int,
        val type: Int,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val isPictureInPicture: Boolean,
        val packageName: String?,
    ) {
        fun contains(x: Int, y: Int): Boolean =
            x in left until right && y in top until bottom
    }

    /** TYPE_ACCESSIBILITY_OVERLAY — our cursor / return bubble. */
    const val TYPE_ACCESSIBILITY_OVERLAY = 4
    /** TYPE_APPLICATION */
    const val TYPE_APPLICATION = 1

    /**
     * True when [x]/[y] (display pixels) hit a PIP window or another app's
     * application window on [displayId] — not our launcher package.
     */
    fun shouldInjectAt(
        displayId: Int,
        xPx: Int,
        yPx: Int,
        ourPackageName: String,
        windows: List<WindowHit>,
    ): Boolean {
        for (window in windows) {
            if (window.displayId != displayId) continue
            if (window.type == TYPE_ACCESSIBILITY_OVERLAY) continue
            if (!window.contains(xPx, yPx)) continue
            if (window.isPictureInPicture) return true
            if (window.type == TYPE_APPLICATION) {
                val pkg = window.packageName ?: continue
                if (pkg != ourPackageName) return true
            }
        }
        return false
    }
}
