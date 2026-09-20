package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Angular desk grid for snap / overlay / widget cell sizing.
 * One cell ≈ desk-icon world half-width × [gridScale] on each axis.
 */
object DeskGrid {
    const val MIN_GRID_SCALE = 0.75f
    const val MAX_GRID_SCALE = 1.5f
    const val DEFAULT_GRID_SCALE = 1f

    /** World half-extent of one grid cell (square). */
    fun cellHalfExtent(iconHalfWidth: Float, gridScale: Float): Float =
        (iconHalfWidth * gridScale.coerceIn(MIN_GRID_SCALE, MAX_GRID_SCALE))
            .coerceAtLeast(0.04f)

    fun cellYawDeg(iconHalfWidth: Float, gridScale: Float, sphereScale: Float): Float =
        HomeSpaceDesk.angularHalfYaw(cellHalfExtent(iconHalfWidth, gridScale), sphereScale)
            .coerceAtLeast(0.5f) * 2f

    fun cellPitchDeg(iconHalfWidth: Float, gridScale: Float, sphereScale: Float): Float =
        HomeSpaceDesk.angularHalfPitch(cellHalfExtent(iconHalfWidth, gridScale), sphereScale)
            .coerceAtLeast(0.5f) * 2f

    fun snapPose(
        yawDeg: Float,
        pitchDeg: Float,
        iconHalfWidth: Float,
        gridScale: Float,
        sphereScale: Float,
    ): Pair<Float, Float> {
        val stepYaw = cellYawDeg(iconHalfWidth, gridScale, sphereScale)
        val stepPitch = cellPitchDeg(iconHalfWidth, gridScale, sphereScale)
        return snapToStep(yawDeg, stepYaw) to snapToStep(pitchDeg, stepPitch)
            .coerceIn(-55f, 55f)
    }

    /**
     * Snapped center plus angular half-extents for the item footprint
     * (1×1 for icons; multi-cell for widgets).
     */
    data class SnapTarget(
        val yawDeg: Float,
        val pitchDeg: Float,
        val halfYawDeg: Float,
        val halfPitchDeg: Float,
        val cellsW: Int,
        val cellsH: Int,
    )

    fun snapTarget(
        yawDeg: Float,
        pitchDeg: Float,
        halfWidth: Float,
        halfHeight: Float,
        iconHalfWidth: Float,
        gridScale: Float,
        sphereScale: Float,
    ): SnapTarget {
        val (yaw, pitch) = snapPose(yawDeg, pitchDeg, iconHalfWidth, gridScale, sphereScale)
        val (cellsW, cellsH) = cellsForExtents(halfWidth, halfHeight, iconHalfWidth, gridScale)
        val stepYaw = cellYawDeg(iconHalfWidth, gridScale, sphereScale)
        val stepPitch = cellPitchDeg(iconHalfWidth, gridScale, sphereScale)
        return SnapTarget(
            yawDeg = yaw,
            pitchDeg = pitch,
            halfYawDeg = cellsW * stepYaw * 0.5f,
            halfPitchDeg = cellsH * stepPitch * 0.5f,
            cellsW = cellsW,
            cellsH = cellsH,
        )
    }

    /** Closed rectangle outline samples for a snap target on the sphere. */
    fun snapTargetOutline(
        target: SnapTarget,
        samplesPerEdge: Int = 8,
    ): List<Pair<Float, Float>> {
        val y0 = target.yawDeg - target.halfYawDeg
        val y1 = target.yawDeg + target.halfYawDeg
        val p0 = target.pitchDeg - target.halfPitchDeg
        val p1 = target.pitchDeg + target.halfPitchDeg
        val n = samplesPerEdge.coerceAtLeast(2)
        val points = ArrayList<Pair<Float, Float>>(n * 4 + 1)
        fun edge(ya: Float, pa: Float, yb: Float, pb: Float) {
            for (i in 0 until n) {
                val t = i / (n - 1).toFloat()
                points += (ya + (yb - ya) * t) to (pa + (pb - pa) * t)
            }
        }
        edge(y0, p0, y1, p0)
        edge(y1, p0, y1, p1)
        edge(y1, p1, y0, p1)
        edge(y0, p1, y0, p0)
        points += points.first()
        return points
    }

    fun snapHalfExtents(
        halfWidth: Float,
        halfHeight: Float,
        iconHalfWidth: Float,
        gridScale: Float,
    ): Pair<Float, Float> {
        val cell = cellHalfExtent(iconHalfWidth, gridScale)
        val cellsW = cellsForHalf(halfWidth, cell).coerceAtLeast(1)
        val cellsH = cellsForHalf(halfHeight, cell).coerceAtLeast(1)
        return extentsForCells(cellsW, cellsH, cell)
    }

    fun cellsForHalf(halfExtent: Float, cellHalf: Float): Int {
        val full = cellHalf * 2f
        if (full <= 1e-4f) return 1
        return ((halfExtent * 2f) / full).roundToInt().coerceAtLeast(1)
    }

    fun extentsForCells(cellsW: Int, cellsH: Int, cellHalf: Float): Pair<Float, Float> {
        val full = cellHalf * 2f
        return (cellsW.coerceAtLeast(1) * full * 0.5f) to
            (cellsH.coerceAtLeast(1) * full * 0.5f)
    }

    fun cellsForExtents(
        halfWidth: Float,
        halfHeight: Float,
        iconHalfWidth: Float,
        gridScale: Float,
    ): Pair<Int, Int> {
        val cell = cellHalfExtent(iconHalfWidth, gridScale)
        return cellsForHalf(halfWidth, cell) to cellsForHalf(halfHeight, cell)
    }

    /**
     * Line samples for a yaw×pitch grid window centered on [centerYaw]/[centerPitch].
     * Each line is a list of (yaw, pitch) samples along the sphere.
     */
    fun overlayLines(
        centerYawDeg: Float,
        centerPitchDeg: Float,
        iconHalfWidth: Float,
        gridScale: Float,
        sphereScale: Float,
        halfSpanYawDeg: Float = 55f,
        halfSpanPitchDeg: Float = 40f,
        samplesPerLine: Int = 12,
    ): List<List<Pair<Float, Float>>> {
        val stepYaw = cellYawDeg(iconHalfWidth, gridScale, sphereScale)
        val stepPitch = cellPitchDeg(iconHalfWidth, gridScale, sphereScale)
        val yawMin = snapToStep(centerYawDeg - halfSpanYawDeg, stepYaw)
        val yawMax = snapToStep(centerYawDeg + halfSpanYawDeg, stepYaw)
        val pitchMin = snapToStep(
            (centerPitchDeg - halfSpanPitchDeg).coerceAtLeast(-55f),
            stepPitch,
        ).coerceAtLeast(-55f)
        val pitchMax = snapToStep(
            (centerPitchDeg + halfSpanPitchDeg).coerceAtMost(55f),
            stepPitch,
        ).coerceAtMost(55f)
        val lines = ArrayList<List<Pair<Float, Float>>>(64)
        var yaw = yawMin
        var guard = 0
        while (yaw <= yawMax + 1e-3f && guard++ < 80) {
            lines += sampleLine(
                yawFrom = yaw,
                pitchFrom = pitchMin,
                yawTo = yaw,
                pitchTo = pitchMax,
                samples = samplesPerLine,
            )
            yaw += stepYaw
        }
        var pitch = pitchMin
        guard = 0
        while (pitch <= pitchMax + 1e-3f && guard++ < 80) {
            lines += sampleLine(
                yawFrom = yawMin,
                pitchFrom = pitch,
                yawTo = yawMax,
                pitchTo = pitch,
                samples = samplesPerLine,
            )
            pitch += stepPitch
        }
        return lines
    }

    private fun sampleLine(
        yawFrom: Float,
        pitchFrom: Float,
        yawTo: Float,
        pitchTo: Float,
        samples: Int,
    ): List<Pair<Float, Float>> {
        val n = samples.coerceAtLeast(2)
        return List(n) { i ->
            val t = i / (n - 1).toFloat()
            (yawFrom + (yawTo - yawFrom) * t) to (pitchFrom + (pitchTo - pitchFrom) * t)
        }
    }

    private fun snapToStep(value: Float, step: Float): Float {
        if (step <= 1e-4f) return value
        return (value / step).roundToInt() * step
    }

    fun nearlyEqual(a: Float, b: Float, eps: Float = 0.02f): Boolean = abs(a - b) <= eps
}
