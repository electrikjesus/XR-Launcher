package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import kotlin.math.abs

/**
 * Home Space projection: a BumpDesk-style FPS camera looking at world-fixed panes
 * whose corners sit on an invisible sphere. Compose is a view of that scene.
 */
object GlassesHomeSpace3d {
    const val PANE_RADIUS = HomeSpaceScene.SPHERE_RADIUS
    const val ROOM_RADIUS = HomeSpaceScene.ROOM_RADIUS
    const val FOV_Y_DEGREES = HomeSpaceScene.FOV_Y_DEGREES
    const val PANE_WIDTH_FRACTION = HomeSpaceScene.PANE_WIDTH_FRACTION
    const val PANE_HEIGHT_FRACTION = HomeSpaceScene.PANE_HEIGHT_FRACTION

    /** @see HomeSpaceScene.paneArcDegrees */
    const val PANE_ARC_DEGREES = 50f

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
        val translationZPx: Float,
        val rotationYDeg: Float,
        val rotationXDeg: Float,
        val scale: Float,
        val alpha: Float,
        val cameraDistancePx: Float,
        val visible: Boolean,
        val viewZ: Float,
    )

    fun paneArcDegrees(
        viewportWidthPx: Float = 1920f,
        viewportHeightPx: Float = 1080f,
        panelScale: Float = 1f,
    ): Float = HomeSpaceScene.paneArcDegrees(viewportWidthPx, viewportHeightPx, panelScale)

    fun cameraYawDegrees(
        look: Float,
        cursorX: Float,
        viewportWidthPx: Float = 1920f,
        viewportHeightPx: Float = 1080f,
        panelScale: Float = 1f,
    ): Float = HomeSpaceScene.camera(
        look,
        cursorX,
        0.5f,
        viewportWidthPx,
        viewportHeightPx,
        panelScale,
    ).yawDeg

    fun cameraPitchDegrees(cursorY: Float): Float =
        HomeSpaceScene.camera(0f, 0.5f, cursorY, 1920f, 1080f).pitchDeg

    fun paneWorldPose(
        worldX: Float,
        viewportWidthPx: Float = 1920f,
        viewportHeightPx: Float = 1080f,
        panelScale: Float = 1f,
        sphereScale: Float = 1f,
    ): WorldPose {
        val pane = HomeSpaceScene.pane(
            worldX,
            viewportWidthPx,
            viewportHeightPx,
            panelScale,
            sphereScale,
        )
        return WorldPose(
            x = pane.center.x,
            y = pane.center.y,
            z = pane.center.z,
            rotationYDeg = -pane.yawDeg,
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
        panelScale: Float = 1f,
        sphereScale: Float = 1f,
    ): ProjectedPane {
        val camera = HomeSpaceScene.camera(
            look,
            cursorX,
            cursorY,
            viewportWidthPx,
            viewportHeightPx,
            panelScale,
        )
        val pane = HomeSpaceScene.pane(
            worldX,
            viewportWidthPx,
            viewportHeightPx,
            panelScale,
            sphereScale,
        )
        val view = camera.viewPoint(pane.center)
        val relYaw = pane.yawDeg - camera.yawDeg
        val inFront = view.z < -0.05f
        if (!inFront || viewportWidthPx <= 0f || viewportHeightPx <= 0f) {
            return hiddenPane(view.z)
        }
        val fade = paneAlpha(relYaw, paneArcDegrees(viewportWidthPx, viewportHeightPx, panelScale))
        if (fade <= 0.02f) {
            return hiddenPane(view.z)
        }
        val projected = HomeSpaceScene.projectToView(
            pane.center,
            camera,
            viewportWidthPx,
            viewportHeightPx,
        )
        val cameraDistancePx = HomeSpaceScene.perspectiveCameraDistancePx(viewportHeightPx)
        val radius = HomeSpaceScene.sphereRadius(sphereScale)
        return ProjectedPane(
            translationXPx = projected.x,
            translationYPx = projected.y,
            translationZPx = (view.z + radius) * (cameraDistancePx / radius),
            rotationYDeg = -relYaw,
            rotationXDeg = -camera.pitchDeg,
            scale = 1f,
            alpha = fade,
            cameraDistancePx = cameraDistancePx,
            visible = true,
            viewZ = view.z,
        )
    }

    fun perspectiveCameraDistancePx(viewportHeightPx: Float): Float =
        HomeSpaceScene.perspectiveCameraDistancePx(viewportHeightPx)

    private fun hiddenPane(viewZ: Float) = ProjectedPane(
        translationXPx = 0f,
        translationYPx = 0f,
        translationZPx = 0f,
        rotationYDeg = 0f,
        rotationXDeg = 0f,
        scale = 1f,
        alpha = 0f,
        cameraDistancePx = 1f,
        visible = false,
        viewZ = viewZ,
    )

    fun paneAlpha(relativeYawDeg: Float, arcDegrees: Float = PANE_ARC_DEGREES): Float {
        val steps = abs(relativeYawDeg) / arcDegrees.coerceAtLeast(1f)
        return when {
            steps >= 1.22f -> 0f
            steps <= 0.88f -> 1f
            else -> ((1.22f - steps) / 0.34f).coerceIn(0f, 1f)
        }
    }
}
