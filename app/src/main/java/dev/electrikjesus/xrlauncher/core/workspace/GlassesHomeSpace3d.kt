package dev.electrikjesus.xrlauncher.core.workspace

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * BumpDesk-style Home Space: camera at the origin, objects in world XYZ on an
 * inward-facing cylinder, wallpaper as a surrounding room farther on Z.
 */
object GlassesHomeSpace3d {
    /** Angular spacing between Home panes, in degrees. */
    const val PANE_ARC_DEGREES = 32f

    /** Distance from the camera to each pane (scene units). */
    const val PANE_RADIUS = 1.85f

    /** Surrounding room / wallpaper cylinder, farther than the panes. */
    const val ROOM_RADIUS = 3.4f

    const val FOV_Y_DEGREES = 52f
    const val CURSOR_YAW_DEGREES = 10f
    const val CURSOR_PITCH_DEGREES = 12f
    const val MAX_PITCH_DEGREES = 18f

    data class WorldPose(
        val x: Float,
        val y: Float,
        val z: Float,
        val rotationYDeg: Float,
        val rotationXDeg: Float,
    )

    data class ProjectedPane(
        val translationXPx: Float,
        val translationYPx: Float,
        val rotationYDeg: Float,
        val rotationXDeg: Float,
        val scale: Float,
        val alpha: Float,
        val cameraDistancePx: Float,
        val visible: Boolean,
        val viewZ: Float,
    )

    fun cameraYawDegrees(look: Float, cursorX: Float): Float =
        look * PANE_ARC_DEGREES + (cursorX.coerceIn(0f, 1f) - 0.5f) * 2f * CURSOR_YAW_DEGREES

    fun cameraPitchDegrees(cursorY: Float): Float =
        ((cursorY.coerceIn(0f, 1f) - 0.5f) * 2f * CURSOR_PITCH_DEGREES)
            .coerceIn(-MAX_PITCH_DEGREES, MAX_PITCH_DEGREES)

    fun paneWorldPose(worldX: Float): WorldPose {
        val yawRad = Math.toRadians((worldX * PANE_ARC_DEGREES).toDouble()).toFloat()
        return WorldPose(
            x = PANE_RADIUS * sin(yawRad),
            y = 0f,
            z = -PANE_RADIUS * cos(yawRad),
            rotationYDeg = -worldX * PANE_ARC_DEGREES,
            rotationXDeg = 0f,
        )
    }

    fun projectPane(
        worldX: Float,
        look: Float,
        cursorX: Float,
        cursorY: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): ProjectedPane {
        val yawDeg = cameraYawDegrees(look, cursorX)
        val pitchDeg = cameraPitchDegrees(cursorY)
        val pose = paneWorldPose(worldX)
        val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()

        // Match CylinderGlRenderer: p_view = Rx(pitch) * Ry(yaw) * p_world
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val x1 = pose.x * cosY + pose.z * sinY
        val z1 = -pose.x * sinY + pose.z * cosY
        val y1 = pose.y

        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)
        val y2 = y1 * cosP - z1 * sinP
        val z2 = y1 * sinP + z1 * cosP
        val x2 = x1

        val relYaw = worldX * PANE_ARC_DEGREES - yawDeg
        val inFront = z2 < -0.25f
        val onScreen = abs(relYaw) < 70f && abs(pitchDeg) <= MAX_PITCH_DEGREES + 1f
        if (!inFront || !onScreen || viewportWidthPx <= 0f || viewportHeightPx <= 0f) {
            return ProjectedPane(0f, 0f, 0f, 0f, 1f, 0f, 1f, false, z2)
        }

        val fovy = Math.toRadians(FOV_Y_DEGREES.toDouble()).toFloat()
        val sy = 1f / tan(fovy / 2f)
        val aspect = viewportWidthPx / viewportHeightPx
        val ndcX = (sy / aspect) * (x2 / -z2)
        val ndcY = sy * (y2 / -z2)
        val fade = paneAlpha(relYaw)
        return ProjectedPane(
            translationXPx = ndcX * viewportWidthPx * 0.5f,
            translationYPx = -ndcY * viewportHeightPx * 0.5f,
            rotationYDeg = pose.rotationYDeg + yawDeg,
            rotationXDeg = pose.rotationXDeg - pitchDeg,
            scale = 1f,
            alpha = fade,
            cameraDistancePx = viewportWidthPx * 2.4f,
            visible = fade > 0.02f,
            viewZ = z2,
        )
    }

    fun paneAlpha(relativeYawDeg: Float): Float {
        val distance = abs(relativeYawDeg)
        return when {
            distance >= 62f -> 0f
            distance <= 38f -> 1f
            else -> ((62f - distance) / 24f).coerceIn(0f, 1f)
        }
    }
}
