package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot

/**
 * BumpDesk-style lasso on the Home Space sphere (yaw/pitch instead of floor XZ).
 * Hold-Left on empty Desktop and drag; release selects icons inside the polygon.
 */
object DeskLassoState {
    data class Point(val yawDeg: Float, val pitchDeg: Float)

    private const val MIN_POINTS = 3
    private const val MIN_SPAN_DEG = 4f

    private val _points = MutableStateFlow<List<Point>>(emptyList())
    val pointsFlow: StateFlow<List<Point>> = _points.asStateFlow()
    val points: List<Point> get() = _points.value

    private val _selectedKeys = MutableStateFlow<Set<String>>(emptySet())
    val selectedKeysFlow: StateFlow<Set<String>> = _selectedKeys.asStateFlow()
    val selectedKeys: Set<String> get() = _selectedKeys.value

    val active: Boolean get() = _points.value.isNotEmpty()

    fun begin(yawDeg: Float, pitchDeg: Float) {
        _points.value = listOf(Point(yawDeg, pitchDeg))
    }

    fun extend(yawDeg: Float, pitchDeg: Float) {
        val current = _points.value
        if (current.isEmpty()) {
            begin(yawDeg, pitchDeg)
            return
        }
        val last = current.last()
        if (hypot(yawDeg - last.yawDeg, pitchDeg - last.pitchDeg) < 0.35f) return
        _points.value = current + Point(yawDeg, pitchDeg)
    }

    /**
     * Call from companion pointer-up **before** clearing `isPressed`.
     * @return true when the companion must not emit a click (lasso stroke is real).
     * Selection is applied later via [completePending] once icon poses are available.
     */
    fun notePointerUp(): Boolean {
        val poly = _points.value
        if (poly.isEmpty()) return false
        if (poly.size < MIN_POINTS || !spansEnough(poly)) {
            cancel()
            return false
        }
        pendingFinish = true
        return true
    }

    fun completePending(icons: List<HomeSpaceDesk.Icon>) {
        if (!pendingFinish) return
        pendingFinish = false
        val poly = _points.value
        _points.value = emptyList()
        if (poly.size < MIN_POINTS || !spansEnough(poly)) return
        _selectedKeys.value = capture(poly, icons)
    }

    /**
     * Finalize the lasso immediately (tests / sync paths).
     * Returns true when the gesture should consume the pointer-up click.
     */
    fun finish(icons: List<HomeSpaceDesk.Icon>): Boolean {
        pendingFinish = false
        val poly = _points.value
        _points.value = emptyList()
        if (poly.size < MIN_POINTS || !spansEnough(poly)) {
            return false
        }
        val captured = capture(poly, icons)
        _selectedKeys.value = captured
        return captured.isNotEmpty() || poly.size >= MIN_POINTS
    }

    fun cancel() {
        pendingFinish = false
        _points.value = emptyList()
    }

    fun clearSelection() {
        _selectedKeys.value = emptySet()
    }

    fun reset() {
        cancel()
        clearSelection()
    }

    private var pendingFinish = false

    /** Ray-yaw/pitch point-in-polygon (even-odd), adapted from BumpDesk floor lasso. */
    fun isInside(yawDeg: Float, pitchDeg: Float, poly: List<Point>): Boolean {
        if (poly.size < MIN_POINTS) return false
        var inside = false
        var j = poly.lastIndex
        for (i in poly.indices) {
            val yi = poly[i].pitchDeg
            val yj = poly[j].pitchDeg
            val xi = poly[i].yawDeg
            val xj = poly[j].yawDeg
            if ((yi > pitchDeg) != (yj > pitchDeg)) {
                val xIntersect = (xj - xi) * (pitchDeg - yi) / (yj - yi) + xi
                if (yawDeg < xIntersect) inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun capture(poly: List<Point>, icons: List<HomeSpaceDesk.Icon>): Set<String> =
        icons
            .filter { it.isDesktopApp && !it.isAppDrawer && !it.isBacking && !it.isPager }
            .filter { isInside(it.yawDeg, it.pitchDeg, poly) }
            .map { it.componentKey }
            .toSet()

    private fun spansEnough(poly: List<Point>): Boolean {
        val minYaw = poly.minOf { it.yawDeg }
        val maxYaw = poly.maxOf { it.yawDeg }
        val minPitch = poly.minOf { it.pitchDeg }
        val maxPitch = poly.maxOf { it.pitchDeg }
        return hypot(maxYaw - minYaw, maxPitch - minPitch) >= MIN_SPAN_DEG
    }
}
