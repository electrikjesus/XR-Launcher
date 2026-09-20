package dev.electrikjesus.xrlauncher.core.workspace

import android.graphics.Color

/** Material-style palette and sizing for BumpDesk radial context menus. */
object RadialMenuStyle {
    const val MIN_SWEEP_DEG = 26f
    const val MIN_SUB_SWEEP_DEG = 22f
    const val BASE_ARC_DEG = 160f
    const val MAX_ARC_DEG = 280f

    /** Inner / outer / secondary-offset radii in dp (BumpDesk ScreenMetrics). */
    const val INNER_RADIUS_DP = 56f
    const val OUTER_RADIUS_DP = 152f
    const val SECONDARY_OFFSET_DP = 92f

    fun totalArcForItemCount(count: Int): Float =
        RadialMenuGeometry.totalArcForItemCount(count, MIN_SWEEP_DEG, BASE_ARC_DEG, MAX_ARC_DEG)

    /** Caps growth from item count so dense menus stay usable. */
    fun itemCountScaleFactor(itemCount: Int, sizeScale: Float = 1f): Float {
        val count = itemCount.coerceAtLeast(1)
        val growth = if (count > 4) 1f + (count - 4) * 0.12f else 1f
        val maxGrowth = when {
            sizeScale <= 0.75f -> 1.28f
            sizeScale <= 1.0f -> 1.45f
            else -> 1.6f
        }
        return growth.coerceAtMost(maxGrowth)
    }

    val surfaceFill: Int = Color.argb(240, 33, 35, 43)
    val secondaryFill: Int = Color.argb(235, 45, 48, 58)
    val labelChipFill: Int = Color.argb(225, 55, 58, 68)
    val strokeColor: Int = Color.argb(90, 180, 190, 210)
    /** Highlight when a wedge is hovered / pressed (BumpDesk theme selection stand-in). */
    val selectionFill: Int = Color.argb(230, 70, 130, 230)
}
