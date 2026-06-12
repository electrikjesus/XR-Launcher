package dev.electrikjesus.xrlauncher.core.workspace

import kotlin.math.cos
import kotlin.math.sin

/**
 * Viewer-inside inner-cylinder placement for workspace panels.
 *
 * Curvature controls how far panels wrap around the arc; radius and standoff keep the
 * wall at a comfortable distance so panels stay selectable and readable.
 */
object WorkspaceCylinderGeometry {
    /** Horizontal arc span (degrees) at curvature = 1, span = 1. */
    const val MAX_ARC_YAW_DEGREES = 72f

    /** Vertical arc span (degrees) at curvature = 1, span = 1. */
    const val MAX_ARC_PITCH_DEGREES = 26f

    /** Compose / perspective focal length as a multiple of viewport width. */
    const val FOCAL_LENGTH_VIEWPORT_FRACTION = 2.85f

    /**
     * Forward distance from viewer to the center of the front wall, as a fraction of viewport width.
     * Keeps center panels off the camera plane.
     */
    const val BASE_WALL_DEPTH_FRACTION = 1.65f

    /** Inner-cylinder horizontal radius multiplier (× half viewport width). */
    const val RADIUS_X_FRACTION = 2.35f

    /** Inner-cylinder vertical radius multiplier (× half viewport height). */
    const val RADIUS_Y_FRACTION = 1.45f

    /** Floor for curvature when scaling radius/depth so low wrap values still feel spacious. */
    const val MIN_CURVATURE_BLEND = 0.5f

    data class PanelPlacement(
        val arcShiftXPx: Float,
        val arcShiftYPx: Float,
        val rotationYDeg: Float,
        val rotationXDeg: Float,
        val scale: Float,
    )

    data class CameraState(
        val yawDegrees: Float,
        val pitchDegrees: Float,
        val panNormX: Float,
        val panNormY: Float,
    )

    data class WallPoint(
        val xPx: Float,
        val yPx: Float,
        /** Distance into the scene from the viewer (always positive). */
        val depthPx: Float,
        val rotationYDeg: Float,
        val rotationXDeg: Float,
        val foreshorten: Float,
    )

    /** World-space panel frame for GLES quads (viewer at origin, −Z into the scene). */
    data class PanelWorldFrame(
        val positionX: Float,
        val positionY: Float,
        val positionZ: Float,
        val rotationYDeg: Float,
        val rotationXDeg: Float,
        val widthScene: Float,
        val heightScene: Float,
        val scale: Float,
    )

    internal fun curvatureBlend(curvature: Float): Float {
        val c = curvature.coerceIn(0f, 1f)
        return MIN_CURVATURE_BLEND + (1f - MIN_CURVATURE_BLEND) * c
    }

    internal fun baseDepthPx(viewportWidthPx: Float, curvature: Float): Float =
        viewportWidthPx * BASE_WALL_DEPTH_FRACTION * curvatureBlend(curvature)

    internal fun horizontalRadiusPx(
        viewportWidthPx: Float,
        spanX: Float,
        curvature: Float,
    ): Float = viewportWidthPx * 0.5f * RADIUS_X_FRACTION * spanX * curvatureBlend(curvature)

    internal fun verticalRadiusPx(
        viewportHeightPx: Float,
        spanY: Float,
        curvature: Float,
    ): Float = viewportHeightPx * 0.5f * RADIUS_Y_FRACTION * spanY * curvatureBlend(curvature)

    /** Normalized GL scene radius on X (matches [panelWorldFrame] units). */
    fun sceneRadiusX(viewportWidthPx: Float, spanX: Float, curvature: Float): Float {
        if (viewportWidthPx <= 0f) return 0f
        return horizontalRadiusPx(viewportWidthPx, spanX, curvature) / (viewportWidthPx * 0.5f)
    }

    /** Normalized GL scene base depth (matches [panelWorldFrame] units). */
    fun sceneBaseDepth(viewportWidthPx: Float, curvature: Float): Float {
        if (viewportWidthPx <= 0f) return 0f
        return baseDepthPx(viewportWidthPx, curvature) / (viewportWidthPx * 0.5f)
    }

    fun wallPoint(
        centerXNorm: Float,
        centerYNorm: Float,
        curvature: Float,
        workspaceWidth: Float,
        workspaceHeight: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): WallPoint? {
        val c = curvature.coerceIn(0f, 1f)
        if (c <= 0f || viewportWidthPx <= 0f || viewportHeightPx <= 0f) return null

        val spanX = workspaceWidth.coerceIn(
            WorkspaceAppearance.MIN_WORKSPACE_SPAN,
            WorkspaceAppearance.MAX_WORKSPACE_SPAN,
        )
        val spanY = workspaceHeight.coerceIn(
            WorkspaceAppearance.MIN_WORKSPACE_SPAN,
            WorkspaceAppearance.MAX_WORKSPACE_SPAN,
        )

        val halfArcYawRad = Math.toRadians((MAX_ARC_YAW_DEGREES * c * spanX) / 2.0)
        val halfArcPitchRad = Math.toRadians((MAX_ARC_PITCH_DEGREES * c * spanY) / 2.0)
        val thetaYaw = (centerXNorm - 0.5f) * 2f * halfArcYawRad.toFloat()
        val thetaPitch = (centerYNorm - 0.5f) * 2f * halfArcPitchRad.toFloat()

        val radiusX = horizontalRadiusPx(viewportWidthPx, spanX, c)
        val radiusY = verticalRadiusPx(viewportHeightPx, spanY, c)
        val baseDepth = baseDepthPx(viewportWidthPx, c)

        // Inner wall: center panel sits at baseDepth; edges wrap on the arc.
        val xPx = radiusX * sin(thetaYaw)
        val depthPx = baseDepth + radiusX * (1f - cos(thetaYaw))
        val yPx = radiusY * sin(thetaPitch)

        val rotationYDeg = Math.toDegrees(-thetaYaw.toDouble()).toFloat()
        val rotationXDeg = Math.toDegrees(-thetaPitch.toDouble()).toFloat()
        val foreshorten = (cos(thetaYaw) * cos(thetaPitch)).coerceIn(0.65f, 1f)

        return WallPoint(
            xPx = xPx,
            yPx = yPx,
            depthPx = depthPx,
            rotationYDeg = rotationYDeg,
            rotationXDeg = rotationXDeg,
            foreshorten = foreshorten,
        )
    }

    fun panelWorldFrame(
        centerXNorm: Float,
        centerYNorm: Float,
        widthNorm: Float,
        heightNorm: Float,
        curvature: Float,
        workspaceWidth: Float,
        workspaceHeight: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): PanelWorldFrame {
        val wall = wallPoint(
            centerXNorm, centerYNorm, curvature, workspaceWidth, workspaceHeight,
            viewportWidthPx, viewportHeightPx,
        )
        if (wall == null) {
            return PanelWorldFrame(
                positionX = (centerXNorm - 0.5f) * 2f,
                positionY = (centerYNorm - 0.5f) * 2f,
                positionZ = 0f,
                rotationYDeg = 0f,
                rotationXDeg = 0f,
                widthScene = widthNorm * 2f,
                heightScene = heightNorm * 2f,
                scale = 1f,
            )
        }

        val placement = panelPlacement(
            centerXNorm, centerYNorm, curvature, workspaceWidth, workspaceHeight,
            viewportWidthPx, viewportHeightPx,
        )
        val halfW = viewportWidthPx / 2f
        val halfH = viewportHeightPx / 2f

        return PanelWorldFrame(
            positionX = wall.xPx / halfW,
            positionY = wall.yPx / halfH,
            positionZ = -wall.depthPx / halfW,
            rotationYDeg = wall.rotationYDeg,
            rotationXDeg = wall.rotationXDeg,
            widthScene = widthNorm * 2f,
            heightScene = heightNorm * 2f,
            scale = placement.scale,
        )
    }

    fun panelPlacement(
        centerXNorm: Float,
        centerYNorm: Float,
        curvature: Float,
        workspaceWidth: Float,
        workspaceHeight: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): PanelPlacement {
        val wall = wallPoint(
            centerXNorm, centerYNorm, curvature, workspaceWidth, workspaceHeight,
            viewportWidthPx, viewportHeightPx,
        ) ?: return PanelPlacement(0f, 0f, 0f, 0f, 1f)

        val focal = viewportWidthPx * FOCAL_LENGTH_VIEWPORT_FRACTION
        val perspective = (focal / (focal + wall.depthPx)).coerceIn(0.55f, 1f)

        val flatCenterX = (centerXNorm - 0.5f) * viewportWidthPx
        val flatCenterY = (centerYNorm - 0.5f) * viewportHeightPx

        val projectedX = wall.xPx * perspective
        val projectedY = wall.yPx * perspective + flatCenterY * (1f - perspective) * 0.15f
        val arcShiftX = projectedX - flatCenterX * perspective
        val arcShiftY = projectedY - flatCenterY * perspective

        return PanelPlacement(
            arcShiftXPx = arcShiftX,
            arcShiftYPx = arcShiftY,
            rotationYDeg = wall.rotationYDeg,
            rotationXDeg = wall.rotationXDeg,
            scale = perspective * wall.foreshorten,
        )
    }

    fun cursorPanNorm(
        cursorX: Float,
        cursorY: Float,
        workspaceWidth: Float,
        workspaceHeight: Float,
    ): Pair<Float, Float> {
        val (cursorNormX, cursorNormY) = WorkspaceWraparound.cursorNorm(cursorX, cursorY)
        val spanX = workspaceWidth.coerceIn(
            WorkspaceAppearance.MIN_WORKSPACE_SPAN,
            WorkspaceAppearance.MAX_WORKSPACE_SPAN,
        )
        val spanY = workspaceHeight.coerceIn(
            WorkspaceAppearance.MIN_WORKSPACE_SPAN,
            WorkspaceAppearance.MAX_WORKSPACE_SPAN,
        )
        val extraX = (spanX - 1f).coerceAtLeast(0f)
        val extraY = (spanY - 1f).coerceAtLeast(0f)
        val widePanX = cursorNormX * extraX * WorkspaceWraparound.WIDE_SPAN_PAN_FRACTION
        val widePanY = cursorNormY * extraY * WorkspaceWraparound.WIDE_SPAN_PAN_FRACTION
        val panX = (cursorNormX * WorkspaceWraparound.PANEL_MOUSE_LOOK_FRACTION + widePanX)
            .coerceIn(-1f, 1f)
        val panY = (cursorNormY * WorkspaceWraparound.PANEL_MOUSE_LOOK_FRACTION + widePanY)
            .coerceIn(-1f, 1f)
        return panX to panY
    }

    fun cameraState(
        cursorX: Float,
        cursorY: Float,
        lookYawDegrees: Float,
        lookPitchDegrees: Float,
        workspaceWidth: Float,
        workspaceHeight: Float,
    ): CameraState {
        val (panX, panY) = cursorPanNorm(cursorX, cursorY, workspaceWidth, workspaceHeight)
        return CameraState(
            yawDegrees = -lookYawDegrees,
            pitchDegrees = -lookPitchDegrees,
            panNormX = -panX,
            panNormY = -panY,
        )
    }
}
