package dev.electrikjesus.xrlauncher.core.workspace

import kotlin.math.cos
import kotlin.math.sin

/**
 * Screen-space ring for the desk context menu. Slot 0 sits above the anchor;
 * further slots walk clockwise. Y is screen-down, so the first offset is negative.
 */
object RadialMenuGeometry {
    fun slotAngleDegrees(index: Int, count: Int): Float {
        if (count <= 0) return -90f
        return -90f + index * (360f / count)
    }

    fun slotOffsetPx(index: Int, count: Int, radiusPx: Float): Pair<Float, Float> {
        if (count <= 0 || radiusPx == 0f) return 0f to 0f
        val rad = Math.toRadians(slotAngleDegrees(index, count).toDouble())
        return (cos(rad) * radiusPx).toFloat() to (sin(rad) * radiusPx).toFloat()
    }
}
