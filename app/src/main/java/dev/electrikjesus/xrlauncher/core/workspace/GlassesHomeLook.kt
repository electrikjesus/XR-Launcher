package dev.electrikjesus.xrlauncher.core.workspace

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/** An app hosted as a spatial plane in the Home Space carousel. */
data class GlassesAppPlane(
    val panelId: String,
    val componentKey: String,
    val label: String,
)

/**
 * Horizontal look across Home Space: All Apps · Home · app planes · Notifications/QS.
 *
 * World X: All Apps = -1, Home = 0, first app = +1, tray = 1 + appCount.
 * Opening an app focuses a new plane at the current look and shifts the previous
 * plane to the left.
 */
object GlassesHomeLook {
    const val PANE_LEFT = -1f
    const val PANE_HOME = 0f
    const val PANE_RIGHT = 1f

    const val EDGE_START = 0.12f
    const val PAN_SPEED = 1.35f

    private val _panNorm = MutableStateFlow(0f)
    val panNormFlow: StateFlow<Float> = _panNorm.asStateFlow()

    private val _appPlanes = MutableStateFlow<List<GlassesAppPlane>>(emptyList())
    val appPlanesFlow: StateFlow<List<GlassesAppPlane>> = _appPlanes.asStateFlow()

    val appPlanes: List<GlassesAppPlane>
        get() = _appPlanes.value

    fun minPan(): Float = PANE_LEFT

    fun maxPan(): Float = PANE_RIGHT + appPlanes.size

    fun trayPane(): Float = maxPan()

    fun appPane(index: Int): Float = PANE_RIGHT + index

    var panNorm: Float
        get() = _panNorm.value
        set(value) {
            _panNorm.value = value.coerceIn(minPan(), maxPan())
        }

    fun lookAt(pane: Float) {
        panNorm = pane
    }

    fun lookingAtAllApps(): Boolean = panNorm <= PANE_LEFT + 0.55f

    fun lookingAtHome(): Boolean = abs(panNorm - PANE_HOME) <= 0.45f

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

    /** Recenter on Home without closing spatial app planes. */
    fun lookHome() {
        panNorm = PANE_HOME
    }

    fun reset() {
        _appPlanes.value = emptyList()
        panNorm = PANE_HOME
    }
}
