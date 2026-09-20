package dev.electrikjesus.xrlauncher.core.input

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Classifies companion touchpad multi-finger motion:
 * - 2 fingers: lock to [SCROLL] or [ZOOM] after the first decisive move (Scheme A mutex)
 * - 3 fingers: [LOOK] pan (camera) without moving the cursor
 */
class CompanionTouchpadMultiTouch(
    var touchSlopPx: Float = DEFAULT_TOUCH_SLOP_PX,
    var pinchZoomThresholdPx: Float = DEFAULT_PINCH_ZOOM_THRESHOLD_PX,
) {
    enum class Lock {
        NONE,
        SCROLL,
        ZOOM,
        LOOK,
    }

    sealed class Action {
        data class Scroll(val dxPx: Float, val dyPx: Float) : Action()
        data class Zoom(val previousDistance: Float, val currentDistance: Float) : Action()
        data class LookPan(val dxPx: Float, val dyPx: Float) : Action()
        data object RightClick : Action()
    }

    var lock: Lock = Lock.NONE
        private set
    var active: Boolean = false
        private set

    private var fingerCount: Int = 0
    private var beginDistance: Float = 0f
    private var beginMidX: Float = 0f
    private var beginMidY: Float = 0f
    private var lastDistance: Float = 0f
    private var lastMidX: Float = 0f
    private var lastMidY: Float = 0f
    private var traveled: Boolean = false

    fun reset() {
        lock = Lock.NONE
        active = false
        fingerCount = 0
        beginDistance = 0f
        beginMidX = 0f
        beginMidY = 0f
        lastDistance = 0f
        lastMidX = 0f
        lastMidY = 0f
        traveled = false
    }

    fun begin(fingerCount: Int, midX: Float, midY: Float, distance: Float) {
        reset()
        active = true
        this.fingerCount = fingerCount.coerceAtLeast(2)
        beginMidX = midX
        beginMidY = midY
        beginDistance = distance
        lastMidX = midX
        lastMidY = midY
        lastDistance = distance
        lock = if (this.fingerCount >= 3) Lock.LOOK else Lock.NONE
    }

    /** Update while fingers are down. Returns actions for this sample. */
    fun move(fingerCount: Int, midX: Float, midY: Float, distance: Float): List<Action> {
        if (!active) return emptyList()
        this.fingerCount = fingerCount.coerceAtLeast(2)
        if (this.fingerCount >= 3 && lock != Lock.LOOK) {
            lock = Lock.LOOK
        }

        val midDx = midX - lastMidX
        val midDy = midY - lastMidY
        val prevDist = lastDistance
        lastMidX = midX
        lastMidY = midY
        lastDistance = distance

        if (hypot(midX - beginMidX, midY - beginMidY) > touchSlopPx ||
            abs(distance - beginDistance) > pinchZoomThresholdPx
        ) {
            traveled = true
        }

        if (lock == Lock.LOOK || this.fingerCount >= 3) {
            lock = Lock.LOOK
            return if (midDx != 0f || midDy != 0f) {
                listOf(Action.LookPan(midDx, midDy))
            } else {
                emptyList()
            }
        }

        if (lock == Lock.NONE) {
            val spanDelta = abs(distance - beginDistance)
            val midTravel = hypot(midX - beginMidX, midY - beginMidY)
            when {
                spanDelta >= pinchZoomThresholdPx -> lock = Lock.ZOOM
                midTravel >= touchSlopPx -> lock = Lock.SCROLL
            }
        }

        return when (lock) {
            Lock.SCROLL -> {
                if (midDx != 0f || midDy != 0f) listOf(Action.Scroll(midDx, midDy)) else emptyList()
            }
            Lock.ZOOM -> {
                if (prevDist > 1f && distance > 1f) {
                    listOf(Action.Zoom(prevDist, distance))
                } else {
                    emptyList()
                }
            }
            Lock.LOOK -> {
                if (midDx != 0f || midDy != 0f) listOf(Action.LookPan(midDx, midDy)) else emptyList()
            }
            Lock.NONE -> emptyList()
        }
    }

    /** Finger-up. Emits [Action.RightClick] for a short 2-finger tap. */
    fun end(): Action? {
        if (!active) return null
        val wasTwoFingerTap = fingerCount == 2 && lock == Lock.NONE && !traveled
        reset()
        return if (wasTwoFingerTap) Action.RightClick else null
    }

    companion object {
        const val DEFAULT_TOUCH_SLOP_PX = 24f
        const val DEFAULT_PINCH_ZOOM_THRESHOLD_PX = 36f

        fun midpoint(points: List<Pair<Float, Float>>): Pair<Float, Float> {
            if (points.isEmpty()) return 0f to 0f
            var sx = 0f
            var sy = 0f
            for ((x, y) in points) {
                sx += x
                sy += y
            }
            val n = points.size.toFloat()
            return (sx / n) to (sy / n)
        }

        fun span(points: List<Pair<Float, Float>>): Float {
            if (points.size < 2) return 0f
            val (ax, ay) = points[0]
            val (bx, by) = points[1]
            return hypot(bx - ax, by - ay)
        }
    }
}
