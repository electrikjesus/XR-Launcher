package dev.electrikjesus.xrlauncher.core.display

enum class GlassesControlMode {
    /** Clicks hit-test the XR Launcher app list on the glasses workspace. */
    LAUNCHER,
    /** Clicks are injected on the glasses display (requires accessibility service). */
    DESKTOP,
}

object GlassesSessionState {
    var secondaryDisplayId: Int? = null
    var controlMode: GlassesControlMode = GlassesControlMode.LAUNCHER

    fun clear() {
        secondaryDisplayId = null
        controlMode = GlassesControlMode.LAUNCHER
    }
}
