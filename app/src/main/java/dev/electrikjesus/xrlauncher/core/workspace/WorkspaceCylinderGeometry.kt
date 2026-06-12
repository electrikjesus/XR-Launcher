package dev.electrikjesus.xrlauncher.core.workspace

import kotlin.math.cos
import kotlin.math.sin

/**
 * Viewer-inside inner-cylinder placement for workspace panels.
 *
 * The viewer sits at the cylinder axis; panels live on the inner wall and rotate to face
 * the viewer. Rotations use Compose/Android conventions (negated arc angles vs raw θ).
 */
object WorkspaceCylinderGeometry {
    /** Horizontal arc span (degrees) at curvature = 1, span = 1. */
    const val MAX_ARC_YAW_DEGREES = 78f

    /** Vertical arc span (degrees) at curvature = 1, span = 1. */
    const val MAX_ARC_PITCH_DEGREES = 28f

    /** Default perspective focal length as a multiple of viewport width. */
    const val FOCAL_LENGTH_VIEWPORT_FRACTION = 1.35f

    /** Cylinder radius as a fraction of half the viewport width. */
    const val RADIUS_X_FRACTION = 0.88f

    /** Cylinder radius as a fraction of half the viewport height. */
    const val RADIUS_Y_FRACTION = 0.55f

    data class PanelPlacement(
        /** Extra horizontal shift (px) from flat slot toward the inner arc. */
        val arcShiftXPx: Float,
        /** Extra vertical shift (px) from flat slot toward the inner arc. */
        val arcShiftYPx: Float,
        /** Y rotation (degrees) so the panel faces the viewer. */
        val rotationYDeg: Float,
        /** X rotation (degrees) for vertical arc. */
        val rotationXDeg: Float,
        /** Perspective foreshortening scale. */
        val scale: Float,
    )

    data class CameraState(
        val yawDegrees: Float,
        val pitchDegrees: Float,
        val panNormX: Float,
        val panNormY: Float,
    )

    fun panelPlacement(
        centerXNorm: Float,
        centerYNorm: Float,
        curvature: Float,
        workspaceWidth: Float,
        workspaceHeight: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): PanelPlacement {
        val c = curvature.coerceIn(0f, 1f)
        if (c <= 0f || viewportWidthPx <= 0f || viewportHeightPx <= 0f) {
            return PanelPlacement(0f, 0f, 0f, 0f, 1f)
        }

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

        // u = 0 left, 1 right → negative θ on left, positive on right (viewer at center).
        val thetaYaw = (centerXNorm - 0.5f) * 2f * halfArcYawRad.toFloat()
        val thetaPitch = (centerYNorm - 0.5f) * 2f * halfArcPitchRad.toFloat()

        val radiusX = viewportWidthPx * 0.5f * RADIUS_X_FRACTION * c * spanX
        val radiusY = viewportHeightPx * 0.5f * RADIUS_Y_FRACTION * c * spanY
        val focal = viewportWidthPx * FOCAL_LENGTH_VIEWPORT_FRACTION

        // Inner wall point in viewer space (+Z toward viewer, wall recedes at edges).
        val wallX = radiusX * sin(thetaYaw)
        val wallZ = radiusX * (1f - cos(thetaYaw))
        val wallY = radiusY * sin(thetaPitch)

        val depth = wallZ.coerceAtLeast(0f)
        val perspective = (focal / (focal + depth)).coerceIn(0.72f, 1f)

        // Flat layout center in px (freeform / normalized slots).
        val flatCenterX = (centerXNorm - 0.5f) * viewportWidthPx
        val flatCenterY = (centerYNorm - 0.5f) * viewportHeightPx

        // Project arc position; shift is delta from flat slot.
        val projectedX = wallX * perspective
        val projectedY = wallY * perspective + flatCenterY * (1f - perspective) * 0.35f
        val arcShiftX = projectedX - flatCenterX * perspective
        val arcShiftY = projectedY - flatCenterY * perspective

        // Face the viewer: invert yaw/pitch vs raw arc angle (fixes outward-bend bug).
        val rotationYDeg = Math.toDegrees(-thetaYaw.toDouble()).toFloat()
        val rotationXDeg = Math.toDegrees(-thetaPitch.toDouble()).toFloat()
        val foreshorten = (cos(thetaYaw) * cos(thetaPitch)).coerceIn(0.72f, 1f)

        return PanelPlacement(
            arcShiftXPx = arcShiftX,
            arcShiftYPx = arcShiftY,
            rotationYDeg = rotationYDeg,
            rotationXDeg = rotationXDeg,
            scale = perspective * foreshorten,
        )
    }

    /** Cursor-only pan; look is applied as scene camera rotation. */
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
