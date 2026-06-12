package dev.electrikjesus.xrlauncher.core.workspace

/** Launcher-owned surface in the spatial workspace. */
enum class PanelKind {
    APP_DRAWER,
    WIDGET,
    HOTSEAT,
    EMPTY_SLOT,
}

/** How a panel hosts a third-party app (Phase 3). */
enum class EmbedMode {
    NONE,
    EMBEDDED,
    FULL_WINDOW,
}

data class PanelState(
    val id: String,
    val kind: PanelKind,
    val visible: Boolean = true,
    val bounds: PanelBounds? = null,
)

data class Workspace(
    val id: String = DEFAULT_ID,
    val hotseatPins: List<String> = emptyList(),
    val panels: List<PanelState> = defaultPanels(),
    /** Tier 0 spatial desktop focused panel index (0-based). */
    val focusedPanelIndex: Int = 0,
    val appearance: WorkspaceAppearance = WorkspaceAppearance.default(),
) {
    companion object {
        const val DEFAULT_ID = "default"

        fun default(): Workspace = Workspace(
            hotseatPins = emptyList(),
            panels = defaultPanels(),
        )

        fun defaultPanels(): List<PanelState> = listOf(
            PanelState(id = "widget_clock", kind = PanelKind.WIDGET),
            PanelState(id = "widget_calendar", kind = PanelKind.WIDGET),
            PanelState(id = "app_drawer", kind = PanelKind.APP_DRAWER),
            PanelState(id = "hotseat", kind = PanelKind.HOTSEAT),
            PanelState(id = "empty_slot", kind = PanelKind.EMPTY_SLOT, visible = false),
        )
    }
}
