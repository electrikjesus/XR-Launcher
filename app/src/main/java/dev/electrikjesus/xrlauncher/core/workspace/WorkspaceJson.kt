package dev.electrikjesus.xrlauncher.core.workspace

import java.util.Locale

/**
 * Compact workspace persistence (Phase 2.8 partial).
 * Format: `{id}|{pin1};{pin2}|{panel1},{panel2},…`
 * Panel: `id~KIND~visible~x~y~w~h` (bounds omitted when stack layout).
 */
object WorkspaceJson {
    private const val FIELD_SEP = "|"
    private const val PIN_SEP = ";"
    private const val PANEL_SEP = ","
    private const val PANEL_FIELD_SEP = "~"

    fun encode(workspace: Workspace): String {
        val pins = workspace.hotseatPins.joinToString(PIN_SEP)
        val panelPart = workspace.panels.joinToString(PANEL_SEP) { encodePanel(it) }
        return listOf(
            workspace.id,
            pins,
            panelPart,
            workspace.focusedPanelIndex.toString(),
            encodeAppearance(workspace.appearance),
        ).joinToString(FIELD_SEP)
    }

    fun decode(raw: String): Workspace {
        if (raw.isBlank()) return Workspace.default()
        val parts = raw.split(FIELD_SEP, limit = 5)
        val id = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: Workspace.DEFAULT_ID
        val pins = parts.getOrElse(1) { "" }
            .split(PIN_SEP)
            .filter { it.isNotBlank() }
        val panels = if (parts.size >= 3 && parts[2].isNotBlank()) {
            parts[2].split(PANEL_SEP).mapNotNull { decodePanel(it) }
        } else {
            Workspace.defaultPanels()
        }
        val focusIndex = parts.getOrElse(3) { "0" }.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val appearance = decodeAppearance(parts.getOrElse(4) { "" })
        return Workspace(
            id = id,
            hotseatPins = pins,
            panels = mergeWithDefaults(panels),
            focusedPanelIndex = focusIndex,
            appearance = appearance,
        )
    }

    internal fun encodeAppearance(appearance: WorkspaceAppearance): String {
        val clamped = appearance.clamped()
        return listOf(
            clamped.uiScale.toCompactString(),
            clamped.panelGapDp.toCompactString(),
            clamped.wrapCurvature.toCompactString(),
            clamped.workspaceWidth.toCompactString(),
            clamped.workspaceHeight.toCompactString(),
            clamped.lookYawDegrees.toCompactString(),
            clamped.lookPitchDegrees.toCompactString(),
        ).joinToString(PANEL_FIELD_SEP)
    }

    internal fun decodeAppearance(raw: String): WorkspaceAppearance {
        if (raw.isBlank()) return WorkspaceAppearance.default()
        val fields = raw.split(PANEL_FIELD_SEP)
        return WorkspaceAppearance(
            uiScale = fields.getOrNull(0)?.toFloatOrNull() ?: WorkspaceAppearance.DEFAULT_UI_SCALE,
            panelGapDp = fields.getOrNull(1)?.toFloatOrNull() ?: 12f,
            wrapCurvature = fields.getOrNull(2)?.toFloatOrNull()
                ?: WorkspaceAppearance.DEFAULT_WRAP_CURVATURE,
            workspaceWidth = fields.getOrNull(3)?.toFloatOrNull() ?: 1f,
            workspaceHeight = fields.getOrNull(4)?.toFloatOrNull()
                ?: WorkspaceAppearance.DEFAULT_WORKSPACE_HEIGHT,
            lookYawDegrees = fields.getOrNull(5)?.toFloatOrNull() ?: 0f,
            lookPitchDegrees = fields.getOrNull(6)?.toFloatOrNull() ?: 0f,
        ).clamped()
    }

    internal fun encodePanel(panel: PanelState): String {
        val visibleFlag = if (panel.visible) "1" else "0"
        val bounds = panel.bounds
        return if (bounds != null) {
            listOf(
                panel.id,
                panel.kind.name,
                visibleFlag,
                bounds.x.toCompactString(),
                bounds.y.toCompactString(),
                bounds.width.toCompactString(),
                bounds.height.toCompactString(),
            ).joinToString(PANEL_FIELD_SEP)
        } else {
            listOf(panel.id, panel.kind.name, visibleFlag).joinToString(PANEL_FIELD_SEP)
        }
    }

    internal fun decodePanel(raw: String): PanelState? {
        val fields = raw.split(PANEL_FIELD_SEP)
        if (fields.size < 3) return null
        val kind = runCatching { PanelKind.valueOf(fields[1]) }.getOrNull() ?: return null
        val visible = fields[2] != "0"
        val bounds = if (fields.size >= 7) {
            PanelBounds(
                x = fields[3].toFloatOrNull() ?: return null,
                y = fields[4].toFloatOrNull() ?: return null,
                width = fields[5].toFloatOrNull() ?: return null,
                height = fields[6].toFloatOrNull() ?: return null,
            ).clamp()
        } else {
            null
        }
        return PanelState(id = fields[0], kind = kind, visible = visible, bounds = bounds)
    }

    private fun mergeWithDefaults(decoded: List<PanelState>): List<PanelState> {
        val decodedById = decoded.associateBy { it.id }
        val defaults = Workspace.defaultPanels()
        return defaults.map { default ->
            decodedById[default.id] ?: default
        }
    }

    private fun Float.toCompactString(): String = String.format(Locale.US, "%.4f", this)
}
