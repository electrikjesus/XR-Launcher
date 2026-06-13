package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode

/**
 * Legacy facade — delegates to [WorkspaceCylinderGeometry] for inner-cylinder math.
 */
object WorkspaceWraparound {
    const val MAX_CYLINDER_ARC_YAW_DEGREES = WorkspaceCylinderGeometry.MAX_ARC_YAW_DEGREES
    const val MAX_CYLINDER_ARC_PITCH_DEGREES = WorkspaceCylinderGeometry.MAX_ARC_PITCH_DEGREES
    const val BACKDROP_LOOK_RATIO = 0.32f
    const val PANEL_MOUSE_LOOK_FRACTION = 0.18f
    internal const val WIDE_SPAN_PAN_FRACTION = 0.5f
    const val BACKDROP_CURSOR_YAW_DEGREES = 14f
    const val BACKDROP_CURSOR_PITCH_DEGREES = 8f

    data class PanelTransform(
        val rotationY: Float,
        val rotationX: Float,
        val scale: Float,
        val translationXFraction: Float = 0f,
        val translationYFraction: Float = 0f,
    )

    fun panelTransform(
        centerX: Float,
        centerY: Float,
        curvature: Float,
        workspaceWidth: Float = 1f,
        workspaceHeight: Float = 1f,
        viewportWidthPx: Float = 1f,
        viewportHeightPx: Float = 1f,
    ): PanelTransform {
        val placement = WorkspaceCylinderGeometry.panelPlacement(
            centerXNorm = centerX,
            centerYNorm = centerY,
            curvature = curvature,
            workspaceWidth = workspaceWidth,
            workspaceHeight = workspaceHeight,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
        )
        val tx = if (viewportWidthPx > 0f) placement.arcShiftXPx / viewportWidthPx else 0f
        val ty = if (viewportHeightPx > 0f) placement.arcShiftYPx / viewportHeightPx else 0f
        return PanelTransform(
            rotationY = placement.rotationYDeg,
            rotationX = placement.rotationXDeg,
            scale = placement.scale,
            translationXFraction = tx,
            translationYFraction = ty,
        )
    }

    fun arcTranslationPx(
        centerX: Float,
        centerY: Float,
        curvature: Float,
        workspaceWidth: Float,
        workspaceHeight: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): Pair<Float, Float> {
        val placement = WorkspaceCylinderGeometry.panelPlacement(
            centerXNorm = centerX,
            centerYNorm = centerY,
            curvature = curvature,
            workspaceWidth = workspaceWidth,
            workspaceHeight = workspaceHeight,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
        )
        return placement.arcShiftXPx to placement.arcShiftYPx
    }

    fun cursorNorm(cursorX: Float, cursorY: Float): Pair<Float, Float> =
        (cursorX - 0.5f) * 2f to (cursorY - 0.5f) * 2f

    fun workspaceLayerPan(
        cursorX: Float,
        cursorY: Float,
        lookYawDegrees: Float,
        lookPitchDegrees: Float,
        workspaceWidth: Float,
        workspaceHeight: Float,
    ): Pair<Float, Float> = WorkspaceCylinderGeometry.cursorPanNorm(
        cursorX = cursorX,
        cursorY = cursorY,
        workspaceWidth = workspaceWidth,
        workspaceHeight = workspaceHeight,
    )

    fun backdropLook(
        cursorX: Float,
        cursorY: Float,
        lookYawDegrees: Float,
        lookPitchDegrees: Float,
    ): Pair<Float, Float> {
        val (cursorNormX, cursorNormY) = cursorNorm(cursorX, cursorY)
        val yaw = lookYawDegrees * BACKDROP_LOOK_RATIO + cursorNormX * BACKDROP_CURSOR_YAW_DEGREES
        val pitch = lookPitchDegrees * BACKDROP_LOOK_RATIO + cursorNormY * BACKDROP_CURSOR_PITCH_DEGREES
        return yaw to pitch
    }

    fun cursorViewportPan(
        cursorX: Float,
        cursorY: Float,
        lookYawDegrees: Float,
        lookPitchDegrees: Float,
        workspaceWidth: Float,
        workspaceHeight: Float,
    ): Pair<Float, Float> = workspaceLayerPan(
        cursorX, cursorY, lookYawDegrees, lookPitchDegrees, workspaceWidth, workspaceHeight,
    )

    fun effectiveLookYaw(appearance: WorkspaceAppearance): Float {
        if (GlassesSessionState.xrInputMode == GlassesXrInputMode.GLASSES_HEAD_TRACKING) return 0f
        return (appearance.lookYawDegrees + WorkspaceLookOffset.yawDegrees)
            .coerceIn(WorkspaceAppearance.MIN_LOOK_YAW, WorkspaceAppearance.MAX_LOOK_YAW)
    }

    fun effectiveLookPitch(appearance: WorkspaceAppearance): Float {
        if (GlassesSessionState.xrInputMode == GlassesXrInputMode.GLASSES_HEAD_TRACKING) return 0f
        return (appearance.lookPitchDegrees + WorkspaceLookOffset.pitchDegrees)
            .coerceIn(WorkspaceAppearance.MIN_LOOK_PITCH, WorkspaceAppearance.MAX_LOOK_PITCH)
    }
}

object WorkspaceLookOffset {
    var yawDegrees: Float = 0f
        private set
    var pitchDegrees: Float = 0f
        private set

    fun setOffset(yaw: Float, pitch: Float) {
        yawDegrees = yaw
        pitchDegrees = pitch
    }

    fun addDelta(deltaYaw: Float, deltaPitch: Float) {
        yawDegrees += deltaYaw
        pitchDegrees += deltaPitch
    }

    fun reset() {
        yawDegrees = 0f
        pitchDegrees = 0f
    }
}
