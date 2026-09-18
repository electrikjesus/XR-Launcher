package dev.electrikjesus.xrlauncher.core.workspace.scene

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Game-engine Home Space (BumpDesk / BumpTop model): FPS camera at the origin,
 * entities with world XYZ, pane quads whose corners sit on an invisible sphere
 * so overlap can be tested in world space instead of Compose screen space.
 */
object HomeSpaceScene {
    const val SPHERE_RADIUS = 1.85f
    const val ROOM_RADIUS = 3.4f
    const val FOV_Y_DEGREES = 64f
    const val PANE_GAP_DEGREES = 8f
    const val PANE_WIDTH_FRACTION = 0.74f
    const val PANE_HEIGHT_FRACTION = 0.88f
    const val CURSOR_YAW_DEGREES = 8f
    const val CURSOR_PITCH_DEGREES = 20f
    const val MAX_PITCH_DEGREES = 24f

    data class Camera(
        val yawDeg: Float,
        val pitchDeg: Float,
    ) {
        fun viewPoint(world: Vec3): Vec3 {
            val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
            val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()
            val cosY = cos(yawRad)
            val sinY = sin(yawRad)
            val x1 = world.x * cosY + world.z * sinY
            val z1 = -world.x * sinY + world.z * cosY
            val cosP = cos(pitchRad)
            val sinP = sin(pitchRad)
            return Vec3(
                x = x1,
                y = world.y * cosP - z1 * sinP,
                z = world.y * sinP + z1 * cosP,
            )
        }
    }

    data class Pane(
        val worldX: Float,
        val yawDeg: Float,
        val center: Vec3,
        val corners: List<Vec3>,
        val halfWidthDeg: Float,
        val halfHeightDeg: Float,
    ) {
        val yawMin: Float get() = yawDeg - halfWidthDeg
        val yawMax: Float get() = yawDeg + halfWidthDeg
        val pitchMin: Float get() = -halfHeightDeg
        val pitchMax: Float get() = halfHeightDeg
    }

    fun perspectiveCameraDistancePx(viewportHeightPx: Float): Float {
        val fovy = Math.toRadians(FOV_Y_DEGREES.toDouble()).toFloat()
        return (viewportHeightPx.coerceAtLeast(1f) * 0.5f) / tan(fovy / 2f)
    }

    fun angularHalfWidthDeg(viewportWidthPx: Float, viewportHeightPx: Float): Float {
        val cam = perspectiveCameraDistancePx(viewportHeightPx)
        val halfPx = viewportWidthPx.coerceAtLeast(1f) * PANE_WIDTH_FRACTION * 0.5f
        return Math.toDegrees(atan(halfPx / cam).toDouble()).toFloat()
    }

    fun angularHalfHeightDeg(viewportWidthPx: Float, viewportHeightPx: Float): Float {
        val cam = perspectiveCameraDistancePx(viewportHeightPx)
        val halfPx = viewportHeightPx.coerceAtLeast(1f) * PANE_HEIGHT_FRACTION * 0.5f
        return Math.toDegrees(atan(halfPx / cam).toDouble()).toFloat()
    }

    /** Angular spacing so adjacent pane edges keep [PANE_GAP_DEGREES] of sphere. */
    fun paneArcDegrees(viewportWidthPx: Float, viewportHeightPx: Float): Float =
        2f * angularHalfWidthDeg(viewportWidthPx, viewportHeightPx) + PANE_GAP_DEGREES

    fun camera(
        look: Float,
        cursorX: Float,
        cursorY: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): Camera {
        val arc = paneArcDegrees(viewportWidthPx, viewportHeightPx)
        val yaw = look * arc + (cursorX.coerceIn(0f, 1f) - 0.5f) * 2f * CURSOR_YAW_DEGREES
        val pitch = ((cursorY.coerceIn(0f, 1f) - 0.5f) * 2f * CURSOR_PITCH_DEGREES)
            .coerceIn(-MAX_PITCH_DEGREES, MAX_PITCH_DEGREES)
        return Camera(yawDeg = yaw, pitchDeg = pitch)
    }

    fun pane(
        worldX: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): Pane {
        val arc = paneArcDegrees(viewportWidthPx, viewportHeightPx)
        val yawDeg = worldX * arc
        val halfW = angularHalfWidthDeg(viewportWidthPx, viewportHeightPx)
        val halfH = angularHalfHeightDeg(viewportWidthPx, viewportHeightPx)
        val tanW = tan(Math.toRadians(halfW.toDouble()).toFloat())
        val tanH = tan(Math.toRadians(halfH.toDouble()).toFloat())
        val depth = SPHERE_RADIUS / sqrt(1f + tanW * tanW + tanH * tanH)
        val halfWWorld = depth * tanW
        val halfHWorld = depth * tanH
        val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
        val center = Vec3(
            x = depth * sin(yawRad),
            y = 0f,
            z = -depth * cos(yawRad),
        )
        val local = listOf(
            Vec3(-halfWWorld, -halfHWorld, 0f),
            Vec3(halfWWorld, -halfHWorld, 0f),
            Vec3(halfWWorld, halfHWorld, 0f),
            Vec3(-halfWWorld, halfHWorld, 0f),
        )
        val corners = local.map { corner ->
            Vec3(
                x = center.x + corner.x * cos(yawRad),
                y = center.y + corner.y,
                z = center.z - corner.x * sin(yawRad),
            )
        }
        return Pane(
            worldX = worldX,
            yawDeg = yawDeg,
            center = center,
            corners = corners,
            halfWidthDeg = halfW,
            halfHeightDeg = halfH,
        )
    }

    fun panesOverlap(a: Pane, b: Pane): Boolean {
        if (a.worldX == b.worldX) return false
        val yawOverlap = a.yawMax > b.yawMin + 0.05f && a.yawMin < b.yawMax - 0.05f
        val pitchOverlap = a.pitchMax > b.pitchMin + 0.05f && a.pitchMin < b.pitchMax - 0.05f
        return yawOverlap && pitchOverlap
    }

    fun overlappingPairs(worldXs: List<Float>, viewportWidthPx: Float, viewportHeightPx: Float): List<Pair<Float, Float>> {
        val panes = worldXs.map { pane(it, viewportWidthPx, viewportHeightPx) }
        val hits = mutableListOf<Pair<Float, Float>>()
        for (i in panes.indices) {
            for (j in i + 1 until panes.size) {
                if (panesOverlap(panes[i], panes[j])) {
                    hits += panes[i].worldX to panes[j].worldX
                }
            }
        }
        return hits
    }

    fun projectToView(
        world: Vec3,
        camera: Camera,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): Vec3 {
        val view = camera.viewPoint(world)
        if (view.z >= -0.05f || viewportWidthPx <= 0f || viewportHeightPx <= 0f) {
            return Vec3(0f, 0f, view.z)
        }
        val fovy = Math.toRadians(FOV_Y_DEGREES.toDouble()).toFloat()
        val aspect = viewportWidthPx / viewportHeightPx.coerceAtLeast(1f)
        val sy = 1f / tan(fovy / 2f)
        val sx = sy / aspect
        val ndcX = sx * (view.x / -view.z)
        val ndcY = sy * (view.y / -view.z)
        return Vec3(
            x = ndcX * viewportWidthPx * 0.5f,
            y = -ndcY * viewportHeightPx * 0.5f,
            z = view.z,
        )
    }
}
