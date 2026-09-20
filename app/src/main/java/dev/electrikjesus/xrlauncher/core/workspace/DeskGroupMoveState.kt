package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot

/**
 * BumpDesk lasso "Move": arm a multi-item selection, show a centroid handle, then drag
 * any armed member (or the handle) so relative yaw/pitch offsets are preserved.
 */
object DeskGroupMoveState {
    const val HANDLE_KEY = "desk:group_move_handle"
    const val HANDLE_LABEL = "Move"

    private val _armedKeys = MutableStateFlow<Set<String>?>(null)
    val armedKeysFlow: StateFlow<Set<String>?> = _armedKeys.asStateFlow()
    val armedKeys: Set<String>? get() = _armedKeys.value

    private val _dragging = MutableStateFlow(false)
    val draggingFlow: StateFlow<Boolean> = _dragging.asStateFlow()
    val isDragging: Boolean get() = _dragging.value

    private var offsets: Map<String, Pair<Float, Float>>? = null
    private var startCursorX: Float = 0f
    private var startCursorY: Float = 0f
    private var startYawDeg: Float = 0f
    private var startPitchDeg: Float = 0f
    private var pulling: Boolean = false

    val isPulling: Boolean get() = pulling

    fun arm(keys: Set<String>) {
        val usable = keys.filter { it.isNotBlank() && it != HANDLE_KEY }.toSet()
        if (usable.size < 2) {
            clear()
            return
        }
        cancelDrag()
        _armedKeys.value = usable
        DeskIconTextureBus.requestRender()
    }

    fun clear() {
        cancelDrag()
        if (_armedKeys.value != null) {
            _armedKeys.value = null
            DeskIconTextureBus.requestRender()
        }
    }

    fun isArmedMember(componentKey: String): Boolean =
        _armedKeys.value?.contains(componentKey) == true

    fun isHandle(componentKey: String): Boolean = componentKey == HANDLE_KEY

    fun canBeginDrag(componentKey: String): Boolean =
        isHandle(componentKey) || isArmedMember(componentKey)

    fun centroid(placed: List<HomeSpaceDesk.Placed>): Pair<Float, Float>? {
        val keys = _armedKeys.value ?: return null
        val items = placed.filter { it.app.componentKey in keys }
        if (items.isEmpty()) return null
        return items.map { it.yawDeg }.average().toFloat() to
            items.map { it.pitchDeg }.average().toFloat()
    }

    /**
     * Start a rigid group drag anchored at the sphere hit under the cursor.
     * Offsets are relative yaw/pitch from that hit so arrangement is preserved.
     */
    fun beginDrag(
        componentKey: String,
        cursorX: Float,
        cursorY: Float,
        grabYawDeg: Float,
        grabPitchDeg: Float,
        placed: List<HomeSpaceDesk.Placed>,
    ): Boolean {
        if (!canBeginDrag(componentKey)) return false
        val keys = _armedKeys.value ?: return false
        val items = placed.filter { it.app.componentKey in keys }
        if (items.size < 2) return false
        offsets = items.associate { item ->
            item.app.componentKey to
                (item.yawDeg - grabYawDeg to item.pitchDeg - grabPitchDeg)
        }
        startCursorX = cursorX
        startCursorY = cursorY
        startYawDeg = grabYawDeg
        startPitchDeg = grabPitchDeg
        pulling = false
        _dragging.value = true
        return true
    }

    /**
     * @return updated yaw/pitch for each armed key once the drag has pulled past slop,
     * or null while still within slop / inactive.
     */
    fun move(
        cursorX: Float,
        cursorY: Float,
        yawDeg: Float,
        pitchDeg: Float,
        cursorSlop: Float = 0.018f,
        angleSlopDeg: Float = 3.5f,
    ): Map<String, Pair<Float, Float>>? {
        val offs = offsets ?: return null
        if (!pulling) {
            pulling = hypot(cursorX - startCursorX, cursorY - startCursorY) > cursorSlop ||
                hypot(yawDeg - startYawDeg, pitchDeg - startPitchDeg) > angleSlopDeg
            if (!pulling) return null
        }
        return offs.mapValues { (_, offset) ->
            (yawDeg + offset.first) to (pitchDeg + offset.second)
        }
    }

    /** Ends an active drag. Clears the armed group after a real pull (BumpDesk). */
    fun endDrag(): Boolean {
        val wasPulling = pulling
        cancelDrag()
        if (wasPulling) {
            _armedKeys.value = null
            DeskIconTextureBus.requestRender()
        }
        return wasPulling
    }

    fun cancelDrag() {
        offsets = null
        pulling = false
        if (_dragging.value) _dragging.value = false
    }

    /** Inject the grab handle at the selection centroid while armed. */
    fun appendHandle(
        icons: List<HomeSpaceDesk.Icon>,
        placed: List<HomeSpaceDesk.Placed>,
        sphereScale: Float,
        halfWidth: Float,
    ): List<HomeSpaceDesk.Icon> {
        val (yaw, pitch) = centroid(placed) ?: return icons
        val handle = HomeSpaceDesk.iconOf(
            app = HomeSpaceDesk.AppRef(
                componentKey = HANDLE_KEY,
                label = HANDLE_LABEL,
                packageName = "",
                kind = HomeSpaceDesk.Kind.GROUP_HANDLE,
            ),
            yawDeg = yaw,
            pitchDeg = pitch,
            sphereScale = sphereScale,
            halfWidth = halfWidth * 0.85f,
            halfHeight = halfWidth * 0.85f,
            lift = HomeSpaceDesk.HOVER_LIFT * 1.4f,
        )
        return icons + handle
    }
}
