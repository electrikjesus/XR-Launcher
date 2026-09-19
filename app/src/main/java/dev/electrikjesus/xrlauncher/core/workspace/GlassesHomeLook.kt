package dev.electrikjesus.xrlauncher.core.workspace

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpacePaneSlot
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import kotlin.math.abs

/** An app hosted as a spatial plane in the Home Space carousel. */
data class GlassesAppPlane(
    val panelId: String,
    val componentKey: String,
    val label: String,
)

/**
 * Horizontal look across Home Space: Desktop · Home · app planes · Notifications/QS.
 *
 * Free-look yaw is stored in **degrees** ([lookYawDegrees]) so mouse-look can spin a full
 * circle. [panNorm] stays as pane-space units for lookingAt* helpers (yaw / pane arc).
 */
object GlassesHomeLook {
    const val PANE_LEFT = -1f
    const val PANE_HOME = 0f
    const val PANE_RIGHT = 1f

    /**
     * Soft UI hint past Desktop / Tray. Free look is not clamped to this.
     */
    const val SIDE_LOOK_EXTRA = 1.15f

    /** @deprecated Use [SIDE_LOOK_EXTRA]. */
    const val DESKTOP_LOOK_EXTRA = SIDE_LOOK_EXTRA

    const val EDGE_START = 0.12f
    const val PAN_SPEED = 0.85f

    /** Fraction of the viewport between pane centers so neighbors stay in view. */
    const val PANE_SPACING = 0.56f
    const val PANE_TILT_DEGREES = 48f
    const val PANE_MAX_TILT = 58f
    const val CONTENT_WIDTH_FRACTION = 0.66f
    const val CONTENT_HEIGHT_FRACTION = 0.84f
    const val CAMERA_DISTANCE_FACTOR = 0.88f

    /** Pitch stop for free look (± almost straight up/down). */
    const val FREE_LOOK_MAX_PITCH_DEGREES = 89f

    fun paneDelta(worldX: Float, look: Float = panNorm): Float = worldX - look

    fun paneRotationY(delta: Float): Float =
        (delta * PANE_TILT_DEGREES).coerceIn(-PANE_MAX_TILT, PANE_MAX_TILT)

    fun paneScale(delta: Float): Float =
        (1f - abs(delta).coerceAtMost(1.15f) * 0.18f).coerceAtLeast(0.72f)

    fun paneAlpha(delta: Float): Float {
        val distance = abs(delta)
        return when {
            distance >= 1.45f -> 0f
            distance <= 0.85f -> 1f
            else -> ((1.45f - distance) / 0.6f).coerceIn(0f, 1f)
        }
    }

    fun paneVisible(delta: Float): Boolean = abs(delta) < 1.5f

    private val _lookPitch = MutableStateFlow(0f)
    val lookPitchFlow: StateFlow<Float> = _lookPitch.asStateFlow()

    var lookPitch: Float
        get() = _lookPitch.value
        set(value) {
            _lookPitch.value = value.coerceIn(
                -FREE_LOOK_MAX_PITCH_DEGREES,
                FREE_LOOK_MAX_PITCH_DEGREES,
            )
        }

    private val _lookYawDeg = MutableStateFlow(0f)
    val lookYawDegFlow: StateFlow<Float> = _lookYawDeg.asStateFlow()

    /** Absolute yaw in degrees — free to spin indefinitely (FPS / host mouse-look). */
    var lookYawDegrees: Float
        get() = _lookYawDeg.value
        set(value) {
            _lookYawDeg.value = value
            syncPanNormFromYaw()
        }

    /**
     * Pane arc used to convert between [lookYawDegrees] and [panNorm].
     * Updated each frame from the live viewport / appearance.
     */
    var lastPaneArcDegrees: Float = 72f
        set(value) {
            field = value.coerceAtLeast(1f)
        }

    private val _panNorm = MutableStateFlow(0f)
    val panNormFlow: StateFlow<Float> = _panNorm.asStateFlow()

    private val _appPlanes = MutableStateFlow<List<GlassesAppPlane>>(emptyList())
    val appPlanesFlow: StateFlow<List<GlassesAppPlane>> = _appPlanes.asStateFlow()

    val appPlanes: List<GlassesAppPlane>
        get() = _appPlanes.value

    /** Soft left bound used by UI hints — look itself is not clamped here. */
    fun minPan(): Float = PANE_LEFT - SIDE_LOOK_EXTRA

    /** Soft right bound used by UI hints — look itself is not clamped here. */
    fun maxPan(): Float = trayPane() + SIDE_LOOK_EXTRA

    fun trayPane(): Float = PANE_RIGHT + appPlanes.size

    fun appPane(index: Int): Float = PANE_RIGHT + index

    fun homeSpaceSlots(appPlanes: List<GlassesAppPlane> = this.appPlanes): List<HomeSpacePaneSlot> {
        val slots = mutableListOf(
            HomeSpacePaneSlot("home", PANE_HOME),
        )
        appPlanes.forEachIndexed { index, plane ->
            slots += HomeSpacePaneSlot(plane.panelId, appPane(index))
        }
        slots += HomeSpacePaneSlot("tray", trayPane())
        return slots
    }

    var panNorm: Float
        get() = _panNorm.value
        set(value) {
            _panNorm.value = value
            _lookYawDeg.value = value * lastPaneArcDegrees
        }

    private fun syncPanNormFromYaw() {
        _panNorm.value = lookYawDegrees / lastPaneArcDegrees
    }

    /** Mouse / touch look deltas in degrees (full-circle yaw). */
    fun addLookDegrees(yawDeltaDeg: Float, pitchDeltaDeg: Float) {
        _lookYawDeg.value += yawDeltaDeg
        lookPitch += pitchDeltaDeg
        syncPanNormFromYaw()
    }

    fun lookAt(pane: Float) {
        panNorm = pane
    }

    /** Desktop / All Apps wall, including empty space left of the drawer. */
    fun lookingAtDesktop(): Boolean = panNorm <= PANE_LEFT + 0.28f

    fun lookingAtAllApps(): Boolean = lookingAtDesktop()

    fun lookingAtHome(): Boolean = abs(panNorm - PANE_HOME) <= 0.45f

    /** Tray / notifications wall, including empty space to the right of the tray. */
    fun lookingAtTray(): Boolean = panNorm >= trayPane() - 0.45f

    fun focusedAppPlane(): GlassesAppPlane? {
        appPlanes.forEachIndexed { index, plane ->
            if (abs(panNorm - appPane(index)) <= 0.45f) return plane
        }
        return null
    }

    /**
     * Open [plane] on the current focus. The previous plane stays in the world to the left.
     * Re-opening the same app looks at its existing plane.
     */
    fun openAppPlane(plane: GlassesAppPlane) {
        val existing = appPlanes.indexOfFirst { it.componentKey == plane.componentKey }
        if (existing >= 0) {
            lookAt(appPane(existing))
            return
        }
        _appPlanes.value = appPlanes + plane
        lookAt(appPane(appPlanes.lastIndex))
    }

    fun closeAppPlane(panelId: String) {
        val index = appPlanes.indexOfFirst { it.panelId == panelId }
        if (index < 0) return
        val remaining = appPlanes.filter { it.panelId != panelId }
        _appPlanes.value = remaining
        val target = when {
            remaining.isEmpty() -> PANE_HOME
            index == 0 -> PANE_HOME
            else -> appPane((index - 1).coerceAtMost(remaining.lastIndex))
        }
        lookAt(target)
    }

    /**
     * Game-style edge look: cursor parked on the left/right of the viewport
     * pans toward the neighboring pane. Stronger the closer to the bezel.
     */
    fun tickEdgePan(cursorX: Float, deltaSeconds: Float) {
        if (GlassesLookMode.effective() == GlassesLookMode.FPS) return
        val dt = deltaSeconds.coerceIn(0f, 0.05f)
        if (dt <= 0f) return
        val x = cursorX.coerceIn(0f, 1f)
        val delta = when {
            x < EDGE_START -> {
                val t = 1f - (x / EDGE_START)
                -PAN_SPEED * dt * t
            }
            x > 1f - EDGE_START -> {
                val t = (x - (1f - EDGE_START)) / EDGE_START
                PAN_SPEED * dt * t
            }
            else -> 0f
        }
        if (delta != 0f) {
            panNorm += delta
        }
    }

    fun addFpsLook(deltaX: Float, deltaY: Float) {
        addLookDegrees(
            yawDeltaDeg = HomeSpaceScene.fpsYawDegreesDelta(deltaX),
            pitchDeltaDeg = HomeSpaceScene.fpsPitchDelta(deltaY),
        )
    }

    /** Recenter on Home without closing spatial app planes. */
    fun lookHome() {
        panNorm = PANE_HOME
        lookPitch = 0f
    }

    fun reset() {
        _appPlanes.value = emptyList()
        panNorm = PANE_HOME
        lookPitch = 0f
    }
}
