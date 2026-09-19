package dev.electrikjesus.xrlauncher.core.workspace

/** Visual tuning for the glasses launcher shell (Tier 1). */
data class WorkspaceAppearance(
    /** Scales launcher panels, text, Home icons, and Desktop BumpDesk tiles (0.75–2.0). */
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
    /** Backdrop preset for the GLES cylinder and Compose wallpaper layer. */
    val wallpaperChoice: WorkspaceWallpaperChoice = WorkspaceWallpaperChoice.SYSTEM,
    /** Multiplies pane width/height on the sphere (0.6–1.4). */
    val panelScale: Float = DEFAULT_PANEL_SCALE,
    /** Multiplies pane-sphere and room radius so the same-size panes sit farther from the camera (0.8–2.5). */
    val sphereScale: Float = DEFAULT_SPHERE_SCALE,
    /** BumpDesk item types allowed on the inner-sphere Desktop. */
    val desktopIcons: Boolean = true,
    val desktopPiles: Boolean = true,
    val desktopTiles: Boolean = true,
    val desktopWidgets: Boolean = true,
    /** Persisted Home Space look: gradient mouse-look vs FPS capture. */
    val lookMode: GlassesLookMode = GlassesLookMode.GRADIENT,
    /**
     * Fraction of center-to-edge (0–0.5) where cursor X does not pan look.
     * 0 keeps the sine curve from the center.
     */
    val lookDeadzoneX: Float = 0f,
    /** Same as [lookDeadzoneX] for cursor Y / pitch. */
    val lookDeadzoneY: Float = 0f,
) {
    fun clamped(): WorkspaceAppearance = copy(
        uiScale = uiScale.coerceIn(MIN_UI_SCALE, MAX_UI_SCALE),
        panelGapDp = panelGapDp.coerceIn(MIN_PANEL_GAP_DP, MAX_PANEL_GAP_DP),
        wrapCurvature = wrapCurvature.coerceIn(MIN_WRAP_CURVATURE, MAX_WRAP_CURVATURE),
        workspaceWidth = workspaceWidth.coerceIn(MIN_WORKSPACE_SPAN, MAX_WORKSPACE_SPAN),
        workspaceHeight = workspaceHeight.coerceIn(MIN_WORKSPACE_SPAN, MAX_WORKSPACE_SPAN),
        lookYawDegrees = lookYawDegrees.coerceIn(MIN_LOOK_YAW, MAX_LOOK_YAW),
        lookPitchDegrees = lookPitchDegrees.coerceIn(MIN_LOOK_PITCH, MAX_LOOK_PITCH),
        wallpaperChoice = wallpaperChoice,
        panelScale = panelScale.coerceIn(MIN_PANEL_SCALE, MAX_PANEL_SCALE),
        sphereScale = sphereScale.coerceIn(MIN_SPHERE_SCALE, MAX_SPHERE_SCALE),
        desktopIcons = desktopIcons,
        desktopPiles = desktopPiles,
        desktopTiles = desktopTiles,
        desktopWidgets = desktopWidgets,
        lookMode = lookMode,
        lookDeadzoneX = lookDeadzoneX.coerceIn(MIN_LOOK_DEADZONE, MAX_LOOK_DEADZONE),
        lookDeadzoneY = lookDeadzoneY.coerceIn(MIN_LOOK_DEADZONE, MAX_LOOK_DEADZONE),
    )

    companion object {
        const val MIN_UI_SCALE = 0.75f
        const val MAX_UI_SCALE = 2f
        /** Scales launcher panels, text, and icons (0.75–2.0). On-device Edit session: 1.20. */
        const val DEFAULT_UI_SCALE = 1.20f
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
        const val MIN_PANEL_SCALE = 0.5f
        const val MAX_PANEL_SCALE = 1.4f
        /** On-device Edit session: 0.70 fills the view without crowding neighbors. */
        const val DEFAULT_PANEL_SCALE = 0.70f
        const val MIN_SPHERE_SCALE = 0.8f
        const val MAX_SPHERE_SCALE = 2.5f
        /**
         * 1.0 keeps today's on-screen pane size. Values above 1 pull the same-size panes
         * farther from the camera (smaller on screen, more room around them).
         */
        const val DEFAULT_SPHERE_SCALE = 1.0f
        const val MIN_LOOK_DEADZONE = 0f
        /** Half of the center-to-edge span. 0.5 means look starts only at mid-screen. */
        const val MAX_LOOK_DEADZONE = 0.5f

        fun default(): WorkspaceAppearance = WorkspaceAppearance()
    }
}
