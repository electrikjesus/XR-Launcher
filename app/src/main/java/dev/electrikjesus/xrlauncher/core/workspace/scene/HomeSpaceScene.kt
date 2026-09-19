package dev.electrikjesus.xrlauncher.core.workspace.scene

import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
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
    const val CURSOR_YAW_DEGREES = 24f
    const val CURSOR_PITCH_DEGREES = 20f
    /** Soft pitch used by companion gradient cursor-look (not free-look FPS). */
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

    fun sphereRadius(sphereScale: Float = 1f): Float =
        SPHERE_RADIUS * sphereScale.coerceAtLeast(0.01f)

    fun roomRadius(sphereScale: Float = 1f): Float =
        ROOM_RADIUS * sphereScale.coerceAtLeast(0.01f)

    fun paneWidthFraction(panelScale: Float = 1f): Float =
        (PANE_WIDTH_FRACTION * panelScale).coerceIn(0.45f, 0.95f)

    fun paneHeightFraction(panelScale: Float = 1f): Float =
        (PANE_HEIGHT_FRACTION * panelScale).coerceIn(0.50f, 0.98f)

    fun perspectiveCameraDistancePx(viewportHeightPx: Float): Float {
        val fovy = Math.toRadians(FOV_Y_DEGREES.toDouble()).toFloat()
        return (viewportHeightPx.coerceAtLeast(1f) * 0.5f) / tan(fovy / 2f)
    }

    fun angularHalfWidthDeg(
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
    ): Float {
        val cam = perspectiveCameraDistancePx(viewportHeightPx)
        val halfPx = viewportWidthPx.coerceAtLeast(1f) * paneWidthFraction(panelScale) * 0.5f
        return Math.toDegrees(atan(halfPx / cam).toDouble()).toFloat()
    }

    fun angularHalfHeightDeg(
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
    ): Float {
        val cam = perspectiveCameraDistancePx(viewportHeightPx)
        val halfPx = viewportHeightPx.coerceAtLeast(1f) * paneHeightFraction(panelScale) * 0.5f
        return Math.toDegrees(atan(halfPx / cam).toDouble()).toFloat()
    }

    /**
     * On-screen angular size at [sphereScale]. World pane size is fixed at scale 1;
     * a larger sphere pulls the same pane farther from the camera so it reads smaller.
     */
    fun angularHalfWidthOnSphere(
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
        sphereScale: Float = 1f,
    ): Float = angularSizeOnSphere(
        angularHalfWidthDeg(viewportWidthPx, viewportHeightPx, panelScale),
        sphereScale,
    )

    fun angularHalfHeightOnSphere(
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
        sphereScale: Float = 1f,
    ): Float = angularSizeOnSphere(
        angularHalfHeightDeg(viewportWidthPx, viewportHeightPx, panelScale),
        sphereScale,
    )

    private fun angularSizeOnSphere(referenceDeg: Float, sphereScale: Float): Float {
        val scale = sphereScale.coerceAtLeast(0.01f)
        if (scale == 1f) return referenceDeg
        val refRad = Math.toRadians(referenceDeg.toDouble())
        return Math.toDegrees(atan(tan(refRad) / scale)).toFloat()
    }

    /** Angular spacing so adjacent pane edges keep [PANE_GAP_DEGREES] of sphere. */
    fun paneArcDegrees(
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
        sphereScale: Float = 1f,
    ): Float = 2f * angularHalfWidthOnSphere(
        viewportWidthPx,
        viewportHeightPx,
        panelScale,
        sphereScale,
    ) + PANE_GAP_DEGREES

    /**
     * Mouse-look on top of [look]: companion cursor yaws/pitches the FPS camera a little,
     * while hover still uses a ray through that same view onto the inner sphere.
     * [GlassesLookMode.FPS] and absolute host use [lookYawDegrees] / [lookPitchDeg] directly
     * so the user can spin a full circle (not clamped to Desktop…Tray pane units).
     */
    fun camera(
        look: Float,
        cursorX: Float,
        cursorY: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
        sphereScale: Float = 1f,
        lookMode: GlassesLookMode = GlassesLookMode.GRADIENT,
        lookPitchDeg: Float = 0f,
        applyCursorOffset: Boolean = lookMode != GlassesLookMode.FPS,
        lookYawDegrees: Float = Float.NaN,
    ): Camera {
        val arc = paneArcDegrees(viewportWidthPx, viewportHeightPx, panelScale, sphereScale)
        val baseYaw = if (lookYawDegrees.isNaN()) look * arc else lookYawDegrees
        if (lookMode == GlassesLookMode.FPS) {
            return Camera(yawDeg = baseYaw, pitchDeg = lookPitchDeg)
        }
        val cursorPitch = ((cursorY.coerceIn(0f, 1f) - 0.5f) * 2f * CURSOR_PITCH_DEGREES)
            .coerceIn(-MAX_PITCH_DEGREES, MAX_PITCH_DEGREES)
        if (!applyCursorOffset) {
            // Absolute host GRADIENT: keep yaw from free-look / edge pan (no cursor-X yaw —
            // that double-offset mis-aimed picks), but still pitch from cursor Y so looking
            // up/down works without middle-drag.
            return Camera(yawDeg = baseYaw, pitchDeg = cursorPitch)
        }
        val yaw = look * arc + (cursorX.coerceIn(0f, 1f) - 0.5f) * 2f * CURSOR_YAW_DEGREES
        return Camera(yawDeg = yaw, pitchDeg = cursorPitch)
    }

    /** One viewport-width of mouse travel → one horizontal FOV of yaw (degrees). */
    fun fpsYawDegreesDelta(
        deltaXNorm: Float,
        viewportWidthPx: Float = 1920f,
        viewportHeightPx: Float = 1080f,
    ): Float {
        val aspect = viewportWidthPx / viewportHeightPx.coerceAtLeast(1f)
        val halfFovY = Math.toRadians(FOV_Y_DEGREES / 2.0)
        val hfovDeg = (2.0 * Math.toDegrees(atan(aspect * tan(halfFovY)))).toFloat()
        return deltaXNorm * hfovDeg
    }

    /** Normalized pointer delta → panNorm units so a full-width swipe matches horizontal FOV. */
    fun fpsPanNormDelta(
        deltaX: Float,
        viewportWidthPx: Float = 1920f,
        viewportHeightPx: Float = 1080f,
        panelScale: Float = 1f,
        sphereScale: Float = 1f,
    ): Float {
        val arc = paneArcDegrees(viewportWidthPx, viewportHeightPx, panelScale, sphereScale)
            .coerceAtLeast(1f)
        return fpsYawDegreesDelta(deltaX, viewportWidthPx, viewportHeightPx) / arc
    }

    fun fpsPitchDelta(deltaY: Float): Float = deltaY * FOV_Y_DEGREES

    fun pane(
        worldX: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
        sphereScale: Float = 1f,
    ): Pane {
        val arc = paneArcDegrees(viewportWidthPx, viewportHeightPx, panelScale, sphereScale)
        val yawDeg = worldX * arc
        val halfW = angularHalfWidthOnSphere(viewportWidthPx, viewportHeightPx, panelScale, sphereScale)
        val halfH = angularHalfHeightOnSphere(viewportWidthPx, viewportHeightPx, panelScale, sphereScale)
        val tanW = tan(Math.toRadians(halfW.toDouble()).toFloat())
        val tanH = tan(Math.toRadians(halfH.toDouble()).toFloat())
        val depth = sphereRadius(sphereScale) / sqrt(1f + tanW * tanW + tanH * tanH)
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

    fun overlappingPairs(
        worldXs: List<Float>,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        panelScale: Float = 1f,
        sphereScale: Float = 1f,
    ): List<Pair<Float, Float>> {
        val panes = worldXs.map { pane(it, viewportWidthPx, viewportHeightPx, panelScale, sphereScale) }
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
