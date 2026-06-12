package dev.electrikjesus.xrlauncher.core.workspace

/**
 * Compact workspace persistence format (Phase 2.2).
 * Format: `{id}|{pin1};{pin2};…` — panel layout uses [Workspace.defaultPanels] until 2.8.
 */
object WorkspaceJson {
    private const val FIELD_SEP = "|"
    private const val PIN_SEP = ";"

    fun encode(workspace: Workspace): String =
        workspace.id + FIELD_SEP + workspace.hotseatPins.joinToString(PIN_SEP)

    fun decode(raw: String): Workspace {
        if (raw.isBlank()) return Workspace.default()
        val parts = raw.split(FIELD_SEP, limit = 2)
        val id = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: Workspace.DEFAULT_ID
        val pins = parts.getOrElse(1) { "" }
            .split(PIN_SEP)
            .filter { it.isNotBlank() }
        return Workspace(
            id = id,
            hotseatPins = pins,
            panels = Workspace.defaultPanels(),
        )
    }
}
