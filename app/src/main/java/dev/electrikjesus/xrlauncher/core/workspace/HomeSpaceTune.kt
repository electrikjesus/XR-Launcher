package dev.electrikjesus.xrlauncher.core.workspace

enum class HomeSpaceTuneAxis {
    PANEL,
    SPHERE,
    ELEMENT,
}

/** Live Edit-mode nudges for Home Space scale. Persisted via [WorkspaceAppearance]. */
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
    }.clamped()
}
