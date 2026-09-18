package dev.electrikjesus.xrlauncher.core.display

import dev.electrikjesus.xrlauncher.core.input.rayneo.RayNeoHeadTrackingController
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsPaginationState
import dev.electrikjesus.xrlauncher.core.launcher.PanelEmbedRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GlassesControlMode {
    /** Fallback when accessibility pointer is unavailable — tap-to-click via hit-testing. */
    LAUNCHER,
    /** Unified pointer: overlay cursor + inject gestures on the glasses display. */
    DESKTOP,
}

/** Maps normalized companion cursor (0..1) to display pixels for the launcher window. */
data class LauncherInjectFrame(
    val offsetXPx: Float = 0f,
    val offsetYPx: Float = 0f,
    val widthPx: Float = 0f,
    val heightPx: Float = 0f,
) {
    fun isValid(): Boolean = widthPx > 0f && heightPx > 0f

    fun toDisplayPixels(normalizedX: Float, normalizedY: Float): Pair<Float, Float> =
        Pair(
            offsetXPx + normalizedX.coerceIn(0f, 1f) * widthPx,
            offsetYPx + normalizedY.coerceIn(0f, 1f) * heightPx,
        )
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

    /**
     * True after [ExternalDisplayActivity] moves to back for a full-window app launch.
     * Unlike [launcherForeground], this is not cleared on activity [onStop] — the phone
     * companion can steal focus while the launcher remains visible on the glasses display.
     */
    var launcherBackgrounded: Boolean = false
        private set

    fun markLauncherForeground() {
        launcherBackgrounded = false
        launcherForeground = true
    }

    fun markLauncherBackgrounded() {
        launcherBackgrounded = true
        launcherForeground = false
    }

    fun clearLauncherSession() {
        launcherBackgrounded = false
        launcherForeground = false
    }

    /** Launcher activity bounds on the glasses display — used to aim inject gestures. */
    var launcherInjectFrame: LauncherInjectFrame = LauncherInjectFrame()

    /** Invoked when Compose reports a stable root size — refreshes [launcherInjectFrame]. */
    var onLauncherRootSized: (() -> Unit)? = null

    /** When true, external display uses Jetpack XR `Subspace` shell (Tier 2 / spatial API). */
    var preferSubspaceShell: Boolean = false

    /** Last Subspace spike decision — for debug logging on EXTERNAL display. */
    var subspaceDecision: SubspaceSpike.Decision = SubspaceSpike.Decision(
        hasSpatialApi = false,
        forcedForSpike = false,
        preferSubspaceShell = false,
    )

    /** Set by [SubspaceSpikeProbe] when the 2D wrapper composes. */
    var subspaceOuterComposed: Boolean = false

    /** Set when content inside [androidx.xr.compose.spatial.Subspace] composes. */
    var subspaceInnerComposed: Boolean = false

    /** Live XR session panel embedder when [PanelEmbedRegistry.fromActivity] succeeds (Tier 3). */
    var panelEmbedRegistry: PanelEmbedRegistry? = null

    private val _allAppsOverlayVisible = MutableStateFlow(false)
    val allAppsOverlayVisibleFlow: StateFlow<Boolean> = _allAppsOverlayVisible.asStateFlow()

    var allAppsOverlayVisible: Boolean
        get() = _allAppsOverlayVisible.value
        set(value) {
            _allAppsOverlayVisible.value = value
        }

    fun showAllAppsOverlay() {
        allAppsOverlayVisible = true
    }

    fun hideAllAppsOverlay() {
        allAppsOverlayVisible = false
    }

    fun toggleAllAppsOverlay() {
        allAppsOverlayVisible = !allAppsOverlayVisible
    }

    private val _layoutPresetsVisible = MutableStateFlow(false)
    val layoutPresetsVisibleFlow: StateFlow<Boolean> = _layoutPresetsVisible.asStateFlow()

    var layoutPresetsVisible: Boolean
        get() = _layoutPresetsVisible.value
        set(value) {
            _layoutPresetsVisible.value = value
        }

    fun toggleLayoutPresets() {
        layoutPresetsVisible = !layoutPresetsVisible
    }

    private val _xrInputMode = MutableStateFlow(GlassesXrInputMode.COMPANION)
    val xrInputModeFlow: StateFlow<GlassesXrInputMode> = _xrInputMode.asStateFlow()

    var xrInputMode: GlassesXrInputMode
        get() = _xrInputMode.value
        set(value) {
            if (_xrInputMode.value != value) {
                _xrInputMode.value = value
            }
        }

    /** Whether RayNeo USB HID IMU was detected the last time we checked. */
    var rayNeoUsbAttached: Boolean = false

    fun clear() {
        secondaryDisplayId = null
        controlMode = GlassesControlMode.LAUNCHER
        clearLauncherSession()
        launcherInjectFrame = LauncherInjectFrame()
        preferSubspaceShell = false
        subspaceDecision = SubspaceSpike.Decision(
            hasSpatialApi = false,
            forcedForSpike = false,
            preferSubspaceShell = false,
        )
        subspaceOuterComposed = false
        subspaceInnerComposed = false
        allAppsOverlayVisible = false
        layoutPresetsVisible = false
        AllAppsPaginationState.reset()
        _xrInputMode.value = GlassesXrInputMode.COMPANION
        rayNeoUsbAttached = false
        RayNeoHeadTrackingController.stop()
    }
}
