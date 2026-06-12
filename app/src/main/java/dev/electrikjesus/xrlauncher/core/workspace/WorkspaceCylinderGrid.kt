package dev.electrikjesus.xrlauncher.core.workspace

import kotlin.math.roundToInt

/**
 * Discrete slot grid for cylinder panel placement and freeform snap.
 * Shared by Compose layout, persistence, and GLES guide quads.
 */
object WorkspaceCylinderGrid {
    const val COLUMNS = 6
    const val ROWS = 4

    data class Slot(
        val col: Int,
        val row: Int,
        val colSpan: Int = 1,
        val rowSpan: Int = 1,
    ) {
        init {
            require(col in 0 until COLUMNS)
            require(row in 0 until ROWS)
            require(colSpan >= 1 && col + colSpan <= COLUMNS)
            require(rowSpan >= 1 && row + rowSpan <= ROWS)
        }
    }

    data class SlotCenter(
        val centerX: Float,
        val centerY: Float,
        val widthNorm: Float,
        val heightNorm: Float,
    )

    val cellWidth: Float get() = 1f / COLUMNS
    val cellHeight: Float get() = 1f / ROWS

    fun slotCenter(slot: Slot): SlotCenter {
        val widthNorm = slot.colSpan * cellWidth
        val heightNorm = slot.rowSpan * cellHeight
        return SlotCenter(
            centerX = slot.col * cellWidth + widthNorm / 2f,
            centerY = slot.row * cellHeight + heightNorm / 2f,
            widthNorm = widthNorm,
            heightNorm = heightNorm,
        )
    }

    fun boundsForSlot(slot: Slot): PanelBounds {
        val width = slot.colSpan * cellWidth
        val height = slot.rowSpan * cellHeight
        return PanelBounds(
            x = slot.col * cellWidth,
            y = slot.row * cellHeight,
            width = width,
            height = height,
        ).clamp()
    }

    /** Default stack-layout slot per panel id (STANDARD preset). */
    fun defaultStackSlot(panelId: String): Slot? = when (panelId) {
        "widget_clock" -> Slot(col = 0, row = 0, colSpan = 3, rowSpan = 1)
        "widget_calendar" -> Slot(col = 3, row = 0, colSpan = 3, rowSpan = 1)
        "app_drawer" -> Slot(col = 0, row = 1, colSpan = 6, rowSpan = 2)
        "hotseat" -> Slot(col = 0, row = 3, colSpan = 6, rowSpan = 1)
        else -> null
    }

    fun defaultStackBounds(panelId: String): PanelBounds? =
        defaultStackSlot(panelId)?.let { boundsForSlot(it) }

    fun stackCentersForPanels(panels: List<PanelState>): List<Pair<String, SlotCenter>> =
        panels.mapNotNull { panel ->
            defaultStackSlot(panel.id)?.let { slot ->
                panel.id to slotCenter(slot)
            }
        }

    fun guideSlotsForPanels(panels: List<PanelState>): List<SlotCenter> =
        panels.filter { it.visible && it.kind != PanelKind.EMPTY_SLOT }
            .mapNotNull { panel ->
                panel.bounds?.let { boundsToSlotCenter(it) }
                    ?: defaultStackSlot(panel.id)?.let { slotCenter(it) }
            }

    fun boundsToSlotCenter(bounds: PanelBounds): SlotCenter = SlotCenter(
        centerX = bounds.x + bounds.width / 2f,
        centerY = bounds.y + bounds.height / 2f,
        widthNorm = bounds.width,
        heightNorm = bounds.height,
    )

    fun snapBounds(bounds: PanelBounds): PanelBounds {
        val snappedWidth = snapSize(bounds.width, cellWidth)
        val snappedHeight = snapSize(bounds.height, cellHeight)
        return PanelBounds(
            x = snapCoordinate(bounds.x, cellWidth, snappedWidth),
            y = snapCoordinate(bounds.y, cellHeight, snappedHeight),
            width = snappedWidth,
            height = snappedHeight,
        ).clamp()
    }

    fun snapCoordinate(value: Float, cellSize: Float, spanSize: Float): Float {
        val maxOrigin = (1f - spanSize).coerceAtLeast(0f)
        val snapped = (value / cellSize).roundToInt() * cellSize
        return snapped.coerceIn(0f, maxOrigin)
    }

    fun snapSize(value: Float, cellSize: Float): Float {
        val cells = (value / cellSize).roundToInt().coerceAtLeast(1)
        return (cells * cellSize).coerceAtMost(1f)
    }

    /** Move all panels to grid-aligned freeform bounds (e.g. when first widget is dragged). */
    fun migrateStackToFreeformBounds(panels: List<PanelState>): List<PanelState> =
        panels.map { panel ->
            if (!panel.visible || panel.kind == PanelKind.EMPTY_SLOT) return@map panel
            panel.copy(bounds = defaultStackBounds(panel.id) ?: panel.bounds)
        }
}
