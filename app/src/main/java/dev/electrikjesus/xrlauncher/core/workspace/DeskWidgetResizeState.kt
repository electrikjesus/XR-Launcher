package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import dev.electrikjesus.xrlauncher.core.workspace.scene.innerSphereRadius
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Radial "Resize": arm a desk widget, show corner handles, drag to change
 * width/height independently (launcher-style). Opposite corner stays fixed.
 */
object DeskWidgetResizeState {
    private const val HANDLE_PREFIX = "desk:resize:"

    enum class Corner(val sx: Float, val sy: Float, val suffix: String) {
        NE(1f, 1f, "ne"),
        NW(-1f, 1f, "nw"),
        SE(1f, -1f, "se"),
        SW(-1f, -1f, "sw"),
    }

    data class Result(
        val yawDeg: Float,
        val pitchDeg: Float,
        val halfWidth: Float,
        val halfHeight: Float,
    )

    private val _widgetKey = MutableStateFlow<String?>(null)
    val widgetKeyFlow: StateFlow<String?> = _widgetKey.asStateFlow()
    val widgetKey: String? get() = _widgetKey.value
    val isArmed: Boolean get() = _widgetKey.value != null
    val isDragging: Boolean get() = activeCorner != null

    private var activeCorner: Corner? = null
    private var startCursorX = 0f
    private var startCursorY = 0f
    private var startYawDeg = 0f
    private var startPitchDeg = 0f
    private var pulling = false
    private var fixedOppYaw = 0f
    private var fixedOppPitch = 0f
    private var iconHalfWidth = HomeSpaceDesk.ICON_HALF_WIDTH
    private var gridScale = DeskGrid.DEFAULT_GRID_SCALE
    private var sphereScale = 1f
    private var snapToGrid = false

    fun handleKey(widgetKey: String, corner: Corner): String =
        "$HANDLE_PREFIX$widgetKey:${corner.suffix}"

    fun parseHandle(componentKey: String): Pair<String, Corner>? {
        if (!componentKey.startsWith(HANDLE_PREFIX)) return null
        val rest = componentKey.removePrefix(HANDLE_PREFIX)
        val sep = rest.lastIndexOf(':')
        if (sep <= 0) return null
        val widget = rest.substring(0, sep)
        val suffix = rest.substring(sep + 1)
        val corner = Corner.entries.firstOrNull { it.suffix == suffix } ?: return null
        return widget to corner
    }

    fun isHandle(componentKey: String): Boolean = parseHandle(componentKey) != null

    fun arm(widgetKey: String) {
        if (widgetKey.isBlank() || !widgetKey.startsWith("widget_")) {
            clear()
            return
        }
        cancelDrag()
        DeskGroupMoveState.clear()
        _widgetKey.value = widgetKey
        DeskIconTextureBus.requestRender()
    }

    fun clear() {
        cancelDrag()
        if (_widgetKey.value != null) {
            _widgetKey.value = null
            DeskIconTextureBus.requestRender()
        }
    }

    fun configure(
        iconHalfWidth: Float,
        gridScale: Float,
        sphereScale: Float,
        snapToGrid: Boolean,
    ) {
        this.iconHalfWidth = iconHalfWidth
        this.gridScale = gridScale
        this.sphereScale = sphereScale
        this.snapToGrid = snapToGrid
    }

    fun beginDrag(
        componentKey: String,
        cursorX: Float,
        cursorY: Float,
        grabYawDeg: Float,
        grabPitchDeg: Float,
        placed: HomeSpaceDesk.Placed,
    ): Boolean {
        val parsed = parseHandle(componentKey) ?: return false
        if (parsed.first != _widgetKey.value) return false
        if (placed.app.componentKey != parsed.first) return false
        val corner = parsed.second
        val halfW = placed.halfWidth ?: (HomeSpaceDesk.ICON_HALF_WIDTH * 3.2f)
        val halfH = placed.halfHeight ?: halfW
        val halfYaw = HomeSpaceDesk.angularHalfYaw(halfW, sphereScale)
        val halfPitch = HomeSpaceDesk.angularHalfPitch(halfH, sphereScale)
        fixedOppYaw = placed.yawDeg - corner.sx * halfYaw
        fixedOppPitch = placed.pitchDeg - corner.sy * halfPitch
        activeCorner = corner
        startCursorX = cursorX
        startCursorY = cursorY
        startYawDeg = grabYawDeg
        startPitchDeg = grabPitchDeg
        pulling = false
        return true
    }

    fun move(
        cursorX: Float,
        cursorY: Float,
        yawDeg: Float,
        pitchDeg: Float,
        cursorSlop: Float = 0.018f,
        angleSlopDeg: Float = 3.5f,
    ): Result? {
        if (activeCorner == null) return null
        if (!pulling) {
            pulling = hypot(cursorX - startCursorX, cursorY - startCursorY) > cursorSlop ||
                hypot(yawDeg - startYawDeg, pitchDeg - startPitchDeg) > angleSlopDeg
            if (!pulling) return null
        }
        return resizeFromOppositeCorner(
            oppositeYawDeg = fixedOppYaw,
            oppositePitchDeg = fixedOppPitch,
            cursorYawDeg = yawDeg,
            cursorPitchDeg = pitchDeg,
            sphereScale = sphereScale,
            iconHalfWidth = iconHalfWidth,
            gridScale = gridScale,
            snapToGrid = snapToGrid,
        )
    }

    /** @return true if a real pull happened (caller should recapture). */
    fun endDrag(): Boolean {
        val wasPulling = pulling
        cancelDrag()
        return wasPulling
    }

    fun cancelDrag() {
        activeCorner = null
        pulling = false
    }

    fun appendHandles(
        icons: List<HomeSpaceDesk.Icon>,
        placed: List<HomeSpaceDesk.Placed>,
        sphereScale: Float,
        handleHalf: Float,
    ): List<HomeSpaceDesk.Icon> {
        val key = _widgetKey.value ?: return icons
        val item = placed.firstOrNull { it.app.componentKey == key } ?: return icons
        val halfW = item.halfWidth ?: (HomeSpaceDesk.ICON_HALF_WIDTH * 3.2f)
        val halfH = item.halfHeight ?: halfW
        val halfYaw = HomeSpaceDesk.angularHalfYaw(halfW, sphereScale)
        val halfPitch = HomeSpaceDesk.angularHalfPitch(halfH, sphereScale)
        val handles = Corner.entries.map { corner ->
            HomeSpaceDesk.iconOf(
                app = HomeSpaceDesk.AppRef(
                    componentKey = handleKey(key, corner),
                    label = " ",
                    packageName = "",
                    kind = HomeSpaceDesk.Kind.RESIZE_HANDLE,
                ),
                yawDeg = item.yawDeg + corner.sx * halfYaw,
                pitchDeg = item.pitchDeg + corner.sy * halfPitch,
                sphereScale = sphereScale,
                halfWidth = handleHalf,
                halfHeight = handleHalf,
                lift = HomeSpaceDesk.HOVER_LIFT * 1.5f,
            )
        }
        return icons + handles
    }

    fun resizeFromOppositeCorner(
        oppositeYawDeg: Float,
        oppositePitchDeg: Float,
        cursorYawDeg: Float,
        cursorPitchDeg: Float,
        sphereScale: Float,
        iconHalfWidth: Float,
        gridScale: Float,
        snapToGrid: Boolean,
    ): Result {
        val sx = if (cursorYawDeg >= oppositeYawDeg) 1f else -1f
        val sy = if (cursorPitchDeg >= oppositePitchDeg) 1f else -1f
        var halfYaw = abs(cursorYawDeg - oppositeYawDeg) * 0.5f
        var halfPitch = abs(cursorPitchDeg - oppositePitchDeg) * 0.5f
        val radius = HomeSpaceScene.innerSphereRadius(sphereScale).coerceAtLeast(0.01f)
        var halfW = Math.toRadians(halfYaw.toDouble()).toFloat() * radius
        var halfH = Math.toRadians(halfPitch.toDouble()).toFloat() * radius
        val clamped = DeskWidgetUtils.clampHalfExtents(halfW, halfH)
        halfW = clamped.first
        halfH = clamped.second
        if (snapToGrid) {
            val snapped = DeskGrid.snapHalfExtents(halfW, halfH, iconHalfWidth, gridScale)
            halfW = snapped.first
            halfH = snapped.second
        }
        halfYaw = HomeSpaceDesk.angularHalfYaw(halfW, sphereScale)
        halfPitch = HomeSpaceDesk.angularHalfPitch(halfH, sphereScale)
        return Result(
            yawDeg = oppositeYawDeg + sx * halfYaw,
            pitchDeg = (oppositePitchDeg + sy * halfPitch).coerceIn(-55f, 55f),
            halfWidth = halfW,
            halfHeight = halfH,
        )
    }
}
