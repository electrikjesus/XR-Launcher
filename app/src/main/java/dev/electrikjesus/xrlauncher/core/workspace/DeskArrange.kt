package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import kotlin.math.ceil
import kotlin.math.sqrt

/** BumpDesk-style layout modes for a lasso / selection group on the sphere. */
enum class DeskArrangeMode {
    /** Compact overlapping stack (pile stand-in until true Pile items land). */
    STACK,
    /** Compact grid — BumpDesk folder layout stand-in. */
    FOLDER,
    GRID,
    ROW,
    COLUMN,
}

object DeskArrange {
    /**
     * Reposition selected Desktop icons around their yaw/pitch centroid.
     * @return false when fewer than 2 placed icons match [keys].
     */
    fun arrange(
        placed: List<HomeSpaceDesk.Placed>,
        keys: Set<String>,
        mode: DeskArrangeMode,
        sphereScale: Float,
        halfWidth: Float,
        halfHeight: Float,
    ): List<HomeSpaceDesk.Placed>? {
        val targets = placed.filter { it.app.componentKey in keys && it.app.kind == HomeSpaceDesk.Kind.APP }
        if (targets.size < 2) return null
        val radius = HomeSpaceScene.sphereRadius(sphereScale).coerceAtLeast(0.01f)
        val spacingMul = when (mode) {
            DeskArrangeMode.STACK -> 0.35f
            DeskArrangeMode.FOLDER -> 1.15f
            DeskArrangeMode.GRID -> 2.45f
            DeskArrangeMode.ROW -> 2.45f
            DeskArrangeMode.COLUMN -> 2.55f
        }
        val yawStep = Math.toDegrees((halfWidth * spacingMul / radius).toDouble()).toFloat()
        val pitchStep = Math.toDegrees((halfHeight * spacingMul / radius).toDouble()).toFloat()
        val centerYaw = targets.map { it.yawDeg }.average().toFloat()
        val centerPitch = targets.map { it.pitchDeg }.average().toFloat()
        val slots = when (mode) {
            DeskArrangeMode.STACK -> targets.indices.map { i ->
                val t = i - (targets.size - 1) * 0.5f
                (centerYaw + t * yawStep * 0.4f) to (centerPitch + t * pitchStep * 0.25f)
            }
            DeskArrangeMode.FOLDER, DeskArrangeMode.GRID -> {
                val cols = ceil(sqrt(targets.size.toDouble())).toInt().coerceAtLeast(1)
                targets.indices.map { i ->
                    val col = i % cols
                    val row = i / cols
                    val x = col - (cols - 1) * 0.5f
                    val y = row - ((targets.size - 1) / cols) * 0.5f
                    (centerYaw + x * yawStep) to (centerPitch - y * pitchStep)
                }
            }
            DeskArrangeMode.ROW -> targets.indices.map { i ->
                val x = i - (targets.size - 1) * 0.5f
                (centerYaw + x * yawStep) to centerPitch
            }
            DeskArrangeMode.COLUMN -> targets.indices.map { i ->
                val y = i - (targets.size - 1) * 0.5f
                centerYaw to (centerPitch - y * pitchStep)
            }
        }
        val byKey = targets.mapIndexed { index, item ->
            item.app.componentKey to item.copy(
                yawDeg = slots[index].first,
                pitchDeg = slots[index].second,
                velYawDeg = 0f,
                velPitchDeg = 0f,
            )
        }.toMap()
        return placed.map { item -> byKey[item.app.componentKey] ?: item }
    }
}
