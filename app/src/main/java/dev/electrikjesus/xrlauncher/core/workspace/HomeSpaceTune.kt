package dev.electrikjesus.xrlauncher.core.workspace

enum class HomeSpaceTuneAxis {
    PANEL,
    SPHERE,
    ELEMENT,
    DESK_ICONS,
    DESK_PILES,
    DESK_TILES,
    DESK_WIDGETS,
    DESK_SNAP,
    DESK_SHOW_GRID,
    LOOK_FPS,
    LOOK_GESTURE,
}

enum class HomeSpaceEditPage {
    PERSPECTIVE,
    DESKTOP,
}

/** Live Edit-mode nudges for Home Space. Persisted via [WorkspaceAppearance]. */
object HomeSpaceTune {
    const val STEP = 0.05f

    fun apply(
        appearance: WorkspaceAppearance,
        axis: HomeSpaceTuneAxis,
        delta: Float,
    ): WorkspaceAppearance = when (axis) {
        HomeSpaceTuneAxis.PANEL -> appearance.copy(panelScale = appearance.panelScale + delta)
        HomeSpaceTuneAxis.SPHERE -> appearance.copy(sphereScale = appearance.sphereScale + delta)
        HomeSpaceTuneAxis.ELEMENT -> appearance.copy(uiScale = appearance.uiScale + delta)
        HomeSpaceTuneAxis.DESK_ICONS -> appearance.copy(desktopIcons = !appearance.desktopIcons)
        HomeSpaceTuneAxis.DESK_PILES -> appearance.copy(desktopPiles = !appearance.desktopPiles)
        HomeSpaceTuneAxis.DESK_TILES -> appearance.copy(desktopTiles = !appearance.desktopTiles)
        HomeSpaceTuneAxis.DESK_WIDGETS -> appearance.copy(desktopWidgets = !appearance.desktopWidgets)
        HomeSpaceTuneAxis.DESK_SNAP -> appearance.copy(deskSnapToGrid = !appearance.deskSnapToGrid)
        HomeSpaceTuneAxis.DESK_SHOW_GRID ->
            appearance.copy(deskShowGridOnMove = !appearance.deskShowGridOnMove)
        // delta > 0 → force FPS; delta < 0 → force GRADIENT; 0 → toggle (Edit Look button).
        HomeSpaceTuneAxis.LOOK_FPS -> appearance.copy(
            lookMode = when {
                delta > 0f -> GlassesLookMode.FPS
                delta < 0f -> GlassesLookMode.GRADIENT
                else -> if (appearance.lookMode == GlassesLookMode.FPS) {
                    GlassesLookMode.GRADIENT
                } else {
                    GlassesLookMode.FPS
                }
            },
        )
        HomeSpaceTuneAxis.LOOK_GESTURE -> appearance.copy(lookMode = GlassesLookMode.GESTURE)
    }.clamped()

    fun enabled(appearance: WorkspaceAppearance, axis: HomeSpaceTuneAxis): Boolean = when (axis) {
        HomeSpaceTuneAxis.DESK_ICONS -> appearance.desktopIcons
        HomeSpaceTuneAxis.DESK_PILES -> appearance.desktopPiles
        HomeSpaceTuneAxis.DESK_TILES -> appearance.desktopTiles
        HomeSpaceTuneAxis.DESK_WIDGETS -> appearance.desktopWidgets
        HomeSpaceTuneAxis.DESK_SNAP -> appearance.deskSnapToGrid
        HomeSpaceTuneAxis.DESK_SHOW_GRID -> appearance.deskShowGridOnMove
        HomeSpaceTuneAxis.LOOK_FPS -> appearance.lookMode == GlassesLookMode.FPS
        else -> true
    }
}
