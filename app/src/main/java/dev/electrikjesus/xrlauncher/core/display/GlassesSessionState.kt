package dev.electrikjesus.xrlauncher.core.display

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GlassesControlMode {
    /** Fallback when accessibility pointer is unavailable — tap-to-click via hit-testing. */
    LAUNCHER,
    /** Unified pointer: overlay cursor + inject gestures; launcher uses hit-testing when foreground. */
    DESKTOP,
}

object GlassesSessionState {
    var secondaryDisplayId: Int? = null
    var controlMode: GlassesControlMode = GlassesControlMode.LAUNCHER

    private val _launcherForeground = MutableStateFlow(false)
    val launcherForegroundFlow: StateFlow<Boolean> = _launcherForeground.asStateFlow()

    var launcherForeground: Boolean
        get() = _launcherForeground.value
        set(value) {
            _launcherForeground.value = value
        }

    /** When true, external display uses Jetpack XR `Subspace` shell (Tier 2 / spatial API). */
    var preferSubspaceShell: Boolean = false

    fun clear() {
        secondaryDisplayId = null
        controlMode = GlassesControlMode.LAUNCHER
        launcherForeground = false
        preferSubspaceShell = false
    }
}
