package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import kotlin.math.ceil
import kotlin.math.sqrt

/** Sphere poses for collapsed / expanded [DeskPile] faces (BumpDesk PileRenderer subset). */
object DeskPileLayout {
    private const val STACK_FAN_SPACING = 2.2f
    private const val FOLDER_GRID_SPACING = 2.45f
    private const val BACKING_PAD = 0.55f

    fun appendIcons(
        icons: List<HomeSpaceDesk.Icon>,
        piles: List<DeskPile>,
        sphereScale: Float,
        halfWidth: Float,
        halfHeight: Float,
        draggingKey: String? = null,
        dragYawDeg: Float = 0f,
        dragPitchDeg: Float = 0f,
    ): List<HomeSpaceDesk.Icon> {
        if (piles.isEmpty()) return icons
        val scale = sphereScale.coerceAtLeast(0.01f)
        val radius = HomeSpaceScene.sphereRadius(scale).coerceAtLeast(0.01f)
        fun yawStep(spacing: Float): Float =
            Math.toDegrees((halfWidth * spacing / radius).toDouble()).toFloat()
        fun pitchStep(spacing: Float): Float =
            Math.toDegrees((halfHeight * spacing / radius).toDouble()).toFloat()
        val out = ArrayList<HomeSpaceDesk.Icon>(icons.size + piles.size * 8)
        out += icons
        piles.forEach { pile ->
            val yaw = if (pile.id == draggingKey) dragYawDeg else pile.yawDeg
            val pitch = if (pile.id == draggingKey) dragPitchDeg else pile.pitchDeg
            val faceLift = if (pile.id == draggingKey) HomeSpaceDesk.HOVER_LIFT else 0f
            out += HomeSpaceDesk.iconOf(
                app = HomeSpaceDesk.AppRef(
                    componentKey = pile.id,
                    label = pile.name,
                    packageName = pile.members.firstOrNull()?.packageName.orEmpty(),
                    kind = pile.kind(),
                ),
                yawDeg = yaw,
                pitchDeg = pitch,
                sphereScale = scale,
                halfWidth = halfWidth * if (pile.isFolder) 1.15f else 1f,
                halfHeight = halfHeight * if (pile.isFolder) 1.15f else 1f,
                lift = faceLift,
            )
            if (!pile.expanded) return@forEach
            when (pile.mode) {
                DeskPileMode.STACK -> {
                    val step = yawStep(STACK_FAN_SPACING)
                    pile.members.forEachIndexed { index, app ->
                        val t = index - (pile.members.size - 1) * 0.5f
                        out += HomeSpaceDesk.iconOf(
                            app = app,
                            yawDeg = yaw + t * step,
                            pitchDeg = pitch + 4f,
                            sphereScale = scale,
                            halfWidth = halfWidth,
                            halfHeight = halfHeight,
                            lift = HomeSpaceDesk.ICON_STACK_LIFT,
                        )
                    }
                }
                DeskPileMode.FOLDER -> {
                    val cols = ceil(sqrt(pile.members.size.toDouble())).toInt().coerceAtLeast(1)
                    val stepY = yawStep(FOLDER_GRID_SPACING)
                    val stepP = pitchStep(FOLDER_GRID_SPACING)
                    val rows = (pile.members.size - 1) / cols
                    pile.members.forEachIndexed { index, app ->
                        val col = index % cols
                        val row = index / cols
                        val x = col - (cols - 1) * 0.5f
                        val y = row - rows * 0.5f
                        out += HomeSpaceDesk.iconOf(
                            app = app,
                            yawDeg = yaw + x * stepY,
                            pitchDeg = pitch - y * stepP + 6f,
                            sphereScale = scale,
                            halfWidth = halfWidth,
                            halfHeight = halfHeight,
                            lift = HomeSpaceDesk.ICON_STACK_LIFT,
                        )
                    }
                    val halfYaw = (cols * 0.5f * stepY) * (1f + BACKING_PAD)
                    val halfPitch = ((rows + 1) * 0.5f * stepP) * (1f + BACKING_PAD)
                    out += HomeSpaceDesk.iconOf(
                        app = HomeSpaceDesk.AppRef(
                            componentKey = pile.id + ":backing",
                            label = pile.name,
                            packageName = "",
                            kind = HomeSpaceDesk.Kind.PILE_BACKING,
                        ),
                        yawDeg = yaw,
                        pitchDeg = pitch + 6f,
                        sphereScale = scale,
                        halfWidth = (Math.toRadians(halfYaw.toDouble()) * radius).toFloat()
                            .coerceAtLeast(halfWidth * 2f),
                        halfHeight = (Math.toRadians(halfPitch.toDouble()) * radius).toFloat()
                            .coerceAtLeast(halfHeight * 2f),
                        lift = HomeSpaceDesk.BACKING_LIFT,
                    )
                }
            }
        }
        return out
    }

    fun pileIdFromBackingKey(key: String): String? =
        key.removeSuffix(":backing").takeIf {
            key.endsWith(":backing") && DeskPile.isPileKey(it)
        }
}
