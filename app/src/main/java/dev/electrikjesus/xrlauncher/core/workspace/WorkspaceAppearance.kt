package dev.electrikjesus.xrlauncher.core.workspace

/** Visual tuning for the glasses launcher shell (Tier 1). */
data class WorkspaceAppearance(
    /** Scales launcher panels, text, and icons (0.75–2.0). */
    val uiScale: Float = DEFAULT_UI_SCALE,
    /** Gap between panels in dp (0–200). */
    val panelGapDp: Float = 12f,
    /** Wraparound cylinder strength (0 = flat, 1 = max edge yaw). */
    val wrapCurvature: Float = DEFAULT_WRAP_CURVATURE,
    /** Horizontal span of the workspace canvas (0.6–1.4× display). */
    val workspaceWidth: Float = 1f,
    /** Vertical span of the workspace canvas (0.6–1.4× display). */
    val workspaceHeight: Float = DEFAULT_WORKSPACE_HEIGHT,
    /** Manual look yaw (degrees). IMU offset added via [WorkspaceLookOffset]. */
    val lookYawDegrees: Float = 0f,
    /** Manual look pitch (degrees). */
    val lookPitchDegrees: Float = 0f,
) {
    fun clamped(): WorkspaceAppearance = copy(
        uiScale = uiScale.coerceIn(MIN_UI_SCALE, MAX_UI_SCALE),
        panelGapDp = panelGapDp.coerceIn(MIN_PANEL_GAP_DP, MAX_PANEL_GAP_DP),
        wrapCurvature = wrapCurvature.coerceIn(MIN_WRAP_CURVATURE, MAX_WRAP_CURVATURE),
        workspaceWidth = workspaceWidth.coerceIn(MIN_WORKSPACE_SPAN, MAX_WORKSPACE_SPAN),
        workspaceHeight = workspaceHeight.coerceIn(MIN_WORKSPACE_SPAN, MAX_WORKSPACE_SPAN),
        lookYawDegrees = lookYawDegrees.coerceIn(MIN_LOOK_YAW, MAX_LOOK_YAW),
        lookPitchDegrees = lookPitchDegrees.coerceIn(MIN_LOOK_PITCH, MAX_LOOK_PITCH),
    )

    companion object {
        const val MIN_UI_SCALE = 0.75f
        const val MAX_UI_SCALE = 2f
        /** Glasses EXTERNAL displays report low DPI — 150% is readable out of the box. */
        const val DEFAULT_UI_SCALE = 1.5f
        const val MIN_PANEL_GAP_DP = 0f
        const val MAX_PANEL_GAP_DP = 200f
        const val MIN_WRAP_CURVATURE = 0f
        const val MAX_WRAP_CURVATURE = 1f
        const val DEFAULT_WRAP_CURVATURE = 0.35f
        const val MIN_WORKSPACE_SPAN = 0.6f
        const val MAX_WORKSPACE_SPAN = 1.4f
        const val DEFAULT_WORKSPACE_HEIGHT = 0.7f
        const val MIN_LOOK_YAW = -60f
        const val MAX_LOOK_YAW = 60f
        const val MIN_LOOK_PITCH = -30f
        const val MAX_LOOK_PITCH = 30f

        fun default(): WorkspaceAppearance = WorkspaceAppearance()
    }
}
