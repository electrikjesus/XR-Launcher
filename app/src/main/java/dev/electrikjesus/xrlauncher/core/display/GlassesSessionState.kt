package dev.electrikjesus.xrlauncher.core.display

import dev.electrikjesus.xrlauncher.core.input.rayneo.RayNeoHeadTrackingController
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsPaginationState
import dev.electrikjesus.xrlauncher.core.launcher.PanelEmbedRegistry
import dev.electrikjesus.xrlauncher.core.launcher.PendingGlassesAppLaunch
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceEditPage
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

    private val _homeOverlay = MutableStateFlow(GlassesHomeOverlay.NONE)
    val homeOverlayFlow: StateFlow<GlassesHomeOverlay> = _homeOverlay.asStateFlow()

    var homeOverlay: GlassesHomeOverlay
        get() = _homeOverlay.value
        set(value) {
            _homeOverlay.value = value
            _allAppsOverlayVisible.value = value == GlassesHomeOverlay.ALL_APPS
        }

    fun showHomeOverlay(overlay: GlassesHomeOverlay) {
        homeOverlay = overlay
    }

    fun hideHomeOverlays() {
        homeOverlay = GlassesHomeOverlay.NONE
    }

    fun toggleHomeOverlay(overlay: GlassesHomeOverlay) {
        homeOverlay = if (homeOverlay == overlay) GlassesHomeOverlay.NONE else overlay
    }

    private val _allAppsOverlayVisible = MutableStateFlow(false)
    val allAppsOverlayVisibleFlow: StateFlow<Boolean> = _allAppsOverlayVisible.asStateFlow()

    var allAppsOverlayVisible: Boolean
        get() = _allAppsOverlayVisible.value
        set(value) {
            homeOverlay = if (value) GlassesHomeOverlay.ALL_APPS else GlassesHomeOverlay.NONE
        }

    fun showAllAppsOverlay() {
        AllAppsPaginationState.reset()
        homeOverlay = GlassesHomeOverlay.ALL_APPS
    }

    fun hideAllAppsOverlay() {
        if (homeOverlay == GlassesHomeOverlay.ALL_APPS) {
            homeOverlay = GlassesHomeOverlay.NONE
        }
    }

    fun toggleAllAppsOverlay() {
        toggleHomeOverlay(GlassesHomeOverlay.ALL_APPS)
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

    private val _homeSpaceEdit = MutableStateFlow(false)
    val homeSpaceEditFlow: StateFlow<Boolean> = _homeSpaceEdit.asStateFlow()

    var homeSpaceEdit: Boolean
        get() = _homeSpaceEdit.value
        set(value) {
            _homeSpaceEdit.value = value
            if (!value) {
                homeSpaceEditPage = HomeSpaceEditPage.PERSPECTIVE
            }
        }

    fun toggleHomeSpaceEdit() {
        homeSpaceEdit = !homeSpaceEdit
    }

    private val _homeSpaceEditPage = MutableStateFlow(HomeSpaceEditPage.PERSPECTIVE)
    val homeSpaceEditPageFlow: StateFlow<HomeSpaceEditPage> = _homeSpaceEditPage.asStateFlow()

    var homeSpaceEditPage: HomeSpaceEditPage
        get() = _homeSpaceEditPage.value
        set(value) {
            _homeSpaceEditPage.value = value
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

    /** App launch queued from the phone HOME / companion chooser for the glasses activity. */
    var pendingAppLaunch: PendingGlassesAppLaunch? = null

    fun consumePendingAppLaunch(): PendingGlassesAppLaunch? {
        val pending = pendingAppLaunch
        pendingAppLaunch = null
        return pending
    }

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
        _homeOverlay.value = GlassesHomeOverlay.NONE
        allAppsOverlayVisible = false
        layoutPresetsVisible = false
        homeSpaceEdit = false
        homeSpaceEditPage = HomeSpaceEditPage.PERSPECTIVE
        AllAppsPaginationState.reset()
        GlassesHomeLook.reset()
        pendingAppLaunch = null
        _xrInputMode.value = GlassesXrInputMode.COMPANION
        rayNeoUsbAttached = false
        RayNeoHeadTrackingController.stop()
    }
}
