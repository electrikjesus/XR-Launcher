package dev.electrikjesus.xrlauncher.core.workspace

/** Normalized panel frame (0..1) within the glasses workspace content area. */
data class PanelBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    fun clamp(): PanelBounds {
        val w = width.coerceIn(MIN_SIZE, 1f)
        val h = height.coerceIn(MIN_SIZE, 1f)
        return copy(
            x = x.coerceIn(0f, 1f - w),
            y = y.coerceIn(0f, 1f - h),
            width = w,
            height = h,
        )
    }

    companion object {
        const val MIN_SIZE = 0.12f
    }
}

enum class LayoutPreset {
    /** Vertical stack — default column layout (no freeform bounds). */
    STANDARD,
    /** Widgets top, drawer fills center, hotseat bottom. */
    SINGLE,
    /** Hotseat column left, app drawer right. */
    DUAL,
    /** Three columns: clock · drawer · hotseat. */
    TRIPTYCH,
}

object WorkspaceLayoutPresets {
    fun apply(panels: List<PanelState>, preset: LayoutPreset): List<PanelState> {
        if (preset == LayoutPreset.STANDARD) {
            return panels.map { it.copy(bounds = null) }
        }
        val boundsById = presetBounds(preset)
        return panels.map { panel ->
            panel.copy(bounds = boundsById[panel.id]?.clamp())
        }
    }

    fun usesFreeformLayout(panels: List<PanelState>): Boolean =
        panels.any { it.visible && it.bounds != null }

    /** Match saved panels to a preset chip, or null when layout was customized. */
    fun inferPreset(panels: List<PanelState>): LayoutPreset? {
        if (!usesFreeformLayout(panels)) return LayoutPreset.STANDARD
        return LayoutPreset.entries
            .filter { it != LayoutPreset.STANDARD }
            .firstOrNull { preset -> panelsMatchPreset(panels, preset) }
    }

    internal fun panelsMatchPreset(panels: List<PanelState>, preset: LayoutPreset): Boolean {
        val expected = apply(Workspace.defaultPanels(), preset)
        if (panels.size != expected.size) return false
        return panels.zip(expected).all { (actual, exp) ->
            actual.id == exp.id &&
                actual.visible == exp.visible &&
                boundsEqual(actual.bounds, exp.bounds)
        }
    }

    private fun boundsEqual(a: PanelBounds?, b: PanelBounds?): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        return kotlin.math.abs(a.x - b.x) < 0.001f &&
            kotlin.math.abs(a.y - b.y) < 0.001f &&
            kotlin.math.abs(a.width - b.width) < 0.001f &&
            kotlin.math.abs(a.height - b.height) < 0.001f
    }

    private fun presetBounds(preset: LayoutPreset): Map<String, PanelBounds> = when (preset) {
        LayoutPreset.STANDARD -> emptyMap()
        LayoutPreset.SINGLE -> mapOf(
            "widget_clock" to PanelBounds(0.02f, 0.02f, 0.47f, 0.14f),
            "widget_calendar" to PanelBounds(0.51f, 0.02f, 0.47f, 0.14f),
            "app_drawer" to PanelBounds(0.02f, 0.18f, 0.96f, 0.64f),
            "hotseat" to PanelBounds(0.02f, 0.84f, 0.96f, 0.14f),
        )
        LayoutPreset.DUAL -> mapOf(
            "widget_clock" to PanelBounds(0.02f, 0.02f, 0.47f, 0.14f),
            "widget_calendar" to PanelBounds(0.51f, 0.02f, 0.47f, 0.14f),
            "hotseat" to PanelBounds(0.02f, 0.18f, 0.28f, 0.80f),
            "app_drawer" to PanelBounds(0.32f, 0.18f, 0.66f, 0.80f),
        )
        LayoutPreset.TRIPTYCH -> mapOf(
            "widget_clock" to PanelBounds(0.02f, 0.02f, 0.30f, 0.46f),
            "widget_calendar" to PanelBounds(0.02f, 0.50f, 0.30f, 0.48f),
            "app_drawer" to PanelBounds(0.34f, 0.02f, 0.32f, 0.96f),
            "hotseat" to PanelBounds(0.68f, 0.02f, 0.30f, 0.96f),
        )
    }
}
